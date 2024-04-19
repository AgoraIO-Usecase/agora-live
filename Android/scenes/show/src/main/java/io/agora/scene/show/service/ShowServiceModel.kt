package io.agora.scene.show.service

import android.os.Parcel
import android.os.Parcelable
import io.agora.scene.base.manager.UserManager
import io.agora.scene.show.R

/**
 * Show room status
 *
 * @property value
 * @constructor Create empty Show room status
 */
enum class ShowRoomStatus(val value: Int) {
    /**
     * Activity
     *
     * @constructor Create empty Activity
     */
    activity(0),

    /**
     * End
     *
     * @constructor Create empty End
     */
    end(1)
}

/**
 * Show room request status
 *
 * @property value
 * @constructor Create empty Show room request status
 */
enum class ShowRoomRequestStatus(val value: Int){
    /**
     * Idle
     *
     * @constructor Create empty Idle
     */
    idle(0),

    /**
     * Waitting
     *
     * @constructor Create empty Waitting
     */
    waitting(1),

    /**
     * Accepted
     *
     * @constructor Create empty Accepted
     */
    accepted(2),

    /**
     * Rejected
     *
     * @constructor Create empty Rejected
     */
    rejected(3),

    /**
     * Ended
     *
     * @constructor Create empty Ended
     */
    ended(4)
}

/**
 * Show interaction status
 *
 * @property value
 * @constructor Create empty Show interaction status
 */
enum class ShowInteractionStatus(val value: Int) {
    /**
     * Idle
     *
     * @constructor Create empty Idle
     */
    idle(0),

    /**
     * On seat
     *
     * @constructor Create empty On seat
     */
    onSeat(1),

    /**
     * Pking
     *
     * @constructor Create empty Pking
     */
    pking(2)
}

/**
 * Show room detail model
 *
 * @property roomId
 * @property roomName
 * @property roomUserCount
 * @property thumbnailId
 * @property ownerId
 * @property ownerAvatar
 * @property ownerName
 * @property roomStatus
 * @property interactStatus
 * @property createdAt
 * @property updatedAt
 * @constructor Create empty Show room detail model
 */
data class ShowRoomDetailModel constructor(
    val roomId: String,
    val roomName: String,
    val roomUserCount: Int,
    val thumbnailId: String, // 0, 1, 2, 3
    val ownerId: String,
    val ownerAvatar: String,// http url
    val ownerName: String,
    val roomStatus: Int = ShowRoomStatus.activity.value,
    val interactStatus: Int = ShowInteractionStatus.idle.value,
    val createdAt: Double,
    val updatedAt: Double
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()?:"",
        parcel.readInt(),
        parcel.readString()?:"",
        parcel.readString()!!,
        parcel.readString()?:"",
        parcel.readString()?:"",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    /**
     * To map
     *
     * @return
     */
    fun toMap(): HashMap<String, Any>{
        return hashMapOf(
            Pair("roomId", roomId),
            Pair("roomName", roomName),
            Pair("roomUserCount", roomUserCount),
            Pair("thumbnailId", thumbnailId),
            Pair("ownerId", ownerId),
            Pair("ownerAvatar", ownerAvatar),
            Pair("ownerName", ownerName),
            Pair("roomStatus", roomStatus),
            Pair("interactStatus", interactStatus),
            Pair("createdAt", createdAt),
            Pair("updatedAt", updatedAt),
        )
    }

    /**
     * Get thumbnail icon
     *
     */
    fun getThumbnailIcon() = when (thumbnailId) {
        "0" -> R.mipmap.show_room_cover_0
        "1" -> R.mipmap.show_room_cover_1
        "2" -> R.mipmap.show_room_cover_2
        "3" -> R.mipmap.show_room_cover_3
        else -> R.mipmap.show_room_cover_0
    }

    /**
     * Get owner avatar full url
     *
     * @return
     */
    fun getOwnerAvatarFullUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(ownerAvatar)
    }


    /**
     * Is robot room
     *
     */
    fun isRobotRoom() = roomId.length > 6

    /**
     * Write to parcel
     *
     * @param parcel
     * @param flags
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(roomId)
        parcel.writeString(roomName)
        parcel.writeInt(roomUserCount)
        parcel.writeString(thumbnailId)
        parcel.writeString(ownerId)
        parcel.writeString(ownerAvatar)
        parcel.writeString(ownerName)
        parcel.writeInt(roomStatus)
        parcel.writeInt(interactStatus)
        parcel.writeDouble(createdAt)
        parcel.writeDouble(updatedAt)
    }

    /**
     * Describe contents
     *
     * @return
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * Creator
     *
     * @constructor Create empty Creator
     */
    companion object CREATOR : Parcelable.Creator<ShowRoomDetailModel> {
        override fun createFromParcel(parcel: Parcel): ShowRoomDetailModel {
            return ShowRoomDetailModel(parcel)
        }

        override fun newArray(size: Int): Array<ShowRoomDetailModel?> {
            return arrayOfNulls(size)
        }
    }


}

/**
 * Show user
 *
 * @property userId
 * @property avatar
 * @property userName
 * @property status
 * @constructor Create empty Show user
 */
data class ShowUser constructor(
    val userId: String,
    val avatar: String,
    val userName: String,
    val status: Int = ShowRoomRequestStatus.idle.value
){
    /**
     * Get avatar full url
     *
     * @return
     */
    fun getAvatarFullUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(avatar)
    }
}

/**
 * Show message
 *
 * @property userId
 * @property userName
 * @property message
 * @property createAt
 * @constructor Create empty Show message
 */
data class ShowMessage constructor(
    val userId: String,
    val userName: String,
    val message: String,
    val createAt: Double
)

/**
 * Show mic seat apply
 *
 * @property userId
 * @property avatar
 * @property userName
 * @property status
 * @property createAt
 * @constructor Create empty Show mic seat apply
 */
data class ShowMicSeatApply constructor(
    val userId: String,
    val avatar: String,
    val userName: String,
    val status: Int,
    val createAt: Double
){
    /**
     * Get avatar full url
     *
     * @return
     */
    fun getAvatarFullUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(avatar)
    }
}

/**
 * Show mic seat invitation
 *
 * @property userId
 * @property avatar
 * @property userName
 * @property status
 * @constructor Create empty Show mic seat invitation
 */
data class ShowMicSeatInvitation constructor(
    val userId: String,
    val avatar: String,
    val userName: String,
    val status: Int,
){
    /**
     * Get avatar full url
     *
     * @return
     */
    fun getAvatarFullUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(avatar)
    }
}

/**
 * Show p k invitation
 *
 * @property userId
 * @property userName
 * @property roomId
 * @property fromUserId
 * @property fromName
 * @property fromRoomId
 * @property status
 * @property userMuteAudio
 * @property fromUserMuteAudio
 * @property createAt
 * @constructor Create empty Show p k invitation
 */
data class ShowPKInvitation constructor(
    val userId: String,
    var userName: String,
    val roomId: String,
    val fromUserId: String,
    val fromName: String,
    val fromRoomId: String,
    val status: Int,
    var userMuteAudio: Boolean = false,
    var fromUserMuteAudio: Boolean = false,
    val createAt: Double
)

/**
 * Show interaction info
 *
 * @property userId
 * @property userName
 * @property roomId
 * @property interactStatus
 * @property muteAudio
 * @property ownerMuteAudio
 * @property createdAt
 * @constructor Create empty Show interaction info
 */
data class ShowInteractionInfo constructor(
    val userId: String,
    val userName: String,
    val roomId: String,
    val interactStatus: Int,
    val muteAudio: Boolean = false,
    val ownerMuteAudio: Boolean = false,
    val createdAt: Double
)

