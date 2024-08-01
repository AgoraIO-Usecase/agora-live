package io.agora.scene.voice.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import io.agora.CallBack
import io.agora.ValueCallBack
import io.agora.chat.ChatRoom
import io.agora.rtmsyncmanager.ISceneResponse
import io.agora.rtmsyncmanager.RoomExpirationPolicy
import io.agora.rtmsyncmanager.RoomService
import io.agora.rtmsyncmanager.SyncManager
import io.agora.rtmsyncmanager.model.AUICommonConfig
import io.agora.rtmsyncmanager.model.AUIRoomContext
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserInfo
import io.agora.rtmsyncmanager.model.AUIUserThumbnailInfo
import io.agora.rtmsyncmanager.service.IAUIUserService
import io.agora.rtmsyncmanager.service.http.HttpManager
import io.agora.rtmsyncmanager.service.room.AUIRoomManager
import io.agora.rtmsyncmanager.service.rtm.AUIRtmException
import io.agora.rtmsyncmanager.service.rtm.AUIRtmUserLeaveReason
import io.agora.rtmsyncmanager.utils.AUILogger
import io.agora.rtmsyncmanager.utils.ObservableHelper
import io.agora.scene.base.ServerConfig
import io.agora.scene.base.manager.UserManager
import io.agora.scene.voice.R
import io.agora.scene.voice.global.VoiceBuddyFactory
import io.agora.scene.voice.imkit.manager.ChatroomIMManager
import io.agora.scene.voice.model.*
import io.agora.scene.voice.netkit.VRCreateRoomResponse
import io.agora.scene.voice.netkit.VoiceToolboxServerHttpManager
import io.agora.scene.voice.rtckit.AgoraRtcEngineController
import io.agora.syncmanager.rtm.*
import io.agora.voice.common.constant.ConfigConstants
import io.agora.voice.common.net.callback.VRValueCallBack
import io.agora.voice.common.utils.LogTools
import io.agora.voice.common.utils.ThreadManager
import kotlin.random.Random

/**
 * @author create by zhangwei03
 */
class VoiceSyncManagerServiceImp(
    private val mContext: Context,
    private val errorHandler: ((Exception?) -> Unit)?
) : VoiceServiceProtocol, ISceneResponse, IAUIUserService.AUIUserRespObserver {

    private val TAG = "VOICE_SYNC_LOG"

    private val voiceSceneId = "scene_chatRoom_0.2.0"

    // current user
    private val mCurrentUser: AUIUserThumbnailInfo get() = AUIRoomContext.shared().currentUserInfo

    private val mObservableHelper = ObservableHelper<VoiceServiceListenerProtocol>()

    private val ROOM_AVAILABLE_DURATION: Long = 20 * 60 * 1000 // 20min


    private val mMainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * Run on main thread
     *
     * @param r
     */
    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mMainHandler.looper.thread) {
            r.run()
        } else {
            mMainHandler.post(r)
        }
    }

    /**
     * sync manager
     */
    private val mSyncManager: SyncManager

    /**
     * room manager
     */
    private val mRoomManager = AUIRoomManager()

    /**
     * room service
     */
    private val mRoomService: RoomService

    /**
     * current room no
     */
    @Volatile
    private var mCurRoomNo: String = ""

    /**
     * room user list
     */
    private val mUserList = mutableListOf<AUIUserInfo>()

    init {
        HttpManager.setBaseURL(ServerConfig.roomManagerUrl)
        val rtmSyncTag = "VOICE_RTM_LOG"
        AUILogger.initLogger(
            AUILogger.Config(mContext, "VOICE", logCallback = object : AUILogger.AUILogCallback {
                override fun onLogDebug(tag: String, message: String) {
                    LogTools.d(rtmSyncTag, "$tag $message")
                }

                override fun onLogInfo(tag: String, message: String) {
                    LogTools.d(rtmSyncTag, "$tag $message")
                }

                override fun onLogWarning(tag: String, message: String) {
                    LogTools.w(rtmSyncTag, "$tag $message")
                }

                override fun onLogError(tag: String, message: String) {
                    LogTools.e(rtmSyncTag, "$tag $message")
                }

            })
        )

        val commonConfig = AUICommonConfig().apply {
            context = mContext
            appId = VoiceBuddyFactory.get().getVoiceBuddy().rtcAppId()
            owner = AUIUserThumbnailInfo().apply {
                userId = UserManager.getInstance().user.id.toString()
                userName = UserManager.getInstance().user.name
                userAvatar = UserManager.getInstance().user.headUrl
            }
            host = ServerConfig.toolBoxUrl
        }
        mSyncManager = SyncManager(mContext, null, commonConfig)

        val roomExpirationPolicy = RoomExpirationPolicy()
        roomExpirationPolicy.expirationTime = ROOM_AVAILABLE_DURATION
        roomExpirationPolicy.isAssociatedWithOwnerOffline = true
        mRoomService = RoomService(roomExpirationPolicy, mRoomManager, mSyncManager)
    }

    private fun startTimer() {
        mMainHandler.postDelayed(timerRoomCountDownTask, 1000)
    }

    private val timerRoomCountDownTask = object : Runnable {
        override fun run() {
            if (mCurRoomNo.isEmpty()) return
            val roomDuration = getCurrentDuration(mCurRoomNo)
            if (roomDuration >= ROOM_AVAILABLE_DURATION) {
                mMainHandler.removeCallbacks(this)
                onSceneExpire(mCurRoomNo)
            } else {
                mMainHandler.postDelayed(this, 1000)
            }
        }
    }

    /**
     * Subscribe event
     *
     * @param delegate
     */
    override fun subscribeListener(listener: VoiceServiceListenerProtocol) {
        mObservableHelper.subscribeEvent(listener)
        if (mUserList.isNotEmpty()) {
            listener.onSyncUserCountUpdate(mUserList.size + ConfigConstants.ROBOT_COUNT)
        }
    }

    /**
     * Unsubscribe event
     */
    override fun unsubscribeEvent() {
        mObservableHelper.unSubscribeAll()
    }

    override fun getSubscribeListeners(): ObservableHelper<VoiceServiceListenerProtocol> {
        return mObservableHelper
    }

    override fun onWillInitSceneMetadata(channelName: String): Map<String, Any>? {
        return super.onWillInitSceneMetadata(channelName)
    }

    override fun onTokenPrivilegeWillExpire(channelName: String?) {
        LogTools.d(TAG, "onTokenPrivilegeWillExpire, channelName:$channelName")
        // rtc rtm
        VoiceToolboxServerHttpManager.generateAllToken { rtmToken, exception ->
            val token = rtmToken ?: run {
                LogTools.e(TAG, "onTokenPrivilegeWillExpire generateAllToken, $exception")
                return@generateAllToken
            }
            mSyncManager.login(token, completion = {
                if (it == null) {
                    LogTools.d(TAG, "onTokenPrivilegeWillExpire, with renewToken loginRtm success")
                } else {
                    LogTools.e(TAG, "onTokenPrivilegeWillExpire, with renewToken loginRtm failed: $it")
                }
            })
            AgoraRtcEngineController.get().renewRtcToken(token)
        }
    }

    override fun onSceneExpire(channelName: String) {
        LogTools.d(TAG, "onSceneExpire, channelName:$channelName")
        if (mCurRoomNo == channelName) {
            leaveRoom { }
            mObservableHelper.notifyEventHandlers { delegate ->
                delegate.onSyncRoomExpire()
            }
        }
    }

    override fun onSceneDestroy(channelName: String) {
        LogTools.d(TAG, "onSceneExpire, channelName:$channelName")
        if (mCurRoomNo == channelName) {
            leaveRoom { }
            mObservableHelper.notifyEventHandlers { delegate ->
                delegate.onSyncRoomDestroy()
            }
        }
    }

    override fun onSceneUserBeKicked(channelName: String, userId: String) {
        LogTools.d(TAG, "onSceneUserBeKicked, channelName:$channelName, userId:$userId")
    }

    override fun onRoomUserSnapshot(roomId: String, userList: MutableList<AUIUserInfo>?) {
        LogTools.d(TAG, "onRoomUserSnapshot, roomId:$roomId, userList:${userList?.count()}")
        userList?.let {
            this.mUserList.clear()
            this.mUserList.addAll(it)
        }
    }

    override fun onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
        LogTools.d(TAG, "onRoomUserEnter, roomId:$roomId, userInfo:$userInfo")
        if (mCurRoomNo != roomId) {
            return
        }
        mUserList.removeIf { it.userId == userInfo.userId }
        mUserList.add(userInfo)
        mObservableHelper.notifyEventHandlers { delegate ->
            delegate.onSyncUserCountUpdate(mUserList.size + ConfigConstants.ROBOT_COUNT)
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId) ?: return
        // all members can update
        cacheRoom.customPayload[VoiceParameters.ROOM_USER_COUNT] = mUserList.size + ConfigConstants.ROBOT_COUNT
        mRoomManager.updateRoomInfo(VoiceBuddyFactory.get().getVoiceBuddy().rtcAppId(),
            voiceSceneId, cacheRoom, callback = { auiException, roomInfo ->
                if (auiException == null) {
                    LogTools.d(TAG, "onRoomUserEnter updateRoom success: $roomInfo")
                } else {
                    LogTools.e(TAG, "onRoomUserEnter updateRoom failed: $mCurRoomNo $auiException")
                }
            })
    }

    override fun onRoomUserLeave(roomId: String, userInfo: AUIUserInfo, reason: AUIRtmUserLeaveReason) {
        LogTools.d(TAG, "onRoomUserLeave, roomId:$roomId, userInfo:$userInfo")
        if (mCurRoomNo != roomId) {
            return
        }
        mUserList.removeIf { it.userId == userInfo.userId }

        mObservableHelper.notifyEventHandlers { delegate ->
            delegate.onSyncUserCountUpdate(mUserList.size + ConfigConstants.ROBOT_COUNT)
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId) ?: return
        // all members can update
        cacheRoom.customPayload[VoiceParameters.ROOM_USER_COUNT] = mUserList.count() + ConfigConstants.ROBOT_COUNT
        mRoomManager.updateRoomInfo(VoiceBuddyFactory.get().getVoiceBuddy().rtcAppId(),
            voiceSceneId, cacheRoom, callback = { auiException, roomInfo ->
                if (auiException == null) {
                    LogTools.d(TAG, "onRoomUserLeave updateRoom success: $roomId, $roomInfo")
                } else {
                    LogTools.d(TAG, "onRoomUserLeave updateRoom failed: $roomId $auiException")
                }
            })
    }

    /**
     * Get current duration
     *
     * @param channelName
     * @return
     */
    override fun getCurrentDuration(channelName: String): Long {
        if (channelName.isEmpty()) return 0
        val scene = mSyncManager.getScene(channelName)
        return scene?.getRoomDuration() ?: 0L
    }

    /**
     * Get current ts
     *
     * @param channelName
     * @return
     */
    override fun getCurrentTs(channelName: String): Long {
        if (channelName.isEmpty()) return 0
        val scene = mSyncManager.getScene(channelName)
        return scene?.getCurrentTs() ?: 0L
    }

    private var restfulDiffTs: Long = 0

    /**
     * Fetch room list
     *
     * @param completion
     * @receiver
     */
    override fun getRoomList(completion: (error: Exception?, roomInfoList: List<AUIRoomInfo>?) -> Unit) {
        initRtmSync {
            if (it != null) {
                completion.invoke(Exception("${it.message}"), null)
                return@initRtmSync
            }
            mRoomService.getRoomList(VoiceBuddyFactory.get().getVoiceBuddy().rtcAppId(), voiceSceneId, 0, 50,
                cleanClosure = { auiRoomInfo ->
                    return@getRoomList auiRoomInfo.roomOwner?.userId == VoiceBuddyFactory.get().getVoiceBuddy()
                        .userId()
                },
                completion = { uiException, ts, roomList ->
                    if (uiException == null) {
                        ts?.let { serverTs ->
                            restfulDiffTs = System.currentTimeMillis() - serverTs
                        }
                        val newRoomList = roomList?.sortedBy { -it.createTime } ?: emptyList()
                        LogTools.d(TAG, "getRoomList success,serverTs:$ts roomCount:${newRoomList.size}")
                        runOnMainThread {
                            completion.invoke(null, newRoomList)
                        }
                    } else {
                        LogTools.e(TAG, "getRoomList error, $uiException")
                        runOnMainThread {
                            completion.invoke(uiException, null)
                        }
                    }
                }
            )
        }
    }

    /**
     * Create room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    override fun createRoom(
        inputModel: VoiceCreateRoomModel, completion: (error: Exception?, out: AUIRoomInfo?) -> Unit
    ) {
        val roomId = (Random(System.currentTimeMillis()).nextInt(100000) + 1000000).toString()

        initRtmSync {
            if (it != null) {
                completion.invoke(Exception("${it.message}"), null)
                return@initRtmSync
            }
            // create chat room
            innerCreateChatRoom(inputModel.roomName, completion = { chatId, error ->
                if (chatId == null) {
                    completion.invoke(error, null)
                    return@innerCreateChatRoom
                }

                val createAt = System.currentTimeMillis() - restfulDiffTs
                val roomInfo = AUIRoomInfo().apply {
                    this.roomId = roomId
                    this.roomName = inputModel.roomName
                    this.roomOwner = AUIUserThumbnailInfo().apply {
                        userId = mCurrentUser.userId
                        userName = mCurrentUser.userName
                        userAvatar = mCurrentUser.userAvatar
                    }
                    this.createTime = createAt
                    this.customPayload[VoiceParameters.ROOM_USER_COUNT] = 2L // 两个机器人
                    this.customPayload[VoiceParameters.ROOM_SOUND_EFFECT] = inputModel.soundEffect
                    this.customPayload[VoiceParameters.PASSWORD] = inputModel.password
                    this.customPayload[VoiceParameters.IS_PRIVATE] = inputModel.password.isNotEmpty()
                    this.customPayload[VoiceParameters.CHATROOM_ID] = chatId
                }

                val scene = mSyncManager.createScene(roomInfo.roomId)
                scene.bindRespDelegate(this)
                scene.userService.registerRespObserver(this)
                mCurRoomNo = roomInfo.roomId
                mRoomService.createRoom(VoiceBuddyFactory.get().getVoiceBuddy().rtcAppId(), voiceSceneId, roomInfo,
                    completion = { rtmException, _ ->
                        if (rtmException == null) {
                            LogTools.d(TAG, "createRoom success: $roomInfo")
                            mCurRoomNo = roomInfo.roomId
                            startTimer()
                            runOnMainThread {
                                completion.invoke(null, roomInfo)
                            }
                        } else {
                            mCurRoomNo = ""
                            LogTools.e(TAG, "createRoom failed: $rtmException")
                            runOnMainThread {
                                completion.invoke(Exception("${rtmException.message}(${rtmException.code})"), null)
                            }
                        }
                    })
            })
        }
    }

    private fun innerCreateChatRoom(roomName: String, completion: (chatId: String?, error: Exception?) -> Unit) {
        VoiceToolboxServerHttpManager.createImRoom(
            roomName = roomName,
            roomOwner = mCurrentUser.userId,
            chatroomId = "",
            type = 2,
            callBack = object : VRValueCallBack<VRCreateRoomResponse> {
                override fun onSuccess(response: VRCreateRoomResponse?) {
                    completion.invoke(response?.chatId, null)
                }

                override fun onError(code: Int, message: String?) {
                    completion.invoke(null, Exception("$message $code"))
                }

            })
    }

    /**
     * Join room
     *
     * @param roomId
     * @param completion
     * @receiver
     */
    override fun joinRoom(
        roomId: String, password: String?, completion: (error: Exception?, roomInfo: AUIRoomInfo?) -> Unit
    ) {
        if (mCurRoomNo.isNotEmpty()) {
            completion.invoke(Exception("already join room $mCurRoomNo!"), null)
            return
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId)
        if (cacheRoom == null) {
            completion.invoke(Exception("room $mCurRoomNo null!"), null)
            return
        }
        val roomPassword = cacheRoom.roomPassword()
        if (roomPassword.isNotEmpty() && roomPassword != password) {
            completion.invoke(Exception(mContext.getString(R.string.voice_room_check_password)), null)
            return
        }
        initRtmSync {
            if (it != null) {
                completion.invoke(Exception("${it.message}"), null)
                return@initRtmSync
            }
            val scene = mSyncManager.createScene(roomId)
            scene.bindRespDelegate(this)
            scene.userService.registerRespObserver(this)
            mCurRoomNo = roomId
            mRoomService.enterRoom(
                VoiceBuddyFactory.get().getVoiceBuddy().rtcAppId(),
                voiceSceneId,
                roomId,
                completion = { rtmException ->
                    if (rtmException == null) {
                        LogTools.d(TAG, "enterRoom success: ${cacheRoom.roomId}")
                        mCurRoomNo = cacheRoom.roomId
                        runOnMainThread {
                            completion.invoke(null, cacheRoom)
                        }
                    } else {
                        mCurRoomNo = ""
                        LogTools.e(TAG, "enterRoom failed: $rtmException")
                        runOnMainThread {
                            completion.invoke(Exception("${rtmException.message}(${rtmException.code})"), null)
                        }
                    }
                })
        }
    }

    /**
     * Leave room
     *
     * @param completion
     * @receiver
     */
    override fun leaveRoom(completion: (error: Exception?) -> Unit) {
        mSyncManager.getScene(mCurRoomNo)?.let { scene ->
            scene.unbindRespDelegate(this)
            scene.userService.unRegisterRespObserver(this)
        }
        if (AUIRoomContext.shared().isRoomOwner(mCurRoomNo)) {
            mMainHandler.removeCallbacks(timerRoomCountDownTask)
        }
        mRoomService.leaveRoom(VoiceBuddyFactory.get().getVoiceBuddy().rtcAppId(), voiceSceneId, mCurRoomNo)

        mUserList.clear()
        mCurRoomNo = ""
        completion.invoke(null)
    }

    /**
     * Fetch room detail
     *
     * @param voiceRoomModel
     * @param completion
     * @receiver
     */
    override fun fetchRoomDetail(
        voiceRoomModel: VoiceRoomModel,
        completion: (error: Int, result: VoiceRoomInfo?) -> Unit
    ) {
        ChatroomIMManager.getInstance().fetchRoomDetail(voiceRoomModel, object : ValueCallBack<VoiceRoomInfo> {
            override fun onSuccess(value: VoiceRoomInfo?) {
                if (value != null) {
                    completion.invoke(VoiceServiceProtocol.ERR_OK, value)
                } else {
                    completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
                }
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Fetch gift contribute
     *
     * @param completion
     * @receiver
     */
    override fun fetchGiftContribute(completion: (error: Int, result: List<VoiceRankUserModel>?) -> Unit) {
        ChatroomIMManager.getInstance().fetchGiftContribute(object :
            ValueCallBack<MutableList<VoiceRankUserModel>> {
            override fun onSuccess(value: MutableList<VoiceRankUserModel>) {
                ThreadManager.getInstance().runOnMainThread {
                    completion.invoke(VoiceServiceProtocol.ERR_OK, value)
                }
            }

            override fun onError(error: Int, errorMsg: String?) {
                ThreadManager.getInstance().runOnMainThread {
                    completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
                }
            }
        })
    }

    /**
     * Fetch room invited members
     *
     * @param completion
     * @receiver
     */
    override fun fetchRoomInvitedMembers(completion: (error: Int, result: List<VoiceMemberModel>) -> Unit) {
        val memberList = ChatroomIMManager.getInstance().fetchRoomInviteMembers()
        if (memberList != null) {
            completion.invoke(VoiceServiceProtocol.ERR_OK, memberList)
        } else {
            completion.invoke(VoiceServiceProtocol.ERR_FAILED, mutableListOf())
        }
    }

    /**
     * Fetch room members
     *
     * @param completion
     * @receiver
     */
    override fun fetchRoomMembers(completion: (error: Int, result: List<VoiceMemberModel>) -> Unit) {
        val memberList = ChatroomIMManager.getInstance().fetchRoomMembers()
        if (memberList != null) {
            completion.invoke(VoiceServiceProtocol.ERR_OK, memberList)
        } else {
            completion.invoke(VoiceServiceProtocol.ERR_FAILED, mutableListOf())
        }
    }

    override fun kickMemberOutOfRoom(
        chatUidList: MutableList<String>,
        completion: (error: Int, result: Boolean) -> Unit
    ) {
        ChatroomIMManager.getInstance().removeMemberToRoom(chatUidList, object :
            ValueCallBack<ChatRoom> {
            override fun onSuccess(value: ChatRoom?) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, true)
            }

            override fun onError(code: Int, error: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, false)
            }
        })
    }

    override fun updateRoomMembers(completion: (error: Int, result: Boolean) -> Unit) {
        ChatroomIMManager.getInstance().updateRoomMembers(object : CallBack {
            override fun onSuccess() {
                completion.invoke(VoiceServiceProtocol.ERR_OK, true)
            }

            override fun onError(code: Int, error: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, false)
            }
        })
    }

    /**
     * Fetch applicants list
     *
     * @param completion
     * @receiver
     */
    override fun fetchApplicantsList(completion: (error: Int, result: List<VoiceMemberModel>) -> Unit) {
        val raisedList = ChatroomIMManager.getInstance().fetchRaisedList()
        if (raisedList != null) {
            completion.invoke(VoiceServiceProtocol.ERR_OK, raisedList)
        } else {
            completion.invoke(VoiceServiceProtocol.ERR_FAILED, mutableListOf())
        }
    }

    /**
     * Start mic seat apply
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun startMicSeatApply(micIndex: Int?, completion: (error: Int, result: Boolean) -> Unit) {
        ChatroomIMManager.getInstance().startMicSeatApply(micIndex ?: -1, object : CallBack {
            override fun onSuccess() {
                completion.invoke(VoiceServiceProtocol.ERR_OK, true)
            }

            override fun onError(code: Int, error: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, false)
            }
        })
    }

    /**
     * Accept mic seat apply
     *
     * @param micIndex
     * @param chatUid
     * @param completion
     * @receiver
     */
    override fun acceptMicSeatApply(
        micIndex: Int?,
        chatUid: String,
        completion: (error: Int, result: VoiceMicInfoModel?) -> Unit
    ) {
        ChatroomIMManager.getInstance().acceptMicSeatApply(micIndex ?: -1, chatUid, object :
            ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Cancel mic seat apply
     *
     * @param chatroomId
     * @param chatUid
     * @param completion
     * @receiver
     */
    override fun cancelMicSeatApply(
        chatroomId: String,
        chatUid: String,
        completion: (error: Int, result: Boolean) -> Unit
    ) {
        ChatroomIMManager.getInstance().cancelMicSeatApply(chatroomId, chatUid, object : CallBack {
            override fun onSuccess() {
                completion.invoke(VoiceServiceProtocol.ERR_OK, true)
            }

            override fun onError(code: Int, error: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, false)
            }
        })
    }

    /**
     * Start mic seat invitation
     *
     * @param chatUid
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun startMicSeatInvitation(
        chatUid: String,
        micIndex: Int?,
        completion: (error: Int, result: Boolean) -> Unit
    ) {
        ChatroomIMManager.getInstance().invitationMic(chatUid, micIndex ?: -1, object : CallBack {
            override fun onSuccess() {
                completion.invoke(VoiceServiceProtocol.ERR_OK, true)
            }

            override fun onError(code: Int, error: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, false)
            }
        })
    }

    /**
     * Accept mic seat invitation
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun acceptMicSeatInvitation(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit) {
        ChatroomIMManager.getInstance().acceptMicSeatInvitation(micIndex, object :
            ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel?) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Refuse invite
     *
     * @param completion
     * @receiver
     */
    override fun refuseInvite(completion: (error: Int, result: Boolean) -> Unit) {
        ChatroomIMManager.getInstance()
            .refuseInvite(VoiceBuddyFactory.get().getVoiceBuddy().chatUserName(), object : CallBack {
                override fun onSuccess() {
                    completion.invoke(VoiceServiceProtocol.ERR_OK, true)
                }

                override fun onError(code: Int, error: String?) {
                    completion.invoke(VoiceServiceProtocol.ERR_OK, false)
                }
            })
    }

    /**
     * Mute local
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun muteLocal(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit) {
        ChatroomIMManager.getInstance().muteLocal(micIndex, object :
            ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel?) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Un mute local
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun unMuteLocal(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit) {
        ChatroomIMManager.getInstance().unMuteLocal(micIndex, object :
            ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Forbid mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun forbidMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit) {
        ChatroomIMManager.getInstance().forbidMic(micIndex, object :
            ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Un forbid mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun unForbidMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit) {
        ChatroomIMManager.getInstance().unForbidMic(micIndex, object :
            ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Lock mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun lockMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit) {
        ChatroomIMManager.getInstance().lockMic(micIndex, object : ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel) {
                ThreadManager.getInstance().runOnIOThread {
                    completion.invoke(VoiceServiceProtocol.ERR_OK, value)
                }
            }

            override fun onError(error: Int, errorMsg: String?) {
                ThreadManager.getInstance().runOnIOThread {
                    completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
                }
            }

        })
    }

    /**
     * Un lock mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun unLockMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit) {
        ChatroomIMManager.getInstance().unLockMic(micIndex, object :
            ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Kick off
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun kickOff(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit) {
        ChatroomIMManager.getInstance().kickOff(micIndex, object : ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Leave mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    override fun leaveMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit) {
        ChatroomIMManager.getInstance().leaveMic(micIndex, object : ValueCallBack<VoiceMicInfoModel> {
            override fun onSuccess(value: VoiceMicInfoModel) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Change mic
     *
     * @param oldIndex
     * @param newIndex
     * @param completion
     * @receiver
     */
    override fun changeMic(
        oldIndex: Int,
        newIndex: Int,
        completion: (error: Int, result: Map<Int, VoiceMicInfoModel>?) -> Unit
    ) {
        ChatroomIMManager.getInstance().changeMic(oldIndex, newIndex, object :
            ValueCallBack<MutableMap<Int, VoiceMicInfoModel>> {
            override fun onSuccess(value: MutableMap<Int, VoiceMicInfoModel>?) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, null)
            }
        })
    }

    /**
     * Update announcement
     *
     * @param content
     * @param completion
     * @receiver
     */
    override fun updateAnnouncement(content: String, completion: (error: Int, result: Boolean) -> Unit) {
        ChatroomIMManager.getInstance().updateAnnouncement(content, object : CallBack {
            override fun onSuccess() {
                completion.invoke(VoiceServiceProtocol.ERR_OK, true)
            }

            override fun onError(code: Int, error: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, false)
            }
        })
    }

    /**
     * Enable robot
     *
     * @param enable
     * @param completion
     * @receiver
     */
    override fun enableRobot(enable: Boolean, completion: (error: Int, result: Boolean) -> Unit) {
        ChatroomIMManager.getInstance().enableRobot(enable, object :
            ValueCallBack<Boolean> {
            override fun onSuccess(value: Boolean) {
                completion.invoke(VoiceServiceProtocol.ERR_OK, value)
            }

            override fun onError(error: Int, errorMsg: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, false)
            }
        })
    }

    /**
     * Update robot volume
     *
     * @param value
     * @param completion
     * @receiver
     */
    override fun updateRobotVolume(value: Int, completion: (error: Int, result: Boolean) -> Unit) {
        ChatroomIMManager.getInstance().updateRobotVolume(value, object : CallBack {
            override fun onSuccess() {
                completion.invoke(VoiceServiceProtocol.ERR_OK, true)
            }

            override fun onError(code: Int, error: String?) {
                completion.invoke(VoiceServiceProtocol.ERR_FAILED, false)
            }
        })
    }

    private fun initRtmSync(completion: (exception: AUIRtmException?) -> Unit) {
        if (mSyncManager.rtmManager.isLogin) {
            completion.invoke(null)
            return
        }
        if (VoiceBuddyFactory.get().getVoiceBuddy().rtmToken().isEmpty()) {
            VoiceToolboxServerHttpManager.generateAllToken { rtmToken, exception ->
                val token = rtmToken ?: run {
                    LogTools.e(TAG, "initRtmSync, $exception")
                    completion.invoke(AUIRtmException(-1, exception?.message ?: "error", ""))
                    return@generateAllToken
                }
                mSyncManager.login(token, completion = {
                    if (it == null) {
                        completion.invoke(null)
                        LogTools.d(TAG, "initRtmSync, with renewToken loginRtm success")
                    } else {
                        completion.invoke(it)
                        LogTools.e(TAG, "initRtmSync, with renewToken loginRtm failed: $it")
                    }
                })
            }
        } else {
            mSyncManager.login(VoiceBuddyFactory.get().getVoiceBuddy().rtmToken(), completion = {
                if (it == null) {
                    completion.invoke(null)
                    LogTools.d(TAG, "initRtmSync, without loginRtm success")
                } else {
                    completion.invoke(it)
                    LogTools.e(TAG, "initRtmSync, without renewToken loginRtm failed: $it")
                }
            })
        }
    }

}