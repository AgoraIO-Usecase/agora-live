package com.agora.entfulldemo.welcome

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.agora.entfulldemo.R
import io.agora.scene.base.api.ApiError
import io.agora.scene.base.api.ApiManager
import io.agora.scene.base.api.ApiManagerService
import io.agora.scene.base.api.InvitationLoginReq
import io.agora.scene.base.api.SSOUserInfo
import io.agora.scene.base.manager.SSOUserManager
import io.agora.scene.widget.toast.CustomToast
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel : ViewModel() {

    private val TAG = "LoginViewModel"

    private val apiService by lazy {
        ApiManager.getApi(ApiManagerService::class.java)
    }

    private val _tokenLiveData: MutableLiveData<String?> = MutableLiveData()
    val tokenLiveData: LiveData<String?> get() = _tokenLiveData

    private val _userInfoLiveData: MutableLiveData<SSOUserInfo?> = MutableLiveData()
    val userInfoLiveData: LiveData<SSOUserInfo?> get() = _userInfoLiveData

    fun checkLogin() {
        _tokenLiveData.postValue(SSOUserManager.getToken())
    }

    fun getUserInfoByToken(token: String) {
        if (SSOUserManager.isInvitationUser()) {
            viewModelScope.launch {
                runCatching {
                    apiService.invitationUserInfo("Bearer $token")
                }.onSuccess { result ->
                    if (result.isSuccess && result.data != null) {
                        SSOUserManager.saveUser(result.data!!)
                        _userInfoLiveData.postValue(result.data)
                    } else {
                        SSOUserManager.logout()
                        _userInfoLiveData.postValue(null)
                        CustomToast.show(R.string.app_login_expired)
                    }
                }.onFailure { e ->
                    SSOUserManager.logout()
                    _userInfoLiveData.postValue(null)
                    CustomToast.show(R.string.app_login_expired)
                    if ((e is HttpException) && e.code() == 401) {
                        Log.e(TAG, "Login timeout, please log in again.")
                    }
                }
            }
        } else {
            viewModelScope.launch {
                runCatching {
                    apiService.ssoUserInfo("Bearer $token")
                }.onSuccess { result ->
                    if (result.isSuccess && result.data != null) {
                        SSOUserManager.saveUser(result.data!!)
                        _userInfoLiveData.postValue(result.data)
                    } else {
                        SSOUserManager.logout()
                        _userInfoLiveData.postValue(null)
                        CustomToast.show(R.string.app_login_expired)
                    }
                }.onFailure { e ->
                    SSOUserManager.logout()
                    _userInfoLiveData.postValue(null)
                    CustomToast.show(R.string.app_login_expired)
                    if ((e is HttpException) && e.code() == 401) {
                        Log.e(TAG, "Token expired, please log in again.")
                    }
                }
            }
        }
    }

    fun invitationLogin(invitationCode: String) {
        val accountUid = SSOUserManager.getOrCreateAccountUid()
        viewModelScope.launch {
            runCatching {
                apiService.invitationLogin(InvitationLoginReq(invitationCode, accountUid))
            }.onSuccess { result ->
                if (result.isSuccess && result.data != null) {
                    val token = result.data?.token ?: ""
                    SSOUserManager.setInvitationUser(true)
                    SSOUserManager.saveToken(token)
                    _tokenLiveData.postValue(token)
                } else {
                    _tokenLiveData.postValue(null)
                    if (result.code == ApiError.TOO_MANY_ATTEMPTS) {
                        CustomToast.show(R.string.app_too_many_attempts)
                    } else {
                        CustomToast.show(R.string.app_invalid_invite_code)
                    }
                }
            }.onFailure {
                _tokenLiveData.postValue(null)
                CustomToast.show(R.string.app_invalid_invite_code)
                it.printStackTrace()
            }
        }
    }
}