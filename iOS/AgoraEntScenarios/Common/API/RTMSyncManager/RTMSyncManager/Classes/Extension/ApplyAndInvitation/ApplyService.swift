//
//  ApplyService.swift
//  FBSnapshotTestCase
//
//  Created by wushengtao on 2024/6/7.
//

import Foundation

@objc public class ApplyInfo: NSObject, Codable {
    public var userId: String = ""
    public var userName: String = ""
    public var userAvatar: String = ""
    
    public override init() {
        super.init()
    }
    
    init?(userInfo: AUIUserThumbnailInfo) {
        self.userId = userInfo.userId
        self.userName = userInfo.userName
        self.userAvatar = userInfo.userAvatar
        super.init()
    }
    
    enum CodingKeys: String, CodingKey {
        case userId, userName, userAvatar
    }
}

@objc public protocol ApplyServiceProtocol: NSObjectProtocol {
    func onApplyListDidUpdate(channelName: String, list: [ApplyInfo])
}

enum ApplyCmd: String {
    case create = "createApply"
    case cancel = "cancelApply"
    case accept = "acceptApply"
    case reject = "rejectApply"
}

private let applyKey: String = "apply"

public class ApplyService: NSObject {
    private let channelName: String
    private let syncManager: AUISyncManager
    
    private var respDelegates = NSHashTable<ApplyServiceProtocol>.weakObjects()
    
    private(set) lazy var applyCollection: AUIListCollection = {
        let collection: AUIListCollection = syncManager.createScene(channelName: channelName).getCollection(key: applyKey)!
        return collection
    }()
    
    required init(channelName: String,
                  syncManager: AUISyncManager) {
        self.channelName = channelName
        self.syncManager = syncManager
        super.init()
        subscribe()
    }
    
    private func subscribe() {
        applyCollection.subscribeAttributesDidChanged {[weak self] channelName, observeKey, value in
            guard let self = self else {return}
            guard applyKey == observeKey else {return}
            let list: [ApplyInfo] = decodeModelArray(value.getList() ?? []) ?? []
            for element in self.respDelegates.allObjects {
                element.onApplyListDidUpdate(channelName: channelName, list: list)
            }
        }
        
        let scene = syncManager.getScene(channelName: channelName)
        scene?.userService.bindRespDelegate(delegate: self)
    }
}

//MARK: public method
extension ApplyService {
    public func subscribe(delegate: ApplyServiceProtocol) {
        if respDelegates.contains(delegate) {
            return
        }
        
        respDelegates.add(delegate)
    }
    
    public func unsubscribe(delegate: ApplyServiceProtocol) {
        respDelegates.remove(delegate)
    }
    
    public func addApply(userId: String, completion: ((NSError?)->())?) {
        let roomId = channelName
        guard let apply = getApplyInfo(userId: userId),
              let value = encodeModel(apply) else {
            aui_info("[\(roomId)]addApply userId: \(userId) fail", tag: "ApplyService")
            completion?(NSError(domain: "ApplyService",
                                code: 0,
                                userInfo: ["msg": "apply info is nil"]))
            return
        }
        
        aui_info("[\(roomId)]addApply userId: \(userId) start", tag: "ApplyService")
        //TODO: Ensure that the application is not in the interaction, and the application cannot be initiated during the interaction.
        applyCollection.addMetaData(valueCmd: ApplyCmd.create.rawValue,
                                    value: value,
                                    filter: [["userId": userId]]) { err in
            aui_info("[\(roomId)]addApply userId: \(userId) completion: \(err?.localizedDescription ?? "success")", tag: "ApplyService")
            completion?(err)
        }
    }
    
    public func acceptApply(userId: String, completion: ((NSError?)->())?) {
        //TODO: The audience can't remove apply and interaction add together. If one fails, the data will be confused.
        /*
         1. Arbitrator audience A initiates mic seat
         2. Homeowner B received to seat.
         3. A cancels Lianmai
         4. B has not received the canceled metadata, and it is believed that there is still A's application locally. At this time, click accept, and this problem will occur.
         
         Click acceptApply from
         1. Check the local status
         2.RemoveApply
         3. StartLinkingInteraction
         
         Change it to
         1. StartLinkingInteraction
         2. The arbitrator requests through interaction and checks the apply form through the subscribeWillAdd callback to see if the corresponding interactive user is in apply.
         3. If it is confirmed that interaction can be inserted, remove the user in the apply by the way.
         */
        removeApply(applyCmd: .accept, userId: userId, completion: completion)
    }
    
    public func cancelApply(userId: String, completion: ((NSError?)->())?) {
        removeApply(applyCmd: .cancel, userId: userId, completion: completion)
    }
    
    
    public func getApplyList(completion: @escaping (NSError?, [ApplyInfo]?)->()) {
        let roomId = channelName
        aui_info("[\(roomId)]getApplyList start", tag: "ApplyService")
        applyCollection.getMetaData { err, value in
            aui_info("[\(roomId)]getApplyList completion: \(err?.localizedDescription ?? "success")", tag: "ApplyService")
            if let err = err {
                completion(err, nil)
                return
            }
            let value = value as? [[String: Any]] ?? []
            let list: [ApplyInfo] = decodeModelArray(value) ?? []
            completion(nil, list)
        }
    }
}

//MARK: private
extension ApplyService {
    private func removeApply(applyCmd: ApplyCmd, userId: String, completion: ((NSError?)->())?) {
        let roomId = channelName
        aui_info("[\(roomId)]removeApply userId: \(userId) cmd: \(applyCmd.rawValue)", tag: "ApplyService")
        applyCollection.removeMetaData(valueCmd: applyCmd.rawValue,
                                       filter: [["userId": userId]],
                                       callback: completion)
    }
    
    private func getApplyInfo(userId: String) -> ApplyInfo? {
        guard let scene = syncManager.getScene(channelName: channelName),
              let userInfo = scene.userService.userList.first(where: { $0.userId == userId }) else {
            return nil
        }
        
        return ApplyInfo(userInfo: userInfo)
    }
}

extension ApplyService: AUIUserRespDelegate {
    public func onRoomUserSnapshot(roomId: String, userList: [AUIUserInfo]) {
        
    }
    
    public func onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
        
    }
    
    public func onRoomUserLeave(roomId: String, userInfo: AUIUserInfo, reason: AUIRtmUserLeaveReason) {
        cancelApply(userId: userInfo.userId) { err in
            
        }
    }
    
    public func onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
        
    }
    
    public func onUserAudioMute(userId: String, mute: Bool) {
        
    }
    
    public func onUserVideoMute(userId: String, mute: Bool) {
        
    }
    
    public func onUserBeKicked(roomId: String, userId: String) {
        
    }
    
    
}
