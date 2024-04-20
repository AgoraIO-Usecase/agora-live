package io.agora.scene.show

import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.RtcEngineEx
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.agora.rtc2.video.VirtualBackgroundSource
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.show.beauty.BeautyProcessorImpl
import io.agora.scene.show.beauty.IBeautyProcessor
import io.agora.scene.show.debugSettings.DebugSettingModel
import io.agora.scene.show.videoLoaderAPI.VideoLoader
import java.util.concurrent.Executors

/**
 * Rtc engine instance
 *
 * @constructor Create empty Rtc engine instance
 */
object RtcEngineInstance {

    /**
     * Video encoder configuration
     */
    val videoEncoderConfiguration = VideoEncoderConfiguration().apply {
        orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
    }

    /**
     * Virtual background source
     */
    val virtualBackgroundSource = VirtualBackgroundSource().apply {
        backgroundSourceType = VirtualBackgroundSource.BACKGROUND_COLOR
    }

    /**
     * Video capture configuration
     */
    val videoCaptureConfiguration = CameraCapturerConfiguration(CameraCapturerConfiguration.CaptureFormat()).apply {
        followEncodeDimensionRatio = false
    }

    /**
     * Debug setting model
     */
    val debugSettingModel = DebugSettingModel().apply { }

    private val workingExecutor = Executors.newSingleThreadExecutor()

    private var innerBeautyProcessor: IBeautyProcessor? = null

    /**
     * Beauty processor
     */
    val beautyProcessor: IBeautyProcessor
        get() {
            if (innerBeautyProcessor == null) {
                innerBeautyProcessor =
                    BeautyProcessorImpl(
                        AgoraApplication.the()
                    )
            }
            return innerBeautyProcessor!!
        }

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
                        ShowLogger.d(
                            "RtcEngineInstance",
                            "Rtc Error code:$err, msg:" + RtcEngine.getErrorDescription(err)
                        )
                    }
                }
                innerRtcEngine = (RtcEngine.create(config) as RtcEngineEx).apply {
                    enableVideo()
                }
                //beautyProcessor.initialize(innerRtcEngine!!)
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

    fun releaseBeautyProcessor() {
        innerBeautyProcessor?.let { processor ->
            processor.release()
            innerBeautyProcessor = null
        }
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
        innerBeautyProcessor?.let { processor ->
            processor.release()
            innerBeautyProcessor = null
        }
        debugSettingModel.apply {
            pvcEnabled = true
            autoFocusFaceModeEnabled = true
            exposurePositionX = null
            exposurePositionY = null
            cameraSelect = null
            videoFullrangeExt = null
            matrixCoefficientsExt = null
            enableHWEncoder = true
            codecType = 3     // 2 -> h264, 3 -> h265
            mirrorMode = false
            renderMode = 0       // 0 -> hidden, 1 -> fix
            colorEnhance = false
            dark = false
            noise = false
            srEnabled = false
            srType = 1.0
        }
    }
}