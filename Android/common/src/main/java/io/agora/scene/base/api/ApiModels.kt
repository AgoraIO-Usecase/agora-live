package io.agora.scene.base.api

import com.google.gson.annotations.SerializedName
import java.io.Serializable

open class BaseResponse<T> : Serializable {
    @SerializedName("errorCode", alternate = ["code"])
    var code: Int? = null

    @SerializedName("message", alternate = ["msg"])
    var message: String? = ""

    @SerializedName(value = "obj", alternate = ["result", "data"])
    var data: T? = null

    val isSuccess: Boolean get() = 0 == code
}

data class UploadLogResponse constructor(val logId: String): BaseResponse<UploadLogResponse>()
