package io.agora.scene.eCommerce.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.auikit.service.http.CommonResp
import io.agora.rtm.*
import io.agora.rtmsyncmanager.ISceneResponse
import io.agora.rtmsyncmanager.Scene
import io.agora.rtmsyncmanager.SyncManager
import io.agora.rtmsyncmanager.model.AUICommonConfig
import io.agora.rtmsyncmanager.model.AUIRoomContext
import io.agora.rtmsyncmanager.model.AUIRoomInfo
import io.agora.rtmsyncmanager.model.AUIUserThumbnailInfo
import io.agora.rtmsyncmanager.service.collection.AUIListCollection
import io.agora.rtmsyncmanager.service.collection.AUIMapCollection
import io.agora.rtmsyncmanager.service.http.HttpManager
import io.agora.rtmsyncmanager.service.http.token.TokenGenerateReq
import io.agora.rtmsyncmanager.service.http.token.TokenGenerateResp
import io.agora.rtmsyncmanager.service.http.token.TokenInterface
import io.agora.rtmsyncmanager.service.room.AUIRoomManager
import io.agora.rtmsyncmanager.utils.AUILogger
import io.agora.rtmsyncmanager.utils.GsonTools
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.TokenGenerator
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import retrofit2.Response
import java.util.*


/**
 * Show sync manager service impl
 *
 * @property context
 * @property errorHandler
 * @constructor Create empty Show sync manager service impl
 */
class ShowSyncManagerServiceImpl constructor(
    private val context: Context,
    private val errorHandler: (Exception) -> Unit
) : ShowServiceProtocol, ISceneResponse {
    /**
     * Tag
     */
    private val TAG = "ShowSyncManagerServiceImpl"

    /**
     * K scene id
     */
    private val kSceneId = "scene_ecommerce_0.2.0"

    /**
     * K collection id user
     */
    private val kCollectionIdUser = "userCollection"

    private val kCollectionIdBid = "commerce_goods_bid_collection"

    private val kCollectionIdBuy = "commerce_goods_buy_collection"

    /**
     * K robot avatars
     */
    private val kRobotAvatars = listOf("https://download.agora.io/demo/release/bot1.png")

    /**
     * K robot uid
     */
    private val kRobotUid = 2000000001

    /**
     * K robot video room ids
     */
    private val kRobotVideoRoomIds = arrayListOf(2024001, 2024002, 2024003)

    /**
     * K robot video stream urls
     */
    private val kRobotVideoStreamUrls = arrayListOf(
        "https://download.agora.io/demo/release/agora_show_video_1.mp4",
        "https://download.agora.io/demo/release/agora_show_video_2.mp4",
        "https://download.agora.io/demo/release/agora_show_video_3.mp4"
    )

    /**
     * Sync initialized
     */
    @Volatile
    private var syncInitialized = false

    /**
     * Cloud player service
     */
    private val cloudPlayerService by lazy { CloudPlayerService() }

    private val rtmClient: RtmClient

    private val syncManager: SyncManager

    private val roomManager = AUIRoomManager()

    private var roomList = emptyList<AUIRoomInfo>()

    init {
        HttpManager.setBaseURL(BuildConfig.ROOM_MANAGER_SERVER_HOST)
        AUILogger.initLogger(AUILogger.Config(context, "eCommerce"))

        val commonConfig = AUICommonConfig()
        commonConfig.context = context
        commonConfig.appId = BuildConfig.AGORA_APP_ID
        val owner = AUIUserThumbnailInfo()
        owner.userId = UserManager.getInstance().user.id.toString()
        owner.userName = UserManager.getInstance().user.name
        owner.userAvatar = UserManager.getInstance().user.headUrl
        commonConfig.owner = owner
        commonConfig.host = BuildConfig.TOOLBOX_SERVER_HOST
        AUIRoomContext.shared().setCommonConfig(commonConfig)
        rtmClient = createRtmClient()
        syncManager = SyncManager(context, rtmClient, commonConfig)
        rtmLogin { }
    }

    data class RoomInfoController constructor(
        val roomId: String,
        val scene: Scene,
        val roomInfo: AUIRoomInfo,
        val rtmChannel: StreamChannel? = null,
        val userList: ArrayList<ShowUser> = ArrayList(),

        var userCollection: AUIListCollection? = null,
        var shopCollection: AUIListCollection? = null,
        var auctionCollection: AUIMapCollection? = null,

        var userChangeSubscriber: ((List<ShowUser>) -> Unit)? = null,
        var auctionChangeSubscriber: ((AuctionModel) -> Unit)? = null,
        var shopChangeSubscriber: ((List<GoodsModel>) -> Unit)? = null,
    )

    /**
     * Room info controllers
     */
    private val roomInfoControllers = Collections.synchronizedList(mutableListOf<RoomInfoController>())

    /**
     * Destroy
     *
     */
    override fun destroy() {
        if (syncInitialized) {
            synchronized(roomInfoControllers){
                roomInfoControllers.forEach {
                }
            }
            roomInfoControllers.clear()
            syncInitialized = false
            kRobotVideoRoomIds.forEach { roomId ->
                cloudPlayerService.stopHeartBeat(roomId.toString())
            }
        }
    }

    override fun getRoomInfo(roomId: String): AUIRoomInfo? {
        return roomList.firstOrNull { it.roomId == roomId }
    }

    override fun getRoomList(): List<AUIRoomInfo> {
        return roomList
    }

    override fun fetchRoomList(
        success: (List<AUIRoomInfo>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        roomManager.getRoomInfoList(BuildConfig.AGORA_APP_ID, kSceneId, System.currentTimeMillis(), 20) { error, list ->
            if (error != null) {
                roomList = appendRobotRooms(emptyList())
                runOnMainThread { success.invoke(roomList) }
            }
            if (list != null) {
                roomList = appendRobotRooms(list)
                runOnMainThread { success.invoke(roomList) }
            }
        }
    }

    /**
     * Append robot rooms
     *
     * @param roomList
     * @return
     */
    private fun appendRobotRooms(roomList: List<AUIRoomInfo>): List<AUIRoomInfo> {
        val retRoomList = mutableListOf<AUIRoomInfo>()
        val robotRoomIds = ArrayList(kRobotVideoRoomIds)
        val kRobotRoomStartId = kRobotVideoRoomIds[0]
        retRoomList.forEach { roomDetail ->
            val differValue = roomDetail.roomId.toInt() - kRobotRoomStartId
            if (differValue >= 0) {
                robotRoomIds.firstOrNull { robotRoomId -> robotRoomId == roomDetail.roomId.toInt() }?.let { id ->
                    robotRoomIds.remove(id)
                }
            }
        }
        for (i in 0 until robotRoomIds.size) {
            val robotRoomId = robotRoomIds[i]
            val robotId = robotRoomId % 10
            val robotOwner = AUIUserThumbnailInfo().apply {
                userId = kRobotUid.toString()
                userName = "Robot $robotId"
                userAvatar = kRobotAvatars[(robotId - 1) % kRobotAvatars.size]
            }
            val roomInfo = AUIRoomInfo().apply {
                roomId = robotRoomId.toString()
                roomName = "Smooth $robotId"
                owner = robotOwner
                memberCount = 1
                thumbnail = "1"
                createTime = TimeUtils.currentTimeMillis()
            }
            retRoomList.add(roomInfo)
        }
        retRoomList.addAll(roomList)
        return retRoomList
    }

    override fun createRoom(roomId: String, roomName: String, thumbnailId: String, success: (AUIRoomInfo) -> Unit, error: ((Exception) -> Unit)?) {
        val roomInfo = AUIRoomInfo()
        roomInfo.roomId = roomId
        roomInfo.roomName = roomName
        val owner = AUIUserThumbnailInfo()
        owner.userId = UserManager.getInstance().user.id.toString()
        owner.userName = UserManager.getInstance().user.name
        owner.userAvatar = UserManager.getInstance().user.headUrl
        roomInfo.owner = owner
        roomInfo.thumbnail = thumbnailId
        roomInfo.createTime = TimeUtils.currentTimeMillis()
        roomManager.createRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomInfo) { e, info ->
            if (info != null) {
                val temp = mutableListOf<AUIRoomInfo>()
                temp.add(roomInfo)
                temp.addAll(roomList)
                roomList = temp
                success.invoke(info)
            }
            if (e != null) {
                error?.invoke(Exception(e.message))
            }
        }
    }

    override fun joinRoom(roomInfo: AUIRoomInfo, success: (AUIRoomInfo) -> Unit, error: ((Exception) -> Unit)?) {
        val ownerId = roomInfo.owner?.userId ?: run {
            error?.invoke(Exception("room info error"))
            return
        }
        rtmLogin { rtmLogin ->
            if (!rtmLogin) {
                error?.invoke(Exception("rtm login failed"))
                return@rtmLogin
            }
            val scene = syncManager.getScene(roomInfo.roomId)
            scene.bindRespDelegate(this)
            if (ownerId == UserManager.getInstance().user.id.toString()) {
                scene.create(null) { er ->
                    if (er != null) {
                        Log.d(TAG, "enter scene fail: ${er.message}")
                        error?.invoke(Exception(er.message))
                        return@create
                    }
                    scene.enter { payload, e ->
                        if (e != null) {
                            Log.d(TAG, "enter scene fail: ${e.message}")
                            error?.invoke(Exception(e.message))
                        } else {
                            val controller = RoomInfoController(roomInfo.roomId, scene, roomInfo)
                            actionSubscribe(controller)
                            roomInfoControllers.add(controller)
                            innerAddLocalUser(controller)
                            addOriginalData(controller)
                            success.invoke(roomInfo)
                        }
                    }
                }
            } else {
                scene.enter { payload, e ->
                    if (e != null) {
                        Log.d(TAG, "enter scene fail: ${e.message}")
                        error?.invoke(Exception(e.message))
                    } else {
                        val controller = RoomInfoController(roomInfo.roomId, scene, roomInfo)
                        actionSubscribe(controller)
                        roomInfoControllers.add(controller)
                        innerAddLocalUser(controller)
                        success.invoke(roomInfo)
                    }
                }
            }
        }
    }

    override fun leaveRoom(roomId: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val roomInfo = controller.roomInfo
        val scene = syncManager.getScene(roomId)
        scene.unbindRespDelegate(this)
        if (roomInfo.owner?.userId == UserManager.getInstance().user.id.toString()) {
            innerRemoveLocalUser(controller)
            scene.delete()
            roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId) { e ->

            }
        } else {
            innerRemoveLocalUser(controller)
            scene.leave()
        }
        cleanRoomInfoController(controller)
        roomInfoControllers.remove(controller)
    }
    override fun deleteRoom(roomId: String, complete: () -> Unit) {
        syncManager.getScene(roomId).delete()
        roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId) { e ->
            complete.invoke()
        }
    }
    private fun cleanRoomInfoController(controller: RoomInfoController) {
        controller.userChangeSubscriber = null
        controller.auctionChangeSubscriber = null
        controller.shopChangeSubscriber = null
    }

    private fun actionSubscribe(controller: RoomInfoController) {
        controller.userCollection = controller.scene.getCollection(kCollectionIdUser) { a, b, c ->
            AUIListCollection(a, b, c)
        }
        controller.userCollection?.subscribeAttributesDidChanged { channelName, key, model ->
            val list = model.getList()
            if (list == null) {
                controller.userChangeSubscriber?.invoke(emptyList())
                return@subscribeAttributesDidChanged
            }
            val gson = Gson()
            val userList: List<ShowUser> = list.map { map ->
                gson.fromJson(gson.toJsonTree(map), ShowUser::class.java)
            }
            controller.userChangeSubscriber?.invoke(userList)
        }
        controller.shopCollection = controller.scene.getCollection(kCollectionIdBuy) { a, b, c ->
            AUIListCollection(a, b, c)
        }
        controller.shopCollection?.subscribeAttributesDidChanged { channelName, key, model ->
            val list = model.getList()
            if (list == null) {
                controller.userChangeSubscriber?.invoke(emptyList())
                return@subscribeAttributesDidChanged
            }
            val gson = Gson()
            val goodsList: List<GoodsModel> = list.map { map ->
                gson.fromJson(gson.toJsonTree(map), GoodsModel::class.java)
            }
            controller.shopChangeSubscriber?.invoke(goodsList)
        }
        controller.auctionCollection = controller.scene.getCollection(kCollectionIdBid) { a, b, c ->
            AUIMapCollection(a, b, c)
        }
        controller.auctionCollection?.subscribeAttributesDidChanged { channelName, key, model ->
            val gson = Gson()
            val json = gson.toJson(model.getMap())
            val auction = gson.fromJson(json, AuctionModel::class.java)
            controller.auctionChangeSubscriber?.invoke(auction)
        }
    }

    private fun addOriginalData(controller: RoomInfoController) {
        val gson = Gson()
        val map1 = gson.fromJson(gson.toJson(GoodsModel(title = "1")), Map::class.java) as Map<String, Any>
        val map2 = gson.fromJson(gson.toJson(GoodsModel(title = "2")), Map::class.java) as Map<String, Any>
        val map3 = gson.fromJson(gson.toJson(GoodsModel(title = "3")), Map::class.java) as Map<String, Any>
        controller.shopCollection?.addMetaData(null, map1) {}
        controller.shopCollection?.addMetaData(null, map2) {}
        controller.shopCollection?.addMetaData(null, map3) {}
    }

    /** User Actions */
    override fun subscribeUser(
        roomId: String,
        onChange: (List<ShowUser>) -> Unit
    ) {
        val roomInfoController = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        roomInfoController.userChangeSubscriber = onChange
    }
    private fun innerAddLocalUser(controller: RoomInfoController) {
        val user = ShowUser(
            UserManager.getInstance().user.id.toString(),
            UserManager.getInstance().user.headUrl,
            UserManager.getInstance().user.name
        )
        val gson = Gson()
        val json = gson.toJson(user)
        val map = gson.fromJson(json, Map::class.java) as Map<String, Any>
        controller.userCollection?.addMetaData(null, map, null) { err ->
        }
    }

    private fun innerRemoveLocalUser(controller: RoomInfoController) {
        val filter = listOf(mapOf<String, Any>("userId" to UserManager.getInstance().user.id.toString()))
        controller.userCollection?.removeMetaData(null, filter) {
        }
    }

    /** Message Actions */
    override fun sendChatMessage(roomId: String, message: String, success: (() -> Unit)?, error: ((Exception) -> Unit)?) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return

    }
    override fun subscribeMessage(
        roomId: String,
        onMessageChange: (ShowMessage) -> Unit
    ) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        rtmClient.unsubscribe(roomId, object: ResultCallback<Void> {
            override fun onSuccess(responseInfo: Void?) {
                TODO("Not yet implemented")
            }

            override fun onFailure(errorInfo: ErrorInfo?) {
                TODO("Not yet implemented")
            }
        })
    }

    /** Auction Actions */
    override fun subscribeAuction(roomId: String, onChange: (AuctionModel) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.auctionChangeSubscriber = onChange
    }
    override fun startAuction(roomId: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val auctionModel = AuctionModel().apply {
            timestamp = TimeUtils.currentTimeMillis().toString()
        }
        val map = GsonTools.beanToMap(auctionModel)
        controller.auctionCollection?.addMetaData(null, map, null) { e ->
            Log.d(TAG, "startAuction e: $e")
        }
    }
    override fun bidding(roomId: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val mergeKey = listOf("bid")
        controller.auctionCollection?.calculateMetaData(null, mergeKey, 1, 0, 1000, null) { err ->
        }
        val user = ShowUser(
            UserManager.getInstance().user.id.toString(),
            UserManager.getInstance().user.headUrl,
            UserManager.getInstance().user.name
        )
        val userMap = GsonTools.beanToMap(user)
        val map = mapOf("bidUser" to userMap)
        controller.auctionCollection?.updateMetaData(null, map) {
        }
    }
    override fun finishAuction(roomId: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val map = mapOf<String, Any>("status" to 0)
        controller.auctionCollection?.updateMetaData(null, map) {
        }
    }

    /** Shop Actions */
    override fun subscribeShop(roomId: String, onChange: (List<GoodsModel>) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.shopChangeSubscriber = onChange
    }
    override fun buyItem(roomId: String, itemId: String, onComplete: (Exception?) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val keys = listOf("quantity")
        val filter = listOf(mapOf<String, Any>("title" to itemId))
        controller.shopCollection?.calculateMetaData(null, keys, -1, 0, 1000, filter) { err ->
            if (err != null) {
                onComplete.invoke(err)
            } else {
                onComplete.invoke(null)
            }
        }
    }
    override fun updateRemain(roomId: String, itemId: String, count: UInt) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val map = mapOf("quantity" to count)
        val filter = listOf(mapOf<String, Any>("title" to itemId))
        controller.shopCollection?.updateMetaData(null, map, filter) {
        }
    }

    private var isRtmLogin = false
    private fun rtmLogin(complete: (Boolean) -> Unit) {
        if (isRtmLogin) {
            complete.invoke(true)
            return
        }
        HttpManager
            .getService(TokenInterface::class.java)
            .tokenGenerate(
                TokenGenerateReq(
                    appId = BuildConfig.AGORA_APP_ID,
                    appCert = "",
                    channelName = "111",
                    userId = UserManager.getInstance().user.id.toString()
                )
            )
            .enqueue(object : retrofit2.Callback<CommonResp<TokenGenerateResp>> {
                override fun onResponse(
                    call: retrofit2.Call<CommonResp<TokenGenerateResp>>,
                    response: Response<CommonResp<TokenGenerateResp>>
                ) {
                    val rspObj = response.body()?.data
                    if (rspObj != null) {
                        rtmClient.login(rspObj.rtmToken, object : ResultCallback<Void> {
                            override fun onSuccess(responseInfo: Void?) {
                                isRtmLogin = true
                                complete.invoke(true)
                            }

                            override fun onFailure(errorInfo: ErrorInfo?) {
                                if (errorInfo?.errorCode == RtmConstants.RtmErrorCode.LOGIN_REJECTED) {
                                    isRtmLogin = true
                                    complete.invoke(true)
                                } else {
                                    complete.invoke(false)
                                }
                            }
                        })
                    } else {
                        complete.invoke(false)
                    }
                }
                override fun onFailure(call: retrofit2.Call<CommonResp<TokenGenerateResp>>, t: Throwable) {
                    complete.invoke(false)
                }
            })
    }

    /**
     * Start cloud player
     *
     */
    override fun startCloudPlayer() {
        for (i in 0 until kRobotVideoRoomIds.size) {
            val roomId = kRobotVideoRoomIds[i]
            cloudPlayerService.startCloudPlayer(
                roomId.toString(),
                UserManager.getInstance().user.id.toString(),
                kRobotUid,
                kRobotVideoStreamUrls[i],
                "cn",
                success = {
                    cloudPlayerService.startHeartBeat(
                        roomId.toString(),
                        UserManager.getInstance().user.id.toString()
                    )
                },
                failure = { })
        }
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mainHandler.looper.thread) {
            r.run()
        } else {
            mainHandler.post(r)
        }
    }

    private fun createRtmClient(): RtmClient {
        val commonConfig = AUIRoomContext.shared().requireCommonConfig()
        val userInfo = AUIRoomContext.shared().currentUserInfo
        val rtmConfig = RtmConfig.Builder(commonConfig.appId, userInfo.userId).apply {
            presenceTimeout(60)
        }.build()
        if (rtmConfig.appId.isEmpty()) {
            assert(false) { "userId is empty" }
        }
        if (rtmConfig.userId.isEmpty()) {
            assert(false) { "appId is empty, please check 'AUIRoomContext.shared.commonConfig.appId'" }
        }
        return RtmClient.create(rtmConfig)
    }

    override fun onSceneDestroy(roomId: String) {
        leaveRoom(roomId)
        roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId) {
        }
    }
}