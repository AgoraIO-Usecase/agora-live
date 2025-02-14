package io.agora.scene.base.api

import io.agora.scene.base.CommonBaseLogger
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class HttpLogger : Interceptor {
    companion object {
        private val SENSITIVE_HEADERS = setOf(
            "authorization",
            "token",
            "access_token",
            "refresh_token",
            "x-token",
            "x-auth-token",
            "appCertificate"
        )
        
        private val SENSITIVE_PARAMS = setOf(
            "token",
            "accessToken",
            "refreshToken",
            "password",
            "secret",
            "appCertificate"
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBody = request.body

        // 记录请求信息
        val curl = StringBuilder("curl -X ${request.method}")
        
        // 过滤敏感 header
        request.headers.forEach { (name, value) ->
            val safeValue = if (name.lowercase() in SENSITIVE_HEADERS) "***" else value
            curl.append(" -H '$name: $safeValue'")
        }

        // 过滤请求体中的敏感信息
        requestBody?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            val charset = body.contentType()?.charset() ?: Charset.defaultCharset()
            var bodyString = buffer.readString(charset)
            
            // 替换敏感参数
            SENSITIVE_PARAMS.forEach { param ->
                bodyString = bodyString.replace(
                    Regex(""""$param"\s*:\s*"[^"]*""""),
                    """"$param":"***""""
                )
            }
            
            curl.append(" -d '$bodyString'")
        }

        // 过滤 URL 中的敏感参数
        val urlBuilder = StringBuilder()
        val url = request.url
        urlBuilder.append(url.scheme).append("://")
            .append(url.host)
        if (url.port != 80 && url.port != 443) {
            urlBuilder.append(":").append(url.port)
        }
        urlBuilder.append(url.encodedPath)
        
        if (url.queryParameterNames.isNotEmpty()) {
            urlBuilder.append("?")
            url.queryParameterNames.forEachIndexed { index, name ->
                if (index > 0) urlBuilder.append("&")
                val value = if (name.lowercase() in SENSITIVE_PARAMS) "***" 
                           else url.queryParameter(name)
                urlBuilder.append("$name=$value")
            }
        }
        
        curl.append(" '${urlBuilder}'")

        CommonBaseLogger.d("HTTP-Request", curl.toString())

        // 记录响应信息
        val startNs = System.nanoTime()
        val response = chain.proceed(request)
        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body?:return response
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"

        CommonBaseLogger.d("HTTP-Response", buildString {
            append("${response.code} ${response.message} for ${urlBuilder}")
            append(" (${tookMs}ms")
            if (response.networkResponse != null && response.networkResponse != response) {
                append(", ${bodySize} body")
            }
            append(")")
            
            // 过滤响应头中的敏感信息
            response.headers.forEach { (name, value) ->
                val safeValue = if (name.lowercase() in SENSITIVE_HEADERS) "***" else value
                append(", $name: $safeValue")
            }

            // 记录响应体，同时过滤敏感信息
            responseBody.let { body ->
                val contentType = body.contentType()
                if (contentType?.type == "application" && 
                    (contentType.subtype.contains("json") || contentType.subtype.contains("xml"))) {
                    val source = body.source()
                    source.request(Long.MAX_VALUE)
                    val buffer = source.buffer
                    val charset = contentType.charset() ?: Charset.defaultCharset()
                    if (contentLength != 0L) {
                        append(", ")
                        var bodyString = buffer.clone().readString(charset)
                        
                        // 替换响应中的敏感信息
                        SENSITIVE_PARAMS.forEach { param ->
                            bodyString = bodyString.replace(
                                Regex(""""$param"\s*:\s*"[^"]*""""),
                                """"$param":"***""""
                            )
                        }
                        append(bodyString)
                    }
                }
            }
        })

        return response
    }
} 