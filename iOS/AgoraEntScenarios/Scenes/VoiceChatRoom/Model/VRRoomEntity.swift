//
//  VRRoomEntity.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 24, 2022
//

import Foundation
import KakaJSON
import RTMSyncManager

@objc public class VRRoomsEntity: NSObject, Convertible {
    public var total: Int? //Total number of rooms
    public var cursor: String? //The cursor for the next room list request
    public var rooms: [VRRoomEntity]? //Room information array

    override public required init() {}

    public func kj_modelKey(from property: Property) -> ModelPropertyKey {
        property.name
    }
}

@objc open class VRRoomEntity: NSObject, Convertible {
    public var room_id: String? //Room ID
    public var channel_id: String? // agora rtc channel id
    public var chatroom_id: String? // agora chat chatroom id
    @objc public var name: String? //Room Name
    public var member_count: Int? = 0 //Number of people in the room
    public var gift_amount: Int? = 0 //Number of gifts
    public var owner: VRUser?
    @objc public var is_private: Bool = false //Is it a private room
    public var type: Int? = 0 //Room type, 0: Normal room, 1:3D room
    public var created_at: UInt? //Create room timestamp in milliseconds
    @objc public var roomPassword = ""
    public var click_count: Int? = 0 //Number of viewers
    public var announcement: String? // Notice
    public var ranking_list: [VRUser]? = [VRUser]() //Rich List
    public var member_list: [VRUser]? = [VRUser]() //User List
    public var rtc_uid: Int? = 0
    public var use_robot: Bool? = false
    public var turn_AIAEC: Bool? = false
    public var turn_AGC: Bool? = false
    public var turn_InEar: Bool? = false
    public var inEar_volume: Int = 100
    public var inEarMode: String?
    public var robot_volume: UInt?
    public var sound_effect: Int = 1
    public var musicIsOrigin: Bool = false 
    var backgroundMusic: VoiceMusicModel?

    override public required init() {}

    public func kj_modelKey(from property: Property) -> ModelPropertyKey {
        property.name
    }
}

extension AUIRoomInfo {
    
    func voice_toRoomEntity() -> VRRoomEntity {
        let model = VRRoomEntity()
        model.room_id = roomId
        model.name = roomName
        let chatRoomOwner = VRUser()
        chatRoomOwner.name = owner?.userName ?? ""
        chatRoomOwner.portrait = owner?.userAvatar ?? ""
        chatRoomOwner.uid = owner?.userId ?? ""
        chatRoomOwner.chat_uid = owner?.userId ?? ""
        chatRoomOwner.rtc_uid = owner?.userId ?? ""
        model.owner = chatRoomOwner
        model.created_at = UInt(createTime)
        model.channel_id = roomId
        model.chatroom_id = customPayload["chatroom_id"] as? String
        model.is_private = customPayload["is_private"] as? Bool ?? false
        model.roomPassword = customPayload["roomPassword"] as? String ?? ""
        model.rtc_uid = Int(owner?.userId ?? "") ?? 0
        model.sound_effect = customPayload["sound_effect"] as? Int ?? 1
        model.member_count = customPayload["member_count"] as? Int ?? 1
        return model
    }
    
    static func voice_fromVRRoomEntity(_ model: VRRoomEntity) -> AUIRoomInfo {
        let roomInfo = AUIRoomInfo()
        roomInfo.roomId = model.room_id ?? ""
        roomInfo.roomName = model.name ?? ""
        let owner = AUIUserThumbnailInfo()
        owner.userId = model.owner?.uid ?? ""
        owner.userName = model.owner?.name ?? ""
        owner.userAvatar = model.owner?.portrait ?? ""
        roomInfo.owner = owner
        roomInfo.createTime = Int64(model.created_at ?? 0)
        roomInfo.customPayload["chatroom_id"] = model.chatroom_id
        roomInfo.customPayload["is_private"] = model.is_private
        roomInfo.customPayload["roomPassword"] = model.roomPassword
        roomInfo.customPayload["sound_effect"] = model.sound_effect
        roomInfo.customPayload["member_count"] = model.member_count
        return roomInfo
    }
}

enum VRRoomMicStatus: Int {
    case idle = -1              //Idle
    case normal = 0             //Normal
    case close = 1              //Closed wheat
    case forbidden = 2          //Prohibition
    case lock = 3               //Lock wheat
    case forbiddenAndLock = 4   //Lock while and Prohibition
}

@objc open class VRRoomMic: NSObject, Convertible {
    var mic_index: Int = 0

    var status: Int = 0 //0: Normal state 1: Closed microphone 2: Forbidden speech 3: Locked microphone 4: Locked microphone and Forbidden speech 1: Idle

    var member: VRUser?

    override public required init() {}

    public func kj_modelKey(from property: Property) -> ModelPropertyKey {
        property.name
    }
    
    open override var description: String {
        return "seat: \(mic_index), status: \(status), memeber: \(member?.rtc_uid ?? "empty")"
    }
}

@objc open class VRUsers: NSObject, Convertible {
    var ranking_list: [VRUser]?

    override public required init() {}

    public func kj_modelKey(from property: Property) -> ModelPropertyKey {
        property.name
    }
}

@objc open class VRRoomInfo: NSObject, Convertible {
    var room: VRRoomEntity?
    var mic_info: [VRRoomMic]?

    override public required init() {}

    public func kj_modelKey(from property: Property) -> ModelPropertyKey {
        property.name
    }
}
