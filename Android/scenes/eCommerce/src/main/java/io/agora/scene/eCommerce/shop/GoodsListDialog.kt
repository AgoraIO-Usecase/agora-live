package io.agora.scene.eCommerce.shop

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.RtcEngineInstance
import io.agora.scene.eCommerce.ShowLogger
import io.agora.scene.eCommerce.databinding.CommerceShopGoodsItemLayoutBinding
import io.agora.scene.eCommerce.databinding.CommerceShopGoodsListDialogBinding
import io.agora.scene.eCommerce.databinding.CommerceWidgetBottomLightListItemBinding
import io.agora.scene.eCommerce.databinding.CommerceWidgetDebugAudienceSettingDialogBinding
import io.agora.scene.eCommerce.widget.BottomFullDialog
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder

data class GoodsListItem(
    val name: String,
    val price: Float,
    val picResource: Int,
    val count: Int
) {}

class GoodsListDialog constructor(context: Context) : BottomSheetDialog(context) {

    private val TAG = "GoodsListDialog"

    private val binding by lazy { CommerceShopGoodsListDialogBinding.inflate(LayoutInflater.from(context)) }

    private val dataSource = arrayListOf(
        GoodsListItem(
            name = context.resources.getString(R.string.commerce_shop_auction_item_0),
            price = 1f,
            picResource = 1,
            count = 0
        ),
        GoodsListItem(
            name = context.resources.getString(R.string.commerce_shop_auction_item_1),
            price = 1f,
            picResource = R.drawable.commerce_auction_goods,
            count = 0
        ),
        GoodsListItem(
            name = context.resources.getString(R.string.commerce_shop_auction_item_2),
            price = 1f,
            picResource = 1,
            count = 0
        )
    )

    private val mAdapter by lazy {
        object : BindingSingleAdapter<GoodsListItem, CommerceShopGoodsItemLayoutBinding>(){

            override fun onBindViewHolder(
                holder: BindingViewHolder<CommerceShopGoodsItemLayoutBinding>,
                position: Int
            ) {
                getItem(position)?.let { item ->
                    holder.binding.tvItemName.text = item.name
                }
            }
        }
    }

    init {
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        binding.recyclerView.adapter = mAdapter
        mAdapter.resetAll(dataSource)
    }

}