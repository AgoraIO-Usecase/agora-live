//
//  ShowServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/2.
//

import Foundation

enum CommerceSubscribeStatus {
    case created
    case deleted
    case updated
}

protocol CommerceServiceProtocol: NSObjectProtocol {
    /// Get room list
    /// - Parameters:
    /// -page: page index, starting from 0 (this property is temporarily invalid because SyncManager cannot page)
    /// - completion: completion callback (error message, room list)
    func getRoomList(page: Int,
                     completion: @escaping (NSError?, [CommerceRoomListModel]?) -> Void)
    
    
    /// Create a room
    /// - Parameters:
    /// -roomName: indicates the room name
    /// -roomId: indicates the roomId
    /// - thumbnailId: thumbnail of the list
    ///   - completion: <#completion description#>
    func createRoom(roomName: String,
                    roomId: String,
                    thumbnailId: String,
                    completion: @escaping (NSError?, CommerceRoomDetailModel?) -> Void)
    
    /// Join the room
    /// - Parameters:
    /// -room: indicates room object information
    /// - completion: completion callback (error message, room message)
    func joinRoom(room: CommerceRoomListModel,
                  completion: @escaping (NSError?, CommerceRoomDetailModel?) -> Void)
    
    /// Leave the room
    func leaveRoom(completion: @escaping (NSError?) -> Void)
    
    
    /// Initializes the room information
    /// - Parameter completion: <#completion description#>
    func initRoom(roomId: String?, completion: @escaping (NSError?) -> Void)
    
    
    /// Release the initialization room information
    /// - Parameter completion: <#completion description#>
    func deinitRoom(roomId: String?, completion: @escaping (NSError?) -> Void)
    
    
    /// Gets all users in the current room
    /// - Parameter completion: Completed callback (error message, user list)
    func getAllUserList(completion: @escaping (NSError?, [CommerceUser]?) -> Void)
    
    /// Send a chat message
    /// - Parameters:
    ///   - roomId: roomId description
    ///   - message: message description
    ///   - completion: completion description
    func sendChatMessage(roomId: String?,
                         message: CommerceMessage,
                         completion: ((NSError?) -> Void)?)
    
    func unsubscribeEvent(delegate: CommerceSubscribeServiceProtocol)
    
    func subscribeEvent(delegate: CommerceSubscribeServiceProtocol)
    
    func getBidGoodsInfo(roomId: String?, completion: @escaping (NSError?, CommerceGoodsAuctionModel?) -> Void)
    
    func addBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel, completion: @escaping (NSError?) -> Void)
    
    func updateBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel, completion: @escaping (NSError?) -> Void)
    
    func cleanBidGoodsInfo(roomId: String?, completion: @escaping (NSError?) -> Void)
    
    func subscribeBidGoodsInfo(roomId: String?, completion: @escaping (NSError?, CommerceGoodsAuctionModel?) -> Void)
}
