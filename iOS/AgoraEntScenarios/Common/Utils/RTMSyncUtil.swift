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
    private static let userDelegate = RTMSyncUtilUserDelegate()
    
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
    class func listCollection(id: String, key: String) -> AUIListCollection? {
        scene(id: id)?.getCollection(key: key)
    }
    
    class func joinScene(id: String, 
                         ownerId: String,
                         payload: [String: Any]?,
                         success: (() -> Void)?,
                         failure: ((NSError?) -> Void)?) {
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
                    success?()
                })
            }
        }, failure: failure)
    }
    
    class func leaveScene(id: String, ownerId: String) {
        let scene = scene(id: id)
        if ownerId == VLUserCenter.user.id {
            scene?.delete()
            let model = SyncRoomDestroyNetworkModel()
            model.roomId = id
            model.request { err, _ in
                print("error == \(err?.localizedDescription ?? "")")
                scene?.unbindRespDelegate(delegate: self.roomDelegate)
            }
        } else {
            scene?.leave()
            scene?.unbindRespDelegate(delegate: roomDelegate)
        }
    }
    
    class func subscribeRoomDestroy(roomDestoryClosure: ((String) -> Void)?) {
        roomDelegate.roomDestoryClosure = roomDestoryClosure
    }
    
    class func getUserList(id: String, callback: @escaping (_ roomId: String, _ userList: [AUIUserInfo]) -> Void) {
        userDelegate.onUserListCallback = callback
        scene(id: id)?.userService.bindRespDelegate(delegate: userDelegate)
    }
    
    class func subscribeUserDidChange(id: String,
                                      userEnter: ((_ roomId: String, _ userInfo: AUIUserInfo) -> Void)?,
                                      userLeave: ((_ roomId: String, _ userInfo: AUIUserInfo) -> Void)?,
                                      userUpdate: ((_ roomId: String, _ userInfo: AUIUserInfo) -> Void)?,
                                      userKicked: ((_ roomId: String, _ userId: String) -> Void)?,
                                      audioMute: ((_ userId: String, _ mute: Bool) -> Void)?,
                                      videoMute: ((_ userId: String, _ mute: Bool) -> Void)?) {
        userDelegate.onUserEnterCallback = userEnter
        userDelegate.onUserLeaveCallback = userLeave
        userDelegate.onUserUpdateCallback = userUpdate
        userDelegate.onUserKickedCallback = userKicked
        userDelegate.onUserAudioMuteCallback = audioMute
        userDelegate.onUserVideoMuteCallback = videoMute
        scene(id: id)?.userService.bindRespDelegate(delegate: userDelegate)
    }
    
    class func muteAudio(channelName: String, isMute: Bool, callback: ((NSError?) -> Void)?) {
        scene(id: channelName)?.userService.muteUserAudio(isMute: isMute, callback: { error in
            callback?(error)
        })
    }
    
    class func muteVideo(channelName: String, isMute: Bool, callback: ((NSError?) -> Void)?) {
        scene(id: channelName)?.userService.muteUserVideo(isMute: isMute, callback: { error in
            callback?(error)
        })
    }
    
    class func subscribeAttributesDidChanged(id: String,
                                             key: String,
                                             changeClosure: ((_ channelName: String, _ object: AUIAttributesModel) -> Void)?) {
        collection(id: id, key: key)?.subscribeAttributesDidChanged(callback: { channelName, key, object in
            changeClosure?(channelName, object)
        })
    }
    
    class func subscribeListAttributesDidChanged(id: String,
                                                 key: String,
                                                 changeClosure: ((_ channelName: String, _ object: AUIAttributesModel) -> Void)?) {
        listCollection(id: id, key: key)?.subscribeAttributesDidChanged(callback: { channelName, key, object in
            changeClosure?(channelName, object)
        })
    }
    
    class func addMetaData(id: String,
                           key: String,
                           data: [String: Any],
                           callback: ((NSError?) -> Void)?) {
        collection(id: id, key: key)?.addMetaData(valueCmd: nil, value: data, filter: nil, callback: callback)
    }
    
    class func addMetaData(id: String,
                           key: String,
                           data: [[String: Any]]?,
                           callback: ((NSError?) -> Void)?) {
        let group = DispatchGroup()
        data?.forEach({
            group.enter()
            listCollection(id: id, key: key)?.addMetaData(valueCmd: nil, value: $0, filter: nil, callback: { error in
                if error != nil {
                    print("error == \(error?.localizedDescription ?? "")")
                }
                group.leave()
            })
        })
        group.notify(queue: .main, work: DispatchWorkItem(block: {
            callback?(nil)
        }))
    }
    
    class func getMetaData(id: String,
                           key: String,
                           callback: ((NSError?, Any?) -> Void)?) {
        collection(id: id, key: key)?.getMetaData(callback: { error, result in
            callback?(error, result)
        })
    }
    
    class func getListMetaData(id: String,
                               key: String,
                               callback: ((NSError?, Any?) -> Void)?) {
        listCollection(id: id, key: key)?.getMetaData(callback: { error, result in
            callback?(error, result)
        })
    }
    
    class func updateMetaData(id: String,
                              key: String,
                              data: [String: Any],
                              callback: ((NSError?) -> Void)?) {
        collection(id: id, key: key)?.updateMetaData(valueCmd: nil, value: data, filter: nil, callback: callback)
    }
    
    class func updateListMetaData(id: String,
                                  key: String,
                                  data: [String: Any],
                                  filter: [[String: Any]]? = nil,
                                  callback: ((NSError?) -> Void)?) {
        listCollection(id: id, key: key)?.updateMetaData(valueCmd: nil, value: data, filter: filter, callback: callback)
//        let group = DispatchGroup()
//        data?.forEach({
//            group.enter()
//            listCollection(id: id, key: key)?.updateMetaData(valueCmd: nil, value: $0, filter: nil, callback: { error in
//                if error != nil {
//                    print("error == \(error?.localizedDescription ?? "")")
//                }
//                group.leave()
//            })
//        })
//        group.notify(queue: .main, work: DispatchWorkItem(block: {
//            callback?(nil)
//        }))
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
