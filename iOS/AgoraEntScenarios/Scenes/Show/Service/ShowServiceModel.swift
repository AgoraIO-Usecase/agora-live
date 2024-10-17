//
//  ShowServiceModel.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/2.
//

import Foundation
import VideoLoaderAPI
import RTMSyncManager

typealias ShowInteractionStatus = InteractionType

/// Room list information
@objcMembers
public class ShowRoomListModel: NSObject, IVideoLoaderRoomInfo {
    public func channelName() -> String {
        return roomId
    }
    
    public func userId() -> String {
        return ownerId
    }
    
    public var anchorInfoList: [AnchorInfo] {
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
    
    public var interactionAnchorInfoList: [AnchorInfo] = []
    
    public var roomId: String = ""                                //Room number
    public var roomName: String?                              //Room name
    public var roomUserCount: Int = 1                         //Number of people in the room
    public var ownerId: String = ""                               //Owner user id (rtc uid)
    public var ownerAvatar: String?                           //Owner avatar
    public var ownerName: String?                             //Owner's name
    public var createdAt: Int64 = 0                           //Creation time, the number of milliseconds compared with the time of 19700101
    public var updatedAt: Int64 = 0                           //Update time
    public var isPureMode: Int64 = 0
}

//PK invitee
typealias ShowPKUserInfo = RoomPresenceInfo


/// Room details
public typealias ShowRoomDetailModel = ShowRoomListModel

///User information
public typealias ShowUser = AUIUserInfo

/// Chat messages
@objcMembers
public class ShowMessage: NSObject, Codable {
    var userId: String = ""
    var userName: String?
    var message: String?
    var createAt: Int64 = 0    //Creation time, the number of milliseconds compared with the time of 19700101
    
    
    enum CodingKeys: String, CodingKey {
        case userId, userName, message, createAt
    }
}

/// on seat application
public typealias ShowMicSeatApply = ApplyInfo


/// on seat invitation
public typealias ShowMicSeatInvitation = InvitationInfo

public typealias ShowPKInvitation = PKInfo

//Mic seat/Pk Model
public typealias ShowInteractionInfo = InteractionInfo

extension AUIRoomInfo {
    @objc var roomUserCount: Int {
        set {
            self.customPayload["roomUserCount"]  = newValue
        } get {
            return self.customPayload["roomUserCount"] as? Int ?? 0
        }
    }
    
    func createShowServiceModel() -> ShowRoomListModel {
        let model = ShowRoomListModel()
        model.roomId = roomId
        model.roomName = roomName
        model.roomUserCount = customPayload["roomUserCount"] as? Int ?? 0
        model.ownerId = owner?.userId ?? ""
        model.ownerName = owner?.userName ?? ""
        model.ownerAvatar = owner?.userAvatar ?? ""
        model.createdAt = createTime
        model.updatedAt = createTime
        return model
    }
    
    static func convertFromShowRoomListModel(_ model: ShowRoomListModel) -> AUIRoomInfo {
        let roomInfo = AUIRoomInfo()
        roomInfo.roomId = model.roomId
        roomInfo.roomName = model.roomName ?? ""
        roomInfo.roomUserCount = model.roomUserCount
        let owner = AUIUserThumbnailInfo()
        owner.userId = model.ownerId
        owner.userName = model.ownerName ?? ""
        owner.userAvatar = model.ownerAvatar ?? ""
        roomInfo.owner = owner
        roomInfo.createTime = model.createdAt
        
        return roomInfo
    }
}
