package io.agora.scene.dreamFlow.widget

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayoutMediator
import io.agora.rtc2.RtcConnection
import io.agora.scene.dreamFlow.R
import io.agora.scene.dreamFlow.VideoSetting
import io.agora.scene.dreamFlow.VideoSetting.toIndex
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceAudioBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceDialogBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceItemSeekbarBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceItemSelectorBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceItemSwitchBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceItemSwitchSeekbarBinding
import io.agora.scene.dreamFlow.databinding.DreamFlowSettingAdvanceVideoBinding
import io.agora.scene.dreamFlow.utils.toInt
import io.agora.scene.widget.basic.BindingViewHolder
import java.util.*

/**
 * Advance setting dialog
 *
 * @property rtcConnection
 * @constructor
 *
 * @param context
 */
class AdvanceSettingDialog constructor(context: Context, val rtcConnection: RtcConnection) :
    BottomFullDialog(context) {

    companion object {
        private const val ITEM_ID_SWITCH_BASE = 0x00000001

        /**
         * Item Id Switch Quality Enhance
         */
        const val ITEM_ID_SWITCH_QUALITY_ENHANCE = ITEM_ID_SWITCH_BASE + 1

        /**
         * Item Id Switch Color Enhance
         */
        const val ITEM_ID_SWITCH_COLOR_ENHANCE = ITEM_ID_SWITCH_BASE + 2

        /**
         * Item Id Switch Dark Enhance
         */
        const val ITEM_ID_SWITCH_DARK_ENHANCE = ITEM_ID_SWITCH_BASE + 3

        /**
         * Item Id Switch Video Noise Reduce
         */
        const val ITEM_ID_SWITCH_VIDEO_NOISE_REDUCE = ITEM_ID_SWITCH_BASE + 4

        /**
         * Item Id Switch Bitrate Save
         */
        const val ITEM_ID_SWITCH_BITRATE_SAVE = ITEM_ID_SWITCH_BASE + 5

        /**
         * Item Id Switch Ear Back
         */
        const val ITEM_ID_SWITCH_EAR_BACK = ITEM_ID_SWITCH_BASE + 6

        /**
         * Item Id Switch Bitrate
         */
        const val ITEM_ID_SWITCH_BITRATE = ITEM_ID_SWITCH_BASE + 7

        private const val ITEM_ID_SEEKBAR_BASE = ITEM_ID_SWITCH_BASE shl 8

        /**
         * Item Id Seekbar Bitrate
         */
        const val ITEM_ID_SEEKBAR_BITRATE = ITEM_ID_SEEKBAR_BASE + 1

        /**
         * Item Id Seekbar Vocal Volume
         */
        const val ITEM_ID_SEEKBAR_VOCAL_VOLUME = ITEM_ID_SEEKBAR_BASE + 2

        /**
         * Item Id Seekbar Music Volume
         */
        const val ITEM_ID_SEEKBAR_MUSIC_VOLUME = ITEM_ID_SEEKBAR_BASE + 3

        private const val ITEM_ID_SELECTOR_BASE = ITEM_ID_SEEKBAR_BASE shl 8

        /**
         * Item Id Selector Resolution
         */
        const val ITEM_ID_SELECTOR_RESOLUTION = ITEM_ID_SELECTOR_BASE + 1

        /**
         * Item Id Selector Frame Rate
         */
        const val ITEM_ID_SELECTOR_FRAME_RATE = ITEM_ID_SELECTOR_BASE + 2

        const val ITEM_ID_SELECTOR_ENCODER = ITEM_ID_SELECTOR_BASE + 3

        private const val VIEW_TYPE_VIDEO_SETTING = 0
        private const val VIEW_TYPE_AUDIO_SETTING = 1
    }

    /**
     * M binding
     */
    private val mBinding by lazy {
        DreamFlowSettingAdvanceDialogBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
    }

    /**
     * Tab page
     *
     * @property viewType
     * @property title
     * @constructor Create empty Tab page
     */
    private data class TabPage constructor(
        val viewType: Int,
        @StringRes val title: Int
    )

    /**
     * Default item values
     */
    private val defaultItemValues = mutableMapOf<Int, Int>().apply {
        put(
            ITEM_ID_SWITCH_COLOR_ENHANCE,
            VideoSetting.getCurrBroadcastSetting().video.colorEnhance.toInt()
        )
        put(
            ITEM_ID_SWITCH_DARK_ENHANCE,
            VideoSetting.getCurrBroadcastSetting().video.lowLightEnhance.toInt()
        )
        put(
            ITEM_ID_SWITCH_VIDEO_NOISE_REDUCE,
            VideoSetting.getCurrBroadcastSetting().video.videoDenoiser.toInt()
        )
        put(ITEM_ID_SWITCH_BITRATE_SAVE, VideoSetting.getCurrBroadcastSetting().video.PVC.toInt())
        put(
            ITEM_ID_SWITCH_EAR_BACK,
            VideoSetting.getCurrBroadcastSetting().audio.inEarMonitoring.toInt()
        )
        put(
            ITEM_ID_SWITCH_BITRATE,
            VideoSetting.getCurrBroadcastSetting().video.bitRateStandard.toInt()
        )
        if (VideoSetting.getCurrBroadcastSetting().video.bitRateStandard) {
            put(ITEM_ID_SEEKBAR_BITRATE, VideoSetting.getCurrBroadcastSetting().video.bitRateRecommend)
        } else {
            put(ITEM_ID_SEEKBAR_BITRATE, VideoSetting.getCurrBroadcastSetting().video.bitRate)
        }
        put(
            ITEM_ID_SEEKBAR_VOCAL_VOLUME,
            VideoSetting.getCurrBroadcastSetting().audio.recordingSignalVolume
        )
        put(
            ITEM_ID_SEEKBAR_MUSIC_VOLUME,
            VideoSetting.getCurrBroadcastSetting().audio.audioMixingVolume
        )
        put(
            ITEM_ID_SELECTOR_RESOLUTION,
            VideoSetting.getCurrBroadcastSetting().video.encodeResolution.toIndex()
        )
        put(
            ITEM_ID_SELECTOR_FRAME_RATE,
            VideoSetting.getCurrBroadcastSetting().video.frameRate.toIndex()
        )
        put(
            ITEM_ID_SELECTOR_ENCODER,
            VideoSetting.getCurrBroadcastSetting().video.codecType.toIndex()
        )
    }

    /**
     * Tag page list
     */
    private val tagPageList = arrayListOf(
        TabPage(
            VIEW_TYPE_VIDEO_SETTING,
            R.string.dream_flow_setting_advance_video_setting,
        ),
        TabPage(
            VIEW_TYPE_AUDIO_SETTING,
            R.string.dream_flow_setting_advance_audio_setting,
        )
    )

    /**
     * Item in visible map
     */
    private val itemInVisibleMap = mutableMapOf<Int, Boolean>()

    /**
     * Item show text map
     */
    private val itemShowTextMap = mutableMapOf<Int, Boolean>()

    /**
     * Preset mode
     */
    private var presetMode = VideoSetting.isCurrBroadcastSettingRecommend()

    /**
     * Preset change tip dialog
     */
    private val presetChangeTipDialog by lazy {
        AlertDialog.Builder(context, R.style.dream_flow_alert_dialog).apply {
            setTitle(R.string.dream_flow_tip)
            setMessage(R.string.dream_flow_setting_advance_preset_mode)
            setPositiveButton(R.string.dream_flow_setting_confirm) { dialog, _ ->
                presetMode = false
                dialog.dismiss()
            }
            setNegativeButton(R.string.dream_flow_setting_cancel) { dialog, _ ->
                dialog.dismiss()
            }
        }.create()
    }

    init {
        setContentView(mBinding.root)

        mBinding.ivBack.setOnClickListener {
            dismiss()
        }

        mBinding.viewPager2.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ) = when (viewType) {
                VIEW_TYPE_VIDEO_SETTING -> BindingViewHolder(
                    DreamFlowSettingAdvanceVideoBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                VIEW_TYPE_AUDIO_SETTING -> BindingViewHolder(
                    DreamFlowSettingAdvanceAudioBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )

                else -> throw RuntimeException("Not support viewType: $viewType")
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                when (getItemViewType(position)) {
                    VIEW_TYPE_VIDEO_SETTING -> (holder as? BindingViewHolder<*>)?.binding?.let {
                        updateVideoSettingView(it as DreamFlowSettingAdvanceVideoBinding)
                    }

                    VIEW_TYPE_AUDIO_SETTING -> (holder as? BindingViewHolder<*>)?.binding?.let {
                        updateAudioSettingView(it as DreamFlowSettingAdvanceAudioBinding)
                    }

                    else -> throw RuntimeException("Can not find position support viewType. position: $position")
                }
            }

            override fun getItemCount() = tagPageList.size

            override fun getItemViewType(position: Int): Int {
                return tagPageList[position].viewType
            }
        }

        TabLayoutMediator(
            mBinding.tabLayout, mBinding.viewPager2
        ) { tab, position ->
            tab.text = context.getString(tagPageList[position].title)
        }.attach()
    }

    /**
     * Set item invisible
     *
     * @param itemId
     * @param invisible
     */
    fun setItemInvisible(itemId: Int, invisible: Boolean) {
        itemInVisibleMap[itemId] = invisible
    }

    /**
     * Set item show text only
     *
     * @param itemId
     * @param showText
     */
    fun setItemShowTextOnly(itemId: Int, showText: Boolean) {
        itemShowTextMap[itemId] = showText
    }

    /**
     * Update audio setting view
     *
     * @param binding
     */
    private fun updateAudioSettingView(binding: DreamFlowSettingAdvanceAudioBinding) {
        setupSwitchItem(
            ITEM_ID_SWITCH_EAR_BACK,
            binding.earBack,
            R.string.dream_flow_setting_advance_ear_back,
            R.string.dream_flow_setting_advance_ear_back_tip
        )
        setupSeekbarItem(
            ITEM_ID_SEEKBAR_VOCAL_VOLUME,
            binding.vocalVolume,
            R.string.dream_flow_setting_advance_vocal_volume,
            "%d",
            0, 100
        )
        setupSeekbarItem(
            ITEM_ID_SEEKBAR_MUSIC_VOLUME,
            binding.musicVolume,
            R.string.dream_flow_setting_advance_music_volume,
            "%d",
            0, 100
        )
    }

    /**
     * Update video setting view
     *
     * @param binding
     */
    private fun updateVideoSettingView(binding: DreamFlowSettingAdvanceVideoBinding) {
        setupSwitchItem(
            ITEM_ID_SWITCH_COLOR_ENHANCE,
            binding.colorEnhance,
            R.string.dream_flow_setting_advance_color_enhance,
            R.string.dream_flow_setting_advance_color_enhance_tip
        )
        setupSwitchItem(
            ITEM_ID_SWITCH_DARK_ENHANCE,
            binding.darkEnhance,
            R.string.dream_flow_setting_advance_dark_enhance,
            R.string.dream_flow_setting_advance_dark_enhance_tip
        )
        setupSwitchItem(
            ITEM_ID_SWITCH_VIDEO_NOISE_REDUCE,
            binding.videoNoiseReduction,
            R.string.dream_flow_setting_advance_video_noise_reduce,
            R.string.dream_flow_setting_advance_video_noise_reduce_tip
        )
        // 码率节省
        setupSwitchItem(
            ITEM_ID_SWITCH_BITRATE_SAVE,
            binding.bitrateSave,
            R.string.dream_flow_setting_advance_bitrate_save,
            R.string.dream_flow_setting_advance_bitrate_save_tip
        )
        // 编码分辨率
        setupSelectorItem(
            ITEM_ID_SELECTOR_RESOLUTION,
            binding.resolution,
            R.string.dream_flow_setting_advance_encode_resolution,
            R.string.dream_flow_setting_advance_encode_resolution_tip,
            VideoSetting.ResolutionList.map { "${it.width}x${it.height}" }
        )
        // 编码帧率
        setupSelectorItem(
            ITEM_ID_SELECTOR_FRAME_RATE,
            binding.frameRate,
            R.string.dream_flow_setting_advance_encode_framerate,
            R.string.dream_flow_setting_advance_encode_framerate_tip,
            VideoSetting.FrameRateList.map { "${it.fps} fps" }
        )
        setupSelectorItem(
            ITEM_ID_SELECTOR_ENCODER,
            binding.encoder,
            R.string.dream_flow_setting_advance_encoder,
            selectList = VideoSetting.EncoderList.map { it.name.split("_")[2] }
        )
        setupSwitchAndSeekbarItem(
            ITEM_ID_SWITCH_BITRATE,
            ITEM_ID_SEEKBAR_BITRATE,
            binding.bitrate,
            R.string.dream_flow_setting_advance_bitrate,
            R.string.dream_flow_setting_advance_bitrate_tips,
            "%d kbps",
            200, 4000
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
        binding.root.isVisible = itemInVisibleMap[itemId]?.not() ?: true
        binding.tvTitle.text = context.getString(title)
        binding.ivTip.setOnClickListener {
            ToastDialog(context).showTip(context.getString(tip))
        }
        if (itemShowTextMap[itemId] == true) {
            binding.switchCompat.isVisible = false
            binding.tvSwitch.isVisible = true
            val isChecked = (defaultItemValues[itemId] ?: 0) > 0
            binding.tvSwitch.text =
                context.getString(if (isChecked) R.string.dream_flow_setting_opened else R.string.dream_flow_setting_closed)
        } else {
            binding.switchCompat.isVisible = true
            binding.tvSwitch.isVisible = false
            binding.switchCompat.setOnCheckedChangeListener(null)
            binding.switchCompat.isChecked = (defaultItemValues[itemId] ?: 0) > 0
            onSwitchChanged(itemId, binding.switchCompat.isChecked)
            binding.switchCompat.setOnCheckedChangeListener { btn, isChecked ->
                if (checkPresetMode()) {
                    btn.isChecked = !isChecked
                } else {
                    defaultItemValues[itemId] = if (isChecked) 1 else 0
                    onSwitchChanged(itemId, isChecked)
                }
            }
        }
    }

    /**
     * Setup selector item
     *
     * @param itemId
     * @param binding
     * @param title
     * @param tip
     * @param selectList
     */
    private fun setupSelectorItem(
        itemId: Int,
        binding: DreamFlowSettingAdvanceItemSelectorBinding,
        @StringRes title: Int,
        @StringRes tip: Int = -1,
        selectList: List<String>
    ) {
        binding.root.isVisible = itemInVisibleMap[itemId]?.not() ?: true
        binding.tvTitle.text = context.getString(title)
        binding.ivTip.visibility = if (tip == -1) View.GONE else View.VISIBLE
        binding.ivTip.setOnClickListener {
            ToastDialog(context).showTip(context.getString(tip))
        }
        val selectPosition = defaultItemValues[itemId] ?: 0
        binding.tvValue.text = selectList.getOrNull(selectPosition)
        //onSelectorChanged(itemId, selectPosition)
        binding.root.setOnClickListener {
            if (!checkPresetMode()) {
                BottomLightListDialog(context).apply {
                    setTitle(title)
                    setListData(selectList)
                    setSelectedPosition(defaultItemValues[itemId] ?: 0)
                    setOnSelectedChangedListener { dialog, index ->
                        defaultItemValues[itemId] = index
                        binding.tvValue.text = selectList.getOrNull(index)
                        onSelectorChanged(itemId, index)
                        dialog.dismiss()
                    }
                    show()
                }
            }

        }
    }

    /**
     * Setup seekbar item
     *
     * @param itemId
     * @param binding
     * @param title
     * @param valueFormat
     * @param fromValue
     * @param toValue
     */
    private fun setupSeekbarItem(
        itemId: Int,
        binding: DreamFlowSettingAdvanceItemSeekbarBinding,
        @StringRes title: Int,
        valueFormat: String,
        fromValue: Int,
        toValue: Int
    ) {
        binding.root.isVisible = itemInVisibleMap[itemId]?.not() ?: true
        binding.tvTitle.text = context.getString(title)
        binding.slider.valueFrom = fromValue.toFloat()
        binding.slider.valueTo = toValue.toFloat()
        val defaultValue = defaultItemValues[itemId]?.toFloat() ?: fromValue.toFloat()
        binding.slider.value = defaultValue
        binding.tvValue.text = String.format(Locale.US, valueFormat, binding.slider.value.toInt())
        binding.slider.clearOnChangeListeners()
        binding.slider.clearOnSliderTouchListeners()
        onSeekbarChanged(itemId, defaultValue.toInt())
        var changed = false
        binding.slider.addOnChangeListener { status, nValue, fromUser ->
            if (fromUser) {
                if (checkPresetMode()) {
                    binding.slider.value = defaultValue
                } else {
                    binding.tvValue.text = String.format(Locale.US, valueFormat, nValue.toInt())
                    defaultItemValues[itemId] = nValue.toInt()
                    changed = true
                }
            }
        }
        binding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {

            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                if (changed) {
                    onSeekbarChanged(itemId, slider.value.toInt())
                    changed = false
                }
            }
        })
    }

    /**
     * Setup switch and seekbar item
     *
     * @param itemIdSwitch
     * @param itemIdSeekbar
     * @param binding
     * @param title
     * @param tip
     * @param valueFormat
     * @param fromValue
     * @param toValue
     */
    private fun setupSwitchAndSeekbarItem(
        itemIdSwitch: Int,
        itemIdSeekbar: Int,
        binding: DreamFlowSettingAdvanceItemSwitchSeekbarBinding,
        @StringRes title: Int,
        @StringRes tip: Int,
        valueFormat: String,
        fromValue: Int,
        toValue: Int
    ) {
        binding.root.isVisible = itemInVisibleMap[itemIdSwitch]?.not() ?: true
        binding.tvTitle.text = context.getString(title)
        binding.ivTip.visibility = if (tip == -1) View.GONE else View.VISIBLE
        binding.ivTip.setOnClickListener {
            ToastDialog(context).showTip(context.getString(tip))
        }
        binding.switchCompat.setOnCheckedChangeListener(null)
        binding.switchCompat.isChecked = (defaultItemValues[itemIdSwitch] ?: 0) > 0
        onSwitchChanged(itemIdSwitch, binding.switchCompat.isChecked)
        binding.switchCompat.setOnCheckedChangeListener { btn, isChecked ->
            if (checkPresetMode()) {
                btn.isChecked = !isChecked
            } else {
                defaultItemValues[itemIdSwitch] = if (isChecked) 1 else 0
                onSwitchChanged(itemIdSwitch, isChecked)
                binding.slider.visibility = if (isChecked) View.GONE else View.VISIBLE
                binding.tvValue.visibility = if (isChecked) View.GONE else View.VISIBLE
                if (!isChecked) { // 关闭时候设置推荐默认值
                    if (VideoSetting.getCurrBroadcastSetting().video.bitRate == 0) {
                        binding.slider.value =
                            VideoSetting.getCurrBroadcastSetting().video.bitRateRecommend.toFloat()
                        VideoSetting.updateBroadcastSetting(
                            rtcConnection,
                            bitRate = VideoSetting.getCurrBroadcastSetting().video.bitRateRecommend
                        )
                    } else {
                        binding.slider.value =
                            VideoSetting.getCurrBroadcastSetting().video.bitRate.toFloat()
                    }
                    binding.tvValue.text =
                        String.format(Locale.US, valueFormat, binding.slider.value.toInt())
                }
            }
        }

        binding.slider.visibility = if (binding.switchCompat.isChecked) View.GONE else View.VISIBLE
        binding.tvValue.visibility = if (binding.switchCompat.isChecked) View.GONE else View.VISIBLE
        binding.slider.valueFrom = fromValue.toFloat()
        binding.slider.valueTo = toValue.toFloat()
        val defaultValue = defaultItemValues[itemIdSeekbar]?.toFloat() ?: fromValue.toFloat()
        binding.slider.value = defaultValue
        binding.tvValue.text = String.format(Locale.US, valueFormat, binding.slider.value.toInt())
        binding.slider.clearOnChangeListeners()
        binding.slider.clearOnSliderTouchListeners()
        if (!binding.switchCompat.isChecked) {
            onSeekbarChanged(itemIdSeekbar, defaultValue.toInt())
        }
        var changed = false
        binding.slider.addOnChangeListener { status, nValue, fromUser ->
            if (fromUser) {
                if (checkPresetMode()) {
                    binding.slider.value = defaultValue
                } else {
                    binding.tvValue.text = String.format(Locale.US, valueFormat, nValue.toInt())
                    defaultItemValues[itemIdSeekbar] = nValue.toInt()
                    changed = true
                }
            }
        }
        binding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {

            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                if (changed) {
                    onSeekbarChanged(itemIdSeekbar, slider.value.toInt())
                    changed = false
                }
            }
        })
    }

    /**
     * Check preset mode
     *
     * @return
     */
    private fun checkPresetMode(): Boolean {
        if (!presetMode) {
            return false
        }
        if (!presetChangeTipDialog.isShowing) {
            presetChangeTipDialog.show()
        }
        return true
    }

    /**
     * On switch changed
     *
     * @param itemId
     * @param isChecked
     */
    private fun onSwitchChanged(itemId: Int, isChecked: Boolean) {
        when (itemId) {
            ITEM_ID_SWITCH_COLOR_ENHANCE -> VideoSetting.updateBroadcastSetting(colorEnhance = isChecked)
            ITEM_ID_SWITCH_DARK_ENHANCE -> VideoSetting.updateBroadcastSetting(lowLightEnhance = isChecked)
            ITEM_ID_SWITCH_VIDEO_NOISE_REDUCE -> VideoSetting.updateBroadcastSetting(videoDenoiser = isChecked)
            ITEM_ID_SWITCH_BITRATE_SAVE -> VideoSetting.updateBroadcastSetting(PVC = isChecked)
            ITEM_ID_SWITCH_EAR_BACK -> VideoSetting.updateBroadcastSetting(inEarMonitoring = isChecked)
            ITEM_ID_SWITCH_BITRATE -> VideoSetting.updateBroadcastSetting(bitRateStandard = isChecked)
        }
    }

    /**
     * On seekbar changed
     *
     * @param itemId
     * @param value
     */
    private fun onSeekbarChanged(itemId: Int, value: Int) {
        when (itemId) {
            ITEM_ID_SEEKBAR_BITRATE -> VideoSetting.updateBroadcastSetting(
                bitRate = value
            )

            ITEM_ID_SEEKBAR_VOCAL_VOLUME -> VideoSetting.updateBroadcastSetting(
                recordingSignalVolume = value
            )

            ITEM_ID_SEEKBAR_MUSIC_VOLUME -> VideoSetting.updateBroadcastSetting(
                rtcConnection,
                audioMixingVolume = value
            )
        }
    }

    /**
     * On selector changed
     *
     * @param itemId
     * @param index
     */
    private fun onSelectorChanged(itemId: Int, index: Int) {
        if (index < 0) return
        when (itemId) {
            ITEM_ID_SELECTOR_RESOLUTION -> VideoSetting.updateBroadcastSetting(
                rtcConnection = rtcConnection,
                encoderResolution = VideoSetting.ResolutionList[index],
                captureResolution = VideoSetting.ResolutionList[index]
            )

            ITEM_ID_SELECTOR_FRAME_RATE -> VideoSetting.updateBroadcastSetting(
                rtcConnection = rtcConnection,
                frameRate = VideoSetting.FrameRateList[index]
            )
            ITEM_ID_SELECTOR_ENCODER -> VideoSetting.updateBroadcastSetting(
                rtcConnection = rtcConnection,
                codecType = VideoSetting.EncoderList[index]
            )
        }
    }
}

