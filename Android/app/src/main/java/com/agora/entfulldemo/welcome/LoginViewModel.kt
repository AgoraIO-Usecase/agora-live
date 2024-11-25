package com.agora.entfulldemo.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.agora.scene.base.api.ApiManager
import io.agora.scene.base.api.ApiManagerService
import io.agora.scene.base.api.InvitationLoginReq
import io.agora.scene.base.api.SSOUserInfo
import io.agora.scene.base.manager.SSOUserManager
import io.agora.scene.widget.toast.CustomToast
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val apiService by lazy {
        ApiManager.getApi(ApiManagerService::class.java)
    }

    private val _tokenLiveData: MutableLiveData<String> = MutableLiveData()
    val tokenLiveData: LiveData<String> get() = _tokenLiveData

    private val _userInfoLiveData: MutableLiveData<SSOUserInfo> = MutableLiveData()
    val userInfoLiveData: LiveData<SSOUserInfo> get() = _userInfoLiveData

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
                    }
                }.onFailure {
                    SSOUserManager.logout()
                    _userInfoLiveData.postValue(null)
                    CustomToast.show("Get User data error:${it.message}")
                    it.printStackTrace()
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
                    }
                }.onFailure {
                    SSOUserManager.logout()
                    _userInfoLiveData.postValue(null)
                    CustomToast.show("Get User data error:${it.message}")
                    it.printStackTrace()
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
                    CustomToast.show("Invalid Code. Please try again.")
                }
            }.onFailure {
                _tokenLiveData.postValue(null)
                CustomToast.show("Invalid Code. Please try again.")
                it.printStackTrace()
            }
        }
    }
}