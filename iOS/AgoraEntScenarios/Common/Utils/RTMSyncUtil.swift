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
    private static var delegate = RTMSyncUtilDeleage()
    
    class func initRTMSyncManager() {
        logOut()
        let config = AUICommonConfig()
        config.appId = KeyCenter.AppId
        let owner = AUIUserThumbnailInfo()
        owner.userId = VLUserCenter.user.id
        owner.userName = VLUserCenter.user.name
        owner.userAvatar = VLUserCenter.user.headUrl
        config.owner = owner
        config.appCert = KeyCenter.IMClientSecret ?? ""
        config.host = KeyCenter.RTMHostUrl
        syncManager = AUISyncManager(rtmClient: nil, commonConfig: config)
        isLogined = false
    }
    
    class func createRoom(roomName: String,
                          roomId: String,
                          payload: [String: Any],
                          callback: @escaping ((NSError?, AUIRoomInfo?) -> Void)) {
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
            callback(error, nil)
        }
    }
    
    class func destroy(roomId: String, callback: ((NSError?) -> Void)?) {
        syncManagerImpl.destroyRoom(roomId: roomId) { error in
            callback?(error)
        }
    }
    
    class func getRoomList(lastCreateTime: Int64 = 0, callback: @escaping ((NSError?, [AUIRoomInfo]?) -> Void)) {
        syncManagerImpl.getRoomInfoList(lastCreateTime: lastCreateTime, pageSize: lastCreateTime == 0 ? 0 : 20, callback: callback)
    }
    
    class func login(channelName: String, success: (() -> Void)?, failure: ((NSError?) -> Void)?) {
        guard isLogined == false else { return }
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
                         success: (() -> Void)?,
                         failure: ((NSError?) -> Void)?) {
        let scene = scene(id: id)
        scene?.bindRespDelegate(delegate: delegate)
        login(channelName: id, success: {
            if ownerId == VLUserCenter.user.id {
                scene?.create { err in
                    if let err = err {
                        print("create scene fail: \(err.localizedDescription)")
                        failure?(err)
                        return
                    }
                    scene?.enter(ownerId: ownerId, completion: { err in
                        if let err = err {
                            print("enter scene fail: \(err.localizedDescription)")
                            return
                        }
                        success?()
                    })
                }
            } else {
                scene?.enter(ownerId: ownerId, completion: { err in
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
        scene?.unbindRespDelegate(delegate: delegate)
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
        delegate.roomDestoryClosure = roomDestoryClosure
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

class RTMSyncUtilDeleage: NSObject, AUISceneRespDelegate {
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

