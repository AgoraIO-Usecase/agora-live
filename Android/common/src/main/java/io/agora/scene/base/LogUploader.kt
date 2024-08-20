package io.agora.scene.base

import android.os.HandlerThread
import android.util.Log
import io.agora.scene.base.api.ApiManager
import io.agora.scene.base.api.ApiManagerService
import io.agora.scene.base.api.BaseResponse
import io.agora.scene.base.api.UploadLogResponse
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.uploader.OverallLayoutController
import io.agora.scene.base.uploader.UploadStatus
import io.agora.scene.base.utils.FileUtils
import io.agora.scene.base.utils.ZipUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.UUID

object LogUploader {

    enum class SceneType(val value: String) {
        KTV("ktv_log"),
        CHAT("voice_log"),
        SHOW("showlive_log"),
        ECOMMERCE("ecommerce_log"),
    }

    private val apiService by lazy {
        ApiManager.getApi(ApiManagerService::class.java)
    }

    private val scope = CoroutineScope(Job() + Dispatchers.Main)

    fun <T> request(
        block: suspend () -> BaseResponse<T>,
        onSuccess: (T) -> Unit,
        onError: (Exception) -> Unit = {},
    ): Job {
        return scope.launch(Dispatchers.Main) {
            runCatching {
                //请求体
                block()
            }.onSuccess { response ->
                runCatching {
                    //校验请求结果码是否正确，不正确会抛出异常走下面的onFailure
                    if (response.isSuccess) {
                        response.data?.let {
                            onSuccess(it)
                        } ?: run {
                            onError(Exception("Response data is null"))
                        }
                    } else {
                        onError(Exception("Error: ${response.message} (Code: ${response.code})"))
                    }
                }.onFailure { exception ->
                    Log.e(tag, "Request failed: ${exception.localizedMessage}", exception)
                    onError(Exception("Request failed due to: ${exception.localizedMessage}"))
                }
            }.onFailure { exception ->
                Log.e(tag, "Request failed: ${exception.localizedMessage}", exception)
                onError(Exception("Request failed due to: ${exception.localizedMessage}"))
            }
        }
    }


    private const val tag = "LogUploader"
    private val logFolder = AgoraApplication.the().getExternalFilesDir("")!!.absolutePath
    private val logFileWriteThread by lazy {
        HandlerThread("AgoraFeedback.$logFolder").apply {
            start()
        }
    }

    private const val rtcSdkPrefix = "agorasdk"
    private const val rtcApiPrefix = "agoraapi"
    private const val rtmSdkPrefix = "agorartmsdk"
    private const val commonBaseMiddle = "commonbase"
    private const val commonUIMiddle = "commonui"

    private fun getAgoraSDKPaths(): List<String> {
        val paths = mutableListOf<String>()
        File(logFolder).listFiles()?.forEach { file ->
            if (file.isFile) {
                if (file.name.startsWith(rtcSdkPrefix) ||
                    file.name.startsWith(rtcApiPrefix) ||
                    file.name.startsWith(rtmSdkPrefix)
                ) {
                    paths.add(file.path)
                }
            }
        }
        return paths
    }

    private fun getScenePaths(type: SceneType): List<String> {
        val paths = mutableListOf<String>()
        File(logFolder + File.separator + "ent").listFiles()?.forEach { file ->
            if (file.isFile) {
                if (!file.name.contains(commonBaseMiddle) &&
                    !file.name.contains(commonUIMiddle) &&
                    file.name.contains(type.value)
                ) {
                    paths.add(file.path)
                }
            }
        }
        return paths
    }

    @Volatile
    private var isUploading = false
    fun uploadLog(type: SceneType) {
        if (isUploading) return
        isUploading = true
        OverallLayoutController.show()

        val sdkLogZipPath = logFolder + File.separator + "agoraSdkLog.zip"

        val sdkPaths = getAgoraSDKPaths()
        val scenePaths = getScenePaths(type)
        val logPaths = mutableListOf<String>().apply {
            addAll(sdkPaths)
            addAll(scenePaths)
        }
        ZipUtils.compressFiles(logPaths, sdkLogZipPath, object : ZipUtils.ZipCallback {
            override fun onFileZipped(destinationFilePath: String) {
                requestUploadLog(File(destinationFilePath),
                    onSuccess = {
                        FileUtils.deleteFile(sdkLogZipPath)
                        isUploading = false
                        OverallLayoutController.uploadStatus(UploadStatus.Upload_Complete, it.logId)
                        Log.d(tag, "upload log success: ${it.logId}")
                    },
                    onError = {
                        FileUtils.deleteFile(sdkLogZipPath)
                        isUploading = false
                        Log.e(tag, "upload log failed:${it.message}")
                        OverallLayoutController.uploadStatus(UploadStatus.Upload_Failed, "")
                        OverallLayoutController.setOnRepeatUploadListener {
                            uploadLog(type)
                        }
                    })
            }

            override fun onError(e: java.lang.Exception?) {
                isUploading = false
                Log.e(tag, "upload log onError:${e?.message}")
                OverallLayoutController.uploadStatus(UploadStatus.Upload_Failed, "")
                OverallLayoutController.setOnRepeatUploadListener {
                    uploadLog(type)
                }
            }
        })
    }

    fun requestUploadLog(file: File, onSuccess: (UploadLogResponse) -> Unit, onError: (Exception) -> Unit) {
        val fileBody = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val partFile = MultipartBody.Part.createFormData("file", file.name, fileBody)
        request(
            block = {
                val traceId = UUID.randomUUID().toString().replace("-", "")
                apiService.requestUploadLog(BuildConfig.AGORA_APP_ID, traceId, partFile)
            },
            onSuccess = onSuccess,
            onError = onError
        )
    }
}