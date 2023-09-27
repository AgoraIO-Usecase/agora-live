package io.agora.scene.show.videoSwitcherAPI

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcConnection
import io.agora.rtc2.RtcEngineEx

/**
 * Video switcher
 *
 * @constructor Create empty Video switcher
 */
interface VideoSwitcher {

    companion object {
        private var rtcEngine: RtcEngineEx? = null
        private var instance: VideoSwitcher? = null

        /**
         * Get impl instance
         *
         * @param engine
         * @return
         */
        fun getImplInstance(engine: RtcEngineEx): VideoSwitcher {
            rtcEngine = engine
            if (instance == null) {
                instance = VideoSwitcherImpl(rtcEngine!!)
            }
            return instance as VideoSwitcher
        }

        /**
         * Release
         *
         */
        fun release() {
            instance = null
        }
    }

    /**
     * I channel event listener
     *
     * @property onTokenGenerateFailedException
     * @property onChannelJoined
     * @property onUserJoined
     * @property onUserOffline
     * @property onLocalVideoStateChanged
     * @property onRemoteVideoStateChanged
     * @property onRtcStats
     * @property onLocalVideoStats
     * @property onRemoteVideoStats
     * @property onLocalAudioStats
     * @property onRemoteAudioStats
     * @property onUplinkNetworkInfoUpdated
     * @property onDownlinkNetworkInfoUpdated
     * @property onContentInspectResult
     * @property onFirstRemoteVideoFrame
     * @constructor Create empty I channel event listener
     */
    data class IChannelEventListener(
        var onTokenGenerateFailedException: ((error: Throwable)->Unit)? = null,
        var onChannelJoined: ((connection: RtcConnection)->Unit)? = null,
        var onUserJoined: ((uid: Int) -> Unit)? = null,
        var onUserOffline: ((uid: Int) -> Unit)? = null,
        var onLocalVideoStateChanged: ((state: Int) -> Unit)? = null,
        var onRemoteVideoStateChanged: ((uid: Int, state: Int) -> Unit)? = null,
        var onRtcStats: ((stats: IRtcEngineEventHandler.RtcStats) -> Unit)? = null,
        var onLocalVideoStats: ((stats: IRtcEngineEventHandler.LocalVideoStats) -> Unit)? = null,
        var onRemoteVideoStats: ((stats: IRtcEngineEventHandler.RemoteVideoStats) -> Unit)? = null,
        var onLocalAudioStats: ((stats: IRtcEngineEventHandler.LocalAudioStats) -> Unit)? = null,
        var onRemoteAudioStats: ((stats: IRtcEngineEventHandler.RemoteAudioStats) -> Unit)? = null,
        var onUplinkNetworkInfoUpdated: ((info: IRtcEngineEventHandler.UplinkNetworkInfo) -> Unit)? = null,
        var onDownlinkNetworkInfoUpdated: ((info: IRtcEngineEventHandler.DownlinkNetworkInfo) -> Unit)? = null,
        var onContentInspectResult: ((result: Int) -> Unit)? = null,
        var onFirstRemoteVideoFrame: ((uid: Int, width: Int, height: Int, elapsed: Int)->Unit)? = null,
    )

    /**
     * Video canvas container
     *
     * @property lifecycleOwner
     * @property container
     * @property uid
     * @property viewIndex
     * @property renderMode
     * @constructor Create empty Video canvas container
     */
    data class VideoCanvasContainer(
        val lifecycleOwner: LifecycleOwner,
        val container: ViewGroup,
        val uid: Int,
        val viewIndex: Int = 0,
        val renderMode: Int = Constants.RENDER_MODE_HIDDEN,
    )

    /**
     * Set preload count
     *
     * @param count
     */
    fun setPreloadCount(count: Int)

    /**
     * Preload connections
     *
     * @param connections
     */
    fun preloadConnections(connections: List<RtcConnection>)

    /**
     * Unload connections
     *
     */
    fun unloadConnections()

    /**
     * Pre join channel
     *
     * @param connection
     * @param mediaOptions
     * @param token
     * @param eventListener
     */
    fun preJoinChannel(
        connection: RtcConnection,
        mediaOptions: ChannelMediaOptions,
        token: String?,
        eventListener: IChannelEventListener?
    )

    /**
     * Pre join channel
     *
     * @param connection
     */
    fun preJoinChannel(
        connection: RtcConnection
    )

    /**
     * Join channel
     *
     * @param connection
     * @param mediaOptions
     * @param token
     * @param eventListener
     * @param needPreJoin
     */
    fun joinChannel(
        connection: RtcConnection,
        mediaOptions: ChannelMediaOptions,
        token: String?,
        eventListener: IChannelEventListener?,
        needPreJoin: Boolean?
    )

    /**
     * Set channel event
     *
     * @param channelName
     * @param uid
     * @param eventHandler
     */
    fun setChannelEvent(channelName: String, uid: Int, eventHandler: IChannelEventListener?)

    /**
     * Leave channel
     *
     * @param connection
     * @param force
     * @return
     */
    fun leaveChannel(connection: RtcConnection, force: Boolean): Boolean

    /**
     * Setup remote video
     *
     * @param connection
     * @param container
     */
    fun setupRemoteVideo(connection: RtcConnection, container: VideoCanvasContainer)

    /**
     * Get first video frame time
     *
     * @return
     */
    fun getFirstVideoFrameTime(): Long

    /**
     * Start audio mixing
     *
     * @param connection
     * @param filePath
     * @param loopbackOnly
     * @param cycle
     */
    fun startAudioMixing(connection: RtcConnection, filePath: String, loopbackOnly: Boolean, cycle: Int)

    /**
     * Stop audio mixing
     *
     * @param connection
     */
    fun stopAudioMixing(connection: RtcConnection)

    /**
     * Adjust audio mixing volume
     *
     * @param connection
     * @param volume
     */
    fun adjustAudioMixingVolume(connection: RtcConnection, volume: Int)
}