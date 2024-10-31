//
//  AUIScene.swift
//  FBSnapshotTestCase
//
//  Created by wushengtao on 2024/1/22.
//

import Foundation
import AgoraRtmKit

let kSceneTag = "AUIScene"
let kRoomInfoKey = "scene_room_info"
let kRoomInfoRoomId = "room_id"
let kRoomInfoRoomOwnerId = "room_owner_id"
let kRoomCreateTime = "room_create_time"
let kRoomInfoPayloadId = "room_payload_id"
public class AUIScene: NSObject {
    private var channelName: String
    public let userService: AUIUserServiceImpl
    public let arbiter: AUIArbiter
    private var removeClosure: ()->()
    private var rtmManager: AUIRtmManager
    private var enterCondition: AUISceneEnterCondition
    private var expireCondition: AUISceneExpiredCondition
    private var collectionMap: [String: IAUICollection] = [:]
    private lazy var roomCollection: AUIMapCollection = getCollection(key: kRoomInfoKey)!
    private var roomPayload: [String: Any]?
    private var enterRoomCompletion: (([String: Any]?, NSError?)-> ())?
    private var respDelegates = NSHashTable<AUISceneRespDelegate>.weakObjects()
    private var subscribeDate: Date?
    
    deinit {
        aui_info("deinit AUIScene[\(channelName)] \(self)")
        userService.unbindRespDelegate(delegate: self)
    }
    
    public required init(channelName: String, 
                         rtmManager: AUIRtmManager,
                         roomExpiration: RoomExpirationPolicy, 
                         removeClosure:@escaping ()->()) {
        self.channelName = channelName
        self.rtmManager = rtmManager
        self.removeClosure = removeClosure
        self.userService = AUIUserServiceImpl(channelName: channelName, rtmManager: rtmManager)
        self.arbiter = AUIArbiter(channelName: channelName, rtmManager: rtmManager, userInfo: AUIRoomContext.shared.currentUserInfo)
        AUIRoomContext.shared.roomArbiterMap[channelName] = self.arbiter
        self.enterCondition = AUISceneEnterCondition(channelName: channelName, arbiter: self.arbiter)
        self.expireCondition = AUISceneExpiredCondition(channelName: channelName, roomExpiration: roomExpiration)
        super.init()
        aui_info("init AUIScene[\(channelName)] \(self)")
        userService.bindRespDelegate(delegate: self)
        
        self.enterCondition.enterCompletion = { [weak self] in
            guard let self = self else {return}
            self.enterRoomCompletion?(self.roomPayload, nil)
            self.enterRoomCompletion = nil
        }
        
        self.expireCondition.roomDidExpired = { [weak self] in
            guard let self = self else {return}
            
            for obj in self.respDelegates.allObjects {
                obj.onSceneExpire?(channelName: channelName)
            }
            
            //The owner just removed it.
            guard AUIRoomContext.shared.isRoomOwner(channelName: channelName) else {return}
            self.cleanScene()
        }
    }
    
    public func bindRespDelegate(delegate: AUISceneRespDelegate) {
        respDelegates.add(delegate)
    }
    
    public func unbindRespDelegate(delegate: AUISceneRespDelegate) {
        respDelegates.remove(delegate)
    }
    
    
    public func create(createTime: Int64, 
                       payload: [String: Any]?,
                       completion:@escaping (NSError?)->()) {
        create(createTime: createTime,
               ownerId: AUIRoomContext.shared.currentUserInfo.userId,
               payload: payload,
               completion: completion)
    }
    
    //TODO: Do you need to pass a room information object like UIKit, or create your own map collection on this object business to write?
    public func create(createTime: Int64,
                       ownerId: String,
                       payload: [String: Any]?,
                       completion:@escaping (NSError?)->()) {
        aui_info("create[\(channelName)] with payload \(payload ?? [:])", tag: kSceneTag)
        
        guard rtmManager.isLogin else {
            aui_error("create[\(channelName)] fail! not login", tag: kSceneTag)
            completion(NSError.auiError("create fail! not login"))
            return
        }
        
        var roomInfo = [
            kRoomInfoRoomId: channelName,
            kRoomInfoRoomOwnerId: ownerId,
            kRoomCreateTime: "\(createTime)"
        ]
        if let payload = payload {
            roomInfo[kRoomInfoPayloadId] = encodeToJsonStr(payload)
        }
        
        let date = Date()
        
        for obj in self.respDelegates.allObjects {
            let collectionDataMap = obj.onWillInitSceneMetadata?(channelName: channelName)
            collectionDataMap?.forEach({ key, value in
                if let metadata = value as? [String: Any] {
                    let collection: AUIMapCollection? = getCollection(key: key)
                    collection?.initMetaData(channelName: channelName, metadata: metadata, fetchImmediately: false, completion: { err in
                        
                    })
                } else if let metadata = value as? [[String: Any]] {
                    let collection: AUIListCollection? = getCollection(key: key)
                    collection?.initMetaData(channelName: channelName, metadata: metadata, fetchImmediately: false, completion: { err in
                    })
                } else {
                    aui_warn("init meta data fail[\(channelName)] key: \(key) value: \(value)")
                }
            })
        }
        
        let group = DispatchGroup()
        group.enter()
        var error: NSError? = nil
        roomCollection.initMetaData(channelName: channelName,
                                    metadata: roomInfo,
                                    fetchImmediately: true) { err in
            aui_benchmark("create room: rtm initMetaData", cost: -date.timeIntervalSinceNow, tag: kSceneTag)
            if let err = err {
                error = err
            }
            group.leave()
        }
        userService.setUserPayload(payload: UUID().uuidString)
        group.enter()
        getArbiter().create { err in
            aui_benchmark("create room: rtm create lock", cost: -date.timeIntervalSinceNow, tag: kSceneTag)
            if let err = err {
                error = err
            }
            group.leave()
        }
        
        group.notify(queue: .main) {
            completion(error)
        }
    }
    
    public func enter(completion:@escaping ([String: Any]?, NSError?)->()) {
        aui_info("enter[\(channelName)]", tag: kSceneTag)
        guard rtmManager.isLogin else {
            aui_error("enter fail! not login", tag: kSceneTag)
            completion(nil, NSError.auiError("enter fail! not login"))
            return
        }
        
        let date = Date()
        subscribeDate = date
        self.expireCondition.joinCompletion = false
        self.enterRoomCompletion = {[weak self] payload, err in
            if let err = err {
                aui_error("enterRoomCompletion fail: \(err.localizedDescription)", tag: kSceneTag)
            } else {
                aui_info("[Benchmark]enterRoomCompletion: \(Int64(-date.timeIntervalSinceNow * 1000)) ms", tag: kSceneTag)
            }
            self?.expireCondition.joinCompletion = true
            completion(payload, err)
        }
        
        if self.enterCondition.ownerId.isEmpty {
            roomCollection.getMetaData {[weak self] err, metadata in
                guard let self = self else {return}
                if let err = err {
                    self._notifyError(error: err)
                    return
                }
                guard let map = metadata as? [String: String],
                      let ownerId = map[kRoomInfoRoomOwnerId],
                      let createTimestemp = UInt64(map[kRoomCreateTime] ?? "") else {
//                    self.ownerId = "owner unknown"
                    //If the user information is not obtained, it is considered that there is something wrong with the room.
                    self.cleanScene()
                    self._notifyError(error: NSError(domain: "get room owner fatel!", code: -1))
                    self.onMsgRecvEmpty(channelName: self.channelName)
                    aui_error("get room owner fatel!", tag: kSceneTag)
                    return
                }
                aui_info("getMetaData[\(ownerId)] in enter success")
                if let payloadStr = map[kRoomInfoPayloadId] {
                    self.roomPayload = decodeToJsonObj(payloadStr) as? [String: Any]
                    aui_info("getMetaData[\(ownerId)] in enter success with payload: \(payloadStr)", tag: kSceneTag)
                }
                self.enterCondition.ownerId = ownerId
                self.expireCondition.createTimestemp = createTimestemp
            }
        }
//        getArbiter().create()
        getArbiter().acquire {[weak self] err in
            //fail onError(channelName: String, error: NSError)，Don't deal with it here.
            if let _ = err {return}
            self?.enterCondition.lockOwnerAcquireSuccess = true
        }
        rtmManager.subscribeError(channelName: channelName, delegate: self)
        getArbiter().subscribeEvent(delegate: self)
        rtmManager.subscribe(channelName: channelName) {[weak self] error in
            guard let self = self else { return }
            if let error = error, error.code != AgoraRtmErrorCode.duplicateOperation.rawValue {
                aui_error("enterRoom subscribe fail: \(error.localizedDescription)", tag: kSceneTag)
                self._notifyError(error: error)
                return
            }
            aui_benchmark("[Benchmark]rtm manager subscribe", cost: -(date.timeIntervalSinceNow), tag: kSceneTag)
            aui_info("enterRoom subscribe finished \(self.channelName) \(error?.localizedDescription ?? "")", tag: kSceneTag)
            self.enterCondition.subscribeSuccess = true
            self.userService.setUserAttr { _ in
                //TODO: error to retry?
            }
        }
    }
    
    /// Leave scene
    public func leave() {
        aui_info("leave[\(channelName)]", tag: kSceneTag)
        getArbiter().release()
        cleanSDK()
        AUIRoomContext.shared.clean(channelName: channelName)
        removeClosure()
    }
    
    /// Destroy the scene and clean up all caches (including all metadata of rtm)
    public func delete() {
        aui_info("delete[\(channelName)]", tag: kSceneTag)
        cleanScene(forceClean: true)
        getArbiter().destroy()
        cleanSDK()
        AUIRoomContext.shared.clean(channelName: channelName)
        removeClosure()
    }
    
    /// Get a collection, for examplelet collection: AUIMapCollection = scene.getCollection("musicList")
    /// - Parameter key: <#sceneKey description#>
    /// - Returns: <#description#>
    public func getCollection<T: IAUICollection>(key: String) -> T? {
        if let collection = collectionMap[key] {
            return collection as? T
        }
        
        let scene = T.init(channelName: channelName, observeKey: key, rtmManager: rtmManager)
        collectionMap[key] = scene
        return scene
    }
    
    public func getRoomDuration() -> UInt64 {
        return expireCondition.roomUsageDuration() ?? 0
    }
    
    public func getCurrentTs() -> UInt64 {
        return expireCondition.roomCurrentTs() ?? 0
    }
}

//MARK: private
extension AUIScene {
    private func _notifyError(error: NSError) {
        aui_error("_notifyError[\(channelName)] fail: \(error.localizedDescription)", tag: kSceneTag)
        if let completion = self.enterRoomCompletion {
            completion(nil, error)
            self.enterRoomCompletion = nil
        }
    }
    
    private func getArbiter() -> AUIArbiter {
        return arbiter
    }
    
    private func cleanUserInfo(userId: String) {
        //TODO: After the user leaves, it is necessary to clean up the information corresponding to the user in the collection, such as the information on the microphone, the information of ordering songs, etc.
    }
    
    private func cleanScene(forceClean: Bool = false) {
        aui_info("cleanScene[\(channelName)]", tag: kSceneTag)
        guard getArbiter().isArbiter() || forceClean else {
            return
        }
        
        _cleanScene()
    }
    
    private func _cleanScene() {
        aui_info("_cleanScene[\(channelName)]", tag: kSceneTag)
        
        //Each collection is emptied so that everyone can receive onMsgRecvEmpty.
        rtmManager.cleanAllMedadata(channelName: channelName, lockName: "") { error in
            aui_info("cleanScene completion: \(error?.localizedDescription ?? "success")", tag: kSceneTag)
        }
        getArbiter().destroy()
    }
    
    private func cleanSDK() {
        aui_info("cleanSDK[\(channelName)]", tag: kSceneTag)
        rtmManager.unSubscribe(channelName: channelName)
        rtmManager.unsubscribeError(channelName: channelName, delegate: self)
        getArbiter().unSubscribeEvent(delegate: self)
        //TODO: syncmanager Need to log out
//        rtmManager.logout()
    }
}

//MARK: AUIRtmLockProxyDelegate
extension AUIScene: AUIArbiterDelegate {
    public func onArbiterDidChange(channelName: String, arbiterId: String) {
        aui_benchmark("onArbiterDidChange[\(channelName)] arbiterId: [\(arbiterId)] cost", cost: -(subscribeDate?.timeIntervalSinceNow ?? 0), tag: kSceneTag)
        if arbiterId.isEmpty {return}
        self.enterCondition.lockOwnerRetrived = true
        
        //TODO: At present, the callback will cause syncLocalMetaData many times, which requires positioning problems.
        //The network is restored and obtained to the arbitrator (not sure whether the lock is lost, so it needs to be obtained), and synchronize the local metadata to the remote end.
        if self.getArbiter().isArbiter() {
            aui_info("retry syncLocalMetaData", tag: kSceneTag)
            self.collectionMap.values.forEach { collection in
                collection.syncLocalMetaData()
            }
        }
    }
    
    public func onError(channelName: String, error: NSError) {
        aui_error("onError[\(channelName)]: \(error.localizedDescription)", tag: kSceneTag)
        //If the lock does not exist, it is also considered that the room has been destroyed.
        if error.code == AgoraRtmErrorCode.lockNotExist.rawValue {
            cleanScene()
//            self.onMsgRecvEmpty(channelName: channelName)
        }
        _notifyError(error: error)
    }
}

//MARK: AUIUserRespDelegate
extension AUIScene: AUIUserRespDelegate {
    public func onUserBeKicked(roomId: String, userId: String) {
        aui_info("onUserBeKicked[\(roomId)] userId: \(userId)", tag: kSceneTag)
    }
    
    public func onRoomUserSnapshot(roomId: String, userList: [AUIUserInfo]) {
        self.expireCondition.userSnapshotList = userList
        
        guard let user = userList.filter({$0.userId == AUIRoomContext.shared.currentUserInfo.userId }).first else {return}
        aui_info("onRoomUserSnapshot[\(roomId)]", tag: kSceneTag)
        if AUIRoomContext.shared.isRoomOwner(channelName: roomId) {
            self.expireCondition.ownerHasLeftRoom = user.customPayload == nil ? true : false
        }
        onUserAudioMute(userId: user.userId, mute: user.muteAudio)
        onUserVideoMute(userId: user.userId, mute: user.muteVideo)
    }
    
    public func onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
        aui_info("onRoomUserEnter[\(roomId)] userId: \(userInfo.userId)", tag: kSceneTag)
    }
    
    public func onRoomUserLeave(roomId: String, userInfo: AUIUserInfo, reason: AUIRtmUserLeaveReason) {
        aui_info("onRoomUserLeave[\(roomId)] userId: \(userInfo.userId)", tag: kSceneTag)
        guard AUIRoomContext.shared.isRoomOwner(channelName: roomId, userId: userInfo.userId) else {
            cleanUserInfo(userId: userInfo.userId)
            return
        }
        cleanUserInfo(userId: userInfo.userId)
        for obj in self.respDelegates.allObjects {
            obj.onSceneDestroy?(channelName: roomId)
        }
    }
    
    public func onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
        aui_info("onRoomUserUpdate[\(roomId)] userId: \(userInfo.userId)", tag: kSceneTag)
    }
    
    public func onUserAudioMute(userId: String, mute: Bool) {
    }
    
    public func onUserVideoMute(userId: String, mute: Bool) {
    }
}

//MARK: AUIRtmErrorProxyDelegate
extension AUIScene: AUIRtmErrorProxyDelegate {
    public func onTimeStampsDidUpdate(timeStamp: UInt64) {
        if expireCondition.lastUpdateTimestemp == nil {
            expireCondition.lastUpdateTimestemp = timeStamp
        }
    }
    
    public func onTokenPrivilegeWillExpire(channelName: String?) {
        aui_info("onTokenPrivilegeWillExpire: \(channelName ?? "")", tag: kSceneTag)
        for obj in self.respDelegates.allObjects {
            obj.onTokenPrivilegeWillExpire?(channelName: channelName)
        }
    }
    
    @objc public func onMsgRecvEmpty(channelName: String) {
        aui_info("onMsgRecvEmpty[\(channelName)]", tag: kSceneTag)
        //TODO: Get full-empty data in a certain scene, which is defined as the destruction of the room.
        self.respDelegates.allObjects.forEach { obj in
            obj.onSceneDestroy?(channelName: channelName)
        }
    }
    
    @objc public func didReceiveLinkStateEvent(event: AgoraRtmLinkStateEvent) {
        aui_info("didReceiveLinkStateEvent state: \(event.currentState.rawValue), reason: \(event.reason ?? "")", tag: kSceneTag)
        if event.currentState == .disconnected, event.previousState == .connected {
            expireCondition.offlineTimestamp = event.timestamp
        } else if event.currentState == .connected, event.operation == .reconnected {
            //TODO: It is recommended to get the snapshot of lock after reconnection.
            getArbiter().acquire()
            
            expireCondition.reconnectNow(timestamp: event.timestamp)
        }

        if event.currentState == .failed {
            for obj in self.respDelegates.allObjects {
                obj.onSceneFailed?(channelName: channelName, reason: event.reason ?? "")
            }
        }
    }
}
