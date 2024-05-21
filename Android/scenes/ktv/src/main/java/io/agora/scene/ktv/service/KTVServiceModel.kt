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

/**
 * Room mic seat info
 *
 * @constructor Create empty Room mic seat info
 */
data class RoomMicSeatInfo constructor(
    var user: AUIUserThumbnailInfo? = null,
    var seatIndex: Int = 0,
    var seatAudioMute: Boolean = false, // 麦位禁用声音，预留
    var seatVideoMute: Boolean = false, // 麦位禁用视频，预留
    @SerializedName("micSeatStatus") @RoomMicSeatStatus
    var seatStatus: Int = RoomMicSeatStatus.idle, // 麦位状态，预留
) : Serializable

/**
 * Room chorister info
 *
 * @property userId
 * @property chorusSongNo
 * @constructor Create empty Room chorister info
 */
data class RoomChoristerInfo constructor(
    var userId: String = "",
    var chorusSongNo: String = ""   //合唱者演唱歌曲  RoomSelSongModel.songCode + RoomSelSongModel.createAt
) : Serializable


/**
 * Join room output model
 *
 * @property roomInfo
 * @property roomConfig
 * @constructor Create empty Join room output model
 */
data class JoinRoomInfo constructor(
    var rtmToken:String = "",
    var rtcToken: String = "", //rtc join用
    var rtcChorusToken: String = "" //rtc 合唱join使用
) : AUIRoomInfo(), Serializable {
    val rtcChorusChannelName get() = roomName + "_rtc_ex"
}

/**
 * Room song info
 *
 * @property songName
 * @property songNo
 * @property singer
 * @property imageUrl
 * @property userNo
 * @property name
 * @property isOriginal
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

    // 获取已点歌记返回的歌词信息，同时也包含上面信息
    val userNo: String? = null,// 点歌人No
    val name: String? = null,// 点歌人昵称
    val isOriginal: Int = 0, //是否原唱

    // 排序字段
    val status: Int, // 0 未开始 1.已唱 2.正在唱
    val createAt: Long,
    val pinAt: Double
) {
    companion object {
        val STATUS_IDLE = 0
        val STATUS_PLAYED = 1
        val STATUS_PLAYING = 2
    }
}

/**
 * Scoring algo control model
 *
 * @property level
 * @property offset
 * @constructor Create empty Scoring algo control model
 */
data class ScoringAlgoControlModel constructor(
    val level: Int,
    val offset: Int
)

/**
 * Scoring average model
 *
 * @property isLocal
 * @property score
 * @constructor Create empty Scoring average model
 */
data class ScoringAverageModel constructor(
    val isLocal: Boolean,
    val score: Int
)

/**
 * Volume model
 *
 * @property uid
 * @property volume
 * @constructor Create empty Volume model
 */
data class VolumeModel constructor(
    val uid: Int,
    val volume: Int
)