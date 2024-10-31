package io.agora.scene.ktv.ktvapi

import io.agora.mediaplayer.Constants
import io.agora.mediaplayer.IMediaPlayer
import io.agora.musiccontentcenter.IAgoraMusicContentCenter
import io.agora.musiccontentcenter.Music
import io.agora.musiccontentcenter.MusicChartInfo
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine

/**
 * KTV scene types
 * @param Normal Regular solo or group singing.
 * @param SingBattle Sing battle
 * @param SingRelay Sing relay
 */
enum class KTVType(val value: Int) {
    Normal(0),
    SingBattle(1),
    SingRelay(2)
}

/**
 * KTV song type
 * @param SONG_CODE mcc songCode
 * @param SONG_URL local song url
 */
enum class KTVMusicType(val value: Int) {
    SONG_CODE(0),
    SONG_URL(1)
}

/**
 * Identity in KTVApi.
 * @param SoloSinger The solo singer; currently, only the user is singing.
 * @param CoSinger The co-singer; to join the chorus, you need to switch roles to “CoSinger” by calling switchSingerRole.
 * @param LeadSinger The lead singer; after chorus members join, you need to switch roles to “LeadSinger” by calling switchSingerRole.
 * @param Audience The audience; default status.
 */
enum class KTVSingRole(val value: Int) {
    SoloSinger(0),
    CoSinger(1),
    LeadSinger(2),
    Audience(3)
}

/**
 * Reasons for loadMusic failure
 * @param NO_LYRIC_URL No lyrics available; does not affect normal music playback.
 * @param MUSIC_PRELOAD_FAIL Music loading failed.
 * @param CANCELED This loading has been canceled.
 * @param GET_SIMPLE_INFO_FAIL
 */
enum class KTVLoadMusicFailReason(val value: Int) {
    NO_LYRIC_URL(0),
    MUSIC_PRELOAD_FAIL(1),
    CANCELED(2),
    GET_SIMPLE_INFO_FAIL(3)
}

/**
 * Reasons for switchSingerRole failure
 * @param JOIN_CHANNEL_FAIL Failed to join channel2.
 * @param NO_PERMISSION An incorrect target role was passed to switchSingerRole (cannot switch from the current role to the target role).
 */
enum class SwitchRoleFailReason(val value: Int) {
    JOIN_CHANNEL_FAIL(0),
    NO_PERMISSION(1)
}

/**
 * Reasons for joining the chorus failure
 * @param JOIN_CHANNEL_FAIL Failed to join the chorus sub-channel.
 * @param MUSIC_OPEN_FAIL Failed to open the song.
 */
enum class KTVJoinChorusFailReason(val value: Int) {
    JOIN_CHANNEL_FAIL(0),
    MUSIC_OPEN_FAIL(1)
}

/**
 * Music loading modes
 * @param LOAD_MUSIC_ONLY Only load music (typically used before joining a chorus).
 * @param LOAD_LRC_ONLY Only load lyrics (typically used by the audience when a song starts playing).
 * @param LOAD_MUSIC_AND_LRC Default mode, load both lyrics and music (typically used by the lead singer when a song starts playing).
 */
enum class KTVLoadMusicMode(val value: Int) {
    LOAD_NONE(-1),
    LOAD_MUSIC_ONLY(0),
    LOAD_LRC_ONLY(1),
    LOAD_MUSIC_AND_LRC(2)
}

/**
 * Music loading status
 * @param COMPLETED Loading completed, progress is 100.
 * @param FAILED Loading failed.
 * @param INPROGRESS Loading in progress.
 */
enum class MusicLoadStatus(val value: Int) {
    COMPLETED(0),
    FAILED(1),
    INPROGRESS(2),
}

/**
 * Music track mode
 * @param YUAN_CHANG Original: When the lead singer activates the original track, they hear the original track, and the audience hears the original track.
 * @param BAN_ZOU Accompaniment: When the lead singer activates the accompaniment, they hear the accompaniment, and the audience hears the accompaniment.
 * @param DAO_CHANG Guide: When the lead singer activates the guide track, they hear the original track, and the audience hears the accompaniment.
 */
enum class AudioTrackMode(val value: Int) {
    YUAN_CHANG(0),
    BAN_ZOU(1),
    DAO_CHANG(2),
}

/**
 * Audio stream selection strategy for singers to listen to each other's audio streams in a large chorus
 * @param RANDOM Randomly select a few streams.
 * @param BY_DELAY Select the streams with the lowest delay.
 * @param TOP_N Select streams based on audio strength.
 * @param BY_DELAY_AND_TOP_N Simultaneously start delay-based selection and audio strength selection.
 */
enum class GiantChorusRouteSelectionType(val value: Int) {
    RANDOM(0),
    BY_DELAY(1),
    TOP_N(2),
    BY_DELAY_AND_TOP_N(3)
}

/**
 * Configuration for singers to listen to each other's audio streams in a large chorus
 * @param type Selection strategy
 * @param streamNum Maximum number of streams to select (recommended: 6)
 */
data class GiantChorusRouteSelectionConfig constructor(
    val type: GiantChorusRouteSelectionType,
    val streamNum: Int
)

/**
 * Lyric component interface. The lyric component you pass to setLrcView must inherit from this interface and implement the following methods.
 */
interface ILrcView {
    /**
     * The ktvApi will actively call this method to pass the pitch value to your lyric component when updating the pitch internally.
     * @param pitch The pitch value
     */
    fun onUpdatePitch(pitch: Float?)

    /**
     * The ktvApi will actively call this method to pass the progress value to your lyric component when updating the music playback progress internally, with a callback every 50ms.
     * @param progress The actual playback progress of the song, with a callback every 20ms.
     */
    fun onUpdateProgress(progress: Long?)

    /**
     * The ktvApi will actively call this method to pass the lyric address (URL) to your lyric component when it retrieves the lyric address. You need to complete the lyric download within this callback.
     * @param url The lyric address.
     */
    fun onDownloadLrcData(url: String?)

    /**
     * The ktvApi will call this method to callback the chorus segment start and end time to the lyric component when
     * it retrieves the singbattle
     * @param highStartTime The start time of the chorus segment.
     * @param highEndTime The end time of the chorus segment.
     */
    fun onHighPartTime(highStartTime: Long, highEndTime: Long)
}

/**
 * Music loading status interface
 */
interface IMusicLoadStateListener {
    /**
     * Music loading succeeded
     * @param songCode The song code, which is consistent with the songCode passed in loadMusic
     * @param lyricUrl The URL of the lyrics
     */
    fun onMusicLoadSuccess(songCode: Long, lyricUrl: String)

    /**
     * Music loading failed
     * @param songCode The song code of the failed loading
     * @param reason The reason for the music loading failure
     */
    fun onMusicLoadFail(songCode: Long, reason: KTVLoadMusicFailReason)

    /**
     * Music loading progress
     * @param songCode The song code
     * @param percent The loading progress of the song
     * @param status The loading status of the song
     * @param msg Related information
     * @param lyricUrl The URL of the lyrics
     */
    fun onMusicLoadProgress(songCode: Long, percent: Int, status: MusicLoadStatus, msg: String?, lyricUrl: String?)
}

/**
 * Interface for switching singing role status
 */
interface ISwitchRoleStateListener {
    /**
     * Successfully switched singing role
     */
    fun onSwitchRoleSuccess()

    /**
     * Failed to switch singing role
     * @param reason The reason for the failure to switch singing role
     */
    fun onSwitchRoleFail(reason: SwitchRoleFailReason)
}

/**
 * Interface On join chorus state listener
 *
 * @constructor Create empty On join chorus state listener
 */
interface OnJoinChorusStateListener {
    /**
     * Successfully join the chorus
     *
     */
    fun onJoinChorusSuccess()

    /**
     * Failed to join the chorus
     *
     * @param reason
     */
    fun onJoinChorusFail(reason: KTVJoinChorusFailReason)
}

/**
 * KTVApi Event Callbacks
 */
abstract class IKTVApiEventHandler {
    /**
     * Player state change
     * @param state MediaPlayer playback state
     * @param reason MediaPlayer error information
     * @param isLocal Whether the Player information is from local or the main singer
     */
    open fun onMusicPlayerStateChanged(
        state: Constants.MediaPlayerState, reason: Constants.MediaPlayerReason, isLocal: Boolean
    ) {
    }

    /**
     * Internal role switching in ktvApi
     * @param oldRole Old role
     * @param newRole New role
     */
    open fun onSingerRoleChanged(oldRole: KTVSingRole, newRole: KTVSingRole) {}

    /**
     * Callback indicating that the rtm or chorus channel token is about to expire; needs to renew this token
     */
    open fun onTokenPrivilegeWillExpire() {}

    /**
     * Audio volume indication in the chorus channel
     * @param speakers Information on the volume of different users
     * @param totalVolume Total volume
     */
    open fun onChorusChannelAudioVolumeIndication(
        speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?,
        totalVolume: Int
    ) {
    }

    /**
     * Playback progress callback
     * @param position_ms Music playback progress
     */
    open fun onMusicPlayerPositionChanged(position_ms: Long, timestamp_ms: Long) {}
}

/**
 * Initialize KTVApi configuration
 * @param appId Used to initialize the Mcc Engine
 * @param rtmToken Required to create the Mcc Engine
 * @param engine RTC engine object
 * @param channelName Channel number; the sub-channel name is generated based on the main channel name + "_ex" fixed rule
 * @param localUid Used for creating the Mcc engine and joining the sub-channel
 * @param chorusChannelName Sub-channel name, required for joining the sub-channel
 * @param chorusChannelToken Sub-channel token, required for joining the sub-channel
 * @param maxCacheSize Maximum number of cached songs
 * @param type KTV scene type
 * @param musicType Music type
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
 * Initialize KTVGiantChorusApi configuration
 * @param appId Used to initialize the Mcc Engine
 * @param rtmToken Required to create the Mcc Engine
 * @param engine RTC engine object
 * @param localUid Used for creating the Mcc engine and joining the sub-channel
 * @param audienceChannelName Audience channel name, required for joining the audience channel
 * @param chorusChannelToken Audience channel token, required for joining the audience channel
 * @param chorusChannelName Singing channel name, required for joining the singing channel
 * @param chorusChannelToken Singing channel token, required for joining the singing channel
 * @param musicStreamUid Music UID pushed by the lead singer into the channel
 * @param musicStreamToken Music stream token
 * @param maxCacheSize Maximum number of cached songs
 * @param musicType Music type
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
 * Configuration for loading songs. Loading of the next song is not allowed until the current song has completed loading (both success and failure count as completion).
 * @param songIdentifier Song ID, usually set by the business side to distinguish each song with a different SongId.
 * @param mainSingerUid UID of the lead singer. If it's a backing singer, they need to mute the main singer's sound in the main channel based on this information.
 * @param mode The mode for loading the song, defaulting to loading both music and lyrics.
 * @param needPrelude Indicates whether to play in the case of a sliced song.
 */
data class KTVLoadMusicConfiguration(
    val songIdentifier: String,
    val mainSingerUid: Int,
    val mode: KTVLoadMusicMode = KTVLoadMusicMode.LOAD_MUSIC_AND_LRC,
    val needPrelude: Boolean = false
)

/**
 * Creates an instance of the regular chorus KTVApi.
 */
fun createKTVApi(config: KTVApiConfig): KTVApi = KTVApiImpl(config)

/**
 * Creates an instance of the giant chorus KTVApi.
 */
fun createKTVGiantChorusApi(config: KTVGiantChorusApiConfig): KTVApi = KTVGiantChorusApiImpl(config)

/**
 * KTVApi Interface
 */
interface KTVApi {

    companion object {
        // Volume of the remote audio
        var remoteVolume: Int = 30

        // Volume of the local MPK playback
        var mpkPlayoutVolume: Int = 50

        // Volume of the MPK publishing
        var mpkPublishVolume: Int = 50

        // Whether to use custom audio source
        var useCustomAudioSource = false

        // Debug mode, will output more logs
        var debugMode = false

        // For internal testing purposes, no need to pay attention to this
        var mccDomain = ""

        // Route selection strategy for giant chorus
        var routeSelectionConfig = GiantChorusRouteSelectionConfig(GiantChorusRouteSelectionType.BY_DELAY, 6)
    }

    /**
     * Updates the internal streamId used by KTVApi; this needs to be updated each time joining a channel.
     */
    fun renewInnerDataStreamId()

    /**
     * Subscribes to KTVApi events, allowing multiple registrations.
     * @param ktvApiEventHandler An instance of the KTVApi event interface.
     */
    fun addEventHandler(ktvApiEventHandler: IKTVApiEventHandler)

    /**
     * Unsubscribes from KTVApi events.
     * @param ktvApiEventHandler An instance of the KTVApi event interface.
     */
    fun removeEventHandler(ktvApiEventHandler: IKTVApiEventHandler)

    /**
     * Clears internal variables/caches, cancels listeners set during initWithRtcEngine,
     * and cancels network requests, etc.
     */
    fun release()

    /**
     * When the IKTVApiEventHandler.onTokenPrivilegeWillExpire callback is received,
     * this method needs to be called to actively update the token.
     * @param rtmToken The RTM token required by the musicContentCenter module.
     * @param chorusChannelRtcToken The RTC token required for the chorus channel.
     */
    fun renewToken(
        rtmToken: String,
        chorusChannelRtcToken: String
    )

    /**
     * Fetches the music chart.
     * @param onMusicChartResultListener Callback for the chart list results.
     */
    fun fetchMusicCharts(
        onMusicChartResultListener: (
            requestId: String?,
            status: Int,        // status=2 token expired
            list: Array<out MusicChartInfo>?
        ) -> Unit
    )

    /**
     * Retrieves the song list based on the type of music chart.
     * @param musicChartId The ID of the music chart.
     * @param page The page number for the song list callback.
     * @param pageSize The size of the page for the song list callback.
     * @param jsonOption Custom filtering options.
     * @param onMusicCollectionResultListener Callback for the song list results.
     */
    fun searchMusicByMusicChartId(
        musicChartId: Int,
        page: Int,
        pageSize: Int,
        jsonOption: String,
        onMusicCollectionResultListener: (
            requestId: String?,
            status: Int,         // status=2 token expired
            page: Int,
            pageSize: Int,
            total: Int,
            list: Array<out Music>?
        ) -> Unit
    )

    /**
     * Searches for songs based on a keyword.
     * @param keyword The keyword to search for.
     * @param page The page number for the song list callback.
     * @param pageSize The size of the page for the song list callback.
     * @param jsonOption Custom filtering options.
     * @param onMusicCollectionResultListener Callback for the song list results.
     */
    fun searchMusicByKeyword(
        keyword: String,
        page: Int, pageSize: Int,
        jsonOption: String,
        onMusicCollectionResultListener: (
            requestId: String?,
            status: Int,         // status=2 token expired
            page: Int,
            pageSize: Int,
            total: Int,
            list: Array<out Music>?
        ) -> Unit
    )

    /**
     * Asynchronously loads a song; only one song can be loaded at a time.
     * The result of loadSong will be notified to the business layer via a callback.
     * @param songCode The unique code for the song.
     * @param config The configuration for loading the song.
     * @param musicLoadStateListener The callback for the song loading result.
     *
     * Recommended usage:
     * At the start of the song:
     * The main singer should call loadMusic(KTVLoadMusicConfiguration(mode=LOAD_MUSIC_AND_LRC, songCode, mainSingerUid))
     * and then switchSingerRole(SoloSinger).
     * Audience should call loadMusic(KTVLoadMusicConfiguration(mode=LOAD_LRC_ONLY, songCode, mainSingerUid)).
     * When joining a chorus:
     * The singer preparing to join the chorus should call loadMusic(KTVLoadMusicConfiguration(mode=LOAD_MUSIC_ONLY, songCode, mainSingerUid)).
     * After loadMusic is successful, switchSingerRole(CoSinger).
     */
    fun loadMusic(
        songCode: Long,
        config: KTVLoadMusicConfiguration,
        musicLoadStateListener: IMusicLoadStateListener
    )

    /**
     * Cancels the loading of a song. This will interrupt the loading process
     * and remove the song from the cache.
     * @param songCode The unique code for the song.
     */
    fun removeMusic(songCode: Long)

    /**
     * Loads a song synchronously. Only one song can be loaded at a time.
     * This method is typically used when the song has been preloaded
     * successfully (the URL is a local file path).
     *
     * Recommended usage:
     * When the song starts:
     * - Main singer: loadMusic(KTVLoadMusicConfiguration(mode=LOAD_MUSIC_AND_LRC, url, mainSingerUid))
     *   then switchSingerRole(SoloSinger).
     * - Audience: loadMusic(KTVLoadMusicConfiguration(mode=LOAD_LRC_ONLY, url, mainSingerUid)).
     *
     * When joining a chorus:
     * - For those preparing to join: loadMusic(KTVLoadMusicConfiguration(mode=LOAD_MUSIC_ONLY, url, mainSingerUid)).
     * - After loadMusic is successful, switchSingerRole(CoSinger).
     *
     * @param url The address of the song.
     * @param config Configuration for loading the song.
     */
    fun loadMusic(
        url: String,
        config: KTVLoadMusicConfiguration
    )

    /**
     * Loads a song synchronously. Only one song can be loaded at a time.
     * This method is typically used when the song has been preloaded
     * successfully (the URLs are local file paths).
     *
     * Recommended usage:
     * When the song starts:
     * - Main singer: loadMusic(KTVLoadMusicConfiguration(mode=LOAD_MUSIC_AND_LRC, url1, mainSingerUid))
     *   then switchSingerRole(SoloSinger).
     * - Audience: loadMusic(KTVLoadMusicConfiguration(mode=LOAD_LRC_ONLY, url1, mainSingerUid)).
     *
     * When joining a chorus:
     * - For those preparing to join: loadMusic(KTVLoadMusicConfiguration(mode=LOAD_MUSIC_ONLY, url1, mainSingerUid)).
     * - After loadMusic is successful, switchSingerRole(CoSinger).
     *
     * @param config Configuration for loading the song; defaults to playing url1.
     * @param url1 The address of the first song.
     * @param url2 The address of the second song.
     */
    fun load2Music(
        url1: String,
        url2: String,
        config: KTVLoadMusicConfiguration
    )

    /**
     * Switches the playback resource between multiple files.
     * The provided URL must be one of the parameters (url1 or url2) used in load2Music.
     *
     * @param url The playback resource to switch to, which must be either url1 or url2 from load2Music.
     * @param syncPts Whether to synchronize the start position between the previous and new playback resources:
     *                true for synchronization, false for no synchronization (starts from 0).
     */
    fun switchPlaySrc(url: String, syncPts: Boolean)

    /**
     * Asynchronously switches the singing role, with the result notified to the business layer via a callback.
     *
     * @param newRole The new singing role to switch to.
     * @param switchRoleStateListener The listener to handle the result of the role switch.
     *
     * Allowed calling paths:
     * 1. Audience -> SoloSinger when playing a song they selected.
     * 2. Audience -> LeadSinger when playing a song they selected, and there are already singers joined when the song starts.
     * 3. SoloSinger -> Audience when the solo performance ends.
     * 4. Audience -> CoSinger when joining a duet.
     * 5. CoSinger -> Audience when exiting the duet.
     * 6. SoloSinger -> LeadSinger when the first duet singer joins, switching the main singer from solo to lead.
     * 7. LeadSinger -> SoloSinger when the last duet singer exits, switching the main singer from lead to solo.
     * 8. LeadSinger -> Audience when ending the song as the lead singer.
     */
    fun switchSingerRole(
        newRole: KTVSingRole,
        switchRoleStateListener: ISwitchRoleStateListener?
    )

    /**
     * Starts playing a song based on its unique code.
     *
     * @param songCode The unique identifier for the song.
     * @param startPos The position (in milliseconds) to start playback.
     */
    fun startSing(songCode: Long, startPos: Long)

    /**
     * Starts playing a song based on its URL.
     *
     * @param url The address of the song.
     * @param startPos The position (in milliseconds) to start playback.
     */
    fun startSing(url: String, startPos: Long)

    /**
     * Resumes playback of the currently playing song.
     */
    fun resumeSing()

    /**
     * Pauses the playback of the current song.
     */
    fun pauseSing()

    /**
     * Adjusts the playback position to the specified time.
     *
     * @param time The target position (in milliseconds) to seek to.
     */
    fun seekSing(time: Long)

    /**
     * Sets the lyrics component, which can be updated at any time.
     *
     * @param view The lyrics component view that needs to implement ILrcView and its three interfaces.
     */
    fun setLrcView(view: ILrcView)

    /**
     * Toggles the microphone on or off.
     *
     * @param mute true to mute the microphone, false to unmute.
     */
    fun muteMic(mute: Boolean)

    /**
     * Sets the current audio playback delay, applicable for scenarios with custom audio capture.
     *
     * @param audioPlayoutDelay The time difference between audio frame processing and playback.
     */
    fun setAudioPlayoutDelay(audioPlayoutDelay: Int)

    /**
     * Sets the current audio playback delay, applicable for self-captured audio.
     *
     * @param audioPlayoutDelay The time difference between audio frame processing and playback (in milliseconds).
     */
    fun getMediaPlayer(): IMediaPlayer

    /**
     * Retrieves the instance of the media player.
     *
     * @return An instance of IMediaPlayer.
     */
    fun getMusicContentCenter(): IAgoraMusicContentCenter

    /**
     * Switches the audio track between the original singer, accompaniment, or guide vocals.
     *
     * @param mode The mode to switch to, defined by AudioTrackMode.
     */
    fun switchAudioTrack(mode: AudioTrackMode)

    /**
     * Enables or disables professional mode. The default setting is off.
     *
     * @param enable true to enable professional mode, false to disable.
     */
    fun enableProfessionalStreamerMode(enable: Boolean)

    /**
     * Enables or disables multipathing. The default setting is enabled.
     *
     * @param enable true to enable multipathing, false to disable.
     */
    fun enableMulitpathing(enable: Boolean)
}