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
    private static var isLogined: Bool = false
    
    class func initRTMSyncManager() {
        let config = AUICommonConfig()
        config.appId = KeyCenter.AppId
        let owner = AUIUserThumbnailInfo()
        owner.userId = VLUserCenter.user.id
        owner.userName = VLUserCenter.user.name
        owner.userAvatar = VLUserCenter.user.headUrl
        config.owner = owner
        config.host = KeyCenter.HostUrl
        syncManager = AUISyncManager(rtmClient: nil, commonConfig: config)
        isLogined = false
    }
    
    class func login(channelName: String, success: (() -> Void)?, failure: ((String?) -> Void)?) {
        guard isLogined == false else { return }
        let model = AUITokenGenerateNetworkModel()
        model.channelName = channelName
        model.userId = VLUserCenter.user.id
        model.request { err, result in
            guard let result = result as? [String: String] else { return }
            let rtmToken = result["rtmToken"] ?? ""
            self.syncManager?.login(with: rtmToken) { err in
                if let err = err {
                    print("login fail: \(err.localizedDescription)")
                    failure?(err.localizedDescription)
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
                         failure: ((String?) -> Void)?) {
        let scene = scene(id: id)
        scene?.bindRespDelegate(delegate: RTMSyncUtilDeleage())
        if ownerId == VLUserCenter.user.id {
            scene?.create { err in
                if let err = err {
                    print("create scene fail: \(err.localizedDescription)")
                    failure?(err.localizedDescription)
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
                    failure?(err.localizedDescription)
                    return
                }
                success?()
            })
        }
    }
    
    class func leaveScene(id: String, ownerId: String) {
        let scene = scene(id: id)
        scene?.unbindRespDelegate(delegate: RTMSyncUtilDeleage())
        if ownerId == VLUserCenter.user.id {
            scene?.delete()
            let model = AUIRoomDestroyNetworkModel()
            model.roomId = id
            model.request { err, _ in
            }
        } else {
            scene?.leave()
        }
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
    
    class func cleanMetaData(id: String, key: String, callback: ((NSError?) -> Void)?) {
        collection(id: id, key: key)?.cleanMetaData(callback: callback)
    }
}

class RTMSyncUtilDeleage: NSObject, AUISceneRespDelegate {
    func onSceneDestroy(roomId: String) {
        print("房间销毁 == \(roomId)")
        let model = AUIRoomDestroyNetworkModel()
        model.roomId = roomId
        model.request { err, _ in
        }
    }
}

