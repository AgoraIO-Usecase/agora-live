package io.agora.scene.base.uploader

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import io.agora.scene.base.LogUploader
import io.agora.scene.base.utils.dp


interface FloatCallBack {

    fun show()

    fun hide()

    fun uploadStatus(uploadStatus: UploadStatus, uuid: String)

    fun setOnRepeatUploadListener(listener: () -> Unit)
}

class FloatMonkService : Service(), FloatCallBack {

    private var mWindowManager: WindowManager? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    private var mFloatLayout: OverallUploadLayout? = null

    private val mMainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        OverallLayoutController.registerCallLittleMonk(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : LifecycleEventObserver {

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d("FloatMonkService", "onStateChanged: $event")
                if (event == Lifecycle.Event.ON_RESUME) {
                    OverallLayoutController.showBackHome()
                } else if (event == Lifecycle.Event.ON_STOP) {
                    OverallLayoutController.hideBackHome()
                }
            }
        })
    }


    override fun onDestroy() {
        super.onDestroy()
        OverallLayoutController.registerCallLittleMonk(null)
        removeFloatWindowManager()
    }

    private var mHasShown = false

    private fun showWindow() {

        mWindowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (mFloatLayout == null) {
            mFloatLayout = OverallUploadLayout(this)
            val outMetrics = DisplayMetrics()
            mWindowManager?.defaultDisplay?.getMetrics(outMetrics)

            windowLayoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                format = PixelFormat.RGBA_8888
                flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

                width = ViewGroup.LayoutParams.WRAP_CONTENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                gravity = Gravity.START or Gravity.TOP

                x = outMetrics.widthPixels / 2 - 150.dp.toInt()
                y = outMetrics.heightPixels / 2 - 100.dp.toInt()

                Log.d("FloatMonkService", "width:$width,heigh:$height,x:$x,y:$y")
            }
            mFloatLayout?.layoutParams = windowLayoutParams
        }
//        mFloatLayout?.setOnTouchListener(ItemViewTouchListener(windowLayoutParams, mWindowManager!!))
        mWindowManager?.addView(mFloatLayout, windowLayoutParams)
    }

    private fun removeFloatWindowManager() {
        mFloatLayout?.let { floatRootView ->
            if (floatRootView.windowToken != null) {
                mWindowManager?.removeView(floatRootView)
            }
        }
    }

    override fun show() {
        mMainHandler.post {
            if (OverallLayoutController.commonROMPermissionCheck(this)) {
                if (!mHasShown) {
                    showWindow()
                    mHasShown = true
                }
            }
        }
    }

    override fun hide() {
        mMainHandler.post {
            removeFloatWindowManager()
            mHasShown = false
        }
    }

    override fun uploadStatus(uploadStatus: UploadStatus, uuid: String) {
        mMainHandler.post {
            mFloatLayout?.uploadStatus(uploadStatus, uuid)
        }
    }

    override fun setOnRepeatUploadListener(listener: () -> Unit) {
        mMainHandler.post {
            mFloatLayout?.setOnRepeatUploadListener(listener)
        }
    }
}

