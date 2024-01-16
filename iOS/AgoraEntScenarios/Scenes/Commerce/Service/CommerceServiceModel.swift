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
    var createdAt: Int64 = 0
    var updatedAt: Int64 = 0
}

/// Room details
@objcMembers
class CommerceRoomDetailModel: CommerceRoomListModel {
}

@objcMembers
class CommerceUser: CommerceBaseInfo {
    var userId: String = ""
    var avatar: String?
    var userName: String?
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
        return "userId: \(userId) objectId: \(objectId ?? "")"
    }
    #endif
}
