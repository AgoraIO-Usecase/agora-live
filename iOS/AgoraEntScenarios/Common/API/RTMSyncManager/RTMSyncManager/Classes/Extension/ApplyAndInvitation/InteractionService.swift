//
//  InteractionService.swift
//  RTMSyncManager
//
//  Created by wushengtao on 2024/6/7.
//

import Foundation

@objc public enum InteractionType: Int, Codable {
    case idle = 0
    case linking = 1
    case pk = 2
    
    public var isInteracting: Bool {
        switch self {
        case .linking, .pk:
            return true
        default:
            return false
        }
    }
}

@objc public class InteractionInfo: NSObject, Codable {
    public var type: InteractionType = .idle
    public var userId: String = ""     // Interactor ID
    public var userName: String = ""   // User name of the interactor
    public var roomId: String = ""     // Room ID of the interactor
    public var createdAt: UInt64 = 0   // The start time of the interaction is different from the time difference of 19700101, and the unit is ms
    
    enum CodingKeys: String, CodingKey {
        case type, userId, userName, roomId, createdAt
    }
}

enum InteractionCmd: String {
    case startPk = "startPKInteraction"
    case startLink = "startLinkingInteraction"
    case stop = "stopInteraction"
}

private let interactionKey = "interaction"

@objc public protocol InteractionServiceProtocol: NSObjectProtocol {
    func onInteractionListDidUpdate(channelName: String, list: [InteractionInfo])
}

public class InteractionService: NSObject {
    private let channelName: String
    private let syncManager: AUISyncManager
    private let roomPresenceService: RoomPresenceService
    private var respDelegates = NSHashTable<InteractionServiceProtocol>.weakObjects()
    private(set) var interactionInfo: InteractionInfo?
    private(set) lazy var interactionCollection: AUIMapCollection = {
        let collection: AUIMapCollection = syncManager.createScene(channelName: channelName).getCollection(key: interactionKey)!
        return collection
    }()
    
    deinit {
        aui_info("deinit InteractionService[\(channelName)]", tag: "InteractionService")
        innerUnsubscribe()
    }
    
    required init(channelName: String,
                  syncManager: AUISyncManager,
                  presenceService: RoomPresenceService) {
        aui_info("init InteractionService[\(channelName)]", tag: "InteractionService")
        self.channelName = channelName
        self.syncManager = syncManager
        self.roomPresenceService = presenceService
        super.init()
        innerSubscribe()
    }
    
    private func innerSubscribe() {
        interactionCollection.subscribeAttributesDidChanged {[weak self] channelName, observeKey, value in
            guard let self = self else {return}
            guard interactionKey == observeKey else {return}
            let info: InteractionInfo = decodeModel(value.getMap() ?? [:]) ?? InteractionInfo()
            self.interactionInfo = info
            var list = [info]
            if info.type == .linking {
                self.roomPresenceService.updateRoomPresenceInfo(roomId: channelName,
                                                                status: .linking,
                                                                interactorId: info.userId,
                                                                interactorName: info.userName,
                                                                completion: nil)
            } else if info.type == .idle {
                self.roomPresenceService.updateRoomPresenceInfo(roomId: channelName,
                                                                status: .idle,
                                                                interactorId: "",
                                                                interactorName: "",
                                                                completion: nil)
                list = []
            }
            for element in self.respDelegates.allObjects {
                element.onInteractionListDidUpdate(channelName: channelName, list: list)
            }
        }
        let scene = syncManager.getScene(channelName: channelName)
        scene?.userService.bindRespDelegate(delegate: self)
    }
    
    private func innerUnsubscribe() {
        interactionCollection.subscribeAttributesDidChanged(callback: nil)
        let scene = syncManager.getScene(channelName: channelName)
        scene?.userService.unbindRespDelegate(delegate: self)
    }
}

//MARK: public method
extension InteractionService {
    public func subscribe(delegate: InteractionServiceProtocol) {
        if respDelegates.contains(delegate) {
            return
        }
        
        respDelegates.add(delegate)
    }
    
    public func unsubscribe(delegate: InteractionServiceProtocol) {
        respDelegates.remove(delegate)
    }
    
    public func startPKInteraction(roomId: String, userId: String, userName: String, completion: ((NSError?)->())?) {
        //TODO: The arbitrator needs to check the Interaction form.
        /*
         The arbitrator needs to test the following situations
         When updating, judge whether Lianmai exists or not.
         */
        aui_info("startPKInteraction roomId: \(roomId) userId: \(userId)", tag: "InteractionService")
        let info = InteractionInfo()
        info.type = .pk
        info.roomId = roomId
        info.userId = userId
        info.userName = userName
        info.createdAt = syncManager.getScene(channelName: channelName)?.getCurrentTs() ?? 0
        let value = encodeModel(info) ?? [:]
        interactionCollection.addMetaData(valueCmd: InteractionCmd.startPk.rawValue,
                                          value: value) { err in
            aui_info("startPKInteraction roomId: \(roomId) userId: \(userId) completion: \(err?.localizedDescription ?? "success")", tag: "InteractionService")
            completion?(err)
        }
    }
    
    public func startLinkingInteraction(userId: String, completion: ((NSError?)->())?) {
        //TODO: The arbitrator needs to check the Interaction form.
        /*
         The arbitrator needs to test the following situations
         When updating, judge whether Lianmai exists or not.
         */
        guard let scene = syncManager.getScene(channelName: channelName),
              let userInfo = scene.userService.userList.first(where: { $0.userId == userId }) else {
            aui_info("startLinkingInteraction fail: apply info is nil", tag: "InteractionService")
            completion?(NSError(domain: "ApplyService",
                               code: 0,
                               userInfo: ["msg": "apply info is nil"]))
            return
        }
        aui_info("startLinkingInteraction userId: \(userId)", tag: "InteractionService")
        let info = InteractionInfo()
        info.type = .linking
        info.roomId = channelName
        info.userId = userId
        info.userName = userInfo.userName
        info.createdAt = syncManager.getScene(channelName: channelName)?.getCurrentTs() ?? 0
        let value = encodeModel(info) ?? [:]
        interactionCollection.addMetaData(valueCmd: InteractionCmd.startLink.rawValue,
                                          value: value) { err in
            aui_info("startLinkingInteraction userId: \(userId) completion: \(err?.localizedDescription ?? "success")", tag: "InteractionService")
            completion?(err)
        }
    }
    
    public func stopInteraction(completion: ((NSError?)->())?) {
        //TODO: You can't directly remove the key'. You need to update with conditions.
        /*
         The arbitrator needs to test the following situations
         After receiving a stop, you need to compare whether the current stop-initiating object is a user in the interaction, and if not, it is rejected.
         Prevent this kind of extreme situation from happening
         1. The owner A and the audience B connected the microphone
         2. B The network is disconnected or the network is not good.
         3. A ends and B connects the microphone
         4. Audience C and A connected microphones
         5. B The network has been restored, but the continuous microphone has not been changed yet.
         6. B initiates the end of the continuous microphone, which will cause the end of the continuous microphone of A and C.
         
         */
        
        aui_info("stopInteraction", tag: "InteractionService")
        let info = InteractionInfo()
        let value = encodeModel(info) ?? [:]
        interactionCollection.addMetaData(valueCmd: InteractionCmd.stop.rawValue,
                                          value: value) { err in
            aui_info("stopInteraction completion: \(err?.localizedDescription ?? "success")", tag: "InteractionService")
            completion?(err)
        }
    }
    
    public func getLatestInteractionInfo(completion: ((NSError?, InteractionInfo?)->())?) {
        let channelName = channelName
        aui_info("getLatestInteractionInfo[\(channelName)]", tag: "InteractionService")
        interactionCollection.getMetaData { err, value in
            aui_info("getLatestInteractionInfo[\(channelName)] completion: \(err?.localizedDescription ?? "success")", tag: "InteractionService")
            var info: InteractionInfo? = nil
            if let value = value as? [String: Any] {
                info = decodeModel(value)
            }
            completion?(err, info)
        }
    }
}

extension InteractionService: AUIUserRespDelegate {
    public func onRoomUserSnapshot(roomId: String, userList: [AUIUserInfo]) {
    }
    
    public func onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
    }
    
    public func onRoomUserLeave(roomId: String, userInfo: AUIUserInfo, reason: AUIRtmUserLeaveReason) {
        guard interactionInfo?.userId == userInfo.userId, reason == .normal else { return }
        aui_info("onRoomUserLeave roomId: \(roomId) userId: \(userInfo.userId)", tag: "InteractionService")
        stopInteraction(completion: nil)
    }
    
    public func onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
    }
    
    public func onUserAudioMute(userId: String, mute: Bool) {
    }
    
    public func onUserVideoMute(userId: String, mute: Bool) {
    }
    
    public func onUserBeKicked(roomId: String, userId: String) {
        guard interactionInfo?.userId == userId else { return }
        aui_info("onUserBeKicked roomId: \(roomId) userId: \(userId)", tag: "InteractionService")
        stopInteraction(completion: nil)
    }
}
