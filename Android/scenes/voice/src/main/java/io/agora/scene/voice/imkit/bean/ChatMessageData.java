package io.agora.scene.voice.imkit.bean;

import java.util.HashMap;
import java.util.Map;

import io.agora.scene.voice.imkit.custorm.CustomMsgType;

/**
 * This class represents a chat message in the VR application.
 * It provides fields for the sender, recipient, message ID, content, conversation ID, message type, custom message type, event, and additional parameters.
 */
public class ChatMessageData {
    private String from;
    private String to;
    private String mMessageId;
    private String mContent;
    private String conversationId;
    // Message type
    private String mType;
    // Custom message type
    private CustomMsgType customMsgType;
    private String mEvent;
    private Map<String,Object> mExt = new HashMap<>();
    private Map<String,String> mCustomParams = new HashMap<>();

    /**
     * Gets the sender of the message.
     *
     * @return The sender of the message.
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the sender of the message.
     *
     * @param from The sender of the message.
     */
    public void setForm(String from) {
        this.from = from;
    }

    /**
     * Sets the recipient of the message.
     *
     * @param to The recipient of the message.
     */
    public void setTo(String to) {
        this.to = to;
    }

    /**
     * Gets the recipient of the message.
     *
     * @return The recipient of the message.
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets the content of the message.
     *
     * @param mContent The content of the message.
     */
    public void setContent(String mContent) {
        this.mContent = mContent;
    }

    /**
     * Gets the content of the message.
     *
     * @return The content of the message.
     */
    public String getContent() {
        return mContent;
    }

    /**
     * Sets the ID of the message.
     *
     * @param mMessageId The ID of the message.
     */
    public void setMessageId(String mMessageId) {
        this.mMessageId = mMessageId;
    }

    /**
     * Gets the ID of the message.
     *
     * @return The ID of the message.
     */
    public String getMessageId() {
        return mMessageId;
    }

    /**
     * Sets the additional parameters of the message.
     *
     * @param ext The additional parameters of the message.
     */
    public void setExt(Map<String,Object> ext) {
        mExt.putAll(ext);
    }

    /**
     * Gets the additional parameters of the message.
     *
     * @return The additional parameters of the message.
     */
    public Map<String,Object> getExt() {
        return mExt;
    }

    /**
     * Sets the type of the message.
     *
     * @param mType The type of the message.
     */
    public void setType(String mType) {
        this.mType = mType;
    }

    /**
     * Gets the type of the message.
     *
     * @return The type of the message.
     */
    public String getType() {
        return mType;
    }

    /**
     * Sets the event of the message.
     *
     * @param mEvent The event of the message.
     */
    public void setEvent(String mEvent) {
        this.mEvent = mEvent;
    }

    /**
     * Gets the event of the message.
     *
     * @return The event of the message.
     */
    public String getEvent() {
        return mEvent;
    }

    /**
     * Sets the custom message type of the message.
     *
     * @param customMsgType The custom message type of the message.
     */
    public void setCustomMsgType(CustomMsgType customMsgType) {
        this.customMsgType = customMsgType;
    }

    /**
     * Gets the custom message type of the message.
     *
     * @return The custom message type of the message.
     */
    public CustomMsgType getCustomMsgType() {
        return customMsgType;
    }

    /**
     * Sets the custom parameters of the message.
     *
     * @param mCustomParams The custom parameters of the message.
     */
    public void setCustomParams(Map<String, String> mCustomParams) {
        this.mCustomParams = mCustomParams;
    }

    /**
     * Gets the custom parameters of the message.
     *
     * @return The custom parameters of the message.
     */
    public Map<String, String> getCustomParams() {
        return mCustomParams;
    }

    /**
     * Sets the conversation ID of the message.
     *
     * @param conversationId The conversation ID of the message.
     */
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * Gets the conversation ID of the message.
     *
     * @return The conversation ID of the message.
     */
    public String getConversationId() {
        return conversationId;
    }
}