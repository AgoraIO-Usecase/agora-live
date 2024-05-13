package io.agora.scene.ktv.ktvapi

import io.agora.mediaplayer.Constants
import io.agora.mediaplayer.IMediaPlayer
import io.agora.musiccontentcenter.IAgoraMusicContentCenter
import io.agora.musiccontentcenter.Music
import io.agora.musiccontentcenter.MusicChartInfo
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine

/**
 * K t v type
 *
 * @property value
 * @constructor Create empty K t v type
 */
enum class KTVType(val value: Int)  {
    /**
     * Normal
     *
     * @constructor Create empty Normal
     */
    Normal(0),

    /**
     * Sing battle
     *
     * @constructor Create empty Sing battle
     */
    SingBattle(1),

    /**
     * Sing relay
     *
     * @constructor Create empty Sing relay
     */
    SingRelay(2)
}

/**
 * K t v music type
 *
 * @property value
 * @constructor Create empty K t v music type
 */
enum class KTVMusicType(val value: Int) {
    /**
     * Song Code
     *
     * @constructor Create empty Song Code
     */
    SONG_CODE(0),

    /**
     * Song Url
     *
     * @constructor Create empty Song Url
     */
    SONG_URL(1)
}

/**
 * K t v sing role
 *
 * @property value
 * @constructor Create empty K t v sing role
 */
enum class KTVSingRole(val value: Int) {
    /**
     * Solo singer
     *
     * @constructor Create empty Solo singer
     */
    SoloSinger(0),

    /**
     * Co singer
     *
     * @constructor Create empty Co singer
     */
    CoSinger(1),

    /**
     * Lead singer
     *
     * @constructor Create empty Lead singer
     */
    LeadSinger(2),

    /**
     * Audience
     *
     * @constructor Create empty Audience
     */
    Audience(3)
}

/**
 * K t v load music fail reason
 *
 * @property value
 * @constructor Create empty K t v load music fail reason
 */
enum class KTVLoadMusicFailReason(val value: Int) {
    /**
     * No Lyric Url
     *
     * @constructor Create empty No Lyric Url
     */
    NO_LYRIC_URL(0),

    /**
     * Music Preload Fail
     *
     * @constructor Create empty Music Preload Fail
     */
    MUSIC_PRELOAD_FAIL(1),

    /**
     * Canceled
     *
     * @constructor Create empty Canceled
     */
    CANCELED(2),

    /**
     * Get Simple Info Fail
     *
     * @constructor Create empty Get Simple Info Fail
     */
    GET_SIMPLE_INFO_FAIL(3)
}

/**
 * Switch role fail reason
 *
 * @property value
 * @constructor Create empty Switch role fail reason
 */
enum class SwitchRoleFailReason(val value: Int) {
    /**
     * Join Channel Fail
     *
     * @constructor Create empty Join Channel Fail
     */
    JOIN_CHANNEL_FAIL(0),

    /**
     * No Permission
     *
     * @constructor Create empty No Permission
     */
    NO_PERMISSION(1)
}

/**
 * K t v join chorus fail reason
 *
 * @property value
 * @constructor Create empty K t v join chorus fail reason
 */
enum class KTVJoinChorusFailReason(val value: Int) {
    /**
     * Join Channel Fail
     *
     * @constructor Create empty Join Channel Fail
     */
    JOIN_CHANNEL_FAIL(0),

    /**
     * Music Open Fail
     *
     * @constructor Create empty Music Open Fail
     */
    MUSIC_OPEN_FAIL(1)
}


/**
 * K t v load music mode
 *
 * @property value
 * @constructor Create empty K t v load music mode
 */
enum class KTVLoadMusicMode(val value: Int) {
    /**
     * Load None
     *
     * @constructor Create empty Load None
     */
    LOAD_NONE(-1),

    /**
     * Load Music Only
     *
     * @constructor Create empty Load Music Only
     */
    LOAD_MUSIC_ONLY(0),

    /**
     * Load Lrc Only
     *
     * @constructor Create empty Load Lrc Only
     */
    LOAD_LRC_ONLY(1),

    /**
     * Load Music And Lrc
     *
     * @constructor Create empty Load Music And Lrc
     */
    LOAD_MUSIC_AND_LRC(2)
}

/**
 * Music load status
 *
 * @property value
 * @constructor Create empty Music load status
 */
enum class MusicLoadStatus(val value: Int) {
    /**
     * Completed
     *
     * @constructor Create empty Completed
     */
    COMPLETED(0),

    /**
     * Failed
     *
     * @constructor Create empty Failed
     */
    FAILED(1),

    /**
     * Inprogress
     *
     * @constructor Create empty Inprogress
     */
    INPROGRESS(2),
}

/**
 * Audio track mode
 *
 * @property value
 * @constructor Create empty Audio track mode
 */
enum class AudioTrackMode(val value: Int) {
    /**
     * Yuan Chang
     *
     * @constructor Create empty Yuan Chang
     */
    YUAN_CHANG(0),

    /**
     * Ban Zou
     *
     * @constructor Create empty Ban Zou
     */
    BAN_ZOU(1),

    /**
     * Dao Chang
     *
     * @constructor Create empty Dao Chang
     */
    DAO_CHANG(2),
}

/**
 * Giant chorus route selection type
 *
 * @property value
 * @constructor Create empty Giant chorus route selection type
 */
enum class GiantChorusRouteSelectionType(val value: Int) {
    /**
     * Random
     *
     * @constructor Create empty Random
     */
    RANDOM(0),

    /**
     * By Delay
     *
     * @constructor Create empty By Delay
     */
    BY_DELAY(1),

    /**
     * Top N
     *
     * @constructor Create empty Top N
     */
    TOP_N(2),

    /**
     * By Delay And Top N
     *
     * @constructor Create empty By Delay And Top N
     */
    BY_DELAY_AND_TOP_N(3)
}

/**
 * Giant chorus route selection config
 *
 * @property type
 * @property streamNum
 * @constructor Create empty Giant chorus route selection config
 */
data class GiantChorusRouteSelectionConfig constructor(
    val type: GiantChorusRouteSelectionType,
    val streamNum: Int
)

/**
 * I lrc view
 *
 * @constructor Create empty I lrc view
 */
interface ILrcView {
    /**
     * On update pitch
     *
     * @param pitch
     */
    fun onUpdatePitch(pitch: Float?)

    /**
     * On update progress
     *
     * @param progress
     */
    fun onUpdateProgress(progress: Long?)

    /**
     * On download lrc data
     *
     * @param url
     */
    fun onDownloadLrcData(url: String?)

    /**
     * On high part time
     *
     * @param highStartTime
     * @param highEndTime
     */
    fun onHighPartTime(highStartTime: Long, highEndTime: Long)
}

/**
 * I music load state listener
 *
 * @constructor Create empty I music load state listener
 */
interface IMusicLoadStateListener {
    /**
     * On music load success
     *
     * @param songCode
     * @param lyricUrl
     */
    fun onMusicLoadSuccess(songCode: Long, lyricUrl: String)

    /**
     * On music load fail
     *
     * @param songCode
     * @param reason
     */
    fun onMusicLoadFail(songCode: Long, reason: KTVLoadMusicFailReason)

    /**
     * On music load progress
     *
     * @param songCode
     * @param percent
     * @param status
     * @param msg
     * @param lyricUrl
     */
    fun onMusicLoadProgress(songCode: Long, percent: Int, status: MusicLoadStatus, msg: String?, lyricUrl: String?)
}

/**
 * I switch role state listener
 *
 * @constructor Create empty I switch role state listener
 */
interface ISwitchRoleStateListener {
    /**
     * On switch role success
     *
     */
    fun onSwitchRoleSuccess()

    /**
     * On switch role fail
     *
     * @param reason
     */
    fun onSwitchRoleFail(reason: SwitchRoleFailReason)
}

/**
 * On join chorus state listener
 *
 * @constructor Create empty On join chorus state listener
 */
interface OnJoinChorusStateListener {
    /**
     * On join chorus success
     *
     */
    fun onJoinChorusSuccess()

    /**
     * On join chorus fail
     *
     * @param reason
     */
    fun onJoinChorusFail(reason: KTVJoinChorusFailReason)
}

/**
 * I k t v api event handler
 *
 * @constructor Create empty I k t v api event handler
 */
abstract class IKTVApiEventHandler {
    /**
     * On music player state changed
     *
     * @param state
     * @param error
     * @param isLocal
     */
    open fun onMusicPlayerStateChanged(
        state: Constants.MediaPlayerState, error: Constants.MediaPlayerError, isLocal: Boolean
    ) {
    }

    /**
     * On singer role changed
     *
     * @param oldRole
     * @param newRole
     */
    open fun onSingerRoleChanged(oldRole: KTVSingRole, newRole: KTVSingRole) {}

    /**
     * On token privilege will expire
     *
     */
    open fun onTokenPrivilegeWillExpire() {}

    /**
     * On chorus channel audio volume indication
     *
     * @param speakers
     * @param totalVolume
     */
    open fun onChorusChannelAudioVolumeIndication(
        speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
        totalVolume: Int) {}

    /**
     * On music player position changed
     *
     * @param position_ms
     * @param timestamp_ms
     */
    open fun onMusicPlayerPositionChanged(position_ms: Long, timestamp_ms: Long) {}
}

/**
 * K t v api config
 *
 * @property appId
 * @property rtmToken
 * @property engine
 * @property channelName
 * @property localUid
 * @property chorusChannelName
 * @property chorusChannelToken
 * @property maxCacheSize
 * @property type
 * @property musicType
 * @constructor Create empty K t v api config
 */
data class KTVApiConfig constructor(
    val appId: String,
    val rtmToken: String,
    val engine: RtcEngine,
    val channelName: String,
    val localUid: Int,
    val chorusChannelName: String,
    var chorusChannelToken: String,
    val maxCacheSize: Int = 10,
    val type: KTVType = KTVType.Normal,
    val musicType: KTVMusicType = KTVMusicType.SONG_CODE
)

/**
 * K t v giant chorus api config
 *
 * @property appId
 * @property rtmToken
 * @property engine
 * @property localUid
 * @property audienceChannelName
 * @property audienceChannelToken
 * @property chorusChannelName
 * @property chorusChannelToken
 * @property musicStreamUid
 * @property musicStreamToken
 * @property maxCacheSize
 * @property musicType
 * @constructor Create empty K t v giant chorus api config
 */
data class KTVGiantChorusApiConfig constructor(
    val appId: String,
    val rtmToken: String,
    val engine: RtcEngine,
    val localUid: Int,
    val audienceChannelName: String,
    val audienceChannelToken: String,
    val chorusChannelName: String,
    val chorusChannelToken: String,
    val musicStreamUid: Int,
    val musicStreamToken: String,
    val maxCacheSize: Int = 10,
    val musicType: KTVMusicType = KTVMusicType.SONG_CODE
)

/**
 * K t v load music configuration
 *
 * @property songIdentifier
 * @property mainSingerUid
 * @property mode
 * @property needPrelude
 * @constructor Create empty K t v load music configuration
 */
data class KTVLoadMusicConfiguration(
    val songIdentifier: String,
    val mainSingerUid: Int,
    val mode: KTVLoadMusicMode = KTVLoadMusicMode.LOAD_MUSIC_AND_LRC,
    val needPrelude: Boolean = false
)

/**
 * Create k t v api
 *
 * @param config
 * @return
 */
fun createKTVApi(config: KTVApiConfig): KTVApi = KTVApiImpl(config)

/**
 * Create k t v giant chorus api
 *
 * @param config
 * @return
 */
fun createKTVGiantChorusApi(config: KTVGiantChorusApiConfig): KTVApi = KTVGiantChorusApiImpl(config)

/**
 * K t v api
 *
 * @constructor Create empty K t v api
 */
interface KTVApi {

    companion object {
        // 听到远端的音量
        var remoteVolume: Int = 30
        // 本地mpk播放音量
        var mpkPlayoutVolume: Int = 50
        // mpk发布音量
        var mpkPublishVolume: Int = 50

        // 是否使用音频自采集
        var useCustomAudioSource = false
        // 调试使用，会输出更多的日志
        var debugMode = false
        // 内部测试使用，无需关注
        var mccDomain = ""
        // 大合唱的选路策略
        var routeSelectionConfig = GiantChorusRouteSelectionConfig(GiantChorusRouteSelectionType.BY_DELAY, 6)
    }

    /**
     * Renew inner data stream id
     *
     */
    fun renewInnerDataStreamId()

    /**
     * Add event handler
     *
     * @param ktvApiEventHandler
     */
    fun addEventHandler(ktvApiEventHandler: IKTVApiEventHandler)

    /**
     * Remove event handler
     *
     * @param ktvApiEventHandler
     */
    fun removeEventHandler(ktvApiEventHandler: IKTVApiEventHandler)

    /**
     * Release
     *
     */
    fun release()

    /**
     * Renew token
     *
     * @param rtmToken
     * @param chorusChannelRtcToken
     */
    fun renewToken(
        rtmToken: String,
        chorusChannelRtcToken: String
    )

    /**
     * Fetch music charts
     *
     * @param onMusicChartResultListener
     * @receiver
     */
    fun fetchMusicCharts(
        onMusicChartResultListener: (
            requestId: String?,
            status: Int,        // status=2 时token过期
            list: Array<out MusicChartInfo>?
        ) -> Unit
    )

    /**
     * Search music by music chart id
     *
     * @param musicChartId
     * @param page
     * @param pageSize
     * @param jsonOption
     * @param onMusicCollectionResultListener
     * @receiver
     */
    fun searchMusicByMusicChartId(
        musicChartId: Int,
        page: Int,
        pageSize: Int,
        jsonOption: String,
        onMusicCollectionResultListener: (
            requestId: String?,
            status: Int,         // status=2 时token过期
            page: Int,
            pageSize: Int,
            total: Int,
            list: Array<out Music>?
        ) -> Unit
    )

    /**
     * Search music by keyword
     *
     * @param keyword
     * @param page
     * @param pageSize
     * @param jsonOption
     * @param onMusicCollectionResultListener
     * @receiver
     */
    fun searchMusicByKeyword(
        keyword: String,
        page: Int, pageSize: Int,
        jsonOption: String,
        onMusicCollectionResultListener: (
            requestId: String?,
            status: Int,         // status=2 时token过期
            page: Int,
            pageSize: Int,
            total: Int,
            list: Array<out Music>?
        ) -> Unit
    )

    /**
     * Load music
     *
     * @param songCode
     * @param config
     * @param musicLoadStateListener
     */
    fun loadMusic(
        songCode: Long,
        config: KTVLoadMusicConfiguration,
        musicLoadStateListener: IMusicLoadStateListener
    )

    /**
     * Remove music
     *
     * @param songCode
     */
    fun removeMusic(songCode: Long)

    /**
     * Load music
     *
     * @param url
     * @param config
     */
    fun loadMusic(
        url: String,
        config: KTVLoadMusicConfiguration
    )

    /**
     * Load2music
     *
     * @param url1
     * @param url2
     * @param config
     */
    fun load2Music(
        url1: String,
        url2: String,
        config: KTVLoadMusicConfiguration
    )

    /**
     * Switch play src
     *
     * @param url
     * @param syncPts
     */
    fun switchPlaySrc(url: String, syncPts: Boolean)

    /**
     * Switch singer role
     *
     * @param newRole
     * @param switchRoleStateListener
     */
    fun switchSingerRole(
        newRole: KTVSingRole,
        switchRoleStateListener: ISwitchRoleStateListener?
    )

    /**
     * Start sing
     *
     * @param songCode
     * @param startPos
     */
    fun startSing(songCode: Long, startPos: Long)

    /**
     * Start sing
     *
     * @param url
     * @param startPos
     */
    fun startSing(url: String, startPos: Long)

    /**
     * Resume sing
     *
     */
    fun resumeSing()

    /**
     * Pause sing
     *
     */
    fun pauseSing()

    /**
     * Seek sing
     *
     * @param time
     */
    fun seekSing(time: Long)

    /**
     * Set lrc view
     *
     * @param view
     */
    fun setLrcView(view: ILrcView)

    /**
     * Mute mic
     *
     * @param mute
     */
    fun muteMic(mute: Boolean)

    /**
     * Set audio playout delay
     *
     * @param audioPlayoutDelay
     */
    fun setAudioPlayoutDelay(audioPlayoutDelay: Int)

    /**
     * Get media player
     *
     * @return
     */
    fun getMediaPlayer() : IMediaPlayer

    /**
     * Get music content center
     *
     * @return
     */
    fun getMusicContentCenter() : IAgoraMusicContentCenter

    /**
     * Switch audio track
     *
     * @param mode
     */
    fun switchAudioTrack(mode: AudioTrackMode)

    /**
     * Enable professional streamer mode
     *
     * @param enable
     */
    fun enableProfessionalStreamerMode(enable: Boolean)

    /**
     * Enable mulitpathing
     *
     * @param enable
     */
    fun enableMulitpathing(enable: Boolean)
}