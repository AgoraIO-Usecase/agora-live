//
//  ShowSubscribeServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/27.
//

import Foundation

public enum ShowServiceConnectState: Int {
    case connecting = 0
    case open = 1
    case fail = 2
    case closed = 3
}

protocol ShowSubscribeServiceProtocol: NSObject {
    
    
    /// Room expired
    func onRoomExpired()
    
    /// Network state change
    /// - Parameter state: <#state description#>
    func onConnectStateChanged(state: ShowServiceConnectState)
    
    /// The number of users in the room changes
    /// - Parameter userCount: <#userCount description#>
    func onUserCountChanged(userCount: Int)
    
    /// User join room
    /// - Parameter user: <#user description#>
    func onUserJoinedRoom(user: ShowUser)
    
    /// User leaves room
    /// - Parameter user: <#user description#>
    func onUserLeftRoom(user: ShowUser)
    
    
    
    /// A new message was received
    /// - Parameter message: <#message description#>
    func onMessageDidAdded(message: ShowMessage)
    
    
    
    /// The application for Line was received
    /// - Parameter apply: <#apply description#>
    func onMicSeatApplyUpdated(apply: ShowMicSeatApply)
    
    /// Cancel the connection request
    /// - Parameter apply: <#apply description#>
    func onMicSeatApplyDeleted(apply: ShowMicSeatApply)
    
    /// Agree to Line's application
    /// - Parameter apply: <#apply description#>
    func onMicSeatApplyAccepted(apply: ShowMicSeatApply)
    
    /// The application for line was denied
    /// - Parameter apply: <#apply description#>
    func onMicSeatApplyRejected(apply: ShowMicSeatApply)
    
    
    /// Received a Lien invitation/Invitation was updated
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationUpdated(invitation: ShowMicSeatInvitation)
    
    /// Cancel the Link invitation
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationDeleted(invitation: ShowMicSeatInvitation)
    
    /// Agree to Line's invitation
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationAccepted(invitation: ShowMicSeatInvitation)
    
    /// Turn down an invitation to Line
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationRejected(invitation: ShowMicSeatInvitation)
    
    
    
    /// PK invitation received/Invitation updated
    /// - Parameter invitation: <#invitation description#>
    func onPKInvitationUpdated(invitation: ShowPKInvitation)

    /// Agree to PK invitation
    /// - Parameter invitation: <#invitation description#>
    func onPKInvitationAccepted(invitation: ShowPKInvitation)
    
    /// Decline PK invitation
    /// - Parameter invitation: <#invitation description#>
    func onPKInvitationRejected(invitation: ShowPKInvitation)
    
    
    
    /// Receive a pk/ Lien interaction/update
    /// - Parameter interation: <#interation description#>
    func onInteractionBegan(interaction: ShowInteractionInfo)
    
    /// pk/ Connect is stopped
    /// - Parameter interaction: <#interaction description#>
    func onInterationEnded(interaction: ShowInteractionInfo)
    
    
    /// pk/ Lien was updated (muted, etc.)
    /// - Parameter interaction: <#interaction description#>
    func onInterationUpdated(interaction: ShowInteractionInfo)
}
