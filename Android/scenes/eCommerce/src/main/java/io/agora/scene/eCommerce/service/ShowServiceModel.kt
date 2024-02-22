package io.agora.scene.eCommerce.service

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

/**
 * Show user
 *
 * @property userId
 * @property avatar
 * @property userName
 * @constructor Create empty Show user
 */
data class ShowUser constructor(
    val userId: String,
    val avatar: String,
    val userName: String,
){
    fun getAvatarFullUrl(): String {
        return UserManager.getInstance().getUserAvatarFullUrl(avatar)
    }
}

enum class AuctionStatus(value: Int) {
    Idle(0),
    Start(1),
    Finish(2)
}
data class AuctionModel constructor(
    var bidUser: ShowUser? = null,
    var timestamp: String = "0",
    var bid: Int = 0,
    val status: Int = 0
)

data class GoodsModel constructor(
    val imageName: String = "",
    val title: String,
    val quantity: Int = 6,
    val price: Float? = null
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

