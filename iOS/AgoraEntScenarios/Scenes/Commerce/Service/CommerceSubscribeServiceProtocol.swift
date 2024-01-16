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
}
