package io.agora.scene.eCommerce.shop

import android.content.Context
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceShopGoodsItemLayoutBinding
import io.agora.scene.eCommerce.databinding.CommerceShopGoodsListDialogBinding
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder

data class GoodsListItem(
    val name: String,
    val price: Float,
    val picResource: Int,
    val qty: Int
) {}

class GoodsListDialog constructor(context: Context) : BottomSheetDialog(context, R.style.commerce_alert_dialog) {

    private val TAG = "GoodsListDialog"

    private val binding by lazy { CommerceShopGoodsListDialogBinding.inflate(LayoutInflater.from(context)) }

    private val isRoomOwner = true

    private val dataSource = arrayListOf(
        GoodsListItem(
            name = context.resources.getString(R.string.commerce_shop_auction_item_0),
            price = 20f,
            picResource = R.drawable.commerce_shop_goods_0,
            qty = 20
        ),
        GoodsListItem(
            name = context.resources.getString(R.string.commerce_shop_auction_item_1),
            price = 5f,
            picResource = R.drawable.commerce_shop_goods_1,
            qty = 0
        ),
        GoodsListItem(
            name = context.resources.getString(R.string.commerce_shop_auction_item_2),
            price = 12f,
            picResource = R.drawable.commerce_shop_goods_2,
            qty = 1
        )
    )

    private val mAdapter by lazy {
        object : BindingSingleAdapter<GoodsListItem, CommerceShopGoodsItemLayoutBinding>(){

            var onClickBuy: ((Int) -> Unit)? = null

            var onUserChangedQty: ((Int) -> Unit)? = null

            override fun onBindViewHolder(
                holder: BindingViewHolder<CommerceShopGoodsItemLayoutBinding>,
                position: Int
            ) {
                getItem(position)?.let { item ->
                    holder.binding.tvItemName.text = item.name
                    holder.binding.ivCommodity.setImageResource(item.picResource)
                    holder.binding.tvPrice.text = String.format("$%.0f", item.price)
                    holder.binding.tvQty.text = context.getString(R.string.commerce_shop_item_qty, item.qty.toString())
                    if (item.qty == 0) {
                        holder.binding.btnBuy.setBackgroundResource(R.drawable.commerce_corner_radius_gray)
                        holder.binding.btnBuy.text = context.getString(R.string.commerce_shop_item_sold_out)
                        holder.binding.btnBuy.setTextColor(Color.parseColor("#191919"))
                        holder.binding.btnBuy.isEnabled = false
                    } else {
                        holder.binding.btnBuy.setBackgroundResource(R.drawable.commerce_corner_radius_gradient_orange)
                        holder.binding.btnBuy.text = context.getString(R.string.commerce_shop_auction_buy)
                        holder.binding.btnBuy.setTextColor(Color.parseColor("#A5ADBA"))
                        holder.binding.btnBuy.isEnabled = true
                    }
                    if (isRoomOwner) {
                        holder.binding.btnBuy.visibility = View.GONE
                        holder.binding.llStepper.visibility = View.VISIBLE
                        holder.binding.etQty.setText(item.qty.toString())
                        holder.binding.btnAdd.setOnClickListener {
                            val value = (holder.binding.etQty.text ?: "0").toString().toInt()
                            if (value < 99) {
                                val newValue = value + 1
                                holder.binding.etQty.setText(newValue.toString())
                                onUserChangedQty?.invoke(position)
                            }
                        }
                        holder.binding.btnReduce.setOnClickListener {
                            val value = (holder.binding.etQty.text ?: "0").toString().toInt()
                            if (value > 99) {
                                val newValue = 99
                                holder.binding.etQty.setText(newValue.toString())
                                onUserChangedQty?.invoke(position)
                            } else if (value > 0) {
                                val newValue = value - 1
                                holder.binding.etQty.setText(newValue.toString())
                                onUserChangedQty?.invoke(position)
                            }
                        }
                        holder.binding.etQty.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) {
                                onUserChangedQty?.invoke(position)
                                Log.d(TAG, "--------- afterTextChanged")
                            }
                        })
                    } else {
                        holder.binding.btnBuy.visibility = View.VISIBLE
                        holder.binding.llStepper.visibility = View.GONE
                        holder.binding.btnBuy.setOnClickListener {
                            onClickBuy?.invoke(position)
                        }
                    }
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
        mAdapter.onClickBuy = { index ->
            if (true) { // 购买结果
                ShoppingResultDialog(context, context.getString(R.string.commerce_shop_alert_bought)).show()
            } else {
                ShoppingResultDialog(context, context.getString(R.string.commerce_shop_alert_sold_out)).show()
            }
        }
        mAdapter.onUserChangedQty = {

        }
    }
}