package io.agora.scene.ktv.ktvapi

import android.os.Handler
import android.os.Looper
import com.google.protobuf.util.JsonFormat.Printer
import io.agora.mediaplayer.Constants
import io.agora.mediaplayer.Constants.MediaPlayerState
import io.agora.mediaplayer.IMediaPlayer
import io.agora.mediaplayer.IMediaPlayerObserver
import io.agora.mediaplayer.data.CacheStatistics
import io.agora.mediaplayer.data.PlayerPlaybackStats
import io.agora.mediaplayer.data.PlayerUpdatedInfo
import io.agora.mediaplayer.data.SrcInfo
import io.agora.musiccontentcenter.*
import io.agora.rtc2.*
import io.agora.rtc2.Constants.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.*

class KTVGiantChorusApiImpl(
   val giantChorusApiConfig: KTVGiantChorusApiConfig
) : KTVApi, IMusicContentCenterEventHandler, IMediaPlayerObserver, IRtcEngineEventHandler() {

    companion object {
        private val scheduledThreadPool: ScheduledExecutorService = Executors.newScheduledThreadPool(5)
        private const val tag = "KTV_API_LOG_GIANT"
        private const val version = "5.0.0"
        private const val lyricSyncVersion = 2
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var mRtcEngine: RtcEngineEx = giantChorusApiConfig.engine as RtcEngineEx
    private lateinit var mMusicCenter: IAgoraMusicContentCenter
    private var mPlayer: IMediaPlayer
    private val apiReporter: APIReporter = APIReporter(APIType.KTV, version, mRtcEngine)

    private var innerDataStreamId: Int = 0
    private var singChannelRtcConnection: RtcConnection? = null
    private var subChorusConnection: RtcConnection? = null
    private var mpkConnection: RtcConnection? = null

    private var mainSingerUid: Int = 0
    private var songCode: Long = 0
    private var songUrl: String = ""
    private var songUrl2: String = ""
    private var songIdentifier: String = ""

    private val lyricCallbackMap =
        mutableMapOf<String, (songNo: Long, lyricUrl: String?) -> Unit>() // (requestId, callback)
    private val lyricSongCodeMap = mutableMapOf<String, Long>() // (requestId, songCode)
    private val loadMusicCallbackMap =
        mutableMapOf<String, (songCode: Long,
                              percent: Int,
                              status: Int,
                              msg: String?,
                              lyricUrl: String?) -> Unit>() // (songNo, callback)
    private val musicChartsCallbackMap =
        mutableMapOf<String, (requestId: String?, errorCode: Int, list: Array<out MusicChartInfo>?) -> Unit>()
    private val musicCollectionCallbackMap =
        mutableMapOf<String, (requestId: String?, errorCode: Int, page: Int, pageSize: Int, total: Int, list: Array<out Music>?) -> Unit>()

    private var lrcView: ILrcView? = null

    private var localPlayerPosition: Long = 0
    private var localPlayerSystemTime: Long = 0

    // Real-time refresh of lyrics.
    private var mReceivedPlayPosition: Long = 0 // The position of the player playback, in milliseconds.
    private var mLastReceivedPlayPosTime: Long? = null

    // event
    private var ktvApiEventHandlerList = mutableListOf<IKTVApiEventHandler>()
    private var mainSingerHasJoinChannelEx: Boolean = false

    // Chorus calibration.
    private var audioPlayoutDelay = 0

    // pitch
    private var pitch = 0.0

    // Whether the microphone is on.
    private var isOnMicOpen = false
    private var isRelease = false

    // mpk status
    private var mediaPlayerState: MediaPlayerState = MediaPlayerState.PLAYER_STATE_IDLE

    private var professionalModeOpen = false
    private var audioRouting = 0
    private var isPublishAudio = false // Determine by whether to send audio stream.

    // Singing score.
    private var singingScore = 0

    // multipath
    private var enableMultipathing = true

    // Whether the lyrics information comes from the data stream.
    private var recvFromDataStream = false

    // Start playing lyrics.
    private var mStopDisplayLrc = true
    private var displayLrcFuture: ScheduledFuture<*>? = null
    private val displayLrcTask = object : Runnable {
        override fun run() {
            if (!mStopDisplayLrc){
                if (singerRole == KTVSingRole.Audience && !recvFromDataStream) return  // audioMetaData audience return
                val lastReceivedTime = mLastReceivedPlayPosTime ?: return
                val curTime = System.currentTimeMillis()
                val offset = curTime - lastReceivedTime
                if (offset <= 100) {
                    val curTs = mReceivedPlayPosition + offset
                    if (singerRole == KTVSingRole.LeadSinger || singerRole == KTVSingRole.SoloSinger) {
                        val lrcTime = LrcTimeOuterClass.LrcTime.newBuilder()
                            .setTypeValue(LrcTimeOuterClass.MsgType.LRC_TIME.number)
                            .setForward(true)
                            .setSongId(songIdentifier)
                            .setTs(curTs)
                            .setUid(giantChorusApiConfig.musicStreamUid)
                            .build()

                        mRtcEngine.sendAudioMetadataEx(lrcTime.toByteArray(), mpkConnection)
                    }
                    runOnMainThread {
                        lrcView?.onUpdatePitch(pitch.toFloat())
                        // (fix ENT-489)Make lyrics delay for 200ms
                        // Per suggestion from Bob, it has a intrinsic buffer/delay between sound and `onPositionChanged(Player)`,
                        // such as AEC/Player/Device buffer.
                        // We choose the estimated 200ms.
                        lrcView?.onUpdateProgress(if (curTs > 200) (curTs - 200) else curTs) // The delay here will impact both singer and audience side
                    }
                }
            }
        }
    }

    // Score-driven mixing.
    private var mSyncScoreFuture :ScheduledFuture<*>? = null
    private var mStopSyncScore = true
    private val mSyncScoreTask = Runnable {
        if (!mStopSyncScore) {
            if (mediaPlayerState == MediaPlayerState.PLAYER_STATE_PLAYING &&
                (singerRole == KTVSingRole.LeadSinger || singerRole == KTVSingRole.CoSinger)) {
                sendSyncScore()
            }
        }
    }

    // Cloud merging information.
    private var mSyncCloudConvergenceStatusFuture :ScheduledFuture<*>? = null
    private var mStopSyncCloudConvergenceStatus = true
    private val mSyncCloudConvergenceStatusTask = Runnable {
        if (!mStopSyncCloudConvergenceStatus && singerRole == KTVSingRole.LeadSinger) {
            sendSyncCloudConvergenceStatus()
        }
    }

    init {
        apiReporter.reportFuncEvent("initialize", mapOf("config" to giantChorusApiConfig), mapOf())
        this.singChannelRtcConnection = RtcConnection(giantChorusApiConfig.chorusChannelName, giantChorusApiConfig.localUid)

        // ------------------ Initialize content center. ------------------
        if (giantChorusApiConfig.musicType == KTVMusicType.SONG_CODE) {
            val contentCenterConfiguration = MusicContentCenterConfiguration()
            contentCenterConfiguration.appId = giantChorusApiConfig.appId
            contentCenterConfiguration.mccUid = giantChorusApiConfig.localUid.toLong()
            contentCenterConfiguration.token = giantChorusApiConfig.rtmToken
            contentCenterConfiguration.maxCacheSize = giantChorusApiConfig.maxCacheSize
            if (KTVApi.debugMode) {
                contentCenterConfiguration.mccDomain = KTVApi.mccDomain
            }
            mMusicCenter = IAgoraMusicContentCenter.create(mRtcEngine)
            mMusicCenter.initialize(contentCenterConfiguration)
            mMusicCenter.registerEventHandler(this)

            // ------------------ Initialize music player instance. ------------------
            mPlayer = mMusicCenter.createMusicPlayer()
        } else {
            mPlayer = mRtcEngine.createMediaPlayer()
        }
        mPlayer.adjustPublishSignalVolume(KTVApi.mpkPublishVolume)
        mPlayer.adjustPlayoutVolume(KTVApi.mpkPlayoutVolume)

        // register observer
        mPlayer.registerPlayerObserver(this)
        setKTVParameters()
        startDisplayLrc()
        startSyncScore()
        startSyncCloudConvergenceStatus()
        isRelease = false

        mPlayer.setPlayerOption("play_pos_change_callback", 100)
    }

    // log printer
    private fun ktvApiLog(msg: String) {
        if (isRelease) return
        apiReporter.writeLog("[$tag] $msg", LOG_LEVEL_INFO)
    }

    // log printer
    private fun ktvApiLogError(msg: String) {
        if (isRelease) return
        apiReporter.writeLog("[$tag] $msg", LOG_LEVEL_ERROR)
    }

    override fun renewInnerDataStreamId() {
        apiReporter.reportFuncEvent("renewInnerDataStreamId", mapOf(), mapOf())

        val innerCfg = DataStreamConfig()
        innerCfg.syncWithAudio = true
        innerCfg.ordered = false
        this.innerDataStreamId = mRtcEngine.createDataStreamEx(innerCfg, singChannelRtcConnection)
    }

    private fun setKTVParameters() {
        mRtcEngine.setParameters("{\"rtc.enable_nasa2\": true}")
        mRtcEngine.setParameters("{\"rtc.ntp_delay_drop_threshold\":1000}")
        mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp\": true}")
        mRtcEngine.setParameters("{\"rtc.net.maxS2LDelay\": 800}")
        mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp_broadcast\":true}")

        mRtcEngine.setParameters("{\"che.audio.neteq.enable_stable_playout\":true}")
        mRtcEngine.setParameters("{\"che.audio.neteq.targetlevel_offset\": 20}")

        mRtcEngine.setParameters("{\"rtc.net.maxS2LDelayBroadcast\":400}")
        mRtcEngine.setParameters("{\"che.audio.neteq.prebuffer\":true}")
        mRtcEngine.setParameters("{\"che.audio.neteq.prebuffer_max_delay\":600}")
        mRtcEngine.setParameters("{\"che.audio.max_mixed_participants\": 8}")
        mRtcEngine.setParameters("{\"che.audio.custom_bitrate\": 48000}")
        mRtcEngine.setParameters("{\"che.audio.uplink_apm_async_process\": true}")

        // Standard sound quality.
        mRtcEngine.setParameters("{\"che.audio.aec.split_srate_for_48k\": 16000}")

        // ENT-901
        mRtcEngine.setParameters("{\"che.audio.ans.noise_gate\": 20}")

        // Android Only
        mRtcEngine.setParameters("{\"che.audio.enable_estimated_device_delay\":false}")

        // TopN + SendAudioMetadata
        mRtcEngine.setParameters("{\"rtc.use_audio4\": true}")

        // mutipath
        enableMultipathing = false
        //mRtcEngine.setParameters("{\"rtc.enableMultipath\": true}")
        mRtcEngine.setParameters("{\"rtc.enable_tds_request_on_join\": true}")
        //mRtcEngine.setParameters("{\"rtc.remote_path_scheduling_strategy\": 0}")
        //mRtcEngine.setParameters("{\"rtc.path_scheduling_strategy\": 0}")

        // Data reporting.
        mRtcEngine.setParameters("{\"rtc.direct_send_custom_event\": true}")
    }

    private fun resetParameters() {
        mRtcEngine.setAudioScenario(AUDIO_SCENARIO_GAME_STREAMING)
        mRtcEngine.setParameters("{\"che.audio.custom_bitrate\": 80000}")     // Compatible with previous profile = 3 setting
        mRtcEngine.setParameters("{\"che.audio.max_mixed_participants\": 3}") // Normal mixing for 3 downstream streams
        mRtcEngine.setParameters("{\"che.audio.neteq.prebuffer\": false}")    // Disable rapid alignment mode for the receiver
        mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp\": false}") // Disable multi-end synchronization for the audience
        mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp_broadcast\": false}") // Disable multi-end synchronization for the host
    }

    override fun addEventHandler(ktvApiEventHandler: IKTVApiEventHandler) {
        apiReporter.reportFuncEvent("addEventHandler", mapOf("ktvApiEventHandler" to ktvApiEventHandler), mapOf())
        ktvApiEventHandlerList.add(ktvApiEventHandler)
    }

    override fun removeEventHandler(ktvApiEventHandler: IKTVApiEventHandler) {
        apiReporter.reportFuncEvent("removeEventHandler", mapOf("ktvApiEventHandler" to ktvApiEventHandler), mapOf())
        ktvApiEventHandlerList.remove(ktvApiEventHandler)
    }

    override fun release() {
        apiReporter.reportFuncEvent("release", mapOf(), mapOf())
        if (isRelease) return
        isRelease = true
        singerRole = KTVSingRole.Audience

        resetParameters()
        stopSyncCloudConvergenceStatus()
        stopSyncScore()
        stopDisplayLrc()
        this.mLastReceivedPlayPosTime = null
        this.mReceivedPlayPosition = 0
        this.innerDataStreamId = 0
        this.singingScore = 0

        lyricCallbackMap.clear()
        loadMusicCallbackMap.clear()
        musicChartsCallbackMap.clear()
        musicCollectionCallbackMap.clear()
        lrcView = null

        mPlayer.unRegisterPlayerObserver(this)

        if (giantChorusApiConfig.musicType == KTVMusicType.SONG_CODE) {
            mMusicCenter.unregisterEventHandler()
        }

        mPlayer.stop()
        mPlayer.destroy()
        IAgoraMusicContentCenter.destroy()

        mainSingerHasJoinChannelEx = false
        professionalModeOpen = false
        audioRouting = 0
        isPublishAudio = false
    }

    override fun enableProfessionalStreamerMode(enable: Boolean) {
        apiReporter.reportFuncEvent("enableProfessionalStreamerMode", mapOf("enable" to enable), mapOf())
        this.professionalModeOpen = enable
        processAudioProfessionalProfile()
    }

    private fun processAudioProfessionalProfile() {
        ktvApiLog("processAudioProfessionalProfile: audioRouting: $audioRouting, professionalModeOpen: $professionalModeOpen， isPublishAudio：$isPublishAudio")
        if (!isPublishAudio) return // Must be a participant with the microphone on.
        if (professionalModeOpen) {
            // professional
            if (audioRouting == 0 || audioRouting == 2 || audioRouting == 5 || audioRouting == 6) {
                // Headphones: Disable 3A, disable MD.
                mRtcEngine.setParameters("{\"che.audio.aec.enable\": false}")
                mRtcEngine.setParameters("{\"che.audio.agc.enable\": false}")
                mRtcEngine.setParameters("{\"che.audio.ans.enable\": false}")
                mRtcEngine.setParameters("{\"che.audio.md.enable\": false}")
                mRtcEngine.setAudioProfile(AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO) // AgoraAudioProfileMusicHighQualityStereo
            } else {
                // Non-professional: Enable 3A and disable MD
                mRtcEngine.setParameters("{\"che.audio.aec.enable\": true}")
                mRtcEngine.setParameters("{\"che.audio.agc.enable\": true}")
                mRtcEngine.setParameters("{\"che.audio.ans.enable\": true}")
                mRtcEngine.setParameters("{\"che.audio.md.enable\": false}")
                mRtcEngine.setAudioProfile(AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO) // AgoraAudioProfileMusicHighQualityStereo
            }
        } else {
            // Non-professional: Enable 3A and disable MD
            mRtcEngine.setParameters("{\"che.audio.aec.enable\": true}")
            mRtcEngine.setParameters("{\"che.audio.agc.enable\": true}")
            mRtcEngine.setParameters("{\"che.audio.ans.enable\": true}")
            mRtcEngine.setParameters("{\"che.audio.md.enable\": false}")
            mRtcEngine.setAudioProfile(AUDIO_PROFILE_MUSIC_STANDARD_STEREO) // AgoraAudioProfileMusicStandardStereo
        }
    }

    override fun enableMulitpathing(enable: Boolean) {
        apiReporter.reportFuncEvent("enableMulitpathing", mapOf("enable" to enable), mapOf())
        this.enableMultipathing = enable

        if (singerRole == KTVSingRole.LeadSinger || singerRole == KTVSingRole.CoSinger) {
            subChorusConnection?.let {
                mRtcEngine.setParametersEx("{\"rtc.enableMultipath\": $enable, \"rtc.path_scheduling_strategy\": 0, \"rtc.remote_path_scheduling_strategy\": 0}", it)
            }
        }
    }

    override fun renewToken(rtmToken: String, chorusChannelRtcToken: String) {
        apiReporter.reportFuncEvent("renewToken", mapOf(), mapOf())
        // renew RtmToken
        mMusicCenter.renewToken(rtmToken)
        // renew chorus channel RtcToken
        if (subChorusConnection != null) {
            val channelMediaOption = ChannelMediaOptions()
            channelMediaOption.token = chorusChannelRtcToken
            mRtcEngine.updateChannelMediaOptionsEx(channelMediaOption, subChorusConnection)
        }
    }

    // 1、Audience -》SoloSinger
    // 2、Audience -》LeadSinger
    // 3、SoloSinger -》Audience
    // 4、Audience -》CoSinger
    // 5、CoSinger -》Audience
    // 6、SoloSinger -》LeadSinger
    // 7、LeadSinger -》SoloSinger
    // 8、LeadSinger -》Audience
    var singerRole: KTVSingRole = KTVSingRole.Audience

    override fun switchSingerRole(
        newRole: KTVSingRole,
        switchRoleStateListener: ISwitchRoleStateListener?
    ) {
        apiReporter.reportFuncEvent("switchSingerRole", mapOf("newRole" to newRole), mapOf())
        ktvApiLog("switchSingerRole oldRole: $singerRole, newRole: $newRole")
        val oldRole = singerRole
        if (this.singerRole == KTVSingRole.Audience && newRole == KTVSingRole.LeadSinger) {
            // 1、Audience -》LeadSinger
            //  Leave the audience channel
            mRtcEngine.leaveChannelEx(RtcConnection(giantChorusApiConfig.audienceChannelName, giantChorusApiConfig.localUid))
            joinChorus(newRole)
            singerRole = newRole
            ktvApiEventHandlerList.forEach { it.onSingerRoleChanged(oldRole, newRole) }
            switchRoleStateListener?.onSwitchRoleSuccess()
        } else if (this.singerRole == KTVSingRole.Audience && newRole == KTVSingRole.CoSinger) {
            // 2、Audience -》CoSinger
            //  Leave the audience channel
            mRtcEngine.leaveChannelEx(RtcConnection(giantChorusApiConfig.audienceChannelName, giantChorusApiConfig.localUid))
            joinChorus(newRole)
            singerRole = newRole
            switchRoleStateListener?.onSwitchRoleSuccess()
            ktvApiEventHandlerList.forEach { it.onSingerRoleChanged(oldRole, newRole) }
        } else if (this.singerRole == KTVSingRole.CoSinger && newRole == KTVSingRole.Audience) {
            // 3、CoSinger -》Audience
            leaveChorus2(singerRole)
            //  Join the audience channel
            mRtcEngine.joinChannelEx(giantChorusApiConfig.audienceChannelToken, RtcConnection(giantChorusApiConfig.audienceChannelName, giantChorusApiConfig.localUid), ChannelMediaOptions(), object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    super.onJoinChannelSuccess(channel, uid, elapsed)
                }

                override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
                    super.onStreamMessage(uid, streamId, data)
                    dealWithStreamMessage(uid, streamId, data)
                }

                override fun onAudioMetadataReceived(uid: Int, data: ByteArray?) {
                    super.onAudioMetadataReceived(uid, data)
                    dealWithAudioMetadata(uid, data)
                }
            })
            mRtcEngine.setParametersEx("{\"rtc.use_audio4\": true}", RtcConnection(giantChorusApiConfig.audienceChannelName, giantChorusApiConfig.localUid))

            singerRole = newRole
            ktvApiEventHandlerList.forEach { it.onSingerRoleChanged(oldRole, newRole) }
            switchRoleStateListener?.onSwitchRoleSuccess()
        } else if (this.singerRole == KTVSingRole.LeadSinger && newRole == KTVSingRole.Audience) {
            // 4、LeadSinger -》Audience
            stopSing()
            leaveChorus2(singerRole)

            //  Join the audience channel
            mRtcEngine.joinChannelEx(giantChorusApiConfig.audienceChannelToken, RtcConnection(giantChorusApiConfig.audienceChannelName, giantChorusApiConfig.localUid), ChannelMediaOptions(), object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    super.onJoinChannelSuccess(channel, uid, elapsed)
                }

                override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
                    super.onStreamMessage(uid, streamId, data)
                    dealWithStreamMessage(uid, streamId, data)
                }

                override fun onAudioMetadataReceived(uid: Int, data: ByteArray?) {
                    super.onAudioMetadataReceived(uid, data)
                    dealWithAudioMetadata(uid, data)
                }
            })
            mRtcEngine.setParametersEx("{\"rtc.use_audio4\": true}", RtcConnection(giantChorusApiConfig.audienceChannelName, giantChorusApiConfig.localUid))

            singerRole = newRole
            ktvApiEventHandlerList.forEach { it.onSingerRoleChanged(oldRole, newRole) }
            switchRoleStateListener?.onSwitchRoleSuccess()
        } else {
            switchRoleStateListener?.onSwitchRoleFail(SwitchRoleFailReason.NO_PERMISSION)
            ktvApiLogError("Error！You can not switch role from $singerRole to $newRole!")
        }
    }

    override fun fetchMusicCharts(onMusicChartResultListener: (requestId: String?, status: Int, list: Array<out MusicChartInfo>?) -> Unit) {
        apiReporter.reportFuncEvent("fetchMusicCharts", mapOf(), mapOf())
        val requestId = mMusicCenter.musicCharts
        musicChartsCallbackMap[requestId] = onMusicChartResultListener
    }

    override fun searchMusicByMusicChartId(
        musicChartId: Int,
        page: Int,
        pageSize: Int,
        jsonOption: String,
        onMusicCollectionResultListener: (requestId: String?, status: Int, page: Int, pageSize: Int, total: Int, list: Array<out Music>?) -> Unit
    ) {
        apiReporter.reportFuncEvent("searchMusicByMusicChartId", mapOf(), mapOf())
        val requestId =
            mMusicCenter.getMusicCollectionByMusicChartId(musicChartId, page, pageSize, jsonOption)
        musicCollectionCallbackMap[requestId] = onMusicCollectionResultListener
    }

    override fun searchMusicByKeyword(
        keyword: String,
        page: Int,
        pageSize: Int,
        jsonOption: String,
        onMusicCollectionResultListener: (requestId: String?, status: Int, page: Int, pageSize: Int, total: Int, list: Array<out Music>?) -> Unit
    ) {
        apiReporter.reportFuncEvent("searchMusicByKeyword", mapOf(), mapOf())
        val requestId = mMusicCenter.searchMusic(keyword, page, pageSize, jsonOption)
        musicCollectionCallbackMap[requestId] = onMusicCollectionResultListener
    }

    override fun loadMusic(
        songCode: Long,
        config: KTVLoadMusicConfiguration,
        musicLoadStateListener: IMusicLoadStateListener
    ) {
        apiReporter.reportFuncEvent("loadMusic", mapOf("songCode" to songCode, "config" to config), mapOf())
        ktvApiLog("loadMusic called: songCode $songCode")
        // Set globally; the latest call takes precedence.
        this.songCode = songCode
        this.songIdentifier = config.songIdentifier
        this.mainSingerUid = config.mainSingerUid
        mLastReceivedPlayPosTime = null
        mReceivedPlayPosition = 0

        if (config.mode == KTVLoadMusicMode.LOAD_NONE) {
            return
        }

        if (config.mode == KTVLoadMusicMode.LOAD_LRC_ONLY) {
            // only load lyrics.
            loadLyric(songCode) { song, lyricUrl ->
                if (this.songCode != song) {
                    // The current song has changed; the latest loaded song takes precedence.
                    ktvApiLogError("loadMusic failed: CANCELED")
                    musicLoadStateListener.onMusicLoadFail(song, KTVLoadMusicFailReason.CANCELED)
                    return@loadLyric
                }

                if (lyricUrl == null) {
                    // Failed to load lyrics.
                    ktvApiLogError("loadMusic failed: NO_LYRIC_URL")
                    musicLoadStateListener.onMusicLoadFail(song, KTVLoadMusicFailReason.NO_LYRIC_URL)
                } else {
                    // Lyrics loaded successfully
                    ktvApiLog("loadMusic success")
                    lrcView?.onDownloadLrcData(lyricUrl)
                    musicLoadStateListener.onMusicLoadSuccess(song, lyricUrl)
                }
            }
            return
        }

        // Preload song.
        preLoadMusic(songCode) { song, percent, status, msg, lrcUrl ->
            if (status == 0) {
                // Preload song successful.
                if (this.songCode != song) {
                    // The current song has changed; the latest loaded song takes precedence.
                    ktvApiLogError("loadMusic failed: CANCELED")
                    musicLoadStateListener.onMusicLoadFail(song, KTVLoadMusicFailReason.CANCELED)
                    return@preLoadMusic
                }
                if (config.mode == KTVLoadMusicMode.LOAD_MUSIC_AND_LRC) {
                    // Need to load lyrics.
                    loadLyric(song) { _, lyricUrl ->
                        if (this.songCode != song) {
                            // The current song has changed; the latest loaded song takes precedence.
                            ktvApiLogError("loadMusic failed: CANCELED")
                            musicLoadStateListener.onMusicLoadFail(song, KTVLoadMusicFailReason.CANCELED)
                            return@loadLyric
                        }

                        if (lyricUrl == null) {
                            // Failed to load lyrics.
                            ktvApiLogError("loadMusic failed: NO_LYRIC_URL")
                            musicLoadStateListener.onMusicLoadFail(song, KTVLoadMusicFailReason.NO_LYRIC_URL)
                        } else {
                            // Lyrics loaded successfully.
                            ktvApiLog("loadMusic success")
                            lrcView?.onDownloadLrcData(lyricUrl)
                            musicLoadStateListener.onMusicLoadProgress(song, 100, MusicLoadStatus.COMPLETED, msg, lrcUrl)
                            musicLoadStateListener.onMusicLoadSuccess(song, lyricUrl)
                        }
                    }
                } else if (config.mode == KTVLoadMusicMode.LOAD_MUSIC_ONLY) {
                    // No need to load lyrics.
                    ktvApiLog("loadMusic success")
                    musicLoadStateListener.onMusicLoadProgress(song, 100, MusicLoadStatus.COMPLETED, msg, lrcUrl)
                    musicLoadStateListener.onMusicLoadSuccess(song, "")
                }
            } else if (status == 2) {
                // Preloading song is in progress.
                musicLoadStateListener.onMusicLoadProgress(song, percent, MusicLoadStatus.values().firstOrNull { it.value == status } ?: MusicLoadStatus.FAILED, msg, lrcUrl)
            } else {
                // Preloading the song failed.
                ktvApiLogError("loadMusic failed: MUSIC_PRELOAD_FAIL")
                musicLoadStateListener.onMusicLoadFail(song, KTVLoadMusicFailReason.MUSIC_PRELOAD_FAIL)
            }
        }
    }

    override fun loadMusic(
        url: String,
        config: KTVLoadMusicConfiguration
    ) {
        apiReporter.reportFuncEvent("loadMusic", mapOf("url" to url, "config" to config), mapOf())
        ktvApiLog("loadMusic called: songCode $songCode")
        this.songIdentifier = config.songIdentifier
        this.songUrl = url
        this.mainSingerUid = config.mainSingerUid
    }

    override fun removeMusic(songCode: Long) {
        apiReporter.reportFuncEvent("removeMusic", mapOf("songCode" to songCode), mapOf())
        val ret = mMusicCenter.removeCache(songCode)
        if (ret < 0) {
            ktvApiLogError("removeMusic failed, ret: $ret")
        }
    }

    override fun load2Music(url1: String, url2: String, config: KTVLoadMusicConfiguration) {
        apiReporter.reportFuncEvent("load2Music", mapOf("url1" to url1, "url2" to url2, "config" to config), mapOf())
        this.songIdentifier = config.songIdentifier
        this.songUrl = url1
        this.songUrl2 = url2
        this.mainSingerUid = config.mainSingerUid
    }

    override fun switchPlaySrc(url: String, syncPts: Boolean) {
        apiReporter.reportFuncEvent("switchPlaySrc", mapOf("url" to url, "syncPts" to syncPts), mapOf())
        if (this.songUrl != url && this.songUrl2 != url) {
            ktvApiLogError("switchPlaySrc failed: canceled")
            return
        }
        val curPlayPosition = if (syncPts) mPlayer.playPosition else 0
        mPlayer.stop()
        startSing(url, curPlayPosition)
    }

    override fun startSing(songCode: Long, startPos: Long) {
        apiReporter.reportFuncEvent("startSing", mapOf("songCode" to songCode, "startPos" to startPos), mapOf())
        ktvApiLog("playSong called: $singerRole")
        if (singerRole != KTVSingRole.SoloSinger && singerRole != KTVSingRole.LeadSinger) {
            ktvApiLogError("startSing failed: error singerRole")
            return
        }
        if (this.songCode != songCode) {
            ktvApiLogError("startSing failed: canceled")
            return
        }
        mRtcEngine.adjustPlaybackSignalVolume(KTVApi.remoteVolume)

        // Lead singing
        mPlayer.setPlayerOption("enable_multi_audio_track", 1)
        val ret = (mPlayer as IAgoraMusicPlayer).open(songCode, startPos)
        if (ret != 0) {
            ktvApiLogError("mpk open failed: $ret")
        }
    }

    override fun startSing(url: String, startPos: Long) {
        apiReporter.reportFuncEvent("startSing", mapOf("url" to url, "startPos" to startPos), mapOf())
        ktvApiLog("playSong called: $singerRole")
        if (singerRole != KTVSingRole.SoloSinger && singerRole != KTVSingRole.LeadSinger) {
            ktvApiLogError("startSing failed: error singerRole")
            return
        }
        if (this.songUrl != url && this.songUrl2 != url) {
            ktvApiLogError("startSing failed: canceled")
            return
        }
        mRtcEngine.adjustPlaybackSignalVolume(KTVApi.remoteVolume)

        // Lead singing
        mPlayer.setPlayerOption("enable_multi_audio_track", 1)
        val ret = mPlayer.open(url, startPos)
        if (ret != 0) {
            ktvApiLogError("mpk open failed: $ret")
        }
    }

    override fun resumeSing() {
        apiReporter.reportFuncEvent("resumeSing", mapOf(), mapOf())
        ktvApiLog("resumePlay called")
        mPlayer.resume()
    }

    override fun pauseSing() {
        apiReporter.reportFuncEvent("pauseSing", mapOf(), mapOf())
        ktvApiLog("pausePlay called")
        mPlayer.pause()
    }

    override fun seekSing(time: Long) {
        apiReporter.reportFuncEvent("seekSing", mapOf("time" to time), mapOf())
        ktvApiLog("seek called")
        mPlayer.seek(time)
        syncPlayProgress(time)
    }

    override fun setLrcView(view: ILrcView) {
        apiReporter.reportFuncEvent("setLrcView", mapOf(), mapOf())
        ktvApiLog("setLrcView called")
        this.lrcView = view
    }

    override fun muteMic(mute: Boolean) {
        apiReporter.reportFuncEvent("muteMic", mapOf("mute" to mute), mapOf())
        this.isOnMicOpen = !mute
        if (singerRole == KTVSingRole.Audience) return
        val channelMediaOption = ChannelMediaOptions()
        channelMediaOption.publishMicrophoneTrack = isOnMicOpen
        channelMediaOption.clientRoleType = CLIENT_ROLE_BROADCASTER
        mRtcEngine.updateChannelMediaOptions(channelMediaOption)
        mRtcEngine.muteLocalAudioStreamEx(!isOnMicOpen, singChannelRtcConnection)
    }

    override fun setAudioPlayoutDelay(audioPlayoutDelay: Int) {
        apiReporter.reportFuncEvent("setAudioPlayoutDelay", mapOf("audioPlayoutDelay" to audioPlayoutDelay), mapOf())
        this.audioPlayoutDelay = audioPlayoutDelay
    }

    fun setSingingScore(score: Int) {
        this.singingScore = score
    }

    fun setAudienceStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
        dealWithStreamMessage(uid, streamId, data)
    }

    fun setAudienceAudioMetadataReceived(uid: Int, data: ByteArray?) {
        dealWithAudioMetadata(uid, data)
    }

    override fun getMediaPlayer(): IMediaPlayer {
        return mPlayer
    }

    override fun getMusicContentCenter(): IAgoraMusicContentCenter {
        return mMusicCenter
    }

    override fun switchAudioTrack(mode: AudioTrackMode) {
        apiReporter.reportFuncEvent("switchAudioTrack", mapOf("mode" to mode), mapOf())
        when (singerRole) {
            KTVSingRole.LeadSinger, KTVSingRole.SoloSinger -> {
                when (mode) {
                    AudioTrackMode.YUAN_CHANG -> mPlayer.selectMultiAudioTrack(0, 0)
                    AudioTrackMode.BAN_ZOU -> mPlayer.selectMultiAudioTrack(1, 1)
                    AudioTrackMode.DAO_CHANG -> mPlayer.selectMultiAudioTrack(0, 1)
                }
            }
            KTVSingRole.CoSinger -> {
                when (mode) {
                    AudioTrackMode.YUAN_CHANG -> mPlayer.selectAudioTrack(0)
                    AudioTrackMode.BAN_ZOU -> mPlayer.selectAudioTrack(1)
                    AudioTrackMode.DAO_CHANG -> ktvApiLogError("CoSinger can not switch to DAO_CHANG")
                }
            }
            KTVSingRole.Audience -> ktvApiLogError("CoSinger can not switch audio track")
        }
    }

    // ------------------ inner KTVApi --------------------
    private fun stopSing() {
        ktvApiLog("stopSong called")

        val channelMediaOption = ChannelMediaOptions()
        channelMediaOption.autoSubscribeAudio = true
        channelMediaOption.publishMediaPlayerAudioTrack = false
        mRtcEngine.updateChannelMediaOptionsEx(channelMediaOption, singChannelRtcConnection)

        mPlayer.stop()

        // Update audio configuration
        mRtcEngine.setAudioScenario(AUDIO_SCENARIO_GAME_STREAMING)
        mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp_broadcast\":true}")
        mRtcEngine.setParameters("{\"che.audio.neteq.enable_stable_playout\":true}")
        mRtcEngine.setParameters("{\"che.audio.custom_bitrate\": 48000}")
    }

    private val subScribeSingerMap = mutableMapOf<Int, Int>() // <uid, ntpE2eDelay>
    private val singerList = mutableListOf<Int>() // <uid>
    private var mainSingerDelay = 0
    private fun joinChorus(newRole: KTVSingRole) {
        ktvApiLog("joinChorus: $newRole")
        val singChannelMediaOptions = ChannelMediaOptions()
        singChannelMediaOptions.autoSubscribeAudio = true
        singChannelMediaOptions.publishMicrophoneTrack = true
        singChannelMediaOptions.clientRoleType = CLIENT_ROLE_BROADCASTER
        singChannelMediaOptions.isAudioFilterable = newRole != KTVSingRole.LeadSinger // // The lead singer does not participate in TopN ranking

        // Join the singing channel
        mRtcEngine.joinChannelEx(giantChorusApiConfig.chorusChannelToken, singChannelRtcConnection, singChannelMediaOptions, object :
            IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                super.onJoinChannelSuccess(channel, uid, elapsed)
                ktvApiLog("singChannel onJoinChannelSuccess: $newRole")
            }

            override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
                super.onStreamMessage(uid, streamId, data)
                dealWithStreamMessage(uid, streamId, data)
            }

            override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
                val allSpeakers = speakers ?: return
                // VideoPitch callback, used for synchronizing pitch across all endpoints
                if (singerRole != KTVSingRole.Audience) {
                    for (info in allSpeakers) {
                        if (info.uid == 0) {
                            pitch =
                                if (mediaPlayerState == MediaPlayerState.PLAYER_STATE_PLAYING && isOnMicOpen) {
                                    info.voicePitch
                                } else {
                                    0.0
                                }
                        }
                    }
                }
            }

            //  Used for chorus calibration.
            override fun onLocalAudioStats(stats: LocalAudioStats?) {
                if (KTVApi.useCustomAudioSource) return
                val audioState = stats ?: return
                audioPlayoutDelay = audioState.audioPlayoutDelay
            }

            // Used to detect headphone status.
            override fun onAudioRouteChanged(routing: Int) { // 0\2\5 earPhone
                audioRouting = routing
                processAudioProfessionalProfile()
            }

            // Used to detect the status of the send and receive streams.
            override fun onAudioPublishStateChanged(
                channel: String?,
                oldState: Int,
                newState: Int,
                elapseSinceLastState: Int
            ) {
                ktvApiLog("onAudioPublishStateChanged: oldState: $oldState, newState: $newState")
                if (newState == 3) {
                    isPublishAudio = true
                    processAudioProfessionalProfile()
                } else if (newState == 1) {
                    isPublishAudio = false
                }
            }

            // Delay-based path selection strategy
            override fun onUserJoined(uid: Int, elapsed: Int) {
                super.onUserJoined(uid, elapsed)
                if (uid != giantChorusApiConfig.musicStreamUid && subScribeSingerMap.size < 8) {
                    mRtcEngine.muteRemoteAudioStreamEx(uid, false, singChannelRtcConnection)
                    if (uid != mainSingerUid) {
                        subScribeSingerMap[uid] = 0
                    }
                } else if (uid != giantChorusApiConfig.musicStreamUid && subScribeSingerMap.size == 8) {
                    mRtcEngine.muteRemoteAudioStreamEx(uid, true, singChannelRtcConnection)
                }
                if (uid != giantChorusApiConfig.musicStreamUid && uid != mainSingerUid) {
                    singerList.add(uid)
                }
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                super.onUserOffline(uid, reason)
                subScribeSingerMap.remove(uid)
                singerList.remove(uid)
            }

            override fun onLeaveChannel(stats: RtcStats?) {
                super.onLeaveChannel(stats)
                subScribeSingerMap.clear()
                singerList.clear()
            }

            override fun onRemoteAudioStats(stats: RemoteAudioStats?) {
                super.onRemoteAudioStats(stats)
                stats ?: return
                if (KTVApi.routeSelectionConfig.type == GiantChorusRouteSelectionType.RANDOM || KTVApi.routeSelectionConfig.type == GiantChorusRouteSelectionType.TOP_N) return
                val uid = stats.uid
                if (uid == mainSingerUid) {
                    mainSingerDelay = stats.e2eDelay
                }
                if (uid != mainSingerUid && uid != giantChorusApiConfig.musicStreamUid && subScribeSingerMap.containsKey(uid)) {
                    subScribeSingerMap[uid] = stats.e2eDelay
                }
            }
        })

        mRtcEngine.setParametersEx("{\"che.audio.max_mixed_participants\": 8}", singChannelRtcConnection)
        mRtcEngine.setParametersEx("{\"rtc.use_audio4\": true}", singChannelRtcConnection)

        // Path selection strategy handling
        if (KTVApi.routeSelectionConfig.type == GiantChorusRouteSelectionType.TOP_N || KTVApi.routeSelectionConfig.type == GiantChorusRouteSelectionType.BY_DELAY_AND_TOP_N) {
            if (newRole == KTVSingRole.LeadSinger) {
                mRtcEngine.setParametersEx("{\"che.audio.filter_streams\":${KTVApi.routeSelectionConfig.streamNum}}", singChannelRtcConnection)
            } else {
                mRtcEngine.setParametersEx("{\"che.audio.filter_streams\":${KTVApi.routeSelectionConfig.streamNum - 1}}", singChannelRtcConnection)
            }
        } else {
            mRtcEngine.setParametersEx("{\"che.audio.filter_streams\": 0}", singChannelRtcConnection)
        }
        mRtcEngine.enableAudioVolumeIndicationEx(50, 10, true, singChannelRtcConnection)

        when (newRole) {
            KTVSingRole.LeadSinger -> {
                // Update audio configuration
                mRtcEngine.setAudioScenario(AUDIO_SCENARIO_CHORUS)
                mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp_broadcast\":false}")
                mRtcEngine.setParameters("{\"che.audio.neteq.enable_stable_playout\":false}")
                mRtcEngine.setParameters("{\"che.audio.custom_bitrate\": 80000}")

                // MPK stream joins the channel
                val options = ChannelMediaOptions()
                options.autoSubscribeAudio = false
                options.autoSubscribeVideo = false
                options.publishMicrophoneTrack = false
                options.publishMediaPlayerAudioTrack = true
                options.publishMediaPlayerId = mPlayer.mediaPlayerId
                options.clientRoleType = CLIENT_ROLE_BROADCASTER
                // Prevent the lead singer and chorus from hearing the MPK stream audio
                options.enableAudioRecordingOrPlayout = false

                val rtcConnection = RtcConnection()
                rtcConnection.channelId = giantChorusApiConfig.chorusChannelName
                rtcConnection.localUid = giantChorusApiConfig.musicStreamUid
                mpkConnection = rtcConnection

                mRtcEngine.joinChannelEx(
                    giantChorusApiConfig.musicStreamToken,
                    mpkConnection,
                    options,
                    object : IRtcEngineEventHandler() {
                        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                            ktvApiLog("onMPKJoinChannelSuccess, channel: $channel, uid: $uid")
                        }

                        override fun onLeaveChannel(stats: RtcStats) {
                            ktvApiLog("onMPKLeaveChannel")
                        }
                    })
                mRtcEngine.setParametersEx("{\"rtc.use_audio4\": true}", mpkConnection)
            }
            KTVSingRole.CoSinger -> {
                // Prevent the lead singer and chorus from hearing the MPK stream audio
                mRtcEngine.muteRemoteAudioStreamEx(
                    giantChorusApiConfig.musicStreamUid,
                    true,
                    singChannelRtcConnection
                )

                // Update audio configuration
                mRtcEngine.setAudioScenario(AUDIO_SCENARIO_CHORUS)
                mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp_broadcast\":false}")
                mRtcEngine.setParameters("{\"che.audio.neteq.enable_stable_playout\":false}")
                mRtcEngine.setParameters("{\"che.audio.custom_bitrate\": 48000}")

                // Preload song successful
                // Guide singing
                mPlayer.setPlayerOption("enable_multi_audio_track", 1)
                if (giantChorusApiConfig.musicType == KTVMusicType.SONG_CODE) {
                    val ret = (mPlayer as IAgoraMusicPlayer).open(songCode, 0) // TODO open failed
                    if (ret != 0) {
                        ktvApiLogError("mpk open failed: $ret")
                    }
                } else {
                    val ret = mPlayer.open(songUrl, 0) // TODO open failed
                    if (ret != 0) {
                        ktvApiLogError("mpk open failed: $ret")
                    }
                }
            }
            else -> {
                ktvApiLogError("JoinChorus with Wrong role: $singerRole")
            }
        }

        mRtcEngine.muteRemoteAudioStreamEx(giantChorusApiConfig.musicStreamUid, true, singChannelRtcConnection)
        // After joining the singing channel, create a data stream
        renewInnerDataStreamId()
    }

    private fun leaveChorus2(role: KTVSingRole) {
        ktvApiLog("leaveChorus: $singerRole")
        when (role) {
            KTVSingRole.LeadSinger -> {
                mRtcEngine.leaveChannelEx(mpkConnection)
            }
            KTVSingRole.CoSinger -> {
                mPlayer.stop()

                // Update audio configuration
                mRtcEngine.setAudioScenario(AUDIO_SCENARIO_GAME_STREAMING)
                mRtcEngine.setParameters("{\"rtc.video.enable_sync_render_ntp_broadcast\":true}")
                mRtcEngine.setParameters("{\"che.audio.neteq.enable_stable_playout\":true}")
                mRtcEngine.setParameters("{\"che.audio.custom_bitrate\": 48000}")
            }
            else -> {
                ktvApiLogError("JoinChorus with wrong role: $singerRole")
            }
        }
        mRtcEngine.leaveChannelEx(singChannelRtcConnection)
    }

    // ------------------ inner --------------------

    private fun isChorusCoSinger(): Boolean {
        return singerRole == KTVSingRole.CoSinger
    }

    private fun sendStreamMessageWithJsonObject(
        obj: JSONObject,
        success: (isSendSuccess: Boolean) -> Unit
    ) {
        val ret = mRtcEngine.sendStreamMessageEx(innerDataStreamId, obj.toString().toByteArray(), singChannelRtcConnection)
        if (ret == 0) {
            success.invoke(true)
        } else {
            ktvApiLogError("sendStreamMessageWithJsonObject failed: $ret, innerDataStreamId:$innerDataStreamId")
        }
    }

    private fun syncPlayState(
        state: Constants.MediaPlayerState,
        error: Constants.MediaPlayerReason
    ) {
        val msg: MutableMap<String?, Any?> = HashMap()
        msg["cmd"] = "PlayerState"
        msg["state"] = Constants.MediaPlayerState.getValue(state)
        msg["error"] = Constants.MediaPlayerReason.getValue(error)
        val jsonMsg = JSONObject(msg)
        sendStreamMessageWithJsonObject(jsonMsg) {}
    }

    private fun syncPlayProgress(time: Long) {
        val msg: MutableMap<String?, Any?> = HashMap()
        msg["cmd"] = "Seek"
        msg["position"] = time
        val jsonMsg = JSONObject(msg)
        sendStreamMessageWithJsonObject(jsonMsg) {}
    }

    // ------------------ Lyrics playback and synchronization ------------------
    private fun startDisplayLrc() {
        ktvApiLog("startDisplayLrc called")
        mStopDisplayLrc = false
        displayLrcFuture = scheduledThreadPool.scheduleAtFixedRate(displayLrcTask, 0,20, TimeUnit.MILLISECONDS)
    }

    // Stop playback of lyrics
    private fun stopDisplayLrc() {
        ktvApiLog("stopDisplayLrc called")
        mStopDisplayLrc = true
        displayLrcFuture?.cancel(true)
        displayLrcFuture = null
        if (scheduledThreadPool is ScheduledThreadPoolExecutor) {
            scheduledThreadPool.remove(displayLrcTask)
        }
    }

    // ------------------  Mixing synchronization driven by scoring ------------------
    private fun sendSyncScore() {
        val jsonObject = JSONObject()
        jsonObject.put("service", "audio_smart_mixer") // The target consumer (service) name for the data message
        jsonObject.put("version", "V1") // Protocol version number (not service version number)
        val payloadJson = JSONObject()
        payloadJson.put("cname", giantChorusApiConfig.chorusChannelName) // Channel name, singing channel
        payloadJson.put("uid", giantChorusApiConfig.localUid.toString()) // Your UID
        payloadJson.put("uLv", -1) // User level, -1 if not available (higher level means more important)
        payloadJson.put("specialLabel", 0) // 0: default mode, 1: this user needs to be excluded from smart mixing
        payloadJson.put("audioRoute", audioRouting) // Audio routing: listen for onAudioRouteChanged
        payloadJson.put("vocalScore", singingScore) // Single sentence score
        jsonObject.put("payload", payloadJson)
        ktvApiLog("sendSyncScore: $jsonObject")
        sendStreamMessageWithJsonObject(jsonObject) {}
    }

    // Start sending scores every 3 seconds.
    private fun startSyncScore() {
        mStopSyncScore = false
        mSyncScoreFuture = scheduledThreadPool.scheduleAtFixedRate(mSyncScoreTask, 0, 3000, TimeUnit.MILLISECONDS)
    }

    // Stop sending scores.
    private fun stopSyncScore() {
        mStopSyncScore = true
        singingScore = 0

        mSyncScoreFuture?.cancel(true)
        mSyncScoreFuture = null
        if (scheduledThreadPool is ScheduledThreadPoolExecutor) {
            scheduledThreadPool.remove(mSyncScoreTask)
        }
    }

    // ------------------ Cloud mixing information synchronization. ------------------
    private fun sendSyncCloudConvergenceStatus() {
        val jsonObject = JSONObject()
        jsonObject.put("service", "audio_smart_mixer_status") // Target consumer service name for the data message
        jsonObject.put("version", "V1") // Protocol version number (not the service version)
        val payloadJson = JSONObject()
        payloadJson.put("Ts", getNtpTimeInMs()) // NTP time
        payloadJson.put("cname", giantChorusApiConfig.chorusChannelName) // Channel name
        payloadJson.put("status", getCloudConvergenceStatus()) //（(-1: unknown, 0: non-Karaoke state, 1: Karaoke playing state, 2: Karaoke paused state)
        payloadJson.put("bgmUID", mpkConnection?.localUid.toString()) // UID of the mpk stream
        payloadJson.put("leadsingerUID", mainSingerUid.toString()) // ("-1" = unknown) // UID of the main singer
        jsonObject.put("payload", payloadJson)
        ktvApiLog("sendSyncCloudConvergenceStatus: $jsonObject")
        sendStreamMessageWithJsonObject(jsonObject) {}
    }

    // -1: unknown, 0: non-Karaoke state, 1: Karaoke playing state, 2: Karaoke paused state
    private fun getCloudConvergenceStatus(): Int {
        var status = -1
        when (this.mediaPlayerState) {
            MediaPlayerState.PLAYER_STATE_PLAYING -> status = 1
            MediaPlayerState.PLAYER_STATE_PAUSED -> status = 2
            else -> {}
        }
        return status
    }

    // Start sending scores every 200ms.
    private fun startSyncCloudConvergenceStatus() {
        mStopSyncCloudConvergenceStatus = false
        mSyncCloudConvergenceStatusFuture = scheduledThreadPool.scheduleAtFixedRate(mSyncCloudConvergenceStatusTask, 0, 200,TimeUnit.MILLISECONDS)
    }

    // Stop sending scores.
    private fun stopSyncCloudConvergenceStatus() {
        mStopSyncCloudConvergenceStatus = true

        mSyncCloudConvergenceStatusFuture?.cancel(true)
        mSyncCloudConvergenceStatusFuture = null
        if (scheduledThreadPool is ScheduledThreadPoolExecutor) {
            scheduledThreadPool.remove(mSyncCloudConvergenceStatusTask)
        }
    }

    // ------------------ Delay routing. ------------------
    private var mStopProcessDelay = true

    private val mProcessDelayTask = Runnable {
        if (!mStopProcessDelay && singerRole != KTVSingRole.Audience) {
            val n = if (singerRole == KTVSingRole.LeadSinger) KTVApi.routeSelectionConfig.streamNum else KTVApi.routeSelectionConfig.streamNum -1
            val sortedEntries = subScribeSingerMap.entries.sortedBy { it.value }
            val other = sortedEntries.drop(3)
            val drop = mutableListOf<Int>()
            if (n > 3) {
                other.drop(n - 3).forEach { (uid, _) ->
                    drop.add(uid)
                    mRtcEngine.muteRemoteAudioStreamEx(uid, true, singChannelRtcConnection)
                    subScribeSingerMap.remove(uid)
                }
            }
            ktvApiLog("Re-subscribe to routing., drop:$drop")

            val filteredList = singerList.filter { !subScribeSingerMap.containsKey(it) }
            val filteredList2 = filteredList.filter { !drop.contains(it) }
            val shuffledList = filteredList2.shuffled()
            if (subScribeSingerMap.size < 8) {
                val randomSingers = shuffledList.take(8 - subScribeSingerMap.size)
                ktvApiLog("Re-subscribe to routing., newSingers:$randomSingers")
                for (singer in randomSingers) {
                    subScribeSingerMap[singer] = 0
                    mRtcEngine.muteRemoteAudioStreamEx(singer, false, singChannelRtcConnection)
                }
            }
            ktvApiLog("Re-subscribe to routing., newSubScribeSingerMap:$subScribeSingerMap")
        }
    }

    private val mProcessSubscribeTask = Runnable {
        if (!mStopProcessDelay && singerRole != KTVSingRole.Audience) {
            val n = if (singerRole == KTVSingRole.LeadSinger) KTVApi.routeSelectionConfig.streamNum else KTVApi.routeSelectionConfig.streamNum -1
            val sortedEntries = subScribeSingerMap.entries.sortedBy { it.value }
            val mustToHave = sortedEntries.take(3)
            mustToHave.forEach { (uid, _) ->
                mRtcEngine.adjustUserPlaybackSignalVolumeEx(uid, 100, singChannelRtcConnection)
            }
            val other = sortedEntries.drop(3)
            if (n > 3) {
                other.take(n - 3).forEach { (uid, delay) ->
                    if (delay > 300) {
                        mRtcEngine.adjustUserPlaybackSignalVolumeEx(uid, 0, singChannelRtcConnection)
                    } else {
                        mRtcEngine.adjustUserPlaybackSignalVolumeEx(uid, 100, singChannelRtcConnection)
                    }
                }
                other.drop(n - 3).forEach { (uid, _) ->
                    mRtcEngine.adjustUserPlaybackSignalVolumeEx(uid, 0, singChannelRtcConnection)
                }
            }

            ktvApiLog("Routing sorting and adjusting playback volume, mustToHave:$mustToHave, other:$other")
        }
    }

    private var mProcessDelayFuture :ScheduledFuture<*>? = null
    private var mProcessSubscribeFuture :ScheduledFuture<*>? = null
    private fun startProcessDelay() {
        if (KTVApi.routeSelectionConfig.type == GiantChorusRouteSelectionType.TOP_N || KTVApi.routeSelectionConfig.type == GiantChorusRouteSelectionType.RANDOM) return
        mStopProcessDelay = false
        mProcessDelayFuture = scheduledThreadPool.scheduleAtFixedRate(mProcessDelayTask, 10000, 20000, TimeUnit.MILLISECONDS)
        mProcessSubscribeFuture = scheduledThreadPool.scheduleAtFixedRate(mProcessSubscribeTask,15000,20000, TimeUnit.MILLISECONDS)
    }

    private fun stopProcessDelay() {
        mStopProcessDelay = true

        mProcessDelayFuture?.cancel(true)
        mProcessSubscribeFuture?.cancel(true)
        mProcessDelayFuture = null
        if (scheduledThreadPool is ScheduledThreadPoolExecutor) {
            scheduledThreadPool.remove(mProcessDelayTask)
            scheduledThreadPool.remove(mProcessSubscribeTask)
        }
    }

    private fun loadLyric(songNo: Long, onLoadLyricCallback: (songNo: Long, lyricUrl: String?) -> Unit) {
        ktvApiLog("loadLyric: $songNo")
        val requestId = mMusicCenter.getLyric(songNo, 0)
        if (requestId.isEmpty()) {
            onLoadLyricCallback.invoke(songNo, null)
            return
        }
        lyricSongCodeMap[requestId] = songNo
        lyricCallbackMap[requestId] = onLoadLyricCallback
    }

    private fun preLoadMusic(songNo: Long, onLoadMusicCallback: (songCode: Long,
                                                                 percent: Int,
                                                                 status: Int,
                                                                 msg: String?,
                                                                 lyricUrl: String?) -> Unit) {
        ktvApiLog("loadMusic: $songNo")
        val ret = mMusicCenter.isPreloaded(songNo)
        if (ret == 0) {
            loadMusicCallbackMap.remove(songNo.toString())
            onLoadMusicCallback(songNo, 100, 0, null, null)
            return
        }

        val retPreload = mMusicCenter.preload(songNo, null)
        if (retPreload != 0) {
            ktvApiLogError("preLoadMusic failed: $retPreload")
            loadMusicCallbackMap.remove(songNo.toString())
            onLoadMusicCallback(songNo, 100, 1, null, null)
            return
        }
        loadMusicCallbackMap[songNo.toString()] = onLoadMusicCallback
    }

    private fun getNtpTimeInMs(): Long {
        val currentNtpTime = mRtcEngine.ntpWallTimeInMs
        return if (currentNtpTime != 0L) {
            currentNtpTime + 2208988800L * 1000
        } else {
            ktvApiLogError("getNtpTimeInMs DeviceDelay is zero!!!")
            System.currentTimeMillis()
        }
    }

    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mainHandler.looper.thread) {
            r.run()
        } else {
            mainHandler.post(r)
        }
    }

    // ------------------------ AgoraRtcEvent ------------------------
    private fun dealWithStreamMessage(uid: Int, streamId: Int, data: ByteArray?) {
        val jsonMsg: JSONObject
        val messageData = data ?: return
        try {
            val strMsg = String(messageData)
            jsonMsg = JSONObject(strMsg)
            if (!jsonMsg.has("cmd")) return
            if (jsonMsg.getString("cmd") == "setLrcTime") { // Sync lyrics
                val position = jsonMsg.getLong("time")
                val realPosition = jsonMsg.getLong("realTime")
                val duration = jsonMsg.getLong("duration")
                val remoteNtp = jsonMsg.getLong("ntp")
                val songId = jsonMsg.getString("songIdentifier")
                val mpkState = jsonMsg.getInt("playerState")

                if (isChorusCoSinger()) {
                    // Local BGM calibration logic
                    if (this.mediaPlayerState == MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED) {
                        // Lower the remote voice volume before the chorus member starts playing music
                        mRtcEngine.adjustPlaybackSignalVolume(KTVApi.remoteVolume)
                        // Start local playback when receiving the lead singer's first playback position message (first calibrate through seek)
                        val delta = getNtpTimeInMs() - remoteNtp
                        val expectPosition = position + delta + audioPlayoutDelay
                        if (expectPosition in 1 until duration) {
                            mPlayer.seek(expectPosition)
                        }
                        mPlayer.play()
                    } else if (this.mediaPlayerState == MediaPlayerState.PLAYER_STATE_PLAYING) {
                        val localNtpTime = getNtpTimeInMs()
                        val localPosition =
                            localNtpTime - this.localPlayerSystemTime + this.localPlayerPosition // Current co-singer's playback time
                        val expectPosition =
                            localNtpTime - remoteNtp + position + audioPlayoutDelay // Actual lead singer's playback time
                        val diff = expectPosition - localPosition
                        if (KTVApi.debugMode) {
                            ktvApiLog("play_status_seek: " + diff + " audioPlayoutDelay：" + audioPlayoutDelay +  "  localNtpTime: " + localNtpTime + "  expectPosition: " + expectPosition +
                                    "  localPosition: " + localPosition + "  ntp diff: " + (localNtpTime - remoteNtp))
                        }
                        if ((diff > 50 || diff < -50) && expectPosition < duration) { // Set threshold to 50ms to avoid frequent seeking
                            mPlayer.seek(expectPosition)
                        }
                    } else {
                        mLastReceivedPlayPosTime = System.currentTimeMillis()
                        mReceivedPlayPosition = realPosition
                    }

                    if (MediaPlayerState.getStateByValue(mpkState) != this.mediaPlayerState) {
                        when (MediaPlayerState.getStateByValue(mpkState)) {
                            MediaPlayerState.PLAYER_STATE_PAUSED -> {
                                mPlayer.pause()
                            }
                            MediaPlayerState.PLAYER_STATE_PLAYING -> {
                                mPlayer.resume()
                            }
                            else -> {}
                        }
                    }
                } else {
                    // Solo audience
                    if (jsonMsg.has("ver")) {
                        recvFromDataStream = false
                    } else {
                        recvFromDataStream = true
                        if (this.songIdentifier == songId) {
                            mLastReceivedPlayPosTime = System.currentTimeMillis()
                            mReceivedPlayPosition = realPosition
                        } else {
                            mLastReceivedPlayPosTime = null
                            mReceivedPlayPosition = 0
                        }
                    }
                }
            } else if (jsonMsg.getString("cmd") == "Seek") {
                // Co-singer received seek command from the lead singer
                if (isChorusCoSinger()) {
                    val position = jsonMsg.getLong("position")
                    mPlayer.seek(position)
                }
            } else if (jsonMsg.getString("cmd") == "PlayerState") {
                // Other clients received seek command from the lead singer
                val state = jsonMsg.getInt("state")
                val error = jsonMsg.getInt("error")
                ktvApiLog("onStreamMessage PlayerState: $state")
                if (isChorusCoSinger()) {
                    when (MediaPlayerState.getStateByValue(state)) {
                        MediaPlayerState.PLAYER_STATE_PAUSED -> {
                            mPlayer.pause()
                        }
                        MediaPlayerState.PLAYER_STATE_PLAYING -> {
                            mPlayer.resume()
                        }
                        else -> {}
                    }
                } else if (this.singerRole == KTVSingRole.Audience) {
                    this.mediaPlayerState = MediaPlayerState.getStateByValue(state)
                }
                ktvApiEventHandlerList.forEach { it.onMusicPlayerStateChanged(
                    MediaPlayerState.getStateByValue(state),
                    Constants.MediaPlayerReason.getErrorByValue(error),
                    false
                ) }
            } else if (jsonMsg.getString("cmd") == "setVoicePitch") {
                val pitch = jsonMsg.getDouble("pitch")
                if (this.singerRole == KTVSingRole.Audience) {
                    this.pitch = pitch
                }
            }
        } catch (exp: JSONException) {
            ktvApiLogError("onStreamMessage:$exp")
        }
    }

    private fun dealWithAudioMetadata(uid: Int, data: ByteArray?) {
        val messageData = data ?: return
        val lrcTime = LrcTimeOuterClass.LrcTime.parseFrom(messageData)
        if (lrcTime.type == LrcTimeOuterClass.MsgType.LRC_TIME) { // sync lyrics
            val realPosition = lrcTime.ts
            val songId = lrcTime.songId
            val curTs = if (this.songIdentifier == songId) realPosition else 0
            runOnMainThread {
                lrcView?.onUpdatePitch(pitch.toFloat())
                // (fix ENT-489)Make lyrics delay for 200ms
                // Per suggestion from Bob, it has a intrinsic buffer/delay between sound and `onPositionChanged(Player)`,
                // such as AEC/Player/Device buffer.
                // We choose the estimated 200ms.
                lrcView?.onUpdateProgress(if (curTs > 200) (curTs - 200) else curTs) // The delay here will impact both singer and audience side
            }
        }
    }

    // ------------------------ AgoraMusicContentCenterEventDelegate  ------------------------
    override fun onPreLoadEvent(
        requestId: String?,
        songCode: Long,
        percent: Int,
        lyricUrl: String?,
        status: Int,
        errorCode: Int
    ) {
        val callback = loadMusicCallbackMap[songCode.toString()] ?: return
        if (status == 0 || status == 1) {
            loadMusicCallbackMap.remove(songCode.toString())
        }
        if (errorCode == 2) {
            // Token expired
            ktvApiEventHandlerList.forEach { it.onTokenPrivilegeWillExpire() }
        }
        callback.invoke(songCode, percent, status, RtcEngine.getErrorDescription(errorCode), lyricUrl)
    }

    override fun onMusicCollectionResult(
        requestId: String?,
        page: Int,
        pageSize: Int,
        total: Int,
        list: Array<out Music>?,
        errorCode: Int
    ) {
        ktvApiLog("onMusicCollectionResult, requestId: $requestId, list: $list, errorCode: $errorCode")
        val id = requestId ?: return
        val callback = musicCollectionCallbackMap[id] ?: return
        musicCollectionCallbackMap.remove(id)
        if (errorCode == 2) {
            // Token expired
            ktvApiEventHandlerList.forEach { it.onTokenPrivilegeWillExpire() }
        }
        callback.invoke(requestId, errorCode, page, pageSize, total, list)
    }

    override fun onMusicChartsResult(requestId: String?, list: Array<out MusicChartInfo>?, errorCode: Int) {
        val id = requestId ?: return
        val callback = musicChartsCallbackMap[id] ?: return
        musicChartsCallbackMap.remove(id)
        if (errorCode == 2) {
            // Token e
            ktvApiEventHandlerList.forEach { it.onTokenPrivilegeWillExpire() }
        }
        callback.invoke(requestId, errorCode, list)
    }

    override fun onLyricResult(
        requestId: String?,
        songCode: Long,
        lyricUrl: String?,
        errorCode: Int
    ) {
        val callback = lyricCallbackMap[requestId] ?: return
        val songCode = lyricSongCodeMap[requestId] ?: return
        lyricCallbackMap.remove(lyricUrl)
        if (errorCode == 2) {
            // Token expired
            ktvApiEventHandlerList.forEach { it.onTokenPrivilegeWillExpire() }
        }
        if (lyricUrl == null || lyricUrl.isEmpty()) {
            callback(songCode, null)
            return
        }
        callback(songCode, lyricUrl)
    }


    override fun onSongSimpleInfoResult(
        requestId: String?,
        songCode: Long,
        simpleInfo: String,
        errorCode: Int
    ) {}

    // ------------------------ AgoraRtcMediaPlayerDelegate ------------------------
    private var duration: Long = 0
    override fun onPlayerStateChanged(
        state: Constants.MediaPlayerState?,
        reason: Constants.MediaPlayerReason?
    ) {
        val mediaPlayerState = state ?: return
        val mediaPlayerError = reason ?: return
        ktvApiLog("onPlayerStateChanged called, state: $mediaPlayerState, error: $mediaPlayerError")
        this.mediaPlayerState = mediaPlayerState
        when (mediaPlayerState) {
            MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED -> {
                duration = mPlayer.duration
                this.localPlayerPosition = 0
                // Accompaniment
                mPlayer.selectMultiAudioTrack(1, 1)
                if (this.singerRole == KTVSingRole.SoloSinger ||
                    this.singerRole == KTVSingRole.LeadSinger
                ) {
                    mPlayer.play()
                }
                startProcessDelay()
            }
            MediaPlayerState.PLAYER_STATE_PLAYING -> {
                mRtcEngine.adjustPlaybackSignalVolume(KTVApi.remoteVolume)
            }
            MediaPlayerState.PLAYER_STATE_PAUSED -> {
                mRtcEngine.adjustPlaybackSignalVolume(100)
            }
            MediaPlayerState.PLAYER_STATE_STOPPED -> {
                mRtcEngine.adjustPlaybackSignalVolume(100)
                duration = 0
                stopProcessDelay()
            }
            else -> {}
        }

        if (this.singerRole == KTVSingRole.SoloSinger || this.singerRole == KTVSingRole.LeadSinger) {
            syncPlayState(mediaPlayerState, mediaPlayerError)
        }
        ktvApiEventHandlerList.forEach { it.onMusicPlayerStateChanged(mediaPlayerState, mediaPlayerError, true) }
    }

    // Synchronize playback progress
    override fun onPositionChanged(position_ms: Long, timestamp_ms: Long) {
        localPlayerPosition = position_ms
        localPlayerSystemTime = timestamp_ms

        if ((this.singerRole == KTVSingRole.SoloSinger || this.singerRole == KTVSingRole.LeadSinger) && position_ms > audioPlayoutDelay) {
            val msg: MutableMap<String?, Any?> = HashMap()
            msg["cmd"] = "setLrcTime"
            msg["ntp"] = timestamp_ms
            msg["duration"] = duration
            msg["time"] =
                position_ms - audioPlayoutDelay // "position-audioDeviceDelay" Calculate the current playback progress accurately.
            msg["realTime"] = position_ms
            msg["playerState"] = MediaPlayerState.getValue(this.mediaPlayerState)
            msg["pitch"] = pitch
            msg["songIdentifier"] = songIdentifier
            msg["forward"] = true
            msg["ver"] = lyricSyncVersion
            val jsonMsg = JSONObject(msg)
            sendStreamMessageWithJsonObject(jsonMsg) {}
        }

        if (this.singerRole != KTVSingRole.Audience) {
            mLastReceivedPlayPosTime = System.currentTimeMillis()
            mReceivedPlayPosition = position_ms
        } else {
            mLastReceivedPlayPosTime = null
            mReceivedPlayPosition = 0
        }
    }

    override fun onPlayerEvent(
        eventCode: Constants.MediaPlayerEvent?,
        elapsedTime: Long,
        message: String?
    ) {
    }

    override fun onMetaData(type: Constants.MediaPlayerMetadataType?, data: ByteArray?) {}

    override fun onPlayBufferUpdated(playCachedBuffer: Long) {}

    override fun onPreloadEvent(src: String?, event: Constants.MediaPlayerPreloadEvent?) {}

    override fun onAgoraCDNTokenWillExpire() {}

    override fun onPlayerSrcInfoChanged(from: SrcInfo?, to: SrcInfo?) {}

    override fun onPlayerInfoUpdated(info: PlayerUpdatedInfo?) {}

    override fun onPlayerCacheStats(stats: CacheStatistics?) {}

    override fun onPlayerPlaybackStats(stats: PlayerPlaybackStats?) {}

    override fun onAudioVolumeIndication(volume: Int) {}
}