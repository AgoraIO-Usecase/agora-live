package io.agora.scene.ktv.service.api

import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.agora.rtmsyncmanager.service.callback.AUIException
import io.agora.scene.base.api.SecureOkHttpClient
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

internal object KTVHttpManager {

    fun <T> errorFromResponse(response: Response<T>): AUIException {
        val errorMsg = response.errorBody()?.string()
        var code = response.code()
        var msg = errorMsg
        if (errorMsg != null) {
            try {
                val obj = JSONObject(errorMsg)
                if (obj.has("code")) {
                    code = obj.getInt("code")
                }
                if (obj.has("message")) {
                    msg = obj.getString("message")
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                SecureOkHttpClient.create()
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
            .registerTypeAdapter(KtvSongApiModel::class.java, KtvSongApiModelSerializer())
            .enableComplexMapKeySerialization()
            .create()

}