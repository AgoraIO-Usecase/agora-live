package io.agora.scene.widget.dialog

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import io.agora.scene.base.R
import io.agora.scene.base.component.BaseDialog
import io.agora.scene.base.databinding.DialogTopFunctionBinding
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.widget.utils.StatusBarUtil

/**
 * top function dialog.
 */
class TopFunctionDialog constructor(context: Context, val showReportUser: Boolean = true) :
    BaseDialog<DialogTopFunctionBinding>
        (context) {
    override fun getViewBinding(inflater: LayoutInflater): DialogTopFunctionBinding {
        return DialogTopFunctionBinding.inflate(inflater)
    }

    /**
     * report content
     */
    var reportContentCallback: (() -> Unit)? = null

    /**
     * report user
     */
    var reportUserCallback: (() -> Unit)? = null

    override fun setContentView(view: View) {
        super.setContentView(view)
        window?.let { window ->
            StatusBarUtil.hideStatusBar(window, 0xF2151325.toInt(), true)
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setDimAmount(0f)
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            )
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            window.attributes.apply {
                val lp = WindowManager.LayoutParams()
                lp.copyFrom(window.attributes)
                lp.width = WindowManager.LayoutParams.MATCH_PARENT
                window.attributes = lp
            }
        }
        setCanceledOnTouchOutside(true)
    }

    override fun initView() {

        binding.layoutReportContent.setOnClickListener {
            ToastUtils.showToast(context.getString(R.string.common_report_content_tips))
            reportContentCallback?.invoke()
            dismiss()
        }

        binding.layoutReportUser.setOnClickListener {
            ToastUtils.showToast(context.getString(R.string.common_report_user_tips))
            reportUserCallback?.invoke()
            dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.layoutReportUser.visibility = if (showReportUser) View.VISIBLE else View.GONE
    }

    override fun setGravity() {
        window?.attributes?.gravity = Gravity.TOP
    }
}