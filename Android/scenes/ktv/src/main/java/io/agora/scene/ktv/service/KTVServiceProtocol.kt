package io.agora.scene.ktv.service

import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.ktv.KTVLogger

/**
 * K t v service protocol
 *
 * @constructor Create empty K t v service protocol
 *//*
 * service 模块
 * 简介：这个模块的作用是负责前端业务模块和业务服务器的交互(包括房间列表+房间内的业务数据同步等)
 * 实现原理：该场景的业务服务器是包装了一个 rethinkDB 的后端服务，用于数据存储，可以认为它是一个 app 端上可以自由写入的 DB，房间列表数据、房间内的业务数据等在 app 上构造数据结构并存储在这个 DB 里
 * 当 DB 内的数据发生增删改时，会通知各端，以此达到业务数据同步的效果
 * TODO 注意⚠️：该场景的后端服务仅做场景演示使用，无法商用，如果需要上线，您必须自己部署后端服务或者云存储服务器（例如leancloud、环信等）并且重新实现这个模块！！！！！！！！！！！
 */
interface KTVServiceProtocol {

    /**
     * K t v subscribe
     *
     * @constructor Create empty K t v subscribe
     */
    enum class KTVSubscribe {
        /**
         * K t v subscribe created
         *
         * @constructor Create empty K t v subscribe created
         */
        KTVSubscribeCreated,      //创建

        /**
         * K t v subscribe deleted
         *
         * @constructor Create empty K t v subscribe deleted
         */
        KTVSubscribeDeleted,      //删除

        /**
         * K t v subscribe updated
         *
         * @constructor Create empty K t v subscribe updated
         */
        KTVSubscribeUpdated,      //更新
    }

    companion object {
        private val instance by lazy {
            // KTVServiceImp()
            KTVSyncManagerServiceImp(AgoraApplication.the()) { error ->
                error?.message?.let { KTVLogger.e("SyncManager", it) }
            }
        }

        fun getImplInstance(): KTVServiceProtocol = instance
    }

    /**
     * Reset
     *
     */
    fun reset()

    // ============== 房间相关 ==============

    /**
     * Get room list
     *
     * @param completion
     * @receiver
     */
    fun getRoomList(completion: (error: Exception?, list: List<RoomListModel>?) -> Unit)

    /**
     * Create room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun createRoom(
        inputModel: CreateRoomInputModel,
        completion: (error: Exception?, out: CreateRoomOutputModel?) -> Unit
    )

    /**
     * Join room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun joinRoom(
        inputModel: JoinRoomInputModel,
        completion: (error: Exception?, out: JoinRoomOutputModel?) -> Unit
    )

    /**
     * Leave room
     *
     * @param completion
     * @receiver
     */
    fun leaveRoom(
        completion: (error: Exception?) -> Unit
    )

    /**
     * Change m v cover
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun changeMVCover(
        inputModel: ChangeMVCoverInputModel, completion: (error: Exception?) -> Unit
    )

    /**
     * Subscribe room status
     *
     * @param changedBlock
     * @receiver
     */
    fun subscribeRoomStatus(
        changedBlock: (KTVSubscribe, RoomListModel?) -> Unit
    )

    /**
     * Subscribe user list count
     *
     * @param changedBlock
     * @receiver
     */
    fun subscribeUserListCount(
        changedBlock: (count: Int) -> Unit
    )

    /**
     * Subscribe room time up
     *
     * @param onRoomTimeUp
     * @receiver
     */
    fun subscribeRoomTimeUp(
        onRoomTimeUp: () -> Unit
    )


    // ===================== 麦位相关 =================================

    /**
     * Get seat status list
     *
     * @param completion
     * @receiver
     */
    fun getSeatStatusList(
        completion: (error: Exception?, list: List<RoomSeatModel>?) -> Unit
    )

    /**
     * On seat
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun onSeat(
        inputModel: OnSeatInputModel,
        completion: (error: Exception?) -> Unit
    )

    /**
     * Auto on seat
     *
     * @param completion
     * @receiver
     */
    fun autoOnSeat(
        completion: (error: Exception?) -> Unit
    )

    /**
     * Out seat
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun outSeat(
        inputModel: OutSeatInputModel, completion: (error: Exception?) -> Unit
    )

    /**
     * Update seat audio mute status
     *
     * @param mute
     * @param completion
     * @receiver
     */
    fun updateSeatAudioMuteStatus(
        mute: Boolean, completion: (error: Exception?) -> Unit
    )

    /**
     * Update seat video mute status
     *
     * @param mute
     * @param completion
     * @receiver
     */
    fun updateSeatVideoMuteStatus(
        mute: Boolean, completion: (error: Exception?) -> Unit
    )

    /**
     * Subscribe seat list
     *
     * @param changedBlock
     * @receiver
     */
    fun subscribeSeatList(
        changedBlock: (KTVServiceProtocol.KTVSubscribe, RoomSeatModel?) -> Unit
    )

    // =================== 歌曲相关 =========================

    /**
     * Get choosed songs list
     *
     * @param completion
     * @receiver
     */
    fun getChoosedSongsList(
        completion: (error: Exception?, list: List<RoomSelSongModel>?) -> Unit
    )

    /**
     * Remove song
     *
     * @param isSingingSong
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun removeSong(
        isSingingSong: Boolean, inputModel: RemoveSongInputModel, completion: (error: Exception?) -> Unit
    )

    /**
     * Choose song
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun chooseSong(
        inputModel: ChooseSongInputModel, completion: (error: Exception?) -> Unit
    )

    /**
     * Make song top
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun makeSongTop(
        inputModel: MakeSongTopInputModel, completion: (error: Exception?) -> Unit
    )

    /**
     * Make song did play
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun makeSongDidPlay(inputModel: RoomSelSongModel, completion: (error: Exception?) -> Unit)

    /**
     * Join chorus
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun joinChorus(
        inputModel: RoomSelSongModel,
        completion: (error: Exception?) -> Unit
    )

    /**
     * Leave chorus
     *
     * @param completion
     * @receiver
     */
    fun leaveChorus(
        completion: (error: Exception?) -> Unit
    )

    /**
     * Subscribe choose song
     *
     * @param changedBlock
     * @receiver
     */
    fun subscribeChooseSong(
        changedBlock: (KTVSubscribe, RoomSelSongModel?) -> Unit
    )

    // =================== 断网重连相关 =========================

    /**
     * Subscribe re connect event
     *
     * @param onReconnect
     * @receiver
     */
    fun subscribeReConnectEvent(onReconnect: () -> Unit)

    /**
     * Get all user list
     *
     * @param success
     * @param error
     * @receiver
     */
    fun getAllUserList(success: (userNum : Int) -> Unit, error: ((Exception) -> Unit)? = null)
}