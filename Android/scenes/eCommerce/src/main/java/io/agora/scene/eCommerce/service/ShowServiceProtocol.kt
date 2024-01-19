package io.agora.scene.eCommerce.service

import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.utils.ToastUtils

/**
 * Show service protocol
 *
 * @constructor Create empty Show service protocol
 */
interface ShowServiceProtocol {

    /**
     * Show subscribe status
     *
     * @constructor Create empty Show subscribe status
     */
    enum class ShowSubscribeStatus {
        /**
         * Deleted
         *
         * @constructor Create empty Deleted
         */
        deleted,

        /**
         * Updated
         *
         * @constructor Create empty Updated
         */
        updated
    }

    companion object {
        /**
         * Room Available Duration
         */
        var ROOM_AVAILABLE_DURATION: Long = 1200 * 1000

        private val instance by lazy {
            ShowSyncManagerServiceImpl(AgoraApplication.the()){
                if (it.message != "action error") {
                    ToastUtils.showToast(it.message)
                }
            }
        }

        /**
         * Get impl instance
         *
         * @return
         */
        fun getImplInstance(): ShowServiceProtocol = instance
    }

    /**
     * Destroy
     *
     */
    fun destroy()

    /**
     * Get room list
     *
     * @param success
     * @param error
     * @receiver
     */
    fun getRoomList(
        success: (List<ShowRoomDetailModel>) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

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
    fun createRoom(
        roomId: String,
        roomName: String,
        thumbnailId: String,
        success: (ShowRoomDetailModel) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Join room
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     */
    fun joinRoom(
        roomId: String,
        success: (ShowRoomDetailModel) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Leave room
     *
     * @param roomId
     */
    fun leaveRoom(roomId: String)

    /**
     * Subscribe curr room event
     *
     * @param roomId
     * @param onUpdate
     * @receiver
     */
    fun subscribeCurrRoomEvent(roomId: String, onUpdate: (status: ShowSubscribeStatus, roomInfo: ShowRoomDetailModel?) -> Unit)

    /**
     * Get all user list
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     */
    fun getAllUserList(roomId: String, success: (List<ShowUser>) -> Unit, error: ((Exception) -> Unit)? = null)

    /**
     * Subscribe user
     *
     * @param roomId
     * @param onUserChange
     * @receiver
     */
    fun subscribeUser(roomId: String, onUserChange: (ShowSubscribeStatus, ShowUser?) -> Unit)

    /**
     * Send chat message
     *
     * @param roomId
     * @param message
     * @param success
     * @param error
     */
    fun sendChatMessage(
        roomId: String,
        message: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Subscribe message
     *
     * @param roomId
     * @param onMessageChange
     * @receiver
     */
    fun subscribeMessage(
        roomId: String,
        onMessageChange: (ShowSubscribeStatus, ShowMessage) -> Unit
    )


    /**
     * Get all mic seat apply list
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     */
    fun getAllMicSeatApplyList(
        roomId: String,
        success: (List<ShowMicSeatApply>) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Subscribe mic seat apply
     *
     * @param roomId
     * @param onMicSeatChange
     * @receiver
     */
    fun subscribeMicSeatApply(roomId: String, onMicSeatChange: (ShowSubscribeStatus, ShowMicSeatApply?) -> Unit)

    /**
     * Create mic seat apply
     *
     * @param roomId
     * @param success
     * @param error
     */
    fun createMicSeatApply(roomId: String, success: (() -> Unit)? = null, error: ((Exception) -> Unit)? = null)

    /**
     * Cancel mic seat apply
     *
     * @param roomId
     * @param success
     * @param error
     */
    fun cancelMicSeatApply(roomId: String, success: (() -> Unit)? = null, error: ((Exception) -> Unit)? = null)

    /**
     * Accept mic seat apply
     *
     * @param roomId
     * @param apply
     * @param success
     * @param error
     */
    fun acceptMicSeatApply(
        roomId: String,
        apply: ShowMicSeatApply,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Reject mic seat apply
     *
     * @param roomId
     * @param apply
     * @param success
     * @param error
     */
    fun rejectMicSeatApply(
        roomId: String,
        apply: ShowMicSeatApply,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Get all mic seat invitation list
     *
     * @param roomId
     * @param success
     * @param error
     * @receiver
     */
    fun getAllMicSeatInvitationList(
        roomId: String,
        success: ((List<ShowMicSeatInvitation>) -> Unit),
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Subscribe mic seat invitation
     *
     * @param roomId
     * @param onMicSeatInvitationChange
     * @receiver
     */
    fun subscribeMicSeatInvitation(roomId: String,onMicSeatInvitationChange: (ShowSubscribeStatus, ShowMicSeatInvitation?) -> Unit)

    /**
     * Create mic seat invitation
     *
     * @param roomId
     * @param user
     * @param success
     * @param error
     */
    fun createMicSeatInvitation(
        roomId: String,
        user: ShowUser,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Cancel mic seat invitation
     *
     * @param roomId
     * @param userId
     * @param success
     * @param error
     */
    fun cancelMicSeatInvitation(
        roomId: String,
        userId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Accept mic seat invitation
     *
     * @param roomId
     * @param success
     * @param error
     */
    fun acceptMicSeatInvitation(
        roomId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Reject mic seat invitation
     *
     * @param roomId
     * @param success
     * @param error
     */
    fun rejectMicSeatInvitation(
        roomId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Mute audio
     *
     * @param roomId
     * @param mute
     * @param userId
     * @param success
     * @param error
     */
    fun muteAudio(
        roomId: String,
        mute: Boolean,
        userId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Subscribe re connect event
     *
     * @param roomId
     * @param onReconnect
     * @receiver
     */
    fun subscribeReConnectEvent(roomId: String, onReconnect: () -> Unit)

    /**
     * Start cloud player
     *
     */
    fun startCloudPlayer()

}