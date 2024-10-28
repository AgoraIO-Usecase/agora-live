package io.agora.scene.show.service

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.show.R
import java.util.UUID


@IntDef(ShowInteractionStatus.idle, ShowInteractionStatus.linking, ShowInteractionStatus.pking)
@Target(AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class ShowInteractionStatus {
    companion object {
        const val idle = 0
        const val linking = 1
        const val pking = 2
    }
}

// Room details information.
data class ShowRoomDetailModel constructor(
    val roomId: String,
    val roomName: String,
    val roomUserCount: Int,
    val ownerId: String,
    val thumbnailId: String,
    val ownerAvatar: String,
    val ownerName: String,
    val isPureMode: Boolean = false,
    val createdAt: Double = 0.0,
    val updatedAt: Double = 0.0
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt() > 0,
        parcel.readDouble(),
        parcel.readDouble()
    )

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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(roomId)
        parcel.writeString(roomName)
        parcel.writeInt(roomUserCount)
        parcel.writeString(ownerId)
        parcel.writeString(thumbnailId)
        parcel.writeString(ownerAvatar)
        parcel.writeString(ownerName)
        parcel.writeInt(if (isPureMode) 1 else 0)
        parcel.writeDouble(createdAt)
        parcel.writeDouble(updatedAt)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ShowRoomDetailModel> {
        override fun createFromParcel(parcel: Parcel): ShowRoomDetailModel {
            return ShowRoomDetailModel(parcel)
        }

        override fun newArray(size: Int): Array<ShowRoomDetailModel?> {
            return arrayOfNulls(size)
        }
    }
}

fun String.isRobotRoom() = length > 6
fun ShowRoomDetailModel.isRobotRoom() = roomId.isRobotRoom()

data class ShowUser constructor(
    val userId: String,
    val avatar: String,
    val userName: String,
    val muteAudio: Boolean,
    @ShowInteractionStatus val status: Int = ShowInteractionStatus.idle,
    val isWaiting: Boolean = false
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

data class ShowPKUser constructor(
    val userId: String,
    val userName: String,
    val roomId: String,
    val avatar: String,
    @ShowInteractionStatus val status: Int = ShowInteractionStatus.idle,
    val isWaiting: Boolean = false
)

data class ShowMessage constructor(
    val userId: String,
    val userName: String,
    val message: String,
)

data class ShowMicSeatApply constructor(
    val userId: String,
    val avatar: String,
    val userName: String
) {
    /**
     * Get avatar full url
     *
     * @return
     */
    fun getAvatarFullUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(avatar)
    }
}

@IntDef(ShowInvitationType.invitation, ShowInvitationType.accept, ShowInvitationType.reject, ShowInvitationType.end)
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
annotation class ShowInvitationType {
    companion object {
        const val invitation = 0
        const val accept = 1
        const val reject = 2
        const val end = 3
    }
}

data class ShowMicSeatInvitation constructor(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userName: String,
    @ShowInvitationType val type: Int = ShowInvitationType.invitation,
)

data class ShowPKInvitation constructor(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    var userName: String,
    val roomId: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromRoomId: String,
    @ShowInvitationType val type: Int = ShowInvitationType.invitation,
)

data class ShowInteractionInfo constructor(
    val userId: String,
    val userName: String,
    val roomId: String,
    @ShowInteractionStatus val interactStatus: Int,
    val createdAt: Double = TimeUtils.currentTimeMillis().toDouble()
)


enum class ShowSubscribeStatus {
    added,
    deleted,
    updated
}