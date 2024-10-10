//
//  VideoLoaderApiProtocol.swift
//  VideoLoaderAPI
//
//  Created by Agora on 2023/7/27.
//

import Foundation
import AgoraRtcKit

/// Loading status
@objc public enum AnchorState: Int {
    case idle = 0                 //Free
    case prejoined                //Preload
    case joinedWithVideo          //Only load videos
    case joinedWithAudioVideo     //Load video and audio
}

/// Initialize configuration information
@objcMembers public class VideoLoaderConfig: NSObject {
    public weak var rtcEngine: AgoraRtcEngineKit?    //rtc engine instance
}

//Room information
@objc public class AnchorInfo: NSObject {
    public var channelName: String = ""   //Channel name
    public var uid: UInt = 0              //The uid corresponding to the channel
    public var token: String = ""         //Tokens corresponding to the channel
}

@objc public class VideoCanvasContainer: NSObject {
    public var container: UIView?                            //The view that needs to be rendered
    public var uid: UInt = 0                                 //User uid that needs to be rendered
    public var setupMode: AgoraVideoViewSetupMode = .add     //Canvas mode
    public var mirrorMode: AgoraVideoMirrorMode = .disabled  //Mirror mode
}

@objc public protocol IVideoLoaderApiListener: NSObjectProtocol {
    
    /// Status change callback
    /// - Parameters:
    ///   - newState: <#newState description#>
    ///   - oldState: <#oldState description#>
    ///   - channelName: <#channelName description#>
    @objc optional func onStateDidChange(newState: AnchorState, oldState: AnchorState, channelName: String)
    
    /// Get the callback to the first frame (the time-consuming calculation is from setting joinedWithVideo/joinedWithAudioVideo to the output of the picture)
    /// - Parameters:
    /// - channelName: room id
    /// - elapsed: time-consuming
    @objc optional func onFirstFrameRecv(channelName: String, uid: UInt, elapsed: Int64)
}

@objc public protocol IVideoLoaderApi: NSObjectProtocol {
    
    /// Initialize the configuration
    /// - Parameters:
    ///   - config: <#config description#>
    func setup(config: VideoLoaderConfig)
    
    /// preload room list
    /// - Parameter preloadAnchorList: preload's list
    /// - Parameter userId: The uid of the current user
    func preloadAnchor(preloadAnchorList: [AnchorInfo], uid: UInt)
    
    /// Switch the state
    /// - Parameters:
    /// - newState: Target status
    /// - localUid: local user id
    /// - anchorInfo: Channel Object
    /// - tagId: Mark the identification of the operation channel's dependence, for example, multiple rooms may share a channel stream. Here, tagId is used to add references to a single channel. If you set multiple tagIds, you need to set multiple tagIds to idle when cleaning, if not If necessary, it can be set to nil.
    func switchAnchorState(newState: AnchorState, localUid: UInt, anchorInfo: AnchorInfo, tagId: String?)
    
    /// Get the current channel status
    /// - Parameter anchorInfo: Channel Object
    /// - Returns: <#description#>
    func getAnchorState(anchorInfo: AnchorInfo) -> AnchorState
    
    /// Get the rtc connection map of all channels
    /// - Returns: <#description#>
    func getConnectionMap() -> [String: AgoraRtcConnection]
    
    /// Render to the specified canvas
    /// - Parameters:
    /// - anchorInfo: Channel Object
    ///   - container: <#container description#>
    func renderVideo(anchorInfo: AnchorInfo, container: VideoCanvasContainer)
    
    /// Clear the cache
    func cleanCache()
 
    /// Add api delegate
    /// - Parameter listener: <#listener description#>
    func addListener(listener: IVideoLoaderApiListener)
    
    /// Remove api delegate
    /// - Parameter listener: <#listener description#>
    func removeListener(listener: IVideoLoaderApiListener)
}
