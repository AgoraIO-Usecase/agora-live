package io.agora.scene.eCommerce.shop

import android.graphics.Color
import android.icu.util.TimeUnit
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.scene.base.GlideApp
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceShopAuctionFragmentBinding
import io.agora.scene.eCommerce.service.*
import io.agora.scene.widget.utils.CenterCropRoundCornerTransform

class LiveAuctionFragment: Fragment() {

    private val tag = "LiveAuctionFragment"

    private lateinit var binding: CommerceShopAuctionFragmentBinding

    private lateinit var mRoomId: String
    private val mService by lazy { ShowServiceProtocol.getImplInstance() }
    private lateinit var mRoomInfo: AUIRoomInfo
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
        idleAuctionStatus()
    }

    fun setRoomId(roomId: String) {
        mRoomId = roomId
        mRoomInfo = mService.getRoomInfo(roomId) ?: AUIRoomInfo()
        isRoomOwner = mRoomInfo.ownerId == UserManager.getInstance().user.id.toInt()
        mService.subscribeAuction(roomId) { auctionModel ->
            data = auctionModel
            updateAuctionStatus()
        }
    }

    private fun updateAuctionStatus() {
        val auctionModel = data ?: run {
            idleAuctionStatus()
            return
        }
        val startTS = auctionModel.timestamp?.toLongOrNull()
        if (startTS != null) {
            val interval = TimeUtils.currentTimeMillis() - startTS
            if (interval < 3*60*1000) {
                inProgressAuctionStatus(interval)
            } else {
                idleAuctionStatus()
            }
        } else {
            idleAuctionStatus()
        }
    }

    private fun idleAuctionStatus() {
        binding.tvCountDown.visibility = View.INVISIBLE
        if (isRoomOwner) {
            binding.btnStart.visibility = View.VISIBLE
            binding.btnBid.visibility = View.GONE
        } else {
            binding.btnStart.visibility = View.GONE
            binding.tvCountDown.visibility = View.GONE
            binding.btnBid.visibility = View.VISIBLE
            binding.btnBid.isEnabled = true
            binding.btnBid.setBackgroundResource(R.drawable.commerce_corner_radius_gray)
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
            binding.tvBid.text = "$${auctionModel.bid}"
            Glide.with(this)
                .load(auctionModel.bidUser?.getAvatarFullUrl())
                .error(R.drawable.commerce_default_avatar)
                .into(binding.ivBuyerAvatar)
//            .transform(CenterCropRoundCornerTransform(10))
            binding.tvBuyerName.text = auctionModel.bidUser?.userName
            // bid button
            if (auctionModel.bidUser?.userId == UserManager.getInstance().user.id.toString()) {
                // leading bidder
                binding.tvBuyerName.text = getString(R.string.commerce_shop_auction_you_leading)
                binding.btnBid.isEnabled = false
                binding.btnBid.text = getString(R.string.commerce_shop_auction_leading_bidder)
                binding.btnBid.setBackgroundResource(R.drawable.commerce_corner_radius_blue)
                binding.btnBid.setTextColor(Color.parseColor("#191919"))
            } else {
                binding.btnBid.isEnabled = true
                binding.btnBid.visibility = View.VISIBLE
                binding.btnBid.setBackgroundResource(R.drawable.commerce_corner_radius_gradient_orange)
                binding.btnBid.text = getString(R.string.commerce_shop_auction_bid, "${auctionModel.bid}")
                binding.btnBid.setTextColor(Color.parseColor("#191919"))
            }
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
            }
            countDownTimer?.start()
        }
    }

    private fun onAuctionFinish() {
        // show dialog
        context?.let {
            AuctionResultDialog(it).show()
        }
    }

    private fun setupView() {
        binding.btnStart.setOnClickListener {
            mService.startAuction(mRoomId)
        }
        binding.btnBid.setOnClickListener {
            mService.bidding(mRoomId)
        }
    }

    private fun formatTime(millisUntilFinished: Long): String {
        val minutes = millisUntilFinished % (1000 * 60 * 60) / (1000 * 60)
        val seconds = millisUntilFinished % (1000 * 60) / 1000
        return String.format("%02d:%02d", minutes, seconds)
    }
}