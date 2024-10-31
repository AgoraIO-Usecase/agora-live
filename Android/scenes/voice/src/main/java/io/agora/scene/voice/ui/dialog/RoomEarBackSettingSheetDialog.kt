package io.agora.scene.voice.ui.dialog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.media.AudioManager
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ImageSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import io.agora.scene.voice.R
import io.agora.scene.voice.databinding.VoiceDialogChatroomEarbackSettingBinding
import io.agora.scene.voice.rtckit.AgoraEarBackMode
import io.agora.scene.voice.rtckit.AgoraRtcEngineController
import io.agora.scene.voice.ui.dialog.common.CommonFragmentAlertDialog
import io.agora.scene.widget.utils.doOnProgressChanged
import io.agora.voice.common.ui.dialog.BaseFixedHeightSheetDialog

class RoomEarBackSettingSheetDialog : BaseFixedHeightSheetDialog<VoiceDialogChatroomEarbackSettingBinding>() {

    private val mReceiver = HeadphoneReceiver()

    private var mAlertFragmentManager: FragmentManager? = null

    private var mOnEarBackStateChange: (() -> Unit)? = null

    private var mSetBack = false

    var isPlugIn = false
    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): VoiceDialogChatroomEarbackSettingBinding {
        return VoiceDialogChatroomEarbackSettingBinding.inflate(inflater, container, false)
    }

    override fun onDestroy() {
        context?.unregisterReceiver(mReceiver)
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.voice_BottomSheetDialogAnimation
        binding?.apply {
            setOnApplyWindowInsets(root)
        }
        setupHeadPhoneReceiver()
        setupView()
        updateViewState()
    }

    fun setOnEarBackStateChange(action: (() -> Unit)?) {
        mOnEarBackStateChange = action
    }

    fun setFragmentManager(fragmentManager: FragmentManager) {
        mAlertFragmentManager = fragmentManager
    }

    private fun setupView() {
        val earBackManager = AgoraRtcEngineController.get().earBackManager() ?: return
        binding?.apply {
            cbSwitch.setOnCheckedChangeListener { _, isOn ->
                earBackManager.setOn(isOn)
                updateViewState()
            }
            slVolume.max = 100
            slVolume.progress = earBackManager.params.volume
            tvVolume.text = "${earBackManager.params.volume}"
            slVolume.doOnProgressChanged { seekBar, progress, fromUser ->
                binding?.tvVolume?.text = "$progress"
                earBackManager.setVolume(progress)
            }
            updateModeSegment()
//        binding?.rgMode?.setOnCheckedChangeListener { _, i ->
//            val mode = when (i) {
//                R.id.tvModeAuto -> AgoraEarBackMode.Default
//                R.id.tvModeOpenSL -> AgoraEarBackMode.OpenSL
//                R.id.tvModeOboe -> AgoraEarBackMode.Oboe
//                else -> AgoraEarBackMode.Default
//            }
//            showDialogWithMode(mode)
//        }
            setPing(earBackManager.params.delay)
            earBackManager.setOnEarBackDelayChanged { value ->
                binding?.root?.post {
                    setPing(value)
                }
            }
            btnClose.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun showDialogWithMode(mode: AgoraEarBackMode) {
        if (mSetBack) {
            mSetBack = false
            return
        }
        if (mode == AgoraEarBackMode.Default) {
            AgoraRtcEngineController.get().earBackManager()?.setMode(mode)
            return
        }
        val c = context ?: return
        val f = mAlertFragmentManager ?: return
        val content = when (mode) {
            AgoraEarBackMode.OpenSL -> "After switching, OpenSL mode will be mandatory. Confirm?"
            AgoraEarBackMode.Oboe -> "After switching, Oboe mode will be mandatory. Confirm?"
            else -> ""
        }
        CommonFragmentAlertDialog().titleText(c.getString(R.string.voice_chatroom_prompt))
            .contentText(content).leftText(c.getString(R.string.voice_room_cancel))
            .rightText(c.getString(R.string.voice_room_confirm))
            .setOnClickListener(object : CommonFragmentAlertDialog.OnClickBottomListener {
                override fun onConfirmClick() {
                    AgoraRtcEngineController.get().earBackManager()?.setMode(mode)
                }

                override fun onCancelClick() {
                    updateModeSegment()
                }
            }).show(f, "botActivatedDialog")
    }

    private fun setupHeadPhoneReceiver() {
        val filter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
        context?.registerReceiver(mReceiver, filter)

        val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setHeadPhonePlugin(audioManager.isWiredHeadsetOn || audioManager.isBluetoothA2dpOn)
    }

    private fun setHeadPhonePlugin(isPlug: Boolean) {
        if (isPlugIn != isPlug) {
            isPlugIn = isPlug
            if (!isPlug) {
                AgoraRtcEngineController.get().earBackManager()?.setOn(false)
            }
            updateViewState()
        }
    }

    private fun updateModeSegment() {
//        mSetBack = true
//        when (AgoraRtcEngineController.get().earBackManager().params.mode) {
//            AgoraEarBackMode.Default -> binding?.tvModeAuto?.isChecked = true
//            AgoraEarBackMode.OpenSL -> binding?.tvModeOpenSL?.isChecked = true
//            else -> binding?.tvModeOboe?.isChecked = true
//        }
    }

    private fun setTips(tips: String) {
        binding?.apply {
            val icon = ContextCompat.getDrawable(root.context, R.drawable.voice_icon_room_setting_introduce)
            icon?.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            val spannableString = SpannableString("  $tips")
            val imageSpan = ImageSpan(icon!!, ImageSpan.ALIGN_BASELINE)
            spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            tvTips.text = spannableString
        }
    }

    private fun updateViewState() {
        val c = context ?: return
        if (!isPlugIn) {
            binding?.apply {
                cbSwitch.isChecked = false
                cbSwitch.isEnabled = false
                tvTips.setTextColor(Color.rgb(255, 18, 22))
                setTips(getString(R.string.voice_chatroom_settings_earback_waring))
                cvPing.visibility = View.GONE
                clSetting.alpha = 0.3f
                enableDisableView(clSetting, false)
            }
            return
        }
        binding?.apply {
            tvTips.setTextColor(ContextCompat.getColor(c, R.color.voice_dark_grey_color_979cbb))
            setTips(getString(R.string.voice_chatroom_settings_earback_tip))
            cbSwitch.isEnabled = true
            val isOn = AgoraRtcEngineController.get().earBackManager()?.params?.isOn ?: false
            cbSwitch.isChecked = isOn
            cvPing.visibility = if (isOn) View.VISIBLE else View.GONE
            clSetting.alpha = if (isOn) 1f else 0.3f
            enableDisableView(clSetting, isOn)
            cvPing.visibility = View.GONE
        }
    }

    private fun enableDisableView(viewGroup: ViewGroup, enable: Boolean) {
        for (idx in 0 until viewGroup.childCount) {
            viewGroup.getChildAt(idx).isEnabled = enable
        }
    }

    private fun setPing(value: Int) {
        binding?.pbPing?.progress = value
        binding?.tvPing?.text = "$value ms"

        val drawable = binding?.pbPing?.progressDrawable ?: return
        if (value <= 50) {
            drawable.colorFilter = PorterDuffColorFilter(Color.parseColor("#57D73E"), PorterDuff.Mode.SRC_IN)
        } else if (value <= 99) {
            drawable.colorFilter = PorterDuffColorFilter(Color.parseColor("#FAAD15"), PorterDuff.Mode.SRC_IN)
        } else {
            drawable.colorFilter = PorterDuffColorFilter(Color.parseColor("#FF1216"), PorterDuff.Mode.SRC_IN)
        }
    }

    private inner class HeadphoneReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action != Intent.ACTION_HEADSET_PLUG) return
            val state = intent.getIntExtra("state", -1)
            if (state == 1) {
                setHeadPhonePlugin(true)
                Log.d("HeadphoneReceiver", "headphone plugged in")
            } else if (state == 0) {
                setHeadPhonePlugin(false)
                Log.d("HeadphoneReceiver", "headphone removed")
            }
        }
    }
}
