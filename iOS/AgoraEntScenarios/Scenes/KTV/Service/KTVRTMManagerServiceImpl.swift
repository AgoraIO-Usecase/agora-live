//
//  KTVRTMManagerServiceImpl.swift
//  AgoraEntScenarios
//
//  Created by CP on 2024/4/11.
//

import Foundation
import RTMSyncManager
import YYModel
import SVProgressHUD
private let kSceneId = "scene_ktv_4.3.0"

/// 座位信息
private let SYNC_MANAGER_SEAT_INFO = "seat_info"
// 选歌
private let SYNC_MANAGER_CHOOSE_SONG_INFO = "choose_song"

private enum AUIMicSeatCmd: String {
    case leaveSeatCmd = "leaveSeatCmd"
    case enterSeatCmd = "enterSeatCmd"
    case kickSeatCmd = "kickSeatCmd"
    case muteAudioCmd = "muteAudioCmd"
    case muteVideoCmd = "muteVideoCmd"
    case closeSeatCmd = "closeSeatCmd"
    case chorusCmd = "chorusCmd"
}

private enum AUIMusicCmd: String {
    case chooseSongCmd = "chooseSongCmd"
    case removeSongCmd = "removeSongCmd"
    case pingSongCmd = "pingSongCmd"
    case updatePlayStatusCmd = "updatePlayStatusCmd"
}

@objc class KTVRTMManagerServiceImpl: NSObject, KTVServiceProtocol {
    
    private var appId: String
    private var user: VLLoginModel
    private var host: String
    private var appCertificate: String
    
    private var roomList: [AUIRoomInfo]?
//    private var userList: [VLLoginModel] = .init()
    private var seatMap: [Int: VLRoomSeatModel] = [:]
    private var songList: [VLRoomSelSongModel] = .init()
    
    private weak var delegate: KTVServiceListenerProtocol?
    
    private var roomNo: String?
    private var expireTimer: Timer?
    private var isConnected: Bool = false
    
    private var room: AUIRoomInfo? {
        return self.roomList?.filter({ $0.roomId == self.roomNo }).first
    }
    
    private lazy var roomManager = AUIRoomManagerImpl(sceneId: kSceneId)
    private lazy var syncManager: AUISyncManager = {
        let config = AUICommonConfig()
        config.appId = appId
        let owner = AUIUserThumbnailInfo()
        owner.userId = String(user.id)
        owner.userName = user.name
        owner.userAvatar = user.headUrl
        config.owner = owner
        config.host = self.host
        let manager = AUISyncManager(rtmClient: nil, commonConfig: config)
        
        return manager
    }()
    
    private lazy var roomService: AUIRoomService = {
        let poliocy = RoomExpirationPolicy()
        poliocy.expirationTime = 20 * 60 * 1000
        let service = AUIRoomService(expirationPolicy: poliocy, roomManager: roomManager, syncmanager: syncManager)
        
        return service
    }()
    
    @objc public required init(appId: String, host: String, appCertificate: String, user: VLLoginModel) {
        self.appId = appId
        self.user = user
        self.host = host
        self.appCertificate = appCertificate
        AUIRoomContext.shared.displayLogClosure = { msg in
            KTVLog.info(text: msg, tag: "RTMSyncManager")
        }
        super.init()
        syncManager.rtmManager.subscribeError(channelName: "", delegate: self)
        login { err in
            self.isConnected = err == nil ? true : false
        }
    }
    
    private func currentUserId() -> String {
        return VLUserCenter.user.id
    }
    
    private func preGenerateToken(completion:@escaping (NSError?)->()) {
        commerceLogger.error("preGenerateToken start")
        AppContext.shared.agoraRTCToken = ""
        AppContext.shared.agoraRTMToken = ""
        AppContext.shared.agoraPlayerRTCToken = ""
        
        let group = DispatchGroup()
        
        let userId = VLUserCenter.user.id
        let playerRTCUid = VLUserCenter.user.agoraPlayerRTCUid;
        let date = Date()
        group.enter()
        NetworkManager.shared.generateTokens(channelName: "",
                                             uid: "\(userId)",
                                             tokenGeneratorType: .token007,
                                             tokenTypes: [.rtc, .rtm],
                                             expire: 24 * 60 * 60) {  tokenMap in
            defer {
                group.leave()
            }
            guard let rtcToken = tokenMap[NetworkManager.AgoraTokenType.rtc.rawValue],
                  rtcToken.count > 0,
                  let rtmToken = tokenMap[NetworkManager.AgoraTokenType.rtm.rawValue],
                  rtmToken.count > 0 else {
                commerceLogger.error("preGenerateToken rtc & rtm fail")
                return
            }
            
            commerceLogger.info("[Timing]preGenerateToken rtc & rtm cost: \(Int64(-date.timeIntervalSinceNow * 1000)) ms")
            AppContext.shared.agoraRTCToken = rtcToken
            AppContext.shared.agoraRTMToken = rtmToken
        }
        
        group.enter()
        NetworkManager.shared.generateToken(channelName: "",
                                            uid: "\(playerRTCUid)",
                                            tokenType: .token007,
                                            type: .rtc,
                                            expire: 24 * 60 * 60) { token in
            defer {
                group.leave()
            }
            guard let token = token, token.count > 0 else {
                commerceLogger.error("preGenerateToken player token fail")
                return
            }
            
            commerceLogger.info("[Timing]preGenerateToken player token cost: \(Int64(-date.timeIntervalSinceNow * 1000)) ms")
            AppContext.shared.agoraPlayerRTCToken = token
        }
        
        group.notify(queue: .main) {
            guard !AppContext.shared.agoraRTCToken.isEmpty,
                  !AppContext.shared.agoraRTMToken.isEmpty,
                  !AppContext.shared.agoraPlayerRTCToken.isEmpty else {
                completion(NSError(domain: "generate token fail", code: -1))
                return
            }
            completion(nil)
        }
    }
    
    private func _subscribeAll() {
        guard let roomNo = self.roomNo else {return}
        let _ = self.syncManager.createScene(channelName: roomNo)
        getCurrentSongCollection(with: roomNo)?.subscribeAttributesDidChanged(callback: {[weak self] str1, str2, model in
            guard let self = self,
                  let list = model.getList(),
                  let songs = NSArray.yy_modelArray(with: VLRoomSelSongModel.self, json: list) as? [VLRoomSelSongModel] else {
                return
            }
            self.delegate?.onUpdateAllChooseSongs(songs: songs)
        })
        
        getCurrentSeatCollection(with: roomNo)?.subscribeAttributesDidChanged(callback: {[weak self] str1, str2, model in
            guard let self = self, let map = model.getMap() as? [String: [String: Any]] else { return }
            var seatMap: [String: VLRoomSeatModel] = [:]
            map.values.forEach { element in
                guard let micSeat = VLRoomSeatModel.yy_model(with: element) else {return}
                seatMap["\(micSeat.seatIndex)"] = micSeat
                
            }
            if self.seatMap.isEmpty {
                self.delegate?.onMicSeatSnapshot(seat: seatMap)
            }
            
            seatMap.values.forEach { micSeat in
                let index = micSeat.seatIndex
                let origMicSeat = self.seatMap[index]
                
                self.seatMap[index] = micSeat
                if let origMicSeat = origMicSeat,
                   let userNo = origMicSeat.userNo, userNo.count > 0,
                   micSeat.userNo != userNo {
//                    delegate.onAnchorLeaveSeat(seatIndex: index, user: origUser)
                    self.delegate?.onAnchorLeaveSeat(seat: micSeat)
                }
                
                if let userNo = micSeat.userNo, userNo.count > 0, origMicSeat?.userNo != userNo {
//                    delegate.onAnchorEnterSeat(seatIndex: index, user: user)
//                    self.seatListDidChanged?(.created, micSeat)
                    self.delegate?.onAnchorEnterSeat(seat: micSeat)
                }
                
                if origMicSeat?.isAudioMuted != micSeat.isAudioMuted {
//                    delegate.onSeatAudioMute(seatIndex: index, isMute: micSeat.muteAudio)
                    self.delegate?.onSeatAudioMute(seat: micSeat)
                }
                
                if origMicSeat?.isVideoMuted != micSeat.isVideoMuted {
//                    delegate.onSeatVideoMute(seatIndex: index, isMute: micSeat.muteVideo)
                    self.delegate?.onSeatVideoMute(seat: micSeat)
                }
            }
        })
    }
}

//only for room
extension KTVRTMManagerServiceImpl {
    func getRoomList(page: UInt, completion: @escaping (Error?, [AUIRoomInfo]?) -> Void) {
        let fetchRoomList: () -> Void = {[weak self] in
            self?.roomService.getRoomList(lastCreateTime: 0, pageSize: 50) {[weak self] err, ts, list in
                let roomList = list ?? []
                self?.roomList = roomList
                completion(nil, roomList)
            }
        }
        
        if isConnected == false {
            login { err in
                if err == nil {
                    fetchRoomList()
                } else {
                    completion(err, nil)
                }
            }
        } else {
            fetchRoomList()
        }
    }
    
    func createRoom(inputModel: KTVCreateRoomInfo, completion: @escaping (Error?, AUIRoomInfo?) -> Void) {
        let roomModel = AUIRoomInfo() // LiveRoomInfo(roomName: inputModel.name)
        //roomInfo.id = VLUserCenter.user.id//NSString.withUUID().md5() ?? ""
        roomModel.name = inputModel.name
        roomModel.isPrivate = inputModel.isPrivate == 1
        roomModel.password = inputModel.password
        roomModel.creatorNo = currentUserId()
        roomModel.roomNo = "\(arc4random_uniform(899999) + 100000)" // roomInfo.id
//        roomModel.bgOption = 0
//        roomModel.roomPeopleNum = "0"
        roomModel.icon = inputModel.icon
        roomModel.creatorName = VLUserCenter.user.name
        roomModel.creatorAvatar = VLUserCenter.user.headUrl
        
        _showLoadingView()
        self.roomNo = roomModel.roomId
        let date = Date()
        func create(roomInfo: AUIRoomInfo) {
            _subscribeAll()
            roomService.createRoom(room: roomInfo) { [weak self] err, room in
                guard let self = self else {return}
                if let err = err {
                    KTVLog.info(text: "enter scene fail: \(err.localizedDescription)")
                    _hideLoadingView()
                    completion(err, nil)
                    return
                }
                self.roomList?.append(room!)
                _hideLoadingView()
                completion(nil, room!)
            }
        }

        if isConnected == false {
            login { err in
                if err == nil {
                    create(roomInfo: roomModel)
                } else {
                    completion(err, nil)
                }
            }
        } else {
            create(roomInfo: roomModel)
        }
    }
    
    func joinRoom(roomId: String, password: String, completion: @escaping (Error?) -> Void) {
        _showLoadingView()
        let date = Date()
        let enterScene: () -> Void = {[weak self] in
            self?._subscribeAll()
            self?.roomService.enterRoom(roomId: roomId) {[weak self] err in
                guard let self = self else { return }
                agoraPrint("joinRoom joinScene cost: \(-date.timeIntervalSinceNow * 1000) ms")
                if let err = err {
                    agoraPrint("enter scene fail: \(err.localizedDescription)")
                    _hideLoadingView()
                    completion(err)
                    return
                }
                self.roomNo = roomId
                _hideLoadingView()
                completion(nil)
            }
        }
                
        if isConnected == false {
            login { err in
                if err == nil {
                    _hideLoadingView()
                    enterScene()
                } else {
                    _hideLoadingView()
                    completion(err)
                }
            }
        } else {
            enterScene()
        }
    }
    
    func leaveRoom(completion: @escaping (Error?) -> Void) {
       let performLeaveRoom: () -> Void = {[weak self] in
           guard let self = self else {return}
           
           //remove current user's choose song
           _removeChooseSong(userId: currentUserId()) { err in
           }
           
           roomService.leaveRoom(roomId: self.roomNo ?? "")

          // syncManager.rtmManager.unsubscribeMessage(channelName: roomInfo.roomId, delegate: self)
           roomNo = nil
           unsubscribeAll()
           completion(nil)
       }
       
       if isConnected == false {
           login { err in
               if err == nil {
                   performLeaveRoom()
               } else {
                  completion(err)
               }
           }
       } else {
           performLeaveRoom()
       }
    }
    
    func updateRoom(with userCount: Int, completion: @escaping (NSError?) -> Void) {
        guard let roomInfo = roomList?.filter({ $0.roomNo == self.roomNo }).first else {
            completion(NSError(domain: "not found roominfo", code: -1))
            return
        }
        let updateRoomInfo: () -> Void = {[weak self] in
            guard let self = self else {return}
            roomInfo.roomPeopleNum = userCount
            roomManager.updateRoom(room: roomInfo) { err, info in
                if let err = err {
                    agoraPrint("enter scene fail: \(err.localizedDescription)")
                    completion(err)
                    return
                }
                completion(nil)
            }
        }
        
        if isConnected == false {
            login { err in
                if err == nil {
                    updateRoomInfo()
                } else {
                    completion(err as NSError?)
                }
            }
        } else {
            updateRoomInfo()
        }
    }
}

//only for seat
extension KTVRTMManagerServiceImpl {
    func enterSeat(seatIndex: NSNumber?, completion: @escaping (Error?) -> Void) {
        let seatInfo = _getUserSeatInfo(seatIndex: Int(seatIndex?.intValue ?? 0))
        _addSeatInfo(seatInfo: seatInfo,
                     finished: completion)
    }
    
    func leaveSeat(completion: @escaping (Error?) -> Void) {
        guard let seatInfo = seatMap.values.filter({ $0.userNo == self.currentUserId() }).first else {return}
        _removeSeat(seatInfo: seatInfo) { error in
        }
        completion(nil)
    }
    
    func updateSeatAudioMuteStatus(muted: Bool, completion: @escaping (Error?) -> Void) {
        guard let seatInfo = self.seatMap
            .filter({ $0.value.userNo == VLUserCenter.user.id })
            .first?.value else {
            agoraAssert("mute seat not found")
            completion(nil)
            return
        }
        
        let params = ["\(seatInfo.seatIndex)": ["isAudioMuted": muted ? 1 : 0]]
        let collection = getCurrentSeatCollection(with: roomNo ?? "")
        collection?.mergeMetaData(valueCmd: AUIMicSeatCmd.muteAudioCmd.rawValue, value: params, callback: completion)
    }
    
    func updateSeatVideoMuteStatus(muted: Bool, completion: @escaping (Error?) -> Void) {
        guard let seatInfo = self.seatMap
            .filter({ $0.value.userNo == VLUserCenter.user.id })
            .first?.value else {
            agoraAssert("open video seat not found")
            completion(nil)
            return
        }
        
        let params = ["\(seatInfo.seatIndex)": ["isVideoMuted": muted ? 1 : 0]]
        let collection = getCurrentSeatCollection(with: roomNo ?? "")
        collection?.mergeMetaData(valueCmd: AUIMicSeatCmd.muteVideoCmd.rawValue, value: params, callback: completion)
    }
}

// only for music
extension KTVRTMManagerServiceImpl {
    func removeSong(songCode: String, completion: @escaping (Error?) -> Void) {
        _removeChooseSong(songCode: songCode, completion: completion)
    }
    
    func getChoosedSongsList(completion: @escaping (Error?, [VLRoomSelSongModel]?) -> Void) {
        _getChooseSongInfo(finished: completion)
    }
    
    func markSongDidPlay(songCode: String, completion: @escaping (Error?) -> Void) {
//        inputModel.status = .playing
//        _updateChooseSong(songInfo: inputModel, finished: completion)
    }
    
    func chooseSong(inputModel: KTVChooseSongInputModel, completion: @escaping (Error?) -> Void) {
        //添加歌曲前先判断
        var flag = false
        _ =  songList.compactMap { model in
            if model.songNo == inputModel.songNo {
                agoraPrint("The song has been added")
                flag = true
                return
            }
        }
        if flag {return}
        
        let songInfo = VLRoomSelSongModel()
       // songInfo.isChorus = inputModel.isChorus
        songInfo.songName = inputModel.songName
        songInfo.songNo = inputModel.songNo
//        songInfo.songUrl = inputModel.songUrl
        songInfo.imageUrl = inputModel.imageUrl
        songInfo.singer = inputModel.singer
        songInfo.status = .idle
        /// 是谁点的歌
        songInfo.userNo = currentUserId()
//        songInfo.userId = UserInfo.userId
        /// 点歌人昵称
        songInfo.name = VLUserCenter.user.name

        _addChooseSongInfo(songInfo: songInfo) { error in
            completion(error)
        }
    }
    
    func pinSong(songCode: String, completion: @escaping (Error?) -> Void) {
        let collection = getCurrentSongCollection(with: roomNo ?? "")
        collection?.mergeMetaData(valueCmd: nil, 
                                  value: ["pinAt": getCurrentTs(channelName: roomNo ?? "")],
                                  filter: [["songNo": songCode]],
                                  callback: completion)
    }
}

//for chorus
extension KTVRTMManagerServiceImpl {
    func joinChorus(songCode: String, completion: @escaping (Error?) -> Void) {
        guard let topSong = self.songList.filter({ $0.songNo == songCode}).first else {
            agoraAssert("join Chorus fail")
            completion(nil)
            return
        }
        //TODO: _markSeatToPlaying without callback
        _markSeatChorusStatus(songCode: topSong.chorusSongId(),  completion: completion)
    }
    
    func coSingerLeaveChorus(completion: @escaping (Error?) -> Void) {
        _markSeatChorusStatus(songCode: "", completion: completion)
    }
    
    func enterSoloMode() {
        _markSoloSongIfNeed()
    }
    
    func changeMVCover(inputModel: KTVChangeMVCoverInputModel, completion: @escaping (Error?) -> Void) {
        
    }
    
}

// for subscribe
extension KTVRTMManagerServiceImpl {
    func subscribe(listener: KTVServiceListenerProtocol?) {
        self.delegate = listener
    }
    
    public func subscribeRoomWillExpire(changedBlock: @escaping () -> Void) {
        expireTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] timer in
            guard let self = self else { return }
            
            if self.checkAndHandleRoomExpire(changedBlock: changedBlock) {
                timer.invalidate()
                self.expireTimer = nil
            }
        }
        
        // 立即执行一次检查到期的方法
        checkAndHandleRoomExpire(changedBlock: changedBlock)
    }

    private func checkAndHandleRoomExpire(changedBlock: @escaping () -> Void) -> Bool {
        guard let roomNo = self.roomNo else { return false }
        let expiredDuration = 20 * 60 * 1000
        let duration = getCurrentDuration(channelName: roomNo)
//        agoraPrint("checkRoomExpire: \(duration) / \(expiredDuration)")
        
        if duration > expiredDuration {
            expireTimer?.invalidate()
            expireTimer = nil
            changedBlock()
            return true
        }
        
        return false
    }
    
    func unsubscribeAll() {
        //取消所有的订阅
        guard let channelName = roomNo else {
            return
        }
        agoraPrint("imp all unsubscribe...")
        if let scene = getCurrentScene(with: channelName) {
            scene.unbindRespDelegate(delegate: self)
            scene.userService.unbindRespDelegate(delegate: self)
        }
        
        expireTimer?.invalidate()
        expireTimer = nil
    }
    
    func getCurrentDuration(channelName: String) -> UInt64 {
        return getCurrentScene(with: channelName)?.getRoomDuration() ?? 0
    }
    
    func getCurrentTs(channelName: String) -> UInt64 {
        return getCurrentScene(with: channelName)?.getCurrentTs() ?? 0
    }
}

// model, dict convert tool
extension KTVRTMManagerServiceImpl {
    private func convertAUIUserInfo2UserInfo(with userInfo: AUIUserInfo) -> VLLoginModel {
        let user = VLLoginModel()
        user.userNo = userInfo.userId
        user.name = userInfo.userName
        user.headUrl = userInfo.userAvatar
        
        return user
    }
    
    private func convertUserDictToUser(with userDict: [String: Any]) -> VLLoginModel? {
        if let model = VLLoginModel.yy_model(with: userDict) {
            return model
        } else {
            return nil
        }
    }
    
    private func convertSeatDictToSeat(with seatDict: [String: Any]) -> VLRoomSeatModel? {
        if let model = VLRoomSeatModel.yy_model(with: seatDict) {
            return model
        } else {
            return nil
        }
    }
    
    private func convertSongDictToSong(with songDict: [String: Any]) -> VLRoomSelSongModel? {
        if let model = VLRoomSelSongModel.yy_model(with: songDict) {
            return model
        } else {
            return nil
        }
    }
    
    private func getCurrentSeatCollection(with roomId: String) -> AUIMapCollection? {
        let collection: AUIMapCollection? = getCurrentScene(with: roomId)?.getCollection(key: SYNC_MANAGER_SEAT_INFO)
        return collection
    }
    
    private func getCurrentSongCollection(with roomId: String) -> AUIListCollection? {
        let collection: AUIListCollection? = getCurrentScene(with: roomId)?.getCollection(key: SYNC_MANAGER_CHOOSE_SONG_INFO)
        return collection
    }
}

extension KTVRTMManagerServiceImpl: AUISceneRespDelegate {
    func onWillInitSceneMetadata(channelName: String) -> [String : Any]? {
        var map: [String: Any] = [:]
        map["0"] = self._getUserSeatInfo(seatIndex: 0).yy_modelToJSONObject()
        for i in 1...7 {
            let seat = VLRoomSeatModel()
            seat.seatIndex = i
            map["\(i)"] = seat.yy_modelToJSONObject()
        }
        return [
            SYNC_MANAGER_SEAT_INFO: map
        ]
    }
    
    func onSceneExpire(channelName: String) {
        KTVLog.info(text: "onSceneExpire: \(channelName)")
        roomService.leaveRoom(roomId: channelName)
        //TODO: expire notify
    }
    
    func onSceneDestroy(channelName: String) {
        KTVLog.info(text: "onSceneDestroy: \(channelName)")
        roomService.leaveRoom(roomId: channelName)
        //self.listener?.onRoomDidDestroy(roomInfo: model)
        
        //TODO: destroy notify
    }
    
    func onTokenPrivilegeWillExpire(channelName: String?) {
        NetworkManager.shared.generateToken(appId: appId, appCertificate: appCertificate, channelName: "", uid: String(user.id), tokenType: .token007, type: .rtm) { token in
            if let token = token {
                self.syncManager.renew(token: token) { err in
                    guard let err = err else { return }
                    KTVLog.error(text: "renew token：err \(err.localizedDescription)")
                }
            }
        }
    }
}

extension KTVRTMManagerServiceImpl: AUIUserRespDelegate {
    func onRoomUserSnapshot(roomId: String, userList: [AUIUserInfo]) {
        if roomService.isRoomOwner(roomId: roomId) {
            self.updateRoom(with: userList.count) { err in
            }
        }
    }
    
    func onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
        KTVLog.info(text: "user: enter\(userInfo.userName)")
        let user = convertAUIUserInfo2UserInfo(with: userInfo)
        let userCount = getCurrentScene(with: roomId)?.userService.userList.count ?? 0
//        self.userDidChanged?(.created, user)
        self.delegate?.onUserCountUpdate(userCount: UInt(userCount))
        if roomService.isRoomOwner(roomId: roomId) {
            self.updateRoom(with: userCount) { err in
            }
        }
    }
    
    func onRoomUserLeave(roomId: String, userInfo: AUIUserInfo) {
        KTVLog.info(text: "user: leave\(userInfo.userName)")
        let user = convertAUIUserInfo2UserInfo(with: userInfo)
//        self.userDidChanged?(.deleted, user)
        let userCount = getCurrentScene(with: roomId)?.userService.userList.count ?? 0
        self.delegate?.onUserCountUpdate(userCount: UInt(userCount))
        if roomService.isRoomOwner(roomId: roomId) {
            self.updateRoom(with: userCount) { err in
            }
        }
    }
    
    func onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
        KTVLog.info(text: "user: update\(userInfo.userName)")
        let user = convertAUIUserInfo2UserInfo(with: userInfo)
//        self.userDidChanged?(.updated, user)
        let userCount = getCurrentScene(with: roomId)?.userService.userList.count ?? 0
        self.delegate?.onUserCountUpdate(userCount: UInt(userCount))
    }
  
    func onUserAudioMute(userId: String, mute: Bool) {
        
    }
    
    func onUserVideoMute(userId: String, mute: Bool) {
        
    }
    
    func onUserBeKicked(roomId: String, userId: String) {
        
    }
}

extension KTVRTMManagerServiceImpl: AUIRtmErrorProxyDelegate {
    
    private func getCurrentScene(with channelName: String) -> AUIScene? {
        let scene = self.syncManager.getScene(channelName: channelName)
        scene?.userService.bindRespDelegate(delegate: self)
        scene?.bindRespDelegate(delegate: self)
        return scene
    }
    
    private func login(completion:(@escaping (Error?)-> Void)) {
        let token = AppContext.shared.agoraRTMToken
        if !token.isEmpty {
            self.syncManager.rtmManager.login(token: token) { err in
                self.isConnected = err == nil ? true : false
                completion(err)
            }
            return
        }
        preGenerateToken { [weak self] err in
            if let err = err {
                completion(err)
                return
            }
            self?.login(completion: completion)
        }
    }
}

// seat
extension KTVRTMManagerServiceImpl {
    private func _markSeatChorusStatus(songCode: String, completion: @escaping (Error?)->()) {
        guard let seatInfo = self.seatMap
            .filter({ $0.value.userNo == VLUserCenter.user.id })
            .first?.value else {
            agoraAssert("mark join seat not found")
            //TODO: error
            completion(nil)
            return
        }
        let params = ["\(seatInfo.seatIndex)": ["chorusSongCode": songCode]]
        let collection = getCurrentSeatCollection(with: roomNo ?? "")
        collection?.mergeMetaData(valueCmd: AUIMicSeatCmd.chorusCmd.rawValue, value: params, callback: completion)
    }
    
    private func _getUserSeatInfo(seatIndex: Int, model: VLRoomSeatModel? = nil) -> VLRoomSeatModel {
        let user = VLUserCenter.user
        let seatInfo = VLRoomSeatModel()
        seatInfo.seatIndex = seatIndex
        seatInfo.rtcUid = user.id
        seatInfo.userNo = user.id
        seatInfo.headUrl = user.headUrl
        seatInfo.name = user.name
        
        if let m = model {
            /// 是否自己静音
            seatInfo.isAudioMuted = m.isAudioMuted
            /// 是否开启视频
            seatInfo.isVideoMuted = m.isVideoMuted

            /// 新增, 判断当前歌曲是否是自己点的
            seatInfo.isOwner = m.isOwner

            seatInfo.chorusSongCode = m.chorusSongCode
        } else {
            /// 是否自己静音
            seatInfo.isAudioMuted = 1
            /// 是否开启视频
            seatInfo.isVideoMuted = 1

            /// 新增, 判断当前歌曲是否是自己点的
            seatInfo.isOwner = false

            seatInfo.chorusSongCode = ""
        }
        
        return seatInfo
    }

    private func _removeSeat(seatInfo: VLRoomSeatModel,
                            finished: @escaping (Error?) -> Void) {
        guard let channelName = roomNo else {
            agoraPrint("removeSeat channelName = nil")
            return
        }
        
        agoraPrint("imp seat delete... [\(String(describing: seatInfo.userNo))]")
        let collection = getCurrentSeatCollection(with: channelName)
        let model = VLRoomSeatModel()
        model.seatIndex = seatInfo.seatIndex
        let params = mapConvert(model: model)
        collection?.addMetaData(valueCmd: AUIMicSeatCmd.leaveSeatCmd.rawValue, value: params, callback: { err in
            finished(err)
        })
    }

    private func _addSeatInfo(seatInfo: VLRoomSeatModel,
                             finished: @escaping (Error?) -> Void) {
        guard let channelName = roomNo else {
//            assert(false, "channelName = nil")
            let error = NSError(domain: "addUserInfo channelName = nil", code: -1)
            agoraPrint(error.localizedDescription)
            finished(error)
            return
        }
        
        agoraPrint("imp seat add...")
        let params = mapConvert(model: seatInfo)
        let collection = getCurrentSeatCollection(with: channelName)
        collection?.addMetaData(valueCmd: AUIMicSeatCmd.enterSeatCmd.rawValue, value: params, callback: { err in
            finished(err)
        })
    }
}

// for song
extension KTVRTMManagerServiceImpl {
    private func _sortChooseSongList() {
        songList = songList.sorted(by: { model1, model2 in
            if model1.status == .playing{
                return true
            }
            if model2.status == .playing {
                return false
            }
            if model1.pinAt < 1,  model2.pinAt < 1 {
                return model1.createAt - model2.createAt < 0 ? true : false
            }
            
            return model1.pinAt - model2.pinAt > 0 ? true : false
        })
    }

    private func _getChooseSongInfo(finished: @escaping (Error?, [VLRoomSelSongModel]?) -> Void) {
        guard let channelName = roomNo else {
//            agoraAssert("channelName = nil")
            return
        }
        agoraPrint("imp song get...")
        let collection = getCurrentSongCollection(with: channelName)
        collection?.getMetaData(callback: {[weak self] err, songs in
            guard let self = self else {return}
            if err != nil {
                agoraPrint("imp song get fail :\(String(describing: err?.description))...")
                finished(err, nil)
            } else {
                let songData = songs as? [[String: Any]] ?? []
                let songs = songData.compactMap { self.convertSongDictToSong(with: $0) }
                finished(nil, songs)
            }
        })
    }

    private func _addChooseSongInfo(songInfo: VLRoomSelSongModel, finished: @escaping (Error?) -> Void) {
        guard let channelName = roomNo, let songNo = songInfo.songNo else {
//            assert(false, "channelName = nil")
            agoraPrint("addUserInfo channelName = nil")
            return
        }
        
        agoraPrint("imp song add...[\(songNo)]")
        songInfo.createAt = getCurrentTs(channelName: channelName)
        let params = mapConvert(model: songInfo)
        let collection = getCurrentSongCollection(with: channelName)
        //add a filter to ensure that objects with the same songNo are not repeatedly inserted
        collection?.addMetaData(valueCmd: AUIMusicCmd.chooseSongCmd.rawValue,
                                value: params,
                                filter: [["songNo": songNo]],
                                callback: finished)
    }

    private func _removeChooseSong(songCode: String, completion: @escaping (Error?) -> Void) {
        guard let channelName = roomNo else {
            agoraAssert("channelName = nil")
            completion(nil)
            return
        }
        agoraPrint("imp song delete... songCode[\(songCode)]")
        let collection = getCurrentSongCollection(with: channelName)
        collection?.removeMetaData(valueCmd: AUIMusicCmd.removeSongCmd.rawValue,
                                   filter: [["songNo": songCode]], callback: { err in
            completion(err)
        })
    }
    
    private func _removeChooseSong(userId: String, completion: @escaping (Error?) -> Void) {
        guard let channelName = roomNo else {
            agoraAssert("channelName = nil")
            completion(nil)
            return
        }
        agoraPrint("imp song delete... userId[\(userId)]")
        let collection = getCurrentSongCollection(with: channelName)
        collection?.removeMetaData(valueCmd: AUIMusicCmd.removeSongCmd.rawValue,
                                   filter: [["userNo": userId]], callback: { err in
            completion(err)
        })
    }

    private func _markSoloSongIfNeed() {
        guard let topSong = songList.first,
            //  topSong.isChorus == true, // current is chorus
              topSong.userNo == VLUserCenter.user.id,
              let roomNo = roomNo,
              let songNo = topSong.songNo else {
            KTVLog.warning(text: "_markSoloSongIfNeed break:  \(songList.first?.status.rawValue ?? 0) \(songList.first?.userNo ?? "")/\(VLUserCenter.user.id)")
            return
        }
        
        let collection = getCurrentSongCollection(with: roomNo)
        collection?.mergeMetaData(valueCmd: AUIMusicCmd.updatePlayStatusCmd.rawValue,
                                  value: ["status": VLSongPlayStatus.playing.rawValue],
                                  filter: [["songNo": songNo]]) { err in
        }
    }
}


private func agoraAssert(_ message: String) {
    agoraAssert(false, message)
}

private func agoraAssert(_ condition: Bool, _ message: String) {
    KTVLog.error(text: message, tag: "KTVService")
    #if DEBUG
//    assert(condition, message)
    #else
    #endif
}

private func agoraPrint(_ message: String) {
//    #if DEBUG
    KTVLog.info(text: message, tag: "KTVService")
//    #else
//    #endif
}

private func _showLoadingView() {
    SVProgressHUD.show()
}

private func _hideLoadingView() {
    SVProgressHUD.dismiss()
}

private func mapConvert(model: NSObject) ->[String: Any] {
    let params = model.yy_modelToJSONObject() as! [String: Any]
    //TODO: convert to swift map to fix SyncManager parse NSDictionary bugs
    var swiftParams = [String: Any]()
    params.forEach { (key: String, value: Any) in
        swiftParams[key] = value
    }
    return swiftParams
}

