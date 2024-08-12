package io.agora.scene.eCommerce.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import io.agora.rtm.*
import io.agora.rtmsyncmanager.*
import io.agora.rtmsyncmanager.model.*
import io.agora.rtmsyncmanager.service.IAUIUserService
import io.agora.rtmsyncmanager.service.collection.AUICollectionException
import io.agora.rtmsyncmanager.service.collection.AUIListCollection
import io.agora.rtmsyncmanager.service.collection.AUIMapCollection
import io.agora.rtmsyncmanager.service.http.HttpManager
import io.agora.rtmsyncmanager.service.room.AUIRoomManager
import io.agora.rtmsyncmanager.service.rtm.AUIRtmMessageRespObserver
import io.agora.rtmsyncmanager.service.rtm.AUIRtmUserLeaveReason
import io.agora.rtmsyncmanager.utils.AUILogger
import io.agora.rtmsyncmanager.utils.GsonTools
import io.agora.scene.base.BuildConfig
import io.agora.scene.base.manager.UserManager
import io.agora.scene.base.utils.TimeUtils
import io.agora.scene.eCommerce.CommerceLogger
import io.agora.scene.eCommerce.RtcEngineInstance
import io.agora.scene.eCommerce.utils.CommerceConstants
import org.json.JSONException
import org.json.JSONObject
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
    private val TAG = "CommerceService"

    /**
     * K scene id
     */
    private val kSceneId = "scene_ecommerce_1.3.0"
    private val kCollectionIdLike = "commerce_like_collection"
    private val kCollectionIdMessage = "commerce_message_collection"
    private val kCollectionIdBid = "commerce_goods_bid_collection"
    private val kCollectionIdBuy = "commerce_goods_buy_collection"

    private val syncManager: SyncManager

    private val roomManager = AUIRoomManager()

    private val roomService: RoomService

    private var roomList = mutableListOf<RoomDetailModel>()

    object CommerceCmdKey {
        const val updateBidGoodsInfo = "updateBidGoodsInfo"
    }

    init {
        HttpManager.setBaseURL(BuildConfig.ROOM_MANAGER_SERVER_HOST)
        AUILogger.initLogger(AUILogger.Config(context, "eCommerce", logCallback = object: AUILogger.AUILogCallback {
            override fun onLogDebug(tag: String, message: String) {
                CommerceLogger.d(tag, message)
            }

            override fun onLogInfo(tag: String, message: String) {
                CommerceLogger.d(tag, message)
            }

            override fun onLogWarning(tag: String, message: String) {
                CommerceLogger.d(tag, message)
            }

            override fun onLogError(tag: String, message: String) {
                CommerceLogger.e(tag, null, message)
            }
        }))

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

        val roomExpirationPolicy = RoomExpirationPolicy()
        roomExpirationPolicy.expirationTime = ShowServiceProtocol.ROOM_AVAILABLE_DURATION
        roomService = RoomService(roomExpirationPolicy, roomManager, syncManager)
        rtmLogin {}
    }

    /**
     * Room info controller
     *
     * @property roomId
     * @property scene
     * @property roomInfo
     * @property rtmChannel
     * @property userList
     * @property goodsList
     * @property auctionModel
     * @property shopCollection
     * @property auctionCollection
     * @property messageCollection
     * @property likeCollection
     * @property likeCount
     * @property userChangeSubscriber
     * @property userJoinSubscriber
     * @property userLeaveSubscriber
     * @property auctionChangeSubscriber
     * @property shopChangeSubscriber
     * @property roomChangeSubscriber
     * @property messageChangeSubscriber
     * @property likeChangeSubscriber
     * @constructor Create empty Room info controller
     */
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
        var likeCount: Long? = null,

        var userChangeSubscriber: ((Int) -> Unit)? = null,
        var userJoinSubscriber: ((String, String, String) -> Unit)? = null,
        var userLeaveSubscriber: ((String, String, String) -> Unit)? = null,
        var auctionChangeSubscriber: ((AuctionModel) -> Unit)? = null,
        var shopChangeSubscriber: ((List<GoodsModel>) -> Unit)? = null,
        var roomChangeSubscriber: ((event: ShowRoomStatus) -> Unit)? = null,
        var messageChangeSubscriber: ((ShowMessage) -> Unit)? = null,
        var likeChangeSubscriber: (() -> Unit)? = null,
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
        CommerceLogger.d(TAG, "destroy")
        synchronized(roomInfoControllers){
            roomInfoControllers.forEach {
                cleanRoomInfoController(it)
            }
        }
        roomInfoControllers.clear()
        syncManager.logout()
        syncManager.release()
        isRtmLogin = false
        RtmClient.release()
    }

    override fun getCurrentTimestamp(roomId: String): Long {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return 0L
        return controller.scene.getCurrentTs()
    }

    override fun getCurrentRoomDuration(roomId: String): Long {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return 0L
        return controller.scene.getRoomDuration()
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
        rtmLogin {
           if (it) {
               roomService.getRoomList(BuildConfig.AGORA_APP_ID, kSceneId, System.currentTimeMillis(), 20, { room ->
                   room.roomOwner?.userId == UserManager.getInstance().user.id.toString()
               }) { e, ts, list ->
                   if (e != null) {
                       roomList = mutableListOf()
                       runOnMainThread { success.invoke(roomList) }
                   }
                   if (list != null) {
                       roomList = toDetailList(list)
                       roomList = removeReportRooms(roomList)
                       runOnMainThread { success.invoke(roomList) }
                   }
               }
           }
        }
    }

    override fun createRoom(roomId: String, roomName: String, thumbnailId: String, success: (RoomDetailModel) -> Unit, error: ((Exception) -> Unit)?) {
        rtmLogin { rtmLogin ->
            if (!rtmLogin) {
                error?.invoke(Exception("rtm login failed"))
                return@rtmLogin
            }

            val roomInfo = AUIRoomInfo()
            roomInfo.roomId = roomId
            roomInfo.roomName = roomName
            val owner = AUIUserThumbnailInfo()
            owner.userId = UserManager.getInstance().user.id.toString()
            owner.userName = UserManager.getInstance().user.name
            owner.userAvatar = UserManager.getInstance().user.headUrl
            roomInfo.roomOwner = owner
            // TODO roomInfo.thumbnail = thumbnailId
            // roomInfo.createTime = TimeUtils.currentTimeMillis()
            val roomDetailModel = RoomDetailModel(
                roomId,
                roomName,
                1,
                thumbnailId,
                UserManager.getInstance().user.id.toString(),
                UserManager.getInstance().user.headUrl,
                UserManager.getInstance().user.name,
                ShowRoomStatus.activity.value,
                System.currentTimeMillis(),
                System.currentTimeMillis()
            )
            roomInfo.customPayload = GsonTools.beanToMap(roomDetailModel)
            roomService.createRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomInfo) { e, info ->
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

            val scene = syncManager.createScene(roomInfo.roomId)
            val controller = RoomInfoController(roomInfo.roomId, scene, roomDetailModel)
            roomInfoControllers.add(controller)
            scene.userService.registerRespObserver(userObserver)
            CommerceLogger.d(TAG, "[commerce] add controller ${roomInfo.roomId}， roomInfoControllers：${roomInfoControllers}")
            actionSubscribe(controller)
            scene.bindRespDelegate(this)
        }
    }

    override fun joinRoom(roomInfo: RoomDetailModel, success: (RoomDetailModel) -> Unit, error: ((Exception) -> Unit)?) {
        rtmLogin { rtmLogin ->
            if (!rtmLogin) {
                error?.invoke(Exception("rtm login failed"))
                return@rtmLogin
            }
            if (roomInfo.ownerId.toLong() != UserManager.getInstance().user.id) {
                val scene = syncManager.createScene(roomInfo.roomId)
                val controller = RoomInfoController(roomInfo.roomId, scene, roomInfo)
                roomInfoControllers.add(controller)

                scene.userService.registerRespObserver(userObserver)

                CommerceLogger.d(TAG, "[commerce] add controller ${roomInfo.roomId}， roomInfoControllers：${roomInfoControllers}")
                actionSubscribe(controller)
                scene.bindRespDelegate(this)
                roomService.enterRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomInfo.roomId) { e ->
                    if (e == null) {
                        success.invoke(roomInfo)
                    } else {
                        error?.invoke(java.lang.Exception(e.message))
                    }
                }
            } else {
                //addOriginalData(controller)
                success.invoke(roomInfo)
            }
        }
    }

    override fun leaveRoom(roomId: String) {
        CommerceLogger.d(TAG, "leaveRoom: $roomId")
        val scene = syncManager.createScene(roomId)
        scene.unbindRespDelegate(this)
        scene.userService.unRegisterRespObserver(userObserver)

        roomService.leaveRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId)
        roomInfoControllers.forEach { c ->
            if (c.roomId == roomId) {
                cleanRoomInfoController(c)
            }
        }
        syncManager.removeScene(roomId)
        roomInfoControllers.removeAll { it.roomId == roomId }
    }

    override fun deleteRoom(roomId: String, complete: () -> Unit) {
        CommerceLogger.d(TAG, "deleteRoom: $roomId")
        syncManager.createScene(roomId).delete()
        roomManager.destroyRoom(BuildConfig.AGORA_APP_ID, kSceneId, roomId) { e ->
            complete.invoke()
        }
    }

    override fun subscribeCurrRoomEvent(roomId: String, onRoomEvent: (event: ShowRoomStatus) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.roomChangeSubscriber = onRoomEvent
    }
    private fun cleanRoomInfoController(controller: RoomInfoController) {
        CommerceLogger.d(TAG, "cleanRoomInfoController: ${controller.roomId}")
        controller.shopCollection?.release()
        controller.auctionCollection?.release()
        controller.messageCollection?.release()
        controller.likeCollection?.release()
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
        controller.shopCollection?.subscribeAttributesDidChanged { channelName, key, model ->
            val list = model.getList()
            if (list == null) {
                controller.shopChangeSubscriber?.invoke(emptyList())
                return@subscribeAttributesDidChanged
            }
            val gson = Gson()
            val goodsList: List<GoodsModel> = list.map { map ->
                gson.fromJson(gson.toJsonTree(map), GoodsModel::class.java)
            }
            CommerceLogger.d(TAG, "shopCollection subscribeAttributesDidChanged, channelName:$channelName key:$key goodsList:$goodsList")
            controller.goodsList = goodsList
            runOnMainThread { controller.shopChangeSubscriber?.invoke(goodsList) }
        }
        controller.auctionCollection = controller.scene.getCollection(kCollectionIdBid) { a, b, c ->
            AUIMapCollection(a, b, c)
        }
        controller.auctionCollection?.subscribeAttributesDidChanged { channelName, key, model ->
            val gson = Gson()
            val json = gson.toJson(model.getMap())
            val auction = gson.fromJson(json, AuctionModel::class.java)
            CommerceLogger.d(TAG, "auctionCollection subscribeAttributesDidChanged, channelName:$channelName key:$key auction:$auction")
            controller.auctionModel = auction
            runOnMainThread { controller.auctionChangeSubscriber?.invoke(auction) }
        }
        controller.messageCollection = controller.scene.getCollection(kCollectionIdMessage) { a, b, c ->
            AUIMapCollection(a, b, c)
        }
        syncManager.rtmManager.subscribeMessage(object : AUIRtmMessageRespObserver {
            override fun onMessageReceive(
                channelName: String,
                publisherId: String,
                message: String
            ) {
                try {
                    val gson = Gson()
                    val model = gson.fromJson(message, ShowMessage::class.java)
                    if (model.message == null && model.userId == null) return
                    runOnMainThread { controller.messageChangeSubscriber?.invoke(model) }
                } catch (e: Exception) {
                    CommerceLogger.d(TAG, "recv message error: ${e.message}")
                }
            }
        })
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
            try {
                val gson = Gson()
                val json = gson.toJson(model.getMap())
                val message = gson.fromJson(json, LikeModel::class.java)
                if (message.count > 0 && controller.likeCount != null) {
                    runOnMainThread { controller.likeChangeSubscriber?.invoke() }
                }
                if (message.count >= 0) {
                    controller.likeCount = message.count.toLong()
                }
            } catch (e: Exception) {
                CommerceLogger.e(TAG, e, "subscribeAttributesDidChanged: ${e.message}")
            }
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
        controller.shopCollection?.addMetaData(null, map1, null) {}
        controller.shopCollection?.addMetaData(null, map2, null) {}
        controller.shopCollection?.addMetaData(null, map3, null) {}
        val auctionModel = AuctionModel().apply {
            goods = GoodsModel(
                goodsId = "",
                title = context.getString(io.agora.scene.eCommerce.R.string.commerce_shop_auction_item_0),
                quantity = 1,
                price = 1f
            )
            status = 0L
        }
        val auctionMap = GsonTools.beanToMap(auctionModel)
        controller.auctionCollection?.addMetaData(null, auctionMap) {}

        val keys = mapOf<String, Any>("count" to 0L)
        controller.likeCollection?.addMetaData(null, keys) {}
    }

    /** User Actions */
    override fun subscribeUser(roomId: String, onChange: (count: Int) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        onChange.invoke(controller.userList.size)
        controller.userChangeSubscriber = onChange
    }

    override fun subscribeUserJoin(roomId: String, onChange: (userId: String, userName: String, userAvatar: String) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.userJoinSubscriber = onChange
    }

    override fun subscribeUserLeave(roomId: String, onChange: (userId: String, userName: String, userAvatar: String) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.userLeaveSubscriber = onChange
    }

    private val userObserver: IAUIUserService.AUIUserRespObserver = object: IAUIUserService.AUIUserRespObserver {
        override fun onRoomUserSnapshot(roomId: String, userList: List<AUIUserInfo?>?) {
            CommerceLogger.d(TAG, "onRoomUserSnapshot: $userList")
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            val list = userList?.mapNotNull { it }
            if (list != null) {
                controller.userList.clear()
                controller.userList.addAll(list)
            }
            renewRoomInfo(controller, controller.userList.size)
        }
        override fun onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
            CommerceLogger.d(TAG, "onRoomUserEnter, roomId: $roomId, userInfo: $userInfo")
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            if (controller.userList.firstOrNull { it.userId == userInfo.userId } == null) {
                controller.userList.add(userInfo)
                renewRoomInfo(controller, controller.userList.size)
                runOnMainThread { controller.userJoinSubscriber?.invoke(userInfo.userId, userInfo.userName, userInfo.userAvatar) }
            }
        }
        override fun onRoomUserLeave(roomId: String, userInfo: AUIUserInfo, reason: AUIRtmUserLeaveReason) {
            CommerceLogger.d(TAG, "onRoomUserLeave, roomId: $roomId, userInfo: $userInfo")
            val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
            controller.userList.removeAll { it.userId == userInfo.userId }
            renewRoomInfo(controller, controller.userList.size)
            runOnMainThread { controller.userLeaveSubscriber?.invoke(userInfo.userId, userInfo.userName, userInfo.userAvatar) }
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

    private fun renewRoomInfo(controller: RoomInfoController, count: Int) {
        if (controller.roomInfo.ownerId == UserManager.getInstance().user.id.toString()) {
            val roomInfo = AUIRoomInfo()
            roomInfo.roomId = controller.roomId
            roomInfo.roomName = controller.roomInfo.roomName
            val owner = AUIUserThumbnailInfo()
            owner.userId = controller.roomInfo.ownerId
            owner.userName = controller.roomInfo.ownerName
            owner.userAvatar = controller.roomInfo.ownerAvatar
            roomInfo.roomOwner = owner
            // TODO roomInfo.thumbnail = controller.roomInfo.thumbnailId
            roomInfo.createTime = controller.roomInfo.createdAt
            val roomDetailModel = RoomDetailModel(
                controller.roomInfo.roomId,
                controller.roomInfo.roomName,
                count,
                controller.roomInfo.thumbnailId,
                controller.roomInfo.ownerId,
                controller.roomInfo.ownerAvatar,
                controller.roomInfo.ownerName,
                controller.roomInfo.roomStatus,
                controller.roomInfo.createdAt,
                controller.roomInfo.updatedAt
            )
            roomInfo.customPayload = GsonTools.beanToMap(roomDetailModel)
            roomManager.updateRoomInfo(
                BuildConfig.AGORA_APP_ID, kSceneId, roomInfo
            ) { e, _ ->
                CommerceLogger.d(TAG, "updateRoomInfo")
            }
        }
        runOnMainThread { controller.userChangeSubscriber?.invoke(controller.userList.size) }
    }

    /** Message Actions */
    override fun sendChatMessage(roomId: String, message: String, success: (() -> Unit)?, error: ((Exception) -> Unit)?) {
        val jsonObject = JSONObject()
        jsonObject.put("userId", UserManager.getInstance().user.id.toString())
        jsonObject.put("userName", UserManager.getInstance().user.name)
        jsonObject.put("message", message)
        jsonObject.put("createAt", System.currentTimeMillis().toDouble())
        syncManager.rtmManager.sendMessage(roomId, jsonObject.toString(), success, error)
    }

    override fun subscribeMessage(roomId: String, onMessageChange: (ShowMessage) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.messageChangeSubscriber = onMessageChange
    }

    /** Like Actions */
    override fun likeSend(roomId: String, success: (() -> Unit)?, error: ((Exception) -> Unit)?) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val keys = listOf("count")
        controller.likeCollection?.calculateMetaData(null, keys, 1, 0, Int.MAX_VALUE) {
        }
    }
    override fun likeSubscribe(roomId: String, onMessageChange: () -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.likeChangeSubscriber = onMessageChange
    }

    /** Auction Actions */
    override fun auctionSubscribe(roomId: String, onChange: (AuctionModel) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        controller.auctionCollection?.subscribeWillUpdate { _, _, newValue, oldValue ->
            val newBid = newValue["bid"] as? Long ?: return@subscribeWillUpdate null
            val oldBid = oldValue["bid"] as? Long ?: return@subscribeWillUpdate null
            CommerceLogger.d(TAG, "auctionCollection subscribeWillUpdate newBid:$newBid, oldBid:$oldBid")

            val endTime = oldValue["endTimestamp"] as? Long ?: return@subscribeWillUpdate null
            val nowTime = controller.scene.getCurrentTs()
            CommerceLogger.d(TAG, "endTimestamp is:$endTime, nowTime is: $nowTime")
            if (nowTime > endTime) {
                return@subscribeWillUpdate AUICollectionException.ErrorCode.unknown.toException(200001, "Time out!")
            }

            if (newBid in 2..oldBid) {
                CommerceLogger.d(TAG, "Unable to proceed with the auction. The bid prices are identical")
                return@subscribeWillUpdate AUICollectionException.ErrorCode.unknown.toException(200002, "Unable to proceed with the auction. The bid prices are identical")
            }
            return@subscribeWillUpdate null
        }
        val model = controller.auctionModel
        if (model != null) {
            onChange.invoke(model)
        }
        controller.auctionChangeSubscriber = onChange
    }

    override fun auctionStart(roomId: String, onComplete: (AUICollectionException?) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val startTs = controller.scene.getCurrentTs()
        val endTs = startTs + 30 * 1000
        val auctionModel = AuctionModel().apply {
            bidUser = ShowUser("", "", "")
            startTimestamp = startTs
            bid = 1L
            status = 1L
            endTimestamp = endTs
        }
        val map = GsonTools.beanToMap(auctionModel)
        controller.auctionCollection?.addMetaData(null, map) { e ->
            CommerceLogger.d(TAG, "startAuction e: $e")
            onComplete.invoke(e)
        }
    }

    override fun auctionBidding(roomId: String, value: Long, onComplete: (AUICollectionException?) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val user = ShowUser(
            UserManager.getInstance().user.id.toString(),
            UserManager.getInstance().user.name,
            UserManager.getInstance().user.headUrl
        )
        val userMap = GsonTools.beanToMap(user)
        val map = mapOf(
            "bidUser" to userMap,
            "bid" to value
        )
        controller.auctionCollection?.updateMetaData(CommerceCmdKey.updateBidGoodsInfo, map) {
            Log.d("auction", "updateMetaData return:$it")
            onComplete.invoke(it)
        }
    }

    override fun auctionComplete(roomId: String, onComplete: (AUICollectionException?) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val map = mapOf(
            "status" to 2L
        )
        controller.auctionCollection?.updateMetaData(CommerceCmdKey.updateBidGoodsInfo, map) {
            Log.d("auction", "updateMetaData return:$it")
            onComplete.invoke(it)
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

    override fun shopBuyItem(roomId: String, itemId: String, onComplete: (AUICollectionException?) -> Unit) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val keys = listOf("quantity")
        val filter = listOf(mapOf<String, Any>("goodsId" to itemId))
        controller.shopCollection?.calculateMetaData(null, keys, -1, 0, Int.MAX_VALUE, filter) { err ->
            if (err != null) {
                runOnMainThread { onComplete.invoke(err) }
            } else {
                runOnMainThread { onComplete.invoke(null) }
            }
        }
    }

    override fun shopUpdateItem(
        roomId: String,
        itemId: String,
        count: Long,
        onComplete: (AUICollectionException?) -> Unit
    ) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == roomId } ?: return
        val map = mapOf("quantity" to count)
        val filter = listOf(mapOf<String, Any>("goodsId" to itemId))
        controller.shopCollection?.updateMetaData(null, map, filter) {
            onComplete.invoke(it)
        }
    }

    private var isRtmLogin = false
    private fun rtmLogin(complete: (Boolean) -> Unit) {
        if (isRtmLogin) {
            complete.invoke(true)
            return
        }
        CommerceLogger.d("ShowSyncManagerServiceImpl", "1.rtmLogin request start -> rtm login")
        syncManager.login(RtcEngineInstance.generalRtmToken()) { error ->
            if (error == null) {
                isRtmLogin = true
                complete.invoke(true)
            } else {
                complete.invoke(false)
            }
        }
    }

    override fun onSceneDestroy(channelName: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == channelName } ?: return
        controller.roomChangeSubscriber?.invoke(ShowRoomStatus.end)
        leaveRoom(channelName)
    }

    override fun onSceneExpire(channelName: String) {
        val controller = roomInfoControllers.firstOrNull { it.roomId == channelName } ?: return
        controller.roomChangeSubscriber?.invoke(ShowRoomStatus.Expire)
        leaveRoom(channelName)
    }

    override fun onWillInitSceneMetadata(channelName: String): Map<String, Any>? {
        //val controller = roomInfoControllers.firstOrNull { it.roomId == channelName } ?: return null
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
        val auctionModel = AuctionModel().apply {
            goods = GoodsModel(
                goodsId = "",
                title = context.getString(io.agora.scene.eCommerce.R.string.commerce_shop_auction_item_0),
                quantity = 1,
                price = 1f
            )
            status = 0L
        }
        val auctionMap = GsonTools.beanToMap(auctionModel)
        val keys = mapOf<String, Any>("count" to 0L)
        return mapOf(
            kCollectionIdBuy to listOf(map1, map2, map3),
            kCollectionIdBid to auctionMap,
            kCollectionIdLike to keys
        )
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

    private fun toDetailList(roomInfoList: List<AUIRoomInfo>): MutableList<RoomDetailModel> {
        val list = mutableListOf<RoomDetailModel>()
        val gson = Gson()
        roomInfoList.forEach { info ->
            val json = gson.toJson(info.customPayload)
            val model = gson.fromJson(json, RoomDetailModel::class.java)
            list.add(model)
        }
        return list
    }

    private fun removeReportRooms(roomList: List<RoomDetailModel>): MutableList<RoomDetailModel> {
        val retRoomList = mutableListOf<RoomDetailModel>()
        retRoomList.addAll(roomList)

        val reportRoomList = roomList.filter {
            (CommerceConstants.reportContents.contains(it.roomName) || CommerceConstants.reportUsers.contains(it.ownerId))
        }
        if (reportRoomList.isNotEmpty()) {
            reportRoomList.forEach {
                retRoomList.remove(it)
            }
        }
        return retRoomList
    }
}