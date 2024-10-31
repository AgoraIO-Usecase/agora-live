//
//  KTVServiceProtocol.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/7/13.
//

import Foundation
import RTMSyncManager

@objc protocol KTVServiceListenerProtocol: NSObjectProtocol {
    
    /// The room is expired.
    func onRoomDidExpire()
    
    /// The room was destroyed.
    func onRoomDidDestroy()
    
    /// Change in the number of room users
    /// - Parameter userCount: Number of users
    func onUserCountUpdate(userCount: UInt)
    
    /// Update of the seat
    /// - Parameter seat: Seat object
    func onMicSeatSnapshot(seat: [String: VLRoomSeatModel])
    
    /// Update of the seat
    /// - Parameter seat: Seat object
    func onUserSeatUpdate(seat: VLRoomSeatModel)
    
    /// There are members on the Seat (take the initiative to go to the seat/the landlord hugs people to the seat)
    /// - Parameters:
    ///   - seatIndex: Seat index
    ///   - user: User information
    func onUserEnterSeat(seatIndex: Int, user: AUIUserThumbnailInfo)
    
    /// Some members go down the seat (take the initiative to go down the seat/homeowner kicks people down the seat)
    /// - Parameters:
    ///   - seatIndex: Seat index
    ///   - user: User information
    func onUserLeaveSeat(seatIndex: Int, user: AUIUserThumbnailInfo)
    
    /// The owner muted/lifted the ban on the seat.
    /// - Parameters:
    ///   - seatIndex: Seat index
    ///   - isMute:Microphone switch status
    func onSeatAudioMute(seatIndex: Int, isMute: Bool)

    /// The owner disables/enables/enables the seat camera
    /// - Parameters:
    ///   - seatIndex: Seat index
    ///   - isMute: Camera switch status
    func onSeatVideoMute(seatIndex: Int, isMute: Bool)
    
    /// Update all song callbacks (such as pin)
    /// - Parameter song: List of songs
    func onChosenSongListDidChanged(songs: [VLRoomSelSongModel])
    
    /// The chorus joins
    /// - Parameter chorus: Joined chorus information
    func onChoristerDidEnter(chorister: KTVChoristerModel)
    
    /// The chorus left.
    /// - Parameter chorister: The chorus who left
    func onChoristerDidLeave(chorister: KTVChoristerModel)
    
}

@objc protocol KTVServiceProtocol: NSObjectProtocol {
    // room info
    
    /// Get the list of rooms
    /// - Parameters:
    ///   - page: Page number
    ///   - completion: Complete the callback
    func getRoomList(page: UInt, completion: @escaping (Error?, [AUIRoomInfo]?) -> Void)
    
    /// Create a room
    /// - Parameters:
    /// - inputModel: Input Model
    /// - completion: Complete the callback
    func createRoom(inputModel: KTVCreateRoomInfo, completion: @escaping (Error?, AUIRoomInfo?) -> Void)
    
    /// Join the room
    /// - Parameters:
    /// - roomId: room id
    /// - password: password
    /// - completion: Complete the callback
    func joinRoom(roomId: String, password: String, completion: @escaping (Error?) -> Void)
    
    /// Leave the room
    /// - Parameter completion: Complete the callback
    func leaveRoom(completion: @escaping (Error?) -> Void)
    
    // mic seat
    
    /// Enter Seat
    /// - Parameters:
    ///   - seatIndex: Seat Index
    ///   - completion: Complete the callback
    func enterSeat(seatIndex: NSNumber?, completion: @escaping (Error?) -> Void)
    
    /// Leave Seat
    /// - Parameters:
    ///   - completion: Complete the callback
    func leaveSeat(completion: @escaping (Error?) -> Void)
    
    /// Kick seat
    /// - Parameters:
    ///   - seatIndex: Seat index
    ///   - completion: Complete the callback
    func kickSeat(seatIndex: Int, completion: @escaping (NSError?) -> ())
    
    /// Set seat voice
    /// - Parameters:
    ///   - muted: Whether to turn off the sound, YES: turn off the sound, NO: turn on the sound
    ///   - completion: Complete the callback
    func updateSeatAudioMuteStatus(muted: Bool, completion: @escaping (Error?) -> Void)
    
    /// Set seat camera state
    /// - Parameters:
    ///   - muted: Whether to turn off the camera, YES: turn off the camera, NO: turn on the camera
    ///   - completion: Complete the callback
    func updateSeatVideoMuteStatus(muted: Bool, completion: @escaping (Error?) -> Void)
    
    // choose songs
    
    /// Delete the selected song
    /// - Parameters:
    /// - songCode: Song id
    /// - completion: Complete the callback
    func removeSong(songCode: String, completion: @escaping (Error?) -> Void)
    
    /// Get the list of selected songs
    /// - Parameter completion: Complete the callback
    func getChoosedSongsList(completion: @escaping (Error?, [VLRoomSelSongModel]?) -> Void)
    
    /// The lead singer tells the songs currently playing in the background
    /// - Parameters:
    /// - songCode: room id
    /// - completion: Complete the callback
    func markSongDidPlay(songCode: String, completion: @escaping (Error?) -> Void)
    
    /// order a song
    /// - Parameters:
    /// - inputModel: Input Model
    /// - completion: Complete the callback
    func chooseSong(inputModel: KTVChooseSongInputModel, completion: @escaping (Error?) -> Void)
    
    /// Pin the top song
    /// - Parameters:
    /// - songCode: Song id
    /// - completion: Complete the callback
    func pinSong(songCode: String, completion: @escaping (Error?) -> Void)
    
    /// Join the chorus
    /// - Parameters:
    /// - songCode: Song id
    /// - completion: Complete the callback
    func joinChorus(songCode: String, completion: @escaping (Error?) -> Void)
    
    /// Cancel the chorus of the accompaniment
    /// - Parameters:
    /// - songCode: Song id
    /// - completion: Complete the callback
    func leaveChorus(songCode: String, completion: @escaping (Error?) -> Void)
    
    /// Subscribe to the listener
    /// - Parameter listener: Monitor object
    func subscribe(listener: KTVServiceListenerProtocol?)
    
    /// Get the current room usage time
    /// - Parameter channelName: Channel Name
    /// - Returns: Duration of use, unit ms
    func getCurrentDuration(channelName: String) -> UInt64
}
