package io.agora.scene.ktv.service.api

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.moczul.ok2curl.CurlInterceptor
import com.moczul.ok2curl.logger.Logger
import io.agora.rtmsyncmanager.service.callback.AUIException
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

internal object KTVHttpManager {

    fun <T>errorFromResponse(response: Response<T>): AUIException {
        val errorMsg = response.errorBody()?.string()
        var code = -1
        var msg = "error"
        if (errorMsg != null) {
            val obj = JSONObject(errorMsg)
            if (obj.has("code")){
                code = obj.getInt("code")
            }
            if (obj.has("message")){
                msg = obj.getString("message")
            }
        }
        return AUIException(code, msg)
    }

    private var baseUrl = ""
    private const val version = "v1"
    private var retrofit: Retrofit? = null

    fun <T> getService(clazz: Class<T>): T {
        return retrofit!!.create(clazz)
    }

    fun setBaseURL(url: String) {
        if (baseUrl == url) {
            return
        }
        baseUrl = url
        retrofit = Retrofit.Builder()
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .addInterceptor(CurlInterceptor(object : Logger {
                        override fun log(message: String) {
                            Log.v("Ok2Curl", message)
                        }
                    }))
                    .build()
            )
            .baseUrl("$url/$version/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    private val gson =
        GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(TypeToken.get(JSONObject::class.java).type, object : TypeAdapter<JSONObject>() {
                @Throws(IOException::class)
                override fun write(jsonWriter: JsonWriter, value: JSONObject) {
                    jsonWriter.jsonValue(value.toString())
                }

                @Throws(IOException::class)
                override fun read(jsonReader: JsonReader): JSONObject? {
                    return null
                }
            })
            .enableComplexMapKeySerialization()
            .create()

}