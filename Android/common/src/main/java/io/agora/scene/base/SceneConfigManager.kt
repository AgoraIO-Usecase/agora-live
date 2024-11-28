package io.agora.scene.base

import android.util.Log
import io.agora.scene.base.api.SecureOkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject

object SceneConfigManager {

    @JvmStatic
    var chatExpireTime = 1200
        private set

    @JvmStatic
    var ktvExpireTime = 1200
        private set

    @JvmStatic
    var showExpireTime = 600
        private set

    @JvmStatic
    var showPkExpireTime = 120
        private set

    @JvmStatic
    var ecommerce = 600
        private set

    @JvmStatic
    var logUpload = false
        private set

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val okHttpClient by lazy {
        val builder = SecureOkHttpClient.create()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }
        builder.build()
    }

    @JvmStatic
    fun fetchSceneConfig(
        success: (() -> Unit)? = null,
        failure: ((Exception?) -> Unit)? = null,
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                val result = fetch()
                if (result.has("chat")) {
                    chatExpireTime = result["chat"] as Int
                }
                if (result.has("ktv")) {
                    ktvExpireTime = result["ktv"] as Int
                }
                if (result.has("showNew")) {
                    showExpireTime = result["showNew"] as Int
                }
                if (result.has("showpk")) {
                    showPkExpireTime = result["showpk"] as Int
                }
                if (result.has("ecommerce")) {
                    ecommerce = result["ecommerce"] as Int
                }
                if (result.has("logUpload")) {
                    logUpload = result["logUpload"] as Boolean
                }
                success?.invoke()
            } catch (e: Exception) {
                Log.d("SceneConfigManager", "${e.message}")
                failure?.invoke(e)
            }
        }
    }

    private suspend fun fetch() = withContext(Dispatchers.IO) {

        val request = Request.Builder().url("${ServerConfig.toolBoxUrl}/v1/configs/scene?appId=${BuildConfig.AGORA_APP_ID}"
        ).addHeader("Content-Type", "application/json").get().build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val body = execute.body
                ?: throw RuntimeException("fetchSceneConfig error: httpCode=${execute.code}, httpMsg=${execute.message}, body is null")
            val bodyJobj = JSONObject(body.string())
            Log.d("SceneConfigManager", "$bodyJobj")
            if (bodyJobj["code"] != 0) {
                throw RuntimeException("fetchSceneConfig error: httpCode=${execute.code}, httpMsg=${execute.message}, reqCode=${bodyJobj["code"]}")
            } else {
                bodyJobj["data"] as JSONObject
            }
        } else {
            throw RuntimeException("fetchSceneAliveTime error: httpCode=${execute.code}, httpMsg=${execute.message}")
        }
    }
}