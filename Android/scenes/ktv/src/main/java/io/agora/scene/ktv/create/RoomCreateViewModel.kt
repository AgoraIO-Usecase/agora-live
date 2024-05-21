package io.agora.scene.ktv.create

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.ktv.service.CreateRoomInfo
import io.agora.scene.ktv.service.JoinRoomInfo
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
    val roomInfoLiveData = MutableLiveData<JoinRoomInfo?>()

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
     * @param name      the name
     * @param password  the password
     * @param icon      the icon
     */
    fun createRoom(name: String, password: String, icon: String) {
        ktvServiceProtocol.createRoom(CreateRoomInfo(icon, name, password)) { err, roomInfo ->
            if (err==null){
                roomInfoLiveData.postValue(roomInfo)
            }else{
                roomInfoLiveData.postValue(null)
            }
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
        ktvServiceProtocol.joinRoom(roomNo, password) { error, roomInfo ->
            if (error==null){
                roomInfoLiveData.postValue(roomInfo)
            }else{
                roomInfoLiveData.postValue(null)
            }
            error?.message?.let {
                ToastUtils.showToast(it)
            }
        }
    }
}
