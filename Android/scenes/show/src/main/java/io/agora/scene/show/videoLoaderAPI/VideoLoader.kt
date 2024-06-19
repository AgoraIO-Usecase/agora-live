package io.agora.scene.show.videoLoaderAPI

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcEngineEx
import io.agora.scene.show.videoLoaderAPI.report.APIReporter
import io.agora.scene.show.videoLoaderAPI.report.APIType

/**
 * Anchor state
 *
 * @constructor Create empty Anchor state
 */
enum class AnchorState {
    /**
     * Idle
     *
     * @constructor Create empty Idle
     */
    IDLE,

    /**
     * Pre Joined
     *
     * @constructor Create empty Pre Joined
     */
    PRE_JOINED,

    /**
     * Joined
     *
     * @constructor Create empty Joined
     */
    JOINED,

    /**
     * Joined Without Audio
     *
     * @constructor Create empty Joined Without Audio
     */
    JOINED_WITHOUT_AUDIO,
}

/**
 * Video loader
 *
 * @constructor Create empty Video loader
 */
interface VideoLoader {

    companion object {
        const val version = "1.0.0"
        private var rtcEngine: RtcEngineEx? = null
        private var instance: VideoLoader? = null
        var reporter: APIReporter? = null

        fun getImplInstance(engine: RtcEngineEx): VideoLoader {
            rtcEngine = engine
            if (instance == null) {
                instance = VideoLoaderImpl(engine)
                reporter = APIReporter(APIType.VIDEO_LOADER, version, engine)
                engine.enableInstantMediaRendering()
            }
            return instance as VideoLoader
        }

        fun videoLoaderApiLog(tag: String, msg: String) {
            reporter?.writeLog("[$tag] $msg", Constants.LOG_LEVEL_INFO)
        }

        fun videoLoaderApiLogWarning(tag: String, msg: String) {
            reporter?.writeLog("[$tag] $msg", Constants.LOG_LEVEL_WARNING)
        }

        fun release() {
            instance = null
            rtcEngine = null
            reporter = null
        }
    }

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
     * Anchor info
     *
     * @property channelId
     * @property anchorUid
     * @property token
     * @constructor Create empty Anchor info
     */
    data class AnchorInfo constructor(
        val channelId: String = "",
        val anchorUid: Int = 0,
        val token: String = ""
    ) {
        override fun toString(): String {
            return "channelId:$channelId, anchorUid:$anchorUid"
        }
    }

    /**
     * Room info
     *
     * @property roomId
     * @property anchorList
     * @constructor Create empty Room info
     */
    data class RoomInfo(
        val roomId: String,
        val anchorList: ArrayList<AnchorInfo>
    )

    /**
     * Clean cache
     *
     */
    fun cleanCache()

    /**
     * Preload anchor
     *
     * @param anchorList
     * @param uid
     */
    fun preloadAnchor(anchorList: List<AnchorInfo>, uid: Int)

    /**
     * Switch anchor state
     *
     * @param newState
     * @param anchorInfo
     * @param localUid
     * @param mediaOptions
     */
    fun switchAnchorState(
        newState: AnchorState,
        anchorInfo: AnchorInfo,
        localUid: Int,
        mediaOptions: ChannelMediaOptions? = null
    )

    /**
     * Get anchor state
     *
     * @param channelId
     * @param localUid
     * @return
     */
    fun getAnchorState(channelId: String, localUid: Int): AnchorState?


    /**
     * Render video
     *
     * @param anchorInfo
     * @param localUid
     * @param container
     */
    fun renderVideo(anchorInfo: AnchorInfo, localUid: Int, container: VideoCanvasContainer)
}