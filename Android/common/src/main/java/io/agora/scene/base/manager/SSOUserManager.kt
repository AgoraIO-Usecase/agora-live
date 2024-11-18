package io.agora.scene.base.manager

import android.text.TextUtils
import com.google.gson.JsonIOException
import io.agora.scene.base.CommonBaseLogger
import io.agora.scene.base.Constant
import io.agora.scene.base.api.SSOUserInfo
import io.agora.scene.base.utils.GsonUtils
import io.agora.scene.base.utils.SPUtil

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

    fun logout() {
        this.mToken = ""
        SPUtil.putString(Constant.CURRENT_SSO_TOKEN, "")
        writeUserInfoToPrefs(true)
    }

    fun saveUser(userData: SSOUserInfo) {
        this.mUserIfo = userData
        writeUserInfoToPrefs(false)
    }

    fun isLogin(): Boolean {
        val userInfo = getUser()
        return userInfo.profileId > 0 && userInfo.displayName.isNotEmpty() && mToken.isNotEmpty()
    }

    @JvmStatic
    fun getUser(): SSOUserInfo {
        if (mUserIfo != null && mUserIfo?.profileId != 0) {
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
            mUserIfo = SSOUserInfo.emptyUser()
        }
        if (mUserIfo == null) mUserIfo = SSOUserInfo.emptyUser()
    }

    private fun writeUserInfoToPrefs(isLogout: Boolean) {
        if (isLogout) {
            this.mUserIfo = null
            SPUtil.putString(Constant.CURRENT_SSO_USER, "")
        } else {
            val userString: String = try {
                GsonUtils.gson.toJson(mUserIfo)
            } catch (io: JsonIOException) {
                CommonBaseLogger.e("SSOUserManager", io.message ?: "parse error")
                ""
            }
            SPUtil.putString(Constant.CURRENT_SSO_USER, userString)
        }
    }
}