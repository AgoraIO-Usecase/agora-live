package com.agora.entfulldemo.home.mine

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import com.agora.entfulldemo.R
import com.agora.entfulldemo.databinding.AppActivityDebugModeBinding
import com.agora.entfulldemo.welcome.WelcomeActivity
import io.agora.scene.base.ServerConfig
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.component.OnButtonClickListener
import io.agora.scene.base.manager.SSOUserManager
import io.agora.scene.widget.dialog.CommonDialog
import io.agora.scene.widget.toast.CustomToast
import io.agora.scene.widget.utils.UiUtils

class AppDebugActivity : BaseViewBindingActivity<AppActivityDebugModeBinding>() {

    companion object {

        @JvmStatic
        fun startActivity(context: Activity) {
            val intent = Intent(context, AppDebugActivity::class.java)
            context.startActivity(intent)
        }
    }

    private var tempEnvRelease = true

    private var debugModeDialog: CommonDialog? = null
    override fun getViewBinding(inflater: LayoutInflater): AppActivityDebugModeBinding {
        return AppActivityDebugModeBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setOnApplyWindowInsetsListener(binding.root)
        tempEnvRelease = ServerConfig.envRelease
        if (tempEnvRelease) {
            binding.rgSwitchEnv.check(R.id.rbEnvRelease)
        } else {
            binding.rgSwitchEnv.check(R.id.rbEnvDev)
        }
        enableEnvSwitch(false)
        binding.rgSwitchEnv.setOnCheckedChangeListener { group, checkedId ->
            tempEnvRelease = checkedId == R.id.rbEnvRelease
            enableEnvSwitch(tempEnvRelease != ServerConfig.envRelease)
        }
        binding.btnEnvSwitch.setOnClickListener {
            if (UiUtils.isFastClick(1000)) {
                return@setOnClickListener
            }
            ServerConfig.envRelease = tempEnvRelease
            SSOUserManager.logout()
            finishAffinity()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        binding.btnExitDebug.setOnClickListener {
            if (UiUtils.isFastClick(1000)) {
                return@setOnClickListener
            }
            showDebugModeCloseDialog()
        }
    }

    private fun enableEnvSwitch(enable: Boolean) {
        binding.btnEnvSwitch.isEnabled = enable
        binding.btnEnvSwitch.alpha = if (enable) 1.0f else 0.6f
    }

    private fun showDebugModeCloseDialog() {
        if (debugModeDialog == null) {
            debugModeDialog = CommonDialog(this)
            debugModeDialog?.setDialogTitle(getString(R.string.app_exit_debug_title))
            debugModeDialog?.setDescText(getString(R.string.app_exit_debug_tip))
            debugModeDialog?.setDialogBtnText(
                getString(io.agora.scene.base.R.string.cancel),
                getString(R.string.app_exit)
            )
            debugModeDialog?.onButtonClickListener = object : OnButtonClickListener {
                override fun onLeftButtonClick() {}
                override fun onRightButtonClick() {
                    AgoraApplication.the().enableDebugMode(false)
                    CustomToast.show(R.string.app_debug_off)
                    finish()
                }
            }
        }
        debugModeDialog?.show()
    }
}