package io.agora.scene.voice.imkit.custorm;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.CallBack;
import io.agora.MessageListener;
import io.agora.chat.ChatClient;
import io.agora.chat.ChatMessage;
import io.agora.chat.CustomMessageBody;
import io.agora.scene.voice.global.VoiceBuddyFactory;
import io.agora.scene.voice.imkit.bean.ChatMessageData;
import io.agora.scene.voice.imkit.manager.ChatroomIMManager;
import io.agora.scene.voice.model.VoiceRoomApply;
import io.agora.voice.common.utils.GsonTools;
import io.agora.voice.common.utils.LogTools;


public class CustomMsgHelper implements MessageListener {
    private static CustomMsgHelper instance;
    private CustomMsgHelper(){}

    private String chatRoomId;
    private OnCustomMsgReceiveListener listener;
    private ArrayList<ChatMessageData> AllGiftList = new ArrayList<>();
    private ArrayList<ChatMessageData> AllNormalList = new ArrayList<>();

    public static CustomMsgHelper getInstance() {
        if(instance == null) {
            synchronized (CustomMsgHelper.class) {
                if(instance == null) {
                    instance = new CustomMsgHelper();
                }
            }
        }
        return instance;
    }

    public void init() {
        ChatClient.getInstance().chatManager().addMessageListener(this);
    }

    public void setChatRoomInfo(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public void setOnCustomMsgReceiveListener(OnCustomMsgReceiveListener listener) {
        this.listener = listener;
    }

    public void removeListener() {
        ChatClient.getInstance().chatManager().removeMessageListener(this);
    }

    @Override
    public void onMessageReceived(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            if (message.getType() == ChatMessage.Type.TXT){
                AllNormalList.add(ChatroomIMManager.getInstance().parseChatMessage(message));
                if(listener != null) {
                    listener.onReceiveNormalMsg(ChatroomIMManager.getInstance().parseChatMessage(message));
                }
            }
            if(message.getType() != ChatMessage.Type.CUSTOM) {
                continue;
            }
            CustomMessageBody body = (CustomMessageBody) message.getBody();
            String event = body.event();
            CustomMsgType msgType = getCustomMsgType(event);

            if(msgType == null) {
                continue;
            }
            switch (msgType) {
                case CHATROOM_INVITE_SITE:
                    Map<String, String> inviteMap = getCustomMsgParams(ChatroomIMManager.getInstance().parseChatMessage(message));
                    if(listener != null) {
                        if (inviteMap.containsKey("chatroomId") && TextUtils.equals(chatRoomId,inviteMap.get("chatroomId"))){
                            listener.onReceiveInviteSite(ChatroomIMManager.getInstance().parseChatMessage(message));
                        }
                    }
                    break;
                case CHATROOM_APPLY_SITE:
                    Map<String, String> applyMap = getCustomMsgParams(ChatroomIMManager.getInstance().parseChatMessage(message));
                    if(listener != null ) {
                        if (applyMap.containsKey("chatroomId") && TextUtils.equals(chatRoomId,applyMap.get("chatroomId"))){
                            if (applyMap.containsKey("user")){
                                VoiceRoomApply voiceRoomApply = GsonTools.toBean(applyMap.get("user"), VoiceRoomApply.class);
                                if (voiceRoomApply != null && voiceRoomApply.getMember() != null){
                                    ChatroomIMManager.getInstance().setSubmitMicList(voiceRoomApply.getMember());
                                }
                            }
                            listener.onReceiveApplySite(ChatroomIMManager.getInstance().parseChatMessage(message));
                        }
                    }
                    break;
                case CHATROOM_CANCEL_APPLY_SITE:
                    if(listener != null) {
                        if (ChatroomIMManager.getInstance().checkMember(message.getFrom())){
                            ChatroomIMManager.getInstance().removeSubmitMember(message.getFrom());
                            listener.onReceiveCancelApplySite(ChatroomIMManager.getInstance().parseChatMessage(message));
                        }
                    }
                    break;
                case CHATROOM_INVITE_REFUSED_SITE:
                    if(listener != null) {
                        listener.onReceiveInviteRefusedSite(ChatroomIMManager.getInstance().parseChatMessage(message));
                    }
                    break;
                case CHATROOM_DECLINE_APPLY:
                    if(listener != null) {
                        listener.onReceiveDeclineApply(ChatroomIMManager.getInstance().parseChatMessage(message));
                    }
                    break;

            }
            if(message.getChatType() != ChatMessage.ChatType.GroupChat && message.getChatType() != ChatMessage.ChatType.ChatRoom) {
                continue;
            }
            String username = message.getTo();
            if(!TextUtils.equals(username, chatRoomId)) {
                continue;
            }
            if(TextUtils.isEmpty(event)) {
                continue;
            }
            switch (msgType) {
                case CHATROOM_GIFT:
                    AllGiftList.add(ChatroomIMManager.getInstance().parseChatMessage(message));
                    if(listener != null) {
                        listener.onReceiveGiftMsg(ChatroomIMManager.getInstance().parseChatMessage(message));
                    }
                    break;
                case CHATROOM_PRAISE:
                    if(listener != null) {
                        listener.onReceivePraiseMsg(ChatroomIMManager.getInstance().parseChatMessage(message));
                    }
                    break;
                case CHATROOM_SYSTEM:
                    AllNormalList.add(ChatroomIMManager.getInstance().parseChatMessage(message));
                    if (listener != null){
                        listener.onReceiveSystem(ChatroomIMManager.getInstance().parseChatMessage(message));
                    }
                    break;
            }
        }
    }

    @Override
    public void onCmdMessageReceived(List<ChatMessage> list) {

    }

    @Override
    public void onMessageRead(List<ChatMessage> list) {

    }

    @Override
    public void onMessageDelivered(List<ChatMessage> list) {

    }

    @Override
    public void onMessageRecalled(List<ChatMessage> list) {

    }

    @Override
    public void onMessageChanged(ChatMessage emMessage, Object o) {

    }

    public ArrayList<ChatMessageData> getGiftData(String chatRoomId){
        ArrayList<ChatMessageData> data = new ArrayList<>();
        for (ChatMessageData chatMessageData : AllGiftList) {
            if (TextUtils.equals(chatRoomId,chatMessageData.getConversationId())){
                data.add(chatMessageData);
            }
        }
        AllGiftList.removeAll(data);
        return data;
    }

    public ArrayList<ChatMessageData> getNormalData(String chatRoomId){
        ArrayList<ChatMessageData> data = new ArrayList<>();
        for (ChatMessageData chatMessageData : AllNormalList) {
            if (TextUtils.equals(chatRoomId,chatMessageData.getConversationId())){
                data.add(chatMessageData);
            }
        }
        AllNormalList.removeAll(data);
        return data;
    }

    public void clear(){
        AllGiftList.clear();
        AllNormalList.clear();
    }

    public void addSendText(ChatMessageData data){
        AllNormalList.add(data);
    }

    public void sendGiftMsg(String nickName,String portrait,String giftId,int num,String price,String giftName, OnMsgCallBack callBack) {
        Map<String, String> params = new HashMap<>();
        params.put(MsgConstant.CUSTOM_GIFT_KEY_ID, giftId);
        params.put(MsgConstant.CUSTOM_GIFT_KEY_NUM, String.valueOf(num));
        params.put(MsgConstant.CUSTOM_GIFT_NAME,giftName);
        params.put(MsgConstant.CUSTOM_GIFT_PRICE,price);
        params.put(MsgConstant.CUSTOM_GIFT_USERNAME,nickName);
        params.put(MsgConstant.CUSTOM_GIFT_PORTRAIT,portrait);
        sendGiftMsg(params, callBack);
    }

    public void sendSystemMsg(String ownerId,OnMsgCallBack callBack) {
        Map<String, String> params = new HashMap<>();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uid", VoiceBuddyFactory.get().getVoiceBuddy().userId());
            jsonObject.put("chat_uid",VoiceBuddyFactory.get().getVoiceBuddy().chatUserName());
            jsonObject.put("name",VoiceBuddyFactory.get().getVoiceBuddy().nickName());
            jsonObject.put("portrait",VoiceBuddyFactory.get().getVoiceBuddy().headUrl());
            jsonObject.put("rtc_uid",VoiceBuddyFactory.get().getVoiceBuddy().rtcUid());
            jsonObject.put("mic_index",TextUtils.equals(ownerId,VoiceBuddyFactory.get().getVoiceBuddy().chatUserName())? "0" : "-1");
            jsonObject.put("micStatus",1);
            params.put("user",jsonObject.toString());
            sendSystemMsg(params, callBack);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    public void sendSystemMsg(Map<String, String> params, final OnMsgCallBack callBack){
        if(params.size() <= 0) {
            return;
        }
        sendCustomMsg(CustomMsgType.CHATROOM_SYSTEM.getName(), params, callBack);
    }

    public void sendGiftMsg(Map<String, String> params, final OnMsgCallBack callBack) {
        if(params.size() <= 0) {
            return;
        }
        sendCustomMsg(CustomMsgType.CHATROOM_GIFT.getName(), params, callBack);
    }

    public void sendPraiseMsg(int num, OnMsgCallBack callBack) {
        if(num <= 0) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        params.put(MsgConstant.CUSTOM_PRAISE_KEY_NUM, String.valueOf(num));
        sendPraiseMsg(params, callBack);
    }

    public void sendPraiseMsg(Map<String, String> params, final OnMsgCallBack callBack) {
        if(params.size() <= 0) {
            return;
        }
        sendCustomMsg(CustomMsgType.CHATROOM_PRAISE.getName(), params, callBack);
    }

    public void sendCustomMsg(String event, Map<String, String> params, final OnMsgCallBack callBack) {
        sendCustomMsg(chatRoomId, ChatMessage.ChatType.ChatRoom, event, params, callBack);
    }

    public void sendCustomSingleMsg(String to,String event, Map<String, String> params, final OnMsgCallBack callBack){
        sendCustomMsg(to, ChatMessage.ChatType.Chat, event, params, callBack);
    }

    public void sendCustomMsg(String to, ChatMessage.ChatType chatType, String event, Map<String, String> params, final OnMsgCallBack callBack) {
        final ChatMessage sendMessage = ChatMessage.createSendMessage(ChatMessage.Type.CUSTOM);
        CustomMessageBody body = new CustomMessageBody(event);
        body.setParams(params);
        sendMessage.addBody(body);
        sendMessage.setTo(to);
        sendMessage.setChatType(chatType);
        sendMessage.setMessageStatusCallback(new CallBack() {
            @Override
            public void onSuccess() {
                if(callBack != null) {
                    if (event.equals(CustomMsgType.CHATROOM_SYSTEM.getName())){
                        AllNormalList.add(ChatroomIMManager.getInstance().parseChatMessage(sendMessage));
                    }else if (event.equals(CustomMsgType.CHATROOM_GIFT.getName())){
                        AllGiftList.add(ChatroomIMManager.getInstance().parseChatMessage(sendMessage));
                    }
                    callBack.onSuccess(ChatroomIMManager.getInstance().parseChatMessage(sendMessage));
                }
            }

            @Override
            public void onError(int i, String s) {
                if(callBack != null) {
                    callBack.onError(sendMessage.getMsgId(), i, s);
                }
            }

            @Override
            public void onProgress(int i, String s) {
                if(callBack != null) {
                    callBack.onProgress(i, s);
                }
            }
        });
        ChatClient.getInstance().chatManager().sendMessage(sendMessage);
    }

    public String getMsgGiftId(ChatMessageData msg) {
        if(!isGiftMsg(msg)) {
            return null;
        }

        Map<String, String> params = getCustomMsgParams(msg);
        LogTools.d("getMsgGiftId","getMsgGiftId_1: "+params.toString());
        if(params.containsKey(MsgConstant.CUSTOM_GIFT_KEY_ID)) {
            LogTools.d("getMsgGiftId",params.get(MsgConstant.CUSTOM_GIFT_KEY_ID));
            return params.get(MsgConstant.CUSTOM_GIFT_KEY_ID);
        }
        LogTools.d("getMsgGiftId","getMsgGiftId_3");
        return null;
    }

    public int getMsgGiftNum(ChatMessageData msg) {
        if(!isGiftMsg(msg)) {
            return 0;
        }
        Map<String, String> params = getCustomMsgParams(msg);
        if(params.containsKey(MsgConstant.CUSTOM_GIFT_KEY_NUM)) {
            String num = params.get(MsgConstant.CUSTOM_GIFT_KEY_NUM);
            if(TextUtils.isEmpty(num)) {
                return 0;
            }
            try {
                return Integer.valueOf(num);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public int getMsgPraiseNum(ChatMessageData msg) {
        if(!isPraiseMsg(msg)) {
            return 0;
        }
        Map<String, String> params = getCustomMsgParams(msg);
        if(params.containsKey(MsgConstant.CUSTOM_PRAISE_KEY_NUM)) {
            String num = params.get(MsgConstant.CUSTOM_PRAISE_KEY_NUM);
            if(TextUtils.isEmpty(num)) {
                return 0;
            }
            try {
                return Integer.valueOf(num);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }


    public boolean isGiftMsg(ChatMessageData msg) {
        return getCustomMsgType(getCustomEvent(msg)) == CustomMsgType.CHATROOM_GIFT;
    }

    public boolean isPraiseMsg(ChatMessageData msg) {
        return getCustomMsgType(getCustomEvent(msg)) == CustomMsgType.CHATROOM_PRAISE;
    }

    public String getCustomEvent(ChatMessageData message) {
        if(message == null) {
            return null;
        }
        if(!message.getType().equals("custom")) {
            return null;
        }
        return message.getEvent();
    }


    public Map<String, String> getCustomMsgParams(ChatMessageData message) {
        if(message == null) {
            return null;
        }
        if(!message.getType().equals("custom")) {
            return null;
        }
        return message.getCustomParams();
    }


    public Map<String, Object> getCustomMsgExt(ChatMessageData message) {
        if(message == null) {
            return null;
        }
        if(!message.getType().equals("custom")) {
            return null;
        }
        return message.getExt();
    }

    public String getCustomVolume(ChatMessageData message){
        if(message == null) {
            return null;
        }
        if(!message.getType().equals("custom")) {
            return null;
        }
        if (message.getCustomParams().containsKey("volume")){
            return message.getCustomParams().get("volume");
        }
        return "";
    }

    public CustomMsgType getCustomMsgType(String event) {
        if(TextUtils.isEmpty(event)) {
            return null;
        }
        return CustomMsgType.fromName(event);
    }
}
