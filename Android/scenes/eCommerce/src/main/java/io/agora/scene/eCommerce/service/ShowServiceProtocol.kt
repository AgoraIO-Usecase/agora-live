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
     */
    fun destroy()

    fun getCurrentTimestamp(roomId: String): Long

    fun getCurrentRoomDuration(roomId: String): Long

    fun getRoomInfo(roomId: String): RoomDetailModel?

    fun getRoomList(): List<RoomDetailModel>

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

    fun deleteRoom(roomId: String, complete: () -> Unit)

    fun subscribeCurrRoomEvent(roomId: String, onRoomEvent: (event: ShowRoomStatus) -> Unit)

    fun subscribeUser(roomId: String, onChange: (count: Int) -> Unit)
    fun subscribeUserJoin(roomId: String, onChange: (userId: String, userName: String, userAvatar: String) -> Unit)
    fun subscribeUserLeave(roomId: String, onChange: (userId: String, userName: String, userAvatar: String) -> Unit)

    /** Bid Actions */
    fun auctionSubscribe(roomId: String, onChange: (AuctionModel) -> Unit)
    fun auctionStart(roomId: String, onComplete: (AUICollectionException?) -> Unit)
    fun auctionBidding(roomId: String, value: Long, onComplete: (AUICollectionException?) -> Unit)
    fun auctionComplete(roomId: String, onComplete: (AUICollectionException?) -> Unit)

    /** Shop Actions */
    fun shopSubscribe(roomId: String, onChange: (List<GoodsModel>) -> Unit)
    fun shopBuyItem(roomId: String, itemId: String, onComplete: (AUICollectionException?) -> Unit)
    fun shopUpdateItem(roomId: String, itemId: String, count: Long, onComplete: (AUICollectionException?) -> Unit)

    /** Chat Message Actions */
    fun sendChatMessage(roomId: String, message: String, success: (() -> Unit)? = null, error: ((Exception) -> Unit)? = null)
    fun subscribeMessage(roomId: String, onMessageChange: (ShowMessage) -> Unit)
    /** Like Actions */
    fun likeSend(roomId: String, success: (() -> Unit)?, error: ((Exception) -> Unit)?)
    fun likeSubscribe(roomId: String, onMessageChange: () -> Unit)
}