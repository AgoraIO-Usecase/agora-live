package io.agora.scene.eCommerce.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.agora.scene.eCommerce.R
import io.agora.scene.eCommerce.databinding.CommerceWidgetBottomDarkDialogBinding
import io.agora.scene.widget.utils.StatusBarUtil


/**
 * Bottom dark dialog
 *
 * @constructor Create empty Bottom dark dialog
 */
open class BottomDarkDialog : BottomSheetDialog {
    /**
     * M binding
     */
    private val mBinding by lazy { CommerceWidgetBottomDarkDialogBinding.inflate(LayoutInflater.from(context)) }

    constructor(context: Context) : this(context, R.style.commerce_bottom_dialog)
    constructor(context: Context, theme: Int) : super(context, theme){
        super.setContentView(mBinding.root)
    }

    /**
     * On start
     *
     */
    override fun onStart() {
        super.onStart()
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    /**
     * On attached to window
     *
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        StatusBarUtil.hideStatusBar(window, false)
    }


    /**
     * Set content view
     *
     * @param view
     */
    override fun setContentView(view: View) {
        throw RuntimeException("setContentView is not allow. Please use setTopView or setBottomView")
    }

    /**
     * Set top view
     *
     * @param view
     */
    protected fun setTopView(view: View){
        mBinding.topLayout.addView(view)
    }

    /**
     * Set bottom view
     *
     * @param view
     */
    protected fun setBottomView(view: View){
        mBinding.bottomLayout.addView(view)
    }



}