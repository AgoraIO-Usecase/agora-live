//
//  ShowSubscribeServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/27.
//

import Foundation

@objc public enum ShowServiceConnectState: Int {
    case connecting = 0
    case open = 1
    case fail = 2
    case closed = 3
}

@objc public protocol ShowSubscribeServiceProtocol: NSObjectProtocol {
    
    /// The room is expired.
    func onRoomExpired(channelName: String)
    
    /// The room was destroyed.
    /// - Parameter channelName: <#channelName description#>
    func onRoomDestroy(channelName: String)
    
    /// Changes in network status
    /// - Parameter state: <#state description#>
    func onConnectStateChanged(channelName: String, state: ShowServiceConnectState)
    
    /// Changes in the number of users in the room
    /// - Parameter userCount: <#userCount description#>
    func onUserCountChanged(channelName: String, userCount: Int)
    
    /// Users join the room
    /// - Parameter user: <#user description#>
    func onUserJoinedRoom(channelName: String, user: ShowUser)
    
    /// The user leaves the room.
    /// - Parameter user: <#user description#>
    func onUserLeftRoom(channelName: String, user: ShowUser)
    
    
    
    /// Received new messages
    /// - Parameter message: <#message description#>
    func onMessageDidAdded(channelName: String, message: ShowMessage)
    
    
    /// Changes in the application list of Lianmai
    /// - Parameter apply: <#apply description#>
    func onMicSeatApplyUpdated(channelName: String, applies: [ShowMicSeatApply])

    
    /// Received on seat invitation/the invitation is updated
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationUpdated(channelName: String, invitation: ShowMicSeatInvitation)
    
    /// Agree to on seat invitation
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationAccepted(channelName: String, invitation: ShowMicSeatInvitation)
    
    /// Refuse on seat invitation
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationRejected(channelName: String, invitation: ShowMicSeatInvitation)
    
    
    
    /// Received PK invitation/the invitation was updated
    /// - Parameter invitation: <#invitation description#>
    func onPKInvitationUpdated(channelName: String, invitation: ShowPKInvitation)

    /// Agree to the PK invitation
    /// - Parameter invitation: <#invitation description#>
    func onPKInvitationAccepted(channelName: String, invitation: ShowPKInvitation)
    
    /// Refuse PK invitation
    /// - Parameter invitation: <#invitation description#>
    func onPKInvitationRejected(channelName: String, invitation: ShowPKInvitation)
    
    
    
    /// pk/Lianmai interactive change
    /// - Parameter interation: <#interation description#>
    func onInteractionUpdated(channelName: String, interactions: [ShowInteractionInfo])
}
