//
//  VideoLoaderApiImpl.swift
//  VideoLoaderAPI
//
//  Created by wushengtao on 2023/7/27.
//

import Foundation
import AgoraRtcKit

#if DEBUG
func createDateFormatter()-> DateFormatter {
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    return formatter
}

let formatter = createDateFormatter()
#endif

public class VideoLoaderApiImpl: NSObject {
    private var config: VideoLoaderConfig?
    
    private let apiProxy = VideoLoaderApiProxy()
    private var profilerMap: [String: VideoLoaderProfiler] = [:]
    private var rtcProxys = [String: VideoLoaderAgoraExProxy]()
    //[ex channelId: connection]
    private var exConnectionMap: [String: AgoraRtcConnection] = [:]
    //[ex channelId: [room id: status]]
    private var exConnectionDeps: [String: [String: RoomStatus]] = [:]
    
    deinit {
        #if DEBUG
        print("deinit-- VideoLoaderApiImpl")
        #endif
        cleanCache()
        rtcProxys.forEach { key, value in
            value.removeAllListener()
        }
    }
    
    public override init() {
        #if DEBUG
        print("init-- VideoLoaderApiImpl")
        #endif
        super.init()
    }
}

//MARK: private
extension VideoLoaderApiImpl {
    private func _getProfiler(roomId: String) -> VideoLoaderProfiler {
        let profiler = profilerMap[roomId] ?? VideoLoaderProfiler(roomId: roomId)
        profiler.printClosure = { [weak self] message in
            self?.apiPrint(message)
        }
        profilerMap[roomId] = profiler
        return profiler
    }
    
    private func _getProxy(roomId: String) -> VideoLoaderAgoraExProxy {
        var rtcProxy = rtcProxys[roomId]
        if rtcProxy == nil {
            let proxy = VideoLoaderAgoraExProxy()
            rtcProxys[roomId] = proxy
            rtcProxy = proxy
        }
        
        return rtcProxy!
    }
    
    private func _updateChannelEx(channelId: String, options: AgoraRtcChannelMediaOptions) {
        guard let engine = config?.rtcEngine,
              let connection = exConnectionMap[channelId] else {
            apiErrorPrint("updateChannelEx fail[\(channelId)]: connection is empty")
            return
        }
        engine.updateChannelEx(with: options, connection: connection)
    }
    
    private func _joinChannelEx(channelId: String,
                                ownerId: UInt,
                                token: String,
                                options:AgoraRtcChannelMediaOptions) {
        guard let engine = config?.rtcEngine, let uid = config?.userId else {
            assert(true, "rtc engine not initlized")
            return
        }
        
        if let connection = exConnectionMap[channelId] {
            return
        }
        let subscribeStatus = false
        let mediaOptions = AgoraRtcChannelMediaOptions()
        mediaOptions.autoSubscribeAudio = subscribeStatus
        mediaOptions.autoSubscribeVideo = subscribeStatus
        mediaOptions.clientRoleType = .audience
        mediaOptions.audienceLatencyLevel = .lowLatency
    
        let connection = AgoraRtcConnection()
        connection.channelId = channelId
        connection.localUid = uid
        
        //TODO: retain cycle in joinChannelEx to use rtcProxy
        let rtcProxy = _getProxy(roomId: channelId)
        let profiler = _getProfiler(roomId: channelId)
        addRTCListener(roomId: channelId, listener: profiler)
        
        let date = Date()
        apiPrint("try to join room[\(connection.channelId)] ex uid: \(connection.localUid)")
        let ret =
        engine.joinChannelEx(byToken: token,
                               connection: connection,
                               delegate: rtcProxy,
                               mediaOptions: mediaOptions) {[weak self] channelName, uid, elapsed in
            let cost = Int(-date.timeIntervalSinceNow * 1000)
            self?.apiPrint("join room[\(channelName)] ex success uid: \(uid) cost \(cost) ms")
        }
        
        engine.updateChannelEx(with: mediaOptions, connection: connection)
        exConnectionMap[channelId] = connection
            
        if ret == 0 {
            apiPrint("join room ex[\(channelId)]: ownerId: \(ownerId) connection count: \(exConnectionMap.count)")
        }else{
            apiErrorPrint("join room ex fail[\(channelId)]: ownerId: \(ownerId) token = \(token), \(ret)")
        }
    }
    
    func _leaveChannelEx(channelId: String) {
        guard let engine = config?.rtcEngine,
              let connection = exConnectionMap[channelId] else { return }
        let depMap = exConnectionDeps[channelId]
        if depMap?.count ?? 0 > 0 {
            apiPrint("leaveChannelEx break, depcount: \(depMap?.count ?? 0), channelId: \(channelId)")
            return
        }
        
        let profiler = _getProfiler(roomId: channelId)
        removeRTCListener(roomId: channelId, listener: profiler)
        
        engine.leaveChannelEx(connection)
        exConnectionMap[channelId] = nil
        apiPrint("leaveChannelEx channelId: \(channelId)  connection count: \(exConnectionMap.count)")
    }
    
    private func apiPrint(_ message: String) {
        let api = apiProxy as IVideoLoaderApiListener
        api.debugInfo?(message)
        #if DEBUG
        print("\(formatter.string(from: Date()))[VideoLoaderApi]\(message)")
        #endif
    }

    private func apiWarningPrint(_ message: String) {
        let api = apiProxy as IVideoLoaderApiListener
        api.debugWarning?(message)
        #if DEBUG
        print("\(formatter.string(from: Date()))[VideoLoaderApi][Warning]\(message)")
        #endif
    }

    private func apiErrorPrint(_ message: String) {
        let api = apiProxy as IVideoLoaderApiListener
        api.debugError?(message)
        #if DEBUG
        print("\(formatter.string(from: Date()))[VideoLoaderApi][Error]\(message)")
        #endif
    }
}

//MARK: VideoLoaderApiProtocol
extension VideoLoaderApiImpl: IVideoLoaderApi {
    public func setup(config: VideoLoaderConfig) {
        self.config = config
    }
    
    public func preloadRoom(preloadRoomList: [RoomInfo]) {
        guard let rtcEngine = self.config?.rtcEngine else {return}
        apiPrint("preloadRoom: \(preloadRoomList.count)")
        preloadRoomList.forEach { roomInfo in
            if let uid = config?.userId {
                rtcEngine.preloadChannel(byToken: roomInfo.token, channelId: roomInfo.channelName, uid: uid)
            }
        }
    }
    
    public func switchRoomState(newState: RoomStatus, roomInfo: RoomInfo, tagId: String?) {
        var map: [String: RoomStatus]? = exConnectionDeps[roomInfo.channelName]
        if map == nil {
            map = [:]
        }
        let tagId = tagId ?? ""
        if newState == .idle {
            map?[tagId] = nil
        } else {
            map?[tagId] = newState
        }
        
        guard let map = map else {
            apiErrorPrint("switchRoomState fatal, map init fail")
            return
        }
        let oldState = getRoomState(roomInfo: roomInfo)
        
        exConnectionDeps[roomInfo.channelName] = map
        
        let realState = getRoomState(roomInfo: roomInfo)
        
        if realState == .idle {
            _leaveChannelEx(channelId: roomInfo.channelName)
        } else {
            _joinChannelEx(channelId: roomInfo.channelName, ownerId: roomInfo.uid, token: roomInfo.token, options: AgoraRtcChannelMediaOptions())
        }
        
        let mediaOptions = AgoraRtcChannelMediaOptions()
        if realState == .joined {
            mediaOptions.autoSubscribeAudio = true
            mediaOptions.autoSubscribeVideo = true
        } else {
            mediaOptions.autoSubscribeAudio = false
            mediaOptions.autoSubscribeVideo = false
        }

        apiPrint("tagId[\(tagId)] updateLoadingType[\(roomInfo.channelName)] want:\(newState.rawValue) real: \(realState.rawValue)")
        _updateChannelEx(channelId:roomInfo.channelName, options: mediaOptions)
        if realState != oldState {
            if realState == .joined {
                let profiler = _getProfiler(roomId: roomInfo.channelName)
                profiler.startTime = Int64(NSDate().timeIntervalSince1970 * 1000)
            }
            let api = apiProxy as IVideoLoaderApiListener
            api.onStateDidChange?(newState: realState, oldState: oldState, channelName: roomInfo.channelName)
        }
    }
    
    public func getRoomState(roomInfo: RoomInfo) -> RoomStatus {
        var realState: RoomStatus = .idle
        
        guard let map: [String: RoomStatus] = exConnectionDeps[roomInfo.channelName] else {
            return realState
        }
        //calc real type
        map.forEach { (key: String, value: RoomStatus) in
            if realState.rawValue < value.rawValue {
                realState = value
            }
        }
        
        return realState
    }
    
    public func getConnectionMap() -> [String: AgoraRtcConnection] {
        return exConnectionMap
    }
    
    public func renderVideo(roomInfo: RoomInfo, container: VideoCanvasContainer) {
        guard let engine = config?.rtcEngine,
              let localUid = config?.userId,
              let connection = exConnectionMap[roomInfo.channelName] else {
            apiErrorPrint("renderVideo fail: connection is empty")
            return
        }
                
        let videoCanvas = AgoraRtcVideoCanvas()
        videoCanvas.uid = container.uid
        videoCanvas.view = container.container
        videoCanvas.renderMode = .hidden
        let ret = engine.setupRemoteVideoEx(videoCanvas, connection: connection)
                
        apiPrint("renderVideo[\(connection.channelId)] ret = \(ret), uid:\(roomInfo.uid) localuid: \(localUid) ")
    }
    
    public func cleanCache() {
        guard let rtcEngine = self.config?.rtcEngine else {return}
        exConnectionMap.forEach { roomId, connection in
            rtcEngine.leaveChannelEx(connection)
        }
        exConnectionMap.removeAll()
        exConnectionDeps.removeAll()
        profilerMap.removeAll()
        apiPrint("cleanCache")
    }
    public func leaveChannelWithout(roomId: String) {
        guard let engine = self.config?.rtcEngine else {return}
        exConnectionMap.forEach { rid, connection in
            if (roomId != rid) {
                engine.leaveChannelEx(connection)
            }
        }
        exConnectionMap = exConnectionMap.filter { $0.key == roomId }
        exConnectionDeps = exConnectionDeps.filter { $0.key == roomId }
    }
    
    public func addListener(listener: IVideoLoaderApiListener) {
        apiProxy.addListener(listener)
    }
    
    public func removeListener(listener: IVideoLoaderApiListener) {
        apiProxy.addListener(listener)
    }
    
    public func addRTCListener(roomId: String, listener: AgoraRtcEngineDelegate) {
        let rtcProxy = _getProxy(roomId: roomId)
        apiPrint("[VideoLoaderProfiler] addRTCListener: \(roomId)")
        rtcProxy.addListener(listener)
    }
    
    public func removeRTCListener(roomId: String, listener: AgoraRtcEngineDelegate) {
        let rtcProxy = _getProxy(roomId: roomId)
        apiPrint("[VideoLoaderProfiler] removeRTCListener: \(roomId)")
        rtcProxy.removeListener(listener)
    }
    
    public func getRTCListener(roomId: String) -> AgoraRtcEngineDelegate? {
        return rtcProxys[roomId]
    }
}
