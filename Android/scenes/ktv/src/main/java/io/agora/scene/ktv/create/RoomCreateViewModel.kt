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
    val roomModelList = MutableLiveData<List<AUIRoomInfo>>()

    /**
     * The Join room result.
     */
    val joinRoomResult = MutableLiveData<JoinRoomOutputModel?>()

    /**
     * The Create room result.
     */
    val createRoomResult = MutableLiveData<AUIRoomInfo>()

    /**
     * 加载房间列表
     */
    fun loadRooms() {
        ktvServiceProtocol.getRoomList { vlRoomListModels ->
            roomModelList.postValue(vlRoomListModels)
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
    fun createRoom(isPrivate: Int, name: String, password: String, userNo: String, icon: String) {
        ktvServiceProtocol.createRoom(CreateRoomInputModel(icon, isPrivate, name, password, userNo)) { err, roomInfo ->
            createRoomResult.postValue(roomInfo)
            err?.message?.let {
                ToastUtils.showToast(it)
            }
        }
    }

    /**
     * Join room.
     *
     * @param roomNo   the room no
     * @param password the password
     */
    fun joinRoom(roomNo: String, password: String?) {
        ktvServiceProtocol.joinRoom(JoinRoomInputModel(roomNo, password)) { error, roomInfo ->
            if (error == null) {
                joinRoomResult.postValue(roomInfo)
            } else {
                joinRoomResult.postValue(null)
            }
            error?.message?.let {
                ToastUtils.showToast(it)
            }
        }
    }
}
