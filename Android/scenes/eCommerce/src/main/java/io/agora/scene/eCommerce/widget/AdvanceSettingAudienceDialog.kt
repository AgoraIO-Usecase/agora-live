package io.agora.scene.eCommerce.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.VideoSetting
import io.agora.scene.eCommerce.databinding.CommerceSettingAdvanceDialogAudienceBinding
import io.agora.scene.eCommerce.databinding.CommerceSettingAdvanceItemSwitchBinding

/**
 * Advance setting audience dialog
 *
 * @constructor
 *
 * @param context
 */
class AdvanceSettingAudienceDialog constructor(context: Context) : BottomFullDialog(context) {

    companion object {
        /**
         * Item Id Switch Base
         */
        private const val ITEM_ID_SWITCH_BASE = 0x00000001

        /**
         * Item Id Switch Quality Enhance
         */
        const val ITEM_ID_SWITCH_QUALITY_ENHANCE = ITEM_ID_SWITCH_BASE + 1

    }

    /**
     * M binding
     */
    private val mBinding by lazy {
        CommerceSettingAdvanceDialogAudienceBinding.inflate(
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
            ITEM_ID_SWITCH_QUALITY_ENHANCE,
            VideoSetting.getCurrAudienceSetting().video.SR.value
        )
    }

    init {
        setContentView(mBinding.root)

        mBinding.ivBack.setOnClickListener {
            dismiss()
        }
        setupSwitchItem(ITEM_ID_SWITCH_QUALITY_ENHANCE, mBinding.qualityEnhance, R.string.commerce_setting_advance_quality_enhance, View.NO_ID)
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
        binding: CommerceSettingAdvanceItemSwitchBinding,
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
            ITEM_ID_SWITCH_QUALITY_ENHANCE -> {
                VideoSetting.setCurrAudienceEnhanceSwitch(isChecked)
                VideoSetting.updateSRSetting(SR = VideoSetting.getCurrAudienceSetting().video.SR)
            }
        }
    }

}

