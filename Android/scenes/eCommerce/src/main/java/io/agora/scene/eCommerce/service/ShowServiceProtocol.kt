package io.agora.scene.eCommerce.service

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
        var ROOM_AVAILABLE_DURATION: Long = 20 * 60 * 1000

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

    /**
     * Auction subscribe
     *
     * @param roomId
     * @param onChange
     * @receiver
     */
    fun auctionSubscribe(roomId: String, onChange: (AuctionModel) -> Unit)

    /**
     * Auction start
     *
     * @param roomId
     * @param onComplete
     * @receiver
     */
    fun auctionStart(roomId: String, onComplete: (AUICollectionException?) -> Unit)

    /**
     * Auction bidding
     *
     * @param roomId
     * @param value
     * @param onComplete
     * @receiver
     */
    fun auctionBidding(roomId: String, value: Long, onComplete: (AUICollectionException?) -> Unit)

    /**
     * Auction complete
     *
     * @param roomId
     * @param onComplete
     * @receiver
     */
    fun auctionComplete(roomId: String, onComplete: (AUICollectionException?) -> Unit)

    /**
     * Shop subscribe
     *
     * @param roomId
     * @param onChange
     * @receiver
     */
    fun shopSubscribe(roomId: String, onChange: (List<GoodsModel>) -> Unit)

    /**
     * Shop buy item
     *
     * @param roomId
     * @param itemId
     * @param onComplete
     * @receiver
     */
    fun shopBuyItem(roomId: String, itemId: String, onComplete: (AUICollectionException?) -> Unit)

    /**
     * Shop update item
     *
     * @param roomId
     * @param itemId
     * @param count
     * @param onComplete
     * @receiver
     */
    fun shopUpdateItem(roomId: String, itemId: String, count: Long, onComplete: (AUICollectionException?) -> Unit)

    /**
     * Send chat message
     *
     * @param roomId
     * @param message
     * @param success
     * @param error
     */
    fun sendChatMessage(roomId: String, message: String, success: (() -> Unit)? = null, error: ((Exception) -> Unit)? = null)

    /**
     * Subscribe message
     *
     * @param roomId
     * @param onMessageChange
     * @receiver
     */
    fun subscribeMessage(roomId: String, onMessageChange: (ShowMessage) -> Unit)

    /**
     * Like send
     *
     * @param roomId
     * @param success
     * @param error
     */
    fun likeSend(roomId: String, success: (() -> Unit)?, error: ((Exception) -> Unit)?)

    /**
     * Like subscribe
     *
     * @param roomId
     * @param onMessageChange
     * @receiver
     */
    fun likeSubscribe(roomId: String, onMessageChange: () -> Unit)
}