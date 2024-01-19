package io.agora.scene.voice.imkit.manager

import android.os.Build
import android.text.TextUtils
import androidx.annotation.RequiresApi
import io.agora.CallBack
import io.agora.ValueCallBack
import io.agora.chat.ChatClient
import io.agora.chat.ChatRoomManager
import io.agora.scene.voice.model.annotation.MicClickAction
import io.agora.scene.voice.model.annotation.MicStatus
import io.agora.scene.voice.global.VoiceBuddyFactory
import io.agora.scene.voice.imkit.bean.ChatMessageData
import io.agora.scene.voice.imkit.custorm.CustomMsgHelper
import io.agora.scene.voice.imkit.custorm.CustomMsgType
import io.agora.scene.voice.imkit.custorm.OnMsgCallBack
import io.agora.scene.voice.model.*
import io.agora.scene.voice.rtckit.AgoraBGMManager
import io.agora.voice.common.constant.ConfigConstants
import io.agora.voice.common.utils.GsonTools
import io.agora.voice.common.utils.LogTools.logD
import io.agora.voice.common.utils.LogTools.logE
import io.agora.voice.common.utils.ThreadManager

/**
 * Chatroom protocol delegate
 *
 * @property roomId
 * @constructor Create empty Chatroom protocol delegate
 */
class ChatroomProtocolDelegate constructor(
    private val roomId: String
) {
    companion object {
        private const val TAG = "ChatroomProtocolDelegate"
    }

    private var roomManager: ChatRoomManager = ChatClient.getInstance().chatroomManager()
    private var ownerBean = VoiceMemberModel()

    /////////////////////// mic ///////////////////////////

    /**
     * Init mic info
     *
     * @param roomType
     * @param ownerBean
     * @param callBack
     */
    fun initMicInfo(roomType: Int, ownerBean: VoiceMemberModel, callBack: CallBack) {
        val attributeMap = mutableMapOf<String, String>()
        this@ChatroomProtocolDelegate.ownerBean = ownerBean
        if (roomType == ConfigConstants.RoomType.Common_Chatroom) {
            attributeMap["use_robot"] = "0"
            attributeMap["robot_volume"] = "50"
            attributeMap["mic_0"] = GsonTools.beanToString(VoiceMicInfoModel(0, ownerBean, MicStatus.Normal)).toString()
            for (i in 1..7) {
                var key = "mic_$i"
                var status = MicStatus.Idle
                if (i >= 6) status = MicStatus.BotInactive
                var mBean = GsonTools.beanToString(VoiceMicInfoModel(i, null, status))
                if (mBean != null) {
                    attributeMap[key] = mBean
                }
            }
        } else if (roomType == ConfigConstants.RoomType.Spatial_Chatroom) {

        }
        roomManager.asyncSetChatroomAttributesForced(
            roomId, attributeMap, true
        ) { code, result_map ->
            if (code == 0 && result_map.isEmpty()) {
                callBack.onSuccess()
                ChatroomCacheManager.cacheManager.setMicInfo(attributeMap)
                "initMicInfo update result onSuccess roomId:$roomId,".logD(TAG)
            } else {
                callBack.onError(code, result_map.toString())
                "initMicInfo update result onError roomId:$roomId, $code $result_map ".logE(TAG)
            }
        }
    }

    /**
     * Fetch room detail
     *
     * @param voiceRoomModel
     * @param callback
     */
    fun fetchRoomDetail(voiceRoomModel: VoiceRoomModel, callback: ValueCallBack<VoiceRoomInfo>){
        val keyList: MutableList<String> =
            mutableListOf("ranking_list", "member_list", "gift_amount", "robot_volume", "use_robot", "room_bgm")
        for (i in 0..7) {
            keyList.add("mic_$i")
        }
        this.ownerBean = voiceRoomModel.owner ?: VoiceMemberModel()
        val voiceRoomInfo = VoiceRoomInfo()
        voiceRoomInfo.roomInfo = voiceRoomModel
        roomManager.asyncFetchChatRoomAnnouncement(roomId, object : ValueCallBack<String> {
            override fun onSuccess(value: String?) {
                voiceRoomModel.announcement = value ?: ""
            }
            override fun onError(error: Int, errorMsg: String?) {
            }
        })
        roomManager.asyncFetchChatroomAttributesFromServer(roomId, keyList,
            object : ValueCallBack<Map<String, String>> {
                @RequiresApi(Build.VERSION_CODES.N)
                override fun onSuccess(result: Map<String, String>) {
                    val micInfoList = mutableListOf<VoiceMicInfoModel>()
                    val micMap = mutableMapOf<String,String>()
                    result.entries.forEach {
                        val key = it.key
                        val value = it.value
                        if (key.startsWith("mic_")){
                            micMap[key] = value
                        }else if (key=="ranking_list"){
                            val rankList = GsonTools.toList(value, VoiceRankUserModel::class.java)
                            rankList?.let { rankUsers ->
                                voiceRoomInfo.roomInfo?.rankingList = rankUsers
                                rankUsers.forEach { rank ->
                                    ChatroomCacheManager.cacheManager.setRankList(rank)
                                }
                            }
                        } else if (key == "member_list") {
                            val memberList = GsonTools.toList(value, VoiceMemberModel::class.java)
                            memberList?.let { members ->
                                "member_list($members) fetchRoomDetail onSuccess: ".logD(TAG)
                                addMemberListBySelf(members, object : ValueCallBack<List<VoiceMemberModel>> {
                                        override fun onSuccess(value: List<VoiceMemberModel>) {
                                            voiceRoomInfo.roomInfo?.memberList = value
                                            value.forEach { member ->
                                                if (!member.chatUid.equals(ownerBean.chatUid)){
                                                    ChatroomCacheManager.cacheManager.setMemberList(member)
                                                }
                                            }
                                        }

                                    override fun onError(code: Int, error: String?) {
                                        voiceRoomInfo.roomInfo?.memberList = memberList
                                        memberList.forEach { member ->
                                            if (!member.chatUid.equals(ownerBean.chatUid)){
                                                ChatroomCacheManager.cacheManager.setMemberList(member)
                                            }
                                        }
                                    }
                                })
                            }
                        }else if (key=="gift_amount"){
                            value.toIntOrNull()?.let {
                                voiceRoomInfo.roomInfo?.giftAmount = it
                                ChatroomCacheManager.cacheManager.setGiftAmountCache(it)
                            }
                        }else if (key=="robot_volume"){
                            value.toIntOrNull()?.let {
                                voiceRoomInfo.roomInfo?.robotVolume = it
                            }
                        }else if (key=="use_robot"){
                            voiceRoomInfo.roomInfo?.useRobot = value == "1"
                        } else if (key == "room_bgm") {
                            voiceRoomInfo.bgmInfo = GsonTools.toBean(value, VoiceBgmModel::class.java)
                        }
                    }
                    ChatroomCacheManager.cacheManager.clearMicInfo()
                    ChatroomCacheManager.cacheManager.setMicInfo(micMap)
                    for (entry in micMap.entries) {
                        GsonTools.toBean(entry.value, VoiceMicInfoModel::class.java)?.let {
                            micInfoList.add(it)
                        }
                    }
                    voiceRoomInfo.micInfo = micInfoList.sortedBy { it.micIndex }
                    "fetchRoomDetail onSuccess roomId:$roomId, $micInfoList".logD(TAG)
                    callback.onSuccess(voiceRoomInfo)
                }

                override fun onError(error: Int, desc: String?) {
                    "fetchRoomDetail onError roomId:$roomId, $error $desc".logE(TAG)
                    callback.onError(error,desc)
                }
            })
    }

    /**
     * Get mic info from server
     *
     * @param callback
     */
    fun getMicInfoFromServer(callback: ValueCallBack<List<VoiceMicInfoModel>>) {
        val keyList: MutableList<String> = mutableListOf()
        for (i in 0..7) {
            keyList.add("mic_$i")
        }
        roomManager.asyncFetchChatroomAttributesFromServer(roomId, keyList,
            object : ValueCallBack<Map<String, String>> {
                override fun onSuccess(value: Map<String, String>) {
                    val micInfoList = mutableListOf<VoiceMicInfoModel>()
                    ChatroomCacheManager.cacheManager.clearMicInfo()
                    ChatroomCacheManager.cacheManager.setMicInfo(value)
                    for (entry in value.entries) {
                       GsonTools.toBean(entry.value, VoiceMicInfoModel::class.java)?.let {
                           micInfoList.add(it)
                       }
                    }
                    "getMicInfoFromServer onSuccess: $micInfoList".logD(TAG)
                    callback.onSuccess(micInfoList.sortedBy { it.micIndex })
                }

                override fun onError(error: Int, desc: String?) {
                    "getMicInfoFromServer onError: $error $desc".logE(TAG)
                    callback.onError(error,desc)
                }
            })
    }

    /**
     * Get mic info from local
     *
     * @return
     */
    fun getMicInfo(): MutableMap<String, VoiceMicInfoModel> {
        val micInfoMap = mutableMapOf<String, VoiceMicInfoModel>()
        var localMap = ChatroomCacheManager.cacheManager.getMicInfoMap()
        if (localMap != null) {
            for (entry in localMap.entries) {
               GsonTools.toBean(entry.value, VoiceMicInfoModel::class.java)?.let {
                   micInfoMap[entry.key] = it
               }
            }
        }
        return micInfoMap
    }

    /**
     * Get mic info
     *
     * @param micIndex
     * @return
     */
    private fun getMicInfo(micIndex: Int): VoiceMicInfoModel? {
        return ChatroomCacheManager.cacheManager.getMicInfoByIndex(micIndex)
    }

    /**
     * Get mic info by index from server
     *
     * @param micIndex
     * @param callback
     */
    fun getMicInfoByIndexFromServer(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        val keyList: MutableList<String> = mutableListOf(getMicIndex(micIndex))
        roomManager.asyncFetchChatroomAttributesFromServer(roomId, keyList, object :
            ValueCallBack<Map<String, String>> {
            override fun onSuccess(value: Map<String, String>) {
                val micBean = GsonTools.toBean(value[getMicIndex(micIndex)], VoiceMicInfoModel::class.java)
                callback.onSuccess(micBean)
            }

            override fun onError(error: Int, desc: String?) {
                "getMicInfoByIndex onError: $error $desc".logE(TAG)
                callback.onError(error, desc)
            }
        })
    }

    /**
     * Leave mic
     *
     * @param micIndex
     * @param callback
     */
    fun leaveMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(null,micIndex, MicClickAction.OffStage, true, callback)
    }

    /**
     * Change mic
     *
     * @param fromMicIndex
     * @param toMicIndex
     * @param callback
     */
    fun changeMic(fromMicIndex: Int, toMicIndex: Int, callback: ValueCallBack<Map<Int, VoiceMicInfoModel>>) {
        val attributeMap = mutableMapOf<String, String>()
        val fromKey = getMicIndex(fromMicIndex)
        val toKey = getMicIndex(toMicIndex)
        val fromBean = getMicInfo(fromMicIndex)
        val toMicBean = getMicInfo(toMicIndex)
        if (toMicBean != null && fromBean != null && (toMicBean.micStatus == MicStatus.Idle || toMicBean.micStatus == MicStatus.Mute || toMicBean.micStatus == MicStatus.ForceMute)) {
            val fromMicStatus = fromBean.micStatus
            val toMicStatus = toMicBean.micStatus
            fromBean.member?.micIndex  = toMicIndex
            fromBean.micIndex = toMicIndex
            when (toMicStatus) {
                MicStatus.ForceMute -> {
                    fromBean.micStatus = MicStatus.ForceMute
                }
                else -> {
                    fromBean.micStatus = MicStatus.Normal
                }
            }

            toMicBean.micIndex = fromMicIndex
            when (fromMicStatus) {
                MicStatus.ForceMute -> {
                    toMicBean.micStatus = MicStatus.ForceMute
                }
                else -> {
                    toMicBean.micStatus = MicStatus.Idle
                }
            }
            val fromBeanValue = GsonTools.beanToString(fromBean)
            val toBeanValue = GsonTools.beanToString(toMicBean)
            if (toBeanValue != null) {
                attributeMap[fromKey] = toBeanValue
            }
            if (fromBeanValue != null) {
                attributeMap[toKey] = fromBeanValue
            }
            roomManager.asyncSetChatroomAttributesForced(
                roomId, attributeMap, false
            ) { code, result_map ->
                if (code == 0 && result_map.isEmpty()) {
                    val map = mutableMapOf<Int, VoiceMicInfoModel>()
                    map[fromMicIndex] = toMicBean
                    map[toMicIndex] = fromBean
                    attributeMap.let { ChatroomCacheManager.cacheManager.setMicInfo(it) }
                    callback.onSuccess(map)
                    "changeMic update result onSuccess: ".logD(TAG)
                } else {
                    callback.onError(code, result_map.toString())
                    "changeMic update result onError: $code $result_map ".logE(TAG)
                }
            }
        }
    }

    /**
     * Mute local
     *
     * @param micIndex
     * @param callback
     */
    fun muteLocal(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(null,micIndex, MicClickAction.Mute, true, callback)
    }

    /**
     * Un mute local
     *
     * @param micIndex
     * @param callback
     */
    fun unMuteLocal(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(null,micIndex, MicClickAction.UnMute, true, callback)
    }

    /**
     * Forbid mic
     *
     * @param micIndex
     * @param callback
     */
    fun forbidMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(null,micIndex, MicClickAction.ForbidMic, true, callback)
    }

    /**
     * Un forbid mic
     *
     * @param micIndex
     * @param callback
     */
    fun unForbidMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(null,micIndex, MicClickAction.UnForbidMic, true, callback)
    }

    /**
     * Kick off
     *
     * @param micIndex
     * @param callback
     */
    fun kickOff(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(null,micIndex, MicClickAction.KickOff, true, callback)
    }

    /**
     * Lock mic
     *
     * @param micIndex
     * @param callback
     */
    fun lockMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(null,micIndex, MicClickAction.Lock, true, callback)
    }

    /**
     * Un lock mic
     *
     * @param micIndex
     * @param callback
     */
    fun unLockMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(null,micIndex, MicClickAction.UnLock, true, callback)
    }

    /**
     * Fetch raised list
     *
     * @return
     */
    fun fetchRaisedList(): MutableList<VoiceMemberModel> {
        return ChatroomCacheManager.cacheManager.getSubmitMicList()
    }

    /**
     * Start mic seat apply
     *
     * @param micIndex
     * @param callback
     */
    fun startMicSeatApply(micIndex: Int, callback: CallBack) {
        val attributeMap = mutableMapOf<String, String>()
        val voiceRoomApply = VoiceRoomApply()
        val memberBean = VoiceMemberModel().apply {
            userId = VoiceBuddyFactory.get().getVoiceBuddy().userId()
            chatUid = VoiceBuddyFactory.get().getVoiceBuddy().chatUserName()
            rtcUid = VoiceBuddyFactory.get().getVoiceBuddy().rtcUid()
            nickName = VoiceBuddyFactory.get().getVoiceBuddy().nickName()
            portrait = VoiceBuddyFactory.get().getVoiceBuddy().headUrl()
        }
        if (micIndex != -1) {
            voiceRoomApply.index = micIndex
            memberBean.micIndex = micIndex
        }
        voiceRoomApply.member = memberBean
        "startMicSeatApply:$memberBean".logD(TAG)
        voiceRoomApply.created_at = System.currentTimeMillis()
        attributeMap["user"] = GsonTools.beanToString(voiceRoomApply).toString()
        attributeMap["chatroomId"] = ChatroomIMManager.getInstance().currentRoomId
        sendChatroomEvent(true, ownerBean.chatUid, CustomMsgType.CHATROOM_APPLY_SITE, attributeMap, callback)
    }

    /**
     * Accept mic seat apply
     *
     * @param chatUid
     * @param micIndex
     * @param callback
     */
    fun acceptMicSeatApply(chatUid:String,micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        val memberBean = ChatroomCacheManager.cacheManager.getMember(chatUid)
        if (memberBean != null) {
            if (micIndex != -1) {
                val micInfoModel = getMicInfo(micIndex)
                memberBean.micIndex = if (micInfoModel?.micStatus == MicStatus.Idle || micInfoModel?.micStatus==MicStatus.ForceMute) micIndex else getFirstFreeMic()
            } else {
                memberBean.micIndex = getFirstFreeMic()
            }
        }
        ThreadManager.getInstance().runOnIOThread {
            if (checkMemberIsOnMic(memberBean)) return@runOnIOThread
            memberBean?.let {
                updateMicByResult(memberBean,
                    it.micIndex, MicClickAction.Accept, true, callback)
            }
        }
    }

    /**
     * Check member is on mic
     *
     * @param memberModel
     * @return
     */
    private fun checkMemberIsOnMic(memberModel: VoiceMemberModel?): Boolean {
        memberModel ?: return true
        val micMap = ChatroomCacheManager.cacheManager.getMicInfoMap()
        micMap?.forEach { (t, u) ->
            val micMember = GsonTools.toBean(u, VoiceMicInfoModel::class.java)
            if (TextUtils.equals(memberModel.chatUid, micMember?.member?.chatUid)) {
                return true
            }
        }
        return false
    }

    /**
     * Reject submit mic
     *
     */
    fun rejectSubmitMic() {

    }

    /**
     * Cancel submit mic
     *
     * @param chatroomId
     * @param chatUid
     * @param callback
     */
    fun cancelSubmitMic(chatroomId: String, chatUid: String, callback: CallBack) {
        val attributeMap = mutableMapOf<String, String>()
        val userBeam = ChatroomCacheManager.cacheManager.getMember(chatUid)
        attributeMap["user"] = GsonTools.beanToString(userBeam).toString()
        attributeMap["chatroomId"] = chatroomId
        sendChatroomEvent(true, ownerBean.chatUid, CustomMsgType.CHATROOM_CANCEL_APPLY_SITE, attributeMap, callback)
    }

    /**
     * Fetch room invite members
     *
     * @return
     */
    fun fetchRoomInviteMembers():MutableList<VoiceMemberModel> {
        return ChatroomCacheManager.cacheManager.getInvitationList()
    }

    /**
     * Fetch room members
     *
     * @return
     */
    fun fetchRoomMembers():MutableList<VoiceMemberModel> {
        return ChatroomCacheManager.cacheManager.getMemberList()
    }

    /**
     * Invitation mic
     *
     * @param chatUid
     * @param micIndex
     * @param callback
     */
    fun invitationMic(chatUid: String, micIndex: Int, callback: CallBack) {
        val attributeMap = mutableMapOf<String, String>()
        val userBeam = ChatroomCacheManager.cacheManager.getMember(chatUid)
        userBeam?.micIndex = micIndex
        attributeMap["user"] = GsonTools.beanToString(userBeam).toString()
        attributeMap["chatroomId"] = ChatroomIMManager.getInstance().currentRoomId
        sendChatroomEvent(true, chatUid, CustomMsgType.CHATROOM_INVITE_SITE, attributeMap, callback)
    }

    /**
     * Refuse invite to mic
     *
     * @param chatUid
     * @param callback
     */
    fun refuseInviteToMic(chatUid: String, callback: CallBack) {
        val attributeMap = mutableMapOf<String, String>()
        attributeMap["chatroomId"] = ChatroomIMManager.getInstance().currentRoomId
        sendChatroomEvent(true, ownerBean.chatUid, CustomMsgType.CHATROOM_INVITE_REFUSED_SITE, attributeMap, callback)
    }

    /**
     * Accept mic seat invitation
     *
     * @param chatUid
     * @param micIndex
     * @param callback
     */
    fun acceptMicSeatInvitation(chatUid: String,micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        val memberBean = ChatroomCacheManager.cacheManager.getMember(chatUid)
        if (memberBean != null) {
            if (micIndex != -1) {
                val micInfoModel = getMicInfo(micIndex)
                memberBean.micIndex = if (micInfoModel?.micStatus == MicStatus.Idle || micInfoModel?.micStatus==MicStatus.ForceMute) micIndex else getFirstFreeMic()
            } else {
                memberBean.micIndex = getFirstFreeMic()
            }
        }
        ThreadManager.getInstance().runOnIOThread {
            if (checkMemberIsOnMic(memberBean)) return@runOnIOThread
            memberBean?.let {
                updateMicByResult(memberBean,
                    it.micIndex, MicClickAction.Accept, true, callback)
            }
        }
    }

    /////////////////////////// room ///////////////////////////////

    /**
     * Update announcement
     *
     * @param content
     * @param callback
     */
    fun updateAnnouncement(content: String, callback: CallBack) {
        roomManager.asyncUpdateChatRoomAnnouncement(roomId, content, object : CallBack {
            override fun onSuccess() {
                callback.onSuccess()
            }

            override fun onError(code: Int, error: String?) {
                callback.onError(code, error)
            }
        })
    }

    /**
     * Enable robot
     *
     * @param enable
     * @param callback
     */
    fun enableRobot(enable: Boolean, callback: ValueCallBack<Boolean>) {
        val attributeMap = mutableMapOf<String, String>()
        val currentUser = VoiceBuddyFactory.get().getVoiceBuddy().chatUserName()
        var robot6 = VoiceMicInfoModel()
        var robot7 = VoiceMicInfoModel()
        var isEnable: String
        if (TextUtils.equals(ownerBean.chatUid, currentUser)) {
            if (enable) {
                robot6.micIndex = 6
                robot6.micStatus = MicStatus.BotActivated
                robot7.micIndex = 7
                robot7.micStatus = MicStatus.BotActivated
                isEnable = "1"
            } else {
                robot6.micIndex = 6
                robot6.micStatus = MicStatus.BotInactive
                robot7.micIndex = 7
                robot7.micStatus = MicStatus.BotInactive
                isEnable = "0"
            }
            attributeMap["mic_6"] = GsonTools.beanToString(robot6).toString()
            attributeMap["mic_7"] = GsonTools.beanToString(robot7).toString()
            attributeMap["use_robot"] = isEnable
            roomManager.asyncSetChatroomAttributesForced(
                roomId, attributeMap, true
            ) { code, result ->
                if (code == 0 && result.isEmpty()) {
                    val map = mutableMapOf<Int, VoiceMicInfoModel>()
                    map[6] = robot6
                    map[7] = robot7
                    ChatroomCacheManager.cacheManager.setMicInfo(attributeMap)
                    callback.onSuccess(true)
                    "enableRobot($enable) update result onSuccess: ".logD(TAG)
                } else {
                    callback.onError(code, result.toString())
                    "enableRobot($enable) update result onError: $code $result ".logE(TAG)
                }
            }
        }
    }

    /**
     * Update robot volume
     *
     * @param value
     * @param callback
     */
    fun updateRobotVolume(value: Int, callback: CallBack) {
        roomManager.asyncSetChatroomAttribute(roomId, "robot_volume", value.toString(), true, object :
            CallBack {
            override fun onSuccess() {
                callback.onSuccess()
            }

            override fun onError(code: Int, error: String?) {
                callback.onError(code, error)
            }
        })
    }

    /**
     * Update the specified microphone seat information and return the successfully updated seat information.
     * 0:Normal state
     * 1:Muted
     * 2:Banned
     * 3:Locked
     * 4:Locked and banned
     * -1:Idle
     * 5:Robot-exclusive active state
     * -2:Robot-exclusive closed state
     */
    private fun updateMicByResult(member: VoiceMemberModel? = null,
                                  micIndex: Int, @MicClickAction clickAction: Int, isForced: Boolean, callback: ValueCallBack<VoiceMicInfoModel>
    ) {
        val voiceMicInfo = getMicInfo(micIndex) ?: return
        updateMicStatusByAction(voiceMicInfo, clickAction, member)
        voiceMicInfo.micIndex = micIndex
        val value = GsonTools.beanToString(voiceMicInfo) ?: return
        if (isForced) {
            roomManager.asyncSetChatroomAttributeForced(roomId, getMicIndex(micIndex),
                value, false, object : CallBack {
                    override fun onSuccess() {
                        val attributeMap = mutableMapOf<String, String>()
                        attributeMap[getMicIndex(micIndex)] = value
                        ChatroomCacheManager.cacheManager.setMicInfo(attributeMap)
                        callback.onSuccess(voiceMicInfo)
                        "Forced updateMic onSuccess: ".logD(TAG)
                    }

                    override fun onError(code: Int, desc: String?) {
                        callback.onError(code, desc)
                        "Forced updateMic onError: $code $desc".logE(TAG)
                    }
                })
        } else {
            roomManager.asyncSetChatroomAttribute(roomId, getMicIndex(micIndex),
                value, false, object : CallBack {
                    override fun onSuccess() {
                        val attributeMap = mutableMapOf<String, String>()
                        attributeMap[getMicIndex(micIndex)] = value
                        ChatroomCacheManager.cacheManager.setMicInfo(attributeMap)
                        callback.onSuccess(voiceMicInfo)
                        "updateMic onSuccess: ".logD(TAG)
                    }

                    override fun onError(code: Int, desc: String?) {
                        callback.onError(code, desc)
                        "updateMic onError: $code $desc".logE(TAG)
                    }
                })
        }
    }

    /**
     * Update mic status by action
     *
     * @param micInfo
     * @param action
     * @param memberBean
     */
    private fun updateMicStatusByAction(micInfo: VoiceMicInfoModel, @MicClickAction action: Int, memberBean: VoiceMemberModel? = null) {
        when (action) {
            MicClickAction.ForbidMic -> {
                if (micInfo.micStatus == MicStatus.Lock) {
                    micInfo.micStatus = MicStatus.LockForceMute
                } else {
                    micInfo.micStatus = MicStatus.ForceMute
                }
            }
            MicClickAction.UnForbidMic -> {
                if (micInfo.micStatus == MicStatus.LockForceMute) {
                    micInfo.micStatus = MicStatus.Lock
                } else {
                    if (micInfo.member == null) {
                        micInfo.micStatus = MicStatus.Idle
                    } else {
                        micInfo.micStatus = MicStatus.Normal
                    }
                }
            }
            MicClickAction.Mute -> {
                micInfo.member?.micStatus = 0
            }
            MicClickAction.UnMute -> {
                if (micInfo.member == null) {
                    micInfo.micStatus = MicStatus.Idle
                } else {
                    micInfo.micStatus = MicStatus.Normal
                    micInfo.member?.micStatus = 1
                }
            }
            MicClickAction.Lock -> {
                if (micInfo.micStatus == MicStatus.ForceMute) {
                    micInfo.micStatus = MicStatus.LockForceMute
                } else {
                    micInfo.micStatus = MicStatus.Lock
                }
                micInfo.member = null
            }
            MicClickAction.UnLock -> {
                if (micInfo.micStatus == MicStatus.LockForceMute) {
                    micInfo.micStatus = MicStatus.ForceMute
                } else {
                    if (micInfo.member == null) {
                        micInfo.micStatus = MicStatus.Idle
                    } else {
                        micInfo.micStatus = MicStatus.Normal
                    }
                }
            }
            MicClickAction.KickOff -> {
                if (micInfo.micStatus == MicStatus.ForceMute) {
                    micInfo.micStatus = MicStatus.ForceMute
                } else {
                    micInfo.micStatus = MicStatus.Idle
                }
                micInfo.member = null
            }
            MicClickAction.OffStage -> {
                if (micInfo.micStatus == MicStatus.ForceMute) {
                    micInfo.micStatus = MicStatus.ForceMute
                } else {
                    micInfo.micStatus = MicStatus.Idle
                }
                micInfo.member = null
            }
            MicClickAction.Accept -> {
                "MicClickAction.Accept: ${micInfo.micStatus}".logD(TAG)
                if (micInfo.micStatus == -1){
                    micInfo.micStatus = MicStatus.Normal
                }
                if (memberBean != null){
                    micInfo.member = memberBean
                }
            }
            MicClickAction.Invite -> {
                "MicClickAction.Invite: ${micInfo.micStatus}".logD(TAG)
                if (micInfo.micStatus == -1){
                    micInfo.micStatus = MicStatus.Normal
                }
                if (memberBean != null){
                    micInfo.member = memberBean
                }
            }
        }
    }

    /**
     * Update rank list
     *
     * @param chatUid
     * @param giftBean
     * @param callback
     */
    fun updateRankList(chatUid:String, giftBean: VoiceGiftModel, callback: CallBack){
        val rankMap = ChatroomCacheManager.cacheManager.getRankMap()
        val rankingList = mutableListOf<VoiceRankUserModel>()
        var voiceRankModel = rankMap[chatUid]
        if (voiceRankModel == null){
            voiceRankModel = VoiceRankUserModel()
        }
        var newAmount = 0
        val name = giftBean.userName
        val portrait = giftBean.portrait
        val count = giftBean.gift_count?.toInt()
        val price = giftBean.gift_price?.toInt()
        if (count != null && price !=null){
            newAmount = count * price
        }
        val oldAmount = voiceRankModel.amount
        voiceRankModel.amount = oldAmount + newAmount
        voiceRankModel.chatUid = chatUid
        voiceRankModel.name = name
        voiceRankModel.portrait = portrait
        rankMap[chatUid] = voiceRankModel
        for (entry in rankMap.entries) {
            rankingList.add(entry.value)
        }
        roomManager.asyncSetChatroomAttributeForced(roomId,"ranking_list", GsonTools.beanToString(rankingList),
            false,object : CallBack{
            override fun onSuccess() {
                ChatroomCacheManager.cacheManager.setRankList(voiceRankModel)
                callback.onSuccess()
            }

            override fun onError(code: Int, error: String?) {
                callback.onError(code,error)
            }
        })
    }

    /**
     * Update gift amount
     *
     * @param chatUid
     * @param newAmount
     * @param callback
     */
    fun updateGiftAmount(chatUid: String,newAmount:Int,callback: CallBack){
        if (TextUtils.equals(ownerBean.chatUid,chatUid)){
            var giftAmount = ChatroomCacheManager.cacheManager.getGiftAmountCache()
            giftAmount += newAmount
            roomManager.asyncSetChatroomAttribute(roomId,"gift_amount",giftAmount.toString(),true,object :
                CallBack{
                override fun onSuccess() {
                    "update giftAmount onSuccess: $giftAmount".logD(TAG)
                    ChatroomCacheManager.cacheManager.updateGiftAmountCache(newAmount)
                    callback.onSuccess()
                }

                override fun onError(code: Int, error: String?) {
                    "update giftAmount onError: $code $error".logE(TAG)
                    callback.onError(code,error)
                }
            })
        }
    }

    /**
     * Fetch gift contribute
     *
     * @param callback
     */
    fun fetchGiftContribute(callback: ValueCallBack<List<VoiceRankUserModel>>){
        val rankingList = ChatroomCacheManager.cacheManager.getRankList()
        if (rankingList.isEmpty()){
            val keyList: MutableList<String> = java.util.ArrayList()
            keyList.add("ranking_list")
            roomManager.asyncFetchChatroomAttributesFromServer(roomId,keyList,object :
                ValueCallBack<MutableMap<String, String>>{
                override fun onSuccess(value: MutableMap<String, String>) {
                    ThreadManager.getInstance().runOnMainThread{
                        value["ranking_list"]?.let {
                            val rankList = GsonTools.toList(it, VoiceRankUserModel::class.java)
                            rankList?.forEach { bean ->
                                ChatroomCacheManager.cacheManager.setRankList(bean)
                            }
                            callback.onSuccess(rankList)
                        }
                        "getRankList onSuccess: $value".logD(TAG)
                    }
                }

                override fun onError(code: Int, errorMsg: String?) {
                    ThreadManager.getInstance().runOnMainThread{
                        callback.onError(code,errorMsg)
                        "getRankList onError: $code $errorMsg".logE(TAG)
                    }
                }
            })
        }else{
            callback.onSuccess(rankingList)
        }
    }

    /**
     * Get member from server
     *
     * @param callback
     */
    fun getMemberFromServer(callback: ValueCallBack<List<VoiceMemberModel>>){
        ChatroomCacheManager.cacheManager.getMemberList()
        val keyList: MutableList<String> = mutableListOf("member_list")
        roomManager.asyncFetchChatroomAttributesFromServer(roomId,keyList,object :
            ValueCallBack<MutableMap<String, String>>{
            override fun onSuccess(value: MutableMap<String, String>) {
                GsonTools.toList(value["member_list"], VoiceMemberModel::class.java)?.let { memberList ->
                    callback.onSuccess(memberList)
                }
            }

            override fun onError(code: Int, errorMsg: String?) {
                callback.onError(code,errorMsg)
            }
        })
    }

    /**
     * Get my self model
     *
     * @return
     */
     fun getMySelfModel(): VoiceMemberModel {
        var micIndex : Int = -1
        if (TextUtils.equals(ownerBean.chatUid, VoiceBuddyFactory.get().getVoiceBuddy().chatUserName())) {
            micIndex = 0
        }
        return VoiceMemberModel(
            userId = VoiceBuddyFactory.get().getVoiceBuddy().userId(),
            chatUid = VoiceBuddyFactory.get().getVoiceBuddy().chatUserName(),
            nickName = VoiceBuddyFactory.get().getVoiceBuddy().nickName(),
            portrait = VoiceBuddyFactory.get().getVoiceBuddy().headUrl(),
            rtcUid = VoiceBuddyFactory.get().getVoiceBuddy().rtcUid(),
            micIndex = micIndex).also {
                "getMySelfModel:$it".logD(TAG)
        }
    }

    /**
     * Add member list by self
     *
     * @param memberList
     * @param callback
     */
    fun addMemberListBySelf(memberList:List<VoiceMemberModel>, callback: ValueCallBack<List<VoiceMemberModel>>){
        val newMemberList = memberList.toMutableList()
        newMemberList.add(getMySelfModel())
        val member = GsonTools.beanToString(memberList)
        roomManager.asyncSetChatroomAttributeForced(roomId,
            "member_list",member,false,object : CallBack{
            override fun onSuccess() {
                callback.onSuccess(newMemberList)
                "addMemberListBySelf onSuccess: ".logD(TAG)
            }

            override fun onError(code: Int, error: String?) {
                callback.onError(code,error)
                "addMemberListBySelf onError: $code $error".logE(TAG)
            }
        })
    }

    /**
     * Update room member
     *
     * @param memberList
     * @param callback
     */
    fun updateRoomMember(memberList:List<VoiceMemberModel>,callback: CallBack){
        if (TextUtils.equals(ownerBean.chatUid, VoiceBuddyFactory.get().getVoiceBuddy().chatUserName())) {
            val member = GsonTools.beanToString(memberList)
            roomManager.asyncSetChatroomAttributeForced(roomId,
                "member_list",member,false,object : CallBack{
                    override fun onSuccess() {
                        callback.onSuccess()
                        "updateRoomMember onSuccess: ".logD(TAG)
                    }

                    override fun onError(code: Int, error: String?) {
                        callback.onError(code,error)
                        "updateRoomMember onError: $code $error".logE(TAG)
                    }
                })
        }
    }

    private fun sendChatroomEvent(
        isSingle: Boolean, chatUid: String?, eventType: CustomMsgType,
        params: MutableMap<String, String>, callback: CallBack
    ) {
        if (isSingle) {
            CustomMsgHelper.getInstance().sendCustomSingleMsg(chatUid,
                eventType.getName(), params, object : OnMsgCallBack() {
                    override fun onSuccess(message: ChatMessageData?) {
                        callback.onSuccess()
                        "sendCustomSingleMsg onSuccess: $message".logD(TAG)
                    }

                    override fun onError(messageId: String?, code: Int, desc: String?) {
                        callback.onError(code, desc)
                        "sendCustomSingleMsg onError: $code $desc".logE(TAG)
                    }
                })
        } else {
            CustomMsgHelper.getInstance().sendCustomMsg(roomId, params, object : OnMsgCallBack() {
                override fun onSuccess(message: ChatMessageData?) {
                    callback.onSuccess()
                    "sendCustomMsg onSuccess: $message".logD(TAG)
                }

                override fun onError(messageId: String?, code: Int, desc: String?) {
                    super.onError(messageId, code, desc)
                    callback.onError(code, desc)
                    "sendCustomMsg onError: $code $desc".logE(TAG)
                }
            })
        }

    }

    /**
     *  按麦位顺序查询空麦位
     */
    private fun getFirstFreeMic(): Int {
        val indexList: MutableList<Int> = mutableListOf<Int>()
        val micInfo = ChatroomCacheManager.cacheManager.getMicInfoMap()
        if (micInfo != null) {
            for (mutableEntry in micInfo) {
                val bean = GsonTools.toBean(mutableEntry.value, VoiceMicInfoModel::class.java)
                if (bean != null ) {
                    "getFirstFreeMic: ${bean.micIndex}  ${bean.micStatus}".logE(TAG)
                    if(bean.micStatus == -1 || bean.micStatus == 2){
                        indexList.add(bean.micIndex)
                    }
                }
            }
        }
        indexList.sortBy { it }
        return indexList[0]
    }

    private fun getMicIndex(index: Int): String {
        return "mic_$index"
    }

    /**
     * Clear cache
     *
     */
    fun clearCache(){
        ChatroomCacheManager.cacheManager.clearAllCache()
    }

    /**
     * Update mic info cache
     *
     * @param kvMap
     */
    fun updateMicInfoCache(kvMap: Map<String,String>){
        ChatroomCacheManager.cacheManager.setMicInfo(kvMap)
    }
}