package io.agora.scene.voice.imkit.custorm;

import io.agora.scene.voice.imkit.bean.ChatMessageData;

/**
 * Define the received message type.
 */
public interface OnCustomMsgReceiveListener {
    /**
     * Received a gift message.
     * @param message
     */
    void onReceiveGiftMsg(ChatMessageData message);

    /**
     * Received a like message.
     * @param message
     */
    void onReceivePraiseMsg(ChatMessageData message);

    /**
     * Received a regular message.
     * @param message
     */
    void onReceiveNormalMsg(ChatMessageData message);

    /**
     * Received an application message.
     * @param message
     */
    void onReceiveApplySite(ChatMessageData message);

    /**
     * Received a cancel application message.
     * @param message
     */
    void onReceiveCancelApplySite(ChatMessageData message);

    /**
     * Received an invitation message.
     * @param message
     */
    void onReceiveInviteSite(ChatMessageData message);

    /**
     * Received a rejection of invitation message.
     * @param message
     */
    void onReceiveInviteRefusedSite(ChatMessageData message);

    /**
     * Received a rejection of application message.
     * @param message
     */
    void onReceiveDeclineApply(ChatMessageData message);

    /**
     * Received a system message.
     * @param message
     */
    void onReceiveSystem(ChatMessageData message);

}
