package io.agora.scene.dreamFlow.widget

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
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
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceItemSwitchBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingDialogBinding
import io.agora.scene.dreamFlow.service.DreamFlowService

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

class StylizedSettingDialog constructor(
    context: Context,
    private val service: DreamFlowService
) : BottomFullDialog(context) {

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

    private var mStyleAdapter: BaseRecyclerViewAdapter<DreamFlowItemAiEffectBinding, StyleBean, StyleHolder>? =
        null

    private var mEffectAdapter: BaseRecyclerViewAdapter<DreamFlowItemAiEffectBinding, StyleBean, StyleHolder>? =
        null


    init {
        setContentView(mBinding.root)

        mBinding.ivBack.setOnClickListener {
            dismiss()
        }
        setupSwitchItem(ITEM_ID_SWITCH_STYLIZED, mBinding.stylizedEffect, R.string.dream_flow_setting_stylized, R.string.dream_flow_setting_stylized)
        setupSwitchItem(ITEM_ID_SWITCH_FACE_MODE, mBinding.FaceMode, R.string.dream_flow_setting_effect, R.string.dream_flow_setting_effect)
        setupStrength()
        setupVoiceEffectAdapter()
        setupStyleAdapter()
        mBinding.etDreamFlowDescribe.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                val layoutParams = mBinding.vDreamFlowSettingBottom.layoutParams
                layoutParams?.height = 300.dp.toInt()
                mBinding.vDreamFlowSettingBottom.layoutParams = layoutParams
                Handler(Looper.getMainLooper()).postDelayed({
                    mBinding.svStylizedSettings.smoothScrollTo(0, mBinding.svStylizedSettings.getChildAt(0).bottom)
                }, 300)
            } else {
                val layoutParams = mBinding.vDreamFlowSettingBottom.layoutParams
                layoutParams?.height = 40.dp.toInt()
                mBinding.vDreamFlowSettingBottom.layoutParams = layoutParams
                Handler(Looper.getMainLooper()).postDelayed({
                    mBinding.svStylizedSettings.smoothScrollTo(0, 0)
                }, 300)
            }
        }
        mBinding.svStylizedSettings.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                hideKeyboard()
                true
            } else {
                false
            }
        }
        window?.let { window ->
            val initialWindowHeight = Rect().apply { window.decorView.getWindowVisibleDisplayFrame(this) }.height()
            mBinding.root.viewTreeObserver?.addOnGlobalLayoutListener {
                Handler(Looper.getMainLooper()).postDelayed({
                    val currentWindowHeight = Rect().apply { window.decorView.getWindowVisibleDisplayFrame(this) }.height()
                    if (currentWindowHeight < initialWindowHeight) {
                    } else {
                        mBinding.etDreamFlowDescribe.clearFocus()
                    }
                }, 300)
            }
        }
        mBinding.tvDreamFlowSave.setOnClickListener {
            onClickSave()
        }
    }

    private fun onClickSave() {
        val bean = DreamFlowService.SettingBean(
            mBinding.stylizedEffect.switchCompat.isChecked,
            mBinding.FaceMode.switchCompat.isChecked,
            mBinding.Strength.sbProgress.progress * 0.01f,
            mStyleAdapter?.dataList?.firstOrNull { it.isSelect }?.title ?: "",
            mEffectAdapter?.dataList?.firstOrNull { it.isSelect }?.title ?: "",
            mBinding.etDreamFlowDescribe.text.toString()
        )

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

    private fun setupStyleAdapter() {
        val stringArray = context.resources.getStringArray(R.array.dream_flow_style)
        val list: MutableList<StyleBean> = ArrayList()
        for (i in stringArray.indices) {
            val drawable: Int = if (i == 0) {
                R.drawable.dream_flow_style_0
            } else if (i == 1) {
                R.drawable.dream_flow_style_1
            } else if (i == 2) {
                R.drawable.dream_flow_style_2
            } else {
                R.drawable.dream_flow_style_3
            }
            //val audioEffect = mSetting.getEffectIndex(i)
            list.add(StyleBean(i, 0, drawable, stringArray[i]))
        }
//        for (item in list) {
//            item.isSelect = (mSetting.mAudioEffect == item.audioEffect)
//        }
        mStyleAdapter =
            BaseRecyclerViewAdapter(
                list, object : OnItemClickListener<StyleBean> {
                    override fun onItemClick(data: StyleBean, view: View?, position: Int, viewType: Long) {
                        super.onItemClick(data, view, position, viewType)
                        //Log.d(TAG, "onItemClick audio effect  $position")
                        mStyleAdapter?.apply {
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

        mBinding.Style.recyclerView.adapter = mStyleAdapter
        mBinding.Style.tvTitle.text = context.resources.getString(R.string.dream_flow_setting_style_title)
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


    private fun setupStrength() {
        mBinding.Strength.apply {
            tvTitle.text = context.resources.getString(R.string.dream_flow_setting_strength_title)
            val value = 0.2
            tvSeekBarValue.text = String.format("%.1f", value)
            sbProgress.max = 1 * 100
            sbProgress.progress = (value * 100).toInt()
            sbProgress.setOnSeekBarChangeListener(object: OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvSeekBarValue.text = String.format("%.1f", (progress * 0.01))
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }
            })
        }
    }

    // 音效
    private fun setupVoiceEffectAdapter() {
        val stringArray = context.resources.getStringArray(R.array.dream_flow_effect)
        val list: MutableList<StyleBean> = ArrayList()
        for (i in stringArray.indices) {
            val drawable: Int = if (i == 0) {
                R.drawable.dream_flow_effect_0
            } else if (i == 1) {
                R.drawable.dream_flow_effect_1
            } else if (i == 2) {
                R.drawable.dream_flow_effect_2
            } else if (i == 3) {
                R.drawable.dream_flow_effect_3
            } else if (i == 4) {
                R.drawable.dream_flow_effect_4
            } else {
                R.drawable.dream_flow_effect_5
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
        mBinding.Effect.tvTitle.text = context.resources.getString(R.string.dream_flow_setting_effect_title)
        mBinding.Effect.recyclerView.adapter = mEffectAdapter
        mBinding.Effect.vBottomLine.visibility = View.GONE
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
        mBinding.Effect.recyclerView.addItemDecoration(itemDecoration)
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val v = mBinding.root
        imm.hideSoftInputFromWindow(v.windowToken, 0)
    }
}

