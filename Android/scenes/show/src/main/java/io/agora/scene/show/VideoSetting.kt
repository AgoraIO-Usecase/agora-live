package io.agora.scene.show

import android.util.Log
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcConnection
import io.agora.rtc2.SimulcastStreamConfig
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.ColorEnhanceOptions
import io.agora.rtc2.video.LowLightEnhanceOptions
import io.agora.rtc2.video.VideoDenoiserOptions
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.agora.scene.base.Constant
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.utils.GsonUtils
import io.agora.scene.base.utils.SPUtil

/**
 * Video setting
 *
 * @constructor Create empty Video setting
 */
object VideoSetting {

    /**
     * Bit rate
     *
     * @property value
     * @constructor Create empty Bit rate
     */
    enum class BitRate constructor(val value: Int) {
        BR_Low_1V1(1461),
        BR_Medium_1V1(1461),
        BR_High_1V1(2099),
        BR_Low_PK(700),
        BR_Medium_PK(800),
        BR_High_PK(800),
        BR_STANDRAD(0)
    }

    /**
     * Super resolution
     *
     * @property value
     * @constructor Create empty Super resolution
     */
    enum class SuperResolution constructor(val value: Int) {
        SR_1(6),
        SR_1_33(7),
        SR_1_5(8),
        SR_2(3),
        SR_SHARP(10),
        SR_NONE(0),
        SR_SUPER(20),
        SR_AUTO(-1)
    }

    /**
     * Resolution
     *
     * @property width
     * @property height
     * @constructor Create empty Resolution
     */
    enum class Resolution constructor(val width: Int, val height: Int) {
        V_1080P(1920, 1080),
        V_720P(1280, 720),
        V_540P(960, 540),
        V_480P(856, 480),
        V_360P(640, 360),
        V_180P(360, 180),
    }

    /**
     * To index
     *
     */
    fun Resolution.toIndex() = ResolutionList.indexOf(this)

    /**
     * Resolution list
     */
    val ResolutionList = listOf(
        Resolution.V_360P,
        Resolution.V_480P,
        Resolution.V_540P,
        Resolution.V_720P,
        Resolution.V_1080P
    )

    /**
     * Frame rate
     *
     * @property fps
     * @constructor Create empty Frame rate
     */
    enum class FrameRate constructor(val fps: Int) {
        FPS_1(1),
        FPS_7(7),
        FPS_10(10),
        FPS_15(15),
        FPS_24(24),
        FPS_30(30),
        FPS_60(60),
    }

    /**
     * To index
     *
     */
    fun FrameRate.toIndex() = FrameRateList.indexOf(this)

    /**
     * Frame rate list
     */
    val FrameRateList = listOf(
        FrameRate.FPS_1,
        FrameRate.FPS_7,
        FrameRate.FPS_10,
        FrameRate.FPS_15,
        FrameRate.FPS_24,
        FrameRate.FPS_30
    )

    /**
     * Device level
     *
     * @property value
     * @constructor Create empty Device level
     */
    enum class DeviceLevel constructor(val value: Int) {
        Low(0),
        Medium(1),
        High(2)
    }

    /**
     * Network level
     *
     * @property value
     * @constructor Create empty Network level
     */
    enum class NetworkLevel constructor(val value: Int) {
        Good(0),
        Normal(1)
    }

    /**
     * Broadcast strategy
     *
     * @property value
     * @constructor Create empty Broadcast strategy
     */
    enum class BroadcastStrategy constructor(val value: Int) {
        Smooth(0),
        Clear(1),
        Pure(2)
    }

    /**
     * Audience play setting
     *
     * @constructor Create empty Audience play setting
     */
    object AudiencePlaySetting {

        /**
         * Enhance Low
         */
        val ENHANCE_LOW = 0

        /**
         * Enhance Medium
         */
        val ENHANCE_MEDIUM = 1

        /**
         * Enhance High
         */
        val ENHANCE_HIGH = 2

        /**
         * Base Low
         */
        val BASE_LOW = 3

        /**
         * Base Medium
         */
        val BASE_MEDIUM = 4

        /**
         * Base High
         */
        val BASE_HIGH = 5

    }

    /**
     * Live mode
     *
     * @property value
     * @constructor Create empty Live mode
     */
    enum class LiveMode constructor(val value: Int) {
        OneVOne(0),
        PK(1)
    }

    /**
     * Audience setting
     *
     * @property video
     * @constructor Create empty Audience setting
     */
    data class AudienceSetting constructor(val video: Video) {
        /**
         * Video
         *
         * @property SR
         * @constructor Create empty Video
         */
        data class Video constructor(
            val SR: SuperResolution // 超分
        )
    }

    /**
     * BroadcastSetting
     *
     * @property video
     * @property audio
     */
    data class BroadcastSetting constructor(
        val video: Video,
        val audio: Audio
    ) {
        /**
         * Video
         *
         * @property H265
         * @property colorEnhance
         * @property lowLightEnhance
         * @property videoDenoiser
         * @property PVC
         * @property captureResolution
         * @property encodeResolution
         * @property frameRate
         * @property bitRate
         * @property bitRateRecommend
         * @property bitRateStandard
         * @property hardwareVideoEncoder
         * @constructor Create empty Video
         */
        data class Video constructor(
            val H265: Boolean,
            val colorEnhance: Boolean,
            val lowLightEnhance: Boolean,
            val videoDenoiser: Boolean,
            val PVC: Boolean,
            val captureResolution: Resolution,
            val encodeResolution: Resolution,
            val frameRate: FrameRate,
            val bitRate: Int,
            val bitRateRecommend: Int,
            val bitRateStandard: Boolean,
            val hardwareVideoEncoder: Boolean
        )

        /**
         * Audio
         *
         * @property inEarMonitoring
         * @property recordingSignalVolume
         * @property audioMixingVolume
         * @constructor Create empty Audio
         */
        data class Audio constructor(
            val inEarMonitoring: Boolean,
            val recordingSignalVolume: Int,
            val audioMixingVolume: Int,
        )
    }

    /**
     * Low stream video setting
     *
     * @property encodeResolution
     * @property frameRate
     * @property bitRate
     * @property SVC
     * @property enableHardwareEncoder
     * @constructor Create empty Low stream video setting
     */
    data class LowStreamVideoSetting constructor(
        val encodeResolution: Resolution,
        val frameRate: FrameRate,
        val bitRate: Int,
        val SVC: Boolean,
        val enableHardwareEncoder: Boolean
    )

    /**
     * Recommend broadcast setting
     *
     * @constructor Create empty Recommend broadcast setting
     */
    object RecommendBroadcastSetting {

        /**
         * Low device1v1
         */
        val LowDevice1v1 = BroadcastSetting(
            BroadcastSetting.Video(
                H265 = true,
                colorEnhance = false,
                lowLightEnhance = false,
                videoDenoiser = false,
                PVC = false,
                captureResolution = Resolution.V_720P,
                encodeResolution = Resolution.V_720P,
                frameRate = FrameRate.FPS_15,
                bitRate = BitRate.BR_STANDRAD.value,
                bitRateRecommend = BitRate.BR_Low_1V1.value,
                bitRateStandard = true,
                hardwareVideoEncoder = true
            ),
            BroadcastSetting.Audio(false, 80, 30)
        )

        /**
         * Medium device1v1
         */
        val MediumDevice1v1 = BroadcastSetting(
            BroadcastSetting.Video(
                H265 = true,
                colorEnhance = false,
                lowLightEnhance = false,
                videoDenoiser = false,
                PVC = false,
                captureResolution = Resolution.V_720P,
                encodeResolution = Resolution.V_720P,
                frameRate = FrameRate.FPS_24,
                bitRate = BitRate.BR_STANDRAD.value,
                bitRateRecommend = BitRate.BR_Medium_1V1.value,
                bitRateStandard = true,
                hardwareVideoEncoder = true
            ),
            BroadcastSetting.Audio(false, 80, 30)
        )

        /**
         * High device1v1
         */
        val HighDevice1v1 = BroadcastSetting(
            BroadcastSetting.Video(
                H265 = true,
                colorEnhance = false,
                lowLightEnhance = false,
                videoDenoiser = false,
                PVC = false,
                captureResolution = Resolution.V_1080P,
                encodeResolution = Resolution.V_1080P,
                frameRate = FrameRate.FPS_24,
                bitRate = BitRate.BR_STANDRAD.value,
                bitRateRecommend = BitRate.BR_High_1V1.value,
                bitRateStandard = true,
                hardwareVideoEncoder = true
            ),
            BroadcastSetting.Audio(false, 80, 30)
        )

        /**
         * Audience1v1
         */
        val Audience1v1 = BroadcastSetting(
            BroadcastSetting.Video(
                H265 = true,
                colorEnhance = false,
                lowLightEnhance = false,
                videoDenoiser = false,
                PVC = true,
                captureResolution = Resolution.V_180P,
                encodeResolution = Resolution.V_180P,
                frameRate = FrameRate.FPS_15,
                bitRate = BitRate.BR_STANDRAD.value,
                bitRateRecommend = BitRate.BR_STANDRAD.value,
                bitRateStandard = true,
                hardwareVideoEncoder = true
            ),
            BroadcastSetting.Audio(false, 80, 30)
        )

        /**
         * Low device p k
         */
        val LowDevicePK = BroadcastSetting(
            BroadcastSetting.Video(
                H265 = true,
                colorEnhance = false,
                lowLightEnhance = false,
                videoDenoiser = false,
                PVC = false,
                captureResolution = Resolution.V_540P,
                encodeResolution = Resolution.V_540P,
                frameRate = FrameRate.FPS_15,
                bitRate = BitRate.BR_STANDRAD.value,
                bitRateRecommend = BitRate.BR_Low_PK.value,
                bitRateStandard = true,
                hardwareVideoEncoder = true
            ),
            BroadcastSetting.Audio(false, 80, 30)
        )

        /**
         * Medium device p k
         */
        val MediumDevicePK = BroadcastSetting(
            BroadcastSetting.Video(
                H265 = true,
                colorEnhance = false,
                lowLightEnhance = false,
                videoDenoiser = false,
                PVC = false,
                captureResolution = Resolution.V_540P,
                encodeResolution = Resolution.V_540P,
                frameRate = FrameRate.FPS_15,
                bitRate = BitRate.BR_STANDRAD.value,
                bitRateRecommend = BitRate.BR_Medium_PK.value,
                bitRateStandard = true,
                hardwareVideoEncoder = true
            ),
            BroadcastSetting.Audio(false, 80, 30)
        )

        /**
         * High device p k
         */
        val HighDevicePK = BroadcastSetting(
            BroadcastSetting.Video(
                H265 = true,
                colorEnhance = false,
                lowLightEnhance = false,
                videoDenoiser = false,
                PVC = false,
                captureResolution = Resolution.V_720P,
                encodeResolution = Resolution.V_720P,
                frameRate = FrameRate.FPS_15,
                bitRate = BitRate.BR_STANDRAD.value,
                bitRateRecommend = BitRate.BR_High_PK.value,
                bitRateStandard = true,
                hardwareVideoEncoder = true
            ),
            BroadcastSetting.Audio(false, 80, 30)
        )

    }

    /**
     * Recommend low stream video setting
     *
     * @constructor Create empty Recommend low stream video setting
     */
    object RecommendLowStreamVideoSetting {
        /**
         * Low device good network1v1
         */
        val LowDeviceGoodNetwork1v1 = LowStreamVideoSetting(
            encodeResolution = Resolution.V_360P,
            frameRate = FrameRate.FPS_15,
            bitRate = 680,
            SVC = false,
            enableHardwareEncoder = true,
        )

        /**
         * Low device normal network1v1
         */
        val LowDeviceNormalNetwork1v1 = LowStreamVideoSetting(
            encodeResolution = Resolution.V_360P,
            frameRate = FrameRate.FPS_15,
            bitRate = 748,
            SVC = true,
            enableHardwareEncoder = false,
        )

        /**
         * Middle device good network1v1
         */
        val MiddleDeviceGoodNetwork1v1 = LowStreamVideoSetting(
            encodeResolution = Resolution.V_360P,
            frameRate = FrameRate.FPS_15,
            bitRate = 680,
            SVC = false,
            enableHardwareEncoder = true,
        )

        /**
         * Middle device normal network1v1
         */
        val MiddleDeviceNormalNetwork1v1 = LowStreamVideoSetting(
            encodeResolution = Resolution.V_360P,
            frameRate = FrameRate.FPS_15,
            bitRate = 748,
            SVC = true,
            enableHardwareEncoder = false,
        )

        /**
         * High device good network1v1
         */
        val HighDeviceGoodNetwork1v1 = LowStreamVideoSetting(
            encodeResolution = Resolution.V_540P,
            frameRate = FrameRate.FPS_15,
            bitRate = 1100,
            SVC = false,
            enableHardwareEncoder = true,
        )

        /**
         * High device normal network1v1
         */
        val HighDeviceNormalNetwork1v1 = LowStreamVideoSetting(
            encodeResolution = Resolution.V_360P,
            frameRate = FrameRate.FPS_15,
            bitRate = 748,
            SVC = true,
            enableHardwareEncoder = false,
        )

        /**
         * Pk
         */
        val PK = LowStreamVideoSetting(
            encodeResolution = Resolution.V_360P,
            frameRate = FrameRate.FPS_15,
            bitRate = 680,
            SVC = false,
            enableHardwareEncoder = true,
        )
    }

    private var currAudienceSetting: AudienceSetting = getCurrAudienceSetting()
    private var currBroadcastSetting: BroadcastSetting = getCurrBroadcastSetting()

    private var currAudienceDeviceLevel: DeviceLevel = DeviceLevel.valueOf(
        SPUtil.getString(Constant.CURR_AUDIENCE_DEVICE_LEVEL, DeviceLevel.Low.toString())
    )

    private var currNetworkLevel: NetworkLevel = NetworkLevel.valueOf(
        SPUtil.getString(Constant.CURR_BROADCAST_NETWORK_LEVEL, NetworkLevel.Good.toString())
    )

    private var currAudiencePlaySetting: Int =
        SPUtil.getInt(Constant.CURR_AUDIENCE_PLAY_SETTING, AudiencePlaySetting.BASE_LOW)

    private var currAudienceEnhanceSwitch =
        SPUtil.getBoolean(Constant.CURR_AUDIENCE_ENHANCE_SWITCH, true)

    @Volatile
    private var isPkMode: Boolean = false

    /**
     * Set is pk mode
     *
     * @param isPkMode
     */
    fun setIsPkMode(isPkMode: Boolean) {
        this.isPkMode = isPkMode
    }

    /**
     * Get curr audience setting
     *
     * @return
     */
    fun getCurrAudienceSetting(): AudienceSetting {
        //
        val jsonStr = SPUtil.getString(Constant.CURR_AUDIENCE_SETTING, "")
        try {
            return GsonUtils.gson.fromJson(jsonStr, AudienceSetting::class.java)
        } catch (e: java.lang.Exception) {
            val result = AudienceSetting(AudienceSetting.Video(SuperResolution.SR_NONE))
            setCurrAudienceSetting(result)
            return result
        }
    }

    /**
     * Get curr broadcast setting
     *
     * @return
     */
    fun getCurrBroadcastSetting(): BroadcastSetting {
        val jsonStr = SPUtil.getString(Constant.CURR_BROADCAST_SETTING, "")
        try {
            return GsonUtils.gson.fromJson(jsonStr, BroadcastSetting::class.java)
        } catch (e: java.lang.Exception) {
            val result = RecommendBroadcastSetting.LowDevice1v1
            setCurrBroadcastSetting(result)
            return result
        }
    }

    /**
     * Get curr low stream setting
     *
     * @return
     */
    fun getCurrLowStreamSetting(): LowStreamVideoSetting? {
        val jsonStr = SPUtil.getString(Constant.CURR_LOW_STREAM_SETTING, "")
        if (jsonStr == "") return null
        try {
            return GsonUtils.gson.fromJson(jsonStr, LowStreamVideoSetting::class.java)
        } catch (e: java.lang.Exception) {
            val result = RecommendLowStreamVideoSetting.LowDeviceGoodNetwork1v1
            setCurrLowStreamSetting(result)
            return result
        }
    }

    /**
     * Get curr audience play setting
     *
     */
    fun getCurrAudiencePlaySetting() = currAudiencePlaySetting

    /**
     * Get curr audience enhance switch
     *
     */
    fun getCurrAudienceEnhanceSwitch() = currAudienceEnhanceSwitch

    /**
     * Set curr audience setting
     *
     * @param audienceSetting
     */
    fun setCurrAudienceSetting(audienceSetting: AudienceSetting) {
        SPUtil.putString(Constant.CURR_AUDIENCE_SETTING, GsonUtils.gson.toJson(audienceSetting))
        currAudienceSetting = audienceSetting
    }

    /**
     * Set curr broadcast setting
     *
     * @param broadcastSetting
     */
    fun setCurrBroadcastSetting(broadcastSetting: BroadcastSetting) {
        SPUtil.putString(
            Constant.CURR_BROADCAST_SETTING,
            GsonUtils.gson.toJson(broadcastSetting)
        )
        currBroadcastSetting = broadcastSetting
    }

    /**
     * Set curr low stream setting
     *
     * @param lowStreamSetting
     */
    fun setCurrLowStreamSetting(lowStreamSetting: LowStreamVideoSetting?) {
        if (lowStreamSetting == null) {
            SPUtil.putString(
                Constant.CURR_LOW_STREAM_SETTING,
                ""
            )
        } else {
            SPUtil.putString(
                Constant.CURR_LOW_STREAM_SETTING,
                GsonUtils.gson.toJson(lowStreamSetting)
            )
        }
    }

    /**
     * Set curr audience device level
     *
     * @param deviceLevel
     */
    fun setCurrAudienceDeviceLevel(deviceLevel: DeviceLevel) {
        currAudienceDeviceLevel = deviceLevel
        SPUtil.putString(Constant.CURR_AUDIENCE_DEVICE_LEVEL, deviceLevel.toString())
    }

    /**
     * Set curr network level
     *
     * @param networkLevel
     */
    fun setCurrNetworkLevel(networkLevel: NetworkLevel) {
        currNetworkLevel = networkLevel
        SPUtil.putString(Constant.CURR_BROADCAST_NETWORK_LEVEL, networkLevel.toString())
    }

    /**
     * Set curr audience play setting
     *
     * @param currAudiencePlaySetting
     */
    fun setCurrAudiencePlaySetting(currAudiencePlaySetting: Int) {
        this.currAudiencePlaySetting = currAudiencePlaySetting
        SPUtil.putInt(Constant.CURR_AUDIENCE_PLAY_SETTING, currAudiencePlaySetting)
    }

    /**
     * Set curr audience enhance switch
     *
     * @param currAudienceEnhanceSwitch
     */
    fun setCurrAudienceEnhanceSwitch(currAudienceEnhanceSwitch: Boolean) {
        this.currAudienceEnhanceSwitch = currAudienceEnhanceSwitch
        SPUtil.putBoolean(Constant.CURR_AUDIENCE_ENHANCE_SWITCH, currAudienceEnhanceSwitch)
    }

    /**
     * Reset audience setting
     *
     */
    fun resetAudienceSetting() {
        isPkMode = false
        val result = AudienceSetting(AudienceSetting.Video(SuperResolution.SR_NONE))
        setCurrAudienceSetting(result)
    }

    /**
     * Reset broadcast setting
     *
     */
    fun resetBroadcastSetting() {
        setCurrBroadcastSetting(
            when (currAudienceDeviceLevel) {
                DeviceLevel.Low -> RecommendBroadcastSetting.LowDevice1v1
                DeviceLevel.Medium -> RecommendBroadcastSetting.MediumDevice1v1
                DeviceLevel.High -> RecommendBroadcastSetting.HighDevice1v1
            }
        )
    }

    /**
     * Update audience setting
     *
     */
    fun updateAudienceSetting() {
        if (isPureMode) return
        if (currAudienceDeviceLevel != DeviceLevel.Low) {
            setCurrAudienceEnhanceSwitch(true)
            updateSRSetting(SR = SuperResolution.SR_AUTO)
        } else {
            setCurrAudienceEnhanceSwitch(false)
            updateSRSetting(SR = SuperResolution.SR_NONE)
        }
    }

    /**
     * Update audio setting
     *
     * @param SR
     */
    fun updateSRSetting(SR: SuperResolution? = null) {
        if (isPureMode) {
            setCurrAudienceSetting(
                AudienceSetting(AudienceSetting.Video(SuperResolution.SR_NONE))
            )
            updateRTCAudioSetting(SuperResolution.SR_NONE)
            return
        }
        setCurrAudienceSetting(
            AudienceSetting(AudienceSetting.Video(SR ?: currAudienceSetting.video.SR))
        )
        updateRTCAudioSetting(SR)
    }

    var isPureMode = false

    /**
     * Update broadcast setting
     *
     * @param deviceLevel
     * @param networkLevel
     * @param broadcastStrategy
     * @param isJoinedRoom
     * @param isByAudience
     * @param rtcConnection
     */
    fun updateBroadcastSetting(
        deviceLevel: DeviceLevel,
        networkLevel: NetworkLevel = NetworkLevel.Good,
        broadcastStrategy: BroadcastStrategy = BroadcastStrategy.Smooth,
        isJoinedRoom: Boolean = false,
        isByAudience: Boolean = false,
        rtcConnection: RtcConnection? = null,
    ) {
        ShowLogger.d("VideoSettings", "updateBroadcastSetting, deviceLevel:$deviceLevel networkLevel:$networkLevel broadcastStrategy:$broadcastStrategy isPureMode:$isPureMode")
        this.isPureMode = broadcastStrategy == BroadcastStrategy.Pure
        var liveMode = LiveMode.OneVOne
        if (isByAudience) {
            setCurrAudienceDeviceLevel(deviceLevel)
            return
        } else {
            setCurrAudienceDeviceLevel(deviceLevel)
            setCurrNetworkLevel(networkLevel)
            liveMode = when (currBroadcastSetting) {
                RecommendBroadcastSetting.LowDevice1v1, RecommendBroadcastSetting.MediumDevice1v1, RecommendBroadcastSetting.HighDevice1v1 -> LiveMode.OneVOne
                RecommendBroadcastSetting.LowDevicePK, RecommendBroadcastSetting.MediumDevicePK, RecommendBroadcastSetting.HighDevicePK -> LiveMode.PK
                else -> LiveMode.OneVOne
            }
        }

        updateBroadcastSetting(
            when (liveMode) {
                LiveMode.OneVOne -> when (deviceLevel) {
                    DeviceLevel.Low -> RecommendBroadcastSetting.LowDevice1v1
                    DeviceLevel.Medium -> RecommendBroadcastSetting.MediumDevice1v1
                    DeviceLevel.High -> RecommendBroadcastSetting.HighDevice1v1
                }

                LiveMode.PK -> when (deviceLevel) {
                    DeviceLevel.Low -> RecommendBroadcastSetting.LowDevicePK
                    DeviceLevel.Medium -> RecommendBroadcastSetting.MediumDevicePK
                    DeviceLevel.High -> RecommendBroadcastSetting.HighDevicePK
                }
            },
            if (broadcastStrategy == BroadcastStrategy.Smooth) when (liveMode) {
                LiveMode.OneVOne -> when (deviceLevel) {
                    DeviceLevel.Low -> if (networkLevel == NetworkLevel.Good) RecommendLowStreamVideoSetting.LowDeviceGoodNetwork1v1 else RecommendLowStreamVideoSetting.LowDeviceNormalNetwork1v1
                    DeviceLevel.Medium -> if (networkLevel == NetworkLevel.Good) RecommendLowStreamVideoSetting.MiddleDeviceGoodNetwork1v1 else RecommendLowStreamVideoSetting.MiddleDeviceNormalNetwork1v1
                    DeviceLevel.High -> if (networkLevel == NetworkLevel.Good) RecommendLowStreamVideoSetting.HighDeviceGoodNetwork1v1 else RecommendLowStreamVideoSetting.HighDeviceNormalNetwork1v1
                }

                LiveMode.PK -> when (deviceLevel) {
                    DeviceLevel.Low -> RecommendLowStreamVideoSetting.PK
                    DeviceLevel.Medium -> RecommendLowStreamVideoSetting.PK
                    DeviceLevel.High -> RecommendLowStreamVideoSetting.PK
                }
            } else null,
            isJoinedRoom,
            rtcConnection
        )
    }

    /**
     * Update broadcast setting
     *
     * @param liveMode
     * @param isLinkAudience
     * @param isJoinedRoom
     * @param rtcConnection
     */
    fun updateBroadcastSetting(
        liveMode: LiveMode,
        isLinkAudience: Boolean = false,
        isJoinedRoom: Boolean = true,
        rtcConnection: RtcConnection? = null
    ) {
        ShowLogger.d("VideoSettings", "updateBroadcastSetting2, liveMode: $liveMode, isLinkAudience: $isLinkAudience, isPKMode: $isPkMode")

        val deviceLevel = when (currBroadcastSetting) {
            RecommendBroadcastSetting.LowDevice1v1, RecommendBroadcastSetting.LowDevicePK -> DeviceLevel.Low
            RecommendBroadcastSetting.MediumDevice1v1, RecommendBroadcastSetting.MediumDevicePK -> DeviceLevel.Medium
            RecommendBroadcastSetting.HighDevice1v1, RecommendBroadcastSetting.HighDevicePK -> DeviceLevel.High
            RecommendBroadcastSetting.Audience1v1 -> DeviceLevel.High
            else -> return
        }

        val networkLevel = currNetworkLevel

        updateBroadcastSetting(
            if (isLinkAudience) RecommendBroadcastSetting.Audience1v1 else {
                when (liveMode) {
                    LiveMode.OneVOne -> when (deviceLevel) {
                        DeviceLevel.Low -> RecommendBroadcastSetting.LowDevice1v1
                        DeviceLevel.Medium -> RecommendBroadcastSetting.MediumDevice1v1
                        DeviceLevel.High -> RecommendBroadcastSetting.HighDevice1v1
                    }

                    LiveMode.PK -> when (deviceLevel) {
                        DeviceLevel.Low -> RecommendBroadcastSetting.LowDevicePK
                        DeviceLevel.Medium -> RecommendBroadcastSetting.MediumDevicePK
                        DeviceLevel.High -> RecommendBroadcastSetting.HighDevicePK
                    }
                }
            },
            if (getCurrLowStreamSetting() != null)
                if (liveMode == LiveMode.PK) RecommendLowStreamVideoSetting.PK
                else when (deviceLevel) {
                    DeviceLevel.Low -> if (networkLevel == NetworkLevel.Good) RecommendLowStreamVideoSetting.LowDeviceGoodNetwork1v1 else RecommendLowStreamVideoSetting.LowDeviceNormalNetwork1v1
                    DeviceLevel.Medium -> if (networkLevel == NetworkLevel.Good) RecommendLowStreamVideoSetting.MiddleDeviceGoodNetwork1v1 else RecommendLowStreamVideoSetting.MiddleDeviceNormalNetwork1v1
                    DeviceLevel.High -> if (networkLevel == NetworkLevel.Good) RecommendLowStreamVideoSetting.HighDeviceGoodNetwork1v1 else RecommendLowStreamVideoSetting.HighDeviceNormalNetwork1v1
                }
            else null,
            isJoinedRoom,
            rtcConnection
        )

        if (isLinkAudience && isPkMode) {
            setCurrAudienceEnhanceSwitch(false)
            updateSRSetting(SR = SuperResolution.SR_NONE)
        }
    }

    private fun updateBroadcastSetting(
        recommendSetting: BroadcastSetting,
        lowStreamSetting: LowStreamVideoSetting?,
        isJoinedRoom: Boolean,
        rtcConnection: RtcConnection? = null
    ) {
        ShowLogger.d("VideoSettings", "updateBroadcastSetting3, lowStreamSetting: $lowStreamSetting")
        setCurrBroadcastSetting(recommendSetting)
        setCurrLowStreamSetting(lowStreamSetting)
        updateRTCBroadcastSetting(
            rtcConnection,
            isJoinedRoom,
            currBroadcastSetting.video.H265,
            currBroadcastSetting.video.colorEnhance,
            currBroadcastSetting.video.lowLightEnhance,
            currBroadcastSetting.video.videoDenoiser,
            currBroadcastSetting.video.PVC,
            currBroadcastSetting.video.captureResolution,
            currBroadcastSetting.video.encodeResolution,
            currBroadcastSetting.video.frameRate,
            currBroadcastSetting.video.bitRate,
            currBroadcastSetting.video.hardwareVideoEncoder,

            currBroadcastSetting.audio.inEarMonitoring,
            currBroadcastSetting.audio.recordingSignalVolume,
            currBroadcastSetting.audio.audioMixingVolume
        )

        if (isJoinedRoom) {
            if (lowStreamSetting == null) {
                updateRTCLowStreamSetting(
                    rtcConnection,
                    false)
            } else {
                updateRTCLowStreamSetting(
                    rtcConnection,
                    true,
                    lowStreamSetting.encodeResolution,
                    lowStreamSetting.frameRate,
                    lowStreamSetting.bitRate,
                    lowStreamSetting.SVC)
            }
        }
    }

    /**
     * Update broadcast setting
     *
     * @param rtcConnection
     * @param isJoinedRoom
     * @param h265
     * @param colorEnhance
     * @param lowLightEnhance
     * @param videoDenoiser
     * @param PVC
     * @param captureResolution
     * @param encoderResolution
     * @param frameRate
     * @param bitRate
     * @param bitRateStandard
     * @param inEarMonitoring
     * @param recordingSignalVolume
     * @param audioMixingVolume
     */
    fun updateBroadcastSetting(
        rtcConnection: RtcConnection? = null,
        isJoinedRoom: Boolean = true,

        h265: Boolean? = null,
        colorEnhance: Boolean? = null,
        lowLightEnhance: Boolean? = null,
        videoDenoiser: Boolean? = null,
        PVC: Boolean? = null,
        captureResolution: Resolution? = null,
        encoderResolution: Resolution? = null,
        frameRate: FrameRate? = null,
        bitRate: Int? = null,
        bitRateRecommend: Int? = null,
        bitRateStandard: Boolean? = null,

        inEarMonitoring: Boolean? = null,
        recordingSignalVolume: Int? = null,
        audioMixingVolume: Int? = null
    ) {

        setCurrBroadcastSetting(
            BroadcastSetting(
                BroadcastSetting.Video(
                    h265 ?: currBroadcastSetting.video.H265,
                    colorEnhance ?: currBroadcastSetting.video.colorEnhance,
                    lowLightEnhance ?: currBroadcastSetting.video.lowLightEnhance,
                    videoDenoiser ?: currBroadcastSetting.video.videoDenoiser,
                    PVC ?: currBroadcastSetting.video.PVC,
                    captureResolution ?: currBroadcastSetting.video.captureResolution,
                    encoderResolution ?: currBroadcastSetting.video.encodeResolution,
                    frameRate ?: currBroadcastSetting.video.frameRate,
                    bitRate ?: currBroadcastSetting.video.bitRate,
                    bitRateRecommend ?: currBroadcastSetting.video.bitRateRecommend,
                    bitRateStandard ?: currBroadcastSetting.video.bitRateStandard,
                    true
                ),
                BroadcastSetting.Audio(
                    inEarMonitoring ?: currBroadcastSetting.audio.inEarMonitoring,
                    recordingSignalVolume ?: currBroadcastSetting.audio.recordingSignalVolume,
                    audioMixingVolume ?: currBroadcastSetting.audio.audioMixingVolume
                )
            )
        )


        var newBitRate = bitRate
        bitRateStandard?.let {
            newBitRate = if (it) {
                0
            } else {
                getRecommendBroadcastSetting().video.bitRateRecommend
            }
        }
        updateRTCBroadcastSetting(
            rtcConnection,
            isJoinedRoom,
            h265,
            colorEnhance,
            lowLightEnhance,
            videoDenoiser,
            PVC,
            captureResolution,
            encoderResolution,
            frameRate,
            newBitRate,
            true,

            inEarMonitoring,
            recordingSignalVolume,
            audioMixingVolume
        )

    }

    /**
     * Is curr broadcast setting recommend
     *
     * @return
     */
    fun isCurrBroadcastSettingRecommend(): Boolean {
        return currBroadcastSetting == RecommendBroadcastSetting.LowDevice1v1
                || currBroadcastSetting == RecommendBroadcastSetting.MediumDevice1v1
                || currBroadcastSetting == RecommendBroadcastSetting.HighDevice1v1
                || currBroadcastSetting == RecommendBroadcastSetting.LowDevicePK
                || currBroadcastSetting == RecommendBroadcastSetting.MediumDevicePK
                || currBroadcastSetting == RecommendBroadcastSetting.HighDevicePK
    }

    /**
     * Get recommend broadcast setting
     *
     * @return
     */
    fun getRecommendBroadcastSetting(): BroadcastSetting {
        return if (isPkMode) {
            when (currAudienceDeviceLevel) {
                DeviceLevel.Low -> RecommendBroadcastSetting.LowDevicePK
                DeviceLevel.Medium -> RecommendBroadcastSetting.MediumDevicePK
                else -> RecommendBroadcastSetting.HighDevicePK
            }
        } else {
            when (currAudienceDeviceLevel) {
                DeviceLevel.Low -> RecommendBroadcastSetting.LowDevice1v1
                DeviceLevel.Medium -> RecommendBroadcastSetting.MediumDevice1v1
                else -> RecommendBroadcastSetting.HighDevice1v1
            }
        }
    }


    private fun updateRTCAudioSetting(SR: SuperResolution? = null) {
        val rtcEngine = RtcEngineInstance.rtcEngine
        SR?.let {
            val enableSR = currAudienceEnhanceSwitch && SR != SuperResolution.SR_NONE
            val autoSR = currAudienceEnhanceSwitch && SR == SuperResolution.SR_AUTO
            ShowLogger.d(
                "VideoSetting",
                "SR_Config -- enable=$enableSR sr_type=$SR currAudienceEnhanceSwitch=$currAudienceEnhanceSwitch"
            )

            if (enableSR) {
                if (autoSR) {
                    rtcEngine.setParameters("{\"rtc.video.sr_max_wh\":921598}")
                    rtcEngine.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":$enableSR, \"mode\": 2}}")
                    return
                }

                rtcEngine.setParameters("{\"rtc.video.sr_max_wh\":921598}")
                rtcEngine.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":false, \"mode\": 2}}")
                rtcEngine.setParameters("{\"rtc.video.sr_type\":${SR.value}}")
            }
            rtcEngine.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":$enableSR, \"mode\": 2}}")
        }
    }


    /**
     * Update r t c broadcast setting
     *
     * @param rtcConnection
     * @param isJoinedRoom
     * @param h265
     * @param colorEnhance
     * @param lowLightEnhance
     * @param videoDenoiser
     * @param PVC
     * @param captureResolution
     * @param encoderResolution
     * @param frameRate
     * @param bitRate
     * @param hardwareVideoEncoder
     * @param inEarMonitoring
     * @param recordingSignalVolume
     * @param audioMixingVolume
     */
    private fun updateRTCBroadcastSetting(
        rtcConnection: RtcConnection? = null,
        isJoinedRoom: Boolean,

        h265: Boolean? = null,
        colorEnhance: Boolean? = null,
        lowLightEnhance: Boolean? = null,
        videoDenoiser: Boolean? = null,
        PVC: Boolean? = null,
        captureResolution: Resolution? = null,
        encoderResolution: Resolution? = null,
        frameRate: FrameRate? = null,
        bitRate: Int? = null,
        hardwareVideoEncoder: Boolean? = null,

        inEarMonitoring: Boolean? = null,
        recordingSignalVolume: Int? = null,
        audioMixingVolume: Int? = null
    ) {
        ShowLogger.d("VideoSettings", "updateRTCBroadcastSetting, frameRate:$frameRate")
        val rtcEngine = RtcEngineInstance.rtcEngine
        val videoEncoderConfiguration = RtcEngineInstance.videoEncoderConfiguration
        h265?.let {
            if (isPureMode) {
                rtcEngine.setParameters("{\"engine.video.enable_hw_encoder\":${it}}")
                rtcEngine.setParameters("{\"che.video.videoCodecIndex\": 1}")
            } else if (!isJoinedRoom) {
                rtcEngine.setParameters("{\"engine.video.enable_hw_encoder\":${it}}")
                rtcEngine.setParameters("{\"che.video.videoCodecIndex\":${if(it) 2 else 1}}")
            } else { }
        }
        colorEnhance?.let {
            rtcEngine.setColorEnhanceOptions(it, ColorEnhanceOptions())
        }
        lowLightEnhance?.let {
            rtcEngine.setLowlightEnhanceOptions(it, LowLightEnhanceOptions())
        }
        videoDenoiser?.let {
            when (currAudienceDeviceLevel) {
                DeviceLevel.High -> rtcEngine.setVideoDenoiserOptions(it, VideoDenoiserOptions(1, 2))
                DeviceLevel.Medium -> rtcEngine.setVideoDenoiserOptions(it, VideoDenoiserOptions(1, 0))
                DeviceLevel.Low -> rtcEngine.setVideoDenoiserOptions(it, VideoDenoiserOptions(1, 1))
            }
        }
        PVC?.let {
            if (!isPureMode) {
                rtcEngine.setParameters("{\"rtc.video.enable_pvc\":${it}}")
            }
        }
        if (!AgoraApplication.the().isDebugModeOpen) {
            captureResolution?.let {
                val fps: Int = frameRate?.fps.let { getCurrBroadcastSetting().video.frameRate.fps }
                rtcEngine.setCameraCapturerConfiguration(
                    CameraCapturerConfiguration(
                    CameraCapturerConfiguration.CaptureFormat(it.width, it.height, fps)
                ).apply {
                    followEncodeDimensionRatio = true
                })
            }
        }
        encoderResolution?.let {
            videoEncoderConfiguration.dimensions =
                VideoEncoderConfiguration.VideoDimensions(it.width, it.height)
            if (rtcConnection != null) {
                rtcEngine.setVideoEncoderConfigurationEx(videoEncoderConfiguration, rtcConnection)
            } else {
                rtcEngine.setVideoEncoderConfiguration(videoEncoderConfiguration)
            }
        }
        frameRate?.let {
            videoEncoderConfiguration.frameRate = it.fps
            if (rtcConnection != null) {
                rtcEngine.setVideoEncoderConfigurationEx(videoEncoderConfiguration, rtcConnection)
            } else {
                rtcEngine.setVideoEncoderConfiguration(videoEncoderConfiguration)
            }
        }
        bitRate?.let {
            videoEncoderConfiguration.bitrate = it
            if (rtcConnection != null) {
                rtcEngine.setVideoEncoderConfigurationEx(
                    videoEncoderConfiguration, rtcConnection
                )
            } else {
                rtcEngine.setVideoEncoderConfiguration(videoEncoderConfiguration)
            }
        }
        hardwareVideoEncoder?.let {
            rtcEngine.setParameters("{\"engine.video.enable_hw_encoder\":\"$hardwareVideoEncoder\"}")
        }

        inEarMonitoring?.let {
            rtcEngine.enableInEarMonitoring(it)
        }
        recordingSignalVolume?.let {
            rtcEngine.adjustRecordingSignalVolume(it)
        }
        audioMixingVolume?.let {
            if (rtcConnection != null) {
                //videoSwitcher.adjustAudioMixingVolume(rtcConnection, it)
            } else {
                rtcEngine.adjustAudioMixingVolume(it)
            }
        }
        rtcEngine.setCameraAutoFocusFaceModeEnabled(true)
    }

    /**
     * Update r t c low stream setting
     *
     * @param rtcConnection
     * @param enableLowStream
     * @param encoderResolution
     * @param frameRate
     * @param bitRate
     * @param svc
     * @param enableHardwareEncoder
     * @param isJoinedRoom
     */
    private fun updateRTCLowStreamSetting(
        rtcConnection: RtcConnection? = null,
        enableLowStream: Boolean,

        encoderResolution: Resolution? = null,
        frameRate: FrameRate? = null,
        bitRate: Int? = null,
        svc: Boolean? = null
    ) {
        ShowLogger.d("VideoSettings", "updateRTCLowStreamSetting, enableLowStream:$enableLowStream, svc:$svc")
        val rtcEngine = RtcEngineInstance.rtcEngine

        val connection = rtcConnection ?: return
        if (enableLowStream) {
            val resolution = encoderResolution ?: return
            val br = bitRate ?: return
            val fps = frameRate ?: return
            val enableSVC = svc ?: return

            if (enableSVC) {
                rtcEngine.setParameters("{\"che.video.minor_stream_num_temporal_layers\": 2}")
                rtcEngine.setParameters("{\"rtc.video.high_low_video_ratio_enabled\": true}")
                rtcEngine.setParameters("{\"che.video.enable_264_fix_svc_nego\": false}")
                rtcEngine.setParameters("{\"rtc.video.low_stream_enable_hw_encoder\": false}")
            } else {
                rtcEngine.setParameters("{\"rtc.video.high_low_video_ratio_enabled\": false}")
                rtcEngine.setParameters("{\"rtc.video.low_stream_enable_hw_encoder\": true}")
            }

            rtcEngine.setDualStreamModeEx(
                Constants.SimulcastStreamMode.ENABLE_SIMULCAST_STREAM, SimulcastStreamConfig(
                    VideoEncoderConfiguration.VideoDimensions(
                        resolution.width, resolution.height
                    ), br, fps.fps), connection)
        } else {
            rtcEngine.setDualStreamModeEx(Constants.SimulcastStreamMode.DISABLE_SIMULCAST_STREAM,SimulcastStreamConfig(), connection)
        }
    }
}