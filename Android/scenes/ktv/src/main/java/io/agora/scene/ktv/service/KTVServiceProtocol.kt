package io.agora.scene.ktv.service

import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserInfo
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.ktv.KTVLogger
import io.agora.scene.ktv.widget.song.SongItem

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

    /**
     * On add or update seat
     *
     * @param roomSeatModel
     */
    fun onUserEnterSeat(roomSeatModel: RoomMicSeatInfo)

    /**
     * On user audio mute
     *
     * @param userId
     * @param mute
     */
    fun onUserAudioMute(userId: String, mute: Boolean)

    /**
     * On user video mute
     *
     * @param userId
     * @param mute
     */
    fun onUserVideoMute(userId: String, mute: Boolean)

    /**
     * On remove seat
     *
     * @param roomSeatModel
     */
    fun onUserLeaveSeat(roomSeatModel: RoomMicSeatInfo)

    /**
     * On update all choose songs
     *
     * @param chosenSongs
     */
    fun onChosenSongListDidChanged(chosenSongList: List<RoomSongInfo>)

    /**
     * On choristers list did changed
     *
     * @param choristerInfo
     */
    fun onChoristerListDidChanged(choristerList: List<RoomChoristerInfo>)

    /**
     * On chorister did enter
     *
     * @param chorister
     */
    fun onChoristerDidEnter(chorister: RoomChoristerInfo)

    /**
     * On chorister did leave
     *
     * @param chorister
     */
    fun onChoristerDidLeave(chorister: RoomChoristerInfo)
}

/**
 * Ktv service protocol
 *
 * @constructor Create empty Ktv service protocol
 */
interface KTVServiceProtocol {

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
     * @param createRoomInfo
     * @param completion
     * @receiver
     */
    fun createRoom(createRoomInfo: CreateRoomInfo, completion: (error: Exception?, out: JoinRoomInfo?) -> Unit)

    /**
     * Join room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    fun joinRoom(roomId: String, password: String?, completion: (error: Exception?, out: JoinRoomInfo?) -> Unit)

    /**
     * Leave room
     *
     * @param completion
     * @receiver
     */
    fun leaveRoom(completion: (error: Exception?) -> Unit)

    /**
     * Get all user list
     *
     * @param success
     * @param error
     * @receiver
     */
    fun getAllUserList(completion: (error: Exception?, list: List<AUIUserInfo>?) -> Unit)

    /**
     * Get user info
     *
     * @param userId
     * @return
     */
    fun getUserInfo(userId: String): AUIUserInfo?

    /**
     * Mute user audio
     *
     * @param mute
     * @param completion
     * @receiver
     */
    fun muteUserAudio(mute: Boolean, completion: (error: Exception?) -> Unit)

    /**
     * Mute user video
     *
     * @param mute
     * @param completion
     * @receiver
     */
    fun muteUserVideo(mute: Boolean, completion: (error: Exception?) -> Unit)

    /**
     * Get all seat list
     *
     * @param completion
     * @receiver
     */
    fun getAllSeatMap(completion: (error: Exception?, map: Map<Int, RoomMicSeatInfo>) -> Unit)

    /**
     * On seat
     *
     * @param seatIndex null autoOnseat
     * @param completion
     * @receiver
     */
    fun enterSeat(seatIndex: Int?, completion: (error: Exception?) -> Unit)

    /**
     * Out seat
     *
     * @param seatIndex
     * @param completion
     * @receiver
     */
    fun outSeat(seatIndex: Int, completion: (error: Exception?) -> Unit)

    /**
     * Get all song
     *
     * @param completion
     * @receiver
     */
    fun getAllSongList(completion: (error: Exception?, list: List<SongItem>) -> Unit)

    /**
     * Get chosen songs list
     *
     * @param completion`
     * @receiver
     */
    fun getChosenSongList(completion: (error: Exception?, list: List<RoomSongInfo>?) -> Unit)

    /**
     * Remove song
     *
     * @param songInfo
     * @param completion
     * @receiver
     */
    fun removeSong(songCode: String, completion: (error: Exception?) -> Unit)

    /**
     * Choose song
     *
     * @param songInfo
     * @param completion
     * @receiver
     */
    fun chooseSong(songInfo: RoomSongInfo, completion: (error: Exception?) -> Unit)

    /**
     * Make song top
     *
     * @param songCode
     * @param completion
     * @receiver
     */
    fun pinSong(songCode: String, completion: (error: Exception?) -> Unit)

    /**
     * Make song did play
     *
     * @param songCode
     * @param completion
     * @receiver
     */
    fun makeSongDidPlay(songCode: String, completion: (error: Exception?) -> Unit)

    /**
     * Join chorus
     *
     * @param songCode
     * @param completion
     * @receiver
     */
    fun joinChorus(songCode: String, completion: (error: Exception?) -> Unit)

    /**
     * Leave chorus
     *
     * @param completion
     * @receiver
     */
    fun leaveChorus(completion: (error: Exception?) -> Unit)

    /**
     * Get chorister list
     *
     * @param completion
     * @receiver
     */
    fun getChoristerList(completion: (error: Exception?, choristerInfoList: List<RoomChoristerInfo>) -> Unit)

    /**
     * Subscribe listener
     *
     * @param listener
     */
    fun subscribeListener(listener: KtvServiceListenerProtocol)
}