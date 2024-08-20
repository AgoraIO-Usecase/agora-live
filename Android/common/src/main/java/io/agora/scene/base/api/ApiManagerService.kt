package io.agora.scene.base.api;

import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query

interface ApiManagerService {

    @Multipart
    @POST("/toolbox/v1/upload/log")
    suspend fun requestUploadLog(
        @Query("appId") appId: String,
        @Query("traceId") traceId: String,
        @Part body: MultipartBody.Part
    ): BaseResponse<UploadLogResponse>
}
