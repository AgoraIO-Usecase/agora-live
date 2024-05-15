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
import kotlin.random.Random

/**
 * Ktv sync manager service imp
 *
 * @property mContext
 * @property mErrorHandler
 * @constructor Create empty K t v sync manager service imp
 */
class KTVSyncManagerServiceImp(
    private val mContext: Context, private val mErrorHandler: ((Exception?) -> Unit)?
) : KTVServiceProtocol, ISceneResponse, IAUIUserService.AUIUserRespObserver {
    private val TAG = "KTV_Service_LOG"
    private val kSceneId = "scene_ktv_4.3.0"
    private val kCollectionIdChooseSong = "choose_song"
    private val kCollectionIdSeatInfo = "seat_info"

    @Volatile
    private var syncUtilsInited = false

    private val mMainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val mSyncManager: SyncManager

    private val mRoomManager = AUIRoomManager()
    private val mRoomService: RoomService

    private val mAppId: String
        get() = BuildConfig.AGORA_APP_ID

    private var mDiffTs: Long = 0

    private var mLastCreateTime: Long = -1

    private val mCurrentServerTs: Long
        get() {
            return SystemClock.elapsedRealtime() + mDiffTs
        }

    private val mCurRoomConfig: AUIRoomConfig = AUIRoomConfig("")

    @Volatile
    private var mCurRoomNo: String = ""

    private val mUser: User
        get() = UserManager.getInstance().user

    private val mUserList = mutableListOf<AUIUserInfo>()
    private val mSeatMap = mutableMapOf<String, RoomSeatModel?>()
    private val mSongChosenList = mutableListOf<RoomSelSongModel>()

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
        if (syncUtilsInited) {
            syncUtilsInited = false
            mSeatMap.clear()
            mSongChosenList.clear()
            mCurRoomNo = ""
        }
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
     * @param inputModel
     * @param completion
     * @receiver
     */
    override fun createRoom(
        inputModel: CreateRoomInputModel, completion: (error: Exception?, out: AUIRoomInfo?) -> Unit
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
                this.roomName = inputModel.name
                this.roomOwner = AUIUserThumbnailInfo().apply {
                    userId = mUser.id.toString()
                    userName = mUser.name
                    userAvatar = mUser.fullHeadUrl
                }
                this.createTime = mCurrentServerTs
                this.customPayload[KTVParameters.ROOM_USER_COUNT] = 1
                this.customPayload[KTVParameters.THUMBNAIL_ID] = inputModel.icon
                this.customPayload[KTVParameters.PASSWORD] = inputModel.password
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
        inputModel: JoinRoomInputModel,
        completion: (error: Exception?, out: JoinRoomOutputModel?) -> Unit
    ) {
        if (mCurRoomNo.isNotEmpty()) {
            completion.invoke(RuntimeException("The room $mCurRoomNo has been joined!"), null)
            return
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(inputModel.roomNo)
        if (cacheRoom == null) {
            completion.invoke(RuntimeException("The room is not available!"), null)
            return
        }
        val password = cacheRoom.customPayload[KTVParameters.PASSWORD] as? String
        if (!password.isNullOrEmpty() && password != inputModel.password) {
            completion.invoke(RuntimeException("The password is error!"), null)
            return
        }
        initRtmSync {
            if (it != null) {
                completion.invoke(Exception(it.message), null)
                return@initRtmSync
            }
            mRoomService.enterRoom(mAppId, kSceneId, cacheRoom, completion = { auiRtmException, auiRoomInfo ->
                if (auiRtmException == null && auiRoomInfo != null) {
                    mCurRoomNo = auiRoomInfo.roomId
                    mCurRoomConfig.channelName = mCurRoomNo
                    mCurRoomConfig.rtcChorusChannelName = AUIRoomConfig(mCurRoomNo).rtcChorusChannelName
                    KTVLogger.d(TAG, "enterRoom success: $mCurRoomNo")
                    val scene = mCurrentScene
                    scene.bindRespDelegate(this)
                    scene.userService.registerRespObserver(this)
                    getListCollection(kCollectionIdChooseSong).subscribeAttributesDidChanged(this::onAttributeChanged)
                    getListCollection(kCollectionIdSeatInfo).subscribeAttributesDidChanged(this::onAttributeChanged)

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
                                        completion.invoke(null, JoinRoomOutputModel(cacheRoom, mCurRoomConfig))
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
                        completion.invoke(Exception(auiRtmException?.message), null)
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
            mSeatMap.forEach {
                it.value?.let { seat ->
                    if (seat.userNo == mUser.id.toString()) {
                        innerRemoveSeat(seat, completion = {})
                        return@forEach
                    }
                }
            }
            // 删除点歌信息
            mSongChosenList.forEachIndexed { index: Int, songModel: RoomSelSongModel ->
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
    override fun getSeatStatusList(completion: (error: Exception?, list: List<RoomSeatModel>?) -> Unit) {
        innerGetSeatInfo(completion)
    }

    /**
     * On seat
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    override fun onSeat(inputModel: OnSeatInputModel, completion: (error: Exception?) -> Unit) {
        mSeatMap.forEach {
            it.value?.let { seat ->
                if (seat.userNo == mUser.id.toString()) {
                    return
                }
            }
        }
        val seatInfo = innerGenUserSeatInfo(inputModel.seatIndex)
        innerAddSeatInfo(seatInfo, completion)
    }

    /**
     * Auto on seat
     *
     * @param completion
     * @receiver
     */
    override fun autoOnSeat(completion: (error: Exception?) -> Unit) {
        val list = mutableListOf(0, 1, 2, 3, 4, 5, 6, 7)
        mSeatMap.forEach {
            it.value?.let { seat ->
                list.removeIf { index ->
                    index == seat.seatIndex
                }
                if (seat.userNo == mUser.id.toString()) {
                    completion.invoke(null)
                    return
                }
            }
        }
        if (list.isEmpty()) {
            completion.invoke(Exception("麦位已满，请在他人下麦后重试"))
        } else {
            val seatInfo = innerGenUserSeatInfo(list[0])
            innerAddSeatInfo(seatInfo, completion)
        }
    }

    /**
     * Out seat
     *
     * @param inputModel
     * @param completion
     * @receiver
     */
    override fun outSeat(inputModel: OutSeatInputModel, completion: (error: Exception?) -> Unit) {
        val seatInfo = mSeatMap[inputModel.userOnSeat.toString()]
        if (seatInfo != null) {
            // 移除歌曲
            innerRemoveAllUsersChooseSong(seatInfo.userNo)
            // 移除座位
            innerRemoveSeat(seatInfo) {}
        }
        completion(null)
    }

    /**
     * Update seat audio mute status
     *
     * @param mute
     * @param completion
     * @receiver
     */
    override fun updateSeatAudioMuteStatus(mute: Boolean, completion: (error: Exception?) -> Unit) {
        mSeatMap.forEach {
            if (it.value?.userNo == mUser.id.toString()) {
                val originSeatInfo = it.value
                if (originSeatInfo != null) {
                    val seatInfo = RoomSeatModel(
                        originSeatInfo.isMaster,
                        originSeatInfo.headUrl,
                        originSeatInfo.userNo,
                        originSeatInfo.rtcUid,
                        originSeatInfo.name,
                        originSeatInfo.seatIndex,
                        originSeatInfo.chorusSongCode,
                        if (mute) RoomSeatModel.MUTED_VALUE_TRUE else RoomSeatModel.MUTED_VALUE_FALSE, // update this
                        originSeatInfo.isVideoMuted
                    )
                    innerUpdateSeat(seatInfo, completion)
                }
            }
        }
    }

    /**
     * Update seat video mute status
     *
     * @param mute
     * @param completion
     * @receiver
     */
    override fun updateSeatVideoMuteStatus(mute: Boolean, completion: (error: Exception?) -> Unit) {
        mSeatMap.forEach {
            if (it.value?.userNo == mUser.id.toString()) {
                val originSeatInfo = it.value
                if (originSeatInfo != null) {
                    val seatInfo = RoomSeatModel(
                        originSeatInfo.isMaster,
                        originSeatInfo.headUrl,
                        originSeatInfo.userNo,
                        originSeatInfo.rtcUid,
                        originSeatInfo.name,
                        originSeatInfo.seatIndex,
                        originSeatInfo.chorusSongCode,
                        originSeatInfo.isAudioMuted,
                        if (mute) 1 else 0// update this
                    )
                    innerUpdateSeat(seatInfo, completion)
                }
            }
        }
    }

    // ---------------------Inner Seat operation --------------------------

    /**
     * Inner gen user seat info
     *
     * @param seatIndex
     * @return
     */
    private fun innerGenUserSeatInfo(
        seatIndex: Int,
        isAudioMuted: Int = RoomSeatModel.MUTED_VALUE_TRUE,
        isVideoMuted: Int = RoomSeatModel.MUTED_VALUE_TRUE
    ): RoomSeatModel {
        return RoomSeatModel(
            AUIRoomContext.shared().isRoomOwner(mCurRoomNo),
            mUser.fullHeadUrl,
            mUser.id.toString(),
            mUser.id.toString(),
            mUser.name,
            seatIndex,
            "",
            isAudioMuted,
            isVideoMuted
        )
    }

    /**
     * Inner auto on seat if need
     *
     * @param completion
     * @receiver
     */
    private fun innerAutoOnSeatIfNeed(completion: (error: Exception?, seat: List<RoomSeatModel>?) -> Unit) {
        innerGetSeatInfo { error, list ->
            if (error != null) {
                completion.invoke(error, null)
                return@innerGetSeatInfo
            }
            var hasMaster = false
            val outList = mutableListOf<RoomSeatModel>()
            mSeatMap.forEach {
                it.value?.let { seat ->
                    outList.add(seat)
                    runOnMainThread {
                        mServiceLister?.onRoomSeatDidChanged(KTVServiceProtocol.KTVSubscribe.KTVSubscribeCreated, seat)
                    }
                    if (seat.isMaster) {
                        hasMaster = true
                    }
                }
            }
            if (!hasMaster && AUIRoomContext.shared().isRoomOwner(mCurRoomNo)) {
                val targetSeatInfo =
                    innerGenUserSeatInfo(0, RoomSeatModel.MUTED_VALUE_FALSE, RoomSeatModel.MUTED_VALUE_TRUE)
                innerAddSeatInfo(targetSeatInfo) { error ->
                    if (error != null) {
                        completion.invoke(error, null)
                        return@innerAddSeatInfo
                    }
                    outList.add(targetSeatInfo)
                    runOnMainThread {
                        mServiceLister?.onRoomSeatDidChanged(
                            KTVServiceProtocol.KTVSubscribe.KTVSubscribeCreated, targetSeatInfo
                        )
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
    private fun innerGetSeatInfo(completion: (error: Exception?, list: List<RoomSeatModel>?) -> Unit) {
        getListCollection(kCollectionIdSeatInfo).getMetaData { error, value ->
            if (error != null) {
                KTVLogger.d(TAG, "innerGetSeatInfo failed roomId:$mCurRoomNo $error")
                runOnMainThread {
                    completion.invoke(Exception(error.message), null)
                }
                return@getMetaData
            }
            try {
                val out = GsonTools.toList(GsonTools.beanToString(value), RoomSeatModel::class.java)
                KTVLogger.d(TAG, "innerGetSeatInfo onSuccess roomId:$mCurRoomNo $out")
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
    private fun innerAddSeatInfo(seatInfo: RoomSeatModel, completion: (error: Exception?) -> Unit) {
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
    private fun innerUpdateSeat(seatInfo: RoomSeatModel, completion: (error: Exception?) -> Unit) {
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
    private fun innerRemoveSeat(seatInfo: RoomSeatModel, completion: (error: Exception?) -> Unit) {
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
    override fun getChoosedSongsList(completion: (error: Exception?, list: List<RoomSelSongModel>?) -> Unit) {
        innerGetChooseSongInfo(completion)
    }

    override fun chooseSong(inputModel: ChooseSongInputModel, completion: (error: Exception?) -> Unit) {
        val song = RoomSelSongModel(
            inputModel.songName,
            inputModel.songNo,
            inputModel.singer,
            inputModel.imageUrl,
            userNo = mUser.id.toString(),
            name = mUser.name,
            status = RoomSelSongModel.STATUS_IDLE,
            createAt = mCurrentServerTs, pinAt = 0.0
        )
        innerAddChooseSongInfo(song, completion)
    }

    override fun makeSongTop(inputModel: MakeSongTopInputModel, completion: (error: Exception?) -> Unit) {
        // move the song to second position
        if (mSongChosenList.size <= 0) {
            completion.invoke(RuntimeException("The chosen song list is empty!"))
            return
        }
        if (mSongChosenList.size < 3) {
            completion.invoke(RuntimeException("The chosen songs size is less then three, it is unnecessary to top up!"))
            return
        }

        val filter = mSongChosenList.filter { it.songNo == inputModel.songNo }
        val targetSong = filter.getOrNull(0)
        if (targetSong == null) {
            completion.invoke(RuntimeException("The song no not found!"))
            return
        }

        //net request and notify others
        val indexOf = mSongChosenList.indexOf(targetSong)
        val newSong = RoomSelSongModel(
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

    override fun makeSongDidPlay(inputModel: RoomSelSongModel, completion: (error: Exception?) -> Unit) {
        if (mSongChosenList.size <= 0) {
            completion.invoke(RuntimeException("The chosen song list is empty!"))
            return
        }

        val filter = mSongChosenList.filter { it.songNo == inputModel.songNo }
        val targetSong = filter.getOrNull(0)
        if (targetSong == null) {
            completion.invoke(RuntimeException("The song no not found!"))
            return
        }
        if (targetSong.status == RoomSelSongModel.STATUS_PLAYING) {
            completion.invoke(null)
            return
        }

        val indexOf = mSongChosenList.indexOf(targetSong)
        val newSong = RoomSelSongModel(
            targetSong.songName,
            targetSong.songNo,
            targetSong.singer,
            targetSong.imageUrl,
            userNo = targetSong.userNo,
            name = targetSong.name,
            isOriginal = targetSong.isOriginal,
            status = RoomSelSongModel.STATUS_PLAYING,
            createAt = targetSong.createAt,
            pinAt = targetSong.pinAt
        )
        mSongChosenList[indexOf] = newSong
        innerDidPlaySong(newSong) {
            completion.invoke(it)
        }
    }

    override fun removeSong(
        isSingingSong: Boolean, inputModel: RemoveSongInputModel, completion: (error: Exception?) -> Unit
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
    private fun innerGetChooseSongInfo(completion: (error: Exception?, list: List<RoomSelSongModel>?) -> Unit) {
        getListCollection(kCollectionIdChooseSong).getMetaData { error, value ->
            KTVLogger.d(TAG, "innerGetChooseSongInfo roomId:$mCurRoomNo $error $value")
            if (error != null) {
                runOnMainThread {
                    completion.invoke(Exception(error.message), null)
                }
                return@getMetaData
            }
            try {
                val out = GsonTools.toList(GsonTools.beanToString(value), RoomSelSongModel::class.java)
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
    private fun innerAddChooseSongInfo(songInfo: RoomSelSongModel, completion: (error: Exception?) -> Unit) {
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
    private fun innerRemoveChooseSong(songModel: RoomSelSongModel, completion: (error: Exception?) -> Unit) {
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
    private fun innerPinSong(songModel: RoomSelSongModel, completion: (error: Exception?) -> Unit) {
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
    private fun innerDidPlaySong(song: RoomSelSongModel, completion: (error: Exception?) -> Unit) {
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
     * @param inputModel
     * @param completion
     * @receiver
     */
    override fun joinChorus(inputModel: RoomSelSongModel, completion: (error: Exception?) -> Unit) {
        innerGetSeatInfo { error, list ->
            if (error == null && list != null) {
                list.forEach { seat ->
                    if (seat.userNo == mUser.id.toString()) {
                        // 座位 joinSing -> true
                        val seatInfo = RoomSeatModel(
                            seat.isMaster,
                            seat.headUrl,
                            seat.userNo,
                            seat.rtcUid,
                            seat.name,
                            seat.seatIndex,
                            inputModel.songNo + inputModel.createAt,
                            RoomSeatModel.MUTED_VALUE_FALSE,
                            seat.isVideoMuted
                        )
                        innerUpdateSeat(seatInfo, completion)
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
        mSeatMap.forEach {
            if (it.value?.userNo == mUser.id.toString()) {
                it.value?.let { originSeatInfo ->
                    val seatInfo = RoomSeatModel(
                        originSeatInfo.isMaster,
                        originSeatInfo.headUrl,
                        originSeatInfo.userNo,
                        originSeatInfo.rtcUid,
                        originSeatInfo.name,
                        originSeatInfo.seatIndex,
                        "",
                        RoomSeatModel.MUTED_VALUE_TRUE,
                        originSeatInfo.isVideoMuted
                    )
                    innerUpdateSeat(seatInfo, completion)
                }
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
                val map: Map<String, Any> = HashMap()
                val seats = value.getMap() ?: GsonTools.toBean(GsonTools.beanToString(value), map.javaClass)
                KTVLogger.d(TAG, "$kCollectionIdSeatInfo seats: $seats")
                seats?.values?.forEach {
                    val ss = GsonTools.toList(GsonTools.beanToString(it), RoomSeatModel::class.java) ?: return
                    ss.forEach { roomSeat ->
                        mServiceLister?.onRoomSeatDidChanged(
                            KTVServiceProtocol.KTVSubscribe.KTVSubscribeUpdated,
                            roomSeat
                        )
                    }
                }
            }

            kCollectionIdChooseSong -> {
                val map: Map<String, Any> = HashMap()
                val songs = value.getMap() ?: GsonTools.toBean(GsonTools.beanToString(value), map.javaClass)
                KTVLogger.d(TAG, "$kCollectionIdChooseSong songs: $songs")
                songs?.values?.forEach {
                    val ss = GsonTools.toList(GsonTools.beanToString(it), RoomSelSongModel::class.java) ?: return
                    ss.forEach { song ->
                        mServiceLister?.onChooseSong(KTVServiceProtocol.KTVSubscribe.KTVSubscribeUpdated, song)
                    }
                }
            }
        }
    }
}