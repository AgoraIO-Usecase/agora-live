package io.agora.scene.voice.service

import io.agora.scene.voice.global.VoiceBuddyFactory
import io.agora.scene.voice.model.*
import io.agora.voice.common.utils.LogTools.logE

/**
 * @author create by zhangwei03
 *
 * voice chat room protocol define
 */
interface VoiceServiceProtocol {

    companion object {

        const val ERR_OK = 0
        const val ERR_FAILED = 1
        const val ERR_LOGIN_ERROR = 2
        const val ERR_LOGIN_SUCCESS = 3
        const val ERR_ROOM_UNAVAILABLE = 4
        const val ERR_ROOM_NAME_INCORRECT = 5
        const val ERR_ROOM_LIST_EMPTY = 1003
        private val instance by lazy {
            // VoiceChatServiceImp()
            VoiceSyncManagerServiceImp(VoiceBuddyFactory.get().getVoiceBuddy().application()) { error ->
                "voice chat protocol error：${error?.message}".logE()
            }
        }

        @JvmStatic
        fun getImplInstance(): VoiceServiceProtocol = instance
    }

    /**
     * Subscribe event
     *
     * @param delegate
     */
    fun subscribeEvent(delegate: VoiceRoomSubscribeDelegate)

    /**
     *  取消订阅
     */
    fun unsubscribeEvent()

    fun reset()

    fun getSubscribeDelegates():MutableList<VoiceRoomSubscribeDelegate>

    /**
     * Fetch room list
     *
     * @param page
     * @param completion
     * @receiver
     */
    fun fetchRoomList(
        page: Int = 0,
        completion: (error: Int, result: List<VoiceRoomModel>) -> Unit
    )

    /**
     * Create room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun createRoom(inputModel: VoiceCreateRoomModel, completion: (error: Int, result: VoiceRoomModel) -> Unit)

    /**
     * Join room
     *
     * @param roomId
     * @param completion
     * @receiver
     */
    fun joinRoom(roomId: String, completion: (error: Int, result: VoiceRoomModel?) -> Unit)

    /**
     * Leave room
     *
     * @param roomId
     * @param isRoomOwnerLeave
     * @param completion
     * @receiver
     */
    fun leaveRoom(roomId: String, isRoomOwnerLeave: Boolean, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * Fetch room detail
     *
     * @param voiceRoomModel
     * @param completion
     * @receiver
     */
    fun fetchRoomDetail(voiceRoomModel: VoiceRoomModel, completion: (error: Int, result: VoiceRoomInfo?) -> Unit)

    /**
     * Fetch gift contribute
     *
     * @param completion
     * @receiver
     */
    fun fetchGiftContribute(completion: (error: Int, result: List<VoiceRankUserModel>?) -> Unit)

    /**
     * Fetch room invited members
     *
     * @param completion
     * @receiver
     */
    fun fetchRoomInvitedMembers(completion: (error: Int, result: List<VoiceMemberModel>) -> Unit)

    /**
     * Fetch room members
     *
     * @param completion
     * @receiver
     */
    fun fetchRoomMembers(completion: (error: Int, result: List<VoiceMemberModel>) -> Unit)

    fun kickMemberOutOfRoom(chatUidList: MutableList<String>,completion: (error: Int, result: Boolean) -> Unit)

    /**
     * Update room members
     *
     * @param completion
     * @receiver
     */
    fun updateRoomMembers(completion: (error: Int, result: Boolean) -> Unit)

    /**
     * Fetch applicants list
     *
     * @param completion
     * @receiver
     */
    fun fetchApplicantsList(completion: (error: Int, result: List<VoiceMemberModel>) -> Unit)

    /**
     * Start mic seat apply
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun startMicSeatApply(micIndex: Int? = null, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * Accept mic seat apply
     *
     * @param micIndex
     * @param chatUid
     * @param completion
     * @receiver
     */
    fun acceptMicSeatApply(micIndex: Int?, chatUid: String, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Cancel mic seat apply
     *
     * @param chatroomId
     * @param chatUid
     * @param completion
     * @receiver
     */
    fun cancelMicSeatApply(chatroomId: String, chatUid: String, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * Start mic seat invitation
     *
     * @param chatUid
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun startMicSeatInvitation(chatUid: String, micIndex: Int?, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * Accept mic seat invitation
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun acceptMicSeatInvitation(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Refuse invite
     *
     * @param completion
     * @receiver
     */
    fun refuseInvite(completion: (error: Int, result: Boolean) -> Unit)

    /**
     * Mute local
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun muteLocal(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Un mute local
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun unMuteLocal(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Forbid mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun forbidMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Un forbid mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun unForbidMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Lock mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun lockMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Un lock mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun unLockMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Kick off
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun kickOff(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Leave mic
     *
     * @param micIndex
     * @param completion
     * @receiver
     */
    fun leaveMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * Change mic
     *
     * @param oldIndex
     * @param newIndex
     * @param completion
     * @receiver
     */
    fun changeMic(
        oldIndex: Int,
        newIndex: Int,
        completion: (error: Int, result: Map<Int, VoiceMicInfoModel>?) -> Unit
    )

    /**
     * Update announcement
     *
     * @param content
     * @param completion
     * @receiver
     */
    fun updateAnnouncement(content: String, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * Update b g m info
     *
     * @param info
     * @param completion
     * @receiver
     */
    fun updateBGMInfo(info: VoiceBgmModel, completion: (error: Int) -> Unit)

    /**
     * Enable robot
     *
     * @param enable
     * @param completion
     * @receiver
     */
    fun enableRobot(enable: Boolean, completion: (error: Int, enable: Boolean) -> Unit)

    /**
     * Update robot volume
     *
     * @param value
     * @param completion
     * @receiver
     */
    fun updateRobotVolume(value: Int, completion: (error: Int, result: Boolean) -> Unit)

    fun subscribeRoomTimeUp(
        onRoomTimeUp: () -> Unit
    )
}