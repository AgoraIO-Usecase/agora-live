//
//  RTMSyncManager.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2024/2/4.
//

import Foundation
import RTMSyncManager
import AgoraRtmKit

class RTMSyncUtil: NSObject {
    private static var syncManager: AUISyncManager?
    private static var roomManager = AUIRoomManagerImpl(sceneId: kEcommerceSceneId)
    private static var isLogined: Bool = false
    
    class func initRTMSyncManager() {
        destroy()
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
        
        AUIRoomContext.shared.displayLogClosure = { msg in
            commercePrintLog(msg)
        }
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
        roomManager.createRoom(room: roomInfo, callback: callback)
    }
    
    class func getRoomList(lastCreateTime: Int64 = 0, callback: @escaping ((NSError?, [AUIRoomInfo]?) -> Void)) {
        roomManager.getRoomInfoList(lastCreateTime: lastCreateTime, pageSize: 20, callback: callback)
    }
    
    class func updateRoomInfo(roomName: String, roomId: String, payload: [String: Any], ownerInfo: AUIUserThumbnailInfo) {
        let roomInfo = AUIRoomInfo()
        roomInfo.roomName = roomName
        roomInfo.roomId = roomId
        roomInfo.customPayload = payload
        roomInfo.owner = ownerInfo
        roomManager.updateRoom(room: roomInfo) { _, _ in }
    }
    
    class func renew(rtmToken: String) {
        syncManager?.renew(token: rtmToken)
    }
    
    class func login(channelName: String, success: (() -> Void)?, failure: ((NSError?) -> Void)?) {
        if isLogined == true {
            success?()
            return
        }
        
        guard let rtmToken = AppContext.shared.commerceRtmToken else {
            let date = Date()
            //TODO: may cause infinite recursion
            CommerceAgoraKitManager.shared.preGenerateToken {
                commercePrintLog("[Timing][\(channelName)] token generate cost: \(Int(-date.timeIntervalSinceNow * 1000)) ms")
                self.login(channelName: channelName, success: success, failure: failure)
            }
            return
        }
        
        let date = Date()
        self.syncManager?.login(with: rtmToken) { err in
            if let err = err {
                print("login fail: \(err.localizedDescription)")
                failure?(err)
                return
            }
            commercePrintLog("[Timing][\(channelName)] login cost: \(Int(-date.timeIntervalSinceNow * 1000)) ms")
            self.isLogined = true
            success?()
        }
    }
    
    class func logout() {
        guard isLogined else { return }
        syncManager?.logout()
        isLogined = false
    }
    
    class func destroy() {
        logout()
        syncManager?.destroy()
        syncManager = nil
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
        commercePrintLog("joinScene[\(id)]", tag: "RTMSyncUtil")
        _ = syncManager?.createScene(channelName: id)
        let scene = scene(id: id)
        login(channelName: id, success: {
            if ownerId == VLUserCenter.user.id {
                let date = Date()
                scene?.create(payload: payload) { err in
                    commercePrintLog("[Timing][\(id)] rtm create scene cost: \(Int(-date.timeIntervalSinceNow * 1000)) ms")
                    if let err = err {
                        print("create scene fail: \(err.localizedDescription)")
                        failure?(err)
                        return
                    }
                    let date = Date()
                    scene?.enter(completion: { res, error in
                        commercePrintLog("[Timing][\(id)] rtm enter scene cost: \(Int(-date.timeIntervalSinceNow * 1000)) ms")
                        if let err = err {
                            print("enter scene fail: \(err.localizedDescription)")
                            failure?(err)
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
    
    class func leaveScene(id: String, asOwner: Bool) {
        commercePrintLog("leaveScene[\(id)]", tag: "RTMSyncUtil")
        let scene = scene(id: id)
        if asOwner {
            scene?.delete()
            roomManager.destroyRoom(roomId: id) { err in
            }
        } else {
            scene?.leave()
        }
    }
    
    class func getUserList(id: String, callback: @escaping (_ roomId: String, _ userList: [AUIUserInfo]) -> Void) {
        scene(id: id)?.userService.getUserInfoList(roomId: id, userIdList: [], callback: { _, userList in
            callback(id, userList ?? [])
        })
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
        let collection = collection(id: id, key: key)
        collection?.subscribeAttributesDidChanged(callback: { channelName, key, object in
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
    
    class func subscribeMessage(channelName: String, delegate: AUIRtmMessageProxyDelegate) {
        syncManager?.rtmManager.subscribeMessage(channelName: channelName, delegate: delegate)
    }
    
    class func unsubscribeMessage(channelName: String, delegate: AUIRtmMessageProxyDelegate) {
        syncManager?.rtmManager.unsubscribeMessage(channelName: channelName, delegate: delegate)
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
                    commerceErrorLog("addMetaData error == \(error?.localizedDescription ?? "")")
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
    
    class func sendMessage(channelName: String, data: [String: Any]) {
        if let jsonData = try? JSONSerialization.data(withJSONObject: data, options: []),
           let message = String(data: jsonData, encoding: .utf8) {
            syncManager?.rtmManager.publish(channelName: channelName, message: message, completion: { _ in
            })
        }
    }
}
