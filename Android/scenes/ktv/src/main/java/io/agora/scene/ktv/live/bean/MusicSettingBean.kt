package io.agora.scene.ktv.live.bean

import io.agora.rtc2.Constants
import io.agora.scene.ktv.live.fragmentdialog.EarPhoneCallback
import io.agora.scene.ktv.live.fragmentdialog.MusicSettingCallback

/**
 * A e c level
 *
 * @property value
 * @constructor Create empty A e c level
 */
enum class AECLevel(val value: Int) {
    /**
     * Standard
     *
     * @constructor Create empty Standard
     */
    Standard(0),

    /**
     * High
     *
     * @constructor Create empty High
     */
    High(1),

    /**
     * Ultra high
     *
     * @constructor Create empty Ultra high
     */
    UltraHigh(2),
}

/**
 * A i n s mode
 *
 * @property value
 * @constructor Create empty A i n s mode
 */
enum class AINSMode(val value: Int) {
    /**
     * Close
     *
     * @constructor Create empty Close
     */
    Close(0),

    /**
     * Medium
     *
     * @constructor Create empty Medium
     */
    Medium(1),

    /**
     * High
     *
     * @constructor Create empty High
     */
    High(2),
}

/**
 * Ear back mode
 *
 * @property value
 * @constructor Create empty Ear back mode
 */
enum class EarBackMode(val value: Int) {
    /**
     * Auto
     *
     * @constructor Create empty Auto
     */
    Auto(0),

    /**
     * Open s l
     *
     * @constructor Create empty Open s l
     */
    OpenSL(1),

    /**
     * Oboe
     *
     * @constructor Create empty Oboe
     */
    Oboe(2),
}

/**
 * Scoring difficulty mode
 *
 * @property value
 * @constructor Create empty Scoring difficulty mode
 */
enum class ScoringDifficultyMode(val value: Int) {
    /**
     * Low
     *
     * @constructor Create empty Low
     */
    Low(0),

    /**
     * Recommend
     *
     * @constructor Create empty Recommend
     */
    Recommend(15),

    /**
     * High
     *
     * @constructor Create empty High
     */
    High(30),
}

/**
 * Music setting bean
 *
 * @property mSettingCallback
 * @constructor Create empty Music setting bean
 */
class MusicSettingBean constructor(private val mSettingCallback: MusicSettingCallback) {

    companion object {
        /**
         *
         * If the identity is the lead singer and co-singer, during the performance, the vocal volume and background music volume should remain at their original settings, while the remote volume should automatically be set to 30.
         * If the identity is the lead singer and co-singer, when the performance is paused or the song is changed, the vocal volume and accompaniment volume should remain at their original settings, while the remote volume should automatically switch to 100.
         * If the identity is a host on stage (not participating in the chorus but on stage), the vocal volume, accompaniment volume, and remote volume should all remain at their original settings.
         */
        const val DEFAULT_MIC_VOL = 100 // default vocal volume 100
        const val DEFAULT_ACC_VOL = 50 // default accompaniment volume 50
        const val DEFAULT_REMOTE_SINGER_VOL = 30  // Lead singer/co-singer, during the performance, the default remote volume is 30.
        const val DEFAULT_REMOTE_VOL = 100  // Lead singer/co-singer，When not performing, the default remote volume is 100.
        const val DEFAULT_EAR_BACK_VOL = 100  // default ear monitoring volume

        const val DEFAULT_AIAEC_STRENGTH = 1  // Default AIAEC, with an intensity of 1.
    }

    var mEarPhoneCallback: EarPhoneCallback? = null

    /**
     * ear monitoring switch
     */
    var mEarBackEnable: Boolean = false
        set(newValue) {
            field = newValue
            mSettingCallback.onEarChanged(newValue)
        }

    /**
     * ear monitoring mode: 0 (automatic), 1 (force OpenSL), 2 (force Oboe).”
     */
    var mEarBackMode = EarBackMode.Auto
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onEarBackModeChanged(newValue.value)
        }

    /**
     * ear monitoring volume
     */
    var mEarBackVolume = DEFAULT_EAR_BACK_VOL
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onEarBackVolumeChanged(newValue)
        }

    /**
     * Is there a headset
     */
    var mHasEarPhone = false
        set(newValue) {
            field = newValue
            mEarPhoneCallback?.onHasEarPhoneChanged(newValue)
        }

    /**
     * ear monitoring delay
     */
    var mEarBackDelay = 0
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mEarPhoneCallback?.onEarMonitorDelay(newValue)
        }

    /**
     * Vocal Volume
     * If the identity is the lead singer and co-singer, during the performance, the vocal volume and accompaniment volume should remain at their original settings, while the remote volume automatically switches to 30.
     * If the identity is the lead singer and co-singer, when the performance is paused or the song is changed, the vocal volume and accompaniment volume should remain at their original settings, while the remote volume automatically switches to 100.
     * If the identity is a host on stage (not participating in the chorus but on stage), the vocal volume, accompaniment volume, and remote volume should all remain at their original settings.
     */
    var mMicVolume = DEFAULT_MIC_VOL
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onMicVolChanged(newValue)
        }

    /**
     *
     * Accompaniment Volume
     *
     * 	If the identity is the lead singer and co-singer, during the performance, the vocal volume and accompaniment volume should remain at their original settings, while the remote volume automatically switches to 30.
     * 	If the identity is the lead singer and co-singer, when the performance is paused or the song is changed, the vocal volume and accompaniment volume should remain at their original settings, while the remote volume automatically switches to 100.
     * 	If the identity is a host on stage (not participating in the chorus but on stage), the vocal volume, accompaniment volume, and remote volume should all remain at their original settings.
     */
    var mAccVolume = DEFAULT_ACC_VOL
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onAccVolChanged(newValue)
        }

    /**
     *
     * Remote Volume
     *
     * 	If the identity is the lead singer and co-singer, during the performance, the vocal volume and accompaniment volume should remain at their original settings, while the remote volume automatically switches to 30.
     * 	If the identity is the lead singer and co-singer, when the performance is paused or the song is changed, the vocal volume and accompaniment volume should remain at their original settings, while the remote volume automatically switches to 100.
     * 	If the identity is a host on stage (not participating in the chorus but on stage), the vocal volume, accompaniment volume, and remote volume should all remain at their original settings.
     */
    var mRemoteVolume = DEFAULT_REMOTE_VOL
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onRemoteVolChanged(newValue)
        }

    /**
     * Sound Effects, default KTV.
     */
    var mAudioEffect: Int = Constants.ROOM_ACOUSTICS_KTV
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onAudioEffectChanged(newValue)
        }

    /**
     * Update audio effect
     *
     * @param audioEffect
     */
    fun updateAudioEffect(audioEffect: Int) {
        this.mAudioEffect = audioEffect
    }

    /**
     * Scoring Difficulty: Low difficulty 0, recommended difficulty 15, high difficulty 30.
     * It can only be set before the song starts, and cannot be switched during the performance.
     */
    var mScoringDifficultyMode = ScoringDifficultyMode.Recommend
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onScoringDifficultyChanged(newValue.value)
        }

    /**
     * professional mode
     */
    var mProfessionalModeEnable = false
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onProfessionalModeChanged(newValue)
        }

    /**
     * multiPath switch
     */
    var mMultiPathEnable = true
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onMultiPathChanged(newValue)
        }

    /**
     * Sound quality 0(16K),1(24K),2(48K)
     */
    var mAecLevel = AECLevel.Standard
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onAECLevelChanged(newValue.value)
        }

    /**
     *  background noise reduction: 0 (Off), 1 (Medium), 2 (High).
     */
    var mAinsMode = AINSMode.Close
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onAINSModeChanged(newValue.value)
        }

    /**
     * low latency mode.
     */
    var mLowLatencyMode = true
        set(newValue) {
            if (field == newValue) return
            field = newValue
            mSettingCallback.onLowLatencyModeChanged(newValue)
        }

    /**
     * AIAEC Switch
     */
    var mAIAECEnable = true
        set(newValue) {
            field = newValue
            mSettingCallback.onAIAECChanged(newValue)
        }

    /**
     * AIAEC Strength
     */
    var mAIAECStrength: Int = DEFAULT_AIAEC_STRENGTH
        set(newValue) {
            field = newValue
            mSettingCallback.onAIAECStrengthSelect(newValue)
        }

    /**
     * Get effect index
     *
     * @param index
     * @return
     */
    fun getEffectIndex(index: Int): Int {
        when (index) {
            0 -> return Constants.ROOM_ACOUSTICS_KTV
            1 -> return Constants.AUDIO_EFFECT_OFF
            2 -> return Constants.ROOM_ACOUSTICS_VOCAL_CONCERT
            3 -> return Constants.ROOM_ACOUSTICS_STUDIO
            4 -> return Constants.ROOM_ACOUSTICS_PHONOGRAPH
            5 -> return Constants.ROOM_ACOUSTICS_SPACIAL
            6 -> return Constants.ROOM_ACOUSTICS_ETHEREAL
            7 -> return Constants.STYLE_TRANSFORMATION_POPULAR
            8 -> return Constants.STYLE_TRANSFORMATION_RNB
        }
        // default ktv
        return Constants.ROOM_ACOUSTICS_KTV
    }
}