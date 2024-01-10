//
//  VideoLoaderApiProtocol.swift
//  VideoLoaderAPI
//
//  Created by Agora on 2023/7/27.
//

import Foundation
import AgoraRtcKit

@objc public enum RoomStatus: Int {
    case idle = 0
    case prejoined
    case joined
}


public class VideoLoaderConfig: NSObject {
    public var rtcEngine: AgoraRtcEngineKit!
    public var userId: UInt = 0
}

public class RoomInfo: NSObject {
    public var channelName: String = ""
    public var uid: UInt = 0
    public var token: String = ""
}

public class VideoCanvasContainer: NSObject {
    public var container: UIView?
    public var uid: UInt = 0
//    public var viewIndex: Int = 0
//    public var renderMode: Int = Constants.RENDER_MODE_HIDDEN
}

@objc public protocol IVideoLoaderApiListener: NSObjectProtocol {
    @objc optional func onStateDidChange(newState: RoomStatus, oldState: RoomStatus, channelName: String)
    
    @objc optional func debugInfo(_ message: String)
    @objc optional func debugWarning(_ message: String)
    @objc optional func debugError(_ message: String)
}

@objc public protocol IVideoLoaderApi: NSObjectProtocol {
    
    /// Initial configuration
    /// - Parameters:
    ///   - config: <#config description#>
    func setup(config: VideoLoaderConfig)
    
    /// preload Room list
    /// - Parameter preloadRoomList: <#preloadRoomList description#>
    func preloadRoom(preloadRoomList: [RoomInfo])
    
    /// Switching state
    /// - Parameters:
    /// -newState: indicates the target status
    /// -roomInfo: channel object
    /// -tagId: indicates the identifier that the operation channel depends on. For example, multiple rooms may share a channel stream. Here, tagId is used to add references to a single channel. If multiple tagids are set, set them to idle during clearing
    func switchRoomState(newState: RoomStatus, roomInfo: RoomInfo, tagId: String?)
    
    /// Gets the current channel status
    /// - Parameter roomInfo: <#roomInfo description#>
    /// - Returns: <#description#>
    func getRoomState(roomInfo: RoomInfo) -> RoomStatus
    
    func getConnectionMap() -> [String: AgoraRtcConnection]
    
    /// Renders to the specified canvas
    /// - Parameters:
    ///   - roomInfo: <#roomInfo description#>
    ///   - container: <#container description#>
    func renderVideo(roomInfo: RoomInfo, container: VideoCanvasContainer)
    
    /// Clear cache
    func cleanCache()
    
    /// Quit a channel other than one
    func leaveChannelWithout(roomId: String)
    
    /// Add api Proxy
    /// - Parameter listener: <#listener description#>
    func addListener(listener: IVideoLoaderApiListener)
    
    /// Remove api proxy
    /// - Parameter listener: <#listener description#>
    func removeListener(listener: IVideoLoaderApiListener)
    
    /// Adding an RTC proxy
    /// - Parameter listener: <#listener description#>
    func addRTCListener(roomId: String, listener: AgoraRtcEngineDelegate)
    
    /// Remove the RTC agent
    /// - Parameter listener: <#listener description#>
    func removeRTCListener(roomId: String, listener: AgoraRtcEngineDelegate)
    
    func getRTCListener(roomId: String) -> AgoraRtcEngineDelegate?
}
