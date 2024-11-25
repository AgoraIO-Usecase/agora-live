package com.agora.entfulldemo.home.mine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import com.agora.entfulldemo.R
import com.agora.entfulldemo.databinding.AppActivityInviteBinding
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.manager.SSOUserManager
import io.agora.scene.base.utils.ToastUtils

/**
 * Invite code activity
 *
 * @constructor Create empty Invite code activity
 */
class InviteCodeActivity : BaseViewBindingActivity<AppActivityInviteBinding>() {

    override fun getViewBinding(inflater: LayoutInflater): AppActivityInviteBinding {
        return AppActivityInviteBinding.inflate(inflater)
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding?.apply {
            titleView.setLeftClick {
                finish()
            }
            if (SSOUserManager.isGenerateCode()) {
                tvInviteCode.text = SSOUserManager.getUser().invitationCode
                tvGenerateCodeTips.text = getString(R.string.app_share_invite_code)
                btnGenerateCode.text = getString(R.string.app_copy)
            } else {
                tvInviteCode.text = "******"
                tvGenerateCodeTips.text = getString(R.string.app_generate_a_code_to_friends)
                btnGenerateCode.text = getString(R.string.app_generate_a_code)
            }
            btnGenerateCode.setOnClickListener {
                if (SSOUserManager.isGenerateCode()) {
                    copyToClipboard(tvInviteCode.text.toString())
                } else {
                    tvInviteCode.text = SSOUserManager.getUser().invitationCode
                    tvGenerateCodeTips.text = getString(R.string.app_share_invite_code)
                    btnGenerateCode.text = getString(R.string.app_copy)

                    SSOUserManager.setGenerateCode(true)
                }
            }
        }

    }

    private fun copyToClipboard(text: String) {
        val cm: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(null, text))
        ToastUtils.showToast(R.string.app_copy_succeed)
    }

    override fun initListener() {
        binding?.apply {
        }
    }

    override fun isBlackDarkStatus(): Boolean = false
}