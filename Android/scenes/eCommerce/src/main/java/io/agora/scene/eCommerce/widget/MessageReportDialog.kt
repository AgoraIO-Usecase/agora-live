package io.agora.scene.eCommerce.widget

import android.app.Dialog
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceMessageReportDialogBinding
import io.agora.scene.eCommerce.databinding.CommerceShoppingResultDialogBinding

class MessageReportDialog constructor(
    context: Context,
    private val message: String
) : Dialog(context, R.style.commerce_bottom_full_dialog) {

    private val TAG = "MessageReportDialog"

    private val binding by lazy { CommerceMessageReportDialogBinding.inflate(LayoutInflater.from(context)) }

    init {
        setContentView(binding.root)
        setupView()
        setCanceledOnTouchOutside(true)
    }

    private fun setupView() {
        binding.tvMessage.text = message
        binding.root.setOnClickListener {
            dismiss()
        }
        binding.tvReport.setOnClickListener {
            dismiss()
        }
        binding.tvCancel.setOnClickListener {
            dismiss()
        }
    }
}