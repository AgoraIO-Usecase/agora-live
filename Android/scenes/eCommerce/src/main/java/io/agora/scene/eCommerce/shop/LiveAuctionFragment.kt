package io.agora.scene.eCommerce.shop

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceShopAuctionFragmentBinding
import io.agora.scene.eCommerce.service.*
import io.agora.scene.widget.utils.CenterCropRoundCornerTransform

class LiveAuctionFragment(
    private val roomId: String
): Fragment() {

    private val tag = "LiveAuctionFragment"

    private val kAuctionInterval = 30*1000

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

    private fun setupAuction() {
        val roomInfo = mService.getRoomInfo(roomId) ?: AUIRoomInfo()
        isRoomOwner = roomInfo.ownerId == UserManager.getInstance().user.id.toInt()
        idleAuctionStatus()
    }

    fun updateAuction(auctionModel: AuctionModel) {
        data = auctionModel
        updateAuctionStatus()
    }

    private fun updateAuctionStatus() {
        val auctionModel = data ?: run {
            idleAuctionStatus()
            return
        }
        when (AuctionStatus.fromValue(auctionModel.status)) {
            AuctionStatus.Start -> {
                val startTS = auctionModel.timestamp.toLong()
                val interval = TimeUtils.currentTimeMillis() - startTS
                val rest = kAuctionInterval - interval
                inProgressAuctionStatus(rest)
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
            binding.btnStart.visibility = View.VISIBLE
            binding.btnBid.visibility = View.GONE
        } else {
            binding.btnStart.visibility = View.GONE
            binding.tvCountDown.visibility = View.GONE
            binding.btnBid.visibility = View.VISIBLE
            binding.btnBid.isEnabled = true
            binding.btnBid.setBackgroundResource(R.drawable.commerce_corner_radius_40white)
            binding.btnBid.text = getString(R.string.commerce_shop_auction_not_start)
            binding.btnBid.setTextColor(Color.parseColor("#FFFFFF"))
        }
    }

    private fun inProgressAuctionStatus(interval: Long) {
        val auctionModel = data ?: return
        binding.tvCountDown.visibility = View.VISIBLE
        if (isRoomOwner) {
            binding.btnStart.visibility = View.INVISIBLE
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
                    binding.btnBid.text = getString(R.string.commerce_shop_auction_bid, "${auctionModel.bid + 1}")
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
            binding.tvPrice.text = getString(R.string.commerce_shop_auction_price, "${auctionModel.bid + 1}")
            binding.ivBuyerAvatar.visibility = View.INVISIBLE
            binding.tvBuyerName.visibility = View.INVISIBLE
        }
        // timer
        if (countDownTimer == null) {
            countDownTimer = object: CountDownTimer(interval, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    binding.tvCountDown.text = formatTime(millisUntilFinished)
                }
                override fun onFinish() {
                    onAuctionFinish()
                }
            }.start()
        }
    }

    private fun onAuctionFinish() {
        countDownTimer?.cancel()
        countDownTimer = null
        // show dialog
        val bidUser = data?.bidUser
        if (bidUser != null && bidUser.id.isNotEmpty()) {
            context?.let {
                AuctionResultDialog(it, bidUser).show()
            }
        }
        if (isRoomOwner) {
            mService.auctionReset(roomId)
        }
    }

    private fun setupView() {
        binding.btnStart.setOnClickListener {
            mService.auctionStart(roomId)
        }
        binding.btnBid.setOnClickListener {
            val bid = data?.bid ?: 0
            mService.auctionBidding(roomId, (bid + 1))
        }
    }

    private fun formatTime(millisUntilFinished: Long): String {
        val minutes = millisUntilFinished % (1000 * 60 * 60) / (1000 * 60)
        val seconds = millisUntilFinished % (1000 * 60) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }
}