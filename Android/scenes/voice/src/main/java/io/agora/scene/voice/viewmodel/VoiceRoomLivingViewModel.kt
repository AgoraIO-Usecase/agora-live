package io.agora.scene.voice.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.agora.ValueCallBack
import io.agora.chat.ChatRoom
import io.agora.scene.voice.global.VoiceBuddyFactory
import io.agora.scene.voice.imkit.manager.ChatroomIMManager
import io.agora.scene.voice.model.*
import io.agora.scene.voice.netkit.VRCreateRoomResponse
import io.agora.scene.voice.netkit.VoiceToolboxServerHttpManager
import io.agora.voice.common.net.callback.VRValueCallBack
import io.agora.voice.common.viewmodel.NetworkOnlyResource
import io.agora.scene.voice.viewmodel.repositories.VoiceRoomLivingRepository
import io.agora.scene.voice.rtckit.AgoraRtcEngineController
import io.agora.voice.common.net.Resource
import io.agora.voice.common.net.callback.ResultCallBack
import io.agora.voice.common.utils.LogTools
import io.agora.voice.common.utils.LogTools.logD
import io.agora.voice.common.utils.LogTools.logE
import io.agora.voice.common.utils.ThreadManager
import io.agora.voice.common.viewmodel.SingleSourceLiveData
import java.util.concurrent.atomic.AtomicBoolean

class VoiceRoomLivingViewModel : ViewModel() {

    private val mRepository by lazy { VoiceRoomLivingRepository() }

    private val joinRtcChannel = AtomicBoolean(false)
    private val joinImRoom = AtomicBoolean(false)

    private val _roomDetailsObservable: SingleSourceLiveData<Resource<VoiceRoomInfo>> =
        SingleSourceLiveData()
    private val _joinObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()
    private val _roomNoticeObservable: SingleSourceLiveData<Resource<Pair<String,Boolean>>> =
        SingleSourceLiveData()
    private val _openBotObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()
    private val _closeBotObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()
    private val _robotVolumeObservable: SingleSourceLiveData<Resource<Pair<Int, Boolean>>> =
        SingleSourceLiveData()
    private val _muteMicObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _unMuteMicObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _leaveMicObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _forbidMicObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _cancelForbidMicObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _kickMicObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _rejectMicInvitationObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()
    private val _lockMicObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _cancelLockMicObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _startMicSeatApplyObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()
    private val _cancelMicSeatApplyObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()
    private val _changeMicObservable: SingleSourceLiveData<Resource<Map<Int, VoiceMicInfoModel>>> =
        SingleSourceLiveData()
    private val _acceptMicSeatInvitationObservable: SingleSourceLiveData<Resource<VoiceMicInfoModel>> =
        SingleSourceLiveData()
    private val _leaveSyncRoomObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()
    private val _updateRoomMemberObservable: SingleSourceLiveData<Resource<Boolean>> =
        SingleSourceLiveData()

    fun roomDetailsObservable(): LiveData<Resource<VoiceRoomInfo>> = _roomDetailsObservable

    fun joinObservable(): LiveData<Resource<Boolean>> = _joinObservable

    fun roomNoticeObservable(): LiveData<Resource<Pair<String,Boolean>>> = _roomNoticeObservable
    fun openBotObservable(): LiveData<Resource<Boolean>> = _openBotObservable

    fun closeBotObservable(): LiveData<Resource<Boolean>> = _closeBotObservable

    fun robotVolumeObservable(): LiveData<Resource<Pair<Int, Boolean>>> = _robotVolumeObservable

    fun muteMicObservable(): LiveData<Resource<VoiceMicInfoModel>> = _muteMicObservable

    fun unMuteMicObservable(): LiveData<Resource<VoiceMicInfoModel>> = _unMuteMicObservable

    fun leaveMicObservable(): LiveData<Resource<VoiceMicInfoModel>> = _leaveMicObservable

    fun forbidMicObservable(): LiveData<Resource<VoiceMicInfoModel>> = _forbidMicObservable

    fun cancelForbidMicObservable(): LiveData<Resource<VoiceMicInfoModel>> = _cancelForbidMicObservable

    fun kickMicObservable(): LiveData<Resource<VoiceMicInfoModel>> = _kickMicObservable

    fun rejectMicInvitationObservable(): LiveData<Resource<Boolean>> = _rejectMicInvitationObservable

    fun lockMicObservable(): LiveData<Resource<VoiceMicInfoModel>> = _lockMicObservable

    fun cancelLockMicObservable(): LiveData<Resource<VoiceMicInfoModel>> = _cancelLockMicObservable

    fun startMicSeatApplyObservable(): LiveData<Resource<Boolean>> = _startMicSeatApplyObservable

    fun cancelMicSeatApplyObservable(): LiveData<Resource<Boolean>> = _cancelMicSeatApplyObservable

    fun changeMicObservable(): LiveData<Resource<Map<Int, VoiceMicInfoModel>>> = _changeMicObservable

    fun acceptMicSeatInvitationObservable(): LiveData<Resource<VoiceMicInfoModel>> = _acceptMicSeatInvitationObservable

    fun updateRoomMemberObservable():LiveData<Resource<Boolean>> = _updateRoomMemberObservable

    fun fetchRoomDetail(voiceRoomModel: VoiceRoomModel) {
        _roomDetailsObservable.setSource(mRepository.fetchRoomDetail(voiceRoomModel))
    }

    fun initSdkJoin(context: Context, voiceRoomModel: VoiceRoomModel) {
        joinRtcChannel.set(false)
        joinImRoom.set(false)
        AgoraRtcEngineController.get().joinChannel(
            context.applicationContext,
            voiceRoomModel.roomId,
            VoiceBuddyFactory.get().getVoiceBuddy().rtcUid(),
            voiceRoomModel.soundEffect, voiceRoomModel.isOwner,
            object : VRValueCallBack<Boolean> {
                override fun onSuccess(value: Boolean) {
                    "rtc  joinChannel onSuccess channelId:${voiceRoomModel.roomId}".logD()
                    joinRtcChannel.set(true)
                    checkJoinRoom()
                }

                override fun onError(error: Int, errorMsg: String) {
                    "rtc  joinChannel onError channelId:${voiceRoomModel.roomId},error:$error  $errorMsg".logE()
                    ThreadManager.getInstance().runOnMainThread {
                        _joinObservable.setSource(object : NetworkOnlyResource<Boolean>() {
                            override fun createCall(callBack: ResultCallBack<LiveData<Boolean>>) {
                                callBack.onError(error, errorMsg)
                            }
                        }.asLiveData())
                    }

                }
            }
        )
        ChatroomIMManager.getInstance()
            .joinRoom(voiceRoomModel.chatroomId, object : ValueCallBack<ChatRoom?> {
                override fun onSuccess(value: ChatRoom?) {
                    "im joinChatRoom onSuccess roomId:${voiceRoomModel.chatroomId}".logD()
                    joinImRoom.set(true)
                    checkJoinRoom()
                }

                override fun onError(error: Int, errorMsg: String) {
                    _joinObservable.setSource(object : NetworkOnlyResource<Boolean>() {
                        override fun createCall(callBack: ResultCallBack<LiveData<Boolean>>) {
                            callBack.onError(error, errorMsg)
                        }
                    }.asLiveData())
                    "im joinChatRoom onError roomId:${voiceRoomModel.chatroomId},$error  $errorMsg".logE()
                }
            })
    }

    private fun checkJoinRoom() {
        ThreadManager.getInstance().runOnMainThread{
            if (joinRtcChannel.get() && joinImRoom.get()) {
                _joinObservable.setSource(object : NetworkOnlyResource<Boolean>() {
                    override fun createCall(callBack: ResultCallBack<LiveData<Boolean>>) {
                        callBack.onSuccess(MutableLiveData(true))
                    }
                }.asLiveData())
            }
        }
    }

    fun enableRobot(active: Boolean) {
        if (active) {
            _openBotObservable.setSource(mRepository.enableRobot(true))
        } else {
            _closeBotObservable.setSource(mRepository.enableRobot(false))
        }
    }

    fun updateBotVolume(robotVolume: Int) {
        _robotVolumeObservable.setSource(mRepository.updateRobotVolume(robotVolume))
    }

    fun updateAnnouncement(notice: String) {
        _roomNoticeObservable.setSource(mRepository.updateAnnouncement(notice))
    }

    fun muteLocal(micIndex: Int) {
        _muteMicObservable.setSource(mRepository.muteLocal(micIndex))
    }

    fun unMuteLocal(micIndex: Int) {
        _unMuteMicObservable.setSource(mRepository.unMuteLocal(micIndex))
    }

    fun leaveMic(micIndex: Int) {
        _leaveMicObservable.setSource(mRepository.leaveMic(micIndex))
    }

    fun forbidMic(micIndex: Int) {
        _forbidMicObservable.setSource(mRepository.forbidMic(micIndex))
    }

    fun cancelMuteMic(micIndex: Int) {
        _cancelForbidMicObservable.setSource(mRepository.unForbidMic(micIndex))
    }

    fun kickOff(micIndex: Int) {
        _kickMicObservable.setSource(mRepository.kickOff(micIndex))
    }

    fun acceptMicSeatInvitation(micIndex: Int) {
        _acceptMicSeatInvitationObservable.setSource(mRepository.acceptMicSeatInvitation(micIndex))
    }

    fun refuseInvite() {
        _rejectMicInvitationObservable.setSource(mRepository.refuseInvite())
    }

    fun lockMic(micIndex: Int) {
        _lockMicObservable.setSource(mRepository.lockMic(micIndex))
    }

    fun unLockMic(micIndex: Int) {
        _cancelLockMicObservable.setSource(mRepository.unLockMic(micIndex))
    }

    fun startMicSeatApply(micIndex: Int?) {
        _startMicSeatApplyObservable.setSource(mRepository.startMicSeatApply(micIndex))
    }

    fun cancelMicSeatApply(chatroomId: String, chatUid: String) {
        _cancelMicSeatApplyObservable.setSource(mRepository.cancelMicSeatApply(chatroomId, chatUid))
    }

    fun changeMic(oldIndex: Int, newIndex: Int) {
        _changeMicObservable.setSource(mRepository.changeMic(oldIndex, newIndex))
    }

    fun leaveSyncManagerRoom() {
        _leaveSyncRoomObservable.setSource(mRepository.leaveSyncManagerRoom())
    }

    fun updateRoomMember(){
        _updateRoomMemberObservable.setSource(mRepository.updateRoomMember())
    }

    fun renewChatToken() {
        VoiceToolboxServerHttpManager.createImRoom(
            roomName = "",
            roomOwner = "",
            chatroomId = "",
            type = 1,
            callBack = object : VRValueCallBack<VRCreateRoomResponse> {
                override fun onSuccess(response: VRCreateRoomResponse?) {
                    LogTools.d("renewChatToken", "renewChatToken getToken success:${response}")
                    response?.chatToken?.let {
                        VoiceBuddyFactory.get().getVoiceBuddy().setupChatToken(it)
                        ChatroomIMManager.getInstance().renewToken(it)
                    }
                }

                override fun onError(code: Int, message: String?) {
                    LogTools.d("renewChatToken", "renewChatToken getToken error:$code + $message")
                }
            })
    }
}