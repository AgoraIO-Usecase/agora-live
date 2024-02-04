//
//  AUIScene.swift
//  FBSnapshotTestCase
//
//  Created by wushengtao on 2024/1/22.
//

import Foundation
import AgoraRtmKit

let kSceneTag = "AUIScene"
private let kRoomInfoKey = "scene_room_info"
private let kRoomInfoRoomId = "room_id"
private let kRoomInfoRoomOwnerId = "room_owner_id"
public class AUIScene: NSObject {
    private var channelName: String
    private var ownerId: String = "" {
        didSet {
            AUIRoomContext.shared.roomOwnerMap[channelName] = ownerId
        }
    }
    private var rtmManager: AUIRtmManager
    private var collectionMap: [String: IAUICollection] = [:]
    private var userService: AUIUserServiceImpl
    private lazy var roomCollection: AUIMapCollection = getCollection(key: kRoomInfoKey)!
    
    private var respDelegates: NSHashTable<AUISceneRespDelegate> = NSHashTable<AUISceneRespDelegate>.weakObjects()
    
    private var subscribeDate: Date?
    private var lockRetrived: Bool = false {
        didSet {
            checkRoomValid()
        }
    }
    private var subscribeSuccess: Bool = false {
        didSet {
            checkRoomValid()
        }
    }
    private var userSnapshotList: [AUIUserInfo]? {
        didSet {
            checkRoomValid()
        }
    }
    
    deinit {
        userService.unbindRespDelegate(delegate: self)
    }
    
    public required init(channelName: String, rtmManager: AUIRtmManager) {
        self.channelName = channelName
        self.rtmManager = rtmManager
        self.userService = AUIUserServiceImpl(channelName: channelName, rtmManager: rtmManager)
        AUIRoomContext.shared.roomArbiterMap[channelName] = AUIArbiter(channelName: channelName, rtmManager: rtmManager, userInfo: AUIRoomContext.shared.currentUserInfo)
        super.init()
        userService.bindRespDelegate(delegate: self)
    }
    
    public func bindRespDelegate(delegate: AUISceneRespDelegate) {
        respDelegates.add(delegate)
    }
    
    public func unbindRespDelegate(delegate: AUISceneRespDelegate) {
        respDelegates.remove(delegate)
    }
    
    //TODO: 是否需要像UIKit一样传入一个房间信息对象，还是这个对象业务上自己创建map collection来写入
    public func create(completion:@escaping (NSError?)->()) {
        guard rtmManager.isLogin else {
            aui_error("create fail! not login", tag: kSceneTag)
            completion(NSError.auiError("create fail! not login"))
            return
        }
        self.ownerId = AUIRoomContext.shared.currentUserInfo.userId
        let roomInfo = [
            kRoomInfoRoomId: channelName,
            kRoomInfoRoomOwnerId: ownerId
        ]
        
        let date = Date()
        roomCollection.initMetaData(channelName: channelName,
                                    metadata: roomInfo) { err in
            aui_benchmark("rtm setMetaData", cost: -date.timeIntervalSince1970, tag: kSceneTag)
            if let err = err {
                completion(err)
                return
            }
            completion(nil)
        }
        AUIRoomContext.shared.getArbiter(channelName: channelName)?.create()
    }
    
    public func enter(ownerId: String, completion:@escaping (NSError?)->()) {
        guard rtmManager.isLogin else {
            aui_error("enter fail! not login", tag: kSceneTag)
            completion(NSError.auiError("enter fail! not login"))
            return
        }
        
        self.ownerId = ownerId
        subscribeDate = Date()
        let date = Date()
        AUIRoomContext.shared.getArbiter(channelName: channelName)?.acquire()
        rtmManager.subscribeError(channelName: channelName, delegate: self)
        rtmManager.subscribeLock(channelName: channelName, lockName: kRTM_Referee_LockName, delegate: self)
        rtmManager.subscribe(channelName: channelName) {[weak self] error in
            guard let self = self else { return }
            if let error = error {
                completion(error)
                return
            }
            self.subscribeSuccess = true
            aui_benchmark("[Benchmark]rtm manager subscribe", cost: -(date.timeIntervalSinceNow), tag: kSceneTag)
            aui_info("enterRoom subscribe finished \(channelName) \(error?.localizedDescription ?? "")", tag: kSceneTag)
            completion(nil)
        }
    }
    
    /// 离开scene
    public func leave() {
        AUIRoomContext.shared.getArbiter(channelName: channelName)?.release()
        cleanSDK()
    }
    
    /// 销毁scene，清理所有缓存（包括rtm的所有metadata）
    public func delete() {
        cleanScene()
        AUIRoomContext.shared.getArbiter(channelName: channelName)?.destroy()
        AUIRoomContext.shared.clean(channelName: channelName)
        cleanSDK()
    }
    
    /// 获取一个collection，例如let collection: AUIMapCollection = scene.getCollection("musicList")
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
}

//MARK: private
extension AUIScene {
    //如果subscribe成功、锁也获取到、用户列表也获取到，可以检查是否是脏房间并且清理
    private func checkRoomValid() {
        guard subscribeSuccess, lockRetrived else { return }
//        if let completion = self.enterRoomCompletion {
//            completion(roomInfo)
//            self.enterRoomCompletion = nil
//            for obj in self.respDelegates.allObjects {
//                obj.onRoomInfoChange?(roomId: roomInfo.roomId, roomInfo: roomInfo)
//            }
//            
//            //TODO: add more service.sereviceDidLoad
////            chatImplement.sereviceDidLoad?()
//        }
        
        guard let userList = userSnapshotList else { return }
        guard let _ = userList.filter({ AUIRoomContext.shared.isRoomOwner(channelName: channelName, userId: $0.userId)}).first else {
            //room owner not found, clean room
            cleanScene()
            return
        }
    }
    
    private func cleanUserInfo(userId: String) {
        //TODO: 用户离开后，需要清理这个用户对应在collection里的信息，例如上麦信息、点歌信息等
    }
    
    private func cleanScene() {
        guard let arbiter = AUIRoomContext.shared.getArbiter(channelName: channelName),
              arbiter.isArbiter() else {
            return
        }
        
        let removeKeys = collectionMap.map { $0.key }
        
        //每个collection都清空，让所有人收到onMsgRecvEmpty
        rtmManager.cleanBatchMetadata(channelName: channelName,
                                      lockName: "",
                                      removeKeys: removeKeys,
                                      fetchImmediately: true) { error in
            aui_collection_log("cleanScene completion: \(error?.localizedDescription ?? "success")")
        }
        arbiter.destroy()
    }
    
    private func cleanSDK() {
        rtmManager.unSubscribe(channelName: channelName)
        rtmManager.unsubscribeError(channelName: channelName, delegate: self)
        rtmManager.unsubscribeLock(channelName: channelName, lockName: kRTM_Referee_LockName, delegate: self)
        //TODO: syncmanager 需要logout
//        rtmManager.logout()
    }
}

//MARK: AUIRtmLockProxyDelegate
extension AUIScene: AUIRtmLockProxyDelegate {
    public func onReceiveLockDetail(channelName: String, lockDetail: AgoraRtmLockDetail) {
        aui_benchmark("onReceiveLockDetail", cost: -(subscribeDate?.timeIntervalSinceNow ?? 0), tag: kSceneTag)
        if lockDetail.owner.isEmpty {return}
        self.lockRetrived = true
    }
    
    public func onReleaseLockDetail(channelName: String, lockDetail: AgoraRtmLockDetail) {
    }
}

//MARK: AUIUserRespDelegate
extension AUIScene: AUIUserRespDelegate {
    public func onUserBeKicked(roomId: String, userId: String) {
    }
    
    public func onRoomUserSnapshot(roomId: String, userList: [AUIUserInfo]) {
        self.userSnapshotList = userList
        
        guard let user = userList.filter({$0.userId == AUIRoomContext.shared.currentUserInfo.userId }).first else {return}
        aui_info("onRoomUserSnapshot", tag: kSceneTag)
        onUserAudioMute(userId: user.userId, mute: user.muteAudio)
        onUserVideoMute(userId: user.userId, mute: user.muteVideo)
    }
    
    public func onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
    }
    
    public func onRoomUserLeave(roomId: String, userInfo: AUIUserInfo) {
        guard AUIRoomContext.shared.isRoomOwner(channelName: roomId, userId: userInfo.userId) else {
            cleanUserInfo(userId: userInfo.userId)
            return
        }
        cleanScene()
    }
    
    public func onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
    }
    
    public func onUserAudioMute(userId: String, mute: Bool) {
//        guard userId == AUIRoomContext.shared.currentUserInfo.userId else {return}
//        aui_info("onUserAudioMute mute current user: \(mute)", tag: kSertviceTag)
//        rtcEngine.adjustRecordingSignalVolume(mute ? 0 : 100)
    }
    
    public func onUserVideoMute(userId: String, mute: Bool) {
//        guard userId == AUIRoomContext.shared.currentUserInfo.userId else {return}
//        aui_info("onMuteVideo onUserVideoMute [\(userId)]: \(mute)", tag: kSertviceTag)
//        rtcEngine.enableLocalVideo(!mute)
//        let option = AgoraRtcChannelMediaOptions()
//        option.publishCameraTrack = !mute
//        rtcEngine.updateChannel(with: option)
    }
}

//MARK: AUIRtmErrorProxyDelegate
extension AUIScene: AUIRtmErrorProxyDelegate {
    public func onTokenPrivilegeWillExpire(channelName: String?) {
        aui_info("onTokenPrivilegeWillExpire: \(channelName ?? "")", tag: kSceneTag)
        for obj in self.respDelegates.allObjects {
            obj.onTokenPrivilegeWillExpire?(channelName: channelName)
        }
    }
    
    @objc public func onMsgRecvEmpty(channelName: String) {
        //TODO: 某个scene里拿到全空数据，定义为房间被销毁了
        self.respDelegates.allObjects.forEach { obj in
            obj.onSceneDestroy?(roomId: channelName)
        }
    }
    
    @objc public func onConnectionStateChanged(channelName: String,
                                               connectionStateChanged state: AgoraRtmClientConnectionState,
                                               result reason: AgoraRtmClientConnectionChangeReason) {
        if reason == .changedRejoinSuccess {
            AUIRoomContext.shared.getArbiter(channelName: channelName)?.acquire()
        }
        guard state == .failed, reason == .changedBannedByServer else {
            return
        }
        
        for obj in self.respDelegates.allObjects {
            obj.onSceneUserBeKicked?(roomId: channelName, userId: AUIRoomContext.shared.currentUserInfo.userId)
        }
    }
}
