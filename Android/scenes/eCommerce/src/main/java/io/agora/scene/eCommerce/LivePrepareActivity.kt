package io.agora.scene.eCommerce

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcConnection
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.VideoCanvas
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.eCommerce.databinding.CommerceLivePrepareActivityBinding
import io.agora.scene.eCommerce.service.ShowServiceProtocol
import io.agora.scene.eCommerce.widget.PresetDialog
import io.agora.scene.widget.dialog.PermissionLeakDialog
import io.agora.scene.widget.utils.StatusBarUtil
import kotlin.random.Random

/**
 * Live prepare activity
 *
 * @constructor Create empty Live prepare activity
 */
@RequiresApi(Build.VERSION_CODES.M)
class LivePrepareActivity : BaseViewBindingActivity<CommerceLivePrepareActivityBinding>() {

    /**
     * M service
     */
    private val mService by lazy { ShowServiceProtocol.getImplInstance() }

    /**
     * M input method manager
     */
    private val mInputMethodManager by lazy { getSystemService(InputMethodManager::class.java) }

    /**
     * M thumbnail id
     */
    private val mThumbnailId by lazy { getRandomThumbnailId() }

    /**
     * M room id
     */
    private val mRoomId by lazy { getRandomRoomId() }

    /**
     * M rtc engine
     */
    private val mRtcEngine by lazy { RtcEngineInstance.rtcEngine }

    /**
     * Is finish to live detail
     */
    private var isFinishToLiveDetail = false

    /**
     * Get view binding
     *
     * @param inflater
     * @return
     */
    override fun getViewBinding(inflater: LayoutInflater): CommerceLivePrepareActivityBinding {
        return CommerceLivePrepareActivityBinding.inflate(inflater)
    }

    override fun onBackPressed() {

    }

    /**
     * Init view
     *
     * @param savedInstanceState
     */
    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        StatusBarUtil.hideStatusBar(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v: View?, insets: WindowInsetsCompat ->
            val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.setPaddingRelative(inset.left, 0, inset.right, inset.bottom)
            WindowInsetsCompat.CONSUMED
        }
        binding.ivRoomCover.setImageResource(getThumbnailIcon(mThumbnailId))
        binding.tvRoomId.text = getString(R.string.commerce_room_id, mRoomId)
        binding.etRoomName.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mInputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        binding.ivClose.setOnClickListener {
            finish()
        }

        binding.ivCopy.setOnClickListener {
            // Copy to system clipboard
            copy2Clipboard(mRoomId)
        }
        binding.btnStartLive.setOnClickListener {
            createAndStartLive(binding.etRoomName.text.toString())
        }
        binding.tvRotate.setOnClickListener {
            mRtcEngine.switchCamera()
//            RtcEngineInstance.isFrontCamera = !RtcEngineInstance.isFrontCamera
        }
        binding.tvSetting.setOnClickListener {
            showPresetDialog()
        }

        toggleVideoRun = Runnable {
            onPresetNetworkModeSelected()
            initRtcEngine()
        }
        requestCameraPermission(true)
    }

    /**
     * Toggle video run
     */
    private var toggleVideoRun: Runnable? = null

    /**
     * On permission dined
     *
     * @param permission
     */
    override fun onPermissionDined(permission: String?) {
        PermissionLeakDialog(this).show(permission,
            { getPermissions() }
        ) { launchAppSetting(permission) }
    }

    /**
     * Get permissions
     *
     */
    override fun getPermissions() {
        if (toggleVideoRun != null) {
            toggleVideoRun?.run()
            toggleVideoRun = null
        }
    }

    /**
     * Show preset dialog
     *
     */
    private fun showPresetDialog() = PresetDialog(this, mRtcEngine.queryDeviceScore(), RtcConnection(mRoomId, UserManager.getInstance().user.id.toInt())).show()

    /**
     * On resume
     *
     */
    override fun onResume() {
        super.onResume()
        mRtcEngine.setParameters("{\"rtc.camera_capture_mirror_mode\":0}")
        mRtcEngine.startPreview()
    }

    /**
     * On pause
     *
     */
    override fun onPause() {
        super.onPause()
        if (!isFinishToLiveDetail) {
            mRtcEngine.stopPreview()
        }
    }

    /**
     * Init rtc engine
     *
     */
    private fun initRtcEngine() {
        val view = TextureView(this)
        binding.flVideoContainer.addView(view)
        val canvas = VideoCanvas(view, 0, 0)
        canvas.mirrorMode = Constants.VIDEO_MIRROR_MODE_DISABLED
        canvas.renderMode = Constants.RENDER_MODE_HIDDEN
        mRtcEngine.setupLocalVideo(canvas)
        mRtcEngine.startPreview()
    }

    /**
     * Copy2clipboard
     *
     * @param roomId
     */
    private fun copy2Clipboard(roomId: String) {
        val clipboardManager = getSystemService(ClipboardManager::class.java)
        clipboardManager.setPrimaryClip(ClipData.newPlainText(roomId, roomId))
        ToastUtils.showToast(R.string.commerce_live_prepare_room_clipboard_copyed)
    }

    /**
     * Create and start live
     *
     * @param roomName
     */
    private fun createAndStartLive(roomName: String) {
        if (TextUtils.isEmpty(roomName)) {
            ToastUtils.showToast(R.string.commerce_live_prepare_room_empty)
            binding.etRoomName.requestFocus()
            mInputMethodManager.showSoftInput(binding.etRoomName, 0)
            return
        }

        binding.btnStartLive.isEnabled = false

        mayFetchUniversalToken {
            mService.createRoom(mRoomId, roomName, mThumbnailId, {
                runOnUiThread {
                    isFinishToLiveDetail = true
                    LiveDetailActivity.launch(this@LivePrepareActivity, it)
                    finish()
                }
            }, {
                runOnUiThread {
                    ToastUtils.showToast(it.message)
                    binding.btnStartLive.isEnabled = true
                }
            })
        }
    }

    /**
     * Fetch universal token
     *
     * @param complete
     * @receiver
     */
    private fun mayFetchUniversalToken(
        complete: () -> Unit
    ) {
        if(RtcEngineInstance.generalToken().isNotEmpty()){
            complete.invoke()
            return
        }
        val localUId = UserManager.getInstance().user.id
        TokenGenerator.generateToken("", localUId.toString(),
            TokenGenerator.TokenGeneratorType.Token007,
            TokenGenerator.AgoraTokenType.Rtc,
            success = {
                //ShowLogger.d("RoomListActivity", "generateToken success：$it， uid：$localUId")
                RtcEngineInstance.setupGeneralToken(it)
                complete.invoke()
            },
            failure = {
                //ShowLogger.e("RoomListActivity", it, "generateToken failure：$it")
                ToastUtils.showToast(it?.message ?: "generate token failure")
            })
    }


    /**
     * Get random room id
     *
     */
    private fun getRandomRoomId() =
        (Random(TimeUtils.currentTimeMillis()).nextInt(10000) + 100000).toString()

    /**
     * Get random thumbnail id
     *
     */
    private fun getRandomThumbnailId() =
        Random(TimeUtils.currentTimeMillis()).nextInt(0, 3).toString()

    /**
     * Get thumbnail icon
     *
     * @param thumbnailId
     */
    @DrawableRes
    private fun getThumbnailIcon(thumbnailId: String) = when (thumbnailId) {
        "0" -> R.drawable.commerce_room_cover_0
        "1" -> R.drawable.commerce_room_cover_1
        "2" -> R.drawable.commerce_room_cover_2
        "3" -> R.drawable.commerce_room_cover_3
        else -> R.drawable.commerce_room_cover_0
    }

    private fun onPresetNetworkModeSelected() {
        val broadcastStrategy = VideoSetting.BroadcastStrategy.Smooth
        val network = VideoSetting.NetworkLevel.Good

        val deviceLevel = if (mRtcEngine.queryDeviceScore() >= 90) {
            VideoSetting.DeviceLevel.High
        } else if (mRtcEngine.queryDeviceScore() >= 75) {
            VideoSetting.DeviceLevel.Medium
        } else {
            VideoSetting.DeviceLevel.Low
        }

        VideoSetting.updateBroadcastSetting(deviceLevel, network, broadcastStrategy, isJoinedRoom = false, isByAudience = false, RtcConnection(mRoomId, UserManager.getInstance().user.id.toInt()))
    }
}