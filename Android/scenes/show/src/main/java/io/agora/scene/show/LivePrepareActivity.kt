package io.agora.scene.show

import AGManifest
import AGResourceManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.faceunity.wrapper.faceunity
import io.agora.rtc2.Constants
import io.agora.rtc2.RtcConnection
import io.agora.rtc2.video.VideoCanvas
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.DynamicLoadUtil
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.show.databinding.ShowLivePrepareActivityBinding
import io.agora.scene.show.debugSettings.DebugSettingDialog
import io.agora.scene.show.service.ShowRoomDetailModel
import io.agora.scene.show.service.ShowServiceProtocol
import io.agora.scene.show.widget.BeautyDialog
import io.agora.scene.show.widget.PresetDialog
import io.agora.scene.widget.dialog.PermissionLeakDialog
import io.agora.scene.widget.utils.StatusBarUtil
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Live prepare activity
 *
 * @constructor Create empty Live prepare activity
 */
@RequiresApi(Build.VERSION_CODES.M)
class LivePrepareActivity : BaseViewBindingActivity<ShowLivePrepareActivityBinding>() {
    private val tag = "LivePrepareActivity"
    /**
     * M service
     */
    private val mService by lazy { ShowServiceProtocol.get() }

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
     * M beauty processor
     */
    private val mBeautyProcessor by lazy { RtcEngineInstance.beautyProcessor }

    /**
     * M rtc engine
     */
    private val mRtcEngine by lazy { RtcEngineInstance.rtcEngine }

    /**
     * Is finish to live detail
     */
    private var isFinishToLiveDetail = false

    private var view: View? = null

    /**
     * Get view binding
     *
     * @param inflater
     * @return
     */
    override fun getViewBinding(inflater: LayoutInflater): ShowLivePrepareActivityBinding {
        return ShowLivePrepareActivityBinding.inflate(inflater)
    }

    override fun onBackPressed() {

    }

    private fun setTips(tips: String) {
        binding.apply {
            // 创建一个 Drawable 对象并设置它的大小
            val icon = ContextCompat.getDrawable(root.context, R.mipmap.show_live_prepare_ic_tip)
            icon?.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            // 创建一个 SpannableString 并将图标设置在字符串的开始位置
            val spannableString = SpannableString("  $tips")
            val imageSpan = ImageSpan(icon!!, ImageSpan.ALIGN_BASELINE)
            spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            // 设置 SpannableString 到 TextView
            tvTip.text = spannableString
        }
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
        setTips(getString(R.string.show_live_prepare_tip))
        binding.ivRoomCover.setImageResource(getThumbnailIcon(mThumbnailId))
        binding.tvRoomId.text = getString(R.string.show_room_id, mRoomId)
        binding.etRoomName.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                mInputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
        binding.ivClose.setOnClickListener {
            RtcEngineInstance.releaseBeautyProcessor()
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
        }
        binding.tvBeauty.setOnClickListener {
            showBeautyDialog()
        }
        binding.tvSetting.setOnClickListener {
            if (AgoraApplication.the().isDebugModeOpen) {
                showDebugModeDialog()
            } else {
                showPresetDialog()
            }
        }

        binding.tvContent.text =
            String.format(resources.getString(R.string.show_beauty_loading), "", "0%")

        if (BuildConfig.BEAUTY_RESOURCE.isEmpty()) {
            binding.statusPrepareViewLrc.isVisible = false
            mBeautyProcessor.initialize(mRtcEngine)
            if (mRtcEngine.queryDeviceScore() < 75) {
                mBeautyProcessor.setBeautyEnable(false)
            } else {
                mBeautyProcessor.setBeautyEnable(true)
            }
            mBeautyProcessor.getBeautyAPI().setupLocalVideo(SurfaceView(this).apply {
                view = this
                binding.flVideoContainer.addView(this)
            }, Constants.RENDER_MODE_HIDDEN)
        } else {
            mRtcEngine.setupLocalVideo(VideoCanvas(SurfaceView(this).apply {
                view = this
                binding.flVideoContainer.addView(this)
            }))

            downloadBeautyResource()
        }

        toggleVideoRun = Runnable {
            mBeautyProcessor.reset()
            onPresetNetworkModeSelected()
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
     * Show debug mode dialog
     *
     */
    private fun showDebugModeDialog() = DebugSettingDialog(this).show()

    /**
     * On resume
     *
     */
    override fun onResume() {
        super.onResume()
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
     * Show beauty dialog
     *
     */
    private fun showBeautyDialog() {
        BeautyDialog(this).apply {
            setBeautyProcessor(mBeautyProcessor)
            setOnDismissListener {
                binding.clController.isVisible = true
            }
            binding.clController.isVisible = false
            show()
        }
    }

    /**
     * Copy2clipboard
     *
     * @param roomId
     */
    private fun copy2Clipboard(roomId: String) {
        val clipboardManager = getSystemService(ClipboardManager::class.java)
        clipboardManager.setPrimaryClip(ClipData.newPlainText(roomId, roomId))
        ToastUtils.showToast(R.string.show_live_prepare_room_clipboard_copyed)
    }

    /**
     * Create and start live
     *
     * @param roomName
     */
    private fun createAndStartLive(roomName: String) {
        if (TextUtils.isEmpty(roomName)) {
            ToastUtils.showToast(R.string.show_live_prepare_room_empty)
            binding.etRoomName.requestFocus()
            mInputMethodManager.showSoftInput(binding.etRoomName, 0)
            return
        }

        binding.btnStartLive.isEnabled = false

        mayFetchUniversalToken {
            isFinishToLiveDetail = true
            LiveDetailActivity.launch(this@LivePrepareActivity, ShowRoomDetailModel(
                mRoomId,
                roomName,
                0,
                UserManager.getInstance().user.id.toString(),
                mThumbnailId,
                UserManager.getInstance().user.headUrl,
                UserManager.getInstance().user.name,
                VideoSetting.isPureMode,
                TimeUtils.currentTimeMillis().toDouble(),
                TimeUtils.currentTimeMillis().toDouble(),
            ))
            finish()
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
                ShowLogger.d("RoomListActivity", "generateToken success：$it， uid：$localUId")
                RtcEngineInstance.setupGeneralToken(it)
                complete.invoke()
            },
            failure = {
                ShowLogger.e("RoomListActivity", it, "generateToken failure：$it")
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
        "0" -> R.mipmap.show_room_cover_0
        "1" -> R.mipmap.show_room_cover_1
        "2" -> R.mipmap.show_room_cover_2
        "3" -> R.mipmap.show_room_cover_3
        else -> R.mipmap.show_room_cover_0
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

    private fun downloadBeautyResource() {
        binding.tvSetting.isEnabled = false
        binding.tvBeauty.isEnabled = false
        binding.tvRotate.isEnabled = false
        binding.btnStartLive.isEnabled = false

        val beautyResource = AGResourceManager(this)
        var manifest: AGManifest? = null
        beautyResource.checkResource(BuildConfig.BEAUTY_RESOURCE)

        lifecycleScope.launch {
            var downloadSuccess = false

            beautyResource.downloadManifest(
                url = BuildConfig.BEAUTY_RESOURCE,
                progressHandler = {
                    ShowLogger.d(tag, "download process: $it")
                },
                completionHandler = { agManifest, e ->
                    if (e == null) {
                        ShowLogger.d(tag, "download success: $agManifest")
                        manifest = agManifest
                    } else {
                        ShowLogger.d(tag, "download failed: ${e.message}")
                        binding.statusPrepareViewLrc.isVisible = false
                        ToastUtils.showToastLong(R.string.show_beauty_loading_failed)
                        downloadSuccess = false
                    }
                }
            )

            manifest?.files?.forEach { resource ->
                if (resource.uri == "beauty_faceunity") {

                    ShowLogger.d(tag, "Processing ${resource.url}")
                    binding.statusPrepareViewLrc.isVisible = true
                    binding.pbLoading.progress = 0
                    binding.tvContent.text =
                        String.format(
                            resources.getString(R.string.show_beauty_loading),
                            "faceunity",
                            "0%"
                        )

                    beautyResource.downloadAndUnZipResource(
                        resource = resource,
                        progressHandler = {
                            binding.pbLoading.progress = it
                            binding.tvContent.text = String.format(
                                resources.getString(R.string.show_beauty_loading),
                                "faceunity",
                                "$it%"
                            )
                        },
                        completionHandler = { _, e ->
                            if (e == null) {
                                ShowLogger.d(tag, "download success: ${resource.uri}")
                                downloadSuccess = true
                            } else {
                                ShowLogger.e(tag, e, "download failed: ${e.message}")
                                binding.statusPrepareViewLrc.isVisible = false
                                ToastUtils.showToastLong(R.string.show_beauty_loading_failed)
                                downloadSuccess = false
                            }
                        }
                    )
                }
            }

            if (!downloadSuccess) {
                return@launch
            }

            val arch = System.getProperty("os.arch")
            if (arch == "armv7") {
                DynamicLoadUtil.loadSoFile(this@LivePrepareActivity, "${this@LivePrepareActivity.getExternalFilesDir("")?.absolutePath}/assets/beauty_faceunity/lib/armeabi-v7a/", "libfuai")
                DynamicLoadUtil.loadSoFile(this@LivePrepareActivity, "${this@LivePrepareActivity.getExternalFilesDir("")?.absolutePath}/assets/beauty_faceunity/lib/armeabi-v7a/", "libCNamaSDK")
            } else if (arch == "aarch64") {
                DynamicLoadUtil.loadSoFile(this@LivePrepareActivity, "${this@LivePrepareActivity.getExternalFilesDir("")?.absolutePath}/assets/beauty_faceunity/lib/arm64-v8a/", "libfuai")
                DynamicLoadUtil.loadSoFile(this@LivePrepareActivity, "${this@LivePrepareActivity.getExternalFilesDir("")?.absolutePath}/assets/beauty_faceunity/lib/arm64-v8a/", "libCNamaSDK")
            }
            faceunity.LoadConfig.loadLibrary(this@LivePrepareActivity.getDir("libs", Context.MODE_PRIVATE).absolutePath)
            RtcEngineInstance.beautySoLoaded = true

            binding.statusPrepareViewLrc.isVisible = false
            binding.tvSetting.isEnabled = true
            binding.tvBeauty.isEnabled = true
            binding.tvRotate.isEnabled = true
            binding.btnStartLive.isEnabled = true

            mBeautyProcessor.initialize(mRtcEngine)
            if (mRtcEngine.queryDeviceScore() < 75) {
                mBeautyProcessor.setBeautyEnable(false)
            } else {
                mBeautyProcessor.setBeautyEnable(true)
            }
            mBeautyProcessor.getBeautyAPI().setupLocalVideo(SurfaceView(this@LivePrepareActivity).apply {
                view = this
                binding.flVideoContainer.addView(this)
            }, Constants.RENDER_MODE_HIDDEN)
        }
    }
}