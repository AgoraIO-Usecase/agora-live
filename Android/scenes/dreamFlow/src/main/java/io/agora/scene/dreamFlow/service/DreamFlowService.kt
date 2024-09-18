package io.agora.scene.dreamFlow.service

import android.util.Log
import io.agora.scene.base.BuildConfig
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject

class DreamFlowService(
    private val domain: String,
    private val region: String,
    private val appId: String,
    private val channelName: String,
) {

    companion object {
        val genaiUid: Int = 999
    }

    private val tag = "DreamFlowServiceAPI"

    data class SettingBean constructor(
        var isEffectOn: Boolean = false,
        var isFaceModeOn: Boolean = false,
        var strength: Float = 0.2f,
        var style: String? = null,
        var effect: String? = null,
        var prmopt: String = ""
    )

    enum class ServiceStatus(val value: String) {
        STARTING("starting"),
        STARTED("started"),
        IDLE("idle");

        companion object {
            private val map = values().associateBy { it.value }

            fun fromString(value: String): ServiceStatus {
                return map[value] ?: IDLE
            }
        }
    }

    var inUid: Int = 0
    var inRole: Int = 0

    var currentSetting: SettingBean = SettingBean()
        private set

    var status: ServiceStatus = ServiceStatus.IDLE
        private set

    private var workerId: String = "b3a21856-88a8-4523-b553-cfaf14af5784"

    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val okHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        }
        builder.build()
    }

    private var listener: IDreamFlowStateListener? = null
    fun setListener(l: IDreamFlowStateListener) {
        listener = l
    }

    fun clean() {
        listener = null
    }

    fun updateStarted() {
        updateProgress(100)
        updateStatus(ServiceStatus.STARTED)
    }

    private fun updateStatus(s: ServiceStatus) {
        if (status != s) {
            status = s
            listener?.onStatusChanged(status)
        }
    }

    private fun updateProgress(p: Int) {
        if (status == ServiceStatus.STARTING) {
            listener?.onLoadingProgressChanged(p)
        }
    }

    fun save(settingBean: SettingBean,
             success: () -> Unit,
             failure: ((Exception?) -> Unit)? = null) {
        scope.launch(Dispatchers.Main) {
            try {
                if (settingBean.isEffectOn) {
                    // turn effect on
                    if (status == ServiceStatus.STARTING ||
                        status == ServiceStatus.STARTED) {
                        update(settingBean)
                        currentSetting = settingBean
                        success.invoke()
                    } else {
                        create(settingBean)
                        currentSetting = settingBean
                        success.invoke()
                        updateStatus(ServiceStatus.STARTING)
                    }
                } else {
                    // turn effect off
                    if (status == ServiceStatus.STARTING ||
                        status == ServiceStatus.STARTED) {
                        delete()
                        currentSetting = settingBean
                        success.invoke()
                        updateStatus(ServiceStatus.IDLE)
                    } else {
                        currentSetting = settingBean
                        success.invoke()
                        updateStatus(ServiceStatus.IDLE)
                    }
                }
            } catch (e: Exception) {
                failure?.invoke(e)
            }
        }
    }

    private fun create(
        settingBean: SettingBean,
        success: () -> Unit,
        failure: ((Exception?) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                create(settingBean)
                success.invoke()
            } catch (e: Exception) {
                failure?.invoke(e)
            }
        }
    }

    fun delete(
        success: () -> Unit,
        failure: ((Exception?) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                delete()
                success.invoke()
            } catch (e: Exception) {
                failure?.invoke(e)
            }
        }
    }

    private fun update(
        settingBean: SettingBean,
        success: () -> Unit,
        failure: ((Exception?) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                update(settingBean)
                success.invoke()
            } catch (e: Exception) {
                failure?.invoke(e)
            }
        }
    }

    fun getStatus(
        success: () -> Unit,
        failure: ((Exception?) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                getStatus()
                success.invoke()
            } catch (e: Exception) {
                failure?.invoke(e)
            }
        }
    }

    private suspend fun create(settingBean: SettingBean) = withContext(Dispatchers.IO) {
        val postBody = JSONObject().apply {
            put("name", "agoralive")
            put("rtcConfigure", JSONObject().apply {
                put("userids", JSONArray().apply {
                    put(JSONObject().apply {
                        put("inUid", inUid)
                        put("inToken", appId)
                        put("inChannelName", channelName)
                        put("inRole", inRole)
                        put("inVideo", "")
                        put("genaiUid", genaiUid)
                        put("genaiToken", "")
                        put("genaiChannelName", "")
                        put("genaiRole", 0)
                        put("genaiVideo", "")
                        put("prompt", settingBean.prmopt)
                    })
                })
            })
            put("prompt", settingBean.prmopt)
            put("style", settingBean.style)
            put("strength", settingBean.strength)
            put("face_mode", settingBean.isFaceModeOn)
        }

        val request = Request.Builder().url("$domain/$region/v1/projects/$appId/stylize").
        addHeader("Content-Type", "application/json").post(postBody.toString().toRequestBody()).build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val body = execute.body ?: throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            Log.d(tag, "create result body: $body")
            val bodyJobj = JSONObject(body.string())
            Log.d(tag, "create result body obj: $bodyJobj")
            if (bodyJobj["code"] != 0 && bodyJobj["code"] != 1) {
                throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            } else {
                val data = bodyJobj["data"] as JSONObject
                workerId = data["id"] as String
                Log.d(tag, "create result succeed: $data workerId: $workerId")
            }
        } else {
            val body = execute.body ?: throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            val obj = JSONObject(body.string())
            val code = obj["code"] as Int
            val msg = obj["message"] as String
            throw RuntimeException("error: $code message: $msg")
        }
    }

    private suspend fun delete() = withContext(Dispatchers.IO) {
        val postBody = JSONObject().apply {
            put("admin", true)
        }
        val request = Request.Builder().url("$domain/$region/v1/projects/$appId/stylize/$workerId").
        addHeader("Content-Type", "application/json").delete(postBody.toString().toRequestBody()).build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val body = execute.body ?: throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            val bodyJobj = JSONObject(body.string())
            Log.d(tag, "delete result body obj: $bodyJobj")
            if (bodyJobj["code"] != 0) {
                throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            } else {
                // update state
                workerId = ""
            }
        } else {
            throw RuntimeException("error: ${execute.code} message: ${execute.message}")
        }
    }

    private suspend fun update(settingBean: SettingBean) = withContext(Dispatchers.IO) {
        val postBody = JSONObject().apply {
            put("name", "agoralive")
            put("rtcConfigure", JSONObject().apply {
                put("userids", JSONArray().apply {
                    put(JSONObject().apply {
                        put("inUid", inUid)
                        put("inToken", appId)
                        put("inChannelName", channelName)
                        put("inRole", inRole)
                        put("inVideo", "")
                        put("genaiUid", genaiUid)
                        put("genaiToken", "")
                        put("genaiChannelName", "")
                        put("genaiRole", 0)
                        put("genaiVideo", "")
                        put("prompt", settingBean.prmopt)
                    })
                })
            })
            put("prompt", settingBean.prmopt)
            put("style", settingBean.style)
            put("strength", settingBean.strength)
            put("face_mode", settingBean.isFaceModeOn)
        }
        val request = Request.Builder().url("$domain/$region/v1/projects/$appId/stylize/$workerId").
        addHeader("Content-Type", "application/json").patch(postBody.toString().toRequestBody()).build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val body = execute.body ?: throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            val bodyJobj = JSONObject(body.string())
            Log.d(tag, "update result body obj: $bodyJobj")
            if (bodyJobj["code"] != 0) {
                throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            } else {
                // do noting
            }
        } else {
            throw RuntimeException("error: ${execute.code} message: ${execute.message}")
        }
    }

    private suspend fun getStatus() = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$domain/$region/v1/projects/$appId/stylize/$workerId").
        addHeader("Content-Type", "application/json").get().build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val body = execute.body ?: throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            val bodyJobj = JSONObject(body.string())
            Log.d(tag, "status result body obj: $bodyJobj")
            if (bodyJobj["code"] != 0) {
                throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            } else {
                // TODO: Analysis status
            }
        } else {
            throw RuntimeException("error: ${execute.code} message: ${execute.message}")
        }
    }
}