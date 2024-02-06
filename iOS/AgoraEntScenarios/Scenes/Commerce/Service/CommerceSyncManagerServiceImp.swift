//
//  ShowSyncManagerServiceImp.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/3.
//

import Foundation
import UIKit
import RTMSyncManager

private let kSceneId = "scene_ecommerce_0_2_0"

private let SYNC_MANAGER_MESSAGE_COLLECTION = "commerce_message_collection"
private let SYNC_MANAGER_SEAT_APPLY_COLLECTION = "commerce_seat_apply_collection"
private let SYNC_MANAGER_SEAT_INVITATION_COLLECTION = "commerce_seat_invitation_collection"
private let SYNC_MANAGER_PK_INVITATION_COLLECTION = "commerce_pk_invitation_collection"
private let SYNC_MANAGER_INTERACTION_COLLECTION = "commerce_interaction_collection"


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
    
    commerceLogger.error(message, context: "Service")
}

private func agoraPrint(_ message: String) {
    commerceLogger.info(message, context: "Service")
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
    private var userList: [CommerceUser] = [CommerceUser]()
    private var messageList: [CommerceMessage] = [CommerceMessage]()
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
//            agoraAssert("roomId == nil")
            return ""
        }

        return _roomId
    }
    
    fileprivate func isOwner(_ room: CommerceRoomListModel) -> Bool {
        return room.ownerId == VLUserCenter.user.id
    }
    
    fileprivate func initScene(completion: @escaping (NSError?) -> Void) {
        RTMSyncUtil.joinScene(id: kSceneId, ownerId: room?.ownerId ?? VLUserCenter.user.id ?? "", payload: nil) {
            self._subscribeAll()
            completion(nil)
        } failure: { error in
            completion(error)
        }
    }
    
    private func cleanCache() {
        userList = [CommerceUser]()
        messageList = [CommerceMessage]()
        userMuteLocalAudio = false
    }
    
    fileprivate func _checkRoomExpire() {
        guard let room = self.room else { return }
        
        let currentTs = Int64(Date().timeIntervalSince1970 * 1000)
        let expiredDuration = 20 * 60 * 1000
        agoraPrint("checkRoomExpire: \(currentTs - room.createdAt) / \(expiredDuration)")
        guard currentTs - room.createdAt > expiredDuration else { return }
        
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
        RTMSyncUtil.createRoom(roomName: roomName, roomId: room.roomId, payload: params) { error, roomInfo in
            if error != nil {
                completion(error, nil)
                return
            }
            RTMSyncUtil.joinScene(id: roomId, ownerId: room.ownerId, payload: params) {
                let channelName = roomInfo?.roomId//result.getPropertyWith(key: "roomId", type: String.self) as? String
                guard let channelName = channelName else {
                    agoraAssert("createRoom fail: channelName == nil")
                    completion(nil, nil)
                    return
                }
                self.roomId = channelName
                let roomModel = CommerceRoomDetailModel()
                roomModel.ownerAvatar = roomInfo?.owner?.userAvatar
                roomModel.ownerId = roomInfo?.owner?.userId ?? ""
                roomModel.ownerName = roomInfo?.owner?.userName
                roomModel.roomId = channelName
                roomModel.roomName = roomInfo?.roomName
                roomModel.thumbnailId = thumbnailId
                roomModel.roomStatus = .end
                roomModel.createdAt = Date().millionsecondSince1970()
                self.roomList?.append(roomModel)
                self._startCheckExpire()
                self._subscribeAll()
                completion(nil, roomModel)
            } failure: { error in
                completion(error, nil)
            }
        }
    }
    
    @objc func joinRoom(room: CommerceRoomListModel,
                        completion: @escaping (NSError?, CommerceRoomDetailModel?) -> Void) {
        isJoined = false
        self.roomId = room.roomId
        let params = (room.yy_modelToJSONObject() as? [String: Any])
        RTMSyncUtil.joinScene(id: room.roomId, ownerId: room.ownerId, payload: params) {
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
            self._subscribeOnRoomDestroy(isOwner: self.isOwner(roomModel))
            self._subscribeAll()
            self.isJoined = true
            completion(nil, roomModel)
        } failure: { error in
            completion(error, nil)
        }
    }
    
    private func _joinRoomRetry(room: CommerceRoomListModel,
                               completion: @escaping (NSError?, CommerceRoomDetailModel?) -> Void, reachLimitTask:(()->Void)?){
        if joinRetry == 3 {
            reachLimitTask?()
        }else {
            joinRetry += 1
            joinRoom(room: room, completion: completion)
        }
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
        _removeUser(roomId: roomId) { err in
        }
        
        //current user is room owner, remove room
        if roomInfo.ownerId == VLUserCenter.user.id {
            _removeRoom(completion: completion)
            return
        }
        
        _leaveRoom(completion: completion)
    }
    
    func initRoom(roomId: String?, completion: @escaping (NSError?) -> Void) {
        if isJoined {
            _sendMessageWithText(roomId: roomId, text: "join_live_room".commerce_localized)
        }
    }
    
    func deinitRoom(roomId: String?, completion: @escaping (NSError?) -> Void) {
        _removeUser(roomId: roomId, completion: completion)
        _sendMessageWithText(roomId: roomId, text: "leave_live_room".commerce_localized)
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
                roomModel.createdAt = (info.customPayload["createdAt"] as? Int64) ?? 0
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
        _removeUser(roomId: roomId) { error in
        }
        
        _leaveScene(roomId: channelName)
    }

    private func _removeRoom(completion: @escaping (NSError?) -> Void) {
        guard let channelName = roomId else {
            agoraAssert("channelName = nil")
            return
        }
        RTMSyncUtil.destroy(roomId: channelName, callback: nil)
        roomId = nil
        completion(nil)
    }
    
    fileprivate func _subscribeAll() {
        agoraPrint("imp all subscribe...")
        _subscribeOnlineUsersChanged()
        _subscribeMessageChanged()
        _getUserList(roomId: room?.roomId) { _, list in
            
        }
    }
    
    private func _unsubscribeAll() {
        guard let channelName = roomId else {
            return
        }
        agoraPrint("imp all unsubscribe...")
        RTMSyncUtil.leaveScene(id: channelName, ownerId: room?.ownerId ?? "")
    }
    
    private func _leaveScene(roomId: String) {
        agoraPrint("imp leave scene: \(roomId)")
        RTMSyncUtil.leaveScene(id: roomId, ownerId: room?.ownerId ?? "")
    }
}

//MARK: user operation
extension CommerceSyncManagerServiceImp {
    private func _getUserList(roomId: String?, finished: @escaping (NSError?, [CommerceUser]?) -> Void) {
        guard let channelName = roomId else {
            finished(NSError(domain: "roomId is empty", code: 0), nil)
            return
        }
        agoraPrint("imp user get...")
        RTMSyncUtil.getUserList(id: channelName) { roomId, userList in
            agoraPrint("imp user get success...")
            print("list == \(userList)")
            let users = userList.compactMap({ item in
                let user = CommerceUser()
                user.userId = item.userId
                user.avatar = item.userAvatar
                user.userName = item.userName
                return user
            })
            self.userList = users
            self._updateUserCount(completion: { error in })
            finished(nil, [])
        }
    }

    private func _subscribeOnlineUsersChanged() {
        guard let channelName = roomId else {
            agoraPrint("channelName = nil")
            return
        }
        agoraPrint("imp user subscribe ...")
        RTMSyncUtil.subscribeUserDidChange(id: channelName) { roomId, userInfo in
            print("userEnter == \(roomId)  object == \(userInfo)")
            userEnter(userInfo: userInfo)
            
        } userLeave: { roomId, userInfo in
            print("userLeave == \(roomId)  object == \(userInfo)")
            userLeave(userId: userInfo.userId)
            
        } userUpdate: { roomId, userInfo in
            print("userUpdate == \(roomId)  object == \(userInfo)")
            userEnter(userInfo: userInfo)
            
        } userKicked: { roomId, userId in
            print("userKicked == \(roomId)  userId == \(userId)")
            userLeave(userId: userId)
            
        } audioMute: { userId, mute in
            print("userAudioMute == \(userId)  userId == \(mute)")
        } videoMute: { userId, mute in
            print("userVideoMute == \(userId)  userId == \(mute)")
        }
        
        func userEnter(userInfo: AUIUserInfo) {
            let user = CommerceUser()
            user.userId = userInfo.userId
            user.avatar = userInfo.userAvatar
            user.userName = userInfo.userName
            if !self.userList.map({ $0.userId }).contains(userInfo.userId) {
                self.userList.append(user)
            }
            defer{
                self._updateUserCount { error in
                }
                self.subscribeDelegate?.onUserCountChanged(userCount: self.userList.count)
            }
            self.subscribeDelegate?.onUserJoinedRoom(user: user)
            self.subscribeDelegate?.onUserCountChanged(userCount: self.userList.count)
        }
        
        func userLeave(userId: String) {
            if let index = self.userList.firstIndex(where: { $0.userId == userId }) {
                let model = self.userList[index]
                self.userList.remove(at: index)
                self._updateUserCount { error in
                }
                self.subscribeDelegate?.onUserLeftRoom(user: model)
                self.subscribeDelegate?.onUserCountChanged(userCount: self.userList.count)
            }
        }
//        SyncUtil
//            .scene(id: channelName)?
//            .subscribe(key: SYNC_SCENE_ROOM_USER_COLLECTION,
//                       onCreated: { _ in
//                       }, onUpdated: { [weak self] object in
//                           agoraPrint("imp user subscribe onUpdated...")
//                           guard let self = self,
//                                 let jsonStr = object.toJson(),
//                                 let model = CommerceUser.yy_model(withJSON: jsonStr) else { return }
//                           defer{
//                               self._updateUserCount { error in
//                               }
//                               self.subscribeDelegate?.onUserCountChanged(userCount: self.userList.count)
//                           }
//                           self.userList.append(model)
//                           self.subscribeDelegate?.onUserJoinedRoom(user: model)
//                           self.subscribeDelegate?.onUserCountChanged(userCount: self.userList.count)
//                           
//                       }, onDeleted: { [weak self] object in
//                           agoraPrint("imp user subscribe onDeleted... [\(object.getId())]")
//                           guard let self = self else { return }
//                           var model: CommerceUser? = nil
//                           if let index = self.userList.firstIndex(where: { object.getId() == $0.objectId }) {
//                               model = self.userList[index]
//                               self.userList.remove(at: index)
//                               self._updateUserCount { error in
//                               }
//                           }
//                           guard let model = model else { return }
//                           self.subscribeDelegate?.onUserLeftRoom(user: model)
//                           self.subscribeDelegate?.onUserCountChanged(userCount: self.userList.count)
//                       }, onSubscribed: {
////                LogUtils.log(message: "subscribe message", level: .info)
//                       }, fail: { error in
//                           agoraPrint("imp user subscribe fail \(error.message)...")
//                           ToastView.show(text: error.message)
//                       })
    }

    private func _removeUser(roomId: String?, completion: @escaping (NSError?) -> Void) {
        guard let channelName = roomId else {
            agoraAssert("channelName = nil")
            return
        }
        
        guard let objectId = userList.filter({ $0.userId == VLUserCenter.user.id }).first?.objectId else {
            agoraPrint("_removeUser objectId = nil")
            return
        }
        agoraPrint("imp user delete... [\(objectId)]")
        //TODO: 待完成
//        RTMSyncUtil.removeMetaData(id: channelName, key: SYNC_SCENE_ROOM_USER_COLLECTION, filter: [["objectId": objectId]]) { error in
//            if error != nil {
//                agoraPrint("imp user delete fail \(error?.localizedDescription ?? "")...")
//                completion(error)
//                return
//            }
//            agoraPrint("imp user delete success...")
//            completion(nil)
//        }
    }

    private func _updateUserCount(completion: @escaping (NSError?) -> Void) {
        _updateUserCount(with: userList.count)
    }

    private func _updateUserCount(with count: Int) {
        guard let channelName = roomId,
              let roomInfo = roomList?.filter({ $0.roomId == self.getRoomId() }).first,
              roomInfo.ownerId == VLUserCenter.user.id
        else {
//            agoraPrint("updateUserCount channelName = nil")
//            userListCountDidChanged?(UInt(count))
            return
        }
        let roomUserCount = count
        if roomUserCount == roomInfo.roomUserCount {
            return
        }
        roomInfo.updatedAt = Int64(Date().timeIntervalSince1970 * 1000)
        roomInfo.roomUserCount = roomUserCount
        roomInfo.objectId = channelName
    }
    
    private func _subscribeOnRoomDestroy(isOwner: Bool) {
        guard isOwner == false else { return }
        RTMSyncUtil.subscribeRoomDestroy { roomId in
            self.subscribeDelegate?.onRoomDestroy(roomId: roomId)
        }
    }
}


//MARK: message operation
extension CommerceSyncManagerServiceImp {
    private func _addMessage(roomId: String?, message: CommerceMessage, finished: ((NSError?) -> Void)?) {
        guard let channelName = roomId else { return }
        agoraPrint("imp message add ...")
        if messageList.contains(where: { $0.userId == message.userId && $0.message == message.message }) {
            return
        }
        let params = message.yy_modelToJSONObject() as! [String: Any]
        RTMSyncUtil.addMetaData(id: channelName, key: SYNC_MANAGER_MESSAGE_COLLECTION, data: params) { error in
            if error != nil {
                agoraPrint("imp message add fail :\(error?.localizedDescription ?? "")...\(channelName)")
                finished?(error)
                return
            }
            agoraPrint("imp message add success...\(channelName) params = \(params)")
            finished?(nil)
        }
    }

    private func _subscribeMessageChanged() {
        let channelName = getRoomId()
        agoraPrint("imp message subscribe ...")
        RTMSyncUtil.subscribeAttributesDidChanged(id: channelName, key: SYNC_MANAGER_MESSAGE_COLLECTION) { channelName, object in
            agoraPrint("imp message subscribe onUpdated... [\(object.getMap() ?? [:])] \(channelName)")
            guard let map = object.getMap(),
                  let model = CommerceMessage.yy_model(with: map) else { return }
            
            if self.messageList.contains(where: { $0.userId == model.userId && $0.message == model.message }) {
                return
            }
            self.messageList.append(model)
            self.subscribeDelegate?.onMessageDidAdded(message: model)
        }
    }
}

class CommerceRobotSyncManagerServiceImp: CommerceSyncManagerServiceImp {
    deinit {
        agoraPrint("deinit-- ShowRobotSyncManagerServiceImp")
    }
    
    override func isOwner(_ room: CommerceRoomListModel) -> Bool {
        if room.roomId.count == 6 {
            return super.isOwner(room)
        }
        
        return true
    }
    
    override func _checkRoomExpire() {
        guard let room = self.room else { return }
        if room.roomId.count == 6 {
            return super._checkRoomExpire()
        }
        CommerceRobotService.shared.playerHeartBeat()
    }
    
    @objc override func _getRoomList(page: Int, completion: @escaping (NSError?, [CommerceRoomListModel]?) -> Void) {
        RTMSyncUtil.getRoomList { error, results in
            agoraPrint("result == \(results?.compactMap { $0 })")
            if error != nil {
                completion(error, nil)
                return
            }
            let dataArray = results?.map({ info in
                let roomModel = CommerceRoomDetailModel()
                roomModel.ownerAvatar = info.owner?.userAvatar
                roomModel.ownerId = info.owner?.userId ?? ""
                roomModel.ownerName = info.owner?.userName
                roomModel.roomId = info.roomId
                roomModel.roomName = info.roomName
                roomModel.thumbnailId = info.customPayload["thumbnailId"] as? String
                roomModel.createdAt = (info.customPayload["createdAt"] as? Int64) ?? 0
                return roomModel
            })
            completion(nil, dataArray)
        }
    }
    
    @objc override func createRoom(roomName: String,
                                   roomId: String,
                                   thumbnailId: String,
                                   completion: @escaping (NSError?, CommerceRoomDetailModel?) -> Void) {
        super.createRoom(roomName: roomName, roomId: roomId, thumbnailId: thumbnailId, completion: completion)
    }
    
    @objc override func joinRoom(room: CommerceRoomListModel,
                        completion: @escaping (NSError?, CommerceRoomDetailModel?) -> Void) {
        super.joinRoom(room: room, completion: completion)
    }
}
