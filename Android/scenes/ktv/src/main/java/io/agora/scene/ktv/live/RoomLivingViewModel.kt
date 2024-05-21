package io.agora.scene.ktv.live

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.SurfaceView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.agora.mediaplayer.Constants.MediaPlayerError
import io.agora.mediaplayer.Constants.MediaPlayerState
import io.agora.musiccontentcenter.Music
import io.agora.musiccontentcenter.MusicChartInfo
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.DataStreamConfig
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcConnection.CONNECTION_STATE_TYPE
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.RtcEngineEx
import io.agora.rtc2.video.ContentInspectConfig
import io.agora.rtc2.video.ContentInspectConfig.ContentInspectModule
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserInfo
import io.agora.scene.base.AudioModeration
import io.agora.scene.base.AudioModeration.moderationAudio
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.bean.User
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.manager.UserManager
import io.agora.scene.ktv.KTVLogger
import io.agora.scene.ktv.R
import io.agora.scene.ktv.debugSettings.KTVDebugSettingBean
import io.agora.scene.ktv.debugSettings.KTVDebugSettingsDialog
import io.agora.scene.ktv.ktvapi.AudioTrackMode
import io.agora.scene.ktv.ktvapi.IKTVApiEventHandler
import io.agora.scene.ktv.ktvapi.ILrcView
import io.agora.scene.ktv.ktvapi.IMusicLoadStateListener
import io.agora.scene.ktv.ktvapi.ISwitchRoleStateListener
import io.agora.scene.ktv.ktvapi.KTVApi
import io.agora.scene.ktv.ktvapi.KTVApiConfig
import io.agora.scene.ktv.ktvapi.KTVLoadMusicConfiguration
import io.agora.scene.ktv.ktvapi.KTVLoadMusicFailReason
import io.agora.scene.ktv.ktvapi.KTVLoadMusicMode
import io.agora.scene.ktv.ktvapi.KTVMusicType
import io.agora.scene.ktv.ktvapi.KTVSingRole
import io.agora.scene.ktv.ktvapi.KTVType
import io.agora.scene.ktv.ktvapi.MusicLoadStatus
import io.agora.scene.ktv.ktvapi.SwitchRoleFailReason
import io.agora.scene.ktv.ktvapi.createKTVApi
import io.agora.scene.ktv.live.bean.MusicSettingBean
import io.agora.scene.ktv.live.bean.NetWorkEvent
import io.agora.scene.ktv.live.bean.SoundCardSettingBean
import io.agora.scene.ktv.live.fragmentdialog.MusicSettingCallback
import io.agora.scene.ktv.service.JoinRoomInfo
import io.agora.scene.ktv.service.KTVServiceProtocol.Companion.getImplInstance
import io.agora.scene.ktv.service.KtvServiceListenerProtocol
import io.agora.scene.ktv.service.RoomChoristerInfo
import io.agora.scene.ktv.service.RoomMicSeatInfo
import io.agora.scene.ktv.service.RoomSongInfo
import io.agora.scene.ktv.service.ScoringAlgoControlModel
import io.agora.scene.ktv.service.ScoringAverageModel
import io.agora.scene.ktv.service.VolumeModel
import io.agora.scene.ktv.widget.lrcView.LrcControlView
import io.agora.scene.widget.toast.CustomToast
import org.json.JSONException
import org.json.JSONObject

/**
 * The type Room living view model.
 */
class RoomLivingViewModel constructor(joinRoomInfo: JoinRoomInfo) : ViewModel() {
    private val TAG = "KTV_Scene_LOG"
    private var mainHandler: Handler? = null
    private fun runOnMainThread(runnable: Runnable) {
        if (mainHandler == null) {
            mainHandler = Handler(Looper.getMainLooper())
        }
        if (Thread.currentThread() == mainHandler?.looper?.thread) {
            runnable.run()
        } else {
            mainHandler?.post(runnable)
        }
    }

    private val ktvServiceProtocol = getImplInstance()
    private lateinit var ktvApiProtocol: KTVApi

    /**
     * Room model live data
     */
    val roomModelLiveData = MutableLiveData(joinRoomInfo)

    /**
     * The Room delete live data.
     */
    val roomDeleteLiveData = MutableLiveData<Boolean>()

    /**
     * The Room time up live data.
     */
    val roomTimeUpLiveData = MutableLiveData<Boolean>()

    /**
     * 麦位信息
     */
    private var isOnSeat = false

    val userEnterSeatLiveData = MutableLiveData<RoomMicSeatInfo>()
    val userLeaveSeatLiveData = MutableLiveData<RoomMicSeatInfo>()

    val updateSeatLiveData = MutableLiveData<RoomMicSeatInfo>()

    private val seatMap = mutableMapOf<Int, RoomMicSeatInfo>()

    fun getCacheSeat(userId: String): RoomMicSeatInfo? {
        return seatMap.entries.firstOrNull { it.value.user?.userId == userId }?.value
    }

    /**
     * User list live data
     */
    val userListLiveData = MutableLiveData<List<AUIUserInfo>>()

    private val choristerInfoList = mutableListOf<RoomChoristerInfo>()

    fun getSongChorusInfo(userId: String): RoomChoristerInfo? {
        return choristerInfoList.firstOrNull { it.userId == userId }
    }

    val userJoinChorusLiveData = MutableLiveData<RoomChoristerInfo>()
    val userLeaveChorusLiveData = MutableLiveData<RoomChoristerInfo>()

    /**
     * The Volume live data.
     */
    val volumeLiveData = MutableLiveData<VolumeModel>()

    /**
     * 歌词信息
     */
    val songsOrderedLiveData = MutableLiveData<List<RoomSongInfo>?>()

    /**
     * The Song playing live data.
     */
    val songPlayingLiveData = MutableLiveData<RoomSongInfo?>()


    /**
     * The type Line score.
     */
    inner class LineScore {
        /**
         * The Score.
         */
        var score = 0

        /**
         * The Index.
         */
        var index = 0

        /**
         * The Cumulative score.
         */
        var cumulativeScore = 0

        /**
         * The Total.
         */
        var total = 0
    }

    /**
     * The Main singer score live data.
     */
    @JvmField
    val mainSingerScoreLiveData = MutableLiveData<LineScore>()

    /**
     * Player/RTC信息
     */
    var streamId = 0

    /**
     * The enum Player music status.
     */
    enum class PlayerMusicStatus {
        /**
         * On prepare player music status.
         */
        ON_PREPARE,

        /**
         * On playing player music status.
         */
        ON_PLAYING,

        /**
         * On pause player music status.
         */
        ON_PAUSE,

        /**
         * On stop player music status.
         */
        ON_STOP,

        /**
         * On lrc reset player music status.
         */
        ON_LRC_RESET,

        /**
         * On changing start player music status.
         */
        ON_CHANGING_START,

        /**
         * On changing end player music status.
         */
        ON_CHANGING_END
    }

    /**
     * The Player music status live data.
     */
    val playerMusicStatusLiveData = MutableLiveData<PlayerMusicStatus>()

    /**
     * The Load music progress live data.
     */
    // 加载音乐进度
    val loadMusicProgressLiveData = MutableLiveData<Int>()

    /**
     * The enum Join chorus status.
     */
    enum class JoinChorusStatus {
        /**
         * On idle join chorus status.
         */
        ON_IDLE,

        /**
         * On join chorus join chorus status.
         */
        ON_JOIN_CHORUS,

        /**
         * On join failed join chorus status.
         */
        ON_JOIN_FAILED,

        /**
         * On leave chorus join chorus status.
         */
        ON_LEAVE_CHORUS
    }

    /**
     * The Joinchorus status live data.
     */
    val joinchorusStatusLiveData = MutableLiveData<JoinChorusStatus>()

    /**
     * The No lrc live data.
     */
    val noLrcLiveData = MutableLiveData<Boolean>()

    /**
     * The Player music open duration live data.
     */
    val playerMusicOpenDurationLiveData = MutableLiveData<Long>()

    /**
     * The Player music play complete live data.
     */
    val playerMusicPlayCompleteLiveData = MutableLiveData<ScoringAverageModel>()

    /**
     * The Network status live data.
     */
    val networkStatusLiveData = MutableLiveData<NetWorkEvent>()

    /**
     * The Scoring algo control live data.
     */
    val scoringAlgoControlLiveData = MutableLiveData<ScoringAlgoControlModel>()

    /**
     * The Scoring algo live data.
     */
    val scoringAlgoLiveData = MutableLiveData<Int>()

    /**
     * rtc engine
     */
    private var mRtcEngine: RtcEngineEx? = null

    /**
     * 主版本的音频设置
     */
    private val mainChannelMediaOption = ChannelMediaOptions()

    /**
     * 播放器配置
     */
    var mSetting: MusicSettingBean? = null

    /**
     * 是否开启后台播放
     */
    var mDebugSetting: KTVDebugSettingBean? = null

    /**
     * 是否开启后台播放
     */
    private val isBackPlay = false

    /**
     * 合唱人数
     */
    var chorusNum = 0

    /**
     * The M sound card setting bean.
     */
    var mSoundCardSettingBean: SoundCardSettingBean? = null

    /**
     * Is room owner
     */
    val isRoomOwner: Boolean get() = roomModelLiveData.getValue()?.roomOwner?.userId == mUser.id.toString()

    /**
     * Room owner id
     */
    val roomOwnerId: String get() = roomModelLiveData.getValue()?.roomOwner?.userId ?: ""

    /**
     * local user
     */
    private val mUser: User get() = UserManager.getInstance().user

    private val mRtcAppid: String get() = BuildConfig.AGORA_APP_ID

    /**
     * Init.
     */
    fun initData() {
        initSettings()
        initRTCPlayer()
        initSubscribeListener()
        initRoom()
    }

    /**
     * Release boolean.
     *
     * @return the boolean
     */
    fun release(): Boolean {
        KTVLogger.d(TAG, "release called")
        streamId = 0
        ktvServiceProtocol.reset()
        mRtcEngine?.let {
            ktvApiProtocol.release()
            mSoundCardSettingBean?.enable(false, true) { }
            it.enableInEarMonitoring(false, Constants.EAR_MONITORING_FILTER_NONE)
            it.leaveChannel()
            RtcEngineEx.destroy()
            mRtcEngine = null
            return true
        }
        return false
    }

    val sDKBuildNum: String get() = RtcEngineEx.getSdkVersion()

    private fun initSubscribeListener() {
        ktvServiceProtocol.subscribeListener(object : KtvServiceListenerProtocol {

            override fun onUserListDidChanged(userList: List<AUIUserInfo>) {
                userListLiveData.postValue(userList)
            }

            override fun onRoomDidChanged(roomInfo: AUIRoomInfo) {

            }

            override fun onRoomExpire(channelName: String) {
                roomTimeUpLiveData.postValue(true)
            }

            override fun onRoomDestroy(channelName: String) {
                roomDeleteLiveData.postValue(true)
            }

            override fun onUserEnterSeat(roomSeatModel: RoomMicSeatInfo) {
                KTVLogger.d(TAG, "onUserEnterSeat $roomSeatModel")
                seatMap[roomSeatModel.seatIndex] = roomSeatModel
                userEnterSeatLiveData.postValue(roomSeatModel)
            }

            override fun onUserLeaveSeat(roomSeatModel: RoomMicSeatInfo) {
                KTVLogger.d(TAG, "onUserLeaveSeat $roomSeatModel")
                seatMap[roomSeatModel.seatIndex] = roomSeatModel
                userLeaveSeatLiveData.postValue(roomSeatModel)
                if (roomSeatModel.user?.userId == mUser.id.toString()) {
                    isOnSeat = false
                    mRtcEngine?.let {
                        mainChannelMediaOption.publishCameraTrack = false
                        mainChannelMediaOption.publishMicrophoneTrack = false
                        mainChannelMediaOption.enableAudioRecordingOrPlayout = true
                        mainChannelMediaOption.autoSubscribeVideo = true
                        mainChannelMediaOption.autoSubscribeAudio = true
                        mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                        it.updateChannelMediaOptions(mainChannelMediaOption)
                    }
                    updateVolumeStatus(false)
                    val songPlayingData = songPlayingLiveData.getValue()
                    if (songPlayingData == null) {
                        return
                    } else {
                        // TODO:
//                        if (roomSeatModel.chorusSongCode == songPlayingData.songNo + songPlayingData.createAt) {
//                            ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null)
//                            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_LEAVE_CHORUS)
//                        }
                    }
                }
            }

            override fun onUserAudioMute(userId: String, mute: Boolean) {
                val seatInfo = seatMap.values.firstOrNull { it.user?.userId == userId }
                seatInfo?.let { roomMicSeatInfo ->
                    updateSeatLiveData.value = roomMicSeatInfo
                }
            }

            override fun onUserVideoMute(userId: String, mute: Boolean) {
                val seatInfo = seatMap.values.firstOrNull { it.user?.userId == userId }
                seatInfo?.let { roomMicSeatInfo ->
                    updateSeatLiveData.value = roomMicSeatInfo
                }
            }

            override fun onChosenSongListDidChanged(chosenSongList: List<RoomSongInfo>) {
                songsOrderedLiveData.value = chosenSongList
            }

            override fun onChoristerListDidChanged(choristerList: List<RoomChoristerInfo>) {
                didChoristerListChanged(choristerList)
            }

            override fun onChoristerDidEnter(chorister: RoomChoristerInfo) {
                userJoinChorusLiveData.value = chorister
            }

            override fun onChoristerDidLeave(chorister: RoomChoristerInfo) {
                userLeaveChorusLiveData.value = chorister
            }
        })
    }

    private fun didChoristerListChanged(choristerList: List<RoomChoristerInfo>) {
        val songInfo = songPlayingLiveData.value
        var chorusNowNum = 0
        choristerList.forEach { roomChoristerInfo ->
            if (songInfo?.songNo == roomChoristerInfo.chorusSongNo) {
                chorusNowNum++
            }
        }
        if (chorusNum == 0 && chorusNowNum > 0) { // 有人加入合唱
            soloSingerJoinChorusMode(true)
        } else if (chorusNum > 0 && chorusNowNum == 0) { // 最后一人退出合唱
            soloSingerJoinChorusMode(false)
        }
        chorusNum = chorusNowNum
        choristerInfoList.clear()
        choristerInfoList.addAll(choristerList)
    }

    private fun initRoom() {
        ktvServiceProtocol.getChosenSongList { error, songList ->
            songsOrderedLiveData.postValue(songList)
        }
        ktvServiceProtocol.getChoristerList { error, choristerList ->
            if (error == null) {
                didChoristerListChanged(choristerList)
            }
        }
        ktvServiceProtocol.getAllSeatMap { error, map ->
            seatMap.clear()
            seatMap.putAll(map)
            map.values.forEach {
                if (!it.user?.userId.isNullOrEmpty()) {
                    userEnterSeatLiveData.postValue(it)
                }
            }
        }
    }

    fun getUserInfoBySeat(seatInfo: RoomMicSeatInfo): AUIUserInfo {
        var userInfo = ktvServiceProtocol.getUserInfo(seatInfo.user?.userId ?: "")
        if (userInfo == null) {
            seatInfo.user?.let { seatUser ->
                userInfo = AUIUserInfo().apply {
                    userId = seatUser.userId
                    userName = seatUser.userName
                    userAvatar = seatUser.userAvatar
                }
            }
        }
        return userInfo ?: AUIUserInfo()
    }

    fun getUserInfo(userId: String): AUIUserInfo {
        return ktvServiceProtocol.getUserInfo(userId) ?: AUIUserInfo()
    }

    /**
     * Exit room
     *
     */
    fun exitRoom() {
        KTVLogger.d(TAG, "RoomLivingViewModel.exitRoom() called")
        ktvServiceProtocol.leaveRoom { e: Exception? ->
            if (e == null) { // success
                KTVLogger.d(TAG, "RoomLivingViewModel.exitRoom() success")
                roomDeleteLiveData.postValue(false)
                roomTimeUpLiveData.postValue(false)
            } else { // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.exitRoom() failed: " + e.message)
            }
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
        }
    }

    /**
     * Solo singer join chorus mode.
     *
     * @param isJoin the is join
     */
    fun soloSingerJoinChorusMode(isJoin: Boolean) {
        val songPlaying = songPlayingLiveData.value ?: return
        if (songPlaying.userNo == mUser.id.toString()) {
            if (isJoin) {
                // 有人加入合唱
                ktvApiProtocol.switchSingerRole(KTVSingRole.LeadSinger, null)
            } else {
                // 最后一人退出合唱
                ktvApiProtocol.switchSingerRole(KTVSingRole.SoloSinger, null)
            }
        }
    }

    /**
     * 上麦
     *
     * @param onSeatIndex the on seat index
     */
    fun haveSeat(onSeatIndex: Int) {
        KTVLogger.d(TAG, "RoomLivingViewModel.haveSeat() called: $onSeatIndex")
        ktvServiceProtocol.enterSeat(onSeatIndex) { e: Exception? ->
            if (e == null) { // success
                KTVLogger.d(TAG, "RoomLivingViewModel.haveSeat() success")
                isOnSeat = true
                mRtcEngine?.let {
                    mainChannelMediaOption.publishCameraTrack = false
                    mainChannelMediaOption.publishMicrophoneTrack = true
                    mainChannelMediaOption.enableAudioRecordingOrPlayout = true
                    mainChannelMediaOption.autoSubscribeVideo = true
                    mainChannelMediaOption.autoSubscribeAudio = true
                    mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    it.updateChannelMediaOptions(mainChannelMediaOption)
                }
                toggleMic(false)
            } else { // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.haveSeat() failed: " + e.message)
            }
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
        }
    }

    /**
     * 离开麦位
     *
     * @param seatModel the seat model
     */
    fun leaveSeat(seatModel: RoomMicSeatInfo) {
        KTVLogger.d(TAG, "RoomLivingViewModel.leaveSeat() called")
        ktvServiceProtocol.outSeat(seatModel.seatIndex) { e: Exception? ->
            if (e == null) { // success
                KTVLogger.d(TAG, "RoomLivingViewModel.leaveSeat() success")
                if (seatModel.user?.userId == mUser.id.toString()) {
                    isOnSeat = false
                    val userInfo = getUserInfoBySeat(seatModel)
                    val isAudioMuted = userInfo.muteAudio
                    if (isAudioMuted) {
                        mRtcEngine?.let {
                            mainChannelMediaOption.publishCameraTrack = false
                            mainChannelMediaOption.publishMicrophoneTrack = false
                            mainChannelMediaOption.enableAudioRecordingOrPlayout = true
                            mainChannelMediaOption.autoSubscribeVideo = true
                            mainChannelMediaOption.autoSubscribeAudio = true
                            mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            it.updateChannelMediaOptions(mainChannelMediaOption)
                        }
                        updateVolumeStatus(false)
                    }
                }
                val songPlayingValue = songPlayingLiveData.value ?: return@outSeat
                val choristerInfo = getSongChorusInfo(mUser.id.toString())
                val isJoinChorus = choristerInfo != null && choristerInfo?.chorusSongNo == songPlayingValue.songNo
                if (isJoinChorus && seatModel.user?.userId == mUser.id.toString()) {
                    leaveChorus()
                }
            } else { // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.leaveSeat() failed: " + e.message)
            }
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
        }
    }

    /**
     * Is camera opened
     */
    private var isCameraOpened = false

    /**
     * Toggle self video.
     *
     * @param isOpen the is open
     */
    fun toggleSelfVideo(isOpen: Boolean) {
        KTVLogger.d(TAG, "RoomLivingViewModel.toggleSelfVideo() called：$isOpen")
        ktvServiceProtocol.muteUserVideo(!isOpen) { e: Exception? ->
            if (e == null) { // success
                KTVLogger.d(TAG, "RoomLivingViewModel.toggleSelfVideo() success")
                isCameraOpened = isOpen
                mRtcEngine?.enableLocalVideo(isOpen)
                val channelMediaOption = ChannelMediaOptions()
                channelMediaOption.publishCameraTrack = isOpen
                mRtcEngine?.updateChannelMediaOptions(channelMediaOption)
            } else { // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.toggleSelfVideo() failed: " + e.message)
            }
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
        }
    }

    /**
     * Toggle mic
     *
     * @param isUnMute
     */
    fun toggleMic(isUnMute: Boolean) {
        KTVLogger.d(TAG, "RoomLivingViewModel.toggleMic() called：$isUnMute")
        updateVolumeStatus(isUnMute)
        ktvServiceProtocol.muteUserAudio(!isUnMute) { e: Exception? ->
            if (e == null) { // success
                KTVLogger.d(TAG, "RoomLivingViewModel.toggleMic() success")
            } else { // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.toggleMic() failed: " + e.message)
            }
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
        }
    }

    /**
     * Update volume status
     *
     * @param isUnMute
     */
    private fun updateVolumeStatus(isUnMute: Boolean) {
        ktvApiProtocol.muteMic(!isUnMute)
        if (!isUnMute && mSetting?.mEarBackEnable == true) {
            mRtcEngine?.enableInEarMonitoring(false, Constants.EAR_MONITORING_FILTER_NONE)
        } else if (isUnMute && mSetting?.mEarBackEnable == true) {
            mRtcEngine?.enableInEarMonitoring(true, Constants.EAR_MONITORING_FILTER_NONE)
        }
        if (isUnMute) {
            KTVLogger.d(TAG, "unmute! setMicVolume: $micOldVolume")
            mRtcEngine?.adjustRecordingSignalVolume(micOldVolume)
        }
    }

    /**
     * Init songs.
     */
    private fun initSongs() {
        onSongChanged()
    }

    /**
     * On song changed
     *
     */
    private fun onSongChanged() {
        ktvServiceProtocol.getChosenSongList { e: Exception?, data: List<RoomSongInfo>? ->
            if (e == null && data != null) { // success
                KTVLogger.d(TAG, "RoomLivingViewModel.onSongChanged() success")
                songsOrderedLiveData.postValue(data)
                if (data.isNotEmpty()) {
                    val value = songPlayingLiveData.getValue()
                    val songPlaying = data[0]
                    if (value == null) {
                        // 无已点歌曲， 直接将列表第一个设置为当前播放歌曲
                        KTVLogger.d(TAG, "RoomLivingViewModel.onSongChanged() chosen song list is empty")
                        songPlayingLiveData.postValue(songPlaying)
                    } else if (value.songNo != songPlaying.songNo) {
                        // 当前有已点歌曲, 且更新歌曲和之前歌曲非同一首
                        KTVLogger.d(TAG, "RoomLivingViewModel.onSongChanged() single or first chorus")
                        songPlayingLiveData.postValue(songPlaying)
                    }
                } else {
                    KTVLogger.d(TAG, "RoomLivingViewModel.onSongChanged() return is emptyList")
                    songPlayingLiveData.postValue(null)
                }
            } else { // failed
                if (e != null) {
                    KTVLogger.e(TAG, "RoomLivingViewModel.getSongChosenList() failed: $e")
                }
                e?.message?.let { error ->
                    CustomToast.show(error, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    /**
     * Song chosen list
     */
    fun getSongChosenList() {
        ktvServiceProtocol.getChosenSongList { e: Exception?, data: List<RoomSongInfo>? ->
            if (e == null && data != null) { // success
                KTVLogger.d(TAG, "RoomLivingViewModel.getSongChosenList() success")
                songsOrderedLiveData.postValue(data)
            } else { // failed
                if (e != null) {
                    KTVLogger.e(TAG, "RoomLivingViewModel.getSongChosenList() failed: " + e.message)
                }
                e?.message?.let { error ->
                    CustomToast.show(error, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    /**
     * Song types
     */
    fun getSongTypes(): LiveData<LinkedHashMap<Int, String>> {
        KTVLogger.d(TAG, "RoomLivingViewModel.getSongTypes() called")
        val liveData = MutableLiveData<LinkedHashMap<Int, String>>()
        ktvApiProtocol.fetchMusicCharts { id: String?, status: Int, list: Array<out MusicChartInfo>? ->
            KTVLogger.d(TAG, "RoomLivingViewModel.getSongTypes() return")
            val musicList: List<MusicChartInfo> = if (list == null) emptyList() else listOf(*list)
            val types = LinkedHashMap<Int, String>()
            // 重新排序 ----> 按照（嗨唱推荐、抖音热歌、热门新歌、KTV必唱）这个顺序进行怕苦
            for (i in 0..3) {
                for (musicChartInfo in musicList) {
                    if ((i == 0 && musicChartInfo.type == 3) ||
                        (i == 1 && musicChartInfo.type == 4) ||
                        (i == 2 && musicChartInfo.type == 2) ||
                        (i == 3 && musicChartInfo.type == 6)
                    ) {
                        types[musicChartInfo.type] = musicChartInfo.name
                    }
                }
            }
            // 将剩余的插到尾部
            for (musicChartInfo in musicList) {
                if (!types.containsKey(musicChartInfo.type)) {
                    types[musicChartInfo.type] = musicChartInfo.name
                }
            }
            // 因为榜单基本是固化的，防止拉取列表失败，直接写入配置
            if (musicList.isEmpty()) {
                types[3] = "嗨唱推荐"
                types[4] = "抖音热歌"
                types[2] = "新歌榜"
                types[6] = "KTV必唱"
                types[0] = "项目热歌榜单"
                types[1] = "声网热歌榜"
                types[5] = "古风热歌"
            }
            liveData.postValue(types)
            null
        }
        return liveData
    }

    /**
     * Get song list
     *
     * @param type
     * @param page
     * @return
     */
    fun getSongList(type: Int, page: Int): LiveData<List<RoomSongInfo>> {
        KTVLogger.d(TAG, "RoomLivingViewModel.getSongList() called, type:$type page:$page")
        val liveData = MutableLiveData<List<RoomSongInfo>>()
        val jsonOption = "{\"pitchType\":1,\"needLyric\":true}"
        ktvApiProtocol.searchMusicByMusicChartId(
            type,
            page,
            30,
            jsonOption
        ) { id: String?, status: Int, p: Int, size: Int, total: Int, list: Array<out Music>? ->
            KTVLogger.d(TAG, "RoomLivingViewModel.getSongList() return")
            val musicList: List<Music> = if (list == null) emptyList() else listOf(*list)
            val songs: MutableList<RoomSongInfo> = ArrayList()
            // 需要再调一个接口获取当前已点的歌单来补充列表信息 >_<
            ktvServiceProtocol.getChosenSongList { e: Exception?, songsChosen: List<RoomSongInfo>? ->
                if (e == null) { // success
                    for (music in musicList) {
                        var songItem: RoomSongInfo? = null
                        songsChosen?.forEach { roomSelSongModel ->
                            if (roomSelSongModel.songNo == music.songCode.toString()) {
                                songItem = roomSelSongModel
                                return@forEach
                            }
                        }
                        if (songItem == null) {
                            songItem = RoomSongInfo(
                                music.name,
                                music.songCode.toString(),
                                music.singer,
                                music.poster,
                                "",
                                "",
                                0,
                                0,
                                0,
                                0.0
                            )
                        }
                        songs.add(songItem!!)
                    }
                    liveData.postValue(songs)
                } else {
                    e?.message?.let { error ->
                        CustomToast.show(error, Toast.LENGTH_SHORT)
                    }
                }
                return@getChosenSongList
            }
            return@searchMusicByMusicChartId
        }
        return liveData
    }

    /**
     * 搜索歌曲
     *
     * @param condition the condition
     * @return the live data
     */
    fun searchSong(condition: String): LiveData<List<RoomSongInfo>> {
        // 从RTC中搜索歌曲
        KTVLogger.d(TAG, "RoomLivingViewModel.searchSong() called, condition:$condition")
        val liveData = MutableLiveData<List<RoomSongInfo>>()

        // 过滤没有歌词的歌曲
        val jsonOption = "{\"pitchType\":1,\"needLyric\":true}"
        ktvApiProtocol.searchMusicByKeyword(
            condition, 0, 50, jsonOption
        ) { id: String?, status: Int, p: Int, size: Int, total: Int, list: Array<out Music>? ->
            val musicList: List<Music> = if (list == null) emptyList() else listOf(*list)

            val songs: MutableList<RoomSongInfo> = ArrayList()

            // 需要再调一个接口获取当前已点的歌单来补充列表信息 >_<
            ktvServiceProtocol.getChosenSongList { e: Exception?, songsChosen: List<RoomSongInfo>? ->
                if (e == null && songsChosen != null) {
                    // success
                    for (music in musicList) {
                        var songItem: RoomSongInfo? = null
                        for (roomSelSongModel in songsChosen) {
                            if (roomSelSongModel.songNo == music.songCode.toString()) {
                                songItem = roomSelSongModel
                                break
                            }
                        }
                        if (songItem == null) {
                            songItem = RoomSongInfo(
                                music.name, music.songCode.toString(),
                                music.singer,
                                music.poster,
                                "",
                                "",
                                0,
                                0,
                                0,
                                0.0
                            )
                        }
                        songs.add(songItem)
                    }
                    liveData.postValue(songs)
                } else {
                    e?.message?.let { error ->
                        CustomToast.show(error, Toast.LENGTH_SHORT)
                    }
                }
            }
        }
        return liveData
    }

    /**
     * 点歌
     *
     * @param songModel the song model
     * @param isChorus  the is chorus
     * @return the live data
     */
    fun chooseSong(songModel: RoomSongInfo, isChorus: Boolean): LiveData<Boolean> {
        KTVLogger.d(TAG, "RoomLivingViewModel.chooseSong() called, name:" + songModel.name + " isChorus:" + isChorus)
        val liveData = MutableLiveData<Boolean>()
        ktvServiceProtocol.chooseSong(songModel) { e: Exception? ->
            if (e == null) { // success
                KTVLogger.d(TAG, "RoomLivingViewModel.chooseSong() success")
            } else { // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.chooseSong() failed: $e")
            }
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
            liveData.postValue(e == null)
        }
        return liveData
    }

    /**
     * 删歌
     *
     * @param songModel the song model
     */
    fun deleteSong(songModel: RoomSongInfo) {
        KTVLogger.d(TAG, "RoomLivingViewModel.deleteSong() called, name:" + songModel.name)
        ktvServiceProtocol.removeSong(songModel.songNo) { e ->
            if (e == null) { // success: do nothing for subscriber dealing with the event already
                KTVLogger.d(TAG, "RoomLivingViewModel.deleteSong() success")
            } else { // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.deleteSong() failed: $e")
            }
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
        }
    }

    /**
     * 置顶歌曲
     *
     * @param songModel the song model
     */
    fun topUpSong(songModel: RoomSongInfo) {
        KTVLogger.d(TAG, "RoomLivingViewModel.topUpSong() called, name:" + songModel.name)
        ktvServiceProtocol.pinSong(songModel.songNo) { e: Exception? ->
            if (e == null) { // success: do nothing for subscriber dealing with the event already
                KTVLogger.d(TAG, "RoomLivingViewModel.topUpSong() success")
            } else { // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.topUpSong() failed: " + e.message)
            }
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
        }
    }

    /**
     * 点击加入合唱
     */
    fun joinChorus() {
        KTVLogger.d(TAG, "RoomLivingViewModel.joinChorus() called")
        if (mRtcEngine?.connectionState != CONNECTION_STATE_TYPE.getValue(CONNECTION_STATE_TYPE.CONNECTION_STATE_CONNECTED)) {
            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
            return
        }
        val musicModel = songPlayingLiveData.getValue()
        if (musicModel == null) {
            KTVLogger.e(TAG, "RoomLivingViewModel.joinChorus() failed, no song playing now")
            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
            return
        }
        if (!isOnSeat) {
            // 不在麦上， 自动上麦
            ktvServiceProtocol.enterSeat(null) { err: Exception? ->
                if (err == null) {
                    isOnSeat = true
                    //自动开麦
                    mainChannelMediaOption.publishMicrophoneTrack = true
                    mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    mRtcEngine?.updateChannelMediaOptions(mainChannelMediaOption)
                    innerJoinChorus(musicModel.songNo)
                } else {
                    joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
                }
            }
        } else {
            // 在麦上，直接加入合唱
            innerJoinChorus(musicModel.songNo)
        }
    }

    /**
     * 加入合唱
     *
     * @param songCode
     */
    private fun innerJoinChorus(songCode: String) {
        ktvApiProtocol.loadMusic(songCode.toLong(),
            KTVLoadMusicConfiguration(
                songCode,
                songPlayingLiveData.getValue()?.userNo?.toIntOrNull() ?: -1,
                KTVLoadMusicMode.LOAD_MUSIC_ONLY,
                false
            ),
            object : IMusicLoadStateListener {
                override fun onMusicLoadProgress(
                    songCode: Long,
                    percent: Int,
                    status: MusicLoadStatus,
                    msg: String?,
                    lyricUrl: String?
                ) {
                    KTVLogger.d(TAG, "onMusicLoadProgress, songCode: $songCode percent: $percent lyricUrl: $lyricUrl")
                    loadMusicProgressLiveData.postValue(percent)
                }

                override fun onMusicLoadFail(songCode: Long, reason: KTVLoadMusicFailReason) {
                    joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
                }

                override fun onMusicLoadSuccess(songCode: Long, lyricUrl: String) {
                    ktvApiProtocol.switchSingerRole(KTVSingRole.CoSinger, object : ISwitchRoleStateListener {
                        override fun onSwitchRoleFail(reason: SwitchRoleFailReason) {
                            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
                        }

                        override fun onSwitchRoleSuccess() {
                            if (isOnSeat) {
                                // 成为合唱成功
                                joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_CHORUS)
                                // 麦位UI 同步
                                val songModel = songPlayingLiveData.value ?: return
                                ktvServiceProtocol.joinChorus(songModel.songNo) { e: Exception? ->
                                    if (e == null) { // success
                                        KTVLogger.d(TAG, "RoomLivingViewModel.joinChorus() success")
                                    } else { // failure
                                        KTVLogger.e(TAG, "RoomLivingViewModel.joinChorus() failed: $e ")
                                    }
                                    e?.message?.let { error ->
                                        CustomToast.show(error, Toast.LENGTH_SHORT)
                                    }
                                }
                            } else {
                                ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null)
                                joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
                            }
                        }
                    })
                }
            })
    }

    /**
     * 退出合唱
     */
    fun leaveChorus() {
        KTVLogger.d(TAG, "RoomLivingViewModel.leaveChorus() called")
        if (isOnSeat) {
            ktvServiceProtocol.leaveChorus { e: Exception? ->
                if (e == null) { // success
                    KTVLogger.d(TAG, "RoomLivingViewModel.leaveChorus() called")
                    ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null)
                    joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_LEAVE_CHORUS)
                } else {
                    // failure
                    KTVLogger.e(TAG, "RoomLivingViewModel.leaveChorus() failed: $e")
                }
                e?.message?.let { error ->
                    CustomToast.show(error, Toast.LENGTH_SHORT)
                }
            }
        } else {
            ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null)
            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_LEAVE_CHORUS)
        }
    }

    /**
     * 开始切歌
     */
    fun changeMusic() {
        KTVLogger.d(TAG, "RoomLivingViewModel.changeMusic() called")
        val musicModel = songPlayingLiveData.getValue() ?: run {
            KTVLogger.e(TAG, "RoomLivingViewModel.changeMusic() failed, no song is playing now!")
            return
        }
        //ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, "", null);
        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_CHANGING_START)
        ktvServiceProtocol.removeSong(musicModel.songNo) { e: Exception? ->
            if (e == null) { // success do nothing for dealing in song subscriber
                KTVLogger.d(TAG, "RoomLivingViewModel.changeMusic() success")
            } else { // failed
                KTVLogger.e(TAG, "RoomLivingViewModel.changeMusic() failed: " + e.message)
            }
            playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_CHANGING_END)
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
        }
    }

    /**
     * 设置歌词view
     *
     * @param view the view
     */
    fun setLrcView(view: ILrcView) {
        ktvApiProtocol.setLrcView(view)
        mSetting?.let { setting ->
            ktvApiProtocol.enableProfessionalStreamerMode(setting.mProfessionalModeEnable)
        }
    }

    // ======================= Player/RTC/MPK相关 =======================
    // ------------------ 初始化音乐播放设置面版 ------------------
    private fun initSettings() {
        // debug 设置
        mDebugSetting = KTVDebugSettingBean(object : KTVDebugSettingsDialog.Callback {
            override fun onAudioDumpEnable(enable: Boolean) {
                if (enable) {
                    mRtcEngine?.setParameters("{\"rtc.debug.enable\": true}")
                    mRtcEngine?.setParameters(
                        "{\"che.audio.frame_dump\":{\"location\":\"all\",\"action\":\"start\"," +
                                "\"max_size_bytes\":\"120000000\",\"uuid\":\"123456789\",\"duration\":\"1200000\"}}"
                    )
                } else {
                    mRtcEngine?.setParameters("{\"rtc.debug.enable\": false}")
                }
            }

            override fun onScoringControl(level: Int, offset: Int) {
                scoringAlgoControlLiveData.postValue(ScoringAlgoControlModel(level, offset))
            }

            override fun onSetParameters(parameters: String) {
                mRtcEngine?.setParameters(parameters)
            }
        })

        // 音乐设置
        mSetting = MusicSettingBean(object : MusicSettingCallback {
            override fun onEarChanged(earBackEnable: Boolean) {
                KTVLogger.d(TAG, "onEarChanged: $earBackEnable")
                val userInfo = getUserInfo(mUser.id.toString())
                val isAudioMuted = userInfo.muteAudio
                if (isAudioMuted) {
                    return
                }
                mRtcEngine?.enableInEarMonitoring(earBackEnable, Constants.EAR_MONITORING_FILTER_NONE)
            }

            override fun onEarBackVolumeChanged(volume: Int) {
                KTVLogger.d(TAG, "onEarBackVolumeChanged: $volume")
                mRtcEngine?.setInEarMonitoringVolume(volume)
            }

            override fun onEarBackModeChanged(mode: Int) {
                KTVLogger.d(TAG, "onEarBackModeChanged: $mode")
                if (mode == 1) { // OpenSL
                    mRtcEngine?.setParameters("{\"che.audio.opensl.mode\": 0}")
                } else if (mode == 2) { // Oboe
                    mRtcEngine?.setParameters("{\"che.audio.oboe.enable\": true}")
                }
            }

            override fun onMicVolChanged(vol: Int) {
                setMicVolume(vol)
            }

            override fun onAccVolChanged(vol: Int) {
                setMusicVolume(vol)
            }

            override fun onRemoteVolChanged(volume: Int) {
                KTVApi.remoteVolume = volume
                mRtcEngine?.adjustPlaybackSignalVolume(volume)
            }

            override fun onAudioEffectChanged(audioEffect: Int) {
                KTVLogger.d(TAG, "onAudioEffectChanged: $audioEffect")
                mRtcEngine?.setAudioEffectPreset(audioEffect)
            }

            override fun onScoringDifficultyChanged(difficulty: Int) {
                KTVLogger.d(TAG, "onScoringDifficultyChanged: $difficulty")
                scoringAlgoLiveData.postValue(difficulty)
            }

            override fun onProfessionalModeChanged(enable: Boolean) {
                KTVLogger.d(TAG, "onProfessionalModeChanged: $enable")
                ktvApiProtocol.enableProfessionalStreamerMode(enable)
            }

            override fun onMultiPathChanged(enable: Boolean) {
                KTVLogger.d(TAG, "onMultiPathChanged: $enable")
                ktvApiProtocol.enableMulitpathing(enable)
            }

            override fun onAECLevelChanged(level: Int) {
                KTVLogger.d(TAG, "onAECLevelChanged: $level")
                // aiaec关闭的情况下音质选项才能生效
                when (level) {
                    0 -> mRtcEngine?.setParameters("{\"che.audio.aec.split_srate_for_48k\": 16000}")
                    1 -> mRtcEngine?.setParameters("{\"che.audio.aec.split_srate_for_48k\": 24000}")
                    2 -> mRtcEngine?.setParameters("{\"che.audio.aec.split_srate_for_48k\": 48000}")
                }
            }

            override fun onLowLatencyModeChanged(enable: Boolean) {
                KTVLogger.d(TAG, "onLowLatencyModeChanged: $enable")
                if (enable) {
                    mRtcEngine?.setParameters("{\"che.audio.ains_mode\": -1}")
                } else {
                    mRtcEngine?.setParameters("{\"che.audio.ains_mode\": 0}")
                }
            }

            override fun onAINSModeChanged(mode: Int) {
                KTVLogger.d(TAG, "onAINSModeChanged: $mode")
                when (mode) {
                    0 -> { // 关闭
                        mRtcEngine?.setParameters("{\"che.audio.ains_mode\": 0}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerBound\": 80}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerMask\": 50}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.statisticalbound\": 5}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.finallowermask\": 30}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.enhfactorstastical\": 200}")
                    }

                    1 -> { // 中
                        mRtcEngine?.setParameters("{\"che.audio.ains_mode\": 2}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerBound\": 80}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerMask\": 50}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.statisticalbound\": 5}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.finallowermask\": 30}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.enhfactorstastical\": 200}")
                    }

                    2 -> { // 高
                        mRtcEngine?.setParameters("{\"che.audio.ains_mode\": 2}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerBound\": 10}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerMask\": 10}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.statisticalbound\": 0}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.finallowermask\": 8}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.enhfactorstastical\": 200}")
                    }
                }
            }

            override fun onAIAECChanged(enable: Boolean) {
                KTVLogger.d(TAG, "onAIAECChanged: $enable")
                if (enable) {
                    mRtcEngine?.setParameters("{\"che.audio.aiaec.working_mode\": 1}")
                } else {
                    mRtcEngine?.setParameters("{\"che.audio.aiaec.working_mode\": 0}")
                }
            }

            override fun onAIAECStrengthSelect(strength: Int) {
                KTVLogger.d(TAG, "onAIAECStrengthSelect: $strength")
                mRtcEngine?.setParameters("{\"che.audio.aiaec.postprocessing_strategy\":$strength}")
            }
        })
        mSoundCardSettingBean = SoundCardSettingBean { presetValue: Int, gainValue: Float, gender: Int, effect: Int ->
            mRtcEngine?.setParameters(
                "{\"che.audio.virtual_soundcard\":{\"preset\":" + presetValue +
                        ",\"gain\":" + gainValue +
                        ",\"gender\":" + gender +
                        ",\"effect\":" + effect + "}}"
            )
        }
    }

    private fun initRTCPlayer() {
        val roomModel = roomModelLiveData.getValue() ?: run {
            throw NullPointerException("roomInfo is null!")
        }
        if (TextUtils.isEmpty(mRtcAppid)) {
            throw NullPointerException("please check \"gradle.properties\"")
        }
        if (mRtcEngine != null) return
        // ------------------ 初始化RTC ------------------
        val config = RtcEngineConfig()
        config.mContext = AgoraApplication.the()
        config.mAppId = mRtcAppid
        config.mEventHandler = object : IRtcEngineEventHandler() {
            override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
                // 网络状态回调, 本地user uid = 0
                if (uid == 0) {
                    networkStatusLiveData.postValue(NetWorkEvent(txQuality, rxQuality))
                }
            }

            override fun onContentInspectResult(result: Int) {
                super.onContentInspectResult(result)
                if (result > 1) {
                    CustomToast.show(R.string.ktv_content, Toast.LENGTH_SHORT)
                }
            }

            override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray) {
                val jsonMsg: JSONObject
                try {
                    val strMsg = String(data)
                    jsonMsg = JSONObject(strMsg)
                    if (jsonMsg.getString("cmd") == "singleLineScore") {
                        val score = jsonMsg.getInt("score")
                        val index = jsonMsg.getInt("index")
                        val cumulativeScore = jsonMsg.getInt("cumulativeScore")
                        val total = jsonMsg.getInt("total")
                        val lineScore = LineScore()
                        lineScore.score = score
                        lineScore.index = index
                        lineScore.cumulativeScore = cumulativeScore
                        lineScore.total = total
                        mainSingerScoreLiveData.postValue(lineScore)
                    } else if (jsonMsg.getString("cmd") == "SingingScore") {
                        val score = jsonMsg.getDouble("score").toFloat()
                        playerMusicPlayCompleteLiveData.postValue(ScoringAverageModel(false, score.toInt()))
                    }
                } catch (exp: JSONException) {
                    KTVLogger.e(TAG, "onStreamMessage:$exp")
                }
            }

            override fun onAudioRouteChanged(routing: Int) { // 0\2\5 earPhone
                super.onAudioRouteChanged(routing)
                KTVLogger.d(TAG, "onAudioRouteChanged, routing:$routing")
                mSetting?.let { setting ->
                    if (routing == 0 || routing == 2 || routing == 5 || routing == 6) {
                        setting.mHasEarPhone = true
                    } else {
                        if (songPlayingLiveData.getValue() != null && setting.mEarBackEnable) {
                            CustomToast.show(R.string.ktv_earphone_close_tip, Toast.LENGTH_SHORT)
                            setting.mEarBackEnable = false
                        }
                        setting.mHasEarPhone = false
                    }
                }
            }

            override fun onLocalAudioStats(stats: LocalAudioStats) {
                super.onLocalAudioStats(stats)
                mSetting?.mEarBackDelay = stats.earMonitorDelay
            }

            override fun onAudioVolumeIndication(speakers: Array<AudioVolumeInfo>, totalVolume: Int) {
                super.onAudioVolumeIndication(speakers, totalVolume)
                for (speaker in speakers) {
                    volumeLiveData.postValue(VolumeModel(speaker.uid, speaker.volume))
                }
            }
        }
        config.mChannelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
        config.mAudioScenario = Constants.AUDIO_SCENARIO_GAME_STREAMING
        config.addExtension("agora_ai_echo_cancellation_extension")
        config.addExtension("agora_ai_noise_suppression_extension")
        try {
            mRtcEngine = RtcEngine.create(config) as RtcEngineEx
        } catch (e: Exception) {
            e.printStackTrace()
            KTVLogger.e(TAG, "RtcEngine.create() called error: $e")
        }
        mRtcEngine?.apply {
            loadExtensionProvider("agora_drm_loader")
            setParameters("{\"che.audio.ains_mode\": -1}")
            setParameters("{\"che.audio.input_sample_rate\" : 48000}")
        }

        // ------------------ 场景化api初始化 ------------------
        KTVApi.debugMode = AgoraApplication.the().isDebugModeOpen
        if (AgoraApplication.the().isDebugModeOpen) {
            KTVApi.mccDomain = "api-test.agora.io"
        }
        ktvApiProtocol = createKTVApi(
            KTVApiConfig(
                appId = mRtcAppid,
                rtmToken = roomModel.rtmToken,
                engine = mRtcEngine!!,
                channelName = roomModel.roomId,
                localUid = mUser.id.toInt(),
                chorusChannelName = roomModel.rtcChorusChannelName,
                chorusChannelToken = roomModel.rtcChorusToken,
                maxCacheSize = 10,
                type = KTVType.Normal,
                musicType = KTVMusicType.SONG_CODE
            )
        )
        ktvApiProtocol.addEventHandler(object : IKTVApiEventHandler() {
            override fun onMusicPlayerStateChanged(state: MediaPlayerState, error: MediaPlayerError, isLocal: Boolean) {
                KTVLogger.d(TAG, "onMusicPlayerStateChanged, state:$state error:$error isLocal:$isLocal")
                when (state) {
                    MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED -> playerMusicOpenDurationLiveData.postValue(
                        ktvApiProtocol.getMediaPlayer().duration
                    )

                    MediaPlayerState.PLAYER_STATE_PLAYING -> {
                        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PLAYING)
                        runOnMainThread {
                            mSetting?.let { setting ->
                                // 若身份是主唱和伴唱，在演唱时，人声音量、伴泰音量保持原先设置，远端音量自动切为30
                                setting.mRemoteVolume = MusicSettingBean.DEFAULT_REMOTE_SINGER_VOL
                                //主唱/合唱 开始唱歌: 默认关闭 aiaec
                                setting.mAIAECEnable = false
                            }
                        }
                    }

                    MediaPlayerState.PLAYER_STATE_PAUSED -> {
                        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PAUSE)
                        // 若身份是主唱和伴唱，演唱暂停/切歌，人声音量、伴奏音量保持原先设置，远端音量自动转为100
                        runOnMainThread {
                            mSetting?.mRemoteVolume = MusicSettingBean.DEFAULT_REMOTE_VOL
                        }
                    }

                    MediaPlayerState.PLAYER_STATE_STOPPED -> {
                        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_STOP)
                        runOnMainThread {
                            mSetting?.let { setting ->
                                // 若身份是主唱和伴唱，演唱暂停/切歌，人声音量、伴奏音量保持原先设置，远端音量自动转为100
                                setting.mRemoteVolume = MusicSettingBean.DEFAULT_REMOTE_VOL
                                // 主唱/合唱 歌曲结束/退出合唱: 默认开启 aiaec, 强度为1
                                setting.mAIAECEnable = true
                                setting.mAIAECStrength = MusicSettingBean.DEFAULT_AIAEC_STRENGTH
                            }
                        }
                    }

                    MediaPlayerState.PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED -> if (isLocal) {
                        playerMusicPlayCompleteLiveData.postValue(ScoringAverageModel(true, 0))
                        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_LRC_RESET)
                    }

                    else -> {}
                }
            }
        }
        )
        if (isRoomOwner) {
            ktvApiProtocol.muteMic(false)
            isOnSeat = true
        }

        // ------------------ 加入频道 ------------------
        mRtcEngine?.apply {
            setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            enableVideo()
            enableLocalVideo(false)
            enableAudio()
            setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO, Constants.AUDIO_SCENARIO_GAME_STREAMING)
            enableAudioVolumeIndication(50, 10, true)
            setClientRole(if (isOnSeat) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE)
            setAudioEffectPreset(mSetting?.mAudioEffect ?: Constants.ROOM_ACOUSTICS_KTV)
            val ret = joinChannel(roomModel.rtcToken, roomModel.roomId, null, mUser.id.toInt())
            if (ret != Constants.ERR_OK) {
                KTVLogger.e(TAG, "joinRTC() called error: $ret")
            }
        }
        // ------------------ 开启鉴黄服务 ------------------
        val contentInspectConfig = ContentInspectConfig()
        try {
            val jsonObject = JSONObject()
            jsonObject.put("sceneName", "ktv")
            jsonObject.put("id", mUser.id.toString())
            jsonObject.put("userNo", mUser.userNo)
            contentInspectConfig.extraInfo = jsonObject.toString()
            val module = ContentInspectModule()
            module.interval = 30
            module.type = ContentInspectConfig.CONTENT_INSPECT_TYPE_MODERATION
            contentInspectConfig.modules = arrayOf(module)
            contentInspectConfig.moduleCount = 1
            mRtcEngine?.enableContentInspect(true, contentInspectConfig)
        } catch (e: JSONException) {
            KTVLogger.e(TAG, e.toString())
        }

        // ------------------ 开启语音鉴定服务 ------------------
        moderationAudio(roomModel.roomId, mUser.id, AudioModeration.AgoraChannelType.rtc, "ktv")

        // 外部使用的StreamId
        if (streamId == 0) {
            val cfg = DataStreamConfig()
            cfg.syncWithAudio = false
            cfg.ordered = false
            streamId = mRtcEngine?.createDataStream(cfg) ?: 0
        }
    }

    // ======================= settings =======================
    private var micOldVolume = 100
    private fun setMusicVolume(v: Int) {
        ktvApiProtocol.getMediaPlayer().adjustPlayoutVolume(v)
        ktvApiProtocol.getMediaPlayer().adjustPublishSignalVolume(v)
    }

    private fun setMicVolume(v: Int) {
        val userInfo = getUserInfo(mUser.id.toString())
        val isAudioMuted = userInfo.muteAudio
        if (isAudioMuted) {
            KTVLogger.d(TAG, "muted! setMicVolume: $v")
            micOldVolume = v
            return
        }
        KTVLogger.d(TAG, "unmute! setMicVolume: $v")
        mRtcEngine?.adjustRecordingSignalVolume(v)
    }

    /**
     * Music toggle original.
     *
     * @param audioTrack the audio track
     */
    fun musicToggleOriginal(audioTrack: LrcControlView.AudioTrack) {
        KTVLogger.d("musicToggleOriginal called, ", "aim: $audioTrack")
        ktvApiProtocol.switchAudioTrack(getAudioTrackMode(audioTrack))
    }

    /**
     * Get audio track mode
     *
     * @param audioTrack
     * @return
     */
    private fun getAudioTrackMode(audioTrack: LrcControlView.AudioTrack): AudioTrackMode {
        return when (audioTrack) {
            LrcControlView.AudioTrack.Acc -> AudioTrackMode.BAN_ZOU
            LrcControlView.AudioTrack.DaoChang -> AudioTrackMode.DAO_CHANG
            else -> AudioTrackMode.YUAN_CHANG
        }
    }

    /**
     * Music toggle start.
     */
    fun musicToggleStart() {
        if (playerMusicStatusLiveData.getValue() == PlayerMusicStatus.ON_PLAYING) {
            ktvApiProtocol.pauseSing()
        } else if (playerMusicStatusLiveData.getValue() == PlayerMusicStatus.ON_PAUSE) {
            ktvApiProtocol.resumeSing()
        }
    }

    /**
     * Render local camera video.
     *
     * @param surfaceView the surface view
     */
    fun renderLocalCameraVideo(surfaceView: SurfaceView?) {
        mRtcEngine?.startPreview()
        mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, 0))
    }

    /**
     * Render remote camera video.
     *
     * @param surfaceView the surface view
     * @param uid         the uid
     */
    fun renderRemoteCameraVideo(surfaceView: SurfaceView?, uid: Int) {
        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid))
    }

    /**
     * Reset music status.
     */
    fun resetMusicStatus() {
        KTVLogger.d(TAG, "RoomLivingViewModel.resetMusicStatus() called")
        chorusNum = 0
        retryTimes = 0
        joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_IDLE)
        ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null)
    }

    private var retryTimes = 0


    /**
     * Music start play.
     *
     * @param music the music
     */
    fun musicStartPlay(music: RoomSongInfo) {
        KTVLogger.d(TAG, "RoomLivingViewModel.musicStartPlay() called")
        if (music.userNo == null) return
        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PREPARE)
        val isOwnSong = music.userNo == mUser.id.toString()
        val songCode = music.songNo.toLong()
        val mainSingerUid = music.userNo.toInt()
        if (isOwnSong) {
            // 主唱加载歌曲
            loadMusic(
                KTVLoadMusicConfiguration(
                    music.songNo, mainSingerUid,
                    KTVLoadMusicMode.LOAD_MUSIC_AND_LRC, false
                ), songCode, true
            )
        } else {
            val isChorus = true
            // TODO:   isChorus = seatLocalValue?.chorusSongCode == music.songNo + music.createAt
            if (isChorus) {
                // 合唱者
                loadMusic(
                    KTVLoadMusicConfiguration(
                        music.songNo, mainSingerUid,
                        KTVLoadMusicMode.LOAD_LRC_ONLY, false
                    ), songCode, false
                )
                // 加入合唱
                innerJoinChorus(music.songNo)
            } else {
                // 观众
                loadMusic(
                    KTVLoadMusicConfiguration(
                        music.songNo, mainSingerUid,
                        KTVLoadMusicMode.LOAD_LRC_ONLY, false
                    ), songCode, false
                )
            }
        }

        // 标记歌曲为播放中
        ktvServiceProtocol.makeSongDidPlay(music.songNo) { e: Exception? ->
            e?.message?.let { error ->
                CustomToast.show(error, Toast.LENGTH_SHORT)
            }
        }
    }

    private fun loadMusic(config: KTVLoadMusicConfiguration, songCode: Long, isOwnSong: Boolean) {
        ktvApiProtocol.loadMusic(songCode, config, object : IMusicLoadStateListener {
            override fun onMusicLoadProgress(
                songCode: Long,
                percent: Int,
                status: MusicLoadStatus,
                msg: String?,
                lyricUrl: String?
            ) {
                KTVLogger.d(TAG, "onMusicLoadProgress, songCode: $songCode percent: $percent lyricUrl: $lyricUrl")
                loadMusicProgressLiveData.postValue(percent)
            }

            override fun onMusicLoadSuccess(songCode: Long, lyricUrl: String) {
                // 当前已被切歌
                if (songPlayingLiveData.getValue() == null) {
                    CustomToast.show(R.string.ktv_load_failed_no_song, Toast.LENGTH_LONG)
                    return
                }
                if (isOwnSong) {
                    // 需要判断此时是否有合唱者，如果有需要切换成LeaderSinger身份
                    if (chorusNum == 0) {
                        ktvApiProtocol.switchSingerRole(KTVSingRole.SoloSinger, null)
                    } else if (chorusNum > 0) {
                        ktvApiProtocol.switchSingerRole(KTVSingRole.LeadSinger, null)
                    }
                    ktvApiProtocol.startSing(songCode, 0)
                }

                // 重置settings
                retryTimes = 0
                playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PLAYING)
            }

            override fun onMusicLoadFail(songCode: Long, reason: KTVLoadMusicFailReason) {
                // 当前已被切歌
                if (songPlayingLiveData.getValue() == null) {
                    CustomToast.show(R.string.ktv_load_failed_no_song, Toast.LENGTH_LONG)
                    return
                }
                KTVLogger.e(TAG, "onMusicLoadFail， reason: $reason")
                if (reason == KTVLoadMusicFailReason.NO_LYRIC_URL) { // 未获取到歌词 正常播放
                    retryTimes = 0
                    playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PLAYING)
                    noLrcLiveData.postValue(true)
                } else if (reason == KTVLoadMusicFailReason.MUSIC_PRELOAD_FAIL) { // 歌曲加载失败 ，重试3次
                    CustomToast.show(R.string.ktv_load_failed, Toast.LENGTH_LONG)
                    retryTimes += 1
                    if (retryTimes < 3) {
                        loadMusic(config, songCode, isOwnSong)
                    } else {
                        playerMusicStatusLiveData.postValue(PlayerMusicStatus.ON_PLAYING)
                        CustomToast.show(R.string.ktv_try, Toast.LENGTH_LONG)
                    }
                } else if (reason == KTVLoadMusicFailReason.CANCELED) { // 当前已被切歌
                    CustomToast.show(R.string.ktv_load_failed_another_song, Toast.LENGTH_LONG)
                }
            }
        })
    }

    /**
     * Re get lrc url.
     */
    fun reGetLrcUrl() {
        val songPlayingValue = songPlayingLiveData.getValue() ?: return
        val isOwnSong = songPlayingValue.userNo == mUser.id.toString()
        loadMusic(
            KTVLoadMusicConfiguration(
                songPlayingValue.songNo,
                songPlayingValue.userNo?.toIntOrNull() ?: -1,
                KTVLoadMusicMode.LOAD_LRC_ONLY,
                false
            ), songPlayingValue.songNo.toLongOrNull() ?: -1, isOwnSong
        )
    }

    /**
     * Music seek.
     *
     * @param time the time
     */
    fun musicSeek(time: Long) {
        ktvApiProtocol.seekSing(time)
    }

    /**
     * Song duration
     */
    val songDuration: Long
        get() = ktvApiProtocol.getMediaPlayer().duration

    /**
     * Music stop.
     */
    fun musicStop() {
        KTVLogger.d(TAG, "RoomLivingViewModel.musicStop() called")
        // 列表中无歌曲， 还原状态
        resetMusicStatus()
    }

    /**
     * On start.
     */
    fun onStart() {
        if (isBackPlay) {
            ktvApiProtocol.getMediaPlayer().mute(false)
        }
    }

    /**
     * On stop.
     */
    fun onStop() {
        if (isBackPlay) {
            ktvApiProtocol.getMediaPlayer().mute(true)
        }
    }

    /**
     * Sync single line score.
     *
     * @param score           the score
     * @param cumulativeScore the cumulative score
     * @param index           the index
     * @param total           the total
     */
    // ------------------ 歌词组件相关 ------------------
    fun syncSingleLineScore(score: Int, cumulativeScore: Int, index: Int, total: Int) {
        val rtcEngine = mRtcEngine ?: return
        val msg: MutableMap<String?, Any?> = HashMap()
        msg["cmd"] = "singleLineScore"
        msg["score"] = score
        msg["index"] = index
        msg["cumulativeScore"] = cumulativeScore
        msg["total"] = total
        val jsonMsg = JSONObject(msg)
        val ret = rtcEngine.sendStreamMessage(streamId, jsonMsg.toString().toByteArray())
        if (ret < 0) {
            KTVLogger.e(TAG, "syncSingleLineScore() sendStreamMessage called returned: $ret")
        }
    }

    /**
     * Sync singing average score.
     *
     * @param score the score
     */
    fun syncSingingAverageScore(score: Double) {
        val rtcEngine = mRtcEngine ?: return
        val msg: MutableMap<String?, Any?> = HashMap()
        msg["cmd"] = "SingingScore"
        msg["score"] = score
        val jsonMsg = JSONObject(msg)
        val ret = rtcEngine.sendStreamMessage(streamId, jsonMsg.toString().toByteArray())
        if (ret < 0) {
            KTVLogger.e(TAG, "syncSingingAverageScore() sendStreamMessage called returned: $ret")
        }
    }
}
