package io.agora.scene.voice.imkit.manager;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.agora.CallBack;
import io.agora.ChatRoomChangeListener;
import io.agora.ConnectionListener;
import io.agora.ValueCallBack;
import io.agora.chat.ChatClient;
import io.agora.chat.ChatMessage;
import io.agora.chat.ChatRoom;
import io.agora.chat.ChatRoomManager;
import io.agora.chat.Conversation;
import io.agora.chat.CustomMessageBody;
import io.agora.chat.TextMessageBody;
import io.agora.chat.adapter.EMAChatRoomManagerListener;
import io.agora.scene.voice.global.VoiceBuddyFactory;
import io.agora.scene.voice.imkit.bean.ChatMessageData;
import io.agora.scene.voice.imkit.custorm.CustomMsgHelper;
import io.agora.scene.voice.imkit.custorm.CustomMsgType;
import io.agora.scene.voice.imkit.custorm.MsgConstant;
import io.agora.scene.voice.imkit.custorm.OnCustomMsgReceiveListener;
import io.agora.scene.voice.imkit.custorm.OnMsgCallBack;
import io.agora.scene.voice.model.VoiceGiftModel;
import io.agora.scene.voice.model.VoiceMemberModel;
import io.agora.scene.voice.model.VoiceMicInfoModel;
import io.agora.scene.voice.model.VoiceRankUserModel;
import io.agora.scene.voice.model.VoiceRoomInfo;
import io.agora.scene.voice.model.VoiceRoomModel;
import io.agora.scene.voice.service.VoiceRoomServiceKickedReason;
import io.agora.util.EMLog;
import io.agora.voice.common.utils.GsonTools;
import io.agora.voice.common.utils.LogTools;
import io.agora.voice.common.utils.ThreadManager;

public class ChatroomIMManager implements ChatRoomChangeListener, ConnectionListener {
    private static ChatroomIMManager instance;

    private ChatroomIMManager() {
    }

    private String chatroomId;
    private boolean isOwner;
    private ArrayList<ChatMessageData> data = new ArrayList<>();
    public OnChatroomEventReceiveListener chatroomEventListener;
    public OnChatroomConnectionListener chatroomConnectionListener;
    private ChatroomProtocolDelegate delegate;
    private ChatroomCacheManager cacheManager = ChatroomCacheManager.Companion.getCacheManager();
    private static final String TAG = "ChatroomIMManager";

    public static ChatroomIMManager getInstance() {
        if (instance == null) {
            synchronized (ChatroomIMManager.class) {
                if (instance == null) {
                    instance = new ChatroomIMManager();
                }
            }
        }
        return instance;
    }

    public void init(String chatroomId, boolean isOwner) {
        this.chatroomId = chatroomId;
        this.isOwner = isOwner;
        //设置消息监听
        setCustomMsgListener();
        //设置聊天室状态变化监听
        setChatRoomChangeListener();
        //设置连接监听
        setConnectionListener();
        //设置相关的房间信息
        CustomMsgHelper.getInstance().setChatRoomInfo(chatroomId);
        //设置语聊房协议代理
        delegate = new ChatroomProtocolDelegate(chatroomId);
        clearCache();
    }

    public boolean isOwner() {
        return this.isOwner;
    }

    public String getCurrentRoomId() {
        return this.chatroomId;
    }

    private ChatRoomManager getChatRoomManager() {
        return ChatClient.getInstance().chatroomManager();
    }

    public void setCustomMsgListener() {
        CustomMsgHelper.getInstance().init();
    }

    public void setChatRoomChangeListener() {
        ChatClient.getInstance().chatroomManager().addChatRoomChangeListener(this);
    }

    public void removeChatRoomChangeListener() {
        ChatClient.getInstance().chatroomManager().removeChatRoomListener(this);
    }

    public void setConnectionListener() {
        ChatClient.getInstance().addConnectionListener(this);
    }

    public void removeChatRoomConnectionListener() {
        ChatClient.getInstance().removeConnectionListener(this);
    }

    public void setChatRoomEventListener(OnChatroomEventReceiveListener listener) {
        this.chatroomEventListener = listener;
    }

    public void setChatRoomConnectionListener(OnChatroomConnectionListener listener) {
        this.chatroomConnectionListener = listener;
    }

    public void removeCustomMsgListener() {
        CustomMsgHelper.getInstance().removeListener();
    }

    public void sendTxtMsg(String content, String nickName, OnMsgCallBack callBack) {
        ChatMessage message = ChatMessage.createTextSendMessage(content, chatroomId);
        message.setAttribute("userName", nickName);
        message.setChatType(ChatMessage.ChatType.ChatRoom);
        message.setMessageStatusCallback(new CallBack() {
            @Override
            public void onSuccess() {
                CustomMsgHelper.getInstance().addSendText(parseChatMessage(message));
                callBack.onSuccess(parseChatMessage(message));
            }

            @Override
            public void onError(int i, String s) {
                callBack.onError(i, s);
            }

            @Override
            public void onProgress(int i, String s) {
                callBack.onProgress(i, s);
            }
        });
        ChatClient.getInstance().chatManager().sendMessage(message);
    }

    public void saveWelcomeMsg(String content, String nick) {
        ChatMessage message = ChatMessage.createSendMessage(ChatMessage.Type.TXT);
        message.setChatType(ChatMessage.ChatType.ChatRoom);
        message.setTo(chatroomId);
        TextMessageBody textMessageBody = new TextMessageBody(content);
        message.setBody(textMessageBody);
        message.setAttribute("userName", nick);
        ChatClient.getInstance().chatManager().saveMessage(message);
    }

    public ArrayList<ChatMessageData> getMessageData(String chatroomId) {
        data.clear();
        Conversation conversation = ChatClient.getInstance().chatManager().getConversation(chatroomId, Conversation.ConversationType.ChatRoom, true);
        if (conversation != null) {
            EMLog.e("getMessageData", conversation.getAllMessages().size() + "");
            for (ChatMessage allMessage : conversation.getAllMessages()) {
                if (allMessage.getBody() instanceof TextMessageBody
                        || allMessage.getBody() instanceof CustomMessageBody &&
                        (((CustomMessageBody) allMessage.getBody()).event()).equals(CustomMsgType.CHATROOM_SYSTEM.getName())) {
                    data.add(parseChatMessage(allMessage));
                }
            }
        }
        return data;
    }

    public ChatMessageData parseChatMessage(ChatMessage chatMessage) {
        ChatMessageData chatMessageData = new ChatMessageData();
        chatMessageData.setForm(chatMessage.getFrom());
        chatMessageData.setTo(chatMessage.getTo());
        chatMessageData.setConversationId(chatMessage.conversationId());
        chatMessageData.setMessageId(chatMessage.getMsgId());
        if (chatMessage.getBody() instanceof TextMessageBody) {
            chatMessageData.setType("text");
            chatMessageData.setContent(((TextMessageBody) chatMessage.getBody()).getMessage());
        } else if (chatMessage.getBody() instanceof CustomMessageBody) {
            chatMessageData.setType("custom");
            chatMessageData.setEvent(((CustomMessageBody) chatMessage.getBody()).event());
            chatMessageData.setCustomParams(((CustomMessageBody) chatMessage.getBody()).getParams());
        }
        chatMessageData.setExt(chatMessage.ext());
        return chatMessageData;
    }

    public void setOnCustomMsgReceiveListener(OnCustomMsgReceiveListener listener) {
        CustomMsgHelper.getInstance().setOnCustomMsgReceiveListener(listener);
    }

    public String getUserName(ChatMessageData msg) {
        String userName = "";
        Map<String, String> params = msg.getCustomParams();
        if (params.containsKey("userName")) {
            userName = params.get("userName");
        }
        if (TextUtils.isEmpty(userName)) {
            Map<String, Object> ext = msg.getExt();
            if (ext.containsKey("userName")) {
                userName = (String) ext.get("userName");
            }
        }
        LogTools.d("Helper", "getUserName: " + userName);
        return userName;
    }

    public String getSystemUserName(ChatMessageData msg) {
        String jsonString = "";
        String userName = "";
        Map<String, String> params = msg.getCustomParams();
        if (params.containsKey("user")) {
            jsonString = params.get("user");
            LogTools.d("getSystemUserName", "jsonString: " + jsonString);
            if (!TextUtils.isEmpty(jsonString)) {
                try {
                    assert jsonString != null;
                    JSONObject jsonObject = new JSONObject(jsonString);
                    userName = jsonObject.getString("name");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        LogTools.d("getSystemUserName", "userName: " + userName);
        return userName;
    }

    public String getUserPortrait(ChatMessageData msg) {
        String userPortrait = "";
        Map<String, String> ext = msg.getCustomParams();
        if (ext.containsKey("portrait")) {
            userPortrait = ext.get("portrait");
        }
        return userPortrait;
    }

    public VoiceGiftModel getGiftModel(ChatMessageData msg) {
        Map<String, String> giftMap = CustomMsgHelper.getInstance().getCustomMsgParams(msg);
        if (giftMap != null) {
            VoiceGiftModel voiceGiftModel = new VoiceGiftModel();
            voiceGiftModel.setGift_id(giftMap.get(MsgConstant.CUSTOM_GIFT_KEY_ID));
            voiceGiftModel.setGift_count(giftMap.get(MsgConstant.CUSTOM_GIFT_KEY_NUM));
            voiceGiftModel.setGift_name(giftMap.get(MsgConstant.CUSTOM_GIFT_NAME));
            voiceGiftModel.setGift_price(giftMap.get(MsgConstant.CUSTOM_GIFT_PRICE));
            voiceGiftModel.setUserName(giftMap.get(MsgConstant.CUSTOM_GIFT_USERNAME));
            voiceGiftModel.setPortrait(giftMap.get(MsgConstant.CUSTOM_GIFT_PORTRAIT));
            return voiceGiftModel;
        }
        return null;
    }

    public VoiceMemberModel getVoiceMemberModel(ChatMessageData msg) {
        Map<String, String> map = CustomMsgHelper.getInstance().getCustomMsgParams(msg);
        if (map.containsKey("user")) {
            return GsonTools.toBean(map.get("user"), VoiceMemberModel.class);
        }
        return null;
    }

    public VoiceRoomServiceKickedReason getKickReason(int reason) {
        switch (reason) {
            case EMAChatRoomManagerListener.BE_KICKED:
                return VoiceRoomServiceKickedReason.removed;
            case EMAChatRoomManagerListener.DESTROYED:
                return VoiceRoomServiceKickedReason.destroyed;
            case EMAChatRoomManagerListener.BE_KICKED_FOR_OFFLINE:
                return VoiceRoomServiceKickedReason.offLined;
            default:
                return null;
        }
    }


    @Override
    public void onChatRoomDestroyed(String roomId, String s1) {
        if (chatroomEventListener != null && TextUtils.equals(roomId, chatroomId))
            chatroomEventListener.onRoomDestroyed(roomId);
    }

    @Override
    public void onMemberJoined(String roomId, String s1) {
        if (chatroomEventListener != null && TextUtils.equals(roomId, chatroomId))
            chatroomEventListener.onMemberJoined(roomId, s1);
    }

    @Override
    public void onMemberExited(String roomId, String s1, String s2) {
        if (chatroomEventListener != null && TextUtils.equals(roomId, chatroomId))
            chatroomEventListener.onMemberExited(roomId, s1, s2);
    }

    @Override
    public void onRemovedFromChatRoom(int i, String roomId, String s1, String s2) {
        if (chatroomEventListener != null && TextUtils.equals(roomId, chatroomId))
            chatroomEventListener.onKicked(roomId, i);
    }

    @Override
    public void onMuteListAdded(String s, List<String> list, long l) {

    }

    @Override
    public void onMuteListRemoved(String s, List<String> list) {

    }

    @Override
    public void onWhiteListAdded(String s, List<String> list) {

    }

    @Override
    public void onWhiteListRemoved(String s, List<String> list) {

    }

    @Override
    public void onAllMemberMuteStateChanged(String s, boolean b) {

    }

    @Override
    public void onAdminAdded(String s, String s1) {

    }

    @Override
    public void onAdminRemoved(String s, String s1) {

    }

    @Override
    public void onOwnerChanged(String s, String s1, String s2) {

    }

    @Override
    public void onAnnouncementChanged(String roomId, String announcement) {
        if (chatroomEventListener != null && TextUtils.equals(roomId, chatroomId))
            chatroomEventListener.onAnnouncementChanged(roomId, announcement);
    }

    @Override
    public void onAttributesUpdate(String roomId, Map<String, String> attributeMap, String from) {
        if (chatroomEventListener != null && TextUtils.equals(roomId, chatroomId))
            chatroomEventListener.onAttributesUpdate(roomId, attributeMap, from);
    }

    @Override
    public void onAttributesRemoved(String roomId, List<String> keyList, String from) {
        if (chatroomEventListener != null && TextUtils.equals(roomId, chatroomId))
            chatroomEventListener.onAttributesRemoved(roomId, keyList, from);
    }

    //////////////////////Connection///////////////////////////

    @Override
    public void onConnected() {
        if (chatroomConnectionListener != null)
            chatroomConnectionListener.onConnected();
    }

    @Override
    public void onDisconnected(int code) {
        if (chatroomConnectionListener != null)
            chatroomConnectionListener.onDisconnected(code);
    }

    @Override
    public void onTokenExpired() {
        if (chatroomConnectionListener != null)
            chatroomConnectionListener.onTokenExpired();
    }

    @Override
    public void onTokenWillExpire() {
        if (chatroomConnectionListener != null)
            chatroomConnectionListener.onTokenWillExpire();
    }

    public void renewToken(String newToken) {
        ChatClient.getInstance().renewToken(newToken);
    }

    public boolean isLoggedIn() {
        return ChatClient.getInstance().isLoggedIn();
    }

    public void login(String uid, String token, CallBack callBack) {
        ChatClient.getInstance().loginWithToken(uid, token, new CallBack() {
            @Override
            public void onSuccess() {
                ThreadManager.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onSuccess();
                    }
                });
                LogTools.d("ChatroomConfigManager", "Login success");
            }

            @Override
            public void onError(int code, String msg) {
                ThreadManager.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onError(code, msg);
                    }
                });
                LogTools.e("ChatroomConfigManager", "Login onError code:" + code + " desc: " + msg);
            }
        });
    }

    public void login(String uid, String token) {
        ChatClient.getInstance().loginWithAgoraToken(uid, token, new CallBack() {
            @Override
            public void onSuccess() {
                LogTools.d("ChatroomConfigManager", "Login success");
            }

            @Override
            public void onError(int code, String msg) {
                LogTools.e("ChatroomConfigManager", "Login onError code:" + code + " desc: " + msg);
            }
        });
    }

    public void logout(boolean unbind, CallBack callBack) {
        ChatClient.getInstance().logout(unbind, new CallBack() {
            @Override
            public void onSuccess() {
                callBack.onSuccess();
            }

            @Override
            public void onError(int i, String s) {
                callBack.onError(i, s);
            }
        });
    }

    public void logout(boolean unbind) {
        if (!ChatClient.getInstance().isSdkInited()) return;
        ChatClient.getInstance().logout(unbind, new CallBack() {
            @Override
            public void onSuccess() {
                EMLog.d(TAG, "logout onSuccess");
            }

            @Override
            public void onError(int code, String desc) {
                EMLog.e(TAG, "logout onError code: " + code + "  " + desc);
            }
        });
    }

    public void joinRoom(String chatroomId, ValueCallBack<ChatRoom> callBack) {
        ChatClient.getInstance().chatroomManager()
                .joinChatRoom(chatroomId, new ValueCallBack<ChatRoom>() {
                    @Override
                    public void onSuccess(ChatRoom value) {
                        ThreadManager.getInstance().runOnMainThreadDelay(new Runnable() {
                            @Override
                            public void run() {
                                callBack.onSuccess(value);
                            }
                        }, 200);
                    }

                    @Override
                    public void onError(int error, String errorMsg) {
                        ThreadManager.getInstance().runOnMainThread(new Runnable() {
                            @Override
                            public void run() {
                                callBack.onError(error, errorMsg);
                            }
                        });
                    }
                });
    }

    public void leaveChatRoom(String chatroomId) {
        ChatClient.getInstance().chatroomManager().leaveChatRoom(chatroomId);
    }

    public void asyncDestroyChatRoom(String chatroomId, CallBack callBack) {
        ChatClient.getInstance().chatroomManager().asyncDestroyChatRoom(chatroomId, new CallBack() {
            @Override
            public void onSuccess() {
                ThreadManager.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onSuccess();
                    }
                });
            }

            @Override
            public void onError(int code, String error) {
                ThreadManager.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        callBack.onError(code, error);
                    }
                });
            }
        });
    }

    public VoiceMemberModel getMySelfModel() {
        return delegate.getMySelfModel();
    }

    public void clearCache() {
        delegate.clearCache();
    }

    public void initMicInfo(CallBack callBack) {
        VoiceMemberModel voiceMemberModel = new VoiceMemberModel(
                VoiceBuddyFactory.get().getVoiceBuddy().userId(),
                VoiceBuddyFactory.get().getVoiceBuddy().chatUserName(),
                VoiceBuddyFactory.get().getVoiceBuddy().nickName(),
                VoiceBuddyFactory.get().getVoiceBuddy().headUrl(),
                VoiceBuddyFactory.get().getVoiceBuddy().rtcUid(),
                0, 1);
        LogTools.d(TAG, "initMicInfo:" + voiceMemberModel);
        delegate.initMicInfo(voiceMemberModel, callBack);
    }

    public void fetchRoomDetail(VoiceRoomModel voiceRoomModel, ValueCallBack<VoiceRoomInfo> callBack) {
        // 麦位信息
        VoiceMemberModel owner = voiceRoomModel.getOwner();
        if (owner != null && TextUtils.equals(owner.getUserId(), VoiceBuddyFactory.get().getVoiceBuddy().userId())) {
            initMicInfo(new CallBack() {
                @Override
                public void onSuccess() {
                    delegate.fetchRoomDetail(voiceRoomModel, callBack);
                }

                @Override
                public void onError(int code, String error) {
                    callBack.onError(code, error);
                }
            });
        } else {
            delegate.fetchRoomDetail(voiceRoomModel, callBack);
        }
    }

    public void invitationMic(String chatUid, int micIndex, CallBack callBack) {
        delegate.invitationMic(chatUid, micIndex, callBack);
    }


    public void forbidMic(int micIndex, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.forbidMic(micIndex, callBack);
    }

    public void unForbidMic(int micIndex, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.unForbidMic(micIndex, callBack);
    }

    public void lockMic(int micIndex, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.lockMic(micIndex, callBack);
    }

    public void unLockMic(int micIndex, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.unLockMic(micIndex, callBack);
    }

    public void kickOff(int micIndex, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.kickOff(micIndex, callBack);
    }

    public void leaveMic(int micIndex, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.leaveMic(micIndex, callBack);
    }

    public void muteLocal(int micIndex, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.muteLocal(micIndex, callBack);
    }

    public void unMuteLocal(int micIndex, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.unMuteLocal(micIndex, callBack);
    }

    public void changeMic(int oldIndex, int newIndex, ValueCallBack<Map<Integer, VoiceMicInfoModel>> callBack) {
        delegate.changeMic(oldIndex, newIndex, callBack);
    }


    public void acceptMicSeatInvitation(int micIndex, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.acceptMicSeatInvitation(VoiceBuddyFactory.get().getVoiceBuddy().chatUserName(), micIndex, callBack);
    }

    public void refuseInvite(String chatUid, CallBack callBack) {
        delegate.refuseInviteToMic(chatUid, callBack);
    }

    public void startMicSeatApply(int micIndex, CallBack callBack) {
        delegate.startMicSeatApply(micIndex, callBack);
    }

    public void acceptMicSeatApply(int micIndex, String chatUid, ValueCallBack<VoiceMicInfoModel> callBack) {
        delegate.acceptMicSeatApply(chatUid, micIndex, callBack);
    }

    public void cancelMicSeatApply(String chatroomId, String chatUid, CallBack callBack) {
        delegate.cancelSubmitMic(chatroomId, chatUid, callBack);
    }


    public void updateAnnouncement(String content, CallBack callBack) {
        delegate.updateAnnouncement(content, callBack);
    }

    public List<VoiceMemberModel> fetchRaisedList() {
        return delegate.fetchRaisedList();
    }

    public void enableRobot(Boolean enable, ValueCallBack<Boolean> callBack) {
        delegate.enableRobot(enable, callBack);
    }

    public void updateRobotVolume(int volume, CallBack callBack) {
        delegate.updateRobotVolume(volume, callBack);
    }

    public List<VoiceMemberModel> fetchRoomInviteMembers() {
        return delegate.fetchRoomInviteMembers();
    }

    public List<VoiceMemberModel> fetchRoomMembers() {
        return delegate.fetchRoomMembers();
    }

    public void updateRoomMembers(CallBack callBack) {
        delegate.updateRoomMember(cacheManager.getMemberList(), callBack);
    }

    public void fetchGiftContribute(ValueCallBack<List<VoiceRankUserModel>> callBack) {
        delegate.fetchGiftContribute(callBack);
    }

    public void updateRankList(String chatUid, VoiceGiftModel giftModel, CallBack callBack) {
        delegate.updateRankList(chatUid, giftModel, callBack);
    }

    public void updateMicInfoCache(Map<String, String> kvMap) {
        delegate.updateMicInfoCache(kvMap);
    }

    public void updateAmount(String chatUid, int amount, CallBack callBack) {
        delegate.updateGiftAmount(chatUid, amount, callBack);
    }

    /**
     * click count
     */
    public void increaseClickCount(String chatUid, CallBack callBack) {
        delegate.increaseClickCount(chatUid, callBack);
    }

    public void setGiftAmountCache(int amount) {
        cacheManager.setGiftAmountCache(amount);
    }

    public int getGiftAmountCache() {
        return cacheManager.getGiftAmountCache();
    }

    public void setClickCountCache(int count) {
        cacheManager.setClickCountCache(count);
    }

    /**
     * get click count
     */
    public int getClickCountCache() {
        return cacheManager.getClickCountCache();
    }

    public void setSubmitMicList(VoiceMemberModel voiceMemberModel) {
        cacheManager.setSubmitMicList(voiceMemberModel);
    }

    public void removeSubmitMember(String chatUid) {
        cacheManager.removeSubmitMember(chatUid);
    }

    public void setMemberList(VoiceMemberModel voiceMemberModel) {
        cacheManager.setMemberList(voiceMemberModel);
    }

    public boolean checkMember(String chatUid) {
        return cacheManager.getSubmitMic(chatUid) != null;
    }

    public boolean checkInvitationMember(String chatUid) {
        return cacheManager.checkInvitationByChatUid(chatUid);
    }

    public void removeMember(String chatUid) {
        cacheManager.removeMember(chatUid);
    }

    public void setRankList(VoiceRankUserModel voiceRankUserModel) {
        cacheManager.setRankList(voiceRankUserModel);
    }

    public List<VoiceRankUserModel> getRankList() {
        return cacheManager.getRankList();
    }

    public int getMicIndexByChatUid(String chatUid) {
        VoiceMicInfoModel bean = cacheManager.getMicInfoByChatUid(chatUid);
        if (bean != null) {
            return bean.getMicIndex();
        } else {
            return -1;
        }
    }

    public void removeMemberToRoom(List<String> userList, ValueCallBack<ChatRoom> callBack) {
        getChatRoomManager().asyncRemoveChatRoomMembers(chatroomId, userList, callBack);
    }
}
