package io.agora.scene.ktv.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.ktv.R
import io.agora.scene.ktv.service.CreateRoomInfo
import io.agora.scene.ktv.service.KTVServiceProtocol.Companion.getImplInstance
import io.agora.scene.widget.toast.CustomToast

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
    val roomModelList = MutableLiveData<List<AUIRoomInfo>?>()

    /**
     * The Join room result.
     */
    val roomInfoLiveData = MutableLiveData<AUIRoomInfo?>()

    /**
     * load rooms.
     */
    fun loadRooms() {
        ktvServiceProtocol.getRoomList { error, vlRoomListModels ->
            roomModelList.postValue(vlRoomListModels)
            error?.message?.let {
                ToastUtils.showToast(it)
            }
        }
    }

    /**
     * Create room.
     *
     * @param name      the name
     * @param password  the password
     * @param icon      the icon
     */
    fun createRoom(name: String, password: String, icon: String) {
        ktvServiceProtocol.createRoom(CreateRoomInfo(icon, name, password)) { err, roomInfo ->
            if (err == null && roomInfo != null) {
                roomInfoLiveData.postValue(roomInfo)
            } else {
                roomInfoLiveData.postValue(null)
                CustomToast.show(R.string.ktv_create_room_failed, err?.message ?: "")
            }
        }
    }

    /**
     * Join room.
     *
     * @param roomNo   the room no
     * @param password the password
     */
    fun joinRoom(roomInfo: AUIRoomInfo, password: String?) {
        ktvServiceProtocol.joinRoom(roomInfo.roomId, password) { err ->
            if (err == null) { // success
                roomInfoLiveData.postValue(roomInfo)
            } else {
                roomInfoLiveData.postValue(null)
                CustomToast.show(R.string.ktv_join_room_failed, err.message ?: "")
            }
        }
    }
}
