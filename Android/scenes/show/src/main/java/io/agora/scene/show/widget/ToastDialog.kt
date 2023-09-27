package io.agora.scene.show.widget

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import io.agora.scene.show.R
import io.agora.scene.show.databinding.ShowWidgetToastLayoutBinding

/**
 * Toast dialog
 *
 * @constructor Create empty Toast dialog
 */
class ToastDialog : Dialog {

    /**
     * M binding
     */
    private val mBinding by lazy {
        ShowWidgetToastLayoutBinding.inflate(LayoutInflater.from(context))
    }

    /**
     * M dismiss run
     */
    private val mDismissRun = Runnable { dismiss() }

    constructor(context: Context) : this(context, R.style.show_toast_dialog)
    constructor(context: Context, themeResId: Int) : super(context, themeResId) {
        setContentView(mBinding.root)
    }

    /**
     * On stop
     *
     */
    override fun onStop() {
        super.onStop()
        mBinding.root.removeCallbacks(mDismissRun)
    }

    /**
     * Dismiss delay short
     *
     */
    fun dismissDelayShort(){
        dismissDelay(1000)
    }

    /**
     * Dismiss delay long
     *
     */
    fun dismissDelayLong(){
        dismissDelay(4000)
    }

    /**
     * Dismiss delay
     *
     * @param duration
     */
    private fun dismissDelay(duration: Long){
        mBinding.root.removeCallbacks(mDismissRun)
        mBinding.root.postDelayed(mDismissRun, duration)
    }

    /**
     * Show tip
     *
     * @param text
     */
    fun showTip(text: String) {
        mBinding.tvMessage.text = text
        setIcon(R.mipmap.show_toast_ic_tip)
        show()
    }

    /**
     * Show error
     *
     * @param text
     */
    fun showError(text: String){
        mBinding.tvMessage.text = text
        setIcon(R.mipmap.show_toast_ic_error)
        show()
    }

    /**
     * Show message
     *
     * @param text
     */
    fun showMessage(text: String){
        mBinding.tvMessage.text = text
        show()
    }

    /**
     * Set icon
     *
     * @param icon
     */
    private fun setIcon(@DrawableRes icon: Int) {
        if(icon != View.NO_ID){
            mBinding.ivIcon.isVisible = true
            mBinding.ivIcon.setImageResource(icon)
        }else{
            mBinding.ivIcon.isVisible = false
        }

    }

}