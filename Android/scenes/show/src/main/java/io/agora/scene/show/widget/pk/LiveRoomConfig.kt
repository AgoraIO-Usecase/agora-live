package io.agora.scene.show.widget.pk

import io.agora.scene.show.service.ShowRoomDetailModel

/**
 * Live room config
 *
 * @constructor
 *
 * @param room
 * @param waitingForPK
 */
class LiveRoomConfig constructor(room: ShowRoomDetailModel, interactStatus: Int, waitingForPK: Boolean) {
    /**
     * Room id
     */
    private val roomId: String

    /**
     * Room name
     */
    private val roomName: String

    /**
     * Room user count
     */
    private val roomUserCount: Int

    /**
     * Thumbnail id
     */
    private val thumbnailId: String // 0, 1, 2, 3

    /**
     * Owner id
     */
    private val ownerId: String

    /**
     * Owner avatar
     */
    private val ownerAvatar: String// http url

    /**
     * Owner name
     */
    private val ownerName: String

    /**
     * Interact status
     */
    private val interactStatus: Int

    /**
     * Created at
     */
    private val createdAt: Double

    /**
     * Updated at
     */
    private val updatedAt: Double

    /**
     * Is Pure Mode
     */
    private val isPureMode: Boolean

    /**
     * Waiting for p k
     */
    private val waitingForPK: Boolean

    private val robotMaxCount = 6

    init {
        roomId = room.roomId
        roomName = room.roomName
        roomUserCount = room.roomUserCount
        ownerId = room.ownerId
        ownerAvatar = room.ownerAvatar
        ownerName = room.ownerName
        this.interactStatus = interactStatus
        createdAt = room.createdAt
        updatedAt = room.updatedAt
        isPureMode = room.isPureMode
        this.waitingForPK = waitingForPK
        thumbnailId = room.thumbnailId
    }

    /**
     * Convert to show room detail model
     *
     * @return
     */
    fun convertToShowRoomDetailModel() : ShowRoomDetailModel {
        return ShowRoomDetailModel(
            roomId,
            roomName,
            roomUserCount,
            ownerId,
            thumbnailId,
            ownerAvatar,
            ownerName,
            isPureMode,
            createdAt,
            updatedAt
        )
    }

    /**
     * Get owner avatar
     *
     * @return
     */
    fun getOwnerAvatar() : String {
        return ownerAvatar
    }

    /**
     * Get owner name
     *
     * @return
     */
    fun getOwnerName() : String {
        return ownerName
    }

    /**
     * Get interact status
     *
     * @return
     */
    fun getInteractStatus() : Int {
        return interactStatus
    }

    /**
     * Is waiting for p k
     *
     * @return
     */
    fun isWaitingForPK() : Boolean {
        return waitingForPK
    }

    /**
     * Is robot room
     *
     */
    fun isRobotRoom() = roomId.length > robotMaxCount

    fun isPureMode() : Boolean {
        return isPureMode
    }

    fun getRoomId() = roomId
}
