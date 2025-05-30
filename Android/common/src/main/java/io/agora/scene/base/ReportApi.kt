package io.agora.scene.base

import android.os.Build
import io.agora.scene.base.api.HttpLogger
import io.agora.scene.base.api.SecureOkHttpClient
import io.agora.scene.base.utils.UUIDUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * @author create by zhangwei03
 *
 */
object ReportApi {

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val okHttpClient by lazy {
        val builder = SecureOkHttpClient.create()
            .addInterceptor(HttpLogger())

        builder.build()
    }

    /**
     * Report enter
     *
     * @param sceneName
     * @param success
     * @param failure
     * @receiver
     */
    @JvmStatic
    fun reportEnter(sceneName: String, success: ((Boolean) -> Unit), failure: ((Exception?) -> Unit)? = null) {
        report("entryScene", sceneName, success, failure)
    }

    private fun report(
        eventName: String, sceneName: String,
        success: ((Boolean) -> Unit)? = null, failure: ((Exception?) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                success?.invoke(fetchReport(eventName, sceneName))
            } catch (e: Exception) {
                failure?.invoke(e)
            }
        }
    }

    private suspend fun fetchReport(eventName: String, sceneName: String) = withContext(Dispatchers.IO) {

        val postBody = JSONObject()
        val ptsObject = JSONObject().apply {
            put("m", "event")
            put("ls", JSONObject().apply {
                put("name", eventName)
                put("project", sceneName)
                put("version", BuildConfig.APP_VERSION_NAME)
                put("platform", "Android")
                put("model", Build.MODEL)
            })
            put("vs", JSONObject().apply {
                put("count", 1)
            })
        }
        val ptsArray = JSONArray().apply {
            put(ptsObject)
        }

        val src = "agora_live_demo"
        val ts = System.currentTimeMillis()
        postBody.put("pts", ptsArray)
        postBody.put("src", src) // agora live src
        postBody.put("ts", ts)
        postBody.put("sign", UUIDUtil.uuid("src=$src&ts=$ts").lowercase())

        val request = Request.Builder()
            .url("https://report-ad.agoralab.co/v1/report")
            .addHeader("Content-Type", "application/json")
            .post(postBody.toString().toRequestBody())
            .build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val body = execute.body
                ?: throw RuntimeException("Fetch report error: httpCode=${execute.code}, httpMsg=${execute.message}, body is null")
            val bodyJsonObj = JSONObject(body.string())
            if (bodyJsonObj["code"] != 0) {
                throw RuntimeException("Fetch report error: httpCode=${execute.code}, httpMsg=${execute.message}, reqCode=${bodyJsonObj["code"]}, reqMsg=${bodyJsonObj["message"]},")
            } else {
                CommonBaseLogger.d("ReportApi", "${bodyJsonObj["data"] as JSONObject}")
                (bodyJsonObj["data"] as JSONObject)["ok"] as Boolean
            }
        } else {
            throw RuntimeException("Fetch report error: httpCode=${execute.code}, httpMsg=${execute.message}")
        }
    }
}