//
//  VoiceRoomRTCManager.swift
//  AgoraScene_iOS
//
//  Created by CP on 2022/9/5.
//

import AgoraRtcKit
import Foundation
import AgoraCommon

/**
 *User role enumeration
 *Unknown unknown role, exception
 *Owner anchor, teacher
 *CoHost Chorus, Students
 *Audience
 */
@objc public enum ASRoleType: Int, Codable {
    case unknown = -1
    case owner = 0
    case coHost = 1
    case audience = 2
}

/**
 *AI noise reduction level
 *
 */
public enum AINS_STATE {
    case high
    case mid
    case off
}

/**
 *Ear return mode
 *
 */
public enum INEAR_MODE: Int {
    case auto = 0
    case opneSL
    case oboe
}

/**
 *Robot type
 *
 */
@objc public enum ALIEN_TYPE: Int, Codable {
    case blue = 1
    case red = 2
    case blueAndRed = 3
    case none = 4
    case ended = 5
}

/**
 *
 */
@objc public enum VMMUSIC_TYPE: Int, Codable {
    case alien = 1
    case ainsHigh = 2
    case ainsMid = 3
    case ainsOff = 4
    case sound = 5
    case game = 6
    case social = 7
    case ktv = 8
    case anchor = 9
}

/**
 *Scene enumeration
 *VoiceChat chatroom 2.0
 *KTV KTV Scene
 */
@objc public enum ASManagerType: Int {
    case VoiceChat = 1
    case KTV = 2
}

/**
 *Echo cancellation level
 *NoEcho corresponds to zero echo on the UI
 *Standard corresponds to the standard on the UI
 *Fluent corresponds to smoothness on the UI
 */
@objc public enum AECGrade: Int {
    //FIXME: lowercase first letter of enumeration value
    case NoEcho = 1
    case Standard = 3
    case Fluent = 5
}

@objc public enum VMScene: Int {
    case game = 0
    case social = 1
    case ktv = 2
    case anchor = 3
}

// MARK: - VMMusicPlayerDelegate

@objc public protocol VMMusicPlayerDelegate: NSObjectProtocol {
    /**
     *Datastream message callback
     *@ param uid User's Uid
     *Message received by @ param data user
     */
    @objc optional func didReceiveStreamMsgOfUid(uid: UInt, data: Data) -> Void

    /**
     *Progress of MPK's seek
     *@ param position MPK current modification progress
     */
    @objc optional func didMPKChangedToPosition(position: Int) -> Void

    /**
     *MPK current state callback
     *@ param state MPK's current state
     *@ param error MPK current error code
     */
    @objc optional func didMPKChangedTo(state: AgoraMediaPlayerState, reason: AgoraMediaPlayerReason) -> Void //MPK status callback
}

// MARK: - VMManagerDelegate

@objc public protocol VMManagerDelegate: NSObjectProtocol {
    /**
     *RTC remote user offline
     *@ param uid Remote Offline User Uid
     */
    @objc optional func didRtcUserOfflineOfUid(uid: UInt) -> Void

    /**
     *RTC current user joining channel
     *@ param uid Current user Uid
     */
    @objc optional func didRtcLocalUserJoinedOfUid(uid: UInt) -> Void

    /**
     *RTC remote user joining channel
     *@ param uid Remote Online User Uid
     */
    @objc optional func didRtcRemoteUserJoinedOfUid(uid: UInt) -> Void

    /**
     *Real time volume return
     *User voice information returned by @ param speakers
     *@ param totalVolume Returns the current total volume
     */
    @objc optional func reportAudioVolumeIndicationOfSpeakers(speakers: [AgoraRtcAudioVolumeInfo]) -> Void

    /**
     *First frame of user video
     *@ param size Rendered video size (width and height)
     *@ param elapsed The time when the first frame of the video is displayed
     */
    // @objc optional func didUserFirstVideoFrameWith(uid: UInt) -> Void

    /**
     * report alien type
     */
    @objc optional func reportAlien(with type: ALIEN_TYPE, musicType: VMMUSIC_TYPE) -> Void
    
    /**
     *Load songs
     *@ param songCode code
     *@ param progress progress
     *@ param status status
     */
    @objc optional func downloadBackgroundMusicStatus(songCode: Int, progress: Int, state: AgoraMusicContentCenterPreloadState)
}

public let kMPK_RTC_UID: UInt = 1
@objc public class VoiceRoomRTCManager: NSObject {
    // init manager
    private static var _sharedInstance: VoiceRoomRTCManager?

    private var mediaPlayer: AgoraRtcMediaPlayerProtocol?

    private var role: ASRoleType = .audience

    private var type: ASManagerType = .VoiceChat

    private var channelName: String?

    private var streamId: Int = -1

    fileprivate var localRtcUid: UInt = 0

    private var musicType: VMMUSIC_TYPE?
    
    private var mcc: AgoraMusicContentCenter?
    private var musicPlayer: AgoraMusicPlayerProtocol?
    typealias MusicListCallback = ([AgoraMusic])->()
    private var onMusicChartsIdCache: [String: MusicListCallback] = [:]
    private var lastSongCode: Int = 0
    var backgroundMusics: [AgoraMusic] = []
    
    @objc public weak var delegate: VMManagerDelegate?

    @objc public weak var playerDelegate: VMMusicPlayerDelegate?
    
    var stopMixingClosure: (() -> ())?
    var downloadBackgroundMusicStatusClosure: ((_ songCode: Int, _ progress: Int, _ status: AgoraMusicContentCenterPreloadState) -> Void)?
    var backgroundMusicPlayingStatusClosure: ((_ state: AgoraMediaPlayerState) -> Void)?

    //Single example
    @objc public class func getSharedInstance() -> VoiceRoomRTCManager {
        guard let instance = _sharedInstance else {
            _sharedInstance = VoiceRoomRTCManager()
            return _sharedInstance!
        }
        return instance
    }

    private var baseMusicCount: Int = 0 {
        didSet {
            guard let musicType = musicType else {
                return
            }
            var count = 0
            var musicPath: String = ""
            var musicIndex: Int = 0
            switch musicType {
            case .alien:
                count = AgoraConfig.baseAlienMic.count
                musicIndex = baseMusicCount < count ? baseMusicCount : count - 1
                musicPath = "\(AgoraConfig.CreateCommonRoom)\(AgoraConfig.baseAlienMic[musicIndex])"
            case .ainsHigh:
                count = AgoraConfig.HighAINSIntroduc.count
                musicIndex = baseMusicCount < count ? baseMusicCount : count - 1
                musicPath = "\(AgoraConfig.SetAINSIntroduce)\(AgoraConfig.HighAINSIntroduc[musicIndex])"
            case .ainsMid:
                count = AgoraConfig.MediumAINSIntroduc.count
                musicIndex = baseMusicCount < count ? baseMusicCount : count - 1
                musicPath = "\(AgoraConfig.SetAINSIntroduce)\(AgoraConfig.MediumAINSIntroduc[musicIndex])"
            case .ainsOff:
                count = AgoraConfig.NoneAINSIntroduc.count
                musicIndex = baseMusicCount < count ? baseMusicCount : count - 1
                musicPath = "\(AgoraConfig.SetAINSIntroduce)\(AgoraConfig.NoneAINSIntroduc[musicIndex])"
            case .social:
                count = AgoraConfig.SoundSelectSocial.count
                musicIndex = baseMusicCount < count ? baseMusicCount : count - 1
                musicPath = AgoraConfig.SoundSelectSocial[musicIndex]
            case .ktv:
                count = AgoraConfig.SoundSelectKTV.count
                musicIndex = baseMusicCount < count ? baseMusicCount : count - 1
                musicPath = AgoraConfig.SoundSelectKTV[musicIndex]
            case .game:
                count = AgoraConfig.SoundSelectGame.count
                musicIndex = baseMusicCount < count ? baseMusicCount : count - 1
                musicPath = AgoraConfig.SoundSelectGame[musicIndex]
            case .anchor:
                count = AgoraConfig.SoundSelectAnchor.count
                musicIndex = baseMusicCount < count ? baseMusicCount : count - 1
                musicPath = AgoraConfig.SoundSelectAnchor[musicIndex]
            case .sound:
                return
            }
            if baseMusicCount >= count {
                rtcKit.stopAudioMixing()
                delegate?.reportAlien?(with: .ended, musicType: musicType)
            } else {
                if musicPath.contains("-B-") {
                    delegate?.reportAlien?(with: .blue, musicType: musicType)
                } else if musicPath.contains("-R-") {
                    delegate?.reportAlien?(with: .red, musicType: musicType)
                } else if musicPath.contains("-B&R-") {
                    delegate?.reportAlien?(with: .blueAndRed, musicType: musicType)
                }
                rtcKit.startAudioMixing(musicPath, loopback: false, cycle: 1)
            }
        }
    }

    // init rtc
    let rtcKit: AgoraRtcEngineKit = AgoraRtcEngineKit.sharedEngine(withAppId: KeyCenter.AppId, delegate: nil)

    /**
     *Set up RTC roles
     *@ param role RMCRoleType
     */
    @objc public func setClientRole(role: ASRoleType) {
        rtcKit.setClientRole(role == .audience ? .audience : .broadcaster)
        self.role = role
    }

    /**
     *Join the chat room
     *@ param channelName Channel Name
     *@ param rtcUid RTCUid If 0 is passed, the big network will automatically allocate it
     *@ param rtmUid is optional. If RTM is not used, use your own IM, and this value does not need to be passed
     *@ param type has four types: social, ktv, game, and anchor
     */
    public func joinVoicRoomWith(with channelName: String,token: String?, rtcUid: Int?, type: VMMUSIC_TYPE) -> Int32 {
        self.type = .VoiceChat
        rtcKit.delegate = self
        rtcKit.enableAudioVolumeIndication(200, smooth: 3, reportVad: true)
        self.setParametersWithMD()
        if type == .ktv || type == .social {
            rtcKit.setChannelProfile(.liveBroadcasting)

            rtcKit.setAudioProfile(.musicHighQuality)
            rtcKit.setAudioScenario(.gameStreaming)
        } else if type == .game {
            rtcKit.setChannelProfile(.communication)
        } else if type == .anchor {
            rtcKit.setChannelProfile(.liveBroadcasting)
            rtcKit.setAudioProfile(.musicHighQualityStereo)
            rtcKit.setAudioScenario(.gameStreaming)
            rtcKit.setParameters("{\"che.audio.custom_payload_type\":73}")
            rtcKit.setParameters("{\"che.audio.custom_bitrate\":128000}")
            //  rtcKit.setRecordingDeviceVolume(128)
            rtcKit.setParameters("{\"che.audio.input_channels\":2}")
        }
        setAINS(with: .mid)
        rtcKit.setParameters("{\"che.audio.start_debug_recording\":\"all\"}")
        rtcKit.setParameters("{\"che.audio.input_sample_rate\":48000}")
        rtcKit.setEnableSpeakerphone(true)
        rtcKit.setDefaultAudioRouteToSpeakerphone(true)
        let mediaOption = AgoraRtcChannelMediaOptions()
        mediaOption.publishMicrophoneTrack = role != .audience
        mediaOption.publishCameraTrack = false
        mediaOption.autoSubscribeAudio = true
        mediaOption.autoSubscribeVideo = false
        mediaOption.clientRoleType = role == .audience ? .audience : .broadcaster
        return rtcKit.joinChannel(byToken: token, channelId: channelName, uid: UInt(rtcUid ?? 0), mediaOptions: mediaOption)
    }

    /**
     *Load RTC
     *@ param channelName Channel Name
     *@ param rtcUid RTCUid If 0 is passed, the big network will automatically allocate it
     */
    private func loadKit(with channelName: String, rtcUid: Int?) {
        rtcKit.delegate = self
    }

    private func setParametersWithMD (){
        rtcKit.setParameters("{\"che.audio.md.enable\":false}")

    }
    
    public func setParameters(with string: String) {
        rtcKit.setParameters(string)
    }
    
    //Dump full link audio data collection
    public func setAPMOn(isOn: Bool){
        rtcKit.setParameters("{\"rtc.debug.enable\": \(isOn)}")
        rtcKit.setParameters("{\"che.audio.frame_dump\":{\"location\":\"all\",\"action\":\"start\",\"max_size_bytes\":\"120000000\",\"uuid\":\"123456789\",\"duration\":\"1200000\"}}");
    }
    
    /**
     *Load RTC
     */
    private func loadRTC(with channalName: String, uid: Int) {
        rtcKit.enableVideo()
        rtcKit.startPreview()
    }

    /**
     *Turn on/off local audio
     *Does @ param enable enable audio
     *@ return The result of turning on/off audio
     */
    @discardableResult
    public func enableLocalAudio(enable: Bool) -> Int32 {
        return rtcKit.enableLocalAudio(enable)
    }
    
    public func initMusicControlCenter() {
        let contentCenterConfiguration = AgoraMusicContentCenterConfig()
        contentCenterConfiguration.appId = KeyCenter.AppId
        contentCenterConfiguration.mccUid = Int(VLUserCenter.user.id) ?? 0
        contentCenterConfiguration.token = AppContext.shared.agoraRTMToken
        contentCenterConfiguration.rtcEngine = rtcKit
        
        mcc = AgoraMusicContentCenter.sharedContentCenter(config: contentCenterConfiguration)
        mcc?.register(self)
        
        musicPlayer = mcc?.createMusicPlayer(delegate: self)

        musicPlayer?.adjustPlayoutVolume(50)
        musicPlayer?.adjustPublishSignalVolume(50)
    }
    
    func fetchMusicList(musicListCallback: @escaping MusicListCallback) {
        if mcc == nil {
            initMusicControlCenter()
        }
        let jsonOption = "{\"pitchType\":1,\"needLyric\":false}"
        let requestId = mcc?.getMusicCollection(musicChartId: 3, page: 0, pageSize: 20, jsonOption: jsonOption)
        onMusicChartsIdCache[requestId ?? ""] = musicListCallback
    }
    
    func playMusic(songCode: Int, startPos: Int = 0) {
        if musicPlayer?.getPlayerState() == .paused  {
            musicPlayer?.resume()
        } else if musicPlayer?.getPlayerState() == .playing {
            musicPlayer?.pause()
        } else {
            mediaPlayer?.pause()
            musicPlayer?.stop()
            let mediaOption = AgoraRtcChannelMediaOptions()
            mediaOption.publishMediaPlayerId = Int(musicPlayer?.getMediaPlayerId() ?? 0)
            mediaOption.publishMediaPlayerAudioTrack = true
            rtcKit.updateChannel(with: mediaOption)
            lastSongCode = songCode
            if let mcc = mcc, mcc.isPreloaded(songCode: songCode) != 0 {
                mcc.preload(songCode: songCode)
            } else {
                musicPlayer?.openMedia(songCode: songCode, startPos: startPos)
                downloadBackgroundMusicStatusClosure?(songCode, 100, .OK)
            }
        }
    }

    /**
     *Stop playing songs
     */
    @objc public func stopMusic() {
        let mediaOption = AgoraRtcChannelMediaOptions()
        mediaOption.publishMediaPlayerAudioTrack = false
        rtcKit.updateChannel(with: mediaOption)
        if musicPlayer?.getPlayerState() != .stopped {
            musicPlayer?.stop()
        }
    }
    
    /**
     *Resume playback
     */
    public func resumeMusic() {
        if musicPlayer?.getPlayerState() == .paused {
            musicPlayer?.resume()
        } else {
            musicPlayer?.play()
        }
    }

    /**
     *Pause playback
     */
    public func pauseMusic() {
        musicPlayer?.pause()
    }

    /**
     *Adjusting progress
     */
    public func seekMusic(time: NSInteger) {
       musicPlayer?.seek(toPosition: time)
        
    }
    
    /**
     *Adjusting volume
     */
    public func adjustMusicVolume(volume: Int) {
        musicPlayer?.adjustPlayoutVolume(Int32(volume))
        musicPlayer?.adjustPublishSignalVolume(Int32(volume))
    }

    /**
     *Select audio tracks, original and backing vocals
     */
    public func selectPlayerTrackMode(isOrigin: Bool) {
        musicPlayer?.selectAudioTrack(isOrigin ? 1: 0)
    }

    /**
     *
     *
     */
    public func playMusic(with type: VMMUSIC_TYPE) {
        let code = rtcKit.stopAudioMixing()
        if code == 0 {
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.03) {
                self.musicType = type
                self.baseMusicCount = 0
            }
        }
    }

    public func stopPlayMusic() {
        guard let musicType = musicType else {
            return
        }
        delegate?.reportAlien?(with: .none, musicType: musicType)
        if musicType == .alien {
            baseMusicCount = AgoraConfig.baseAlienMic.count
        } else if musicType == .ainsHigh {
            baseMusicCount = AgoraConfig.HighAINSIntroduc.count
        } else if musicType == .ainsMid {
            baseMusicCount = AgoraConfig.MediumAINSIntroduc.count
        } else if musicType == .ainsOff {
            baseMusicCount = AgoraConfig.NoneAINSIntroduc.count
        } else if musicType == .social {
            baseMusicCount = AgoraConfig.SoundSelectSocial.count
        } else if musicType == .ktv {
            baseMusicCount = AgoraConfig.SoundSelectKTV.count
        } else if musicType == .game {
            baseMusicCount = AgoraConfig.SoundSelectGame.count
        } else if musicType == .anchor {
            baseMusicCount = AgoraConfig.SoundSelectAnchor.count
        }
    }

    public func playSound(with index: Int, type: VMMUSIC_TYPE) {
        stopPlayMusic()
        musicType = type
        var path = ""
        if type == .ainsHigh {
            path = AgoraConfig.HighSound[index]
        } else if type == .ainsOff {
            path = AgoraConfig.NoneSound[index]
        }
        rtcKit.startAudioMixing(path, loopback: false, cycle: 1)
    }

    public func stopPlaySound() {
        rtcKit.stopAudioMixing()
    }

    /**
     *Turn on/off echo cancellation
     *Does @ param enable enable echo cancellation
     *@ return The result of turning on/off echo cancellation
     */
    @discardableResult
    public func enableAEC(with grade: AECGrade) -> Int32 {
        return rtcKit.setParameters("{\"rtc.audio.music_mode\": \(grade.rawValue)}")
    }
    
    //AIAEC-AI Echo Cancellation
    public func setAIAECOn(isOn:Bool){
        //agora_ai_echo_cancellation
//        rtcKit.enableExtension(withVendor: "agora_ai_echo_cancellation", extension: "", enabled: true)
        
        if (isOn){
            rtcKit.setParameters("{\"che.audio.aiaec.working_mode\":1}");

        } else {
            rtcKit.setParameters("{\"che.audio.aiaec.working_mode\":0}");

        }
    }

    //AGC - Added automatic vocal gain switch
    public func setAGCOn(isOn:Bool){
        if (isOn) {
            rtcKit.setParameters("{\"che.audio.agc.enable\":true}")
        } else {
            rtcKit.setParameters("{\"che.audio.agc.enable\":false}")
        }
        rtcKit.setParameters("{\"che.audio.agc.targetlevelBov\":3}")
        rtcKit.setParameters("{\"che.audio.agc.compressionGain\":18}")
    }
    
    /**
     *Turn AI noise reduction on/off
     *@ param
     *@ return The result of turning on/off echo cancellation
     */
    public func setAINS(with level: AINS_STATE) {
        switch level {
        case .high:
            rtcKit.setParameters("{\"che.audio.ains_mode\":2}")
            rtcKit.setParameters("{\"che.audio.nsng.lowerBound\":10}")
            rtcKit.setParameters("{\"che.audio.nsng.lowerMask\":10}")
            rtcKit.setParameters("{\"che.audio.nsng.statisticalbound\":0}")
            rtcKit.setParameters("{\"che.audio.nsng.finallowermask\":8}")
            rtcKit.setParameters("{\"che.audio.nsng.enhfactorstastical\":200}")
        case .mid:
            rtcKit.setParameters("{\"che.audio.ains_mode\":2}")
            rtcKit.setParameters("{\"che.audio.nsng.lowerBound\":80}")
            rtcKit.setParameters("{\"che.audio.nsng.lowerMask\":50}")
            rtcKit.setParameters("{\"che.audio.nsng.statisticalbound\":5}")
            rtcKit.setParameters("{\"che.audio.nsng.finallowermask\":30}")
            rtcKit.setParameters("{\"che.audio.nsng.enhfactorstastical\":200}")
        case .off:
            rtcKit.setParameters("{\"che.audio.ains_mode\":-1}")
            rtcKit.setParameters("{\"che.audio.nsng.lowerBound\":80}")
            rtcKit.setParameters("{\"che.audio.nsng.lowerMask\":50}")
            rtcKit.setParameters("{\"che.audio.nsng.statisticalbound\":5}")
            rtcKit.setParameters("{\"che.audio.nsng.finallowermask\":30}")
            rtcKit.setParameters("{\"che.audio.nsng.enhfactorstastical\":200}")
        }
    }

    /**
     *Turn on/off ear return
     *@ param
     * @Return The result of turning on/off the earlobe
     */
    public func setInEarMode(with mode: INEAR_MODE) {
        switch mode {
        case .auto:
            rtcKit.setParameters("{\"opensl che.audio.opensl.mode\": 0}")
            rtcKit.setParameters("{\"oboe che.audio.oboe.enable\": 0}")
        case .opneSL:
            rtcKit.setParameters("{\"opensl che.audio.opensl.mode\": 1}")
        case .oboe:
            rtcKit.setParameters("{\"oboe che.audio.oboe.enable\": 1}")
        }
    }
    
    /**
     * Turn on/off local videos
     * @Does param enable enable video
     * @Return The result of turning on/off the video
     */
    @discardableResult
    public func enableLocalVideo(enable: Bool) -> Int32 {
        return rtcKit.enableLocalVideo(enable)
    }

    /**
     * Cancel or resume publishing local audio streams
     * @Does param enable publish local audio streams
     * @Return Cancel or restore the result of publishing local audio streams
     */
    @discardableResult
    public func muteLocalAudioStream(mute: Bool) -> Int32 {
        let mediaOption = AgoraRtcChannelMediaOptions()
        mediaOption.publishMicrophoneTrack = !mute
        return rtcKit.updateChannel(with: mediaOption)
    }

    /**
     * Cancel or resume publishing local video streams
     * @Does param enable publish local video streams
     * @Return Cancel or restore the result of publishing local video streams
     */
    @discardableResult
    public func muteLocalVideoStream(mute: Bool) -> Int32 {
        let mediaOption = AgoraRtcChannelMediaOptions()
        mediaOption.publishCameraTrack = !mute
        return rtcKit.updateChannel(with: mediaOption)
    }

    /**
     * Open ear return
     * @Does param enable enable ear return
     * @Return The result of turning on/off the earlobe
     */
    @discardableResult
    public func enableinearmonitoring(enable: Bool) -> Int32 {
        return rtcKit.enable(inEarMonitoring: enable, includeAudioFilters: .noiseSuppression)
    }

    /**
     * Set ear return volume
     * @Param volume earlobe volume value
     * @Return The result of setting the early volume
     */
    @discardableResult
    public func setInEarMonitoringVolume(with volume: Int) -> Int32 {
        return rtcKit.setInEarMonitoringVolume(volume)
    }

    /**
     * Set user's local collection volume
     * @Param volume volume value
     * @Return Set the result of the user's local collection volume
     */
    @discardableResult
    public func adjustRecordingSignalVolume(with volume: Int) -> Int32 {
        return rtcKit.adjustRecordingSignalVolume(volume)
    }

    /**
     * Set the volume of the specified remote user for local playback
     * @Param volume volume value
     * @The user's UID that needs to be set for the param UID
     * @Return The result of setting the volume of the specified remote user for local playback
     */
    @discardableResult
    public func adjustUserPlaybackSignalVolume(with uid: UInt, volume: Int32) -> Int32 {
        return rtcKit.adjustUserPlaybackSignalVolume(uid, volume: volume)
    }

    /**
     * Set up bel canto
     * @Parameter configuration of params bel canto
     * @Return Set the result of bel canto
     */
    @discardableResult
    public func setVoiceBeautifierParameters(with preset: AgoraVoiceBeautifierPreset) -> Int32 {
        return rtcKit.setVoiceBeautifierPreset(preset)
    }

    /**
     * Set preset bel canto effects
     * @Param preset parameter configuration for bel canto
     * @The gender characteristics of param param1 singing:
        1: Male voice
        2: Female voice
     * The reversal effect of @ param param2 singing:
        1: The reversal effect of singing in a small room
        2: The reversal effect of singing in a large room
        3: The reversal effect of singing in the hall

     * @Return The result of setting the preset bel canto effect
     */
    @discardableResult
    public func setVoiceBeautifierParameters(with preset: AgoraVoiceBeautifierPreset, param1: Int32, param2: Int32) -> Int32 {
        return rtcKit.setVoiceBeautifierParameters(.presetSingingBeautifier, param1: param1, param2: param2)
    }

    /**
     * Set voice change
     * @Parameter Configuration of Param Params Sound Transformation
     * @Return Set the result of voice change
     */
    //   @discardableResult
//    public func setLocalVoiceChanger(with voiceChanger: AgoraAudioVoiceChanger) -> Int32 {
//        return rtcKit.setLocalVoiceChanger(voiceChanger)
//    }

    /**
     * Set local video view
     * @Parameter configuration for param local local canvas
     * @Return The result of setting the local video view
     */
    @discardableResult
    public func setupLocalVideo(local: AgoraRtcVideoCanvas?) -> Int32 {
        return rtcKit.setupLocalVideo(local ?? AgoraRtcVideoCanvas())
    }

    /**
     * Set remote video view
     * @Param remote parameter configuration for remote canvas
     * @Return The result of setting the remote video view
     */
    @discardableResult
    public func setupRemoteVideo(remote: AgoraRtcVideoCanvas?) -> Int32 {
        return rtcKit.setupRemoteVideo(remote ?? AgoraRtcVideoCanvas())
    }

    /**
     * Send dataStream message
     * @Param data sent data
     * @Return The result of sending a dataStream message
     */
    @discardableResult
    @objc public func sendStreamMessage(with data: Data) -> Int32 {
        return rtcKit.sendStreamMessage(streamId, data: data)
    }

    /**
     * Open music
     * @The local or online address of the param URL music
     * @Where does param startPos music start playing in milliseconds
     * @Return The result of opening the music
     */
    @discardableResult
    @objc public func open(with url: String, startPos: Int) -> Int32 {
        mediaPlayer?.setLoopCount(-1)
        return mediaPlayer?.open(url, startPos: startPos) ?? -1
    }

    /**
     *Play music
     *@ return The result of playing music
     */
    @discardableResult
    @objc public func play() -> Int32 {
        musicPlayer?.pause()
        return mediaPlayer?.play() ?? -1
    }

    /**
     *Pause playback
     *@ return The result of pausing playback
     */
    @discardableResult
    @objc public func pause() -> Int32 {
        return mediaPlayer?.pause() ?? -1
    }

    /**
     *Stop playing
     *@ return The result of stopping playback
     */
    @discardableResult
    @objc public func stop() -> Int32 {
        return mediaPlayer?.stop() ?? -1
    }

    /**
     *Set up music channels
     *@ return The result of setting the music channel
     */
    @discardableResult
    @objc public func setAudioDualMonoMode(with mode: AgoraAudioDualMonoMode) -> Int32 {
        return Int32((mediaPlayer?.setAudioDualMonoMode(mode))!)
    }

    /**
     * Teacher, students set the accounting volume
     * @Param volume accompaniment volume value
     * @Return The result of setting the accounting volume
     */
    @discardableResult
    public func adjustPlayoutVolume(with volume: Int32) -> Int32 {
        return mediaPlayer?.adjustPlayoutVolume(volume) ?? -1
    }

    @discardableResult
    public func adjustAudioMixingVolume(with volume: Int) -> Int32 {
        return rtcKit.adjustAudioMixingVolume(volume)
    }

    /**
     *Get the playback status of MPK
     *The result of @ return MPK's playback status
     */
    @discardableResult
    public func getPlayerState() -> AgoraMediaPlayerState {
        return mediaPlayer?.getPlayerState() ?? .failed
    }

    /**
     *Get playback progress
     *@ return Get the result of playback progress
     */
    @discardableResult
    public func getPosition() -> Int {
        return mediaPlayer?.getPosition() ?? 0
    }

    /**
     *Obtain the total duration of the song
     *@ return Get the result of the total duration of the song
     */
    @discardableResult
    public func getDuration() -> Int {
        return mediaPlayer?.getDuration() ?? 0
    }

    /**
     *Set song playback progress
     *@ return The result of setting the playback progress of the song
     */
    @discardableResult
    public func seek(to position: Int) -> Int32 {
        return (mediaPlayer?.seek(toPosition: position))!
    }

    /**
     *Leave the channel and release resources
     */
    @objc public func leaveChannel() {
        rtcKit.stopPreview()
        rtcKit.leaveChannel(nil)
        rtcKit.delegate = nil
        selectPlayerTrackMode(isOrigin: true)
        musicPlayer?.stop()
        musicPlayer = nil
        mcc = nil
        if !backgroundMusics.isEmpty {
            backgroundMusics.removeAll()
        }
        AgoraMusicContentCenter.destroy()
        AgoraRtcEngineKit.destroy()
        VoiceRoomRTCManager._sharedInstance = nil //Release singleton
    }
}

// MARK: - AgoraRtcEngineDelegate

extension VoiceRoomRTCManager: AgoraRtcEngineDelegate {
    // remote joined
    public func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        if role == .coHost && type == .KTV && uid == kMPK_RTC_UID {
            _ = rtcKit.muteRemoteAudioStream(kMPK_RTC_UID, mute: true)
        }

        guard let _ = delegate else {
            return
        }

        if uid == kMPK_RTC_UID {
            return
        }

        delegate?.didRtcRemoteUserJoinedOfUid?(uid: uid)
    }

    // remote offline
    public func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        guard let _ = delegate else {
            return
        }

        delegate?.didRtcUserOfflineOfUid?(uid: uid)
    }

    // local joined
    public func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        localRtcUid = uid

        guard let _ = delegate else {
            return
        }

        if uid == kMPK_RTC_UID {
            return
        }

        delegate?.didRtcLocalUserJoinedOfUid?(uid: uid)
    }
    
    public func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        
    }

    // dataStream received
    public func rtcEngine(_ engine: AgoraRtcEngineKit, receiveStreamMessageFromUid uid: UInt, streamId: Int, data: Data) {
        playerDelegate?.didReceiveStreamMsgOfUid?(uid: uid, data: data)
    }

    
    public func rtcEngine(_ engine: AgoraRtcEngineKit, reportAudioVolumeIndicationOfSpeakers speakers: [AgoraRtcAudioVolumeInfo], totalVolume: Int) {
        guard let _ = delegate else {
            return
        }

        //If Uid=0, it means it is the voice of a local user
        var real_speakers: [AgoraRtcAudioVolumeInfo] = speakers
        for (index, value) in speakers.enumerated() {
            if value.uid == kMPK_RTC_UID {
                real_speakers.remove(at: index)
            }

            if value.uid == 0 {
                real_speakers[index].uid = localRtcUid
            }
        }
        delegate?.reportAudioVolumeIndicationOfSpeakers?(speakers: real_speakers)
    }
}

// MARK: - AgoraRtcMediaPlayerDelegate

extension VoiceRoomRTCManager: AgoraRtcMediaPlayerDelegate {
    public func rtcEngine(_ engine: AgoraRtcEngineKit, audioMixingStateChanged state: AgoraAudioMixingStateType, reasonCode: AgoraAudioMixingReasonCode) {
        if state == .stopped {
            if self.stopMixingClosure != nil {
                self.stopMixingClosure!()
            }
            guard let musicType = musicType else { return }
            var count = 0
            switch musicType {
            case .alien:
                count = AgoraConfig.baseAlienMic.count
            case .ainsHigh:
                count = AgoraConfig.HighAINSIntroduc.count
            case .ainsMid:
                count = AgoraConfig.MediumAINSIntroduc.count
            case .ainsOff:
                count = AgoraConfig.NoneAINSIntroduc.count
            case .sound:
                delegate?.reportAlien?(with: .none, musicType: .sound)
                return
            case .social:
                count = AgoraConfig.SoundSelectSocial.count
            case .ktv:
                count = AgoraConfig.SoundSelectKTV.count
            case .game:
                count = AgoraConfig.SoundSelectGame.count
            case .anchor:
                count = AgoraConfig.SoundSelectAnchor.count
            }
            if baseMusicCount < count {
                baseMusicCount += 1
            }
        }
    }

    // mpk didChangedToPosition
    public func agoraRtcMediaPlayer(_ playerKit: AgoraRtcMediaPlayerProtocol, didChangedToPosition position: Int) {
        guard let _ = delegate else {
            return
        }

        playerDelegate?.didMPKChangedToPosition?(position: position)
    }

    // mpk didChangedTo
    public func AgoraRtcMediaPlayer(_ playerKit: AgoraRtcMediaPlayerProtocol, didChangedTo state: AgoraMediaPlayerState, reason: AgoraMediaPlayerReason) {
        if delegate != nil {
            playerDelegate?.didMPKChangedTo?(state: state, reason: reason)
        }
        if let musicPlayer = musicPlayer, state == .openCompleted {
            musicPlayer.play()
        }
        backgroundMusicPlayingStatusClosure?(state)
    }
}

extension VoiceRoomRTCManager: AgoraMusicContentCenterEventDelegate {
    public func onMusicChartsResult(_ requestId: String, result: [AgoraMusicChartInfo], reason: AgoraMusicContentCenterStateReason) {
        print("songCode == \(result)")
    }
    
    public func onMusicCollectionResult(_ requestId: String, result: AgoraMusicCollection, reason: AgoraMusicContentCenterStateReason) {
        guard let callback = onMusicChartsIdCache[requestId] else { return }
        backgroundMusics = result.musicList
        DispatchQueue.main.async(execute: {
            callback(result.musicList)
        })
    }
    
    public func onLyricResult(_ requestId: String, songCode: Int, lyricUrl: String?, reason: AgoraMusicContentCenterStateReason) {
        print("songCode == \(songCode)")
    }
    
    public func onSongSimpleInfoResult(_ requestId: String, songCode: Int, simpleInfo: String?, reason: AgoraMusicContentCenterStateReason) {
        print("songCode == \(songCode)")
    }
    
    public func onPreLoadEvent(_ requestId: String, songCode: Int, percent: Int, lyricUrl: String?, state: AgoraMusicContentCenterPreloadState, reason: AgoraMusicContentCenterStateReason) {
        delegate?.downloadBackgroundMusicStatus?(songCode: songCode, progress: percent, state: state)
        downloadBackgroundMusicStatusClosure?(songCode, percent, state)
        if state == .OK, lastSongCode == songCode {
            musicPlayer?.openMedia(songCode: songCode, startPos: 0)
        }
    }
}
