package io.agora.scene.dreamFlow.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.agora.scene.dreamFlow.R
import io.agora.scene.widget.utils.StatusBarUtil

/**
 * Bottom full dialog
 *
 * @constructor Create empty Bottom full dialog
 */
open class BottomFullDialog : BottomSheetDialog {
    /**
     * M parent context
     */
    private val mParentContext: Context

    constructor(context: Context) : this(context, R.style.dream_flow_bottom_full_dialog)
    constructor(context: Context, theme: Int) : super(context, theme) {
        mParentContext = context
    }

    /**
     * On start
     *
     */
    override fun onStart() {
        super.onStart()
        val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        val container = findViewById<View>(com.google.android.material.R.id.container)
        bottomSheet?.let {
            it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        behavior.isDraggable = false
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        container?.let { view ->
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val inset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPaddingRelative(inset.left, 0, inset.right, inset.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    /**
     * On attached to window
     *
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        StatusBarUtil.hideStatusBar(window, true)
    }

    /**
     * Get parent context
     *
     */
    fun getParentContext() = mParentContext

    /**
     * On detached from window
     *
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        StatusBarUtil.hideStatusBar(window, false)
    }
}