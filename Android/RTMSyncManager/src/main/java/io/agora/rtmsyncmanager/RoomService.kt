package io.agora.rtmsyncmanager

import io.agora.rtmsyncmanager.model.AUIRoomContext
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.service.callback.AUIException
import io.agora.rtmsyncmanager.service.room.AUIRoomManager
import io.agora.rtmsyncmanager.service.rtm.AUIRtmException
import io.agora.rtmsyncmanager.utils.AUILogger

class RoomService(
    private val expirationPolicy: RoomExpirationPolicy,
    private val roomManager: AUIRoomManager,
    private val syncManager: SyncManager
) {
    private val tag = "RoomService"
    private val roomInfoMap: MutableMap<String, AUIRoomInfo> = mutableMapOf()

    fun getRoomList(
        appId: String,
        sceneId: String,
        lastCreateTime: Long,
        pageSize: Int,
        completion: (AUIException?, Long?, List<AUIRoomInfo>?) -> Unit
    ) {
        // 房间列表会返回最新的服务端时间ts
        roomManager.getRoomInfoList(appId, sceneId, lastCreateTime, pageSize) { err, roomList, ts ->
            if (err != null || ts == null) {
                completion(err, ts, null)
                return@getRoomInfoList
            }

            val list: MutableList<AUIRoomInfo> = mutableListOf()
            roomList?.forEach { roomInfo ->
                // 遍历每个房间信息，查询是否已经过期
                if (expirationPolicy.expirationTime > 0 && ts - roomInfo.createTime >= expirationPolicy.expirationTime) {
                    val scene = syncManager.createScene(roomInfo.roomId, expirationPolicy)
                    scene.delete()
                    roomManager.destroyRoom(appId, sceneId, roomInfo.roomId) {}
                    return@forEach
                }

                list.add(roomInfo)
            }
            completion(null, ts, list)
        }
    }

    // TODO: 将 AUIRoomInfo 替换为接口 IAUIRoomInfo。服务端会创建房间id，这里是否 roomManager 创建后往外抛 roomId
    fun createRoom(appId: String, sceneId: String, room: AUIRoomInfo, completion: (AUIRtmException?, AUIRoomInfo?) -> Unit) {
        val scene = syncManager.createScene(channelName = room.roomId, roomExpiration = expirationPolicy)
        roomManager.createRoom(appId, sceneId, room) { err, roomInfo ->
            if (err != null) {
                completion(AUIRtmException(err.code, err.message.toString(), ""), null)
                return@createRoom
            }

            if (roomInfo == null) {
                return@createRoom
            }

            roomInfoMap[roomInfo.roomId] = roomInfo

            scene.create(createTime = roomInfo.createTime, payload = null) { error ->
                if (error != null) {
                    // 失败需要清理脏房间信息
                    createRoomRevert(appId, sceneId, room.roomId)
                    completion(error, null)
                    return@create
                }

                scene.enter { _, err ->
                    if (err != null) {
                        // 失败需要清理脏房间信息
                        createRoomRevert(appId, sceneId, room.roomId)
                        completion(err, null)
                        return@enter
                    }

                    completion(null, roomInfo)
                }
            }
        }
    }

    fun enterRoom(appId: String, sceneId: String, room: AUIRoomInfo, completion: (AUIRtmException?, AUIRoomInfo?) -> Unit) {
        val scene = syncManager.createScene(channelName = room.roomId, roomExpiration = expirationPolicy)
        scene.enter { _, err ->
            if (err != null) {
                enterRoomRevert(appId, sceneId, room.roomId)
                completion(err, null)
                return@enter
            }

            roomInfoMap[room.roomId] = room
            completion(null, room)
        }
    }

    fun leaveRoom(appId: String, sceneId: String, roomId: String) {
        val scene = syncManager.getScene(roomId)
        val isOwner = roomInfoMap[roomId]?.roomOwner?.userId == AUIRoomContext.shared().currentUserInfo.userId

        if (isOwner) {
            roomManager.destroyRoom(appId, sceneId, roomId) {}
            scene?.delete()
        } else {
            scene?.leave()
        }

        roomInfoMap.remove(roomId)
    }

    private fun createRoomRevert(appId: String, sceneId: String, roomId: String) {
        AUILogger.logger().d(tag, "createRoomRevert: $roomId")
        leaveRoom(appId, sceneId, roomId)
    }

    private fun enterRoomRevert(appId: String, sceneId: String, roomId: String) {
        AUILogger.logger().d(tag, "enterRoomRevert: $roomId")
        leaveRoom(appId, sceneId, roomId)
    }
}