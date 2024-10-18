package io.agora.scene.base.api;

import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query

interface ApiManagerService {

    companion object{
        const val requestUploadLog = "/toolbox-overseas/v1/upload/log"
    }

    @Multipart
    @POST(requestUploadLog)
    suspend fun requestUploadLog(
        @Query("appId") appId: String,
        @Query("traceId") traceId: String,
        @Part body: MultipartBody.Part
    ): BaseResponse<UploadLogResponse>
}
