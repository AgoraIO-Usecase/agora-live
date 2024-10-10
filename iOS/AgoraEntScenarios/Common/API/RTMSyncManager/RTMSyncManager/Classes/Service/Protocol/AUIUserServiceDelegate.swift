//
//  AUIUserServiceDelegate.swift
//  AUIKit
//
//  Created by wushengtao on 2023/4/7.
//

import Foundation

@objc public protocol AUIUserServiceDelegate: AUICommonServiceDelegate {
    
    /// Binding response
    /// - Parameter delegate: the object that needs to be called back
    func bindRespDelegate(delegate: AUIUserRespDelegate)
    
    /// Unbinding protocol
    /// - Parameter delegate: the object that needs to be called back
    func unbindRespDelegate(delegate: AUIUserRespDelegate)
    
    /// Get all user information of the specified channel
    /// - Parameters:
    /// - roomId: room id
    /// - callback: Operation completed callback
    func getUserInfoList(roomId: String, callback: @escaping AUIUserListCallback)
    
    /// Mute yourself/unmute yourself
    /// - Parameters:
    /// - isMute: true: Turn off the microphone false: Turn on the microphone
    /// - callback: Operation completed callback
    func muteUserAudio(isMute: Bool, callback: @escaping AUICallback)
    
    /// Ban the camera to yourself/Unban the camera
    /// - Parameters:
    /// - isMute: true: Turn off the camera false: Turn on the camera
    /// - callback: Operation completed callback
    func muteUserVideo(isMute: Bool, callback: @escaping AUICallback)
    
    /// Description Kick out the user
    /// - Parameters:
    /// - roomId: room id
    /// - userId: Kicked user id
    func kickUser(roomId: String ,userId: String, callback: @escaping AUICallback)
}

@objc public protocol AUIUserRespDelegate: NSObjectProtocol {
    
    /// All user information obtained after the user enters the room
    /// - Parameters:
    /// - roomId: room id
    /// - userList: User List
    func onRoomUserSnapshot(roomId: String, userList: [AUIUserInfo])
    
    /// The user enters the room to call back
    /// - Parameters:
    /// - roomId: room id
    /// - userInfo: User Information
    func onRoomUserEnter(roomId: String, userInfo: AUIUserInfo)
    
    /// The user leaves the room to call back
    /// - Parameters:
    /// - roomId: room id
    /// - userInfo: User Information
    /// - reason: The reason for leaving the room
    func onRoomUserLeave(roomId: String, userInfo: AUIUserInfo, reason: AUIRtmUserLeaveReason)
    
    /// The user's information has been modified
    /// - Parameters:
    /// - roomId: room id
    /// - userInfo: User Information
    func onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo)
    
    /// The user turned off/on the microphone
    /// - Parameters:
    /// - userId: user id
    /// - mute: mute state
    func onUserAudioMute(userId: String, mute: Bool)
    
    /// The user turned off/on the camera
    /// - Parameters:
    /// - userId: user id
    /// - mute: Camera Status
    func onUserVideoMute(userId: String, mute: Bool)
    
    /// Description The user was kicked out of the room
    /// - Parameters:
    /// - roomId: room id
    /// - userId: user id
    func onUserBeKicked(roomId: String, userId: String)
    
}
