package io.agora.scene.voice.ui.dialog.soundcard

import android.graphics.Rect
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.isVisible
import io.agora.scene.voice.R
import io.agora.scene.voice.databinding.VoiceDialogSoundCardBinding
import io.agora.scene.voice.rtckit.*
import io.agora.voice.common.ui.dialog.BaseFixedHeightSheetDialog

class SoundCardSettingDialog: BaseFixedHeightSheetDialog<VoiceDialogSoundCardBinding>() {

    companion object {
        const val TAG: String = "SoundCardFragment"
    }

    private lateinit var mManager: AgoraSoundCardManager

    var onSoundCardStateChange: (() -> Unit)? = null

    var onClickSoundCardType: (() -> Unit)? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VoiceDialogSoundCardBinding {
        return VoiceDialogSoundCardBinding.inflate(inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.voice_BottomSheetDialogAnimation
        mManager = AgoraRtcEngineController.get().soundCardManager() ?: run {
            dismiss()
            return
        }
        super.onViewCreated(view, savedInstanceState)
        binding?.apply {
            setOnApplyWindowInsets(root)
        }
        setupView()
    }

    fun updateView(){
        setupView()
    }

    private fun setupView() {
        val spannableString = SpannableString(getString(R.string.voice_sound_card_supports))
        spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, 9, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        binding?.tvSoundCardSupport?.text = spannableString
        binding?.apply {
            groupSoundCardSettings.visibility = View.VISIBLE
            if (mManager.isEnable()) {
                setupPresetSoundView(mManager.presetSound())
                setupGainView(mManager.gainValue())
                setupPresetView(mManager.presetValue())
                vPramsMark.visibility = View.INVISIBLE
                clSoundCardParams.alpha = 1f
            } else {
                vPramsMark.visibility = View.VISIBLE
                clSoundCardParams.alpha = 0.4f
            }
            groupSoundCardAbnormal.isVisible = false
            mcbSoundCardSwitch.isChecked = mManager.isEnable()
            btnClose.setOnClickListener {
                dismiss()
            }
            mcbSoundCardSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    vPramsMark.visibility = View.INVISIBLE
                    clSoundCardParams.alpha = 1f
                } else {
                    vPramsMark.visibility = View.VISIBLE
                    clSoundCardParams.alpha = 0.4f
                }
                mManager.enable(isChecked, force = true, callback = {
                    if (isChecked) {
                        setupPresetSoundView(mManager.presetSound())
                        setupGainView(mManager.gainValue())
                        setupPresetView(mManager.presetValue())
                    }
                    onSoundCardStateChange?.invoke()
                })
            }
            pbGainAdjust.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.progress?.let { progress ->
                        val gainValue: Float = progress.toFloat()
                        etGainAdjust.setText(gainValue.toString())
                        mManager.setGainValue(gainValue)
                    }
                }
            })
            vPramsMark.setOnClickListener {

            }
            tvSoundTypeSelect.setOnClickListener {
                onClickSoundCardType?.invoke()
            }
            pbMicType.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.progress?.let { value ->
                        val micType = value - 1
                        tvMicType.text = if (micType == -1) getString(R.string.voice_sound_preset_off) else micType.toString()
                        mManager.setPresetValue(micType)
                    }
                }
            })
            activity?.window?.let { window ->
                val initialWindowHeight = Rect().apply { window.decorView.getWindowVisibleDisplayFrame(this) }.height()
                root.viewTreeObserver.addOnGlobalLayoutListener {
                    Handler(Looper.getMainLooper()).postDelayed({
                        val currentWindowHeight = Rect().apply { window.decorView.getWindowVisibleDisplayFrame(this) }.height()
                        if (currentWindowHeight < initialWindowHeight) {
                            Log.d(TAG, "current: $currentWindowHeight, initial: $initialWindowHeight, show: true")
                        } else {
                            var value = 100f
                            try {
                                val input = etGainAdjust.text.toString()
                                value = input.toFloat()
                            } catch (e: NumberFormatException) {}
                            if (value < 0) {
                                value = 100f
                            } else if (value > 400) {
                                value = 400f
                            }
                            setupGainView(value)
                            etGainAdjust.clearFocus()
                            mManager.setGainValue(value)
                        }
                    }, 300)
                }
            }
        }
    }

    private fun setupGainView(gainValue: Float) {
        binding?.apply {
            pbGainAdjust.progress = gainValue.toInt()
            etGainAdjust.setText(gainValue.toString())
        }
    }

    private fun setupPresetView(presetValue: Int) {
        binding?.apply {
            val sliderValue = presetValue + 1
            pbMicType.progress = sliderValue
            tvMicType.text = if (presetValue == -1) getString(R.string.voice_sound_preset_off) else presetValue.toString()
        }
    }

    private fun setupPresetSoundView(presetSound: AgoraPresetSound) {
        val text = "${getString(presetSound.titleStringID)}（${getString(presetSound.infoStringID)}）"
        binding?.tvSoundTypeSelect?.text = text
    }
}