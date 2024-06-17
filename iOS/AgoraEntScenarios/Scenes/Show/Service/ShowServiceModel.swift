//
//  ShowServiceModel.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/2.
//

import Foundation
import VideoLoaderAPI

@objc enum ShowRoomStatus: Int {
    case activity = 0
    case end = 1
}

@objc enum ShowRoomRequestStatus: Int {
    case idle = 0
    case waitting = 1
    case accepted = 2
    case rejected = 3
    case ended = 4
}

@objc enum ShowInteractionStatus: Int {
    case idle = 0
    case onSeat = 1
    case pking = 2
    
    var toastTitle: String {
        switch self {
        case .idle: return ""
        case .onSeat: return "show_end_broadcasting".show_localized
        case .pking: return "show_end_pk".show_localized
        }
    }
    
    var isInteracting: Bool {
        switch self {
        case .onSeat, .pking:
            return true
        default:
            return false
        }
    }
}

@objcMembers
class ShowBaseInfo: NSObject {
    var objectId: String?
}

/// Room list information
@objcMembers
class ShowRoomListModel: ShowBaseInfo, IVideoLoaderRoomInfo {
    func channelName() -> String {
        return roomId
    }
    
    func userId() -> String {
        return ownerId
    }
    
    var anchorInfoList: [AnchorInfo] {
        get {
            let anchorInfo = AnchorInfo()
            anchorInfo.channelName = roomId
            if !ownerId.isEmpty {
                anchorInfo.uid = UInt(ownerId)!
            }
            anchorInfo.token = AppContext.shared.rtcToken ?? ""
            
            return [anchorInfo] + interactionAnchorInfoList
        }
    }
    
    var interactionAnchorInfoList: [AnchorInfo] = []
    
    var roomId: String = ""
    var roomName: String?
    var roomUserCount: Int = 1
    var thumbnailId: String?
    var ownerId: String = ""
    var ownerAvatar: String?
    var ownerName: String?
    var roomStatus: ShowRoomStatus = .activity
    var interactStatus: ShowInteractionStatus = .idle
    var createdAt: Int64 = 0
    var updatedAt: Int64 = 0
    var isPureMode: Int64 = 0
}

//PK Invite objects
typealias ShowPKUserInfo = ShowRoomListModel


/// Room details
@objcMembers
class ShowRoomDetailModel: ShowRoomListModel {
}

@objcMembers
class ShowUser: ShowBaseInfo {
    var userId: String = ""
    var avatar: String?
    var userName: String?
    var status: ShowRoomRequestStatus = .idle
}

@objcMembers
class ShowMessage: ShowBaseInfo {
    var userId: String = ""
    var userName: String?
    var message: String?
    var createAt: Int64 = 0
}

class ShowMicSeatApply: ShowUser {
//    var userId: String?
//    var avatar: String?
//    var userName: String?
//    var status: ShowRoomRequestStatus = .idle
    var createdAt: Int64 = 0
    
    #if DEBUG
    override var description: String {
        return "userId: \(userId) status: \(status) objectId: \(objectId ?? "")"
    }
    #endif
}


typealias ShowMicSeatInvitation = ShowUser

class ShowPKInvitation: ShowBaseInfo {
    var userId: String = ""
    var userName: String?
    var roomId: String = ""
    var fromUserId: String = ""
    var fromName: String?
    var fromRoomId: String = ""
    var status: ShowRoomRequestStatus = .waitting
    var userMuteAudio: Bool = false
    var fromUserMuteAudio: Bool = false
    var createdAt: Int64 = 0
    
    override func isEqual(_ object: Any?) -> Bool {
        guard let info = object as? ShowPKInvitation,
              userId == info.userId,
              userName == info.userName,
              roomId == info.roomId,
              fromUserId == info.fromUserId,
              fromName == info.fromName,
              fromRoomId == info.fromRoomId,
              status == info.status,
              userMuteAudio == info.userMuteAudio,
              fromUserMuteAudio == info.fromUserMuteAudio,
              createdAt == info.createdAt else {
            return false
        }
        
        return true
    }
    
    #if DEBUG
    override var description: String {
        return "userId: \(userId) roomId: \(roomId) fromUserId: \(fromUserId) fromRoomId: \(fromRoomId) status: \(status) objectId: \(objectId ?? "")"
    }
    #endif
}

class ShowInteractionInfo: ShowBaseInfo {
    var userId: String = ""
    var userName: String?
    var roomId: String = ""
    var interactStatus: ShowInteractionStatus = .idle
    var muteAudio: Bool = false
    var ownerMuteAudio: Bool = false
    var createdAt: Int64 = 0                            
    
    override var description: String {
        return "userId: \(userId) userName: \(userName ?? "") roomId: \(roomId) status: \(interactStatus.rawValue) objectId: \(objectId ?? "")"
    }
    
    override func isEqual(_ object: Any?) -> Bool {
        guard let info = object as? ShowInteractionInfo,
              userId == info.userId,
              roomId == info.roomId,
              interactStatus == info.interactStatus else {
            return false
        }
        
        return true
    }
}
