//
//  VoiceServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/18.
//

import Foundation

enum VoiceChatSubscribeStatus {
    case created     //Subscription to object creation
    case deleted     //Subscription to object deletion
    case updated     //Subscription to object updates
}

protocol VoiceServiceProtocol: NSObjectProtocol {

    ///Get room list
    /// - Parameters:
    ///- Page: pagination index, starting from 0 (this attribute is temporarily invalid due to SyncManager's inability to perform pagination)
    ///- completion: Complete callback (error message, room list)
    func getRoomList(page: Int,
                     completion: @escaping (Error?, [VoiceChatRoom]?) -> Void)
    
    ///Create a room
    /// - Parameters:
    ///- room: Room object information
    ///- completion: Complete callback (error message)
    func createRoom(room: VoiceChatRoom,
                    completion: @escaping (Error?) -> Void)
    
    ///Join Room
    /// - Parameters:
    ///- roomName: Room name
    ///- completion: Complete callback (error message, room information)
    func joinRoom(roomName: String,
                  completion: @escaping (Error?, VoiceChatRoom?) -> Void)
    
    ///Leave the room
    func leaveRoom()
    
    ///Monitor user changes
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    func subscribeUser(subscribeClosure: @escaping (VoiceChatSubscribeStatus, VoiceChatRoom) -> Void)
    
    ///Get all users in the current room
    ///- Parameter completion: Completed callback (error message, user list)
    func getAllUserList(completion: @escaping (Error?, [VoiceChatUsers]?) -> Void)

    
    
    
    

    ///Set the number of viewers
    /// - Parameters:
    ///- visitCount: Number of visitors
    ///   - completion: <#completion description#>
    func setVisit(visitCount: Int,
                  completion: @escaping (Error?) -> Void)
    
    ///Get the number of viewers
    /// - Parameter completion: <#completion description#>
    func getVisit(completion: @escaping (Error?, Int?) -> Void)
    
    ///Changes in the number of subscribed viewers
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    func subscribeVisit(subscribeClosure: @escaping (VoiceChatSubscribeStatus, Int) -> Void)
    
    
    
    
    
    ///Chat robot switch
    /// - Parameters:
    ///- enable: true: opens chatbot, false: closes chatbot
    ///   - completion: <#completion description#>
    func toggleSmartRobot(enable: Bool,
                          completion: @escaping (Error?) -> Void)
    
    ///Chat robot switch changes
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    func subscribeSmartRobot(subscribeClosure: @escaping (Bool) -> Void)
    
    
    
    
    
    ///Get a list of all booths
    /// - Parameter completion: <#completion description#>
    func getAllMicSeatsList(completion: @escaping (Error?, [VoiceChatMicSeat]?) -> Void)
    
    ///Update microphone location information
    /// - Parameters:
    ///   - seat: <#seatInfo description#>
    ///   - completion: <#completion description#>
    func updateMicSeat(seat: VoiceChatMicSeat, completion: @escaping (Error?) -> Void)
    
    ///Subscription slot changes
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    func subscribeMicSeat(subscribeClosure: @escaping (VoiceChatSubscribeStatus, VoiceChatMicSeat) -> Void)
    
    ///Audience applies for admission to the microphone
    /// - Parameters:
    ///- seatIndex: wheat spot index
    ///   - completion: <#completion description#>
    func createMicSeatRequest(seatIndex: Int,
                              completion: @escaping (Error?) -> Void)
    
    
    ///Audience cancels the application for online streaming
    /// - Parameters:
    ///   - seatIndex: <#seatIndex description#>
    ///   - completion: <#completion description#>
    func cancelMicSeatRequest(seatIndex: Int,
                              completion: @escaping (Error?) -> Void)
    
    ///Get all requests to access the microphone
    /// - Parameter completion: <#completion description#>
    func getAllMicSeatRequests(completion: @escaping (Error?, [VoiceChatMicSeatRequest]?) -> Void)
    
    ///Agree to go online
    /// - Parameters:
    ///- request: Request object for uploading to the microphone
    ///   - completion: <#completion description#>
    func approveMicSeatRequest(request: VoiceChatMicSeatRequest,
                               completion: @escaping (Error?) -> Void)
        
    ///Refuse to connect to the microphone
    /// - Parameters:
    ///- request: Request object for uploading to the microphone
    ///   - completion: <#completion description#>
    func rejectMicSeatRequest(request: VoiceChatMicSeatRequest,
                              completion: @escaping (Error?) -> Void)
    
    ///Subscription to microphone request changes
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    func subscribeMicSeatRequestList(subscribeClosure: @escaping (VoiceChatSubscribeStatus, VoiceChatMicSeatRequest) -> Void)
    
    ///The anchor invites the audience to join the microphone
    /// - Parameters:
    ///   - invitation: <#invitation description#>
    ///   - completion: <#completion description#>
    func createMicSeatInvitation(invitation: VoiceChatMicSeatInvitation,
                                 completion: @escaping (Error?) -> Void)
    
    ///Anchor cancels audience's invitation to join the microphone
    /// - Parameters:
    ///   - invitation: <#invitation description#>
    ///   - completion: <#completion description#>
    func cancelMicSeatInvitation(invitation: VoiceChatMicSeatInvitation,
                                 completion: @escaping (Error?) -> Void)
    
    ///Audience agrees to the invitation for online streaming
    /// - Parameters:
    ///   - invitation: <#invitation description#>
    ///   - completion: <#completion description#>
    func approveMicSeatInvitation(invitation: VoiceChatMicSeatInvitation,
                                  completion: @escaping (Error?) -> Void)
        
    ///Refuse the invitation to access the microphone
    /// - Parameters:
    ///   - invitation: <#invitation description#>
    ///   - completion: <#completion description#>
    func rejectMicSeatInvitation(invitation: VoiceChatMicSeatInvitation,
                                 completion: @escaping (Error?) -> Void)
    
    ///Get all online invitations
    /// - Parameter completion: <#completion description#>
    func getAllMicSeatInvitations(completion: @escaping (Error?, [VoiceChatMicSeatInvitation]?) -> Void)
    
    ///Subscription to microphone invitation changes
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    func subscribeMicSeatInvitation(subscribeClosure: @escaping (VoiceChatSubscribeStatus, VoiceChatMicSeatInvitation) -> Void)
    
    
    
    
    
    ///Send chat messages
    /// - Parameters:
    ///   - message: <#message description#>
    ///   - completion: <#completion description#>
    func sendChatMessage(message: VoiceChatMessage,
                         completion: ((Error) -> Void)?)
    
    ///Subscription chat message changes
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    func subscribeMicSeatInvitation(subscribeClosure: @escaping (VoiceChatSubscribeStatus, VoiceChatMessage) -> Void)
    
    
    
    
    
    
    ///Obtain the number of gifts received
    /// - Parameter completion: <#completion description#>
    func getTotalGiftCount(completion: ((Error?, Int) -> Void)?)
    
    ///Sending gifts
    /// - Parameters:
    ///- gift: gift information
    ///   - completion: <#completion description#>
    func sendGift(gift: VoiceChatGift, completion: ((Error?) -> Void)?)
    
    ///Subscription received gift quantity change
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    func subscribeTotalGiftCount(subscribeClosure: @escaping (Int) -> Void)
    
    
    
    
    
    
    ///Update Announcement
    /// - Parameters:
    ///- Content: Announcement information
    ///   - completion: <#completion description#>
    func setRoomNotice(content: String, completion: ((Error?) -> Void)?)
    
    
    ///Update Announcement information (currently only changes)
    /// - Parameters:
    ///   - subscribeClosure: <#subscribeClosure description#>
    func subscribeRoomNotice(subscribeClosure: @escaping (VoiceChatSubscribeStatus, String) -> Void)
    
    

    ///Cancel all listening
    func unsubscribeAll()
}
