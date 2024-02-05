//
//  RTMSyncManager.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2024/2/4.
//

import Foundation
import RTMSyncManager

class RTMSyncUtil: NSObject {
    private static var syncManager: AUISyncManager?
    private static var syncManagerImpl = AUIRoomManagerImpl()
    private static var isLogined: Bool = false
    private static let roomDelegate = RTMSyncUtilRoomDeleage()
    
    class func initRTMSyncManager() {
        let config = AUICommonConfig()
        config.appId = KeyCenter.AppId
        let owner = AUIUserThumbnailInfo()
        owner.userId = VLUserCenter.user.id
        owner.userName = VLUserCenter.user.name
        owner.userAvatar = VLUserCenter.user.headUrl
        config.owner = owner
        config.appCert = KeyCenter.Certificate ?? ""
        config.host = KeyCenter.RTMHostUrl
        syncManager = AUISyncManager(rtmClient: nil, commonConfig: config)
        isLogined = false
        syncManager?.logout()
    }
    
    class func createRoom(roomName: String,
                          roomId: String,
                          payload: [String: Any],
                          callback: @escaping ((NSError?, AUIRoomInfo?) -> Void)) {
        logOut()
        let roomInfo = AUIRoomInfo()
        roomInfo.roomName = roomName
        roomInfo.roomId = roomId
        roomInfo.customPayload = payload
        let userInfo = AUIUserThumbnailInfo()
        userInfo.userId = VLUserCenter.user.id
        userInfo.userAvatar = VLUserCenter.user.headUrl
        userInfo.userName = VLUserCenter.user.name
        roomInfo.owner = userInfo
        login(channelName: roomId, success: {
            self.syncManagerImpl.createRoom(room: roomInfo, callback: callback)
        }) { error in
            if error?.code == 10002 {
                createRoom(roomName: roomName, roomId: roomId, payload: payload, callback: callback)
                return
            }
            callback(error, nil)
        }
    }
    
    class func destroy(roomId: String, callback: ((NSError?) -> Void)?) {
        syncManagerImpl.destroyRoom(roomId: roomId) { error in
            callback?(error)
        }
    }
    
    class func getRoomList(lastCreateTime: Int64 = 0, callback: @escaping ((NSError?, [AUIRoomInfo]?) -> Void)) {
        syncManagerImpl.getRoomInfoList(lastCreateTime: lastCreateTime, pageSize: 20, callback: callback)
    }
    
    class func login(channelName: String, success: (() -> Void)?, failure: ((NSError?) -> Void)?) {
        if isLogined == true {
            success?()
            return
        }
        let model = SyncTokenGenerateNetworkModel()
        model.channelName = channelName
        model.userId = VLUserCenter.user.id
        model.request { err, result in
            let result = result as? [String: String]
            let rtmToken = (result?["rtmToken"]) ?? ""
            self.syncManager?.login(with: rtmToken) { err in
                if let err = err {
                    print("login fail: \(err.localizedDescription)")
                    failure?(err)
                    return
                }
                self.isLogined = true
                success?()
            }
        }
    }
    
    class func logOut() {
        guard isLogined else { return }
        syncManager?.logout()
        isLogined = false
    }
    
    class func scene(id: String) -> AUIScene? {
        syncManager?.getScene(channelName: id)
    }
    
    class func collection(id: String, key: String) -> AUIMapCollection? {
        scene(id: id)?.getCollection(key: key)
    }
    
    class func joinScene(id: String, 
                         ownerId: String,
                         payload: [String: Any]?,
                         success: (() -> Void)?,
                         failure: ((NSError?) -> Void)?) {
        leaveScene(id: id, ownerId: ownerId)
        let scene = scene(id: id)
        scene?.bindRespDelegate(delegate: roomDelegate)
        login(channelName: id, success: {
            if ownerId == VLUserCenter.user.id {
                scene?.create(payload: payload) { err in
                    if let err = err {
                        print("create scene fail: \(err.localizedDescription)")
                        failure?(err)
                        return
                    }
                    scene?.enter(completion: { res, error in
                        if let err = err {
                            print("enter scene fail: \(err.localizedDescription)")
                            return
                        }
                        print("res == \(res)")
                        success?()
                    })
                }
            } else {
                scene?.enter(completion: { res, err in
                    if let err = err {
                        print("enter scene fail: \(err.localizedDescription)")
                        failure?(err)
                        return
                    }
                    print("res == \(res)")
                    success?()
                })
            }
        }, failure: failure)
    }
    
    class func leaveScene(id: String, ownerId: String) {
        let scene = scene(id: id)
        scene?.unbindRespDelegate(delegate: roomDelegate)
        if ownerId == VLUserCenter.user.id {
            scene?.delete()
            let model = SyncRoomDestroyNetworkModel()
            model.roomId = id
            model.request { err, _ in
            }
        } else {
            scene?.leave()
        }
    }
    
    class func subscribeRoomDestroy(roomDestoryClosure: ((String) -> Void)?) {
        roomDelegate.roomDestoryClosure = roomDestoryClosure
    }
    
    class func getUserList(id: String, callback: @escaping (_ roomId: String, _ userList: [AUIUserInfo]) -> Void) {
        let userDelegate = RTMSyncUtilUserDelegate()
        userDelegate.onUserListCallback = callback
        scene(id: id)?.userService.bindRespDelegate(delegate: userDelegate)
    }
    class func userEnter(id: String, callback: @escaping (_ roomId: String, _ userInfo: AUIUserInfo) -> Void) {
        let userDelegate = RTMSyncUtilUserDelegate()
        userDelegate.onUserEnterCallback = callback
        scene(id: id)?.userService.bindRespDelegate(delegate: userDelegate)
    }
    class func userLeave(id: String, callback: @escaping (_ roomId: String, _ userInfo: AUIUserInfo) -> Void) {
        let userDelegate = RTMSyncUtilUserDelegate()
        userDelegate.onUserLeaveCallback = callback
        scene(id: id)?.userService.bindRespDelegate(delegate: userDelegate)
    }
    class func userUpdate(id: String, callback: @escaping (_ roomId: String, _ userInfo: AUIUserInfo) -> Void) {
        let userDelegate = RTMSyncUtilUserDelegate()
        userDelegate.onUserUpdateCallback = callback
        scene(id: id)?.userService.bindRespDelegate(delegate: userDelegate)
    }
    class func userKicked(id: String, callback: @escaping (_ roomId: String, _ userId: String) -> Void) {
        let userDelegate = RTMSyncUtilUserDelegate()
        userDelegate.onUserKickedCallback = callback
        scene(id: id)?.userService.bindRespDelegate(delegate: userDelegate)
    }
    class func userAudioMute(id: String, callback: @escaping (_ userId: String, _ mute: Bool) -> Void) {
        let userDelegate = RTMSyncUtilUserDelegate()
        userDelegate.onUserAudioMuteCallback = callback
        scene(id: id)?.userService.bindRespDelegate(delegate: userDelegate)
    }
    class func userVideoMute(id: String, callback: @escaping (_ userId: String, _ mute: Bool) -> Void) {
        let userDelegate = RTMSyncUtilUserDelegate()
        userDelegate.onUserVideoMuteCallback = callback
        scene(id: id)?.userService.bindRespDelegate(delegate: userDelegate)
    }
    
    class func subscribeAttributesDidChanged(id: String,
                                             key: String,
                                             changeClosure: ((_ channelName: String, _ object: [String: Any]) -> Void)?) {
        collection(id: id, key: key)?.subscribeAttributesDidChanged(callback: { channelName, key, object in
            print("channelName == \(channelName)")
            print("object == \(object)")
            print("key == \(key)")
        })
    }
    
    class func addMetaData(id: String,
                           key: String,
                           data: [String: Any],
                           callback: ((NSError?) -> Void)?) {
        collection(id: id, key: key)?.addMetaData(valueCmd: nil, value: data, filter: nil, callback: callback)
    }
    
    class func getMetaData(id: String,
                           key: String,
                           callback: ((NSError?, Any?) -> Void)?) {
        collection(id: id, key: key)?.getMetaData(callback: { error, result in
            callback?(error, result)
        })
    }
    
    class func updateMetaData(id: String,
                              key: String,
                              data: [String: Any],
                              callback: ((NSError?) -> Void)?) {
        collection(id: id, key: key)?.updateMetaData(valueCmd: nil, value: data, filter: nil, callback: callback)
    }
    
    class func mergeMetaData(id: String,
                             key: String,
                             data: [String: Any],
                             callback: ((NSError?) -> Void)?) {
        collection(id: id, key: key)?.mergeMetaData(valueCmd: nil, value: data, filter: nil, callback: callback)
    }
    
    class func calculateMetaData(id: String,
                                 key: String,
                                 paramsKeys: [String],
                                 value: Int,
                                 min: Int,
                                 max: Int,
                                 callback: ((NSError?) -> Void)?) {
        collection(id: id, key: key)?.calculateMetaData(valueCmd: nil,
                                                        key: paramsKeys,
                                                        value: value,
                                                        min: min,
                                                        max: max,
                                                        filter: nil,
                                                        callback: callback)
    }
    
    class func removeMetaData(id: String, key: String, filter: [[String: Any]]?, callback: ((NSError?) -> Void)?) {
        collection(id: id, key: key)?.removeMetaData(valueCmd: nil, filter: filter, callback: callback)
    }
    
    class func cleanMetaData(id: String, key: String, callback: ((NSError?) -> Void)?) {
        collection(id: id, key: key)?.cleanMetaData(callback: callback)
    }
}

class RTMSyncUtilRoomDeleage: NSObject, AUISceneRespDelegate {
    var roomDestoryClosure: ((String) -> Void)?
    func onSceneDestroy(roomId: String) {
        print("房间销毁 == \(roomId)")
        let model = SyncRoomDestroyNetworkModel()
        model.roomId = roomId
        model.request { err, _ in
        }
        roomDestoryClosure?(roomId)
    }
}

class RTMSyncUtilUserDelegate: NSObject, AUIUserRespDelegate {
    var onUserListCallback: ((_ roomId: String, _ userList: [AUIUserInfo]) -> Void)?
    var onUserEnterCallback: ((_ roomId: String, _ userInfo: AUIUserInfo) -> Void)?
    var onUserLeaveCallback: ((_ roomId: String, _ userInfo: AUIUserInfo) -> Void)?
    var onUserUpdateCallback: ((_ roomId: String, _ userInfo: AUIUserInfo) -> Void)?
    var onUserKickedCallback: ((_ roomId: String, _ userId: String) -> Void)?
    var onUserAudioMuteCallback: ((_ userId: String, _ mute: Bool) -> Void)?
    var onUserVideoMuteCallback: ((_ userId: String, _ mute: Bool) -> Void)?
    
    func onRoomUserSnapshot(roomId: String, userList: [RTMSyncManager.AUIUserInfo]) {
        onUserListCallback?(roomId, userList)
    }
    
    func onRoomUserEnter(roomId: String, userInfo: RTMSyncManager.AUIUserInfo) {
        onUserEnterCallback?(roomId, userInfo)
    }
    
    func onRoomUserLeave(roomId: String, userInfo: RTMSyncManager.AUIUserInfo) {
        onUserLeaveCallback?(roomId, userInfo)
    }
    
    func onRoomUserUpdate(roomId: String, userInfo: RTMSyncManager.AUIUserInfo) {
        onUserUpdateCallback?(roomId, userInfo)
    }
    
    func onUserAudioMute(userId: String, mute: Bool) {
        onUserAudioMuteCallback?(userId, mute)
    }
    
    func onUserVideoMute(userId: String, mute: Bool) {
        onUserVideoMuteCallback?(userId, mute)
    }
    
    func onUserBeKicked(roomId: String, userId: String) {
        onUserKickedCallback?(roomId, userId)
    }
}
