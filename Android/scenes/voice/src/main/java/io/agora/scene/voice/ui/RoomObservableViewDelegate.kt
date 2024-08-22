package io.agora.scene.voice.ui

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.CompoundButton
import androidx.fragment.app.FragmentActivity
import com.google.gson.reflect.TypeToken
import io.agora.CallBack
import io.agora.scene.base.component.OnItemClickListener
import io.agora.scene.voice.R
import io.agora.scene.voice.global.VoiceBuddyFactory
import io.agora.scene.voice.imkit.bean.ChatMessageData
import io.agora.scene.voice.imkit.manager.ChatroomCacheManager
import io.agora.scene.voice.imkit.manager.ChatroomIMManager
import io.agora.scene.voice.model.*
import io.agora.scene.voice.model.annotation.MicClickAction
import io.agora.scene.voice.model.annotation.MicStatus
import io.agora.scene.voice.model.constructor.RoomInfoConstructor
import io.agora.scene.voice.model.constructor.RoomSoundAudioConstructor
import io.agora.scene.voice.model.constructor.RoomSoundSelectionConstructor
import io.agora.scene.voice.rtckit.AgoraRtcEngineController
import io.agora.scene.voice.rtckit.listener.RtcMicVolumeListener
import io.agora.scene.voice.ui.activity.ChatroomLiveActivity
import io.agora.scene.voice.ui.dialog.*
import io.agora.scene.voice.ui.dialog.common.CommonFragmentAlertDialog
import io.agora.scene.voice.ui.dialog.common.CommonFragmentContentDialog
import io.agora.scene.voice.ui.dialog.common.CommonSheetAlertDialog
import io.agora.scene.voice.ui.dialog.soundcard.SoundCardSettingDialog
import io.agora.scene.voice.ui.dialog.soundcard.SoundPresetTypeDialog
import io.agora.scene.voice.ui.widget.mic.IRoomMicView
import io.agora.scene.voice.ui.widget.primary.ChatPrimaryMenuView
import io.agora.scene.voice.ui.widget.top.IRoomLiveTopView
import io.agora.scene.voice.viewmodel.VoiceRoomLivingViewModel
import io.agora.util.EMLog
import io.agora.voice.common.constant.ConfigConstants
import io.agora.voice.common.net.OnResourceParseCallback
import io.agora.voice.common.net.Resource
import io.agora.voice.common.ui.IParserSource
import io.agora.voice.common.utils.GsonTools
import io.agora.voice.common.utils.LogTools.logD
import io.agora.voice.common.utils.ThreadManager
import io.agora.voice.common.utils.ToastTools


class RoomObservableViewDelegate constructor(
    private val activity: FragmentActivity,
    private val roomLivingViewModel: VoiceRoomLivingViewModel,
    private var voiceRoomModel: VoiceRoomModel,
    private val iRoomTopView: IRoomLiveTopView,
    private val iRoomMicView: IRoomMicView,
    private val chatPrimaryMenuView: ChatPrimaryMenuView,
) : IParserSource {
    companion object {
        private const val TAG = "RoomObservableDelegate"
    }

    /**麦位信息，index,rtcUid*/
    private val micMap = mutableMapOf<Int, Int>()

    private var localUserMicInfo: VoiceMicInfoModel? = null

    /**举手dialog*/
    private var handsDialog: ChatroomHandsDialog? = null

    /**申请上麦标志*/
    private var isRequesting: Boolean = false

    private fun localUserIndex(): Int {
        return localUserMicInfo?.micIndex ?: -1
    }

    private var robotDialog: RoomRobotEnableDialog? = null

    init {
        roomLivingViewModel.roomNoticeObservable().observe(activity) { response: Resource<Pair<String, Boolean>> ->
            parseResource(response, object : OnResourceParseCallback<Pair<String, Boolean>>() {
                override fun onSuccess(data: Pair<String, Boolean>?) {
                    if (data?.second != true) return
                    voiceRoomModel.announcement = data.first
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_notice_posted))
                }

                override fun onError(code: Int, message: String?) {
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_notice_posted_error))
                }
            })
        }
        roomLivingViewModel.openBotObservable().observe(activity) { response: Resource<Boolean> ->
            parseResource(response, object : OnResourceParseCallback<Boolean>() {
                override fun onSuccess(data: Boolean?) {
                    "robot open：$data".logD()
                    if (data != true) return
                    iRoomMicView.activeBot(true)
                    voiceRoomModel.useRobot = true
                    roomAudioSettingDialog?.apply {
                        audioSettingsInfo.botOpen = true
                        updateBotStateView()
                    }
                    if (VoiceBuddyFactory.get().rtcChannelTemp.firstActiveBot) {
                        VoiceBuddyFactory.get().rtcChannelTemp.firstActiveBot = false
                        AgoraRtcEngineController.get()
                            .updateEffectVolume(voiceRoomModel.robotVolume)
                        RoomSoundAudioConstructor.createRoomSoundAudioMap[ConfigConstants.RoomType.Common_Chatroom]?.let {
                            AgoraRtcEngineController.get().playMusic(it)
                        }
                    }
                }
            })
        }
        roomLivingViewModel.closeBotObservable().observe(activity) { response: Resource<Boolean> ->
            parseResource(response, object : OnResourceParseCallback<Boolean>() {
                override fun onSuccess(data: Boolean?) {
                    "robot close：$data".logD()
                    if (data != true) return
                    iRoomMicView.activeBot(false)
                    voiceRoomModel.useRobot = false
                    AgoraRtcEngineController.get().resetMediaPlayer()
                }
            })
        }
        roomLivingViewModel.robotVolumeObservable().observe(activity) { response: Resource<Pair<Int, Boolean>> ->
            parseResource(response, object : OnResourceParseCallback<Pair<Int, Boolean>>() {
                override fun onSuccess(data: Pair<Int, Boolean>?) {
                    "robotVolume update：$data".logD()
                    data?.let {
                        if (it.second) {
                            voiceRoomModel.robotVolume = it.first
                            AgoraRtcEngineController.get().updateEffectVolume(it.first)
                        }
                    }
                }
            })
        }
        AgoraRtcEngineController.get().setMicVolumeListener(object : RtcMicVolumeListener() {
            override fun onBotVolume(speaker: Int, finished: Boolean) {
                if (finished) {
                    iRoomMicView.updateBotVolume(speaker, ConfigConstants.VolumeType.Volume_None)
                } else {
                    iRoomMicView.updateBotVolume(speaker, ConfigConstants.VolumeType.Volume_Medium)
                }
            }

            override fun onUserVolume(rtcUid: Int, volume: Int) {
                if (rtcUid == 0) {
                    val myselfIndex = localUserIndex()
                    if (myselfIndex >= 0) {
                        iRoomMicView.updateVolume(myselfIndex, volume)
                    }
                } else {
                    val micIndex = findIndexByRtcUid(rtcUid)
                    if (micIndex >= 0) {
                        iRoomMicView.updateVolume(micIndex, volume)
                    }
                }
            }
        })
        roomLivingViewModel.startMicSeatApplyObservable().observe(activity) { result: Resource<Boolean> ->
            parseResource(result, object : OnResourceParseCallback<Boolean>() {
                override fun onSuccess(data: Boolean?) {
                    "start mic seat apply:$data".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_submit_sent))
                    chatPrimaryMenuView.setShowHandStatus(false, true)
                    isRequesting = true
                }
            })
        }
        roomLivingViewModel.cancelMicSeatApplyObservable().observe(activity) { result: Resource<Boolean> ->
            parseResource(result, object : OnResourceParseCallback<Boolean>() {
                override fun onSuccess(data: Boolean?) {
                    "cancel mic seat apply:$data".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_cancel_apply_success))
                    chatPrimaryMenuView.setShowHandStatus(false, false)
                    isRequesting = false
                }
            })
        }
        roomLivingViewModel.muteMicObservable().observe(activity) { response: Resource<VoiceMicInfoModel> ->
            parseResource(response, object : OnResourceParseCallback<VoiceMicInfoModel>() {
                override fun onSuccess(data: VoiceMicInfoModel?) {
                    "mute mic：${data?.micIndex}".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_muted))
                    data?.let {
                        val newMicMap = mutableMapOf(it.micIndex to it)
                        dealMicDataMap(newMicMap)
                        updateViewByMicMap(newMicMap)
                    }
                }
            })
        }
        roomLivingViewModel.unMuteMicObservable().observe(activity) { response: Resource<VoiceMicInfoModel> ->
            parseResource(response, object : OnResourceParseCallback<VoiceMicInfoModel>() {
                override fun onSuccess(data: VoiceMicInfoModel?) {
                    "cancel mute mic：${data?.micIndex}".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_unmuted))
                    data?.let {
                        val newMicMap = mutableMapOf(it.micIndex to it)
                        dealMicDataMap(newMicMap)
                        updateViewByMicMap(newMicMap)
                    }
                }
            })
        }
        roomLivingViewModel.leaveMicObservable().observe(activity) { response: Resource<VoiceMicInfoModel> ->
            parseResource(response, object : OnResourceParseCallback<VoiceMicInfoModel>() {
                override fun onSuccess(data: VoiceMicInfoModel?) {
                    "leave mic：${data?.micIndex}".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_off_stage))
                    data?.let {
                        val newMicMap = mutableMapOf(it.micIndex to it)
                        dealMicDataMap(newMicMap)
                        updateViewByMicMap(newMicMap)
                    }
                }
            })
        }
        roomLivingViewModel.kickMicObservable().observe(activity) { response: Resource<VoiceMicInfoModel> ->
            parseResource(response, object : OnResourceParseCallback<VoiceMicInfoModel>() {
                override fun onSuccess(data: VoiceMicInfoModel?) {
                    "kick mic：${data?.micIndex}".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_kicked_off))
                    data?.let {
                        val newMicMap = mutableMapOf(it.micIndex to it)
                        dealMicDataMap(newMicMap)
                        updateViewByMicMap(newMicMap)
                    }
                }
            })
        }
        roomLivingViewModel.forbidMicObservable().observe(activity) { response: Resource<VoiceMicInfoModel> ->
            parseResource(response, object : OnResourceParseCallback<VoiceMicInfoModel>() {
                override fun onSuccess(data: VoiceMicInfoModel?) {
                    "force mute mic：${data?.micIndex}".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_muted))
                    data?.let {
                        val newMicMap = mutableMapOf(it.micIndex to it)
                        dealMicDataMap(newMicMap)
                        updateViewByMicMap(newMicMap)
                    }
                }
            })
        }
        roomLivingViewModel.cancelForbidMicObservable().observe(activity) { response: Resource<VoiceMicInfoModel> ->
            parseResource(response, object : OnResourceParseCallback<VoiceMicInfoModel>() {
                override fun onSuccess(data: VoiceMicInfoModel?) {
                    "cancel force mute mic：${data?.micIndex}".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_unmuted))
                    data?.let {
                        val newMicMap = mutableMapOf(it.micIndex to it)
                        dealMicDataMap(newMicMap)
                        updateViewByMicMap(newMicMap)
                    }
                }
            })
        }
        roomLivingViewModel.lockMicObservable().observe(activity) { response: Resource<VoiceMicInfoModel> ->
            parseResource(response, object : OnResourceParseCallback<VoiceMicInfoModel>() {
                override fun onSuccess(data: VoiceMicInfoModel?) {
                    "lock mic：${data?.micIndex}".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_blocked))
                    data?.let {
                        val newMicMap = mutableMapOf(it.micIndex to it)
                        dealMicDataMap(newMicMap)
                        updateViewByMicMap(newMicMap)
                    }
                }
            })
        }
        roomLivingViewModel.cancelLockMicObservable().observe(activity) { response: Resource<VoiceMicInfoModel> ->
            parseResource(response, object : OnResourceParseCallback<VoiceMicInfoModel>() {
                override fun onSuccess(data: VoiceMicInfoModel?) {
                    "cancel lock mic：${data?.micIndex}".logD()
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_unblocked))
                    data?.let {
                        val newMicMap = mutableMapOf(it.micIndex to it)
                        dealMicDataMap(newMicMap)
                        updateViewByMicMap(newMicMap)
                    }
                }
            })
        }
        roomLivingViewModel.rejectMicInvitationObservable().observe(activity) { response: Resource<Boolean> ->
            parseResource(response, object : OnResourceParseCallback<Boolean>() {
                override fun onSuccess(data: Boolean?) {
                    "reject mic invitation：$data".logD()
                }
            })
        }
        roomLivingViewModel.acceptMicSeatInvitationObservable()
            .observe(activity) { response: Resource<VoiceMicInfoModel> ->
                parseResource(response, object : OnResourceParseCallback<VoiceMicInfoModel>() {
                    override fun onSuccess(data: VoiceMicInfoModel?) {
                        data?.let {
                            val newMicMap = mutableMapOf(it.micIndex to it)
                            dealMicDataMap(newMicMap)
                            updateViewByMicMap(newMicMap)
                        }
                    }
                })
            }
        roomLivingViewModel.changeMicObservable().observe(activity) { response: Resource<Map<Int, VoiceMicInfoModel>> ->
            parseResource(response, object : OnResourceParseCallback<Map<Int, VoiceMicInfoModel>>() {
                override fun onSuccess(data: Map<Int, VoiceMicInfoModel>?) {
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_exchange_mic_success))
                    data?.let {
                        dealMicDataMap(it)
                        updateViewByMicMap(it)
                    }
                }

                override fun onError(code: Int, message: String?) {
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_exchange_mic_failed))
                }
            })
        }
    }

    private fun findIndexByRtcUid(rtcUid: Int): Int {
        micMap.entries.forEach {
            if (it.value == rtcUid) {
                return it.key
            }
        }
        return -1
    }

    /**
     * 房间详情
     */
    fun onRoomDetails(voiceRoomInfo: VoiceRoomInfo) {
        voiceRoomInfo.roomInfo?.let { vRoomInfo ->
            this.voiceRoomModel = vRoomInfo
            iRoomTopView.onChatroomInfo(vRoomInfo)
        }
        if (!voiceRoomInfo.micInfo.isNullOrEmpty()) {
            voiceRoomInfo.micInfo?.let { micList ->
                val micInfoList: List<VoiceMicInfoModel> =
                    RoomInfoConstructor.extendMicInfoList(micList, voiceRoomModel.owner?.userId ?: "")
                micInfoList.forEach { micInfo ->
                    micInfo.member?.let { userInfo ->
                        val rtcUid = userInfo.rtcUid
                        val micIndex = micInfo.micIndex
                        if (rtcUid > 0) {
                            if (rtcUid == VoiceBuddyFactory.get().getVoiceBuddy().rtcUid()) {
                                localUserMicInfo = micInfo
                            }
                            micMap[micIndex] = rtcUid
                        }
                    }
                }
                iRoomMicView.onInitMic(micInfoList, voiceRoomModel.useRobot)
            }
        }
        val isOn = (localUserMicInfo?.member?.micStatus == 1 &&
                localUserMicInfo?.micStatus == MicStatus.Normal)
        val onStage = localUserIndex() >= 0
        chatPrimaryMenuView.showMicVisible(onStage, isOn)
        AgoraRtcEngineController.get().earBackManager()?.setForbidden(!onStage)
        AgoraRtcEngineController.get().soundCardManager()?.setForbidden(!onStage)
    }

    /**
     * 排行榜
     */
    fun onClickRank(currentItem: Int = 0) {
        val dialog = RoomContributionAndAudienceSheetDialog().apply {
            arguments = Bundle().apply {
                putSerializable(RoomContributionAndAudienceSheetDialog.KEY_ROOM_KIT_BEAN, voiceRoomModel)
                putInt(RoomContributionAndAudienceSheetDialog.KEY_CURRENT_ITEM, currentItem)
            }
        }
        dialog.show(
            activity.supportFragmentManager, "ContributionAndAudienceSheetDialog"
        )
    }

    /**
     * 公告
     */
    fun onClickNotice() {
        var announcement = voiceRoomModel.announcement
        if (announcement.isNullOrEmpty()) {
            announcement = activity.getString(R.string.voice_voice_voice_chatroom_first_enter_room_notice_tips)
        }
        val roomNoticeDialog = RoomNoticeSheetDialog().contentText(announcement).apply {
            arguments = Bundle().apply {
                putSerializable(RoomNoticeSheetDialog.KEY_ROOM_KIT_BEAN, voiceRoomModel)
            }
        }
        roomNoticeDialog.confirmCallback = { newNotice ->
            roomLivingViewModel.updateAnnouncement(newNotice)
        }
        roomNoticeDialog.show(activity.supportFragmentManager, "roomNoticeSheetDialog")
    }

    fun onClickSoundSocial(soundSelection: Int, finishBack: () -> Unit) {
        val curSoundSelection = RoomSoundSelectionConstructor.builderCurSoundSelection(activity, soundSelection)
        val socialDialog = RoomSocialChatSheetDialog().titleText(curSoundSelection.soundName)
            .contentText(curSoundSelection.soundIntroduce).customers(mutableListOf())
        socialDialog.onClickSocialChatListener = object : RoomSocialChatSheetDialog.OnClickSocialChatListener {

            override fun onMoreSound() {
                onSoundSelectionDialog(voiceRoomModel.soundEffect, finishBack)
            }
        }
        socialDialog.show(activity.supportFragmentManager, "chatroomSocialChatSheetDialog")
    }

    var roomAudioSettingDialog: RoomAudioSettingsSheetDialog? = null

    fun onAudioSettingsDialog(finishBack: () -> Unit) {
        roomAudioSettingDialog = RoomAudioSettingsSheetDialog().apply {
            arguments = Bundle().apply {
                val audioSettingsInfo = RoomAudioSettingsBean(
                    enable = voiceRoomModel.isOwner,
                    roomType = ConfigConstants.RoomType.Common_Chatroom,
                    botOpen = voiceRoomModel.useRobot,
                    botVolume = voiceRoomModel.robotVolume,
                    soundSelection = voiceRoomModel.soundEffect,
                    AINSMode = VoiceBuddyFactory.get().rtcChannelTemp.AINSMode,
                    isAIAECOn = VoiceBuddyFactory.get().rtcChannelTemp.isAIAECOn,
                    isAIAGCOn = VoiceBuddyFactory.get().rtcChannelTemp.isAIAGCOn,
                    spatialOpen = false
                )
                putSerializable(RoomAudioSettingsSheetDialog.KEY_AUDIO_SETTINGS_INFO, audioSettingsInfo)
            }
        }

        roomAudioSettingDialog?.audioSettingsListener =
            object : RoomAudioSettingsSheetDialog.OnClickAudioSettingsListener {

                override fun onAINS(mode: Int, isEnable: Boolean) {
                    onAINSDialog(mode)
                }

                override fun onAIAEC(isOn: Boolean, isEnable: Boolean) {
                    onAIAECDialog(isOn)
                }

                override fun onAGC(isOn: Boolean, isEnable: Boolean) {
                    onAIAGCDialog(isOn)
                }

                override fun onEarBackSetting() {
                    onEarBackSettingDialog()
                }

                override fun onVirtualSoundCardSetting() {
                    onVirtualSoundCardSettingDialog()
                }

                override fun onBotCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
                    roomLivingViewModel.enableRobot(isChecked)
                }

                override fun onBotVolumeChange(progress: Int) {
                    roomLivingViewModel.updateBotVolume(progress)
                }

                override fun onSoundEffect(soundSelectionType: Int, isEnable: Boolean) {
                    onSoundSelectionDialog(soundSelectionType, finishBack)
                }
            }
        roomAudioSettingDialog?.show(activity.supportFragmentManager, "mtAudioSettings")
    }

    fun onSoundSelectionDialog(soundSelection: Int, finishBack: () -> Unit) {
        RoomSoundSelectionSheetDialog(
            voiceRoomModel.isOwner,
            object : RoomSoundSelectionSheetDialog.OnClickSoundSelectionListener {
                override fun onSoundEffect(soundSelection: SoundSelectionBean, isCurrentUsing: Boolean) {
                    if (isCurrentUsing) {
                        if (voiceRoomModel.useRobot) {
                            RoomSoundAudioConstructor.soundSelectionAudioMap[soundSelection.soundSelectionType]?.let {
                                AgoraRtcEngineController.get().playMusic(it)
                            }
                        } else {
                            onBotMicClick(
                                activity.getString(R.string.voice_chatroom_open_bot_to_sound_effect),
                                finishBack
                            )
                        }
                    } else {
                        onExitRoom(
                            activity.getString(R.string.voice_chatroom_prompt),
                            activity.getString(R.string.voice_chatroom_exit_and_create_one),
                            finishBack
                        )
                    }
                }

            }).apply {
            arguments = Bundle().apply {
                putInt(RoomSoundSelectionSheetDialog.KEY_CURRENT_SELECTION, soundSelection)
            }
        }.show(activity.supportFragmentManager, "mtSoundSelection")
    }

    fun onAINSDialog(ainsMode: Int) {
        val ainsDialog = RoomAINSSheetDialog().apply {
            arguments = Bundle().apply {
                putInt(RoomAINSSheetDialog.KEY_AINS_MODE, ainsMode)
                putBoolean(RoomAINSSheetDialog.KEY_IS_ENABLE, voiceRoomModel.isOwner)
            }
        }
        ainsDialog.anisModeCallback = {
            VoiceBuddyFactory.get().rtcChannelTemp.AINSMode = it.anisMode
            AgoraRtcEngineController.get().deNoise(it.anisMode)
            roomAudioSettingDialog?.apply {
                audioSettingsInfo.AINSMode = it.anisMode
                updateAINSView()
            }
            if (voiceRoomModel.isOwner && voiceRoomModel.useRobot && VoiceBuddyFactory.get().rtcChannelTemp.firstSwitchAnis) {
                VoiceBuddyFactory.get().rtcChannelTemp.firstSwitchAnis = false
                RoomSoundAudioConstructor.anisIntroduceAudioMap[it.anisMode]?.let { soundAudioList ->
                    AgoraRtcEngineController.get().playMusic(soundAudioList)
                }
            }
        }
        ainsDialog.anisSoundCallback = { position, ainsSoundBean ->
            "onAINSDialog anisSoundCallback：$ainsSoundBean".logD(TAG)
            val playSound = {
                ainsDialog.updateAnisSoundsAdapter(position, true)
                RoomSoundAudioConstructor.AINSSoundMap[ainsSoundBean.soundType]?.let { soundAudioBean ->
                    val audioUrl =
                        if (ainsSoundBean.soundMode == ConfigConstants.AINSMode.AINS_High) soundAudioBean.audioUrlHigh else soundAudioBean.audioUrl
                    AgoraRtcEngineController.get()
                        .playMusic(soundAudioBean.soundId, audioUrl, soundAudioBean.speakerType)
                }
            }
            if (voiceRoomModel.useRobot) {
                playSound.invoke()
            } else {
                CommonFragmentAlertDialog().titleText(activity.getString(R.string.voice_chatroom_prompt))
                    .contentText(activity.getString(R.string.voice_chatroom_open_bot_to_sound_effect))
                    .leftText(activity.getString(R.string.voice_room_cancel))
                    .rightText(activity.getString(R.string.voice_room_confirm))
                    .setOnClickListener(object : CommonFragmentAlertDialog.OnClickBottomListener {
                        override fun onConfirmClick() {
                            VoiceBuddyFactory.get().rtcChannelTemp.firstActiveBot = false
                            roomLivingViewModel.enableRobot(true)
                            roomAudioSettingDialog?.apply {
                                audioSettingsInfo.botOpen = true
                                updateBotStateView()
                            }
                            playSound.invoke()
                        }
                    }).show(activity.supportFragmentManager, "botActivatedDialog")
            }
        }
        ainsDialog.show(activity.supportFragmentManager, "mtAnis")
    }

    fun onAIAECDialog(isOn: Boolean) {
        val dialog = RoomAIAECSheetDialog().apply {
            arguments = Bundle().apply {
                putBoolean(RoomAIAECSheetDialog.KEY_IS_ON, isOn)
            }
        }
        dialog.onClickCheckBox = { isOn ->
            AgoraRtcEngineController.get().setAIAECOn(isOn)
            VoiceBuddyFactory.get().rtcChannelTemp.isAIAECOn = isOn
            roomAudioSettingDialog?.apply {
                audioSettingsInfo.isAIAECOn = isOn
                updateAIAECView()
            }
        }
        dialog.show(activity.supportFragmentManager, "mtAIAEC")
    }

    fun onAIAGCDialog(isOn: Boolean) {
        val dialog = RoomAIAGCSheetDialog().apply {
            arguments = Bundle().apply {
                putBoolean(RoomAIAGCSheetDialog.KEY_IS_ON, isOn)
            }
        }
        dialog.onClickCheckBox = { isOn ->
            AgoraRtcEngineController.get().setAIAGCOn(isOn)
            VoiceBuddyFactory.get().rtcChannelTemp.isAIAGCOn = isOn
            roomAudioSettingDialog?.audioSettingsInfo?.isAIAGCOn = isOn
            roomAudioSettingDialog?.updateAIAGCView()
        }
        dialog.show(activity.supportFragmentManager, "mtAIAGC")
    }

    fun onEarBackSettingDialog() {
        if (AgoraRtcEngineController.get().earBackManager()?.params?.isForbidden == true) {
            ToastTools.showTips(activity, activity.getString(R.string.voice_chatroom_settings_earback_forbidden_toast))
            return
        }
        val dialog = RoomEarBackSettingSheetDialog()
        dialog.setFragmentManager(activity.supportFragmentManager)
        dialog.setOnEarBackStateChange {
            roomAudioSettingDialog?.updateEarBackState()
        }
        dialog.show(activity.supportFragmentManager, "mtBGMSetting")
    }

    fun onVirtualSoundCardSettingDialog() {
        if (AgoraRtcEngineController.get().soundCardManager()?.isForbidden() == true) {
            ToastTools.showTips(activity, activity.getString(R.string.voice_settings_sound_card_forbidden_toast))
            return
        }
        val dialog = SoundCardSettingDialog()
        dialog.onClickSoundCardType = {
            val preset = SoundPresetTypeDialog()
            preset.mOnSoundTypeChange = {
                dialog.updateView()
            }
            preset.show(activity.supportFragmentManager, SoundPresetTypeDialog.TAG)
        }
        dialog.onSoundCardStateChange = {
            roomAudioSettingDialog?.updateSoundCardState()
        }
        dialog.show(activity.supportFragmentManager, SoundCardSettingDialog.TAG)
    }

    fun onExitRoom(title: String, content: String, finishBack: () -> Unit) {
        CommonFragmentAlertDialog().titleText(title).contentText(content)
            .leftText(activity.getString(R.string.voice_room_cancel))
            .rightText(activity.getString(R.string.voice_room_confirm))
            .setOnClickListener(object : CommonFragmentAlertDialog.OnClickBottomListener {
                override fun onConfirmClick() {
                    finishBack.invoke()
                }
            }).show(activity.supportFragmentManager, "mtCenterDialog")
    }

    fun onTimeUpExitRoom(content: String, finishBack: () -> Unit) {
        if (activity.isFinishing) {
            return
        }
        CommonFragmentContentDialog().contentText(content)
            .setOnClickListener(object : CommonFragmentContentDialog.OnClickBottomListener {
                override fun onConfirmClick() {
                    finishBack.invoke()
                }
            }).show(activity.supportFragmentManager, "mtTimeOutDialog")
    }

    private var lastUserMicClick: Long = 0
    fun onUserMicClick(micInfo: VoiceMicInfoModel) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUserMicClick < 500) {
            return
        }
        lastUserMicClick = currentTime

        val isMyself = TextUtils.equals(VoiceBuddyFactory.get().getVoiceBuddy().userId(), micInfo.member?.userId)
        if (voiceRoomModel.isOwner || isMyself) {
            val roomMicMangerDialog = RoomMicManagerSheetDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(RoomMicManagerSheetDialog.KEY_MIC_INFO, micInfo)
                    putSerializable(RoomMicManagerSheetDialog.KEY_IS_OWNER, voiceRoomModel.isOwner)
                    putSerializable(RoomMicManagerSheetDialog.KEY_IS_MYSELF, isMyself)
                }
            }
            roomMicMangerDialog.onItemClickListener = object : OnItemClickListener<MicManagerBean> {
                override fun onItemClick(data: MicManagerBean, view: View, position: Int, viewType: Long) {
                    when (data.micClickAction) {
                        MicClickAction.Invite -> {
                            if (data.enable) {
                                showOwnerHandsDialog(micInfo.micIndex)
                            } else {
                                ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_close_by_host))
                            }
                        }

                        MicClickAction.ForbidMic -> {
                            roomLivingViewModel.forbidMic(micInfo.micIndex)
                        }

                        MicClickAction.UnForbidMic -> {
                            if (data.enable) {
                                roomLivingViewModel.cancelMuteMic(micInfo.micIndex)
                            } else {
                                ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_close_by_host))
                            }
                        }

                        MicClickAction.Mute -> {
                            muteLocalAudio(true, micInfo.micIndex)
                        }

                        MicClickAction.UnMute -> {
                            if (activity is ChatroomLiveActivity) {
                                activity.toggleSelfAudio(true, callback = {
                                    muteLocalAudio(false, micInfo.micIndex)
                                })
                            }
                        }

                        MicClickAction.Lock -> {
                            roomLivingViewModel.lockMic(micInfo.micIndex)
                        }

                        MicClickAction.UnLock -> {
                            roomLivingViewModel.unLockMic(micInfo.micIndex)
                        }

                        MicClickAction.KickOff -> {
                            roomLivingViewModel.kickOff(micInfo.micIndex)
                        }

                        MicClickAction.OffStage -> {
                            roomLivingViewModel.leaveMic(micInfo.micIndex)
                        }
                    }
                }
            }
            roomMicMangerDialog.show(activity.supportFragmentManager, "RoomMicManagerSheetDialog")
        } else if (micInfo.micStatus == MicStatus.Lock || micInfo.micStatus == MicStatus.LockForceMute) {
            ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_close_by_host))
        } else if ((micInfo.micStatus == MicStatus.Idle || micInfo.micStatus == MicStatus.ForceMute) && micInfo.member == null) {
            val mineMicIndex = iRoomMicView.findMicByUid(VoiceBuddyFactory.get().getVoiceBuddy().userId())
            if (mineMicIndex > 0) {
                roomLivingViewModel.changeMic(mineMicIndex, micInfo.micIndex)
//                showAlertDialog(activity.getString(R.string.voice_chatroom_exchange_mic),
//                    object : CommonSheetAlertDialog.OnClickBottomListener {
//                        override fun onConfirmClick() {
//                            roomLivingViewModel.changeMic(mineMicIndex, micInfo.micIndex)
//                        }
//                    })
            } else {
                if (isRequesting) {
                    ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_submit_sent))
                } else {
                    showMemberHandsDialog(micInfo.micIndex)
                }
            }
        }
    }

    fun onBotMicClick(content: String, finishBack: () -> Unit) {
        if (voiceRoomModel.isOwner) {
            if (!voiceRoomModel.useRobot) {
                CommonFragmentAlertDialog().titleText(activity.getString(R.string.voice_chatroom_prompt))
                    .contentText(content).leftText(activity.getString(R.string.voice_room_cancel))
                    .rightText(activity.getString(R.string.voice_room_confirm))
                    .setOnClickListener(object : CommonFragmentAlertDialog.OnClickBottomListener {
                        override fun onConfirmClick() {
                            roomLivingViewModel.enableRobot(true)
                        }
                    }).show(activity.supportFragmentManager, "botActivatedDialog")
            } else {
                // nothing
                RoomRobotEnableDialog(object : RoomRobotEnableDialog.OnClickBtnListener {
                    override fun onClickCloseBtn() {
                        roomLivingViewModel.enableRobot(false)
                    }

                    override fun onClickSettingBtn() {
                        onAudioSettingsDialog(finishBack)
                    }
                }).apply {
                    robotDialog = this
                }.show(activity.supportFragmentManager, "mtClickRobotDialog")
            }
        } else {
            ToastTools.showTips(activity, activity.getString(R.string.voice_chatroom_only_host_can_change_robot))
        }
    }

    private fun showAlertDialog(content: String, onClickListener: CommonSheetAlertDialog.OnClickBottomListener) {
        CommonSheetAlertDialog().contentText(content).rightText(activity.getString(R.string.voice_room_confirm))
            .leftText(activity.getString(R.string.voice_room_cancel)).setOnClickListener(onClickListener)
            .show(activity.supportFragmentManager, "CommonSheetAlertDialog")
    }

    fun muteLocalAudio(mute: Boolean, index: Int = -1) {
        AgoraRtcEngineController.get().enableLocalAudio(!mute)
        val micIndex = if (index < 0) localUserIndex() else index
        if (mute) {
            roomLivingViewModel.muteLocal(micIndex)
        } else {
            roomLivingViewModel.unMuteLocal(micIndex)
        }
    }

    fun onSendGiftSuccess(roomId: String, message: ChatMessageData?) {
        val voiceGiftModel = ChatroomIMManager.getInstance().getGiftModel(message)
        val count = voiceGiftModel.gift_count?.toIntOrNull() ?: 0
        val price = voiceGiftModel.gift_price?.toIntOrNull() ?: 0
        val amount = count * price
        ChatroomIMManager.getInstance()
            .updateRankList(VoiceBuddyFactory.get().getVoiceBuddy().chatUserName(), voiceGiftModel, object : CallBack {
                override fun onSuccess() {
                    ThreadManager.getInstance().runOnMainThread {
                        iRoomTopView.onRankMember(ChatroomIMManager.getInstance().rankList)
                    }
                    EMLog.d(TAG, "onSendGiftSuccess updateAmount success")
                }

                override fun onError(code: Int, error: String?) {
                    EMLog.d(TAG, "onSendGiftSuccess updateAmount error$code $error")
                }
            })
        ChatroomIMManager.getInstance()
            .updateAmount(VoiceBuddyFactory.get().getVoiceBuddy().chatUserName(), amount, object : CallBack {
                override fun onSuccess() {
                    ThreadManager.getInstance().runOnMainThread {
                        iRoomTopView.onUpdateGiftCount(ChatroomIMManager.getInstance().giftAmountCache)
                    }
                    EMLog.d(TAG, "onSendGiftSuccess updateAmount success")
                }

                override fun onError(code: Int, error: String) {
                    EMLog.d(TAG, "onSendGiftSuccess updateAmount error$code $error")
                }
            })
    }

    // onUserJoinedRoom clickCount+1
    fun onUserJoinedRoom() {
        ChatroomIMManager.getInstance().increaseClickCount(VoiceBuddyFactory.get().getVoiceBuddy().chatUserName(),
            object : CallBack {
                override fun onSuccess() {
                    ThreadManager.getInstance().runOnMainThread {
                        iRoomTopView.onUpdateWatchCount(ChatroomIMManager.getInstance().clickCountCache)
                    }
                    EMLog.d(TAG, "increaseClickCount success")
                }

                override fun onError(code: Int, error: String) {
                    EMLog.d(TAG, "increaseClickCount error$code $error")
                }
            })
    }

    fun receiveGift(roomId: String, message: ChatMessageData?) {
        val voiceGiftModel = ChatroomIMManager.getInstance().getGiftModel(message)
        val count = voiceGiftModel.gift_count?.toIntOrNull() ?: 0
        val price = voiceGiftModel.gift_price?.toIntOrNull() ?: 0
        val amount = count * price
        ChatroomIMManager.getInstance()
            .updateAmount(VoiceBuddyFactory.get().getVoiceBuddy().chatUserName(), amount, object : CallBack {
                override fun onSuccess() {
                    ThreadManager.getInstance().runOnMainThread {
                        iRoomTopView.onUpdateGiftCount(ChatroomIMManager.getInstance().giftAmountCache)
                    }
                    EMLog.d(TAG, "receiveGift updateAmount success")
                }

                override fun onError(code: Int, error: String) {
                    EMLog.d(TAG, "receiveGift updateAmount error$code $error")
                }
            })
    }

    fun receiveInviteSite(roomId: String, micIndex: Int) {
        CommonFragmentAlertDialog().contentText(activity.getString(R.string.voice_chatroom_mic_anchor_invited_you_on_stage))
            .leftText(activity.getString(R.string.voice_room_decline))
            .rightText(activity.getString(R.string.voice_room_accept))
            .setOnClickListener(object : CommonFragmentAlertDialog.OnClickBottomListener {
                override fun onConfirmClick() {

                    if (activity is ChatroomLiveActivity) {
                        activity.toggleSelfAudio(true, callback = {
                            roomLivingViewModel.acceptMicSeatInvitation(micIndex)
                        })
                    }
                }

                override fun onCancelClick() {
                    roomLivingViewModel.refuseInvite()
                }
            }).show(activity.supportFragmentManager, "CommonFragmentAlertDialog")
    }

    fun destroy() {
        AgoraRtcEngineController.get().destroy()
    }

    fun showOwnerHandsDialog(inviteIndex: Int) {
        handsDialog = activity.supportFragmentManager.findFragmentByTag("room_hands") as ChatroomHandsDialog?
        if (handsDialog == null) {
            handsDialog = ChatroomHandsDialog.newInstance
        }
        handsDialog?.setInviteMicIndex(inviteIndex)
        handsDialog?.setFragmentListener(object : ChatroomHandsDialog.OnFragmentListener {
            override fun onAcceptMicSeatApply(voiceMicInfoModel: VoiceMicInfoModel) {
                // 更新麦位
                val newMicMap = mutableMapOf(voiceMicInfoModel.micIndex to voiceMicInfoModel)
                dealMicDataMap(newMicMap)
                updateViewByMicMap(newMicMap)
            }
        })
        handsDialog?.show(activity.supportFragmentManager, "room_hands")
        chatPrimaryMenuView.setShowHandStatus(true, false)
    }

    fun showMemberHandsDialog(micIndex: Int) {
        CommonSheetAlertDialog().contentText(
            if (isRequesting) activity.getString(R.string.voice_chatroom_cancel_request_speak)
            else activity.getString(R.string.voice_chatroom_request_speak)
        ).rightText(activity.getString(R.string.voice_room_confirm))
            .leftText(activity.getString(R.string.voice_room_cancel))
            .setOnClickListener(object : CommonSheetAlertDialog.OnClickBottomListener {
                override fun onConfirmClick() {
                    if (isRequesting) {
                        roomLivingViewModel.cancelMicSeatApply(
                            voiceRoomModel.chatroomId,
                            VoiceBuddyFactory.get().getVoiceBuddy().chatUserName()
                        )
                    } else {
                        if (activity is ChatroomLiveActivity) {
                            activity.toggleSelfAudio(true, callback = {
                                roomLivingViewModel.startMicSeatApply(micIndex)
                            })
                        }
                    }
                }
            }).show(activity.supportFragmentManager, "room_hands_apply")
    }

    fun handsUpdate(index: Int) {
        handsDialog?.update(index)
    }

    fun onClickBottomMic() {
        if (localUserMicInfo?.micStatus == MicStatus.ForceMute) {
            ToastTools.show(activity, activity.getString(R.string.voice_chatroom_mic_muted_by_host))
            return
        }
        val isOn = localUserMicInfo?.member?.micStatus == 1
        val toState = !isOn
        if (activity is ChatroomLiveActivity) {
            activity.toggleSelfAudio(toState, callback = {
                chatPrimaryMenuView.setEnableMic(toState)
                muteLocalAudio(!toState)
            })
        }
    }

    fun onClickBottomHandUp() {
        if (voiceRoomModel.isOwner) {
            showOwnerHandsDialog(-1)
        } else {
            showMemberHandsDialog(-1)
        }
    }

    fun updateAnnouncement(announcement: String?) {
        if (voiceRoomModel.announcement != announcement) {
            voiceRoomModel.announcement = announcement ?: ""
            ToastTools.show(activity, activity.getString(R.string.voice_chatroom_notice_changed))
        }
    }

    fun onAttributeMapUpdated(attributeMap: Map<String, String>) {
        if (attributeMap.containsKey("gift_amount")) {
            attributeMap["gift_amount"]?.toIntOrNull()?.let {
                voiceRoomModel.giftAmount = it
                ChatroomIMManager.getInstance().giftAmountCache = it
                ThreadManager.getInstance().runOnMainThread {
                    iRoomTopView.onUpdateGiftCount(it)
                }
            }
        }
        if (attributeMap.containsKey("click_count")) {
            attributeMap["click_count"]?.toIntOrNull()?.let {
                voiceRoomModel.clickCount = it
                ChatroomIMManager.getInstance().setClickCountCache(it)
                ThreadManager.getInstance().runOnMainThread {
                    iRoomTopView.onUpdateWatchCount(it)
                }
            }
        }
        if (attributeMap.containsKey("robot_volume")) {
            attributeMap["robot_volume"]?.toIntOrNull()?.let {
                voiceRoomModel.robotVolume = it
            }
        }
        if (attributeMap.containsKey("use_robot")) {
            voiceRoomModel.useRobot = attributeMap["use_robot"] == "1"
            ThreadManager.getInstance().runOnMainThread {
                iRoomMicView.activeBot(voiceRoomModel.useRobot)
            }
        }
        if (attributeMap.containsKey("ranking_list")) {
            val rankList = GsonTools.toList(attributeMap["ranking_list"], VoiceRankUserModel::class.java)
            rankList?.let { rankUsers ->
                rankUsers.forEach { rank ->
                    ChatroomIMManager.getInstance().setRankList(rank)
                }
                ThreadManager.getInstance().runOnMainThread {
                    iRoomTopView.onRankMember(rankUsers)
                }
            }
        } else if (attributeMap.containsKey("member_list")) {
            val memberList = GsonTools.toList(attributeMap["member_list"], VoiceMemberModel::class.java)
            memberList?.let { members ->
                members.forEach { member ->
                    if (!member.chatUid.equals(voiceRoomModel.owner?.chatUid)) {
                        ChatroomCacheManager.cacheManager.setMemberList(member)
                    }
                }
            }
        } else {
            // mic
            val micInfoMap = mutableMapOf<String, VoiceMicInfoModel>()
            attributeMap
                .filter { it.key.startsWith("mic_") }
                .forEach { (key, value) ->
                    val micInfo =
                        GsonTools.toBean<VoiceMicInfoModel>(value, object : TypeToken<VoiceMicInfoModel>() {}.type)
                    micInfo?.let {
                        micInfoMap[key] = it
                        if (it.member?.rtcUid == VoiceBuddyFactory.get().getVoiceBuddy().rtcUid()) {
                            localUserMicInfo = micInfo
                        }
                    }
                }
            val newMicMap = RoomInfoConstructor.extendMicInfoMap(micInfoMap, voiceRoomModel.owner?.userId ?: "")
            dealMicDataMap(newMicMap)
            ThreadManager.getInstance().runOnMainThread {
                updateViewByMicMap(newMicMap)
            }
        }
    }

    private fun dealMicDataMap(updateMap: Map<Int, VoiceMicInfoModel>) {
        var kvLocalUser: VoiceMicInfoModel? = null
        updateMap.forEach { (index, micInfo) ->
            val rtcUid = micInfo.member?.rtcUid ?: -1
            if (rtcUid > 0) {
                micMap[index] = rtcUid
                if (rtcUid == VoiceBuddyFactory.get().getVoiceBuddy().rtcUid()) kvLocalUser = micInfo
            } else {
                val removeRtcUid = micMap.remove(index)
                if (removeRtcUid == VoiceBuddyFactory.get().getVoiceBuddy().rtcUid()) localUserMicInfo = null
            }
        }
        kvLocalUser?.let { localUserMicInfo = it }
        AgoraRtcEngineController.get().switchRole(localUserIndex() >= 0)
        if (localUserMicInfo?.member?.micStatus == 1 &&
            localUserMicInfo?.micStatus == MicStatus.Normal
        ) {   // 状态正常
            AgoraRtcEngineController.get().enableLocalAudio(true)
        } else {
            AgoraRtcEngineController.get().enableLocalAudio(false)
        }
    }

    private fun updateViewByMicMap(newMicMap: Map<Int, VoiceMicInfoModel>) {
        iRoomMicView.onSeatUpdated(newMicMap)
        val isOn = (localUserMicInfo?.member?.micStatus == 1 &&
                localUserMicInfo?.micStatus == MicStatus.Normal)
        val onStage = localUserIndex() >= 0
        chatPrimaryMenuView.showMicVisible(onStage, isOn)
        AgoraRtcEngineController.get().earBackManager()?.setForbidden(!onStage)
        AgoraRtcEngineController.get().soundCardManager()?.setForbidden(!onStage)
        if (voiceRoomModel.isOwner) {
            val handsCheckMap = mutableMapOf<Int, String>()
            newMicMap.forEach { (t, u) ->
                handsCheckMap[t] = u.member?.userId ?: ""
            }
            handsDialog?.check(handsCheckMap)
        } else {
            chatPrimaryMenuView.setEnableHand(localUserIndex() >= 0)
            isRequesting = false
        }
    }

    fun checkUserLeaveMic() {
        val localUserIndex = localUserIndex()
        if (localUserIndex > 0) {
            roomLivingViewModel.leaveMic(localUserIndex)
        }
    }

    fun checkUserLeaveMic(index: Int) {
        if (index > 0) {
            roomLivingViewModel.leaveMic(index)
        }
    }
}