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

data class SSOUserInfo constructor(
    val accountUid: String,
    val accountType: String,
    val email: String,
    val verifyPhone: String,
    val companyId: Int,
    val profileId: Int,
    val displayName: String,
    val companyName: String,
    val companyCountry: String
): BaseResponse<UploadLogResponse>(){

    companion object{

        @JvmStatic
        fun emptyUser(): SSOUserInfo {
            return SSOUserInfo("", "", "", "", 0, 0, "", "", "")
        }
    }
}
