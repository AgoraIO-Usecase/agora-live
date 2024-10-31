//
//  AUISceneRespDelegate.swift
//  RTMSyncManager
//
//  Created by wushengtao on 2024/1/25.
//

import Foundation

/// Response corresponding to Scene operation
@objc public protocol AUISceneRespDelegate: NSObjectProtocol {
    
    /// Metadata is about to be updated
    /// - Parameter channelName: <#channelName description#>
    /// - Returns: Returned map object [key: value], key: collection id, value: initialization structure, map is map collection, array is list Collection
    @objc optional func onWillInitSceneMetadata(channelName: String) -> [String: Any]?
    
    /// Token is about to expire.
    /// - Parameter channelName: <#channelName description#>
    @objc optional func onTokenPrivilegeWillExpire(channelName: String?)

    /// The callback of the expired room
    /// - Parameter channelName: <#channelName description#>
    @objc optional func onSceneExpire(channelName: String)
    
    /// The callback of the destroyed room
    /// - Parameter channelName: Room id
    @objc optional func onSceneDestroy(channelName: String)
    
    /// Description The room is abnormal and needs to be exited
    /// - Parameters:
    /// - channelName: room id
    /// - reason: Abnormal reason
    @objc optional func onSceneFailed(channelName: String, reason: String)
}
