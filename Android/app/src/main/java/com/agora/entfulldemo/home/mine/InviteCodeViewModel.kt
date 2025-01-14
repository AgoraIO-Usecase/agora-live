package com.agora.entfulldemo.home.mine

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.agora.scene.base.manager.SSOUserManager

class InviteCodeViewModel : ViewModel() {

    private val _isGenerateCodeLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val isGenerateCodeLiveData: LiveData<Boolean> get() = _isGenerateCodeLiveData

    fun checkIsGenerateCode() {
        _isGenerateCodeLiveData.postValue(SSOUserManager.isGenerateCode())
    }
}