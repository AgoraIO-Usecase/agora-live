package io.agora.scene.voice.imkit.manager;

import java.util.List;
import java.util.Map;

public interface OnChatroomEventReceiveListener {

    void onRoomDestroyed(String roomId);

    void onMemberJoined(String roomId,String uid);

    void onMemberExited(String roomId,String name,String reason);

    void onKicked(String roomId,int reason);

    void onAnnouncementChanged(String roomId,String announcement);

    void onAttributesUpdate(String roomId, Map<String, String> attributeMap, String fromId);

    void onAttributesRemoved(String roomId, List<String> keyList, String fromId);

}
