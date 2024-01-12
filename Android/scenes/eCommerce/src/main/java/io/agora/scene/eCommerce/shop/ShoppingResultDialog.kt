package io.agora.scene.eCommerce.shop

import android.app.Dialog
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceShoppingResultDialogBinding

class ShoppingResultDialog constructor(
    context: Context,
    private val title: String
) : Dialog(context, R.style.commerce_alert_dialog) {

    private val TAG = "ShoppingResultDialog"

    private val binding by lazy { CommerceShoppingResultDialogBinding.inflate(LayoutInflater.from(context)) }

    init {
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        binding.tvState.text = title
        binding.tvSubmit.setOnClickListener {
            dismiss()
        }
    }
}