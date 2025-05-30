package io.agora.scene.voice.model

import com.google.gson.annotations.SerializedName
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserThumbnailInfo
import io.agora.scene.base.manager.UserManager
import io.agora.scene.voice.global.VoiceBuddyFactory
import io.agora.voice.common.constant.ConfigConstants

object VoiceParameters {
    const val ROOM_USER_COUNT = "member_count"
    const val ROOM_SOUND_EFFECT = "sound_effect"
    const val PASSWORD = "roomPassword"
    const val IS_PRIVATE = "is_private"
    const val CHATROOM_ID = "chatroom_id"
}

/**
 * This data class represents the initial properties of a room in the VR application.
 * These properties include the room name, whether the room is private, the room password, the sound effect, and the room type.
 */
data class VoiceCreateRoomModel constructor(
    val roomName: String,
    val password: String,
    val soundEffect: Int = 0,
) : BaseRoomBean

/**
 * This data class represents a user in the VR application.
 * It includes the user's ID, chat UID, nickname, portrait, RTC UID, mic index, and mic status.
 */
data class VoiceMemberModel constructor(
    @SerializedName("uid") var userId: String? = null,
    @SerializedName("chat_uid") var chatUid: String? = null,
    @SerializedName("name") var nickName: String? = null,
    @SerializedName("portrait") var portrait: String? = null,
    @SerializedName("rtc_uid") var rtcUid: Int = 0,
    @SerializedName("mic_index") var micIndex: Int = -1,
    @SerializedName("micStatus") var micStatus: Int = 1, // Mic status (0 off, 1 on)
) : BaseRoomBean {
    /**
     * This method returns the full URL of the user's avatar.
     *
     * @return The full URL of the user's avatar.
     */
    fun getAvatarUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(portrait)
    }
}

/**
 * This data class represents a user on the contribution leaderboard in the VR application.
 * It includes the user's chat UID, name, portrait, and contribution amount.
 */
data class VoiceRankUserModel constructor(
    @SerializedName("chat_uid") var chatUid: String? = null,
    var name: String? = null,
    var portrait: String? = "",
    var amount: Int = 0
) : BaseRoomBean {
    /**
     * This method returns the full URL of the user's avatar.
     *
     * @return The full URL of the user's avatar.
     */
    fun getAvatarUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(portrait)
    }
}

fun AUIRoomInfo.memberCount(): Int {
    return when (val userCount = customPayload[VoiceParameters.ROOM_USER_COUNT]) {
        is Int -> userCount
        is Long -> userCount.toInt()
        else -> 0
    }
}

fun AUIRoomInfo.soundEffect(): Int {
    return when (val soundEffect = customPayload[VoiceParameters.ROOM_SOUND_EFFECT]) {
        is Int -> soundEffect
        is Long -> soundEffect.toInt()
        else -> ConfigConstants.SoundSelection.Social_Chat
    }
}

fun AUIRoomInfo.roomPassword(): String {
    return customPayload[VoiceParameters.PASSWORD] as? String ?: ""
}

fun AUIRoomInfo.isPrivate(): Boolean {
    return customPayload[VoiceParameters.IS_PRIVATE] as? Boolean ?: false
}

fun AUIRoomInfo.chatroomId(): String {
    return customPayload[VoiceParameters.CHATROOM_ID] as? String ?: ""
}


/**
 * This data class represents a room in the VR application.
 * It includes the room owner, room ID, whether the room is private, member count, click count, room type, room name, sound effect, channel ID, chatroom ID, creation time, room password, ranking list, member list, gift amount, whether the robot is used, robot volume, and announcement.
 */
data class VoiceRoomModel constructor(
    var owner: VoiceMemberModel? = null,
    @SerializedName("room_id") var roomId: String = "",
    @SerializedName("is_private") var isPrivate: Boolean = false,
    @SerializedName("member_count") var memberCount: Int = 0,
    @SerializedName("click_count") var clickCount: Int = 0,
    @SerializedName("room_name") var roomName: String = "",
    @SerializedName("sound_effect") var soundEffect: Int = 0,
    @SerializedName("chatroom_id") var chatroomId: String = "",
    @SerializedName("created_at") var createdAt: Long = 0,
    @SerializedName("roomPassword") var roomPassword: String = "",
    @Transient var rankingList: List<VoiceRankUserModel>? = null,
    @Transient var memberList: List<VoiceMemberModel>? = null,
    @Transient var giftAmount: Int = 0,
    @Transient var useRobot: Boolean = false,
    @Transient var robotVolume: Int = 50,
    @Transient var announcement: String = "",
) : BaseRoomBean {
    val isOwner: Boolean get() = owner?.userId == VoiceBuddyFactory.get().getVoiceBuddy().userId()
}

/**
 * This data class represents a microphone information model in the VR application.
 * It includes the microphone index, member, microphone status, owner tag, and audio volume type.
 */
data class VoiceMicInfoModel constructor(
    @SerializedName("mic_index") var micIndex: Int = -1,
    var member: VoiceMemberModel? = null,
    @SerializedName("status") var micStatus: Int = -1, // Microphone status
    @Transient var ownerTag: Boolean = false,
    @Transient var audioVolumeType: Int = ConfigConstants.VolumeType.Volume_None,
) : BaseRoomBean

/**
 * This data class represents a room application model in the VR application.
 * It includes the index, member, and creation time.
 */
data class VoiceRoomApply constructor(
    var index: Int? = -1,
    var member: VoiceMemberModel? = null,
    var created_at: Long? = 0

) : BaseRoomBean

/**
 * This data class represents a room information model in the VR application.
 * It includes the room information, microphone information, and background music information.
 */
data class VoiceRoomInfo constructor(
    var roomInfo: VoiceRoomModel? = null,
    var micInfo: List<VoiceMicInfoModel>? = null,
) : BaseRoomBean

/**
 * This data class represents a gift model in the VR application.
 * It includes the gift ID, gift count, gift name, gift price, user name, portrait, and a boolean indicating whether it is checked.
 */
data class VoiceGiftModel constructor(
    var gift_id: String? = "",
    var gift_count: String? = "",
    var gift_name: String? = "",
    var gift_price: String? = "",
    var userName: String? = "",
    var portrait: String? = "",
    var isChecked: Boolean? = false
)

val AUIUserThumbnailInfo.fullHeadUrl
    get() = if (this.userAvatar.startsWith("http")) {
        this.userAvatar
    } else {
        "file:///android_asset/" + this.userAvatar + ".png"
    }