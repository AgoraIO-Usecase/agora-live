package io.agora.scene.base.api

import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.agora.scene.base.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

object ApiManager {

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
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .create()

    private val okHttpClient by lazy {
        val builder = SecureOkHttpClient.create()
            .addInterceptor(DynamicConnectTimeout())
            .addInterceptor(HttpLogger())
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
        builder.build()
    }

    private fun extractBaseUrl(url: String): String {
        val uri = URI(url)
        return "${uri.scheme}://${uri.host}/"
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(extractBaseUrl(BuildConfig.TOOLBOX_SERVER_HOST))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun <T> getApi(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }
}

class DynamicConnectTimeout : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestUrl = request.url.toString()
        val isUploadFileApi = requestUrl.contains(ApiManagerService.requestUploadLog)
        if (isUploadFileApi) {
            return chain.withConnectTimeout(60 * 3, TimeUnit.SECONDS)
                .withReadTimeout(60 * 3, TimeUnit.SECONDS)
                .withWriteTimeout(60 * 3, TimeUnit.SECONDS)
                .proceed(request)
        }
        return chain.proceed(request)
    }
}
