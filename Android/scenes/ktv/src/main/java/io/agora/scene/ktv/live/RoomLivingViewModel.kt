package io.agora.scene.ktv.live

import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.SurfaceView
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.agora.mediaplayer.Constants.MediaPlayerReason
import io.agora.mediaplayer.Constants.MediaPlayerState
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.DataStreamConfig
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcConnection.CONNECTION_STATE_TYPE
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.RtcEngineEx
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserThumbnailInfo
import io.agora.scene.base.AgoraScenes
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.manager.SSOUserManager
import io.agora.scene.base.utils.DownloadManager
import io.agora.scene.base.utils.reportRoom
import io.agora.scene.ktv.KTVLogger
import io.agora.scene.ktv.KtvCenter
import io.agora.scene.ktv.KtvCenter.rtcChorusChannelName
import io.agora.scene.ktv.R
import io.agora.scene.ktv.debugSettings.KTVDebugSettingBean
import io.agora.scene.ktv.debugSettings.KTVDebugSettingsDialog
import io.agora.scene.ktv.ktvapi.AudioTrackMode
import io.agora.scene.ktv.ktvapi.IKTVApiEventHandler
import io.agora.scene.ktv.ktvapi.ILrcView
import io.agora.scene.ktv.ktvapi.ISwitchRoleStateListener
import io.agora.scene.ktv.ktvapi.KTVApi
import io.agora.scene.ktv.ktvapi.KTVApiConfig
import io.agora.scene.ktv.ktvapi.KTVLoadMusicConfiguration
import io.agora.scene.ktv.ktvapi.KTVLoadMusicMode
import io.agora.scene.ktv.ktvapi.KTVMusicType
import io.agora.scene.ktv.ktvapi.KTVSingRole
import io.agora.scene.ktv.ktvapi.KTVType
import io.agora.scene.ktv.ktvapi.MusicLoadStatus
import io.agora.scene.ktv.ktvapi.SwitchRoleFailReason
import io.agora.scene.ktv.ktvapi.createKTVApi
import io.agora.scene.ktv.live.bean.JoinChorusStatus
import io.agora.scene.ktv.live.bean.LineScore
import io.agora.scene.ktv.live.bean.MusicSettingBean
import io.agora.scene.ktv.live.bean.NetWorkEvent
import io.agora.scene.ktv.live.bean.PlayerMusicStatus
import io.agora.scene.ktv.live.bean.ScoringAlgoControlModel
import io.agora.scene.ktv.live.bean.ScoringAverageModel
import io.agora.scene.ktv.live.bean.SoundCardSettingBean
import io.agora.scene.ktv.live.bean.VolumeModel
import io.agora.scene.ktv.live.fragmentdialog.MusicSettingCallback
import io.agora.scene.ktv.live.listener.SongLoadFailReason
import io.agora.scene.ktv.live.listener.SongLoadStateListener
import io.agora.scene.ktv.service.ChooseSongInputModel
import io.agora.scene.ktv.service.KTVServiceProtocol.Companion.getImplInstance
import io.agora.scene.ktv.service.KtvServiceListenerProtocol
import io.agora.scene.ktv.service.PlayStatus
import io.agora.scene.ktv.service.RoomChoristerInfo
import io.agora.scene.ktv.service.RoomMicSeatInfo
import io.agora.scene.ktv.service.ChosenSongInfo
import io.agora.scene.ktv.service.api.KtvApiManager
import io.agora.scene.ktv.service.api.KtvSongApiModel
import io.agora.scene.ktv.widget.lrcView.LrcControlView
import io.agora.scene.ktv.widget.song.SongItem
import io.agora.scene.widget.toast.CustomToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The type Room living view model.
 */
class RoomLivingViewModel constructor(val mRoomInfo: AUIRoomInfo) : ViewModel() {
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

    private val ktvApiManager = KtvApiManager()

    // room destroy
    val roomDestroyLiveData = MutableLiveData<Boolean>()

    // room expire
    val roomExpireLiveData = MutableLiveData<Boolean>()

    // room user count
    val userCountLiveData = MutableLiveData<Int>()

    // Seat list
    val seatListLiveData = MutableLiveData<MutableList<RoomMicSeatInfo>>()

    // seat update
    val seatUpdateLiveData = MutableLiveData<RoomMicSeatInfo>()

    // current local seat info
    val localSeatInfo: RoomMicSeatInfo? get() = seatListLiveData.value?.firstOrNull { it.owner?.userId == KtvCenter.mUser.id.toString() }

    // Selected Song List
    val chosenSongListLiveData = MutableLiveData<List<ChosenSongInfo>?>()

    // Chorus List
    private val chorusInfoList = mutableListOf<RoomChoristerInfo>()

    // Get Chorus Users
    fun getSongChorusInfo(userId: String, songCode: String): RoomChoristerInfo? {
        return chorusInfoList.firstOrNull { it.userId == userId && it.chorusSongNo == songCode }
    }

    val volumeLiveData = MutableLiveData<VolumeModel>()

    // current playing song
    val songPlayingLiveData = MutableLiveData<ChosenSongInfo?>()

    // main singer score
    val mainSingerScoreLiveData = MutableLiveData<LineScore>()

    // rtc stream id
    var streamId = 0

    // music play status
    val playerMusicStatusLiveData = MutableLiveData<PlayerMusicStatus>()

    // laoding music status
    val loadMusicProgressLiveData = MutableLiveData<Int>()

    // current chorus status
    val joinchorusStatusLiveData = MutableLiveData<JoinChorusStatus>()

    // no lrc
    val noLrcLiveData = MutableLiveData<Boolean>()

    val playerMusicOpenDurationLiveData = MutableLiveData<Long>()

    val playerMusicPlayCompleteLiveData = MutableLiveData<ScoringAverageModel>()

    val networkStatusLiveData = MutableLiveData<NetWorkEvent>()

    val scoringAlgoControlLiveData = MutableLiveData<ScoringAlgoControlModel>()

    val scoringAlgoLiveData = MutableLiveData<Int>()

    // rtc engine
    private var mRtcEngine: RtcEngineEx? = null

    // Main version audio settings
    private val mainChannelMediaOption = ChannelMediaOptions()

    // music settings
    var mMusicSetting: MusicSettingBean? = null

    // debug settings
    var mDebugSetting: KTVDebugSettingBean? = null

    // effects settings
    var mSoundCardSettingBean: SoundCardSettingBean? = null

    val isRoomOwner: Boolean get() = mRoomInfo.roomOwner?.userId == KtvCenter.mUser.id.toString()

    /**
     * Init.
     */
    fun initData() {
        initSettings()
        initRTCPlayer()
        initRoom()
    }

    // after init rtcPlayer
    fun reportEnterRoom(){
        mRtcEngine?.reportRoom(SSOUserManager.getUser().accountUid, AgoraScenes.KTV)
        KTVLogger.d("reportRoom","reportEnterRoom")
    }

    /**
     * Release boolean.
     *
     * @return the boolean
     */
    private fun innerRelease(): Boolean {
        ktvServiceProtocol.unsubscribeListener(serviceListenerProtocol)
        KTVLogger.d(TAG, "release called")
        streamId = 0
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

    private val serviceListenerProtocol = object : KtvServiceListenerProtocol {

        private fun updateLocalEnterSeat(seatInfo: RoomMicSeatInfo) {
            if (seatInfo.owner?.userId == KtvCenter.mUser.id.toString()) {
                mRtcEngine?.let {
                    mainChannelMediaOption.publishCameraTrack = false
                    mainChannelMediaOption.publishMicrophoneTrack = !seatInfo.isAudioMuted
                    mainChannelMediaOption.enableAudioRecordingOrPlayout = true
                    mainChannelMediaOption.autoSubscribeVideo = true
                    mainChannelMediaOption.autoSubscribeAudio = true
                    mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    it.updateChannelMediaOptions(mainChannelMediaOption)
                }
            }
        }

        override fun onRoomDestroy() {
            innerRelease()
            roomDestroyLiveData.value = true
        }

        override fun onRoomExpire() {
            innerRelease()
            roomExpireLiveData.value = true
        }


        override fun onUserCountUpdate(userCount: Int) {
            userCountLiveData.value = userCount
        }

        override fun onMicSeatSnapshot(seatMap: Map<Int, RoomMicSeatInfo>) {
            val seatList = mutableListOf<RoomMicSeatInfo>()
            seatMap.values.forEach { roomMicSeatInfo ->
                seatList.add(roomMicSeatInfo)
            }
            seatList.sortBy { it.seatIndex }
            seatListLiveData.value = seatList
            // Fix for ENT-1826:
            // When the lead singer terminates the process and re-enters the room,
            // ensure the RTC role is set correctly to allow for proper lyric synchronization.
            // This prevents issues with syncing lyrics due to role discrepancies.
            seatList.firstOrNull { it.owner?.userId == KtvCenter.mUser.id.toString() }?.let { originSeat ->
                updateLocalEnterSeat(originSeat)
            }
        }

        override fun onUserSeatUpdate(seatInfo: RoomMicSeatInfo) {
            seatUpdateLiveData.value = seatInfo
        }

        override fun onUserEnterSeat(seatIndex: Int, user: AUIUserThumbnailInfo) {
            val originSeat = seatListLiveData.value?.firstOrNull { it.seatIndex == seatIndex } ?: return
            originSeat.owner = user
            seatListLiveData.value?.set(seatIndex, originSeat)
            if (user.userId == KtvCenter.mUser.id.toString()) {
                updateLocalEnterSeat(originSeat)
            }
        }

        override fun onUserLeaveSeat(seatIndex: Int, user: AUIUserThumbnailInfo) {
            val originSeat = seatListLiveData.value?.firstOrNull { it.seatIndex == seatIndex } ?: return
            originSeat.owner = user
            originSeat.owner = AUIUserThumbnailInfo()
            seatListLiveData.value?.set(seatIndex, originSeat)
            if (user.userId == KtvCenter.mUser.id.toString()) {
                mRtcEngine?.let {
                    mainChannelMediaOption.publishCameraTrack = false
                    mainChannelMediaOption.publishMicrophoneTrack = false
                    mainChannelMediaOption.enableAudioRecordingOrPlayout = true
                    mainChannelMediaOption.autoSubscribeVideo = true
                    mainChannelMediaOption.autoSubscribeAudio = true
                    mainChannelMediaOption.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                    it.updateChannelMediaOptions(mainChannelMediaOption)
                }
                ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null)
            }
        }

        override fun onSeatAudioMute(seatIndex: Int, isMute: Boolean) {
            val originSeat = seatListLiveData.value?.firstOrNull { it.seatIndex == seatIndex } ?: return
            originSeat.isAudioMuted = isMute
            seatListLiveData.value?.set(seatIndex, originSeat)
            if (originSeat.owner?.userId == KtvCenter.mUser.id.toString()) {// toggle mirc
                toggleSelfAudioBySign(!isMute)
            }
        }

        override fun onSeatVideoMute(seatIndex: Int, isMute: Boolean) {
            val originSeat = seatListLiveData.value?.firstOrNull { it.seatIndex == seatIndex } ?: return
            originSeat.isVideoMuted = isMute
            seatListLiveData.value?.set(seatIndex, originSeat)
            if (originSeat.owner?.userId == KtvCenter.mUser.id.toString()) {// toggle video
                toggleSelfVideoBySign(!isMute)
            }
        }

        override fun onChoristerDidEnter(chorister: RoomChoristerInfo) {
            val lastChorusNum = chorusInfoList.size
            chorusInfoList.removeIf { it.userId == chorister.userId }
            chorusInfoList.add(chorister)
            soloSingerJoinChorusMode(lastChorusNum)

            val originSeat = seatListLiveData.value?.firstOrNull { it.owner?.userId == chorister.userId }
            if (originSeat != null) {
                onUserSeatUpdate(originSeat)
            }
            val songPlaying = songPlayingLiveData.getValue() ?: return
            if (chorister.userId == KtvCenter.mUser.id.toString() && songPlaying.songNo == chorister.chorusSongNo) {
                // Fix for ENT-2031:
                // When clicking to join the chorus:
                // 1. Load the song.
                // 2. Call `rtm joinChorus`.
                // 3. Switch the KTV API role to "chorus".
                //
                // Note: When an audience member joins the chorus, the sequence of publishing RTM messages and handling `onMetaData` may not be guaranteed.
                // Thus, after successfully joining the chorus, it's necessary to update the role to "chorus".
                innerRtmOnSelfJoinedChorus()
            }
        }

        override fun onChoristerDidLeave(chorister: RoomChoristerInfo) {
            val lastChorusNum = chorusInfoList.size
            chorusInfoList.removeIf { it.userId == chorister.userId }
            soloSingerJoinChorusMode(lastChorusNum)

            val originSeat = seatListLiveData.value?.firstOrNull { it.owner?.userId == chorister.userId }
            if (originSeat != null) {
                onUserSeatUpdate(originSeat)
            }
            val songPlaying = songPlayingLiveData.getValue() ?: return
            if (chorister.userId == KtvCenter.mUser.id.toString() && songPlaying.songNo == chorister.chorusSongNo) {
                joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_LEAVE_CHORUS)
            }
        }

        override fun onChosenSongListDidChanged(chosenSongList: List<ChosenSongInfo>) {
            songPlayingLiveData.value?.let { currentSong ->
                val firstSong = chosenSongList.firstOrNull()
                if (/*currentSong.owner?.userId == KtvCenter.mUser.id.toString() && */firstSong?.songNo != currentSong.songNo) {
                    if (loadingMusic.get()) {
                        KTVLogger.d(TAG, "RoomLivingViewModel remove music: ${currentSong.songNo}")
                        getRestfulSongList {
                            songList.firstOrNull { it.songCode == currentSong.songNo }?.let { song ->
                                DownloadManager.instance.cancelDownload(song.music)
                                loadingMusic.set(false)
                            }
                        }
                    }
                }
            }
            chosenSongListLiveData.value = chosenSongList
            onUpdateAllChooseSongs(chosenSongList)
        }
    }

    private fun initRoom() {
        ktvServiceProtocol.subscribeListener(serviceListenerProtocol)
        ktvServiceProtocol.getChosenSongList { error, songList ->
            if (error == null) {
                chosenSongListLiveData.value = songList
            }
        }
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
            } else { // failure
                KTVLogger.e(TAG, "RoomLivingViewModel.exitRoom() failed: $e")
                e.message?.let { error ->
                    CustomToast.show(error, Toast.LENGTH_SHORT)
                }
            }
        }
        innerRelease()
    }

    /**
     * enter seat
     *
     * @param onSeatIndex the on seat index
     */
    fun enterSeat(onSeatIndex: Int) {
        KTVLogger.d(TAG, "RoomLivingViewModel.haveSeat() called: $onSeatIndex")
        ktvServiceProtocol.enterSeat(onSeatIndex) { error ->
            if (error != null) { // failure
                CustomToast.show(R.string.ktv_enter_seat_failed, error.message ?: "")
            }
        }
    }

    /**
     * leave seat
     *
     * @param seatModel the seat model
     */
    fun leaveSeat(seatModel: RoomMicSeatInfo) {
        KTVLogger.d(TAG, "RoomLivingViewModel.leaveSeat() called")
        ktvServiceProtocol.leaveSeat() { error ->
            if (error != null) { // failure
                CustomToast.show(R.string.ktv_leave_seat_failed, error.message ?: "")
            }
        }
    }

    /**
     * Kick seat
     *
     * @param seatModel
     */
    fun kickSeat(seatModel: RoomMicSeatInfo) {
        KTVLogger.d(TAG, "RoomLivingViewModel.kickSeat() called")
        ktvServiceProtocol.kickSeat(seatModel.seatIndex) { error ->
            if (error != null) { // failure
                CustomToast.show(R.string.ktv_kick_seat_failed, error.message ?: "")
            }
        }
    }

    /**
     * Is camera opened
     */
    private var isCameraOpened = false

    /**
     * Update seat video mute status
     *
     * @param mute
     */
    fun updateSeatVideoMuteStatus(mute: Boolean, completion: (error: Exception?) -> Unit) {
        KTVLogger.d(TAG, "RoomLivingViewModel.updateSeatVideoMuteStatus() called mute：$mute")
        ktvServiceProtocol.updateSeatVideoMuteStatus(mute) { error ->
            if (error != null) { // failure
                error.message?.let {
                    CustomToast.show(it, Toast.LENGTH_SHORT)
                }
            }
            completion.invoke(error)
        }
    }

    // Toggle the camera based on signaling.
    private fun toggleSelfVideoBySign(isOpen: Boolean) {
        KTVLogger.d(TAG, "RoomLivingViewModel.toggleSelfVideoBySign() isOpen:$isOpen")
        isCameraOpened = isOpen
        mRtcEngine?.enableLocalVideo(isOpen)
        val channelMediaOption = ChannelMediaOptions()
        channelMediaOption.publishCameraTrack = isOpen
        mRtcEngine?.updateChannelMediaOptions(channelMediaOption)
    }

    // Toggle the microphone based on signaling.
    private fun toggleSelfAudioBySign(isOpen: Boolean) {
        KTVLogger.d(TAG, "RoomLivingViewModel.toggleSelfAudioBySign() isOpen:$isOpen")
        ktvApiProtocol.muteMic(!isOpen)
        if (!isOpen && mMusicSetting?.mEarBackEnable == true) {
            mRtcEngine?.enableInEarMonitoring(false, Constants.EAR_MONITORING_FILTER_NONE)
        } else if (isOpen && mMusicSetting?.mEarBackEnable == true) {
            mRtcEngine?.enableInEarMonitoring(true, Constants.EAR_MONITORING_FILTER_NONE)
        }
        if (isOpen) {
            KTVLogger.d(TAG, "RoomLivingViewModel unmute! setMicVolume: $micOldVolume")
            mRtcEngine?.adjustRecordingSignalVolume(micOldVolume)
        }
    }

    /**
     * Update seat audio mute status
     *
     * @param mute
     */
    fun updateSeatAudioMuteStatus(mute: Boolean, completion: (error: Exception?) -> Unit) {
        KTVLogger.d(TAG, "RoomLivingViewModel.updateSeatAudioMuteStatus() called mute：$mute")
        ktvServiceProtocol.updateSeatAudioMuteStatus(mute) { error ->
            if (error != null) { // failure
                error.message?.let {
                    CustomToast.show(it, Toast.LENGTH_SHORT)
                }
            }
            completion.invoke(error)
        }
    }

    /**
     * Song chosen list
     */
    fun getSongChosenList() {
        KTVLogger.d(TAG, "RoomLivingViewModel.getSongChosenList() called")
        ktvServiceProtocol.getChosenSongList { error, chosenSongList ->
            if (error == null) { // success
                chosenSongListLiveData.value = chosenSongList
            }
        }
    }

    private fun onUpdateAllChooseSongs(songList: List<ChosenSongInfo>?) {
        if (!songList.isNullOrEmpty()) {
            val value = songPlayingLiveData.getValue()
            val songPlaying = songList[0]
            if (value == null) {
                // If there are no selected songs, set the first item in the list as the currently playing song.
                KTVLogger.d(TAG, "RoomLivingViewModel.onUpdateAllChooseSongs() chosen song list is empty")
                songPlayingLiveData.postValue(songPlaying)
            } else if (value.songNo != songPlaying.songNo) {
                // There are currently selected songs, and the updated song is different from the previous one.
                KTVLogger.d(TAG, "RoomLivingViewModel.onUpdateAllChooseSongs() single or first chorus")
                songPlayingLiveData.postValue(songPlaying)
            }
        } else {
            KTVLogger.d(TAG, "RoomLivingViewModel.onSongChanged() return is emptyList")
            songPlayingLiveData.postValue(null)
        }
    }

    private val songList = mutableListOf<KtvSongApiModel>()

    private fun getRestfulSongList(completion: (error: Exception?) -> Unit) {
        if (songList.isNotEmpty()) {
            completion.invoke(null)
            return
        }
        ktvApiManager.getSongList { error, musicList ->
            KTVLogger.d(TAG, "RoomLivingViewModel.getSongList() return error:$error")
            if (error != null) {
                CustomToast.show(R.string.ktv_get_songs_failed, error.message ?: "")
                completion.invoke(error)
            } else {
                songList.apply {
                    clear()
                    addAll(musicList.map { musicItem ->
                        musicItem.copy(
                            music = musicItem.music.replace("cn-beijing", "accelerate-overseas"),
                            lyric = musicItem.lyric.replace("cn-beijing", "accelerate-overseas")
                        )
                    })
                }
                completion.invoke(null)
            }
        }
    }

    fun getSongList(): LiveData<List<ChosenSongInfo>> {
        KTVLogger.d(TAG, "RoomLivingViewModel.getSongList() called")
        val liveData = MutableLiveData<List<ChosenSongInfo>>()
        ktvApiManager.getSongList { error, musicList ->
            KTVLogger.d(TAG, "RoomLivingViewModel.getSongList() return error:$error")
            val songs: MutableList<ChosenSongInfo> = ArrayList()
            if (error != null) {
                CustomToast.show(R.string.ktv_get_songs_failed, error.message ?: "")
            } else {
                if (musicList.isNotEmpty()) {
                    songList.apply {
                        clear()
                        addAll(musicList.map { musicItem ->
                            musicItem.copy(
                                music = musicItem.music.replace("cn-beijing", "accelerate-overseas"),
                                lyric = musicItem.lyric.replace("cn-beijing", "accelerate-overseas")
                            )
                        })
                    }
                }
            }
            // Need to call another interface to get the currently selected song list to supplement the list information. >_<
            ktvServiceProtocol.getChosenSongList { e: Exception?, songsChosen: List<ChosenSongInfo>? ->
                if (e == null && songsChosen != null) { // success
                    for (music in musicList) {
                        var songItem = songsChosen.firstOrNull { it.songNo == music.songCode }
                        if (songItem == null) {
                            songItem = ChosenSongInfo(
                                songName = music.name,
                                songNo = music.songCode,
                                singer = music.singer,
                                imageUrl = "",
                                owner = AUIUserThumbnailInfo(),
                                status = PlayStatus.idle,
                            )
                        }
                        songs.add(songItem)
                    }
                    liveData.postValue(songs)
                } else {
                    liveData.postValue(emptyList())
                }
                return@getChosenSongList
            }
        }
        return liveData
    }

    /**
     * choose song
     *
     * @param songItem the song model
     * @param isChorus  the is chorus
     * @return the live data
     */
    fun chooseSong(songItem: SongItem, isChorus: Boolean): LiveData<Boolean> {
        KTVLogger.d(TAG, "RoomLivingViewModel.chooseSong() called, name:" + songItem.songName + " isChorus:" + isChorus)
        val liveData = MutableLiveData<Boolean>()
        val chosenSong = ChooseSongInputModel(
            songName = songItem.songName,
            songNo = songItem.songNo,
            singer = songItem.singer,
            imageUrl = songItem.imageUrl,
        )
        ktvServiceProtocol.chooseSong(chosenSong) { error ->
            if (error != null) { // failure
                CustomToast.show(R.string.ktv_choose_song_failed, error.message ?: "")
            }
            liveData.postValue(error == null)
        }
        return liveData
    }

    /**
     * delete song
     *
     * @param songModel the song model
     */
    fun deleteSong(songModel: ChosenSongInfo) {
        KTVLogger.d(TAG, "RoomLivingViewModel.deleteSong() called, name:" + songModel.songName)
        ktvServiceProtocol.removeSong(songModel.songNo) { error ->
            if (error != null) { // failure
                CustomToast.show(R.string.ktv_remove_song_failed, error.message ?: "")
            }
        }
    }

    /**
     * pin song
     *
     * @param songModel the song model
     */
    fun pinSong(songModel: ChosenSongInfo) {
        KTVLogger.d(TAG, "RoomLivingViewModel.topUpSong() called, name:" + songModel.songName)
        ktvServiceProtocol.pinSong(songModel.songNo) { error ->
            if (error != null) { // failure
                CustomToast.show(R.string.ktv_pin_song_failed, error.message ?: "")
            }
        }
    }

    /**
     * click to join the chorus
     */
    fun joinChorus() {
        KTVLogger.d(TAG, "RoomLivingViewModel.joinChorus() viewClick called")
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
        getRestfulSongList {
            if (localSeatInfo == null) { // Not on the seat, automatically enter the seat
                ktvServiceProtocol.enterSeat(null) { err: Exception? ->
                    if (err == null) {
                        innerJoinChorus(musicModel)
                    } else {
                        joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
                    }
                }
            } else { // On the seat, directly join the chorus.
                innerJoinChorus(musicModel)
            }
        }

    }

    /**
     * inner join the chorus
     *
     * @param songCode
     */
    private fun innerJoinChorus(songInfo: ChosenSongInfo) {
        loadingMusic.set(true)
        val config = KTVLoadMusicConfiguration(
            songInfo.songNo,
            songPlayingLiveData.getValue()?.owner?.userId?.toIntOrNull() ?: -1,
            KTVLoadMusicMode.LOAD_MUSIC_ONLY,
            false
        )

        innerLoadMusic(config, songInfo, object : SongLoadStateListener {
            override fun onMusicLoadProgress(
                songCode: String,
                percent: Int,
                status: MusicLoadStatus,
                lyricUrl: String?
            ) {
                loadMusicProgressLiveData.postValue(percent)
            }

            override fun onMusicLoadSuccess(songCode: String, musicUri: String, lyricUrl: String) {
                loadingMusic.set(false)
                KTVLogger.d(TAG, "joinChorus onMusicLoadSuccess,songCode:$songCode,lyricUrl:$lyricUrl")
                ktvApiProtocol.loadMusic(musicUri, config)
                KTVLogger.d(TAG, "RoomLivingViewModel.joinChorus called")
                val songModel = songPlayingLiveData.value ?: run {
                    KTVLogger.d(TAG, "RoomLivingViewModel.joinChorus songPlayingLiveData is null")
                    return
                }
                ktvServiceProtocol.joinChorus(songModel.songNo) { e: Exception? ->
                    if (e == null) {
//                        nothing
                    } else { // failure
                        // Fix the issue where the publish message callback time precedes the rtm onMetaData.
                        if (joinchorusStatusLiveData.value == JoinChorusStatus.ON_JOIN_CHORUS) return@joinChorus
                        joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
                        e.message?.let { error ->
                            CustomToast.show(error, Toast.LENGTH_SHORT)
                        }
                    }
                }
            }

            override fun onMusicLoadFail(songCode: String, reason: SongLoadFailReason) {
                loadingMusic.set(false)
                KTVLogger.e(TAG, "joinChorus onMusicLoadFail,songCode:$songCode,reason:$reason")
                joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
            }
        })
    }

    private fun innerRtmOnSelfJoinedChorus() {
        if (joinchorusStatusLiveData.value == JoinChorusStatus.ON_JOIN_CHORUS) return
        ktvApiProtocol.switchSingerRole(KTVSingRole.CoSinger, object : ISwitchRoleStateListener {
            override fun onSwitchRoleFail(reason: SwitchRoleFailReason) {
                KTVLogger.e(TAG, "RoomLivingViewModel.onSwitchRoleFail(CoSinger) reason:$reason")
                if (reason == SwitchRoleFailReason.JOIN_CHANNEL_FAIL) {
                    joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
                }
            }

            override fun onSwitchRoleSuccess() {
                if (localSeatInfo != null && songPlayingLiveData.value != null) {
                    KTVLogger.d(TAG, "RoomLivingViewModel.onSwitchRoleSuccess(CoSinger)")
                    joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_CHORUS)
                } else {
                    KTVLogger.d(
                        TAG,
                        "RoomLivingViewModel.onSwitchRoleSuccess(CoSinger) but localSeat or songPlayingLiveData is null"
                    )
                    ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null)
                    joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_JOIN_FAILED)
                }
            }
        })
    }

    /**
     * leave the chorus
     */
    fun leaveChorus() {
        KTVLogger.d(TAG, "RoomLivingViewModel.leaveChorus() called")
        val songCode = songPlayingLiveData.value?.songNo
        if (localSeatInfo != null && songCode != null) {
            ktvServiceProtocol.leaveChorus(songCode) { e: Exception? ->
                if (e == null) { // success
                    KTVLogger.d(TAG, "RoomLivingViewModel.leaveChorus.switchSingerRole called KTVSingRole.Audience")
                    ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null)
                    joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_LEAVE_CHORUS)
                } else { // failure
                    e.message?.let { error ->
                        CustomToast.show(error, Toast.LENGTH_SHORT)
                    }
                }
            }
        } else {
            KTVLogger.d(TAG, "RoomLivingViewModel.switchSingerRole called KTVSingRole.Audience")
            ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, null)
            joinchorusStatusLiveData.postValue(JoinChorusStatus.ON_LEAVE_CHORUS)
        }
    }

    /**
     * start change to next song
     */
    fun changeMusic() {
        KTVLogger.d(TAG, "RoomLivingViewModel.changeMusic() called")
        val musicModel = songPlayingLiveData.getValue() ?: run {
            KTVLogger.e(TAG, "RoomLivingViewModel.changeMusic() failed, no song is playing now!")
            return
        }
        //ktvApiProtocol.switchSingerRole(KTVSingRole.Audience, "", null);
        playerMusicStatusLiveData.value = PlayerMusicStatus.ON_CHANGING_START
        ktvServiceProtocol.removeSong(musicModel.songNo) { error ->
            if (error != null) { // failed
                CustomToast.show(R.string.ktv_change_song_failed, error.message ?: "")
            }
            playerMusicStatusLiveData.value = PlayerMusicStatus.ON_CHANGING_END
        }
    }

    private var lrcControlView: WeakReference<ILrcView>? = null

    /**
     * set lrc view
     *
     * @param view the view
     */
    fun setLrcView(view: ILrcView) {
        lrcControlView = WeakReference(view)
        ktvApiProtocol.setLrcView(view)
        mMusicSetting?.let { setting ->
            ktvApiProtocol.enableProfessionalStreamerMode(setting.mProfessionalModeEnable)
        }
    }

    // ======================= Player/RTC/MPK =======================
    // ------------------ Initialize the music playback settings panel. ------------------
    private fun initSettings() {
        // debug settings
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

        // music settings
        mMusicSetting = MusicSettingBean(object : MusicSettingCallback {
            override fun onEarChanged(earBackEnable: Boolean) {
                KTVLogger.d(TAG, "onEarChanged: $earBackEnable")
                val isAudioMuted = localSeatInfo?.isAudioMuted ?: true
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
                // The audio quality options can only take effect when AIAEC is turned off.
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
                    0 -> { // close
                        mRtcEngine?.setParameters("{\"che.audio.ains_mode\": 0}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerBound\": 80}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerMask\": 50}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.statisticalbound\": 5}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.finallowermask\": 30}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.enhfactorstastical\": 200}")
                    }

                    1 -> { // medium
                        mRtcEngine?.setParameters("{\"che.audio.ains_mode\": 2}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerBound\": 80}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.lowerMask\": 50}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.statisticalbound\": 5}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.finallowermask\": 30}")
                        mRtcEngine?.setParameters("{\"che.audio.nsng.enhfactorstastical\": 200}")
                    }

                    2 -> { // high
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
            KTVLogger.d(TAG, "SoundCard parameter preset:$presetValue,gain:$gainValue,gender:$gender,effect:$effect")
            mRtcEngine?.setParameters(
                "{\"che.audio.virtual_soundcard\":{\"preset\":" + presetValue +
                        ",\"gain\":" + gainValue +
                        ",\"gender\":" + gender +
                        ",\"effect\":" + effect + "}}"
            )
        }
    }

    private fun initRTCPlayer() {
        val rtcAppId = KtvCenter.mAppId
        if (TextUtils.isEmpty(rtcAppId)) {
            throw NullPointerException("please check \"gradle.properties\"")
        }
        if (mRtcEngine != null) return
        // ------------------ RTC Initialize------------------
        val config = RtcEngineConfig()
        config.mContext = AgoraApplication.the()
        config.mAppId = rtcAppId
        config.mEventHandler = object : IRtcEngineEventHandler() {
            override fun onNetworkQuality(uid: Int, txQuality: Int, rxQuality: Int) {
                // Network status callback, local user uid = 0
                if (uid == 0) {
                    networkStatusLiveData.postValue(NetWorkEvent(txQuality, rxQuality))
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
                        val lineScore = LineScore(
                            score = score,
                            index = index,
                            cumulativeScore = cumulativeScore,
                            total = total
                        )
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
                mMusicSetting?.let { setting ->
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
                mMusicSetting?.mEarBackDelay = stats.earMonitorDelay
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

        // ------------------ ktv api Initialize ------------------
        KTVApi.debugMode = AgoraApplication.the().isDebugModeOpen
        if (AgoraApplication.the().isDebugModeOpen) {
            KTVLogger.d(TAG, "isDebugModeOpen: true")
            KTVApi.mccDomain = "api-test.agora.io"
        }
        ktvApiProtocol = createKTVApi(
            KTVApiConfig(
                appId = rtcAppId,
                rtmToken = KtvCenter.mRtmToken,
                engine = mRtcEngine!!,
                channelName = mRoomInfo.roomId,
                localUid = KtvCenter.mUser.id.toInt(),
                chorusChannelName = mRoomInfo.rtcChorusChannelName,
                chorusChannelToken = KtvCenter.mRtcToken,
                maxCacheSize = 10,
                type = KTVType.Normal,
                musicType = KTVMusicType.SONG_URL
            )
        )
        ktvApiProtocol.addEventHandler(object : IKTVApiEventHandler() {
            override fun onMusicPlayerStateChanged(
                state: MediaPlayerState, error: MediaPlayerReason, isLocal:
                Boolean
            ) {
                KTVLogger.d(TAG, "onMusicPlayerStateChanged, state:$state error:$error isLocal:$isLocal")
                when (state) {
                    MediaPlayerState.PLAYER_STATE_OPEN_COMPLETED -> playerMusicOpenDurationLiveData.postValue(
                        ktvApiProtocol.getMediaPlayer().duration
                    )

                    MediaPlayerState.PLAYER_STATE_PLAYING -> {
                        runOnMainThread {
                            playerMusicStatusLiveData.value = PlayerMusicStatus.ON_PLAYING
                            mMusicSetting?.let { setting ->
                                // If the role is lead singer or backing singer, when singing is paused or the song is changed,
                                // the volume of the local voice and the accompaniment will remain at their original settings,
                                // while the remote volume will automatically be set to 100
                                setting.mRemoteVolume = MusicSettingBean.DEFAULT_REMOTE_SINGER_VOL
                                //For the leader singer/co=singer starting to sing: AIAEC is turned off by default.
                                setting.mAIAECEnable = false
                            }
                        }
                    }

                    MediaPlayerState.PLAYER_STATE_PAUSED -> {
                        // If the role is lead singer or backing singer, when singing is paused or the song changes,
                        // the local voice volume and accompaniment volume will remain at their original settings,
                        // while the remote volume will automatically be set to 100.
                        runOnMainThread {
                            playerMusicStatusLiveData.value = PlayerMusicStatus.ON_PAUSE
                            mMusicSetting?.mRemoteVolume = MusicSettingBean.DEFAULT_REMOTE_VOL
                        }
                    }

                    MediaPlayerState.PLAYER_STATE_STOPPED -> {
                        runOnMainThread {
                            playerMusicStatusLiveData.value = PlayerMusicStatus.ON_STOP
                            mMusicSetting?.let { setting ->
                                // If the user’s role is either the lead singer or a co singer, when the singing is
                                // paused or the song is switched,
                                // the volume of the user’s voice and the accompaniment will remain at their original settings, while the volume of the remote (i.e., other participants) will be automatically adjusted to 100
                                setting.mRemoteVolume = MusicSettingBean.DEFAULT_REMOTE_VOL
                                // For the lead singer, co singer, when the song ends or exits the chorus:
                                // AIAEC is enabled by default, with a strength of 1.
                                setting.mAIAECEnable = true
                                setting.mAIAECStrength = MusicSettingBean.DEFAULT_AIAEC_STRENGTH
                            }
                        }
                    }

                    MediaPlayerState.PLAYER_STATE_PLAYBACK_ALL_LOOPS_COMPLETED -> if (isLocal) {
                        runOnMainThread {
                            playerMusicPlayCompleteLiveData.value = ScoringAverageModel(true, 0)
                            playerMusicStatusLiveData.value = PlayerMusicStatus.ON_LRC_RESET
                        }
                    }

                    else -> {}
                }
            }

            override fun onTokenPrivilegeWillExpire() {
                super.onTokenPrivilegeWillExpire()
                KTVLogger.d(TAG, "ktvapi onTokenPrivilegeWillExpire")
            }
        }
        )
        if (isRoomOwner) {
            ktvApiProtocol.muteMic(false)
        }

        // ------------------ join channel ------------------
        mRtcEngine?.apply {
            setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
            enableVideo()
            enableLocalVideo(false)
            enableAudio()
            setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO, Constants.AUDIO_SCENARIO_GAME_STREAMING)
            enableAudioVolumeIndication(50, 10, true)
            setClientRole(if (isRoomOwner) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE)
            setAudioEffectPreset(mMusicSetting?.mAudioEffect ?: Constants.ROOM_ACOUSTICS_KTV)
            val ret = joinChannel(KtvCenter.mRtcToken, mRoomInfo.roomId, null, KtvCenter.mUser.id.toInt())
            if (ret != Constants.ERR_OK) {
                KTVLogger.e(TAG, "joinRTC() called error: $ret")
            }
        }

        // StreamId
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
        val isAudioMuted = localSeatInfo?.isAudioMuted ?: true
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
        KTVLogger.d(TAG, "musicToggleOriginal called, aim:$audioTrack")
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
            KTVLogger.d(TAG, "musicToggleStart called, pauseSing")
            ktvApiProtocol.pauseSing()
        } else if (playerMusicStatusLiveData.getValue() == PlayerMusicStatus.ON_PAUSE) {
            KTVLogger.d(TAG, "musicToggleStart called, resumeSing")
            ktvApiProtocol.resumeSing()
        }
    }

    /**
     * Render local camera video.
     *
     * @param surfaceView the surface view
     */
    fun renderLocalCameraVideo(surfaceView: SurfaceView?) {
        KTVLogger.d(TAG, "renderLocalCameraVideo called")
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
        KTVLogger.d(TAG, "RoomLivingViewModel.renderRemoteCameraVideo() called $uid")
        mRtcEngine?.setupRemoteVideo(VideoCanvas(surfaceView, Constants.RENDER_MODE_HIDDEN, uid))
    }

    /**
     * Reset music status.
     */
    fun resetMusicStatus() {
        KTVLogger.d(TAG, "RoomLivingViewModel.resetMusicStatus() called")
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
    fun musicStartPlay(music: ChosenSongInfo) {
        KTVLogger.d(TAG, "RoomLivingViewModel.musicStartPlay() called music:$music")
        if (music.owner?.userId.isNullOrEmpty()) return
        playerMusicStatusLiveData.value = PlayerMusicStatus.ON_PREPARE
        val isOwnSong = music.owner?.userId == KtvCenter.mUser.id.toString()
        val mainSingerUid = music.owner?.userId?.toIntOrNull() ?: -1


        getRestfulSongList {
            if (isOwnSong) {
                // leader signer load song
                loadMusic(
                    KTVLoadMusicConfiguration(
                        music.songNo, mainSingerUid, KTVLoadMusicMode.LOAD_MUSIC_AND_LRC, false
                    ), music, true
                )
            } else {
                val choristerInfo = getSongChorusInfo(KtvCenter.mUser.id.toString(), music.songNo)
                if (choristerInfo != null) {
                    // co-signer
                    loadMusic(
                        KTVLoadMusicConfiguration(
                            music.songNo, mainSingerUid, KTVLoadMusicMode.LOAD_LRC_ONLY, false
                        ), music, false
                    )
                    innerJoinChorus(music)
                } else {
                    // audience
                    loadMusic(
                        KTVLoadMusicConfiguration(
                            music.songNo, mainSingerUid,
                            KTVLoadMusicMode.LOAD_LRC_ONLY, false
                        ), music, false
                    )
                }
            }
        }

        if (music.owner?.userId == KtvCenter.mUser.id.toString()) {
            // Mark the song as playing.
            ktvServiceProtocol.makeSongDidPlay(music.songNo) { e ->
                e?.message?.let { error ->
                    CustomToast.show(error, Toast.LENGTH_SHORT)
                }
            }
        }
    }

    private var loadingMusic: AtomicBoolean = AtomicBoolean(false)

    private fun loadMusic(config: KTVLoadMusicConfiguration, songInfo: ChosenSongInfo, isOwnSong: Boolean) {
        loadingMusic.set(true)
        innerLoadMusic(config, songInfo, object : SongLoadStateListener {
            override fun onMusicLoadProgress(
                songCode: String,
                percent: Int,
                status: MusicLoadStatus,
                lyricUrl: String?
            ) {
                loadMusicProgressLiveData.postValue(percent)
            }

            override fun onMusicLoadSuccess(songCode: String, musicUri: String, lyricUrl: String) {
                loadingMusic.set(false)
                KTVLogger.d(TAG, "onMusicLoadSuccess, songCode: $songCode lyricUrl: $lyricUrl")
                // Currently has been switched to another song.
                if (songPlayingLiveData.getValue() == null) {
                    CustomToast.show(R.string.ktv_load_failed_no_song, Toast.LENGTH_LONG)
                    return
                }
                if (isOwnSong) {
                    // Need to check if there are any chorus members at this time; if so, switch to LeaderSinger identity.
                    if (chorusInfoList.size == 0) {
                        ktvApiProtocol.switchSingerRole(KTVSingRole.SoloSinger, null)
                    } else if (chorusInfoList.size > 0) {
                        ktvApiProtocol.switchSingerRole(KTVSingRole.LeadSinger, null)
                    }
                    ktvApiProtocol.loadMusic(musicUri, config)
                    ktvApiProtocol.startSing(musicUri, 0)
                } else {
                    ktvApiProtocol.loadMusic(musicUri, config)
                }

                // reset settings
                retryTimes = 0
                runOnMainThread {
                    playerMusicStatusLiveData.value = PlayerMusicStatus.ON_PLAYING
                }
            }

            override fun onMusicLoadFail(songCode: String, reason: SongLoadFailReason) {
                loadingMusic.set(false)
                KTVLogger.e(TAG, "onMusicLoadFail，songCode:$songCode, reason:$reason")
                // Currently has been switched to another song.
                if (songPlayingLiveData.getValue() == null) {
                    CustomToast.show(R.string.ktv_load_failed_no_song, Toast.LENGTH_LONG)
                    return
                }
                if (reason == SongLoadFailReason.MUSIC_DOWNLOAD_FAIL) { // Song loading failed, retrying 3 times.
                    CustomToast.show(R.string.ktv_load_failed, Toast.LENGTH_LONG)
                    retryTimes += 1
                    if (retryTimes < 3) {
                        loadMusic(config, songInfo, isOwnSong)
                    } else {
                        runOnMainThread {
                            playerMusicStatusLiveData.value = PlayerMusicStatus.ON_PLAYING
                        }
                        CustomToast.show(R.string.ktv_try, Toast.LENGTH_LONG)
                    }
                } else if (reason == SongLoadFailReason.CANCELED) { // Currently has been switched to another song.
                    CustomToast.show(R.string.ktv_load_failed_another_song, Toast.LENGTH_LONG)
                } else {
                    CustomToast.show(R.string.ktv_load_failed, Toast.LENGTH_LONG)
                }
            }
        })
    }

    private fun soloSingerJoinChorusMode(lastChorusNum: Int) {
        if (songPlayingLiveData.getValue() == null || seatListLiveData.getValue() == null) return
        if (songPlayingLiveData.value?.owner?.userId == KtvCenter.mUser.id.toString()) {
            if (lastChorusNum == 0 && chorusInfoList.size > 0) { // somebody join the chorus
                ktvApiProtocol.switchSingerRole(KTVSingRole.LeadSinger, null)
            } else if (lastChorusNum > 0 && chorusInfoList.size == 0) { // last user leave the chorus
                ktvApiProtocol.switchSingerRole(KTVSingRole.SoloSinger, null)
            }
        }
    }

    /**
     * Re get lrc url.
     */
    fun reGetLrcUrl() {
        KTVLogger.e(TAG, "reGetLrcUrl，called")
        val songPlayingValue = songPlayingLiveData.getValue() ?: return
        val isOwnSong = songPlayingValue.owner?.userId == KtvCenter.mUser.id.toString()
        loadMusic(
            KTVLoadMusicConfiguration(
                songPlayingValue.songNo,
                songPlayingValue.owner?.userId?.toIntOrNull() ?: -1,
                KTVLoadMusicMode.LOAD_LRC_ONLY,
                false
            ), songPlayingValue, isOwnSong
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
    val songDuration: Long get() = ktvApiProtocol.getMediaPlayer().duration

    /**
     * Music stop.
     */
    fun musicStop() {
        KTVLogger.d(TAG, "RoomLivingViewModel.musicStop() called")
        resetMusicStatus()
    }

    /**
     * Sync single line score.
     *
     * @param score           the score
     * @param cumulativeScore the cumulative score
     * @param index           the index
     * @param total           the total
     */
    // ------------------ Lrc  ------------------
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

    val scope = CoroutineScope(Job() + Dispatchers.Main)

    private fun getMusicFolder(): String? {
        val folder = AgoraApplication.the().getExternalFilesDir("musics")
        return folder?.absolutePath
    }

    private fun innerLoadMusic(
        config: KTVLoadMusicConfiguration,
        songInfo: ChosenSongInfo,
        songLoadStateListener: SongLoadStateListener
    ) {
        if (config.mode == KTVLoadMusicMode.LOAD_NONE) {
            return
        }
        val song = songList.firstOrNull { it.songCode == songInfo.songNo } ?: return
        if (config.mode == KTVLoadMusicMode.LOAD_LRC_ONLY) {
            if (songPlayingLiveData.value?.songNo != songInfo.songNo) {
                // The current song has changed; the latest loaded song will prevail.
                songLoadStateListener.onMusicLoadFail(songInfo.songNo, SongLoadFailReason.CANCELED)
                return
            }

            lrcControlView?.get()?.onDownloadLrcData(song.lyric)
            songLoadStateListener.onMusicLoadSuccess(songInfo.songNo, "", song.lyric)
            return
        }

        val path =
            getMusicFolder() ?: return songLoadStateListener.onMusicLoadFail(songInfo.songNo, SongLoadFailReason.UNKNOW)


        scope.launch(Dispatchers.IO) {
            DownloadManager.instance.download(
                url = song.music,
                destinationPath = path,
                callback = object : DownloadManager.FileDownloadCallback {
                    override fun onProgress(file: File, progress: Int) {
                        songLoadStateListener.onMusicLoadProgress(
                            songCode = songInfo.songNo,
                            percent = progress,
                            status = MusicLoadStatus.INPROGRESS,
                            lyricUrl = song.lyric
                        )
                    }

                    override fun onSuccess(file: File) {
                        // Currently has been switched to another song.
                        if (songPlayingLiveData.value?.songNo != songInfo.songNo) {
                            songLoadStateListener.onMusicLoadFail(songInfo.songNo, SongLoadFailReason.CANCELED)
                            return
                        }
                        val musicUri = path + File.separator + song.music.substringAfterLast("/")
                        if (config.mode == KTVLoadMusicMode.LOAD_MUSIC_AND_LRC) {
                            lrcControlView?.get()?.onDownloadLrcData(song.lyric)
                            songLoadStateListener.onMusicLoadProgress(
                                songCode = songInfo.songNo,
                                percent = 100,
                                status = MusicLoadStatus.INPROGRESS,
                                lyricUrl = song.lyric
                            )
                            songLoadStateListener.onMusicLoadSuccess(
                                songInfo.songNo, musicUri, song.lyric
                            )
                        } else if (config.mode == KTVLoadMusicMode.LOAD_MUSIC_ONLY) {
                            songLoadStateListener.onMusicLoadProgress(
                                songCode = songInfo.songNo,
                                percent = 100,
                                status = MusicLoadStatus.INPROGRESS,
                                lyricUrl = song.lyric
                            )
                            songLoadStateListener.onMusicLoadSuccess(
                                songInfo.songNo,
                                musicUri,
                                song.lyric
                            )
                        }
                    }

                    override fun onFailed(exception: Exception) {
                        songLoadStateListener.onMusicLoadFail(songInfo.songNo, SongLoadFailReason.MUSIC_DOWNLOAD_FAIL)
                    }
                }
            )
        }
    }
}