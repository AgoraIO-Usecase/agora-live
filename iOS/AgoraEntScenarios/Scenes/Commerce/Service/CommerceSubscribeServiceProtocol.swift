//
//  ShowSubscribeServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/27.
//

import Foundation

public enum CommerceServiceConnectState: Int {
    case connecting = 0
    case open = 1
    case fail = 2
    case closed = 3
}

protocol CommerceSubscribeServiceProtocol: NSObject {
    
    
    /// Room expired
    func onRoomExpired()
    
    /// Network state change
    /// - Parameter state: <#state description#>
    func onConnectStateChanged(state: CommerceServiceConnectState)
    
    /// The number of users in the room changes
    /// - Parameter userCount: <#userCount description#>
    func onUserCountChanged(userCount: Int)
    
    /// User join room
    /// - Parameter user: <#user description#>
    func onUserJoinedRoom(user: CommerceUser)
    
    /// User leaves room
    /// - Parameter user: <#user description#>
    func onUserLeftRoom(user: CommerceUser)
    
    
    
    /// A new message was received
    /// - Parameter message: <#message description#>
    func onMessageDidAdded(message: CommerceMessage)
    
    
    
    /// The application for Line was received
    /// - Parameter apply: <#apply description#>
    func onMicSeatApplyUpdated(apply: CommerceMicSeatApply)
    
    /// Cancel the connection request
    /// - Parameter apply: <#apply description#>
    func onMicSeatApplyDeleted(apply: CommerceMicSeatApply)
    
    /// Agree to Line's application
    /// - Parameter apply: <#apply description#>
    func onMicSeatApplyAccepted(apply: CommerceMicSeatApply)
    
    /// The application for line was denied
    /// - Parameter apply: <#apply description#>
    func onMicSeatApplyRejected(apply: CommerceMicSeatApply)
    
    
    /// Received a Lien invitation/Invitation was updated
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationUpdated(invitation: CommerceMicSeatInvitation)
    
    /// Cancel the Link invitation
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationDeleted(invitation: CommerceMicSeatInvitation)
    
    /// Agree to Line's invitation
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationAccepted(invitation: CommerceMicSeatInvitation)
    
    /// Turn down an invitation to Line
    /// - Parameter invitation: <#invitation description#>
    func onMicSeatInvitationRejected(invitation: CommerceMicSeatInvitation)
    
    
    
    /// PK invitation received/Invitation updated
    /// - Parameter invitation: <#invitation description#>
    func onPKInvitationUpdated(invitation: CommercePKInvitation)

    /// Agree to PK invitation
    /// - Parameter invitation: <#invitation description#>
    func onPKInvitationAccepted(invitation: CommercePKInvitation)
    
    /// Decline PK invitation
    /// - Parameter invitation: <#invitation description#>
    func onPKInvitationRejected(invitation: CommercePKInvitation)
    
    
    
    /// Receive a pk/ Lien interaction/update
    /// - Parameter interation: <#interation description#>
    func onInteractionBegan(interaction: CommerceInteractionInfo)
    
    /// pk/ Connect is stopped
    /// - Parameter interaction: <#interaction description#>
    func onInterationEnded(interaction: CommerceInteractionInfo)
    
    
    /// pk/ Lien was updated (muted, etc.)
    /// - Parameter interaction: <#interaction description#>
    func onInterationUpdated(interaction: CommerceInteractionInfo)
}
