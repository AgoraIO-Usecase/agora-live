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
    
    
    /// Get the current room usage time
    /// - Parameter roomId: <#roomId description#>
    /// - Returns: <#description#>
    func getRoomDuration(roomId: String) -> UInt64
    
    
    /// Get the current timestamp
    /// - Parameter roomId: <#roomId description#>
    /// - Returns: <#description#>
    func getCurrentTs(roomId: String) -> UInt64
    
    
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
    
    func upvote(roomId: String?, count: Int, completion: ((NSError?) -> Void)?)
    
    func subscribeUpvoteEvent(roomId: String?, completion: ((String?, Int) -> Void)?)
    
    func unsubscribeEvent(delegate: CommerceSubscribeServiceProtocol)
    
    func subscribeEvent(delegate: CommerceSubscribeServiceProtocol)
    
    func getBidGoodsInfo(roomId: String?, completion: @escaping (NSError?, CommerceGoodsAuctionModel?) -> Void)
    
    func addBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel, completion: @escaping (NSError?) -> Void)
    
    func endBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel, completion: @escaping (NSError?) -> Void)
    
    func updateBidGoodsInfo(roomId: String?, goods: CommerceGoodsAuctionModel, completion: @escaping (NSError?) -> Void)
        
    func subscribeBidGoodsInfo(roomId: String?, completion: @escaping (NSError?, CommerceGoodsAuctionModel?) -> Void)
    
    func addGoodsList(roomId: String?, goods: [CommerceGoodsModel]?, completion: @escaping (NSError?) -> Void)
    
    func getGoodsList(roomId: String?, completion: @escaping (NSError?, [CommerceGoodsBuyModel]?) -> Void)
    
    func updateGoodsInfo(roomId: String?, goods: CommerceGoodsModel?, completion: @escaping (NSError?) -> Void)
    
    func calcGoodsInfo(roomId: String?, goods: CommerceGoodsModel?, increase: Bool, completion: @escaping (NSError?) -> Void)
    
    func subscribeGoodsInfo(roomId: String?, completion: @escaping (NSError?, [CommerceGoodsModel]?) -> Void)
    
    func getGoodsInfo(roomId: String?, goodsId: String?, completion: @escaping (NSError?, CommerceGoodsBuyModel?) -> Void)
}
