package io.agora.scene.eCommerce

import android.animation.AnimatorInflater
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.*
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
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
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcConnection
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.base.utils.UiUtil
import io.agora.scene.eCommerce.databinding.CommerceLiveDetailFragmentBinding
import io.agora.scene.eCommerce.databinding.CommerceLiveDetailMessageItemBinding
import io.agora.scene.eCommerce.databinding.CommerceLivingEndDialogBinding
import io.agora.scene.eCommerce.service.*
import io.agora.scene.eCommerce.shop.GoodsListDialog
import io.agora.scene.eCommerce.shop.LiveAuctionFragment
import io.agora.scene.eCommerce.utils.CommerceConstants
import io.agora.scene.eCommerce.videoLoaderAPI.OnPageScrollEventHandler
import io.agora.scene.eCommerce.videoLoaderAPI.VideoLoader
import io.agora.scene.eCommerce.widget.*
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
    private val TAG = "LiveDetailFragment${this.hashCode()}"

    companion object {

        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"

        /**
         * New instance
         *
         * @param roomId
         */
        fun newInstance(roomId: String, handler: OnPageScrollEventHandler, position: Int) = LiveDetailFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_ROOM_ID, roomId)
            }
            mHandler = handler
            mPosition = position
        }
    }

    private val mRoomId by lazy { (arguments?.getString(EXTRA_ROOM_ID))!! }

    private val mRoomInfo: RoomDetailModel by lazy { mService.getRoomInfo(mRoomId)!! }

    /**
     * Page scroll event handler
     */
    private lateinit var mHandler: OnPageScrollEventHandler

    /**
     * M position
     */
    private var mPosition: Int = 0

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
    private val isRoomOwner by lazy { mRoomInfo.ownerId.toLong() == UserManager.getInstance().user.id }

    /**
     * M message adapter
     */
    private var mMessageAdapter: BindingSingleAdapter<ShowMessage, CommerceLiveDetailMessageItemBinding>? =
        null

    /**
     * M setting dialog
     */
    private val mSettingDialog by lazy { SettingDialog(requireContext()) }

    private lateinit var auctionFragment: LiveAuctionFragment
    /**
     * M rtc engine
     */
    private val mRtcEngine by lazy { RtcEngineInstance.rtcEngine }

    /**
     * M rtc video loader api
     */
    private val mRtcVideoLoaderApi by lazy { VideoLoader.getImplInstance(mRtcEngine) }

    /**
     * Is audio only mode
     */
    private var isAudioOnlyMode = false

    private var isLoadSafely = false

    private var isLoaded = false

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
        CommerceLogger.d(TAG,"timer end!")
    }

    /**
     * M main rtc connection
     */
    private val mMainRtcConnection by lazy { RtcConnection(mRoomInfo.roomId, UserManager.getInstance().user.id.toInt()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        CommerceLogger.d(TAG, "[commerce]$this $mRoomId onCreateView")
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
        CommerceLogger.d(TAG, "[commerce]$this $mRoomId onViewCreated")

        initView()
        activity?.onBackPressedDispatcher?.addCallback(enabled = isVisible) {
            onBackPressed()
        }
        changeStatisticVisible(false)
    }

    /**
     * On attach
     *
     * @param context
     */
    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity ?: return
        CommerceLogger.d(TAG, "[commerce]$this $mRoomId onAttach")
        if (isLoadSafely) {
            startLoadPage()
        }
    }

    /**
     * On detach
     *
     */
    override fun onDetach() {
        super.onDetach()
        CommerceLogger.d(TAG, "[commerce]$this $mRoomId onDetach")
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
    fun startLoadPageSafely() {
        CommerceLogger.d(TAG, "[commerce]${this.hashCode()} $mRoomId startLoadPageSafely1")
        isLoadSafely = true
        activity ?: return
        CommerceLogger.d(TAG, "[commerce]$this $mRoomId startLoadPageSafely2")
        startLoadPage()
    }

    fun onPageLoaded() {

    }

    private fun startLoadPage() {
        if (isLoaded) return
        isLoaded = true
        subscribeMediaTime = SystemClock.elapsedRealtime()
        if (mRoomInfo.isRobotRoom()) {
            initRtcEngine()
            initServiceWithJoinRoom()
        } else {
            val roomLeftTime = ShowServiceProtocol.ROOM_AVAILABLE_DURATION - mService.getCurrentRoomDuration(mRoomId)
            if (roomLeftTime > 0) {
                mBinding.root.postDelayed(timerRoomEndRun, roomLeftTime)
                initRtcEngine()
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
        CommerceLogger.d(TAG, "[commerce]$this $mRoomId stopLoadPage")
        isLoaded = false
        isLoadSafely = false
        removeAllMessage()
        auctionFragment.release()
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
        mService.leaveRoom(mRoomInfo.roomId)
        CommerceLogger.d(TAG, "[commerce]$this $mRoomId destroy")
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
            val dialog = TopFunctionDialog(it, true)
            dialog.reportContentCallback = {
                CommerceConstants.reportContents[mRoomInfo.roomName] = true
                activity?.finish()
            }
            dialog.reportUserCallback = {
                CommerceConstants.reportUsers[mRoomInfo.ownerId] = true
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
        initAuctionLayout()
        if (isRoomOwner) {
            mBinding.vLike.likeView.isVisible = false
        } else {
            mBinding.vLike.likeView.setOnClickListener {
                mBinding.vLike.addFavor()
                mService.likeSend(mRoomId, {}, {})
            }
        }

        // Render host video
        if (needRender) {
            mRtcVideoLoaderApi.renderVideo(
                VideoLoader.AnchorInfo(
                    mRoomInfo.roomId,
                    mRoomInfo.ownerId.toIntOrNull() ?: 0,
                    RtcEngineInstance.generalRtcToken()
                ),
                UserManager.getInstance().user.id.toInt(),
                VideoLoader.VideoCanvasContainer(
                    viewLifecycleOwner,
                    mBinding.videoLinkingLayout.videoContainer,
                    mRoomInfo.ownerId.toIntOrNull() ?: 0
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
            return VideoLoader.VideoCanvasContainer(
                viewLifecycleOwner,
                mBinding.videoLinkingLayout.videoContainer,
                mRoomInfo.ownerId.toInt()
            )
        }
        return null
    }

    /**
     * Init living end layout
     *
     */
    private fun initLivingEndLayout() {
        val livingEndLayout = mBinding.livingEndLayout
        livingEndLayout.root.isVisible = ShowServiceProtocol.ROOM_AVAILABLE_DURATION < (mService.getCurrentRoomDuration(mRoomId)) && !isRoomOwner && !mRoomInfo.isRobotRoom()
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
//        CommerceLogger.d(
//           TAG,
//           "TopTimer curr=${TimeUtils.currentTimeMillis()}, createAt=${mRoomInfo.createdAt}, diff=${TimeUtils.currentTimeMillis() - mRoomInfo.createdAt}, time=${
//               dataFormat.format(Date(TimeUtils.currentTimeMillis() - mRoomInfo.createdAt))
//           }"
//       )
        topLayout.tvTimer.post(object : Runnable {
            override fun run() {
                topLayout.tvTimer.text =
                    dataFormat.format(Date(mService.getCurrentRoomDuration(mRoomId)))
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
                    holder.binding.text.setOnClickListener {
                        reportMessageDialog(item)
                    }
                }
            }
        (messageLayout.rvMessage.layoutManager as LinearLayoutManager).let {
            it.stackFromEnd = true
        }
        messageLayout.rvMessage.adapter = mMessageAdapter
    }

    /** Auction */
    private fun initAuctionLayout() {
        auctionFragment = LiveAuctionFragment.newInstance(mRoomId)
        val transaction = childFragmentManager.beginTransaction()
        transaction.add(R.id.flAuction, auctionFragment)
        transaction.commit()
        val layoutParams = mBinding.flAuction.layoutParams
        if (isRoomOwner) {
            layoutParams.height = UiUtil.dp2px(88)
            mBinding.flAuction.setBackgroundResource(R.drawable.commerce_auction_bg_owner)
        } else {
            layoutParams.height = UiUtil.dp2px(140)
            mBinding.flAuction.setBackgroundResource(R.drawable.commerce_auction_bg_user)
        }
        mBinding.flAuction.layoutParams = layoutParams
    }

    private fun setVideoOverlayVisible(visible: Boolean, userId: String) {
        if (userId == UserManager.getInstance().user.id.toString()) {
            // Local video state change
            if (isRoomOwner) {
                mBinding.videoLinkingLayout.videoOverlay.isVisible = visible
            }
        } else if (userId == mRoomInfo.ownerId) {
            // Room owner video state change
            if (!isRoomOwner) {
                mBinding.videoLinkingLayout.videoOverlay.isVisible = visible
            }
        }
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
    private fun reportMessageDialog(message: ShowMessage) {
        context?.let {
            MessageReportDialog(it, message.message).show()
        }
    }
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
                mService.sendChatMessage(mRoomInfo.roomId, msg, {
                    insertMessageItem(
                        ShowMessage(
                            UserManager.getInstance().user.id.toString(),
                            UserManager.getInstance().user.name,
                            msg,
                            System.currentTimeMillis().toDouble()
                    ))
                }) { e ->
                    ToastUtils.showToast(context?.getString(R.string.commerce_send_chat_message_failed, e.message))
                }
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
        topBinding.tlStatisticSender.isVisible = isRoomOwner
        topBinding.tlStatisticReceiver.isVisible = !isRoomOwner
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

        val showSender = isRoomOwner
        topBinding.tlStatisticSender.isVisible = showSender

        val showReceiver = !isRoomOwner
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
        encodeVideoSize?.let { topBinding.tvEncodeResolution.text = getString(R.string.commerce_statistic_encode_resolution, "${it.height}x${it.width}") }
        if (topBinding.tvEncodeResolution.text.isEmpty()) topBinding.tvEncodeResolution.text = getString(R.string.commerce_statistic_encode_resolution, "--")
        encodeFps?.let { topBinding.tvStatisticEncodeFPS.text = getString(R.string.commerce_statistic_encode_fps, it.toString()) }
        if (topBinding.tvStatisticEncodeFPS.text.isEmpty()) topBinding.tvStatisticEncodeFPS.text = getString(R.string.commerce_statistic_encode_fps, "--")
        upBitrate?.let { topBinding.tvStatisticUpBitrate.text = getString(R.string.commerce_statistic_up_bitrate, it.toString()) }
        if (topBinding.tvStatisticUpBitrate.text.isEmpty()) topBinding.tvStatisticUpBitrate.text = getString(R.string.commerce_statistic_up_bitrate, "--")
        upLossPackage?.let { topBinding.tvStatisticUpLossPackage.text = getString(R.string.commerce_statistic_up_loss_package, it.toString()) }
        if (topBinding.tvStatisticUpLossPackage.text.isEmpty()) topBinding.tvStatisticUpLossPackage.text = getString(R.string.commerce_statistic_up_loss_package, "--")
        topBinding.tvStatisticUpNet.isVisible = !isAudioOnlyMode
        upLinkBps?.let { topBinding.tvStatisticUpNet.text = getString(R.string.commerce_statistic_up_net_speech, (it / 8192).toString()) }
        if (topBinding.tvStatisticUpNet.text.isEmpty()) topBinding.tvStatisticUpNet.text = getString(R.string.commerce_statistic_up_net_speech, "--")

        // receiver
        receiveVideoSize?.let { topBinding.tvReceiveResolution.text = getString(R.string.commerce_statistic_receive_resolution, "${it.height}x${it.width}") }
        if (topBinding.tvReceiveResolution.text.isEmpty()) topBinding.tvReceiveResolution.text = getString(R.string.commerce_statistic_receive_resolution, "--")
        receiveFPS?.let { topBinding.tvStatisticReceiveFPS.text = getString(R.string.commerce_statistic_receive_fps, it.toString()) }
        if (topBinding.tvStatisticReceiveFPS.text.isEmpty()) topBinding.tvStatisticReceiveFPS.text = getString(R.string.commerce_statistic_receive_fps, "--")
        downDelay?.let { topBinding.tvStatisticDownDelay.text = getString(R.string.commerce_statistic_delay, it.toString()) }
        if (topBinding.tvStatisticDownDelay.text.isEmpty()) topBinding.tvStatisticDownDelay.text = getString(R.string.commerce_statistic_delay, "--")
        downLossPackage?.let { topBinding.tvStatisticDownLossPackage.text = getString(R.string.commerce_statistic_down_loss_package, it.toString()) }
        if (topBinding.tvStatisticDownLossPackage.text.isEmpty()) topBinding.tvStatisticDownLossPackage.text = getString(R.string.commerce_statistic_down_loss_package, "--")
        downBitrate?.let { topBinding.tvStatisticDownBitrate.text = getString(R.string.commerce_statistic_down_bitrate, it.toString()) }
        if (topBinding.tvStatisticDownBitrate.text.isEmpty()) topBinding.tvStatisticDownBitrate.text = getString(R.string.commerce_statistic_down_bitrate, "--")

        topBinding.tvStatisticDownNet.isVisible = !isAudioOnlyMode
        downLinkBps?.let { topBinding.tvStatisticDownNet.text = getString(R.string.commerce_statistic_down_net_speech, (it / 8192).toString()) }
        if (topBinding.tvStatisticDownNet.text.isEmpty()) topBinding.tvStatisticDownNet.text = getString(R.string.commerce_statistic_down_net_speech, "--")

        // other
        topBinding.tvLocalUid.text =
            getString(R.string.commerce_local_uid, UserManager.getInstance().user.id.toString())

        topBinding.tvEncoder.isVisible = isRoomOwner
        encodeType?.let {
            topBinding.tvEncoder.text =
                getString(
                    R.string.commerce_statistic_encoder, when (it) {
                        VideoEncoderConfiguration.VIDEO_CODEC_TYPE.VIDEO_CODEC_H264.value -> "H264"
                        VideoEncoderConfiguration.VIDEO_CODEC_TYPE.VIDEO_CODEC_H265.value -> "H265"
                        VideoEncoderConfiguration.VIDEO_CODEC_TYPE.VIDEO_CODEC_AV1.value -> "AV1"
                        else -> "--"
                    }
                )
        }
        if (topBinding.tvEncoder.text.isEmpty()) topBinding.tvEncoder.text = getString(R.string.commerce_statistic_encoder, "--")



        topBinding.trSVCPVC.isVisible = isRoomOwner
        topBinding.tvStatisticSVC.text = getString(R.string.commerce_statistic_svc,
            if (VideoSetting.getCurrLowStreamSetting()?.SVC == true)
                getString(R.string.commerce_setting_opened)
            else getString(R.string.commerce_setting_closed)
        )
        topBinding.tvStatisticPVC.text = getString(R.string.commerce_statistic_pvc,
            if (VideoSetting.getCurrBroadcastSetting().video.PVC)
                getString(R.string.commerce_setting_opened)
            else
                getString(R.string.commerce_setting_closed)
        )

        val score = mRtcEngine.queryDeviceScore()
        topBinding.tvStatisticDeviceGrade.isVisible = true
        if (score >= 90) {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.commerce_device_grade, getString(R.string.commerce_setting_preset_device_high)) + "（$score）"
        } else if (score >= 75) {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.commerce_device_grade, getString(R.string.commerce_setting_preset_device_medium)) + "（$score）"
        } else {
            topBinding.tvStatisticDeviceGrade.text = getString(R.string.commerce_device_grade, getString(R.string.commerce_setting_preset_device_low)) + "（$score）"
        }


        topBinding.trStatisticSR.isVisible = !isRoomOwner
        if (isRoomOwner) {
            topBinding.tvStatisticSR.text = getString(R.string.commerce_statistic_sr, "--")
        } else {
            topBinding.tvStatisticSR.text = getString(R.string.commerce_statistic_sr, if (VideoSetting.getCurrAudienceEnhanceSwitch()) getString(R.string.commerce_setting_opened) else getString(R.string.commerce_setting_closed))
        }


        topBinding.trStatisticLowStream.isVisible = isRoomOwner
        topBinding.tvStatisticLowStream.text =
            getString(R.string.commerce_statistic_low_stream,
                if (VideoSetting.getCurrLowStreamSetting() == null)
                    getString(R.string.commerce_setting_closed)
                else
                    getString(R.string.commerce_setting_opened)
            )

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

    private fun removeAllMessage() = runOnUiThread {
        mMessageAdapter?.removeAll()
    }

    private fun goodsListDialog() {
        val context = this.context ?: return
        GoodsListDialog(context, mRoomId).show()
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
                    SettingDialog.ITEM_ID_CAMERA -> {
                        mRtcEngine.switchCamera()
//                        RtcEngineInstance.isFrontCamera = !RtcEngineInstance.isFrontCamera
//                        mRtcEngine.setVideoEncoderConfigurationEx(RtcEngineInstance.videoEncoderConfiguration.apply {
//                            mirrorMode = if (RtcEngineInstance.isFrontCamera) VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_ENABLED else VideoEncoderConfiguration.MIRROR_MODE_TYPE.MIRROR_MODE_DISABLED
//                        }, RtcConnection(mRoomId, UserManager.getInstance().user.id.toInt()))
                    }
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
                        if (isHostView()) showAdvanceSettingDialog() else AdvanceSettingAudienceDialog(context).show()
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
            //setItemShowTextOnly(AdvanceSettingDialog.ITEM_ID_SWITCH_BITRATE_SAVE, true)
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
                mService.leaveRoom(mRoomInfo.roomId)
                activity?.finish()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.commerce_setting_cancel) { dialog, id ->
                dialog.dismiss()
            }
            .show()
    }
    //================== Service Operation ===============

    private fun initServiceWithJoinRoom() {
        CommerceLogger.d(TAG, "[commerce]$this $mRoomId initServiceWithJoinRoom")
        mBinding.root.post {
            mService.joinRoom(mRoomInfo,
                success = {
                    initService()
                    CommerceLogger.d(TAG, "[commerce]$this $mRoomId initServiceWithJoinRoom mRoomInfo.roomId:${mRoomInfo.roomId}")

                    if (!isRoomOwner) {
                        insertMessageItem(
                            ShowMessage(
                                UserManager.getInstance().user.id.toString(),
                                UserManager.getInstance().user.name,
                                getString(R.string.commerce_live_chat_coming),
                                System.currentTimeMillis().toDouble()
                            )
                        )
                    }
                },
                error = { e ->
                    runOnUiThread {
                        CommerceLogger.d(TAG, "join room error!:${e.message}")
                        ToastUtils.showToast("You are disconnected. Error:${e.message}")
                        destroy(false)
                        activity?.finish()
                    }
                })
        }
    }

    /**
     * Init service
     *
     */
    private fun initService() {
        CommerceLogger.d(TAG, "[commerce]$this $mRoomId initService")
        mService.subscribeCurrRoomEvent(mRoomId) {
            destroy(false)
            showLivingEndLayout()
            CommerceLogger.d("showLivingEndLayout","room delete by owner!")
        }
        mService.subscribeMessage(mRoomId) { showMessage ->
            insertMessageItem(showMessage)
        }
        mService.subscribeUser(mRoomId) { count ->
            refreshTopUserCount(count)
        }
        mService.subscribeUserJoin(mRoomId) { id, name, _ ->
            if (context != null) {
                animateUserEntryExit(name, mBinding.root)
                insertMessageItem(
                    ShowMessage(
                        id,
                        name,
                        getString(R.string.commerce_live_chat_coming),
                        System.currentTimeMillis().toDouble()
                    )
                )
            }
        }
        mService.subscribeUserLeave(mRoomId) { id, name, _ ->
            if (context != null) {
                insertMessageItem(
                    ShowMessage(
                        id,
                        name,
                        getString(R.string.commerce_live_chat_leaving),
                        System.currentTimeMillis().toDouble()
                    )
                )
            }
        }
        mService.auctionSubscribe(mRoomId) { auctionModel ->
            CommerceLogger.d("LiveAuctionFragment", "[commerce]$this $mRoomId auctionSubscribe call back, auctionModel:$auctionModel")
            auctionFragment.updateAuction(auctionModel)
        }
        mService.likeSubscribe(mRoomId) {
            mBinding.vLike.addFavor()
        }
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
                        .load(mRoomInfo.getOwnerAvatarFullUrl())
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

    private var quickStartTime = 0L
    private var subscribeMediaTime = 0L

    private fun initRtcEngine() {
        val eventListener = object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                super.onJoinChannelSuccess(channel, uid, elapsed)
                VideoSetting.updateBroadcastSetting(
                    VideoSetting.LiveMode.OneVOne,
                    isJoinedRoom = true,
                    rtcConnection = mMainRtcConnection
                )
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
                if (stats.uid == mRoomInfo.ownerId.toInt()) {
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
        }

        if (activity is LiveDetailActivity) {
            (activity as LiveDetailActivity).toggleSelfVideo(isRoomOwner, callback = {
                joinChannel(eventListener)
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
        if (isRoomOwner) {
            mRtcEngine.stopPreview()
            mRtcEngine.leaveChannelEx(mMainRtcConnection)
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
        channelMediaOptions.autoSubscribeAudio = true
        channelMediaOptions.publishCameraTrack = isRoomOwner
        channelMediaOptions.publishMicrophoneTrack = isRoomOwner
        if (!isRoomOwner) {
            channelMediaOptions.audienceLatencyLevel = AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
        }

        if (isRoomOwner) {
            mRtcEngine.joinChannelEx(RtcEngineInstance.generalRtcToken(), mMainRtcConnection, channelMediaOptions, eventListener)
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
        mRtcEngine.setupLocalVideo(local)
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

    private fun animateUserEntryExit(userName: String, view: View) {
        val fadeIn = AnimatorInflater.loadAnimator(context, R.animator.commerce_slide_and_fade_in)
        val fadeOut = AnimatorInflater.loadAnimator(context, R.animator.commerce_fade_out)

        val spannableString = SpannableString(activity?.getString(R.string.commerce_user_joined, userName))
        val styleSpan = StyleSpan(Typeface.BOLD)
        spannableString.setSpan(styleSpan, 0, userName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        mBinding.userView.text = spannableString
        fadeIn.setTarget(mBinding.userView)
        fadeIn.start()

        view.postDelayed({
            fadeOut.setTarget(mBinding.userView)
            fadeOut.start()
        }, 1500)
    }

}