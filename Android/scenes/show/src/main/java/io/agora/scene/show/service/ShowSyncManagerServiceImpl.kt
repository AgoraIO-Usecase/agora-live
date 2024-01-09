package io.agora.scene.show.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.show.R
import io.agora.scene.show.ShowLogger
import io.agora.scene.show.utils.ShowConstants
import io.agora.syncmanager.rtm.IObject
import io.agora.syncmanager.rtm.RethinkConfig
import io.agora.syncmanager.rtm.Scene
import io.agora.syncmanager.rtm.SceneReference
import io.agora.syncmanager.rtm.Sync
import io.agora.syncmanager.rtm.Sync.EventListener
import io.agora.syncmanager.rtm.Sync.JoinSceneCallback
import io.agora.syncmanager.rtm.SyncManagerException
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

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
) : ShowServiceProtocol {
    /**
     * Tag
     */
    private val TAG = "ShowSyncManagerServiceImpl"

    /**
     * K scene id
     */
    private val kSceneId = "scene_show_1.0.0"

    /**
     * K collection id user
     */
    private val kCollectionIdUser = "userCollection"

    /**
     * K collection id message
     */
    private val kCollectionIdMessage = "show_message_collection"

    /**
     * K collection id seat apply
     */
    private val kCollectionIdSeatApply = "show_seat_apply_collection"

    /**
     * K collection id p k invitation
     */
    private val kCollectionIdPKInvitation = "show_pk_invitation_collection"

    /**
     * K collection id interaction info
     */
    private val kCollectionIdInteractionInfo = "show_interaction_collection"

    /**
     * K robot avatars
     */
    private val kRobotAvatars = listOf("https://download.agora.io/demo/release/bot1.png")

    /**
     * K robot uid
     */
    private val kRobotUid = 2000000001

    /**
     * K robot video room ids
     */
    private val kRobotVideoRoomIds = arrayListOf(2023001, 2023002, 2023003)

    /**
     * K robot video stream urls
     */
    private val kRobotVideoStreamUrls = arrayListOf(
        "https://download.agora.io/demo/release/agora_show_video_1.mp4",
        "https://download.agora.io/demo/release/agora_show_video_2.mp4",
        "https://download.agora.io/demo/release/agora_show_video_3.mp4"
    )

    /**
     * Sync initialized
     */
    @Volatile
    private var syncInitialized = false

    /**
     * Main handler
     */
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * Worker executor
     */
    private val workerExecutor by lazy { Executors.newSingleThreadExecutor() }

    /**
     * Cloud player service
     */
    private val cloudPlayerService by lazy { CloudPlayerService() }

    /**
     * Room map
     */// global cache data
    private val roomMap = mutableMapOf<String, ShowRoomDetailModel>()

    /**
     * Room info controller
     *
     * @property roomId
     * @property sceneReference
     * @property pkSceneReference
     * @property eventListeners
     * @property pkEventListeners
     * @property roomChangeSubscriber
     * @property userChangeSubscriber
     * @property micSeatApplySubscriber
     * @property micSeatInvitationSubscriber
     * @property micPKInvitationSubscriber
     * @property micInteractionInfoSubscriber
     * @property onReconnectSubscriber
     * @property objIdOfUserId
     * @property userList
     * @property micSeatApplyList
     * @property micSeatInvitationList
     * @property pKInvitationList
     * @property interactionInfoList
     * @property pKCompetitorInvitationList
     * @property objIdOfSeatApply
     * @property objIdOfSeatInvitation
     * @property objIdOfPKInvitation
     * @property objIdOfInteractionInfo
     * @property objIdOfPKCompetitorInvitation
     * @constructor Create empty Room info controller
     */// current room cache data
    data class RoomInfoController constructor(
        val roomId: String,

        var sceneReference: SceneReference? = null,
        var pkSceneReference: SceneReference? = null,

        val eventListeners: MutableList<EventListener> = Collections.synchronizedList(mutableListOf()),
        val pkEventListeners: MutableList<EventListener> = Collections.synchronizedList(
            mutableListOf()
        ),

        var roomChangeSubscriber: ((ShowServiceProtocol.ShowSubscribeStatus, ShowRoomDetailModel?) -> Unit)? = null,
        var userChangeSubscriber: ((ShowServiceProtocol.ShowSubscribeStatus, ShowUser?) -> Unit)? = null,
        var micSeatApplySubscriber: ((ShowServiceProtocol.ShowSubscribeStatus, ShowMicSeatApply?) -> Unit)? = null,
        var micSeatInvitationSubscriber: ((ShowServiceProtocol.ShowSubscribeStatus, ShowMicSeatInvitation?) -> Unit)? = null,
        var micPKInvitationSubscriber: ((ShowServiceProtocol.ShowSubscribeStatus, ShowPKInvitation?) -> Unit)? = null,
        var micInteractionInfoSubscriber: ((ShowServiceProtocol.ShowSubscribeStatus, ShowInteractionInfo?) -> Unit)? = null,
        var onReconnectSubscriber: (() -> Unit)? = null,

        val objIdOfUserId: MutableMap<String, String> = mutableMapOf(), // key: userId, value: objectId
        val userList: ArrayList<ShowUser> = ArrayList(),
        val micSeatApplyList: ArrayList<ShowMicSeatApply> = ArrayList(),
        val micSeatInvitationList: ArrayList<ShowUser> = ArrayList(),
        val pKInvitationList: ArrayList<ShowPKInvitation> = ArrayList(),
        val interactionInfoList: ArrayList<ShowInteractionInfo> = ArrayList(),
        val pKCompetitorInvitationList: ArrayList<ShowPKInvitation> = ArrayList(),

        val objIdOfSeatApply: ArrayList<String> = ArrayList(), // objectId of seat Apply
        val objIdOfSeatInvitation: ArrayList<String> = ArrayList(), // objectId of seat Invitation
        val objIdOfPKInvitation: ArrayList<String> = ArrayList(), // objectId of pk Invitation
        val objIdOfInteractionInfo: ArrayList<String> = ArrayList(), // objectId of pk Invitation
        val objIdOfPKCompetitorInvitation: ArrayList<String> = ArrayList(), // objectId of pk competitor Invitation
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
        if (syncInitialized) {
            synchronized(roomInfoControllers){
                roomInfoControllers.forEach {
                    cleanRoomInfoController(it)
                }
            }
            roomInfoControllers.clear()
            roomMap.clear()
            Sync.Instance().destroy()
            syncInitialized = false
            kRobotVideoRoomIds.forEach { roomId ->
                cloudPlayerService.stopHeartBeat(roomId.toString())
            }
        }
    }

    /**
     * Get room list
     *
     * @param success
     * @param error
     * @receiver
     */
    override fun getRoomList(
        success: (List<ShowRoomDetailModel>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        initSync {
            Sync.Instance().getScenes(object : Sync.DataListCallback {
                override fun onSuccess(result: MutableList<IObject>?) {
                    workerExecutor.execute {

                        var roomList = result!!.map {
                            it.toObject(ShowRoomDetailModel::class.java)
                        }
                        roomList = removeExpiredRooms(roomList)

                        roomMap.clear()
                        roomList.forEach { roomMap[it.roomId] = it.copy() }

                        roomList = appendRobotRooms(roomList)

                        roomList = removeReportRooms(roomList)

                        val sortedBy = roomList.sortedByDescending { it.createdAt }
                        runOnMainThread { success.invoke(sortedBy) }
                    }
                }

                override fun onFail(exception: SyncManagerException?) {
                    if (exception?.message?.contains("empty") == true) {
                        workerExecutor.execute {
                            val roomList = appendRobotRooms(emptyList())

                            val sortedBy = roomList.sortedBy { it.createdAt }
                            runOnMainThread { success.invoke(sortedBy) }
                        }
                    } else {
                        runOnMainThread {
                            error?.invoke(exception!!) ?: errorHandler.invoke(exception!!)
                        }
                    }
                }
            })
        }
    }

    /**
     * Append robot rooms
     *
     * @param roomList
     * @return
     */
    private fun appendRobotRooms(roomList: List<ShowRoomDetailModel>): List<ShowRoomDetailModel> {
        val retRoomList = mutableListOf<ShowRoomDetailModel>()
        retRoomList.addAll(roomList)

        val robotRoomIds = ArrayList(kRobotVideoRoomIds)
        val kRobotRoomStartId = kRobotVideoRoomIds[0]
        retRoomList.forEach { roomDetail ->
            val differValue = roomDetail.roomId.toInt() - kRobotRoomStartId
            if (differValue >= 0) {
                robotRoomIds.firstOrNull { robotRoomId -> robotRoomId == roomDetail.roomId.toInt() }?.let { id ->
                    robotRoomIds.remove(id)
                }
            }

        }
        for (i in 0 until robotRoomIds.size) {
            val robotRoomId = robotRoomIds[i]
            val robotId = robotRoomId % 10
            val roomInfo = ShowRoomDetailModel(
                robotRoomId.toString(), // roomId
                "Smooth $robotId", // roomName
                1,
                "1",
                kRobotUid.toString(),
                kRobotAvatars[(robotId - 1) % kRobotAvatars.size],
                "Robot $robotId",
                ShowRoomStatus.activity.value,
                ShowInteractionStatus.idle.value,
                TimeUtils.currentTimeMillis().toDouble(),
                TimeUtils.currentTimeMillis().toDouble()
            )
            roomMap[roomInfo.roomId] = roomInfo
            retRoomList.add(roomInfo)
        }
        return retRoomList
    }

    /**
     * Remove expired rooms
     *
     * @param roomList
     * @return
     */
    private fun removeExpiredRooms(
        roomList: List<ShowRoomDetailModel>
    ): List<ShowRoomDetailModel> {
        val retRoomList = mutableListOf<ShowRoomDetailModel>()
        retRoomList.addAll(roomList)

        val expireRoomList = roomList.filter {
            (TimeUtils.currentTimeMillis() - it.createdAt.toLong() > ROOM_AVAILABLE_DURATION) && !it.isRobotRoom()
        }
        if (expireRoomList.isNotEmpty()) {
            val expireLetchCount = CountDownLatch(expireRoomList.size)
            runOnMainThread {
                expireRoomList.forEach {

                    Sync.Instance()
                        .joinScene(it.roomId, object : JoinSceneCallback {
                            override fun onSuccess(sceneReference: SceneReference?) {
                                runOnMainThread {
                                    sceneReference?.delete(object : Sync.Callback {
                                        override fun onSuccess() {
                                            retRoomList.remove(it)
                                            expireLetchCount.countDown()
                                        }

                                        override fun onFail(exception: SyncManagerException?) {
                                            errorHandler.invoke(exception!!)
                                            expireLetchCount.countDown()
                                        }
                                    }) ?: expireLetchCount.countDown()
                                }
                            }

                            override fun onFail(exception: SyncManagerException?) {
                                errorHandler.invoke(exception!!)
                                expireLetchCount.countDown()
                            }
                        })
                }
            }

            try {
                expireLetchCount.await()
            } catch (e: Exception) {
                ShowLogger.e(TAG, e)
            }
        }
        return retRoomList
    }

    private fun removeReportRooms(roomList: List<ShowRoomDetailModel>): List<ShowRoomDetailModel> {
        val retRoomList = mutableListOf<ShowRoomDetailModel>()
        retRoomList.addAll(roomList)

        val reportRoomList = roomList.filter {
            (ShowConstants.reportContents.contains(it.roomName) || ShowConstants.reportUsers.contains(it.ownerId))
        }
        if (reportRoomList.isNotEmpty()) {
            reportRoomList.forEach {
                retRoomList.remove(it)
            }
        }
        return retRoomList
    }

    /**
     * Create room
     *
     * @param roomId
     * @param roomName
     * @param thumbnailId
     * @param success
     * @param error
     * @receiver
     */
    override fun createRoom(
        roomId: String,
        roomName: String,
        thumbnailId: String,
        success: (ShowRoomDetailModel) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        initSync {
            createRoomInner(
                roomId,
                roomName,
                0,
                thumbnailId,
                UserManager.getInstance().user.id.toString(),
                UserManager.getInstance().user.headUrl,
                UserManager.getInstance().user.name,
                success,
                error
            )
        }
    }

    /**
     * Create room inner
     *
     * @param roomId
     * @param roomName
     * @param roomUserCount
     * @param thumbnailId
     * @param ownerUid
     * @param ownerAvatar
     * @param ownerName
     * @param success
     * @param error
     * @receiver
     */
    private fun createRoomInner(
        roomId: String,
        roomName: String,
        roomUserCount: Int,
        thumbnailId: String,
        ownerUid: String,
        ownerAvatar: String,
        ownerName: String,
        success: (ShowRoomDetailModel) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        val roomDetail = ShowRoomDetailModel(
            roomId,
            roomName,
            roomUserCount,
            thumbnailId,
            ownerUid,
            ownerAvatar,
            ownerName,
            ShowRoomStatus.activity.value,
            ShowInteractionStatus.idle.value,
            TimeUtils.currentTimeMillis().toDouble(),
            TimeUtils.currentTimeMillis().toDouble()
        )

        val scene = Scene().apply {
            id = roomDetail.roomId
            userId = roomDetail.ownerId
            property = roomDetail.toMap()
        }
        Sync.Instance().createScene(
            scene,
            object : Sync.Callback {
                override fun onSuccess() {
                    roomMap[roomDetail.roomId] = roomDetail.copy()
                    success.invoke(roomDetail)
                }

                override fun onFail(exception: SyncManagerException?) {
                    errorHandler.invoke(exception!!)
                    error?.invoke(exception)
                }
            })
    }

    /**
     * Join room
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     */
    override fun joinRoom(
        roomId: String,
        success: (ShowRoomDetailModel) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfo = roomMap[roomId]
        if (roomInfo == null) {
            error?.invoke(RoomException("The room has been destroyed!", roomId))
            return
        }
        if (roomInfoControllers.any { it.roomId == roomId }) {
            success.invoke(roomInfo)
            return
        }
        val roomInfoController = RoomInfoController(roomId)
        roomInfoControllers.add(roomInfoController)
        initSync {
            Sync.Instance().joinScene(
                roomInfo.ownerId == UserManager.getInstance().user.id.toString() || roomInfo.isRobotRoom(),
                true,
                roomId, object : JoinSceneCallback {
                    override fun onSuccess(sceneReference: SceneReference?) {
                        roomInfoController.sceneReference = sceneReference

                        innerMayAddLocalUser(roomId, {
                            innerSubscribeUserChange(roomId)
                            innerSubscribeSeatApplyChanged(roomId)
                            innerSubscribeInteractionChanged(roomId)
                            innerSubscribePKInvitationChanged(roomId)
                            innerSubscribeRoomChange(roomId)
                            if (roomInfo.isRobotRoom()) {
                                cloudPlayerService.startHeartBeat(
                                    roomId,
                                    UserManager.getInstance().user.id.toString()
                                )
                            }

                            success.invoke(roomInfo)
                        }, {
                            roomInfoControllers.remove(roomInfoController)
                            error?.invoke(RoomException(it.message, roomId)) ?: errorHandler.invoke(it)
                        })
                    }

                    override fun onFail(exception: SyncManagerException?) {
                        exception ?: return
                        if (exception.code == -2000 && roomInfo.isRobotRoom()) {
                            ShowLogger.d(TAG, "create robotRoom ${roomInfo.roomId}")
                            createRoomInner(
                                roomId,
                                roomInfo.roomName,
                                roomInfo.roomUserCount,
                                roomInfo.thumbnailId,
                                roomInfo.ownerId,
                                roomInfo.ownerAvatar,
                                roomInfo.ownerName,
                                success = {
                                    runOnMainThread {
                                        roomInfoControllers.remove(roomInfoController)
                                        joinRoom(roomId, { joinRet ->
                                            innerAddUser(roomId, ShowUser(
                                                roomInfo.ownerId,
                                                roomInfo.ownerAvatar,
                                                roomInfo.ownerName
                                            ),
                                                success = {
                                                    roomInfoController.objIdOfUserId[roomInfo.ownerId] = it
                                                    innerUpdateRoomUserCount(roomId, 2, {}, {})
                                                },
                                                error = {})
                                            success.invoke(joinRet)
                                        }, error)
                                    }
                                },
                                {
                                    roomInfoControllers.remove(roomInfoController)
                                    error?.invoke(RoomException(it.message, roomId))
                                }
                            )
                        } else {
                            roomInfoControllers.remove(roomInfoController)
                            error?.invoke(RoomException(exception.message, roomId))
                                ?: errorHandler.invoke(exception)
                        }
                    }
                }
            )
        }
    }

    /**
     * Leave room
     *
     * @param roomId
     */
    override fun leaveRoom(roomId: String) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val roomDetail = roomMap[roomId] ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        sendChatMessage(roomId, context.getString(R.string.show_live_chat_leaving))

        val targetApply =
            roomInfoController.micSeatApplyList.filter { it.userId == UserManager.getInstance().user.id.toString() }
                .getOrNull(0)
        if (targetApply != null) {
            val indexOf = roomInfoController.micSeatApplyList.indexOf(targetApply)
            roomInfoController.micSeatApplyList.removeAt(indexOf)
            val removedSeatApplyObjId = roomInfoController.objIdOfSeatApply.removeAt(indexOf)
            innerRemoveSeatApply(roomId, removedSeatApplyObjId, null, null)
        }

        roomInfoController.pKCompetitorInvitationList.forEach {
            val index = roomInfoController.pKCompetitorInvitationList.indexOf(it)
            innerRemovePKInvitation(
                roomInfoController.sceneReference,
                roomInfoController.objIdOfPKCompetitorInvitation[index],
                null,
                null
            )
        }
        roomInfoController.pKCompetitorInvitationList.clear()

        innerRemoveUser(
            roomId,
            UserManager.getInstance().user.id.toString(),
            success = {},
            error = { errorHandler.invoke(it) }
        )
        innerUpdateRoomUserCount(roomId, roomDetail.roomUserCount - 1, {}, {})

        ShowLogger.d(
            TAG,
            "leaveRoom roomId=${roomId} ownerId=${roomDetail.ownerId} myId=${UserManager.getInstance().user.id}"
        )
        if (roomDetail.isRobotRoom()) {
            // nothing
        } else if (roomDetail.ownerId == UserManager.getInstance().user.id.toString()
            || TimeUtils.currentTimeMillis() - roomDetail.createdAt.toLong() >= ROOM_AVAILABLE_DURATION
        ) {
            ShowLogger.d(TAG, "leaveRoom delete room")
            sceneReference.delete(object : Sync.Callback {
                override fun onSuccess() {
                    roomMap.remove(roomId)
                }

                override fun onFail(exception: SyncManagerException?) {
                    errorHandler.invoke(exception!!)
                }
            })
        }

        cleanRoomInfoController(roomInfoController)
        roomInfoControllers.remove(roomInfoController)
    }

    /**
     * Clean room info controller
     *
     * @param infoController
     */
    private fun cleanRoomInfoController(infoController: RoomInfoController) {
        infoController.roomChangeSubscriber = null
        infoController.userChangeSubscriber = null
        infoController.micSeatApplySubscriber = null
        infoController.micSeatInvitationSubscriber = null
        infoController.micPKInvitationSubscriber = null
        infoController.micInteractionInfoSubscriber = null
        infoController.onReconnectSubscriber = null

        infoController.sceneReference?.let { ref ->
            synchronized(infoController.eventListeners){
                infoController.eventListeners.forEach {
                    ref.unsubscribe(it)
                }
            }
        }
        infoController.eventListeners.clear()
        infoController.sceneReference = null

        infoController.pkSceneReference?.let { ref ->
            synchronized(infoController.pkEventListeners){
                infoController.pkEventListeners.forEach {
                    ref.unsubscribe(it)
                }
            }
        }
        infoController.pkEventListeners.clear()
        infoController.pkSceneReference = null
    }

    /**
     * Subscribe curr room event
     *
     * @param roomId
     * @param onUpdate
     * @receiver
     */
    override fun subscribeCurrRoomEvent(
        roomId: String,
        onUpdate: (status: ShowServiceProtocol.ShowSubscribeStatus, roomInfo: ShowRoomDetailModel?) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.roomChangeSubscriber = onUpdate
    }

    /**
     * Get all user list
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     */
    override fun getAllUserList(
        roomId: String,
        success: (List<ShowUser>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        innerGetUserList(
            roomId,
            {
                if (roomMap[roomId]?.ownerId == UserManager.getInstance().user.id.toString()) {
                    innerUpdateRoomUserCount(roomId, roomInfoController.userList.size, {}, {})
                }
                success.invoke(it)
            },
            {
                error?.invoke(it) ?: errorHandler.invoke(it)
            })
    }

    /**
     * Subscribe user
     *
     * @param roomId
     * @param onUserChange
     * @receiver
     */
    override fun subscribeUser(
        roomId: String,
        onUserChange: (ShowServiceProtocol.ShowSubscribeStatus, ShowUser?) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.userChangeSubscriber = onUserChange
    }

    /**
     * Send chat message
     *
     * @param roomId
     * @param message
     * @param success
     * @param error
     */
    override fun sendChatMessage(
        roomId: String,
        message: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return
        sceneReference.collection(kCollectionIdMessage)
            .add(ShowMessage(
                UserManager.getInstance().user.id.toString(),
                UserManager.getInstance().user.name,
                message,
                TimeUtils.currentTimeMillis().toDouble()
            ), object : Sync.DataItemCallback {
                override fun onSuccess(result: IObject?) {
                    success?.invoke()
                }

                override fun onFail(exception: SyncManagerException?) {
                    error?.invoke(exception!!) ?: errorHandler.invoke(exception!!)
                }
            })
    }

    /**
     * Subscribe message
     *
     * @param roomId
     * @param onMessageChange
     * @receiver
     */
    override fun subscribeMessage(
        roomId: String,
        onMessageChange: (ShowServiceProtocol.ShowSubscribeStatus, ShowMessage) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return
        val listener = object : EventListener {
            override fun onCreated(item: IObject?) {
                // do nothing
            }

            override fun onUpdated(item: IObject?) {
                item ?: return
                onMessageChange.invoke(
                    ShowServiceProtocol.ShowSubscribeStatus.updated,
                    item.toObject(ShowMessage::class.java)
                )
            }

            override fun onDeleted(item: IObject?) {

            }

            override fun onSubscribeError(ex: SyncManagerException?) {
                errorHandler.invoke(ex!!)
            }
        }
        roomInfoController.eventListeners.add(listener)
        sceneReference.collection(kCollectionIdMessage)
            .subscribe(listener)
    }

    /**
     * Get all mic seat apply list
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     */
    override fun getAllMicSeatApplyList(
        roomId: String,
        success: (List<ShowMicSeatApply>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        innerGetSeatApplyList(roomId, success, error)
    }

    /**
     * Subscribe mic seat apply
     *
     * @param roomId
     * @param onMicSeatChange
     * @receiver
     */
    override fun subscribeMicSeatApply(
        roomId: String,
        onMicSeatChange: (ShowServiceProtocol.ShowSubscribeStatus, ShowMicSeatApply?) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.micSeatApplySubscriber = onMicSeatChange
    }

    /**
     * Create mic seat apply
     *
     * @param roomId
     * @param success
     * @param error
     */
    override fun createMicSeatApply(
        roomId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val targetApply =
            roomInfoController.micSeatApplyList.filter { it.userId == UserManager.getInstance().user.id.toString() }
                .getOrNull(0)
        if (targetApply != null) {
            error?.invoke(RuntimeException("The seat apply found!"))
            return
        }
        val apply = ShowMicSeatApply(
            UserManager.getInstance().user.id.toString(),
            UserManager.getInstance().user.headUrl,
            UserManager.getInstance().user.name,
            ShowRoomRequestStatus.waitting.value,
            TimeUtils.currentTimeMillis().toDouble()
        )
        innerCreateSeatApply(roomId, apply, success, error)
    }

    /**
     * Cancel mic seat apply
     *
     * @param roomId
     * @param success
     * @param error
     */
    override fun cancelMicSeatApply(
        roomId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

        if (roomInfoController.micSeatApplyList.size <= 0) {
            error?.invoke(RuntimeException("The seat apply list is empty!"))
            return
        }
        val targetApply =
            roomInfoController.micSeatApplyList.filter { it.userId == UserManager.getInstance().user.id.toString() }
                .getOrNull(0)
        if (targetApply == null) {
            error?.invoke(RuntimeException("The seat apply not found!"))
            return
        }

        val indexOf = roomInfoController.micSeatApplyList.indexOf(targetApply)
        roomInfoController.micSeatApplyList.removeAt(indexOf)
        val removedSeatApplyObjId = roomInfoController.objIdOfSeatApply.removeAt(indexOf)

        innerRemoveSeatApply(roomId, removedSeatApplyObjId, success, error)
    }

    /**
     * Accept mic seat apply
     *
     * @param roomId
     * @param apply
     * @param success
     * @param error
     */
    override fun acceptMicSeatApply(
        roomId: String,
        apply: ShowMicSeatApply,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

        if (roomInfoController.micSeatApplyList.size <= 0) {
            error?.invoke(RuntimeException("The seat apply list is empty!"))
            return
        }
        val targetApply =
            roomInfoController.micSeatApplyList.filter { it.userId == apply.userId }.getOrNull(0)
        if (targetApply == null) {
            error?.invoke(RuntimeException("The seat apply found!"))
            return
        }

        val indexOf = roomInfoController.micSeatApplyList.indexOf(targetApply)
        roomInfoController.micSeatApplyList.removeAt(indexOf)
        val removedSeatApplyObjId = roomInfoController.objIdOfSeatApply.removeAt(indexOf)
        innerRemoveSeatApply(roomId, removedSeatApplyObjId, success, error)

        val interaction = ShowInteractionInfo(
            apply.userId,
            apply.userName,
            roomId,
            ShowInteractionStatus.onSeat.value,
            muteAudio = false,
            ownerMuteAudio = false,
            createdAt = apply.createAt
        )
        innerCreateInteraction(roomId, interaction, null, null)
    }

    /**
     * Reject mic seat apply
     *
     * @param roomId
     * @param apply
     * @param success
     * @param error
     */
    override fun rejectMicSeatApply(
        roomId: String,
        apply: ShowMicSeatApply,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

        if (roomInfoController.micSeatApplyList.size <= 0) {
            error?.invoke(RuntimeException("The seat apply list is empty!"))
            return
        }
        val targetApply =
            roomInfoController.micSeatApplyList.filter { it.userId == apply.userId }.getOrNull(0)
        if (targetApply == null) {
            error?.invoke(RuntimeException("The seat apply found!"))
            return
        }

        val seatApply = ShowMicSeatApply(
            targetApply.userId,
            targetApply.avatar,
            targetApply.userName,
            ShowRoomRequestStatus.rejected.value,
            targetApply.createAt
        )
        val indexOf = roomInfoController.micSeatApplyList.indexOf(targetApply)
        roomInfoController.micSeatApplyList[indexOf] = seatApply
        innerUpdateSeatApply(
            roomId,
            roomInfoController.objIdOfSeatApply[indexOf],
            seatApply,
            success,
            error
        )
    }

    /**
     * Get all mic seat invitation list
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     */
    override fun getAllMicSeatInvitationList(
        roomId: String,
        success: (List<ShowMicSeatInvitation>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {

    }

    /**
     * Subscribe mic seat invitation
     *
     * @param roomId
     * @param onMicSeatInvitationChange
     * @receiver
     */
    override fun subscribeMicSeatInvitation(
        roomId: String,
        onMicSeatInvitationChange: (ShowServiceProtocol.ShowSubscribeStatus, ShowMicSeatInvitation?) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.micSeatInvitationSubscriber = onMicSeatInvitationChange
    }

    /**
     * Create mic seat invitation
     *
     * @param roomId
     * @param user
     * @param success
     * @param error
     */
    override fun createMicSeatInvitation(
        roomId: String,
        user: ShowUser,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val userItem = ShowUser(
            user.userId,
            user.avatar,
            user.userName,
            ShowRoomRequestStatus.waitting.value,
        )
        innerUpdateUserRoomRequestStatus(roomId, userItem, {}, {})
    }

    /**
     * Cancel mic seat invitation
     *
     * @param roomId
     * @param userId
     * @param success
     * @param error
     */
    override fun cancelMicSeatInvitation(
        roomId: String,
        userId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {

    }

    /**
     * Accept mic seat invitation
     *
     * @param roomId
     * @param success
     * @param error
     */
    override fun acceptMicSeatInvitation(
        roomId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

        if (roomInfoController.userList.size <= 0) {
            error?.invoke(RuntimeException("The seat invitation list is empty!"))
            return
        }
        val user =
            roomInfoController.userList.filter { it.userId == UserManager.getInstance().user.id.toString() }
                .getOrNull(0)
        if (user == null) {
            error?.invoke(RuntimeException("The seat invitation found!"))
            return
        }
        val userItem = ShowUser(
            user.userId,
            user.avatar,
            user.userName,
            ShowRoomRequestStatus.accepted.value,
        )
        innerUpdateUserRoomRequestStatus(roomId, userItem, {}, {})

        val interaction = ShowInteractionInfo(
            user.userId,
            user.userName,
            roomId,
            ShowInteractionStatus.onSeat.value,
            muteAudio = false,
            ownerMuteAudio = false,
            createdAt = TimeUtils.currentTimeMillis().toDouble() //TODO
        )
        innerCreateInteraction(roomId, interaction, { }, { })
    }

    /**
     * Reject mic seat invitation
     *
     * @param roomId
     * @param success
     * @param error
     */
    override fun rejectMicSeatInvitation(
        roomId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

        if (roomInfoController.userList.size <= 0) {
            error?.invoke(RuntimeException("The seat invitation list is empty!"))
            return
        }
        val user =
            roomInfoController.userList.filter { it.userId == UserManager.getInstance().user.id.toString() }
                .getOrNull(0)
        if (user == null) {
            error?.invoke(RuntimeException("The seat invitation found!"))
            return
        }
        val userItem = ShowUser(
            user.userId,
            user.avatar,
            user.userName,
            ShowRoomRequestStatus.idle.value,
        )
        innerUpdateUserRoomRequestStatus(roomId, userItem, {}, {})
    }

    /**
     * Get all p k user list
     *
     * @param success
     * @param error
     * @receiver
     */
    override fun getAllPKUserList(
        success: ((List<ShowRoomDetailModel>) -> Unit),
        error: ((Exception) -> Unit)?
    ) {
        Sync.Instance().getScenes(object : Sync.DataListCallback {
            override fun onSuccess(result: MutableList<IObject>?) {
                val roomList = result!!.map {
                    it.toObject(ShowRoomDetailModel::class.java)
                }
                val list =
                    roomList.filter { it.ownerId != UserManager.getInstance().user.id.toString() }
                runOnMainThread { success.invoke(list.sortedBy { it.createdAt }) }
            }

            override fun onFail(exception: SyncManagerException?) {
                error?.invoke(exception!!) ?: errorHandler.invoke(exception!!)
            }
        })
    }

    /**
     * Get all p k invitation list
     *
     * @param roomId
     * @param isFromUser
     * @param success
     * @param error
     * @receiver
     */
    override fun getAllPKInvitationList(
        roomId: String,
        isFromUser: Boolean,
        success: (List<ShowPKInvitation>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

        if (isFromUser) {
            success.invoke(roomInfoController.pKCompetitorInvitationList)
        } else {
            innerGetPKInvitationList(roomId, null, success, error)
        }
    }

    /**
     * Subscribe p k invitation changed
     *
     * @param roomId
     * @param onPKInvitationChanged
     * @receiver
     */
    override fun subscribePKInvitationChanged(
        roomId: String,
        onPKInvitationChanged: (ShowServiceProtocol.ShowSubscribeStatus, ShowPKInvitation?) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.micPKInvitationSubscriber = onPKInvitationChanged
    }

    /**
     * Create p k invitation
     *
     * @param roomId
     * @param room
     * @param success
     * @param error
     */
    override fun createPKInvitation(
        roomId: String,
        room: ShowRoomDetailModel,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

        if (roomInfoController.interactionInfoList.size > 0) {
            error?.invoke(RuntimeException("InteractionInfoList is not empty, stop interacting first!"))
        }
        innerGetPKInvitationList(roomId, room, {
            val invitation = it.filter { filterRoom -> filterRoom.roomId == room.roomId }.getOrNull(0)
            if (invitation == null) {
                val pkInvitation = ShowPKInvitation(
                    room.ownerId,
                    room.ownerName,
                    room.roomId,
                    UserManager.getInstance().user.id.toString(),
                    UserManager.getInstance().user.name,
                    roomId,
                    ShowRoomRequestStatus.waitting.value,
                    userMuteAudio = false,
                    fromUserMuteAudio = false,
                    createAt = TimeUtils.currentTimeMillis().toDouble()
                )
                innerCreatePKInvitation(
                    roomInfoController.pkSceneReference,
                    pkInvitation,
                    success = {},
                    error = {}
                )
            }
        }, null)
    }

    /**
     * Accept p k invitation
     *
     * @param roomId
     * @param success
     * @param error
     */
    override fun acceptPKInvitation(
        roomId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

        if (roomInfoController.pKInvitationList.size <= 0) {
            error?.invoke(RuntimeException("The seat invitation list is empty!"))
            return
        }
        val targetInvitation =
            roomInfoController.pKInvitationList.filter { it.userId == UserManager.getInstance().user.id.toString() }
                .getOrNull(0)
        if (targetInvitation == null) {
            error?.invoke(RuntimeException("The seat invitation found!"))
            return
        }

        val invitation = ShowPKInvitation(
            targetInvitation.userId,
            targetInvitation.userName,
            roomId,
            targetInvitation.fromUserId,
            targetInvitation.fromName,
            targetInvitation.fromRoomId,
            ShowRoomRequestStatus.accepted.value,
            userMuteAudio = false,
            fromUserMuteAudio = false,
            createAt = TimeUtils.currentTimeMillis().toDouble()
        )

        val indexOf = roomInfoController.pKInvitationList.indexOf(targetInvitation)
        roomInfoController.pKInvitationList[indexOf] = invitation
        innerUpdatePKInvitation(
            roomInfoController.sceneReference,
            roomInfoController.objIdOfPKInvitation[indexOf],
            invitation,
            success,
            error
        )

        val interaction = ShowInteractionInfo(
            invitation.fromUserId,
            invitation.fromName,
            invitation.fromRoomId,
            ShowInteractionStatus.pking.value,
            muteAudio = false,
            ownerMuteAudio = false,
            createdAt = invitation.createAt
        )
        innerCreateInteraction(roomId, interaction, null, null)
    }

    /**
     * Reject p k invitation
     *
     * @param roomId
     * @param success
     * @param error
     */
    override fun rejectPKInvitation(
        roomId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        if (roomInfoController.pKInvitationList.size <= 0) {
            error?.invoke(RuntimeException("The seat invitation list is empty!"))
            return
        }
        val targetInvitation =
            roomInfoController.pKInvitationList.filter { it.userId == UserManager.getInstance().user.id.toString() }
                .getOrNull(0)
        if (targetInvitation == null) {
            error?.invoke(RuntimeException("The seat invitation found!"))
            return
        }

        val indexOf = roomInfoController.pKInvitationList.indexOf(targetInvitation)
        roomInfoController.pKInvitationList.removeAt(indexOf)
        val removedObjId = roomInfoController.objIdOfPKInvitation.removeAt(indexOf)
        innerRemovePKInvitation(roomInfoController.sceneReference, removedObjId, success, error)
    }

    /**
     * Get all interation list
     *
     * @param roomId
     * @param success
     * @param error
     */
    override fun getAllInterationList(
        roomId: String,
        success: ((List<ShowInteractionInfo>) -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        innerGetAllInteractionList(roomId, success, error)
    }

    /**
     * Subscribe interaction changed
     *
     * @param roomId
     * @param onInteractionChanged
     * @receiver
     */
    override fun subscribeInteractionChanged(
        roomId: String,
        onInteractionChanged: (ShowServiceProtocol.ShowSubscribeStatus, ShowInteractionInfo?) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.micInteractionInfoSubscriber = onInteractionChanged
    }

    /**
     * Stop interaction
     *
     * @param roomId
     * @param interaction
     * @param success
     * @param error
     */
    override fun stopInteraction(
        roomId: String,
        interaction: ShowInteractionInfo,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

        if (roomInfoController.interactionInfoList.size <= 0) {
            error?.invoke(RuntimeException("The interaction list is empty!"))
            return
        }
        val targetInvitation =
            roomInfoController.interactionInfoList.filter { it.userId == interaction.userId }
                .getOrNull(0)
        if (targetInvitation == null) {
            error?.invoke(RuntimeException("The interaction not found!"))
            return
        }

        innerGetAllInteractionList(roomId, {
            roomInfoController.objIdOfInteractionInfo.forEach {
                innerRemoveInteraction(
                    roomId,
                    it,
                    success,
                    error
                )
            }
        }, null)

        val apply = roomInfoController.micSeatApplyList.filter { it.userId == interaction.userId }
            .getOrNull(0)
        if (apply != null) {
            val index = roomInfoController.micSeatApplyList.indexOf(apply)
            innerRemoveSeatApply(roomId, roomInfoController.objIdOfSeatApply[index], {}, {})
        }

        // pk
        if (interaction.interactStatus == ShowInteractionStatus.pking.value) {
            if (roomInfoController.pKCompetitorInvitationList.isEmpty()) {
                val invitation =
                    roomInfoController.pKInvitationList.filter { it.fromUserId == interaction.userId }
                        .getOrNull(0)
                if (invitation != null) {
                    val index = roomInfoController.pKInvitationList.indexOf(invitation)
                    roomInfoController.pKInvitationList.removeAt(index)
                    val objId = roomInfoController.objIdOfPKInvitation.removeAt(index)
                    innerRemovePKInvitation(roomInfoController.sceneReference, objId, null, null)
                }
            } else {
                val invitation =
                    roomInfoController.pKCompetitorInvitationList.filter { it.userId == interaction.userId }
                        .getOrNull(0)
                if (invitation != null) {
                    val index = roomInfoController.pKCompetitorInvitationList.indexOf(invitation)
                    roomInfoController.pKCompetitorInvitationList.removeAt(index)
                    val objId = roomInfoController.objIdOfPKCompetitorInvitation.removeAt(index)
                    innerRemovePKInvitation(roomInfoController.pkSceneReference, objId, null, null)
                }
            }

        }

        val user =
            roomInfoController.userList.filter { it.userId == interaction.userId }.getOrNull(0)
        if (user != null && user.status != ShowRoomRequestStatus.idle.value) {
            val userItem = ShowUser(
                user.userId,
                user.avatar,
                user.userName,
                ShowRoomRequestStatus.idle.value,
            )
            innerUpdateUserRoomRequestStatus(roomId, userItem, {}, {})
        }
    }

    /**
     * Mute audio
     *
     * @param roomId
     * @param mute
     * @param userId
     * @param success
     * @param error
     */
    override fun muteAudio(
        roomId: String,
        mute: Boolean,
        userId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val oldInteraction =
            roomInfoController.interactionInfoList.filter { it.userId == userId }.getOrNull(0)
        if (oldInteraction != null) {
            val indexOf = roomInfoController.interactionInfoList.indexOf(oldInteraction)
            val objId = roomInfoController.objIdOfInteractionInfo[indexOf]

            val interaction = ShowInteractionInfo(
                oldInteraction.userId,
                oldInteraction.userName,
                oldInteraction.roomId,
                oldInteraction.interactStatus,
                mute,
                oldInteraction.ownerMuteAudio,
                oldInteraction.createdAt
            )
            innerUpdateInteraction(
                roomInfoController.sceneReference,
                objId,
                interaction,
                success = {},
                error = {}
            )
        }

        // pk
        if (roomInfoController.pKCompetitorInvitationList.isEmpty()) {
            val invitation =
                roomInfoController.pKInvitationList.filter { it.userId == userId }.getOrNull(0)
            if (invitation != null) {
                val index = roomInfoController.pKInvitationList.indexOf(invitation)
                val newInvitation = ShowPKInvitation(
                    invitation.userId,
                    invitation.userName,
                    invitation.roomId,
                    invitation.fromUserId,
                    invitation.fromName,
                    invitation.fromRoomId,
                    invitation.status,
                    mute,
                    invitation.fromUserMuteAudio,
                    invitation.createAt
                )
                val objId = roomInfoController.objIdOfPKInvitation[index]
                innerUpdatePKInvitation(
                    roomInfoController.sceneReference,
                    objId,
                    newInvitation,
                    null,
                    null
                )
            }
        } else {
            val invitation =
                roomInfoController.pKCompetitorInvitationList.filter { it.fromUserId == userId }
                    .getOrNull(0)
            if (invitation != null) {
                val index = roomInfoController.pKCompetitorInvitationList.indexOf(invitation)
                val objId = roomInfoController.objIdOfPKCompetitorInvitation[index]
                val newInvitation = ShowPKInvitation(
                    invitation.userId,
                    invitation.userName,
                    invitation.roomId,
                    invitation.fromUserId,
                    invitation.fromName,
                    invitation.fromRoomId,
                    invitation.status,
                    invitation.userMuteAudio,
                    mute,
                    invitation.createAt
                )
                innerUpdatePKInvitation(
                    roomInfoController.pkSceneReference,
                    objId,
                    newInvitation,
                    null,
                    null
                )
            }
        }
    }

    /**
     * Subscribe re connect event
     *
     * @param roomId
     * @param onReconnect
     * @receiver
     */
    override fun subscribeReConnectEvent(roomId: String, onReconnect: () -> Unit) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.onReconnectSubscriber = onReconnect
    }

    /**
     * Run on main thread
     *
     * @param r
     */
    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mainHandler.looper.thread) {
            r.run()
        } else {
            mainHandler.post(r)
        }
    }

    /**
     * Init sync
     *
     * @param complete
     * @receiver
     */
    private fun initSync(complete: () -> Unit) {
        if (syncInitialized) {
            complete.invoke()
            return
        }
        syncInitialized = true
        Sync.Instance().init(
            RethinkConfig(BuildConfig.AGORA_APP_ID, kSceneId),
            object : Sync.Callback {
                override fun onSuccess() {
                    runOnMainThread { complete.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    syncInitialized = false
                    errorHandler.invoke(exception!!)
                }
            }
        )
        Sync.Instance().subscribeConnectState {
            ShowLogger.d(TAG, "subscribeConnectState state=$it")
            if (it == Sync.ConnectionState.open) {
                runOnMainThread {
                    roomInfoControllers.forEach { roomInfoController ->
                        val roomId = roomInfoController.roomId
                        val oldRoomInfo = roomMap[roomId]
                        if (oldRoomInfo != null) {
                            getRoomList({
                                val roomInfo = roomMap[roomId]
                                if (roomInfo == null) {
                                    runOnMainThread {
                                        roomInfoController.roomChangeSubscriber?.invoke(
                                            ShowServiceProtocol.ShowSubscribeStatus.deleted,
                                            oldRoomInfo
                                        )
                                    }
                                }
                            })
                        }
                        roomInfoController.onReconnectSubscriber?.invoke()
                    }
                }
            }
        }
    }

    /**
     * Inner update room interact status
     *
     * @param roomId
     * @param interactStatus
     * @param success
     * @param error
     * @receiver
     * @receiver
     */
    private fun innerUpdateRoomInteractStatus(
        roomId: String,
        interactStatus: Int,
        success: () -> Unit,
        error: (Exception) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val roomInfo = roomMap[roomId] ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        val nRoomInfo = ShowRoomDetailModel(
            roomInfo.roomId,
            roomInfo.roomName,
            roomInfo.roomUserCount,
            roomInfo.thumbnailId,
            roomInfo.ownerId,
            roomInfo.ownerAvatar,
            roomInfo.ownerName,
            roomInfo.roomStatus,
            interactStatus,
            roomInfo.createdAt,
            roomInfo.updatedAt,
        )
        sceneReference.update(nRoomInfo.toMap(), object : Sync.DataItemCallback {
            override fun onSuccess(result: IObject?) {
                roomMap[roomId] = nRoomInfo
                success.invoke()
            }

            override fun onFail(exception: SyncManagerException?) {
                error.invoke(exception!!)
            }
        })
    }

    /**
     * Inner update room user count
     *
     * @param roomId
     * @param userCount
     * @param success
     * @param error
     * @receiver
     * @receiver
     */
    private fun innerUpdateRoomUserCount(
        roomId: String,
        userCount: Int,
        success: () -> Unit,
        error: (Exception) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val roomInfo = roomMap[roomId] ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        val nRoomInfo = ShowRoomDetailModel(
            roomInfo.roomId,
            roomInfo.roomName,
            Math.max(1, userCount),
            roomInfo.thumbnailId,
            roomInfo.ownerId,
            roomInfo.ownerAvatar,
            roomInfo.ownerName,
            roomInfo.roomStatus,
            roomInfo.interactStatus,
            roomInfo.createdAt,
            roomInfo.updatedAt,
        )
        sceneReference.update(nRoomInfo.toMap(), object : Sync.DataItemCallback {
            override fun onSuccess(result: IObject?) {
                roomMap[roomId] = nRoomInfo
                success.invoke()
            }

            override fun onFail(exception: SyncManagerException?) {
                error.invoke(exception!!)
            }
        })
    }

    /**
     * Inner may add local user
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     * @receiver
     */
    private fun innerMayAddLocalUser(
        roomId: String,
        success: () -> Unit,
        error: (Exception) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val userId = UserManager.getInstance().user.id.toString()
        val avatarUrl = UserManager.getInstance().user.headUrl
        innerGetUserList(roomId, { list ->
            if (list.none { it.userId == it.toString() }) {
                innerAddUser(roomId,
                    ShowUser(userId, avatarUrl, UserManager.getInstance().user.name),
                    {
                        roomInfoController.objIdOfUserId[userId] = it
                        innerUpdateRoomUserCount(roomId, list.size + 1, {
                            success.invoke()
                        }, { ex ->
                            error.invoke(ex)
                        })
                    },
                    {
                        error.invoke(it)
                    })
            } else {
                success.invoke()
            }
        }, {
            error.invoke(it)
        })
    }

    /**
     * Inner get user list
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     * @receiver
     */
    private fun innerGetUserList(
        roomId: String,
        success: (List<ShowUser>) -> Unit,
        error: (Exception) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        sceneReference.collection(kCollectionIdUser)
            .get(object : Sync.DataListCallback {
                override fun onSuccess(result: MutableList<IObject>?) {
                    result ?: return
                    val map = result.map { it.toObject(ShowUser::class.java) }
                    roomInfoController.userList.clear()

                    val ret = ArrayList<ShowUser>()
                    result.forEach {
                        val obj = it.toObject(ShowUser::class.java)
                        roomInfoController.objIdOfUserId[obj.userId] = it.id
                        ret.add(obj)
                    }
                    roomInfoController.userList.addAll(ret)
                    runOnMainThread {
                        success.invoke(map)
                    }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner add user
     *
     * @param roomId
     * @param user
     * @param success
     * @param error
     * @receiver
     * @receiver
     */
    private fun innerAddUser(
        roomId: String,
        user: ShowUser,
        success: (String) -> Unit,
        error: (Exception) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        sceneReference.collection(kCollectionIdUser)
            .add(user, object : Sync.DataItemCallback {
                override fun onSuccess(result: IObject?) {
                    success.invoke(result?.id!!)
                }

                override fun onFail(exception: SyncManagerException?) {
                    error.invoke(exception!!)
                }
            })
    }

    /**
     * Inner remove user
     *
     * @param roomId
     * @param userId
     * @param success
     * @param error
     * @receiver
     * @receiver
     */
    private fun innerRemoveUser(
        roomId: String,
        userId: String,
        success: () -> Unit,
        error: (Exception) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return
        val objectId = roomInfoController.objIdOfUserId[userId] ?: return
        sceneReference.collection(kCollectionIdUser)
            .delete(objectId, object : Sync.Callback {
                override fun onSuccess() {
                    success.invoke()
                }

                override fun onFail(exception: SyncManagerException?) {
                    error.invoke(exception!!)
                }
            })
    }

    /**
     * Inner update user room request status
     *
     * @param roomId
     * @param user
     * @param success
     * @param error
     * @receiver
     * @receiver
     */
    private fun innerUpdateUserRoomRequestStatus(
        roomId: String,
        user: ShowUser,
        success: () -> Unit,
        error: (Exception) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return
        val objectId = roomInfoController.objIdOfUserId[user.userId] ?: return

        sceneReference.collection(kCollectionIdUser)
            ?.update(objectId, user, object : Sync.Callback {
                override fun onSuccess() {
                    runOnMainThread { success.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner subscribe user change
     *
     * @param roomId
     */
    private fun innerSubscribeUserChange(roomId: String) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return
        val listener = object : EventListener {
            override fun onCreated(item: IObject?) {
                // do nothing
            }

            override fun onUpdated(item: IObject?) {
                val user = item?.toObject(ShowUser::class.java) ?: return
                roomInfoController.objIdOfUserId[user.userId] = item.id


                val list =
                    roomInfoController.micSeatInvitationList.filter { it.userId == user.userId }
                if (list.isEmpty()) {
                    roomInfoController.micSeatInvitationList.add(user)
                    roomInfoController.objIdOfSeatInvitation.add(item.id)
                } else {
                    val indexOf = roomInfoController.micSeatInvitationList.indexOf(list[0])
                    roomInfoController.micSeatInvitationList[indexOf] = user
                    roomInfoController.objIdOfSeatInvitation[indexOf] = item.id
                }
                runOnMainThread {
                    roomInfoController.userChangeSubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.updated,
                        user
                    )
                }

            }

            override fun onDeleted(item: IObject?) {
                val userId =
                    roomInfoController.objIdOfUserId.filterValues { it == item?.id }.entries.firstOrNull()?.key
                        ?: return
                val userInfo =
                    roomInfoController.userList.filter { it.userId == userId }.getOrNull(0)
                        ?: return
                roomInfoController.userList.remove(userInfo)
                runOnMainThread {
                    roomInfoController.userChangeSubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.deleted,
                        userInfo
                    )
                }
            }

            override fun onSubscribeError(ex: SyncManagerException?) {
                errorHandler.invoke(ex!!)
            }
        }
        roomInfoController.eventListeners.add(listener)
        sceneReference.collection(kCollectionIdUser)
            .subscribe(listener)
    }

    /**
     * Inner subscribe room change
     *
     * @param roomId
     */
    private fun innerSubscribeRoomChange(roomId: String) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return
        val listener = object : EventListener {
            override fun onCreated(item: IObject?) {
                // do nothing
            }

            override fun onUpdated(item: IObject?) {
                item ?: return
                val roomInfo = item.toObject(ShowRoomDetailModel::class.java)
                roomMap[item.id] = roomInfo
                ShowLogger.d(
                    TAG,
                    "SubscribeRoomChange Update roomNo=${item.id}, roomInfo=${roomInfo}"
                )
                if (roomInfoController.roomId.isNotEmpty()) {
                    runOnMainThread {
                        roomInfoController.roomChangeSubscriber?.invoke(
                            ShowServiceProtocol.ShowSubscribeStatus.updated,
                            roomInfo
                        )
                    }
                }

            }

            override fun onDeleted(item: IObject?) {
                item ?: return
                val roomInfo = roomMap.remove(item.id)
                ShowLogger.d(TAG, "SubscribeRoomChange Delete roomNo=${item.id}")
                if (roomInfoController.roomId.isNotEmpty() && roomInfoController.roomId == item.id) {
                    runOnMainThread {
                        roomInfoController.roomChangeSubscriber?.invoke(
                            ShowServiceProtocol.ShowSubscribeStatus.deleted,
                            roomInfo
                        )
                    }
                }
            }

            override fun onSubscribeError(ex: SyncManagerException?) {
                ShowLogger.e(TAG, ex)
                errorHandler.invoke(ex!!)
            }
        }
        roomInfoController.eventListeners.add(listener)
        Sync.Instance().subscribeScene(sceneReference, listener)
    }

    /**
     * Inner get seat apply list
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     */
    private fun innerGetSeatApplyList(
        roomId: String,
        success: (List<ShowMicSeatApply>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        sceneReference.collection(kCollectionIdSeatApply)?.get(object :
            Sync.DataListCallback {
            override fun onSuccess(result: MutableList<IObject>?) {
                val ret = ArrayList<ShowMicSeatApply>()
                val retObjId = ArrayList<String>()
                result?.forEach {
                    val obj = it.toObject(ShowMicSeatApply::class.java)
                    ret.add(obj)
                    retObjId.add(it.id)
                }
                roomInfoController.micSeatApplyList.clear()
                roomInfoController.micSeatApplyList.addAll(ret)
                roomInfoController.objIdOfSeatApply.clear()
                roomInfoController.objIdOfSeatApply.addAll(retObjId)

                //ret.sortBy { it.createdAt }
                runOnMainThread { success.invoke(ret) }
            }

            override fun onFail(exception: SyncManagerException?) {
                runOnMainThread { error?.invoke(exception!!) }
            }
        })
    }

    /**
     * Inner create seat apply
     *
     * @param roomId
     * @param seatApply
     * @param success
     * @param error
     */
    private fun innerCreateSeatApply(
        roomId: String,
        seatApply: ShowMicSeatApply,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        sceneReference.collection(kCollectionIdSeatApply)
            ?.add(seatApply, object : Sync.DataItemCallback {
                override fun onSuccess(result: IObject) {
                    //micSeatApplyList.add(seatApply)
                    runOnMainThread { success?.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner update seat apply
     *
     * @param roomId
     * @param objectId
     * @param seatApply
     * @param success
     * @param error
     */
    private fun innerUpdateSeatApply(
        roomId: String,
        objectId: String,
        seatApply: ShowMicSeatApply,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        sceneReference.collection(kCollectionIdSeatApply)
            ?.update(objectId, seatApply, object : Sync.Callback {
                override fun onSuccess() {
                    runOnMainThread { success?.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner remove seat apply
     *
     * @param roomId
     * @param objectId
     * @param success
     * @param error
     */
    private fun innerRemoveSeatApply(
        roomId: String,
        objectId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        sceneReference.collection(kCollectionIdSeatApply)
            ?.delete(objectId, object : Sync.Callback {
                override fun onSuccess() {
                    runOnMainThread { success?.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner subscribe seat apply changed
     *
     * @param roomId
     */
    private fun innerSubscribeSeatApplyChanged(roomId: String) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        val listener = object : EventListener {
            override fun onCreated(item: IObject?) {
                // do Nothing
            }

            override fun onUpdated(item: IObject?) {
                val info = item?.toObject(ShowMicSeatApply::class.java) ?: return
                val list = roomInfoController.micSeatApplyList.filter { it.userId == info.userId }
                if (list.isEmpty()) {
                    roomInfoController.micSeatApplyList.add(info)
                    roomInfoController.objIdOfSeatApply.add(item.id)
                } else {
                    val indexOf = roomInfoController.micSeatApplyList.indexOf(list[0])
                    roomInfoController.micSeatApplyList[indexOf] = info
                    roomInfoController.objIdOfSeatApply[indexOf] = item.id
                }
                runOnMainThread {
                    roomInfoController.micSeatApplySubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.updated,
                        info
                    )
                }
            }

            override fun onDeleted(item: IObject?) {
                val info = item?.toObject(ShowMicSeatApply::class.java)
                if (info != null) {
                    val list =
                        roomInfoController.micSeatApplyList.filter { it.userId == info.userId }
                    if (list.isNotEmpty()) {
                        val indexOf = roomInfoController.micSeatApplyList.indexOf(list[0])
                        roomInfoController.micSeatApplyList.removeAt(indexOf)
                        roomInfoController.objIdOfSeatApply.removeAt(indexOf)
                    }
                }
                runOnMainThread {
                    roomInfoController.micSeatApplySubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.deleted,
                        null
                    )
                }
            }

            override fun onSubscribeError(ex: SyncManagerException?) {
            }
        }
        roomInfoController.eventListeners.add(listener)
        sceneReference.collection(kCollectionIdSeatApply)
            .subscribe(listener)
    }

    /**
     * Inner get p k invitation list
     *
     * @param localRoomId
     * @param room
     * @param success
     * @param error
     * @receiver
     */
    private fun innerGetPKInvitationList(
        localRoomId: String,
        room: ShowRoomDetailModel?,
        success: (List<ShowPKInvitation>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController =
            roomInfoControllers.firstOrNull { it.roomId == localRoomId } ?: return
        if (room != null) {
            if (room.roomId == "") return
            val roomId = room.roomId
            if (roomId != localRoomId) {
                initSync {
                    Sync.Instance().joinScene(
                        roomId, object : JoinSceneCallback {
                            override fun onSuccess(sceneReference: SceneReference?) {
                                roomInfoController.pkSceneReference = sceneReference!!
                                roomInfoController.pkSceneReference?.collection(
                                    kCollectionIdPKInvitation
                                )
                                    ?.get(object : Sync.DataListCallback {
                                        override fun onSuccess(result: MutableList<IObject>?) {
                                            val ret = ArrayList<ShowPKInvitation>()
                                            val retObjId = ArrayList<String>()
                                            result?.forEach {
                                                val obj = it.toObject(ShowPKInvitation::class.java)
                                                ret.add(obj)
                                                retObjId.add(it.id)
                                            }
                                            innerSubscribeCompetitorPKInvitationChanged(localRoomId)
                                            runOnMainThread { success.invoke(ret) }
                                        }

                                        override fun onFail(exception: SyncManagerException?) {
                                            runOnMainThread { error?.invoke(exception!!) }
                                        }
                                    })
                            }

                            override fun onFail(exception: SyncManagerException?) {
                                error?.invoke(exception!!) ?: errorHandler.invoke(exception!!)
                            }
                        }
                    )
                }
                return
            }
        }

        roomInfoController.sceneReference?.collection(kCollectionIdPKInvitation)
            ?.get(object : Sync.DataListCallback {
                override fun onSuccess(result: MutableList<IObject>?) {
                    val ret = ArrayList<ShowPKInvitation>()
                    val retObjId = ArrayList<String>()
                    result?.forEach {
                        val obj = it.toObject(ShowPKInvitation::class.java)
                        ret.add(obj)
                        retObjId.add(it.id)
                    }
                    roomInfoController.pKInvitationList.clear()
                    roomInfoController.pKInvitationList.addAll(ret)
                    roomInfoController.objIdOfPKInvitation.clear()
                    roomInfoController.objIdOfPKInvitation.addAll(retObjId)

                    runOnMainThread { success.invoke(ret) }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner create p k invitation
     *
     * @param sceneReference
     * @param pkInvitation
     * @param success
     * @param error
     */
    private fun innerCreatePKInvitation(
        sceneReference: SceneReference?,
        pkInvitation: ShowPKInvitation,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        sceneReference?.collection(kCollectionIdPKInvitation)
            ?.add(pkInvitation, object : Sync.DataItemCallback {
                override fun onSuccess(result: IObject) {
                    runOnMainThread { success?.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner update p k invitation
     *
     * @param sceneReference
     * @param objectId
     * @param pkInvitation
     * @param success
     * @param error
     */
    private fun innerUpdatePKInvitation(
        sceneReference: SceneReference?,
        objectId: String,
        pkInvitation: ShowPKInvitation,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        sceneReference?.collection(kCollectionIdPKInvitation)
            ?.update(objectId, pkInvitation, object : Sync.Callback {
                override fun onSuccess() {
                    runOnMainThread { success?.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner remove p k invitation
     *
     * @param sceneReference
     * @param objectId
     * @param success
     * @param error
     */
    private fun innerRemovePKInvitation(
        sceneReference: SceneReference?,
        objectId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        sceneReference?.collection(kCollectionIdPKInvitation)
            ?.delete(objectId, object : Sync.Callback {
                override fun onSuccess() {
                    runOnMainThread { success?.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner subscribe p k invitation changed
     *
     * @param roomId
     */
    private fun innerSubscribePKInvitationChanged(roomId: String) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return
        val listener = object : EventListener {
            override fun onCreated(item: IObject?) {
                // do Nothing
            }

            override fun onUpdated(item: IObject?) {
                val info = item?.toObject(ShowPKInvitation::class.java) ?: return
                val list = roomInfoController.pKInvitationList.filter { it.userId == info.userId }
                if (list.isEmpty()) {
                    roomInfoController.pKInvitationList.add(info)
                    roomInfoController.objIdOfPKInvitation.add(item.id)
                } else {
                    val indexOf = roomInfoController.pKInvitationList.indexOf(list[0])
                    roomInfoController.pKInvitationList[indexOf] = info
                    roomInfoController.objIdOfPKInvitation[indexOf] = item.id
                }

                if (roomInfoController.interactionInfoList.isNotEmpty()) {
                    val oldInteraction =
                        roomInfoController.interactionInfoList.filter { it.userId == info.fromUserId }
                            .getOrNull(0)
                    if (oldInteraction != null) {
                        val indexOf = roomInfoController.interactionInfoList.indexOf(oldInteraction)
                        val objId = roomInfoController.objIdOfInteractionInfo[indexOf]

                        val interaction = ShowInteractionInfo(
                            oldInteraction.userId,
                            oldInteraction.userName,
                            oldInteraction.roomId,
                            oldInteraction.interactStatus,
                            info.fromUserMuteAudio,
                            info.userMuteAudio,
                            oldInteraction.createdAt
                        )
                        innerUpdateInteraction(
                            roomInfoController.sceneReference,
                            objId,
                            interaction,
                            success = {},
                            error = {}
                        )
                    }
                }

                runOnMainThread {
                    roomInfoController.micPKInvitationSubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.updated,
                        info
                    )
                }
            }

            override fun onDeleted(item: IObject?) {
                val objId = item!!.id
                val index = roomInfoController.objIdOfPKInvitation.indexOf(objId)
                roomInfoController.objIdOfPKInvitation.removeAt(index)
                val invitation = roomInfoController.pKInvitationList.removeAt(index)

                runOnMainThread {
                    roomInfoController.micPKInvitationSubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.deleted,
                        invitation
                    )
                }
            }

            override fun onSubscribeError(ex: SyncManagerException?) {
            }
        }
        roomInfoController.eventListeners.add(listener)
        sceneReference.collection(kCollectionIdPKInvitation).subscribe(listener)
    }

    /**
     * Is interaction created
     */
    private var isInteractionCreated = false // TODO workaround

    /**
     * Inner subscribe competitor p k invitation changed
     *
     * @param roomId
     */
    private fun innerSubscribeCompetitorPKInvitationChanged(roomId: String) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val pkSceneReference = roomInfoController.pkSceneReference ?: return
        val listener = object : EventListener {
            override fun onCreated(item: IObject?) {
                // do Nothing
            }

            override fun onUpdated(item: IObject?) {
                val info = item?.toObject(ShowPKInvitation::class.java) ?: return

                val acceptItem =
                    roomInfoController.pKCompetitorInvitationList.filter { it.status == ShowRoomRequestStatus.accepted.value }
                        .getOrNull(0)
                if (acceptItem != null && acceptItem.userId != info.userId && info.status == ShowRoomRequestStatus.accepted.value) {
                    innerRemovePKInvitation(roomInfoController.sceneReference, item.id, null, null)
                    return
                }

                val list =
                    roomInfoController.pKCompetitorInvitationList.filter { it.userId == info.userId }
                if (list.isEmpty()) {
                    roomInfoController.pKCompetitorInvitationList.add(info)
                    roomInfoController.objIdOfPKCompetitorInvitation.add(item.id)
                } else {
                    val indexOf = roomInfoController.pKCompetitorInvitationList.indexOf(list[0])
                    roomInfoController.pKCompetitorInvitationList[indexOf] = info
                    roomInfoController.objIdOfPKCompetitorInvitation[indexOf] = item.id
                }

                if (roomInfoController.interactionInfoList.isEmpty() && info.status == ShowRoomRequestStatus.accepted.value && !isInteractionCreated) {
                    isInteractionCreated = true
                    val interaction = ShowInteractionInfo(
                        info.userId,
                        info.userName,
                        info.roomId,
                        ShowInteractionStatus.pking.value,
                        muteAudio = false,
                        ownerMuteAudio = false,
                        createdAt = info.createAt
                    )
                    innerCreateInteraction(info.fromRoomId, interaction, null, null)
                } else {
                    val oldInteraction =
                        roomInfoController.interactionInfoList.filter { it.userId == info.userId }
                            .getOrNull(0)
                    if (oldInteraction != null) {
                        val indexOf = roomInfoController.interactionInfoList.indexOf(oldInteraction)
                        val objId = roomInfoController.objIdOfInteractionInfo[indexOf]

                        val interaction = ShowInteractionInfo(
                            oldInteraction.userId,
                            oldInteraction.userName,
                            oldInteraction.roomId,
                            oldInteraction.interactStatus,
                            info.userMuteAudio,
                            info.fromUserMuteAudio,
                            oldInteraction.createdAt
                        )
                        innerUpdateInteraction(
                            roomInfoController.sceneReference,
                            objId,
                            interaction,
                            success = {},
                            error = {}
                        )
                    }
                }

                runOnMainThread {
                    roomInfoController.micPKInvitationSubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.updated,
                        info
                    )
                }
            }

            override fun onDeleted(item: IObject?) {
                val objId = item!!.id
                val index = roomInfoController.objIdOfPKCompetitorInvitation.indexOf(objId)
                val invitation = roomInfoController.pKCompetitorInvitationList[index]

                val sceneReference = roomInfoController.pkSceneReference ?: return
                synchronized(roomInfoController.pkEventListeners){
                    roomInfoController.pkEventListeners.forEach {
                        sceneReference.unsubscribe(it)
                    }
                }

                roomInfoController.pkSceneReference = null
                roomInfoController.objIdOfPKCompetitorInvitation.removeAt(index)
                roomInfoController.pKCompetitorInvitationList.removeAt(index)
                runOnMainThread {
                    roomInfoController.micPKInvitationSubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.deleted,
                        invitation
                    )
                }
            }

            override fun onSubscribeError(ex: SyncManagerException?) {
            }
        }
        roomInfoController.pkEventListeners.add(listener)
        pkSceneReference.collection(kCollectionIdPKInvitation).subscribe(listener)
    }

    /**
     * Inner get all interaction list
     *
     * @param roomId
     * @param success
     * @param error
     */
    private fun innerGetAllInteractionList(
        roomId: String,
        success: ((List<ShowInteractionInfo>) -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.sceneReference?.collection(kCollectionIdInteractionInfo)
            ?.get(object : Sync.DataListCallback {
                override fun onSuccess(result: MutableList<IObject>?) {
                    val ret = ArrayList<ShowInteractionInfo>()
                    val retObjId = ArrayList<String>()
                    result?.forEach {
                        val obj = it.toObject(ShowInteractionInfo::class.java)
                        ret.add(obj)
                        retObjId.add(it.id)
                    }
                    roomInfoController.interactionInfoList.clear()
                    roomInfoController.interactionInfoList.addAll(ret)
                    roomInfoController.objIdOfInteractionInfo.clear()
                    roomInfoController.objIdOfInteractionInfo.addAll(retObjId)

                    runOnMainThread { success?.invoke(ret) }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner create interaction
     *
     * @param roomId
     * @param info
     * @param success
     * @param error
     */
    private fun innerCreateInteraction(
        roomId: String,
        info: ShowInteractionInfo,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        ShowLogger.d(TAG, "innerCreateInteraction called")
        roomInfoController.sceneReference?.collection(kCollectionIdInteractionInfo)
            ?.add(info, object : Sync.DataItemCallback {
                override fun onSuccess(result: IObject) {
                    ShowLogger.d(TAG, "innerCreateInteraction success")
                    runOnMainThread { success?.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    ShowLogger.d(TAG, "innerCreateInteraction failed")
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner update interaction
     *
     * @param sceneReference
     * @param objectId
     * @param info
     * @param success
     * @param error
     */
    private fun innerUpdateInteraction(
        sceneReference: SceneReference?,
        objectId: String,
        info: ShowInteractionInfo,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        sceneReference?.collection(kCollectionIdInteractionInfo)
            ?.update(objectId, info, object : Sync.Callback {
                override fun onSuccess() {
                    runOnMainThread { success?.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner remove interaction
     *
     * @param roomId
     * @param objectId
     * @param success
     * @param error
     */
    private fun innerRemoveInteraction(
        roomId: String,
        objectId: String,
        success: (() -> Unit)?,
        error: ((Exception) -> Unit)?
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.sceneReference?.collection(kCollectionIdInteractionInfo)
            ?.delete(objectId, object : Sync.Callback {
                override fun onSuccess() {
                    innerUpdateRoomInteractStatus(roomId, ShowInteractionStatus.idle.value, {}, {})
                    runOnMainThread { success?.invoke() }
                }

                override fun onFail(exception: SyncManagerException?) {
                    runOnMainThread { error?.invoke(exception!!) }
                }
            })
    }

    /**
     * Inner subscribe interaction changed
     *
     * @param roomId
     */
    private fun innerSubscribeInteractionChanged(roomId: String) {
        ShowLogger.d(TAG, "innerSubscribeInteractionChanged called")
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val sceneReference = roomInfoController.sceneReference ?: return

        val listener = object : EventListener {
            override fun onCreated(item: IObject?) {
                ShowLogger.d(TAG, "innerSubscribeInteractionChanged onCreated")
                // do Nothing
            }

            override fun onUpdated(item: IObject?) {
                ShowLogger.d(TAG, "innerSubscribeInteractionChanged onUpdated")
                val info = item?.toObject(ShowInteractionInfo::class.java) ?: return

                val interactionInfo = roomInfoController.interactionInfoList.getOrNull(0)
                if (interactionInfo != null && interactionInfo.userId != info.userId) {
                    stopInteraction(roomId, info)
                    val userItem = roomInfoController.userList.filter { it.userId == info.userId }.getOrNull(0)
                    if (userItem != null) {
                        val newUserItem = ShowUser(
                            userItem.userId,
                            userItem.avatar,
                            userItem.userName,
                            ShowRoomRequestStatus.idle.value,
                        )
                        innerUpdateUserRoomRequestStatus(roomId, newUserItem, {}, {})
                    }
                    return
                }

                val list =
                    roomInfoController.interactionInfoList.filter { it.userId == info.userId }
                if (list.isEmpty()) {
                    roomInfoController.interactionInfoList.add(info)
                    roomInfoController.objIdOfInteractionInfo.add(item.id)
                } else {
                    val indexOf = roomInfoController.interactionInfoList.indexOf(list[0])
                    roomInfoController.interactionInfoList[indexOf] = info
                    roomInfoController.objIdOfInteractionInfo[indexOf] = item.id
                }
                innerUpdateRoomInteractStatus(roomId, info.interactStatus, {}, {})

                runOnMainThread {
                    roomInfoController.micInteractionInfoSubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.updated,
                        info
                    )
                }

            }

            override fun onDeleted(item: IObject?) {
                ShowLogger.d(TAG, "innerSubscribeInteractionChanged onDeleted")
                isInteractionCreated = false
                val objId = item!!.id
                val index = roomInfoController.objIdOfInteractionInfo.indexOf(objId)
                if (index < 0) return
                roomInfoController.objIdOfInteractionInfo.removeAt(index)
                roomInfoController.interactionInfoList.removeAt(index)
                innerUpdateRoomInteractStatus(roomId, ShowInteractionStatus.idle.value, {}, {})

                runOnMainThread {
                    roomInfoController.micInteractionInfoSubscriber?.invoke(
                        ShowServiceProtocol.ShowSubscribeStatus.deleted,
                        null
                    )
                }
            }

            override fun onSubscribeError(ex: SyncManagerException?) {
                ShowLogger.d(TAG, "innerSubscribeInteractionChanged onSubscribeError: $ex")
            }
        }
        roomInfoController.eventListeners.add(listener)
        sceneReference.collection(kCollectionIdInteractionInfo).subscribe(listener)
    }

    /**
     * Start cloud player
     *
     */
    override fun startCloudPlayer() {
        for (i in 0 until kRobotVideoRoomIds.size) {
            val roomId = kRobotVideoRoomIds[i]
            cloudPlayerService.startCloudPlayer(
                roomId.toString(),
                UserManager.getInstance().user.id.toString(),
                kRobotUid,
                kRobotVideoStreamUrls[i],
                "cn",
                success = {
                    cloudPlayerService.startHeartBeat(
                        roomId.toString(),
                        UserManager.getInstance().user.id.toString()
                    )
                },
                failure = { })
        }
    }
}