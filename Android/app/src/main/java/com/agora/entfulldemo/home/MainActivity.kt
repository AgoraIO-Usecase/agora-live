package com.agora.entfulldemo.home

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.agora.entfulldemo.R
import com.agora.entfulldemo.databinding.AppActivityMainBinding
import com.agora.entfulldemo.welcome.LoginViewModel
import com.agora.entfulldemo.welcome.WelcomeActivity
import io.agora.scene.base.SceneConfigManager.fetchSceneConfig
import io.agora.scene.base.SceneConfigManager.logUpload
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.manager.SSOUserManager
import io.agora.scene.base.uploader.OverallLayoutController
import io.agora.scene.base.uploader.OverallLayoutController.checkOverlayPermission
import io.agora.scene.base.uploader.OverallLayoutController.startMonkServer
import io.agora.scene.widget.dialog.PermissionLeakDialog
import io.agora.scene.widget.toast.CustomToast

/**
 * Main Activity.
 */
class MainActivity : BaseViewBindingActivity<AppActivityMainBinding?>() {
    private var navController: NavController? = null

    private val mLoginViewModel: LoginViewModel by viewModels()

    override fun getViewBinding(inflater: LayoutInflater): AppActivityMainBinding {
        return AppActivityMainBinding.inflate(inflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fetchSceneConfig(
            success = {
                if (logUpload) {
                    checkPermissionAndStartMonkServer()
                }
            })

        // get user info
        fetchUserInfo(SSOUserManager.getToken())

        mLoginViewModel.userInfoLiveData.observe(this) { userInfo ->
            if (userInfo != null) {
                // nothing
            } else {
                val intent = Intent(this@MainActivity, WelcomeActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }

        TokenGenerator.tokenErrorCompletion = {
            if (it!=null){
                SSOUserManager.logout()
                val intent = Intent(this@MainActivity, WelcomeActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
    }

    private fun checkPermissionAndStartMonkServer() {
        checkOverlayPermission(this) {
            startMonkServer(this@MainActivity)
            null
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        navController = this.findNavController(R.id.nav_host_fragment_activity_main)
        binding?.navView?.setupWithNavController(navController!!)
    }

    override fun isCanExit(): Boolean = true

    override fun initListener() {
        binding?.navView?.itemIconTintList = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == OverallLayoutController.REQUEST_FLOAT_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startMonkServer(this@MainActivity)
            } else {
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onPermissionDined(permission: String) {
        super.onPermissionDined(permission)
        PermissionLeakDialog(this).show(permission, null) { launchAppSetting(permission) }
    }

    private fun fetchUserInfo(token: String) {
        mLoginViewModel.getUserInfoByToken(token)
    }
}