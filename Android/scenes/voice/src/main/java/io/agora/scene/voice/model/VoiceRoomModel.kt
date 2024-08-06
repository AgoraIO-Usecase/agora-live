package io.agora.scene.voice.model

import androidx.annotation.DrawableRes
import io.agora.scene.voice.model.annotation.MicClickAction
import io.agora.voice.common.constant.ConfigConstants
import java.io.Serializable

/**
 * This interface represents a base room bean in the VR application.
 * It extends Serializable and IKeepProguard.
 */
interface BaseRoomBean : IKeepProguard, Serializable {
}

/**
 * This data class represents a bot mic info bean in the VR application.
 * It includes two VoiceMicInfoModel objects, one for the blue bot and one for the red bot.
 */
data class BotMicInfoBean constructor(
    var blueBot: VoiceMicInfoModel,
    var redBot: VoiceMicInfoModel
) : BaseRoomBean

/**
 * This data class represents a mic manager bean in the VR application.
 * It includes a name, a boolean indicating whether the mic is enabled, and an action to perform when the mic is clicked.
 */
data class MicManagerBean constructor(
    val name: String,
    var enable: Boolean = true,
    @MicClickAction var micClickAction: Int = MicClickAction.Invite
) : BaseRoomBean

/**
 * This data class represents the audio settings of a room in the VR application.
 * It includes various settings related to audio, such as whether audio is enabled, the room type, whether the bot is open, the bot volume, the sound selection, the AINS mode, whether spatial audio is open, whether AI AEC is on, whether AI AGC is on, whether ear feedback is on, and the voice changer mode.
 */
data class RoomAudioSettingsBean constructor(
    var enable: Boolean = true, // Whether audio is enabled
    var roomType: Int = 0,
    var botOpen: Boolean = false,
    var botVolume: Int = ConfigConstants.RotDefaultVolume,
    var soundSelection: Int = ConfigConstants.SoundSelection.Social_Chat,
    var AINSMode: Int = ConfigConstants.AINSMode.AINS_Medium,
    var spatialOpen: Boolean = false,
    var isAIAECOn: Boolean = false,
    var isAIAGCOn: Boolean = false,
    var isEarBckOn: Boolean = false,
    var voiceChangerMode: Int = 0,
) : BaseRoomBean

/**
 * This data class represents an AINS mode bean in the VR application.
 * It includes the name of the AINS mode and the AINS mode itself.
 */
data class AINSModeBean constructor(
    val anisName: String = "",
    var anisMode: Int = ConfigConstants.AINSMode.AINS_Medium // Default
) : BaseRoomBean

/**
 * This data class represents an AINS sounds bean in the VR application.
 * It includes the type of the AINS sound, the name of the AINS sound, the subname of the AINS sound, and the AINS mode.
 */
data class AINSSoundsBean constructor(
    val soundType: Int = ConfigConstants.AINSSoundType.AINS_TVSound,
    val soundName: String = "",
    val soundSubName: String = "",
    var soundMode: Int = ConfigConstants.AINSMode.AINS_Unknown
) : BaseRoomBean

/**
 * This data class represents a sound audio bean in the VR application.
 * It includes the type of the speaker, the sound ID, the URL of the audio, the URL of the high noise reduction audio, and the URL of the medium noise reduction audio.
 */
data class SoundAudioBean constructor(
    val speakerType: Int, // The type of the speaker
    var soundId: Int,
    var audioUrl: String, // The URL of the audio
    var audioUrlHigh: String = "", // The URL of the high noise reduction audio
    var audioUrlMedium: String = "", // The URL of the medium noise reduction audio
) : BaseRoomBean

/**
 * This data class represents a sound selection bean in the VR application.
 * It includes the type of the sound selection, the index, the name of the sound, the introduction of the sound, and a boolean indicating whether it is currently being used.
 */
data class SoundSelectionBean constructor(
    val soundSelectionType: Int = ConfigConstants.SoundSelection.Social_Chat,
    val index: Int = 0,
    val soundName: String = "",
    val soundIntroduce: String = "",
    var isCurrentUsing: Boolean = false,
) :BaseRoomBean

/**
 * This data class represents a customer usage bean in the VR application.
 * It includes the name of the customer and the avatar of the customer.
 */
data class CustomerUsageBean constructor(
    val name: String? = "",
    @DrawableRes val avatar: Int = 0
) : BaseRoomBean