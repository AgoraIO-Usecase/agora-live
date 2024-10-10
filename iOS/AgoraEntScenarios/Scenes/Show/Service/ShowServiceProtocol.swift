//
//  ShowServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/2.
//

import Foundation

enum ShowSubscribeStatus {
    case created     //Subscribe to object creation
    case deleted     //Subscribe to the object to delete
    case updated     //Subscribe to the object update
}

protocol ShowServiceProtocol: NSObjectProtocol {
    /// Get the list of rooms
    /// - Parameters:
    /// - page: paging index, starting from 0 (this attribute is temporarily invalid because SyncManager cannot paging)
    /// - completion: Complete the callback (error message, room list)
    func getRoomList(page: Int,
                     completion: @escaping (NSError?, [ShowRoomListModel]?) -> Void)
    
    /// Create a room
    /// - Parameters:
    /// - roomId: Room Id
    /// - roomName: Room Name
    /// - completion: Complete the callback
    func createRoom(roomId: String,
                    roomName: String,
                    completion: @escaping (NSError?, ShowRoomDetailModel?) -> Void)
    
    /// Join the room
    /// - Parameters:
    /// - room: Room object information
    /// - completion: Complete the callback (error message, room information)
    func joinRoom(room: ShowRoomListModel,
                  completion: @escaping (NSError?, ShowRoomDetailModel?) -> Void)
    
    
    /// Leave the room
    /// - Parameters:
    /// - roomId: Room Id
    /// - completion: Complete the callback
    func leaveRoom(roomId: String,
                   completion: @escaping (NSError?) -> Void)
    
    /// Get all users in the current room
    /// - Parameters:
    /// - roomId: Room Id
    /// - completion: Complete the callback (error message, user list)
    func getAllUserList(roomId: String,
                        completion: @escaping (NSError?, [ShowUser]?) -> Void)
    
    /// Send chat messages
    /// - Parameters:
    /// - roomId: Room Id
    /// - message: message content
    /// - completion: Complete the callback
    func sendChatMessage(roomId: String,
                         message: ShowMessage,
                         completion: ((NSError?) -> Void)?)
    
    /// Get the list of applications on the microphone seat
    /// - Parameters:
    /// - roomId: Room Id
    /// - completion: Complete the callback
    func getAllMicSeatApplyList(roomId: String,
                                completion: @escaping (NSError?, [ShowMicSeatApply]?) -> Void)

    
    /// The audience applies for mic seat
    /// - Parameters:
    /// - roomId: Room Id
    /// - completion: Complete the callback
    func createMicSeatApply(roomId: String,
                            completion: @escaping (NSError?) -> Void)
    
    /// The audience cancels the mic seat application
    /// - Parameters:
    /// - roomId: Room Id
    /// - completion: Complete the callback
    func cancelMicSeatApply(roomId: String,
                            completion: @escaping (NSError?) -> Void)
    
    /// The anchor accepts the application of mic seat
    /// - Parameters:
    /// - roomId: Room Id
    /// - userId: User Id
    /// - completion: Complete the callback
    func acceptMicSeatApply(roomId: String,
                            userId: String,
                            completion: @escaping (NSError?) -> Void)
    
    
    /// The anchor creates a microphone seat invitation
    /// - Parameters:
    /// - roomId: Room ID
    /// - userId: Invite user id
    /// - completion: Complete the callback
    func createMicSeatInvitation(roomId: String,
                                 userId: String,
                                 completion: @escaping (NSError?) -> Void)
    
    /// The audience agrees to mic seat
    /// - Parameters:
    /// - roomId: Room ID
    /// - invitationId: invitation id
    /// - completion: Complete the callback
    func acceptMicSeatInvitation(roomId: String,
                                 invitationId: String,
                                 completion: @escaping (NSError?) -> Void)
    
    /// The audience refused to connect the microphone seat
    /// - Parameters:
    /// - roomId: Room ID
    /// - invitationId: invitation id
    /// - completion: Complete the callback
    func rejectMicSeatInvitation(roomId: String,
                                 invitationId: String,
                                 completion: @escaping (NSError?) -> Void)
    
    
    
    
    /// Get a list of PK objects (currently equivalent getRoomList)
    /// - Parameter completion: Complete the callback
    func getAllPKUserList(completion: @escaping (NSError?, [ShowPKUserInfo]?) -> Void)
    
    /// Create a PK invitation
    /// - Parameters:
    /// - roomId: room id
    /// - pkRoomId: Room id that requires pk
    /// - completion: Complete the callback
    func createPKInvitation(roomId: String,
                            pkRoomId: String,
                            completion: @escaping (NSError?) -> Void)
    
    /// Agree to PK
    /// - Parameters:
    /// - roomId: room id
    /// - invitationId: invitation id
    /// - completion: Complete the callback
    func acceptPKInvitation(roomId: String,
                            invitationId: String,
                            completion: @escaping (NSError?) -> Void)
    
    /// Reject PK
    /// - Parameters:
    /// - roomId: room id
    /// - invitationId: invitation id
    /// - completion: Complete the callback
    func rejectPKInvitation(roomId: String,
                            invitationId: String,
                            completion: @escaping (NSError?) -> Void)
    
    
    /// Get the current interactive information
    /// - Parameters:
    /// - roomId: room id
    /// - completion: Complete the callback
    func getInterationInfo(roomId: String,
                           completion: @escaping (NSError?, ShowInteractionInfo?) -> Void)
    
    /// Stop interacting
    /// - Parameters:
    /// - roomId: room id
    /// - completion: Complete the callback
    func stopInteraction(roomId: String,
                         completion: @escaping (NSError?) -> Void)
    
    
    
    
    /// Mute setting
    /// - Parameters:
    ///   - mute: <#mute description#>
    ///   - userId: <#userId description#>
    ///   - completion: <#completion description#>
    func muteAudio(roomId: String,
                   mute:Bool,
                   completion: @escaping (NSError?) -> Void)

    
    func getCurrentNtpTs(roomId: String) -> UInt64
    
    func unsubscribeEvent(roomId: String, delegate: ShowSubscribeServiceProtocol)
    
    func subscribeEvent(roomId: String, delegate: ShowSubscribeServiceProtocol)
}
