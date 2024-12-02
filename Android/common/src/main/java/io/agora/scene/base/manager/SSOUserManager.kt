package io.agora.scene.base.manager

import android.text.TextUtils
import com.google.gson.JsonIOException
import io.agora.scene.base.CommonBaseLogger
import io.agora.scene.base.Constant
import io.agora.scene.base.api.SSOUserInfo
import io.agora.scene.base.utils.GsonUtils
import io.agora.scene.base.utils.SPUtil
import java.util.UUID

object SSOUserManager {

    private var mToken: String = ""

    private var mUserIfo: SSOUserInfo? = null

    fun saveToken(token: String) {
        this.mToken = token
        SPUtil.putString(Constant.CURRENT_SSO_TOKEN, mToken)
    }

    fun getToken(): String {
        if (mToken.isEmpty()) {
            mToken = SPUtil.getString(Constant.CURRENT_SSO_TOKEN, "")
        }
        return mToken
    }

    /**
     * Is invitation
     *
     * @return
     */
    @JvmStatic
    fun isInvitationUser(): Boolean {
        return SPUtil.getBoolean(Constant.IS_INVITATION_USER, false)
    }

    fun setInvitationUser(isInvite: Boolean) {
        SPUtil.putBoolean(Constant.IS_INVITATION_USER, isInvite)
    }

    /**
     * Is generate code
     *
     * @return
     */
    fun isGenerateCode(): Boolean {
        return SPUtil.getBoolean(Constant.IS_GENERATE_CODE, false)
    }

    fun setGenerateCode(isGenerateCode: Boolean) {
        SPUtil.putBoolean(Constant.IS_GENERATE_CODE, isGenerateCode)
    }

    fun getOrCreateAccountUid(): String {
        var accountUid = SPUtil.getString(Constant.INVITATION_ACCOUNT_UID, "")
        if (TextUtils.isEmpty(accountUid)) {
            accountUid = UUID.randomUUID().toString().replace("-", "")
            SPUtil.putString(Constant.INVITATION_ACCOUNT_UID, accountUid)
        }
        return accountUid
    }

    @JvmStatic
    fun logout() {
        this.mToken = ""
        this.mUserIfo = null
        SPUtil.clear()
        UserManager.getInstance().clear();
    }

    fun saveUser(userData: SSOUserInfo) {
        this.mUserIfo = userData
        val userString: String = try {
            GsonUtils.gson.toJson(mUserIfo)
        } catch (io: JsonIOException) {
            CommonBaseLogger.e("SSOUserManager", io.message ?: "parse error")
            ""
        }
        CommonBaseLogger.d("SSOUserManager", "$userString")
        SPUtil.putString(Constant.CURRENT_SSO_USER, userString)
    }

    fun isLogin(): Boolean {
        val userInfo = getUser()
        return userInfo.accountUid.isNotEmpty() && userInfo.displayName.isNotEmpty() && mToken.isNotEmpty()
    }

    @JvmStatic
    fun getUser(): SSOUserInfo {
        if (mUserIfo != null && !mUserIfo?.accountUid.isNullOrEmpty()) {
            return mUserIfo!!
        }
        readingUserInfoFromPrefs()
        return mUserIfo!!
    }

    private fun readingUserInfoFromPrefs() {
        val userInfo = SPUtil.getString(Constant.CURRENT_SSO_USER, "")
        try {
            if (!TextUtils.isEmpty(userInfo)) {
                mUserIfo = GsonUtils.gson.fromJson(userInfo, SSOUserInfo::class.java)
            }
        } catch (e: Exception) {
            CommonBaseLogger.e("SSOUserManager", "Error parsing user info: ${e.message}")
            mUserIfo = SSOUserInfo("")
        }
        if (mUserIfo == null) mUserIfo = SSOUserInfo("")
    }

//    private fun writeUserInfoToPrefs(isLogout: Boolean) {
//        if (isLogout) {
//            this.mUserIfo = null
//            SPUtil.putString(Constant.CURRENT_SSO_USER, "")
//        } else {
//            val userString: String = try {
//                GsonUtils.gson.toJson(mUserIfo)
//            } catch (io: JsonIOException) {
//                CommonBaseLogger.e("SSOUserManager", io.message ?: "parse error")
//                ""
//            }
//            SPUtil.putString(Constant.CURRENT_SSO_USER, userString)
//        }
//    }
}