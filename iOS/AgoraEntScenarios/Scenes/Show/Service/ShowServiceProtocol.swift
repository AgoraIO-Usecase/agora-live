//
//  ShowServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/2.
//

import Foundation

enum ShowSubscribeStatus {
    case created
    case deleted
    case updated
}

protocol ShowServiceProtocol: NSObjectProtocol {
    /// Get room list
    /// - Parameters:
    /// -page: page index, starting from 0 (this property is temporarily invalid because SyncManager cannot page)
    /// - completion: completion callback (error message, room list)
    func getRoomList(page: Int,
                     completion: @escaping (NSError?, [ShowRoomListModel]?) -> Void)
    
    
    /// Create a room
    /// - Parameters:
    /// -roomName: indicates the room name
    /// -roomId: indicates the roomId
    /// - thumbnailId: thumbnail of the list
    ///   - completion: <#completion description#>
    func createRoom(roomName: String,
                    roomId: String,
                    thumbnailId: String,
                    completion: @escaping (NSError?, ShowRoomDetailModel?) -> Void)
    
    /// Join the room
    /// - Parameters:
    /// -room: indicates room object information
    /// - completion: completion callback (error message, room message)
    func joinRoom(room: ShowRoomListModel,
                  completion: @escaping (NSError?, ShowRoomDetailModel?) -> Void)
    
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
    func getAllUserList(completion: @escaping (NSError?, [ShowUser]?) -> Void)
    
    /// Send a chat message
    /// - Parameters:
    ///   - roomId: roomId description
    ///   - message: message description
    ///   - completion: completion description
    func sendChatMessage(roomId: String?,
                         message: ShowMessage,
                         completion: ((NSError?) -> Void)?)
    
    
    /// Obtain the list of applications for accessing the MIC
    /// - Parameter completion: <#completion description#>
    func getAllMicSeatApplyList(completion: @escaping (NSError?, [ShowMicSeatApply]?) -> Void)

    /// Audience request Line
    /// - Parameters:
    ///   - completion: <#completion description#>
    func createMicSeatApply(completion: @escaping (NSError?) -> Void)
    
    /// Audience cancels connection request
    /// - Parameters:
    ///   - completion: <#completion description#>
    func cancelMicSeatApply(completion: @escaping (NSError?) -> Void)
    
    /// The host accepts applications for Line
    /// - Parameters:
    /// -apply: indicates the application object
    ///   - completion: <#completion description#>
    func acceptMicSeatApply(apply:ShowMicSeatApply,
                            completion: @escaping (NSError?) -> Void)
    
    /// The host rejected the application
    /// - Parameters:
    /// -apply: indicates the application object
    ///   - completion: <#completion description#>
    func rejectMicSeatApply(apply:ShowMicSeatApply,
                            completion: @escaping (NSError?) -> Void)
    
    
    /// Get the current Line or PK anchor
    /// - Parameters:
    /// -roomId: indicates the room ID
    /// -completion: callback to the current user
    func getCurrentApplyUser(roomId: String?, completion: @escaping (ShowRoomListModel?) -> Void)
    
    
    /// Get a list of mic invites
    /// - Parameter completion: <#completion description#>
    func getAllMicSeatInvitationList(completion: @escaping (NSError?, [ShowMicSeatInvitation]?) -> Void)
    
    /// Anchor creates Lien invitation
    /// - Parameters:
    /// -user: invites users
    ///   - completion: <#completion description#>
    func createMicSeatInvitation(user: ShowUser,
                                 completion: @escaping (NSError?) -> Void)
    
    /// The anchor rescinded the invitation to connect the mic
    /// - Parameters:
    /// -user: indicates a user
    ///   - completion: <#completion description#>
    func cancelMicSeatInvitation(userId: String,
                                 completion: @escaping (NSError?) -> Void)
    
    /// The audience agrees with Lian Mai
    /// - Parameters:
    ///   - completion: <#completion description#>
    func acceptMicSeatInvitation(completion: @escaping (NSError?) -> Void)
    
    /// The audience rejected the mic
    /// - Parameters:
    ///   - completion: <#completion description#>
    func rejectMicSeatInvitation(completion: @escaping (NSError?) -> Void)
    
    
    
    
    /// Get a list of PK objects (currently equivalent to getRoomList)
    /// - Parameter completion: <#completion description#>
    func getAllPKUserList(completion: @escaping (NSError?, [ShowPKUserInfo]?) -> Void)
    
    /// Get a list of PK invitations (including invitations initiated and accepted, differentiated by fromUerId)
    /// - Parameter completion: <#completion description#>
    func getAllPKInvitationList(completion: @escaping (NSError?, [ShowPKInvitation]?) -> Void)
    

    /// Create PK invitation
    /// - Parameters:
    /// -user: invites users
    ///   - completion: <#completion description#>
    func createPKInvitation(room: ShowRoomListModel,
                            completion: @escaping (NSError?) -> Void)
    
    /// Agree to PK
    /// - Parameters:
    ///   - completion: <#completion description#>
    func acceptPKInvitation(completion: @escaping (NSError?) -> Void)
    
    /// Reject PK
    /// - Parameters:
    ///   - completion: <#completion description#>
    func rejectPKInvitation(completion: @escaping (NSError?) -> Void)
    
    
    
    /// Get interactive list
    /// - Parameter completion: <#completion description#>
    func getAllInterationList(completion: @escaping (NSError?, [ShowInteractionInfo]?) -> Void)
    
    /// Stop interaction
    /// - Parameter completion: <#completion description#>
    func stopInteraction(interaction: ShowInteractionInfo, completion: @escaping (NSError?) -> Void)
    
    
    
    /// Mute setting
    /// - Parameters:
    ///   - mute: <#mute description#>
    ///   - userId: <#userId description#>
    ///   - completion: <#completion description#>
    func muteAudio(mute:Bool, userId: String, completion: @escaping (NSError?) -> Void)

    
    
    func unsubscribeEvent(delegate: ShowSubscribeServiceProtocol)
    
    func subscribeEvent(delegate: ShowSubscribeServiceProtocol)
}
