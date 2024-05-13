package io.agora.scene.ktv.create;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import io.agora.scene.base.utils.ToastUtils;
import io.agora.scene.ktv.service.CreateRoomInputModel;
import io.agora.scene.ktv.service.CreateRoomOutputModel;
import io.agora.scene.ktv.service.JoinRoomInputModel;
import io.agora.scene.ktv.service.JoinRoomOutputModel;
import io.agora.scene.ktv.service.KTVServiceProtocol;
import io.agora.scene.ktv.service.RoomListModel;
import io.reactivex.internal.observers.LambdaObserver;
import kotlin.jvm.internal.Lambda;

/**
 * The type Room create view model.
 */
public class RoomCreateViewModel extends AndroidViewModel {
    private final KTVServiceProtocol ktvServiceProtocol = KTVServiceProtocol.Companion.getImplInstance();

    /**
     * The Room model list.
     */
    public final MutableLiveData<List<RoomListModel>> roomModelList = new MutableLiveData<>();
    /**
     * The Join room result.
     */
    public final MutableLiveData<JoinRoomOutputModel> joinRoomResult = new MutableLiveData<>();
    /**
     * The Create room result.
     */
    public final MutableLiveData<CreateRoomOutputModel> createRoomResult = new MutableLiveData<>();

    /**
     * Instantiates a new Room create view model.
     *
     * @param application the application
     */
    public RoomCreateViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * 加载房间列表
     */
    public void loadRooms() {
        ktvServiceProtocol.getRoomList((e, vlRoomListModels) -> {
            if (e != null) {
                roomModelList.postValue(null);
                return null;
            }
            roomModelList.postValue(vlRoomListModels);
            return null;
        });
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
    public void createRoom(int isPrivate,
                           String name, String password,
                           String userNo, String icon) {
        ktvServiceProtocol.createRoom(new CreateRoomInputModel(
                icon, isPrivate, name, password, userNo
        ), (e, createRoomOutputModel) -> {
            if (e == null && createRoomOutputModel != null) {
                // success
                createRoomResult.postValue(createRoomOutputModel);
            } else {
                // failed
                if (e != null) {
                    ToastUtils.showToast(e.getMessage());
                }
                createRoomResult.postValue(null);
            }
            return null;
        });
    }

    /**
     * Join room.
     *
     * @param roomNo   the room no
     * @param password the password
     */
    public void joinRoom(String roomNo, String password) {
        ktvServiceProtocol.joinRoom(new JoinRoomInputModel(roomNo, password),
                (e, joinRoomOutputModel) -> {
                    if (e == null && joinRoomOutputModel != null) {
                        // success
                        joinRoomResult.postValue(joinRoomOutputModel);
                    } else {
                        // failed
                        if (e != null) {
                            ToastUtils.showToast(e.getMessage());
                        }
                        joinRoomResult.postValue(null);
                    }
                    return null;
                });
    }

}
