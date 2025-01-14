package io.agora.scene.eCommerce.service

import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.api.HttpLogger
import io.agora.scene.base.api.SecureOkHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

/**
 * Cloud player service
 *
 * @constructor Create empty Cloud player service
 */
class CloudPlayerService {
    /**
     * Scope
     */
    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    /**
     * Tag
     */
    private val tag = "CloudPlayerService"

    /**
     * Base url
     */
    private val baseUrl = "${BuildConfig.TOOLBOX_SERVER_HOST}/v1/"

    /**
     * Ok http client
     */
    private val okHttpClient by lazy {
        val builder = SecureOkHttpClient.create()
            .addInterceptor(HttpLogger())
        builder.build()
    }

    /**
     * Heart beat timer map
     */
    private val heartBeatTimerMap = mutableMapOf<String, CountDownTimer>()

    /**
     * Start cloud player
     *
     * @param channelName
     * @param uid
     * @param robotUid
     * @param streamUrl
     * @param streamRegion
     * @param success
     * @param failure
     * @receiver
     * @receiver
     */
    fun startCloudPlayer(
        channelName: String,
        uid: String,
        robotUid: Int,
        streamUrl: String,
        streamRegion: String, // cn, ap, na, eu
        success: () -> Unit,
        failure: (Exception) -> Unit
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    val traceId = UUID.randomUUID().toString().replace("-","")
                    reqStartCloudPlayer(channelName, uid, robotUid, streamUrl, streamRegion, traceId)
                }
                success.invoke()
            } catch (ex: Exception) {
                failure.invoke(ex)
                Log.e(tag, "start cloud player failure $ex")
            }
        }
    }

    /**
     * Start heart beat
     *
     * @param channelName
     * @param uid
     * @param failure
     */
    fun startHeartBeat(
        channelName: String,
        uid: String,
        failure: ((Exception) -> Unit)? = null
    ) {
        if (heartBeatTimerMap[channelName] != null) return
        heartBeatTimerMap[channelName] = object : CountDownTimer(Long.MAX_VALUE, 30 * 1000) {
            override fun onTick(millisUntilFinished: Long) {
                reqHeatBeatAsync(channelName, uid, failure)
            }

            override fun onFinish() {
                // do nothing
            }
        }.start()
    }

    /**
     * Stop heart beat
     *
     * @param channelName
     */
    fun stopHeartBeat(channelName: String) {
        val countDownTimer = heartBeatTimerMap.remove(channelName) ?: return
        countDownTimer.cancel()
        Log.d(tag, "cloud player stop heartbeat $channelName")
    }

    /**
     * Req heat beat async
     *
     * @param channelName
     * @param uid
     * @param failure
     */
    private fun reqHeatBeatAsync(
        channelName: String,
        uid: String,
        failure: ((Exception) -> Unit)?
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) {
                    val traceId = UUID.randomUUID().toString().replace("-","")
                    reqHeatBeat(channelName, uid, traceId)
                }
            } catch (ex: Exception) {
                failure?.invoke(ex)
                Log.e(tag, "cloud player heartbeat failure $ex")
            }
        }
    }


    /**
     * Req start cloud player
     *
     * @param channelName
     * @param uid
     * @param robotUid
     * @param streamUrl
     * @param streamRegion
     * @param traceId
     */
    private fun reqStartCloudPlayer(
        channelName: String,
        uid: String,
        robotUid: Int,
        streamUrl: String,
        streamRegion: String, // cn, ap, na, eu
        traceId: String
    ) {
        post(
            baseUrl + "rte-cloud-player/start",
            JSONObject()
                .put("appId", BuildConfig.AGORA_APP_ID)
                .put(
                    "basicAuth",
                    Base64.encodeToString(
                        "${io.agora.scene.eCommerce.BuildConfig.CLOUD_PLAYER_KEY}:${io.agora.scene.eCommerce.BuildConfig.CLOUD_PLAYER_SECRET}".toByteArray(Charsets.UTF_8),
                        Base64.NO_WRAP
                    )
                )
                .put("channelName", channelName)
                .put("uid", uid)
                .put("robotUid", robotUid)
                .put("region", streamRegion)
                .put("streamUrl", streamUrl)
                .put("traceId", traceId)
                .put("src", "Android")
                .toString()
                .toRequestBody()
        )
    }

    /**
     * Req heat beat
     *
     * @param channelName
     * @param uid
     * @param traceId
     */
    private fun reqHeatBeat(
        channelName: String,
        uid: String,
        traceId: String
    ) {
        post(
            baseUrl + "heartbeat",
            JSONObject()
                .put("appId", BuildConfig.AGORA_APP_ID)
                .put("channelName", channelName)
                .put("uid", uid)
                .put("traceId", traceId)
                .put("src", "Android")
                .toString()
                .toRequestBody()
        )
    }

    /**
     * Post
     *
     * @param url
     * @param body
     */
    private fun post(url: String, body: RequestBody) {
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val _body = execute.body
                ?: throw RuntimeException("$url error: httpCode=${execute.code}, httpMsg=${execute.message}, body is null")
            val bodyJson = JSONObject(_body.string())
            Log.d(tag, "response $url $bodyJson")
            if (bodyJson["code"] != 0) {
                throw RuntimeException("$url error: httpCode=${execute.code}, httpMsg=${execute.message}, reqCode=${bodyJson["code"]}, reqMsg=${bodyJson["message"]},")
            }
        } else {
            throw RuntimeException("$url error: httpCode=${execute.code}, httpMsg=${execute.message}")
        }
    }
}