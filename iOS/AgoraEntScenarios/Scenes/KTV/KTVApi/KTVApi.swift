//
//  KTVApiDelegate.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2023/3/14.
//

import Foundation
import AgoraRtcKit

/// User role
@objc public enum KTVSingRole: Int {
    case soloSinger = 0     //Soloist
    case coSinger           //Sing an accompaniment to
    case leadSinger         //Lead singer
    case audience           //Audience
}

@objc public enum loadMusicType: Int {
    case mcc
    case local
}

/// The status of the song
@objc public enum KTVPlayerTrackMode: Int {
    case origin = 0    //Original singer
    case lead          //Director and singer
    case acc           //Accompany
}

/// Reasons for the failure of loading songs
@objc public enum KTVLoadMusicMode: Int {
    case loadNone
    case loadMusicOnly
    case loadLrcOnly
    case loadMusicAndLrc
}

/// Reasons for the failure of loading songs
@objc public enum KTVLoadSongFailReason: Int {
    case noLyricUrl = 0         //No lyrics
    case musicPreloadFail   //Song preload failed
    case cancled // This loading is canceled/stopped
}

@objc public enum KTVSwitchRoleState: Int {
    case success = 0
    case fail
}

@objc public enum KTVSwitchRoleFailReason: Int {
    case none = 0
    case joinChannelFail
    case noPermission
}


/// Join the chorus result state
@objc public enum KTVJoinChorusState: Int {
    case success = 0    //Join the chorus successfully
    case fail           //Failed to join the chorus
}


/// Reasons for failure to join the chorus
@objc public enum KTVJoinChorusFailReason: Int {
    case musicOpenFail     //Failed to open the song
    case joinChannelFail   //Failed to join the ex channel
}

@objc public enum KTVType: Int {
    case normal
    case singbattle
    case singRelay
}

@objc public protocol IMusicLoadStateListener: NSObjectProtocol {
    
    
    /// Song progress
    /// - Parameters:
    ///   - songCode: <#songCode description#>
    ///   - percent: Song loading progress Range: 0-100
    ///   - status: <#status description#>
    ///   - msg: <#msg description#>
    ///   - lyricUrl: <#lyricUrl description#>
    func onMusicLoadProgress(songCode: Int, percent: Int, state: AgoraMusicContentCenterPreloadState, msg: String?, lyricUrl: String?)
    
    /// The song is loaded successfully
    /// - Parameters:
    /// - songCode: Song code
    /// - lyricUrl: Lyrics remote url
    func onMusicLoadSuccess(songCode: Int, lyricUrl: String)

    /// Song loading failed
    /// - Parameters:
    /// - songCode: Song code
    /// - lyricUrl: Song remote url
    /// - reason: Reason for error
    func onMusicLoadFail(songCode: Int, reason: KTVLoadSongFailReason)
}

@objc public protocol KTVLrcViewDelegate: NSObjectProtocol {
    func onUpdatePitch(pitch: Float)
    func onUpdateProgress(progress: Int)
    func onDownloadLrcData(url: String)
    func onHighPartTime(highStartTime: Int, highEndTime: Int)
}

@objc public protocol KTVApiEventHandlerDelegate: NSObjectProtocol {
    
    /// Change of song playback status
    /// - Parameters:
    ///   - state: <#state description#>
    ///   - error: <#error description#>
    ///   - isLocal: <#isLocal description#>
    func onMusicPlayerStateChanged(state: AgoraMediaPlayerState,
                                   reason: AgoraMediaPlayerReason,
                                   isLocal: Bool)
    
    
    /// Song score back
    /// - Parameter score: <#score description#>
    func onSingingScoreResult(score: Float)
     
    /// Role switching callback
    /// - Parameters:
    ///   - oldRole: <#oldRole description#>
    ///   - newRole: <#newRole description#>
    func onSingerRoleChanged(oldRole: KTVSingRole, newRole: KTVSingRole)
    
    func onTokenPrivilegeWillExpire()
    
    /**
    * Chorus channel voice volume prompt
    * @param speakers Volume information of different users
    * @param totalVolume Total Volume
    */
    func onChorusChannelAudioVolumeIndication(
        speakers: [AgoraRtcAudioVolumeInfo],
        totalVolume: Int)
    
    //MPK time callback is only for homeowners. It is only suitable for singing.
    func onMusicPlayerProgressChanged(with progress: Int)
}

// The path selection strategy for singers to listen to each other's audio streams in the chorus
enum GiantChorusRouteSelectionType: Int {
    case random = 0 // Randomly select several streams
    case byDelay = 1 // Select the lowest stream according to the delay
    case topN = 2 // Select the flow according to the tone strength
    case byDelayAndTopN = 3 // At the same time, start to delay the route selection and tone strong flow selection.
}

// In the chorus, the singers listen to each other's audio stream.
@objc public class GiantChorusRouteSelectionConfig: NSObject {
    let type: GiantChorusRouteSelectionType // Path selection strategy
    let streamNum: Int // The maximum number of selected streams (recommended 6)

    init(type: GiantChorusRouteSelectionType, streamNum: Int) {
        self.type = type
        self.streamNum = streamNum
    }
}

@objc open class GiantChorusConfiguration: NSObject {
    var appId: String
    var rtmToken: String
    weak var engine: AgoraRtcEngineKit?
    var channelName: String
    var localUid: Int = 0
    var chorusChannelName: String
    var chorusChannelToken: String
    var maxCacheSize: Int = 10
    var musicType: loadMusicType = .mcc
    var audienceChannelToken: String = ""
    var musicStreamUid: Int = 0
    var musicChannelToken: String = ""
    var routeSelectionConfig: GiantChorusRouteSelectionConfig = GiantChorusRouteSelectionConfig(type: .byDelay, streamNum: 6)
    var mccDomain: String?
    @objc public
    init(appId: String,
         rtmToken: String,
         engine: AgoraRtcEngineKit,
         localUid: Int,
         audienceChannelName: String,
         audienceChannelToken: String,
         chorusChannelName: String,
         chorusChannelToken: String,
         musicStreamUid: Int,
         musicChannelToken: String,
         maxCacheSize: Int,
         musicType: loadMusicType,
         routeSelectionConfig: GiantChorusRouteSelectionConfig,
         mccDomain: String?
    ) {
        self.appId = appId
        self.rtmToken = rtmToken
        self.engine = engine
        self.channelName = audienceChannelName
        self.localUid = localUid
        self.chorusChannelName = chorusChannelName
        self.chorusChannelToken = chorusChannelToken
        self.maxCacheSize = maxCacheSize
        self.musicType = musicType
        self.audienceChannelToken = audienceChannelToken
        self.musicStreamUid = musicStreamUid
        self.musicChannelToken = musicChannelToken
        self.routeSelectionConfig = routeSelectionConfig
        self.mccDomain = mccDomain
    }
}

@objc open class KTVApiConfig: NSObject{
    var appId: String
    var rtmToken: String
    weak var engine: AgoraRtcEngineKit?
    var channelName: String
    var localUid: Int = 0
    var chorusChannelName: String
    var chorusChannelToken: String
    var type: KTVType = .normal
    var maxCacheSize: Int = 10
    var musicType: loadMusicType = .mcc
    var mccDomain: String?
    @objc public
    init(appId: String,
         rtmToken: String,
         engine: AgoraRtcEngineKit,
         channelName: String,
         localUid: Int,
         chorusChannelName: String,
         chorusChannelToken: String,
         type: KTVType,
         musicType: loadMusicType,
         maxCacheSize: Int,
         mccDomain: String?
    ) {
        self.appId = appId
        self.rtmToken = rtmToken
        self.engine = engine
        self.channelName = channelName
        self.localUid = localUid
        self.chorusChannelName = chorusChannelName
        self.chorusChannelToken = chorusChannelToken
        self.type = type
        self.maxCacheSize = maxCacheSize
        self.musicType = musicType
        self.mccDomain = mccDomain
    }
    
    
}

/// Song loading configuration information
@objcMembers open class KTVSongConfiguration: NSObject {
    public var songIdentifier: String = ""
    public var mainSingerUid: Int = 0     //The lead singer uid
    public var mode: KTVLoadMusicMode = .loadMusicAndLrc
    public var songCutter: Bool = false
    func printObjectContent() -> String {
        var content = ""
        
        let mirror = Mirror(reflecting: self)
        for child in mirror.children {
            if let propertyName = child.label {
                if let propertyValue = child.value as? CustomStringConvertible {
                    content += "\(propertyName): \(propertyValue)\n"
                } else {
                    content += "\(propertyName): \(child.value)\n"
                }
            }
        }
        
        return content
   }
}


public typealias LyricCallback = ((String?) -> Void)
public typealias LoadMusicCallback = ((AgoraMusicContentCenterPreloadState, NSInteger) -> Void)
public typealias ISwitchRoleStateListener = (KTVSwitchRoleState, KTVSwitchRoleFailReason) -> Void
public typealias MusicChartCallBacks = (String, AgoraMusicContentCenterStateReason, [AgoraMusicChartInfo]?) -> Void
public typealias MusicResultCallBacks = (String, AgoraMusicContentCenterStateReason, AgoraMusicCollection) -> Void
public typealias JoinExChannelCallBack = ((Bool, KTVJoinChorusFailReason?)-> Void)

@objc public protocol KTVApiDelegate: NSObjectProtocol {
    
    @objc optional func createKtvApi(config: KTVApiConfig) //Small chorus is a must
    
    @objc optional func createKTVGiantChorusApi(config: GiantChorusConfiguration) //Chorus is a must
    
    /// Subscribe to KTVApi events
    /// - Parameter ktvApiEventHandler: <#ktvApiEventHandler description#>
    func addEventHandler(ktvApiEventHandler: KTVApiEventHandlerDelegate)
    
    
    /// Unsubscribe from KTVApi event
    /// - Parameter ktvApiEventHandler: <#ktvApiEventHandler description#>
    func removeEventHandler(ktvApiEventHandler: KTVApiEventHandlerDelegate)
    
    
    /// Empty internal variables/caches, cancel listening when initWithRtcEngine, and cancel network requests, etc.
    func cleanCache()
    
    /**
     * When receiving an IKTVApiEventHandler.onTokenPrivilegeWillExpire callback, you need to actively call the method to update Token
     * @param rtmToken rtm token required by musicContentCenter module
     * @param chorusChannelRtcToken The channel needed for chorus rtc token
    */
    func renewToken(
        rtmToken: String,
        chorusChannelRtcToken: String)
    
    /**
     * Get the song list
     * Parameter completion: The list of the list is adjusted back
     */
    func fetchMusicCharts(completion:@escaping MusicChartCallBacks)
    
    /**

    * Search for song lists according to the type of song list
    * Parameters:
    * musicChartId: List id
    * page: The number of query pages of the list
    * pageSize: Query the data length of each page
    * jsonOption: Custom filter mode
    * completion: Song list callback
    */
    func searchMusic(musicChartId: Int,
                     page: Int,
                     pageSize: Int,
                     jsonOption: String,
                     completion:@escaping MusicResultCallBacks)
    
    /**

    * Search for songs by keywords
    * Parameters:
    * keyword: search keyword
    * page: The number of query pages of the list
    * pageSize: Query the data length of each page
    * jsonOption: Custom filter mode
    * completion: Song list callback
    */
    func searchMusic(keyword: String,
                     page: Int, pageSize: Int,
                     jsonOption: String,
                     completion: @escaping MusicResultCallBacks)
            
    
    
    /// Load songs
    /// - Parameters:
    ///   - config: <#config description#>
    ///   - onMusicLoadStateListener: <#onMusicLoadStateListener description#>
    func loadMusic(songCode: Int, config: KTVSongConfiguration, onMusicLoadStateListener: IMusicLoadStateListener)
    
    /// Load songs through url
    /// - Parameters:
    ///   - config: <#config description#>
    ///   - onMusicLoadStateListener: <#onMusicLoadStateListener description#>
    func loadMusic(config: KTVSongConfiguration, url: String)
    
    
    /// Switch roles
    /// - Parameters:
    ///   - newRole: <#newRole description#>
    ///   - token: <#token description#>
    ///   - onSwitchRoleState: <#onSwitchRoleState description#>
    func switchSingerRole(newRole: KTVSingRole, onSwitchRoleState:@escaping ISwitchRoleStateListener)
    
    
    /// Play
    /// - Parameter startPos: <#startPos description#>
    func startSing(songCode: Int, startPos: Int)
    
    /**
     * Play the song
     * @param url Song address
     * @param startPos Where to start playing
     * For the lead singer:
     * If you select autoPlay = true when loadingMusic, you don't need to actively call startSing.
     * If you select autoPlay = false when loadingMusic, you need to call startSing after loadMusic is successful.
    */
    func startSing(url: String, startPos: Int)
    
    /// Resume playback
    func resumeSing()
    
    
    /// Pause playback
    func pauseSing()
    
    
    /// Adjust the progress
    /// - Parameter time: progress, unit ms
    func seekSing(time: Int)
    
    /**
     * Set the current audio playback delay, which is suitable for audio self-acquisition
     * @param audioPlayoutDelay Time difference between audio frame processing and playback
    */
    func setAudioPlayoutDelay(audioPlayoutDelay: Int)
    
    /// Set up the lyrics component, and the settings can take effect at any time.
    /// - Parameter view: <#view description#>
    func setLrcView(view: KTVLrcViewDelegate)
    
    
    /// Set the current mic switch status and turn off the microphone call.
    /// At present, onAudioVolumeIndication will still be executed after the microphone is called adjustRecordSignalVolume(0). ktvApi needs to add a variable to determine whether the microphone is currently closed. If the microphone is turned off, the pitch set to the lyrics component is changed to 0
    /// - Parameter muteStatus: mute mic status
    func muteMic(muteStatus: Bool)
    
    func getMusicPlayer() -> AgoraRtcMediaPlayerProtocol?
    
    /// Get the MCC instance
    /// - Returns: description
    func getMusicContentCenter() -> AgoraMusicContentCenter?
    
    
    /// Pull the list of songs
    func fetchSongList(complete: ((_ list: NSArray) -> Void)?)
    
    // Turn on the professional anchor mode
    func enableProfessionalStreamerMode(_ enable: Bool)
    
    /**
     Create dataStreamID
     */
    func renewInnerDataStreamId()
    
    
    /**
     * Load the song, and only loadSong for a song at the same time can be called synchronously. Generally, using this loadSong means that the song has been successfully preloaded (url is the local file address)
     * @param config Load song configuration, config.autoPlay = true, default playback url1
     * @param url1 Song Address 1
     * @param url2 Song Address 2
     * Recommended call:
     * At the beginning of the song:
     * Lead singer loadMusic(KTVSongConfiguration(autoPlay=true, mode=LOAD_MUSIC_AND_LRC, url, mainSingerUid)) swi TchSingerRole (SoloSinger)
     * Audience loadMusic(KTVSongConfiguration(autoPlay=false, mode=LOAD_LRC_ONLY, url, mainSingerUid))
     * When joining the chorus:
     * Prepare to join the chorus: loadMusic(KTVSongConfiguration(autoPlay=false, mode=LOAD_MUSIC_ONLY, url, mainSingerUid))
     * After loadMusic is successful, switchSingerRole(CoSinger)
    */
  func load2Music(
      url1: String,
      url2: String,
      config: KTVSongConfiguration
  )
  
  /**
   * Multi-file switching playback resources
   * @param url The playback resources that need to be switched need to be one of the parameters url1 and url2 in load2Music.
   * @param syncPts Whether to synchronize the starting playback position before and after switching: true synchronization, false asynchronous, starting from 0
   */
  func switchPlaySrc(url: String, syncPts: Bool)
    
  /**
   * Canceling the song download will interrupt the process of loading the song and remove the song cache.
   * @param songCode The unique encoding of the song
   */
      
   func removeMusic(songCode: Int)
    
   @objc func didAudioMetadataReceived( uid: UInt, metadata: Data)
}
