package io.agora.scene.voice.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import io.agora.scene.voice.viewmodel.repositories.VoiceUserListRepository
import io.agora.scene.voice.model.VoiceMemberModel
import io.agora.scene.voice.model.VoiceMicInfoModel
import io.agora.scene.voice.model.VoiceRankUserModel
import io.agora.voice.common.net.Resource
import io.agora.voice.common.viewmodel.SingleSourceLiveData

class VoiceUserListViewModel : ViewModel() {

    private val mRepository: VoiceUserListRepository by lazy { VoiceUserListRepository() }
    private val _applicantsListObservable: SingleSourceLiveData<Resource<List<VoiceMemberModel>>> =
        SingleSourceLiveData()
    private val _inviteListObservable: SingleSourceLiveData<Resource<List<VoiceMemberModel>>> =
        SingleSourceLiveData()
    private val _contributeListObservable: SingleSourceLiveData<Resource<List<VoiceRankUserModel>>> =
        SingleSourceLiveData()
    private val _startMicSeatInvitationObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()
    private val _acceptMicSeatApplyObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _roomMemberObservable: SingleSourceLiveData<Resource<List<VoiceMemberModel>>> =
        SingleSourceLiveData()
    private val _kickMemberObservable: SingleSourceLiveData<Resource<Int>> =
        SingleSourceLiveData()

    fun applicantsListObservable(): LiveData<Resource<List<VoiceMemberModel>>> = _applicantsListObservable

    fun inviteListObservable(): LiveData<Resource<List<VoiceMemberModel>>> = _inviteListObservable

    fun memberListObservable(): LiveData<Resource<List<VoiceMemberModel>>> = _roomMemberObservable

    fun contributeListObservable(): LiveData<Resource<List<VoiceRankUserModel>>> = _contributeListObservable

    fun startMicSeatInvitationObservable(): LiveData<Resource<Boolean>> = _startMicSeatInvitationObservable

    fun acceptMicSeatApplyObservable(): LiveData<Resource<VoiceMicInfoModel>> = _acceptMicSeatApplyObservable

    fun kickOffObservable():LiveData<Resource<Int>> = _kickMemberObservable

    fun fetchApplicantsList() {
        _applicantsListObservable.setSource(mRepository.fetchApplicantsList())
    }

    fun fetchInviteList() {
        _inviteListObservable.setSource(mRepository.fetchInvitedList())
    }

    fun fetchGiftContribute() {
        _contributeListObservable.setSource(mRepository.fetchGiftContribute())
    }

    fun fetchMemberList(){
        _roomMemberObservable.setSource(mRepository.fetchRoomMembers())
    }

    fun startMicSeatInvitation(chatUid: String, micIndex: Int?) {
        _startMicSeatInvitationObservable.setSource(mRepository.startMicSeatInvitation(chatUid, micIndex))
    }

    fun acceptMicSeatApply(micIndex: Int?,chatUid: String) {
        _acceptMicSeatApplyObservable.setSource(mRepository.acceptMicSeatApply(micIndex,chatUid))
    }

    fun kickMembersOutOfTheRoom(chatUid:String,index:Int){
        val userList = mutableListOf<String>()
        userList.add(chatUid)
        _kickMemberObservable.setSource(mRepository.kickRoomMember(userList,index))
    }
}