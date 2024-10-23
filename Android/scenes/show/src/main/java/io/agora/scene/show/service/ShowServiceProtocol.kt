package io.agora.scene.show.service

import io.agora.scene.base.component.AgoraApplication

interface ShowServiceProtocol {

    companion object {
        var ROOM_AVAILABLE_DURATION: Long = 10 * 60 * 1000
        var PK_AVAILABLE_DURATION: Long = 120 * 1000

        private var instance : ShowServiceProtocol? = null
            get() {
                if (field == null) {
                    field = ShowServiceImpl(AgoraApplication.the())
                }
                return field
            }

        @Synchronized
        fun get(): ShowServiceProtocol = instance!!

        @Synchronized
        fun destroy() {
            (instance as? ShowServiceImpl)?.destroy()
            instance = null
        }
    }

    fun getRoomList(
        success: (List<ShowRoomDetailModel>) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    fun createRoom(
        roomId: String,
        roomName: String,
        thumbnailId: String,
        success: (ShowRoomDetailModel) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    fun joinRoom(
        roomId: String,
        success: () -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    fun leaveRoom(roomId: String)

    fun subscribeCurrRoomEvent(roomId: String, onUpdate: (status: ShowSubscribeStatus, roomInfo: ShowRoomDetailModel?) -> Unit)

    fun getAllUserList(roomId: String, success: (List<ShowUser>) -> Unit, error: ((Exception) -> Unit)? = null)

    fun subscribeUser(roomId: String, onUserChange: (ShowSubscribeStatus, ShowUser?) -> Unit)

    fun sendChatMessage(
        roomId: String,
        message: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun subscribeMessage(
        roomId: String,
        onMessageChange: (ShowSubscribeStatus, ShowMessage) -> Unit
    )

    fun getAllMicSeatApplyList(
        roomId: String,
        success: (List<ShowMicSeatApply>) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    fun subscribeMicSeatApply(roomId: String, onMicSeatChange: (ShowSubscribeStatus, List<ShowMicSeatApply>) -> Unit)

    fun createMicSeatApply(roomId: String, success: ((ShowMicSeatApply) -> Unit)? = null, error: ((Exception) -> Unit)? = null)

    fun cancelMicSeatApply(roomId: String, success: (() -> Unit)? = null, error: ((Exception) -> Unit)? = null)

    fun acceptMicSeatApply(
        roomId: String,
        userId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )


    fun subscribeMicSeatInvitation(
        roomId: String,
        onMicSeatInvitationChange: (ShowSubscribeStatus, ShowMicSeatInvitation?) -> Unit
    )

    fun createMicSeatInvitation(
        roomId: String,
        userId: String,
        success: ((ShowMicSeatInvitation) -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun acceptMicSeatInvitation(
        roomId: String,
        invitationId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun rejectMicSeatInvitation(
        roomId: String,
        invitationId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun getAllPKUserList(
        roomId: String,
        success: ((List<ShowPKUser>) -> Unit),
        error: ((Exception) -> Unit)? = null
    )

    fun subscribePKInvitationChanged(roomId: String, onPKInvitationChanged: (ShowSubscribeStatus, ShowPKInvitation?) -> Unit)

    fun createPKInvitation(
        roomId: String,
        pkRoomId: String,
        success: ((ShowPKInvitation) -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun acceptPKInvitation(
        roomId: String,
        invitationId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun rejectPKInvitation(
        roomId: String,
        invitationId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun getInteractionInfo(
        roomId: String,
        success: ((ShowInteractionInfo?) -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun subscribeInteractionChanged(roomId: String, onInteractionChanged: (ShowSubscribeStatus, ShowInteractionInfo?) -> Unit)

    fun stopInteraction(
        roomId: String,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun muteAudio(
        roomId: String,
        mute: Boolean,
        success: (() -> Unit)? = null,
        error: ((Exception) -> Unit)? = null
    )

    fun subscribeReConnectEvent(roomId: String, onReconnect: () -> Unit)

    fun startCloudPlayer()

}