package io.agora.scene.eCommerce.shop

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.eCommerce.CommerceLogger
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceShopAuctionFragmentBinding
import io.agora.scene.eCommerce.service.*
import io.agora.scene.widget.utils.CenterCropRoundCornerTransform

class LiveAuctionFragment: Fragment() {

    companion object {

        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        fun newInstance(roomId: String) = LiveAuctionFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_ROOM_ID, roomId)
            }
        }
    }

    private val mRoomId by lazy { (arguments?.getString(EXTRA_ROOM_ID))!! }

    private val tag = "LiveAuctionFragment"

    private val kAuctionInterval = 30 * 1000

    private lateinit var binding: CommerceShopAuctionFragmentBinding

    private val mService by lazy { ShowServiceProtocol.getImplInstance() }

    private var isRoomOwner = false

    private var data: AuctionModel? = null

    private var countDownTimer: CountDownTimer? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = CommerceShopAuctionFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupAuction()
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mainHandler.looper.thread) {
            r.run()
        } else {
            mainHandler.post(r)
        }
    }

    private fun setupAuction() {
        mService.getRoomInfo(mRoomId)?.let { roomInfo ->
            isRoomOwner = roomInfo.ownerId.toLong() == UserManager.getInstance().user.id
            idleAuctionStatus()
        }
    }

    fun updateAuction(auctionModel: AuctionModel) {
        data = auctionModel
        updateAuctionStatus()
    }

    fun release() {
        hasStart = false
        countDownTimer?.cancel()
        countDownTimer = null
    }

    private var hasStart = false
    private fun updateAuctionStatus() {
        val auctionModel = data ?: run {
            idleAuctionStatus()
            return
        }
        when (AuctionStatus.fromValue(auctionModel.status)) {
            AuctionStatus.Start -> {
                hasStart = true
                val startTS = auctionModel.startTimestamp
                val interval = mService.getCurrentTimestamp(mRoomId) - startTS
                val rest = kAuctionInterval - interval
                CommerceLogger.d(tag, "start auction, startTS:$startTS, interval:$interval, rest:$rest")
                inProgressAuctionStatus(rest)
            }
            AuctionStatus.Finish -> {
                val bidUser = auctionModel.bidUser
                if (bidUser != null && bidUser.id.isNotEmpty() && hasStart) {
                    context?.let {
                        AuctionResultDialog(it, bidUser).show()
                    }
                }
                idleAuctionStatus()
            }
            else -> {
                idleAuctionStatus()
            }
        }
    }

    private fun idleAuctionStatus() {
        binding.tvCountDown.visibility = View.INVISIBLE
        binding.ivBuyerAvatar.visibility = View.INVISIBLE
        binding.tvBuyerName.visibility = View.INVISIBLE
        binding.tvPrice.text = getString(R.string.commerce_shop_auction_price, "1")
        if (isRoomOwner) {
            binding.layoutSubmit.visibility = View.VISIBLE
            binding.btnBid.visibility = View.GONE
        } else {
            binding.layoutSubmit.visibility = View.GONE
            binding.tvCountDown.visibility = View.GONE
            binding.btnBid.visibility = View.VISIBLE
            binding.btnBid.isEnabled = false
            binding.btnBid.setBackgroundResource(R.drawable.commerce_corner_radius_40white)
            binding.btnBid.text = getString(R.string.commerce_shop_auction_not_start)
            binding.btnBid.setTextColor(Color.parseColor("#FFFFFF"))
        }
    }

    private fun inProgressAuctionStatus(interval: Long) {
        val auctionModel = data ?: return
        binding.tvCountDown.visibility = View.VISIBLE
        if (isRoomOwner) {
            binding.layoutSubmit.visibility = View.INVISIBLE
            binding.btnBid.visibility = View.GONE
        } else {
            // bid button
            if (auctionModel.bidUser?.id == UserManager.getInstance().user.id.toString()) {
                // leading bidder
                binding.btnBid.isEnabled = false
                binding.btnBid.text = getString(R.string.commerce_shop_auction_leading_bidder)
                binding.btnBid.setBackgroundResource(R.drawable.commerce_corner_radius_blue)
                binding.btnBid.setTextColor(Color.parseColor("#191919"))
            } else {
                binding.btnBid.isEnabled = true
                binding.btnBid.visibility = View.VISIBLE
                binding.btnBid.setBackgroundResource(R.drawable.commerce_corner_radius_gradient_orange)
                binding.btnBid.setTextColor(Color.parseColor("#191919"))
                if (auctionModel.bidUser?.id?.isNotEmpty() == true) {
                    binding.btnBid.text = getString(R.string.commerce_shop_auction_bid, "${auctionModel.bid}")
                } else {
                    binding.btnBid.text = getString(R.string.commerce_shop_auction_bid, "${auctionModel.bid}")
                }
            }
        }
        // bid user
        val bidUser = auctionModel.bidUser
        if (bidUser != null && bidUser.id.isNotEmpty()) {
            binding.tvBidStatus.text = getString(R.string.commerce_shop_auction_current_bid)
            binding.tvPrice.text = getString(R.string.commerce_shop_auction_price, "${auctionModel.bid}")
            binding.ivBuyerAvatar.visibility = View.VISIBLE
            binding.tvBuyerName.visibility = View.VISIBLE
            Glide.with(this)
                .load(bidUser.getAvatarFullUrl())
                .error(R.drawable.commerce_default_avatar)
                .transform(CenterCropRoundCornerTransform(8))
                .into(binding.ivBuyerAvatar)
            if (bidUser.id == UserManager.getInstance().user.id.toString()) {
                // leading bidder
                binding.tvBuyerName.text = getString(R.string.commerce_shop_auction_you_leading)
            } else {
                val buyer = (bidUser.name.firstOrNull() ?: "*").toString() + "**"
                binding.tvBuyerName.text = buyer
            }
        } else {
            binding.tvBidStatus.text = getString(R.string.commerce_shop_auction_start_from)
            binding.tvPrice.text = getString(R.string.commerce_shop_auction_price, "${auctionModel.bid}")
            binding.ivBuyerAvatar.visibility = View.INVISIBLE
            binding.tvBuyerName.visibility = View.INVISIBLE
        }
        // timer
        countDownTimer?.cancel()
        countDownTimer = null
        countDownTimer = object: CountDownTimer(interval, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(tag, "auction tick:$millisUntilFinished")
                binding.tvCountDown.text = formatTime(millisUntilFinished)
            }
            override fun onFinish() {
                onAuctionFinish()
            }
        }.start()
    }

    private fun onAuctionFinish() {
        CommerceLogger.d(tag, "onAuctionFinish")
        countDownTimer?.cancel()
        countDownTimer = null
        // show dialog
        if (isRoomOwner) {
            CommerceLogger.d(tag, "finish bid")
            mService.auctionComplete(mRoomId) { e ->
                if (e != null && data?.status != 2L) {
                    CommerceLogger.d(tag, "failed to finish bid, code: ${e.code}, message: ${e.message}")
                    binding.root.postDelayed({ onAuctionFinish() }, 5000)
                }
            }
        }
    }

    private fun setupView() {
        binding.layoutSubmit.setOnClickListener {
            binding.tvStart.isVisible = false
            binding.progressLoading.isVisible = true
            CommerceLogger.d(tag, "start bid click")
            mService.auctionStart(mRoomId) { e ->
                if (e != null) {
                    CommerceLogger.d(tag, "failed to start bid, code: ${e.code}, message: ${e.message}")
                    ToastUtils.showToast(context?.getString(R.string.commerce_start_auction_failed, e.code))
                }

                runOnMainThread {
                    binding.tvStart.isVisible = true
                    binding.progressLoading.isVisible = false
                }
            }
        }
        binding.btnBid.setOnClickListener {
            val bid = data?.bid ?: 0
            CommerceLogger.d(tag, "click bid")
            mService.auctionBidding(mRoomId, (bid + 1)) { e ->
                if (e != null) {
                    CommerceLogger.d(tag, "failed bid, code: ${e.code}, message: ${e.message}")
                    ToastUtils.showToast(context?.getString(R.string.commerce_bid_failed, e.code))
                }
            }
        }
    }

    private fun formatTime(millisUntilFinished: Long): String {
        val minutes = millisUntilFinished % (1000 * 60 * 60) / (1000 * 60)
        val seconds = millisUntilFinished % (1000 * 60) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }
}