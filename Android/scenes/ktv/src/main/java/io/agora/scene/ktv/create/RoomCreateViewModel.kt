package io.agora.scene.ktv.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.ktv.service.CreateRoomInputModel
import io.agora.scene.ktv.service.JoinRoomInputModel
import io.agora.scene.ktv.service.JoinRoomOutputModel
import io.agora.scene.ktv.service.KTVServiceProtocol.Companion.getImplInstance

/**
 * The type Room create view model.
 */
class RoomCreateViewModel
/**
 * Instantiates a new Room create view model.
 *
 * @param application the application
 */
    (application: Application) : AndroidViewModel(application) {
    private val ktvServiceProtocol = getImplInstance()

    /**
     * The Room model list.
     */
    @JvmField
    val roomModelList = MutableLiveData<List<AUIRoomInfo>>()

    /**
     * The Join room result.
     */
    @JvmField
    val joinRoomResult = MutableLiveData<JoinRoomOutputModel?>()

    /**
     * The Create room result.
     */
    val createRoomResult = MutableLiveData<AUIRoomInfo>()

    /**
     * 加载房间列表
     */
    fun loadRooms() {
        ktvServiceProtocol.getRoomList { vlRoomListModels: List<AUIRoomInfo> ->
            roomModelList.postValue(vlRoomListModels)
            null
        }
    }

    /**
     * Create room.
     *
     * @param isPrivate the is private
     * @param name      the name
     * @param password  the password
     * @param userNo    the user no
     * @param icon      the icon
     */
    fun createRoom(
        isPrivate: Int, name: String, password: String, userNo: String, icon: String
    ) {
        ktvServiceProtocol.createRoom(
            CreateRoomInputModel(icon, isPrivate, name, password, userNo)
        ) { e: Exception?, roomInfo: AUIRoomInfo? ->
            if (e == null && roomInfo != null) {
                // success
                createRoomResult.postValue(roomInfo)
            } else {
                // failed
                if (e != null) {
                    ToastUtils.showToast(e.message)
                }
                createRoomResult.postValue(null)
            }
        }
    }

    /**
     * Join room.
     *
     * @param roomNo   the room no
     * @param password the password
     */
    fun joinRoom(roomNo: String?, password: String?) {
        ktvServiceProtocol.joinRoom(
            JoinRoomInputModel(
                roomNo!!,
                password
            )
        ) { e: Exception?, joinRoomOutputModel: JoinRoomOutputModel? ->
            if (e == null && joinRoomOutputModel != null) {
                // success
                joinRoomResult.postValue(joinRoomOutputModel)
            } else {
                // failed
                if (e != null) {
                    ToastUtils.showToast(e.message)
                }
                joinRoomResult.postValue(null)
            }
        }
    }
}
