package io.agora.scene.ktv.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.agora.rtmsyncmanager.ISceneResponse
import io.agora.rtmsyncmanager.RoomExpirationPolicy
import io.agora.rtmsyncmanager.RoomService
import io.agora.rtmsyncmanager.Scene
import io.agora.rtmsyncmanager.SyncManager
import io.agora.rtmsyncmanager.model.AUICommonConfig
import io.agora.rtmsyncmanager.model.AUIRoomConfig
import io.agora.rtmsyncmanager.model.AUIRoomContext
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserInfo
import io.agora.rtmsyncmanager.model.AUIUserThumbnailInfo
import io.agora.rtmsyncmanager.service.IAUIUserService
import io.agora.rtmsyncmanager.service.collection.AUIAttributesModel
import io.agora.rtmsyncmanager.service.collection.AUIListCollection
import io.agora.rtmsyncmanager.service.collection.AUIMapCollection
import io.agora.rtmsyncmanager.service.http.HttpManager
import io.agora.rtmsyncmanager.service.room.AUIRoomManager
import io.agora.rtmsyncmanager.service.rtm.AUIRtmException
import io.agora.rtmsyncmanager.utils.AUILogger
import io.agora.rtmsyncmanager.utils.GsonTools
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.ServerConfig
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.bean.User
import io.agora.scene.base.manager.UserManager
import io.agora.scene.ktv.KTVLogger
import io.agora.scene.ktv.widget.song.SongItem
import kotlin.random.Random

/**
 * Ktv sync manager service imp
 *
 * @property mContext
 * @property mErrorHandler
 * @constructor Create empty Ktv sync manager service imp
 */
class KTVSyncManagerServiceImp constructor(
    private val mContext: Context, private val mErrorHandler: ((Exception?) -> Unit)?
) : KTVServiceProtocol, ISceneResponse, IAUIUserService.AUIUserRespObserver {
    private val TAG = "KTV_Service_LOG"
    private val kSceneId = "scene_ktv_4.3.0"
    private val kCollectionIdChooseSong = "choose_song"
    private val kCollectionIdSeatInfo = "seat_info"
    private val kCollectionIdChoristerInfo = "chorister_info"

    private val mMainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val mSyncManager: SyncManager

    private val mRoomManager = AUIRoomManager()
    private val mRoomService: RoomService

    private val mAppId: String get() = BuildConfig.AGORA_APP_ID

    private var mDiffTs: Long = 0

    private var mLastCreateTime: Long = -1

    private val mCurrentServerTs: Long get() = SystemClock.elapsedRealtime() + mDiffTs

    private val mCurRoomConfig: AUIRoomConfig = AUIRoomConfig("")

    @Volatile
    private var mCurRoomNo: String = ""

    private val mUser: User get() = UserManager.getInstance().user

    private val mUserList = mutableListOf<AUIUserInfo>()
    private val mSeatMap = mutableMapOf<String, RoomMicSeatInfo>()
    private val mSongChosenList = mutableListOf<RoomSongInfo>()

    private var mServiceLister: KtvServiceListenerProtocol? = null

    init {
        HttpManager.setBaseURL(ServerConfig.roomManagerUrl)
        AUILogger.initLogger(AUILogger.Config(mContext, "KTV"))

        val commonConfig = AUICommonConfig().apply {
            context = mContext
            appId = mAppId
            owner = AUIUserThumbnailInfo().apply {
                userId = mUser.id.toString()
                userName = mUser.name
                userAvatar = mUser.fullHeadUrl
            }
            host = ServerConfig.toolBoxUrl
        }
        mSyncManager = SyncManager(mContext, null, commonConfig)

        val roomExpirationPolicy = RoomExpirationPolicy()
        roomExpirationPolicy.expirationTime = 20 * 60 * 1000
        roomExpirationPolicy.isAssociatedWithOwnerOffline = true
        mRoomService = RoomService(roomExpirationPolicy, mRoomManager, mSyncManager)
    }

    /**
     * Renew rtm token
     *
     * @param callback
     * @receiver
     */
    private fun renewRtmToken(callback: (rtmToken: String?, exception: Exception?) -> Unit) {
        TokenGenerator.generateToken("", // 万能 token
            mUser.id.toString(),
            TokenGenerator.TokenGeneratorType.Token006,
            TokenGenerator.AgoraTokenType.Rtm,
            success = { ret ->
                mCurRoomConfig.rtmToken = ret
                KTVLogger.d(TAG, "renewRtmTokens success")
                callback.invoke(ret, null)
            },
            failure = {
                KTVLogger.e(TAG, it, "renewRtmToken failed,$it")
                callback.invoke(null, it)
            })
    }

    /**
     * Reset
     *
     */
    override fun reset() {
        mSeatMap.clear()
        mSongChosenList.clear()
        mCurRoomNo = ""
    }

    private val mCurrentScene: Scene
        get() = mSyncManager.createScene(mCurRoomNo)

    /**
     * Get map collection
     *
     * @param kCollectionId
     * @return
     */
    private fun getMapCollection(kCollectionId: String): AUIMapCollection {
        return mCurrentScene.getCollection(kCollectionId) { a, b, c ->
            AUIMapCollection(a, b, c)
        }
    }

    /**
     * Get list collection
     *
     * @param kCollectionId
     * @return
     */
    private fun getListCollection(kCollectionId: String): AUIListCollection {
        return mCurrentScene.getCollection(kCollectionId) { a, b, c ->
            AUIListCollection(a, b, c)
        }
    }

    // ========= 房间相关 =====================
    /**
     * Get room list
     *
     * @param completion
     * @receiver
     */
    override fun getRoomList(completion: (list: List<AUIRoomInfo>) -> Unit) {
        KTVLogger.d(TAG, "getRoomList start")
        mRoomService.getRoomList(mAppId, kSceneId, mLastCreateTime, 20) { error, ts, roomList ->
            if (error == null && roomList != null) {
                ts?.let { serverTs ->
                    mDiffTs = SystemClock.elapsedRealtime() - serverTs
                }
                roomList.sortedBy { it.createTime }
                KTVLogger.d(TAG, "getRoomList end,roomCount:${roomList.size}")
                runOnMainThread { completion.invoke(roomList) }
            } else {
                KTVLogger.e(TAG, error, "getRoomList error")
                runOnMainThread { completion.invoke(emptyList()) }
            }
        }
    }

    /**
     * Create room
     *
     * @param createRoomInfo
     * @param completion
     * @receiver
     */
    override fun createRoom(
        createRoomInfo: CreateRoomInfo, completion: (error: Exception?, out: AUIRoomInfo?) -> Unit
    ) {
        KTVLogger.d(TAG, "createRoom start")
        initRtmSync {
            if (it != null) {
                completion.invoke(Exception(it.message), null)
                return@initRtmSync
            }
            val roomId = (Random(mCurrentServerTs).nextInt(100000) + 1000000).toString()
            val roomInfo = AUIRoomInfo().apply {
                this.roomId = roomId
                this.roomName = createRoomInfo.name
                this.roomOwner = AUIUserThumbnailInfo().apply {
                    userId = mUser.id.toString()
                    userName = mUser.name
                    userAvatar = mUser.fullHeadUrl
                }
                this.createTime = mCurrentServerTs
                this.customPayload[KTVParameters.ROOM_USER_COUNT] = 1
                this.customPayload[KTVParameters.THUMBNAIL_ID] = createRoomInfo.icon
                this.customPayload[KTVParameters.PASSWORD] = createRoomInfo.password
            }
            mCurRoomNo = ""
            mRoomService.createRoom(mAppId, kSceneId, roomInfo, completion = { auiRtmException, auiRoomInfo ->
                if (auiRtmException == null && auiRoomInfo != null) {
                    KTVLogger.d(TAG, "createRoom successs: $auiRoomInfo")
                    runOnMainThread {
                        completion.invoke(null, roomInfo)
                    }
                } else {
                    KTVLogger.e(TAG, "createRoom failed: $auiRtmException")
                    runOnMainThread {
                        completion.invoke(Exception(auiRtmException?.message), null)
                    }
                }
            })
        }
    }

    /**
     * Join room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    override fun joinRoom(
        roomId: String, password: String?, completion: (error: Exception?, out: JoinRoomInfo?) -> Unit
    ) {
        if (mCurRoomNo.isNotEmpty()) {
            completion.invoke(RuntimeException("The room $mCurRoomNo has been joined!"), null)
            return
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId)
        if (cacheRoom == null) {
            completion.invoke(RuntimeException("The room is not available!"), null)
            return
        }
        val password = cacheRoom.customPayload[KTVParameters.PASSWORD] as? String
        if (!password.isNullOrEmpty() && password != roomId) {
            completion.invoke(RuntimeException("The password is error!"), null)
            return
        }
        initRtmSync {
            if (it != null) {
                completion.invoke(Exception(it.message), null)
                return@initRtmSync
            }
            mRoomService.enterRoom(mAppId, kSceneId, cacheRoom, completion = { auiRtmException, roomInfo ->
                if (auiRtmException == null) {
                    mCurRoomNo = roomId
                    mCurRoomConfig.channelName = mCurRoomNo
                    mCurRoomConfig.rtcChorusChannelName = AUIRoomConfig(mCurRoomNo).rtcChorusChannelName
                    KTVLogger.d(TAG, "enterRoom success: $mCurRoomNo")
                    val scene = mCurrentScene
                    scene.bindRespDelegate(this)
                    scene.userService.registerRespObserver(this)
                    getListCollection(kCollectionIdChooseSong).subscribeAttributesDidChanged(this::onAttributeChanged)
                    getListCollection(kCollectionIdSeatInfo).subscribeAttributesDidChanged(this::onAttributeChanged)
                    getListCollection(kCollectionIdChoristerInfo).subscribeAttributesDidChanged(this::onAttributeChanged)

                    // TODO:  是否进入房间在获取麦位，以及上麦
                    innerAutoOnSeatIfNeed { error, seats ->
                        if (error != null) {
                            completion.invoke(error, null)
                            mCurRoomNo = ""
                            return@innerAutoOnSeatIfNeed
                        }
                        TokenGenerator.generateToken(mCurRoomConfig.channelName, mUser.id.toString(),
                            TokenGenerator.TokenGeneratorType.Token006, TokenGenerator.AgoraTokenType.Rtc,
                            success = { rtcToken ->
                                mCurRoomConfig.rtcToken = rtcToken
                                TokenGenerator.generateToken(mCurRoomConfig.rtcChorusChannelName, mUser.id.toString(),
                                    TokenGenerator.TokenGeneratorType.Token006, TokenGenerator.AgoraTokenType.Rtc,
                                    success = { chorusToken ->
                                        mCurRoomConfig.rtcChorusRtcToken = chorusToken
                                        val joinRoomInfo = JoinRoomInfo(roomConfig = mCurRoomConfig)
                                        joinRoomInfo.roomOwner = cacheRoom.roomOwner
                                        joinRoomInfo.roomName = cacheRoom.roomName
                                        joinRoomInfo.roomId = cacheRoom.roomId
                                        joinRoomInfo.customPayload = cacheRoom.customPayload

                                        completion.invoke(null, joinRoomInfo)
                                    },
                                    failure = {
                                        mCurRoomNo = ""
                                        KTVLogger.e(TAG, "enterRoom failed,generate chorusToken: $it")
                                        completion.invoke(it, null)
                                    })
                            },
                            failure = {
                                mCurRoomNo = ""
                                KTVLogger.e(TAG, "enterRoom failed,generate rtcToken: $it")
                                completion.invoke(it, null)
                            })
                    }
                } else {
                    KTVLogger.e(TAG, "enterRoom failed: $auiRtmException")
                    runOnMainThread {
                        completion.invoke(Exception(auiRtmException.message), null)
                    }
                }
            })
        }
    }

    /**
     * Leave room
     *
     * @param completion
     * @receiver
     */
    override fun leaveRoom(completion: (error: Exception?) -> Unit) {
        val scene = mCurrentScene
        scene.unbindRespDelegate(this)
        scene.userService.unRegisterRespObserver(this)
        getListCollection(kCollectionIdChooseSong).subscribeAttributesDidChanged(null)
        getListCollection(kCollectionIdSeatInfo).subscribeAttributesDidChanged(null)
        mRoomService.leaveRoom(mAppId, kSceneId, mCurRoomNo)
        resetCacheInfo(AUIRoomContext.shared().isRoomOwner(mCurRoomNo))
        completion.invoke(null)
    }

    /**
     * Update room
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    override fun updateRoom(inputModel: AUIRoomInfo, completion: (error: Exception?) -> Unit) {
        mRoomManager.updateRoomInfo(mAppId, kSceneId, inputModel, callback = { error, roomInfo ->
            if (error == null && roomInfo != null) {
                KTVLogger.d(TAG, "updateRoom success: $mCurRoomNo, $roomInfo")
            } else {
                KTVLogger.d(TAG, "updateRoom failed: $mCurRoomNo $error")
            }
        })
    }

    /**
     * Get all user list
     *
     * @param completion
     * @receiver
     */
    override fun getAllUserList(completion: (error: Exception?, list: List<AUIUserInfo>?) -> Unit) {
        mCurrentScene.userService.getUserInfoList(mCurRoomNo, callback = { error, userList ->
            if (error == null && userList != null) {
                KTVLogger.d(TAG, "getAllUserList success: $mCurRoomNo, ${userList.size}")
                mUserList.clear()
                mUserList.addAll(userList)
                mServiceLister?.onUserListDidChanged(userList)
                completion.invoke(error, userList)
            } else {
                KTVLogger.d(TAG, "getAllUserList failed: $mCurRoomNo $error")
            }
        })
    }

    /**
     * Reset cache info
     *
     * @param isRoomDestroyed
     */
    private fun resetCacheInfo(isRoomDestroyed: Boolean = false) {
        if (!isRoomDestroyed) {
            // 如果上麦了要下麦，并清空麦位信息
            mSeatMap.values.forEach { seat ->
                if (seat.user?.userId == mUser.id.toString()) {
                    innerRemoveSeat(seat, completion = {})
                    return@forEach
                }
            }
            // 删除点歌信息
            mSongChosenList.forEachIndexed { index: Int, songModel: RoomSongInfo ->
                if (songModel.userNo == mUser.id.toString()) {
                    innerRemoveChooseSong(mSongChosenList.removeAt(index)) {}
                }
            }
        }
        mSeatMap.clear()
        mSongChosenList.clear()
        mCurRoomNo = ""
    }


    // =================== 麦位相关 ===============================

    /**
     * Get seat status list
     *
     * @param completion
     * @receiver
     */
    override fun getAllSeatList(completion: (error: Exception?, list: List<RoomMicSeatInfo>?) -> Unit) {
        innerGetSeatInfo(completion)
    }

    /**
     * On seat
     *
     * @param seatIndex
     * @param completion
     * @receiver
     */
    override fun onSeat(seatIndex: Int, completion: (error: Exception?) -> Unit) {
        mSeatMap.values.forEach { seat ->
            if (seat.user?.userId == mUser.id.toString()) {
                return
            }
        }
        val targetSeatInfo = RoomMicSeatInfo(
            user = AUIRoomContext.shared().currentUserInfo,
            seatIndex = seatIndex,
            seatStatus = RoomMicSeatStatus.used
        )
        innerAddSeatInfo(targetSeatInfo, completion)
    }

    /**
     * Auto on seat
     *
     * @param completion
     * @receiver
     */
    override fun autoOnSeat(completion: (error: Exception?) -> Unit) {
        val list = mutableListOf(0, 1, 2, 3, 4, 5, 6, 7)
        mSeatMap.values.forEach { seat ->
            list.removeIf { it == seat.seatIndex }
            if (seat.user?.userId == mUser.id.toString()) {
                completion.invoke(null)
                return
            }
        }
        if (list.isEmpty()) {
            completion.invoke(Exception("麦位已满，请在他人下麦后重试"))
        } else {
            val targetSeatInfo = RoomMicSeatInfo(
                user = AUIRoomContext.shared().currentUserInfo,
                seatIndex = list[0],
                seatStatus = RoomMicSeatStatus.used
            )
            innerAddSeatInfo(targetSeatInfo, completion)
        }
    }

    /**
     * Out seat
     *
     * @param seatIndex
     * @param completion
     * @receiver
     */
    override fun outSeat(seatIndex: Int, completion: (error: Exception?) -> Unit) {
        mSeatMap[seatIndex.toString()]?.let { roomSeat ->
            // 移除歌曲
            roomSeat.user?.userId?.let { userId ->
                innerRemoveAllUsersChooseSong(userId)
            }
            // 移除座位
            innerRemoveSeat(roomSeat) {}
        }
        completion(null)
    }

    /**
     * Mute user audio
     *
     * @param mute
     * @param completion
     * @receiver
     */
    override fun muteUserAudio(mute: Boolean, completion: (error: Exception?) -> Unit) {
        mUserList.forEach { useInfo ->
            if (useInfo.userId == mUser.id.toString()) {
                mCurrentScene.userService.muteUserAudio(mute, callback = { auiException ->
                    auiException?.let {
                        completion.invoke(Exception("${it.message}(${it.code})"))
                    } ?: run {
                        completion.invoke(null)
                    }
                })
                return@forEach
            }
        }
    }

    /**
     * Mute user video
     *
     * @param mute
     * @param completion
     * @receiver
     */
    override fun muteUserVideo(mute: Boolean, completion: (error: Exception?) -> Unit) {
        mUserList.forEach { useInfo ->
            if (useInfo.userId == mUser.id.toString()) {
                mCurrentScene.userService.muteUserVideo(mute, callback = { auiException ->
                    auiException?.let {
                        completion.invoke(Exception("${it.message}(${it.code})"))
                    } ?: run {
                        completion.invoke(null)
                    }
                })
                return@forEach
            }
        }
    }

    // ---------------------Inner Seat operation --------------------------

    /**
     * Inner auto on seat if need
     *
     * @param completion
     * @receiver
     */
    private fun innerAutoOnSeatIfNeed(completion: (error: Exception?, seat: List<RoomMicSeatInfo>?) -> Unit) {
        innerGetSeatInfo { error, list ->
            if (error != null) {
                completion.invoke(error, null)
                return@innerGetSeatInfo
            }
            var hasMaster = false
            val outList = mutableListOf<RoomMicSeatInfo>()
            mSeatMap.values.forEach { seat ->
                outList.add(seat)
                runOnMainThread {
                    mServiceLister?.onAddOrUpdateSeat(seat)
                }
                if (AUIRoomContext.shared().isRoomOwner(mCurRoomNo, seat.user?.userId)) {
                    hasMaster = true
                }
            }
            if (!hasMaster && AUIRoomContext.shared().isRoomOwner(mCurRoomNo)) {
                val targetSeatInfo = RoomMicSeatInfo(
                    user = AUIRoomContext.shared().currentUserInfo,
                    seatIndex = 0,
                )
                innerAddSeatInfo(targetSeatInfo) {
                    if (it != null) {
                        completion.invoke(it, null)
                        return@innerAddSeatInfo
                    }
                    outList.add(targetSeatInfo)
                    runOnMainThread {
                        mServiceLister?.onAddOrUpdateSeat(targetSeatInfo)
                        completion.invoke(null, outList)
                    }
                }
            } else {
                completion.invoke(null, outList)
            }
        }
    }

    /**
     * Inner get seat info
     *
     * @param completion
     * @receiver
     */
    private fun innerGetSeatInfo(completion: (error: Exception?, list: List<RoomMicSeatInfo>?) -> Unit) {
        getListCollection(kCollectionIdSeatInfo).getMetaData { error, value ->
            if (error != null) {
                KTVLogger.d(TAG, "innerGetSeatInfo failed roomId:$mCurRoomNo $error")
                runOnMainThread {
                    completion.invoke(Exception(error.message), null)
                }
                return@getMetaData
            }
            try {
                val out = GsonTools.toList(GsonTools.beanToString(value), RoomMicSeatInfo::class.java)
                KTVLogger.d(TAG, "innerGetSeatInfo onSuccess roomId:$mCurRoomNo $out")
                out?.let { romSeats ->
                    mSeatMap.clear()
                    romSeats.forEachIndexed { index, roomSeatModel ->
                        mSeatMap[roomSeatModel.seatIndex.toString()] = roomSeatModel
                    }
                }
                runOnMainThread {
                    completion.invoke(null, out)
                }
            } catch (e: Exception) {
                KTVLogger.d(TAG, "innerGetSeatInfo onFail roomId:$mCurRoomNo $e")
                runOnMainThread {
                    completion.invoke(e, null)
                }
            }
        }
    }

    /**
     * Inner add seat info
     *
     * @param seatInfo
     * @param completion
     * @receiver
     */
    private fun innerAddSeatInfo(seatInfo: RoomMicSeatInfo, completion: (error: Exception?) -> Unit) {
        getListCollection(kCollectionIdSeatInfo).addMetaData(null, GsonTools.beanToMap(seatInfo), null, callback = {
            if (it == null) {
                KTVLogger.d(TAG, "innerAddSeatInfo success roomId:$mCurRoomNo, seatInfo:$seatInfo")
            } else {
                KTVLogger.d(TAG, "innerAddSeatInfo failed roomId:$mCurRoomNo, $it")
            }
            runOnMainThread {
                completion.invoke(it)
            }
        })
    }

    /**
     * Inner update seat
     *
     * @param seatInfo
     * @param completion
     * @receiver
     */
    private fun innerUpdateSeat(seatInfo: RoomMicSeatInfo, completion: (error: Exception?) -> Unit) {
        getListCollection(kCollectionIdSeatInfo).updateMetaData(null, GsonTools.beanToMap(seatInfo), null, callback = {
            if (it == null) {
                KTVLogger.d(TAG, "innerUpdateSeat success roomId:$mCurRoomNo, seatInfo:$seatInfo")
            } else {
                KTVLogger.d(TAG, "innerUpdateSeat failed roomId:$mCurRoomNo, $it")
            }
            runOnMainThread {
                completion.invoke(it)
            }
        })
    }

    /**
     * Inner remove seat
     *
     * @param seatInfo
     * @param completion
     * @receiver
     */
    private fun innerRemoveSeat(seatInfo: RoomMicSeatInfo, completion: (error: Exception?) -> Unit) {
        getListCollection(kCollectionIdSeatInfo).removeMetaData(null, filter = mutableListOf(
            mapOf(Pair("seatIndex", seatInfo.seatIndex))
        ), callback = {
            if (it == null) {
                KTVLogger.d(TAG, "innerRemoveSeat success roomId:$mCurRoomNo, seatInfo:$seatInfo")
            } else {
                KTVLogger.d(TAG, "innerRemoveSeat failed roomId:$mCurRoomNo, $it")
            }
            runOnMainThread {
                completion.invoke(it)
            }
        })
    }

    // ============= song start =============================
    /**
     * Get all song list
     *
     * @param completion
     * @receiver
     */
    override fun getAllSongList(completion: (error: Exception?, list: List<SongItem>) -> Unit) {

    }

    /**
     * Get choosed songs list
     *
     * @param completion
     * @receiver
     */
    override fun getChosenSongList(completion: (error: Exception?, list: List<RoomSongInfo>?) -> Unit) {
        innerGetChooseSongInfo(completion)
    }

    override fun chooseSong(songInfo: RoomSongInfo, completion: (error: Exception?) -> Unit) {
        val song = RoomSongInfo(
            songInfo.songName,
            songInfo.songNo,
            songInfo.singer,
            songInfo.imageUrl,
            userNo = mUser.id.toString(),
            name = mUser.name,
            status = RoomSongInfo.STATUS_IDLE,
            createAt = mCurrentServerTs, pinAt = 0.0
        )
        innerAddChooseSongInfo(song, completion)
    }

    override fun makeSongTop(songCode: String, completion: (error: Exception?) -> Unit) {
        // move the song to second position
        if (mSongChosenList.size <= 0) {
            completion.invoke(RuntimeException("The chosen song list is empty!"))
            return
        }
        if (mSongChosenList.size < 3) {
            completion.invoke(RuntimeException("The chosen songs size is less then three, it is unnecessary to top up!"))
            return
        }

        val filter = mSongChosenList.filter { it.songNo == songCode }
        val targetSong = filter.getOrNull(0)
        if (targetSong == null) {
            completion.invoke(RuntimeException("The song no not found!"))
            return
        }

        //net request and notify others
        val indexOf = mSongChosenList.indexOf(targetSong)
        val newSong = RoomSongInfo(
            targetSong.songName,
            targetSong.songNo,
            targetSong.singer,
            targetSong.imageUrl,
            userNo = targetSong.userNo,
            name = targetSong.name,
            isOriginal = targetSong.isOriginal,
            status = targetSong.status,
            createAt = targetSong.createAt,
            pinAt = mCurrentServerTs.toDouble()
        )
        mSongChosenList[indexOf] = newSong
        innerPinSong(newSong) {
            completion.invoke(it)
        }
    }

    override fun makeSongDidPlay(songCode: String, completion: (error: Exception?) -> Unit) {
        if (mSongChosenList.size <= 0) {
            completion.invoke(RuntimeException("The chosen song list is empty!"))
            return
        }

        val filter = mSongChosenList.filter { it.songNo == songCode }
        val targetSong = filter.getOrNull(0)
        if (targetSong == null) {
            completion.invoke(RuntimeException("The song no not found!"))
            return
        }
        if (targetSong.status == RoomSongInfo.STATUS_PLAYING) {
            completion.invoke(null)
            return
        }

        val indexOf = mSongChosenList.indexOf(targetSong)
        val newSong = RoomSongInfo(
            targetSong.songName,
            targetSong.songNo,
            targetSong.singer,
            targetSong.imageUrl,
            userNo = targetSong.userNo,
            name = targetSong.name,
            isOriginal = targetSong.isOriginal,
            status = RoomSongInfo.STATUS_PLAYING,
            createAt = targetSong.createAt,
            pinAt = targetSong.pinAt
        )
        mSongChosenList[indexOf] = newSong
        innerDidPlaySong(newSong) {
            completion.invoke(it)
        }
    }

    override fun removeSong(
        isSingingSong: Boolean, inputModel: RoomSongInfo, completion: (error: Exception?) -> Unit
    ) {
        if (mSongChosenList.size <= 0) {
            completion.invoke(RuntimeException("The chosen song list is empty!"))
            return
        }
        val targetSong = mSongChosenList.filter {
            it.songNo == inputModel.songNo && it.userNo == mUser.id.toString()
        }.getOrNull(0)
        if (targetSong == null) {
            completion.invoke(RuntimeException("The song no not found!"))
            return
        }

        val indexOf = mSongChosenList.indexOf(targetSong)
        mSongChosenList.removeAt(indexOf)
        innerRemoveChooseSong(targetSong) {
            completion.invoke(it)
        }
    }

    // ------------ inner Choose song operation ---------------------

    /**
     * Inner get choose song info
     *
     * @param completion
     * @receiver
     */
    private fun innerGetChooseSongInfo(completion: (error: Exception?, list: List<RoomSongInfo>?) -> Unit) {
        getListCollection(kCollectionIdChooseSong).getMetaData { error, value ->
            KTVLogger.d(TAG, "innerGetChooseSongInfo roomId:$mCurRoomNo $error $value")
            if (error != null) {
                runOnMainThread {
                    completion.invoke(Exception(error.message), null)
                }
                return@getMetaData
            }
            try {
                val out = GsonTools.toList(GsonTools.beanToString(value), RoomSongInfo::class.java)
                KTVLogger.d(TAG, "innerGetChooseSongInfo onSuccess roomId:$mCurRoomNo $out")
                runOnMainThread {
                    completion.invoke(null, out)
                }
            } catch (e: Exception) {
                KTVLogger.d(TAG, "innerGetChooseSongInfo onFail roomId:$mCurRoomNo $e")
                runOnMainThread {
                    completion.invoke(e, null)
                }
            }
        }
    }

    /**
     * Inner remove all users choose song
     *
     * @param userId
     */
    private fun innerRemoveAllUsersChooseSong(userId: String) {
        mSongChosenList.filter { it.userNo == userId }.forEach {
            val indexOf = mSongChosenList.indexOf(it)
            innerRemoveChooseSong(mSongChosenList.removeAt(indexOf)) {}
        }
    }

    /**
     * Inner add choose song info
     *
     * @param songInfo
     * @param completion
     * @receiver
     */
    private fun innerAddChooseSongInfo(songInfo: RoomSongInfo, completion: (error: Exception?) -> Unit) {
        getListCollection(kCollectionIdChooseSong).addMetaData(null,
            GsonTools.beanToMap(songInfo),
            null,
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "innerAddChooseSongInfo success roomId:$mCurRoomNo, song:$songInfo")
                } else {
                    KTVLogger.d(TAG, "innerAddChooseSongInfo failed roomId:$mCurRoomNo, $it")
                }
                runOnMainThread {
                    completion.invoke(it)
                }
            })
    }

    /**
     * Inner remove choose song
     *
     * @param songModel
     * @param completion
     * @receiver
     */
    private fun innerRemoveChooseSong(songModel: RoomSongInfo, completion: (error: Exception?) -> Unit) {
        getListCollection(kCollectionIdChooseSong).removeMetaData(null, filter = mutableListOf(
            mapOf(Pair("songCode", songModel.songNo))
        ), callback = {
            if (it == null) {
                KTVLogger.d(TAG, "innerRemoveChooseSong success roomId:$mCurRoomNo, song:$songModel")
            } else {
                KTVLogger.d(TAG, "innerRemoveChooseSong failed roomId:$mCurRoomNo, $it")
            }
            runOnMainThread {
                completion.invoke(it)
            }
        })
    }

    /**
     * Inner pin song
     *
     * @param songModel
     * @param completion
     * @receiver
     */
    private fun innerPinSong(songModel: RoomSongInfo, completion: (error: Exception?) -> Unit) {
        getListCollection(kCollectionIdChooseSong).mergeMetaData(null,
            mapOf("pinAt" to mCurrentServerTs),
            filter = mutableListOf(
                mapOf(
                    Pair("songNo", songModel.songNo), Pair("userNo", songModel.userNo ?: "")
                )
            ),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "innerRemoveChooseSong success roomId:$mCurRoomNo, song:$songModel")
                } else {
                    KTVLogger.d(TAG, "innerRemoveChooseSong failed roomId:$mCurRoomNo, $it")
                }
                runOnMainThread {
                    completion.invoke(it)
                }
            })
    }

    /**
     * Inner did play song
     *
     * @param song
     * @param completion
     * @receiver
     */
    private fun innerDidPlaySong(song: RoomSongInfo, completion: (error: Exception?) -> Unit) {
        getListCollection(kCollectionIdChooseSong).mergeMetaData(null,
            mapOf("status" to song.status),
            filter = mutableListOf(
                mapOf(
                    Pair("songNo", song.songNo), Pair("userNo", song.userNo ?: "")
                )
            ),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "innerRemoveChooseSong success roomId:$mCurRoomNo, song:$song")
                } else {
                    KTVLogger.d(TAG, "innerRemoveChooseSong failed roomId:$mCurRoomNo, $it")
                }
                runOnMainThread {
                    completion.invoke(it)
                }
            })
    }


    // =============  Chorus =============================
    /**
     * Join chorus
     *
     * @param songCode
     * @param completion
     * @receiver
     */
    override fun joinChorus(songCode: String, completion: (error: Exception?) -> Unit) {
        innerGetSeatInfo { error, list ->
            if (error == null && list != null) {
                list.forEach { seat ->
                    if (seat.user?.userId == mUser.id.toString()) {
                        // TODO: 合唱 collection
                    }
                }
            }
        }
    }

    /**
     * Leave chorus
     *
     * @param completion
     * @receiver
     */
    override fun leaveChorus(completion: (error: Exception?) -> Unit) {
        mSeatMap.values.forEach { seat ->
            if (seat.user?.userId == mUser.id.toString()) {
                // TODO: 合唱 collection
            }
        }
    }

    /**
     * Run on main thread
     *
     * @param r
     */
    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mMainHandler.looper.thread) {
            r.run()
        } else {
            mMainHandler.post(r)
        }
    }

    /**
     * Init rtm sync
     *
     * @param completion
     * @receiver
     */
    private fun initRtmSync(completion: (exception: AUIRtmException?) -> Unit) {
        if (mSyncManager.rtmManager.isLogin) {
            completion.invoke(null)
            return
        }
        if (mCurRoomConfig.rtmToken.isEmpty()) {
            KTVLogger.d(TAG, "initRtmSync, renewToken start")
            renewRtmToken { rtmToken, exception ->
                rtmToken ?: return@renewRtmToken
                mSyncManager.login(rtmToken, completion = {
                    if (it == null) {
                        completion.invoke(null)
                    } else {
                        completion.invoke(it)
                        KTVLogger.e(TAG, "initRtmSync, with renewToken loginRtm failed: $it")
                    }
                })
            }
        } else {
            mSyncManager.login(mCurRoomConfig.rtmToken, completion = {
                if (it == null) {
                    completion.invoke(null)
                } else {
                    completion.invoke(it)
                    KTVLogger.e(TAG, "initRtmSync, without renewToken loginRtm failed: $it")
                }
            })
        }
    }

    override fun getChoristerList(completion: (error: Exception?, choristerInfoList: List<RoomChoristerInfo>) -> Unit) {
        // TODO:  
    }

    /**
     * Subscribe listener
     *
     * @param listener
     */
    override fun subscribeListener(listener: KtvServiceListenerProtocol) {
        this.mServiceLister = listener
    }

// =======================   ISceneResponse Start =======================
    /**
     * On token privilege will expire
     *
     * @param channelName
     */
    override fun onTokenPrivilegeWillExpire(channelName: String?) {
        KTVLogger.d(TAG, "onTokenPrivilegeWillExpire, $channelName")
        renewRtmToken { rtmToken, exception ->
            rtmToken ?: return@renewRtmToken
            mSyncManager.login(rtmToken, completion = {
                if (it == null) {
                    KTVLogger.d(TAG, "onTokenPrivilegeWillExpire, with renewToken loginRtm success")
                } else {
                    KTVLogger.e(TAG, "onTokenPrivilegeWillExpire, with renewToken loginRtm failed: $it")
                }
            })
        }
    }

    /**
     * On scene expire
     *
     * @param channelName
     */
    override fun onSceneExpire(channelName: String) {
        KTVLogger.d(TAG, "onSceneExpire, channelName:$channelName")
        if (mCurRoomNo == channelName) {
            mServiceLister?.onRoomExpire(channelName)
            mCurRoomNo = ""
        }
    }

    /**
     * On scene destroy
     *
     * @param channelName
     */
    override fun onSceneDestroy(channelName: String) {
        KTVLogger.d(TAG, "onSceneExpire, channelName:$channelName")
        if (mCurRoomNo == channelName) {
            mServiceLister?.onRoomDestroy(channelName)
            mCurRoomNo = ""
        }
    }

    /**
     * On scene user be kicked
     *
     * @param channelName
     * @param userId
     */
    override fun onSceneUserBeKicked(channelName: String, userId: String) {
        KTVLogger.d(TAG, "onSceneUserBeKicked, channelName:$channelName, userId:$userId")
    }


// ======================= AUIUserRespObserver Start =======================
    /**
     * On room user snapshot
     *
     * @param roomId
     * @param userList
     *
     */
    override fun onRoomUserSnapshot(roomId: String, userList: MutableList<AUIUserInfo>?) {
        KTVLogger.d(TAG, "onRoomUserSnapshot, roomId:$roomId, userList:${userList?.count()}")
        if (mCurRoomNo == roomId) {
            userList?.let {
                this.mUserList.clear()
                this.mUserList.addAll(it)
            }
            mServiceLister?.onUserListDidChanged(userList ?: emptyList())
        }
    }

    /**
     * On room user enter
     *
     * @param roomId
     * @param userInfo
     */
    override fun onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
        KTVLogger.d(TAG, "onRoomUserEnter, roomId:$roomId, userInfo:$userInfo")
        mUserList.removeIf { it.userId == userInfo.userId }
        mUserList.add(userInfo)
        mServiceLister?.onUserListDidChanged(mUserList)
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId) ?: return
        if (mUser.id.toString() == cacheRoom.roomOwner?.userId) {
            cacheRoom.customPayload[KTVParameters.ROOM_USER_COUNT] = mUserList.count()
            updateRoom(cacheRoom, completion = {})
        }
    }

    /**
     * On room user leave
     *
     * @param roomId
     * @param userInfo
     */
    override fun onRoomUserLeave(roomId: String, userInfo: AUIUserInfo) {
        KTVLogger.d(TAG, "onRoomUserLeave, roomId:$roomId, userInfo:$userInfo")
        mUserList.removeIf { it.userId == userInfo.userId }
        mServiceLister?.onUserListDidChanged(mUserList)
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId) ?: return
        if (mUser.id.toString() == cacheRoom.roomOwner?.userId) {
            cacheRoom.customPayload[KTVParameters.ROOM_USER_COUNT] = mUserList.count()
            updateRoom(cacheRoom, completion = {})
        }
    }

    /**
     * On room user update
     *
     * @param roomId
     * @param userInfo
     */
    override fun onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
        KTVLogger.d(TAG, "onRoomUserUpdate, roomId:$roomId, userInfo:$userInfo")
        mUserList.removeIf { it.userId == userInfo.userId }
        mUserList.add(userInfo)
        mServiceLister?.onUserListDidChanged(mUserList)
    }

    /**
     * On user audio mute
     *
     * @param userId
     * @param mute
     */
    override fun onUserAudioMute(userId: String, mute: Boolean) {
        KTVLogger.d(TAG, "onUserAudioMute, userId:$userId, mute:$mute")
    }

    /**
     * On user video mute
     *
     * @param userId
     * @param mute
     */
    override fun onUserVideoMute(userId: String, mute: Boolean) {
        KTVLogger.d(TAG, "onUserVideoMute, userId:$userId, mute:$mute")
    }
    // =======================  AUIUserRespObserver End =======================


    /**
     * On attribute changed
     *
     * @param channelName
     * @param key
     * @param value
     */
    private fun onAttributeChanged(channelName: String, key: String, value: AUIAttributesModel) {
        KTVLogger.d(TAG, "onAttributeChanged, channelName:$channelName, key:$key, value:${value.getList()}")
        KTVLogger.d(TAG, "onAttributeChanged, channelName:$channelName, key:$key class: ${value.javaClass}")
        when (key) {
            kCollectionIdSeatInfo -> {
                val seats: List<RoomMicSeatInfo> =
                    GsonTools.toList(GsonTools.beanToString(value.getList()), RoomMicSeatInfo::class.java)
                        ?: mutableListOf()
                KTVLogger.d(TAG, "$kCollectionIdSeatInfo seats: $seats")
                // TODO: 移除麦位
                seats.forEach { roomSeat ->
                    mServiceLister?.onAddOrUpdateSeat(roomSeat)
                }
            }

            kCollectionIdChooseSong -> {
                val songs: List<RoomSongInfo> =
                    GsonTools.toList(GsonTools.beanToString(value.getList()), RoomSongInfo::class.java)
                        ?: mutableListOf()
                mServiceLister?.onChosenSongListDidChanged(songs)
                KTVLogger.d(TAG, "$kCollectionIdChooseSong songs: $songs")
            }

            kCollectionIdChoristerInfo -> {
                // TODO:
            }
        }
    }
}