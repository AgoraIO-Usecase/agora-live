package io.agora.scene.eCommerce

import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.RtcEngineEx
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.eCommerce.videoLoaderAPI.VideoLoader
import java.util.concurrent.Executors

/**
 * Rtc engine instance
 *
 * @constructor Create empty Rtc engine instance
 */
object RtcEngineInstance {
    var isFrontCamera = true
    /**
     * Video encoder configuration
     */
    val videoEncoderConfiguration = VideoEncoderConfiguration().apply {
        orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
    }
    /**
     * Video capture configuration
     */
    val videoCaptureConfiguration = CameraCapturerConfiguration(CameraCapturerConfiguration.CaptureFormat()).apply {
        followEncodeDimensionRatio = false
    }

    private val workingExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var generalToken: String = ""

    /**
     * Setup general token
     *
     * @param generalToken
     */
    fun setupGeneralToken(generalToken: String) {
        if (generalToken.isEmpty()) {
            this.generalToken = ""
        } else {
            if (this.generalToken.isEmpty()) {
                this.generalToken = generalToken
            }
        }
    }

    /**
     * General token
     *
     * @return
     */
    fun generalToken(): String = generalToken

    private var innerRtcEngine: RtcEngineEx? = null

    /**
     * Rtc engine
     */
    val rtcEngine: RtcEngineEx
        get() {
            if (innerRtcEngine == null) {
                val config = RtcEngineConfig()
                config.mContext = AgoraApplication.the()
                config.mAppId = io.agora.scene.base.BuildConfig.AGORA_APP_ID
                config.mEventHandler = object : IRtcEngineEventHandler() {
                    override fun onError(err: Int) {
                        super.onError(err)
                        CommerceLogger.d(
                            "RtcEngineInstance",
                            "Rtc Error code:$err, msg:" + RtcEngine.getErrorDescription(err)
                        )
                    }
                }
                innerRtcEngine = (RtcEngine.create(config) as RtcEngineEx).apply {
                    enableVideo()
                }
            }
            return innerRtcEngine!!
        }

    /**
     * Clean cache
     *
     */
    fun cleanCache() {
        VideoLoader.getImplInstance(rtcEngine).cleanCache()
    }


    /**
     * Destroy
     *
     */
    fun destroy() {
        VideoLoader.release()
        innerRtcEngine?.let {
            workingExecutor.execute { RtcEngineEx.destroy() }
            innerRtcEngine = null
        }
    }
}