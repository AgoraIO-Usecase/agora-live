package io.agora.scene.ktv.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.gson.reflect.TypeToken
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
    private val kSceneId = "scene_ktv_1.3.0"
    private val kCollectionSeatInfo = "seat_info" // map collection
    private val kCollectionChosenSong = "choose_song" // list collection
    private val kCollectionChorusInfo = "chorister_info" // list collection

    private var mSeatInfoCollection: AUIMapCollection? = null //
    private var mChosenSongCollection: AUIListCollection? = null
    private var mChoristerInfoCollection: AUIListCollection? = null

    private fun initCollection(roomId: String) {
        val scene = mSyncManager.createScene(roomId)
        mSeatInfoCollection = scene.getCollection(kCollectionSeatInfo) { a, b, c -> AUIMapCollection(a, b, c) }
        mChosenSongCollection = scene.getCollection(kCollectionChosenSong) { a, b, c -> AUIListCollection(a, b, c) }
        mChoristerInfoCollection = scene.getCollection(kCollectionChorusInfo) { a, b, c -> AUIListCollection(a, b, c) }
    }

    private val mMainHandler by lazy { Handler(Looper.getMainLooper()) }

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
     * sync manager
     */
    private val mSyncManager: SyncManager

    /**
     * room manager
     */
    private val mRoomManager = AUIRoomManager()

    /**
     * room service
     */
    private val mRoomService: RoomService

    /**
     * app id
     */
    private val mAppId: String = BuildConfig.AGORA_APP_ID

    /**
     * diff ts
     */
    private var mDiffTs: Long = 0

    /**
     * current server ts
     */
    private val mCurrentServerTs: Long get() = SystemClock.elapsedRealtime() + mDiffTs

    /**
     * current room no
     */
    @Volatile
    private var mCurRoomNo: String = ""

    /**
     * current user
     */
    private val mCurrentUser: AUIUserThumbnailInfo get() = AUIRoomContext.shared().currentUserInfo

    /**
     * room user list
     */
    private val mUserList = mutableListOf<AUIUserInfo>()

    /**
     * room seat map
     */
    private val mSeatMap = mutableMapOf<Int, RoomMicSeatInfo>()

    /**
     * room song chosen list
     */
    private val mSongChosenList = mutableListOf<RoomSongInfo>()

    /**
     * room chorister list
     */
    private val mChoristerList = mutableListOf<RoomChoristerInfo>()

    /**
     * Observable helper
     */
    private val mObservableHelper = ObservableHelper<KtvServiceListenerProtocol>()

    /**
     * rtm token
     */
    private var mRTmToken: String = ""

    init {
        HttpManager.setBaseURL(ServerConfig.roomManagerUrl)
        AUILogger.initLogger(AUILogger.Config(mContext, "KTV"))

        val commonConfig = AUICommonConfig().apply {
            context = mContext
            appId = BuildConfig.AGORA_APP_ID
            owner = AUIUserThumbnailInfo().apply {
                userId = UserManager.getInstance().user.id.toString()
                userName = UserManager.getInstance().user.name
                userAvatar = UserManager.getInstance().user.fullHeadUrl
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
            mCurrentUser.userId,
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
        TokenGenerator.generateToken(channelName, mCurrentUser.userId,
            TokenGenerator.TokenGeneratorType.Token006, TokenGenerator.AgoraTokenType.Rtc,
            success = { rtcToken ->
                KTVLogger.d(TAG, "renewRtcToken success")
                TokenGenerator.generateToken(channelName + "_rtc_ex", mCurrentUser.userId,
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
                mRTmToken = rtmToken
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

    /**
     * Get room list
     *
     * @param completion
     * @receiver
     */
    override fun getRoomList(completion: (list: List<AUIRoomInfo>) -> Unit) {
        KTVLogger.d(TAG, "getRoomList start")
        initRtmSync {
            if (it != null) {
                completion.invoke(emptyList())
                return@initRtmSync
            }
            mRoomService.getRoomList(mAppId, kSceneId, 0, 20) { uiException, ts, roomList ->
                if (uiException == null) {
                    ts?.let { serverTs ->
                        mDiffTs = serverTs - SystemClock.elapsedRealtime()
                    }
                    val newRoomList = roomList?.sortedBy { it.createTime } ?: emptyList()
                    KTVLogger.d(TAG, "getRoomList success, roomCount:${newRoomList.size}")
                    runOnMainThread { completion.invoke(newRoomList) }
                } else {
                    KTVLogger.e(TAG, "getRoomList error, $uiException")
                    runOnMainThread { completion.invoke(emptyList()) }
                }
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
        initRtmSync {
            if (it != null) {
                completion.invoke(Exception("${it.message}(${it.code})"), null)
                return@initRtmSync
            }
            renewRtcToken(roomId, callback = { rtcToken, rtcChorusToken, exception ->
                if (rtcToken.isNullOrEmpty() || rtcChorusToken.isNullOrEmpty()) {
                    completion.invoke(exception, null)
                    return@renewRtcToken
                }
                val roomInfo = AUIRoomInfo().apply {
                    this.roomId = roomId
                    this.roomName = createInfo.name
                    this.roomOwner = AUIUserThumbnailInfo().apply {
                        userId = mCurrentUser.userId
                        userName = mCurrentUser.userName
                        userAvatar = mCurrentUser.userAvatar
                    }
                    this.createTime = mCurrentServerTs
                    this.customPayload[KTVParameters.ROOM_USER_COUNT] = 1
                    this.customPayload[KTVParameters.THUMBNAIL_ID] = createInfo.icon
                    this.customPayload[KTVParameters.PASSWORD] = createInfo.password
                }
                val scene = mSyncManager.createScene(roomInfo.roomId)
                scene.bindRespDelegate(this)
                scene.userService.registerRespObserver(this)
                initCollection(roomId)
                mSeatInfoCollection?.subscribeWillMerge(this::onSeatWillMerge)
                mSeatInfoCollection?.subscribeAttributesDidChanged(this::onAttributeChanged)
                mChosenSongCollection?.subscribeAttributesDidChanged(this::onAttributeChanged)
                mChoristerInfoCollection?.subscribeAttributesDidChanged(this::onAttributeChanged)
                mRoomService.createRoom(mAppId, kSceneId, roomInfo, completion = { rtmException, _ ->
                    if (rtmException == null) {
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
                        KTVLogger.e(TAG, "createRoom failed: $rtmException")
                        runOnMainThread {
                            completion.invoke(Exception("${rtmException.message}(${rtmException.code})"), null)
                        }
                    }
                })
            })
        }
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
            completion.invoke(Exception("already join room $mCurRoomNo!"), null)
            return
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId)
        if (cacheRoom == null) {
            completion.invoke(Exception("room $mCurRoomNo null!"), null)
            return
        }
        val password = cacheRoom.customPayload[KTVParameters.PASSWORD] as? String
        if (!password.isNullOrEmpty() && password != roomId) {
            completion.invoke(Exception("password is wrong!"), null)
            return
        }
        initRtmSync {
            if (it != null) {
                completion.invoke(Exception("${it.message}(${it.code})"), null)
                return@initRtmSync
            }
            renewRtcToken(roomId, callback = { rtcToken, rtcChorusToken, exception ->
                if (rtcToken.isNullOrEmpty() || rtcChorusToken.isNullOrEmpty()) {
                    completion.invoke(exception, null)
                    return@renewRtcToken
                }
                val scene = mSyncManager.createScene(roomId)
                scene.bindRespDelegate(this)
                scene.userService.registerRespObserver(this)
                initCollection(roomId)
                mSeatInfoCollection?.subscribeWillMerge(this::onSeatWillMerge)
                mSeatInfoCollection?.subscribeAttributesDidChanged(this::onAttributeChanged)
                mChosenSongCollection?.subscribeAttributesDidChanged(this::onAttributeChanged)
                mChoristerInfoCollection?.subscribeAttributesDidChanged(this::onAttributeChanged)
                mRoomService.enterRoom(mAppId, kSceneId, roomId, completion = { rtmException ->
                    if (rtmException == null) {
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
                        KTVLogger.e(TAG, "enterRoom failed: $rtmException")
                        runOnMainThread {
                            completion.invoke(Exception("${rtmException.message}(${rtmException.code})"), null)
                        }
                    }
                })
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
        val scene = mSyncManager.createScene(mCurRoomNo)
        scene.unbindRespDelegate(this)
        scene.userService.unRegisterRespObserver(this)
        mSeatInfoCollection?.subscribeAttributesDidChanged(null)
        mChosenSongCollection?.subscribeAttributesDidChanged(null)
        mChoristerInfoCollection?.subscribeAttributesDidChanged(null)
        mRoomService.leaveRoom(mAppId, kSceneId, mCurRoomNo)
        mUserList.clear()
        mSeatMap.clear()
        mSongChosenList.clear()
        mChoristerList.clear()
        mCurRoomNo = ""
        completion.invoke(null)
    }

    /**
     * Reset
     *
     */
    override fun reset() {
        mObservableHelper.unSubscribeAll()
        mUserList.clear()
        mSeatMap.clear()
        mSongChosenList.clear()
        mChoristerList.clear()
        mCurRoomNo = ""
    }

    /**
     * Get all user list
     *
     * @param completion
     * @receiver
     */
    override fun getAllUserList(completion: (error: Exception?, list: List<AUIUserInfo>?) -> Unit) {
        if (mCurRoomNo.isEmpty()) {
            completion.invoke(Exception("current room null"), null)
            return
        }
        val scene = mSyncManager.createScene(mCurRoomNo)
        scene.userService.getUserInfoList(mCurRoomNo, callback = { uiException, userList ->
            if (uiException == null) {
                val newUserList: List<AUIUserInfo> = userList ?: emptyList()
                KTVLogger.d(TAG, "getAllUserList success: $mCurRoomNo, $userList")
                mUserList.clear()
                mUserList.addAll(newUserList)
                mObservableHelper.notifyEventHandlers { delegate ->
                    delegate.onUserListDidChanged(newUserList)
                }
                runOnMainThread {
                    completion.invoke(null, newUserList)
                }
            } else {
                KTVLogger.e(TAG, "getAllUserList failed: $mCurRoomNo $uiException")
                runOnMainThread {
                    completion.invoke(Exception("${uiException.message}(${uiException.code})"), userList)
                }
            }
        })
    }

    override fun getUserInfo(userId: String): AUIUserInfo? {
        val scene = mSyncManager.createScene(mCurRoomNo)
        return scene.userService.getUserInfo(userId)
    }

    /**
     * On seat
     *
     * @param seatIndex
     * @param completion
     * @receiver
     */
    override fun enterSeat(seatIndex: Int?, completion: (error: Exception?) -> Unit) {
        val seatInfo = mSeatMap.values.firstOrNull { it.owner?.userId == mCurrentUser.userId }
        if (seatInfo != null) {
            completion.invoke(Exception("already on seat"))
            return
        }
        var needEnter = false
        if (seatIndex == null) {
            val list = mutableListOf(0, 1, 2, 3, 4, 5, 6, 7)
            mSeatMap.values.forEach { seat ->
                list.removeIf { it == seat.seatIndex }
                if (seat.owner?.userId == mCurrentUser.userId) {
                    completion.invoke(Exception("already on seat"))
                    return
                }
            }
            if (list.isEmpty()) {
                completion.invoke(Exception("no empty seat"))
            } else {
                needEnter = true
            }
        } else {
            needEnter = true
        }
        if (needEnter) {
            mSeatInfoCollection?.mergeMetaData(
                valueCmd = RoomSeatCmd.enterSeatCmd.name,
                value = mapOf(
                    seatIndex.toString() to mapOf(
                        "owner" to GsonTools.beanToMap(AUIRoomContext.shared().currentUserInfo)
                    )
                ),
                callback = { collectionException ->
                    if (collectionException == null) {
                        KTVLogger.d(TAG, "enterSeat success roomId:$mCurRoomNo")
                        runOnMainThread {
                            completion.invoke(null)
                        }
                    } else {
                        KTVLogger.e(TAG, "enterSeat failed roomId:$mCurRoomNo, $collectionException")
                        runOnMainThread {
                            completion.invoke(Exception("${collectionException.message}(${collectionException.code})"))
                        }
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
        val targetSeatInfo = mSeatMap.values.find { it.owner?.userId == mCurrentUser.userId }
        if (targetSeatInfo == null) {
            completion.invoke(Exception("You are not on seat"))
        } else {
            mSeatInfoCollection?.mergeMetaData(
                valueCmd = RoomSeatCmd.leaveSeatCmd.name,
                value = mapOf(
                    seatIndex.toString() to mapOf(
                        "owner" to GsonTools.beanToMap(AUIUserThumbnailInfo())
                    )
                ),
                callback = { collectionException ->
                    if (collectionException == null) {
                        KTVLogger.d(TAG, "outSeat success roomId:$mCurRoomNo, seatInfo:$targetSeatInfo")
                        runOnMainThread {
                            completion.invoke(collectionException)
                        }
                    } else {
                        KTVLogger.e(TAG, "outSeat failed roomId:$mCurRoomNo, $collectionException")
                        runOnMainThread {
                            completion.invoke(Exception("${collectionException.message}(${collectionException.code})"))
                        }
                    }
                })
        }
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
        scene.userService.muteUserAudio(mute, callback = { uiException ->
            if (uiException == null) {
                KTVLogger.d(TAG, "muteUserAudio success mute:$mute")
                runOnMainThread {
                    completion.invoke(null)
                }
            } else {
                KTVLogger.e(TAG, "muteUserAudio failed mute:$mute $uiException")
                runOnMainThread {
                    completion.invoke(Exception("${uiException.message}(${uiException.code})"))
                }
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
        scene.userService.muteUserVideo(mute, callback = { uiException ->
            if (uiException == null) {
                KTVLogger.d(TAG, "muteUserVideo success mute:$mute")
                runOnMainThread {
                    completion.invoke(null)
                }
            } else {
                KTVLogger.e(TAG, "muteUserVideo failed mute:$mute $uiException")
                runOnMainThread {
                    completion.invoke(Exception("${uiException.message}(${uiException.code})"))
                }
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
     * Get chosen songs list
     *
     * @param completion
     * @receiver
     */
    override fun getChosenSongList(completion: (error: Exception?, list: List<RoomSongInfo>?) -> Unit) {
        mChosenSongCollection?.getMetaData { collectionException, value ->
            if (collectionException != null) {
                KTVLogger.e(TAG, "getChosenSongList roomId:$mCurRoomNo $collectionException $value")
                runOnMainThread {
                    completion.invoke(Exception("${collectionException.message}(${collectionException.code})"), null)
                }
                return@getMetaData
            }
            try {
                val out = GsonTools.toList(GsonTools.beanToString(value), RoomSongInfo::class.java)
                KTVLogger.d(TAG, "getChosenSongList onSuccess roomId:$mCurRoomNo $out")
                out?.let {
                    mSongChosenList.clear()
                    mSongChosenList.addAll(it)
                }
                runOnMainThread {
                    completion.invoke(null, mSongChosenList)
                }

            } catch (e: Exception) {
                KTVLogger.e(TAG, "getChosenSongList onFail roomId:$mCurRoomNo $e")
                runOnMainThread {
                    completion.invoke(e, null)
                }
            }
        }
    }

    /**
     * Choose song
     *
     * @param songInfo
     * @param completion
     * @receiver
     */
    override fun chooseSong(songInfo: RoomSongInfo, completion: (error: Exception?) -> Unit) {
        val chosenSong = songInfo.copy(
            owner = mCurrentUser,
            status = PlayStatus.idle,
            createAt = mCurrentServerTs,
            pinAt = 0.0
        )
        mChosenSongCollection?.addMetaData(
            valueCmd = RoomSongCmd.chooseSongCmd.name,
            value = GsonTools.beanToMap(chosenSong),
            filter = null,
            callback = { collectionException ->
                if (collectionException == null) {
                    mSongChosenList.add(chosenSong)
                    KTVLogger.d(TAG, "chooseSong success roomId:$mCurRoomNo, song:$songInfo")
                    runOnMainThread {
                        completion.invoke(null)
                    }
                } else {
                    KTVLogger.e(TAG, "chooseSong failed roomId:$mCurRoomNo, $collectionException")
                    runOnMainThread {
                        completion.invoke(Exception("${collectionException.message}(${collectionException.code})"))
                    }
                }
            })
    }

    /**
     * Pin song
     *
     * @param songCode
     * @param completion
     * @receiver
     */
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

        val targetSong = mSongChosenList.firstOrNull { it.songNo == songCode } ?: run {
            completion.invoke(Exception("The song no not found!"))
            return
        }

        val newSong = targetSong.copy(pinAt = mCurrentServerTs.toDouble())
        mChosenSongCollection?.mergeMetaData(
            valueCmd = RoomSongCmd.pingSongCmd.name,
            value = mapOf("pinAt" to newSong.pinAt),
            filter = listOf(
                mapOf("songNo" to songCode, "userId" to mCurrentUser.userId)
            ),
            callback = { collectionException ->
                if (collectionException == null) {
                    KTVLogger.d(TAG, "pinSong success roomId:$mCurRoomNo, song:$newSong")
                    runOnMainThread {
                        completion.invoke(null)
                    }
                } else {
                    KTVLogger.e(TAG, "pinSong failed roomId:$mCurRoomNo, $collectionException")
                    runOnMainThread {
                        completion.invoke(Exception("${collectionException.message}(${collectionException.code})"))
                    }
                }
            })
    }

    /**
     * Make song did play
     *
     * @param songCode
     * @param completion
     * @receiver
     */
    override fun makeSongDidPlay(songCode: String, completion: (error: Exception?) -> Unit) {
        if (mSongChosenList.size <= 0) {
            completion.invoke(Exception("The chosen song list is empty!"))
            return
        }
        val targetSong = mSongChosenList.firstOrNull { it.songNo == songCode } ?: run {
            completion.invoke(Exception("The song no not found!"))
            return
        }
        if (targetSong.status == PlayStatus.playing) {
            completion.invoke(Exception("The song is playing!"))
            return
        }

        val newSong = targetSong.copy(status = PlayStatus.playing)
        mChosenSongCollection?.mergeMetaData(
            valueCmd = RoomSongCmd.updatePlayStatusCmd.name,
            value = mapOf("status" to newSong.status),
            filter = listOf(
                mapOf("songNo" to songCode, "userId" to mCurrentUser.userId)
            ),
            callback = { collectionException ->
                if (collectionException == null) {
                    KTVLogger.d(TAG, "makeSongDidPlay success roomId:$mCurRoomNo, song:$newSong")
                    runOnMainThread {
                        completion.invoke(null)
                    }
                } else {
                    KTVLogger.e(TAG, "makeSongDidPlay failed roomId:$mCurRoomNo, $collectionException")
                    runOnMainThread {
                        completion.invoke(Exception("${collectionException.message}(${collectionException.code})"))
                    }
                }
            })
    }

    override fun removeSong(songCode: String, completion: (error: Exception?) -> Unit) {
        if (mSongChosenList.size <= 0) {
            completion.invoke(Exception("The chosen song list is empty!"))
            return
        }
        val targetSong = mSongChosenList.firstOrNull { it.songNo == songCode } ?: run {
            completion.invoke(Exception("The song no not found!"))
            return
        }
        mChosenSongCollection?.removeMetaData(
            valueCmd = RoomSongCmd.removeSongCmd.name,
            filter = listOf(mapOf("songNo" to songCode)),
            callback = { collectionException ->
                if (collectionException == null) {
                    KTVLogger.d(TAG, "removeSong success roomId:$mCurRoomNo, song:$targetSong")
                    runOnMainThread {
                        completion.invoke(null)
                    }
                } else {
                    KTVLogger.e(TAG, "removeSong failed roomId:$mCurRoomNo, $collectionException")
                    runOnMainThread {
                        completion.invoke(Exception("${collectionException.message}(${collectionException.code})"))
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
        val choristerInfo = RoomChoristerInfo(mCurrentUser.userId, songCode)
        mChoristerInfoCollection?.addMetaData(
            valueCmd = RoomChoristerCmd.joinChorusCmd.name,
            value = GsonTools.beanToMap(choristerInfo),
            callback = { collectionException ->
                if (collectionException == null) {
                    KTVLogger.d(TAG, "joinChorus success roomId:$mCurRoomNo, choristerInfo:$choristerInfo")
                    runOnMainThread {
                        completion.invoke(null)
                    }
                } else {
                    KTVLogger.e(TAG, "joinChorus failed roomId:$mCurRoomNo,  $collectionException")
                    runOnMainThread {
                        completion.invoke(Exception("${collectionException.message}(${collectionException.code})"))
                    }
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
        mChoristerInfoCollection?.removeMetaData(
            valueCmd = RoomChoristerCmd.leaveChorusCmd.name,
            filter = listOf(mapOf("userId" to mCurrentUser.userId)),
            callback = { collectionException ->
                if (collectionException == null) {
                    KTVLogger.d(TAG, "leaveChorus success roomId:$mCurRoomNo")
                    runOnMainThread {
                        completion.invoke(null)
                    }
                } else {
                    KTVLogger.e(TAG, "leaveChorus failed roomId:$mCurRoomNo, $collectionException")
                    runOnMainThread {
                        completion.invoke(Exception("${collectionException.message}(${collectionException.code})"))
                    }
                }
            })
    }

    /**
     * Get chorister list
     *
     * @param completion
     * @receiver
     */
    override fun getChoristerList(completion: (error: Exception?, choristerList: List<RoomChoristerInfo>?) -> Unit) {
        mChoristerInfoCollection?.getMetaData { collectionException, value ->
            if (collectionException != null) {
                KTVLogger.e(TAG, "getChoristerList failed roomId:$mCurRoomNo $collectionException")
                runOnMainThread {
                    completion.invoke(
                        Exception("${collectionException.message}(${collectionException.code})"), emptyList()
                    )
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
                    completion.invoke(null, choristerList)
                }
            } catch (e: Exception) {
                KTVLogger.e(TAG, "getChoristerList onFail roomId:$mCurRoomNo $e")
                runOnMainThread {
                    completion.invoke(e, null)
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
        mObservableHelper.subscribeEvent(listener)
        if (mUserList.isNotEmpty()) {
            listener.onUserListDidChanged(mUserList)
        }
        if (mSongChosenList.isNotEmpty()) {
            listener.onChosenSongListDidChanged(mSongChosenList)
        }
        if (mChoristerList.isNotEmpty()) {
            listener.onChoristerListDidChanged(mChoristerList)
        }
        if (mSeatMap.isNotEmpty()) {
            listener.onMicSeatSnapshot(mSeatMap)
        }
    }

    /**
     * Unsubscribe listener
     *
     * @param listener
     */
    override fun unsubscribeListener(listener: KtvServiceListenerProtocol) {
        mObservableHelper.unSubscribeEvent(listener)
    }

    override fun getSeatMap(): Map<Int, RoomMicSeatInfo> {
        return mSeatMap
    }

    override fun getChoristerList(): List<RoomChoristerInfo> {
        return mChoristerList
    }

    override fun getChosenSongList(): List<RoomSongInfo> {
        return mSongChosenList
    }

    /**
     * On will init scene metadata
     *
     * @param channelName
     * @return
     */
    override fun onWillInitSceneMetadata(channelName: String): Map<String, Any> {
        val seatMap = mutableMapOf<String, Any>()
        for (i in 0 until 8) {
            val seat = RoomMicSeatInfo().apply {
                seatIndex = i
                if (i == 0) {
                    owner = AUIRoomContext.shared().currentUserInfo
                    seatStatus = RoomMicSeatStatus.used
                } else {
                    owner = AUIUserThumbnailInfo()
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
            val token = rtmToken ?: run {
                KTVLogger.e(TAG, "onTokenPrivilegeWillExpire, with renewRtmToken failed: $exception")
                return@renewRtmToken
            }
            mSyncManager.login(token, completion = { rtmException ->
                if (rtmException == null) {
                    KTVLogger.d(TAG, "onTokenPrivilegeWillExpire, with loginRtm success")
                } else {
                    KTVLogger.e(TAG, "onTokenPrivilegeWillExpire, with loginRtm failed: $rtmException")
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
            mObservableHelper.notifyEventHandlers { delegate ->
                delegate.onRoomExpire(channelName)
            }
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
            mObservableHelper.notifyEventHandlers { delegate ->
                delegate.onRoomDestroy(channelName)
            }
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


    /**
     * On room user snapshot
     *
     * @param roomId
     * @param userList
     *
     */
    override fun onRoomUserSnapshot(roomId: String, userList: MutableList<AUIUserInfo>?) {
        KTVLogger.d(TAG, "onRoomUserSnapshot, roomId:$roomId, userList:${userList?.count()}")
        if (mCurRoomNo != roomId) {
            return
        }
        userList?.let {
            this.mUserList.clear()
            this.mUserList.addAll(it)
        }
        mObservableHelper.notifyEventHandlers { delegate ->
            delegate.onUserListDidChanged(mUserList)
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
        if (mCurRoomNo != roomId) {
            return
        }
        mUserList.removeIf { it.userId == userInfo.userId }
        mUserList.add(userInfo)
        mObservableHelper.notifyEventHandlers { delegate ->
            delegate.onUserListDidChanged(mUserList)
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId) ?: return
        if (mCurrentUser.userId == cacheRoom.roomOwner?.userId) {
            cacheRoom.customPayload[KTVParameters.ROOM_USER_COUNT] = mUserList.count()
            mRoomManager.updateRoomInfo(mAppId, kSceneId, cacheRoom, callback = { auiException, roomInfo ->
                if (auiException == null) {
                    KTVLogger.d(TAG, "onRoomUserEnter updateRoom success: $mCurRoomNo, $roomInfo")
                } else {
                    KTVLogger.e(TAG, "onRoomUserEnter updateRoom failed: $mCurRoomNo $auiException")
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
        if (mCurRoomNo != roomId) {
            return
        }
        if (AUIRoomContext.shared().isRoomOwner(roomId)) {
            innerCleanUserInfo(userInfo.userId)
        }
        mUserList.removeIf { it.userId == userInfo.userId }
        mObservableHelper.notifyEventHandlers { delegate ->
            delegate.onUserListDidChanged(mUserList)
        }
        val cacheRoom = AUIRoomContext.shared().getRoomInfo(roomId) ?: return
        if (mCurrentUser.userId == cacheRoom.roomOwner?.userId) {
            cacheRoom.customPayload[KTVParameters.ROOM_USER_COUNT] = mUserList.count()
            mRoomManager.updateRoomInfo(mAppId, kSceneId, cacheRoom, callback = { auiException, roomInfo ->
                if (auiException == null) {
                    KTVLogger.d(TAG, "onRoomUserLeave updateRoom success: $mCurRoomNo, $roomInfo")
                } else {
                    KTVLogger.d(TAG, "onRoomUserLeave updateRoom failed: $mCurRoomNo $auiException")
                }
            })
        }
    }

    /**
     * Inner clean user info
     *
     * @param userId
     */
    private fun innerCleanUserInfo(userId: String) {
        val seatIndex = mSeatMap.values.firstOrNull { it.owner?.userId == userId }?.seatIndex
        if (seatIndex != null) {
            // 移除麦位信息
            mSeatInfoCollection?.mergeMetaData(
                valueCmd = RoomSeatCmd.leaveSeatCmd.name,
                value = mapOf(
                    seatIndex.toString() to mapOf(
                        "owner" to GsonTools.beanToMap(AUIUserThumbnailInfo())
                    )
                ),
                callback = {
                    if (it == null) {
                        KTVLogger.d(TAG, "innerCleanUserInfo-->removeSeat success, index:$seatIndex")
                    } else {
                        KTVLogger.e(TAG, "innerCleanUserInfo-->removeSeat failed, index:$seatIndex, $it")
                    }
                })
        }
        // 移除点歌信息，
        mChosenSongCollection?.removeMetaData(
            valueCmd = RoomSongCmd.removeSongCmd.name,
            filter = listOf(mapOf("owner" to mapOf("userId" to userId))),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "innerCleanUserInfo-->removeSongByUid success, uid:$userId")
                } else {
                    KTVLogger.e(TAG, "innerCleanUserInfo-->removeSongByUid failed, uid:$userId $it")
                }
            })
        // 移除合唱信息
        mChoristerInfoCollection?.removeMetaData(
            valueCmd = null,
            filter = listOf(mapOf("userId" to userId)),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "innerCleanUserInfo-->leaveChorus success, useId:$userId")
                } else {
                    KTVLogger.e(TAG, "innerCleanUserInfo-->leaveChorus failed, useId:$userId, $it")
                }
            })
    }

    /**
     * On room user update
     *
     * @param roomId
     * @param userInfo
     */
    override fun onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
        KTVLogger.d(TAG, "onRoomUserUpdate, roomId:$roomId, userInfo:$userInfo")
        if (mCurRoomNo != roomId) {
            return
        }
        mUserList.removeIf { it.userId == userInfo.userId }
        mUserList.add(userInfo)
        mObservableHelper.notifyEventHandlers { delegate ->
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
        mObservableHelper.notifyEventHandlers { delegate ->
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
        mObservableHelper.notifyEventHandlers { delegate ->
            delegate.onUserVideoMute(userId, mute)
        }
    }

    /**
     * On attribute changed
     *
     * @param channelId
     * @param key
     * @param value
     */
    private fun onAttributeChanged(channelId: String, key: String, value: AUIAttributesModel) {
        KTVLogger.d(
            TAG,
            "onAttributeChanged, channelId:$channelId, key:$key, list:${value.getList()}, map:${value.getMap()}"
        )
        when (key) {
            kCollectionSeatInfo -> { // 麦位信息
                val seats = value.getMap() ?: GsonTools.toBean(
                    GsonTools.beanToString(value),
                    object : TypeToken<Map<String, Any>>() {}.type
                )
                seats?.values?.forEach {
                    val newSeatInfo =
                        GsonTools.toBean(GsonTools.beanToString(it), RoomMicSeatInfo::class.java) ?: return
                    val index = newSeatInfo.seatIndex
                    val oldSeatInfo = mSeatMap[index]
                    mSeatMap[index] = newSeatInfo
                    val newSeatUserId = newSeatInfo.owner?.userId ?: ""
                    val oldSeatUserId = oldSeatInfo?.owner?.userId ?: ""
                    if (oldSeatUserId.isEmpty() && newSeatUserId.isNotEmpty()) {
                        KTVLogger.d(TAG, "onUserEnterSeat: $newSeatInfo")
                        val newUser = newSeatInfo.owner ?: return
                        mObservableHelper.notifyEventHandlers { delegate ->
                            delegate.onUserEnterSeat(index, newUser)
                        }
                    }
                    if (oldSeatUserId.isNotEmpty() && newSeatUserId.isEmpty()) {
                        KTVLogger.d(TAG, "onUserLeaveSeat: $oldSeatInfo")
                        val originUser = oldSeatInfo?.owner ?: return

                        mObservableHelper.notifyEventHandlers { delegate ->
                            delegate.onUserLeaveSeat(index, originUser)
                        }
                    }
                }
            }

            kCollectionChosenSong -> { // 已选歌单
                val songList = GsonTools.toList(GsonTools.beanToString(value.getList()), RoomSongInfo::class.java)
                KTVLogger.d(TAG, "$kCollectionChosenSong songList: $songList")
                mObservableHelper.notifyEventHandlers { delegate ->
                    delegate.onChosenSongListDidChanged(songList ?: emptyList())
                }
            }

            kCollectionChorusInfo -> { // 合唱信息
                val choristerList =
                    GsonTools.toList(GsonTools.beanToString(value.getList()), RoomChoristerInfo::class.java)

                KTVLogger.d(TAG, "$kCollectionChorusInfo choristerList: $choristerList")
                choristerList?.forEach { newChorister ->
                    var hasChorister = false
                    this.mChoristerList.forEach {
                        if (it.userId == newChorister.userId) {
                            hasChorister = true
                        }
                    }
                    if (!hasChorister) {
                        mObservableHelper.notifyEventHandlers { delegate ->
                            delegate.onChoristerDidEnter(newChorister)
                        }
                    }
                }
                this.mChoristerList.forEach { oldChorister ->
                    var hasChorister = false
                    choristerList?.forEach {
                        if (it.userId == oldChorister.userId) {
                            hasChorister = true
                        }
                    }
                    if (!hasChorister) {
                        mObservableHelper.notifyEventHandlers { delegate ->
                            delegate.onChoristerDidLeave(oldChorister)
                        }
                    }
                }
                this.mChoristerList.clear()
                this.mChoristerList.addAll(choristerList ?: emptyList())
                mObservableHelper.notifyEventHandlers { delegate ->
                    delegate.onChoristerListDidChanged(mChoristerList)
                }
            }
        }
    }

    private fun onSeatWillMerge(
        publisherId: String, valueCmd: String?, newValue: Map<String, Any>, oldValue: Map<String, Any>
    ): AUICollectionException? {
        Log.d(TAG, "onMetadataWillMerge $valueCmd $newValue $oldValue")

        val seatInfoPair = newValue.toList()[0]
        val seatIndex = seatInfoPair.first.toInt()
        val seatInfoMap = seatInfoPair.second as? Map<*, *>
        val userId = (seatInfoMap?.get("owner") as? Map<*, *>)?.get("userId") as? String
        when (valueCmd) {
            RoomSeatCmd.enterSeatCmd.name -> {
                if (mSeatMap.values.any { it.owner?.userId == userId }) {
                    return AUICollectionException.ErrorCode.unknown.toException(msg = "user already in seat")
                }
            }

            RoomSeatCmd.leaveSeatCmd.name -> {
                if (seatIndex == 0) {
                    return AUICollectionException.ErrorCode.unknown.toException(msg = "can't leave seat 0")
                }
                if (mSeatMap[seatIndex]?.owner?.userId != publisherId ||
                    AUIRoomContext.shared().isRoomOwner(mCurRoomNo, publisherId)
                ) {
                    return AUICollectionException.ErrorCode.unknown.toException(msg = "seat not mine")
                }
                innerOnSeatWillLeave(publisherId)
                // 离开麦位后清空该麦位用户已点歌曲
                mChosenSongCollection?.removeMetaData(
                    valueCmd = RoomSongCmd.removeSongCmd.name,
                    filter = listOf(mapOf("userId" to publisherId)),
                    callback = {})
                //  离开麦位后移除该用户合唱
                mChoristerInfoCollection?.removeMetaData(
                    valueCmd = RoomChoristerCmd.leaveChorusCmd.name,
                    filter = listOf(mapOf("userId" to publisherId)),
                    callback = {})
            }
        }
        return null
    }

    /**
     * Inner on seat will leave
     *
     * @param userId
     */
    private fun innerOnSeatWillLeave(userId: String) {
        // 移除点歌信息，
        mChosenSongCollection?.removeMetaData(
            valueCmd = RoomSongCmd.removeSongCmd.name,
            filter = listOf(mapOf("owner" to mapOf("userId" to userId))),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "innerOnSeatWillLeave-->removeSongByUid success, uid:$userId")
                } else {
                    KTVLogger.e(TAG, "innerOnSeatWillLeave-->removeSongByUid failed, uid:$userId $it")
                }
            })
        // 移除合唱信息
        mChoristerInfoCollection?.removeMetaData(
            valueCmd = null,
            filter = listOf(mapOf("userId" to userId)),
            callback = {
                if (it == null) {
                    KTVLogger.d(TAG, "innerOnSeatWillLeave-->removeChorusByUid success, useId:$userId")
                } else {
                    KTVLogger.e(TAG, "innerOnSeatWillLeave-->removeChorusByUid failed, useId:$userId, $it")
                }
            })
    }
}