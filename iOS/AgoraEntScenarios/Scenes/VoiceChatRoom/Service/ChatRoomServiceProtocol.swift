//
//  TemplateServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/18.
//

import Foundation

public enum updateRoomState {
    case activeAlien
    case announcement
    case robotVoleme
}

@objc public enum ChatRoomServiceConnectState: Int {
    case connecting = 0
    case open = 1
    case fail = 2
    case closed = 3
}

@objc public enum ChatRoomServiceKickedReason: UInt {
    case removed
    case destroyed
    case offLined
    
    func errorDesc() -> String {
        switch self {
        case .removed:
            return "voice_you_were_kicked_off_from_the_room".voice_localized
        case .destroyed:
            return "voice_this_room_has_been_dissolved_by_the_host".voice_localized
        case .offLined:
            return "voice_you_were_offline".voice_localized
        default:
            return ""
        }
    }
}

@objc public protocol ChatRoomServiceSubscribeDelegate: NSObjectProtocol {
    
    ///Room expired
    func onRoomExpired()
    
    ///Changes in network status
    /// - Parameter state:
    func onConnectStateChanged(state: ChatRoomServiceConnectState)
    
    ///Description token expired
    func chatTokenWillExpire()
    
    ///Description Received text message
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- message: message model
    func receiveTextMessage(roomId: String, message: VoiceRoomChatEntity)
    ///Description Received gift message
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- Meta: Transparent transmission of information
    func onReceiveGift(roomId: String, gift: VoiceRoomGiftEntity)
    ///Description Received an online application message to replace the receiveApplySite
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- Meta: Transparent transmission of information
    func onReceiveSeatRequest(roomId: String, applicant: VoiceRoomApply)
    
    ///Description Received a message to cancel the online application and replace the receiveCancelApplySite
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- Chat_ Uid: Environmental Information IMSDK User ID
    func onReceiveSeatRequestRejected(roomId: String, chat_uid: String)
    
    ///Description Received invitation message
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- Meta: Transparent transmission of information
    func onReceiveSeatInvitation(roomId: String, user: VRUser)
    
    ///Description Received invitation message
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- Meta: Transparent transmission of information
    func onReceiveCancelSeatInvitation(roomId: String, chat_uid: String)
    
    ///Description: User joining chat room callback with all user information
    /// - Parameters:
    ///- roomId: Chat room ID
    ///- username: User UID or username of the user in the Huanxin IMSDK
    ///- ext: User JSONObject Information
    func onUserJoinedRoom(roomId: String, user: VRUser)
    
    ///Description chat room announcement has changed
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- Content: Announcement changes
    func onAnnouncementChanged(roomId: String, content: String)
    
    ///Description User Kicked
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- Reason: Reason for being kicked
    func onUserBeKicked(roomId: String, reason: ChatRoomServiceKickedReason)
    
    ///Description: The custom microphone properties in the chat room have changed
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- attributeMap: The transformed attribute kv
    ///- From Id: Who operated the change that occurred
    func onSeatUpdated(roomId: String, mics: [VRRoomMic], from fromId: String)
    
    ///Description Robot
    /// - Parameters:
    ///- roomId: Chat room ID
    ///- enable: Robot switch change
    ///- From Id: Operator userName
    func onRobotSwitch(roomId: String, enable: Bool, from fromId: String)
    
    ///Description Robot
    /// - Parameters:
    ///- roomId: Chat room ID
    ///- Volume: Robot switch changes
    ///- From Id: Operator userName
    func onRobotVolumeChanged(roomId: String, volume: UInt, from fromId: String)
    
    ///Changes in the Description Contribution Ranking
    /// - Parameters:
    ///- roomId: Chat room ID
    ///- Ranking_ List: Ranking list
    ///- From Id: Operator userName
    func onContributionListChanged(roomId: String, ranking_list: [VRUser], from fromId: String)
    
    /// Description
    /// - Parameters: watched count changed
    ///   - roomId: Chat room ID
    ///   - count: watched count
    func onClickCountChanged(roomId: String, count: Int)
    
    /// Description
    /// - Parameters: member in room count changed
    ///   - roomId: Chat room ID
    ///   - count: member count
    func onMemberCountChanged(roomId: String, count: Int)
    
    ///Description member leaves
    /// - Parameters:
    ///- roomId: Huanxin IMSDK chat room ID
    ///- userName: The ID of the outgoing environmental information user
    func onUserLeftRoom(roomId: String, userName: String)
    
}

///Environmental protection KV is required inside the room
protocol ChatRoomServiceProtocol: NSObjectProtocol {
    ///Register subscription
    ///- Parameter delegate: ChatRoomServiceSubscribeDelegate IM callback processing in the chat room
    func subscribeEvent(with delegate: ChatRoomServiceSubscribeDelegate)
        
    ///Unsubscribe
    ///- Parameter delegate: ChatRoomServiceSubscribeDelegate IM callback processing in the chat room
    func unsubscribeEvent()

    ///Join Room
    /// - Parameters:
    ///
    func joinRoom(_ roomId: String, completion: @escaping (Error?, VRRoomEntity?) -> Void)
    
    ///Leave the room
    /// - Parameters:
    ///
    func leaveRoom(_ roomId: String, completion: @escaping (Error?, Bool) -> Void)
    
    ///Get room details
    /// - Parameters:
    ///
    func fetchRoomDetail(entity: VRRoomEntity, completion: @escaping (Error?, VRRoomInfo?) -> Void)

    ///Get gift list
    /// - Parameters:
    ///
    func fetchGiftContribute(completion: @escaping (Error?, [VRUser]?) -> Void)
    ///Get personnel list
    /// - Parameters:
    ///
    func fetchRoomMembers(completion: @escaping (Error?, [VRUser]?) -> Void)
    
    /// Description update room members
    /// - Parameter completion: callback
    func updateRoomMembers(completion: @escaping (Error?) -> Void)
    
    ///Description Application List
    ///- Parameter completion: Callback
    func fetchApplicantsList(completion: @escaping (Error?, [VoiceRoomApply]?) -> Void)

    ///Prohibition of designated microphone positions
    /// - Parameters:
    ///
    func forbidMic(mic_index: Int,
                   completion: @escaping (Error?, VRRoomMic?) -> Void)

    ///Cancel the ban on designated microphone slots
    /// - Parameters:
    ///
    func unForbidMic(mic_index: Int,
                     completion: @escaping (Error?, VRRoomMic?) -> Void)

    ///Lock wheat
    /// - Parameters:
    ///
    func lockMic(mic_index: Int,
                 completion: @escaping (Error?, VRRoomMic?) -> Void)

    ///Unlock microphone
    /// - Parameters:
    ///
    func unLockMic(mic_index: Int,
                   completion: @escaping (Error?, VRRoomMic?) -> Void)

    ///Kicking users off the microphone
    /// - Parameters:
    ///
    func kickOff(mic_index: Int,
                 completion: @escaping (Error?, VRRoomMic?) -> Void)

    ///Xiamai
    /// - Parameters:
    ///
    func leaveMic(mic_index: Int,
                  completion: @escaping (Error?, VRRoomMic?) -> Void)

    /// mute
    /// - Parameters:
    ///
    func muteLocal(mic_index: Int,completion: @escaping (Error?, VRRoomMic?) -> Void)

    /// unmute
    /// - Parameters:
    ///
    func unmuteLocal(mic_index: Int, completion: @escaping (Error?, VRRoomMic?) -> Void)
    
    
    ///Description: Update the current microphone user's microphone status
    /// - Parameters:
    ///- Status: 0 Autonomous silent microphone 1 Active on microphone
    ///- completion: Callback
    func changeMicUserStatus(status: Int,completion: @escaping (Error?, VRRoomMic?) -> Void)
    ///Swapping wheat
    /// - Parameters:
    ///
    func changeMic(old_index: Int,new_index:Int,completion: @escaping (Error?, [Int:VRRoomMic]?) -> Void)
    
    ///Invite to sell
    /// - Parameters:
    ///
    func startMicSeatInvitation(chatUid: String,index: Int?,completion: @escaping (Error?, Bool) -> Void)

    ///Accept invitation
    /// - Parameters:
    ///
    func acceptMicSeatInvitation(index: Int?,completion: @escaping (Error?, VRRoomMic?) -> Void)
    
    ///Refuse invitation
    /// - Parameters:
    ///
    func refuseInvite(chat_uid: String,completion: @escaping (Error?, Bool) -> Void)

    ///Apply for access to the microphone
    /// - Parameters:
    ///
    func startMicSeatApply(index: Int?,completion: @escaping (Error?, Bool) -> Void)

    ///Cancel the wheat feeding
    /// - Parameters:
    ///
    func cancelMicSeatApply(chat_uid: String,
                     completion: @escaping (Error?, Bool) -> Void)
    
    ///Description Agreed to apply
    /// - Parameters:
    ///   - user: VRUser instance
    ///- completion: Callback
    func acceptMicSeatApply(chatUid: String, completion: @escaping (Error?,VRRoomMic?) -> Void)

    ///Get room list
    /// - Parameters:
    ///- Page: pagination index, starting from 0 (this attribute is temporarily invalid due to SyncManager's inability to perform pagination)
    ///- completion: Complete callback (error message, room list)
    func fetchRoomList(page: Int, completion: @escaping (Error?, [VRRoomEntity]?) -> Void)
    
    ///Create a room
    /// - Parameters:
    ///- room: Room object information
    ///- completion: Complete callback (error message)
    func createRoom(room: VRRoomEntity, completion: @escaping (Error?, VRRoomEntity?) -> Void)
    
    ///Description Update Announcement
    /// - Parameters:
    ///   - content: content
    ///- completion: Callback
    func updateAnnouncement(content: String,completion: @escaping (Bool) -> Void)
    
    ///Is the description enabled for robots
    /// - Parameter enable: true or false
    func enableRobot(enable: Bool,completion: @escaping (Error?) -> Void)
    
    ///Update robot volume in Description
    ///- Parameter value: Volume value
    func updateRobotVolume(value: Int,completion: @escaping (Error?) -> Void)
    
    ///Update room background music in Description
    ///- Parameter songName: Song name
    ///- Parameter singer Name: Singer
    ///- Parameter isOrigin: Is it playing
    func updateRoomBGM(songName: String?, singerName: String?, isOrigin: Bool)
    
    ///Description: Monitor room background music changes
    ///- Parameter roomId: Room ID
    ///- Parameter completion: Callback
    func subscribeRoomBGMChange(roomId: String?, completion: @escaping (_ songName: String?, _ singerName: String?, _ isPlaying: Bool) -> Void)
}
