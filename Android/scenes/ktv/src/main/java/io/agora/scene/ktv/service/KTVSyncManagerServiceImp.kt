package io.agora.scene.ktv.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.agora.rtmsyncmanager.ISceneResponse
import io.agora.rtmsyncmanager.RoomExpirationPolicy
import io.agora.rtmsyncmanager.RoomService
import io.agora.rtmsyncmanager.SyncManager
import io.agora.rtmsyncmanager.model.AUICommonConfig
import io.agora.rtmsyncmanager.model.AUIRoomContext
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserInfo
import io.agora.rtmsyncmanager.model.AUIUserThumbnailInfo
import io.agora.rtmsyncmanager.service.IAUIUserService
import io.agora.rtmsyncmanager.service.collection.AUIAttributesModel
import io.agora.rtmsyncmanager.service.collection.AUICollectionException
import io.agora.rtmsyncmanager.service.collection.AUIListCollection
import io.agora.rtmsyncmanager.service.collection.AUIMapCollection
import io.agora.rtmsyncmanager.service.http.HttpManager
import io.agora.rtmsyncmanager.service.room.AUIRoomManager
import io.agora.rtmsyncmanager.service.rtm.AUIRtmException
import io.agora.rtmsyncmanager.utils.AUILogger
import io.agora.rtmsyncmanager.utils.GsonTools
import io.agora.rtmsyncmanager.utils.ObservableHelper
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.ServerConfig
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.bean.User
import io.agora.scene.base.manager.UserManager
import io.agora.scene.ktv.KTVLogger
import io.agora.scene.ktv.widget.song.SongItem
import kotlinx.coroutines.delay
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
    private val kCollectionChosenSong = "choose_song" // list collection
    private val kCollectionSeatInfo = "seat_info" // map collection
    private val kCollectionChorusInfo = "chorister_info" // list collection

    private val mMainHandler by lazy { Handler(Looper.getMainLooper()) }

    private val mSyncManager: SyncManager

    private val mRoomManager = AUIRoomManager()
    private val mRoomService: RoomService

    private val mAppId: String get() = BuildConfig.AGORA_APP_ID

    private var mDiffTs: Long = 0

    private var mLastCreateTime: Long = -1

    private val mCurrentServerTs: Long get() = SystemClock.elapsedRealtime() + mDiffTs

    @Volatile
    private var mCurRoomNo: String = ""

    private val mUser: User get() = UserManager.getInstance().user

    private val mUserList = mutableListOf<AUIUserInfo>()
    private val mSeatMap = mutableMapOf<Int, RoomMicSeatInfo>()
    private val mSongChosenList = mutableListOf<RoomSongInfo>()
    private val mChoristerList = mutableListOf<RoomChoristerInfo>()

    private val observableHelper = ObservableHelper<KtvServiceListenerProtocol>()

    private var mRTmToken: String = ""

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
            success = { rtmToken ->
                KTVLogger.d(TAG, "renewRtmTokens success")
                callback.invoke(rtmToken, null)
            },
            failure = {
                KTVLogger.e(TAG, it, "renewRtmToken failed,$it")
                callback.invoke(null, it)
            })
    }

    /**
     * Renew rtc token
     *
     * @param channelName
     * @param callback
     * @receiver
     */
    private fun renewRtcToken(
        channelName: String,
        callback: (rtcToken: String?, rtcChorusToken: String?, exception: Exception?) -> Unit
    ) {
        TokenGenerator.generateToken(channelName, mUser.id.toString(),
            TokenGenerator.TokenGeneratorType.Token006, TokenGenerator.AgoraTokenType.Rtc,
            success = { rtcToken ->
                KTVLogger.d(TAG, "renewRtcToken success")
                TokenGenerator.generateToken(channelName + "_rtc_ex", mUser.id.toString(),
                    TokenGenerator.TokenGeneratorType.Token006, TokenGenerator.AgoraTokenType.Rtc,
                    success = { rtcChorusToken ->
                        KTVLogger.d(TAG, "renewRtcChorusToken success")
                        callback.invoke(rtcToken, rtcChorusToken, null)
                    },
                    failure = { exception ->
                        KTVLogger.e(TAG, "renewRtcChorusToken failed, $exception")
                        callback.invoke(rtcToken, null, exception)
                    })
            },
            failure = { exception ->
                KTVLogger.e(TAG, "renewRtcToken failed, $exception")
                callback.invoke(null, null, exception)
            })
    }

    /**
     * Reset
     *
     */
    override fun reset() {
        mSeatMap.clear()
        mSongChosenList.clear()
        mChoristerList.clear()
        mUserList.clear()
        mCurRoomNo = ""
    }

    /**
     * Get map collection
     *
     * @param kCollectionId
     * @return
     */
    private fun getMapCollection(kCollectionId: String, roomId: String = mCurRoomNo): AUIMapCollection {
        val scene = mSyncManager.createScene(roomId)
        return scene.getCollection(kCollectionId) { a, b, c -> AUIMapCollection(a, b, c) }
    }

    /**
     * Get list collection
     *
     * @param kCollectionId
     * @return
     */
    private fun getListCollection(kCollectionId: String, roomId: String = mCurRoomNo): AUIListCollection {
        val scene = mSyncManager.createScene(roomId)
        return scene.getCollection(kCollectionId) { a, b, c -> AUIListCollection(a, b, c) }
    }

    /**
     * Get room list
     *
     * @param completion
     * @receiver
     */
    override fun getRoomList(completion: (list: List<AUIRoomInfo>) -> Unit) {
        KTVLogger.d(TAG, "getRoomList start")
        mRoomService.getRoomList(mAppId, kSceneId, mLastCreateTime, 20) { error, ts, roomList ->
            if (error == null) {
                ts?.let { serverTs ->
                    mDiffTs = SystemClock.elapsedRealtime() - serverTs
                }
                val newRoomList = roomList?.sortedBy { it.createTime } ?: emptyList()
                KTVLogger.d(TAG, "getRoomList success, roomCount:${newRoomList.size}")
                runOnMainThread { completion.invoke(newRoomList) }
            } else {
                KTVLogger.e(TAG, error, "getRoomList error, $error")
                runOnMainThread { completion.invoke(emptyList()) }
            }
        }
    }

    /**
     * Create room
     *
     * @param createInfo
     * @param completion
     * @receiver
     */
    override fun createRoom(createInfo: CreateRoomInfo, completion: (error: Exception?, out: JoinRoomInfo?) -> Unit) {
        KTVLogger.d(TAG, "createRoom start")
        val roomId = (Random(mCurrentServerTs).nextInt(100000) + 1000000).toString()
        renewRtcToken(roomId, callback = { rtcToken, rtcChorusToken, exception ->
            if (rtcToken.isNullOrEmpty() || rtcChorusToken.isNullOrEmpty()) {
                completion.invoke(exception, null)
                return@renewRtcToken
            }
            initRtmSync {
                if (it != null) {
                    completion.invoke(Exception(it.message), null)
                    return@initRtmSync
                }
                val roomInfo = AUIRoomInfo().apply {
                    this.roomId = roomId
                    this.roomName = createInfo.name
                    this.roomOwner = AUIUserThumbnailInfo().apply {
                        userId = mUser.id.toString()
                        userName = mUser.name
                        userAvatar = mUser.fullHeadUrl
                    }
                    this.createTime = mCurrentServerTs
                    this.customPayload[KTVParameters.ROOM_USER_COUNT] = 1
                    this.customPayload[KTVParameters.THUMBNAIL_ID] = createInfo.icon
                    this.customPayload[KTVParameters.PASSWORD] = createInfo.password
                }
                val scene = mSyncManager.createScene(roomInfo.roomId)
                scene.bindRespDelegate(this)
                scene.userService.registerRespObserver(this)
                getMapCollection(kCollectionSeatInfo, roomId).subscribeAttributesDidChanged(this::onAttributeChanged)
                getMapCollection(kCollectionSeatInfo, roomId).subscribeWillMerge(this::onMetadataWillMerge)
                getListCollection(kCollectionChosenSong, roomId).subscribeAttributesDidChanged(this::onAttributeChanged)
                getListCollection(kCollectionChorusInfo, roomId).subscribeAttributesDidChanged(this::onAttributeChanged)
                mRoomService.createRoom(mAppId, kSceneId, roomInfo, completion = { error, _ ->
                    if (error == null) {
                        KTVLogger.d(TAG, "createRoom success: $roomInfo")
                        mCurRoomNo = roomInfo.roomId
                        runOnMainThread {
                            val joinInfo = JoinRoomInfo(mRTmToken, rtcToken, rtcChorusToken)
                            joinInfo.roomOwner = roomInfo.roomOwner
                            joinInfo.roomName = roomInfo.roomName
                            joinInfo.roomId = roomInfo.roomId
                            joinInfo.customPayload = roomInfo.customPayload
                            joinInfo.createTime = roomInfo.createTime
                            completion.invoke(null, joinInfo)
                        }
                    } else {
                        KTVLogger.e(TAG, "createRoom failed: $error")
                        runOnMainThread {
                            completion.invoke(Exception("${error.message}(${error.code})"), null)
                        }
                    }
                })
            }
        })
    }

    /**
     * Join room
     *
     * @param completion
     * @receiver
     */
    override fun joinRoom(
        roomId: String, password: String?, completion: (error: Exception?, out: JoinRoomInfo?) -> Unit
    ) {
        if (mCurRoomNo.isNotEmpty()) {
            completion.invoke(Exception("您已加入房间 $mCurRoomNo"), null)
            return
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId)
        if (cacheRoom == null) {
            completion.invoke(Exception("房间 $mCurRoomNo 不可用"), null)
            return
        }
        val password = cacheRoom.customPayload[KTVParameters.PASSWORD] as? String
        if (!password.isNullOrEmpty() && password != roomId) {
            completion.invoke(Exception("密码错误!"), null)
            return
        }
        renewRtcToken(roomId, callback = { rtcToken, rtcChorusToken, exception ->
            if (rtcToken.isNullOrEmpty() || rtcChorusToken.isNullOrEmpty()) {
                completion.invoke(exception, null)
                return@renewRtcToken
            }
            initRtmSync {
                if (it != null) {
                    completion.invoke(Exception(it.message), null)
                    return@initRtmSync
                }
                val scene = mSyncManager.createScene(roomId)
                scene.bindRespDelegate(this)
                scene.userService.registerRespObserver(this)
                getMapCollection(kCollectionSeatInfo, roomId).subscribeAttributesDidChanged(this::onAttributeChanged)
                getMapCollection(kCollectionSeatInfo, roomId).subscribeWillMerge(this::onMetadataWillMerge)
                getListCollection(kCollectionChosenSong, roomId).subscribeAttributesDidChanged(this::onAttributeChanged)
                getListCollection(kCollectionChorusInfo, roomId).subscribeAttributesDidChanged(this::onAttributeChanged)
                mRoomService.enterRoom(mAppId, kSceneId, roomId, completion = { error ->
                    if (error == null) {
                        KTVLogger.d(TAG, "enterRoom success: ${cacheRoom.roomId}")
                        mCurRoomNo = cacheRoom.roomId
                        runOnMainThread {
                            val joinInfo = JoinRoomInfo(mRTmToken, rtcToken, rtcChorusToken)
                            joinInfo.roomOwner = cacheRoom.roomOwner
                            joinInfo.roomName = cacheRoom.roomName
                            joinInfo.roomId = cacheRoom.roomId
                            joinInfo.customPayload = cacheRoom.customPayload
                            joinInfo.createTime = cacheRoom.createTime
                            completion.invoke(null, joinInfo)
                        }
                    } else {
                        KTVLogger.e(TAG, "enterRoom failed: $error")
                        runOnMainThread {
                            completion.invoke(Exception("${error.message}(${error.code})"), null)
                        }
                    }
                })
            }
        })
    }

    /**
     * Leave room
     *
     * @param completion
     * @receiver
     */
    override fun leaveRoom(completion: (error: Exception?) -> Unit) {
        val scene = mSyncManager.createScene(mCurRoomNo)
        scene.unbindRespDelegate(this)
        scene.userService.unRegisterRespObserver(this)
        getListCollection(kCollectionChosenSong).subscribeAttributesDidChanged(null)
        getMapCollection(kCollectionSeatInfo).subscribeAttributesDidChanged(null)
        getListCollection(kCollectionChorusInfo).subscribeAttributesDidChanged(null)
        val isOwner = AUIRoomContext.shared().isRoomOwner(mCurRoomNo)
        mRoomService.leaveRoom(mAppId, kSceneId, mCurRoomNo)
        val roomId = mCurRoomNo
        if (!isOwner) {
            // 如果上麦了要下麦，并清空麦位信息
            val seatIndex = mSeatMap.values.firstOrNull { it.user?.userId == mUser.id.toString() }?.seatIndex
            if (seatIndex != null) {
                getMapCollection(kCollectionSeatInfo).mergeMetaData(null,
                    value = mapOf(
                        seatIndex.toString() to mapOf(
                            "user" to GsonTools.beanToMap(AUIUserThumbnailInfo())
                        )
                    ),
                    callback = {
                        if (it == null) {
                            KTVLogger.d(
                                TAG, "leaveRoom-->removeSeatByIndex success roomId:$roomId, index:$seatIndex"
                            )
                        } else {
                            KTVLogger.e(
                                TAG,
                                "leaveRoom-->removeSeatByIndex failed roomId:$roomId, index:$seatIndex, $it"
                            )
                        }
                    })
            }
            // 删除点歌信息
            getListCollection(kCollectionChosenSong).removeMetaData(null,
                filter = listOf(mapOf("userNo" to mUser.id.toString())),
                callback = {
                    if (it == null) {
                        KTVLogger.d(TAG, "leaveRoom-->removeSongByUid success roomId:$roomId, uid:${mUser.id}")
                    } else {
                        KTVLogger.e(TAG, "leaveRoom-->removeSongByUid failed roomId:$roomId, uid:${mUser.id} $it")
                    }
                })
            // 离开合唱
            getListCollection(kCollectionChorusInfo).removeMetaData(null,
                filter = listOf(mapOf("userId" to mUser.id.toString())),
                callback = {
                    if (it == null) {
                        KTVLogger.d(TAG, "leaveRoom-->leaveChorus success roomId:$roomId, useId:${mUser.id}")
                    } else {
                        KTVLogger.d(TAG, "leaveRoom-->leaveChorus failed roomId:$roomId,useId:${mUser.id}, $it")
                    }
                })
        }
        mSeatMap.clear()
        mSongChosenList.clear()
        mChoristerList.clear()
        mCurRoomNo = ""
        completion.invoke(null)
    }

    /**
     * Get all user list
     *
     * @param completion
     * @receiver
     */
    override fun getAllUserList(completion: (error: Exception?, list: List<AUIUserInfo>?) -> Unit) {
        val scene = mSyncManager.createScene(mCurRoomNo)
        scene.userService.getUserInfoList(mCurRoomNo, callback = { error, userList ->
            if (error == null) {
                val newUserList: List<AUIUserInfo> = userList ?: emptyList()
                KTVLogger.d(TAG, "getAllUserList success: $mCurRoomNo, $userList")
                mUserList.clear()
                mUserList.addAll(newUserList)
                observableHelper.notifyEventHandlers { delegate->
                    delegate.onUserListDidChanged(newUserList)
                }
                runOnMainThread {
                    completion.invoke(null, newUserList)
                }
            } else {
                KTVLogger.d(TAG, "getAllUserList failed: $mCurRoomNo $error")
                runOnMainThread {
                    completion.invoke(Exception("${error.message}(${error.code})"), userList)
                }
            }
        })
    }

    override fun getUserInfo(userId: String): AUIUserInfo? {
        val scene = mSyncManager.createScene(mCurRoomNo)
        return scene.userService.getUserInfo(userId)
    }

    // =================== 麦位相关 ===============================


    override fun getAllSeatMap(completion: (error: Exception?, map: Map<Int, RoomMicSeatInfo>) -> Unit) {
        if (mSeatMap.isNotEmpty()) {
            completion.invoke(null, mSeatMap)
        }
        // TODO: 获取麦位数据
    }

    /**
     * On seat
     *
     * @param seatIndex
     * @param completion
     * @receiver
     */
    override fun enterSeat(seatIndex: Int?, completion: (error: Exception?) -> Unit) {
        val seatInfo = mSeatMap.values.firstOrNull { it.user?.userId == mUser.id.toString() }
        if (seatInfo != null) {
            completion.invoke(Exception("您已在麦位，请先下麦后重试"))
            return
        }
        if (seatIndex == null) {
            val list = mutableListOf(0, 1, 2, 3, 4, 5, 6, 7)
            mSeatMap.values.forEach { seat ->
                list.removeIf { it == seat.seatIndex }
                if (seat.user?.userId == mUser.id.toString()) {
                    completion.invoke(Exception("您已在麦位，请先下麦后重试"))
                    return
                }
            }
            if (list.isEmpty()) {
                completion.invoke(Exception("麦位已满，请稍后再试"))
            } else {
                getMapCollection(kCollectionSeatInfo).mergeMetaData(
                    valueCmd = null,
                    value = mapOf(
                        list[0].toString() to mapOf(
                            "owner" to GsonTools.beanToMap(AUIRoomContext.shared().currentUserInfo)
                        )
                    ),
                    callback = {
                        if (it == null) {
                            KTVLogger.d(TAG, "enterSeat success roomId:$mCurRoomNo")
                            runOnMainThread {
                                completion.invoke(null)
                            }
                        } else {
                            KTVLogger.d(TAG, "enterSeat failed roomId:$mCurRoomNo, $it")
                            runOnMainThread {
                                completion.invoke(Exception("${it.message}(${it.code})"))
                            }
                        }
                    })
            }
        } else {
            getMapCollection(kCollectionSeatInfo).mergeMetaData(
                valueCmd = null,
                value = mapOf(
                    seatIndex.toString() to mapOf(
                        "owner" to GsonTools.beanToMap(AUIRoomContext.shared().currentUserInfo)
                    )
                ),
                callback = {
                    if (it == null) {
                        KTVLogger.d(TAG, "enterSeat success roomId:$mCurRoomNo")
                    } else {
                        KTVLogger.d(TAG, "enterSeat failed roomId:$mCurRoomNo, $it")
                    }
                    runOnMainThread {
                        completion.invoke(it)
                    }
                })
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
        val targetSeatInfo = mSeatMap.values.find { it.user?.userId == mUser.id.toString() }
        if (targetSeatInfo == null) {
            completion.invoke(Exception("您不在麦位，请先上麦后重试"))
        } else {
            val userId = mUser.id.toString()
            getMapCollection(kCollectionSeatInfo).subscribeWillMerge { publisherId, valueCmd, newValue, oldValue ->

                return@subscribeWillMerge null
            }
            // 清空麦位
            getMapCollection(kCollectionSeatInfo).mergeMetaData(null,
                value = mapOf(
                    seatIndex.toString() to mapOf(
                        "user" to GsonTools.beanToMap(AUIUserThumbnailInfo())
                    )
                ),
                callback = {
                    if (it == null) {
                        KTVLogger.d(TAG, "outSeat success roomId:$mCurRoomNo, seatInfo:$targetSeatInfo")
                    } else {
                        KTVLogger.d(TAG, "outSeat failed roomId:$mCurRoomNo, $it")
                    }
                    runOnMainThread {
                        completion.invoke(it)
                    }
                })
            // 移除歌曲
            getListCollection(kCollectionChosenSong).removeMetaData(null,
                filter = listOf(mapOf("userNo" to userId)),
                callback = {
                    if (it == null) {
                        KTVLogger.d(TAG, "outSeat removeSongWithMineOrder success roomId:$mCurRoomNo, uid:$userId")
                    } else {
                        KTVLogger.d(TAG, "outSeat removeSongWithMineOrder failed roomId:$mCurRoomNo, $it")
                    }
                })
            // 移除合唱
            getListCollection(kCollectionChorusInfo).removeMetaData(null,
                filter = listOf(mapOf("userNo" to userId)),
                callback = {
                    if (it == null) {
                        KTVLogger.d(TAG, "outSeat removeChoristerWithMine success roomId:$mCurRoomNo, uid:$userId")
                    } else {
                        KTVLogger.d(TAG, "outSeat removeChoristerWithMine failed roomId:$mCurRoomNo, $it")
                    }
                })
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
        val scene = mSyncManager.createScene(mCurRoomNo)
        scene.userService.muteUserAudio(mute, callback = { auiException ->
            auiException?.let {
                completion.invoke(Exception("${it.message}(${it.code})"))
            } ?: run {
                completion.invoke(null)
            }
        })
    }

    /**
     * Mute user video
     *
     * @param mute
     * @param completion
     * @receiver
     */
    override fun muteUserVideo(mute: Boolean, completion: (error: Exception?) -> Unit) {
        val scene = mSyncManager.createScene(mCurRoomNo)
        scene.userService.muteUserVideo(mute, callback = { auiException ->
            auiException?.let {
                completion.invoke(Exception("${it.message}(${it.code})"))
            } ?: run {
                completion.invoke(null)
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
        getListCollection(kCollectionChosenSong).getMetaData { error, value ->
            KTVLogger.d(TAG, "getChosenSongList roomId:$mCurRoomNo $error $value")
            if (error != null) {
                runOnMainThread {
                    completion.invoke(Exception(error.message), null)
                }
                return@getMetaData
            }
            try {
                val out = GsonTools.toList(GsonTools.beanToString(value), RoomSongInfo::class.java)
                KTVLogger.d(TAG, "getChosenSongList onSuccess roomId:$mCurRoomNo $out")
                runOnMainThread {
                    completion.invoke(null, out)
                }
            } catch (e: Exception) {
                KTVLogger.d(TAG, "getChosenSongList onFail roomId:$mCurRoomNo $e")
                runOnMainThread {
                    completion.invoke(e, null)
                }
            }
        }
    }

    override fun chooseSong(songInfo: RoomSongInfo, completion: (error: Exception?) -> Unit) {
        val song = RoomSongInfo(
            songName = songInfo.songName,
            songNo = songInfo.songNo,
            singer = songInfo.singer,
            imageUrl = songInfo.imageUrl,
            userNo = mUser.id.toString(),
            name = mUser.name,
            status = RoomSongInfo.STATUS_IDLE,
            createAt = mCurrentServerTs,
            pinAt = 0.0
        )
        getListCollection(kCollectionChosenSong).addMetaData(null,
            GsonTools.beanToMap(song),
            null,
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "chooseSong success roomId:$mCurRoomNo, song:$songInfo")
                } else {
                    KTVLogger.d(TAG, "chooseSong failed roomId:$mCurRoomNo, $it")
                }
                runOnMainThread {
                    completion.invoke(it)
                }
            })
    }

    override fun pinSong(songCode: String, completion: (error: Exception?) -> Unit) {
        // move the song to second position
        if (mSongChosenList.size <= 0) {
            completion.invoke(Exception("The chosen song list is empty!"))
            return
        }
        if (mSongChosenList.size < 3) {
            completion.invoke(Exception("The chosen songs size is less then three, it is unnecessary to top up!"))
            return
        }

        val filter = mSongChosenList.filter { it.songNo == songCode }
        val targetSong = filter.getOrNull(0)
        if (targetSong == null) {
            completion.invoke(Exception("The song no not found!"))
            return
        }

        //net request and notify others
        val indexOf = mSongChosenList.indexOf(targetSong)
        val newSong = targetSong.copy(pinAt = mCurrentServerTs.toDouble())
        mSongChosenList[indexOf] = newSong
        getListCollection(kCollectionChosenSong).mergeMetaData(null,
            mapOf("pinAt" to mCurrentServerTs),
            filter = listOf(
                mapOf("songNo" to newSong.songNo, "userNo" to (newSong.userNo ?: ""))
            ),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "pinSong success roomId:$mCurRoomNo, song:$newSong")
                } else {
                    KTVLogger.d(TAG, "pinSong failed roomId:$mCurRoomNo, $it")
                }
                runOnMainThread {
                    completion.invoke(it)
                }
            })
    }

    override fun makeSongDidPlay(songCode: String, completion: (error: Exception?) -> Unit) {
        if (mSongChosenList.size <= 0) {
            completion.invoke(Exception("已选歌单为空"))
            return
        }

        val filter = mSongChosenList.filter { it.songNo == songCode }
        val targetSong = filter.getOrNull(0)
        if (targetSong == null) {
            completion.invoke(Exception("已选歌单中不存在该歌曲"))
            return
        }
        if (targetSong.status == RoomSongInfo.STATUS_PLAYING) {
            completion.invoke(Exception("歌单中该歌曲已经处于播放状态"))
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
        getListCollection(kCollectionChosenSong).mergeMetaData(null,
            mapOf("status" to newSong.status),
            filter = listOf(
                mapOf("songNo" to newSong.songNo, "userNo" to (newSong.userNo ?: ""))
            ),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "makeSongDidPlay success roomId:$mCurRoomNo, song:$newSong")
                    runOnMainThread {
                        completion.invoke(null)
                    }
                } else {
                    KTVLogger.e(TAG, "makeSongDidPlay failed roomId:$mCurRoomNo, $it")
                    runOnMainThread {
                        completion.invoke(Exception("${it.message}(${it.code})"))
                    }
                }
            })
    }

    override fun removeSong(songCode: String, completion: (error: Exception?) -> Unit) {
        if (mSongChosenList.size <= 0) {
            completion.invoke(Exception("已选歌单为空"))
            return
        }
        val targetSong = mSongChosenList.filter {
            it.songNo == songCode && it.userNo == mUser.id.toString()
        }.getOrNull(0)
        if (targetSong == null) {
            completion.invoke(Exception("已选歌单中不存在该歌曲"))
            return
        }
        getListCollection(kCollectionChosenSong).removeMetaData(null,
            filter = listOf(mapOf("songCode" to targetSong.songNo)),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "removeSong success roomId:$mCurRoomNo, song:$targetSong")
                    runOnMainThread {
                        completion.invoke(null)
                    }
                } else {
                    KTVLogger.e(TAG, "removeSong failed roomId:$mCurRoomNo, $it")
                    runOnMainThread {
                        completion.invoke(Exception("${it.message}(${it.code})"))
                    }
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
        val choristerInfo = RoomChoristerInfo(mUser.id.toString(), songCode)
        getListCollection(kCollectionChorusInfo).addMetaData(
            valueCmd = RoomChoristerCmd.joinChorusCmd.name,
            value = GsonTools.beanToMap(choristerInfo),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "joinChorus success roomId:$mCurRoomNo, choristerInfo:$choristerInfo")
                } else {
                    KTVLogger.d(TAG, "joinChorus failed roomId:$mCurRoomNo, choristerInfo:$choristerInfo, $it")
                }
                runOnMainThread {
                    completion.invoke(it)
                }
            })
    }

    /**
     * Leave chorus
     *
     * @param completion
     * @receiver
     */
    override fun leaveChorus(completion: (error: Exception?) -> Unit) {
        getListCollection(kCollectionChorusInfo).removeMetaData(
            valueCmd = RoomChoristerCmd.leaveChorusCmd.name,
            filter = listOf(mapOf("userId" to mUser.id.toString())),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "leaveChorus success roomId:$mCurRoomNo, useId:${mUser.id}")
                } else {
                    KTVLogger.d(TAG, "leaveChorus failed roomId:$mCurRoomNo,useId:${mUser.id}, $it")
                }
                runOnMainThread {
                    completion.invoke(it)
                }
            })
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
        if (mRTmToken.isEmpty()) {
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
            mSyncManager.login(mRTmToken, completion = {
                if (it == null) {
                    completion.invoke(null)
                } else {
                    completion.invoke(it)
                    KTVLogger.e(TAG, "initRtmSync, without renewToken loginRtm failed: $it")
                }
            })
        }
    }

    override fun getChoristerList(completion: (error: Exception?, choristerList: List<RoomChoristerInfo>) -> Unit) {
        getListCollection(kCollectionChorusInfo).getMetaData { error, value ->
            KTVLogger.d(TAG, "getChoristerList roomId:$mCurRoomNo $error $value")
            if (error != null) {
                KTVLogger.d(TAG, "getChoristerList failed roomId:$mCurRoomNo $error")
                runOnMainThread {
                    completion.invoke(Exception("${error.message}(${error.code})"), emptyList())
                }
                return@getMetaData
            }
            try {
                val choristerList =
                    GsonTools.toList(GsonTools.beanToString(value), RoomChoristerInfo::class.java)
                KTVLogger.d(TAG, "getChoristerList onSuccess roomId:$mCurRoomNo $choristerList")
                choristerList?.let {
                    mChoristerList.clear()
                    mChoristerList.addAll(it)
                }
                runOnMainThread {
                    completion.invoke(null, mChoristerList)
                }
            } catch (e: Exception) {
                KTVLogger.d(TAG, "getChoristerList onFail roomId:$mCurRoomNo $e")
                runOnMainThread {
                    completion.invoke(e, emptyList())
                }
            }
        }
    }

    /**
     * Subscribe listener
     *
     * @param listener
     */
    override fun subscribeListener(listener: KtvServiceListenerProtocol) {
        observableHelper.subscribeEvent(listener)
    }

// =======================   ISceneResponse Start =======================

    override fun onWillInitSceneMetadata(channelName: String): Map<String, Any>? {
        val seatMap = mutableMapOf<String, Any>()
        for (i in 0 until 8) {
            val seat = RoomMicSeatInfo().apply {
                seatIndex = i
                if (i == 0) {
                    user = AUIRoomContext.shared().currentUserInfo
                    seatStatus = RoomMicSeatStatus.used
                } else {
                    user = AUIUserThumbnailInfo()
                }
            }
            seatMap[i.toString()] = seat
        }

        return mapOf(kCollectionSeatInfo to seatMap)
    }

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
            observableHelper.notifyEventHandlers { delegate->
                delegate.onRoomExpire(channelName)
            }
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
            observableHelper.notifyEventHandlers { delegate->
                delegate.onRoomDestroy(channelName)
            }
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
            observableHelper.notifyEventHandlers { delegate->
                delegate.onUserListDidChanged(mUserList)
            }
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
        observableHelper.notifyEventHandlers { delegate->
            delegate.onUserListDidChanged(mUserList)
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId) ?: return
        if (mUser.id.toString() == cacheRoom.roomOwner?.userId) {
            cacheRoom.customPayload[KTVParameters.ROOM_USER_COUNT] = mUserList.count()
            mRoomManager.updateRoomInfo(mAppId, kSceneId, cacheRoom, callback = { auiException, roomInfo ->
                if (auiException == null) {
                    KTVLogger.d(TAG, "updateRoom success: $mCurRoomNo, $roomInfo")
                } else {
                    KTVLogger.d(TAG, "updateRoom failed: $mCurRoomNo $auiException")
                }
            })
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
        observableHelper.notifyEventHandlers { delegate->
            delegate.onUserListDidChanged(mUserList)
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId) ?: return
        if (mUser.id.toString() == cacheRoom.roomOwner?.userId) {
            cacheRoom.customPayload[KTVParameters.ROOM_USER_COUNT] = mUserList.count()
            mRoomManager.updateRoomInfo(mAppId, kSceneId, cacheRoom, callback = { auiException, roomInfo ->
                if (auiException == null) {
                    KTVLogger.d(TAG, "updateRoom success: $mCurRoomNo, $roomInfo")
                } else {
                    KTVLogger.d(TAG, "updateRoom failed: $mCurRoomNo $auiException")
                }
            })
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
        observableHelper.notifyEventHandlers { delegate->
            delegate.onUserListDidChanged(mUserList)
        }
    }

    /**
     * On user audio mute
     *
     * @param userId
     * @param mute
     */
    override fun onUserAudioMute(userId: String, mute: Boolean) {
        KTVLogger.d(TAG, "onUserAudioMute, userId:$userId, mute:$mute")
        observableHelper.notifyEventHandlers { delegate->
            delegate.onUserAudioMute(userId, mute)
        }
    }

    /**
     * On user video mute
     *
     * @param userId
     * @param mute
     */
    override fun onUserVideoMute(userId: String, mute: Boolean) {
        KTVLogger.d(TAG, "onUserVideoMute, userId:$userId, mute:$mute")
        observableHelper.notifyEventHandlers { delegate->
            delegate.onUserVideoMute(userId, mute)
        }
    }
    // =======================  AUIUserRespObserver End =======================


    /**
     * On attribute changed
     *
     * @param channelId
     * @param key
     * @param value
     */
    private fun onAttributeChanged(channelId: String, key: String, value: AUIAttributesModel) {
        KTVLogger.d(
            TAG, "onAttributeChanged, channelId:$channelId, key:$key, list:${value.getList()}, map:${value.getMap()}"
        )
        when (key) {
            kCollectionSeatInfo -> { // 麦位信息
                val map: Map<String, Any> = HashMap()
                val seats = value.getMap() ?: GsonTools.toBean(GsonTools.beanToString(value), map.javaClass)
                seats?.values?.forEach {
                    val newSeatInfo =
                        GsonTools.toBean(GsonTools.beanToString(it), RoomMicSeatInfo::class.java) ?: return
                    val index = newSeatInfo.seatIndex
                    val oldSeatInfo = mSeatMap[index]
                    mSeatMap[index] = newSeatInfo
                    val newSeatUserId = newSeatInfo.user?.userId ?: ""
                    val oldSeatUserId = oldSeatInfo?.user?.userId ?: ""
                    if (oldSeatUserId.isEmpty() && newSeatUserId.isNotEmpty()) {
                        KTVLogger.d(TAG, "onAddOrUpdateSeat: $newSeatInfo")
                        observableHelper.notifyEventHandlers { delegate->
                            delegate.onUserEnterSeat(newSeatInfo)
                        }
                    }
                    if (oldSeatUserId.isNotEmpty() && newSeatUserId.isEmpty()) {
                        KTVLogger.d(TAG, "onRemoveSeat: $oldSeatInfo")
                        observableHelper.notifyEventHandlers { delegate->
                            delegate.onUserEnterSeat(oldSeatInfo!!)
                        }
                    }
                }
            }

            kCollectionChosenSong -> { // 已选歌单
                val songList: List<RoomSongInfo> =
                    GsonTools.toList(GsonTools.beanToString(value.getList()), RoomSongInfo::class.java)
                        ?: emptyList()
                KTVLogger.d(TAG, "$kCollectionChosenSong songList: $songList")
                observableHelper.notifyEventHandlers { delegate->
                    delegate.onChosenSongListDidChanged(songList)
                }
            }

            kCollectionChorusInfo -> { // 合唱信息
                val choristerList: List<RoomChoristerInfo> =
                    GsonTools.toList(GsonTools.beanToString(value.getList()), RoomChoristerInfo::class.java)
                        ?: emptyList()
                KTVLogger.d(TAG, "$kCollectionChorusInfo choristerList: $choristerList")
                choristerList.forEach { newChorister ->
                    var hasChorister = false
                    this.mChoristerList.forEach {
                        if (it.userId == newChorister.userId) {
                            hasChorister = true
                        }
                    }
                    if (!hasChorister) {
                        observableHelper.notifyEventHandlers { delegate->
                            delegate.onChoristerDidEnter(newChorister)
                        }
                    }
                }
                this.mChoristerList.forEach { oldChorister ->
                    var hasChorister = false
                    choristerList.forEach {
                        if (it.userId == oldChorister.userId) {
                            hasChorister = true
                        }
                    }
                    if (!hasChorister) {
                        observableHelper.notifyEventHandlers { delegate->
                            delegate.onChoristerDidLeave(oldChorister)
                        }
                    }
                }
                this.mChoristerList.clear()
                this.mChoristerList.addAll(choristerList)
                observableHelper.notifyEventHandlers { delegate->
                    delegate.onChoristerListDidChanged(choristerList)
                }
            }
        }
    }

    private fun onMetadataWillMerge(
        publisherId: String, valueCmd: String?, newValue: Map<String, Any>, oldValue: Map<String, Any>
    ): AUICollectionException? {
        val seatInfoPair = newValue.toList()[0]
        val seatIndex = seatInfoPair.first.toInt()
        val seatInfoMap = seatInfoPair.second as? Map<*, *>
        when (valueCmd) {
            RoomSeatCmd.enterSeatCmd.name -> {

            }

            RoomSeatCmd.leaveSeatCmd.name -> {
                if (seatIndex == 0) {
                    return AUICollectionException.ErrorCode.unknown.toException()
                }
                // 离开麦位后清空合唱信息
                leaveChorus { }
            }
        }
        return null
    }
}