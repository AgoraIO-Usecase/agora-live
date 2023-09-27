//
//  TemplateServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/18.
//

import Foundation
import AgoraSyncManager_overseas

protocol TemplateServiceProtocol: NSObjectProtocol {
    /// Join the room
    /// - Parameters:
    /// -roomName: indicates the room name
    ///   - completion: <#completion description#>
    func join(roomName: String, completion: @escaping (SyncError?, TemplateScene.JoinResponse?) -> Void)

    /// Leave the room (Audience)
    func leave()

    /// Delete room (Anchor)
    func removeRoom()

    /// Add  user
    /// - Parameters:
    ///   - user: <#user description#>
    ///   - completion: <#completion description#>
    func addUser(user: TemplateScene.UsersModel, completion: @escaping (SyncError?, TemplateScene.UsersModel?) -> Void)

    /// Delete a user
    /// - Parameters:
    ///   - user: <#user description#>
    ///   - completion: <#completion description#>
    func removeUser(user: TemplateScene.UsersModel, completion: @escaping (SyncError?, [TemplateScene.UsersModel]?) -> Void)

    /// Modifying User Information
    /// - Parameters:
    ///   - user: <#user description#>
    ///   - completion: <#completion description#>
    func updateUser(user: TemplateScene.UsersModel, completion: @escaping (SyncError?, TemplateScene.UsersModel?) -> Void)

    /// Get all users
    /// - Parameter completion: <#completion description#>
    func getUserStatus(completion: @escaping (SyncError?, [TemplateScene.UsersModel]?) -> Void)

    /// Monitor room changes
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    ///   - onSubscribed: <#onSubscribed description#>
    ///   - fail: <#fail description#>
    func subscribeRoom(subscribeClosure: @escaping (TemplateScene.SubscribeStatus, TemplateScene.LiveRoomInfo?) -> Void,
                       onSubscribed: (() -> Void)?,
                       fail: ((SyncError) -> Void)?)

    /// Monitor user changes
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    ///   - onSubscribed: <#onSubscribed description#>
    ///   - fail: <#fail description#>
    func subscribeUser(subscribeClosure: @escaping (TemplateScene.SubscribeStatus, TemplateScene.UsersModel?) -> Void,
                       onSubscribed: (() -> Void)?,
                       fail: ((SyncError) -> Void)?)

    /// unlisten
    func unsubscribe()
}
