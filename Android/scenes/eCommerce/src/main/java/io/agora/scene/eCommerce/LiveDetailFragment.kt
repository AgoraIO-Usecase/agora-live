package io.agora.scene.eCommerce

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import io.agora.scene.base.utils.UiUtil
import io.agora.scene.eCommerce.databinding.CommerceLiveDetailFragmentBinding
import io.agora.scene.eCommerce.databinding.CommerceLiveDetailMessageItemBinding
import io.agora.scene.eCommerce.databinding.CommerceLivingEndDialogBinding
import io.agora.scene.eCommerce.debugSettings.DebugAudienceSettingDialog
import io.agora.scene.eCommerce.debugSettings.DebugSettingDialog
import io.agora.scene.eCommerce.service.ROOM_AVAILABLE_DURATION
import io.agora.scene.eCommerce.videoSwitcherAPI.VideoSwitcher
import io.agora.scene.eCommerce.service.ShowMessage
import io.agora.scene.eCommerce.service.ShowRoomDetailModel
import io.agora.scene.eCommerce.service.ShowServiceProtocol
import io.agora.scene.eCommerce.shop.GoodsListDialog
import io.agora.scene.eCommerce.widget.AdvanceSettingAudienceDialog
import io.agora.scene.eCommerce.widget.AdvanceSettingDialog
import io.agora.scene.eCommerce.widget.PictureQualityDialog
import io.agora.scene.eCommerce.widget.SettingDialog
import io.agora.scene.eCommerce.widget.TextInputDialog
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
        CommerceLiveDetailFragmentBinding.inflate(
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
    private var mMessageAdapter: BindingSingleAdapter<ShowMessage, CommerceLiveDetailMessageItemBinding>? =
        null

    /**
     * M setting dialog
     */
    private val mSettingDialog by lazy { SettingDialog(requireContext()) }
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
        initLivingEndLayout()
        initTopLayout()
        initBottomLayout()
        initMessageLayout()
        initAuctionLayout()
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
            .error(R.drawable.commerce_default_avatar)
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
            .error(R.drawable.commerce_default_avatar)
            .into(topLayout.ivOwnerAvatar)
        topLayout.tvRoomName.text = mRoomInfo.roomName
        topLayout.tvRoomId.text = getString(R.string.commerce_room_id, mRoomInfo.roomId)
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
            showSettingDialog()
        }
        bottomLayout.ivShop.setOnClickListener {
            goodsListDialog()
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
            object : BindingSingleAdapter<ShowMessage, CommerceLiveDetailMessageItemBinding>() {
                override fun onBindViewHolder(
                    holder: BindingViewHolder<CommerceLiveDetailMessageItemBinding>, position: Int
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
    /** 拍卖 */
    private fun initAuctionLayout() {
        val layoutParams = mBinding.flAuction.layoutParams
        if (isRoomOwner) {
            layoutParams.height = UiUtil.dp2px(140)
            mBinding.flAuction.setBackgroundResource(R.drawable.commerce_auction_bg_owner)
        } else {
            layoutParams.height = UiUtil.dp2px(88)
            mBinding.flAuction.setBackgroundResource(R.drawable.commerce_auction_bg_user)
        }
        mBinding.flAuction.layoutParams = layoutParams
    }
    /**
     * Refresh bottom layout
     *
     */
    private fun refreshBottomLayout() {
        if (isRoomOwner) {
            mSettingDialog.apply {
                resetSettingsItem(false)
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
        encodeVideoSize?.let { topBinding.tvEncodeResolution.text = getString(R.string.commerce_statistic_encode_resolution, "${it.height}x${it.width}") }
        if (topBinding.tvEncodeResolution.text.isEmpty()) topBinding.tvEncodeResolution.text = getString(R.string.commerce_statistic_encode_resolution, "--")
        receiveVideoSize?.let { topBinding.tvReceiveResolution.text = getString(R.string.commerce_statistic_receive_resolution, "${it.height}x${it.width}") }
        if (topBinding.tvReceiveResolution.text.isEmpty()) topBinding.tvReceiveResolution.text = getString(R.string.commerce_statistic_receive_resolution, "--")
        encodeFps?.let { topBinding.tvStatisticEncodeFPS.text = getString(R.string.commerce_statistic_encode_fps, it.toString()) }
        if (topBinding.tvStatisticEncodeFPS.text.isEmpty()) topBinding.tvStatisticEncodeFPS.text = getString(R.string.commerce_statistic_encode_fps, "--")
        receiveFPS?.let { topBinding.tvStatisticReceiveFPS.text = getString(R.string.commerce_statistic_receive_fps, it.toString()) }
        if (topBinding.tvStatisticReceiveFPS.text.isEmpty()) topBinding.tvStatisticReceiveFPS.text = getString(R.string.commerce_statistic_receive_fps, "--")
        downDelay?.let { topBinding.tvStatisticDownDelay.text = getString(R.string.commerce_statistic_delay, it.toString()) }
        if (topBinding.tvStatisticDownDelay.text.isEmpty()) topBinding.tvStatisticDownDelay.text = getString(R.string.commerce_statistic_delay, "--")
        upLossPackage?.let { topBinding.tvStatisticUpLossPackage.text = getString(R.string.commerce_statistic_up_loss_package, it.toString()) }
        if (topBinding.tvStatisticUpLossPackage.text.isEmpty()) topBinding.tvStatisticUpLossPackage.text = getString(R.string.commerce_statistic_up_loss_package, "--")
        downLossPackage?.let { topBinding.tvStatisticDownLossPackage.text = getString(R.string.commerce_statistic_down_loss_package, it.toString()) }
        if (topBinding.tvStatisticDownLossPackage.text.isEmpty()) topBinding.tvStatisticDownLossPackage.text = getString(R.string.commerce_statistic_down_loss_package, "--")
        upBitrate?.let { topBinding.tvStatisticUpBitrate.text = getString(R.string.commerce_statistic_up_bitrate, it.toString()) }
        if (topBinding.tvStatisticUpBitrate.text.isEmpty()) topBinding.tvStatisticUpBitrate.text = getString(R.string.commerce_statistic_up_bitrate, "--")
        downBitrate?.let { topBinding.tvStatisticDownBitrate.text = getString(R.string.commerce_statistic_down_bitrate, it.toString()) }
        if (topBinding.tvStatisticDownBitrate.text.isEmpty()) topBinding.tvStatisticDownBitrate.text = getString(R.string.commerce_statistic_down_bitrate, "--")
        topBinding.tvStatisticUpNet.isVisible = !isAudioOnlyMode
        upLinkBps?.let { topBinding.tvStatisticUpNet.text = getString(R.string.commerce_statistic_up_net_speech, (it / 8192).toString()) }
        if (topBinding.tvStatisticUpNet.text.isEmpty()) topBinding.tvStatisticUpNet.text = getString(R.string.commerce_statistic_up_net_speech, "--")
        topBinding.tvStatisticDownNet.isVisible = !isAudioOnlyMode
        downLinkBps?.let { topBinding.tvStatisticDownNet.text = getString(R.string.commerce_statistic_down_net_speech, (it / 8192).toString()) }
        if (topBinding.tvStatisticDownNet.text.isEmpty()) topBinding.tvStatisticDownNet.text = getString(R.string.commerce_statistic_down_net_speech, "--")
        topBinding.tvQuickStartTime.isVisible = true
        if (isRoomOwner) {
            topBinding.tvQuickStartTime.text = getString(R.string.commerce_statistic_quick_start_time, "--")
        } else {
            topBinding.tvQuickStartTime.text = getString(R.string.commerce_statistic_quick_start_time,
                mRtcVideoSwitcher.getFirstVideoFrameTime().toString())
        }
        topBinding.tvStatisticDeviceGrade.isVisible = true
        val score = mRtcEngine.queryDeviceScore()
        if (score >= 90) {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.commerce_device_grade, getString(R.string.commerce_setting_preset_device_high)) + "（$score）"
        } else if (score >= 75) {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.commerce_device_grade, getString(R.string.commerce_setting_preset_device_medium)) + "（$score）"
        } else {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.commerce_device_grade, getString(R.string.commerce_setting_preset_device_low)) + "（$score）"
        }
        topBinding.tvStatisticH265.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticH265.text = getString(R.string.commerce_statistic_h265, getString(R.string.commerce_setting_opened))
        } else {
            topBinding.tvStatisticH265.text = getString(R.string.commerce_statistic_h265, "--")
        }
        topBinding.tvStatisticSR.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticSR.text = getString(R.string.commerce_statistic_sr, "--")
        } else {
            topBinding.tvStatisticSR.text = getString(R.string.commerce_statistic_sr, if (VideoSetting.getCurrAudienceEnhanceSwitch()) getString(R.string.commerce_setting_opened) else getString(R.string.commerce_setting_closed))
        }
        topBinding.tvStatisticPVC.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticPVC.text = getString(R.string.commerce_statistic_pvc,
                if (VideoSetting.getCurrBroadcastSetting().video.PVC)
                    getString(R.string.commerce_setting_opened)
                else
                    getString(R.string.commerce_setting_closed)
            )
        } else {
            topBinding.tvStatisticPVC.text = getString(R.string.commerce_statistic_pvc, "--")
        }

        topBinding.tvStatisticLowStream.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticLowStream.text =
                getString(R.string.commerce_statistic_low_stream,
                    if (VideoSetting.getCurrLowStreamSetting() == null)
                        getString(R.string.commerce_setting_closed)
                    else
                        getString(R.string.commerce_setting_opened)
                )
        } else {
            topBinding.tvStatisticLowStream.text = getString(R.string.commerce_statistic_low_stream, "--")
        }

        topBinding.tvStatisticSVC.isVisible = true
        if (isRoomOwner) {
            topBinding.tvStatisticSVC.text = getString(R.string.commerce_statistic_svc,
                if (VideoSetting.getCurrLowStreamSetting()?.SVC == true)
                    getString(R.string.commerce_setting_opened)
                else getString(R.string.commerce_setting_closed)
            )
        } else {
            topBinding.tvStatisticSVC.text = getString(R.string.commerce_statistic_svc, "--")
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

    private fun goodsListDialog() {
        val context = this.context ?: return
        GoodsListDialog(context).show()
    }
    /**
     * Show setting dialog
     *
     */
    private fun showSettingDialog() {
        mSettingDialog.apply {
            setHostView(isRoomOwner)
            setOnItemActivateChangedListener { _, itemId, activated ->
                when (itemId) {
                    SettingDialog.ITEM_ID_CAMERA -> mRtcEngine.switchCamera()
                    SettingDialog.ITEM_ID_QUALITY -> showPictureQualityDialog(this)
                    SettingDialog.ITEM_ID_VIDEO -> {
                        if (activity is LiveDetailActivity){
                            (activity as LiveDetailActivity).toggleSelfVideo(activated, callback = {
                                enableLocalVideo(activated)
                            })
                        }
                    }
                    SettingDialog.ITEM_ID_MIC -> {
                        if (activity is LiveDetailActivity){
                            (activity as LiveDetailActivity).toggleSelfAudio(activated, callback = {
                                if (isRoomOwner) {
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
     * Show end room dialog
     *
     */
    private fun showEndRoomDialog() {
        AlertDialog.Builder(requireContext(), R.style.commerce_alert_dialog)
            .setTitle(R.string.commerce_tip)
            .setMessage(R.string.commerce_live_end_room_or_not)
            .setPositiveButton(R.string.commerce_setting_advance_ok) { dialog, id ->
                activity?.finish()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.commerce_setting_cancel) { dialog, id ->
                dialog.dismiss()
            }
            .show()
    }
    //================== Service Operation ===============

    /**
     * Init service with join room
     *
     */
    private fun initServiceWithJoinRoom() {
        mService.joinRoom(mRoomInfo.roomId,
            success = {
                mService.sendChatMessage(mRoomInfo.roomId, getString(R.string.commerce_live_chat_coming))
                initService()
            },
            error = {
                if ((it as? io.agora.scene.eCommerce.service.RoomException)?.currRoomNo == mRoomInfo.roomId) {
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
        }
        mService.subscribeCurrRoomEvent(mRoomInfo.roomId) { status, _ ->
            if (status == ShowServiceProtocol.ShowSubscribeStatus.deleted) {
                destroy(false)
                showLivingEndLayout()
                ShowLogger.d("showLivingEndLayout","room delete by owner!")
            }
        }
        mService.subscribeMessage(mRoomInfo.roomId) { _, showMessage ->
            insertMessageItem(showMessage)
        }
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
    private fun showLivingEndLayout() {
        if (isRoomOwner) {
            val context = activity ?: return
            AlertDialog.Builder(context, R.style.commerce_alert_dialog)
                .setView(CommerceLivingEndDialogBinding.inflate(LayoutInflater.from(requireContext())).apply {
                    Glide.with(this@LiveDetailFragment)
                        .load(mRoomInfo.ownerAvatar)
                        .into(ivAvatar)
                }.root)
                .setCancelable(false)
                .setPositiveButton(R.string.commerce_living_end_back_room_list) { dialog, _ ->
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
                if (stats.uid == mRoomInfo.ownerId.toInt() || isRoomOwner) {
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
                    ToastUtils.showToast(R.string.commerce_content)
                }
            },
            onChannelJoined = {
                onJoinChannelSuccess.invoke()
            },
            onFirstRemoteVideoFrame = { uid, width, height, elapsed ->
            }
        )

        if (activity is LiveDetailActivity){
            (activity as LiveDetailActivity).toggleSelfVideo(isRoomOwner, callback = {
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
            (activity as LiveDetailActivity).toggleSelfAudio(isRoomOwner, callback = {
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
     * Update idle mode
     *
     */
    private fun updateIdleMode() {
        ShowLogger.d(TAG, "Interaction >> updateIdleMode")
        if (prepareRkRoomId.isNotEmpty()) {
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
        channelMediaOptions.autoSubscribeVideo = true
        channelMediaOptions.autoSubscribeAudio = true
        channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER

        (activity as? io.agora.scene.eCommerce.LiveDetailActivity)?.let {
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
     * Release countdown
     *
     */
    private fun releaseCountdown() {
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