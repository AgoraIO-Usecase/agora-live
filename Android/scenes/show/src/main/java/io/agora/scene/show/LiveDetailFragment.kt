package io.agora.scene.show

import android.content.Context
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.*
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
import io.agora.mediaplayer.IMediaPlayer
import io.agora.mediaplayer.data.MediaPlayerSource
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.LeaveChannelOptions
import io.agora.rtc2.RtcConnection
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration.VIDEO_CODEC_TYPE
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.show.databinding.ShowLiveDetailFragmentBinding
import io.agora.scene.show.databinding.ShowLiveDetailMessageItemBinding
import io.agora.scene.show.databinding.ShowLivingEndDialogBinding
import io.agora.scene.show.debugSettings.DebugAudienceSettingDialog
import io.agora.scene.show.debugSettings.DebugSettingDialog
import io.agora.scene.show.service.ShowInteractionInfo
import io.agora.scene.show.service.ShowInteractionStatus
import io.agora.scene.show.service.ShowInvitationType
import io.agora.scene.show.service.ShowMessage
import io.agora.scene.show.service.ShowMicSeatApply
import io.agora.scene.show.service.ShowMicSeatInvitation
import io.agora.scene.show.service.ShowPKInvitation
import io.agora.scene.show.service.ShowRoomDetailModel
import io.agora.scene.show.service.ShowServiceProtocol
import io.agora.scene.show.service.ShowSubscribeStatus
import io.agora.scene.show.service.ShowUser
import io.agora.scene.show.service.isRobotRoom
import io.agora.scene.show.utils.ShowConstants
import io.agora.scene.show.videoLoaderAPI.OnPageScrollEventHandler
import io.agora.scene.show.videoLoaderAPI.VideoLoader
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
import io.agora.scene.widget.dialog.TopFunctionDialog
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
        private const val EXTRA_CREATE_ROOM = "createRoom"

        /**
         * New instance
         *
         * @param roomDetail
         */
        fun newInstance(
            roomDetail: ShowRoomDetailModel,
            handler: OnPageScrollEventHandler,
            position: Int,
            createRoom: Boolean
        ) = LiveDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(EXTRA_ROOM_DETAIL_INFO, roomDetail)
                putBoolean(EXTRA_CREATE_ROOM, createRoom)
            }
            mHandler = handler
            mPosition = position
        }
    }

    /**
     * M room info
     */
    val mRoomInfo by lazy { (arguments?.getParcelable(EXTRA_ROOM_DETAIL_INFO) as? ShowRoomDetailModel)!! }

    private lateinit var mHandler: OnPageScrollEventHandler
    private var mPosition: Int = 0

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
    private val mService by lazy { ShowServiceProtocol.get() }

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
     * M rtc video loader api
     */
    private val mRtcVideoLoaderApi by lazy { VideoLoader.getImplInstance(mRtcEngine) }

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
        ShowLogger.d("showLivingEndLayout","timer end!")
        destroy(false)
        showLivingEndLayout()
    }

    /**
     * M main rtc connection
     */
    private val mMainRtcConnection by lazy { RtcConnection(mRoomInfo.roomId, UserManager.getInstance().user.id.toInt()) }

    private var mPKEventHandler: IRtcEngineEventHandler? = null

    private var mMicInvitationDialog: AlertDialog?= null

    private var mPKInvitationDialog: AlertDialog?= null

    private val mUserMuteAudioStateMap = mutableMapOf<Int, Boolean>()

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
            startLoadPage()
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
        startLoadPage()
    }

    /**
     * Re load page
     *
     */
    fun onPageLoaded() {
        updatePKingMode()
        refreshPKTimeCount()
    }

    /**
     * Start load page
     *
     * @param isScrolling
     */
    private fun startLoadPage() {
        ShowLogger.d(TAG, "Fragment PageLoad start load, roomId=${mRoomInfo.roomId}")
        isPageLoaded = true
        subscribeMediaTime = SystemClock.elapsedRealtime()
        if (mRoomInfo.isRobotRoom()) {
            initRtcEngine()
            initServiceWithJoinRoom()
        } else {
            val roomLeftTime =
                ShowServiceProtocol.ROOM_AVAILABLE_DURATION - (TimeUtils.currentTimeMillis() - mRoomInfo.createdAt.toLong())
            if (roomLeftTime > 0) {
                mBinding.root.postDelayed(timerRoomEndRun, ShowServiceProtocol.ROOM_AVAILABLE_DURATION)
                initRtcEngine()
                initServiceWithJoinRoom()
            }
        }

        startTopLayoutTimer()
        VideoSetting.isPureMode = mRoomInfo.isPureMode

        if (!isRoomOwner) {
            if (mRoomInfo.isPureMode) {
                Log.d("happy", "updateSRSetting: 111")
                VideoSetting.setCurrAudienceEnhanceSwitch(false)
                VideoSetting.updateSRSetting(VideoSetting.SuperResolution.SR_NONE)
            } else {
                Log.d("happy", "updateSRSetting:333")
                val deviceScore = RtcEngineInstance.rtcEngine.queryDeviceScore()
                val deviceLevel = if (deviceScore >= 90) {
                    VideoSetting.updateSRSetting(SR = VideoSetting.SuperResolution.SR_AUTO)
                    VideoSetting.setCurrAudienceEnhanceSwitch(true)
                    VideoSetting.DeviceLevel.High
                } else if (deviceScore >= 75) {
                    VideoSetting.updateSRSetting(SR = VideoSetting.SuperResolution.SR_AUTO)
                    VideoSetting.setCurrAudienceEnhanceSwitch(true)
                    VideoSetting.DeviceLevel.Medium
                } else {
                    VideoSetting.setCurrAudienceEnhanceSwitch(false)
                    VideoSetting.DeviceLevel.Low
                }
                VideoSetting.updateBroadcastSetting(
                    deviceLevel = deviceLevel,
                    isByAudience = true
                )
            }
        }
        refreshStatisticInfo()
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
            stopLoadPage(false)
            activity?.finish()
        }
    }


    private fun onClickMore() {
        context?.let {
            val dialog = TopFunctionDialog(it)
            dialog.reportContentCallback = {
                ShowConstants.reportContents[mRoomInfo.roomName] = true
                activity?.finish()
            }
            dialog.reportUserCallback = {
                ShowConstants.reportUsers[mRoomInfo.ownerId] = true
                activity?.finish()
            }
            dialog.show()
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
        if (needRender) {
            mRtcVideoLoaderApi.renderVideo(
                VideoLoader.AnchorInfo(
                    mRoomInfo.roomId,
                    mRoomInfo.ownerId.toInt(),
                    RtcEngineInstance.generalToken()
                ),
                UserManager.getInstance().user.id.toInt(),
                VideoLoader.VideoCanvasContainer(
                    viewLifecycleOwner,
                    mBinding.videoLinkingLayout.videoContainer,
                    mRoomInfo.ownerId.toInt()
                )
            )
        }
    }

    /**
     * Init video view
     *
     */
    private fun initVideoView() {
        activity?.let {
            if (isRoomOwner) {
                setupLocalVideo(
                    VideoLoader.VideoCanvasContainer(
                        it,
                        mBinding.videoLinkingLayout.videoContainer,
                        0
                    )
                )
            }
        }
    }

    /**
     * need render
     */
    private var needRender = false

    /**
     * Init anchor video view
     */
    fun initAnchorVideoView(info: VideoLoader.AnchorInfo) : VideoLoader.VideoCanvasContainer? {
        needRender = activity == null
        activity?.let {
            if (interactionInfo != null && interactionInfo!!.interactStatus == ShowInteractionStatus.pking) {
                if (info.channelId == mRoomInfo.roomId) {
                    return VideoLoader.VideoCanvasContainer(
                        viewLifecycleOwner,
                        mBinding.videoPKLayout.iBroadcasterAView,
                        mRoomInfo.ownerId.toInt()
                    )
                } else if (info.channelId == interactionInfo!!.roomId) {
                    return VideoLoader.VideoCanvasContainer(
                        viewLifecycleOwner,
                        mBinding.videoPKLayout.iBroadcasterBView,
                        interactionInfo!!.userId.toInt()
                    )
                }
            } else {
                return VideoLoader.VideoCanvasContainer(
                    viewLifecycleOwner,
                    mBinding.videoLinkingLayout.videoContainer,
                    mRoomInfo.ownerId.toInt()
                )
            }
        }
        return null
    }

    /**
     * Init living end layout
     *
     */
    private fun initLivingEndLayout() {
        val livingEndLayout = mBinding.livingEndLayout
        livingEndLayout.root.isVisible = ShowServiceProtocol.ROOM_AVAILABLE_DURATION < (TimeUtils.currentTimeMillis() - mRoomInfo.createdAt.toLong()) && !isRoomOwner && !mRoomInfo.isRobotRoom()
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
        topLayout.ivMore.setOnClickListener { onClickMore() }
        topLayout.ivMore.isVisible = !isRoomOwner
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
            showSettingDialog()
        }
        bottomLayout.ivBeauty.setOnClickListener {
            showBeautyDialog()
        }
        bottomLayout.ivMusic.setOnClickListener {
            showMusicEffectDialog()
        }
        bottomLayout.ivLinking.setOnClickListener {view ->
            if (mRoomInfo.isRobotRoom()) {
                ToastUtils.showToast(context?.getString(R.string.show_tip1))
                return@setOnClickListener
            }
            if (!isRoomOwner) {
                if (interactionInfo == null
                    || interactionInfo?.interactStatus == ShowInteractionStatus.idle
                    || interactionInfo?.userId != UserManager.getInstance().user.id.toString()
                ) {
                    prepareLinkingMode()
                    mService.createMicSeatApply(mRoomInfo.roomId,
                        success = {
                            // success
                            mLinkDialog.setOnApplySuccess()

                        },
                        error = {
                            ToastUtils.showToast(getString(
                                R.string.show_error_apply_mic_seat, it.message
                            ))
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
            } else if (isLinking()) {
                if (isMeLinking()) {
                    bottomLayout.ivMusic.isVisible = false
                    bottomLayout.ivBeauty.isVisible = false

                    bottomLayout.flLinking.isVisible = true
                    bottomLayout.ivLinking.imageTintList = null
                } else {
                    bottomLayout.ivMusic.isVisible = false
                    bottomLayout.ivBeauty.isVisible = false
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
        val visible = !mBinding.topLayout.flStatistic.isVisible
        changeStatisticVisible(visible)
    }

    /**
     * Change statistic visible
     *
     * @param visible
     */
    private fun changeStatisticVisible(visible: Boolean) {
        val topBinding = mBinding.topLayout
        topBinding.flStatistic.isVisible = visible
        topBinding.tlStatisticSender.isVisible = isRoomOwner || isMeLinking()
        topBinding.tlStatisticReceiver.isVisible = !isRoomOwner || isMeLinking() || isPKing()
        topBinding.tlStatisticOther.isVisible = false
        topBinding.ivStatisticVector.isActivated = false
        refreshStatisticInfo(0, 0)
        topBinding.ivStatisticClose.setOnClickListener {
            topBinding.flStatistic.isVisible = false
        }
        topBinding.ivStatisticVector.setOnClickListener {
            topBinding.ivStatisticVector.isActivated = !topBinding.ivStatisticVector.isActivated
            updateStatisticView()
        }
    }

    private fun updateStatisticView() {
        val topBinding = mBinding.topLayout
        val expand = topBinding.ivStatisticVector.isActivated

        val showSender = isRoomOwner || isMeLinking()
        topBinding.tlStatisticSender.isVisible = showSender

        val showReceiver = !isRoomOwner || isLinking() || isPKing()
        if (showSender) {
            topBinding.tlStatisticReceiver.isVisible = expand && showReceiver
        } else {
            topBinding.tlStatisticReceiver.isVisible = showReceiver
        }

        topBinding.tlStatisticOther.isVisible = expand
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
        encodeType: Int? = null,
        downDelay: Int? = null,
        upLossPackage: Int? = null, downLossPackage: Int? = null,
        upBitrate: Int? = null, downBitrate: Int? = null,
    ) {
        activity ?: return
        val topBinding = mBinding.topLayout
        val visible = topBinding.flStatistic.isVisible
        if (!visible) {
            return
        }

        // sender
        encodeVideoSize?.let { topBinding.tvEncodeResolution.text = getString(R.string.show_statistic_encode_resolution, "${it.height}x${it.width}") }
        if (topBinding.tvEncodeResolution.text.isEmpty()) topBinding.tvEncodeResolution.text = getString(R.string.show_statistic_encode_resolution, "--")
        encodeFps?.let { topBinding.tvStatisticEncodeFPS.text = getString(R.string.show_statistic_encode_fps, it.toString()) }
        if (topBinding.tvStatisticEncodeFPS.text.isEmpty()) topBinding.tvStatisticEncodeFPS.text = getString(R.string.show_statistic_encode_fps, "--")
        upBitrate?.let { topBinding.tvStatisticUpBitrate.text = getString(R.string.show_statistic_up_bitrate, it.toString()) }
        if (topBinding.tvStatisticUpBitrate.text.isEmpty()) topBinding.tvStatisticUpBitrate.text = getString(R.string.show_statistic_up_bitrate, "--")
        upLossPackage?.let { topBinding.tvStatisticUpLossPackage.text = getString(R.string.show_statistic_up_loss_package, it.toString()) }
        if (topBinding.tvStatisticUpLossPackage.text.isEmpty()) topBinding.tvStatisticUpLossPackage.text = getString(R.string.show_statistic_up_loss_package, "--")
        topBinding.tvStatisticUpNet.isVisible = !isAudioOnlyMode
        upLinkBps?.let { topBinding.tvStatisticUpNet.text = getString(R.string.show_statistic_up_net_speech, (it / 8192).toString()) }
        if (topBinding.tvStatisticUpNet.text.isEmpty()) topBinding.tvStatisticUpNet.text = getString(R.string.show_statistic_up_net_speech, "--")

        // receiver
        receiveVideoSize?.let { topBinding.tvReceiveResolution.text = getString(R.string.show_statistic_receive_resolution, "${it.height}x${it.width}") }
        if (topBinding.tvReceiveResolution.text.isEmpty()) topBinding.tvReceiveResolution.text = getString(R.string.show_statistic_receive_resolution, "--")
        receiveFPS?.let { topBinding.tvStatisticReceiveFPS.text = getString(R.string.show_statistic_receive_fps, it.toString()) }
        if (topBinding.tvStatisticReceiveFPS.text.isEmpty()) topBinding.tvStatisticReceiveFPS.text = getString(R.string.show_statistic_receive_fps, "--")
        downDelay?.let { topBinding.tvStatisticDownDelay.text = getString(R.string.show_statistic_delay, it.toString()) }
        if (topBinding.tvStatisticDownDelay.text.isEmpty()) topBinding.tvStatisticDownDelay.text = getString(R.string.show_statistic_delay, "--")
        downLossPackage?.let { topBinding.tvStatisticDownLossPackage.text = getString(R.string.show_statistic_down_loss_package, it.toString()) }
        if (topBinding.tvStatisticDownLossPackage.text.isEmpty()) topBinding.tvStatisticDownLossPackage.text = getString(R.string.show_statistic_down_loss_package, "--")
        downBitrate?.let { topBinding.tvStatisticDownBitrate.text = getString(R.string.show_statistic_down_bitrate, it.toString()) }
        if (topBinding.tvStatisticDownBitrate.text.isEmpty()) topBinding.tvStatisticDownBitrate.text = getString(R.string.show_statistic_down_bitrate, "--")

        topBinding.tvStatisticDownNet.isVisible = !isAudioOnlyMode
        downLinkBps?.let { topBinding.tvStatisticDownNet.text = getString(R.string.show_statistic_down_net_speech, (it / 8192).toString()) }
        if (topBinding.tvStatisticDownNet.text.isEmpty()) topBinding.tvStatisticDownNet.text = getString(R.string.show_statistic_down_net_speech, "--")

        // other
        topBinding.tvLocalUid.text =
            getString(R.string.show_local_uid, UserManager.getInstance().user.id.toString())

        topBinding.tvEncoder.isVisible = isRoomOwner
        encodeType?.let {
            topBinding.tvEncoder.text =
                getString(
                    R.string.show_statistic_encoder, when (it) {
                        VIDEO_CODEC_TYPE.VIDEO_CODEC_H264.value -> "H264"
                        VIDEO_CODEC_TYPE.VIDEO_CODEC_H265.value -> "H265"
                        VIDEO_CODEC_TYPE.VIDEO_CODEC_AV1.value -> "AV1"
                        else -> "--"
                    }
                )
        }
        if (topBinding.tvEncoder.text.isEmpty()) topBinding.tvEncoder.text = getString(R.string.show_statistic_encoder, "--")



        topBinding.trSVCPVC.isVisible = isRoomOwner
        topBinding.tvStatisticSVC.text = getString(R.string.show_statistic_svc,
            if (VideoSetting.getCurrLowStreamSetting()?.SVC == true)
                getString(R.string.show_setting_opened)
            else getString(R.string.show_setting_closed)
        )
        topBinding.tvStatisticPVC.text = getString(R.string.show_statistic_pvc,
            if (VideoSetting.getCurrBroadcastSetting().video.PVC)
                getString(R.string.show_setting_opened)
            else
                getString(R.string.show_setting_closed)
        )

        val score = mRtcEngine.queryDeviceScore()
        topBinding.tvStatisticDeviceGrade.isVisible = true
        if (score >= 90) {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.show_device_grade, getString(R.string.show_setting_preset_device_high)) + "（$score）"
        } else if (score >= 75) {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.show_device_grade, getString(R.string.show_setting_preset_device_medium)) + "（$score）"
        } else {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.show_device_grade, getString(R.string.show_setting_preset_device_low)) + "（$score）"
        }


        topBinding.trStatisticSR.isVisible = !isRoomOwner
        if (isRoomOwner) {
            topBinding.tvStatisticSR.text = getString(R.string.show_statistic_sr, "--")
        } else {
            topBinding.tvStatisticSR.text = getString(R.string.show_statistic_sr, if (VideoSetting.getCurrAudienceEnhanceSwitch()) getString(R.string.show_setting_opened) else getString(R.string.show_setting_closed))
        }


        topBinding.trStatisticLowStream.isVisible = isRoomOwner
        topBinding.tvStatisticLowStream.text =
            getString(R.string.show_statistic_low_stream,
                if (VideoSetting.getCurrLowStreamSetting() == null)
                    getString(R.string.show_setting_closed)
                else if (mRoomInfo.isPureMode)
                    getString(R.string.show_setting_closed)
                else
                    getString(R.string.show_setting_opened)
            )

    }

    /**
     * Refresh view detail layout
     *
     * @param status
     */
    private fun refreshViewDetailLayout(status: Int) {
        ShowLogger.d("interaction","refreshViewDetailLayout: $status")
        when (status) {
            ShowInteractionStatus.idle -> {
                if (interactionInfo?.interactStatus == ShowInteractionStatus.linking) {
                    ToastUtils.showToast(R.string.show_link_is_stopped)
                } else if (interactionInfo?.interactStatus == ShowInteractionStatus.pking) {
                    ToastUtils.showToast(R.string.show_pk_is_stopped)
                }

                mBinding.videoLinkingAudienceLayout.root.isVisible = false
                mBinding.videoPKLayout.root.isVisible = false
                mBinding.videoLinkingLayout.root.isVisible = true
            }
            ShowInteractionStatus.linking -> {
                mBinding.videoPKLayout.root.isVisible = false
                mBinding.videoLinkingLayout.root.isVisible = true
                mBinding.videoLinkingAudienceLayout.root.isVisible = true
            }
            ShowInteractionStatus.pking -> {
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
        if (interactionInfo != null && interactionInfo!!.interactStatus == ShowInteractionStatus.pking) {
            if (mPKCountDownLatch != null) {
                mPKCountDownLatch!!.cancel()
                mPKCountDownLatch = null
            }
            mPKCountDownLatch = object : CountDownTimer((ShowServiceProtocol.PK_AVAILABLE_DURATION - (TimeUtils.currentTimeMillis() - interactionInfo!!.createdAt)).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val min: Long = (millisUntilFinished / 1000) / 60
                    val sec: Long = (millisUntilFinished / 1000) % 60 + 1
                    activity ?: return
                    mBinding.videoPKLayout.iPKTimeText.text =
                        getString(R.string.show_count_time_for_pk, min.toString(), sec.toString())
                }

                override fun onFinish() {
                    if (isRoomOwner) {
                        mService.stopInteraction(mRoomInfo.roomId, error = {
                            ToastUtils.showToast("stop interaction error: ${it.message}")
                        })
                    }
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
                                enableLocalAudio(activated)
                                mLinkSettingDialog.resetSettingsItem(!activated)
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
                    stopAudioMixing()
                }
                MusicEffectDialog.ITEM_ID_BACK_MUSIC_JOY -> {
                    startAudioMixing("https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/ent/music/happy.mp3", false, -1)
                }
                MusicEffectDialog.ITEM_ID_BACK_MUSIC_ROMANTIC -> {
                    startAudioMixing("https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/ent/music/romantic.mp3", false, -1)
                }
                MusicEffectDialog.ITEM_ID_BACK_MUSIC_JOY2 -> {
                    startAudioMixing("https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/ent/music/relax.mp3", false, -1)
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
            setOnItemActivateChangedListener { _, itemId, activated ->
                when (itemId) {
                    LiveLinkAudienceSettingsDialog.ITEM_ID_MIC -> {
                        if (activity is LiveDetailActivity){
                            (activity as LiveDetailActivity).toggleSelfAudio(activated, callback = {
                                enableLocalAudio(activated)
                                mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_MIC, activated)
                            })
                        }
                    }
                    LiveLinkAudienceSettingsDialog.ITEM_ID_STOP_LINK -> {
                        if (interactionInfo != null) {
                            mService.stopInteraction(mRoomInfo.roomId, success = {
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
                seatApply: ShowMicSeatApply,
                view: View
            ) {
                if (interactionInfo != null) {
                    ToastUtils.showToast(R.string.show_cannot_accept)
                    return
                }
                linkStartTime = TimeUtils.currentTimeMillis()
                view.isEnabled = false
                mService.acceptMicSeatApply(
                    mRoomInfo.roomId,
                    seatApply.userId,
                    success = {
                        view.isEnabled = true
                        ToastUtils.showToast("accept apply successfully!")
                    },
                    error = {
                        view.isEnabled = true
                        ToastUtils.showToast(getString(R.string.show_error_accept_mic_seat, it.message))
                    })
            }

            override fun onOnlineAudienceRefreshing(dialog: LiveLinkDialog) {
                mService.getAllUserList(mRoomInfo.roomId, {
                    val list =
                        it.filter { it.userId != UserManager.getInstance().user.id.toString() }
                    mLinkDialog.setSeatInvitationList(list)
                })
            }

            override fun onOnlineAudienceInvitation(dialog: LiveLinkDialog, userItem: ShowUser, view: View) {
                if (interactionInfo != null) {
                    ToastUtils.showToast(R.string.show_cannot_invite)
                    return
                }
                view.isEnabled = false
                mService.createMicSeatInvitation(
                    mRoomInfo.roomId,
                    userItem.userId,
                    success = {
                        view.isEnabled = true
                        ToastUtils.showToast("invite successfully!")
                    },
                    error = {
                        view.isEnabled = true
                        ToastUtils.showToast(getString(R.string.show_error_invite_mic_seat, it.message))
                    })
            }

            override fun onStopLinkingChosen(dialog: LiveLinkDialog, view: View) {
                view.isEnabled = false
                mService.stopInteraction(mRoomInfo.roomId, success = {
                    // success
                    view.isEnabled = true
                }, error = {
                    view.isEnabled = true
                    ToastUtils.showToast("stop interaction error: ${it.message}")
                })
            }

            override fun onStopApplyingChosen(dialog: LiveLinkDialog, view: View) {
                updateIdleMode()
                view.isEnabled = false
                mService.cancelMicSeatApply(mRoomInfo.roomId,
                    success = {
                        view.isEnabled = true
                        mLinkDialog.dismiss()
                        ToastUtils.showToast("cancel apply successfully!")
                    },
                    error = {
                        view.isEnabled = true
                        ToastUtils.showToast(getString(R.string.show_error_cancel_mic_seat, it.message))
                    })
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
    private fun showInvitationDialog(invitation: ShowMicSeatInvitation) {
        if (mMicInvitationDialog?.isShowing == true) {
            return
        }
        prepareLinkingMode()
        mMicInvitationDialog = AlertDialog.Builder(requireContext(), R.style.show_alert_dialog).apply {
            setCancelable(false)
            setTitle(getString(R.string.show_ask_for_link, mRoomInfo.ownerName))
            setPositiveButton(R.string.accept, null)
            setNegativeButton(R.string.decline) { dialog, which ->
                updateIdleMode()
                val button = (dialog as? AlertDialog)?.getButton(which)
                button?.isEnabled = false
                mService.rejectMicSeatInvitation(
                    mRoomInfo.roomId,
                    invitation.id,
                    success = {
                        button?.isEnabled = true
                        dismissMicInvitaionDialog()
                        ToastUtils.showToast("reject invitation successfully!")
                    },
                    error = { error ->
                        button?.isEnabled = true
                        ToastUtils.showToast(getString(R.string.show_error_reject_mic_seat, error.message))
                    }
                )
            }
        }.create()
        mMicInvitationDialog?.setOnShowListener {
            mMicInvitationDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {btn ->
                btn.isEnabled = false
                mService.acceptMicSeatInvitation(
                    mRoomInfo.roomId,
                    invitation.id,
                    success = {
                        btn.isEnabled = true
                        ToastUtils.showToast("accept invitation successfully!")
                        dismissMicInvitaionDialog()
                    },
                    error = { error ->
                        btn.isEnabled = true
                        ToastUtils.showToast(getString(R.string.show_error_accept_mic_seat_invite, error.message))
                    }
                )
            }
        }
        mMicInvitationDialog?.show()
        if (mLinkInvitationCountDownLatch != null) {
            mLinkInvitationCountDownLatch!!.cancel()
            mLinkInvitationCountDownLatch = null
        }
        mLinkInvitationCountDownLatch = object : CountDownTimer(15 * 1000 - 1, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                mMicInvitationDialog?.getButton(DialogInterface.BUTTON_NEGATIVE)?.text =
                    "${getString(R.string.decline)}(" + millisUntilFinished / 1000 + "s)"
            }

            override fun onFinish() {
                mService.rejectMicSeatInvitation(
                    mRoomInfo.roomId,
                    invitation.id,
                    error = { error ->
                        ToastUtils.showToast(getString(R.string.show_error_reject_mic_seat, error.message))
                    }
                )
                dismissMicInvitaionDialog()
            }
        }.start()
    }

    private fun dismissMicInvitaionDialog() {
        mMicInvitationDialog?.dismiss()
        mLinkInvitationCountDownLatch?.cancel()
        mLinkInvitationCountDownLatch = null
    }

    /**
     * Show p k dialog
     *
     */
    private fun showPKDialog() {
        mPKDialog.setPKDialogActionListener(object : OnPKDialogActionListener {
            override fun onRequestMessageRefreshing(dialog: LivePKDialog) {
                mService.getAllPKUserList(mRoomInfo.roomId,
                    success = { pkUserList ->
                        mPKDialog.setOnlineBroadcasterList(
                            interactionInfo,
                            pkUserList.map {
                                LiveRoomConfig(
                                    ShowRoomDetailModel(
                                        it.roomId,
                                        "",
                                        0,
                                        it.userId,
                                        "",
                                        it.avatar,
                                        it.userName,
                                    ),
                                    it.status,
                                    false
                                )
                            }
                        )
                    },
                    error = {
                        ToastUtils.showToast(it.message)
                    })
            }

            override fun onInviteButtonChosen(dialog: LivePKDialog, roomItem: LiveRoomConfig, view: View) {
                if (roomItem.isRobotRoom()) {
                    ToastUtils.showToast(context?.getString(R.string.show_tip1))
                    return
                }
                if (isRoomOwner) {
                    val roomDetail = roomItem.convertToShowRoomDetailModel()
                    preparePKingMode(roomDetail.roomId)
                    view.isEnabled = false
                    mService.createPKInvitation(
                        mRoomInfo.roomId,
                        roomDetail.roomId,
                        success = {
                            view.isEnabled = true
                            ToastUtils.showToast("invite successfully!")
                        },
                        error = {
                            view.isEnabled = true
                            ToastUtils.showToast(getString(R.string.show_error_invite_pk, it.message))
                        })
                }
            }

            override fun onStopPKingChosen(dialog: LivePKDialog, view: View) {
                view.isEnabled = false
                mService.stopInteraction(mRoomInfo.roomId,
                    success = {
                        view.isEnabled = true
                    },
                    error = {
                        view.isEnabled = true
                        ToastUtils.showToast("stop Interaction error: ${it.message}")
                    })
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
    private fun showPKInvitationDialog(name: String, invitation: ShowPKInvitation) {
        if(mPKInvitationDialog?.isShowing == true){
            return
        }
        mPKInvitationDialog = AlertDialog.Builder(requireContext(), R.style.show_alert_dialog).apply {
            setCancelable(false)
            setTitle(getString(R.string.show_ask_for_pk, name))
            setPositiveButton(R.string.accept, null)
            setNegativeButton(R.string.decline) { dialog, which ->
                updateIdleMode()
                val button = (dialog as? AlertDialog)?.getButton(which)
                button?.isEnabled = false
                mService.rejectPKInvitation(
                    mRoomInfo.roomId,
                    invitation.id,
                    success = {
                        button?.isEnabled = true
                        dismissPKInvitationDialog()
                    },
                    error = {
                        button?.isEnabled = true
                        ToastUtils.showToast("reject message failed!")
                    }
                )
                isPKCompetition = false
            }
        }.create()
        mPKInvitationDialog?.setOnShowListener {
            mPKInvitationDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.let { btn ->
                btn.setOnClickListener {
                    pkStartTime = TimeUtils.currentTimeMillis()
                    btn.isEnabled = false
                    mService.acceptPKInvitation(mRoomInfo.roomId, invitation.id, {
                        btn.isEnabled = true
                        ToastUtils.showToast("accept message successfully!")
                        dismissPKInvitationDialog()
                    }) {
                        btn.isEnabled = true
                        ToastUtils.showToast(context?.getString(R.string.show_error_accept_pk, it.message))
                    }
                }
            }
        }
        mPKInvitationDialog?.show()
        if (mPKInvitationCountDownLatch != null) {
            mPKInvitationCountDownLatch!!.cancel()
            mPKInvitationCountDownLatch = null
        }
        mPKInvitationCountDownLatch = object : CountDownTimer(15 * 1000 - 1, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                mPKInvitationDialog?.getButton(DialogInterface.BUTTON_NEGATIVE)?.text =
                    "${getString(R.string.decline)}(${millisUntilFinished / 1000}s)"
            }

            override fun onFinish() {
                updateIdleMode()
                mService.rejectPKInvitation(mRoomInfo.roomId, invitation.id, error = {
                    ToastUtils.showToast(getString(R.string.show_error_reject_pk, it.message))
                })
                dismissPKInvitationDialog()
            }
        }.start()
    }

    private fun dismissPKInvitationDialog() {
        mPKInvitationDialog?.dismiss()
        mPKInvitationCountDownLatch?.cancel()
        mPKInvitationCountDownLatch = null
    }

    /**
     * Show p k settings dialog
     *
     */
    private fun showPKSettingsDialog() {
        mPKSettingsDialog.apply {
            resetSettingsItem(false)
            setPKInfo(interactionInfo!!.userName)
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
                                enableLocalAudio(activated)
                            })
                        }
                    }
                    LivePKSettingsDialog.ITEM_ID_STOP_PK -> {
                        if (interactionInfo != null) {
                            mService.stopInteraction(mRoomInfo.roomId, success = {
                                // success
                                dismiss()
                            }, error = {
                                ToastUtils.showToast("stop interaction error: ${it.message}")
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
        val create = arguments?.getBoolean(EXTRA_CREATE_ROOM, false) ?: false

        if (create) {
            mService.createRoom(
                mRoomInfo.roomId,
                mRoomInfo.roomName,
                mRoomInfo.thumbnailId,
                success = {
                    initService()
                },
                error = {
                    runOnUiThread {
                        destroy(false)
                        // 进房Error
                        showLivingEndLayout(true) // 进房Error
                        ShowLogger.d("showLivingEndLayout", "create room error!:${it.message}")
                    }
                })
        } else {
            mService.joinRoom(mRoomInfo.roomId,
                success = {
                    initService()
                },
                error = {
                    runOnUiThread {
                        destroy(false)
                        // 进房Error
                        showLivingEndLayout(true) // 进房Error
                        ShowLogger.d("showLivingEndLayout", "join room error!:${it.message}")
                    }
                })
        }
    }

    /**
     * Init service
     *
     */
    private fun initService() {
        reFetchUserList()
        mService.subscribeReConnectEvent(mRoomInfo.roomId) {
            context ?: return@subscribeReConnectEvent
            reFetchUserList()
        }
        mService.subscribeCurrRoomEvent(mRoomInfo.roomId) { status, _ ->
            if (status == ShowSubscribeStatus.deleted) {
                destroy(false)
                showLivingEndLayout()
                ShowLogger.d("showLivingEndLayout","room delete by owner!")
            }
        }
        mService.subscribeMicSeatInvitation(mRoomInfo.roomId) { _, invitation ->
            invitation ?: return@subscribeMicSeatInvitation
            context ?: return@subscribeMicSeatInvitation
            if (invitation.type == ShowInvitationType.invitation) {
                showInvitationDialog(invitation)
            }
        }
        mService.subscribeUser(mRoomInfo.roomId) { status, user ->
            reFetchUserList()
        }
        mService.subscribeMessage(mRoomInfo.roomId) { _, showMessage ->
            insertMessageItem(showMessage)
        }
        mService.subscribeMicSeatApply(mRoomInfo.roomId) { _, list ->
            ShowLogger.d("Link","mic seat apply changed")
            mBinding.bottomLayout.vLinkingDot.isVisible = list.isNotEmpty()
            mLinkDialog.setSeatApplyList(interactionInfo, list)
        }
        mService.subscribeInteractionChanged(mRoomInfo.roomId) { status, info ->
            ShowLogger.d("interaction","interaction changed")
            context ?: return@subscribeInteractionChanged
            if (status == ShowSubscribeStatus.updated
                && info != null
                && info.interactStatus != ShowInteractionStatus.idle
            ) {
                ShowLogger.d("interaction", "old interaction: $interactionInfo, new interaction: $info")
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

                dismissMicInvitaionDialog()
                dismissPKInvitationDialog()

            } else {
                ShowLogger.d("interaction","old interaction: $interactionInfo, new interaction: $info")
                // UI
                refreshViewDetailLayout(ShowInteractionStatus.idle)
                mLinkDialog.setOnSeatStatus("", null)
                mPKDialog.setPKInvitationItemStatus("", null)
                // RTC
                updateIdleMode()
                interactionInfo = null
                updateStatisticView()
                refreshBottomLayout()
                refreshPKTimeCount()
                updateVideoSetting(false)
                onMeLinkingListener?.onMeLinking(false)
            }
        }

        mService.subscribePKInvitationChanged(mRoomInfo.roomId) { status, info ->
            ShowLogger.d("pk","pk invitation changed: $status, info:$info")
            info ?: return@subscribePKInvitationChanged
            context ?: return@subscribePKInvitationChanged
            when(info.type){
                ShowInvitationType.invitation -> {
                    isPKCompetition = true
                    preparePKingMode(info.fromRoomId)
                    showPKInvitationDialog(info.fromUserName, info)
                }
                ShowInvitationType.reject -> {
                    isPKCompetition = false
                    updateIdleMode()
                }
            }
        }

        mService.getInteractionInfo(mRoomInfo.roomId, { interactionInfo ->
            ShowLogger.d(TAG,"getInteractionInfo: ${mRoomInfo.roomId}, interactionInfo:$interactionInfo")
            this.interactionInfo = interactionInfo
            if (isRoomOwner) {
                mService.stopInteraction(mRoomInfo.roomId)
            }
            refreshBottomLayout()
            val isPkMode = interactionInfo?.interactStatus == ShowInteractionStatus.pking
            updateVideoSetting(isPkMode)
            if (interactionInfo != null && interactionInfo.interactStatus != ShowInteractionStatus.idle) {
                refreshViewDetailLayout(interactionInfo.interactStatus)
                if (interactionInfo.interactStatus == ShowInteractionStatus.linking) {
                    updateLinkingMode()
                } else if (interactionInfo.interactStatus == ShowInteractionStatus.pking) {
                    updatePKingMode()
                    refreshPKTimeCount()
                }
            } else {
                refreshViewDetailLayout(ShowInteractionStatus.idle)
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
        ?: ShowInteractionStatus.idle) == ShowInteractionStatus.linking

    /**
     * Is p king
     *
     */
    private fun isPKing() = (interactionInfo?.interactStatus
        ?: ShowInteractionStatus.idle) == ShowInteractionStatus.pking

    /**
     * Destroy service
     *
     */
    private fun destroyService() {
        mService.leaveRoom(mRoomInfo.roomId)
    }

    /**
     * Show living end layout
     *
     */
    private fun showLivingEndLayout(fromError: Boolean = false) {
        if (isRoomOwner) {
            val context = activity ?: return
            AlertDialog.Builder(context, R.style.show_alert_dialog)
                .setView(ShowLivingEndDialogBinding.inflate(LayoutInflater.from(requireContext())).apply {
                    if (fromError) {
                        tvTitle.setText(R.string.show_living_end_title_error)
                    }
                    Glide.with(this@LiveDetailFragment)
                        .load(mRoomInfo.getOwnerAvatarFullUrl())
                        .into(ivAvatar)
                }.root)
                .setCancelable(false)
                .setPositiveButton(R.string.show_living_end_back_room_list) { dialog, _ ->
                    activity?.finish()
                    dialog.dismiss()
                }
                .show()
        } else {
            if (fromError) {
                mBinding.livingEndLayout.tvLivingEnd.setText(R.string.show_live_detail_living_end_error)
            }
            mBinding.livingEndLayout.root.isVisible = true
        }
    }

    //================== RTC Operation ===================

    private var quickStartTime = 0L
    private var subscribeMediaTime = 0L

    /**
     * Init rtc engine
     *
     * @param isScrolling
     * @param onJoinChannelSuccess
     * @receiver
     */
    private fun initRtcEngine() {
        val eventListener = object : IRtcEngineEventHandler() {
            override fun onUserJoined(uid: Int, elapsed: Int) {
                super.onUserJoined(uid, elapsed)
                if (uid != mAudioMxingChannel?.localUid) {
                    mRtcEngine.muteRemoteAudioStreamEx(uid, false, mMainRtcConnection)
                }
            }

            override fun onUserOffline(uid: Int, reason: Int) {
                super.onUserOffline(uid, reason)
                if (uid != mAudioMxingChannel?.localUid) {
                    mRtcEngine.muteRemoteAudioStreamEx(uid, true, mMainRtcConnection)
                }
            }

            override fun onAudioPublishStateChanged(
                channel: String?,
                oldState: Int,
                newState: Int,
                elapseSinceLastState: Int
            ) {
                super.onAudioPublishStateChanged(channel, oldState, newState, elapseSinceLastState)
                ShowLogger.d(TAG, "onAudioPublishStateChanged: channel=$channel, oldState=$oldState, newState=$newState, elapseSinceLastState=$elapseSinceLastState")
                runOnUiThread {
                    if (newState == 1) {
                        if (isMeLinking()) {
                            mBinding.videoLinkingAudienceLayout.userName.isActivated = false
                        }
                        mBinding.videoPKLayout.userNameA.isActivated = false
                    } else if (newState == 3) {
                        if (isMeLinking()) {
                            mBinding.videoLinkingAudienceLayout.userName.isActivated = true
                        }
                        mBinding.videoPKLayout.userNameA.isActivated = true
                    }
                }
            }

            override fun onRemoteAudioStateChanged(
                uid: Int,
                state: Int,
                reason: Int,
                elapsed: Int
            ) {
                super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
                ShowLogger.d(TAG, "onRemoteAudioStateChanged: uid=$uid, state=$state, reason=$reason")
                val unmute = state == Constants.REMOTE_AUDIO_STATE_DECODING
                mUserMuteAudioStateMap[uid] = !unmute
                if (isLinking() && uid == interactionInfo?.userId?.toInt()) {
                    runOnUiThread {
                        mBinding.videoLinkingAudienceLayout.userName.isActivated = unmute
                    }
                } else if (isPKing()) {
                    runOnUiThread {
                        mBinding.videoPKLayout.userNameA.isActivated = unmute
                    }
                }
            }

            override fun onLocalVideoStateChanged(
                source: Constants.VideoSourceType?,
                state: Int,
                error: Int
            ) {
                super.onLocalVideoStateChanged(source, state, error)
                if (isRoomOwner) {
                    isAudioOnlyMode = state == Constants.LOCAL_VIDEO_STREAM_STATE_STOPPED
                }
            }

            override fun onRemoteVideoStateChanged(
                uid: Int,
                state: Int,
                reason: Int,
                elapsed: Int
            ) {
                super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
                if (uid == mRoomInfo.ownerId.toInt()) {
                    isAudioOnlyMode = state == Constants.REMOTE_VIDEO_STATE_STOPPED
                }

                if (state == Constants.REMOTE_VIDEO_STATE_DECODING
                    && (reason == Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED || reason == Constants.REMOTE_VIDEO_STATE_REASON_LOCAL_UNMUTED)
                ) {
                    val durationFromSubscribe = SystemClock.elapsedRealtime() - subscribeMediaTime
                    quickStartTime = durationFromSubscribe
                }

                runOnUiThread {
                    if (reason == Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED) {
                        setVideoOverlayVisible(true, uid.toString())
                    } else if (reason == Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED) {
                        setVideoOverlayVisible(false, uid.toString())
                    }
                }
            }

            override fun onLocalVideoStats(
                source: Constants.VideoSourceType,
                stats: LocalVideoStats
            ) {
                super.onLocalVideoStats(source, stats)
                runOnUiThread {
                    refreshStatisticInfo(
                        upBitrate = stats.sentBitrate,
                        encodeFps = stats.encoderOutputFrameRate,
                        upLossPackage = stats.txPacketLossRate,
                        encodeVideoSize = Size(stats.encodedFrameWidth, stats.encodedFrameHeight),
                        encodeType = stats.codecType
                    )
                }
            }

            override fun onRemoteVideoStats(stats: RemoteVideoStats) {
                super.onRemoteVideoStats(stats)
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
            }

            override fun onUplinkNetworkInfoUpdated(info: UplinkNetworkInfo) {
                super.onUplinkNetworkInfoUpdated(info)
                runOnUiThread {
                    refreshStatisticInfo(
                        upLinkBps = info.video_encoder_target_bitrate_bps
                    )
                }
            }

            override fun onDownlinkNetworkInfoUpdated(info: DownlinkNetworkInfo) {
                super.onDownlinkNetworkInfoUpdated(info)
                runOnUiThread {
                    refreshStatisticInfo(
                        downLinkBps = info.bandwidth_estimation_bps
                    )
                }
            }

            override fun onFirstRemoteVideoFrame(uid: Int, width: Int, height: Int, elapsed: Int) {
                super.onFirstRemoteVideoFrame(uid, width, height, elapsed)
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
                runOnUiThread {
                    setVideoOverlayVisible(false, uid.toString())
                }

            }
        }

        if (activity is LiveDetailActivity) {
            (activity as LiveDetailActivity).toggleSelfVideo(isRoomOwner || isMeLinking(), callback = {
                joinChannel(eventListener)
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
        if (isRoomOwner) {
            mRtcEngine.stopPreview()
            mRtcEngine.leaveChannelEx(mMainRtcConnection)
            stopAudioMixing()

            if (isPKing()) {
                mRtcEngine.leaveChannelEx(RtcConnection(interactionInfo!!.roomId, UserManager.getInstance().user.id.toInt()))
            }
            mRtcEngine.setVoiceConversionPreset(Constants.VOICE_CONVERSION_OFF)
            mRtcEngine.setAudioEffectPreset(Constants.AUDIO_EFFECT_OFF)
            RtcEngineInstance.releaseBeautyProcessor()
        } else if (isPKing()) {
            mPKEventHandler?.let {
                mRtcEngine.removeHandlerEx(it, RtcConnection(interactionInfo!!.roomId, UserManager.getInstance().user.id.toInt()))
                mPKEventHandler = null
            }
        }
        return true
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
        setVideoOverlayVisible(!enable, UserManager.getInstance().user.id.toString())
    }

    private fun setVideoOverlayVisible(visible: Boolean, userId: String) {
        if (userId == UserManager.getInstance().user.id.toString()) {
            // Local video state change
            if (isRoomOwner) {
                mBinding.videoLinkingLayout.videoOverlay.isVisible = visible
                mBinding.videoPKLayout.iBroadcasterAViewOverlay.isVisible = visible
            } else {
                mBinding.videoLinkingAudienceLayout.videoOverlay.isVisible = visible
            }
        } else if (userId == mRoomInfo.ownerId) {
            // Room owner video state change
            if (!isRoomOwner) {
                mBinding.videoLinkingLayout.videoOverlay.isVisible = visible
                mBinding.videoPKLayout.iBroadcasterAViewOverlay.isVisible = visible
            }
        } else if (interactionInfo?.userId == userId) {
            // Interaction user video state change
            mBinding.videoLinkingAudienceLayout.videoOverlay.isVisible = visible
            mBinding.videoPKLayout.iBroadcasterBViewOverlay.isVisible = visible
        }
    }

    /**
     * Join channel
     *
     * @param eventListener
     */
    private fun joinChannel(eventListener: IRtcEngineEventHandler) {
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
        channelMediaOptions.autoSubscribeAudio = false
        channelMediaOptions.publishCameraTrack = isRoomOwner
        channelMediaOptions.publishMicrophoneTrack = isRoomOwner
        if (!isRoomOwner) {
            channelMediaOptions.audienceLatencyLevel = AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
        }

        if (isRoomOwner) {
            mRtcEngine.joinChannelEx(RtcEngineInstance.generalToken(), mMainRtcConnection, channelMediaOptions, eventListener)
        } else {
            mRtcEngine.addHandlerEx(eventListener, mMainRtcConnection)
        }
    }

    /**
     * Setup local video
     *
     * @param container
     */
    private fun setupLocalVideo(container: VideoLoader.VideoCanvasContainer) {
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
        ShowLogger.d("interaction","updateVideoSetting: $isPkMode")
        VideoSetting.setIsPkMode(isPkMode)
        if (isRoomOwner || isMeLinking()) {
            VideoSetting.updateBroadcastSetting(
                when (interactionInfo?.interactStatus) {
                    ShowInteractionStatus.pking -> VideoSetting.LiveMode.PK
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
     * Update idle mode
     *
     */
    private fun updateIdleMode() {
        ShowLogger.d(TAG, "Interaction >> updateIdleMode, old interaction:$interactionInfo")
        if (interactionInfo?.interactStatus == ShowInteractionStatus.pking) {
            if (isRoomOwner) {
                mRtcEngine.leaveChannelEx(RtcConnection(interactionInfo!!.roomId, UserManager.getInstance().user.id.toInt()))
            } else {
                mPKEventHandler?.let {
                    mRtcEngine.removeHandlerEx(mPKEventHandler, RtcConnection(interactionInfo!!.roomId, UserManager.getInstance().user.id.toInt()))
                    mPKEventHandler = null
                }
                mHandler.updateRoomInfo(
                    position = mPosition,
                    VideoLoader.RoomInfo(mRoomInfo.roomId, arrayListOf(
                        VideoLoader.AnchorInfo(mRoomInfo.roomId, mRoomInfo.ownerId.toInt(), RtcEngineInstance.generalToken())
                    ))
                )
            }
        } else if (prepareRkRoomId.isNotEmpty()) {
            mRtcEngine.leaveChannelEx(RtcConnection(prepareRkRoomId, UserManager.getInstance().user.id.toInt()))
        }
        prepareRkRoomId = ""
        mBinding.videoLinkingAudienceLayout.videoContainer.setOnClickListener(null)
        mBinding.videoPKLayout.iBroadcasterBView.setOnClickListener(null)

        if (isRoomOwner) {
            enableLocalAudio(true)
            enableLocalVideo(true)
            mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_VIDEO, true)
            activity?.let {
                setupLocalVideo(VideoLoader.VideoCanvasContainer(it, mBinding.videoLinkingLayout.videoContainer, 0))
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
        if (interactionInfo?.interactStatus != ShowInteractionStatus.linking) return
        val rtcConnection = mMainRtcConnection
        ShowLogger.d("interaction", "Interaction >> updateLinkingMode")
        updateStatisticView()

        mBinding.videoLinkingAudienceLayout.userName.text = interactionInfo!!.userName
        mBinding.videoLinkingAudienceLayout.userName.bringToFront()
        mBinding.videoLinkingAudienceLayout.userName.isActivated = !mUserMuteAudioStateMap.getOrDefault(
            interactionInfo!!.userId.toInt(),
            false
        )
        if (isRoomOwner) {
            mBinding.videoLinkingAudienceLayout.videoContainer.setOnClickListener {
                showLinkSettingsDialog()
            }
            enableLocalAudio(true)
            mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_MIC, true)
            mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_VIDEO, true)
            mPKSettingsDialog.resetItemStatus(LivePKSettingsDialog.ITEM_ID_CAMERA, true)
            enableLocalVideo(true)
            activity?.let {
                setupLocalVideo(
                    VideoLoader.VideoCanvasContainer(
                        it,
                        mBinding.videoLinkingLayout.videoContainer,
                        0
                    )
                )
                val view = TextureView(it)
                mBinding.videoLinkingAudienceLayout.videoContainer.removeAllViews()
                mBinding.videoLinkingAudienceLayout.videoContainer.addView(view)
                setVideoOverlayVisible(false, interactionInfo?.userId!!)
                mRtcEngine.setupRemoteVideoEx(
                    VideoCanvas(
                        view,
                        1,
                        interactionInfo?.userId!!.toInt()
                    ),
                    rtcConnection
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
                mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_MIC, true)
                val channelMediaOptions = ChannelMediaOptions()
                channelMediaOptions.publishCameraTrack = true
                channelMediaOptions.publishMicrophoneTrack = true
                channelMediaOptions.publishCustomAudioTrack = false
                channelMediaOptions.enableAudioRecordingOrPlayout = true
                channelMediaOptions.autoSubscribeVideo = true
                channelMediaOptions.autoSubscribeAudio = true
                channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                if (activity is LiveDetailActivity) {
                    (activity as LiveDetailActivity).toggleSelfVideo(true, callback = {
                        if (it) {
                            mRtcEngine.updateChannelMediaOptionsEx(channelMediaOptions, rtcConnection)
                            val context = activity ?: return@toggleSelfVideo
                            enableLocalVideo(true)
                            setupLocalVideo(
                                VideoLoader.VideoCanvasContainer(
                                    context,
                                    mBinding.videoLinkingAudienceLayout.videoContainer,
                                    0
                                )
                            )
                        } else {
                            mService.stopInteraction(mRoomInfo.roomId)
                        }
                    })
                    (activity as LiveDetailActivity).toggleSelfAudio(true, callback = {
                        // nothing
                    })
                }
            } else {
                activity?.let {
                    val view = TextureView(it)
                    mBinding.videoLinkingAudienceLayout.videoContainer.removeAllViews()
                    mBinding.videoLinkingAudienceLayout.videoContainer.addView(view)
                    setVideoOverlayVisible(false, interactionInfo?.userId!!)
                    mRtcEngine.setupRemoteVideoEx(
                        VideoCanvas(
                            view,
                            1,
                            interactionInfo?.userId!!.toInt()
                        ),
                        rtcConnection
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
        mRtcEngine.joinChannelEx(
            RtcEngineInstance.generalToken(),
            pkRtcConnection,
            channelMediaOptions,
            object: IRtcEngineEventHandler() {}
        )
        prepareRkRoomId = pkRoomId
    }

    /**
     * Update p king mode
     *
     */
    private var pkAgainstView: View? = null
    private fun updatePKingMode() {
        if (interactionInfo == null) return
        if (interactionInfo?.interactStatus != ShowInteractionStatus.pking) return
        ShowLogger.d("interaction", "Interaction >> updatePKingMode pkRoomId=${interactionInfo!!.roomId}")
        updateStatisticView()
        val eventListener = object: IRtcEngineEventHandler() {

            override fun onRemoteAudioStateChanged(
                uid: Int,
                state: Int,
                reason: Int,
                elapsed: Int
            ) {
                super.onRemoteAudioStateChanged(uid, state, reason, elapsed)
                ShowLogger.d(TAG, "onRemoteAudioStateChanged pk : uid=$uid, state=$state, reason=$reason")
                if(isPKing() && uid == interactionInfo?.userId?.toInt()){
                    runOnUiThread {
                        if (state == Constants.REMOTE_AUDIO_STATE_STOPPED) {
                            mBinding.videoPKLayout.userNameB.isActivated = false
                        } else if (state == Constants.REMOTE_AUDIO_STATE_DECODING) {
                            mBinding.videoPKLayout.userNameB.isActivated = true
                        }
                    }
                }
            }

            override fun onRemoteVideoStateChanged(
                uid: Int,
                state: Int,
                reason: Int,
                elapsed: Int
            ) {
                super.onRemoteVideoStateChanged(uid, state, reason, elapsed)
                runOnUiThread {
                    if (reason == Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_MUTED) {
                        setVideoOverlayVisible(true, uid.toString())
                    } else if (reason == Constants.REMOTE_VIDEO_STATE_REASON_REMOTE_UNMUTED) {
                        setVideoOverlayVisible(false, uid.toString())
                    }
                }
            }

            override fun onRemoteVideoStats(stats: RemoteVideoStats) {
                super.onRemoteVideoStats(stats)
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
            }

            override fun onDownlinkNetworkInfoUpdated(info: DownlinkNetworkInfo) {
                super.onDownlinkNetworkInfoUpdated(info)
                activity?.runOnUiThread {
                    refreshStatisticInfo(downLinkBps = info.bandwidth_estimation_bps)
                }
            }

            override fun onFirstRemoteVideoFrame(uid: Int, width: Int, height: Int, elapsed: Int) {
                super.onFirstRemoteVideoFrame(uid, width, height, elapsed)
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
                runOnUiThread {
                    setVideoOverlayVisible(false, uid.toString())
                }
            }
        }

        mBinding.videoPKLayout.userNameA.text = mRoomInfo.ownerName
        mBinding.videoPKLayout.userNameA.isActivated = true
        mBinding.videoPKLayout.userNameB.text = interactionInfo!!.userName
        mBinding.videoPKLayout.userNameB.isActivated = true
        if (isRoomOwner) {
            mBinding.videoPKLayout.iBroadcasterBView.setOnClickListener {
                showPKSettingsDialog()
            }
            activity?.let {
                setupLocalVideo(
                    VideoLoader.VideoCanvasContainer(
                        it,
                        mBinding.videoPKLayout.iBroadcasterAView,
                        0,
                        viewIndex = 0
                    )
                )
            }
            enableLocalAudio(true)
            mSettingDialog.resetItemStatus(SettingDialog.ITEM_ID_MIC, true)
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
            mRtcEngine.joinChannelEx(
                RtcEngineInstance.generalToken(),
                pkRtcConnection,
                channelMediaOptions,
                eventListener
            ).let {
                if(Math.abs(it) == Constants.ERR_JOIN_CHANNEL_REJECTED){
                    mPKEventHandler = eventListener
                    mRtcEngine.addHandlerEx(mPKEventHandler, RtcConnection(interactionInfo!!.roomId, UserManager.getInstance().user.id.toInt()))
                }
            }
            activity?.let {
                mBinding.videoPKLayout.iBroadcasterBView.removeView(pkAgainstView)
                pkAgainstView = TextureView(it)
                mBinding.videoPKLayout.iBroadcasterBView.addView(pkAgainstView, 0)
                setVideoOverlayVisible(false, interactionInfo?.userId!!)
                mRtcEngine.setupRemoteVideoEx(
                    VideoCanvas(
                        pkAgainstView,
                        1,
                        interactionInfo?.userId!!.toInt(),
                    ),
                    pkRtcConnection
                )
            }
        } else {
            // audience
            val channelMediaOptions = ChannelMediaOptions()
            channelMediaOptions.publishCameraTrack = false
            channelMediaOptions.publishMicrophoneTrack = false
            channelMediaOptions.publishCustomAudioTrack = false
            channelMediaOptions.enableAudioRecordingOrPlayout = true
            channelMediaOptions.autoSubscribeVideo = true
            channelMediaOptions.autoSubscribeAudio = true
            channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
            channelMediaOptions.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY


            mHandler.updateRoomInfo(
                position = mPosition,
                VideoLoader.RoomInfo(mRoomInfo.roomId, arrayListOf(
                    VideoLoader.AnchorInfo(mRoomInfo.roomId, mRoomInfo.ownerId.toInt(), RtcEngineInstance.generalToken()),
                    VideoLoader.AnchorInfo(interactionInfo!!.roomId, interactionInfo?.userId!!.toInt(), RtcEngineInstance.generalToken()),
                ))
            )

            mPKEventHandler = eventListener
            mRtcEngine.addHandlerEx(mPKEventHandler, RtcConnection(interactionInfo!!.roomId, UserManager.getInstance().user.id.toInt()))
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

    // 播放音乐相关接口
    private var mAudioMxingChannel: RtcConnection? = null
    private var mMediaPlayer: IMediaPlayer? = null
    private var mAudioMixing = false
    private fun startAudioMixing(
        filePath: String,
        loopbackOnly: Boolean,
        cycle: Int
    ) {
        val mediaPlayer = mMediaPlayer ?: mRtcEngine.createMediaPlayer()
        mMediaPlayer = mediaPlayer
        mediaPlayer.stop()
        mediaPlayer.openWithMediaSource(MediaPlayerSource().apply {
            url = filePath
            isAutoPlay = true
        })
        adjustAudioMixingVolume(VideoSetting.getCurrBroadcastSetting().audio.audioMixingVolume)
        mediaPlayer.setLoopCount(if (cycle >= 0) 0 else Int.MAX_VALUE)
        mAudioMixing = true
        if (!loopbackOnly && mAudioMxingChannel == null) {
            val uid = UserManager.getInstance().user.id.toInt() + 100000
            val channel = RtcConnection(mRoomInfo.roomId, uid)
            mAudioMxingChannel = channel

            val mediaOptions = ChannelMediaOptions()
            mediaOptions.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING
            mediaOptions.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            mediaOptions.publishMediaPlayerId = mediaPlayer.mediaPlayerId
            mediaOptions.publishMediaPlayerAudioTrack = true
            mediaOptions.publishCameraTrack = false
            mediaOptions.autoSubscribeAudio = false
            mediaOptions.autoSubscribeVideo = false
            mediaOptions.enableAudioRecordingOrPlayout = false

            TokenGenerator.generateToken(channel.channelId, channel.localUid.toString(),
                TokenGenerator.TokenGeneratorType.Token007,
                TokenGenerator.AgoraTokenType.Rtc,
                success = {
                    ShowLogger.d("RoomListActivity", "generateToken success， uid：${channel.localUid}")
                    if (!mAudioMixing) {
                        return@generateToken
                    }
                    val ret = mRtcEngine.joinChannelEx(
                        it,
                        channel,
                        mediaOptions,
                        object : IRtcEngineEventHandler() {
                            override fun onError(err: Int) {
                                super.onError(err)
                                ToastUtils.showToast("startAudioMixing joinChannelEx onError, error code: $err, ${RtcEngine.getErrorDescription(err)}")
                            }
                        }
                    )
                    if(ret != Constants.ERR_OK){
                        ToastUtils.showToast("startAudioMixing joinChannelEx failed, error code: $ret, ${RtcEngine.getErrorDescription(ret)}")
                    }
                },
                failure = {
                    ShowLogger.e("RoomListActivity", it, "generateToken failure：$it")
                    mAudioMxingChannel = null
                    ToastUtils.showToast(it?.message ?: "generate token failure")
                })
        }
    }

    private fun stopAudioMixing() {
        mAudioMixing = false

        // 停止播放，拿到connection对应的MediaPlayer并停止释放
        mMediaPlayer?.stop()

        // 停止推流，使用updateChannelMediaOptionEx
        mAudioMxingChannel?.let {
            val options = LeaveChannelOptions()
            options.stopMicrophoneRecording = false
            mRtcEngine.leaveChannelEx(it, options)
            mAudioMxingChannel = null
        }
    }

    private fun adjustAudioMixingVolume(volume: Int) {
        mMediaPlayer?.adjustPlayoutVolume(volume)
        mMediaPlayer?.adjustPublishSignalVolume(volume)
    }
}