package io.agora.scene.ktv.service

import io.agora.rtmsyncmanager.model.AUIRoomConfig
import io.agora.rtmsyncmanager.model.AUIRoomInfo

/*
 * service 模块
 * 简介：这个模块的作用是负责前端业务模块和业务服务器的交互(包括房间列表+房间内的业务数据同步等)
 * 实现原理：该场景的业务服务器是包装了一个 rethinkDB 的后端服务，用于数据存储，可以认为它是一个 app 端上可以自由写入的 DB，房间列表数据、房间内的业务数据等在 app 上构造数据结构并存储在这个 DB 里
 * 当 DB 内的数据发生增删改时，会通知各端，以此达到业务数据同步的效果
 * TODO 注意⚠️：该场景的后端服务仅做场景演示使用，无法商用，如果需要上线，您必须自己部署后端服务或者云存储服务器（例如leancloud、环信等）并且重新实现这个模块！！！！！！！！！！！
 */

object KTVParameters {
    const val ROOM_USER_COUNT = "roomUserCount"
    const val THUMBNAIL_ID = "thumbnailId"
    const val IS_PRIVATE = "isPrivate"
    const val PASSWORD = "password"
}

/**
 * Room list model
 *
 * @property roomNo
 * @property name
 * @property icon
 * @property isPrivate
 * @property password
 * @property creatorNo
 * @property creatorName
 * @property creatorAvatar
 * @property createdAt
 * @property bgOption
 * @property roomPeopleNum
 * @constructor Create empty Room list model
 */
data class RoomListModel constructor(
    val roomNo: String = "",
    val name: String = "",
    val icon: String = "",
    val isPrivate: Boolean = false,
    val password: String = "",
    val creatorNo: String = "",
    val creatorName: String = "",
    val creatorAvatar: String = "",
    val createdAt: String = System.currentTimeMillis().toString(),

    /**
     * 背景图
     */
    val bgOption: String = "",
    /**
     * 房间内人数
     */
    val roomPeopleNum: Int = 0,
) {}

/**
 * Room seat model
 *
 * @property isMaster
 * @property headUrl
 * @property userNo
 * @property rtcUid
 * @property name
 * @property seatIndex
 * @property chorusSongCode
 * @property isAudioMuted
 * @property isVideoMuted
 * @constructor Create empty Room seat model
 */
data class RoomSeatModel constructor(
    val isMaster: Boolean,// 是否是房主
    val headUrl: String,// 头像
    val userNo: String,// 座位上用户no
    val rtcUid: String,// 座位上用户id，与rtc的userId一致
    val name: String,// 座位上用户昵称
    val seatIndex: Int,// 座位编号
    val chorusSongCode: String = "",// 是否合唱, RoomSelSongModel.songCode + RoomSelSongModel.createAt
    val isAudioMuted: Int,// 是否静音
    val isVideoMuted: Int,// 是否开启视频
) : java.io.Serializable {

    companion object {
        val MUTED_VALUE_TRUE = 1
        val MUTED_VALUE_FALSE = 0
    }
}

/**
 * Create room input model
 *
 * @property icon
 * @property isPrivate
 * @property name
 * @property password
 * @property userNo
 * @constructor Create empty Create room input model
 */
data class CreateRoomInputModel constructor(
    val icon: String,
    val isPrivate: Int,
    val name: String,
    val password: String,
    val userNo: String,
)

/**
 * Join room input model
 *
 * @property roomNo
 * @property password
 * @constructor Create empty Join room input model
 */
data class JoinRoomInputModel constructor(
    val roomNo: String,
    val password: String?,
)

/**
 * Join room output model
 *
 * @property roomInfo
 * @property roomConfig
 * @constructor Create empty Join room output model
 */
data class JoinRoomOutputModel constructor(
    val roomInfo: AUIRoomInfo,
    val roomConfig: AUIRoomConfig,
) : java.io.Serializable

/**
 * On seat input model
 *
 * @property seatIndex
 * @constructor Create empty On seat input model
 */
data class OnSeatInputModel constructor(
    val seatIndex: Int
)

/**
 * Out seat input model
 *
 * @property userNo
 * @property userId
 * @property userName
 * @property userHeadUrl
 * @property userOnSeat
 * @constructor Create empty Out seat input model
 */
data class OutSeatInputModel constructor(
    val userNo: String,
    val userId: String,
    val userName: String,
    val userHeadUrl: String,
    val userOnSeat: Int,
)

/**
 * Remove song input model
 *
 * @property songNo
 * @constructor Create empty Remove song input model
 */
data class RemoveSongInputModel(
    val songNo: String,
)

/**
 * Room sel song model
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
data class RoomSelSongModel(
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
 * Join chorus input model
 *
 * @property songNo
 * @constructor Create empty Join chorus input model
 */
data class JoinChorusInputModel(
    val songNo: String
)

/**
 * Choose song input model
 *
 * @property songName
 * @property songNo
 * @property singer
 * @property imageUrl
 * @constructor Create empty Choose song input model
 */
data class ChooseSongInputModel(
    val songName: String,
    val songNo: String,
    val singer: String,
    val imageUrl: String,
)

/**
 * Make song top input model
 *
 * @property songNo
 * @constructor Create empty Make song top input model
 */
data class MakeSongTopInputModel(
    val songNo: String
)

/**
 * Update singing score input model
 *
 * @property score
 * @constructor Create empty Update singing score input model
 */
data class UpdateSingingScoreInputModel(
    val score: Double
)

/**
 * Scoring algo control model
 *
 * @property level
 * @property offset
 * @constructor Create empty Scoring algo control model
 */
data class ScoringAlgoControlModel(
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
data class ScoringAverageModel(
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
data class VolumeModel(
    val uid: Int,
    val volume: Int
)
