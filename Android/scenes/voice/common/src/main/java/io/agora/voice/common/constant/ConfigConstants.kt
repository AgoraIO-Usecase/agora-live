package io.agora.voice.common.constant

/**
 * This object holds all the constant values used across the application.
 * It includes constants for enabling and disabling alpha, default volume, room types, sound selections, AINS modes, bot speakers, volume types, AINS sound types, and mic constants.
 */
object ConfigConstants {

    /**
     * Alpha value when enabled.
     */
    const val ENABLE_ALPHA = 1.0f

    /**
     * Alpha value when disabled.
     */
    const val DISABLE_ALPHA = 0.2f

    /**
     * Default volume for the bot.
     */
    const val RotDefaultVolume = 50

    /**
     * Constants for different types of rooms.
     */
    object RoomType {
        const val Common_Chatroom = 0
        const val Spatial_Chatroom = 1
    }

    /**
     * Constants for different sound selections.
     */
    object SoundSelection {
        const val Social_Chat = 1
        const val Karaoke = 2
        const val Gaming_Buddy = 3
        const val Professional_Broadcaster = 4
    }

    /**
     * Text representations of different sound selections.
     */
    object SoundSelectionText {
        const val Social_Chat = "Social Chat"
        const val Karaoke = "Karaoke"
        const val Gaming_Buddy = "Gaming Buddy"
        const val Professional_Broadcaster = "Professional podcaster"
    }

    /**
     * Constants for different AINS modes.
     */
    object AINSMode {
        const val AINS_High = 0
        const val AINS_Medium = 1
        const val AINS_Off = 2
        const val AINS_Unknown = -1
    }

    /**
     * Constants for different bot speakers.
     */
    object BotSpeaker {
        const val None = -1
        const val BotBlue = 0
        const val BotRed = 1
        const val BotBoth = 2
    }

    /**
     * Constants for different volume types.
     */
    object VolumeType {
        const val Volume_None = 0
        const val Volume_Low = 1
        const val Volume_Medium = 2
        const val Volume_High = 3
        const val Volume_Max = 4
    }

    /**
     * Constants for different AINS sound types.
     */
    object AINSSoundType {
        const val AINS_TVSound = 1
        const val AINS_KitchenSound = 2
        const val AINS_StreetSound = 3
        const val AINS_MachineSound = 4
        const val AINS_OfficeSound = 5
        const val AINS_HomeSound = 6
        const val AINS_ConstructionSound = 7
        const val AINS_AlertSound = 8
        const val AINS_ApplauseSound = 9
        const val AINS_WindSound = 10
        const val AINS_MicPopFilterSound = 11
        const val AINS_AudioFeedback = 12
        const val AINS_MicrophoneFingerRub = 13
        const val AINS_MicrophoneScreenTap = 14
    }

    /**
     * Constants for different mic keys and their corresponding indices.
     */
    object MicConstant {
        const val KeyMic0 = "mic_0"
        const val KeyMic1 = "mic_1"
        const val KeyMic2 = "mic_2"
        const val KeyMic3 = "mic_3"
        const val KeyMic4 = "mic_4"
        const val KeyMic5 = "mic_5"
        const val KeyMic6 = "mic_6"
        const val KeyMic7 = "mic_7"

        const val KeyIndex0 = 0
        const val KeyIndex1 = 1
        const val KeyIndex2 = 2
        const val KeyIndex3 = 3
        const val KeyIndex4 = 4
        const val KeyIndex5 = 5
        const val KeyIndex6 = 6
        const val KeyIndex7 = 7

        /**
         * A map of mic keys to their corresponding indices.
         */
        val micMap: MutableMap<String, Int> by lazy {
            mutableMapOf(
                KeyMic0 to KeyIndex0,
                KeyMic1 to KeyIndex1,
                KeyMic2 to KeyIndex2,
                KeyMic3 to KeyIndex3,
                KeyMic4 to KeyIndex4,
                KeyMic5 to KeyIndex5,
                KeyMic6 to KeyIndex6,
                KeyMic7 to KeyIndex7
            )
        }
    }
}