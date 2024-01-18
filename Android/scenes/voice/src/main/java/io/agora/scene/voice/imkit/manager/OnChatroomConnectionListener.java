package io.agora.scene.voice.imkit.manager;

/**
 * The interface On chatroom connection listener.
 */
public interface OnChatroomConnectionListener {
    void onConnected();

    void onDisconnected(int error);

    void onTokenWillExpire();

    void onTokenExpired();
}
