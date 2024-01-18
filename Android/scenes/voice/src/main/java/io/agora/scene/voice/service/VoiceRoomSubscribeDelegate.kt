package io.agora.scene.voice.service

import io.agora.scene.voice.imkit.bean.ChatMessageData
import io.agora.scene.voice.model.VoiceMemberModel


/**
 * Voice room subscribe delegate
 *
 * @constructor Create empty Voice room subscribe delegate
 */
interface VoiceRoomSubscribeDelegate {
    /**
     * On receive gift
     *
     * @param roomId
     * @param message
     */
    fun onReceiveGift(roomId: String, message: ChatMessageData?){}

    /**
     * On receive text msg
     *
     * @param roomId
     * @param message
     */
    fun onReceiveTextMsg(roomId: String,message: ChatMessageData?){}

    /**
     * On receive seat request
     *
     * @param message
     */
    fun onReceiveSeatRequest( message: ChatMessageData) {}

    /**
     * On receive seat request rejected
     *
     * @param chatUid
     */
    fun onReceiveSeatRequestRejected(chatUid: String) {}

    /**
     * On receive seat invitation
     *
     * @param message
     */
    fun onReceiveSeatInvitation(message: ChatMessageData) {}

    /**
     * On receive seat invitation rejected
     *
     * @param chatUid
     * @param message
     */
    fun onReceiveSeatInvitationRejected(chatUid: String, message: ChatMessageData?) {}

    /**
     * On announcement changed
     *
     * @param roomId
     * @param content
     */
    fun onAnnouncementChanged(roomId: String, content: String) {}

    /**
     * On user joined room
     *
     * @param roomId
     * @param voiceMember
     */
    fun onUserJoinedRoom(roomId: String, voiceMember: VoiceMemberModel) {}

    /**
     * On user left room
     *
     * @param roomId
     * @param chatUid
     */
    fun onUserLeftRoom(roomId: String, chatUid: String) {}

    /**
     * On user be kicked
     *
     * @param roomId
     * @param reason
     */
    fun onUserBeKicked(roomId: String, reason: VoiceRoomServiceKickedReason) {}

    /**
     * On room destroyed
     *
     * @param roomId
     */
    fun onRoomDestroyed(roomId: String){}

    /**
     * On seat updated
     *
     * @param roomId
     * @param attributeMap
     * @param fromId
     */
    fun onSeatUpdated(roomId: String, attributeMap: Map<String, String>, fromId: String) {}
}

/**
 * Voice room service kicked reason
 *
 * @constructor Create empty Voice room service kicked reason
 */
enum class VoiceRoomServiceKickedReason{
    removed,
    destroyed,
    offLined,
}
