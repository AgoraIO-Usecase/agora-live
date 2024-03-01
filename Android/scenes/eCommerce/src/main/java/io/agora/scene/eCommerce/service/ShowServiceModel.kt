package io.agora.scene.eCommerce.service

import android.os.Parcel
import android.os.Parcelable
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserThumbnailInfo
import io.agora.scene.base.manager.UserManager
import io.agora.scene.eCommerce.R

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

data class ShowUser constructor(
    val id: String = "",
    val name: String = "",
    val headUrl: String = "",
){
    fun getAvatarFullUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(headUrl)
    }
}

enum class AuctionStatus(val value: Int) {
    Idle(0),
    Start(1),
    Finish(2);
    companion object {
        fun fromValue(value: Int): AuctionStatus {
            return values().first { it.value == value }
        }
    }
}
data class AuctionModel constructor(
    var bidUser: ShowUser? = null,
    var timestamp: String = "0",
    var goods: GoodsModel? = null,
    var bid: Int = 0,
    var status: Int = 0
)

data class GoodsModel constructor(
    val goodsId: String,
    val imageName: String = "",
    val title: String = "",
    var quantity: Int = 6,
    val price: Float? = null,
    @Transient var picResource: Int = 0
)

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

data class RoomDetailModel constructor(
    val roomId: String,
    val roomName: String,
    val roomUserCount: Int,
    val thumbnailId: String, // 0, 1, 2, 3
    val ownerId: String,
    val ownerAvatar: String,// http url
    val ownerName: String,
    val roomStatus: Int = ShowRoomStatus.activity.value,
    val createdAt: Double,
    val updatedAt: Double,
) {

    fun getThumbnailIcon() = when (thumbnailId) {
        "0" -> R.drawable.commerce_room_cover_0
        "1" -> R.drawable.commerce_room_cover_1
        "2" -> R.drawable.commerce_room_cover_2
        "3" -> R.drawable.commerce_room_cover_3
        else -> R.drawable.commerce_room_cover_0
    }

    fun getOwnerAvatarFullUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(ownerAvatar)
    }

    fun isRobotRoom() = roomId.length > 6
}

fun AUIRoomInfo.isRobotRoom(): Boolean {
    return this.roomId.length > 6
}

val AUIRoomInfo.ownerId: Int
    get() {
        return this.owner?.userId?.toIntOrNull() ?: 0
    }

fun AUIRoomInfo.getThumbnailIcon(): Int {
    return when (this.thumbnail) {
        "0" -> R.drawable.commerce_room_cover_0
        "1" -> R.drawable.commerce_room_cover_1
        "2" -> R.drawable.commerce_room_cover_2
        "3" -> R.drawable.commerce_room_cover_3
        else -> R.drawable.commerce_room_cover_0
    }
}

fun AUIUserThumbnailInfo.getAvatarFullUrl(): String {
    return UserManager.getInstance().getUserAvatarFullUrl(userAvatar)
}

