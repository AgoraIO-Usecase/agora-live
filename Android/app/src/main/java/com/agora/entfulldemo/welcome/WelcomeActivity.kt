package com.agora.entfulldemo.welcome

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.TypedValue.COMPLEX_UNIT_SP
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import com.agora.entfulldemo.R
import com.agora.entfulldemo.databinding.AppActivityWelcomeBinding
import com.agora.entfulldemo.home.MainActivity
import com.agora.entfulldemo.home.constructor.URLStatics
import com.agora.entfulldemo.webview.SSOWebViewActivity
import com.agora.entfulldemo.webview.WebViewActivity
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.manager.SSOUserManager
import io.agora.scene.base.utils.dp
import io.agora.scene.widget.toast.CustomToast
import io.agora.scene.widget.utils.navBarHeight

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
            root.viewTreeObserver.addOnGlobalLayoutListener {
                val rect = android.graphics.Rect()
                binding.root.getWindowVisibleDisplayFrame(rect)
                val screenHeight = binding.root.rootView.height
                val keypadHeight = screenHeight - rect.bottom

                if (keypadHeight > screenHeight * 0.15) { // 键盘弹出
                    val nav = navBarHeight
                    includeLoginWithCode.layoutLoginWithCode.translationY = -keypadHeight.toFloat() +
                            includeLoginWithCode.btnLoginWithSSO.bottom - nav - 20.dp
                } else {
                    includeLoginWithCode.layoutLoginWithCode.translationY = 0f
                }
            }

            btnLoginWithSSO.setOnClickListener { v: View? ->
                if (cbPolicy.isChecked) {
                    startSSOLogin()
                } else {
                    animCheckTip()
                }
            }
            btnLoginWithCode.setOnClickListener { v: View? ->
                if (cbPolicy.isChecked) {
                    includeLoginWithCode.layoutLoginWithCode.visibility = View.VISIBLE
                    val slideUp = TranslateAnimation(
                        0f, 0f, includeLoginWithCode.layoutLoginWithCode.height.toFloat(), 0f
                    ).apply {
                        duration = 300
                    }
                    includeLoginWithCode.layoutLoginWithCode.startAnimation(slideUp)
                } else {
                    animCheckTip()
                }
            }
            includeLoginWithCode.closeLoginWithCode.setOnClickListener {
                hideSoftKeyboard()
                includeLoginWithCode.layoutInviteCode.clearFocus()
                includeLoginWithCode.editInviteCode.clearFocus()
                binding.root.requestFocus()
                val slideDown = TranslateAnimation(
                    0f, 0f, 0f, includeLoginWithCode.layoutLoginWithCode.height.toFloat()
                ).apply {
                    duration = 300
                    setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                        override fun onAnimationEnd(animation: Animation?) {
                            includeLoginWithCode.layoutLoginWithCode.visibility = View.GONE
                        }
                    })
                }
                includeLoginWithCode.layoutLoginWithCode.startAnimation(slideDown)
            }
            includeLoginWithCode.btnLoginWithSSO.setOnClickListener {
                hideSoftKeyboard()
                includeLoginWithCode.layoutInviteCode.clearFocus()
                includeLoginWithCode.editInviteCode.clearFocus()
                binding.root.requestFocus()
                val inviteCode = includeLoginWithCode.editInviteCode.text.toString()
                if (inviteCode.isEmpty()) {
                    CustomToast.show(getString(R.string.app_input_invite_code))
                } else {
                    startInviteCodeLogin(inviteCode)
                }
            }
            includeLoginWithCode.editInviteCode.doAfterTextChanged { s->
                if (!s.isNullOrEmpty()) {
                    includeLoginWithCode.editInviteCode.setTextSize(COMPLEX_UNIT_SP, 16f)
                    includeLoginWithCode.editInviteCode.setTypeface(null, Typeface.BOLD)
                } else {
                    includeLoginWithCode.editInviteCode.setTextSize(COMPLEX_UNIT_SP, 13f)
                    includeLoginWithCode.editInviteCode.setTypeface(null, Typeface.NORMAL)
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
        overridePendingTransition(0,0)
        finish()
    }

    private fun startSSOLogin() {
        val intent = Intent(this, SSOWebViewActivity::class.java)
        intent.putExtra(WebViewActivity.EXTRA_URL, BuildConfig.TOOLBOX_SERVER_HOST + "/v1/sso/login")
        activityResultLauncher.launch(intent)
    }

    private fun startInviteCodeLogin(inviteCode: String) {
        mLoginViewModel.invitationLogin(inviteCode)
    }

    private fun hideSoftKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        val currentFocusView = currentFocus
        if (currentFocusView != null) {
            imm?.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
            currentFocusView.clearFocus()
        }
    }
}