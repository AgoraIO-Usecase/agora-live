package io.agora.scene.show

import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Looper
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
import io.agora.rtc2.RtcConnection
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.show.databinding.ShowLiveDetailFragmentBinding
import io.agora.scene.show.databinding.ShowLiveDetailMessageItemBinding
import io.agora.scene.show.databinding.ShowLivingEndDialogBinding
import io.agora.scene.show.debugSettings.DebugAudienceSettingDialog
import io.agora.scene.show.debugSettings.DebugSettingDialog
import io.agora.scene.show.service.ROOM_AVAILABLE_DURATION
import io.agora.scene.show.service.RoomException
import io.agora.scene.show.service.ShowInteractionInfo
import io.agora.scene.show.service.ShowInteractionStatus
import io.agora.scene.show.service.ShowMessage
import io.agora.scene.show.service.ShowMicSeatApply
import io.agora.scene.show.service.ShowRoomDetailModel
import io.agora.scene.show.service.ShowRoomRequestStatus
import io.agora.scene.show.service.ShowServiceProtocol
import io.agora.scene.show.service.ShowUser
import io.agora.scene.show.videoSwitcherAPI.VideoSwitcher
import io.agora.scene.show.widget.AdvanceSettingAudienceDialog
import io.agora.scene.show.widget.AdvanceSettingDialog
import io.agora.scene.show.widget.BeautyDialog
import io.agora.scene.show.widget.MusicEffectDialog
import io.agora.scene.show.widget.PictureQualityDialog
import io.agora.scene.show.widget.SettingDialog
import io.agora.scene.show.widget.TextInputDialog
import io.agora.scene.show.widget.link.LiveLinkAudienceSettingsDialog
import io.agora.scene.show.widget.link.LiveLinkDialog
import io.agora.scene.show.widget.link.OnLinkDialogActionListener
import io.agora.scene.show.widget.pk.LivePKDialog
import io.agora.scene.show.widget.pk.LivePKSettingsDialog
import io.agora.scene.show.widget.pk.LiveRoomConfig
import io.agora.scene.show.widget.pk.OnPKDialogActionListener
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone


/**
 * Live detail fragment
 *
 * @constructor Create empty Live detail fragment
 */
class LiveDetailFragment : Fragment() {
    /**
     * Tag
     */
    private val TAG = this.toString()

    companion object {

        private const val EXTRA_ROOM_DETAIL_INFO = "roomDetailInfo"

        /**
         * New instance
         *
         * @param roomDetail
         */
        fun newInstance(roomDetail: ShowRoomDetailModel) = LiveDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(EXTRA_ROOM_DETAIL_INFO, roomDetail)
            }
        }

    }

    /**
     * M room info
     */
    val mRoomInfo by lazy { (arguments?.getParcelable(EXTRA_ROOM_DETAIL_INFO) as? ShowRoomDetailModel)!! }

    /**
     * M binding
     */
    private val mBinding by lazy {
        ShowLiveDetailFragmentBinding.inflate(
            LayoutInflater.from(requireContext())
        )
    }

    /**
     * M service
     */
    private val mService by lazy { ShowServiceProtocol.getImplInstance() }

    /**
     * Is room owner
     */
    private val isRoomOwner by lazy { mRoomInfo.ownerId == UserManager.getInstance().user.id.toString() }

    /**
     * M message adapter
     */
    private var mMessageAdapter: BindingSingleAdapter<ShowMessage, ShowLiveDetailMessageItemBinding>? =
        null

    /**
     * M music effect dialog
     */
    private val mMusicEffectDialog by lazy { MusicEffectDialog(requireContext()) }

    /**
     * M setting dialog
     */
    private val mSettingDialog by lazy { SettingDialog(requireContext()) }

    /**
     * M link setting dialog
     */
    private val mLinkSettingDialog by lazy { LiveLinkAudienceSettingsDialog(requireContext()) }

    /**
     * M p k settings dialog
     */
    private val mPKSettingsDialog by lazy { LivePKSettingsDialog(requireContext()) }

    /**
     * M link dialog
     */
    private val mLinkDialog by lazy { LiveLinkDialog() }

    /**
     * M p k dialog
     */
    private val mPKDialog by lazy { LivePKDialog() }

    /**
     * M beauty processor
     */
    private val mBeautyProcessor by lazy { RtcEngineInstance.beautyProcessor }

    /**
     * M rtc engine
     */
    private val mRtcEngine by lazy { RtcEngineInstance.rtcEngine }

    /**
     * M rtc video switcher
     */
    private val mRtcVideoSwitcher by lazy { VideoSwitcher.getImplInstance(mRtcEngine) }

    /**
     * Show debug mode dialog
     *
     */
    private fun showDebugModeDialog() = DebugSettingDialog(requireContext()).show()

    /**
     * Show audience debug mode dialog
     *
     */
    private fun showAudienceDebugModeDialog() = DebugAudienceSettingDialog(requireContext()).show()

    /**
     * Interaction info
     */// 当前互动状态
    private var interactionInfo: ShowInteractionInfo? = null

    /**
     * Is p k competition
     */
    private var isPKCompetition: Boolean = false
//    private var deletedPKInvitation: ShowPKInvitation? = null

    /**
     * M link invitation count down latch
     */
    private var mLinkInvitationCountDownLatch: CountDownTimer? = null

    /**
     * M p k invitation count down latch
     */
    private var mPKInvitationCountDownLatch: CountDownTimer? = null

    /**
     * M p k count down latch
     */
    private var mPKCountDownLatch: CountDownTimer? = null

    /**
     * Is audio only mode
     */
    private var isAudioOnlyMode = false

    /**
     * Is page loaded
     */
    private var isPageLoaded = false

    /**
     * Local video canvas
     */
    private var localVideoCanvas: LocalVideoCanvasWrap? = null

    /**
     * Timer room end run
     */
    private val timerRoomEndRun = Runnable {
        destroy(false)
        showLivingEndLayout()
        ShowLogger.d("showLivingEndLayout","timer end!")
    }

    /**
     * M main rtc connection
     */
    private val mMainRtcConnection by lazy { RtcConnection(mRoomInfo.roomId, UserManager.getInstance().user.id.toInt()) }

    /**
     * On create view
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ShowLogger.d(TAG, "Fragment Lifecycle: onCreateView")
        return mBinding.root
    }

    /**
     * On view created
     *
     * @param view
     * @param savedInstanceState
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ShowLogger.d(TAG, "Fragment Lifecycle: onViewCreated")
        initView()
        activity?.onBackPressedDispatcher?.addCallback(enabled = isVisible) {
            onBackPressed()
        }
        changeStatisticVisible(true)
    }

    /**
     * On attach
     *
     * @param context
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        ShowLogger.d(TAG, "Fragment Lifecycle: onAttach")
        onMeLinkingListener = (activity as? LiveDetailActivity)
        if (isPageLoaded) {
            startLoadPage(false)
        }
    }

    /**
     * On detach
     *
     */
    override fun onDetach() {
        super.onDetach()
        ShowLogger.d(TAG, "Fragment Lifecycle: onDetach")
    }

    /**
     * Run on ui thread
     *
     * @param run
     */
    private fun runOnUiThread(run: Runnable) {
        val activity = activity ?: return
        if (Thread.currentThread() == Looper.getMainLooper().thread) {
            run.run()
        } else {
            activity.runOnUiThread(run)
        }
    }

    /**
     * Start load page safely
     *
     */
    fun startLoadPageSafely(){
        isPageLoaded = true
        activity ?: return
        startLoadPage(true)
    }

    /**
     * Re load page
     *
     */
    fun reLoadPage() {
        if (!isRoomOwner) {
            mRtcVideoSwitcher.preJoinChannel(mMainRtcConnection)
        }
        updatePKingMode()
    }

    /**
     * Start load page
     *
     * @param isScrolling
     */
    private fun startLoadPage(isScrolling: Boolean){
        ShowLogger.d(TAG, "Fragment PageLoad start load, roomId=${mRoomInfo.roomId}")
        isPageLoaded = true

        if (mRoomInfo.isRobotRoom()) {
            initRtcEngine(isScrolling) {}
            initServiceWithJoinRoom()
        } else {
            val roomLeftTime =
                ROOM_AVAILABLE_DURATION - (TimeUtils.currentTimeMillis() - mRoomInfo.createdAt.toLong())
            if (roomLeftTime > 0) {
                mBinding.root.postDelayed(timerRoomEndRun, ROOM_AVAILABLE_DURATION)
                initRtcEngine(isScrolling) {}
                initServiceWithJoinRoom()
            }
        }

        startTopLayoutTimer()
    }

    /**
     * Stop load page
     *
     * @param isScrolling
     */
    fun stopLoadPage(isScrolling: Boolean){
        ShowLogger.d(TAG, "Fragment PageLoad stop load, roomId=${mRoomInfo.roomId}")
        isPageLoaded = false
        destroy(isScrolling)
    }

    /**
     * Destroy
     *
     * @param isScrolling
     * @return
     */
    private fun destroy(isScrolling: Boolean): Boolean {
        mBinding.root.removeCallbacks(timerRoomEndRun)
        releaseCountdown()
        destroyService()
        return destroyRtcEngine(isScrolling)
    }

    /**
     * On back pressed
     *
     */
    private fun onBackPressed() {
        if (isRoomOwner) {
            showEndRoomDialog()
        } else {
            activity?.finish()
        }
    }

    //================== UI Operation ===============

    /**
     * Init view
     *
     */
    private fun initView() {
        initLivingEndLayout()
        initTopLayout()
        initBottomLayout()
        initMessageLayout()

        // Render host video
        // initVideoView()
    }

    /**
     * Init video view
     *
     */
    private fun initVideoView() {
        activity?.let {
            if (isRoomOwner) {
                setupLocalVideo(
                    VideoSwitcher.VideoCanvasContainer(
                        it,
                        mBinding.videoLinkingLayout.videoContainer,
                        0
                    )
                )
            } else {
                mRtcVideoSwitcher.setupRemoteVideo(
                    mMainRtcConnection,
                    VideoSwitcher.VideoCanvasContainer(
                        it,
                        mBinding.videoLinkingLayout.videoContainer,
                        mRoomInfo.ownerId.toInt()
                    )
                )
            }
        }
    }

    /**
     * Init living end layout
     *
     */
    private fun initLivingEndLayout() {
        val livingEndLayout = mBinding.livingEndLayout
        livingEndLayout.root.isVisible = ROOM_AVAILABLE_DURATION < (TimeUtils.currentTimeMillis() - mRoomInfo.createdAt.toLong()) && !isRoomOwner && !mRoomInfo.isRobotRoom()
        livingEndLayout.tvUserName.text = mRoomInfo.ownerName
        Glide.with(this@LiveDetailFragment)
            .load(mRoomInfo.getOwnerAvatarFullUrl())
            .error(R.mipmap.show_default_avatar)
            .into(livingEndLayout.ivAvatar)
        livingEndLayout.ivClose.setOnClickListener {
            activity?.finish()
        }
    }

    /**
     * Init top layout
     *
     */
    private fun initTopLayout() {
        val topLayout = mBinding.topLayout
        Glide.with(this)
            .load(mRoomInfo.getOwnerAvatarFullUrl())
            .error(R.mipmap.show_default_avatar)
            .into(topLayout.ivOwnerAvatar)
        topLayout.tvRoomName.text = mRoomInfo.roomName
        topLayout.tvRoomId.text = getString(R.string.show_room_id, mRoomInfo.roomId)
        topLayout.tvUserCount.text = mRoomInfo.roomUserCount.toString()
        topLayout.ivClose.setOnClickListener { onBackPressed() }
    }

    /**
     * Start top layout timer
     *
     */
    private fun startTopLayoutTimer() {
        val topLayout = mBinding.topLayout
        val dataFormat =
            SimpleDateFormat("HH:mm:ss").apply { timeZone = TimeZone.getTimeZone("GMT") }
        Log.d(
            TAG,
            "TopTimer curr=${TimeUtils.currentTimeMillis()}, createAt=${mRoomInfo.createdAt.toLong()}, diff=${TimeUtils.currentTimeMillis() - mRoomInfo.createdAt.toLong()}, time=${
                dataFormat.format(Date(TimeUtils.currentTimeMillis() - mRoomInfo.createdAt.toLong()))
            }"
        )
        topLayout.tvTimer.post(object : Runnable {
            override fun run() {
                topLayout.tvTimer.text =
                    dataFormat.format(Date(TimeUtils.currentTimeMillis() - mRoomInfo.createdAt.toLong()))
                topLayout.tvTimer.postDelayed(this, 1000)
                topLayout.tvTimer.tag = this
            }
        })
    }

    /**
     * Init bottom layout
     *
     */
    private fun initBottomLayout() {
        val bottomLayout = mBinding.bottomLayout
        bottomLayout.layoutChat.setOnClickListener {
            showMessageInputDialog()
        }
        bottomLayout.ivSetting.setOnClickListener {
            if (interactionInfo != null && interactionInfo!!.interactStatus == ShowInteractionStatus.pking.value && isRoomOwner) {
                showPKSettingsDialog()
            } else {
                showSettingDialog()
            }
        }
        bottomLayout.ivBeauty.setOnClickListener {
            showBeautyDialog()
        }
        bottomLayout.ivMusic.setOnClickListener {
            showMusicEffectDialog()
        }
        bottomLayout.ivLinking.setOnClickListener {
            if (mRoomInfo.isRobotRoom()) {
                ToastUtils.showToast(context?.getString(R.string.show_tip1))
                return@setOnClickListener
            }
            bottomLayout.vLinkingDot.isVisible = false
            if (!isRoomOwner) {
                if (!(interactionInfo != null && interactionInfo!!.userId == UserManager.getInstance().user.id.toString())) {
                    prepareLinkingMode()
                    mService.createMicSeatApply(mRoomInfo.roomId, {
                        // success
                        mLinkDialog.setOnApplySuccess()
                    })
                }
            }
            showLinkingDialog()
        }
        bottomLayout.flPK.setOnClickListener {
            bottomLayout.vPKDot.isVisible = false
            if (isRoomOwner) {
                showPKDialog()
            }
        }
        refreshBottomLayout()
    }

    /**
     * Init message layout
     *
     */
    private fun initMessageLayout() {
        val messageLayout = mBinding.messageLayout
        mMessageAdapter =
            object : BindingSingleAdapter<ShowMessage, ShowLiveDetailMessageItemBinding>() {
                override fun onBindViewHolder(
                    holder: BindingViewHolder<ShowLiveDetailMessageItemBinding>, position: Int
                ) {
                    val item = getItem(position) ?: return
                    holder.binding.text.text = SpannableStringBuilder().append(
                        "${item.userName}: ",
                        ForegroundColorSpan(Color.parseColor("#A6C4FF")),
                        SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE
                    ).append(
                        item.message,
                        ForegroundColorSpan(Color.WHITE),
                        SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
            }
        (messageLayout.rvMessage.layoutManager as LinearLayoutManager).let {
            it.stackFromEnd = true
        }
        messageLayout.rvMessage.adapter = mMessageAdapter
    }

    /**
     * Refresh bottom layout
     *
     */
    private fun refreshBottomLayout() {
        val context = context ?: return
        val bottomLayout = mBinding.bottomLayout
        if (isRoomOwner) {
            bottomLayout.ivSetting.isVisible = true
            bottomLayout.ivMusic.isVisible = true
            bottomLayout.ivBeauty.isVisible = true

            if (isPKing()) {
                bottomLayout.ivLinking.isEnabled = false
                bottomLayout.flPK.isEnabled = true
                bottomLayout.flPK.isVisible = true
            } else if (isLinking()) {
                bottomLayout.flPK.isEnabled = false
                bottomLayout.flLinking.isVisible = true
                bottomLayout.ivLinking.imageTintList = null
                mSettingDialog.apply {
                    resetSettingsItem(false)
                }
            } else {
                bottomLayout.flPK.isEnabled = true
                bottomLayout.ivLinking.isEnabled = true
                bottomLayout.flPK.isVisible = true
                bottomLayout.flLinking.isVisible = true
                bottomLayout.ivLinking.imageTintList =
                    ColorStateList.valueOf(context.resources.getColor(R.color.grey_7e))
                mSettingDialog.apply {
                    resetSettingsItem(false)
                }
            }

        } else {

            bottomLayout.ivSetting.isVisible = true
            bottomLayout.flPK.isVisible = false


            if (isPKing()) {
                bottomLayout.ivMusic.isVisible = false
                bottomLayout.ivBeauty.isVisible = false
                bottomLayout.flLinking.isVisible = false
            } else if (isLinking()) {
                if (isMeLinking()) {
                    bottomLayout.ivMusic.isVisible = false
                    bottomLayout.ivBeauty.isVisible = false

                    bottomLayout.flLinking.isVisible = true
                    bottomLayout.ivLinking.imageTintList = null
                } else {
                    bottomLayout.ivMusic.isVisible = false
                    bottomLayout.ivBeauty.isVisible = false
                    bottomLayout.flLinking.isVisible = false
                }
            } else {
                bottomLayout.ivMusic.isVisible = false
                bottomLayout.ivBeauty.isVisible = false

                bottomLayout.flLinking.isVisible = true
                bottomLayout.ivLinking.imageTintList =
                    ColorStateList.valueOf(context.resources.getColor(R.color.grey_7e))
            }
        }
    }

    /**
     * Show message input dialog
     *
     */
    private fun showMessageInputDialog() {
        TextInputDialog(requireContext())
            .setMaxInput(80)
            .setOnInsertHeightChangeListener {
                mBinding.messageLayout.root.layoutParams =
                    (mBinding.messageLayout.root.layoutParams as MarginLayoutParams).apply {
                        bottomMargin = it
                    }
            }
            .setOnSentClickListener { dialog, msg ->
                mService.sendChatMessage(mRoomInfo.roomId, msg)
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Refresh top user count
     *
     * @param count
     */
    private fun refreshTopUserCount(count: Int) =
        runOnUiThread { mBinding.topLayout.tvUserCount.text = count.toString() }

    /**
     * Change statistic visible
     *
     */
    private fun changeStatisticVisible() {
        val visible = !mBinding.topLayout.tlStatistic.isVisible
        changeStatisticVisible(visible)
    }

    /**
     * Change statistic visible
     *
     * @param visible
     */
    private fun changeStatisticVisible(visible: Boolean) {
        val topBinding = mBinding.topLayout
        topBinding.tlStatistic.isVisible = visible
        topBinding.ivStatisticClose.isVisible = visible
        refreshStatisticInfo(0, 0)
        topBinding.ivStatisticClose.setOnClickListener {
            topBinding.tlStatistic.isVisible = false
            topBinding.ivStatisticClose.isVisible = false
        }
    }

    /**
     * Refresh statistic info
     *
     * @param upLinkBps
     * @param downLinkBps
     * @param audioBitrate
     * @param audioLossPackage
     * @param cpuAppUsage
     * @param cpuTotalUsage
     * @param encodeVideoSize
     * @param receiveVideoSize
     * @param encodeFps
     * @param receiveFPS
     * @param downDelay
     * @param upLossPackage
     * @param downLossPackage
     * @param upBitrate
     * @param downBitrate
     * @param codecType
     */
    private fun refreshStatisticInfo(
        upLinkBps: Int? = null, downLinkBps: Int? = null,
        encodeVideoSize: Size? = null, receiveVideoSize: Size? = null,
        encodeFps: Int? = null, receiveFPS: Int? = null,
        downDelay: Int? = null,
        upLossPackage: Int? = null, downLossPackage: Int? = null,
        upBitrate: Int? = null, downBitrate: Int? = null,
    ) {
        activity ?: return
        val topBinding = mBinding.topLayout
        val statisticBinding = topBinding.tlStatistic
        val visible = statisticBinding.isVisible
        if (!visible) {
            return
        }
        encodeVideoSize?.let { topBinding.tvEncodeResolution.text = getString(R.string.show_statistic_encode_resolution, "${it.height}x${it.width}") }
        if (topBinding.tvEncodeResolution.text.isEmpty()) topBinding.tvEncodeResolution.text = getString(R.string.show_statistic_encode_resolution, "--")
        receiveVideoSize?.let { topBinding.tvReceiveResolution.text = getString(R.string.show_statistic_receive_resolution, "${it.height}x${it.width}") }
        if (topBinding.tvReceiveResolution.text.isEmpty()) topBinding.tvReceiveResolution.text = getString(R.string.show_statistic_receive_resolution, "--")
        encodeFps?.let { topBinding.tvStatisticEncodeFPS.text = getString(R.string.show_statistic_encode_fps, it.toString()) }
        if (topBinding.tvStatisticEncodeFPS.text.isEmpty()) topBinding.tvStatisticEncodeFPS.text = getString(R.string.show_statistic_encode_fps, "--")
        receiveFPS?.let { topBinding.tvStatisticReceiveFPS.text = getString(R.string.show_statistic_receive_fps, it.toString()) }
        if (topBinding.tvStatisticReceiveFPS.text.isEmpty()) topBinding.tvStatisticReceiveFPS.text = getString(R.string.show_statistic_receive_fps, "--")
        downDelay?.let { topBinding.tvStatisticDownDelay.text = getString(R.string.show_statistic_delay, it.toString()) }
        if (topBinding.tvStatisticDownDelay.text.isEmpty()) topBinding.tvStatisticDownDelay.text = getString(R.string.show_statistic_delay, "--")
        upLossPackage?.let { topBinding.tvStatisticUpLossPackage.text = getString(R.string.show_statistic_up_loss_package, it.toString()) }
        if (topBinding.tvStatisticUpLossPackage.text.isEmpty()) topBinding.tvStatisticUpLossPackage.text = getString(R.string.show_statistic_up_loss_package, "--")
        downLossPackage?.let { topBinding.tvStatisticDownLossPackage.text = getString(R.string.show_statistic_down_loss_package, it.toString()) }
        if (topBinding.tvStatisticDownLossPackage.text.isEmpty()) topBinding.tvStatisticDownLossPackage.text = getString(R.string.show_statistic_down_loss_package, "--")
        upBitrate?.let { topBinding.tvStatisticUpBitrate.text = getString(R.string.show_statistic_up_bitrate, it.toString()) }
        if (topBinding.tvStatisticUpBitrate.text.isEmpty()) topBinding.tvStatisticUpBitrate.text = getString(R.string.show_statistic_up_bitrate, "--")
        downBitrate?.let { topBinding.tvStatisticDownBitrate.text = getString(R.string.show_statistic_down_bitrate, it.toString()) }
        if (topBinding.tvStatisticDownBitrate.text.isEmpty()) topBinding.tvStatisticDownBitrate.text = getString(R.string.show_statistic_down_bitrate, "--")
        topBinding.tvStatisticUpNet.isVisible = !isAudioOnlyMode
        upLinkBps?.let { topBinding.tvStatisticUpNet.text = getString(R.string.show_statistic_up_net_speech, (it / 8192).toString()) }
        if (topBinding.tvStatisticUpNet.text.isEmpty()) topBinding.tvStatisticUpNet.text = getString(R.string.show_statistic_up_net_speech, "--")
        topBinding.tvStatisticDownNet.isVisible = !isAudioOnlyMode
        downLinkBps?.let { topBinding.tvStatisticDownNet.text = getString(R.string.show_statistic_down_net_speech, (it / 8192).toString()) }
        if (topBinding.tvStatisticDownNet.text.isEmpty()) topBinding.tvStatisticDownNet.text = getString(R.string.show_statistic_down_net_speech, "--")
        topBinding.tvQuickStartTime.isVisible = true
        if (isRoomOwner) {
            topBinding.tvQuickStartTime.text = getString(R.string.show_statistic_quick_start_time, "--")
        } else {
            topBinding.tvQuickStartTime.text = getString(R.string.show_statistic_quick_start_time,
                mRtcVideoSwitcher.getFirstVideoFrameTime())
        }
        topBinding.tvStatisticDeviceGrade.isVisible = true
        val score = mRtcEngine.queryDeviceScore()
        if (score >= 90) {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.show_device_grade, getString(R.string.show_setting_preset_device_high)) + "（$score）"
        } else if (score >= 75) {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.show_device_grade, getString(R.string.show_setting_preset_device_medium)) + "（$score）"
        } else {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.show_device_grade, getString(R.string.show_setting_preset_device_low)) + "（$score）"
        }
        topBinding.tvStatisticH265.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticH265.text = getString(R.string.show_statistic_h265, getString(R.string.show_setting_opened))
        } else {
            topBinding.tvStatisticH265.text = getString(R.string.show_statistic_h265, "--")
        }
        topBinding.tvStatisticSR.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticSR.text = getString(R.string.show_statistic_sr, "--")
        } else {
            topBinding.tvStatisticSR.text = getString(R.string.show_statistic_sr, if (VideoSetting.getCurrAudienceEnhanceSwitch()) getString(R.string.show_setting_opened) else getString(R.string.show_setting_closed))
        }
        topBinding.tvStatisticPVC.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticPVC.text = getString(R.string.show_statistic_pvc,
                if (VideoSetting.getCurrBroadcastSetting().video.PVC)
                    getString(R.string.show_setting_opened)
                else
                    getString(R.string.show_setting_closed)
            )
        } else {
            topBinding.tvStatisticPVC.text = getString(R.string.show_statistic_pvc, "--")
        }

        topBinding.tvStatisticLowStream.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticLowStream.text =
                getString(R.string.show_statistic_low_stream,
                    if (VideoSetting.getCurrLowStreamSetting() == null)
                        getString(R.string.show_setting_closed)
                    else
                        getString(R.string.show_setting_opened)
                )
        } else {
            topBinding.tvStatisticLowStream.text = getString(R.string.show_statistic_low_stream, "--")
        }

        topBinding.tvStatisticSVC.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticSVC.text = getString(R.string.show_statistic_svc,
                if (VideoSetting.getCurrLowStreamSetting()?.SVC == true)
                    getString(R.string.show_setting_opened)
                else getString(R.string.show_setting_closed)
            )
        } else {
            topBinding.tvStatisticSVC.text = getString(R.string.show_statistic_svc, "--")
        }
    }

    /**
     * Refresh view detail layout
     *
     * @param status
     */
    private fun refreshViewDetailLayout(status: Int) {
        when (status) {
            ShowInteractionStatus.idle.value -> {
                if (interactionInfo?.interactStatus == ShowInteractionStatus.onSeat.value) {
                    ToastUtils.showToast(R.string.show_link_is_stopped)
                } else if (interactionInfo?.interactStatus == ShowInteractionStatus.pking.value) {
                    ToastUtils.showToast(R.string.show_pk_is_stopped)
                }

                mBinding.videoLinkingAudienceLayout.root.isVisible = false
                mBinding.videoPKLayout.root.isVisible = false
                mBinding.videoLinkingLayout.root.isVisible = true
            }
            ShowInteractionStatus.onSeat.value -> {
                mBinding.videoPKLayout.root.isVisible = false
                mBinding.videoLinkingLayout.root.isVisible = true
                mBinding.videoLinkingAudienceLayout.root.isVisible = true
            }
            ShowInteractionStatus.pking.value -> {
                mBinding.videoLinkingLayout.root.isVisible = false
                mBinding.videoLinkingAudienceLayout.root.isVisible = false
                mBinding.videoPKLayout.root.isVisible = true
            }
        }
    }

    /**
     * Refresh p k time count
     *
     */
    private fun refreshPKTimeCount() {
        if (interactionInfo != null && interactionInfo!!.interactStatus == ShowInteractionStatus.pking.value) {
            if (mPKCountDownLatch != null) {
                mPKCountDownLatch!!.cancel()
                mPKCountDownLatch = null
            }
            mPKCountDownLatch = object : CountDownTimer(120 * 1000 - 1, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val min: Long = (millisUntilFinished / 1000) / 60
                    val sec: Long = (millisUntilFinished / 1000) % 60
                    activity ?: return
                    mBinding.videoPKLayout.iPKTimeText.text =
                        getString(R.string.show_count_time_for_pk, min.toString(), sec.toString())
                }

                override fun onFinish() {
                    mService.stopInteraction(mRoomInfo.roomId, interactionInfo!!)
                }
            }.start()
        } else {
            if (mPKCountDownLatch != null) {
                mPKCountDownLatch!!.cancel()
                mPKCountDownLatch = null
            }
        }
    }

    /**
     * Refresh mic mute status
     *
     */
    private fun refreshMicMuteStatus() {
        if (interactionInfo == null) return
        if (interactionInfo!!.interactStatus == ShowInteractionStatus.onSeat.value) {
            mBinding.videoLinkingAudienceLayout.userName.isActivated = !interactionInfo!!.muteAudio
        } else if (interactionInfo!!.interactStatus == ShowInteractionStatus.pking.value) {
            mBinding.videoPKLayout.userNameA.isActivated = !interactionInfo!!.ownerMuteAudio
            mBinding.videoPKLayout.userNameB.isActivated = !interactionInfo!!.muteAudio
        }
    }

    /**
     * Insert message item
     *
     * @param msg
     */
    private fun insertMessageItem(msg: ShowMessage) = runOnUiThread {
        mMessageAdapter?.let {
            it.insertLast(msg)
            mBinding.messageLayout.rvMessage.scrollToPosition(it.itemCount - 1)
        }
    }

    /**
     * Show setting dialog
     *
     */
    private fun showSettingDialog() {
        mSettingDialog.apply {
            setHostView(isRoomOwner || isMeLinking())
            if (isMeLinking()) {
                resetSettingsItem(interactionInfo!!.muteAudio)
            }
            setOnItemActivateChangedListener { _, itemId, activated ->
                when (itemId) {
                    SettingDialog.ITEM_ID_CAMERA -> mRtcEngine.switchCamera()
                    SettingDialog.ITEM_ID_QUALITY -> showPictureQualityDialog(this)
                    SettingDialog.ITEM_ID_VIDEO -> {
                        if (activity is LiveDetailActivity){
                            (activity as LiveDetailActivity).toggleSelfVideo(activated, callback = {
                                enableLocalVideo(activated)
                                mPKSettingsDialog.resetItemStatus(LivePKSettingsDialog.ITEM_ID_CAMERA, activated)
                            })
                        }
                    }
                    SettingDialog.ITEM_ID_MIC -> {
                        if (activity is LiveDetailActivity){
                            (activity as LiveDetailActivity).toggleSelfAudio(activated, callback = {
                                if (!isRoomOwner) {
                                    mService.muteAudio(mRoomInfo.roomId, !activated, interactionInfo!!.userId)
                                } else {
                                    enableLocalAudio(activated)
                                }
                            })
                        }
                    }
                    SettingDialog.ITEM_ID_STATISTIC -> changeStatisticVisible()
                    SettingDialog.ITEM_ID_SETTING -> {
                        if (AgoraApplication.the().isDebugModeOpen) {
                            if (isHostView()) showDebugModeDialog() else showAudienceDebugModeDialog()
                        } else {
                            if (isHostView()) showAdvanceSettingDialog() else AdvanceSettingAudienceDialog(context).show()
                        }
                    }
                }
            }
            show()
        }
    }

    /**
     * Show advance setting dialog
     *
     */
    private fun showAdvanceSettingDialog() {
        AdvanceSettingDialog(requireContext(), mMainRtcConnection).apply {
            setItemShowTextOnly(AdvanceSettingDialog.ITEM_ID_SWITCH_QUALITY_ENHANCE, true)
            setItemShowTextOnly(AdvanceSettingDialog.ITEM_ID_SWITCH_BITRATE_SAVE, true)
            show()
        }
    }

    /**
     * Show picture quality dialog
     *
     * @param parentDialog
     */
    private fun showPictureQualityDialog(parentDialog: SettingDialog) {
        PictureQualityDialog(requireContext()).apply {
            setOnQualitySelectListener { _, _, size ->
                mRtcEngine.setCameraCapturerConfiguration(
                    CameraCapturerConfiguration(
                        CameraCapturerConfiguration.CaptureFormat(
                            size.width,
                            size.height,
                            15
                        )
                    )
                )
            }

            setOnShowListener { parentDialog.dismiss() }
            setOnDismissListener { parentDialog.show() }
            show()
        }
    }

    /**
     * Show beauty dialog
     *
     */
    private fun showBeautyDialog() {
        BeautyDialog(requireContext()).apply {
            setBeautyProcessor(mBeautyProcessor)
            show()
        }
    }

    /**
     * Show end room dialog
     *
     */
    private fun showEndRoomDialog() {
        AlertDialog.Builder(requireContext(), R.style.show_alert_dialog)
            .setTitle(R.string.show_tip)
            .setMessage(R.string.show_live_end_room_or_not)
            .setPositiveButton(R.string.show_setting_advance_ok) { dialog, id ->
                activity?.finish()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.show_setting_cancel) { dialog, id ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Show music effect dialog
     *
     */
    private fun showMusicEffectDialog() {
        mMusicEffectDialog.setOnItemSelectedListener { musicEffectDialog, itemId ->
            when (itemId) {
                MusicEffectDialog.ITEM_ID_BACK_MUSIC_NONE -> {
                    mRtcVideoSwitcher.stopAudioMixing(mMainRtcConnection)
                }
                MusicEffectDialog.ITEM_ID_BACK_MUSIC_JOY -> {
                    mRtcVideoSwitcher.startAudioMixing(mMainRtcConnection, "/assets/happy.mp3", false, -1)
                }
                MusicEffectDialog.ITEM_ID_BACK_MUSIC_ROMANTIC -> {
                    mRtcVideoSwitcher.startAudioMixing(mMainRtcConnection, "/assets/romantic.mp3", false, -1)
                }
                MusicEffectDialog.ITEM_ID_BACK_MUSIC_JOY2 -> {
                    mRtcVideoSwitcher.startAudioMixing(mMainRtcConnection, "/assets/relax.mp3", false, -1)
                }

                MusicEffectDialog.ITEM_ID_BEAUTY_VOICE_ORIGINAL -> {
                    mRtcEngine.setVoiceConversionPreset(Constants.VOICE_CONVERSION_OFF)
                }
                MusicEffectDialog.ITEM_ID_BEAUTY_VOICE_SWEET -> {
                    mRtcEngine.setVoiceConversionPreset(Constants.VOICE_CHANGER_SWEET)
                }
                MusicEffectDialog.ITEM_ID_BEAUTY_VOICE_ZHONGXIN -> {
                    mRtcEngine.setVoiceConversionPreset(Constants.VOICE_CHANGER_NEUTRAL)
                }
                MusicEffectDialog.ITEM_ID_BEAUTY_VOICE_WENZHONG -> {
                    mRtcEngine.setVoiceConversionPreset(Constants.VOICE_CHANGER_SOLID)
                }
                MusicEffectDialog.ITEM_ID_BEAUTY_VOICE_MOHUAN -> {
                    mRtcEngine.setVoiceConversionPreset(Constants.VOICE_CHANGER_BASS)
                }

                MusicEffectDialog.ITEM_ID_MIXING_NONE -> {
                    mRtcEngine.setAudioEffectPreset(Constants.AUDIO_EFFECT_OFF)
                }
                MusicEffectDialog.ITEM_ID_MIXING_KTV -> {
                    mRtcEngine.setAudioEffectPreset(Constants.ROOM_ACOUSTICS_KTV)
                }
                MusicEffectDialog.ITEM_ID_MIXING_CONCERT -> {
                    mRtcEngine.setAudioEffectPreset(Constants.ROOM_ACOUSTICS_VOCAL_CONCERT)
                }
                MusicEffectDialog.ITEM_ID_MIXING_LUYINPEN -> {
                    mRtcEngine.setAudioEffectPreset(Constants.ROOM_ACOUSTICS_STUDIO)
                }
                MusicEffectDialog.ITEM_ID_MIXING_KONGKUANG -> {
                    mRtcEngine.setAudioEffectPreset(Constants.ROOM_ACOUSTICS_SPACIAL)
                }
            }
        }
        mMusicEffectDialog.show()
    }

    /**
     * Show link settings dialog
     *
     */
    private fun showLinkSettingsDialog() {
        mLinkSettingDialog.apply {
            setAudienceInfo(interactionInfo!!.userName)
            resetSettingsItem(interactionInfo!!.muteAudio)
            setOnItemActivateChangedListener { _, itemId, activated ->
                when (itemId) {
                    LiveLinkAudienceSettingsDialog.ITEM_ID_MIC -> {
                        if (activity is LiveDetailActivity){
                            (activity as LiveDetailActivity).toggleSelfAudio(activated, callback = {
                                mService.muteAudio(mRoomInfo.roomId, !activated, interactionInfo!!.userId)
                            })
                        }
                    }
                    LiveLinkAudienceSettingsDialog.ITEM_ID_STOP_LINK -> {
                        if (interactionInfo != null) {
                            mService.stopInteraction(mRoomInfo.roomId, interactionInfo!!, {
                                // success
                                dismiss()
                            })
                        }
                    }
                }
            }
            show()
        }
    }

    /**
     * Link start time
     */
    private var linkStartTime = 0L

    /**
     * Show linking dialog
     *
     */
    private fun showLinkingDialog() {
        mLinkDialog.setIsRoomOwner(isRoomOwner)
        mLinkDialog.setLinkDialogActionListener(object : OnLinkDialogActionListener {
            override fun onRequestMessageRefreshing(dialog: LiveLinkDialog) {
                mService.getAllMicSeatApplyList(mRoomInfo.roomId, {
                    mLinkDialog.setSeatApplyList(interactionInfo, it)
                })
            }

            override fun onAcceptMicSeatApplyChosen(
                dialog: LiveLinkDialog,
                seatApply: ShowMicSeatApply
            ) {
                if (interactionInfo != null) {
                    ToastUtils.showToast(R.string.show_cannot_accept)
                    return
                }
                linkStartTime = TimeUtils.currentTimeMillis()
                mService.acceptMicSeatApply(mRoomInfo.roomId, seatApply)
            }

            override fun onOnlineAudienceRefreshing(dialog: LiveLinkDialog) {
                mService.getAllUserList(mRoomInfo.roomId, {
                    val list =
                        it.filter { it.userId != UserManager.getInstance().user.id.toString() }
                    mLinkDialog.setSeatInvitationList(list)
                })
            }

            override fun onOnlineAudienceInvitation(dialog: LiveLinkDialog, userItem: ShowUser) {
                if (interactionInfo != null) {
                    ToastUtils.showToast(R.string.show_cannot_invite)
                    return
                }
                mService.createMicSeatInvitation(mRoomInfo.roomId, userItem)
            }

            override fun onStopLinkingChosen(dialog: LiveLinkDialog) {
                if (interactionInfo != null) {
                    mService.stopInteraction(mRoomInfo.roomId, interactionInfo!!, {
                        // success
                    })
                }
            }

            override fun onStopApplyingChosen(dialog: LiveLinkDialog) {
                updateIdleMode()
                mService.cancelMicSeatApply(mRoomInfo.roomId){}
            }
        })

        if (!mLinkDialog.isVisible) {
            val ft = childFragmentManager.beginTransaction()
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            mLinkDialog.show(ft, "LinkDialog")
        }
    }

    /**
     * Show invitation dialog
     *
     */
    private fun showInvitationDialog() {
        prepareLinkingMode()
        val dialog = AlertDialog.Builder(requireContext(), R.style.show_alert_dialog).apply {
            setTitle(getString(R.string.show_ask_for_link, mRoomInfo.ownerName))
            setPositiveButton(R.string.accept) { dialog, _ ->
                if (mLinkInvitationCountDownLatch != null) {
                    mLinkInvitationCountDownLatch!!.cancel()
                    mLinkInvitationCountDownLatch = null
                }
                mService.acceptMicSeatInvitation(mRoomInfo.roomId)
                dialog.dismiss()
            }
            setNegativeButton(R.string.decline) { dialog, _ ->
                updateIdleMode()
                mService.rejectMicSeatInvitation(mRoomInfo.roomId)
                dialog.dismiss()
            }
        }.create()
        dialog.show()
        if (mLinkInvitationCountDownLatch != null) {
            mLinkInvitationCountDownLatch!!.cancel()
            mLinkInvitationCountDownLatch = null
        }
        mLinkInvitationCountDownLatch = object : CountDownTimer(15 * 1000 - 1, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).text =
                    "${getString(R.string.decline)}(" + millisUntilFinished / 1000 + "s)"
            }

            override fun onFinish() {
                mService.rejectMicSeatInvitation(mRoomInfo.roomId)
                dialog.dismiss()
            }
        }.start()
    }

    /**
     * Show p k dialog
     *
     */
    private fun showPKDialog() {
        mPKDialog.setPKDialogActionListener(object : OnPKDialogActionListener {
            override fun onRequestMessageRefreshing(dialog: LivePKDialog) {
                mService.getAllPKUserList({ roomList ->
                    mService.getAllPKInvitationList(mRoomInfo.roomId,true, { invitationList ->
                        mPKDialog.setOnlineBroadcasterList(
                            interactionInfo,
                            roomList,
                            invitationList
                        )
                    })
                })
            }

            override fun onInviteButtonChosen(dialog: LivePKDialog, roomItem: LiveRoomConfig) {
                if (roomItem.isRobotRoom()) {
                    ToastUtils.showToast(context?.getString(R.string.show_tip1))
                    return
                }
                if (isRoomOwner) {
                    val roomDetail = roomItem.convertToShowRoomDetailModel()
                    preparePKingMode(roomDetail.roomId)
                    mService.createPKInvitation(mRoomInfo.roomId, roomDetail)
                }
            }

            override fun onStopPKingChosen(dialog: LivePKDialog) {
                mService.stopInteraction(mRoomInfo.roomId, interactionInfo!!)
            }
        })
        if (!mPKDialog.isVisible) {
            val ft = childFragmentManager.beginTransaction()
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            mPKDialog.show(ft, "PKDialog")
        }
    }

    /**
     * Pk start time
     */
    var pkStartTime = 0L

    /**
     * Show p k invitation dialog
     *
     * @param name
     */
    private fun showPKInvitationDialog(name: String) {
        val dialog = AlertDialog.Builder(requireContext(), R.style.show_alert_dialog).apply {
            setCancelable(false)
            setTitle(getString(R.string.show_ask_for_pk, name))
            setPositiveButton(R.string.accept) { dialog, _ ->
                if (mPKInvitationCountDownLatch != null) {
                    mPKInvitationCountDownLatch!!.cancel()
                    mPKInvitationCountDownLatch = null
                }
                pkStartTime = TimeUtils.currentTimeMillis()
                mService.acceptPKInvitation(mRoomInfo.roomId){}
                dialog.dismiss()
            }
            setNegativeButton(R.string.decline) { dialog, _ ->
                updateIdleMode()
                mService.rejectPKInvitation(mRoomInfo.roomId) { }
                dialog.dismiss()
            }
        }.create()
        dialog.show()
        if (mPKInvitationCountDownLatch != null) {
            mPKInvitationCountDownLatch!!.cancel()
            mPKInvitationCountDownLatch = null
        }
        mPKInvitationCountDownLatch = object : CountDownTimer(15 * 1000 - 1, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).text =
                    "${getString(R.string.decline)}(${millisUntilFinished / 1000}s)"
            }

            override fun onFinish() {
                updateIdleMode()
                mService.rejectPKInvitation(mRoomInfo.roomId) { }
                dialog.dismiss()
            }
        }.start()
    }

    /**
     * Show p k settings dialog
     *
     */
    private fun showPKSettingsDialog() {
        mPKSettingsDialog.apply {
            resetSettingsItem(interactionInfo!!.ownerMuteAudio)
            setOnItemActivateChangedListener { _, itemId, activated ->
                when (itemId) {
                    LivePKSettingsDialog.ITEM_ID_CAMERA -> {
                        if (activity is LiveDetailActivity){
                            (activity as LiveDetailActivity).toggleSelfVideo(activated, callback = {
                                enableLocalVideo(activated)
                                mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_VIDEO, activated)
                            })
                        }

                    }
                    LivePKSettingsDialog.ITEM_ID_SWITCH_CAMERA -> mRtcEngine.switchCamera()
                    LivePKSettingsDialog.ITEM_ID_MIC -> {
                        if (activity is LiveDetailActivity){
                            (activity as LiveDetailActivity).toggleSelfAudio(activated, callback = {
                                mService.muteAudio(mRoomInfo.roomId, !activated, mRoomInfo.ownerId)
                                mRtcEngine.muteLocalAudioStreamEx(!activated, RtcConnection(interactionInfo!!.roomId, UserManager.getInstance().user.id.toInt()))
                            })
                        }
                    }
                    LivePKSettingsDialog.ITEM_ID_STOP_PK -> {
                        if (interactionInfo != null) {
                            mService.stopInteraction(mRoomInfo.roomId, interactionInfo!!, {
                                // success
                                dismiss()
                            })
                        }
                    }
                }
            }
            show()
        }
    }

    //================== Service Operation ===============

    /**
     * Init service with join room
     *
     */
    private fun initServiceWithJoinRoom() {
        mService.joinRoom(mRoomInfo.roomId,
            success = {
                mService.sendChatMessage(mRoomInfo.roomId, getString(R.string.show_live_chat_coming))
                initService()
            },
            error = {
                if ((it as? RoomException)?.currRoomNo == mRoomInfo.roomId) {
                    runOnUiThread {
                        destroy(false)
                        showLivingEndLayout()
                        ShowLogger.d("showLivingEndLayout", "join room error!:${it.message}")
                    }
                }
            })
    }

    /**
     * Init service
     *
     */
    private fun initService() {
        reFetchUserList()
        mService.subscribeReConnectEvent(mRoomInfo.roomId) {
            reFetchUserList()
            reFetchPKInvitationList()
        }
        mService.subscribeCurrRoomEvent(mRoomInfo.roomId) { status, _ ->
            if (status == ShowServiceProtocol.ShowSubscribeStatus.deleted) {
                destroy(false)
                showLivingEndLayout()
                ShowLogger.d("showLivingEndLayout","room delete by owner!")
            }
        }
        mService.subscribeUser(mRoomInfo.roomId) { status, user ->
            reFetchUserList()
            if (status == ShowServiceProtocol.ShowSubscribeStatus.updated && user != null) {
                if (user.status == ShowRoomRequestStatus.waitting.value) {
                    if (isRoomOwner) {
                        mLinkDialog.setSeatInvitationItemStatus(
                            ShowUser(
                                user.userId,
                                user.avatar,
                                user.userName,
                                user.status
                            )
                        )
                    } else if (user.userId == UserManager.getInstance().user.id.toString()) {
                        showInvitationDialog()
                    }
                } else {
                    mLinkDialog.setSeatInvitationItemStatus(
                        ShowUser(
                            user.userId,
                            user.avatar,
                            user.userName,
                            user.status
                        )
                    )
                }
            }
        }
        mService.subscribeMessage(mRoomInfo.roomId) { _, showMessage ->
            insertMessageItem(showMessage)
        }
        mService.subscribeMicSeatApply(mRoomInfo.roomId) { _, _ ->
            mService.getAllMicSeatApplyList(mRoomInfo.roomId, { list ->
                if (isRoomOwner) {
                    mBinding.bottomLayout.vLinkingDot.isVisible =
                        list.any { it.status == ShowRoomRequestStatus.waitting.value }
                }
                mLinkDialog.setSeatApplyList(interactionInfo, list)
            })
        }
        mService.subscribeInteractionChanged(mRoomInfo.roomId) { status, info ->
            if (status == ShowServiceProtocol.ShowSubscribeStatus.updated && info != null) {
                if (interactionInfo == null) {
//                    if (deletedPKInvitation != null) {
//                        mService.stopInteraction(mRoomInfo.roomId, info, {
//                            // success
//                        })
//                        deletedPKInvitation = null
//                        return@subscribeInteractionChanged
//                    }
                    interactionInfo = info
                    // UI
                    updateVideoSetting(true)
                    refreshBottomLayout()
                    refreshViewDetailLayout(info.interactStatus)
                    mLinkDialog.setOnSeatStatus(info.userName, info.interactStatus)
                    mPKDialog.setPKInvitationItemStatus(info.userName, info.interactStatus)
                    // RTC
                    updateLinkingMode()
                    updatePKingMode()
                    refreshPKTimeCount()
                } else {
                    interactionInfo = info
                    updateAudioMuteStatus()
                    refreshMicMuteStatus()
                }
            } else {
                // UI
                refreshViewDetailLayout(ShowInteractionStatus.idle.value)
                mLinkDialog.setOnSeatStatus("", null)
                mPKDialog.setPKInvitationItemStatus("", null)
                // RTC
                updateIdleMode()
                interactionInfo = null
                refreshBottomLayout()
                refreshPKTimeCount()
                updateVideoSetting(false)
                onMeLinkingListener?.onMeLinking(false)
            }
        }

        mService.subscribePKInvitationChanged(mRoomInfo.roomId) { status, info ->
            mService.getAllPKUserList({ roomList ->
                mService.getAllPKInvitationList(mRoomInfo.roomId, true, { invitationList ->
                    mPKDialog.setOnlineBroadcasterList(interactionInfo, roomList, invitationList)
                })
            })
            if (status == ShowServiceProtocol.ShowSubscribeStatus.updated && info != null) {
                if (info.status == ShowRoomRequestStatus.waitting.value && info.userId == UserManager.getInstance().user.id.toString()) {
                    isPKCompetition = true
                    preparePKingMode(info.fromRoomId)
                    showPKInvitationDialog(info.fromName)
                }
            } else {
                val curUserId = UserManager.getInstance().user.id.toString()
                if (info != null && (info.userId == curUserId || info.fromUserId == curUserId)) {
//                    deletedPKInvitation = info
                    updateIdleMode()
                    if (interactionInfo != null) {
                        mService.stopInteraction(mRoomInfo.roomId, interactionInfo!!, {
                            // success
                        })
//                        deletedPKInvitation = null
                    }
                }
            }
        }

        mService.getAllInterationList(mRoomInfo.roomId, {
            val interactionInfo = it.getOrNull(0)
            this.interactionInfo = interactionInfo
            if (interactionInfo != null && isRoomOwner) {
                mService.stopInteraction(mRoomInfo.roomId, interactionInfo)
            }
            refreshBottomLayout()
            val isPkMode = interactionInfo?.interactStatus == ShowInteractionStatus.pking.value
            updateVideoSetting(isPkMode)
            if (interactionInfo != null) {
                refreshViewDetailLayout(interactionInfo.interactStatus)
                if (interactionInfo.interactStatus == ShowInteractionStatus.onSeat.value) {
                    updateLinkingMode()
                } else if (interactionInfo.interactStatus == ShowInteractionStatus.pking.value) {
                    updatePKingMode()
                }
            } else {
                refreshViewDetailLayout(ShowInteractionStatus.idle.value)
            }
        })
    }

    /**
     * Re fetch user list
     *
     */
    private fun reFetchUserList() {
        mService.getAllUserList(mRoomInfo.roomId, {
            refreshTopUserCount(it.size)
        })
    }

    /**
     * Re fetch p k invitation list
     *
     */
    private fun reFetchPKInvitationList() {
        mService.getAllPKInvitationList(mRoomInfo.roomId,false, { list ->
            list.forEach {
                if (it.userId == UserManager.getInstance().user.id.toString()
                    && it.status == ShowRoomRequestStatus.waitting.value
                ) {
                    preparePKingMode(it.fromRoomId)
                    showPKInvitationDialog(it.fromName)
                }
            }
        })
    }

    /**
     * Is me linking
     *
     */
    private fun isMeLinking() =
        isLinking() && interactionInfo?.userId == UserManager.getInstance().user.id.toString()

    /**
     * Is linking
     *
     */
    private fun isLinking() = (interactionInfo?.interactStatus
        ?: ShowInteractionStatus.idle.value) == ShowInteractionStatus.onSeat.value

    /**
     * Is p king
     *
     */
    private fun isPKing() = (interactionInfo?.interactStatus
        ?: ShowInteractionStatus.idle.value) == ShowInteractionStatus.pking.value

    /**
     * Destroy service
     *
     */
    private fun destroyService() {
        if (interactionInfo != null &&
            ((interactionInfo!!.interactStatus == ShowInteractionStatus.pking.value) && isRoomOwner)
        ) {
            mService.stopInteraction(mRoomInfo.roomId, interactionInfo!!, {
                mService.leaveRoom(mRoomInfo.roomId)
            }, {
                mService.leaveRoom(mRoomInfo.roomId)
            })
        }else{
            mService.leaveRoom(mRoomInfo.roomId)
        }
    }

    /**
     * Show living end layout
     *
     */
    private fun showLivingEndLayout() {
        if (isRoomOwner) {
            val context = activity ?: return
            AlertDialog.Builder(context, R.style.show_alert_dialog)
                .setView(ShowLivingEndDialogBinding.inflate(LayoutInflater.from(requireContext())).apply {
                    Glide.with(this@LiveDetailFragment)
                        .load(mRoomInfo.ownerAvatar)
                        .into(ivAvatar)
                }.root)
                .setCancelable(false)
                .setPositiveButton(R.string.show_living_end_back_room_list) { dialog, _ ->
                    activity?.finish()
                    dialog.dismiss()
                }
                .show()
        } else {
            mBinding.livingEndLayout.root.isVisible = true
        }
    }

    //================== RTC Operation ===================

    /**
     * Init rtc engine
     *
     * @param isScrolling
     * @param onJoinChannelSuccess
     * @receiver
     */
    private fun initRtcEngine(isScrolling: Boolean, onJoinChannelSuccess: () -> Unit) {
        val eventListener = VideoSwitcher.IChannelEventListener(
            onUserOffline = { uid ->
                if (interactionInfo != null && interactionInfo!!.userId == uid.toString()) {
                    mService.stopInteraction(mRoomInfo.roomId, interactionInfo!!)
                }
            },
            onLocalVideoStateChanged = { state ->
                if (isRoomOwner) {
                    isAudioOnlyMode = state == Constants.LOCAL_VIDEO_STREAM_STATE_STOPPED
                }
            },
            onRemoteVideoStateChanged = { uid, state ->
                if (uid == mRoomInfo.ownerId.toInt()) {
                    isAudioOnlyMode = state == Constants.REMOTE_VIDEO_STATE_STOPPED
                }
            },
            onLocalVideoStats = { stats ->
                runOnUiThread {
                    refreshStatisticInfo(
                        upBitrate = stats.sentBitrate,
                        encodeFps = stats.encoderOutputFrameRate,
                        upLossPackage = stats.txPacketLossRate,
                        encodeVideoSize = Size(stats.encodedFrameWidth, stats.encodedFrameHeight)
                    )
                }
            },
            onRemoteVideoStats = { stats ->
                //setEnhance(stats)
                val isLinkingAudience = isRoomOwner && isLinking() && stats.uid.toString() == interactionInfo?.userId
                if (stats.uid == mRoomInfo.ownerId.toInt() || isLinkingAudience) {
                    runOnUiThread {
                        refreshStatisticInfo(
                            downBitrate = stats.receivedBitrate,
                            receiveFPS = stats.decoderOutputFrameRate,
                            downLossPackage = stats.packetLossRate,
                            receiveVideoSize = Size(stats.width, stats.height),
                            downDelay = stats.delay
                        )
                    }
                }
            },
            onUplinkNetworkInfoUpdated = { info ->
                runOnUiThread {
                    refreshStatisticInfo(
                        upLinkBps = info.video_encoder_target_bitrate_bps
                    )
                }
            },
            onDownlinkNetworkInfoUpdated = { info ->
                runOnUiThread {
                    refreshStatisticInfo(
                        downLinkBps = info.bandwidth_estimation_bps
                    )
                }
            },
            onContentInspectResult = { result ->
                if (result > 1) {
                    ToastUtils.showToast(R.string.show_content)
                }
            },
            onChannelJoined = {
                onJoinChannelSuccess.invoke()
            },
            onFirstRemoteVideoFrame = { uid, width, height, elapsed ->
                if (interactionInfo?.userId == uid.toString()) {
                    if (linkStartTime != 0L) {
                        ShowLogger.d(
                            TAG,
                            "Interaction user first video frame from host accept linking: ${TimeUtils.currentTimeMillis() - linkStartTime}"
                        )
                        linkStartTime = 0L
                    } else {
                        ShowLogger.d(
                            TAG,
                            "Interaction user first video frame from user accept linking: ${TimeUtils.currentTimeMillis() - (interactionInfo?.createdAt?.toLong() ?: 0L)}"
                        )
                    }
                }
            }
        )

        if (activity is LiveDetailActivity){
            (activity as LiveDetailActivity).toggleSelfVideo(isRoomOwner || isMeLinking(), callback = {
                // Render host video
                if (isScrolling || isRoomOwner) {
                    ShowLogger.d("hugo", "joinRoom from scroll")
                    joinChannel(eventListener)
                } else {
                    ShowLogger.d("hugo", "joinRoom from click")
                    mRtcVideoSwitcher.setChannelEvent(mRoomInfo.roomId, UserManager.getInstance().user.id.toInt(), eventListener)
                }
                if (!isRoomOwner) {
                    mRtcEngine.adjustUserPlaybackSignalVolumeEx(mRoomInfo.ownerId.toInt(), 100, mMainRtcConnection)
                }
                initVideoView()
            })
            (activity as LiveDetailActivity).toggleSelfAudio(isRoomOwner || isMeLinking(), callback = {
                // nothing
            })
        }
    }


    /**
     * Destroy rtc engine
     *
     * @param isScrolling
     * @return
     */
    private fun destroyRtcEngine(isScrolling: Boolean): Boolean {
        if (isRoomOwner) mRtcEngine.stopPreview()
        return mRtcVideoSwitcher.leaveChannel(mMainRtcConnection, !isScrolling)
    }

    /**
     * Enable local audio
     *
     * @param enable
     */
    private fun enableLocalAudio(enable: Boolean) {
        mRtcEngine.muteLocalAudioStreamEx(!enable, mMainRtcConnection)
        if (enable) {
            VideoSetting.updateBroadcastSetting(
                inEarMonitoring = VideoSetting.getCurrBroadcastSetting().audio.inEarMonitoring
            )
        }
    }

    /**
     * Enable local video
     *
     * @param enable
     */
    private fun enableLocalVideo(enable: Boolean){
        mRtcEngine.muteLocalVideoStreamEx(!enable, mMainRtcConnection)
        if (enable) {
            mRtcEngine.startPreview()
        } else {
            mRtcEngine.stopPreview()
        }
    }

    /**
     * Join channel
     *
     * @param eventListener
     */
    private fun joinChannel(eventListener: VideoSwitcher.IChannelEventListener) {
        val rtcConnection = mMainRtcConnection

        if (!isRoomOwner && mRtcEngine.queryDeviceScore() < 75) {
            mRtcEngine.setParameters("{\"che.hardware_decoding\": 1}")
            mRtcEngine.setParameters("{\"rtc.video.decoder_out_byte_frame\": true}")
        } else {
            mRtcEngine.setParameters("{\"che.hardware_decoding\": 0}")
        }

        val channelMediaOptions = ChannelMediaOptions()
        channelMediaOptions.clientRoleType =
            if (isRoomOwner) Constants.CLIENT_ROLE_BROADCASTER else Constants.CLIENT_ROLE_AUDIENCE
        channelMediaOptions.autoSubscribeVideo = true
        channelMediaOptions.autoSubscribeAudio = true
        channelMediaOptions.publishCameraTrack = isRoomOwner
        channelMediaOptions.publishMicrophoneTrack = isRoomOwner
        if (!isRoomOwner) {
            channelMediaOptions.audienceLatencyLevel = AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
            mRtcEngine.setParameters("{\"rtc.video.jb_smooth_scene\":1}")
        }
        mRtcVideoSwitcher.joinChannel(
            rtcConnection,
            channelMediaOptions,
            RtcEngineInstance.generalToken(),
            eventListener,
            !isRoomOwner
        )
    }

    /**
     * Setup local video
     *
     * @param container
     */
    private fun setupLocalVideo(container: VideoSwitcher.VideoCanvasContainer) {
        localVideoCanvas?.let {
            if (it.lifecycleOwner == container.lifecycleOwner && it.renderMode == container.renderMode && it.uid == container.uid) {
                val videoView = it.view
                val viewIndex = container.container.indexOfChild(videoView)
                if (viewIndex == container.viewIndex) {
                    return
                }
                (videoView.parent as? ViewGroup)?.removeView(videoView)
                container.container.addView(videoView, container.viewIndex)
                return
            }
        }
        var videoView = container.container.getChildAt(container.viewIndex)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (videoView !is SurfaceView) {
                videoView = SurfaceView(container.container.context)
                container.container.addView(videoView, container.viewIndex)
            }
        } else {
            if (videoView !is TextureView) {
                videoView = TextureView(container.container.context)
                container.container.addView(videoView, container.viewIndex)
            }
        }

        val local = LocalVideoCanvasWrap(
            container.lifecycleOwner,
            videoView, container.renderMode, container.uid
        )
        local.mirrorMode = Constants.VIDEO_MIRROR_MODE_DISABLED
        mRtcEngine.setupLocalVideo(local)
    }

    /**
     * Update video setting
     *
     * @param isPkMode
     */
    private fun updateVideoSetting(isPkMode:Boolean) {
        VideoSetting.setIsPkMode(isPkMode)
        if (isRoomOwner || isMeLinking()) {
            VideoSetting.updateBroadcastSetting(
                when (interactionInfo?.interactStatus) {
                    ShowInteractionStatus.pking.value -> VideoSetting.LiveMode.PK
                    else -> VideoSetting.LiveMode.OneVOne
                },
                isLinkAudience = !isRoomOwner,
                rtcConnection = mMainRtcConnection
            )
        } else {
            VideoSetting.updateAudienceSetting()
        }
    }

    /**
     * Update audio mute status
     *
     */
    private fun updateAudioMuteStatus() {
        if (interactionInfo == null) return
        if (interactionInfo!!.interactStatus == ShowInteractionStatus.onSeat.value) {
            if (interactionInfo!!.userId == UserManager.getInstance().user.id.toString()) {
                enableLocalAudio(!interactionInfo!!.muteAudio)
            }
        } else if (interactionInfo!!.interactStatus == ShowInteractionStatus.pking.value) {
            if (isRoomOwner) {
                enableLocalAudio(!interactionInfo!!.ownerMuteAudio)
            }
        }
    }

    /**
     * Update idle mode
     *
     */
    private fun updateIdleMode() {
        ShowLogger.d(TAG, "Interaction >> updateIdleMode")
        if (interactionInfo?.interactStatus == ShowInteractionStatus.pking.value) {
            mRtcVideoSwitcher.leaveChannel(
                RtcConnection(interactionInfo!!.roomId, UserManager.getInstance().user.id.toInt()),
                isRoomOwner
            )
        } else if (prepareRkRoomId.isNotEmpty()) {
            mRtcVideoSwitcher.leaveChannel(
                RtcConnection(prepareRkRoomId, UserManager.getInstance().user.id.toInt()),
                isRoomOwner
            )
        }
        prepareRkRoomId = ""

        if (isRoomOwner) {
            enableLocalAudio(true)
            activity?.let {
                setupLocalVideo(VideoSwitcher.VideoCanvasContainer(it, mBinding.videoLinkingLayout.videoContainer, 0))
            }
            refreshStatisticInfo(
                receiveVideoSize = Size(0, 0),
                downBitrate = 0
            )
        } else {
            val channelMediaOptions = ChannelMediaOptions()
            val rtcConnection = mMainRtcConnection
            channelMediaOptions.publishCameraTrack = false
            channelMediaOptions.publishMicrophoneTrack = false
            channelMediaOptions.publishCustomAudioTrack = false
            channelMediaOptions.enableAudioRecordingOrPlayout = true
            channelMediaOptions.autoSubscribeVideo = true
            channelMediaOptions.autoSubscribeAudio = true
            channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
            channelMediaOptions.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
            mRtcEngine.updateChannelMediaOptionsEx(channelMediaOptions, rtcConnection)
            activity?.let {
                mRtcVideoSwitcher.setupRemoteVideo(
                    rtcConnection,
                    VideoSwitcher.VideoCanvasContainer(it, mBinding.videoLinkingLayout.videoContainer, mRoomInfo.ownerId.toInt())
                )
            }
            refreshStatisticInfo(
                encodeVideoSize = Size(0, 0),
                upBitrate = 0,
            )
        }
    }

    /**
     * Prepare linking mode
     *
     */
    private fun prepareLinkingMode(){
        ShowLogger.d(TAG, "Interaction >> prepareLinkingMode")

        val channelMediaOptions = ChannelMediaOptions()
        channelMediaOptions.publishCameraTrack = true
        channelMediaOptions.publishMicrophoneTrack = false
        channelMediaOptions.publishCustomAudioTrack = false
        channelMediaOptions.enableAudioRecordingOrPlayout = true
        channelMediaOptions.autoSubscribeAudio = true
        channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER

        (activity as? LiveDetailActivity)?.let {
            it.toggleSelfVideo(true) { hasPermission ->
                if (hasPermission) {
                    mRtcEngine.updateChannelMediaOptionsEx(
                        channelMediaOptions,
                        mMainRtcConnection
                    )
                }
            }
        }
    }

    /**
     * Update linking mode
     *
     */
    private fun updateLinkingMode() {
        if (interactionInfo == null) return
        if (interactionInfo?.interactStatus != ShowInteractionStatus.onSeat.value) return
        val rtcConnection = mMainRtcConnection
        ShowLogger.d(TAG, "Interaction >> updateLinkingMode")

        mBinding.videoLinkingAudienceLayout.userName.text = interactionInfo!!.userName
        mBinding.videoLinkingAudienceLayout.userName.bringToFront()
        mBinding.videoLinkingAudienceLayout.userName.isActivated =
            interactionInfo?.muteAudio?.not() ?: false
        if (isRoomOwner) {
            mBinding.videoLinkingAudienceLayout.videoContainer.setOnClickListener {
                showLinkSettingsDialog()
            }
            enableLocalAudio(true)
            mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_VIDEO, true)
            mPKSettingsDialog.resetItemStatus(LivePKSettingsDialog.ITEM_ID_CAMERA, true)
            enableLocalVideo(true)
            activity?.let {
                setupLocalVideo(
                    VideoSwitcher.VideoCanvasContainer(
                        it,
                        mBinding.videoLinkingLayout.videoContainer,
                        0
                    )
                )
                mRtcVideoSwitcher.setupRemoteVideo(
                    rtcConnection,
                    VideoSwitcher.VideoCanvasContainer(
                        it,
                        mBinding.videoLinkingAudienceLayout.videoContainer,
                        interactionInfo?.userId!!.toInt()
                    )
                )
            }
        } else {
            if (interactionInfo?.userId.equals(UserManager.getInstance().user.id.toString())) {
                onMeLinkingListener?.onMeLinking(true)
                mBinding.videoLinkingAudienceLayout.videoContainer.setOnClickListener {
                    showLinkSettingsDialog()
                }
                mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_VIDEO, true)
                enableLocalAudio(true)
                val channelMediaOptions = ChannelMediaOptions()
                channelMediaOptions.publishCameraTrack = true
                channelMediaOptions.publishMicrophoneTrack = true
                channelMediaOptions.publishCustomAudioTrack = false
                channelMediaOptions.enableAudioRecordingOrPlayout = true
                channelMediaOptions.autoSubscribeVideo = true
                channelMediaOptions.autoSubscribeAudio = true
                channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                if (activity is LiveDetailActivity){
                    (activity as LiveDetailActivity).toggleSelfVideo(true, callback = {
                        if (it){
                            mRtcEngine.updateChannelMediaOptionsEx(channelMediaOptions, rtcConnection)
                            val context = activity ?: return@toggleSelfVideo
                            mRtcVideoSwitcher.setupRemoteVideo(
                                rtcConnection,
                                VideoSwitcher.VideoCanvasContainer(
                                    context,
                                    mBinding.videoLinkingLayout.videoContainer,
                                    mRoomInfo.ownerId.toInt()
                                )
                            )
                            setupLocalVideo(
                                VideoSwitcher.VideoCanvasContainer(
                                    context,
                                    mBinding.videoLinkingAudienceLayout.videoContainer,
                                    0
                                )
                            )
                        }else{
                            mService.stopInteraction(mRoomInfo.roomId, interactionInfo!!)
                        }
                    })
                    (activity as LiveDetailActivity).toggleSelfAudio(true, callback = {
                        // nothing
                    })
                }
            } else {
                activity?.let {
                    mRtcVideoSwitcher.setupRemoteVideo(
                        rtcConnection,
                        VideoSwitcher.VideoCanvasContainer(
                            it,
                            mBinding.videoLinkingAudienceLayout.videoContainer,
                            interactionInfo?.userId!!.toInt()
                        )
                    )
                    mRtcVideoSwitcher.setupRemoteVideo(
                        rtcConnection,
                        VideoSwitcher.VideoCanvasContainer(
                            it,
                            mBinding.videoLinkingLayout.videoContainer,
                            mRoomInfo.ownerId.toInt()
                        )
                    )
                }
            }
        }
    }

    /**
     * Prepare rk room id
     */
    private var prepareRkRoomId = ""

    /**
     * Prepare p king mode
     *
     * @param pkRoomId
     */
    private fun preparePKingMode(pkRoomId: String){
        ShowLogger.d(TAG, "Interaction >> preparePKingMode pkRoomId=$pkRoomId")
        val channelMediaOptions = ChannelMediaOptions()
        channelMediaOptions.publishCameraTrack = false
        channelMediaOptions.publishMicrophoneTrack = false
        channelMediaOptions.publishCustomAudioTrack = false
        channelMediaOptions.autoSubscribeVideo = true
        channelMediaOptions.autoSubscribeAudio = false
        channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
        channelMediaOptions.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_ULTRA_LOW_LATENCY
        channelMediaOptions.isInteractiveAudience = true
        val pkRtcConnection = RtcConnection(
            pkRoomId,
            UserManager.getInstance().user.id.toInt()
        )
        mRtcVideoSwitcher.preJoinChannel(
            pkRtcConnection, channelMediaOptions, RtcEngineInstance.generalToken(), null
        )
        prepareRkRoomId = pkRoomId
    }

    /**
     * Update p king mode
     *
     */
    private fun updatePKingMode() {
        if (interactionInfo == null) return
        if (interactionInfo?.interactStatus != ShowInteractionStatus.pking.value) return
        ShowLogger.d(TAG, "Interaction >> updatePKingMode pkRoomId=${interactionInfo!!.roomId}")
        val eventListener = VideoSwitcher.IChannelEventListener(
            onRemoteVideoStats = { stats ->
                if (isRoomOwner) {
                    activity?.runOnUiThread {
                        refreshStatisticInfo(
                            downBitrate = stats.receivedBitrate,
                            receiveFPS = stats.decoderOutputFrameRate,
                            downLossPackage = stats.packetLossRate,
                            receiveVideoSize = Size(stats.width, stats.height),
                            downDelay = stats.delay
                        )
                    }
                }
            },
            onDownlinkNetworkInfoUpdated = { info ->
                activity?.runOnUiThread {
                    refreshStatisticInfo(downLinkBps = info.bandwidth_estimation_bps)
                }
            },
            onFirstRemoteVideoFrame = { uid, width, height, elapsed ->
                if (interactionInfo?.userId == uid.toString()) {
                    if (pkStartTime != 0L) {
                        ShowLogger.d(
                            TAG,
                            "Interaction user first video frame from host accept pking : ${TimeUtils.currentTimeMillis() - pkStartTime}"
                        )
                        pkStartTime = 0L
                    } else {
                        ShowLogger.d(
                            TAG,
                            "Interaction user first video frame from host accepted pking : ${TimeUtils.currentTimeMillis() - (interactionInfo?.createdAt?.toLong() ?: 0L)}"
                        )
                        pkStartTime = 0L
                    }
                }
            }
        )

        val rtcConnection = mMainRtcConnection
        mBinding.videoPKLayout.userNameA.text = mRoomInfo.ownerName
        mBinding.videoPKLayout.userNameA.isActivated = interactionInfo!!.ownerMuteAudio.not()
        mBinding.videoPKLayout.userNameB.text = interactionInfo!!.userName
        mBinding.videoPKLayout.userNameB.isActivated = interactionInfo!!.muteAudio.not()
        if (isRoomOwner) {
            mBinding.videoPKLayout.iBroadcasterBView.setOnClickListener {
                showPKSettingsDialog()
            }
            activity?.let {
                setupLocalVideo(
                    VideoSwitcher.VideoCanvasContainer(
                        it,
                        mBinding.videoPKLayout.iBroadcasterAView,
                        0,
                        viewIndex = 0
                    )
                )
            }
            enableLocalAudio(true)
            if (isRoomOwner){
                mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_VIDEO, true)
                mPKSettingsDialog.resetItemStatus(LivePKSettingsDialog.ITEM_ID_CAMERA, true)
                enableLocalVideo(true)
            }
            val channelMediaOptions = ChannelMediaOptions()
            channelMediaOptions.publishCameraTrack = false
            channelMediaOptions.publishMicrophoneTrack = false
            channelMediaOptions.publishCustomAudioTrack = false
            channelMediaOptions.autoSubscribeVideo = true
            channelMediaOptions.autoSubscribeAudio = true
            channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
            channelMediaOptions.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_ULTRA_LOW_LATENCY
            channelMediaOptions.isInteractiveAudience = true
            val pkRtcConnection = RtcConnection(
                interactionInfo!!.roomId,
                UserManager.getInstance().user.id.toInt()
            )
            mRtcVideoSwitcher.joinChannel(
                pkRtcConnection, channelMediaOptions, RtcEngineInstance.generalToken(), eventListener, false
            )
            activity?.let {
                mRtcVideoSwitcher.setupRemoteVideo(
                    pkRtcConnection,
                    VideoSwitcher.VideoCanvasContainer(
                        it,
                        mBinding.videoPKLayout.iBroadcasterBView,
                        interactionInfo?.userId!!.toInt(),
                        viewIndex = 0
                    )
                )
            }
        } else {
            val channelMediaOptions = ChannelMediaOptions()
            channelMediaOptions.publishCameraTrack = false
            channelMediaOptions.publishMicrophoneTrack = false
            channelMediaOptions.publishCustomAudioTrack = false
            channelMediaOptions.enableAudioRecordingOrPlayout = true
            channelMediaOptions.autoSubscribeVideo = true
            channelMediaOptions.autoSubscribeAudio = true
            channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
            channelMediaOptions.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
            val pkRtcConnection = RtcConnection(
                interactionInfo!!.roomId,
                UserManager.getInstance().user.id.toInt()
            )
            mRtcVideoSwitcher.joinChannel(
                pkRtcConnection, channelMediaOptions, RtcEngineInstance.generalToken(), eventListener, false
            )
            activity?.let {
                mRtcVideoSwitcher.setupRemoteVideo(
                    pkRtcConnection,
                    VideoSwitcher.VideoCanvasContainer(
                        it,
                        mBinding.videoPKLayout.iBroadcasterBView,
                        interactionInfo?.userId!!.toInt(),
                        viewIndex = 0
                    )
                )
                mRtcVideoSwitcher.setupRemoteVideo(
                    rtcConnection,
                    VideoSwitcher.VideoCanvasContainer(
                        it,
                        mBinding.videoPKLayout.iBroadcasterAView,
                        mRoomInfo.ownerId.toInt(),
                        viewIndex = 0
                    )
                )
            }
        }
    }

    /**
     * Release countdown
     *
     */
    private fun releaseCountdown() {
        if (mLinkInvitationCountDownLatch != null) {
            mLinkInvitationCountDownLatch!!.cancel()
            mLinkInvitationCountDownLatch = null
        }
        if (mPKInvitationCountDownLatch != null) {
            mPKInvitationCountDownLatch!!.cancel()
            mPKInvitationCountDownLatch = null
        }
        if (mPKCountDownLatch != null) {
            mPKCountDownLatch!!.cancel()
            mPKCountDownLatch = null
        }
        (mBinding.topLayout.tvTimer.tag as? Runnable)?.let {
            it.run()
            mBinding.topLayout.tvTimer.removeCallbacks(it)
            mBinding.topLayout.tvTimer.tag = null
        }
    }

    /**
     * On me linking listener
     */
    private var onMeLinkingListener: OnMeLinkingListener? = null

    /**
     * On me linking listener
     *
     * @constructor Create empty On me linking listener
     */
    interface OnMeLinkingListener {
        /**
         * On me linking
         *
         * @param isLinking
         */
        fun onMeLinking(isLinking: Boolean)
    }

    /**
     * Local video canvas wrap
     *
     * @property lifecycleOwner
     * @constructor
     *
     * @param view
     * @param renderMode
     * @param uid
     */
    inner class LocalVideoCanvasWrap constructor(
        val lifecycleOwner: LifecycleOwner,
        view: View,
        renderMode: Int,
        uid: Int
    ) : DefaultLifecycleObserver, VideoCanvas(view, renderMode, uid) {

        init {
            lifecycleOwner.lifecycle.addObserver(this)
            if (localVideoCanvas != this) {
                localVideoCanvas?.release()
                localVideoCanvas = this
            }
        }

        /**
         * On destroy
         *
         * @param owner
         */
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            if (lifecycleOwner == owner) {
                release()
            }
        }

        /**
         * Release
         *
         */
        fun release() {
            lifecycleOwner.lifecycle.removeObserver(this)
            view = null
            mRtcEngine.setupLocalVideo(this)
            localVideoCanvas = null
        }
    }
}