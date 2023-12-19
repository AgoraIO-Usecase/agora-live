//
//  ShowServiceModel.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/2.
//

import Foundation


@objc enum CommerceRoomStatus: Int {
    case activity = 0
    case end = 1
}

@objc enum CommerceRoomRequestStatus: Int {
    case idle = 0
    case waitting = 1
    case accepted = 2
    case rejected = 3
    case ended = 4
}

@objc enum CommerceInteractionStatus: Int {
    case idle = 0
    case onSeat = 1
    case pking = 2
    
    var toastTitle: String {
        switch self {
        case .idle: return ""
        case .onSeat: return "show_end_broadcasting".commerce_localized
        case .pking: return "show_end_pk".commerce_localized
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
class CommerceBaseInfo: NSObject {
    var objectId: String?
}

/// Room list information
@objcMembers
class CommerceRoomListModel: CommerceBaseInfo {
    var roomId: String = ""
    var roomName: String?
    var roomUserCount: Int = 1
    var thumbnailId: String?
    var ownerId: String = ""
    var ownerAvatar: String?
    var ownerName: String?
    var roomStatus: CommerceRoomStatus = .activity
    var interactStatus: CommerceInteractionStatus = .idle
    var createdAt: Int64 = 0
    var updatedAt: Int64 = 0
}

//PK Invite objects
typealias CommercePKUserInfo = CommerceRoomListModel


/// Room details
@objcMembers
class CommerceRoomDetailModel: CommerceRoomListModel {
}

@objcMembers
class CommerceUser: CommerceBaseInfo {
    var userId: String = ""
    var avatar: String?
    var userName: String?
    var status: CommerceRoomRequestStatus = .idle
}

@objcMembers
class CommerceMessage: CommerceBaseInfo {
    var userId: String = ""
    var userName: String?
    var message: String?
    var createAt: Int64 = 0
}

class CommerceMicSeatApply: CommerceUser {
    var createdAt: Int64 = 0
    
    #if DEBUG
    override var description: String {
        return "userId: \(userId) status: \(status) objectId: \(objectId ?? "")"
    }
    #endif
}


typealias CommerceMicSeatInvitation = CommerceUser

class CommercePKInvitation: CommerceBaseInfo {
    var userId: String = ""
    var userName: String?
    var roomId: String = ""
    var fromUserId: String = ""
    var fromName: String?
    var fromRoomId: String = ""
    var status: CommerceRoomRequestStatus = .waitting
    var userMuteAudio: Bool = false
    var fromUserMuteAudio: Bool = false
    var createdAt: Int64 = 0
    
    override func isEqual(_ object: Any?) -> Bool {
        guard let info = object as? CommercePKInvitation,
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

class CommerceInteractionInfo: CommerceBaseInfo {
    var userId: String = ""
    var userName: String?
    var roomId: String = ""
    var interactStatus: CommerceInteractionStatus = .idle
    var muteAudio: Bool = false
    var ownerMuteAudio: Bool = false
    var createdAt: Int64 = 0                            
    
    #if DEBUG
    override var description: String {
        return "userId: \(userId) roomId: \(roomId) status: \(interactStatus) objectId: \(objectId ?? "")"
    }
    #endif
    
    override func isEqual(_ object: Any?) -> Bool {
        guard let info = object as? CommerceInteractionInfo,
              userId == info.userId,
              roomId == info.roomId,
              interactStatus == info.interactStatus else {
            return false
        }
        
        return true
    }
}
