//
//  CommerceAgoraKitManager.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/22.
//

import Foundation
import AgoraRtcKit
import UIKit
import YYCategories
import VideoLoaderAPI

class CommerceAgoraKitManager: NSObject {
    
    static let shared = CommerceAgoraKitManager()
    
    // Whether to enable the green screen function
    static var isOpenGreen: Bool = false
    static var isBlur: Bool = false
    
    public let rtcParam = CommerceRTCParams()
    public var deviceLevel: DeviceLevel = .medium
    public var deviceScore: Int = 100
    public var netCondition: NetCondition = .good
    public var performanceMode: PerformanceMode = .smooth
    
    private var broadcasterConnection: AgoraRtcConnection?
    
    var exposureRangeX: Int?
    var exposureRangeY: Int?
    var matrixCoefficientsExt: Int?
    var videoFullrangeExt: Int?
    
    let encoderConfig = AgoraVideoEncoderConfiguration()
    
    public lazy var captureConfig: AgoraCameraCapturerConfiguration = {
        let config = AgoraCameraCapturerConfiguration()
        config.followEncodeDimensionRatio = true
        config.cameraDirection = .front
        config.frameRate = 15
        return config
    }()
    
    public var engine: AgoraRtcEngineKit?
    
    private var player: AgoraRtcMediaPlayerProtocol?
    func mediaPlayer() -> AgoraRtcMediaPlayerProtocol? {
        if let p = player {
            return p
        } else {
            player = engine?.createMediaPlayer(with: self)
            player?.setLoopCount(-1)
            return player
        }
    }
    
    func prepareEngine() {
        let engine = AgoraRtcEngineKit.sharedEngine(with: engineConfig(), delegate: nil)
        self.engine = engine
        let loader = VideoLoaderApiImpl.shared
        loader.addListener(listener: self)
        let config = VideoLoaderConfig()
        config.rtcEngine = engine
        loader.setup(config: config)
        
        commerceLogger.info("load AgoraRtcEngineKit, sdk version: \(AgoraRtcEngineKit.getSdkVersion())", context: kCommerceLogBaseContext)
    }
    
    func destoryEngine() {
        AgoraRtcEngineKit.destroy()
        commerceLogger.info("deinit-- CommerceAgoraKitManager")
    }

    func leaveAllRoom() {
        VideoLoaderApiImpl.shared.cleanCache()
        if let p = player {
            engine?.destroyMediaPlayer(p)
            player = nil
        }
    }
    
    //MARK: private
    private func engineConfig() -> AgoraRtcEngineConfig {
        let config = AgoraRtcEngineConfig()
         config.appId = KeyCenter.AppId
         config.channelProfile = .liveBroadcasting
         config.areaCode = .global
         return config
    }
    
    private func setupContentInspectConfig(_ enable: Bool, connection: AgoraRtcConnection) {
        let config = AgoraContentInspectConfig()
        let dic: [String: String] = [
            "id": VLUserCenter.user.id,
            "sceneName": "commerce",
            "userNo": VLUserCenter.user.userNo
        ]
        
        guard let jsonData = try? JSONSerialization.data(withJSONObject: dic, options: .prettyPrinted) else {
            commerceLogger.error("setupContentInspectConfig fail")
            return
        }
        let jsonStr = String(data: jsonData, encoding: .utf8)
        config.extraInfo = jsonStr
        let module = AgoraContentInspectModule()
        module.interval = 30
        module.type = .imageModeration
        config.modules = [module]
        let ret = engine?.enableContentInspectEx(enable, config: config, connection: connection)
        commerceLogger.info("setupContentInspectConfig: \(ret ?? -1)")
    }
    
    private func _joinChannelEx(currentChannelId: String,
                                targetChannelId: String,
                                ownerId: UInt,
                                token: String,
                                options:AgoraRtcChannelMediaOptions,
                                role: AgoraClientRole) {
        let localUid = UInt(VLUserCenter.user.id)!
        if role == .audience {
            let anchorInfo = getAnchorInfo(channelId: targetChannelId, uid: ownerId)
            let newState: AnchorState = broadcasterConnection == nil ? .prejoined : .joinedWithVideo
            VideoLoaderApiImpl.shared.switchAnchorState(newState: newState, localUid: localUid, anchorInfo: anchorInfo, tagId: currentChannelId)
            return
        }
        
        guard let engine = engine else {
            assert(true, "rtc engine not initlized")
            return
        }

        if let _ = broadcasterConnection {
            return
        }
        
        let mediaOptions = AgoraRtcChannelMediaOptions()
        mediaOptions.publishCameraTrack = true
        mediaOptions.publishMicrophoneTrack = true
        mediaOptions.autoSubscribeAudio = true
        mediaOptions.autoSubscribeVideo = true
        mediaOptions.clientRoleType = .broadcaster

        updateVideoEncoderConfigurationForConnenction(currentChannelId: currentChannelId)

        let connection = AgoraRtcConnection()
        connection.channelId = targetChannelId
        connection.localUid = localUid

        let proxy = VideoLoaderApiImpl.shared.getRTCListener(anchorId: currentChannelId)
        let date = Date()
        commerceLogger.info("try to join room[\(connection.channelId)] ex uid: \(connection.localUid)", context: kCommerceLogBaseContext)
        let ret =
        engine.joinChannelEx(byToken: token,
                               connection: connection,
                               delegate: proxy,
                               mediaOptions: mediaOptions) {[weak self] channelName, uid, elapsed in
            let cost = Int(-date.timeIntervalSinceNow * 1000)
            commerceLogger.info("join room[\(channelName)] ex success uid: \(uid) cost \(cost) ms", context: kCommerceLogBaseContext)
            self?.setupContentInspectConfig(true, connection: connection)

//            self?.moderationAudio(channelName: targetChannelId, role: role)
            self?.applySimulcastStream(connection: connection)
        }
        engine.updateChannelEx(with: mediaOptions, connection: connection)
        broadcasterConnection = connection

        if ret == 0 {
            commerceLogger.info("join room ex: channelId: \(targetChannelId) ownerId: \(ownerId)",
                            context: "AgoraKitManager")
        }else{
            commerceLogger.error("join room ex fail: channelId: \(targetChannelId) ownerId: \(ownerId) token = \(token), \(ret)",
                             context: kCommerceLogBaseContext)
        }
    }
    
    func updateVideoEncoderConfigurationForConnenction(currentChannelId: String) {
        guard let engine = engine else {
            assert(true, "rtc engine not initlized")
            return
        }
        let connection = AgoraRtcConnection()
        connection.channelId = currentChannelId
        connection.localUid = UInt(VLUserCenter.user.id) ?? 0
        let encoderRet = engine.setVideoEncoderConfigurationEx(encoderConfig, connection: connection)
        commerceLogger.info("setVideoEncoderConfigurationEx  dimensions = \(encoderConfig.dimensions), bitrate = \(encoderConfig.bitrate), fps = \(encoderConfig.frameRate),  encoderRet = \(encoderRet)", context: kCommerceLogBaseContext)
    }
    
    //MARK: public method
    func addRtcDelegate(delegate: AgoraRtcEngineDelegate, roomId: String) {
        VideoLoaderApiImpl.shared.addRTCListener(anchorId: roomId, listener: delegate)
    }
    
    func removeRtcDelegate(delegate: AgoraRtcEngineDelegate, roomId: String) {
        VideoLoaderApiImpl.shared.removeRTCListener(anchorId: roomId, listener: delegate)
    }
    
    func renewToken(channelId: String) {
        commerceLogger.info("renewToken with channelId: \(channelId)",
                        context: kCommerceLogBaseContext)
        NetworkManager.shared.generateToken(channelName: channelId,
                                            uid: UserInfo.userId,
                                            tokenType: .token007,
                                            type: .rtc) {[weak self] token in
            guard let token = token else {
                commerceLogger.error("renewToken fail: token is empty")
                return
            }
            let option = AgoraRtcChannelMediaOptions()
            option.token = token
            AppContext.shared.rtcToken = token
            self?.updateChannelEx(channelId: channelId, options: option)
        }
    }
    
    private var callTimeStampsSaved: Date?
    private var callTimestampEndSaved: TimeInterval?

    func callTimestampStart() {
        callTimeStampsSaved = Date()
    }
    
    func callTimestampEnd() -> TimeInterval? {
        guard let saved = callTimeStampsSaved else {
            return callTimestampEndSaved
        }
        let value = -saved.timeIntervalSinceNow * 1000
        callTimeStampsSaved = nil
        callTimestampEndSaved = value
        return value
    }
    
    //MARK: public sdk method
    /// Initialize and preview
    /// -Parameter canvasView: the canvas
    func startPreview(canvasView: UIView) {
        guard let engine = engine else {
            assert(true, "rtc engine not initlized")
            return
        }
        isFrontCamera = true
        engine.setClientRole(.broadcaster)
        engine.setVideoEncoderConfiguration(encoderConfig)
        engine.setCameraCapturerConfiguration(captureConfig)
        let canvas = AgoraRtcVideoCanvas()
        canvas.view = canvasView
        canvas.uid = 0
        canvas.renderMode = .hidden
        canvas.mirrorMode = .disabled
        engine.setupLocalVideo(canvas)
        engine.enableVideo()
        engine.startPreview()
        setMirrorMode(mode: .enabled)
    }
    
    private var isFrontCamera: Bool = true
    func switchCamera(_ channelId: String? = nil) {
        isFrontCamera = !isFrontCamera
        engine?.switchCamera()
        setMirrorMode(mode: isFrontCamera ? .enabled : .disabled)
    }
    
    func updateChannelEx(channelId: String, options: AgoraRtcChannelMediaOptions) {
        guard let engine = engine,
              let connection = (broadcasterConnection?.channelId == channelId ? broadcasterConnection : nil) ?? VideoLoaderApiImpl.shared.getConnectionMap()[channelId] else {
            commerceLogger.error("updateChannelEx fail: connection is empty")
            return
        }
        commerceLogger.info("updateChannelEx[\(channelId)]: \(options.publishMicrophoneTrack) \(options.publishCameraTrack)")
        engine.updateChannelEx(with: options, connection: connection)
    }
    
    func switchRole(role: AgoraClientRole,
                    channelId: String,
                    options:AgoraRtcChannelMediaOptions,
                    uid: String?,
                    canvasView: UIView?) {
        guard let uid = UInt(uid ?? ""), let canvasView = canvasView else {
            commerceLogger.error("switchRole fatel")
            return
        }
        options.clientRoleType = role
        options.audienceLatencyLevel = role == .audience ? .lowLatency : .ultraLowLatency
        updateChannelEx(channelId:channelId, options: options)
        if "\(uid)" == VLUserCenter.user.id {
            if role == .broadcaster {
                setupLocalVideo(uid: uid, canvasView: canvasView)
            } else {
                cleanCapture()
            }
        } else {
            setupRemoteVideo(channelId: channelId, uid: uid, canvasView: canvasView)
        }
    }
    
    func updateMediaOptions(publishCamera: Bool) {
        let mediaOptions = AgoraRtcChannelMediaOptions()
        mediaOptions.publishCameraTrack = publishCamera
        mediaOptions.publishMicrophoneTrack = false
        mediaOptions.clientRoleType = publishCamera ? .broadcaster : .audience
        engine?.updateChannel(with: mediaOptions)
    }
    func updateMediaOptionsEx(channelId: String, publishCamera: Bool, publishMic: Bool = false) {
        let mediaOptions = AgoraRtcChannelMediaOptions()
        mediaOptions.publishCameraTrack = publishCamera
        mediaOptions.publishMicrophoneTrack = publishMic
        mediaOptions.autoSubscribeAudio = publishMic
        mediaOptions.autoSubscribeVideo = publishCamera
        mediaOptions.clientRoleType = publishCamera ? .broadcaster : .audience
        let uid = Int(VLUserCenter.user.id) ?? 0
        let connection = AgoraRtcConnection(channelId: channelId, localUid: uid)
        engine?.updateChannelEx(with: mediaOptions, connection: connection)
    }
    
    /// Set encoding resolution
    /// -Parameter size: indicates the resolution
    func setVideoDimensions(_ size: CGSize){
        guard let engine = engine else {
            assert(true, "rtc engine not initlized")
            return
        }
        encoderConfig.dimensions = CGSize(width: size.width, height: size.height)
        engine.setVideoEncoderConfiguration(encoderConfig)
    }
    
    func cleanCapture() {
        guard let engine = engine else {
            assert(true, "rtc engine not initlized")
            return
        }
//        setupContentInspectConfig(false)
        engine.stopPreview()
    }
    
    func leaveChannelEx(roomId: String, channelId: String) {
        if let connection = broadcasterConnection, connection.channelId == channelId {
            engine?.leaveChannelEx(connection)
            broadcasterConnection = nil
            return
        }
        let anchorInfo = getAnchorInfo(channelId: channelId)
        VideoLoaderApiImpl.shared.switchAnchorState(newState: .idle, localUid: anchorInfo.uid, anchorInfo: anchorInfo, tagId: roomId)
    }
    
    func joinChannelEx(currentChannelId: String,
                       targetChannelId: String,
                       ownerId: UInt,
                       options:AgoraRtcChannelMediaOptions,
                       role: AgoraClientRole,
                       completion: (()->())?) {
        if let rtcToken = AppContext.shared.commerceRtcToken {
            _joinChannelEx(currentChannelId: currentChannelId,
                           targetChannelId: targetChannelId,
                           ownerId: ownerId,
                           token: rtcToken,
                           options: options,
                           role: role)
            completion?()
            return
        }
        
        NetworkManager.shared.generateToken(channelName: targetChannelId,
                                            uid: VLUserCenter.user.id,
                                            tokenType: .token007,
                                            type: .rtc) {[weak self] token in
            defer {
                completion?()
            }
            
            guard let token = token else {
                commerceLogger.error("joinChannelEx fail: token is empty")
                return
            }
            AppContext.shared.rtcToken = token
            self?._joinChannelEx(currentChannelId: currentChannelId,
                                 targetChannelId: targetChannelId,
                                 ownerId: ownerId,
                                 token: token,
                                 options: options,
                                 role: role)
        }
    }
    
    func setupLocalVideo(uid: UInt, canvasView: UIView) {
        guard let engine = engine else {
            assert(true, "rtc engine not initlized")
            return
        }
        let canvas = AgoraRtcVideoCanvas()
        canvas.view = canvasView
        canvas.uid = uid
        canvas.mirrorMode = .disabled
        engine.setupLocalVideo(canvas)
        engine.startPreview()
        engine.setDefaultAudioRouteToSpeakerphone(true)
        engine.enableLocalAudio(true)
        engine.enableLocalVideo(true)
        setMirrorMode(mode: isFrontCamera ? .enabled : .disabled)
        commerceLogger.info("setupLocalVideo target uid:\(uid), user uid\(UserInfo.userId)", context: kCommerceLogBaseContext)
    }
    
    func setupRemoteVideo(channelId: String, uid: UInt, canvasView: UIView?) {
        if let connection = broadcasterConnection, broadcasterConnection?.channelId == channelId {
            let videoCanvas = AgoraRtcVideoCanvas()
            videoCanvas.uid = uid
            videoCanvas.view = canvasView
            videoCanvas.renderMode = .hidden
            let ret = engine?.setupRemoteVideoEx(videoCanvas, connection: connection)
                    
            commerceLogger.info("setupRemoteVideoEx ret = \(ret ?? -1), uid:\(uid) localuid: \(UserInfo.userId) channelId: \(channelId)", context: kCommerceLogBaseContext)
            return
        }
        let anchorInfo = getAnchorInfo(channelId: channelId, uid: uid)
        let container = VideoCanvasContainer()
        container.uid = uid
        container.container = canvasView
        VideoLoaderApiImpl.shared.renderVideo(anchorInfo: anchorInfo, container: container)
    }
    
    func setMirrorMode(mode: AgoraVideoMirrorMode) {
        engine?.setParameters("{\"rtc.camera_capture_mirror_mode\":\(mode.rawValue)}")
    }
    
    func updateLoadingType(roomId: String, channelId: String, playState: AnchorState) {
        if broadcasterConnection?.channelId == channelId {return}
        let anchorInfo = getAnchorInfo(channelId: channelId)
        VideoLoaderApiImpl.shared.switchAnchorState(newState: playState, localUid: anchorInfo.uid, anchorInfo: anchorInfo, tagId: roomId)
    }
    
    func cleanChannel(without roomIds: [String]) {
        let videoLoader = VideoLoaderApiImpl.shared
        for (key, _) in videoLoader.getConnectionMap() {
            if roomIds.contains(key) {continue}
            let anchorInfo = AnchorInfo()
            anchorInfo.channelName = key
            anchorInfo.uid = UInt(VLUserCenter.user.id)!
            videoLoader.switchAnchorState(newState: .idle, localUid: anchorInfo.uid, anchorInfo: anchorInfo, tagId: key)
        }
    }
}

//MARK: private param
extension CommerceAgoraKitManager {
    
    func initBroadcasterConfig() {
        engine?.setParameters("{\"rtc.enable_crypto_access\":false}")
        engine?.setParameters("{\"rtc.use_global_location_priority_domain\":true}")
        engine?.setParameters("{\"che.video.has_intra_request\":false}")
        engine?.setParameters("{\"che.hardware_encoding\":1}")
        engine?.setParameters("{\"engine.video.enable_hw_encoder\":true}")
        engine?.setParameters("{\"che.video.keyFrameInterval\":2}")
        engine?.setParameters("{\"che.video.hw265_enc_enable\":1}")
        engine?.setParameters("{\"che.video.enable_first_frame_sw_decode\":true}")
        engine?.setParameters("{\"rtc.asyncCreateMediaEngine\":true}")
    }
    
    func initAudienceConfig() {
        engine?.setParameters("{\"rtc.enable_crypto_access\":false}")
        engine?.setParameters("{\"rtc.use_global_location_priority_domain\":true}")
        engine?.setParameters("{\"che.hardware_decoding\":0}")
        engine?.setParameters("{\"rtc.enable_nasa2\": false}")
        engine?.setParameters("{\"rtc.asyncCreateMediaEngine\":true}")
        engine?.setParameters("{\"che.video.enable_first_frame_sw_decode\":true}")
    }
    
    func initH265Config() {
        engine?.setParameters("{\"che.video.videoCodecIndex\":2}") // 265
    }
    
    func initH264Config() {
        engine?.setParameters("{\"che.video.videoCodecIndex\":1}") //264
        engine?.setParameters("{\"che.video.minQP\":10}")
        engine?.setParameters("{\"che.video.maxQP\":35}")
    }
    
}

extension CommerceAgoraKitManager {
    
    func getAnchorInfo(channelId: String, uid: UInt? = nil)->AnchorInfo {
        let anchorInfo = AnchorInfo()
        anchorInfo.channelName = channelId
        anchorInfo.uid = uid ?? (UInt(VLUserCenter.user.id) ?? 0)
        anchorInfo.token = AppContext.shared.rtcToken ?? ""
        
        return anchorInfo
    }
    
    func setOffMediaOptionsVideo(roomid: String) {
        guard let connection = VideoLoaderApiImpl.shared.getConnectionMap()[roomid] else {
            commerceLogger.info("setOffMediaOptionsVideo  connection 不存在 \(roomid)")
            return
        }
        commerceLogger.info("setOffMediaOptionsVideo with roomid = \(roomid)")
        let mediaOptions = AgoraRtcChannelMediaOptions()
        mediaOptions.autoSubscribeVideo = false
        engine?.updateChannelEx(with: mediaOptions, connection: connection)
    }
    
    func setOffMediaOptionsAudio() {
        VideoLoaderApiImpl.shared.getConnectionMap().forEach { _, connention in
            let mediaOptions = AgoraRtcChannelMediaOptions()
            mediaOptions.autoSubscribeAudio = false
            engine?.updateChannelEx(with: mediaOptions, connection: connention)
        }
    }
    
}
// MARK: - IVideoLoaderApiListener
extension CommerceAgoraKitManager: IVideoLoaderApiListener {
    public func debugInfo(_ message: String) {
        commerceLogger.info(message, context: "VideoLoaderApi")
    }
    public func debugWarning(_ message: String) {
        commerceLogger.warning(message, context: "VideoLoaderApi")
    }
    public func debugError(_ message: String) {
        commerceLogger.error(message, context: "VideoLoaderApi")
    }
}
// MARK: - AgoraRtcMediaPlayerDelegate
extension CommerceAgoraKitManager: AgoraRtcMediaPlayerDelegate {
    
    func AgoraRtcMediaPlayer(_ playerKit: AgoraRtcMediaPlayerProtocol, didChangedTo state: AgoraMediaPlayerState, error: AgoraMediaPlayerError) {
        if state == .openCompleted {
            playerKit.play()
        }
    }
}
