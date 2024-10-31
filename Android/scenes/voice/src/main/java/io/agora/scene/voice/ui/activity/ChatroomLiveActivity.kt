package io.agora.scene.voice.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.reflect.TypeToken
import io.agora.CallBack
import io.agora.Error
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.scene.base.AgoraScenes
import io.agora.scene.base.LogUploader
import io.agora.scene.base.SceneConfigManager
import io.agora.scene.base.component.AgoraApplication
import io.agora.scene.base.component.BaseViewBindingActivity
import io.agora.scene.base.component.OnItemClickListener
import io.agora.scene.voice.R
import io.agora.scene.voice.databinding.VoiceActivityChatroomBinding
import io.agora.scene.voice.global.VoiceBuddyFactory
import io.agora.scene.voice.imkit.bean.ChatMessageData
import io.agora.scene.voice.imkit.custorm.CustomMsgHelper
import io.agora.scene.voice.imkit.custorm.OnMsgCallBack
import io.agora.scene.voice.imkit.manager.ChatroomIMManager
import io.agora.scene.voice.model.*
import io.agora.scene.voice.model.constructor.RoomInfoConstructor.convertByRoomInfo
import io.agora.scene.voice.service.VoiceRoomServiceKickedReason
import io.agora.scene.voice.service.VoiceServiceListenerProtocol
import io.agora.scene.voice.service.VoiceServiceProtocol
import io.agora.scene.voice.ui.RoomGiftViewDelegate
import io.agora.scene.voice.ui.RoomObservableViewDelegate
import io.agora.scene.voice.ui.dialog.VoiceRoomDebugOptionsDialog
import io.agora.scene.voice.ui.widget.barrage.ChatroomMessagesView
import io.agora.scene.voice.ui.widget.primary.MenuItemClickListener
import io.agora.scene.voice.ui.widget.top.OnLiveTopClickListener
import io.agora.scene.voice.viewmodel.VoiceRoomLivingViewModel
import io.agora.scene.widget.dialog.PermissionLeakDialog
import io.agora.scene.widget.dialog.TopFunctionDialog
import io.agora.voice.common.constant.ConfigConstants
import io.agora.voice.common.net.OnResourceParseCallback
import io.agora.voice.common.net.Resource
import io.agora.voice.common.ui.IParserSource
import io.agora.voice.common.ui.dialog.BaseSheetDialog
import io.agora.voice.common.utils.GsonTools
import io.agora.voice.common.utils.LogTools
import io.agora.voice.common.utils.LogTools.logD
import io.agora.voice.common.utils.LogTools.logE
import io.agora.voice.common.utils.StatusBarCompat
import io.agora.voice.common.utils.ThreadManager
import io.agora.voice.common.utils.ToastTools


class ChatroomLiveActivity : BaseViewBindingActivity<VoiceActivityChatroomBinding>(), VoiceServiceListenerProtocol,
    IParserSource {

    companion object {
        const val KEY_VOICE_ROOM_MODEL = "voice_chat_room_model"
        const val TAG = "ChatroomLiveActivity"

        fun startActivity(activity: Activity, voiceRoomModel: AUIRoomInfo) {
            val intent = Intent(activity, ChatroomLiveActivity::class.java).apply {
                putExtra(KEY_VOICE_ROOM_MODEL, voiceRoomModel)
            }
            activity.startActivity(intent)
        }
    }

    /**room viewModel*/
    private lateinit var roomLivingViewModel: VoiceRoomLivingViewModel
    private lateinit var giftViewDelegate: RoomGiftViewDelegate
    private val voiceServiceProtocol = VoiceServiceProtocol.serviceProtocol
    private var isActivityStop = false


    private lateinit var roomObservableDelegate: RoomObservableViewDelegate

    private val voiceRoomModel = VoiceRoomModel()
    private val dialogFragments = mutableListOf<BottomSheetDialogFragment>()

    override fun getViewBinding(inflater: LayoutInflater): VoiceActivityChatroomBinding {
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        return VoiceActivityChatroomBinding.inflate(inflater)
    }

    override fun onStop() {
        isActivityStop = true
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        isActivityStop = false
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        StatusBarCompat.setLightStatusBar(this, false)
        roomLivingViewModel = ViewModelProvider(this)[VoiceRoomLivingViewModel::class.java]
        giftViewDelegate =
            RoomGiftViewDelegate.getInstance(this, roomLivingViewModel, binding.chatroomGiftView, binding.svgaView)
        val rtmRoomInfo: AUIRoomInfo = intent.getSerializableExtra(KEY_VOICE_ROOM_MODEL) as AUIRoomInfo

        this.voiceRoomModel.convertByRoomInfo(rtmRoomInfo)
        initView()
        giftViewDelegate.onRoomDetails(this.voiceRoomModel.roomId, this.voiceRoomModel.owner?.userId)
        ChatroomIMManager.getInstance().init(this.voiceRoomModel.chatroomId, this.voiceRoomModel.isOwner)
        ChatroomIMManager.getInstance().saveWelcomeMsg(
            getString(R.string.voice_room_welcome),
            VoiceBuddyFactory.get().getVoiceBuddy().nickName()
        )

//        binding.messageView.refreshSelectLast()
        if (this.voiceRoomModel.isOwner) {
            toggleAudioRun = Runnable {
                "onPermissionGrant initSdkJoin".logD(TAG)
                roomLivingViewModel.initSdkJoin(this, this.voiceRoomModel)
            }
            requestRecordPermission(true)
        } else {
            roomLivingViewModel.initSdkJoin(this, this.voiceRoomModel)
        }
    }

    private var toggleAudioRun: Runnable? = null

    fun toggleSelfAudio(isOpen: Boolean, callback: () -> Unit) {
        if (isOpen) {
            toggleAudioRun = Runnable {
                callback.invoke()
            }
            requestRecordPermission(true)
        } else {
            callback.invoke()
        }
    }

    override fun onPermissionDined(permission: String?) {
        PermissionLeakDialog(this).show(permission,
            { getPermissions() }
        ) { launchAppSetting(permission) }
    }

    override fun getPermissions() {
        if (toggleAudioRun != null) {
            toggleAudioRun?.run()
            toggleAudioRun = null
        }
    }

    override fun initListener() {
        // room detail
        roomLivingViewModel.roomDetailsObservable().observe(this) { response: Resource<VoiceRoomInfo> ->
            parseResource(response, object : OnResourceParseCallback<VoiceRoomInfo>() {

                override fun onLoading(data: VoiceRoomInfo?) {
                    super.onLoading(data)
                    showLoadingView()
                }

                override fun onHideLoading() {
                    super.onHideLoading()
                    hideLoadingView()
                }

                override fun onSuccess(data: VoiceRoomInfo?) {
                    data?.let {
                        roomObservableDelegate.onRoomDetails(it)
                    }
                }
            })
        }
        roomLivingViewModel.joinObservable().observe(this) { response: Resource<Boolean> ->
            parseResource(response, object : OnResourceParseCallback<Boolean>() {

                override fun onSuccess(data: Boolean?) {
                    ToastTools.show(this@ChatroomLiveActivity, getString(R.string.voice_chatroom_join_room_success))
                    roomLivingViewModel.fetchRoomDetail(this@ChatroomLiveActivity.voiceRoomModel)
                    CustomMsgHelper.getInstance().sendSystemMsg(
                        this@ChatroomLiveActivity.voiceRoomModel.owner?.chatUid, object : OnMsgCallBack() {
                            override fun onSuccess(message: ChatMessageData?) {
                                "sendSystemMsg onSuccess $message".logD()
                                binding.messageView.refreshSelectLast()
                            }

                            override fun onError(messageId: String?, code: Int, error: String?) {
                                "sendSystemMsg onFail $code $error".logE()
                            }
                        }
                    )
                }

                override fun onError(code: Int, message: String?) {
                    voiceServiceProtocol.leaveRoom { }
                    ToastTools.show(
                        this@ChatroomLiveActivity,
                        message ?: getString(R.string.voice_chatroom_join_room_failed)
                    )
                    ThreadManager.getInstance().runOnMainThreadDelay({
                        finish()
                    }, 1000)
                }
            })
        }
        roomLivingViewModel.updateRoomMemberObservable().observe(this) { response: Resource<Boolean> ->
            parseResource(response, object : OnResourceParseCallback<Boolean>() {
                override fun onSuccess(data: Boolean?) {
                    "ChatroomLiveActivity updateRoomMember onSuccess".logD()
                }

                override fun onError(code: Int, message: String?) {
                    super.onError(code, message)
                    "ChatroomLiveActivity updateRoomMember onError $code $message".logE()
                }
            })
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _: View?, insets: WindowInsetsCompat ->
            val systemInset = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            "systemInset:left:${systemInset.left},top:${systemInset.top},right:${systemInset.right},bottom:${systemInset.bottom}".logD(
                "insets=="
            )
            binding.clMain.setPaddingRelative(0, systemInset.top, 0, systemInset.bottom)
            WindowInsetsCompat.CONSUMED
        }
        binding.clMain.setOnTouchListener { v, event ->
            resetUI()
            false
        }
        binding.messageView.setMessageViewListener(object : ChatroomMessagesView.MessageViewListener {
            override fun onItemClickListener(message: ChatMessageData?) {
            }

            override fun onListClickListener() {
                resetUI()
            }
        })
        voiceServiceProtocol.subscribeListener(object : VoiceServiceListenerProtocol {
            override fun onChatTokenWillExpire() {
                roomLivingViewModel.renewChatToken()
            }

            override fun onReceiveGift(roomId: String, message: ChatMessageData?) {
                if (!TextUtils.equals(this@ChatroomLiveActivity.voiceRoomModel.chatroomId, roomId)) return
                "onReceiveGift $roomId ${message?.content}".logD(TAG)
                ThreadManager.getInstance().runOnMainThread {
                    binding.chatroomGiftView.refresh()
                    if (CustomMsgHelper.getInstance().getMsgGiftId(message).equals("VoiceRoomGift9")) {
                        giftViewDelegate.showGiftAction()
                        binding.subtitle.showSubtitleView(
                            resources.getString(
                                R.string.voice_chatroom_gift_notice,
                                ChatroomIMManager.getInstance().getUserName(message),
                                this@ChatroomLiveActivity.voiceRoomModel.owner?.nickName
                            )
                        )
                    }
                    roomObservableDelegate.receiveGift(this@ChatroomLiveActivity.voiceRoomModel.roomId, message)
                }
            }

            override fun onReceiveTextMsg(roomId: String, message: ChatMessageData?) {
                if (!TextUtils.equals(this@ChatroomLiveActivity.voiceRoomModel.chatroomId, roomId)) return
                "onReceiveTextMsg $roomId ${message?.content}".logD(TAG)
                ThreadManager.getInstance().runOnMainThread {
                    binding.messageView.refreshSelectLast()
                }
            }

            override fun onReceiveSeatRequest(message: ChatMessageData) {
                "onReceiveSeatRequest ${this@ChatroomLiveActivity.voiceRoomModel.isOwner}".logD(TAG)
                ThreadManager.getInstance().runOnMainThread {
                    binding.chatBottom.setShowHandStatus(this@ChatroomLiveActivity.voiceRoomModel.isOwner, true)
                }
            }

            override fun onReceiveSeatRequestRejected(chatUid: String) {
                "onReceiveSeatRequestRejected $chatUid".logD(TAG)
                ThreadManager.getInstance().runOnMainThread {
                    // refresh owner apply list
                    roomObservableDelegate.handsUpdate(0)
                }
            }

            override fun onReceiveSeatInvitation(message: ChatMessageData) {
                "onReceiveSeatInvitation $message".logD(TAG)
                if (message.customParams.containsKey("user")) {
                    val voiceRoomInvite = GsonTools.toBean(message.customParams["user"], VoiceMemberModel::class.java)
                    if (voiceRoomInvite != null) {
                        ThreadManager.getInstance().runOnMainThread {
                            roomObservableDelegate.receiveInviteSite(
                                this@ChatroomLiveActivity.voiceRoomModel.roomId,
                                voiceRoomInvite.micIndex
                            )
                        }
                    }
                }

            }

            override fun onReceiveSeatInvitationRejected(
                chatUid: String,
                message: ChatMessageData?
            ) {
                "onReceiveSeatInvitationRejected $chatUid ${message?.content}".logD(TAG)
            }

            override fun onAnnouncementChanged(roomId: String, content: String) {
                "onAnnouncementChanged $content".logD(TAG)
                if (!TextUtils.equals(this@ChatroomLiveActivity.voiceRoomModel.chatroomId, roomId)) return
                ThreadManager.getInstance().runOnMainThread {
                    roomObservableDelegate.updateAnnouncement(content)
                }
            }

            override fun onUserJoinedRoom(roomId: String, voiceMember: VoiceMemberModel) {
                if (!TextUtils.equals(this@ChatroomLiveActivity.voiceRoomModel.chatroomId, roomId)) return
                LogTools.d(TAG, "onUserJoinedRoom $roomId, ${voiceMember.chatUid}")
                if (this@ChatroomLiveActivity.voiceRoomModel.isOwner) {
                    ChatroomIMManager.getInstance().setMemberList(voiceMember)
                    roomLivingViewModel.updateRoomMember()
                    roomObservableDelegate.onUserJoinedRoom()
                }
                binding.messageView.refreshSelectLast()
            }

            override fun onUserLeftRoom(roomId: String, chatUid: String) {
                if (!TextUtils.equals(this@ChatroomLiveActivity.voiceRoomModel.chatroomId, roomId)) return
                LogTools.d(TAG, "onUserLeftRoom $roomId, $chatUid")
                if (this@ChatroomLiveActivity.voiceRoomModel.isOwner) {
                    ChatroomIMManager.getInstance().removeMember(chatUid)
                    ChatroomIMManager.getInstance().removeSubmitMember(chatUid)
                    roomObservableDelegate.handsUpdate(1)
                    roomObservableDelegate.handsUpdate(0)
                    roomLivingViewModel.updateRoomMember()
                    roomObservableDelegate.checkUserLeaveMic(
                        ChatroomIMManager.getInstance().getMicIndexByChatUid(chatUid)
                    )
                }
            }

            override fun onUserBeKicked(roomId: String, reason: VoiceRoomServiceKickedReason) {
                super.onUserBeKicked(roomId, reason)
                if (!TextUtils.equals(this@ChatroomLiveActivity.voiceRoomModel.chatroomId, roomId)) return
                "userBeKicked $reason".logD(TAG)
                ThreadManager.getInstance().runOnMainThread {
                    if (reason == VoiceRoomServiceKickedReason.destroyed) {
                        innerRelease()
                        roomObservableDelegate.onTimeUpExitRoom(
                            getString(R.string.room_has_close), finishBack = {
                                finish()
                            })
                    } else if (reason == VoiceRoomServiceKickedReason.removed) {
                        innerRelease()
                        roomObservableDelegate.onTimeUpExitRoom(
                            getString(R.string.voice_room_kick_member), finishBack = {
                                finish()
                            })
                    }
                }
            }

            override fun onAttributeMapUpdated(
                roomId: String,
                attributeMap: Map<String, String>,
                fromId: String
            ) {
                super.onAttributeMapUpdated(roomId, attributeMap, fromId)
                "roomAttributesDidUpdated ${Thread.currentThread()},roomId:$roomId,fromId:$fromId,map:$attributeMap".logD()
                if (isFinishing || !TextUtils.equals(
                        this@ChatroomLiveActivity.voiceRoomModel.chatroomId,
                        roomId
                    )
                ) return
                attributeMap.let {
                    ChatroomIMManager.getInstance().updateMicInfoCache(it)
                    roomObservableDelegate.onAttributeMapUpdated(it)
                }
                attributeMap
                    .filter { it.key.startsWith("mic_") }
                    .forEach { (key, value) ->
                        val micInfo =
                            GsonTools.toBean<VoiceMicInfoModel>(value, object : TypeToken<VoiceMicInfoModel>() {}.type)
                        micInfo?.let {
                            if (it.member?.chatUid != null) {
                                if (ChatroomIMManager.getInstance().checkMember(it.member?.chatUid)) {
                                    ChatroomIMManager.getInstance().removeSubmitMember(it.member?.chatUid)
                                    ThreadManager.getInstance().runOnMainThread {
                                        roomObservableDelegate.handsUpdate(0)
                                    }
                                }
                                if (ChatroomIMManager.getInstance().checkInvitationMember(it.member?.chatUid)) {
                                    ThreadManager.getInstance().runOnMainThread {
                                        roomObservableDelegate.handsUpdate(1)
                                    }
                                }
                            }
                        }
                    }
            }

            override fun onRoomDestroyed(roomId: String) {
                super.onRoomDestroyed(roomId)
                if (!TextUtils.equals(this@ChatroomLiveActivity.voiceRoomModel.chatroomId, roomId)) return
                "onRoomDestroyed $roomId".logD(TAG)
                ThreadManager.getInstance().runOnMainThread {
                    innerRelease()
                    roomObservableDelegate.onTimeUpExitRoom(
                        getString(R.string.room_has_close), finishBack = {
                            finish()
                        })
                }
            }

            override fun onSyncUserCountUpdate(userCount: Int) {
                LogTools.d(TAG, "onSyncUserCountUpdate 1 ${this@ChatroomLiveActivity.voiceRoomModel.memberCount}")
                this@ChatroomLiveActivity.voiceRoomModel.memberCount = userCount
                LogTools.d(TAG, "onSyncUserCountUpdate 2 ${this@ChatroomLiveActivity.voiceRoomModel.memberCount}")
                binding.cTopView.onUpdateMemberCount(this@ChatroomLiveActivity.voiceRoomModel.memberCount)
            }

            override fun onSyncRoomDestroy() {
                innerRelease()
                roomObservableDelegate.onTimeUpExitRoom(
                    getString(R.string.room_has_close), finishBack = {
                        finish()
                    })
            }

            override fun onSyncRoomExpire() {
                innerRelease()
                roomObservableDelegate.onTimeUpExitRoom(
                    getString(R.string.voice_chatroom_time_up_tips), finishBack = {
                       finish()
                    })
            }
        })
    }

    private fun initView() {
        binding.chatBottom.initMenu(ConfigConstants.RoomType.Common_Chatroom)
        binding.likeView.likeView.setOnClickListener { binding.likeView.addFavor() }
        binding.chatroomGiftView.init(this.voiceRoomModel.chatroomId)
        binding.messageView.init(this.voiceRoomModel.chatroomId, this.voiceRoomModel.owner?.chatUid)
        binding.rvChatroom2dMicLayout.isVisible = true
        roomObservableDelegate =
            RoomObservableViewDelegate(
                this,
                roomLivingViewModel,
                this.voiceRoomModel,
                binding.cTopView,
                binding.rvChatroom2dMicLayout,
                binding.chatBottom
            )
        binding.rvChatroom2dMicLayout.setMyRtcUid(VoiceBuddyFactory.get().getVoiceBuddy().rtcUid())
        binding.rvChatroom2dMicLayout.onItemClickListener(
            object : OnItemClickListener<VoiceMicInfoModel> {
                override fun onItemClick(
                    data: VoiceMicInfoModel,
                    view: View,
                    position: Int,
                    viewType: Long
                ) {
                    roomObservableDelegate.onUserMicClick(data)
                }
            },
            object : OnItemClickListener<VoiceMicInfoModel> {
                override fun onItemClick(
                    data: VoiceMicInfoModel,
                    view: View,
                    position: Int,
                    viewType: Long
                ) {
                    roomObservableDelegate.onBotMicClick(getString(R.string.voice_chatroom_open_bot_prompt)) {
                        finish()
                    }
                }
            }
        ).setUpInitAdapter()
        binding.cTopView.setOnLiveTopClickListener(object : OnLiveTopClickListener {
            override fun onClickBack(view: View) {
                onBackPressed()
            }

            override fun onClickRank(view: View, pageIndex: Int) {
                roomObservableDelegate.onClickRank(pageIndex)
            }

            override fun onClickNotice(view: View) {
                roomObservableDelegate.onClickNotice()
            }

            override fun onClickSoundSocial(view: View) {
                roomObservableDelegate.onClickSoundSocial(
                    this@ChatroomLiveActivity.voiceRoomModel.soundEffect,
                    finishBack = {
                        innerRelease()
                        finish()
                    })
            }

            override fun onClickMore(view: View) {
                TopFunctionDialog(this@ChatroomLiveActivity).show()
            }
        })
        binding.chatBottom.setMenuItemOnClickListener(object :
            MenuItemClickListener {
            override fun onChatExtendMenuItemClick(itemId: Int, view: View?) {
                when (itemId) {
                    R.id.voice_extend_item_eq -> {
                        roomObservableDelegate.onAudioSettingsDialog(finishBack = {
                            innerRelease()
                            finish()
                        })
                    }

                    R.id.voice_extend_item_mic -> {
                        roomObservableDelegate.onClickBottomMic()
                    }

                    R.id.voice_extend_item_hand_up -> {
                        roomObservableDelegate.onClickBottomHandUp()
                    }

                    R.id.voice_extend_item_gift -> {
                        giftViewDelegate.showGiftDialog(object : OnMsgCallBack() {
                            override fun onSuccess(message: ChatMessageData?) {
                                roomObservableDelegate.onSendGiftSuccess(
                                    this@ChatroomLiveActivity.voiceRoomModel.roomId,
                                    message
                                )
                                if (CustomMsgHelper.getInstance().getMsgGiftId(message).equals("VoiceRoomGift9")) {
                                    binding.subtitle.showSubtitleView(
                                        resources.getString(
                                            R.string.voice_chatroom_gift_notice,
                                            ChatroomIMManager.getInstance().getUserName(message),
                                            this@ChatroomLiveActivity.voiceRoomModel.owner?.nickName
                                        )
                                    )
                                }
                            }

                            override fun onError(messageId: String?, code: Int, error: String?) {
                                ToastTools.show(
                                    this@ChatroomLiveActivity,
                                    getString(R.string.voice_chatroom_send_gift_fail)
                                )
                            }
                        })
                    }
                }
            }

            override fun onInputLayoutClick() {
                checkFocus(false)
            }

            override fun onSendMessage(content: String?) {
                if (!content.isNullOrEmpty())
                    ChatroomIMManager.getInstance().sendTxtMsg(content,
                        VoiceBuddyFactory.get().getVoiceBuddy().nickName(),
                        object : OnMsgCallBack() {
                            override fun onSuccess(message: ChatMessageData?) {
                                ThreadManager.getInstance().runOnMainThread {
                                    binding.messageView.refreshSelectLast()
                                    binding.likeView.isVisible = true
                                }
                            }

                            override fun onError(code: Int, error: String?) {
                                "onSendMessage onError  $code $error".logE(TAG)
                                binding.likeView.isVisible = true
                                if (code == Error.MODERATION_FAILED) {
                                    ToastTools.show(
                                        this@ChatroomLiveActivity,
                                        getString(R.string.voice_room_content_prohibited)
                                    )
                                }
                            }
                        })
            }
        })

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                if (f is BottomSheetDialogFragment) {
                    if (dialogFragments.contains(f)) {
                        return
                    }
                    val lastFragment = dialogFragments.lastOrNull()
                    dialogFragments.add(f)
//                    lastFragment?.dismiss()
                }
                super.onFragmentStarted(fm, f)
            }

            override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
                super.onFragmentStopped(fm, f)
                if (isActivityStop) {
                    return
                }
                if (f is BottomSheetDialogFragment) {
                    if (f is BaseSheetDialog<*> && f.onCancel) {
                        val iterator = dialogFragments.iterator()
                        while (iterator.hasNext()) {
                            iterator.next().dismiss()
                            iterator.remove()
                        }
                        return
                    }

//                    val lastFragment = dialogFragments.lastOrNull()
//                    if (lastFragment == f) {
//                        dialogFragments.remove(f)
//                        dialogFragments.lastOrNull()?.let {
//                            it.show(fm, it.tag)
//                        }
//                    }
                }
            }
        }, true)
        // debug mode
        if (AgoraApplication.the().isDebugModeOpen) {
            binding.btnDebug.isVisible = true
            VoiceRoomDebugOptionsDialog.debugMode()
        } else {
            binding.btnDebug.isVisible = false
        }
        binding.btnDebug.setOnClickListener {
            VoiceRoomDebugOptionsDialog().show(supportFragmentManager, "mtDebug")
        }
    }

    override fun onBackPressed() {
        if (binding.chatBottom.showNormalLayout()) {
            return
        }
        if (this.voiceRoomModel.isOwner) {
            roomObservableDelegate.onExitRoom(
                getString(R.string.voice_chatroom_end_live),
                getString(R.string.voice_chatroom_end_live_tips), finishBack = {
                    innerRelease()
                    finish()
                })
        } else {
            roomObservableDelegate.checkUserLeaveMic()
            innerRelease()
            finish()
        }
    }

    private fun innerRelease() {

        roomLivingViewModel.leaveSyncManagerRoom()

        if (this.voiceRoomModel.isOwner) {
            ChatroomIMManager.getInstance().leaveChatRoom(this.voiceRoomModel.chatroomId)
            ChatroomIMManager.getInstance().asyncDestroyChatRoom(this.voiceRoomModel.chatroomId, object :
                CallBack {
                override fun onSuccess() {}

                override fun onError(code: Int, error: String?) {}
            })
        } else {
            roomObservableDelegate.checkUserLeaveMic()
            ChatroomIMManager.getInstance().leaveChatRoom(this.voiceRoomModel.chatroomId)
        }
        ChatroomIMManager.getInstance().removeChatRoomChangeListener()
        ChatroomIMManager.getInstance().clearCache()

        binding.chatroomGiftView.clear()
        roomObservableDelegate.destroy()
        voiceServiceProtocol.unsubscribeEvent()
        binding.subtitle.clearTask()
        dialogFragments.clear()
        finish()
    }

    override fun finish() {
        super.finish()
    }

    private fun resetUI() {
        binding.chatBottom.hideExpressionView(false)
        hideInput()
        binding.chatBottom.showInput()
        binding.likeView.isVisible = true
        binding.chatBottom.hindViewChangeIcon()
    }

    private fun checkFocus(focus: Boolean) {
        binding.likeView.isVisible = focus
    }

    override fun onDestroy() {
        super.onDestroy()
        if (SceneConfigManager.logUpload) {
            LogUploader.uploadLog(AgoraScenes.ChatRoom)
        }
    }
}