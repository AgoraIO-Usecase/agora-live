package io.agora.scene.dreamFlow.widget

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import io.agora.scene.base.component.BaseRecyclerViewAdapter
import io.agora.scene.base.component.OnItemClickListener
import io.agora.scene.base.utils.dp
import io.agora.scene.dreamFlow.R
import io.agora.scene.dreamFlow.VideoSetting
import io.agora.scene.dreamFlow.databinding.DreamFlowItemAiEffectBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceDialogAudienceBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceItemSwitchBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingDialogBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingsItemSliderBinding

data class StyleBean constructor(
    var id: Int,
    var audioEffect: Int,
    @field:DrawableRes var resId: Int,
    var title: String,
    var isSelect: Boolean = false
)

class StyleHolder constructor(mBinding: DreamFlowItemAiEffectBinding) :
    BaseRecyclerViewAdapter.BaseViewHolder<DreamFlowItemAiEffectBinding, StyleBean>(mBinding) {
    override fun binding(data: StyleBean?, selectedIndex: Int) {
        data ?: return
        mBinding.ivBg.setImageResource(data.resId)
        mBinding.tvTitle.text = data.title
        mBinding.select.isVisible = data.isSelect
    }
}

class StylizedSettingDialog constructor(context: Context) : BottomFullDialog(context) {

    companion object {

        private const val ITEM_ID_SWITCH_BASE = 0x00000001

        const val ITEM_ID_SWITCH_STYLIZED = ITEM_ID_SWITCH_BASE + 1

        const val ITEM_ID_SWITCH_FACE_MODE = ITEM_ID_SWITCH_BASE + 2

        const val ITEM_ID_SELECTOR_STYLE = ITEM_ID_SWITCH_BASE + 3

        const val ITEM_ID_SELECTOR_EFFECT = ITEM_ID_SWITCH_BASE + 4

    }

    /**
     * M binding
     */
    private val mBinding by lazy {
        DreamFlowSettingDialogBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
    }

    /**
     * Default item values
     */
    private val defaultItemValues = mutableMapOf<Int, Int>().apply {
        put(
            ITEM_ID_SWITCH_STYLIZED,
            VideoSetting.getCurrAudienceSetting().video.SR.value
        )
    }

    private var mEffectAdapter: BaseRecyclerViewAdapter<DreamFlowItemAiEffectBinding, StyleBean, StyleHolder>? =
        null


    init {
        setContentView(mBinding.root)

        mBinding.ivBack.setOnClickListener {
            dismiss()
        }
        setupSwitchItem(ITEM_ID_SWITCH_STYLIZED, mBinding.stylizedEffect, R.string.dream_flow_setting_stylized, 0)
        setupSwitchItem(ITEM_ID_SWITCH_FACE_MODE, mBinding.FaceMode, R.string.dream_flow_setting_effect, 0)
        setupVoiceEffectAdapter()
    }

    /**
     * Setup switch item
     *
     * @param itemId
     * @param binding
     * @param title
     * @param tip
     */
    private fun setupSwitchItem(
        itemId: Int,
        binding: DreamFlowSettingAdvanceItemSwitchBinding,
        @StringRes title: Int,
        @StringRes tip: Int
    ) {
        binding.tvTitle.text = context.getString(title)
        binding.ivTip.isVisible = tip != View.NO_ID
        binding.ivTip.setOnClickListener {
            ToastDialog(context).showTip(context.getString(tip))
        }
        binding.switchCompat.setOnCheckedChangeListener(null)
        binding.switchCompat.isChecked = VideoSetting.getCurrAudienceEnhanceSwitch()
        onSwitchChanged(itemId, binding.switchCompat.isChecked)
        binding.switchCompat.setOnCheckedChangeListener { btn, isChecked ->
            defaultItemValues[itemId] = if (isChecked) 1 else 0
            onSwitchChanged(itemId, isChecked)
        }
    }


    /**
     * On switch changed
     *
     * @param itemId
     * @param isChecked
     */
    private fun onSwitchChanged(itemId: Int, isChecked: Boolean) {
        when (itemId) {
            ITEM_ID_SWITCH_STYLIZED -> {

            }
            ITEM_ID_SWITCH_FACE_MODE -> {

            }
        }
    }

    // 音效
    private fun setupVoiceEffectAdapter() {
        val stringArray = context.resources.getStringArray(R.array.dream_flow_style)
        val list: MutableList<StyleBean> = ArrayList()
        for (i in stringArray.indices) {
            val drawable: Int = if (i == 0) {
                R.drawable.dream_flow_miyazaki
            } else if (i == 1) {
                R.drawable.dream_flow_toonyou
            } else if (i == 2) {
                R.drawable.dream_flow_sexytoon
            } else {
                R.drawable.dream_flow_clay
            }
            //val audioEffect = mSetting.getEffectIndex(i)
            list.add(StyleBean(i, 0, drawable, stringArray[i]))
        }
//        for (item in list) {
//            item.isSelect = (mSetting.mAudioEffect == item.audioEffect)
//        }

        mEffectAdapter =
            BaseRecyclerViewAdapter(
                list, object : OnItemClickListener<StyleBean> {
                    override fun onItemClick(data: StyleBean, view: View?, position: Int, viewType: Long) {
                        super.onItemClick(data, view, position, viewType)
                        //Log.d(TAG, "onItemClick audio effect  $position")
                        mEffectAdapter?.apply {
                            for (i in list.indices) {
                                list[i].isSelect = i == position
                                notifyItemChanged(i)
                            }
                            //mSetting.mAudioEffect = data.audioEffect
                        }
                    }
                },
                StyleHolder::class.java
            )

        mBinding.Style.recyclerView.adapter = mEffectAdapter
        val context = context ?: return
        val itemDecoration = object : DividerItemDecoration(context, HORIZONTAL) {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val itemCount = state.itemCount
                when (parent.getChildAdapterPosition(view)) {
                    0 -> { // first
                        //outRect.left = 20.dp.toInt()
                        outRect.right = 10.dp.toInt()
                    }

                    itemCount - 1 -> { // last
                        outRect.right = 20.dp.toInt()
                    }

                    else -> {
                        outRect.right = 10.dp.toInt()
                    }
                }
            }
        }
        mBinding.Style.recyclerView.addItemDecoration(itemDecoration)
    }

}

