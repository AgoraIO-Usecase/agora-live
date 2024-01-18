package io.agora.voice.common.utils

import android.app.Activity
import android.app.Service
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

private const val RES_NAME_NAV_BAR = "navigationBarBackground"
private val Context.navBarResId
    get() = resources.getIdentifier(
        "navigation_bar_height",
        "dimen", "android"
    )

/**
 * Get the height of the virtual navigation bar. This method must be called after the layout is drawn
 * to get the correct value (can be called in onWindowFocusChanged()).
 * The unit is px.
 */
val Context.navBarHeight: Int
    get() {
        val resourceId = navBarResId
        return if (resourceId != 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

/**
 * Does the phone have a virtual navigation bar
 */
val Context.hasNavBar
    @JvmName("hasNavBar")
    get() = navBarResId != 0

/**
 * If the virtual navigation bar show
 */
val Activity.isNavBarShowed: Boolean
    get()  {
        val viewGroup = window.decorView as ViewGroup? ?: return false
        return (0 until viewGroup.childCount).firstOrNull {
            viewGroup.getChildAt(it).id != View.NO_ID
                    && this.resources.getResourceEntryName(viewGroup.getChildAt(it).id) == RES_NAME_NAV_BAR
        } != null
    }

object NavigationUtils {

    /**
     * Get the height of the virtual navigation bar (NavigationBar), which may not be visible.
     */
    fun getNavigationBarHeight(context: Context): Int {
        var result = 0
        val resources = context.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) result = resources.getDimensionPixelSize(resourceId)
        return result
    }

    /**
     * Get whether the virtual navigation bar (NavigationBar) is visible or not.
     * @return true indicates that the virtual navigation bar is visible, false indicates that the virtual navigation bar is not visible.
     */
    fun hasNavigationBar(context: Context) = when {
        getNavigationBarHeight(context) == 0 -> false
        RomUtils.isHuaweiRom() && isHuaWeiHideNav(context) -> false
//        RomUtils.isMiuiRom() && isMiuiFullScreen(context) -> false
        RomUtils.isVivoRom() && isVivoFullScreen(context) -> false
        else -> isHasNavigationBar(context)
    }

    private fun isHuaWeiHideNav(context: Context) =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Settings.System.getInt(context.contentResolver, "navigationbar_is_min", 0)
        } else {
            Settings.Global.getInt(context.contentResolver, "navigationbar_is_min", 0)
        } != 0

    private fun isMiuiFullScreen(context: Context) =
        Settings.Global.getInt(context.contentResolver, "force_fsg_nav_bar", 0) != 0

    private fun isVivoFullScreen(context: Context) =
        Settings.Secure.getInt(context.contentResolver, "navigation_gesture_on", 0) != 0

    private fun isHasNavigationBar(context: Context): Boolean {
        val windowManager: WindowManager =
            context.getSystemService(Service.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay

        val realDisplayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(realDisplayMetrics)
        }
        val realHeight = realDisplayMetrics.heightPixels
        val realWidth = realDisplayMetrics.widthPixels

        val displayMetrics = DisplayMetrics()
        display.getMetrics(displayMetrics)
        val displayHeight = displayMetrics.heightPixels
        val displayWidth = displayMetrics.widthPixels

//        if (displayHeight > displayWidth) {
//            if (displayHeight + getNavigationBarHeight(context) > realHeight) return false
//        } else {
//            if (displayWidth + getNavigationBarHeight(context) > realWidth) return false
//        }

        return realWidth - displayWidth > 0 || realHeight - displayHeight > 0
    }
}