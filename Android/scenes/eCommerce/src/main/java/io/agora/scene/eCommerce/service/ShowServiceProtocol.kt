package io.agora.scene.eCommerce.service

import io.agora.rtmsyncmanager.model.AUIRoomInfo
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

    fun getRoomInfo(roomId: String): AUIRoomInfo?

    fun getRoomList(): List<AUIRoomInfo>

    fun fetchRoomList(
        success: (List<AUIRoomInfo>) -> Unit,
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
        success: (AUIRoomInfo) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    fun joinRoom(
        roomInfo: AUIRoomInfo,
        success: (AUIRoomInfo) -> Unit,
        error: ((Exception) -> Unit)? = null
    )

    /**
     * Leave room
     *
     * @param roomId
     */
    fun leaveRoom(roomId: String)

    fun deleteRoom(roomId: String, complete: () -> Unit)

    fun subscribeUser(roomId: String, onChange: (List<ShowUser>) -> Unit)

    /** Bid Actions */
    fun subscribeAuction(roomId: String, onChange: (AuctionModel) -> Unit)
    fun startAuction(roomId: String)
    fun bidding(roomId: String)
    fun finishAuction(roomId: String)

    /** Shop Actions */
    fun subscribeShop(roomId: String, onChange: (List<GoodsModel>) -> Unit)
    fun buyItem(roomId: String, itemId: String, onComplete: (Exception?) -> Unit)
    fun updateRemain(roomId: String, itemId: String, count: UInt)

    /** Chat Message Actions */
    fun sendChatMessage(roomId: String, message: String, success: (() -> Unit)? = null, error: ((Exception) -> Unit)? = null)
    fun subscribeMessage(roomId: String, onMessageChange: (ShowMessage) -> Unit)

    /**
     * Start cloud player
     *
     */
    fun startCloudPlayer()

}