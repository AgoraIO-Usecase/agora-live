package io.agora.scene.eCommerce.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import io.agora.auikit.service.http.CommonResp
import io.agora.rtm.*
import io.agora.rtmsyncmanager.ISceneResponse
import io.agora.rtmsyncmanager.Scene
import io.agora.rtmsyncmanager.SyncManager
import io.agora.rtmsyncmanager.model.*
import io.agora.rtmsyncmanager.service.IAUIUserService
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
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import org.json.JSONException
import org.json.JSONObject
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
    private val kSceneId = "scene_ecommerce_0_2_0"
    private val kCollectionIdLike = "commerce_like_collection"
    private val kCollectionIdMessage = "commerce_message_collection"
    private val kCollectionIdBid = "commerce_goods_bid_collection"
    private val kCollectionIdBuy = "commerce_goods_buy_collection"

    private val syncManager: SyncManager

    private val roomManager = AUIRoomManager()

    private var roomList = emptyList<RoomDetailModel>()

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
        commonConfig.host = BuildConfig.ROOM_MANAGER_SERVER_HOST
        AUIRoomContext.shared().setCommonConfig(commonConfig)
        syncManager = SyncManager(context, null, commonConfig)
        rtmLogin { }
    }

    data class RoomInfoController constructor(
        val roomId: String,
        val scene: Scene,
        val roomInfo: RoomDetailModel,
        val rtmChannel: StreamChannel? = null,
        var userList: MutableList<AUIUserInfo> = mutableListOf(),
        var goodsList: List<GoodsModel>? = null,
        var auctionModel: AuctionModel? = null,

        var shopCollection: AUIListCollection? = null,
        var auctionCollection: AUIMapCollection? = null,
        var messageCollection: AUIMapCollection? = null,
        var likeCollection: AUIMapCollection? = null,

        var userChangeSubscriber: ((Int) -> Unit)? = null,
        var auctionChangeSubscriber: ((AuctionModel) -> Unit)? = null,
        var shopChangeSubscriber: ((List<GoodsModel>) -> Unit)? = null,
        var roomChangeSubscriber: (() -> Unit)? = null,
        var messageChangeSubscriber: ((ShowMessage) -> Unit)? = null,
        var likeChangeSubscriber: ((ShowMessage) -> Unit)? = null,
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
        synchronized(roomInfoControllers){
            roomInfoControllers.forEach {
                cleanRoomInfoController(it)
            }
        }
        roomInfoControllers.clear()
    }

    override fun getRoomInfo(roomId: String): RoomDetailModel? {
        return roomList.firstOrNull { it.roomId == roomId }
    }

    override fun getRoomList(): List<RoomDetailModel> {
        return roomList
    }

    override fun fetchRoomList(
        success: (List<RoomDetailModel>) -> Unit,
        error: ((Exception) -> Unit)?
    ) {
        roomManager.getRoomInfoList(BuildConfig.AGORA_APP_ID, kSceneId, System.currentTimeMillis(), 20) { e, list ->
            if (e != null) {
                roomList = emptyList()
                runOnMainThread { success.invoke(roomList) }
            }
            if (list != null) {
                roomList = toDetailList(list)
                runOnMainThread { success.invoke(roomList) }
            }
        }
    }

    override fun createRoom(roomId: String, roomName: String, thumbnailId: String, success: (RoomDetailModel) -> Unit, error: ((Exception) -> Unit)?) {
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
        val roomDetailModel = RoomDetailModel(
            roomId,
            roomName,
            1,
            thumbnailId,
            UserManager.getInstance().user.id.toString(),
            UserManager.getInstance().user.headUrl,
            UserManager.getInstance().user.name,
            ShowRoomStatus.activity.value,
            TimeUtils.currentTimeMillis(),
            TimeUtils.currentTimeMillis()
        )
        roomInfo.customPayload = GsonTools.beanToMap(roomDetailModel)
        roomManager.createRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomInfo) { e, info ->
            if (info != null) {
                val temp = mutableListOf<RoomDetailModel>()
                temp.add(roomDetailModel)
                temp.addAll(roomList)
                roomList = temp
                success.invoke(roomDetailModel)
            }
            if (e != null) {
                error?.invoke(Exception(e.message))
            }
        }
    }

    override fun joinRoom(roomInfo: RoomDetailModel, success: (RoomDetailModel) -> Unit, error: ((Exception) -> Unit)?) {
        rtmLogin { rtmLogin ->
            if (!rtmLogin) {
                error?.invoke(Exception("rtm login failed"))
                return@rtmLogin
            }
            leaveRoom(roomInfo.roomId)
            val scene = syncManager.getScene(roomInfo.roomId)
            scene.userService.registerRespObserver(userObserver)

            val controller = RoomInfoController(roomInfo.roomId, scene, roomInfo)
            roomInfoControllers.add(controller)
            Log.d(TAG, "[commerce] add controller ${roomInfo.roomId}")
            actionSubscribe(controller)
            scene.bindRespDelegate(this)
            if (roomInfo.ownerId.toLong() == UserManager.getInstance().user.id) {
                scene.create(null) { er ->
                    if (er != null) {
                        Log.d(TAG, "enter scene fail: ${er.message}")
                        error?.invoke(Exception(er.message))
                        return@create
                    }
                    scene.enter { _, e ->
                        if (e != null) {
                            Log.d(TAG, "enter scene fail: ${e.message}")
                            roomInfoControllers.remove(controller)
                            error?.invoke(Exception(e.message))
                        } else {
                            success.invoke(roomInfo)
                            addOriginalData(controller)
                        }
                    }
                }
            } else {
                scene.enter { _, e ->
                    if (e != null) {
                        Log.d(TAG, "enter scene fail: ${e.message}")
                        roomInfoControllers.remove(controller)
                        error?.invoke(Exception(e.message))
                    } else {
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
        if (roomInfo.ownerId.toLong() == UserManager.getInstance().user.id) {
            scene.delete()
            roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId) { e ->
            }
        } else {
            scene.leave()
        }
        roomInfoControllers.forEach { c ->
            if (controller.roomId == roomId) {
                cleanRoomInfoController(c)
            }
        }
        roomInfoControllers.removeAll { it.roomId == roomId }
        Log.d(TAG, "[commerce] leaveRoom $roomId")
    }
    override fun deleteRoom(roomId: String, complete: () -> Unit) {
        syncManager.getScene(roomId).delete()
        roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId) { e ->
            complete.invoke()
        }
    }

    override fun subscribeCurrRoomEvent(roomId: String, onUpdate: () -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.roomChangeSubscriber = onUpdate
    }
    private fun cleanRoomInfoController(controller: RoomInfoController) {
        controller.userChangeSubscriber = null
        controller.auctionChangeSubscriber = null
        controller.shopChangeSubscriber = null
        controller.roomChangeSubscriber = null
        controller.messageChangeSubscriber = null
    }
    private fun actionSubscribe(controller: RoomInfoController) {
        controller.shopCollection = controller.scene.getCollection(kCollectionIdBuy) { a, b, c ->
            AUIListCollection(a, b, c)
        }
        controller.shopCollection?.subscribeAttributesDidChanged { _, _, model ->
            val list = model.getList()
            if (list == null) {
                controller.shopChangeSubscriber?.invoke(emptyList())
                return@subscribeAttributesDidChanged
            }
            val gson = Gson()
            val goodsList: List<GoodsModel> = list.map { map ->
                gson.fromJson(gson.toJsonTree(map), GoodsModel::class.java)
            }
            controller.goodsList = goodsList
            runOnMainThread { controller.shopChangeSubscriber?.invoke(goodsList) }
        }
        controller.auctionCollection = controller.scene.getCollection(kCollectionIdBid) { a, b, c ->
            AUIMapCollection(a, b, c)
        }
        controller.auctionCollection?.subscribeAttributesDidChanged { _, _, model ->
            val gson = Gson()
            val json = gson.toJson(model.getMap())
            val auction = gson.fromJson(json, AuctionModel::class.java)
            controller.auctionModel = auction
            runOnMainThread { controller.auctionChangeSubscriber?.invoke(auction) }
        }
        controller.messageCollection = controller.scene.getCollection(kCollectionIdMessage) { a, b, c ->
            AUIMapCollection(a, b, c)
        }
        controller.messageCollection?.subscribeAttributesDidChanged { _, _, model ->
            val gson = Gson()
            val json = gson.toJson(model.getMap())
            val message = gson.fromJson(json, ShowMessage::class.java)
            runOnMainThread { controller.messageChangeSubscriber?.invoke(message) }
        }
        controller.likeCollection = controller.scene.getCollection(kCollectionIdLike) { a, b, c ->
            AUIMapCollection(a, b, c)
        }
        controller.likeCollection?.subscribeAttributesDidChanged { _, _, model ->
            val gson = Gson()
            val json = gson.toJson(model.getMap())
            val message = gson.fromJson(json, ShowMessage::class.java)
            runOnMainThread { controller.likeChangeSubscriber?.invoke(message) }
        }
    }

    private fun addOriginalData(controller: RoomInfoController) {
        val map1 = GsonTools.beanToMap(
            GoodsModel(
                goodsId = "0",
                imageName = "commerce_shop_goods_0",
                title = context.getString(io.agora.scene.eCommerce.R.string.commerce_shop_auction_item_0),
                quantity = 6,
                price = 20f,
            )
        )
        val map2 = GsonTools.beanToMap(
            GoodsModel(
                goodsId = "1",
                imageName = "commerce_shop_goods_1",
                title = context.getString(io.agora.scene.eCommerce.R.string.commerce_shop_auction_item_1),
                quantity = 0,
                price = 5f,
            )
        )
        val map3 = GsonTools.beanToMap(
            GoodsModel(
                goodsId = "2",
                imageName = "commerce_shop_goods_2",
                title = context.getString(io.agora.scene.eCommerce.R.string.commerce_shop_auction_item_2),
                quantity = 6,
                price = 12f,
            )
        )
        controller.shopCollection?.addMetaData(null, map1, listOf(mapOf<String, Any>("goodsId" to "0"))) {}
        mainHandler.postDelayed({
            controller.shopCollection?.addMetaData(null, map2, listOf(mapOf<String, Any>("goodsId" to "1"))) {}
        }, 200)
        mainHandler.postDelayed({
            controller.shopCollection?.addMetaData(null, map3, listOf(mapOf<String, Any>("goodsId" to "2"))) {}
        }, 400)
        val auctionModel = AuctionModel().apply {
            goods = GoodsModel(
                goodsId = "",
                title = context.getString(io.agora.scene.eCommerce.R.string.commerce_shop_auction_item_1),
                quantity = 1,
                price = 1f
            )
            status = 0
        }
        val auctionMap = GsonTools.beanToMap(auctionModel)
        controller.auctionCollection?.addMetaData(null, auctionMap, null) {}
    }

    /** User Actions */
    override fun subscribeUser(roomId: String, onChange: (count: Int) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        onChange.invoke(controller.userList.size)
        controller.userChangeSubscriber = onChange
    }

    private val userObserver: IAUIUserService.AUIUserRespObserver = object: IAUIUserService.AUIUserRespObserver {
        override fun onRoomUserSnapshot(roomId: String, userList: List<AUIUserInfo?>?) {
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            val list = userList?.mapNotNull { it }
            if (list != null) {
                controller.userList.clear()
                controller.userList.addAll(list)
            }
            runOnMainThread { controller.userChangeSubscriber?.invoke(controller.userList.size) }
        }
        override fun onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            if (controller.userList.firstOrNull { it.userId == userInfo.userId } == null) {
                controller.userList.add(userInfo)
                runOnMainThread { controller.userChangeSubscriber?.invoke(controller.userList.size) }
            }
        }
        override fun onRoomUserLeave(roomId: String, userInfo: AUIUserInfo) {
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            controller.userList.removeAll { it.userId == userInfo.userId }
            runOnMainThread { controller.userChangeSubscriber?.invoke(controller.userList.size) }
        }
        override fun onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            val existUser = controller.userList.firstOrNull { it.userId == userInfo.userId }
            if (existUser != null) {
                controller.userList.remove(existUser)
                controller.userList.add(userInfo)
            }
        }
    }

    /** Message Actions */
    override fun sendChatMessage(roomId: String, message: String, success: (() -> Unit)?, error: ((Exception) -> Unit)?) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val messageModel = ShowMessage(
            UserManager.getInstance().user.id.toString(),
            UserManager.getInstance().user.name,
            message,
            System.currentTimeMillis().toDouble()
        )
        val messageMap = GsonTools.beanToMap(messageModel)
        controller.messageCollection?.addMetaData(null, messageMap) {}
    }
    override fun subscribeMessage(roomId: String, onMessageChange: (ShowMessage) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.messageChangeSubscriber = onMessageChange
    }

    /** Like Actions */
    override fun likeSend(roomId: String, success: (() -> Unit)?, error: ((Exception) -> Unit)?) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val keys = listOf("count")
        controller.likeCollection?.calculateMetaData(null, keys, 1, 0, Int.MAX_VALUE, null) {
        }
    }
    override fun likeSubscribe(roomId: String, onLikeCountChange: (ShowMessage) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.likeChangeSubscriber = onLikeCountChange
    }

    /** Auction Actions */
    override fun auctionSubscribe(roomId: String, onChange: (AuctionModel) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val model = controller.auctionModel
        if (model != null) {
            onChange.invoke(model)
        }
        controller.auctionChangeSubscriber = onChange
    }
    override fun auctionStart(roomId: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val auctionModel = AuctionModel().apply {
            timestamp = TimeUtils.currentTimeMillis().toString()
            bid = 1
            status = 1
        }
        val map = GsonTools.beanToMap(auctionModel)
        controller.auctionCollection?.updateMetaData(null, map, null) { e ->
            Log.d(TAG, "startAuction e: $e")
        }
    }
    override fun auctionBidding(roomId: String, value: Int) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val user = ShowUser(
            UserManager.getInstance().user.id.toString(),
            UserManager.getInstance().user.headUrl,
            UserManager.getInstance().user.name
        )
        val userMap = GsonTools.beanToMap(user)
        val map = mapOf(
            "bidUser" to userMap,
            "bid" to value
        )
        controller.auctionCollection?.updateMetaData(null, map) {
        }
    }
    override fun auctionReset(roomId: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val map = mapOf(
            "timestamp" to "0",
            "bid" to 1,
            "status" to 2
        )
        controller.auctionCollection?.updateMetaData(null, map) {
        }
    }

    /** Shop Actions */
    override fun shopSubscribe(roomId: String, onChange: (List<GoodsModel>) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val list = controller.goodsList
        if (list != null) {
            onChange.invoke(list)
        }
        controller.shopChangeSubscriber = onChange
    }
    override fun shopBuyItem(roomId: String, itemId: String, onComplete: (Exception?) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val keys = listOf("quantity")
        val filter = listOf(mapOf<String, Any>("goodsId" to itemId))
        controller.shopCollection?.calculateMetaData(null, keys, -1, 0, 1000, filter) { err ->
            if (err != null) {
                runOnMainThread { onComplete.invoke(err) }
            } else {
                runOnMainThread { onComplete.invoke(null) }
            }
        }
    }
    override fun shopUpdateItem(roomId: String, itemId: String, count: Int) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val map = mapOf("quantity" to count)
        val filter = listOf(mapOf<String, Any>("goodsId" to itemId))
        controller.shopCollection?.updateMetaData(null, map, filter) {
        }
    }

    private var isRtmLogin = false
    private fun rtmLogin(complete: (Boolean) -> Unit) {
        if (isRtmLogin) {
            complete.invoke(true)
            return
        }
        Log.d("ShowSyncManagerServiceImpl", "1.rtmLogin request start -> rtm login")
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
                        syncManager.login(rspObj.rtmToken) { error ->
                            if (error == null) {
                                isRtmLogin = true
                                complete.invoke(true)
                            } else {
                                complete.invoke(false)
                            }
                        }
                    } else {
                        complete.invoke(false)
                    }
                }
                override fun onFailure(call: retrofit2.Call<CommonResp<TokenGenerateResp>>, t: Throwable) {
                    complete.invoke(false)
                }
            })
    }

    override fun onSceneDestroy(roomId: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.roomChangeSubscriber?.invoke()
        leaveRoom(roomId)
    }

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private fun runOnMainThread(r: Runnable) {
        if (Thread.currentThread() == mainHandler.looper.thread) {
            r.run()
        } else {
            mainHandler.post(r)
        }
    }

    private fun isJsonString(str: String): Boolean {
        return try {
            JSONObject(str)
            true
        } catch (e: JSONException) {
            false
        }
    }

    private fun toDetailList(roomInfoList: List<AUIRoomInfo>): List<RoomDetailModel> {
        val list = mutableListOf<RoomDetailModel>()
        val gson = Gson()
        roomInfoList.forEach { info ->
            val json = gson.toJson(info.customPayload)
            val model = gson.fromJson(json, RoomDetailModel::class.java)
            list.add(model)
        }
        return list
    }
}