package io.agora.scene.eCommerce

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcConnection
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.eCommerce.databinding.CommerceRoomItemBinding
import io.agora.scene.eCommerce.databinding.CommerceRoomListActivityBinding
import io.agora.scene.eCommerce.service.ShowRoomDetailModel
import io.agora.scene.eCommerce.service.ShowServiceProtocol
import io.agora.scene.eCommerce.videoSwitcherAPI.VideoSwitcher
import io.agora.scene.eCommerce.widget.PresetAudienceDialog
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder
import io.agora.scene.widget.utils.StatusBarUtil

/**
 * Room list activity
 *
 * @constructor Create empty Room list activity
 */
class RoomListActivity : AppCompatActivity() {

    /**
     * M binding
     */
    private val mBinding by lazy { CommerceRoomListActivityBinding.inflate(LayoutInflater.from(this)) }

    /**
     * M room adapter
     */
    private lateinit var mRoomAdapter: BindingSingleAdapter<ShowRoomDetailModel, CommerceRoomItemBinding>

    /**
     * M service
     */
    private val mService by lazy { ShowServiceProtocol.getImplInstance() }

    /**
     * M rtc video switcher
     */
    private val mRtcVideoSwitcher by lazy { VideoSwitcher.getImplInstance(mRtcEngine) }

    /**
     * M rtc engine
     */
    private val mRtcEngine by lazy { RtcEngineInstance.rtcEngine }

    /**
     * Room detail model list
     */
    private val roomDetailModelList = mutableListOf<ShowRoomDetailModel>()

    /**
     * Is first load
     */
    private var isFirstLoad = true

    /**
     * On create
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtil.hideStatusBar(window, true)
        setContentView(mBinding.root)
        mService.startCloudPlayer()
        fetchUniversalToken ({
            preloadChannels()
        })
        initView()
        initVideoSettings()
    }

    /**
     * Init view
     *
     */
    private fun initView() {
        mBinding.titleView.setLeftClick { finish() }
        mBinding.titleView.setRightIconClick {
            showAudienceSetting()
        }
        mRoomAdapter = object : BindingSingleAdapter<ShowRoomDetailModel, CommerceRoomItemBinding>() {
            override fun onBindViewHolder(
                holder: BindingViewHolder<CommerceRoomItemBinding>,
                position: Int
            ) {
                updateRoomItem(mDataList, position, holder.binding, getItem(position) ?: return)
            }
        }
        mBinding.rvRooms.adapter = mRoomAdapter
        mBinding.rvRooms.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    val firstVisibleItem = layoutManager.findFirstVisibleItemPosition()
                    val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                    Log.d("RoomListActivity", "firstVisible $firstVisibleItem, lastVisible $lastVisibleItem")
                    val firstPreloadPosition = if (firstVisibleItem - 7 < 0) 0 else firstVisibleItem - 7
                    val lastPreloadPosition = if (firstPreloadPosition + 19 >= roomDetailModelList.size)
                        roomDetailModelList.size - 1 else firstPreloadPosition + 19
                    preloadChannels(firstPreloadPosition, lastPreloadPosition)
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

            }
        })

        mBinding.smartRefreshLayout.setEnableLoadMore(false)
        mBinding.smartRefreshLayout.setEnableRefresh(true)
        mBinding.smartRefreshLayout.setOnRefreshListener {
            mService.getRoomList(
                success = {
                    roomDetailModelList.clear()
                    roomDetailModelList.addAll(it)
                    if (isFirstLoad) {
                        preloadChannels()
                        isFirstLoad = false
                    }
                    updateList(it)
                },
                error = {
                    updateList(emptyList())
                }
            )
        }
        mBinding.smartRefreshLayout.autoRefresh()

        mBinding.btnCreateRoom.setOnClickListener { goLivePrepareActivity() }
        mBinding.btnCreateRoom2.setOnClickListener { goLivePrepareActivity() }
    }

    /**
     * Update list
     *
     * @param data
     */
    private fun updateList(data: List<ShowRoomDetailModel>) {
        mBinding.tvTips1.isVisible = data.isEmpty()
        mBinding.ivBgMobile.isVisible = data.isEmpty()
        mBinding.btnCreateRoom2.isVisible = data.isEmpty()
        mBinding.btnCreateRoom.isVisible = data.isNotEmpty()
        mBinding.rvRooms.isVisible = data.isNotEmpty()
        mRoomAdapter.resetAll(data)

        mBinding.smartRefreshLayout.finishRefresh()

        val preloadCount = 3
        mRtcVideoSwitcher.setPreloadCount(preloadCount)
        mRtcVideoSwitcher.preloadConnections(data.map {
            RtcConnection(
                it.roomId,
                UserManager.getInstance().user.id.toInt()
            )
        })
    }

    /**
     * Update room item
     *
     * @param list
     * @param position
     * @param binding
     * @param roomInfo
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun updateRoomItem(
        list: List<ShowRoomDetailModel>,
        position: Int,
        binding: CommerceRoomItemBinding,
        roomInfo: ShowRoomDetailModel
    ) {
        binding.tvRoomName.text = roomInfo.roomName
        binding.tvRoomId.text = getString(R.string.commerce_room_id, roomInfo.roomId)
        binding.tvUserCount.text = getString(R.string.commerce_user_count, roomInfo.roomUserCount)
        binding.ivCover.setImageResource(roomInfo.getThumbnailIcon())

        binding.root.setOnTouchListener { v, event ->
            val rtcConnection =
                RtcConnection(roomInfo.roomId, UserManager.getInstance().user.id.toInt())
            val isRoomOwner = roomInfo.ownerId == UserManager.getInstance().user.id.toString()
            if (isRoomOwner) {
                if (event!!.action == MotionEvent.ACTION_UP) {
                    ToastUtils.showToast(R.string.commerce_broadcaster_bad_exit)
                }
            } else {
                when (event!!.action) {
                    MotionEvent.ACTION_DOWN -> {
                        mRtcVideoSwitcher.preloadConnections(list.map {
                            RtcConnection(
                                it.roomId,
                                UserManager.getInstance().user.id.toInt()
                            )
                        })
                        if (RtcEngineInstance.generalToken() == "") {
                            fetchUniversalToken({
                            }, {
                                ToastUtils.showToast("Fetch Token Failed")
                            })
                        } else {
                            if (mRtcEngine.queryDeviceScore() < 75) {
                                mRtcEngine.setParameters("{\"che.hardware_decoding\": 1}")
                                mRtcEngine.setParameters("{\"rtc.video.decoder_out_byte_frame\": true}")
                            }
                            val channelMediaOptions = ChannelMediaOptions()
                            channelMediaOptions.clientRoleType = Constants.CLIENT_ROLE_AUDIENCE
                            channelMediaOptions.autoSubscribeVideo = true
                            channelMediaOptions.autoSubscribeAudio = true
                            channelMediaOptions.publishCameraTrack = false
                            channelMediaOptions.publishMicrophoneTrack = false
                            channelMediaOptions.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                            mRtcVideoSwitcher.joinChannel(
                                rtcConnection,
                                channelMediaOptions,
                                RtcEngineInstance.generalToken(),
                                null,
                                true
                            )
                            mRtcVideoSwitcher.preJoinChannel(rtcConnection)
                            mRtcEngine.adjustUserPlaybackSignalVolumeEx(roomInfo.ownerId.toInt(), 0, rtcConnection)
                            mService.startCloudPlayer()
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        mRtcVideoSwitcher.leaveChannel(rtcConnection, true)
                    }
                    MotionEvent.ACTION_UP -> {
                        if (RtcEngineInstance.generalToken() != "") {
                            goLiveDetailActivity(list, position, roomInfo)
                        }
                    }
                }
            }
            true
        }
    }

    /**
     * Go live prepare activity
     *
     */
    private fun goLivePrepareActivity() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(this, LivePrepareActivity::class.java).let {
                startActivity(it)
            }
        }
    }

    /**
     * Go live detail activity
     *
     * @param list
     * @param position
     * @param roomInfo
     */
    private fun goLiveDetailActivity(list: List<ShowRoomDetailModel>, position: Int, roomInfo: ShowRoomDetailModel) {
        LiveDetailActivity.launch(
            this,
            ArrayList(list),
            position,
            roomInfo.ownerId != UserManager.getInstance().user.id.toString()
        )
    }

    /**
     * Show audience setting
     *
     */
    private fun showAudienceSetting() {
        PresetAudienceDialog(this).show()
    }

    /**
     * On destroy
     *
     */
    override fun onDestroy() {
        super.onDestroy()
        mService.destroy()
        mRtcVideoSwitcher.unloadConnections()
        VideoSwitcher.release()
        RtcEngineInstance.destroy()
        RtcEngineInstance.setupGeneralToken("")
    }

    /**
     * Fetch universal token
     *
     * @param success
     * @param error
     * @receiver
     */
    private fun fetchUniversalToken(
        success: () -> Unit,
        error: ((Exception?) -> Unit)? = null
    ) {
        val localUId = UserManager.getInstance().user.id
        TokenGenerator.generateToken("", localUId.toString(),
            TokenGenerator.TokenGeneratorType.Token007,
            TokenGenerator.AgoraTokenType.Rtc,
            success = {
                ShowLogger.d("RoomListActivity", "generateToken success：$it， uid：$localUId")
                RtcEngineInstance.setupGeneralToken(it)
                success.invoke()
            },
            failure = {
                ShowLogger.e("RoomListActivity", it, "generateToken failure：$it")
                ToastUtils.showToast(it?.message ?: "generate token failure")
                error?.invoke(it)
            })
    }

    /**
     * Preload channels
     *
     */
    private fun preloadChannels() {
        val generalToken = RtcEngineInstance.generalToken()
        if (roomDetailModelList.isNotEmpty() && generalToken.isNotEmpty()) {
            roomDetailModelList.take(20).forEach { room ->
                val ret = RtcEngineInstance.rtcEngine.preloadChannel(
                    generalToken, room.roomId, UserManager.getInstance().user.id.toInt()
                )
                Log.d("RoomListActivity", "call rtc sdk preloadChannel ${room.roomId} ret:$ret")
            }
        }
    }

    /**
     * Preload channels
     *
     * @param from
     * @param to
     */
    private fun preloadChannels(from: Int, to: Int) {
        val generalToken = RtcEngineInstance.generalToken()
        if (roomDetailModelList.isNotEmpty() && generalToken.isNotEmpty()) {
            val size = roomDetailModelList.size
            for (i in from until to + 1) {
                if (i >= size) return
                val room = roomDetailModelList[i]
                val ret = RtcEngineInstance.rtcEngine.preloadChannel(
                    generalToken, room.roomId, UserManager.getInstance().user.id.toInt()
                )
                Log.d("RoomListActivity", "call rtc sdk preloadChannel ${room.roomId} ret:$ret")
            }
        }
    }

    /**
     * Init video settings
     *
     */
    private fun initVideoSettings() {
        val deviceScore = RtcEngineInstance.rtcEngine.queryDeviceScore()
        val deviceLevel = if (deviceScore >= 90) {
            VideoSetting.updateAudioSetting(SR = VideoSetting.SuperResolution.SR_AUTO)
            VideoSetting.setCurrAudienceEnhanceSwitch(true)
            VideoSetting.DeviceLevel.High
        } else if (deviceScore >= 75) {
            VideoSetting.updateAudioSetting(SR = VideoSetting.SuperResolution.SR_AUTO)
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