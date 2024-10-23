package io.agora.scene.eCommerce.shop

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.view.isVisible
import io.agora.scene.base.manager.UserManager
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceShopGoodsItemLayoutBinding
import io.agora.scene.eCommerce.databinding.CommerceShopGoodsListDialogBinding
import io.agora.scene.eCommerce.service.GoodsModel
import io.agora.scene.eCommerce.service.ShowServiceProtocol
import io.agora.scene.widget.basic.BindingSingleAdapter
import io.agora.scene.widget.basic.BindingViewHolder

class GoodsListDialog constructor(
    context: Context,
    private val roomId: String
) : Dialog(context, R.style.commerce_full_screen_dialog) {

    private val tag = "GoodsListDialog"

    private val binding by lazy { CommerceShopGoodsListDialogBinding.inflate(LayoutInflater.from(context)) }

    private var isRoomOwner = false

    private val mService by lazy { ShowServiceProtocol.getImplInstance() }

    private val dataSource = arrayListOf<GoodsModel>()

    private var mAdapter: ShopAdapter? = null

    init {
        setContentView(binding.root)
        setupView()
        setupShop()
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mainHandler.looper.thread) {
            r.run()
        } else {
            mainHandler.post(r)
        }
    }

    private fun setupShop() {
        mService.shopSubscribe(roomId) { models ->
            updateList(models)
        }
    }

    private fun updateList(goodsModels: List<GoodsModel>) {
        if (dataSource.count() != goodsModels.count()) {
            dataSource.clear()
            for (model in goodsModels) {
                if (model.imageName.contains("0")) {
                    model.picResource = R.drawable.commerce_shop_goods_0
                } else if (model.imageName.contains("1")) {
                    model.picResource = R.drawable.commerce_shop_goods_1
                } else {
                    model.picResource = R.drawable.commerce_shop_goods_2
                }
                dataSource.add(model)
            }
        } else {
            for ((index, model) in goodsModels.withIndex()) {
                val item = dataSource[index]
                item.quantity = model.quantity
            }
        }
        mAdapter?.resetAll(dataSource)
    }

    private fun setupView() {
        val roomInfo = mService.getRoomInfo(roomId) ?: run {
            return
        }
        isRoomOwner = roomInfo.ownerId.toLong() == UserManager.getInstance().user.id
        mAdapter = ShopAdapter(isRoomOwner)
        binding.recyclerView.adapter = mAdapter
        mAdapter?.onClickBuy = { goodsId, btn, pb, tv ->
            mService.shopBuyItem(roomId, goodsId) { e ->
                runOnMainThread {
                    if (e != null) {
                        showOperationInfo(context.getString(R.string.commerce_shop_buy_failed, e.code.toString()))
                    } else {
                        showOperationInfo(context.getString(R.string.commerce_shop_alert_bought))
                    }
                    tv.isVisible = true
                    pb.isVisible = false

                    val goods = dataSource.firstOrNull { it.goodsId == goodsId } ?: return@runOnMainThread
                    if (goods.quantity != 0L) {
                        btn.isEnabled = true
                        btn.setBackgroundResource(R.drawable.commerce_corner_radius_gradient_orange)
                    }
                }
            }
        }
        mAdapter?.onUserChangedQty = { goodsId, qty, btn, pb, tv ->
            mService.shopUpdateItem(roomId, goodsId, qty) { e ->
                runOnMainThread {
                    if (e != null) {
                        showOperationInfo(context.getString(R.string.commerce_adjust_quantity_failed, e.code.toString()))
                    } else {
                        showOperationInfo(context.getString(R.string.commerce_shop_update_success))
                    }
                    btn.isEnabled = true
                    btn.setBackgroundResource(R.drawable.commerce_corner_radius_gradient_orange)
                    tv.isVisible = true
                    pb.isVisible = false
                }
            }
        }
        binding.root.setOnClickListener {
            dismiss()
        }
        binding.clAlert.setOnClickListener {
            binding.clAlert.visibility = View.INVISIBLE
        }
        binding.tvAlertSubmit.setOnClickListener {
            binding.clAlert.visibility = View.INVISIBLE
        }
    }

    private fun showOperationInfo(text: String) {
        binding.tvAlertInfo.text = text
        binding.clAlert.visibility = View.VISIBLE
    }

    private class ShopAdapter(
        private val isRoomOwner: Boolean
    ): BindingSingleAdapter<GoodsModel, CommerceShopGoodsItemLayoutBinding>(){

        var onClickBuy: ((goodsId: String, btn: View, progress: View, text: View) -> Unit)? = null

        var onUserChangedQty: ((goodsId: String, qty: Long, btn: View, progress: View, text: View) -> Unit)? = null

        override fun onBindViewHolder(
            holder: BindingViewHolder<CommerceShopGoodsItemLayoutBinding>,
            position: Int
        ) {
            getItem(position)?.let { item ->
                holder.binding.tvItemName.text = item.title
                holder.binding.ivCommodity.setImageResource(item.picResource)
                holder.binding.tvPrice.text = String.format("$%.0f", item.price)
                val context = holder.itemView.context
                holder.binding.tvQty.text = context.getString(R.string.commerce_shop_item_qty, item.quantity.toString())
                if (item.quantity == 0L) {
                    holder.binding.layoutBuy.setBackgroundResource(R.drawable.commerce_corner_radius_gray)
                    holder.binding.tvBuy.text = context.getString(R.string.commerce_shop_item_sold_out)
                    holder.binding.tvBuy.setTextColor(Color.parseColor("#A5ADBA"))
                    holder.binding.layoutBuy.isEnabled = false
                } else {
                    holder.binding.layoutBuy.setBackgroundResource(R.drawable.commerce_corner_radius_gradient_orange)
                    holder.binding.tvBuy.text = context.getString(R.string.commerce_shop_auction_buy)
                    holder.binding.tvBuy.setTextColor(Color.parseColor("#191919"))
                    holder.binding.layoutBuy.isEnabled = true
                }
                if (isRoomOwner) {
                    holder.binding.layoutBuy.visibility = View.GONE
                    holder.binding.llStepper.visibility = View.VISIBLE
                    holder.binding.etQty.setText(item.quantity.toString())
                    holder.binding.btnAdd.setOnClickListener {
                        val value = (holder.binding.etQty.text ?: "0").toString().toInt()
                        val newValue = fitValue(value + 1)
                        holder.binding.etQty.setText(newValue.toString())
                    }
                    holder.binding.btnReduce.setOnClickListener {
                        val value = (holder.binding.etQty.text ?: "0").toString().toInt()
                        val newValue = fitValue(value - 1)
                        holder.binding.etQty.setText(newValue.toString())
                    }
                    holder.binding.layoutSubmit.setOnClickListener {
                        val qty = holder.binding.etQty.text.toString().toIntOrNull() ?: 0
                        val newValue = fitValue(qty)
                        holder.binding.layoutSubmit.isEnabled = false
                        holder.binding.layoutSubmit.setBackgroundResource(R.drawable.commerce_corner_radius_gray)
                        holder.binding.tvSubmit.isVisible = false
                        holder.binding.progressLoading.isVisible = true
                        onUserChangedQty?.invoke(item.goodsId, newValue.toLong(), holder.binding.layoutSubmit, holder.binding.progressLoading, holder.binding.tvSubmit)
                    }
                    holder.binding.etQty.setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            inputMethodManager.hideSoftInputFromWindow(holder.binding.etQty.windowToken, 0)
                            holder.binding.etQty.clearFocus()
                            return@setOnEditorActionListener true
                        }
                        false
                    }
                } else {
                    holder.binding.layoutBuy.visibility = View.VISIBLE
                    holder.binding.llStepper.visibility = View.GONE
                    holder.binding.layoutSubmit.visibility = View.GONE
                    holder.binding.layoutBuy.setOnClickListener {
                        holder.binding.layoutBuy.isEnabled = false
                        holder.binding.layoutBuy.setBackgroundResource(R.drawable.commerce_corner_radius_gray)
                        holder.binding.tvBuy.isVisible = false
                        holder.binding.buyProgressLoading.isVisible = true
                        onClickBuy?.invoke(item.goodsId, holder.binding.layoutBuy, holder.binding.buyProgressLoading, holder.binding.tvBuy)
                    }
                }
            }
        }

        private fun fitValue(value: Int): Int {
            if (value < 0) { return 0 }
            if (value > 99) { return 99 }
            return value
        }
    }
}