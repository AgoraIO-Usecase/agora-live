package io.agora.scene.ktv.service

import androidx.annotation.IntDef
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserThumbnailInfo
import java.io.Serializable

/**
 * Ktv parameters
 * [AUIRoomInfo.customPayload] key
 *
 * @constructor Create empty Ktv parameters
 */
object KTVParameters {
    const val ROOM_USER_COUNT = "roomPeopleNum"
    const val THUMBNAIL_ID = "icon"
    const val PASSWORD = "password"
    const val IS_PRIVATE = "isPrivate"
}

/**
 * Create room input model
 *
 * @property icon
 * @property name
 * @property password
 * @constructor Create empty Create room input model
 */
data class CreateRoomInfo constructor(
    val icon: String,
    val name: String,
    val password: String,
)

/**
 * Room mic seat status
 *
 * @constructor Create empty Room mic seat status
 */
@IntDef(RoomMicSeatStatus.idle, RoomMicSeatStatus.used, RoomMicSeatStatus.locked)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class RoomMicSeatStatus {
    companion object {
        const val idle = 0
        const val used = 1
        const val locked = 2
    }
}

enum class RoomSeatCmd {
    enterSeatCmd,
    leaveSeatCmd,
    muteAudioCmd,
    muteVideoCmd,
    kickSeatCmd,
}

/**
 * Room mic seat info
 *
 * @constructor Create empty Room mic seat info
 */
data class RoomMicSeatInfo constructor(
    var owner: AUIUserThumbnailInfo? = null,
    var seatIndex: Int = 0,
    var isAudioMuted: Boolean = false,
    var isVideoMuted: Boolean = true,
) : Serializable

enum class RoomChorusCmd {
    joinChorusCmd,
    leaveChorusCmd,
    kickAllOutOfChorusCmd,
    KickUserOutOfChorusCmd,
}

/**
 * Room chorister info
 *
 * @property userId
 * @property chorusSongNo
 * @constructor Create empty Room chorister info
 */
data class RoomChoristerInfo constructor(
    var userId: String = "",
    var chorusSongNo: String = ""
) : Serializable

@IntDef(PlayStatus.idle, PlayStatus.playing)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class PlayStatus {
    companion object {
        const val idle = 0
        const val playing = 1
    }
}

enum class RoomSongCmd {
    chooseSongCmd,
    removeSongCmd,
    pingSongCmd,
    updatePlayStatusCmd,
    removedUserSongsCmd,
}

data class ChooseSongInputModel constructor(
    val songName: String,
    val songNo: String,
    val singer: String,
    val imageUrl: String,
)

/**
 * Room song info
 *
 * @property songName
 * @property songNo
 * @property singer
 * @property imageUrl
 * @property owner
 * @property status
 * @property createAt
 * @property pinAt
 * @constructor Create empty Room sel song model
 */
data class ChosenSongInfo constructor(
    val songName: String,
    val songNo: String,
    val singer: String,
    val imageUrl: String,

    var owner: AUIUserThumbnailInfo? = null,

    // Sorting Field.
    @PlayStatus
    val status: Int = PlayStatus.idle,
    val createAt: Long = 0,
    val pinAt: Long = 0,
)

/**
 * Enum value or null
 *
 * @param T
 * @param name
 * @return
 */
inline fun <reified T : Enum<T>> enumValueOrNull(name: String?): T? {
    return try {
        enumValueOf<T>(name ?: "")
    } catch (e: IllegalArgumentException) {
        null
    }
}

val AUIUserThumbnailInfo.fullHeadUrl
    get() = if (this.userAvatar.startsWith("http")) {
        this.userAvatar
    } else {
        "file:///android_asset/" + this.userAvatar + ".png"
    }
