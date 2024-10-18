package io.agora.scene.base.uploader

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast

object OverallLayoutController {

    const val REQUEST_FLOAT_CODE = 1001

    var isReceptionShow = false
        private set(value) {
            field = value
        }

    @JvmStatic
    fun checkOverlayPermission(context: Activity, completion: () -> Unit) {
        if (commonROMPermissionCheck(context)) {
            completion()
        } else {
            context.startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }, REQUEST_FLOAT_CODE)
        }
    }

    @JvmStatic
    fun commonROMPermissionCheck(context: Context): Boolean {
        var result = true
        try {
            val clazz: Class<*> = Settings::class.java
            val canDrawOverlays =
                clazz.getDeclaredMethod("canDrawOverlays", Context::class.java)
            result = canDrawOverlays.invoke(null, context) as Boolean
        } catch (e: Exception) {
            Log.e("ServiceUtils", Log.getStackTraceString(e))
        }
        return result
    }

    private var floatCallback: FloatCallBack? = null

    @JvmStatic
    fun startMonkServer(context: Context) {
        val intent = Intent(context, FloatMonkService::class.java)
        context.startService(intent)
    }

    @JvmStatic
    fun stopMonkServer(context: Context) {
        val intent = Intent(context, FloatMonkService::class.java)
        context.stopService(intent)
    }

    @JvmStatic
    fun registerCallLittleMonk(callLittleMonk: FloatCallBack?) {
        floatCallback = callLittleMonk
    }

    @JvmStatic
    fun show() {
        floatCallback?.show()
        isReceptionShow = true
    }

    @JvmStatic
    fun hide() {
        floatCallback?.hide()
        isReceptionShow = false
    }

    @JvmStatic
    fun showBackHome() {
        if (isReceptionShow) {
            floatCallback?.show()
        }
    }

    @JvmStatic
    fun hideBackHome() {
        if (isReceptionShow) {
            floatCallback?.hide()
        }
    }

    @JvmStatic
    fun uploadStatus(uploadStatus: UploadStatus, uuid: String) {
        floatCallback?.uploadStatus(uploadStatus, uuid)
    }
    @JvmStatic
    fun  setOnRepeatUploadListener(listener: () -> Unit){
        floatCallback?.setOnRepeatUploadListener(listener)
    }
}