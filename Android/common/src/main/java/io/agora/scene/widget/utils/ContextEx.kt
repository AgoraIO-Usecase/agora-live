package io.agora.scene.widget.utils

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.util.Log
import android.view.WindowManager

private const val TAG = "ContextEx"

private val Context.navBarResId
    get() = resources.getIdentifier("navigation_bar_height", "dimen", "android")

private fun checkNavigationBarShow(context: Context): Boolean {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
    }

    val appUsableSize = Point()
    val realScreenSize = Point()

    display?.getSize(appUsableSize)
    display?.getRealSize(realScreenSize)

    Log.d(TAG, "checkNavigationBar ${realScreenSize.y} ${appUsableSize.y}")
    return realScreenSize.y != appUsableSize.y
}


val Context.navBarHeight: Int
    get() {
        val resourceId = navBarResId
        return if (resourceId != 0 && checkNavigationBarShow(this)) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }