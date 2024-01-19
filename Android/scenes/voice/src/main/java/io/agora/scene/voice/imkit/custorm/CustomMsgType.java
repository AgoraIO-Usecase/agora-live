package io.agora.scene.voice.imkit.custorm;

import android.text.TextUtils;

public enum CustomMsgType {

    CHATROOM_GIFT("chatroom_gift"),


    CHATROOM_PRAISE("chatroom_praise"),


    CHATROOM_APPLY_SITE("chatroom_submitApplySiteNotify"),


    CHATROOM_CANCEL_APPLY_SITE("chatroom_submitApplySiteNotifyCancel"),


    CHATROOM_DECLINE_APPLY("chatroom_applyRefusedNotify"),


    CHATROOM_INVITE_SITE("chatroom_inviteSiteNotify"),


    CHATROOM_INVITE_REFUSED_SITE("chatroom_inviteRefusedNotify"),

    CHATROOM_SYSTEM("chatroom_join"),
    ;

    private String name;
    private CustomMsgType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static CustomMsgType fromName(String name) {
        for (CustomMsgType type : CustomMsgType.values()) {
            if(TextUtils.equals(type.getName(), name)) {
                return type;
            }
        }
        return null;
    }
}
