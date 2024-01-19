package io.agora.scene.voice.imkit.custorm;

import io.agora.CallBack;
import io.agora.scene.voice.imkit.bean.ChatMessageData;

public abstract class OnMsgCallBack implements CallBack {

    @Override
    public void onSuccess() {

    }

    /**
     * Successful callback for sending a barrage message.
     * @param message
     */
    public abstract void onSuccess(ChatMessageData message);

    /**
     * @see #onError(String, int, String)
     * @param code
     * @param error
     */
    @Override
    public void onError(int code, String error) {

    }

    /**
     * Return the message ID for convenient deletion of the corresponding message based on error.
     * @param messageId
     * @param code
     * @param error
     */
    public void onError(String messageId, int code, String error){

    }

    @Override
    public void onProgress(int i, String s) {

    }
}
