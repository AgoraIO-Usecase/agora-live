package io.agora.scene.base

import android.util.Log
import com.moczul.ok2curl.CurlInterceptor
import com.moczul.ok2curl.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject

/**
 * Token generator
 *
 * @constructor Create empty Token generator
 */
object TokenGenerator {
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val okHttpClient by lazy {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                .addInterceptor(
                    CurlInterceptor(object : Logger {
                        override fun log(message: String) {
                            Log.d("CurlInterceptor", message)
                        }
                    })
                )
        }
        builder.build()
    }

    /**
     * Expire second
     */
    var expireSecond: Long = -1

    /**
     * Default expire second
     */
    private const val defaultExpireSecond: Long = 60 * 60 * 24

    /**
     * Token generator type
     *
     * @constructor Create empty Token generator type
     */
    enum class TokenGeneratorType {
        /**
         * Token006
         *
         * @constructor Create empty Token006
         */
        Token006,

        /**
         * Token007
         *
         * @constructor Create empty Token007
         */
        Token007
    }

    /**
     * Agora token type
     *
     * @property value
     * @constructor Create empty Agora token type
     */
    enum class AgoraTokenType(val value: Int) {
        /**
         * Rtc
         *
         * @constructor Create empty Rtc
         */
        Rtc(1),

        /**
         * Rtm
         *
         * @constructor Create empty Rtm
         */
        Rtm(2),

        /**
         * Chat
         *
         * @constructor Create empty Chat
         */
        Chat(3)
    }

    /**
     * Generate tokens
     *
     * @param channelName
     * @param uid
     * @param genType
     * @param tokenTypes
     * @param success
     * @param failure
     * @receiver
     */
    fun generateTokens(
        channelName: String,
        uid: String,
        genType: TokenGeneratorType,
        tokenTypes: Array<AgoraTokenType>,
        success: (Map<AgoraTokenType, String>) -> Unit,
        failure: ((Exception?) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                val out = mutableMapOf<AgoraTokenType, String>()
                tokenTypes.forEach {
                    out[it] = fetchToken(channelName, uid, genType, it)
                }
                success.invoke(out)
            } catch (e: Exception) {
                failure?.invoke(e)
            }
        }
    }


    /**
     * Generate token
     *
     * @param channelName
     * @param uid
     * @param genType
     * @param tokenType
     * @param success
     * @param failure
     * @receiver
     */
    fun generateToken(
        channelName: String,
        uid: String,
        genType: TokenGeneratorType,
        tokenType: AgoraTokenType,
        success: (String) -> Unit,
        failure: ((Exception?) -> Unit)? = null
    ) {
        scope.launch(Dispatchers.Main) {
            try {
                success.invoke(fetchToken(channelName, uid, genType, tokenType))
            } catch (e: Exception) {
                failure?.invoke(e)
            }
        }
    }

    private suspend fun fetchToken(
        channelName: String,
        uid: String,
        genType: TokenGeneratorType,
        tokenType: AgoraTokenType
    ) = withContext(Dispatchers.IO) {
        val postBody = JSONObject()
        postBody.put("appId", BuildConfig.AGORA_APP_ID)
        postBody.put("appCertificate", BuildConfig.AGORA_APP_CERTIFICATE)
        postBody.put("channelName", channelName)
        postBody.put("expire", if (expireSecond > 0) expireSecond else defaultExpireSecond)
        postBody.put("src", "Android")
        postBody.put("ts", System.currentTimeMillis().toString() + "")
        postBody.put("type", tokenType.value)
        postBody.put("uid", uid + "")

        val request = Request.Builder().url(
            if (genType == TokenGeneratorType.Token006) {
                "${BuildConfig.TOOLBOX_SERVER_HOST}/v2/token006/generate"
            } else {
                "${BuildConfig.TOOLBOX_SERVER_HOST}/v2/token/generate"
            }
        ).addHeader("Content-Type", "application/json").post(postBody.toString().toRequestBody())
            .build()
        val execute = okHttpClient.newCall(request).execute()
        if (execute.isSuccessful) {
            val body = execute.body
                ?: throw RuntimeException("Fetch token error: httpCode=${execute.code}, httpMsg=${execute.message}, body is null")
            val bodyJobj = JSONObject(body.string())
            if (bodyJobj["code"] != 0) {
                throw RuntimeException(
                    "Fetch token error: httpCode=${execute.code}, httpMsg=${execute.message}, reqCode=${bodyJobj["code"]}, reqMsg=${bodyJobj["message"]},"
                )
            } else {
                (bodyJobj["data"] as JSONObject)["token"] as String
            }
        } else {
            throw RuntimeException("Fetch token error: httpCode=${execute.code}, httpMsg=${execute.message}")
        }
    }
}
