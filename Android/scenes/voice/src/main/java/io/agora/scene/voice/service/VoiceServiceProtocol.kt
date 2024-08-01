package io.agora.scene.voice.service

import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.utils.ObservableHelper
import io.agora.scene.voice.global.VoiceBuddyFactory
import io.agora.scene.voice.model.*
import io.agora.voice.common.utils.LogTools

/**
 * This interface defines the protocol for voice chat room service.
 * It includes methods for subscribing to events, fetching room lists, creating rooms, joining rooms, leaving rooms, fetching room details, fetching gift contributions, fetching room invited members, fetching room members, kicking members out of room, updating room members, fetching applicants list, starting mic seat apply, accepting mic seat apply, cancelling mic seat apply, starting mic seat invitation, accepting mic seat invitation, refusing invite, muting local, unmuting local, forbidding mic, unforbidding mic, locking mic, unlocking mic, kicking off, leaving mic, changing mic, updating announcement, updating BGM info, enabling robot, updating robot volume, and subscribing room time up.
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
        private var innerProtocol: VoiceServiceProtocol? = null

        @JvmStatic
        val serviceProtocol: VoiceServiceProtocol
            get() {
                if (innerProtocol == null) {
                    innerProtocol =   VoiceSyncManagerServiceImp(VoiceBuddyFactory.get().getVoiceBuddy().application()) { error ->
                        LogTools.e("VoiceServiceProtocol", "voice chat protocol error：${error?.message}")
                    }
                }
                return innerProtocol!!
            }

        fun reset() {
            innerProtocol = null
        }
    }

    /**
     * This method subscribes to events.
     *
     * @param delegate The delegate to subscribe to events.
     */
    fun subscribeListener(delegate: VoiceServiceListenerProtocol)

    /**
     * This method unsubscribes from events.
     */
    fun unsubscribeEvent()

    /**
     * Get current duration
     *
     * @param channelName
     * @return
     */
    fun getCurrentDuration(channelName: String): Long

    /**
     * Get current ts
     *
     * @param channelName
     * @return
     */
    fun getCurrentTs(channelName: String): Long

    /**
     * This method gets the list of subscribed delegates.
     *
     * @return The list of subscribed delegates.
     */
    fun getSubscribeListeners(): ObservableHelper<VoiceServiceListenerProtocol>

    /**
     * This method fetches the list of rooms.
     *
     * @param page The page number to fetch.
     * @param completion The completion handler to call when the fetch is complete.
     */
    fun getRoomList(
        completion: (error: Exception?, roomList: List<AUIRoomInfo>?) -> Unit
    )

    /**
     * This method creates a room.
     *
     * @param inputModel The model of the room to create.
     * @param completion The completion handler to call when the creation is complete.
     */
    fun createRoom(inputModel: VoiceCreateRoomModel,  completion: (error: Exception?, out: AUIRoomInfo?) -> Unit)

    /**
     * This method joins a room.
     *
     * @param roomId The ID of the room to join.
     * @param password The password of the room to join.
     * @param completion The completion handler to call when the join is complete.
     */
    fun joinRoom(roomId: String, password: String?, completion: (error: Exception?, roomInfo: AUIRoomInfo?) -> Unit)

    /**
     * This method leaves a room.
     *
     * @param completion The completion handler to call when the leave is complete.
     */
    fun leaveRoom(completion: (error: Exception?) -> Unit)

    /**
     * This method fetches the details of a room.
     *
     * @param voiceRoomModel The model of the room to fetch details for.
     * @param completion The completion handler to call when the fetch is complete.
     */
    fun fetchRoomDetail(voiceRoomModel: VoiceRoomModel, completion: (error: Int, result: VoiceRoomInfo?) -> Unit)

    /**
     * This method fetches the gift contribution.
     *
     * @param completion The completion handler to call when the fetch is complete.
     */
    fun fetchGiftContribute(completion: (error: Int, result: List<VoiceRankUserModel>?) -> Unit)

    /**
     * This method fetches the list of members invited to the room.
     *
     * @param completion The completion handler to call when the fetch is complete.
     */
    fun fetchRoomInvitedMembers(completion: (error: Int, result: List<VoiceMemberModel>) -> Unit)

    /**
     * This method fetches the list of members in the room.
     *
     * @param completion The completion handler to call when the fetch is complete.
     */
    fun fetchRoomMembers(completion: (error: Int, result: List<VoiceMemberModel>) -> Unit)

    /**
     * This method kicks a member out of the room.
     *
     * @param chatUidList The list of chat UIDs of the members to kick out.
     * @param completion The completion handler to call when the kick out is complete.
     */
    fun kickMemberOutOfRoom(chatUidList: MutableList<String>, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * This method updates the list of members in the room.
     *
     * @param completion The completion handler to call when the update is complete.
     */
    fun updateRoomMembers(completion: (error: Int, result: Boolean) -> Unit)

    /**
     * This method fetches the list of applicants to the room.
     *
     * @param completion The completion handler to call when the fetch is complete.
     */
    fun fetchApplicantsList(completion: (error: Int, result: List<VoiceMemberModel>) -> Unit)

    /**
     * This method starts a mic seat apply.
     *
     * @param micIndex The index of the mic to apply for.
     * @param completion The completion handler to call when the apply is complete.
     */
    fun startMicSeatApply(micIndex: Int? = null, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * This method accepts a mic seat apply.
     *
     * @param micIndex The index of the mic to accept the apply for.
     * @param chatUid The chat UID of the member to accept the apply for.
     * @param completion The completion handler to call when the accept is complete.
     */
    fun acceptMicSeatApply(
        micIndex: Int?,
        chatUid: String,
        completion: (error: Int, result: VoiceMicInfoModel?) -> Unit
    )

    /**
     * This method cancels a mic seat apply.
     *
     * @param chatroomId The ID of the chatroom to cancel the apply for.
     * @param chatUid The chat UID of the member to cancel the apply for.
     * @param completion The completion handler to call when the cancel is complete.
     */
    fun cancelMicSeatApply(chatroomId: String, chatUid: String, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * This method starts a mic seat invitation.
     *
     * @param chatUid The chat UID of the member to invite.
     * @param micIndex The index of the mic to invite the member to.
     * @param completion The completion handler to call when the invitation is complete.
     */
    fun startMicSeatInvitation(chatUid: String, micIndex: Int?, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * This method accepts a mic seat invitation.
     *
     * @param micIndex The index of the mic to accept the invitation for.
     * @param completion The completion handler to call when the accept is complete.
     */
    fun acceptMicSeatInvitation(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * This method refuses an invite.
     *
     * @param completion The completion handler to call when the refuse is complete.
     */
    fun refuseInvite(completion: (error: Int, result: Boolean) -> Unit)

    /**
     * This method mutes the local mic.
     *
     * @param micIndex The index of the mic to mute.
     * @param completion The completion handler to call when the mute is complete.
     */
    fun muteLocal(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * This method unmutes the local mic.
     *
     * @param micIndex The index of the mic to unmute.
     * @param completion The completion handler to call when the unmute is complete.
     */
    fun unMuteLocal(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * This method forbids a mic.
     *
     * @param micIndex The index of the mic to forbid.
     * @param completion The completion handler to call when the forbid is complete.
     */
    fun forbidMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * This method unforbids a mic.
     *
     * @param micIndex The index of the mic to unforbid.
     * @param completion The completion handler to call when the unforbid is complete.
     */
    fun unForbidMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * This method locks a mic.
     *
     * @param micIndex The index of the mic to lock.
     * @param completion The completion handler to call when the lock is complete.
     */
    fun lockMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * This method unlocks a mic.
     *
     * @param micIndex The index of the mic to unlock.
     * @param completion The completion handler to call when the unlock is complete.
     */
    fun unLockMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * This method kicks a user off a microphone in the VR application.
     *
     * @param micIndex The index of the microphone to kick the user off from.
     * @param completion The completion handler to call when the kick off is complete. It includes an error code and the updated microphone information.
     */
    fun kickOff(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * This method allows a user to leave a microphone in the VR application.
     *
     * @param micIndex The index of the microphone to leave.
     * @param completion The completion handler to call when the leave is complete. It includes an error code and the updated microphone information.
     */
    fun leaveMic(micIndex: Int, completion: (error: Int, result: VoiceMicInfoModel?) -> Unit)

    /**
     * This method allows a user to change from one microphone to another in the VR application.
     *
     * @param oldIndex The index of the microphone to leave.
     * @param newIndex The index of the microphone to join.
     * @param completion The completion handler to call when the change is complete. It includes an error code and a map of the updated microphone information.
     */
    fun changeMic(
        oldIndex: Int,
        newIndex: Int,
        completion: (error: Int, result: Map<Int, VoiceMicInfoModel>?) -> Unit
    )

    /**
     * This method updates the announcement in the VR application.
     *
     * @param content The new announcement content.
     * @param completion The completion handler to call when the update is complete. It includes an error code and a boolean indicating whether the update was successful.
     */
    fun updateAnnouncement(content: String, completion: (error: Int, result: Boolean) -> Unit)

    /**
     * This method enables or disables the robot in the VR application.
     *
     * @param enable A boolean indicating whether to enable the robot.
     * @param completion The completion handler to call when the enable or disable is complete. It includes an error code and a boolean indicating whether the robot is enabled.
     */
    fun enableRobot(enable: Boolean, completion: (error: Int, enable: Boolean) -> Unit)

    /**
     * This method updates the robot's volume in the VR application.
     *
     * @param value The new volume value for the robot.
     * @param completion The completion handler to call when the update is complete. It includes an error code and a boolean indicating whether the update was successful.
     */
    fun updateRobotVolume(value: Int, completion: (error: Int, result: Boolean) -> Unit)
}