package io.agora.scene.base

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
    private val agoraLogFolder = AgoraApplication.the().getExternalFilesDir("")!!.absolutePath
    private val entLogFolder = AgoraApplication.the().getExternalFilesDir("")!!.absolutePath  + File.separator + "ent"

    private fun getAgoraSDKPaths(): List<String> {
        val paths = mutableListOf<String>()
        File(agoraLogFolder).listFiles()?.forEach { file ->
            if (file.isFile) {
                if (file.name.startsWith("agorasdk") ||
                    file.name.startsWith("agoraapi") ||
                    file.name.startsWith("agorartmsdk")
                ) {
                    paths.add(file.path)
                }
            }
        }
        return paths
    }

    private fun getScenePaths(type: AgoraScenes): List<String> {
        val paths = mutableListOf<String>()
        File(entLogFolder).listFiles()?.forEach { file ->
            if (file.isFile) {
                // 不需要传 common
                if (!file.name.contains(AgoraScenes.CommonBase.name) &&
                    file.name.contains(type.name,true)
                ) {
                    paths.add(file.path)
                }
            }
        }
        return paths
    }

    @Volatile
    private var isUploading = false
    fun uploadLog(type: AgoraScenes) {
        if (isUploading) return
        isUploading = true
        OverallLayoutController.show()

        val allLogZipPath = agoraLogFolder + File.separator + "agoraSdkLog.zip"

        val sdkPaths = getAgoraSDKPaths()
        val scenePaths = getScenePaths(type)
        val logPaths = mutableListOf<String>().apply {
            addAll(sdkPaths)
            addAll(scenePaths)
        }
        ZipUtils.compressFiles(logPaths, allLogZipPath, object : ZipUtils.ZipCallback {
            override fun onFileZipped(destinationFilePath: String) {
                requestUploadLog(File(destinationFilePath),
                    onSuccess = {
                        FileUtils.deleteFile(allLogZipPath)
                        isUploading = false
                        OverallLayoutController.uploadStatus(UploadStatus.Upload_Complete, it.logId)
                        Log.d(tag, "upload log success: ${it.logId}")
                    },
                    onError = {
                        FileUtils.deleteFile(allLogZipPath)
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