package io.agora.scene.eCommerce.videoSwitcherAPI

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.TextureView
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.agora.mediaplayer.IMediaPlayer
import io.agora.mediaplayer.IMediaPlayerObserver
import io.agora.mediaplayer.data.PlayerUpdatedInfo
import io.agora.mediaplayer.data.SrcInfo
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcConnection
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineEx
import io.agora.rtc2.video.VideoCanvas
import io.agora.scene.eCommerce.RtcEngineInstance
import io.agora.scene.eCommerce.ShowLogger
import java.util.Collections

/**
 * Room status
 *
 * @constructor Create empty Room status
 */
enum class RoomStatus {
    /**
     * Idle
     *
     * @constructor Create empty Idle
     */
    IDLE,

    /**
     * Prejoined
     *
     * @constructor Create empty Prejoined
     */
    PREJOINED,

    /**
     * Joined
     *
     * @constructor Create empty Joined
     */
    JOINED,
}

/**
 * Video switcher impl
 *
 * @property rtcEngine
 * @constructor Create empty Video switcher impl
 */
class VideoSwitcherImpl constructor(private val rtcEngine: RtcEngineEx) : VideoSwitcher {
    /**
     * Tag
     */
    private val tag = "VideoSwitcherImpl"

    /**
     * Preload count
     */
    private var preloadCount = 3

    /**
     * Connections for preloading
     */
    private val connectionsForPreloading = Collections.synchronizedList(mutableListOf<RtcConnection>())

    /**
     * Connections joined
     */
    private val connectionsJoined = Collections.synchronizedList(mutableListOf<RtcConnection>())

    /**
     * Main handler
     */
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * Room state map
     */
    private val roomStateMap = Collections.synchronizedMap(mutableMapOf<RtcConnectionWrap, RoomStatus>())

    /**
     * Remote video canvas list
     */
    private val remoteVideoCanvasList = Collections.synchronizedList(mutableListOf<RemoteVideoCanvasWrap>())

    /**
     * Quick start time
     */
    private var quickStartTime = 0L

    /**
     * Need subscribe
     */
    private var needSubscribe = false

    /**
     * Need subscribe connection
     */
    private var needSubscribeConnection: RtcConnection? = null

    /**
     * Set preload count
     *
     * @param count
     */
    override fun setPreloadCount(count: Int) {
        preloadCount = count
        ShowLogger.d(tag, "setPreloadCount count=$count")
    }

    /**
     * Preload connections
     *
     * @param connections
     */
    override fun preloadConnections(connections: List<RtcConnection>) {
        connectionsForPreloading.clear()
        connectionsForPreloading.addAll(connections.map { it })
        connections.forEach {
            rtcEngine.preloadChannel(RtcEngineInstance.generalToken(), it.channelId, it.localUid)
            var hasConnection = false
            roomStateMap.forEach { room ->
                if (room.key.isSameChannel(it)) {
                    hasConnection = true
                }
            }
            if (!hasConnection) {
                roomStateMap[RtcConnectionWrap(it)] = RoomStatus.IDLE
            }
        }
        ShowLogger.d(tag, "preloadConnections connections=$connectionsForPreloading")
    }

    /**
     * Unload connections
     *
     */
    override fun unloadConnections() {
        mainHandler.removeCallbacksAndMessages(null)

        roomStateMap.forEach {
            switchRoomState(RoomStatus.IDLE, it.key, null, null, null)
        }
        connectionsJoined.clear()
        roomStateMap.clear()
    }

    /**
     * Pre join channel
     *
     * @param connection
     * @param mediaOptions
     * @param token
     * @param eventListener
     */
    override fun preJoinChannel(
        connection: RtcConnection,
        mediaOptions: ChannelMediaOptions,
        token: String?,
        eventListener: VideoSwitcher.IChannelEventListener?
    ) {
        switchRoomState(RoomStatus.PREJOINED, connection, token, eventListener, mediaOptions)
    }

    /**
     * Pre join channel
     *
     * @param connection
     */
    override fun preJoinChannel(connection: RtcConnection) {
        connectionsJoined.add(connection)
        preloadChannels()
    }

    /**
     * Join channel
     *
     * @param connection
     * @param mediaOptions
     * @param token
     * @param eventListener
     * @param needPreJoin
     */
    override fun joinChannel(
        connection: RtcConnection,
        mediaOptions: ChannelMediaOptions,
        token: String?,
        eventListener: VideoSwitcher.IChannelEventListener?,
        needPreJoin: Boolean?
    ) {
        switchRoomState(RoomStatus.JOINED, connection, token, eventListener, mediaOptions)
    }

    /**
     * Set channel event
     *
     * @param channelName
     * @param uid
     * @param eventHandler
     */
    override fun setChannelEvent(
        channelName: String,
        uid: Int,
        eventHandler: VideoSwitcher.IChannelEventListener?
    ) {
        roomStateMap.forEach {
            if (it.key.isSameChannel(RtcConnection(channelName, uid))) {
                it.key.rtcEventHandler?.setEventListener(eventHandler)
            }
        }
    }

    /**
     * Preload channels
     *
     */
    private fun preloadChannels() {
        val size = connectionsForPreloading.size
        val index =
            connectionsForPreloading.indexOfFirst { it.channelId == connectionsJoined.firstOrNull()?.channelId }
        ShowLogger.d(tag, "switchRoomState, index: $index")

        // joined房间的上下两个房间
        val connPreLoaded = mutableListOf<RtcConnection>()
        for (i in (index - (preloadCount - 1) / 2)..(index + preloadCount / 2)) {
            if (i == index) {
                continue
            }
            // workaround
            if (size == 0) {
                return
            }
            val realIndex = (if (i < 0) size + i else i) % size
            if (realIndex < 0 || realIndex >= size) {
                continue
            }
            val conn = connectionsForPreloading[realIndex]
            if (connectionsJoined.any { it.channelId == conn.channelId }) {
                continue
            }
            if (getRoomState(conn.channelId, conn.localUid) != RoomStatus.PREJOINED) {
                switchRoomState(RoomStatus.PREJOINED, conn, RtcEngineInstance.generalToken(), null, null)
            }
            connPreLoaded.add(conn)
        }

        // 非prejoin房间需要退出频道
        roomStateMap.forEach { room ->
            if (room.value == RoomStatus.PREJOINED && connPreLoaded.none {room.key.channelId == it.channelId}) {
                switchRoomState(RoomStatus.IDLE, room.key, RtcEngineInstance.generalToken(), null, null)
            }
        }
    }

    /**
     * Leave channel
     *
     * @param connection
     * @param force
     * @return
     */
    override fun leaveChannel(connection: RtcConnection, force: Boolean): Boolean {
        connectionsJoined.removeIf { it.channelId == connection.channelId }
        if (force) {
            switchRoomState(RoomStatus.IDLE, connection, null, null, null)
        } else {
            switchRoomState(RoomStatus.PREJOINED, connection, RtcEngineInstance.generalToken(), null, null)
        }
        return true
    }

    /**
     * Setup remote video
     *
     * @param connection
     * @param container
     */
    override fun setupRemoteVideo(
        connection: RtcConnection,
        container: VideoSwitcher.VideoCanvasContainer
    ) {
        remoteVideoCanvasList.firstOrNull {
            it.connection.channelId == connection.channelId && it.uid == container.uid && it.renderMode == container.renderMode && it.lifecycleOwner == container.lifecycleOwner
        }?.let {
            val videoView = it.view
            val viewIndex = container.container.indexOfChild(videoView)

            if (viewIndex == container.viewIndex) {
                if (it.connection.rtcEventHandler?.isJoinChannelSuccess == true) {
                    ShowLogger.d("hugo", "setupRemoteVideoEx111")
                    rtcEngine.setupRemoteVideoEx(
                        it,
                        it.connection
                    )
                }
                return
            }
            it.release()
        }

        var videoView = container.container.getChildAt(container.viewIndex)
        if (videoView !is TextureView) {
            ShowLogger.d("hugo", "setupRemoteVideoEx2221")
            videoView = TextureView(container.container.context)
            container.container.addView(videoView, container.viewIndex)
        } else {
            ShowLogger.d("hugo", "setupRemoteVideoEx2222")
            container.container.removeViewInLayout(videoView)
            videoView = TextureView(container.container.context)
            container.container.addView(videoView, container.viewIndex)
        }

        roomStateMap.forEach {
            if (it.key.isSameChannel(connection)) {
                val connectionWrap = it.key
                val remoteVideoCanvasWrap = RemoteVideoCanvasWrap(
                    connectionWrap,
                    container.lifecycleOwner,
                    videoView,
                    container.renderMode,
                    container.uid
                )
                //if (connectionWrap.rtcEventHandler?.isJoinChannelSuccess == true) {
                ShowLogger.d("hugo", "setupRemoteVideoEx222")
                rtcEngine.setupRemoteVideoEx(
                    remoteVideoCanvasWrap,
                    connectionWrap
                )
                //}
                return
            }
        }

        val connectionWrap = RtcConnectionWrap(connection)
        val remoteVideoCanvasWrap = RemoteVideoCanvasWrap(
            connectionWrap,
            container.lifecycleOwner,
            videoView,
            container.renderMode,
            container.uid
        )
        //if (connectionWrap.rtcEventHandler?.isJoinChannelSuccess == true) {
        ShowLogger.d("hugo", "setupRemoteVideoEx333")
        rtcEngine.setupRemoteVideoEx(
            remoteVideoCanvasWrap,
            connectionWrap
        )
        //}
    }

    /**
     * Get first video frame time
     *
     * @return
     */
    override fun getFirstVideoFrameTime(): Long {
        return quickStartTime
    }

    /**
     * Start audio mixing
     *
     * @param connection
     * @param filePath
     * @param loopbackOnly
     * @param cycle
     */
    override fun startAudioMixing(
        connection: RtcConnection,
        filePath: String,
        loopbackOnly: Boolean,
        cycle: Int
    ) {
        // 判断connetion是否加入了频道，即connectionsJoined是否包含，不包含则直接返回
        roomStateMap.forEach {
            if (it.key.isSameChannel(connection) && it.value == RoomStatus.JOINED) {
                val connectionWrap = it.key
                // 播放使用MPK，rtcEngine.createMediaPlayer
                // 使用一个Map缓存起来key:RtcConnection, value:MediaPlayer
                // 从缓存里取MediaPlayer，如不存在则重新创建
                // val mediaPlayer = rtcEngine.createMediaPlayer()
                val mediaPlayer = connectionWrap.audioMixingPlayer ?: rtcEngine.createMediaPlayer().apply {
                    registerPlayerObserver(object : IMediaPlayerObserver {
                        override fun onPlayerStateChanged(
                            state: io.agora.mediaplayer.Constants.MediaPlayerState?,
                            error: io.agora.mediaplayer.Constants.MediaPlayerError?
                        ) {
                            if(error == io.agora.mediaplayer.Constants.MediaPlayerError.PLAYER_ERROR_NONE){
                                if(state == io.agora.mediaplayer.Constants.MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED){
                                    play()
                                }
                            }
                        }

                        override fun onPositionChanged(position_ms: Long, timestamp_ms: Long) {

                        }

                        override fun onPlayerEvent(
                            eventCode: io.agora.mediaplayer.Constants.MediaPlayerEvent?,
                            elapsedTime: Long,
                            message: String?
                        ) {

                        }

                        override fun onMetaData(
                            type: io.agora.mediaplayer.Constants.MediaPlayerMetadataType?,
                            data: ByteArray?
                        ) {

                        }

                        override fun onPlayBufferUpdated(playCachedBuffer: Long) {

                        }

                        override fun onPreloadEvent(
                            src: String?,
                            event: io.agora.mediaplayer.Constants.MediaPlayerPreloadEvent?
                        ) {

                        }

                        override fun onAgoraCDNTokenWillExpire() {

                        }

                        override fun onPlayerSrcInfoChanged(from: SrcInfo?, to: SrcInfo?) {

                        }

                        override fun onPlayerInfoUpdated(info: PlayerUpdatedInfo?) {

                        }

                        override fun onAudioVolumeIndication(volume: Int) {

                        }
                    })
                }
                connectionWrap.audioMixingPlayer = mediaPlayer
                mediaPlayer.stop()
                mediaPlayer.open(filePath, 0)
                mediaPlayer.setLoopCount(if (cycle >= 0) 0 else Int.MAX_VALUE)

                // 开始推流，使用updateChannelMediaOptionEx
                // 使用一个Map缓存ChannelMediaOptions--key:RtcConnection, value:ChannelMediaOptions
                // val channelMediaOptions = ChannelMediaOptions()
                // channelMediaOptions.publishMediaPlayerId = mediaPlayer.getId()
                // channelMediaOptions.publishMediaPlayerAudioTrack = true
                // rtcEngine.updateChannelMediaOptionsEx(channelMediaOptions, connection)
                if(!loopbackOnly){
                    val mediaOptions = connectionWrap.mediaOptions
                    mediaOptions.publishMediaPlayerId = mediaPlayer.mediaPlayerId
                    // 没开启麦克风权限情况下，publishMediaPlayerAudioTrack = true 会自动停止音频播放
                    mediaOptions.publishMediaPlayerAudioTrack = true
                    rtcEngine.updateChannelMediaOptionsEx(mediaOptions, connectionWrap)
                }
            }
        }
    }

    /**
     * Stop audio mixing
     *
     * @param connection
     */
    override fun stopAudioMixing(connection: RtcConnection) {
        // 判断connetion是否加入了频道，即connectionsJoined是否包含，不包含则直接返回
        roomStateMap.forEach {
            if (it.key.isSameChannel(connection) && it.value == RoomStatus.JOINED) {
                val connectionWrap =
                    it.key

                // 停止播放，拿到connection对应的MediaPlayer并停止释放
                connectionWrap.audioMixingPlayer?.stop()

                // 停止推流，使用updateChannelMediaOptionEx
                val mediaOptions = connectionWrap.mediaOptions
                if (mediaOptions.isPublishMediaPlayerAudioTrack) {
                    mediaOptions.publishMediaPlayerAudioTrack = false
                    rtcEngine.updateChannelMediaOptionsEx(mediaOptions, connectionWrap)
                }
            }
        }
    }

    /**
     * Adjust audio mixing volume
     *
     * @param connection
     * @param volume
     */
    override fun adjustAudioMixingVolume(connection: RtcConnection, volume: Int) {
        roomStateMap.forEach {
            if (it.key.isSameChannel(connection) && it.value == RoomStatus.JOINED) {
                val connectionWrap =
                    it.key
                connectionWrap.audioMixingPlayer?.adjustPlayoutVolume(volume)
                connectionWrap.audioMixingPlayer?.adjustPublishSignalVolume(volume)
            }
        }
    }

    /**
     * Switch room state
     *
     * @param newState
     * @param connection
     * @param token
     * @param eventListener
     * @param mediaOptions
     */
    private fun switchRoomState(
        newState: RoomStatus,
        connection: RtcConnection,
        token: String?,
        eventListener: VideoSwitcher.IChannelEventListener?,
        mediaOptions: ChannelMediaOptions?) {
        ShowLogger.d(tag, "switchRoomState, newState: $newState, connection: $connection, roomStateMap: $roomStateMap")
        // roomStateMap 无当前房间记录
        if (roomStateMap.none {it.key.isSameChannel(connection)}) {
            val rtcConnectionWrap = RtcConnectionWrap(connection)
            val eventHandler = RtcEngineEventHandlerImpl(SystemClock.elapsedRealtime(), connection)
            eventHandler.setEventListener(eventListener)
            rtcConnectionWrap.rtcEventHandler = eventHandler
            if (newState == RoomStatus.PREJOINED) {
                // 加入频道但不收流
                val options = mediaOptions ?: ChannelMediaOptions().apply {
                    clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                    audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                    autoSubscribeVideo = false
                    autoSubscribeAudio = false
                }
                val ret = rtcEngine.joinChannelEx(token, connection, options, rtcConnectionWrap.rtcEventHandler)
                ShowLogger.d(tag, "joinChannelEx2, connection:$connection, ret:$ret")
            } else if (newState == RoomStatus.JOINED) {
                // 加入频道且收流
                val options = mediaOptions ?: ChannelMediaOptions().apply {
                    // 加入频道且收流
                    autoSubscribeVideo = true
                    autoSubscribeAudio = true
                }
                val ret = rtcEngine.joinChannelEx(token, connection, options, rtcConnectionWrap.rtcEventHandler)
                ShowLogger.d(tag, "joinChannelEx3, connection:$connection, ret:$ret")
            }
            roomStateMap[rtcConnectionWrap] = newState
            return
        }

        roomStateMap.forEach {
            if (it.key.isSameChannel(connection)) {
                val oldState = it.value
                if (oldState == newState) {
                    if (eventListener != null) {
                        it.key.rtcEventHandler?.subscribeMediaTime = SystemClock.elapsedRealtime()
                        it.key.rtcEventHandler?.setEventListener(eventListener)
                    }
                    ShowLogger.d(tag, "switchRoomState is already this state")
                    return
                }
                roomStateMap[it.key] = newState
                when {
                    oldState == RoomStatus.IDLE && newState == RoomStatus.PREJOINED -> {
                        val eventHandler = RtcEngineEventHandlerImpl(SystemClock.elapsedRealtime(), connection)
                        eventHandler.setEventListener(eventListener)
                        it.key.rtcEventHandler = eventHandler

                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            autoSubscribeVideo = false
                            autoSubscribeAudio = false
                        }
                        val ret = rtcEngine.joinChannelEx(token, connection, options, it.key.rtcEventHandler)
                        ShowLogger.d(tag, "joinChannelEx0, connection:$connection, ret:$ret")
                    }
                    oldState == RoomStatus.PREJOINED && newState == RoomStatus.JOINED -> {
                        it.key.rtcEventHandler?.subscribeMediaTime = SystemClock.elapsedRealtime()
                        it.key.rtcEventHandler?.setEventListener(eventListener)
                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            autoSubscribeVideo = true
                            autoSubscribeAudio = true
                        }
                        val ret = rtcEngine.updateChannelMediaOptionsEx(options, connection)
                        if (ret == -8) {
                            needSubscribe = true
                            needSubscribeConnection = connection
                        }
                        ShowLogger.d(tag, "updateChannelMediaOptionsEx, connection:$connection, ret:$ret")
                    }
                    oldState == RoomStatus.JOINED && newState == RoomStatus.PREJOINED -> {
                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            autoSubscribeVideo = false
                            autoSubscribeAudio = false
                        }
                        val ret = rtcEngine.updateChannelMediaOptionsEx(options, connection)
                        it.key.audioMixingPlayer?.stop()
                        ShowLogger.d(tag, "updateChannelMediaOptionsEx, connection:$connection, ret:$ret")
                    }
                    oldState == RoomStatus.IDLE && newState == RoomStatus.JOINED -> {
                        val eventHandler = RtcEngineEventHandlerImpl(SystemClock.elapsedRealtime(), connection)
                        eventHandler.setEventListener(eventListener)
                        it.key.rtcEventHandler = eventHandler

                        val options = mediaOptions ?: ChannelMediaOptions().apply {
                            autoSubscribeVideo = true
                            autoSubscribeAudio = true
                        }
                        // TODO eventHandler
                        val ret = rtcEngine.joinChannelEx(token, connection, options, eventHandler)
                        ShowLogger.d(tag, "joinChannelEx1, connection:$connection, ret:$ret")
                    }
                    newState == RoomStatus.IDLE -> {
                        leaveRtcChannel(it.key)
                    }
                }
                return
            }
        }
    }

    /**
     * Get room state
     *
     * @param channelName
     * @param uid
     * @return
     */
    private fun getRoomState(channelName: String, uid: Int): RoomStatus? {
        roomStateMap.forEach {
            if (it.key.isSameChannel(RtcConnection(channelName, uid))) {
                return it.value
            }
        }
        return null
    }

    /**
     * Leave rtc channel
     *
     * @param connection
     */
    private fun leaveRtcChannel(connection: RtcConnectionWrap) {
        val ret = rtcEngine.leaveChannelEx(connection)
        ShowLogger.d(
            tag,
            "leaveChannel ret : connection=$connection, code=$ret, message=${RtcEngine.getErrorDescription(ret)}"
        )
        connection.audioMixingPlayer?.stop()
        connection.audioMixingPlayer?.destroy()
        connection.audioMixingPlayer = null
        remoteVideoCanvasList.filter { it.connection.isSameChannel(connection) }.forEach { it.release() }
    }

    /**
     * Rtc engine event handler impl
     *
     * @property joinChannelTime
     * @property connection
     * @constructor Create empty Rtc engine event handler impl
     */
    inner class RtcEngineEventHandlerImpl constructor(
        private val joinChannelTime: Long,
        private val connection: RtcConnection,
    ) : IRtcEngineEventHandler() {

        /**
         * First remote uid
         */
        private var firstRemoteUid: Int = 0

        /**
         * Is join channel success
         */
        var isJoinChannelSuccess = false

        /**
         * Event listener
         */
        private var eventListener: VideoSwitcher.IChannelEventListener? = null

        /**
         * Subscribe media time
         */
        var subscribeMediaTime: Long = joinChannelTime

        /**
         * Set event listener
         *
         * @param listener
         */
        fun setEventListener(listener: VideoSwitcher.IChannelEventListener?) {
            eventListener = listener
            if (isJoinChannelSuccess) {
                eventListener?.onChannelJoined?.invoke(connection)
            }
            if (firstRemoteUid != 0) {
                eventListener?.onUserJoined?.invoke(firstRemoteUid)
            }
        }

        /**
         * On error
         *
         * @param err
         */
        override fun onError(err: Int) {
            super.onError(err)
            ShowLogger.e(
                tag,
                message = "channel ${connection.channelId} error : code=$err, message=${
                    RtcEngine.getErrorDescription(err)
                }"
            )
        }

        /**
         * On join channel success
         *
         * @param channel
         * @param uid
         * @param elapsed
         */
        override fun onJoinChannelSuccess(
            channel: String?,
            uid: Int,
            elapsed: Int
        ) {
            super.onJoinChannelSuccess(channel, uid, elapsed)
            ShowLogger.d(tag, "onJoinChannelSuccess, needSubscribe:$needSubscribe, needSubscribeConnection:$needSubscribeConnection")
            if (needSubscribe && needSubscribeConnection!= null && channel == needSubscribeConnection?.channelId) {
                needSubscribe = false
                needSubscribeConnection = null
                runOnUiThread {
                    val options = ChannelMediaOptions()
                    options.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                    options.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                    options.autoSubscribeVideo = true
                    options.autoSubscribeAudio = true
                    val ret = rtcEngine.updateChannelMediaOptionsEx(options, needSubscribeConnection)
                    ShowLogger.d(tag, "updateChannelMediaOptionsEx2, channel:$channel, ret:$ret")
                }
            }


            isJoinChannelSuccess = true
            eventListener?.onChannelJoined?.invoke(connection)
            ShowLogger.d(
                tag,
                "join channel $channel success cost time : ${SystemClock.elapsedRealtime() - joinChannelTime} ms"
            )
        }

        /**
         * On leave channel
         *
         * @param stats
         */
        override fun onLeaveChannel(stats: RtcStats?) {
            super.onLeaveChannel(stats)
            ShowLogger.d(
                tag,
                "leave channel ${connection.channelId} success"
            )
            isJoinChannelSuccess = false
        }

        /**
         * On first remote video frame
         *
         * @param uid
         * @param width
         * @param height
         * @param elapsed
         */
        override fun onFirstRemoteVideoFrame(
            uid: Int,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            super.onFirstRemoteVideoFrame(uid, width, height, elapsed)
            ShowLogger.d(
                tag,
                "$uid first remote video frame cost time : ${SystemClock.elapsedRealtime() - joinChannelTime} ms"
            )
            eventListener?.onFirstRemoteVideoFrame?.invoke(uid, width, height, elapsed)
        }

        /**
         * On first local video frame
         *
         * @param source
         * @param width
         * @param height
         * @param elapsed
         */
        override fun onFirstLocalVideoFrame(
            source: Constants.VideoSourceType?,
            width: Int,
            height: Int,
            elapsed: Int
        ) {
            super.onFirstLocalVideoFrame(source, width, height, elapsed)
            ShowLogger.d(
                tag,
                "$source first local video frame cost time : ${SystemClock.elapsedRealtime() - joinChannelTime} ms"
            )
        }

        /**
         * On user joined
         *
         * @param uid
         * @param elapsed
         */
        override fun onUserJoined(uid: Int, elapsed: Int) {
            super.onUserJoined(uid, elapsed)
            if (firstRemoteUid == 0) {
                firstRemoteUid = uid
            }
            eventListener?.onUserJoined?.invoke(uid)
        }

        /**
         * On user offline
         *
         * @param uid
         * @param reason
         */
        override fun onUserOffline(uid: Int, reason: Int) {
            super.onUserOffline(uid, reason)
            if (uid == firstRemoteUid) {
                firstRemoteUid = 0
            }
            eventListener?.onUserOffline?.invoke(uid)
        }

        /**
         * On local video state changed
         *
         * @param source
         * @param state
         * @param error
         */
        override fun onLocalVideoStateChanged(
            source: Constants.VideoSourceType?,
            state: Int,
            error: Int
        ) {
            super.onLocalVideoStateChanged(source, state, error)
            eventListener?.onLocalVideoStateChanged?.invoke(state)
        }

        /**
         * On remote video state changed
         *
         * @param uid
         * @param state
         * @param reason
         * @param elapsed
         */
        override fun onRemoteVideoStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
            super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
            if (state == Constants.REMOTE_VIDEO_STATE_PLAYING
                && (reason == Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED || reason == Constants.REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED)
            ) {
                val durationFromSubscribe = SystemClock.elapsedRealtime() - subscribeMediaTime
                val durationFromJoiningRoom = SystemClock.elapsedRealtime() - joinChannelTime
                quickStartTime = durationFromSubscribe
                ShowLogger.d(
                    tag,
                    "video cost time : channel=${connection.channelId}, uid=$uid, durationFromJoiningRoom=$durationFromJoiningRoom, durationFromSubscribe=$durationFromSubscribe "
                )
            }
            eventListener?.onRemoteVideoStateChanged?.invoke(uid, state)
        }

        /**
         * On rtc stats
         *
         * @param stats
         */
        override fun onRtcStats(stats: RtcStats?) {
            super.onRtcStats(stats)
            stats ?: return
            eventListener?.onRtcStats?.invoke(stats)
        }

        /**
         * On local video stats
         *
         * @param source
         * @param stats
         */
        override fun onLocalVideoStats(
            source: Constants.VideoSourceType?,
            stats: LocalVideoStats?
        ) {
            super.onLocalVideoStats(source, stats)
            stats ?: return
            //ShowLogger.d(tag, "onLocalVideoStats, dualStreamEnabled:${stats.dualStreamEnabled},
            // captureFrameWidth:${stats.captureFrameWidth}, captureFrameHeight:${stats.captureFrameHeight},
            // codecType:${stats.codecType}, hwEncoderAccelerating:${stats.hwEncoderAccelerating}")
            eventListener?.onLocalVideoStats?.invoke(stats)
        }

        /**
         * On remote video stats
         *
         * @param stats
         */
        override fun onRemoteVideoStats(stats: RemoteVideoStats?) {
            super.onRemoteVideoStats(stats)
            stats ?: return
            eventListener?.onRemoteVideoStats?.invoke(stats)
        }

        /**
         * On local audio stats
         *
         * @param stats
         */
        override fun onLocalAudioStats(stats: LocalAudioStats?) {
            super.onLocalAudioStats(stats)
            stats ?: return
            eventListener?.onLocalAudioStats?.invoke(stats)
        }

        /**
         * On remote audio stats
         *
         * @param stats
         */
        override fun onRemoteAudioStats(stats: RemoteAudioStats?) {
            super.onRemoteAudioStats(stats)
            stats ?: return
            eventListener?.onRemoteAudioStats?.invoke(stats)
        }

        /**
         * On uplink network info updated
         *
         * @param info
         */
        override fun onUplinkNetworkInfoUpdated(info: UplinkNetworkInfo?) {
            super.onUplinkNetworkInfoUpdated(info)
            info ?: return
            eventListener?.onUplinkNetworkInfoUpdated?.invoke(info)
        }

        /**
         * On downlink network info updated
         *
         * @param info
         */
        override fun onDownlinkNetworkInfoUpdated(info: DownlinkNetworkInfo?) {
            super.onDownlinkNetworkInfoUpdated(info)
            info ?: return
            eventListener?.onDownlinkNetworkInfoUpdated?.invoke(info)
        }

        /**
         * On content inspect result
         *
         * @param result
         */
        override fun onContentInspectResult(result: Int) {
            super.onContentInspectResult(result)
            eventListener?.onContentInspectResult?.invoke(result)
        }

    }

    /**
     * Rtc connection wrap
     *
     * @constructor
     *
     * @param connection
     */
    inner class RtcConnectionWrap constructor(connection: RtcConnection) :
        RtcConnection(connection.channelId, connection.localUid) {

        /**
         * Media options
         */
        var mediaOptions = ChannelMediaOptions()

        /**
         * Rtc event handler
         */
        var rtcEventHandler: RtcEngineEventHandlerImpl? = null

        /**
         * Audio mixing player
         */
        var audioMixingPlayer : IMediaPlayer? = null

        /**
         * Is same channel
         *
         * @param connection
         */
        fun isSameChannel(connection: RtcConnection?) =
            connection != null && channelId == connection.channelId && localUid == connection.localUid

        /**
         * To string
         *
         * @return
         */
        override fun toString(): String {
            return "{channelId=$channelId, localUid=$localUid"
        }
    }

    /**
     * Remote video canvas wrap
     *
     * @property connection
     * @property lifecycleOwner
     * @constructor
     *
     * @param view
     * @param renderMode
     * @param uid
     */
    inner class RemoteVideoCanvasWrap constructor(
        val connection: RtcConnectionWrap,
        val lifecycleOwner: LifecycleOwner,
        view: View,
        renderMode: Int,
        uid: Int
    ) : DefaultLifecycleObserver, VideoCanvas(view, renderMode, uid) {

        init {
            lifecycleOwner.lifecycle.addObserver(this)
            remoteVideoCanvasList.add(this)
        }

        /**
         * On destroy
         *
         * @param owner
         */
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            if (lifecycleOwner == owner) {
                release()
            }
        }

        /**
         * Release
         *
         */
        fun release() {
            lifecycleOwner.lifecycle.removeObserver(this)
            view = null
            //rtcEngine.setupRemoteVideoEx(this, connection)
            remoteVideoCanvasList.remove(this)
        }
    }

    /**
     * Run on ui thread
     *
     * @param run
     * @receiver
     */
    private fun runOnUiThread(run: () -> Unit) {
        if (Thread.currentThread() == mainHandler.looper.thread) {
            run.invoke()
        } else {
            mainHandler.post(run)
        }
    }
}