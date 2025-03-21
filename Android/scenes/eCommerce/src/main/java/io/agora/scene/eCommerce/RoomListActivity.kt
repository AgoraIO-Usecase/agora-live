package io.agora.scene.eCommerce

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import io.agora.scene.base.AgoraTokenType
import io.agora.scene.base.SceneConfigManager
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.TokenGeneratorType
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.eCommerce.databinding.CommerceRoomItemBinding
import io.agora.scene.eCommerce.databinding.CommerceRoomListActivityBinding
import io.agora.scene.eCommerce.service.RoomDetailModel
import io.agora.scene.eCommerce.service.ShowServiceProtocol
import io.agora.scene.eCommerce.videoLoaderAPI.OnLiveRoomItemTouchEventHandler
import io.agora.scene.eCommerce.videoLoaderAPI.OnRoomListScrollEventHandler
import io.agora.scene.eCommerce.videoLoaderAPI.VideoLoader
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
    private lateinit var mRoomAdapter: BindingSingleAdapter<RoomDetailModel, CommerceRoomItemBinding>

    /**
     * M service
     */
    private val mService by lazy { ShowServiceProtocol.getImplInstance() }

    /**
     * M rtc engine
     */
    private val mRtcEngine by lazy { RtcEngineInstance.rtcEngine }

    /**
     * Room detail model list
     */
    private val mRoomList = mutableListOf<RoomDetailModel>()

    /**
     * Is first load
     */
    private var isFirstLoad = true

    /**
     * Room List Scroll Event Handler
     */
    private var onRoomListScrollEventHandler: OnRoomListScrollEventHandler? = null

    /**
     * On create
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtil.hideStatusBar(window, true)
        setContentView(mBinding.root)
        initView()
        initVideoSettings()

        ShowServiceProtocol.ROOM_AVAILABLE_DURATION = SceneConfigManager.ecommerce * 1000L
    }

    /**
     * Init view
     */
    private fun initView() {
        onRoomListScrollEventHandler = object: OnRoomListScrollEventHandler(mRtcEngine, UserManager.getInstance().user.id.toInt()) {}
        mBinding.titleView.setLeftClick {
            ShowServiceProtocol.destroy()
            RtcEngineInstance.destroy()
            RtcEngineInstance.setupGeneralRtcToken("")
            RtcEngineInstance.setupGeneralRtmToken("")
            finish()
        }
        mRoomAdapter = object : BindingSingleAdapter<RoomDetailModel, CommerceRoomItemBinding>() {
            override fun onBindViewHolder(
                holder: BindingViewHolder<CommerceRoomItemBinding>,
                position: Int
            ) {
                updateRoomItem(mDataList, position, holder.binding, getItem(position) ?: return)
            }
        }
        mBinding.rvRooms.adapter = mRoomAdapter
        mBinding.rvRooms.addOnScrollListener(onRoomListScrollEventHandler as OnRoomListScrollEventHandler)

        mBinding.smartRefreshLayout.setEnableLoadMore(false)
        mBinding.smartRefreshLayout.setEnableRefresh(true)
        mBinding.smartRefreshLayout.setOnRefreshListener {
            fetchUniversalToken ({
                mService.fetchRoomList(
                    success = {
                        val filterRoom = it.filter { it.ownerId != UserManager.getInstance().user.id.toString() }
                        mRoomList.clear()
                        mRoomList.addAll(filterRoom)
                        if (isFirstLoad) {
                            val roomList = arrayListOf<VideoLoader.RoomInfo>( )
                            it.forEach { room ->
                                roomList.add(
                                    VideoLoader.RoomInfo(
                                        room.roomId,
                                        arrayListOf(
                                            VideoLoader.AnchorInfo(
                                                room.roomId,
                                                room.ownerId.toInt(),
                                                RtcEngineInstance.generalRtcToken()
                                            )
                                        )
                                    )
                                )
                            }
                            onRoomListScrollEventHandler?.updateRoomList(roomList)
                            isFirstLoad = false
                        }
                        updateList(it)
                    },
                    error = {
                        updateList(emptyList())
                    }
                )
            }) {
                updateList(emptyList())
            }
        }
        mBinding.smartRefreshLayout.autoRefresh()
        mBinding.btnCreateRoom.setOnClickListener { goLivePrepareActivity() }
        mBinding.btnCreateRoom2.setOnClickListener { goLivePrepareActivity() }
    }

    override fun onRestart() {
        super.onRestart()
        mBinding.smartRefreshLayout.autoRefresh()
    }

    /**
     * Update list
     *
     * @param data
     */
    private fun updateList(data: List<RoomDetailModel>) {
        mBinding.tvTips1.isVisible = data.isEmpty()
        mBinding.ivBgMobile.isVisible = data.isEmpty()
        mBinding.btnCreateRoom2.isVisible = false
        mBinding.btnCreateRoom.isVisible = true
        mBinding.rvRooms.isVisible = data.isNotEmpty()
        mRoomAdapter.resetAll(data)

        mBinding.smartRefreshLayout.finishRefresh()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateRoomItem(
        list: List<RoomDetailModel>,
        position: Int,
        binding: CommerceRoomItemBinding,
        roomInfo: RoomDetailModel
    ) {
        binding.tvRoomName.text = roomInfo.roomName
        binding.tvRoomId.text = getString(R.string.commerce_room_id, roomInfo.roomId)
        binding.tvUserCount.text = getString(R.string.commerce_user_count, roomInfo.roomUserCount)
        binding.ivCover.setImageResource(roomInfo.getThumbnailIcon())

        val onTouchEventHandler = object : OnLiveRoomItemTouchEventHandler(
            mRtcEngine,
            VideoLoader.RoomInfo(
                roomInfo.roomId,
                arrayListOf(
                    VideoLoader.AnchorInfo(
                        roomInfo.roomId,
                        roomInfo.ownerId.toInt(),
                        RtcEngineInstance.generalRtcToken()
                    )
                )
            ),
            UserManager.getInstance().user.id.toInt()) {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val isRoomOwner = roomInfo.ownerId.toLong() == UserManager.getInstance().user.id
                if (isRoomOwner) {
                    if (event!!.action == MotionEvent.ACTION_UP) {
                        mService.deleteRoom(roomInfo.roomId) {
                            mBinding.smartRefreshLayout.autoRefresh()
                        }
                        ToastUtils.showToast(R.string.commerce_broadcaster_bad_exit)
                    }
                } else {
                    when (event!!.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if (RtcEngineInstance.generalRtcToken() == "") {
                                fetchUniversalToken({
                                }, {
                                    ToastUtils.showToast(getString(R.string.commerce_Fetch_token_failed))
                                })
                            } else {
                                if (RtcEngineInstance.rtcEngine.queryDeviceScore() < 75) {
                                    RtcEngineInstance.rtcEngine.setParameters("{\"che.hardware_decoding\": 1}")
                                    RtcEngineInstance.rtcEngine.setParameters("{\"rtc.video.decoder_out_byte_frame\": true}")
                                }
                                super.onTouch(v, event)
                            }
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            super.onTouch(v, event)
                        }
                        MotionEvent.ACTION_UP -> {
                            if (RtcEngineInstance.generalRtcToken() != "") {
                                super.onTouch(v, event)
                                goLiveDetailActivity(list, position, roomInfo)
                            }
                        }
                    }
                }
                return true
            }

            override fun onRequireRenderVideo(info: VideoLoader.AnchorInfo): VideoLoader.VideoCanvasContainer? {
                Log.d("RoomListActivity", "onRequireRenderVideo")
                return null
            }
        }
        binding.root.setOnTouchListener(onTouchEventHandler)
    }

    /**
     * Go live prepare activity
     *
     */
    private fun goLivePrepareActivity() {
        Intent(this, LivePrepareActivity::class.java).let {
            startActivity(it)
        }
    }

    /**
     * Go live detail activity
     *
     * @param list
     * @param position
     * @param roomInfo
     */
    private fun goLiveDetailActivity(list: List<RoomDetailModel>, position: Int, roomInfo: RoomDetailModel) {
        LiveDetailActivity.launch(
            this,
            ArrayList(list),
            position,
            roomInfo.ownerId.toLong() != UserManager.getInstance().user.id
        )
    }

    /**
     * On Back Pressed
     *
     */
    override fun onBackPressed() {
        super.onBackPressed()
        ShowServiceProtocol.destroy()
        RtcEngineInstance.destroy()
        RtcEngineInstance.setupGeneralRtcToken("")
        RtcEngineInstance.setupGeneralRtmToken("")
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
        if (RtcEngineInstance.generalRtcToken() != "") {
            success.invoke()
            return
        }
        val localUId = UserManager.getInstance().user.id
        TokenGenerator.generateTokens("", localUId.toString(),
            TokenGeneratorType.Token007,
            arrayOf(
                AgoraTokenType.Rtc,
                AgoraTokenType.Rtm
            ),
            success = {
                //ShowLogger.d("RoomListActivity", "generateToken success：$it， uid：$localUId")
                RtcEngineInstance.setupGeneralRtcToken(it)
                RtcEngineInstance.setupGeneralRtmToken(it)
                success.invoke()
            },
            failure = {
                //ShowLogger.e("RoomListActivity", it, "generateToken failure：$it")
                ToastUtils.showToast(it?.message ?: getString(R.string.commerce_generate_token_failure))
                error?.invoke(it)
            })
    }

    /**
     * Init video settings
     *
     */
    private fun initVideoSettings() {
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