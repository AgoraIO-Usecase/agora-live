package io.agora.scene.voice.model.constructor

import io.agora.scene.voice.model.SoundAudioBean
import io.agora.voice.common.utils.ResourcesTools
import io.agora.voice.common.constant.ConfigConstants

/**
 * @author create by zhangwei03
 */
object RoomSoundAudioConstructor {

    // https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemochat/aisound/01CreateRoomCommonChatroom/EN/01-01-B-EN.wav
    private const val BASE_URL = "https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemochat/aisound"

    private const val EN = "EN"
    private const val CreateCommonRoom = "/01CreateRoomCommonChatroom%1\$s"
    private const val SocialChat = "/03SoundSelectionSocialChat%1\$s"
    private const val Karaoke = "/04SoundSelectionKaraoke%1\$s"
    private const val GamingBuddy = "/05SoundSelectionGamingBuddy%1\$s"
    private const val ProfessionalBroadcaster = "/06SoundProfessionalBroadcaster%1\$s"
    private const val AINSIntroduce = "/07AINSIntroduce%1\$s"
    //AI噪⾳ 08AINSTVSound/EN/High/08-01-B-CN-High.wav  1./08AINSTVSound/EN/High 2./08-01-B 3.CN 4.High.wav
    private const val AINSSound = "%1\$s%2\$s-%3\$s-%4\$s"
    private const val AINS_TVSound = "/08AINSTVSound"
    private const val AINS_KitchenSound = "/09AINSKitchenSound"
    private const val AINS_StreetSound = "/10AINStreetSound"
    private const val AINS_MachineSound = "/11AINSRobotSound"
    private const val AINS_OfficeSound = "/12AINSOfficeSound"
    private const val AINS_HomeSound = "/13AINSHomeSound"
    private const val AINS_ConstructionSound = "/14AINSConstructionSound"
    private const val AINS_AlertSound = "/15AINSAlertSound"
    private const val AINS_ApplauseSound = "/16AINSApplause"
    private const val AINS_WindSound = "/17AINSWindSound"
    private const val AINS_MicPopFilterSound = "/18AINSMicPopFilter"
    private const val AINS_AudioFeedback = "/19AINSAudioFeedback"
    private const val AINS_MicrophoneFingerRub = "/20ANISMicrophoneFingerRubSound"
    private const val AINS_MicrophoneScreenTap = "/21ANISScreenTapSound"


    private const val CreateCommonRoomSoundId = 100
    private const val SocialChatSoundId = 300
    private const val KaraokeSoundId = 400
    private const val GamingBuddySoundId = 500
    private const val ProfessionalBroadcasterSoundId = 600
    private const val AINSIntroduceSoundId = 700
    private const val AINSSoundId = 800

    val createRoomSoundAudioMap: Map<Int, List<SoundAudioBean>> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableMapOf(
            ConfigConstants.RoomType.Common_Chatroom to
                    mutableListOf(
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            CreateCommonRoomSoundId + 1,
                            getCreateCommonRoomUrl("/EN/01-01-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            CreateCommonRoomSoundId + 2,
                            getCreateCommonRoomUrl("/EN/01-02-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBoth,
                            CreateCommonRoomSoundId + 3,
                            getCreateCommonRoomUrl("/EN/01-03-B&R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            CreateCommonRoomSoundId + 4,
                            getCreateCommonRoomUrl("/EN/01-04-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            CreateCommonRoomSoundId + 5,
                            getCreateCommonRoomUrl("/EN/01-05-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            CreateCommonRoomSoundId + 6,
                            getCreateCommonRoomUrl("/EN/01-06-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            CreateCommonRoomSoundId + 7,
                            getCreateCommonRoomUrl("/EN/01-07-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            CreateCommonRoomSoundId + 8,
                            getCreateCommonRoomUrl("/EN/01-08-B-EN.wav")
                        )
                    ),
        )
    }

    val soundSelectionAudioMap: Map<Int, List<SoundAudioBean>> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableMapOf(
            ConfigConstants.SoundSelection.Social_Chat to
                    mutableListOf(
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            SocialChatSoundId + 1,
                            getSocialChatUrl("/EN/03-01-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            SocialChatSoundId + 2,
                            getSocialChatUrl("/EN/03-02-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            SocialChatSoundId + 3,
                            getSocialChatUrl("/EN/03-03-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            SocialChatSoundId + 4,
                            getSocialChatUrl("/EN/03-04-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            SocialChatSoundId + 5,
                            getSocialChatUrl("/EN/03-05-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            SocialChatSoundId + 6,
                            getSocialChatUrl("/EN/03-06-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            SocialChatSoundId + 7,
                            getSocialChatUrl("/EN/03-07-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            SocialChatSoundId + 8,
                            getSocialChatUrl("/EN/03-08-R-EN.wav")
                        )
                    ),
            ConfigConstants.SoundSelection.Karaoke to
                    mutableListOf(
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            KaraokeSoundId + 1,
                            getKaraokeUrl("/EN/04-01-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            KaraokeSoundId + 2,
                            getKaraokeUrl("/EN/04-02-B-EN.wav")
                        ),
                    ),
            ConfigConstants.SoundSelection.Gaming_Buddy to
                    mutableListOf(
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            GamingBuddySoundId + 1,
                            getGamingBuddyUrl("/EN/05-01-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            GamingBuddySoundId + 2,
                            getGamingBuddyUrl("/EN/05-02-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            GamingBuddySoundId + 3,
                            getGamingBuddyUrl("/EN/05-03-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            GamingBuddySoundId + 4,
                            getGamingBuddyUrl("/EN/05-04&05-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            GamingBuddySoundId + 6,
                            getGamingBuddyUrl("/EN/05-06-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            GamingBuddySoundId + 7,
                            getGamingBuddyUrl("/EN/05-07-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            GamingBuddySoundId + 8,
                            getGamingBuddyUrl("/EN/05-08-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            GamingBuddySoundId + 9,
                            getGamingBuddyUrl("/EN/05-09-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            GamingBuddySoundId + 10,
                            getGamingBuddyUrl("/EN/05-10-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            GamingBuddySoundId + 11,
                            getGamingBuddyUrl("/EN/05-11-R-EN.wav")
                        ),
                    ),
            ConfigConstants.SoundSelection.Professional_Broadcaster to
                    mutableListOf(
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            ProfessionalBroadcasterSoundId + 1,
                            getProfessionalBroadcasterUrl("/EN/06-01-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            ProfessionalBroadcasterSoundId + 2,
                            getProfessionalBroadcasterUrl("/EN/06-02-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            ProfessionalBroadcasterSoundId + 3,
                            getProfessionalBroadcasterUrl("/EN/06-03-R-EN.wav")
                        ),
                    )
        )
    }

    val anisIntroduceAudioMap: Map<Int, List<SoundAudioBean>> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableMapOf(
            ConfigConstants.AINSMode.AINS_High to
                    mutableListOf(
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 1,
                            getANISIntroduceUrl("/EN/Share/07-01-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            AINSIntroduceSoundId + 2,
                            getANISIntroduceUrl("/EN/High/07-02-B-EN-High.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 3,
                            getANISIntroduceUrl("/EN/High/07-03-R-EN-High.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            AINSIntroduceSoundId + 4,
                            getANISIntroduceUrl("/EN/Share/07-04-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 5,
                            getANISIntroduceUrl("/EN/Share/07-05-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            AINSIntroduceSoundId + 6,
                            getANISIntroduceUrl("/EN/Share/07-06-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 7,
                            getANISIntroduceUrl("/EN/Share/07-07-R-EN.wav")
                        )
                    ),
            ConfigConstants.AINSMode.AINS_Medium to
                    mutableListOf(
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 1,
                            getANISIntroduceUrl("/EN/Share/07-01-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            AINSIntroduceSoundId + 2,
                            getANISIntroduceUrl("/EN/Medium/07-02-B-EN-Medium.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 3,
                            getANISIntroduceUrl("/EN/Medium/07-03-R-EN-Medium.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            AINSIntroduceSoundId + 4,
                            getANISIntroduceUrl("/EN/Share/07-04-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 5,
                            getANISIntroduceUrl("/EN/Share/07-05-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            AINSIntroduceSoundId + 6,
                            getANISIntroduceUrl("/EN/Share/07-06-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 7,
                            getANISIntroduceUrl("/EN/Share/07-07-R-EN.wav")
                        )
                    ),
            ConfigConstants.AINSMode.AINS_Off to
                    mutableListOf(
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 1,
                            getANISIntroduceUrl("/EN/Share/07-01-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            AINSIntroduceSoundId + 2,
                            getANISIntroduceUrl("/EN/None/07-02-B-EN-None.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 3,
                            getANISIntroduceUrl("/EN/None/07-03-R-EN-None.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            AINSIntroduceSoundId + 4,
                            getANISIntroduceUrl("/EN/Share/07-04-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 5,
                            getANISIntroduceUrl("/EN/Share/07-05-R-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotBlue,
                            AINSIntroduceSoundId + 6,
                            getANISIntroduceUrl("/EN/Share/07-06-B-EN.wav")
                        ),
                        SoundAudioBean(
                            ConfigConstants.BotSpeaker.BotRed,
                            AINSIntroduceSoundId + 7,
                            getANISIntroduceUrl("/EN/Share/07-07-R-EN.wav")
                        )
                    )
        )
    }

    val AINSSoundMap: Map<Int, SoundAudioBean> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        mutableMapOf(
            ConfigConstants.AINSSoundType.AINS_TVSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 1,
                getAINSSoundUrl(AINS_TVSound, "/08-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_TVSound, "/08-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_TVSound, "/08-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_KitchenSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 2,
                getAINSSoundUrl(AINS_KitchenSound, "/09-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_KitchenSound, "/09-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_KitchenSound, "/09-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_StreetSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 3,
                getAINSSoundUrl(AINS_StreetSound, "/10-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_StreetSound, "/10-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_StreetSound, "/10-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_MachineSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 4,
                getAINSSoundUrl(AINS_MachineSound, "/11-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_MachineSound, "/11-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_MachineSound, "/11-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_OfficeSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 5,
                getAINSSoundUrl(AINS_OfficeSound, "/12-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_OfficeSound, "/12-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_OfficeSound, "/12-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_HomeSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 6,
                getAINSSoundUrl(AINS_HomeSound, "/13-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_HomeSound, "/13-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_HomeSound, "/13-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_ConstructionSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 7,
                getAINSSoundUrl(AINS_ConstructionSound, "/14-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_ConstructionSound, "/14-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_ConstructionSound, "/14-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_AlertSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 8,
                getAINSSoundUrl(AINS_AlertSound, "/15-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_AlertSound, "/15-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_AlertSound, "/15-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_ApplauseSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 9,
                getAINSSoundUrl(AINS_ApplauseSound, "/16-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_ApplauseSound, "/16-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_ApplauseSound, "/16-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_WindSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 10,
                getAINSSoundUrl(AINS_WindSound, "/17-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_WindSound, "/17-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_WindSound, "/17-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_MicPopFilterSound to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 11,
                getAINSSoundUrl(AINS_MicPopFilterSound, "/18-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_MicPopFilterSound, "/18-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_MicPopFilterSound, "/18-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_AudioFeedback to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 12,
                getAINSSoundUrl(AINS_AudioFeedback, "/19-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_AudioFeedback, "/19-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_AudioFeedback, "/19-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_MicrophoneFingerRub to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 13,
                getAINSSoundUrl(AINS_MicrophoneFingerRub, "/20-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_MicrophoneFingerRub, "/20-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_MicrophoneFingerRub, "/20-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
            ConfigConstants.AINSSoundType.AINS_MicrophoneScreenTap to SoundAudioBean(
                ConfigConstants.BotSpeaker.BotBlue,
                AINSSoundId + 14,
                getAINSSoundUrl(AINS_MicrophoneScreenTap, "/21-01-B", ConfigConstants.AINSMode.AINS_Off),
                getAINSSoundUrl(AINS_MicrophoneScreenTap, "/21-01-B", ConfigConstants.AINSMode.AINS_High),
                getAINSSoundUrl(AINS_MicrophoneScreenTap, "/21-01-B", ConfigConstants.AINSMode.AINS_Medium)
            ),
        )
    }

    private fun getCreateCommonRoomUrl(audioPath: String): String {
        return BASE_URL + String.format(CreateCommonRoom, audioPath)
    }

    private fun getSocialChatUrl(audioPath: String): String {
        return BASE_URL + String.format(SocialChat, audioPath)
    }

    private fun getKaraokeUrl(audioPath: String): String {
        return BASE_URL + String.format(Karaoke, audioPath)
    }

    private fun getGamingBuddyUrl(audioPath: String): String {
        return BASE_URL + String.format(GamingBuddy, audioPath)
    }

    private fun getProfessionalBroadcasterUrl(audioPath: String): String {
        return BASE_URL + String.format(ProfessionalBroadcaster, audioPath)
    }

    private fun getANISIntroduceUrl(audioPath: String): String {
        return BASE_URL + String.format(AINSIntroduce, audioPath)
    }

    private fun getAINSSoundUrl(audioPathPrefix: String, audioPath: String, ainsMode: Int): String {
        val local = EN

        val audioPathP = when (ainsMode) {
            ConfigConstants.AINSMode.AINS_High -> "$audioPathPrefix/$local/High"
            ConfigConstants.AINSMode.AINS_Medium -> "$audioPathPrefix/$local/Medium"
            else -> "$audioPathPrefix/$local/None"
        }
        val ainsPath = when (ainsMode) {
            ConfigConstants.AINSMode.AINS_High -> "High.wav"
            ConfigConstants.AINSMode.AINS_Medium -> "Medium.wav"
            else -> "None.wav"
        }
        //AI噪⾳ 08AINSTVSound/EN/High/08-01-B-EN-High.wav  1./08AINSTVSound 2.CN 3.high 4.08-01-B 5.CN 6.High
        return BASE_URL + String.format(AINSSound,audioPathP, audioPath, local, ainsPath)
    }
}