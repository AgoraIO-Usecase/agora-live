package com.agora.entfulldemo.welcome

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.webkit.CookieManager
import android.widget.CompoundButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.agora.entfulldemo.R
import com.agora.entfulldemo.databinding.AppActivityWelcomeBinding
import com.agora.entfulldemo.home.MainActivity
import com.agora.entfulldemo.home.constructor.URLStatics
import com.agora.entfulldemo.webview.SSOWebViewActivity
import com.agora.entfulldemo.webview.WebViewActivity
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.manager.SSOUserManager
import io.agora.scene.widget.toast.CustomToast

/**
 * The type Welcome activity.
 */
class WelcomeActivity : BaseViewBindingActivity<AppActivityWelcomeBinding>() {

    private val mLoginViewModel: LoginViewModel by viewModels()

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    override fun getViewBinding(inflater: LayoutInflater): AppActivityWelcomeBinding {
        return AppActivityWelcomeBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        val policyText = getString(R.string.app_policy_accept)
        val split = policyText.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val ss = SpannableString(split[0] + split[1])
        val policyClickableSpan: ClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                WebViewActivity.launch(this@WelcomeActivity, URLStatics.termsOfServiceURL)
            }
        }
        ss.setSpan(
            policyClickableSpan,
            split[0].length,
            split[0].length + split[1].length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        ss.setSpan(
            ForegroundColorSpan(Color.parseColor("#32AEFF")),
            split[0].length,
            split[0].length + split[1].length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding?.apply {
            tvPolicy.text = ss
            tvPolicy.movementMethod = LinkMovementMethod.getInstance()
        }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    val token = data?.getStringExtra("token")
                    if (token != null) {
                        SSOUserManager.saveToken(token)
                        gotoHomeActivity()
                    } else {
                        CustomToast.show("Login error")
                    }
                }
            }

        mLoginViewModel.checkLogin()
    }

    override fun initListener() {
        binding?.apply {
            btnEnterRoom.setOnClickListener { v: View? ->
                if (cbPolicy.isChecked) {
                    startSSOLogin()
                } else {
                    animCheckTip()
                }
            }
            cbPolicy.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                if (isChecked && tvCheckTip.visibility == View.VISIBLE) {
                    binding.tvCheckTip.visibility = View.GONE
                }
            }
        }

        mLoginViewModel.tokenLiveData.observe(this) { token ->
            if (!token.isNullOrEmpty()) {
                gotoHomeActivity()
            }
        }
    }

    override fun isBlackDarkStatus(): Boolean = false

    private fun animCheckTip() {
        binding?.apply {
            tvCheckTip.visibility = View.VISIBLE
            val animation = TranslateAnimation(
                -10f, 10f, 0f, 0f
            )
            animation.duration = 60
            animation.repeatCount = 4
            animation.repeatMode = Animation.REVERSE
            tvCheckTip.clearAnimation()
            tvCheckTip.startAnimation(animation)
        }
    }

    private fun gotoHomeActivity() {
        startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
        finish()
    }

    private fun startSSOLogin() {
//        clearCookies()

        val intent = Intent(this, SSOWebViewActivity::class.java)
        intent.putExtra(WebViewActivity.EXTRA_URL, BuildConfig.TOOLBOX_SERVER_HOST + "/v1/sso/login")
        activityResultLauncher.launch(intent)
    }

    private fun clearCookies() {
        // Get the CookieManager instance
        val cookieManager = CookieManager.getInstance()
        // Clear all cookies
        cookieManager.removeAllCookies(null)
        cookieManager.flush() // Ensure cookies are cleared immediately
    }
}