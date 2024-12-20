package io.agora.scene.show.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import io.agora.rtc2.RtcConnection
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.show.R
import io.agora.scene.show.VideoSetting
import io.agora.scene.show.databinding.ShowSettingPresetDialogBinding

/**
 * Preset dialog
 *
 * @constructor
 *
 * @param context
 * @param deviceScore
 * @param rtcConnection
 */
class PresetDialog constructor(context: Context, deviceScore: Int, rtcConnection: RtcConnection) : BottomFullDialog(context) {

    /**
     * M binding
     */
    private val mBinding by lazy {
        ShowSettingPresetDialogBinding.inflate(
            LayoutInflater.from(
                context
            )
        )
    }

    /**
     * Device score
     */
    private val deviceScore by lazy {
        deviceScore
    }

    /**
     * Rtc connection
     */
    private val rtcConnection by lazy {
        rtcConnection
    }

    init {
        setContentView(mBinding.root)

        if (AgoraApplication.the().isDebugModeOpen) {
            mBinding.deviceSelector.isVisible = true
        }

        mBinding.ivClose.isVisible = false
        mBinding.tvConfirm.setOnClickListener {
            val networkSelectPosition = getGroupSelectedItem(
                mBinding.basicChooseItemGoodNetwork,
                mBinding.basicChooseItemNormalNetwork
            )
            if (networkSelectPosition < 0) {
                ToastDialog(context).apply {
                    dismissDelayShort()
                    showMessage(context.getString(R.string.show_setting_preset_no_choise_tip))
                }
                return@setOnClickListener
            }

            val broadcastStrategySelectPosition = getGroupSelectedItem(
                mBinding.broadcastStrategyItemSmooth,
                mBinding.broadcastStrategyItemClear,
                mBinding.broadcastStrategyItemPureMode
            )
            if (broadcastStrategySelectPosition < 0) {
                ToastDialog(context).apply {
                    dismissDelayShort()
                    showMessage(context.getString(R.string.show_setting_preset_no_choise_tip))
                }
                return@setOnClickListener
            }

            if (AgoraApplication.the().isDebugModeOpen) {
                val showSelectPosition = getGroupSelectedItem(
                    mBinding.showChooseItemLowDevice,
                    mBinding.showChooseItemMediumDevice,
                    mBinding.showChooseItemHighDevice
                )
                onPresetNetworkModeSelected(networkSelectPosition, broadcastStrategySelectPosition, showSelectPosition)
            } else {
                onPresetNetworkModeSelected(networkSelectPosition, broadcastStrategySelectPosition, null)
            }
            dismiss()
        }
        groupItems(
            {}, 0,
            mBinding.basicChooseItemGoodNetwork,
            mBinding.basicChooseItemNormalNetwork
        )

        groupItems(
            {
                when (it) {
                    0 -> mBinding.networkView.isVisible = true
                    1 -> mBinding.networkView.isVisible = false
                    2 -> mBinding.networkView.isVisible = false
                }
            }, 0,
            mBinding.broadcastStrategyItemSmooth,
            mBinding.broadcastStrategyItemClear,
            mBinding.broadcastStrategyItemPureMode
        )

        groupItems(
            {}, -1,
            mBinding.showChooseItemLowDevice,
            mBinding.showChooseItemMediumDevice,
            mBinding.showChooseItemHighDevice
        )

        val deviceLevelStr = if (deviceScore >= 90) {
            context.getString(R.string.show_setting_preset_device_high)
        } else if (deviceScore >= 75) {
            context.getString(R.string.show_setting_preset_device_medium)
        } else {
            mBinding.tvBeautyTips.isVisible = true
            context.getString(R.string.show_setting_preset_device_low)
        }
        mBinding.tvDeviceScore.text = context.getString(R.string.show_video_device_result, deviceLevelStr, deviceScore)
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
     * On preset network mode selected
     *
     * @param networkLevel
     * @param broadcastStrategyLevel
     * @param device
     */
    private fun onPresetNetworkModeSelected(networkLevel: Int, broadcastStrategyLevel: Int, device: Int?){
        if (networkLevel < 0 || broadcastStrategyLevel < 0) {
            return
        }

        val broadcastStrategy = when (broadcastStrategyLevel) {
            0 -> VideoSetting.BroadcastStrategy.Smooth
            1 -> VideoSetting.BroadcastStrategy.Clear
            2 -> VideoSetting.BroadcastStrategy.Pure
            else -> VideoSetting.BroadcastStrategy.Smooth
        }
        val network = if (networkLevel == 0) VideoSetting.NetworkLevel.Good else VideoSetting.NetworkLevel.Normal

        device?.let {
            val deviceLevel = when (it) {
                0 -> {
                    VideoSetting.DeviceLevel.Low
                }
                1 -> {
                    VideoSetting.DeviceLevel.Medium
                }
                2 -> {
                    VideoSetting.DeviceLevel.High
                }
                else -> {
                    VideoSetting.DeviceLevel.High
                }
            }

            VideoSetting.updateBroadcastSetting(deviceLevel, network, broadcastStrategy, isJoinedRoom = false, isByAudience = false, rtcConnection)
            ToastDialog(context).apply {
                dismissDelayShort()
                showMessage(context.getString(R.string.show_setting_preset_done))
            }
            return
        }

        val deviceLevel = if (deviceScore >= 90) {
            VideoSetting.DeviceLevel.High
        } else if (deviceScore >= 75) {
            VideoSetting.DeviceLevel.Medium
        } else {
            VideoSetting.DeviceLevel.Low
        }

        VideoSetting.updateBroadcastSetting(deviceLevel, network, broadcastStrategy, isJoinedRoom = false, isByAudience = false, rtcConnection)
        ToastDialog(context).apply {
            dismissDelayShort()
            showMessage(context.getString(R.string.show_setting_preset_done))
        }
    }
}