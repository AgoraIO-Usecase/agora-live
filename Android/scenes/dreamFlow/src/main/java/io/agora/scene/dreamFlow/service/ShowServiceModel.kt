package io.agora.scene.dreamFlow.service

import android.os.Parcel
import android.os.Parcelable
import io.agora.scene.base.manager.UserManager
import io.agora.scene.dreamFlow.R

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
    end(1),

    /**
     * Expire
     *
     * @constructor Create empty Expire
     */
    Expire(2),
}

/**
 * Show user
 *
 * @property id
 * @property name
 * @property headUrl
 * @constructor Create empty Show user
 */
data class ShowUser constructor(
    val id: String = "",
    val name: String = "",
    val headUrl: String = "",
){
    /**
     * Get avatar full url
     *
     * @return
     */
    fun getAvatarFullUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(headUrl)
    }
}

/**
 * Room detail model
 *
 * @property roomId
 * @property roomName
 * @property roomUserCount
 * @property thumbnailId
 * @property ownerId
 * @property ownerAvatar
 * @property ownerName
 * @property roomStatus
 * @property createdAt
 * @property updatedAt
 * @constructor Create empty Room detail model
 */
data class RoomDetailModel constructor(
    val roomId: String,
    val roomName: String,
    val roomUserCount: Int = 0,
    val thumbnailId: String, // 0, 1, 2, 3
    val ownerId: String,
    val ownerAvatar: String,// http url
    val ownerName: String,
    val roomStatus: Int = ShowRoomStatus.activity.value,
    val createdAt: Long,
    val updatedAt: Long,
): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readLong(),
        parcel.readLong()
    ) {}

    /**
     * Get thumbnail icon
     *
     */
    fun getThumbnailIcon() = when (thumbnailId) {
        "0" -> R.drawable.commerce_room_cover_0
        "1" -> R.drawable.commerce_room_cover_1
        "2" -> R.drawable.commerce_room_cover_2
        "3" -> R.drawable.commerce_room_cover_3
        else -> R.drawable.commerce_room_cover_0
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
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(roomId)
        parcel.writeString(roomName)
        parcel.writeInt(roomUserCount)
        parcel.writeString(thumbnailId)
        parcel.writeString(ownerId)
        parcel.writeString(ownerAvatar)
        parcel.writeString(ownerName)
        parcel.writeInt(roomStatus)
        parcel.writeLong(createdAt)
        parcel.writeLong(updatedAt)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RoomDetailModel> {
        override fun createFromParcel(parcel: Parcel): RoomDetailModel {
            return RoomDetailModel(parcel)
        }

        override fun newArray(size: Int): Array<RoomDetailModel?> {
            return arrayOfNulls(size)
        }
    }
}

