package io.agora.scene.eCommerce.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.VideoSetting
import io.agora.scene.eCommerce.databinding.CommerceSettingPresetAudienceDialogBinding

/**
 * Preset audience dialog
 *
 * @constructor
 *
 * @param context
 * @param showCloseBtn
 */
class PresetAudienceDialog constructor(context: Context, showCloseBtn: Boolean = true) : BottomFullDialog(context) {

    /**
     * Call back
     */
    var callBack: OnPresetAudienceDialogCallBack? = null

    /**
     * M binding
     */
    private val mBinding by lazy {
        CommerceSettingPresetAudienceDialogBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
    }

    init {
        setContentView(mBinding.root)
        mBinding.ivClose.visibility = if (showCloseBtn) View.VISIBLE else View.INVISIBLE
        mBinding.ivClose.setOnClickListener {
            onPresetShowModeSelected(-1)
            dismiss()
        }
        mBinding.tvConfirm.setOnClickListener {
            val showSelectPosition = getGroupSelectedItem(
                mBinding.enhanceChooseItemLowDevice,
                mBinding.enhanceChooseItemMediumDevice,
                mBinding.enhanceChooseItemHighDevice,
                mBinding.basicChooseItemLowDevice,
                mBinding.basicChooseItemMediumDevice,
                mBinding.basicChooseItemHighDevice
            )
            if (showSelectPosition < 0) {
                ToastDialog(context).apply {
                    dismissDelayShort()
                    showMessage(context.getString(R.string.commerce_setting_preset_no_choise_tip))
                }
                return@setOnClickListener
            }
            onPresetShowModeSelected(showSelectPosition)
            callBack?.onClickConfirm()
            dismiss()
        }
        groupItems(
            {}, -1,
            mBinding.enhanceChooseItemLowDevice,
            mBinding.enhanceChooseItemMediumDevice,
            mBinding.enhanceChooseItemHighDevice,
            mBinding.basicChooseItemLowDevice,
            mBinding.basicChooseItemMediumDevice,
            mBinding.basicChooseItemHighDevice
        )
    }

    /**
     * Get group selected item
     *
     * @param itemViews
     * @return
     */
    private fun getGroupSelectedItem(vararg itemViews: View): Int {
        itemViews.forEachIndexed { index, view ->
            if (view.isActivated) {
                return index
            }
        }
        return -1
    }

    /**
     * Group items
     *
     * @param onSelectChanged
     * @param activateIndex
     * @param itemViews
     * @receiver
     */
    private fun groupItems(
        onSelectChanged: (Int) -> Unit,
        activateIndex: Int,
        vararg itemViews: View
    ) {
        itemViews.forEachIndexed { index, view ->
            view.isActivated = activateIndex == index
            view.setOnClickListener {
                if (view.isActivated) {
                    return@setOnClickListener
                }
                itemViews.forEach { it.isActivated = it == view }
                onSelectChanged.invoke(index)
            }
        }
    }

    /**
     * On preset show mode selected
     *
     * @param level
     */
    private fun onPresetShowModeSelected(level: Int) {
        if (level < 0) {
            // 没有选择默认使用低端机配置
            return
        }
        VideoSetting.setCurrAudiencePlaySetting(level)
        when (VideoSetting.getCurrAudiencePlaySetting()) {
            // 低端机：画质增强
            VideoSetting.AudiencePlaySetting.ENHANCE_LOW -> {
                VideoSetting.setCurrAudienceEnhanceSwitch(false)
                VideoSetting.updateAudioSetting(SR = VideoSetting.SuperResolution.SR_NONE)
                VideoSetting.updateBroadcastSetting(deviceLevel = VideoSetting.DeviceLevel.Low, isByAudience = true)
            }
            // 中端机：画质增强
            VideoSetting.AudiencePlaySetting.ENHANCE_MEDIUM -> {
                VideoSetting.setCurrAudienceEnhanceSwitch(true)
                VideoSetting.updateAudioSetting(SR = VideoSetting.SuperResolution.SR_SUPER)
                VideoSetting.updateBroadcastSetting(deviceLevel = VideoSetting.DeviceLevel.Medium, isByAudience = true)
            }
            // 高端机：画质增强
            VideoSetting.AudiencePlaySetting.ENHANCE_HIGH -> {
                VideoSetting.setCurrAudienceEnhanceSwitch(true)
                VideoSetting.updateAudioSetting(SR = VideoSetting.SuperResolution.SR_SUPER)
                VideoSetting.updateBroadcastSetting(deviceLevel = VideoSetting.DeviceLevel.High, isByAudience = true)
            }
            // 低端机：基础
            VideoSetting.AudiencePlaySetting.BASE_LOW -> {
                VideoSetting.setCurrAudienceEnhanceSwitch(false)
                VideoSetting.updateAudioSetting(SR = VideoSetting.SuperResolution.SR_NONE)
                VideoSetting.updateBroadcastSetting(deviceLevel = VideoSetting.DeviceLevel.Low, isByAudience = true)
            }
            // 中端机：基础
            VideoSetting.AudiencePlaySetting.BASE_MEDIUM -> {
                VideoSetting.setCurrAudienceEnhanceSwitch(false)
                VideoSetting.updateAudioSetting(SR = VideoSetting.SuperResolution.SR_NONE)
                VideoSetting.updateBroadcastSetting(deviceLevel = VideoSetting.DeviceLevel.Medium, isByAudience = true)
            }
            // 高端机：基础
            VideoSetting.AudiencePlaySetting.BASE_HIGH -> {
                VideoSetting.setCurrAudienceEnhanceSwitch(false)
                VideoSetting.updateAudioSetting(SR = VideoSetting.SuperResolution.SR_NONE)
                VideoSetting.updateBroadcastSetting(deviceLevel = VideoSetting.DeviceLevel.High, isByAudience = true)
            }
        }

        ToastDialog(context).apply {
            dismissDelayShort()
            showMessage(context.getString(R.string.commerce_setting_preset_done))
        }
    }


}

/**
 * On preset audience dialog call back
 *
 * @constructor Create empty On preset audience dialog call back
 */
interface OnPresetAudienceDialogCallBack {

    /**
     * On click confirm
     *
     */// 用户点击了确认按钮(并且有选择配置)
    fun onClickConfirm()

}