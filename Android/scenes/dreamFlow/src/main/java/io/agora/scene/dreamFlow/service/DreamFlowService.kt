package io.agora.scene.dreamFlow.service

import android.util.Log
import com.bumptech.glide.load.HttpException
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.EntLogger
import io.agora.scene.dreamFlow.DreamFlowLogger
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class DreamFlowService constructor(
    private val region: String,
    private val appId: String,
    private val channelName: String,
    private val inUid: Int,
) {

    companion object {
        val genaiUid: Int = 999
    }

    private val tag = "DreamFlowServiceAPI"

    var isEffectOn: Boolean = false

    enum class ServerType {
        Server1,
        Server2,
        Server3,
        Server4;

        fun host(): String {
            return when(this) {
                Server1 -> "http://175.121.93.70:40743"
                Server2 -> "http://175.121.93.70:50249"
                Server3 -> "http://104.15.30.249:49327"
                Server4 -> "http://66.114.112.70:55587"
            }
        }

        fun title(): String {
            return when(this) {
                Server1 -> "Server1"
                Server2 -> "Server2"
                Server3 -> "Server3"
                Server4 -> "Server4"
            }
        }
    }

    enum class Style {
        Toonyou,
        Miyazaki,
        Sexytoon,
        Clay,
        Fantasy;

        fun serverString(): String {
            return when(this) {
                Toonyou -> "toonyou"
                Miyazaki -> "Miyazaki"
                Sexytoon -> "sexytoon"
                Clay -> "Clay"
                Fantasy -> "Fantasy"
            }
        }
    }

    data class SettingBean constructor(
        var isFaceModeOn: Boolean = false,
        var strength: Float = 0.2f,
        var superFrame: Int = 1,
        var style: Style? = Style.Toonyou,
        var prompt: String = ""
    )

    enum class ServiceStatus(val value: String) {
        STARTING("starting"),
        START_SUCCESS("start_success"),
        STOPPED("stopped");

        companion object {
            private val map = values().associateBy { it.value }

            fun fromString(value: String): ServiceStatus {
                return map[value] ?: STOPPED
            }
        }
    }

    var server: ServerType = ServerType.Server2

    var selectedPreset: Int = 0

    var currentSetting: SettingBean? = null

    var status: ServiceStatus = ServiceStatus.STOPPED
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
        endStateTimer()
        listener = null
    }

    private var job: Job? = null
    private fun startStateTimer() {
        job?.cancel()
        job = scope.launch(Dispatchers.Main) {
            repeat(100) {
                delay(3000)
                try {
                    getStatus()
                } catch (e: Exception) {
                    listener?.onOccurError(e)
                }
            }
        }
    }

    private fun endStateTimer() {
        job?.cancel()
        job = null
    }

    fun updateStarted() {
        updateProgress(100)
        updateStatus(ServiceStatus.START_SUCCESS)
    }

    private fun updateStatus(s: ServiceStatus) {
        if (status != s) {
            status = s
            when (status) {
                ServiceStatus.STARTING -> {
                    startStateTimer()
                }
                ServiceStatus.START_SUCCESS,
                ServiceStatus.STOPPED -> {
                    endStateTimer()
                }
            }
            scope.launch(Dispatchers.Main) {
                listener?.onStatusChanged(status)
            }
        }
    }

    private fun updateProgress(p: Int) {
        if (status == ServiceStatus.STARTING) {
            listener?.onLoadingProgressChanged(p)
        }
    }

    fun save(effectOn: Boolean,
             settingBean: SettingBean,
             success: () -> Unit,
             failure: ((Exception?) -> Unit)? = null) {
        scope.launch(Dispatchers.Main) {
            isEffectOn = effectOn
            try {
                if (isEffectOn) {
                    // turn effect on
                    if (status == ServiceStatus.STARTING ||
                        status == ServiceStatus.START_SUCCESS) {
                        currentSetting = settingBean
                        update(settingBean)
                        success.invoke()
                    } else {
                        currentSetting = settingBean
                        val code = create(settingBean)
                        success.invoke()
                        if (code == 0) {
                            updateStatus(ServiceStatus.STARTING)
                        } else {
                            updateStatus(ServiceStatus.START_SUCCESS)
                        }
                    }
                } else {
                    // turn effect off
                    delete(false)
                    currentSetting = settingBean
                    success.invoke()
                    updateStatus(ServiceStatus.STOPPED)
//                    if (status == ServiceStatus.STARTING ||
//                        status == ServiceStatus.STARTED) {
//                        delete(false)
//                        currentSetting = settingBean
//                        success.invoke()
//                        updateStatus(ServiceStatus.IDLE)
//                    } else {
//                        currentSetting = settingBean
//                        success.invoke()
//                        updateStatus(ServiceStatus.IDLE)
//                    }
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
                delete(true)
                success.invoke()
            } catch (e: Exception) {
                failure?.invoke(e)
            }
        }
    }

    private suspend fun create(settingBean: SettingBean) = withContext(Dispatchers.IO) {
        val postBody = JSONObject().apply {
            put("name", "agoralive")
            put("style", settingBean.style?.serverString())
            put("strength", settingBean.strength)
            put("faceMode", settingBean.isFaceModeOn)
            put("superFrameFactor", settingBean.superFrame)
            put("rtcConfigure", JSONObject().apply {
                put("userids", JSONArray().apply {
                    put(JSONObject().apply {
                        put("inUid", inUid)
                        put("inChannelName", channelName)
                        put("genaiUid", genaiUid)
                        put("genaiToken", "")
                        put("genaiVideoWidth", 720)
                        put("genaiVideoHeight", 1280)
                        put("prompt", settingBean.prompt)
                    })
                })
            })
        }
        DreamFlowLogger.d(tag, "create, $postBody")
        val request = Request.Builder().url("${server.host()}/$region/v1/projects/$appId/stylize").
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
                bodyJobj["code"]
            }
        } else {
            val body = execute.body ?: throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            val obj = JSONObject(body.string())
            val code = obj["code"] as Int
            val msg = obj["message"] as String
            throw RuntimeException("error: $code message: $msg")
        }
    }

    private suspend fun delete(admin: Boolean) = withContext(Dispatchers.IO) {
        DreamFlowLogger.d(tag, "delete, $workerId")
        val postBody = JSONObject().apply {
            put("admin", admin)
        }
        val request = Request.Builder().url("${server.host()}/$region/v1/projects/$appId/stylize/$workerId").
        addHeader("Content-Type", "application/json").delete(postBody.toString().toRequestBody()).build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val body = execute.body ?: throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            val bodyJobj = JSONObject(body.string())
            Log.d(tag, "delete result body obj: $bodyJobj")
            val code = bodyJobj["code"]
            if (code != 0) {
                throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            } else {
                // update state
                workerId = ""
                code
            }
        } else {
            throw RuntimeException("error: ${execute.code} message: ${execute.message}")
        }
    }

    private suspend fun update(settingBean: SettingBean) = withContext(Dispatchers.IO) {
        val postBody = JSONObject().apply {
            put("name", "agoralive")
            put("style", settingBean.style?.serverString())
            put("strength", settingBean.strength)
            put("faceMode", settingBean.isFaceModeOn)
            put("superFrameFactor", settingBean.superFrame)
            put("rtcConfigure", JSONObject().apply {
                put("userids", JSONArray().apply {
                    put(JSONObject().apply {
                        put("inUid", inUid)
                        put("inChannelName", channelName)
                        put("genaiUid", genaiUid)
                        put("genaiToken", "")
                        put("genaiVideoWidth", 720)
                        put("genaiVideoHeight", 1280)
                        put("prompt", settingBean.prompt)
                    })
                })
            })
        }
        DreamFlowLogger.d(tag, "update, worker: $workerId, body: $postBody")
        val request = Request.Builder().url("${server.host()}/$region/v1/projects/$appId/stylize/$workerId").
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
        DreamFlowLogger.d(tag, "getStatus, worker: $workerId")
        val request = Request.Builder().url("${server.host()}/$region/v1/projects/$appId/stylize/$workerId").
        addHeader("Content-Type", "application/json").get().build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val body = execute.body ?: throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            val bodyJobj = JSONObject(body.string())
            val code = bodyJobj["code"]
            Log.d(tag, "status result body obj: $bodyJobj")
            if (code == 0) {
                val data = bodyJobj["data"] as JSONObject
                val workerInfo = data.optJSONObject("workerInfo")
                val stylize_ai = workerInfo?.optJSONObject("stylize_ai")
                val stateStr = stylize_ai?.optString("state")
                if (stateStr != null) {
                    val status = ServiceStatus.fromString(stateStr)
                    updateStatus(status)
                }
                code
            } else if (code == 10003) {
                updateStatus(ServiceStatus.STOPPED)
                val message = bodyJobj["message"]
                throw RuntimeException("error: $code $message")
            } else {
                throw RuntimeException("error: ${execute.code} message: ${execute.message}")
            }
        } else {
            throw RuntimeException("error: ${execute.code} message: ${execute.message}")
        }
    }
}