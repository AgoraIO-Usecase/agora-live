package io.agora.scene.show

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import io.agora.scene.base.SceneAliveTime
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.show.databinding.ShowRoomItemBinding
import io.agora.scene.show.databinding.ShowRoomListActivityBinding
import io.agora.scene.show.service.ShowRoomDetailModel
import io.agora.scene.show.service.ShowServiceProtocol
import io.agora.scene.show.videoLoaderAPI.OnLiveRoomItemTouchEventHandler
import io.agora.scene.show.videoLoaderAPI.OnRoomListScrollEventHandler
import io.agora.scene.show.videoLoaderAPI.VideoLoader
import io.agora.scene.show.widget.PresetAudienceDialog
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
    private val mBinding by lazy { ShowRoomListActivityBinding.inflate(LayoutInflater.from(this)) }

    /**
     * M room adapter
     */
    private lateinit var mRoomAdapter: BindingSingleAdapter<ShowRoomDetailModel, ShowRoomItemBinding>

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
    private val roomDetailModelList = mutableListOf<ShowRoomDetailModel>()

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
        mService.startCloudPlayer()
        fetchUniversalToken ({
            val roomList = arrayListOf<VideoLoader.RoomInfo>( )
            roomDetailModelList.forEach { room ->
                roomList.add(
                    VideoLoader.RoomInfo(
                        room.roomId,
                        arrayListOf(
                            VideoLoader.AnchorInfo(
                                room.roomId,
                                room.ownerId.toInt(),
                                RtcEngineInstance.generalToken()
                            )
                        )
                    )
                )
            }
            onRoomListScrollEventHandler?.updateRoomList(roomList)
        })
        initView()
        initVideoSettings()

        SceneAliveTime.fetchShowAliveTime ({ show, pk ->
            ShowLogger.d("RoomListActivity", "fetchShowAliveTime: show: $show, pk: $pk")
            ShowServiceProtocol.ROOM_AVAILABLE_DURATION = show * 1000L
            ShowServiceProtocol.PK_AVAILABLE_DURATION = pk * 1000L
        })
    }

    /**
     * Init view
     */
    private fun initView() {
        onRoomListScrollEventHandler = object: OnRoomListScrollEventHandler(mRtcEngine, UserManager.getInstance().user.id.toInt()) {}
        mBinding.titleView.setLeftClick {
            mService.destroy()
            RtcEngineInstance.destroy()
            RtcEngineInstance.setupGeneralToken("")
            finish()
        }
        mBinding.titleView.setRightIconClick {
            showAudienceSetting()
        }
        mRoomAdapter = object : BindingSingleAdapter<ShowRoomDetailModel, ShowRoomItemBinding>() {
            override fun onBindViewHolder(
                holder: BindingViewHolder<ShowRoomItemBinding>,
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
            mService.getRoomList(
                success = {
                    roomDetailModelList.clear()
                    roomDetailModelList.addAll(it)
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
                                            RtcEngineInstance.generalToken()
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
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun updateRoomItem(
        list: List<ShowRoomDetailModel>,
        position: Int,
        binding: ShowRoomItemBinding,
        roomInfo: ShowRoomDetailModel
    ) {
        binding.tvRoomName.text = roomInfo.roomName
        binding.tvRoomId.text = getString(R.string.show_room_id, roomInfo.roomId)
        binding.ivCover.setImageResource(roomInfo.getThumbnailIcon())

        val onTouchEventHandler = object : OnLiveRoomItemTouchEventHandler(
            mRtcEngine,
            VideoLoader.RoomInfo(
                roomInfo.roomId,
                arrayListOf(
                    VideoLoader.AnchorInfo(
                        roomInfo.roomId,
                        roomInfo.ownerId.toInt(),
                        RtcEngineInstance.generalToken()
                    )
                )
            ),
            UserManager.getInstance().user.id.toInt()) {
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                val isRoomOwner = roomInfo.ownerId == UserManager.getInstance().user.id.toString()
                if (isRoomOwner) {
                    if (event!!.action == MotionEvent.ACTION_UP) {
                        ToastUtils.showToast(R.string.show_broadcaster_bad_exit)
                    }
                } else {
                    when (event!!.action) {
                        MotionEvent.ACTION_DOWN -> {
                            if (RtcEngineInstance.generalToken() == "") {
                                fetchUniversalToken({
                                }, {
                                    ToastUtils.showToast("Fetch Token Failed")
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
                            if (RtcEngineInstance.generalToken() != "") {
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
     * On back pressed
     *
     */
    override fun onBackPressed() {
        mService.destroy()
        RtcEngineInstance.destroy()
        RtcEngineInstance.setupGeneralToken("")
        finish()
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