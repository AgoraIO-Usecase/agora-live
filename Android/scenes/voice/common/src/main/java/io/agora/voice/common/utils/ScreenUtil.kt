package io.agora.voice.common.utils

import android.app.Activity
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.content.res.Configuration
import android.graphics.Point
import android.view.WindowManager

/**
 * Screen width
 */
val Context.screenWidth: Int
    get() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return point.x
    }

/**
 * Screen height
 */
val Context.screenHeight: Int
    get() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return point.y
    }

/**
 * Is full screen
 */
var Activity.isFullScreen: Boolean
    get() {
        val flag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        return (window.attributes.flags and flag) == flag
    }
    set(value) {
        if (value) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

/**
 * Is portrait
 */
val Activity.isPortrait: Boolean
    get() = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT


/**
 * Is landscape
 */
val Activity.isLandscape: Boolean
    get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
