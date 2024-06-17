package io.agora.scene.voice.ui.dialog

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import com.github.penfeizhou.animation.apng.APNGDrawable
import io.agora.mediaplayer.Constants
import io.agora.mediaplayer.IMediaPlayer
import io.agora.scene.voice.R
import io.agora.scene.voice.databinding.VoiceDialogChatroomAiagcBinding
import io.agora.scene.voice.rtckit.AgoraRtcEngineController
import io.agora.scene.voice.rtckit.listener.MediaPlayerObserver
import io.agora.voice.common.ui.dialog.BaseSheetDialog
import java.util.*

class RoomAIAGCSheetDialog: BaseSheetDialog<VoiceDialogChatroomAiagcBinding>() {

    companion object {
        const val KEY_IS_ON = "isOn"
    }

    private val isOn by lazy {
        arguments?.getBoolean(RoomAIAECSheetDialog.KEY_IS_ON, true) ?: true
    }

    var onClickCheckBox: ((isOn: Boolean) -> Unit)? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): VoiceDialogChatroomAiagcBinding? {
        return VoiceDialogChatroomAiagcBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.voice_BottomSheetDialogAnimation

        binding?.accbAGC?.isChecked = isOn
        binding?.accbAGC?.setOnCheckedChangeListener { _, b ->
            onClickCheckBox?.invoke(b)
        }
    }
}
