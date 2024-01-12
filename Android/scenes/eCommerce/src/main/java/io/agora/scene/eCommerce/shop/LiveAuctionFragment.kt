package io.agora.scene.eCommerce.shop

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceShopAuctionFragmentBinding

class LiveAuctionFragment: Fragment() {

    private lateinit var binding: CommerceShopAuctionFragmentBinding

    private val tag = "LiveAuctionFragment"

    private var countDownTimer: CountDownTimer? = null
    private var timeRemaining: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = CommerceShopAuctionFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()

    }

    private fun setupView() {
        binding.btnStart.setOnClickListener {
            sendStartAuction()
        }
        binding.btnBid.setOnClickListener {
            sendBid()
        }
    }

    private fun sendStartAuction() {
        onReceiveStartAuction()
    }

    private fun sendBid() {

    }

    private fun onReceiveStartAuction() {
        binding.btnStart.isVisible = false
        binding.tvCountDown.isVisible = true
        // bid button
        binding.btnBid.setBackgroundResource(R.drawable.commerce_corner_radius_gradient_orange)
        binding.btnBid.text = getString(R.string.commerce_shop_auction_bid, "1")
        binding.btnBid.setTextColor(Color.parseColor("#191919"))
        // timer
        val total = 26000L
        timeRemaining = total
        countDownTimer = object: CountDownTimer(total, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvCountDown.text = formatTime(millisUntilFinished)
            }
            override fun onFinish() {
                auctionFinish()
            }
        }
        countDownTimer?.start()
    }

    private fun onReceiveBid() {
        // update bid price/avatar/leading bidder
    }

    private fun auctionFinish() {
        binding.btnStart.isVisible = false
        binding.tvCountDown.isVisible = false
        binding.btnBid.setBackgroundResource(R.drawable.commerce_corner_radius_gray)
        binding.btnBid.text = getString(R.string.commerce_shop_auction_not_start)
        binding.btnBid.setTextColor(Color.parseColor("#FFFFFF"))
        // show dialog
        context?.let {
            AuctionResultDialog(it).show()
        }
    }

    private fun formatTime(millisUntilFinished: Long): String {
        val minutes = millisUntilFinished % (1000 * 60 * 60) / (1000 * 60)
        val seconds = millisUntilFinished % (1000 * 60) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }
}