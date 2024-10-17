package io.agora.scene.dreamFlow.widget

import android.content.Context
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import io.agora.scene.base.component.BaseRecyclerViewAdapter
import io.agora.scene.base.component.OnItemClickListener
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.base.utils.dp
import io.agora.scene.dreamFlow.R
import io.agora.scene.dreamFlow.VideoSetting
import io.agora.scene.dreamFlow.databinding.DreamFlowItemAiEffectBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingDialogBinding
import io.agora.scene.dreamFlow.service.DreamFlowService

data class ItemBean constructor(
    var id: Int,
    var audioEffect: Int,
    @field:DrawableRes var resId: Int,
    var title: String,
    var isSelect: Boolean = false
)

class StyleHolder constructor(mBinding: DreamFlowItemAiEffectBinding) :
    BaseRecyclerViewAdapter.BaseViewHolder<DreamFlowItemAiEffectBinding, ItemBean>(mBinding) {
    override fun binding(data: ItemBean?, selectedIndex: Int) {
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

    private val mBinding by lazy { DreamFlowSettingDialogBinding.inflate(LayoutInflater.from(context)) }

    private var mStyleAdapter: BaseRecyclerViewAdapter<DreamFlowItemAiEffectBinding, ItemBean, StyleHolder>? =
        null

    private var mEffectAdapter: BaseRecyclerViewAdapter<DreamFlowItemAiEffectBinding, ItemBean, StyleHolder>? =
        null

    init {
        setContentView(mBinding.root)
        if (service.currentSetting == null) {
            service.selectedPreset = 0
            service.currentSetting = getPresets().firstOrNull()?.getPreset()
        }
        setupView()
        setupPresetAdapter()
        setupStyleAdapter()
        // setup data
        mBinding.stylizedEffect.switchCompat.isChecked = service.isEffectOn
        service.currentSetting?.let {
            setupWithSetting(it)
        }
    }

    private fun setupWithSetting(bean: DreamFlowService.SettingBean) {
        mBinding.FaceMode.switchCompat.isChecked = bean.isFaceModeOn
        mBinding.Strength.apply {
            val value = bean.strength
            sbProgress.progress = (value * 100).toInt()
            tvSeekBarValue.text = String.format("%.1f", value)
        }
        mBinding.superFrame.apply {
            val value = bean.superFrame
            sbProgress.progress = value * 100
            tvSeekBarValue.text = String.format("%.1f", value.toFloat())
        }
        val styleIndex = getStyles().indexOfFirst { it == bean.style }
        mStyleAdapter?.let { adapter ->
            val list = adapter.dataList
            for (i in adapter.dataList.indices) {
                val selected = i == styleIndex
                if (list[i].isSelect != selected) {
                    list[i].isSelect = selected
                    adapter.notifyItemChanged(i)
                }
            }
        }
        mBinding.etDreamFlowPrompt.setText(bean.prompt)
        if (getPresets()[service.selectedPreset] == PresetTypes.Customized) {
            mBinding.vCustomMask.visibility = View.GONE
        } else {
            mBinding.vCustomMask.visibility = View.VISIBLE
        }
        if (service.isEffectOn) {
            mBinding.vOnMask.visibility = View.VISIBLE
        } else {
            mBinding.vOnMask.visibility = View.GONE
        }
    }

    private fun onClickSave() {
        val styleIndex = mStyleAdapter?.dataList?.indexOfFirst { it.isSelect } ?: 0
        val style = getStyles()[styleIndex]
        val bean = DreamFlowService.SettingBean(
            mBinding.FaceMode.switchCompat.isChecked,
            mBinding.Strength.sbProgress.progress * 0.01f,
            (mBinding.superFrame.sbProgress.progress * 0.01f).toInt(),
            style,
            mBinding.etDreamFlowPrompt.text.toString()
        )
        addLoadingView()
        service.save(
            mBinding.stylizedEffect.switchCompat.isChecked,
            bean, {
            // succeed
            hideLoadingView()
            dismiss()
        }, { e ->
            // failure
            hideLoadingView()
            ToastUtils.showToast(e?.message ?: "error")
        })
    }

    private fun onClickServer() {
        val serverList = DreamFlowService.ServerType.values()
        val titles = serverList.map { it.title() }
        BottomLightListDialog(context).apply {
            setTitle(context.getString(R.string.dream_flow_setting_service))
            setListData(titles)
            setOnSelectedChangedListener { dialog, index ->
                service.server = serverList[index]
                mBinding.itemService.tvDetailInfo.text = service.server.title()
                dialog.dismiss()
            }
            show()
        }
    }

    private fun setupView() {
        mBinding.ivBack.setOnClickListener {
            dismiss()
        }
        mBinding.tvDreamFlowSave.setOnClickListener {
            onClickSave()
        }
        mBinding.vCustomMask.setOnClickListener {
        }
        mBinding.vOnMask.setOnClickListener {
        }
        mBinding.stylizedEffect.apply {
            tvTitle.text = context.getString(R.string.dream_flow_setting_stylized)
            ivTips.isVisible = false
            switchCompat.setOnCheckedChangeListener(null)
            switchCompat.isChecked = VideoSetting.getCurrAudienceEnhanceSwitch()
            switchCompat.setOnCheckedChangeListener { btn, isChecked ->
            }
        }
        mBinding.itemService.apply {
            tvTitle.text = context.getString(R.string.dream_flow_setting_service)
            tvDetailInfo.text = service.server.title()
            root.setOnClickListener {
                onClickServer()
            }
        }
        mBinding.FaceMode.apply {
            tvTitle.text = context.getString(R.string.dream_flow_setting_effect)
            ivTips.isVisible = false
            switchCompat.setOnCheckedChangeListener(null)
            switchCompat.isChecked = VideoSetting.getCurrAudienceEnhanceSwitch()
            switchCompat.setOnCheckedChangeListener { btn, isChecked ->
            }
        }
        // Strength
        mBinding.Strength.apply {
            tvTitle.text = context.resources.getString(R.string.dream_flow_setting_strength_title)
            val value = 0.2
            tvSeekBarValue.text = String.format("%.1f", value)
            sbProgress.max = 1 * 100
            tvSeekBarMax.text = "1.0"
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
        // superFrame
        mBinding.superFrame.apply {
            tvTitle.text = context.resources.getString(R.string.dream_flow_setting_frame_title)
            val value = 1
            tvSeekBarValue.text = String.format("%d.0", value)
            sbProgress.max = 2 * 100
            tvSeekBarMax.text = "2.0"
            sbProgress.progress = value * 200
            sbProgress.setOnSeekBarChangeListener(object: OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    tvSeekBarValue.text = String.format("%d.0", (progress * 0.01).toInt())
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }
            })
        }
        // prompt
        mBinding.etDreamFlowPrompt.setOnFocusChangeListener { v, hasFocus ->
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
                        mBinding.etDreamFlowPrompt.clearFocus()
                    }
                }, 300)
            }
        }
    }

    private fun setupStyleAdapter() {
        val styles = getStyles()
        val list: MutableList<ItemBean> = ArrayList()
        for (i in styles.indices) {
            val style = styles[i]
            list.add(ItemBean(i, 0, style.getRes(), styles[i].getString()))
        }
        mStyleAdapter =
            BaseRecyclerViewAdapter(
                list, object : OnItemClickListener<ItemBean> {
                    override fun onItemClick(data: ItemBean, view: View?, position: Int, viewType: Long) {
                        super.onItemClick(data, view, position, viewType)
                        mStyleAdapter?.apply {
                            for (i in list.indices) {
                                list[i].isSelect = i == position
                                notifyItemChanged(i)
                            }
                        }
                    }
                },
                StyleHolder::class.java
            )

        mBinding.Style.recyclerView.adapter = mStyleAdapter
        mBinding.Style.tvTitle.text = context.resources.getString(R.string.dream_flow_setting_style_title)
        val context = context
        val itemDecoration = object : DividerItemDecoration(context, HORIZONTAL) {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val itemCount = state.itemCount
                when (parent.getChildAdapterPosition(view)) {
                    0 -> { // first
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

    private fun setupPresetAdapter() {
        val presets = getPresets()
        val list: MutableList<ItemBean> = ArrayList()
        for (i in presets.indices) {
            val style = presets[i]
            list.add(ItemBean(i, 0, style.getRes(), presets[i].getString()))
        }
        list[service.selectedPreset].isSelect = true
        mEffectAdapter = BaseRecyclerViewAdapter(
                list, object : OnItemClickListener<ItemBean> {
                    override fun onItemClick(data: ItemBean, view: View?, position: Int, viewType: Long) {
                        super.onItemClick(data, view, position, viewType)
                        mEffectAdapter?.apply {
                            for (i in list.indices) {
                                val selected = (i == position)
                                if (list[i].isSelect != selected) {
                                    list[i].isSelect = selected
                                    notifyItemChanged(i)
                                }
                            }
                        }
                        service.selectedPreset = position
                        val bean = getPresets()[position].getPreset()
                        setupWithSetting(bean)
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

    private var loadingView: View? = null
    private fun addLoadingView() {
        if (this.loadingView == null) {
            val rootView = window?.decorView?.findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as ViewGroup
            this.loadingView = LayoutInflater.from(context).inflate(R.layout.view_base_loading, rootView, false)
            rootView.addView(
                this.loadingView,
                ViewGroup.LayoutParams(-1, -1)
            )
        }
        this.loadingView?.visibility = View.VISIBLE
    }

    private fun hideLoadingView() {
        if (loadingView == null) {
            return
        }
        window?.apply {
            decorView.post {
                loadingView?.visibility = View.GONE
            }
        }
    }

    private fun getStyles(): List<DreamFlowService.Style> {
        return listOf(
            DreamFlowService.Style.Toonyou,
            DreamFlowService.Style.Miyazaki,
            DreamFlowService.Style.Sexytoon,
            DreamFlowService.Style.Clay,
            DreamFlowService.Style.Fantasy
        )
    }

    private fun DreamFlowService.Style.getString(): String {
        return when (this) {
            DreamFlowService.Style.Toonyou -> "Toonyou"
            DreamFlowService.Style.Miyazaki -> "Miyazaki"
            DreamFlowService.Style.Sexytoon -> "Sexytoon"
            DreamFlowService.Style.Clay -> "Clay"
            DreamFlowService.Style.Fantasy -> "Fantasy"
        }
    }

    private fun DreamFlowService.Style.getRes(): Int {
        return when (this) {
            DreamFlowService.Style.Toonyou -> R.drawable.dream_flow_style_toonyou
            DreamFlowService.Style.Miyazaki -> R.drawable.dream_flow_style_miyazaki
            DreamFlowService.Style.Sexytoon -> R.drawable.dream_flow_style_sexytoon
            DreamFlowService.Style.Clay -> R.drawable.dream_flow_style_clay
            DreamFlowService.Style.Fantasy -> R.drawable.dream_flow_style_fantasy
        }
    }

    private fun getPresets(): List<PresetTypes> {
        return listOf(
            PresetTypes.CartoonsFull,
            PresetTypes.ClayFull,
            PresetTypes.Anime,
            PresetTypes.Anime3D,
            PresetTypes.Joker,
            PresetTypes.Customized
        )
    }

    enum class PresetTypes {
        CartoonsFull,
        ClayFull,
        Anime,
        Anime3D,
        Joker,
        Customized;

        fun getString(): String {
            return when (this) {
                CartoonsFull -> "CartoonsFull"
                ClayFull -> "ClayFull"
                Anime -> "Anime"
                Anime3D -> "3D"
                Joker -> "Joker"
                Customized -> "Customized"
            }
        }

        fun getRes(): Int {
            return when (this) {
                CartoonsFull -> R.drawable.dream_flow_preset_cartoonsfull
                ClayFull -> R.drawable.dream_flow_preset_clayfull
                Anime -> R.drawable.dream_flow_preset_anime
                Anime3D -> R.drawable.dream_flow_preset_3d
                Joker -> R.drawable.dream_flow_preset_joker
                Customized -> R.drawable.dream_flow_preset_customized
            }
        }

        fun getPreset(): DreamFlowService.SettingBean {
            return when (this) {
                CartoonsFull -> DreamFlowService.SettingBean(
                    false,
                    0.4f,
                    1,
                    DreamFlowService.Style.Toonyou,
                    "best quality"
                )
                ClayFull -> DreamFlowService.SettingBean(
                    false,
                    0.5f,
                    1,
                    DreamFlowService.Style.Miyazaki,
                    "Claymation, best quality"
                )
                Anime -> DreamFlowService.SettingBean(
                    true,
                    0.5f,
                    1,
                    DreamFlowService.Style.Miyazaki,
                    "anime, cute"
                )
                Anime3D -> DreamFlowService.SettingBean(
                    true,
                    0.6f,
                    1,
                    DreamFlowService.Style.Sexytoon,
                    "3D, big eyes"
                )
                Joker -> DreamFlowService.SettingBean(
                    true,
                    0.8f,
                    1,
                    DreamFlowService.Style.Fantasy,
                    "Joker, pale face"
                )
                Customized -> DreamFlowService.SettingBean(
                    true,
                    0.6f,
                    1,
                    DreamFlowService.Style.Toonyou,
                    ""
                )
            }
        }
    }
}

