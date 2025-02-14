package io.agora.scene.base.api

import com.google.gson.annotations.SerializedName
import io.agora.meta.ILocalUserAvatar
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

data class UploadLogResponse constructor(val logId: String) : BaseResponse<UploadLogResponse>()

data class SSOUserInfo constructor(
    val accountUid: String,
    val accountType: String = "",
    val email: String = "",
    val verifyPhone: String = "",
    val companyId: Int = 0,
    val profileId: Int = 0,
    var displayName: String = "",
    val companyName: String = "",
    val companyCountry: String = "",
    val invitationCode: String = ""
) : BaseResponse<SSOUserInfo>()

data class InvitationLoginReq(
    val invitationCode: String,
    val accountUid: String
)

data class InvitationLoginResp(
    val token: String
)

object ApiError {
    const val INVALID_INVITE_CODE = 142
    const val TOO_MANY_ATTEMPTS = 145
}