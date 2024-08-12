package io.agora.scene.voice.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.agora.scene.voice.R
import io.agora.scene.voice.databinding.VoiceDialogChatroomAiaecBinding
import io.agora.voice.common.ui.dialog.BaseFixedHeightSheetDialog

class RoomAIAECSheetDialog: BaseFixedHeightSheetDialog<VoiceDialogChatroomAiaecBinding>() {

    companion object {
        const val KEY_IS_ON = "isOn"
    }

    private val isOn by lazy {
        arguments?.getBoolean(KEY_IS_ON, true) ?: true
    }

    var onClickCheckBox: ((isOn: Boolean) -> Unit)? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): VoiceDialogChatroomAiaecBinding? {
        return VoiceDialogChatroomAiaecBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.voice_BottomSheetDialogAnimation
        binding?.apply {
            setOnApplyWindowInsets(root)
            accbAEC.isChecked = isOn
            accbAEC.setOnCheckedChangeListener { _, isChecked ->
                onClickCheckBox?.invoke(isChecked)
            }
            ivBottomSheetBack.setOnClickListener {
                onHandleOnBackPressed()
            }
        }
    }
}
