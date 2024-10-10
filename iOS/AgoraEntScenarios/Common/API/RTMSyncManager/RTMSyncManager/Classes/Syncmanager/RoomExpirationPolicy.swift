//
//  RoomExpirationPolicy.swift
//  RTMSyncManager
//
//  Created by wushengtao on 2024/5/6.
//

import Foundation

// Room expiration strategy model
public class RoomExpirationPolicy: NSObject {
    // Room expiration time, unit ms, 0 means that the room has not expired.
    public var expirationTime: UInt64 = 0
    
    /// The landlord is offline for the longest time, the unit ms, after that time is considered that the room has been destroyed, 0 means not to deal with it.
    public var ownerReconnectMaxTime: UInt64 = 5 * 60 * 1000
    
    // Whether to be offline with the landlord, true: If the owner is not online, the room will be destroyed, false: If the owner is not online, the room will not be destroyed.
    public var isAssociatedWithOwnerOffline: Bool = true
    
    public static func defaultPolicy() -> RoomExpirationPolicy {
        let policy = RoomExpirationPolicy()
        policy.expirationTime = 20 * 60 * 1000
        
        return policy
    }
}
