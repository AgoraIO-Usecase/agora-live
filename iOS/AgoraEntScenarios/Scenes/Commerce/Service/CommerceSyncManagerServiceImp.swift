//
//  ShowSyncManagerServiceImp.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/3.
//

import Foundation
import UIKit
import RTMSyncManager

let kEcommerceSceneId = "scene_ecommerce_0_2_0"

private let SYNC_MANAGER_MESSAGE_COLLECTION = "commerce_message_collection"
private let SYNC_MANAGER_SEAT_APPLY_COLLECTION = "commerce_seat_apply_collection"
private let SYNC_MANAGER_SEAT_INVITATION_COLLECTION = "commerce_seat_invitation_collection"
private let SYNC_MANAGER_PK_INVITATION_COLLECTION = "commerce_pk_invitation_collection"
private let SYNC_MANAGER_INTERACTION_COLLECTION = "commerce_interaction_collection"
private let SYNC_MANAGER_BID_GOODS_COLLECTION = "commerce_goods_bid_collection"
private let SYNC_MANAGER_BUY_GOODS_COLLECTION = "commerce_goods_buy_collection"
private let SYNC_MANAGER_UPVOTE_COLLECTION = "commerce_like_collection"


private struct CommerceCmdKey {
    static let updateBidGoodsInfo: String = "updateBidGoodsInfo"
}

enum CommerceError: Int, Error {
    case unknown = 1                   //unknown error
    case networkError                  //network fail
    
    func desc() -> String {
        switch self {
        case .networkError:
            return "network fail"
        default:
            return "unknown error"
        }
    }
    
    func toNSError() -> NSError {
        return NSError(domain: "Show Service Error", code: rawValue, userInfo: [ NSLocalizedDescriptionKey : self.desc()])
    }
}

private func agoraAssert(_ message: String) {
    agoraAssert(false, message)
}

private func agoraAssert(_ condition: Bool, _ message: String) {
    #if DEBUG
//    assert(condition, message)
    #else
    #endif
    if condition {
        return
    }
    
    commerceErrorLog(message, tag: "Service")
}

func commercePrintLog(_ message: String, tag: String? = "UI") {
    commerceLogger.info(message, context: tag)
}

func commerceWarnLog(_ message: String, tag: String? = "UI") {
    commerceLogger.warning(message, context: tag)
}

func commerceErrorLog(_ message: String, tag: String? = "UI") {
    commerceLogger.error(message, context: tag)
}

private func agoraPrint(_ message: String) {
    commercePrintLog(message, tag: "Service")
}

class CommerceSyncManagerServiceImp: NSObject, CommerceServiceProtocol {
    private let uniqueId: String = NSString.withUUID().md5()!
    fileprivate var roomList: [CommerceRoomListModel]? {
        set {
            AppContext.shared.commerceRoomList = newValue
        }
        get {
            return AppContext.shared.commerceRoomList
        }
    }
    fileprivate var room: CommerceRoomListModel? {
        return self.roomList?.filter({ $0.roomId == roomId}).first
    }
    private var userList: [CommerceUser]?
    private weak var subscribeDelegate: CommerceSubscribeServiceProtocol?
    
    private var userMuteLocalAudio: Bool = false
    
    private var isAdded = false
    
    private var isJoined = false
    
    private var joinRetry = 0
    
    fileprivate var roomId: String?
    
    deinit {
        agoraPrint("deinit-- ShowSyncManagerServiceImp")
        SyncUtilsWrapper.cleanScene(uniqueId: uniqueId)
    }
    
    // MARK: Private
    private func getRoomId() -> String {
        guard let _roomId = roomId else {
            return ""
        }
        
        return _roomId
    }
    
    fileprivate func isOwner(_ room: CommerceRoomListModel?) -> Bool {
        return room?.ownerId == VLUserCenter.user.id
    }
    
    private func cleanCache() {
        userList = [CommerceUser]()
        userMuteLocalAudio = false
    }
    
    fileprivate func _checkRoomExpire() {
        guard let room = self.room else { return }
        
        let expiredDuration = 20 * 60 * 1000
        guard RTMSyncUtil.getRoomDuration(roomId: room.roomId) > expiredDuration else { return }
        
        subscribeDelegate?.onRoomExpired()
    }
    
    fileprivate func _startCheckExpire() {
        Timer.scheduledTimer(withTimeInterval: 5, repeats: true) { [weak self] timer in
            guard let self = self else { return }
            
            self._checkRoomExpire()
            if self.roomId == nil {
                timer.invalidate()
            }
        }
        
        DispatchQueue.main.async {
            self._checkRoomExpire()
        }
    }
    
    //MARK: ShowServiceProtocol
    func getRoomList(page: Int, completion: @escaping (NSError?, [CommerceRoomListModel]?) -> Void) {
        _getRoomList(page: page) { [weak self] error, list in
            guard let self = self else {
                completion(error, nil)
                return
            }
            if let error = error {
                completion(error, nil)
                return
            }
            self.roomList = list
            completion(nil, list)
        }
    }
    
    @objc func createRoom(roomName: String,
                          roomId: String,
                          thumbnailId: String,
                          completion: @escaping (NSError?, CommerceRoomDetailModel?) -> Void) {
        let room = CommerceRoomListModel()
        room.roomName = roomName
        room.roomId = roomId
        room.thumbnailId = thumbnailId
        room.ownerId = VLUserCenter.user.id
        room.ownerName = VLUserCenter.user.name
        room.ownerAvatar = VLUserCenter.user.headUrl
        room.createdAt = Date().millionsecondSince1970()
        let params = (room.yy_modelToJSONObject() as? [String: Any]) ?? [:]
        
        let roomModel = CommerceRoomDetailModel()
        let date = Date()
        RTMSyncUtil.createRoom(roomName: roomName, roomId: room.roomId, payload: params) { error, roomInfo in
            commercePrintLog("[Timing][\(roomId)] restful createRoom cost: \(Int(-date.timeIntervalSinceNow * 1000)) ms")
            if let err = error {
                completion(error, nil)
                return
            }
            RTMSyncUtil.addMetaData(id: room.roomId, key: SYNC_MANAGER_UPVOTE_COLLECTION,
                                    data: ["userId": VLUserCenter.user.id, "count":  0, "createAt": Date().millionsecondSince1970()]) {[weak self] err in
                guard let self = self else {return}
                commercePrintLog("[Timing][\(room.roomId)] rtm addMetaData cost: \(Int(-date.timeIntervalSinceNow * 1000)) ms")
                if let err = err {
                    if let roomInfo = roomInfo {
                        RTMSyncUtil.leaveScene(room: roomInfo)
                    }
                    completion(err, nil)
                    return
                }
                guard let roomInfo = roomInfo else {
                    completion(err, nil)
                    return
                }
                
                self.roomId = roomId
                roomModel.ownerAvatar = room.ownerAvatar
                roomModel.ownerId = room.ownerId
                roomModel.ownerName = room.ownerName
                roomModel.roomId = room.roomId
                roomModel.roomName = room.roomName
                roomModel.thumbnailId = thumbnailId
                roomModel.roomStatus = .end
                roomModel.createdAt = roomInfo.createTime
                
                self.roomList?.append(roomModel)
                self._startCheckExpire()
                self._subscribeAll()
                self.isJoined = true
                completion(nil, roomModel)
            }
        }
        
        let scene = RTMSyncUtil.scene(id: room.roomId)
        scene?.bindRespDelegate(delegate: self)
    }
    
    @objc func joinRoom(room: CommerceRoomListModel,
                        completion: @escaping (NSError?, CommerceRoomDetailModel?) -> Void) {
        isJoined = false
        self.roomId = room.roomId
        
        RTMSyncUtil.joinScene(roomId: room.roomId) { [weak self] error, roomInfo in
            guard let self = self else {return}
            let roomModel = CommerceRoomDetailModel()
            roomModel.ownerAvatar = room.ownerAvatar
            roomModel.ownerId = room.ownerId
            roomModel.ownerName = room.ownerName
            roomModel.roomId = room.roomId
            roomModel.roomName = room.roomName
            roomModel.roomUserCount = room.roomUserCount
            roomModel.thumbnailId = room.thumbnailId
            roomModel.roomStatus = .end
            roomModel.createdAt = room.createdAt
            self.roomList?.append(roomModel)
            self._startCheckExpire()
            self._subscribeAll()
            self.isJoined = true
            completion(nil, roomModel)
            self.initRoom(roomId: self.roomId) { _ in }
        }
        
        let scene = RTMSyncUtil.scene(id: room.roomId)
        scene?.bindRespDelegate(delegate: self)
    }
    
    func leaveRoom(completion: @escaping (NSError?) -> Void) {
        isJoined = false
        defer {
            cleanCache()
        }
        guard let roomInfo = roomList?.filter({ $0.roomId == self.getRoomId() }).first else {
            //            agoraAssert("leaveRoom channelName = nil")
            completion(nil)
            return
        }
        
        
        let scene = RTMSyncUtil.scene(id: roomInfo.roomId)
        scene?.unbindRespDelegate(delegate: self)
        
        deinitRoom(roomId: roomId) { _ in }
        
        _leaveRoom(completion: completion)
    }
    
    private func initRoom(roomId: String?, completion: @escaping (NSError?) -> Void) {
        if isJoined {
            _sendMessageWithText(roomId: roomId, text: "join_live_room".commerce_localized)
        }
    }
    
    private func deinitRoom(roomId: String?, completion: @escaping (NSError?) -> Void) {
        _sendMessageWithText(roomId: roomId, text: "leave_live_room".commerce_localized)
    }
    
    func getRoomDuration(roomId: String) -> UInt64 {
        return RTMSyncUtil.getRoomDuration(roomId: roomId)
    }
    
    func getAllUserList(completion: @escaping (NSError?, [CommerceUser]?) -> Void) {
        _getUserList(roomId: roomId, finished: completion)
    }
    
    func sendChatMessage(roomId: String?, message: CommerceMessage, completion: ((NSError?) -> Void)?) {
        _addMessage(roomId: roomId, message: message, finished: completion)
    }
    
    private func _sendMessageWithText(roomId: String?, text: String) {
        let showMsg = CommerceMessage()
        showMsg.userId = VLUserCenter.user.id
        showMsg.userName = VLUserCenter.user.name
        showMsg.message = text
        showMsg.createAt = Date().millionsecondSince1970()
        sendChatMessage(roomId: roomId, message: showMsg) { error in }
    }
    
    func getBidGoodsInfo(roomId: String?, completion: @escaping (NSError?, CommerceGoodsAuctionModel?) -> Void) {
        _getBidGoodsInfo(roomId: roomId, completion: completion)
    }
    
    func addBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel, completion: @escaping (NSError?) -> Void) {
        _addBidGoodsInfo(roomId: roomId, goods: goods, completion: completion)
    }
    
    func endBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel, completion: @escaping (NSError?) -> Void) {
        _endBidGoodsInfo(roomId: roomId, goods: goods, completion: completion)
    }
    
    func updateBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel, completion: @escaping (NSError?) -> Void) {
        _updateBidGoodsInfo(roomId: roomId, goods: goods, completion: completion)
    }
    
    func subscribeBidGoodsInfo(roomId: String?, completion: @escaping (NSError?, CommerceGoodsAuctionModel?) -> Void) {
        _subscribeBidGoodsInfo(roomId: roomId, completion: completion)
    }
    
    func addGoodsList(roomId: String?, goods: [CommerceGoodsModel]?, completion: @escaping (NSError?) -> Void) {
        _addGoodsList(roomId: roomId, goods: goods, completion: completion)
    }
    
    func getGoodsList(roomId: String?, completion: @escaping (NSError?, [CommerceGoodsBuyModel]?) -> Void) {
        _getGoodsList(roomId: roomId, completion: completion)
    }
    
    func updateGoodsInfo(roomId: String?, goods: CommerceGoodsModel?, completion: @escaping (NSError?) -> Void) {
        _updateGoodsInfo(roomId: roomId, goods: goods, completion: completion)
    }
    
    func calcGoodsInfo(roomId: String?, goods: CommerceGoodsModel?, increase: Bool, completion: @escaping (NSError?) -> Void) {
        _calcGoodsInfo(roomId: roomId, goods: goods, increase: increase, completion: completion)
    }
    
    func subscribeGoodsInfo(roomId: String?, completion: @escaping (NSError?, [CommerceGoodsModel]?) -> Void) {
        _subscribeGoodsInfo(roomId: roomId, completion: completion)
    }
    
    func getGoodsInfo(roomId: String?, goodsId: String?, completion: @escaping (NSError?, CommerceGoodsBuyModel?) -> Void) {
        _getGoodsInfo(roomId: roomId, goodsId: goodsId, completion: completion)
    }
    
    func unsubscribeEvent(delegate: CommerceSubscribeServiceProtocol) {
        //TODO: weak map
        self.subscribeDelegate = nil
    }
    
    func subscribeEvent(delegate: CommerceSubscribeServiceProtocol) {
        //TODO: weak map
        self.subscribeDelegate = delegate
    }
}

//MARK: room operation
extension CommerceSyncManagerServiceImp {
    @objc func _getRoomList(page: Int, completion: @escaping (NSError?, [CommerceRoomListModel]?) -> Void) {
        RTMSyncUtil.getRoomList { error, roomList in
            if error != nil {
                completion(error, nil)
                return
            }
            let dataArray = roomList?.map({ info in
                let roomModel = CommerceRoomDetailModel()
                roomModel.ownerAvatar = info.owner?.userAvatar
                roomModel.ownerId = info.owner?.userId ?? ""
                roomModel.ownerName = info.owner?.userName
                roomModel.roomId = info.roomId
                roomModel.roomName = info.roomName
                roomModel.thumbnailId = info.customPayload["thumbnailId"] as? String
                roomModel.createdAt = info.createTime
                return roomModel
            })
            completion(nil, dataArray)
        }
    }
    
    private func _leaveRoom(completion: @escaping (NSError?) -> Void) {
        defer {
            _unsubscribeAll()
            roomId = nil
            completion(nil)
        }
        
        guard let channelName = roomId else {
            agoraAssert("channelName = nil")
            return
        }
        
        _leaveScene(roomId: channelName)
    }
    
    fileprivate func _subscribeAll() {
        agoraPrint("imp[\(roomId ?? "")] all subscribe...")
        _subscribeOnlineUsersChanged()
        _getUserList(roomId: room?.roomId) { _, list in
            
        }
        RTMSyncUtil.subscribeMessage(channelName: "", delegate: self)
    }
    
    private func _unsubscribeAll() {
        agoraPrint("imp[\(roomId ?? "")] all unsubscribe...")
        RTMSyncUtil.unsubscribeMessage(channelName: "", delegate: self)
    }
    
    private func _leaveScene(roomId: String) {
        agoraPrint("_leaveScene: \(roomId)")
        RTMSyncUtil.leaveScene(roomId: roomId)
    }
}

//MARK: Bid Goods operation
extension CommerceSyncManagerServiceImp {
    private func _getBidGoodsInfo(roomId: String?, completion: @escaping (NSError?, CommerceGoodsAuctionModel?) -> Void) {
        guard let channelName = roomId else {
            completion(NSError(domain: "roomId is empty", code: 0), nil)
            return
        }
        RTMSyncUtil.getMetaData(id: channelName, key: SYNC_MANAGER_BID_GOODS_COLLECTION) { error, res in
            agoraPrint("imp bid goods get success...")
            if error != nil {
                completion(error, nil)
                return
            }
            guard let res = res else { return }
            let auctionModel = CommerceGoodsAuctionModel.yy_model(withJSON: res)
            completion(nil, auctionModel)
        }
    }
    
    private func _addBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel?, completion: @escaping (NSError?) -> Void) {
        guard let channelName = roomId,
              let params = goods?.yy_modelToJSONObject() as? [String: Any] else {
            completion(NSError(domain: "_addBidGoodsInfo fail: roomId is nil or params is nil", code: 0))
            return
        }
        RTMSyncUtil.addMetaData(id: channelName, key: SYNC_MANAGER_BID_GOODS_COLLECTION, data: params, callback: completion)
    }
    
    private func _endBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel?, completion: @escaping (NSError?) -> Void) {
        guard let channelName = roomId, let status = goods?.status else {
            completion(NSError(domain: "_endBidGoodsInfo fail: roomId is nil or status == nil", code: 0))
            return
        }
        //结束只改状态，防止房主非仲裁者时，价格不是最终的导致回滚
        RTMSyncUtil.updateMetaData(id: channelName, 
                                   key: SYNC_MANAGER_BID_GOODS_COLLECTION,
                                   valueCmd: CommerceCmdKey.updateBidGoodsInfo,
                                   data: ["status": status.rawValue], 
                                   callback: completion)
    }
    
    private func _updateBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel?, completion: @escaping (NSError?) -> Void) {
        guard let channelName = roomId,
              var params = goods?.yy_modelToJSONObject() as? [String: Any]  else {
            completion(NSError(domain: "roomId is nil or params is nil", code: 0))
            return
        }
        //加价时不可修改状态
        params.removeValue(forKey: "status")
        RTMSyncUtil.updateMetaData(id: channelName, 
                                   key: SYNC_MANAGER_BID_GOODS_COLLECTION,
                                   valueCmd: CommerceCmdKey.updateBidGoodsInfo, 
                                   data: params,
                                   callback: completion)
    }
    
    private func _subscribeBidGoodsInfo(roomId: String?, completion: @escaping (NSError?, CommerceGoodsAuctionModel?) -> Void) {
        guard let channelName = roomId else {
            completion(NSError(domain: "roomId is empty", code: 0), nil)
            return
        }
        
        let collection = RTMSyncUtil.collection(id: channelName, key: SYNC_MANAGER_BID_GOODS_COLLECTION)
        collection?.subscribeWillUpdate(callback: { uid, valueCmd, newItem, oldItem in
            if let newBid = newItem["bid"] as? Int,
               let oldBid = oldItem["bid"] as? Int,
               oldBid >= newBid {
                return NSError(domain: "Unable to proceed with the auction. The bid prices are identical", code: -1)
            }
            
            return nil
        })
        
        RTMSyncUtil.subscribeAttributesDidChanged(id: channelName, key: SYNC_MANAGER_BID_GOODS_COLLECTION) { channelName, object in
            guard let res = object.getMap() else { return }
            let auctionModel = CommerceGoodsAuctionModel.yy_model(with: res)
            completion(nil, auctionModel)
        }
    }
}

//MARK: Goods operation
extension CommerceSyncManagerServiceImp {
    private func _addGoodsList(roomId: String?, goods: [CommerceGoodsModel]?, completion: @escaping (NSError?) -> Void) {
        guard let channelName = roomId else {
            completion(NSError(domain: "roomId is empty", code: 0))
            return
        }
        let jsonArray = goods?.compactMap({ $0.yy_modelToJSONObject() as? [String: Any] })
        RTMSyncUtil.addMetaData(id: channelName, key: SYNC_MANAGER_BUY_GOODS_COLLECTION, data: jsonArray) { _ in }
    }
    
    private func _getGoodsList(roomId: String?, completion: @escaping (NSError?, [CommerceGoodsBuyModel]?) -> Void) {
        guard let channelName = roomId else {
            completion(NSError(domain: "roomId is empty", code: 0), nil)
            return
        }
        RTMSyncUtil.getListMetaData(id: channelName, key: SYNC_MANAGER_BUY_GOODS_COLLECTION) { error, res in
            if error != nil {
                completion(error, nil)
                return
            }
            agoraPrint("imp goods get success...")
            guard let res = res as? [[String: Any]]? else { return }
            let listModel = res?.compactMap({ item in
                let model = CommerceGoodsBuyModel()
                let goodsModel = CommerceGoodsModel.yy_model(withJSON: item)
                model.goods = goodsModel
                return model
            })
            completion(nil, listModel)
        }
    }
    
    private func _updateGoodsInfo(roomId: String?, goods: CommerceGoodsModel?, completion: @escaping (NSError?) -> Void) {
        guard let channelName = roomId, let params = goods?.yy_modelToJSONObject() as? [String: Any] else {
            completion(NSError(domain: "roomId is empty", code: 0))
            return
        }
        agoraPrint("_updateGoodsInfo[\(roomId ?? "")] \(goods?.title ?? "")")
        RTMSyncUtil.updateListMetaData(id: channelName,
                                       key: SYNC_MANAGER_BUY_GOODS_COLLECTION,
                                       data: params,
                                       filter: [["goodsId": goods?.goodsId ?? ""]],
                                       callback: completion)
    }
    
    private func _calcGoodsInfo(roomId: String?, goods: CommerceGoodsModel?, increase: Bool, completion: @escaping (NSError?) -> Void) {
        guard let channelName = roomId else {
            completion(NSError(domain: "roomId is empty", code: 0))
            return
        }
        agoraPrint("_calcGoodsInfo[\(roomId ?? "")] \(goods?.title ?? "")")
        let collecton = RTMSyncUtil.listCollection(id: channelName, key: SYNC_MANAGER_BUY_GOODS_COLLECTION)
        collecton?.calculateMetaData(valueCmd: nil,
                                     key: ["quantity"],
                                     value: increase ? 1 : -1,
                                     min: 0,
                                     max: Int(Int32.max),
                                     filter: [["goodsId": goods?.goodsId ?? ""]],
                                     callback: { err in
            guard let err = err else {
                completion(nil)
                return
            }
            var title = err.localizedDescription
            if err.code == AUICollectionOperationError.calculateMapOutOfRange.rawValue {
                title = "Sold Out!"
            }
            let error = NSError(domain: "Service Error",
                                code: err.code,
                                userInfo: [ NSLocalizedDescriptionKey : title])
            completion(error)
        })
    }
    
    private func _subscribeGoodsInfo(roomId: String?, completion: @escaping (NSError?, [CommerceGoodsModel]?) -> Void) {
        guard let channelName = roomId else {
            completion(NSError(domain: "roomId is empty", code: 0), nil)
            return
        }
        RTMSyncUtil.subscribeListAttributesDidChanged(id: channelName, key: SYNC_MANAGER_BUY_GOODS_COLLECTION) { channelName, object in
            guard let res = object.getList() else { return }
            let models = res.compactMap({ CommerceGoodsModel.yy_model(with: $0) })
            completion(nil, models)
        }
    }
    
    private func _getGoodsInfo(roomId: String?, goodsId: String?, completion: @escaping (NSError?, CommerceGoodsBuyModel?) -> Void) {
        _getGoodsList(roomId: roomId) { error, list in
            let goods = list?.filter({ $0.goods?.goodsId == goodsId }).first
            completion(error, goods)
        }
    }
}

extension CommerceSyncManagerServiceImp {
    func upvote(roomId: String?, count: Int, completion: ((NSError?) -> Void)?) {
        guard let channelName = roomId else {
            completion?(NSError(domain: "roomId is empty", code: 0))
            return
        }
        let collecton = RTMSyncUtil.collection(id: channelName, key: SYNC_MANAGER_UPVOTE_COLLECTION)
        collecton?.calculateMetaData(valueCmd: nil,
                                     key: ["count"],
                                     value: 1,
                                     min: 0,
                                     max: Int(Int32.max),
                                     callback: completion)
    }
    
    func subscribeUpvoteEvent(roomId: String?, completion: ((String?, Int) -> Void)?) {
        guard let channelName = roomId else {
            completion?(nil, 0)
            return
        }
        RTMSyncUtil.subscribeAttributesDidChanged(id: channelName, key: SYNC_MANAGER_UPVOTE_COLLECTION) { channelName, object in
            let userId = object.getMap()?["userId"] as? String
            let count = object.getMap()?["count"] as? Int
            completion?(userId, count ?? 1)
        }
    }
}

//MARK: user operation
extension CommerceSyncManagerServiceImp {
    private func _getUserList(roomId: String?, finished: @escaping (NSError?, [CommerceUser]?) -> Void) {
        guard let channelName = roomId else {
            finished(NSError(domain: "roomId is empty", code: 0), nil)
            return
        }
        agoraPrint("_getUserList...")
        RTMSyncUtil.getUserList(id: channelName) { roomId, userList in
            agoraPrint("imp user get success...")
            agoraPrint("_getUserList count: \(userList.count)")
            let users = userList.compactMap({ item in
                let user = CommerceUser()
                user.userId = item.userId
                user.avatar = item.userAvatar
                user.userName = item.userName
                return user
            })
            if let userList = self.userList, !userList.isEmpty {
                self.userList = combineAndDistinctObjects(userList, users)
            } else {
                self.userList = users
            }
            self._updateUserCount()
            finished(nil, [])
        }
        
        func combineAndDistinctObjects(_ array1: [CommerceUser],
                                       _ array2: [CommerceUser]) -> [CommerceUser] {
            var result = array1
            for object in array2 {
                if !result.contains(where: { $0.userId == object.userId }) {
                    result.append(object)
                }
            }
            return result
        }
    }

    private func _subscribeOnlineUsersChanged() {
        guard let channelName = roomId else {
            agoraPrint("channelName = nil")
            return
        }
        agoraPrint("imp user subscribe ...")
        let scene = RTMSyncUtil.scene(id: channelName)
        scene?.userService.bindRespDelegate(delegate: self)
    }
    
    private func _updateUserCount() {
        self.subscribeDelegate?.onUserCountChanged(userCount: self.userList?.count ?? 1)
        guard let channelName = roomId,
              let roomInfo = roomList?.filter({ $0.roomId == self.getRoomId() }).first,
              roomInfo.ownerId == VLUserCenter.user.id
        else {
//            agoraPrint("updateUserCount channelName = nil")
//            userListCountDidChanged?(UInt(count))
            return
        }
        let roomUserCount = userList?.count ?? 1
        if roomUserCount == roomInfo.roomUserCount {
            return
        }
        roomInfo.updatedAt = Int64(Date().timeIntervalSince1970 * 1000)
        roomInfo.roomUserCount = roomUserCount
        roomInfo.objectId = channelName
        
        let params = (roomInfo.yy_modelToJSONObject() as? [String: Any]) ?? [:]
        
        let ownerInfo = AUIUserThumbnailInfo()
        ownerInfo.userId = roomInfo.ownerId
        ownerInfo.userAvatar = roomInfo.ownerAvatar ?? ""
        ownerInfo.userName = roomInfo.ownerName ?? ""
        RTMSyncUtil.updateRoomInfo(roomName: roomInfo.roomName ?? "",
                                   roomId: roomInfo.roomId,
                                   payload: params,
                                   ownerInfo: ownerInfo)
    }
}


//MARK: message operation
extension CommerceSyncManagerServiceImp {
    private func _addMessage(roomId: String?, message: CommerceMessage, finished: ((NSError?) -> Void)?) {
        guard let channelName = roomId else { return }
        agoraPrint("imp message add ...")
        let params = message.yy_modelToJSONObject() as! [String: Any]
        RTMSyncUtil.sendMessage(channelName: channelName, data: params)
        subscribeDelegate?.onMessageDidAdded(message: message)
        finished?(nil)
    }
}

extension CommerceSyncManagerServiceImp: AUIRtmMessageProxyDelegate {
    func onMessageReceive(publisher: String, channelName: String, message: String) {
        guard channelName == roomId, let model = CommerceMessage.yy_model(withJSON: message) else { return }
        agoraPrint("imp onMessageReceive... [\(message)] \(channelName)")
        self.subscribeDelegate?.onMessageDidAdded(message: model)
    }
}

extension CommerceSyncManagerServiceImp: AUISceneRespDelegate {
    func onSceneExpire(channelName: String) {
        RTMSyncUtil.leaveScene(roomId: channelName)
        subscribeDelegate?.onRoomDestroy(roomId: channelName)
    }
    
    func onSceneDestroy(channelName: String){
        RTMSyncUtil.leaveScene(roomId: channelName)
        subscribeDelegate?.onRoomDestroy(roomId: channelName)
    }
    
    func onSceneUserBeKicked(roomId: String,userId: String) {
        
    }
}

extension CommerceSyncManagerServiceImp: AUIUserRespDelegate {
    func onRoomUserSnapshot(roomId: String, userList: [AUIUserInfo]) {
        let users = userList.compactMap({ item in
            let user = CommerceUser()
            user.userId = item.userId
            user.avatar = item.userAvatar
            user.userName = item.userName
            return user
        })
        self.userList = users
        _updateUserCount()
    }
    
    func onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
        agoraPrint("userEnter == \(roomId)  object == \(userInfo)")
        _userEnter(userInfo: userInfo)
    }
    
    func onRoomUserLeave(roomId: String, userInfo: AUIUserInfo) {
        agoraPrint("userLeave == \(roomId)  object == \(userInfo)")
        _userLeave(userId: userInfo.userId)
    }
    
    func onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
        agoraPrint("userUpdate == \(roomId)  object == \(userInfo)")
        _userEnter(userInfo: userInfo)
    }
    
    func onUserAudioMute(userId: String, mute: Bool) {
        
    }
    
    func onUserVideoMute(userId: String, mute: Bool) {
        
    }
    
    func onUserBeKicked(roomId: String, userId: String) {
        agoraPrint("userKicked == \(roomId)  userId == \(userId)")
        _userLeave(userId: userId)
    }
    
    func _userEnter(userInfo: AUIUserInfo) {
        let user = CommerceUser()
        user.userId = userInfo.userId
        user.avatar = userInfo.userAvatar
        user.userName = userInfo.userName
        
        if let userList = self.userList,
            !userList.isEmpty,
            !userList.map({ $0.userId }).contains(userInfo.userId) {
            self.userList?.append(user)
            
        } else if (self.userList ?? []).isEmpty {
            self.userList = [user]
        }
        
        defer{
            self._updateUserCount()
        }
        self.subscribeDelegate?.onUserJoinedRoom(user: user)
        let count = self.userList?.count ?? 1
        self.subscribeDelegate?.onUserCountChanged(userCount: count)
    }
    
    func _userLeave(userId: String) {
        if let index = self.userList?.firstIndex(where: { $0.userId == userId }) {
            guard let model = self.userList?[index] else { return }
            self.userList?.remove(at: index)
            self._updateUserCount()
            self.subscribeDelegate?.onUserLeftRoom(user: model)
            self.subscribeDelegate?.onUserCountChanged(userCount: self.userList?.count ?? 1)
        }
    }
}
