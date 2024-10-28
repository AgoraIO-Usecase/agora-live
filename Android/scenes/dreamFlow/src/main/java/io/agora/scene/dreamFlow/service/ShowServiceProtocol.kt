package io.agora.scene.dreamFlow.service

import io.agora.rtmsyncmanager.service.collection.AUICollectionException
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.utils.ToastUtils

/**
 * Show service protocol
 *
 * @constructor Create empty Show service protocol
 */
interface ShowServiceProtocol {

    companion object {
        /**
         * Room Available Duration
         */
        val ROOM_AVAILABLE_DURATION: Long = 72 * 60 * 60 * 1000

        private var instance : ShowServiceProtocol? = null
            get() {
                if (field == null) {
                    field = ShowSyncManagerServiceImpl(AgoraApplication.the()) {
                        if (it.message != "action error") {
                            ToastUtils.showToast(it.message)
                        }
                    }
                }
                return field
            }

        @Synchronized
        fun getImplInstance(): ShowServiceProtocol = instance!!

        @Synchronized
        fun destroy() {
            (instance as? ShowSyncManagerServiceImpl)?.destroy()
            instance = null
        }
    }

    /**
     * Destroy
     *
     */
    fun destroy()

    /**
     * Get current timestamp
     *
     * @param roomId
     * @return
     */
    fun getCurrentTimestamp(roomId: String): Long

    /**
     * Get current room duration
     *
     * @param roomId
     * @return
     */
    fun getCurrentRoomDuration(roomId: String): Long

    /**
     * Get room info
     *
     * @param roomId
     * @return
     */
    fun getRoomInfo(roomId: String): RoomDetailModel?

    /**
     * Get room list
     *
     * @return
     */
    fun getRoomList(): List<RoomDetailModel>

    /**
     * Fetch room list
     *
     * @param success
     * @param error
     * @receiver
     */
    fun fetchRoomList(
        success: (List<RoomDetailModel>) -> Unit,
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
        success: (RoomDetailModel) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Join room
     *
     * @param roomInfo
     * @param success
     * @param error
     * @receiver
     */
    fun joinRoom(
        roomInfo: RoomDetailModel,
        success: (RoomDetailModel) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Leave room
     *
     * @param roomId
     */
    fun leaveRoom(roomId: String)

    /**
     * Delete room
     *
     * @param roomId
     * @param complete
     * @receiver
     */
    fun deleteRoom(roomId: String, complete: () -> Unit)

    /**
     * Subscribe curr room event
     *
     * @param roomId
     * @param onRoomEvent
     * @receiver
     */
    fun subscribeCurrRoomEvent(roomId: String, onRoomEvent: (event: ShowRoomStatus) -> Unit)

    /**
     * Subscribe user
     *
     * @param roomId
     * @param onChange
     * @receiver
     */
    fun subscribeUser(roomId: String, onChange: (count: Int) -> Unit)

    /**
     * Subscribe user join
     *
     * @param roomId
     * @param onChange
     * @receiver
     */
    fun subscribeUserJoin(roomId: String, onChange: (userId: String, userName: String, userAvatar: String) -> Unit)

    /**
     * Subscribe user leave
     *
     * @param roomId
     * @param onChange
     * @receiver
     */
    fun subscribeUserLeave(roomId: String, onChange: (userId: String, userName: String, userAvatar: String) -> Unit)
}