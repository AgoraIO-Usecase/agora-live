package com.agora.entfulldemo.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.agora.scene.base.api.ApiManager
import io.agora.scene.base.api.ApiManagerService
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
        viewModelScope.launch {
            runCatching {
                apiService.ssoUserInfo("Bearer $token")
            }.onSuccess { result ->
                if (result.isSuccess && result.data != null) {
                    SSOUserManager.saveUser(result.data!!)
                    _userInfoLiveData.postValue(result.data)
                } else {
                    _userInfoLiveData.postValue(null)
                }
            }.onFailure {
                CustomToast.showError("Get User data error:${it.message}")
                it.printStackTrace()
            }
        }
    }
}