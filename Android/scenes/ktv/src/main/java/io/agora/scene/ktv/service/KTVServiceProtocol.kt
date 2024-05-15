package io.agora.scene.ktv.service

import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserInfo
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.ktv.KTVLogger

/**
 * Ktv service listener protocol
 *
 * @constructor Create empty Ktv service listener protocol
 */
interface KtvServiceListenerProtocol {

    /**
     * On user list did changed
     *
     * @param userList
     */
    fun onUserListDidChanged(userList: List<AUIUserInfo>)

    /**
     * On room did changed
     *
     * @param roomInfo
     */
    fun onRoomDidChanged(roomInfo: AUIRoomInfo)

    /**
     * On room expire
     *
     * @param channelName
     */
    fun onRoomExpire(channelName: String)

    /**
     * On room destroy
     *
     * @param channelName
     */
    fun onRoomDestroy(channelName: String)

    fun onRoomSeatDidChanged(ktvSubscribe: KTVServiceProtocol.KTVSubscribe, roomSeatModel: RoomSeatModel)

    fun onChooseSong(ktvSubscribe: KTVServiceProtocol.KTVSubscribe, roomSelSongModel: RoomSelSongModel)
}

/**
 * K t v service protocol
 *
 * @constructor Create empty K t v service protocol
 *//*
 * service 模块
 * 简介：这个模块的作用是负责前端业务模块和业务服务器的交互(包括房间列表+房间内的业务数据同步等)
 * 实现原理：该场景的业务服务器是包装了一个 rethinkDB 的后端服务，用于数据存储，可以认为它是一个 app 端上可以自由写入的 DB，房间列表数据、房间内的业务数据等在 app 上构造数据结构并存储在这个 DB 里
 * 当 DB 内的数据发生增删改时，会通知各端，以此达到业务数据同步的效果
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
    fun getRoomList(completion: (list: List<AUIRoomInfo>) -> Unit)

    /**
     * Create room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun createRoom(inputModel: CreateRoomInputModel, completion: (error: Exception?, out: AUIRoomInfo?) -> Unit)

    /**
     * Join room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun joinRoom(inputModel: JoinRoomInputModel, completion: (error: Exception?, out: JoinRoomOutputModel?) -> Unit)

    /**
     * Leave room
     *
     * @param completion
     * @receiver
     */
    fun leaveRoom(completion: (error: Exception?) -> Unit)

    /**
     * Update room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun updateRoom(inputModel: AUIRoomInfo, completion: (error: Exception?) -> Unit)

    /**
     * Get all user list
     *
     * @param success
     * @param error
     * @receiver
     */
    fun getAllUserList(completion: (error: Exception?, list: List<AUIUserInfo>?) -> Unit)

    // ===================== 麦位相关 =================================

    /**
     * Get seat status list
     *
     * @param completion
     * @receiver
     */
    fun getSeatStatusList(completion: (error: Exception?, list: List<RoomSeatModel>?) -> Unit)

    /**
     * On seat
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun onSeat(inputModel: OnSeatInputModel, completion: (error: Exception?) -> Unit)

    /**
     * Auto on seat
     *
     * @param completion
     * @receiver
     */
    fun autoOnSeat(completion: (error: Exception?) -> Unit)

    /**
     * Out seat
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun outSeat(inputModel: OutSeatInputModel, completion: (error: Exception?) -> Unit)

    /**
     * Update seat audio mute status
     *
     * @param mute
     * @param completion
     * @receiver
     */
    fun updateSeatAudioMuteStatus(mute: Boolean, completion: (error: Exception?) -> Unit)

    /**
     * Update seat video mute status
     *
     * @param mute
     * @param completion
     * @receiver
     */
    fun updateSeatVideoMuteStatus(mute: Boolean, completion: (error: Exception?) -> Unit)

    // =================== 歌曲相关 =========================
    /**
     * Get choosed songs list
     *
     * @param completion
     * @receiver
     */
    fun getChoosedSongsList(completion: (error: Exception?, list: List<RoomSelSongModel>?) -> Unit)

    /**
     * Remove song
     *
     * @param isSingingSong
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun removeSong(isSingingSong: Boolean, inputModel: RemoveSongInputModel, completion: (error: Exception?) -> Unit)

    /**
     * Choose song
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun chooseSong(inputModel: ChooseSongInputModel, completion: (error: Exception?) -> Unit)

    /**
     * Make song top
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun makeSongTop(inputModel: MakeSongTopInputModel, completion: (error: Exception?) -> Unit)

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
    fun joinChorus(inputModel: RoomSelSongModel, completion: (error: Exception?) -> Unit)

    /**
     * Leave chorus
     *
     * @param completion
     * @receiver
     */
    fun leaveChorus(completion: (error: Exception?) -> Unit)

    /**
     * Subscribe listener
     *
     * @param listener
     */
    fun subscribeListener(listener: KtvServiceListenerProtocol)
}