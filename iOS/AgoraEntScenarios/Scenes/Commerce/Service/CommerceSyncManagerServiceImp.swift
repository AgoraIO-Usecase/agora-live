//
//  ShowSyncManagerServiceImp.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/3.
//

import Foundation
import UIKit
import AgoraSyncManager_overseas

private let kSceneId = "scene_commerce_1.0.0"

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
    
    fileprivate var connectState: SocketConnectState?
    
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
        
        SyncUtilsWrapper.initScene(uniqueId: uniqueId, sceneId: kSceneId) {[weak self] state, inited in
            guard let self = self else {
                return
            }
            
            defer {
                completion(state == .open ? nil : CommerceError.networkError.toNSError())
            }
            
            if let _ = self.connectState {
                self.connectState = state
                return
            }
            self.connectState = state
            let showState = CommerceServiceConnectState(rawValue: state.rawValue) ?? .open
            self.subscribeDelegate?.onConnectStateChanged(state: showState)
            guard state == .open else { return }
            self._subscribeAll()
            guard !inited else {
                self._getUserList(roomId: self.roomId) {[weak self] (err, list) in
                    self?.subscribeDelegate?.onUserCountChanged(userCount: list?.count ?? 0)
                }
                return
            }
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
        let params = room.yy_modelToJSONObject() as? [String: Any]
        initScene { [weak self] error in
            if let error = error  {
                completion(error, nil)
                return
            }
            SyncUtilsWrapper.joinSceneByQueue(id: room.roomId,
                                              userId: room.ownerId,
                                              isOwner: true,
                                              property: params) { result in
                //            LogUtils.log(message: "result == \(result.toJson() ?? "")", level: .info)
                let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
                guard let channelName = channelName else {
                    agoraAssert("createRoom fail: channelName == nil")
                    completion(nil, nil)
                    return
                }
                self?.roomId = channelName
                let output = CommerceRoomDetailModel.yy_model(with: params!)
                self?.roomList?.append(room)
                self?._startCheckExpire()
                self?._subscribeAll()
                completion(nil, output)
            } fail: { error in
                completion(error.toNSError(), nil)
            }
        }
    }
    
    @objc func joinRoom(room: CommerceRoomListModel,
                        completion: @escaping (NSError?, CommerceRoomDetailModel?) -> Void) {
        isJoined = false
        let params = room.yy_modelToJSONObject() as? [String: Any]
        initScene { [weak self] error in
            if let error = error  {
                self?._joinRoomRetry(room: room, completion: completion, reachLimitTask: {
                    completion(error, nil)
                })
                return
            }
            SyncUtilsWrapper.joinSceneByQueue(id: room.roomId,
                                         userId: room.ownerId,
                                         isOwner: self?.isOwner(room) ?? false,
                                         property: params) { result in
                //            LogUtils.log(message: "result == \(result.toJson() ?? "")", level: .info)
                let channelName = result.getPropertyWith(key: "roomId", type: String.self) as? String
                guard let channelName = channelName else {
                    agoraAssert("joinRoom fail: channelName == nil")
                    completion(nil, nil)
                    return
                }
                self?.roomId = channelName
                let output = CommerceRoomDetailModel.yy_model(with: params!)
                self?._startCheckExpire()
                self?._subscribeAll()
                self?.isJoined = true
                completion(nil, output)
            } fail: { error in
                self?._joinRoomRetry(room: room, completion: completion, reachLimitTask: {
                    completion(error.toNSError(), nil)
                })
            }
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
            _addUserIfNeed(roomId: roomId, finished: completion)
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
        initScene { error in
            if let error = error {
                completion(error, nil)
                return
            }
            SyncUtil.fetchAll { results in
                agoraPrint("result == \(results.compactMap { $0.toJson() })")
                let dataArray = results.filter({$0.getId().count > 0}).map({ info in
                    return CommerceRoomListModel.yy_model(with: info.toJson()!.toDictionary())!
                })
                let roomList = dataArray.sorted(by: { ($0.updatedAt > 0 ? $0.updatedAt : $0.createdAt) > ($1.updatedAt > 0 ? $1.updatedAt : $1.createdAt) })
                completion(nil, roomList)
            } fail: { error in
                completion(error.toNSError(), nil)
            }
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
        SyncUtil.scene(id: channelName)?.deleteScenes()
        roomId = nil
        completion(nil)
    }
    
    fileprivate func _subscribeAll() {
        agoraPrint("imp all subscribe...")
        _subscribeOnlineUsersChanged()
        _subscribeMessageChanged()
    }
    
    private func _unsubscribeAll() {
        guard let channelName = roomId else {
            return
        }
        agoraPrint("imp all unsubscribe...")
        SyncUtil
            .scene(id: channelName)?
            .unsubscribeScene()
    }
    
    private func _leaveScene(roomId: String) {
        agoraPrint("imp leave scene: \(roomId)")
        SyncUtil.leaveScene(id: roomId)
    }
}

//MARK: user operation
extension CommerceSyncManagerServiceImp {
    fileprivate func _addUserIfNeed(roomId: String?, finished: @escaping (NSError?) -> Void) {
        _getUserList(roomId: roomId) {[weak self] error, userList in
            guard let self = self, roomId == self.roomId else {
                finished(NSError(domain: "unknown error", code: -1))
                return
            }
            // current user already add
            if self.userList.contains(where: { $0.userId == VLUserCenter.user.id }) {
                finished(nil)
                return
            }
            self._addUserInfo(roomId: roomId) {
                finished(nil)
            }
        }
    }

    private func _getUserList(roomId: String?, finished: @escaping (NSError?, [CommerceUser]?) -> Void) {
        guard let channelName = roomId else {
            finished(NSError(domain: "roomId is empty", code: 0), nil)
            return
        }
        agoraPrint("imp user get...")
        SyncUtil
            .scene(id: channelName)?
            .collection(className: SYNC_SCENE_ROOM_USER_COLLECTION)
            .get(success: { [weak self] list in
                agoraPrint("imp user get success...")
                guard (list.first?.getId().count ?? 0) > 0 else { return }
                let users = list.compactMap({ CommerceUser.yy_model(withJSON: $0.toJson()!)! })
//            guard !users.isEmpty else { return }
                self?.userList = users
                self?._updateUserCount(completion: { error in

                })
                finished(nil, users)
            }, fail: { error in
                agoraPrint("imp user get fail :\(error.message)...")
                finished(error.toNSError(), nil)
            })
    }

    private func _addUserInfo(roomId: String?, finished: @escaping () -> Void) {
        guard let channelName = roomId else {
//            assert(false, "channelName = nil")
            print("addUserInfo channelName = nil")
            return
        }
        let model = CommerceUser()
        model.userId = VLUserCenter.user.id
        model.avatar = VLUserCenter.user.headUrl
        model.userName = VLUserCenter.user.name

        let params = model.yy_modelToJSONObject() as! [String: Any]
        agoraPrint("imp user add ...\(channelName)")
        SyncUtil
            .scene(id: channelName)?
            .collection(className: SYNC_SCENE_ROOM_USER_COLLECTION)
            .add(data: params, success: { [weak self] object in
                agoraPrint("imp user add success...\(channelName)")
                guard let self = self,
                      let jsonStr = object.toJson(),
                      let model = CommerceUser.yy_model(withJSON: jsonStr) else {
                    return
                }
                
                if self.userList.contains(where: { $0.userId == model.userId }) {
                    return
                }
                
                self.userList.append(model)
                finished()
            }, fail: { error in
                agoraPrint("imp user add fail :\(error.message)...")
                finished()
            })
    }
    
    
    private func _updateUserInfo(user: CommerceUser, completion: @escaping (NSError?) -> Void) {
        let channelName = getRoomId()
        agoraPrint("imp user update...")

        let params = user.yy_modelToJSONObject() as! [String: Any]
        SyncUtil
            .scene(id: channelName)?
            .collection(className: SYNC_SCENE_ROOM_USER_COLLECTION)
            .update(id: user.objectId!,
                    data:params,
                    success: {
                agoraPrint("imp user update success...")
                completion(nil)
            }, fail: { error in
                agoraPrint("imp user update fail :\(error.message)...")
                completion(error.toNSError())
            })
    }

    private func _subscribeOnlineUsersChanged() {
        guard let channelName = roomId else {
            agoraPrint("channelName = nil")
            return
        }
        agoraPrint("imp user subscribe ...")
        SyncUtil
            .scene(id: channelName)?
            .subscribe(key: SYNC_SCENE_ROOM_USER_COLLECTION,
                       onCreated: { _ in
                       }, onUpdated: { [weak self] object in
                           agoraPrint("imp user subscribe onUpdated...")
                           guard let self = self,
                                 let jsonStr = object.toJson(),
                                 let model = CommerceUser.yy_model(withJSON: jsonStr) else { return }
                           defer{
                               self._updateUserCount { error in
                               }
                               self.subscribeDelegate?.onUserCountChanged(userCount: self.userList.count)
                           }
                           self.userList.append(model)
                           self.subscribeDelegate?.onUserJoinedRoom(user: model)
                           self.subscribeDelegate?.onUserCountChanged(userCount: self.userList.count)
                           
                       }, onDeleted: { [weak self] object in
                           agoraPrint("imp user subscribe onDeleted... [\(object.getId())]")
                           guard let self = self else { return }
                           var model: CommerceUser? = nil
                           if let index = self.userList.firstIndex(where: { object.getId() == $0.objectId }) {
                               model = self.userList[index]
                               self.userList.remove(at: index)
                               self._updateUserCount { error in
                               }
                           }
                           guard let model = model else { return }
                           self.subscribeDelegate?.onUserLeftRoom(user: model)
                           self.subscribeDelegate?.onUserCountChanged(userCount: self.userList.count)
                       }, onSubscribed: {
//                LogUtils.log(message: "subscribe message", level: .info)
                       }, fail: { error in
                           agoraPrint("imp user subscribe fail \(error.message)...")
                           ToastView.show(text: error.message)
                       })
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
        SyncUtil
            .scene(id: channelName)?
            .collection(className: SYNC_SCENE_ROOM_USER_COLLECTION)
            .delete(id: objectId,
                    success: { _ in
                agoraPrint("imp user delete success...")
            }, fail: { error in
                agoraPrint("imp user delete fail \(error.message)...")
                completion(error.toNSError())
            })
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
        let params = roomInfo.yy_modelToJSONObject() as! [String: Any]
        agoraPrint("imp room update user count... [\(channelName)]")
        SyncUtil
            .scene(id: channelName)?
            .update(key: "",
                    data: params,
                    success: { obj in
                agoraPrint("imp room update user count success...")
            }, fail: { error in
                agoraPrint("imp room update user count fail \(error.message)...")
            })

//        userListCountDidChanged?(UInt(count))
    }
}


//MARK: message operation
extension CommerceSyncManagerServiceImp {
    private func _getMessageList(finished: @escaping (NSError?, [CommerceMessage]?) -> Void) {
        let channelName = getRoomId()
        agoraPrint("imp message get...")
        SyncUtil
            .scene(id: channelName)?
            .collection(className: SYNC_MANAGER_MESSAGE_COLLECTION)
            .get(success: { [weak self] list in
                agoraPrint("imp user get success...")
                let messageList = list.compactMap({ CommerceMessage.yy_model(withJSON: $0.toJson()!)! })
//            guard !users.isEmpty else { return }
                self?.messageList = messageList
                finished(nil, messageList)
            }, fail: { error in
                agoraPrint("imp user get fail :\(error.message)...")
                agoraPrint("error = \(error.description)")
                finished(error.toNSError(), nil)
            })
    }

    private func _addMessage(roomId: String?, message: CommerceMessage, finished: ((NSError?) -> Void)?) {
        guard let channelName = roomId else { return }
        agoraPrint("imp message add ...")

        let params = message.yy_modelToJSONObject() as! [String: Any]
        SyncUtil
            .scene(id: channelName)?
            .collection(className: SYNC_MANAGER_MESSAGE_COLLECTION)
            .add(data: params, success: { object in
                agoraPrint("imp message add success...\(channelName) params = \(params)")
                finished?(nil)
            }, fail: { error in
                agoraPrint("imp message add fail :\(error.message)...\(channelName)")
                agoraPrint(error.message)
                finished?(error.toNSError())
            })
    }

    private func _subscribeMessageChanged() {
        let channelName = getRoomId()
        agoraPrint("imp message subscribe ...")
        SyncUtil
            .scene(id: channelName)?
            .subscribe(key: SYNC_MANAGER_MESSAGE_COLLECTION,
                       onCreated: { _ in
                       }, onUpdated: {[weak self] object in
                           agoraPrint("imp message subscribe onUpdated... [\(object.getId())] \(channelName)")
                           guard let self = self,
                                 let jsonStr = object.toJson(),
                                 let model = CommerceMessage.yy_model(withJSON: jsonStr)
                           else {
                               return
                           }
                           self.messageList.append(model)
                           self.subscribeDelegate?.onMessageDidAdded(message: model)
                       }, onDeleted: { object in
                           agoraPrint("imp message subscribe onDeleted... [\(object.getId())] \(channelName)")
                           agoraAssert("not implemented")
                       }, onSubscribed: {
                       }, fail: { error in
                           agoraPrint("imp message subscribe fail \(error.message)...")
                           ToastView.show(text: error.message)
                       })
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
        initScene { error in
            if let error = error {
                completion(error, nil)
                return
            }
            SyncUtil.fetchAll { results in
                agoraPrint("result == \(results.compactMap { $0.toJson() })")
                var dataArray = results.filter({$0.getId().count > 0}).map({ info in
                    return CommerceRoomListModel.yy_model(with: info.toJson()!.toDictionary())!
                })
                dataArray = CommerceRobotService.shared.generateRobotRoomsAppend(rooms: dataArray)
                let roomList = dataArray.sorted(by: { $0.createdAt > $1.createdAt })
                completion(nil, roomList)
            } fail: { error in
                completion(error.toNSError(), nil)
            }
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
