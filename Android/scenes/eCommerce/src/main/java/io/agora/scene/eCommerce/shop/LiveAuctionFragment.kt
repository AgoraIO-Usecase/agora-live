package io.agora.scene.eCommerce.shop

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.agora.scene.eCommerce.databinding.CommerceShopAuctionFragmentBinding

class LiveAuctionFragment: Fragment() {

    private lateinit var binding: CommerceShopAuctionFragmentBinding

    private val tag = "LiveAuctionFragment"

    private val startTime = System.currentTimeMillis()
    private var timerHandler: Handler? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = CommerceShopAuctionFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()

    }

    private fun setupView() {

    }

}