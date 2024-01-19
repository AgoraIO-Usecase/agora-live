package io.agora.scene.voice.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import io.agora.scene.voice.model.VoiceRoomModel
import io.agora.scene.voice.viewmodel.repositories.VoiceCreateRepository
import io.agora.voice.common.net.Resource
import io.agora.voice.common.viewmodel.SingleSourceLiveData

class VoiceCreateViewModel constructor(application: Application) : AndroidViewModel(application) {

    private val voiceRoomRepository by lazy { VoiceCreateRepository() }

    private val _roomListObservable: SingleSourceLiveData<Resource<List<VoiceRoomModel>>> =
        SingleSourceLiveData()

    private val _checkPasswordObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()

    private val _createRoomObservable: SingleSourceLiveData<Resource<VoiceRoomModel>> =
        SingleSourceLiveData()

    private val _joinRoomObservable: SingleSourceLiveData<Resource<VoiceRoomModel>> =
        SingleSourceLiveData()

    fun roomListObservable(): LiveData<Resource<List<VoiceRoomModel>>> = _roomListObservable

    fun checkPasswordObservable(): LiveData<Resource<Boolean>> = _checkPasswordObservable

    fun createRoomObservable(): LiveData<Resource<VoiceRoomModel>> = _createRoomObservable

    fun joinRoomObservable(): LiveData<Resource<VoiceRoomModel>> = _joinRoomObservable


    fun getRoomList(page: Int) {
        _roomListObservable.setSource(voiceRoomRepository.fetchRoomList(page))
    }

    fun checkPassword(roomId: String, password: String, userInput: String) {
        _checkPasswordObservable.setSource(voiceRoomRepository.checkPassword(roomId, password, userInput))
    }

    fun createRoom(roomName: String, soundEffect: Int = 0, password: String? = null) {
        _createRoomObservable.setSource(voiceRoomRepository.createRoom(roomName, soundEffect, 0, password))
    }

    fun createSpatialRoom(roomName: String, soundEffect: Int = 0, password: String? = null) {
        _createRoomObservable.setSource(voiceRoomRepository.createRoom(roomName, soundEffect, 0, password))
    }

    fun joinRoom(roomId: String) {
        _joinRoomObservable.setSource(voiceRoomRepository.joinRoom(roomId))
    }
}