package io.agora.scene.ktv.service

import androidx.annotation.IntDef
import com.google.gson.annotations.SerializedName
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
    const val ROOM_USER_COUNT = "roomUserCount"
    const val THUMBNAIL_ID = "thumbnailId"
    const val PASSWORD = "password"
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
        const val idle = 0 // 空闲
        const val used = 1 // 使用中
        const val locked = 2 // 锁定
    }
}

enum class RoomSeatCmd {
    initSeatCmd,
    enterSeatCmd,
    leaveSeatCmd,
    muteAudioCmd,
    muteVideoCmd,
}

/**
 * Room mic seat info
 *
 * @constructor Create empty Room mic seat info
 */
data class RoomMicSeatInfo constructor(
    var owner: AUIUserThumbnailInfo? = null,
    var seatIndex: Int = 0,
    var seatAudioMute: Boolean = false, // 麦位禁用声音
    var seatVideoMute: Boolean = true, // 麦位禁用视频
    @SerializedName("micSeatStatus") @RoomMicSeatStatus
    var seatStatus: Int = RoomMicSeatStatus.idle, // 麦位状态，预留
) : Serializable

enum class RoomChoristerCmd {
    joinChorusCmd,
    leaveChorusCmd,
}

/**
 * Room chorister info
 *
 * @property userId
 * @property songCode
 * @constructor Create empty Room chorister info
 */
data class RoomChoristerInfo constructor(
    var userId: String = "",
    var songCode: String = ""  //合唱演唱歌曲
) : Serializable

/**
 * Join room output model
 *
 * @property roomInfo
 * @property roomConfig
 * @constructor Create empty Join room output model
 */
data class JoinRoomInfo constructor(
    var rtmToken: String = "",
    var rtcToken: String = "", //rtc join用
    var rtcChorusToken: String = "" //rtc 合唱join使用
) : AUIRoomInfo(), Serializable {
    val rtcChorusChannelName get() = roomName + "_rtc_ex"
}


@IntDef(PlayStatus.idle, PlayStatus.playing)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FIELD)
annotation class PlayStatus {
    companion object {
        const val idle = 0 // 未播放
        const val playing = 1 // 播放中
    }
}

enum class RoomSongCmd {
    chooseSongCmd,
    removeSongCmd,
    pingSongCmd,
    updatePlayStatusCmd
}
/**
 * Room song info
 *
 * @property songName
 * @property songNo
 * @property singer
 * @property imageUrl
 * @property orderUser
 * @property status
 * @property createAt
 * @property pinAt
 * @constructor Create empty Room sel song model
 */
data class RoomSongInfo constructor(
    // 获取歌词列表返回的歌词信息
    val songName: String,// 歌曲名
    val songNo: String, // 歌词唯一标识
    val singer: String, // 演唱者
    val imageUrl: String,// 歌曲封面

    var owner: AUIUserThumbnailInfo? = null, // 点歌人

    // 排序字段
    @PlayStatus
    val status: Int, // 0 未开始 1.播放中
    val createAt: Long,
    val pinAt: Double
)

