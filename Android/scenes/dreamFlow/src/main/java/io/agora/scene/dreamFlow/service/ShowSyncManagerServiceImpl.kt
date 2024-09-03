package io.agora.scene.dreamFlow.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import io.agora.rtm.*
import io.agora.rtmsyncmanager.*
import io.agora.rtmsyncmanager.model.*
import io.agora.rtmsyncmanager.service.IAUIUserService
import io.agora.rtmsyncmanager.service.http.HttpManager
import io.agora.rtmsyncmanager.service.room.AUIRoomManager
import io.agora.rtmsyncmanager.service.rtm.AUIRtmUserLeaveReason
import io.agora.rtmsyncmanager.utils.AUILogger
import io.agora.rtmsyncmanager.utils.GsonTools
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.manager.UserManager
import io.agora.scene.dreamFlow.DreamFlowLogger
import io.agora.scene.dreamFlow.RtcEngineInstance
import io.agora.scene.dreamFlow.utils.DreamFlowConstants
import org.json.JSONException
import org.json.JSONObject
import java.util.*


/**
 * Show sync manager service impl
 *
 * @property context
 * @property errorHandler
 * @constructor Create empty Show sync manager service impl
 */
class ShowSyncManagerServiceImpl constructor(
    private val context: Context,
    private val errorHandler: (Exception) -> Unit
) : ShowServiceProtocol, ISceneResponse {
    /**
     * Tag
     */
    private val TAG = "DreamFlowService"

    /**
     * K scene id
     */
    private val kSceneId = "scene_dreamflow_1.3.100"

    private val syncManager: SyncManager

    private val roomManager = AUIRoomManager()

    private val roomService: RoomService

    private var roomList = mutableListOf<RoomDetailModel>()

    init {
        HttpManager.setBaseURL(BuildConfig.ROOM_MANAGER_SERVER_HOST)
        AUILogger.initLogger(AUILogger.Config(context, "eCommerce", logCallback = object: AUILogger.AUILogCallback {
            override fun onLogDebug(tag: String, message: String) {
                DreamFlowLogger.d(tag, message)
            }

            override fun onLogInfo(tag: String, message: String) {
                DreamFlowLogger.d(tag, message)
            }

            override fun onLogWarning(tag: String, message: String) {
                DreamFlowLogger.d(tag, message)
            }

            override fun onLogError(tag: String, message: String) {
                DreamFlowLogger.e(tag, null, message)
            }
        }))

        val commonConfig = AUICommonConfig()
        commonConfig.context = context
        commonConfig.appId = BuildConfig.AGORA_APP_ID
        val owner = AUIUserThumbnailInfo()
        owner.userId = UserManager.getInstance().user.id.toString()
        owner.userName = UserManager.getInstance().user.name
        owner.userAvatar = UserManager.getInstance().user.headUrl
        commonConfig.owner = owner
        commonConfig.host = BuildConfig.ROOM_MANAGER_SERVER_HOST
        AUIRoomContext.shared().setCommonConfig(commonConfig)
        syncManager = SyncManager(context, null, commonConfig)

        val roomExpirationPolicy = RoomExpirationPolicy()
        roomExpirationPolicy.expirationTime = ShowServiceProtocol.ROOM_AVAILABLE_DURATION
        roomService = RoomService(roomExpirationPolicy, roomManager, syncManager)
        rtmLogin {}
    }

    /**
     * Room info controller
     *
     * @property roomId
     * @property scene
     * @property roomInfo
     * @property rtmChannel
     * @property userList
     * @property userChangeSubscriber
     * @property userJoinSubscriber
     * @property userLeaveSubscriber
     * @property roomChangeSubscriber
     * @constructor Create empty Room info controller
     */
    data class RoomInfoController constructor(
        val roomId: String,
        val scene: Scene,
        val roomInfo: RoomDetailModel,
        val rtmChannel: StreamChannel? = null,
        var userList: MutableList<AUIUserInfo> = mutableListOf(),
        var userChangeSubscriber: ((Int) -> Unit)? = null,
        var userJoinSubscriber: ((String, String, String) -> Unit)? = null,
        var userLeaveSubscriber: ((String, String, String) -> Unit)? = null,
        var roomChangeSubscriber: ((event: ShowRoomStatus) -> Unit)? = null,
    )

    /**
     * Room info controllers
     */
    private val roomInfoControllers = Collections.synchronizedList(mutableListOf<RoomInfoController>())

    /**
     * Destroy
     *
     */
    override fun destroy() {
        DreamFlowLogger.d(TAG, "destroy")
        synchronized(roomInfoControllers){
            roomInfoControllers.forEach {
                cleanRoomInfoController(it)
            }
        }
        roomInfoControllers.clear()
        syncManager.logout()
        syncManager.release()
        isRtmLogin = false
        RtmClient.release()
    }

    override fun getCurrentTimestamp(roomId: String): Long {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return 0L
        return controller.scene.getCurrentTs()
    }

    override fun getCurrentRoomDuration(roomId: String): Long {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return 0L
        return controller.scene.getRoomDuration()
    }

    override fun getRoomInfo(roomId: String): RoomDetailModel? {
        return roomList.firstOrNull { it.roomId == roomId }
    }

    override fun getRoomList(): List<RoomDetailModel> {
        return roomList
    }

    override fun fetchRoomList(
        success: (List<RoomDetailModel>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        rtmLogin {
           if (it) {
               roomService.getRoomList(BuildConfig.AGORA_APP_ID, kSceneId, System.currentTimeMillis(), 20, { room ->
                   room.roomOwner?.userId == UserManager.getInstance().user.id.toString()
               }) { e, ts, list ->
                   if (e != null) {
                       roomList = mutableListOf()
                       runOnMainThread { success.invoke(roomList) }
                   }
                   if (list != null) {
                       roomList = toDetailList(list)
                       roomList = removeReportRooms(roomList)
                       runOnMainThread { success.invoke(roomList) }
                   }
               }
           }
        }
    }

    override fun createRoom(roomId: String, roomName: String, thumbnailId: String, success: (RoomDetailModel) -> Unit, error: ((Exception) -> Unit)?) {
        rtmLogin { rtmLogin ->
            if (!rtmLogin) {
                error?.invoke(Exception("rtm login failed"))
                return@rtmLogin
            }

            val roomInfo = AUIRoomInfo()
            roomInfo.roomId = roomId
            roomInfo.roomName = roomName
            val owner = AUIUserThumbnailInfo()
            owner.userId = UserManager.getInstance().user.id.toString()
            owner.userName = UserManager.getInstance().user.name
            owner.userAvatar = UserManager.getInstance().user.headUrl
            roomInfo.roomOwner = owner
            // TODO roomInfo.thumbnail = thumbnailId
            // roomInfo.createTime = TimeUtils.currentTimeMillis()
            val roomDetailModel = RoomDetailModel(
                roomId,
                roomName,
                1,
                thumbnailId,
                UserManager.getInstance().user.id.toString(),
                UserManager.getInstance().user.headUrl,
                UserManager.getInstance().user.name,
                ShowRoomStatus.activity.value,
                System.currentTimeMillis(),
                System.currentTimeMillis()
            )
            roomInfo.customPayload = GsonTools.beanToMap(roomDetailModel)
            roomService.createRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomInfo) { e, info ->
                if (info != null) {
                    val temp = mutableListOf<RoomDetailModel>()
                    temp.add(roomDetailModel)
                    temp.addAll(roomList)
                    roomList = temp
                    success.invoke(roomDetailModel)
                }
                if (e != null) {
                    error?.invoke(Exception(e.message))
                }
            }

            val scene = syncManager.createScene(roomInfo.roomId)
            val controller = RoomInfoController(roomInfo.roomId, scene, roomDetailModel)
            roomInfoControllers.add(controller)
            scene.userService.registerRespObserver(userObserver)
            DreamFlowLogger.d(TAG, "[commerce] add controller ${roomInfo.roomId}， roomInfoControllers：${roomInfoControllers}")
            scene.bindRespDelegate(this)
        }
    }

    override fun joinRoom(roomInfo: RoomDetailModel, success: (RoomDetailModel) -> Unit, error: ((Exception) -> Unit)?) {
        rtmLogin { rtmLogin ->
            if (!rtmLogin) {
                error?.invoke(Exception("rtm login failed"))
                return@rtmLogin
            }
            if (roomInfo.ownerId.toLong() != UserManager.getInstance().user.id) {
                val scene = syncManager.createScene(roomInfo.roomId)
                val controller = RoomInfoController(roomInfo.roomId, scene, roomInfo)
                roomInfoControllers.add(controller)

                scene.userService.registerRespObserver(userObserver)

                DreamFlowLogger.d(TAG, "[commerce] add controller ${roomInfo.roomId}， roomInfoControllers：${roomInfoControllers}")
                scene.bindRespDelegate(this)
                roomService.enterRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomInfo.roomId) { e ->
                    if (e == null) {
                        success.invoke(roomInfo)
                    } else {
                        error?.invoke(java.lang.Exception(e.message))
                    }
                }
            } else {
                //addOriginalData(controller)
                success.invoke(roomInfo)
            }
        }
    }

    override fun leaveRoom(roomId: String) {
        DreamFlowLogger.d(TAG, "leaveRoom: $roomId")
        val scene = syncManager.createScene(roomId)
        scene.unbindRespDelegate(this)
        scene.userService.unRegisterRespObserver(userObserver)

        roomService.leaveRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId)
        roomInfoControllers.forEach { c ->
            if (c.roomId == roomId) {
                cleanRoomInfoController(c)
            }
        }
        syncManager.removeScene(roomId)
        roomInfoControllers.removeAll { it.roomId == roomId }
    }

    override fun deleteRoom(roomId: String, complete: () -> Unit) {
        DreamFlowLogger.d(TAG, "deleteRoom: $roomId")
        syncManager.createScene(roomId).delete()
        roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId) { e ->
            complete.invoke()
        }
    }

    override fun subscribeCurrRoomEvent(roomId: String, onRoomEvent: (event: ShowRoomStatus) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.roomChangeSubscriber = onRoomEvent
    }
    private fun cleanRoomInfoController(controller: RoomInfoController) {
        DreamFlowLogger.d(TAG, "cleanRoomInfoController: ${controller.roomId}")
        controller.userChangeSubscriber = null
        controller.roomChangeSubscriber = null
    }

    /** User Actions */
    override fun subscribeUser(roomId: String, onChange: (count: Int) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        onChange.invoke(controller.userList.size)
        controller.userChangeSubscriber = onChange
    }

    override fun subscribeUserJoin(roomId: String, onChange: (userId: String, userName: String, userAvatar: String) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.userJoinSubscriber = onChange
    }

    override fun subscribeUserLeave(roomId: String, onChange: (userId: String, userName: String, userAvatar: String) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.userLeaveSubscriber = onChange
    }

    private val userObserver: IAUIUserService.AUIUserRespObserver = object: IAUIUserService.AUIUserRespObserver {
        override fun onRoomUserSnapshot(roomId: String, userList: List<AUIUserInfo?>?) {
            DreamFlowLogger.d(TAG, "onRoomUserSnapshot: $userList")
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            val list = userList?.mapNotNull { it }
            if (list != null) {
                controller.userList.clear()
                controller.userList.addAll(list)
            }
            renewRoomInfo(controller, controller.userList.size)
        }
        override fun onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
            DreamFlowLogger.d(TAG, "onRoomUserEnter, roomId: $roomId, userInfo: $userInfo")
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            if (controller.userList.firstOrNull { it.userId == userInfo.userId } == null) {
                controller.userList.add(userInfo)
                renewRoomInfo(controller, controller.userList.size)
                runOnMainThread { controller.userJoinSubscriber?.invoke(userInfo.userId, userInfo.userName, userInfo.userAvatar) }
            }
        }
        override fun onRoomUserLeave(roomId: String, userInfo: AUIUserInfo, reason: AUIRtmUserLeaveReason) {
            DreamFlowLogger.d(TAG, "onRoomUserLeave, roomId: $roomId, userInfo: $userInfo")
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            controller.userList.removeAll { it.userId == userInfo.userId }
            renewRoomInfo(controller, controller.userList.size)
            runOnMainThread { controller.userLeaveSubscriber?.invoke(userInfo.userId, userInfo.userName, userInfo.userAvatar) }
        }
        override fun onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            val existUser = controller.userList.firstOrNull { it.userId == userInfo.userId }
            if (existUser != null) {
                controller.userList.remove(existUser)
                controller.userList.add(userInfo)
            }
        }
    }

    private fun renewRoomInfo(controller: RoomInfoController, count: Int) {
        if (controller.roomInfo.ownerId == UserManager.getInstance().user.id.toString()) {
            val roomInfo = AUIRoomInfo()
            roomInfo.roomId = controller.roomId
            roomInfo.roomName = controller.roomInfo.roomName
            val owner = AUIUserThumbnailInfo()
            owner.userId = controller.roomInfo.ownerId
            owner.userName = controller.roomInfo.ownerName
            owner.userAvatar = controller.roomInfo.ownerAvatar
            roomInfo.roomOwner = owner
            // TODO roomInfo.thumbnail = controller.roomInfo.thumbnailId
            roomInfo.createTime = controller.roomInfo.createdAt
            val roomDetailModel = RoomDetailModel(
                controller.roomInfo.roomId,
                controller.roomInfo.roomName,
                count,
                controller.roomInfo.thumbnailId,
                controller.roomInfo.ownerId,
                controller.roomInfo.ownerAvatar,
                controller.roomInfo.ownerName,
                controller.roomInfo.roomStatus,
                controller.roomInfo.createdAt,
                controller.roomInfo.updatedAt
            )
            roomInfo.customPayload = GsonTools.beanToMap(roomDetailModel)
            roomManager.updateRoomInfo(
                BuildConfig.AGORA_APP_ID, kSceneId, roomInfo
            ) { e, _ ->
                DreamFlowLogger.d(TAG, "updateRoomInfo")
            }
        }
        runOnMainThread { controller.userChangeSubscriber?.invoke(controller.userList.size) }
    }

    private var isRtmLogin = false
    private fun rtmLogin(complete: (Boolean) -> Unit) {
        if (isRtmLogin) {
            complete.invoke(true)
            return
        }
        DreamFlowLogger.d("ShowSyncManagerServiceImpl", "1.rtmLogin request start -> rtm login")
        syncManager.login(RtcEngineInstance.generalRtmToken()) { error ->
            if (error == null) {
                isRtmLogin = true
                complete.invoke(true)
            } else {
                complete.invoke(false)
            }
        }
    }

    override fun onSceneDestroy(channelName: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == channelName } ?: return
        controller.roomChangeSubscriber?.invoke(ShowRoomStatus.end)
        leaveRoom(channelName)
    }

    override fun onSceneExpire(channelName: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == channelName } ?: return
        controller.roomChangeSubscriber?.invoke(ShowRoomStatus.Expire)
        leaveRoom(channelName)
    }

    override fun onWillInitSceneMetadata(channelName: String): Map<String, Any>? {
        return null
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mainHandler.looper.thread) {
            r.run()
        } else {
            mainHandler.post(r)
        }
    }

    private fun isJsonString(str: String): Boolean {
        return try {
            JSONObject(str)
            true
        } catch (e: JSONException) {
            false
        }
    }

    private fun toDetailList(roomInfoList: List<AUIRoomInfo>): MutableList<RoomDetailModel> {
        val list = mutableListOf<RoomDetailModel>()
        val gson = Gson()
        roomInfoList.forEach { info ->
            val json = gson.toJson(info.customPayload)
            val model = gson.fromJson(json, RoomDetailModel::class.java)
            list.add(model)
        }
        return list
    }

    private fun removeReportRooms(roomList: List<RoomDetailModel>): MutableList<RoomDetailModel> {
        val retRoomList = mutableListOf<RoomDetailModel>()
        retRoomList.addAll(roomList)

        val reportRoomList = roomList.filter {
            (DreamFlowConstants.reportContents.contains(it.roomName) || DreamFlowConstants.reportUsers.contains(it.ownerId))
        }
        if (reportRoomList.isNotEmpty()) {
            reportRoomList.forEach {
                retRoomList.remove(it)
            }
        }
        return retRoomList
    }
}