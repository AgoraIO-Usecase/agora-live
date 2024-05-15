package io.agora.scene.ktv.live

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import io.agora.rtc2.Constants
import io.agora.scene.base.GlideApp
import io.agora.scene.base.bean.User
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.component.OnButtonClickListener
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.LiveDataUtils
import io.agora.scene.ktv.KTVLogger.d
import io.agora.scene.ktv.R
import io.agora.scene.ktv.databinding.KtvActivityRoomLivingBinding
import io.agora.scene.ktv.databinding.KtvItemRoomSpeakerBinding
import io.agora.scene.ktv.debugSettings.KTVDebugSettingsDialog
import io.agora.scene.ktv.live.RoomLivingViewModel.JoinChorusStatus
import io.agora.scene.ktv.live.RoomLivingViewModel.LineScore
import io.agora.scene.ktv.live.RoomLivingViewModel.PlayerMusicStatus
import io.agora.scene.ktv.live.bean.NetWorkEvent
import io.agora.scene.ktv.live.fragmentdialog.MusicSettingDialog
import io.agora.scene.ktv.live.fragmentdialog.UserLeaveSeatMenuDialog
import io.agora.scene.ktv.live.listener.LrcActionListenerImpl
import io.agora.scene.ktv.live.listener.SongActionListenerImpl
import io.agora.scene.ktv.service.JoinRoomOutputModel
import io.agora.scene.ktv.service.RoomSeatModel
import io.agora.scene.ktv.service.RoomSelSongModel
import io.agora.scene.ktv.service.ScoringAlgoControlModel
import io.agora.scene.ktv.service.ScoringAverageModel
import io.agora.scene.ktv.service.VolumeModel
import io.agora.scene.ktv.widget.KtvCommonDialog
import io.agora.scene.ktv.widget.lrcView.LrcControlView
import io.agora.scene.ktv.widget.song.SongDialog
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder
import io.agora.scene.widget.dialog.CommonDialog
import io.agora.scene.widget.dialog.PermissionLeakDialog
import io.agora.scene.widget.dialog.TopFunctionDialog
import io.agora.scene.widget.toast.CustomToast
import io.agora.scene.widget.utils.DividerDecoration
import io.agora.scene.widget.utils.UiUtils

/**
 * 房间主页
 */
class RoomLivingActivity : BaseViewBindingActivity<KtvActivityRoomLivingBinding>() {

    companion object {
        private const val EXTRA_ROOM_INFO = "roomInfo"

        /**
         * Launch
         *
         * @param context
         * @param roomOutputModel
         */
        fun launch(context: Context, roomOutputModel: JoinRoomOutputModel) {
            val intent = Intent(context, RoomLivingActivity::class.java)
            intent.putExtra(EXTRA_ROOM_INFO, roomOutputModel)
            context.startActivity(intent)
        }

        private fun fillWithRenderView(container: ViewGroup): SurfaceView {
            val context = container.context
            val cardView = MaterialCardView(context, null, R.attr.materialCardViewStyle)
            cardView.cardElevation = 0f
            cardView.addOnLayoutChangeListener { v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int ->
                cardView.radius = (right - left) / 2f
            }
            val surfaceView = SurfaceView(context)
            surfaceView.setLayoutParams(
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            cardView.addView(surfaceView)
            container.addView(cardView)
            return surfaceView
        }
    }

    private var musicSettingDialog: MusicSettingDialog? = null
    private var mRoomSpeakerAdapter: BindingSingleAdapter<RoomSeatModel, KtvItemRoomSpeakerBinding>? = null
    private var creatorExitDialog: KtvCommonDialog? = null
    private var exitDialog: CommonDialog? = null
    private var mUserLeaveSeatMenuDialog: UserLeaveSeatMenuDialog? = null
    private var mChooseSongDialog: SongDialog? = null

    private val mUser: User get() = UserManager.getInstance().getUser()

    private val roomLivingViewModel: RoomLivingViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(aClass: Class<T>): T {
                val romModel = (intent.getSerializableExtra(EXTRA_ROOM_INFO) as JoinRoomOutputModel?)!!
                return RoomLivingViewModel(romModel) as T
            }
        })[RoomLivingViewModel::class.java]
    }

    // 房间存活时间，单位ms
    private var timeUpExitDialog: KtvCommonDialog? = null
    override fun getViewBinding(inflater: LayoutInflater): KtvActivityRoomLivingBinding {
        return KtvActivityRoomLivingBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            finish()
            return
        }
        window.decorView.keepScreenOn = true
        setOnApplyWindowInsetsListener(binding.superLayout)
        mRoomSpeakerAdapter = object : BindingSingleAdapter<RoomSeatModel, KtvItemRoomSpeakerBinding>() {
            override fun onBindViewHolder(holder: BindingViewHolder<KtvItemRoomSpeakerBinding>, position: Int) {
                val item = getItem(position)
                val binding = holder.binding
                val isOutSeat = TextUtils.isEmpty(item?.userNo)
                binding.getRoot().setOnClickListener { v: View? ->
                    if (!isOutSeat) { // 下麦
                        item ?: return@setOnClickListener
                        if (roomLivingViewModel.isRoomOwner) {
                            if (item.userNo != mUser.id.toString()) {
                                showUserLeaveSeatMenuDialog(item)
                            }
                        } else if (item.userNo == mUser.id.toString()) {
                            showUserLeaveSeatMenuDialog(item)
                        }

                    } else {
                        // 上麦
                        val seatLocal = roomLivingViewModel.seatLocalLiveData.getValue()
                        if (seatLocal == null || seatLocal.seatIndex < 0) {
                            toggleAudioRun = Runnable {
                                roomLivingViewModel.haveSeat(position)
                                getBinding().cbMic.setChecked(false)
                                getBinding().cbVideo.setChecked(false)
                            }
                            requestRecordPermission()
                        }
                    }
                }
                if (isOutSeat) {
                    binding.vMicWave.endWave()
                    binding.vMicWave.visibility = View.INVISIBLE
                    binding.avatarItemRoomSpeaker.setImageResource(R.mipmap.ktv_ic_seat)
                    binding.avatarItemRoomSpeaker.setVisibility(View.VISIBLE)
                    binding.tvZC.visibility = View.GONE
                    binding.tvHC.visibility = View.GONE
                    binding.tvRoomOwner.visibility = View.GONE
                    binding.ivMute.setVisibility(View.GONE)
                    binding.tvUserName.text = getString(R.string.ktv_seat_num, (position + 1).toString())
                    binding.flVideoContainer.removeAllViews()
                } else {
                    binding.vMicWave.visibility = View.VISIBLE
                    binding.tvUserName.text = item?.name
                    if (item?.isMaster == true && position == 0) {
                        binding.tvRoomOwner.visibility = View.VISIBLE
                    } else {
                        binding.tvRoomOwner.visibility = View.GONE
                    }

                    // microphone
                    if (item?.isAudioMuted == RoomSeatModel.MUTED_VALUE_TRUE) {
                        binding.vMicWave.endWave()
                        binding.ivMute.setVisibility(View.VISIBLE)
                    } else {
                        binding.ivMute.setVisibility(View.GONE)
                    }

                    // video
                    if (item?.isVideoMuted == RoomSeatModel.MUTED_VALUE_TRUE) {
                        binding.avatarItemRoomSpeaker.setVisibility(View.VISIBLE)
                        binding.flVideoContainer.removeAllViews()
                        GlideApp.with(binding.getRoot())
                            .load(item.headUrl)
                            .error(io.agora.scene.base.R.mipmap.default_user_avatar)
                            .apply(RequestOptions.circleCropTransform())
                            .into(binding.avatarItemRoomSpeaker)
                    } else {
                        binding.avatarItemRoomSpeaker.setVisibility(View.INVISIBLE)
                        binding.flVideoContainer.removeAllViews()
                        val surfaceView = fillWithRenderView(binding.flVideoContainer)
                        if (item?.userNo == mUser.id.toString()) { // 是本人
                            roomLivingViewModel.renderLocalCameraVideo(surfaceView)
                        } else {
                            val uid = item?.rtcUid?.toIntOrNull() ?: -1
                            roomLivingViewModel.renderRemoteCameraVideo(surfaceView, uid)
                        }
                    }
                    val songModel = roomLivingViewModel.songPlayingLiveData.getValue()
                    if (songModel != null) {
                        if (item?.userNo == songModel.userNo) {
                            binding.tvZC.setText(R.string.ktv_zc)
                            binding.tvHC.visibility = View.GONE
                            binding.tvZC.visibility = View.VISIBLE
                        } else if (item?.chorusSongCode == songModel.songNo + songModel.createAt) {
                            binding.tvHC.setText(R.string.ktv_hc)
                            binding.tvZC.visibility = View.GONE
                            binding.tvHC.visibility = View.VISIBLE
                        } else {
                            binding.tvZC.visibility = View.GONE
                            binding.tvHC.visibility = View.GONE
                        }
                    } else {
                        binding.tvZC.visibility = View.GONE
                        binding.tvHC.visibility = View.GONE
                    }
                }
            }
        }
        binding.rvUserMember.addItemDecoration(DividerDecoration(4, 24, 8))
        binding.rvUserMember.adapter = mRoomSpeakerAdapter
        mRoomSpeakerAdapter?.resetAll(mutableListOf<RoomSeatModel?>(null, null, null, null, null, null, null, null))
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            binding.rvUserMember.setOverScrollMode(View.OVER_SCROLL_NEVER)
        }
        binding.lrcControlView.setRole(LrcControlView.Role.Listener)
        binding.lrcControlView.post {
            // TODO workaround 先强制申请权限， 避免首次安装无声
            if (roomLivingViewModel.isRoomOwner) {
                toggleAudioRun = Runnable {
                    roomLivingViewModel.initData()
                    roomLivingViewModel.setLrcView(binding.lrcControlView)
                }
                requestRecordPermission()
            } else {
                roomLivingViewModel.initData()
                roomLivingViewModel.setLrcView(binding.lrcControlView)
            }
        }
        val roomModel = roomLivingViewModel.roomModelLiveData.getValue()
        binding.tvRoomName.text = roomModel?.roomInfo?.roomName ?: ""
        GlideApp.with(binding.getRoot())
            .load(roomModel?.roomInfo?.roomOwner?.userAvatar)
            .error(io.agora.scene.base.R.mipmap.default_user_avatar)
            .apply(RequestOptions.circleCropTransform())
            .into(binding.ivOwnerAvatar)
        binding.btnDebug.isVisible = AgoraApplication.the().isDebugModeOpen
        binding.btnDebug.setOnClickListener { v: View? ->
            val dialog = KTVDebugSettingsDialog(
                roomLivingViewModel.mDebugSetting,
                roomModel?.roomInfo?.roomId,
                roomLivingViewModel.sDKBuildNum
            )
            dialog.show(supportFragmentManager, "debugSettings")
        }
        binding.ivMore.setOnClickListener { v: View? -> TopFunctionDialog(this).show() }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val params = binding.rvUserMember.layoutParams as ConstraintLayout.LayoutParams
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val heightPixels: Int // current window
        val widthPixels: Int // current window
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val rect = windowManager.currentWindowMetrics.bounds
            heightPixels = rect.height()
            widthPixels = rect.width()
        } else {
            val point = Point()
            windowManager.defaultDisplay.getSize(point)
            heightPixels = point.y
            widthPixels = point.x
        }
        if (heightPixels * 1.0 / widthPixels > 16.0 / 9) { // 2K/Slim/> 16:9 screens
            // TODO(HAI_GUO) Flip/Fold/Split screens and One-handed mode may not supported well
            params.bottomMargin = (heightPixels * (1.0 - 16.0 * widthPixels / (9 * heightPixels))).toInt()
            binding.rvUserMember.setLayoutParams(params)
        }
        // density 4.0 densityDpi 640 system resources 2560 1440 display real 2560 1440 current window 2560 1440 HUAWEI V9
        // density 3.0 densityDpi 480 system resources 2297 1080 display real 2400 1080 current window 2400 1080 1+ 9R
    }

    override fun initListener() {
        binding.ivExit.setOnClickListener { view: View? -> showExitDialog() }
        binding.superLayout.setOnClickListener { view: View? -> setDarkStatusIcon(isBlackDarkStatus) }
        binding.cbMic.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            if (!compoundButton.isPressed) return@setOnCheckedChangeListener
            val seatLocal = roomLivingViewModel.seatLocalLiveData.getValue() ?: return@setOnCheckedChangeListener
            mRoomSpeakerAdapter?.getItem(seatLocal.seatIndex) ?: return@setOnCheckedChangeListener
            if (b) {
                toggleAudioRun = Runnable { roomLivingViewModel.toggleMic(true) }
                requestRecordPermission(true)
            } else {
                roomLivingViewModel.toggleMic(false)
            }
        }
        binding.iBtnChooseSong.setOnClickListener { v: View? -> showChooseSongDialog() }
        binding.btnMenu.setOnClickListener { v: View? -> showMusicSettingDialog() }
        binding.btnOK.setOnClickListener { view: View? -> binding.groupResult.visibility = View.GONE }
        val lrcActionListenerImpl: LrcActionListenerImpl =
            object : LrcActionListenerImpl(this, roomLivingViewModel, binding.lrcControlView) {
                override fun onChangeMusicClick() {
                    super.onChangeMusicClick()
                    showChangeMusicDialog()
                }

                override fun onVocalHighlightClick() {
                    super.onVocalHighlightClick()
                    //showVoiceHighlightDialog();
                }
            }
        binding.lrcControlView.setOnLrcClickListener(lrcActionListenerImpl)
        binding.cbVideo.setOnCheckedChangeListener { compoundButton: CompoundButton, b: Boolean ->
            if (!compoundButton.isPressed) return@setOnCheckedChangeListener
            toggleSelfVideo(b)
        }

        // 房间相关
        roomLivingViewModel.roomDeleteLiveData.observe(this) { deletedByCreator: Boolean ->
            if (deletedByCreator) {
                showCreatorExitDialog()
            } else {
                finish()
            }
        }
        roomLivingViewModel.roomUserCountLiveData.observe(this) { count: Int ->
            binding.tvRoomMCount.text = getString(
                R.string.ktv_room_count, count.toString()
            )
        }
        roomLivingViewModel.roomTimeUpLiveData.observe(this) { isTimeUp: Boolean ->
            if (roomLivingViewModel.release() && isTimeUp) {
                showTimeUpExitDialog()
            }
        }

        // 麦位相关
        roomLivingViewModel.seatLocalLiveData.observe(this, Observer { seatModel: RoomSeatModel? ->
            val isOnSeat = seatModel != null && seatModel.seatIndex >= 0
            binding.groupBottomView.visibility = if (isOnSeat) View.VISIBLE else View.GONE
            binding.groupEmptyPrompt.visibility = if (isOnSeat) View.GONE else View.VISIBLE
            val isVideoChecked = seatModel != null && seatModel.isVideoMuted == RoomSeatModel.MUTED_VALUE_FALSE
            binding.cbVideo.setChecked(isVideoChecked)
            val isAudioChecked = seatModel != null && seatModel.isAudioMuted == RoomSeatModel.MUTED_VALUE_FALSE
            binding.cbMic.setChecked(isAudioChecked)
            binding.lrcControlView.onSeat(seatModel != null)
        })
        roomLivingViewModel.seatListLiveData.observe(this) { seatModels: List<RoomSeatModel>? ->
            if (seatModels == null || roomLivingViewModel.mSetting == null) {
                return@observe
            }
            var chorusNowNum = 0
            for (seatModel in seatModels) {
                val oSeatModel = mRoomSpeakerAdapter?.getItem(seatModel.seatIndex)
                if (oSeatModel == null || oSeatModel.isAudioMuted != seatModel.isAudioMuted || oSeatModel.isVideoMuted != seatModel.isVideoMuted || oSeatModel.chorusSongCode != seatModel.chorusSongCode) {
                    mRoomSpeakerAdapter?.replace(seatModel.seatIndex, seatModel)
                }
                seatModel.chorusSongCode
                roomLivingViewModel.songPlayingLiveData.getValue()?.let { songPlay ->
                    if (seatModel.chorusSongCode == songPlay.songNo + songPlay.createAt) {
                        chorusNowNum++
                    }
                }
                if (roomLivingViewModel.chorusNum == 0 && chorusNowNum > 0) { // 有人加入合唱
                    roomLivingViewModel.soloSingerJoinChorusMode(true)
                } else if (roomLivingViewModel.chorusNum > 0 && chorusNowNum == 0) { // 最后一人退出合唱
                    roomLivingViewModel.soloSingerJoinChorusMode(false)
                }
                roomLivingViewModel.chorusNum = chorusNowNum
                mRoomSpeakerAdapter?.let { roomSpeakerAdapter ->
                    for (i in 0 until roomSpeakerAdapter.itemCount) {
                        val seatModel = roomSpeakerAdapter.getItem(i) ?: continue
                        var exist = false
                        for (model in seatModels) {
                            if (seatModel.seatIndex == model.seatIndex) {
                                exist = true
                                break
                            }
                        }
                        if (!exist) {
                            onMemberLeave(seatModel)
                        }
                    }
                }

                if (seatModels.size == 8) {
                    binding.lrcControlView.onSeatFull(true)
                } else if (seatModels.size < 8) {
                    binding.lrcControlView.onSeatFull(false)
                }
            }
        }
        roomLivingViewModel.volumeLiveData.observe(this) { value: VolumeModel ->
            var volumeModel = value
            if (volumeModel.uid == 0) {
                volumeModel = VolumeModel(UserManager.getInstance().getUser().id.toInt(), volumeModel.volume)
            }
            mRoomSpeakerAdapter?.let { roomSpeakerAdapter ->
                for (i in 0 until roomSpeakerAdapter.itemCount) {
                    val seatModel = roomSpeakerAdapter.getItem(i)
                    if (seatModel != null && seatModel.rtcUid.toInt() == volumeModel.uid) {
                        val holder =
                            binding.rvUserMember.findViewHolderForAdapterPosition(i) as BindingViewHolder<KtvItemRoomSpeakerBinding>?
                                ?: return@observe
                        if (volumeModel.volume == 0 || seatModel.isAudioMuted == RoomSeatModel.MUTED_VALUE_TRUE) {
                            holder.binding.vMicWave.endWave()
                        } else {
                            holder.binding.vMicWave.startWave()
                        }
                    }
                }
            }

        }

        // 歌词相关
        roomLivingViewModel.mainSingerScoreLiveData.observe(this) { score: LineScore ->
            binding.lrcControlView.onReceiveSingleLineScore(
                score.score,
                score.index,
                score.cumulativeScore,
                score.total
            )
        }
        roomLivingViewModel.songsOrderedLiveData.observe(this) { models: List<RoomSelSongModel>? ->
            if (models.isNullOrEmpty()) {
                // songs empty
                binding.lrcControlView.setRole(LrcControlView.Role.Listener)
                binding.lrcControlView.onIdleStatus()
                mRoomSpeakerAdapter?.notifyDataSetChanged()
            }
            mChooseSongDialog?.resetChosenSongList(SongActionListenerImpl.transSongModel(models))
        }
        roomLivingViewModel.songPlayingLiveData.observe(this) { model: RoomSelSongModel? ->
            if (model == null) {
                roomLivingViewModel.musicStop()
                return@observe
            }
            onMusicChanged(model)
            if (roomLivingViewModel.isRoomOwner) {
                binding.lrcControlView.showHighLightButton(false)
                binding.lrcControlView.setHighLightPersonHeadUrl("")
            }
            roomLivingViewModel.mSetting?.mHighLighterUid = ""
        }
        roomLivingViewModel.scoringAlgoControlLiveData.observe(this) { model: ScoringAlgoControlModel? ->
            model ?: return@observe
            binding.lrcControlView.karaokeView.scoringLevel = model.level
            binding.lrcControlView.karaokeView.scoringCompensationOffset = model.offset
        }
        roomLivingViewModel.noLrcLiveData.observe(this) { isNoLrc: Boolean ->
            if (isNoLrc) {
                binding.lrcControlView.onNoLrc()
            }
        }
        roomLivingViewModel.playerMusicStatusLiveData.observe(this) { status: PlayerMusicStatus ->
            if (status == PlayerMusicStatus.ON_PREPARE) {
                binding.lrcControlView.onPrepareStatus(roomLivingViewModel.isRoomOwner)
            } else if (status == PlayerMusicStatus.ON_PLAYING) {
                binding.lrcControlView.onPlayStatus(roomLivingViewModel.songPlayingLiveData.getValue())
            } else if (status == PlayerMusicStatus.ON_PAUSE) {
                binding.lrcControlView.onPauseStatus()
            } else if (status == PlayerMusicStatus.ON_LRC_RESET) {
                binding.lrcControlView.lyricsView.reset()
                if (binding.lrcControlView.role == LrcControlView.Role.Singer) {
                    roomLivingViewModel.changeMusic()
                }
            } else if (status == PlayerMusicStatus.ON_CHANGING_START) {
                binding.lrcControlView.setEnabled(false)
            } else if (status == PlayerMusicStatus.ON_CHANGING_END) {
                binding.lrcControlView.setEnabled(true)
            }
        }
        roomLivingViewModel.joinchorusStatusLiveData.observe(this) { status: JoinChorusStatus ->
            if (status == JoinChorusStatus.ON_JOIN_CHORUS) {
                binding.cbMic.setChecked(true)
                binding.lrcControlView.onSelfJoinedChorus()
            } else if (status == JoinChorusStatus.ON_JOIN_FAILED) {
                binding.lrcControlView.onSelfJoinedChorusFailed()
                val yOfChorusBtn = binding.lrcControlView.getYOfChorusBtn()
                CustomToast.showByPosition(R.string.ktv_join_chorus_failed, Gravity.TOP, yOfChorusBtn)
            } else if (status == JoinChorusStatus.ON_LEAVE_CHORUS) {
                binding.cbMic.setChecked(false)
                binding.lrcControlView.onSelfLeavedChorus()
            }
        }
        roomLivingViewModel.playerMusicOpenDurationLiveData.observe(this) { duration: Long ->
            binding.lrcControlView.lyricsView.setDuration(duration)
        }
        roomLivingViewModel.playerMusicPlayCompleteLiveData.observe(this) { (isLocal, score1): ScoringAverageModel ->
            if (isLocal) {
                val sc = binding.lrcControlView.cumulativeScoreInPercentage
                binding.tvResultScore.text = sc.toString()
                if (sc >= 90) {
                    binding.ivResultLevel.setImageResource(R.mipmap.ic_s)
                } else if (sc >= 80) {
                    binding.ivResultLevel.setImageResource(R.mipmap.ic_a)
                } else if (sc >= 70) {
                    binding.ivResultLevel.setImageResource(R.mipmap.ic_b)
                } else {
                    binding.ivResultLevel.setImageResource(R.mipmap.ic_c)
                }
                binding.groupResult.visibility = View.VISIBLE
                if (binding.lrcControlView.role == LrcControlView.Role.Singer) {
                    roomLivingViewModel.syncSingingAverageScore(sc.toDouble())
                }
            } else {
                if (binding.lrcControlView.role != LrcControlView.Role.Listener) return@observe
                binding.tvResultScore.text = score1.toString()
                if (score1 >= 90) {
                    binding.ivResultLevel.setImageResource(R.mipmap.ic_s)
                } else if (score1 >= 80) {
                    binding.ivResultLevel.setImageResource(R.mipmap.ic_a)
                } else if (score1 >= 70) {
                    binding.ivResultLevel.setImageResource(R.mipmap.ic_b)
                } else {
                    binding.ivResultLevel.setImageResource(R.mipmap.ic_c)
                }
                binding.groupResult.visibility = View.VISIBLE
            }
        }
        roomLivingViewModel.networkStatusLiveData.observe(this) { netWorkStatus: NetWorkEvent ->
            setNetWorkStatus(
                netWorkStatus.txQuality,
                netWorkStatus.rxQuality
            )
        }
        roomLivingViewModel.loadMusicProgressLiveData.observe(this) { percent: Int ->
            binding.lrcControlView.onMusicLoadProgress(percent)
        }
        roomLivingViewModel.scoringAlgoLiveData.observe(this) { difficulty: Int ->
            binding.lrcControlView.karaokeView.scoringLevel = difficulty
        }
    }

    private fun setNetWorkStatus(txQuality: Int, rxQuality: Int) {
        if (txQuality == Constants.QUALITY_BAD || txQuality == Constants.QUALITY_POOR || rxQuality == Constants.QUALITY_BAD || rxQuality == Constants.QUALITY_POOR) {
            binding.ivNetStatus.setImageResource(R.drawable.bg_round_yellow)
            binding.tvNetStatus.setText(R.string.ktv_net_status_m)
        } else if (txQuality == Constants.QUALITY_VBAD || txQuality == Constants.QUALITY_DOWN || rxQuality == Constants.QUALITY_VBAD || rxQuality == Constants.QUALITY_DOWN) {
            binding.ivNetStatus.setImageResource(R.drawable.bg_round_red)
            binding.tvNetStatus.setText(R.string.ktv_net_status_low)
        } else if (txQuality == Constants.QUALITY_EXCELLENT || txQuality == Constants.QUALITY_GOOD || rxQuality == Constants.QUALITY_EXCELLENT || rxQuality == Constants.QUALITY_GOOD) {
            binding.ivNetStatus.setImageResource(R.drawable.bg_round_green)
            binding.tvNetStatus.setText(R.string.ktv_net_status_good)
        } else if (txQuality == Constants.QUALITY_UNKNOWN || rxQuality == Constants.QUALITY_UNKNOWN) {
            binding.ivNetStatus.setImageResource(R.drawable.bg_round_red)
            binding.tvNetStatus.setText(R.string.ktv_net_status_un_know)
        } else {
            binding.ivNetStatus.setImageResource(R.drawable.bg_round_green)
            binding.tvNetStatus.setText(R.string.ktv_net_status_good)
        }
    }

    override fun onResume() {
        super.onResume()
        d("ktv", "onResume() $isBlackDarkStatus")
        setDarkStatusIcon(isBlackDarkStatus)
    }

    /**
     * 下麦提示
     */
    private fun showUserLeaveSeatMenuDialog(seatModel: RoomSeatModel) {
        if (mUserLeaveSeatMenuDialog == null) {
            mUserLeaveSeatMenuDialog = UserLeaveSeatMenuDialog(this)
        }
        mUserLeaveSeatMenuDialog?.onButtonClickListener = object : OnButtonClickListener {
            override fun onLeftButtonClick() {
                setDarkStatusIcon(isBlackDarkStatus)
                roomLivingViewModel.leaveChorus()
            }

            override fun onRightButtonClick() {
                setDarkStatusIcon(isBlackDarkStatus)
                roomLivingViewModel.leaveSeat(seatModel)
            }
        }
        mUserLeaveSeatMenuDialog?.setAgoraMember(seatModel.name, seatModel.headUrl)
        mUserLeaveSeatMenuDialog?.show()
    }

    private fun showTimeUpExitDialog() {
        if (timeUpExitDialog == null) {
            timeUpExitDialog = KtvCommonDialog(this).apply {
                if (roomLivingViewModel.isRoomOwner) {
                    setDescText(getString(R.string.time_up_exit_room))
                } else {
                    setDescText(getString(R.string.expire_exit_room))
                }
                setDialogBtnText("", getString(R.string.ktv_confirm))
                onButtonClickListener = object : OnButtonClickListener {
                    override fun onLeftButtonClick() {}
                    override fun onRightButtonClick() {
                        roomLivingViewModel.exitRoom()
                    }
                }
            }
        }
        timeUpExitDialog?.show()
    }

    private fun showExitDialog() {
        if (exitDialog == null) {
            exitDialog = CommonDialog(this).apply {
                if (roomLivingViewModel.isRoomOwner) {
                    setDialogTitle(getString(R.string.dismiss_room))
                    setDescText(getString(R.string.confirm_to_dismiss_room))
                } else {
                    setDialogTitle(getString(R.string.exit_room))
                    setDescText(getString(R.string.confirm_to_exit_room))
                }
                setDialogBtnText(getString(R.string.ktv_cancel), getString(R.string.ktv_confirm))
                onButtonClickListener = object : OnButtonClickListener {
                    override fun onLeftButtonClick() {
                        setDarkStatusIcon(isBlackDarkStatus)
                    }

                    override fun onRightButtonClick() {
                        setDarkStatusIcon(isBlackDarkStatus)
                        roomLivingViewModel.exitRoom()
                        finish()
                    }
                }
            }
        }
        exitDialog?.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onMusicChanged(music: RoomSelSongModel) {
        hideMusicSettingDialog()
        binding.lrcControlView.setMusic(music)
        if (UserManager.getInstance().getUser().id.toString() == music.userNo) {
            binding.lrcControlView.setRole(LrcControlView.Role.Singer)
        } else {
            binding.lrcControlView.setRole(LrcControlView.Role.Listener)
        }
        roomLivingViewModel.resetMusicStatus()
        roomLivingViewModel.musicStartPlay(music)
        mRoomSpeakerAdapter?.notifyDataSetChanged()
    }

    private fun filterSongTypeMap(typeMap: LinkedHashMap<Int, String>): LinkedHashMap<Int, String> {
        // 0 -> "项目热歌榜单"
        // 1 -> "声网热歌榜"
        // 2 -> "新歌榜" ("热门新歌")
        // 3 -> "嗨唱推荐"
        // 4 -> "抖音热歌"
        // 5 -> "古风热歌"
        // 6 -> "KTV必唱"
        val ret = LinkedHashMap<Int, String>()
        for (entry in typeMap.entries) {
            val key = entry.key
            var value = entry.value
            if (key == 2) {
                value = getString(R.string.ktv_song_rank_7)
                ret[key] = value
            } else if (key == 3 || key == 4 || key == 6) {
                ret[key] = value
            }
        }
        return ret
    }

    private var showChooseSongDialogTag = false
    private fun showChooseSongDialog() {
        if (showChooseSongDialogTag) {
            return
        }
        showChooseSongDialogTag = true
        if (mChooseSongDialog == null) {
            mChooseSongDialog = SongDialog()
            mChooseSongDialog?.setChosenControllable(roomLivingViewModel.isRoomOwner)
            showLoadingView()
            LiveDataUtils.observerThenRemove(this, roomLivingViewModel.getSongTypes()) { typeMap ->
                val chooseSongListener =
                    SongActionListenerImpl(this, roomLivingViewModel, filterSongTypeMap(typeMap), false)
                mChooseSongDialog?.setChooseSongTabsTitle(
                    chooseSongListener.getSongTypeTitles(this),
                    chooseSongListener.getSongTypeList(),
                    0
                )
                mChooseSongDialog?.setChooseSongListener(chooseSongListener)
                hideLoadingView()
                if (mChooseSongDialog?.isAdded == false) {
                    roomLivingViewModel.getSongChosenList()
                    mChooseSongDialog?.show(supportFragmentManager, "ChooseSongDialog")
                }
                binding.getRoot().post { showChooseSongDialogTag = false }
            }
            return
        }
        if (mChooseSongDialog?.isAdded == false) {
            roomLivingViewModel.getSongChosenList()
            mChooseSongDialog?.show(supportFragmentManager, "ChooseSongDialog")
        }
        binding.getRoot().post { showChooseSongDialogTag = false }
    }

    private fun showMusicSettingDialog() {
        if (musicSettingDialog == null) {
            musicSettingDialog = MusicSettingDialog(
                roomLivingViewModel.mSetting!!,
                roomLivingViewModel.mSoundCardSettingBean!!,
                binding.lrcControlView.role == LrcControlView.Role.Listener,
                roomLivingViewModel.songPlayingLiveData.getValue()
            )
        }
        if (musicSettingDialog?.isAdded == false) {
            musicSettingDialog?.show(supportFragmentManager, MusicSettingDialog.TAG)
        }
    }

    private fun hideMusicSettingDialog() {
        musicSettingDialog?.dismiss()
        musicSettingDialog = null
    }

    /**
     * Close music settings dialog.
     */
    fun closeMusicSettingsDialog() {
        setDarkStatusIcon(isBlackDarkStatus)
        hideMusicSettingDialog()
    }

    private var changeMusicDialog: CommonDialog? = null
    private fun showChangeMusicDialog() {
        if (UiUtils.isFastClick(2000)) {
            CustomToast.show(R.string.ktv_too_fast, Toast.LENGTH_SHORT)
            return
        }
        if (changeMusicDialog == null) {
            changeMusicDialog = CommonDialog(this).apply {
                setDialogTitle(getString(R.string.ktv_room_change_music_title))
                setDescText(getString(R.string.ktv_room_change_music_msg))
                setDialogBtnText(getString(R.string.ktv_cancel), getString(R.string.ktv_confirm))
                onButtonClickListener = object : OnButtonClickListener {
                    override fun onLeftButtonClick() {
                        setDarkStatusIcon(isBlackDarkStatus)
                    }

                    override fun onRightButtonClick() {
                        setDarkStatusIcon(isBlackDarkStatus)
                        roomLivingViewModel.changeMusic()
                    }
                }
            }
        }
        changeMusicDialog?.show()
    }

    override fun onStart() {
        super.onStart()
        roomLivingViewModel.onStart()
    }

    override fun onStop() {
        super.onStop()
        roomLivingViewModel.onStop()
    }

    override fun isBlackDarkStatus(): Boolean {
        return false
    }

    private var toggleVideoRun: Runnable? = null
    private var toggleAudioRun: Runnable? = null

    //开启 关闭摄像头
    private fun toggleSelfVideo(isOpen: Boolean) {
        if (isOpen) {
            toggleVideoRun = Runnable { roomLivingViewModel.toggleSelfVideo(true) }
            requestCameraPermission(true)
        } else {
            roomLivingViewModel.toggleSelfVideo(false)
        }
    }

    override fun getPermissions() {
        toggleVideoRun?.let {
            it.run()
            toggleVideoRun = null
        }
        toggleAudioRun?.let {
            it.run()
            toggleAudioRun = null
        }
    }

    override fun onPermissionDined(permission: String) {
        PermissionLeakDialog(this).show(permission, { getPermissions() }
        ) { launchAppSetting(permission) }
    }

    private fun onMemberLeave(member: RoomSeatModel) {
        if (member.userNo == UserManager.getInstance().getUser().id.toString()) {
            binding.groupBottomView.visibility = View.GONE
            binding.groupEmptyPrompt.visibility = View.VISIBLE
        }
        mRoomSpeakerAdapter?.getItem(member.seatIndex)?.let {
            mRoomSpeakerAdapter?.replace(member.seatIndex, null)
        }
    }

    private fun showCreatorExitDialog() {
        if (creatorExitDialog == null) {
            creatorExitDialog = KtvCommonDialog(this).apply {
                setDescText(getString(R.string.room_has_close))
                setDialogBtnText("", getString(R.string.ktv_iknow))
                onButtonClickListener = object : OnButtonClickListener {
                    override fun onLeftButtonClick() {}
                    override fun onRightButtonClick() {
                        setDarkStatusIcon(isBlackDarkStatus)
                        finish()
                    }
                }
            }
        }
        creatorExitDialog?.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        roomLivingViewModel.release()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showExitDialog()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}