package io.agora.scene.voice.ui.widget.primary

import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.agora.voice.common.utils.*

class KeyboardStatusWatcher(
    private val activity: FragmentActivity,
    private val lifecycleOwner: LifecycleOwner,
    private val listener: (isKeyboardShowed: Boolean, keyboardHeight: Int) -> Unit
) : PopupWindow(activity), ViewTreeObserver.OnGlobalLayoutListener {

    private val rootView by lazy { activity.window.decorView.rootView }

    private val TAG = "Keyboard-Tag"

    private var visibleHeight = 0

    var isKeyboardShowed = false
        private set

    var keyboardHeight = 0
        private set

    private val popupView by lazy {
        FrameLayout(activity).also {
            it.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            it.viewTreeObserver.addOnGlobalLayoutListener(this)
        }
    }

    init {
        contentView = popupView
        softInputMode =
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        inputMethodMode = INPUT_METHOD_NEEDED
        width = 0
        height = ViewGroup.LayoutParams.MATCH_PARENT
        setBackgroundDrawable(ColorDrawable(0))
        rootView.post { showAtLocation(rootView, Gravity.NO_GRAVITY, 0, 0) }

        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                super.onDestroy(owner)
                dismiss()
            }
        })
    }

    override fun onGlobalLayout() {
        val rect = Rect()
        popupView.getWindowVisibleDisplayFrame(rect)
        if (visibleHeight == (rect.bottom - rect.top)) {
            return
        } else {
            visibleHeight = (rect.bottom - rect.top)
        }
        val heightDiff = rootView.height - visibleHeight
        if (heightDiff > activity.screenHeight / 3) {
            isKeyboardShowed = true
            keyboardHeight =
                if (activity.isFullScreen) heightDiff else heightDiff - StatusBarCompat.getStatusBarHeight(activity)
//            Log.d(TAG, "navBarHeight = ${activity.navBarHeight}   ")
//            Log.d(TAG, "hasNavBar = ${activity.hasNavBar}   ")
//            Log.d(TAG, "isNavBarShowed = ${NavigationUtils.hasNavigationBar(activity)}   ")
//            Log.d(TAG, "isPortrait = ${activity.isPortrait}   ")
            if (activity.hasNavBar && NavigationUtils.hasNavigationBar(activity) && activity.isPortrait) {
                keyboardHeight -= activity.navBarHeight
                LogTools.d(TAG, "keyboardHeight = $keyboardHeight   ")
            }
        } else {
            isKeyboardShowed = false
            keyboardHeight = 0
        }
        listener.invoke(isKeyboardShowed, keyboardHeight)
    }
}
