package io.agora.scene.eCommerce.shop

import android.app.Dialog
import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceShopAuctionResultDialogBinding

class AuctionResultDialog constructor(context: Context) : Dialog(context, R.style.commerce_alert_dialog) {

    private val TAG = "AuctionResultDialog"

    private val binding by lazy { CommerceShopAuctionResultDialogBinding.inflate(LayoutInflater.from(context)) }

    private var countDownTimer: CountDownTimer? = null

    init {
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        binding.btnClose.setOnClickListener {
            finish()
        }
        countDownTimer = object: CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvCountDown.text = String.format("%ds", millisUntilFinished/1000 + 1)
            }
            override fun onFinish() {
                finish()
            }
        }
        countDownTimer?.start()
    }

    private fun finish() {
        countDownTimer?.cancel()
        countDownTimer = null
        dismiss()
    }
}