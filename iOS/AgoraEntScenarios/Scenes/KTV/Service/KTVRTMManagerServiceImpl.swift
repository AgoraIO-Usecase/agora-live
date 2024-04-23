//
//  KTVRTMManagerServiceImpl.swift
//  AgoraEntScenarios
//
//  Created by CP on 2024/4/11.
//

import Foundation
import RTMSyncManager
import YYModel
import AgoraRtmKit
import SVProgressHUD
private let kSceneId = "scene_ktv_4.3.0"

/// 座位信息
private let SYNC_MANAGER_SEAT_INFO = "seat_info"
// 选歌
private let SYNC_MANAGER_CHOOSE_SONG_INFO = "choose_song"
///// 房间内用户列表
//private let SYNC_SCENE_ROOM_USER_COLLECTION = "userCollection"

@objc class KTVRTMManagerServiceImpl: NSObject, KTVServiceProtocol {
    
    private var appId: String
    private var user: VLLoginModel
    private var rtmClient: AgoraRtmClientKit?
    private var host: String
    private var appCertificate: String
    
    private var roomList: [VLRoomListModel]?
    private var userList: [VLLoginModel] = .init()
    private var seatMap: [String: VLRoomSeatModel] = .init()
    private var songList: [VLRoomSelSongModel] = .init()
    private var seatList: [VLRoomSeatModel] = .init()
    
    private var userListCountDidChanged: ((UInt) -> Void)?
    private var userDidChanged: ((KTVSubscribe, VLLoginModel) -> Void)?
    private var seatListDidChanged: ((KTVSubscribe, VLRoomSeatModel) -> Void)?
    private var roomStatusDidChanged: ((KTVSubscribe, VLRoomListModel) -> Void)?
    private var chooseSongDidChanged: ((KTVSubscribe, VLRoomSelSongModel, [VLRoomSelSongModel]) -> Void)?
    private var networkDidChanged: ((KTVServiceNetworkStatus) -> Void)?
    private var roomExpiredDidChanged: (() -> Void)?
    
    private var roomNo: String?
    private var expireTimer: Timer?
    private var isConnected: Bool = false
    private var sceneBinded: Bool = false
    
    private var room: VLRoomListModel? {
        return self.roomList?.filter({ $0.roomNo == self.roomNo }).first
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
        let manager = AUISyncManager(rtmClient: rtmClient, commonConfig: config)
        
        return manager
    }()
    
    @objc public required init(appId: String, host: String, appCertificate: String, user: VLLoginModel, rtmClient: AgoraRtmClientKit?) {
        self.appId = appId
        self.user = user
        self.rtmClient = rtmClient
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
}

//only for room
extension KTVRTMManagerServiceImpl {
    func getRoomList(page: UInt, completion: @escaping (Error?, [VLRoomListModel]?) -> Void) {
        let fetchRoomList: () -> Void = {[weak self] in
            self?.roomManager.getRoomInfoList(lastCreateTime: 0, pageSize: 50) {[weak self] err, list in
                let roomList = list?.compactMap{ self?.convertAUIRoomInfo2KTVRoomInfo(with:$0) } ?? []
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
    
    func createRoom(inputModel: KTVCreateRoomInputModel, completion: @escaping (Error?, KTVCreateRoomOutputModel?) -> Void) {
        let roomModel = VLRoomListModel() // LiveRoomInfo(roomName: inputModel.name)
        //roomInfo.id = VLUserCenter.user.id//NSString.withUUID().md5() ?? ""
        roomModel.name = inputModel.name
        roomModel.isPrivate = inputModel.isPrivate == 1
        roomModel.password = inputModel.password
        roomModel.creatorNo = VLUserCenter.user.id
        roomModel.roomNo = "\(arc4random_uniform(899999) + 100000)" // roomInfo.id
        roomModel.bgOption = 0
        roomModel.roomPeopleNum = "0"
        roomModel.icon = inputModel.icon
        roomModel.createdAt = Int64(Date().timeIntervalSince1970 * 1000)
        roomModel.creatorName = VLUserCenter.user.name
        roomModel.creatorAvatar = VLUserCenter.user.headUrl
        
        _showLoadingIfNeed()
        let roomInfo = AUIRoomInfo()
        roomInfo.roomName = inputModel.name ?? ""
        roomInfo.roomId = roomModel.roomNo ?? ""
        roomInfo.customPayload = [
            "roomPeopleNum": 1,
            "createdAt": roomModel.createdAt,
            "isPrivate": inputModel.isPrivate == 1,
            "password": inputModel.password as Any,
            "creatorNo": VLUserCenter.user.id,
            "icon": inputModel.icon as Any
        ]

        let owner = AUIUserThumbnailInfo()
        owner.userId = VLUserCenter.user.id
        owner.userName = VLUserCenter.user.name
        owner.userAvatar = VLUserCenter.user.headUrl
        roomInfo.owner = owner
        
        self.roomNo = roomInfo.roomId

        func create(roomInfo: AUIRoomInfo) {
            let scene = getCurrentScene(with: roomInfo.roomId)
            roomManager.createRoom(room: roomInfo) { [weak self] err, info in
                guard let self = self else { return }
                
                if let err = err {
                    KTVLog.info(text: "create room fail: \(err.localizedDescription)")
                    _hideLoadingIfNeed()
                    completion(err, nil)
                    return
                }
                
                let date = Date()
                scene.create(payload: [:]) { [weak self] err in
                    if let err = err {
                        KTVLog.info(text: "create scene fail: \(err.localizedDescription)")
                        _hideLoadingIfNeed()
                        completion(err, nil)
                        return
                    }
//                  
                    agoraPrint("createRoom initScene cost: \(-date.timeIntervalSinceNow * 1000) ms")
                    scene.enter { [weak self] payload, err in
                        agoraPrint("createRoom joinScene cost: \(-date.timeIntervalSinceNow * 1000) ms")
                        if let err = err {
                            KTVLog.info(text: "enter scene fail: \(err.localizedDescription)")
                            _hideLoadingIfNeed()
                            completion(err, nil)
                            return
                        }
                        
                        let channelName = roomInfo.roomId
                        let userId = VLUserCenter.user.id
                        
                        let playerRTCUid = UserInfo.userId//VLUserCenter.user.agoraPlayerRTCUid;
                        var tokenMap1:[Int: String] = [:], tokenMap2:[Int: String] = [:]
                        
                        let dispatchGroup = DispatchGroup()
                        dispatchGroup.enter()
                        NetworkManager.shared.generateTokens(channelName: channelName ,
                                                             uid: "\(UserInfo.userId)",
                                                             tokenGeneratorType: .token006,
                                                             tokenTypes: [.rtc, .rtm]) { tokenMap in
                            tokenMap1 = tokenMap
                            dispatchGroup.leave()
                        }
                        
                        dispatchGroup.enter()
                        NetworkManager.shared.generateTokens(channelName: "\(channelName )_ex",
                                                             uid: "\(playerRTCUid)",
                                                             tokenGeneratorType: .token006,
                                                             tokenTypes: [.rtc]) { tokenMap in
                            tokenMap2 = tokenMap
                            dispatchGroup.leave()
                        }
                        
                        dispatchGroup.notify(queue: .main){
                            agoraPrint("createRoom get token cost: \(-date.timeIntervalSinceNow * 1000) ms")
                            guard let self = self,
                                  let rtcToken = tokenMap1[NetworkManager.AgoraTokenType.rtc.rawValue],
                                  let rtmToken = tokenMap1[NetworkManager.AgoraTokenType.rtm.rawValue],
                                  let rtcPlayerToken = tokenMap2[NetworkManager.AgoraTokenType.rtc.rawValue]
                            else {
                                _hideLoadingIfNeed()
                                return
                            }
                            VLUserCenter.user.ifMaster = VLUserCenter.user.id == userId ? true : false
                            VLUserCenter.user.agoraRTCToken = rtcToken
                            VLUserCenter.user.agoraRTMToken = rtmToken
                            VLUserCenter.user.agoraPlayerRTCToken = rtcPlayerToken
                            self.roomList?.append(roomModel)
                            self._autoOnSeatIfNeed { seatArray in
                                agoraPrint("createRoom _autoOnSeatIfNeed cost: \(-date.timeIntervalSinceNow * 1000) ms")
                                _hideLoadingIfNeed()
                                let output = KTVCreateRoomOutputModel()
                                output.name = inputModel.name
                                output.roomNo = roomModel.roomNo ?? ""
                                output.seatsArray = seatArray
                                completion(nil, output)
                                self._addUserIfNeed()
                            }
                        }
                    }
                }
            }
        }

        if isConnected == false {
            login { err in
                if err == nil {
                    create(roomInfo: roomInfo)
                } else {
                    completion(err, nil)
                }
            }
        } else {
            create(roomInfo: roomInfo)
        }
        
    }
    
    func joinRoom(inputModel: KTVJoinRoomInputModel, completion: @escaping (Error?, KTVJoinRoomOutputModel?) -> Void) {
        guard let roomInfo = roomList?.filter({ $0.roomNo == inputModel.roomNo }).first else {
            agoraAssert("join Room fail")
            completion(nil, nil)
            return
        }

        let params = mapConvert(model: roomInfo)

        _showLoadingIfNeed()
        let date = Date()

        let enterScene: () -> Void = {[weak self] in
            let scene = self?.getCurrentScene(with: roomInfo.roomNo ?? "")
            agoraPrint("joinRoom initScene cost: \(-date.timeIntervalSinceNow * 1000) ms")
            scene?.enter {[weak self] payload, err in
                agoraPrint("joinRoom joinScene cost: \(-date.timeIntervalSinceNow * 1000) ms")
                if let err = err {
                    agoraPrint("enter scene fail: \(err.localizedDescription)")
                    _hideLoadingIfNeed()
                    completion(err, nil)
                    return
                }
                                
                let channelName = inputModel.roomNo
                let userId = roomInfo.creatorNo
                self?.roomNo = channelName
                
                let playerRTCUid = UserInfo.userId//VLUserCenter.user.agoraPlayerRTCUid
                var tokenMap1:[Int: String] = [:], tokenMap2:[Int: String] = [:]
                
                let dispatchGroup = DispatchGroup()
                dispatchGroup.enter()
                NetworkManager.shared.generateTokens(channelName: channelName ?? "",
                                                     uid: "\(UserInfo.userId)",
                                                     tokenGeneratorType: .token006,
                                                     tokenTypes: [.rtc, .rtm]) { tokenMap in
                    tokenMap1 = tokenMap
                    dispatchGroup.leave()
                }
                
                dispatchGroup.enter()
                NetworkManager.shared.generateTokens(channelName: "\(channelName ?? "")_ex",
                                                     uid: "\(playerRTCUid)",
                                                     tokenGeneratorType: .token006,
                                                     tokenTypes: [.rtc]) { tokenMap in
                    tokenMap2 = tokenMap
                    dispatchGroup.leave()
                }
                
                dispatchGroup.notify(queue: .main){
                    agoraPrint("joinRoom get token cost: \(-date.timeIntervalSinceNow * 1000) ms")
                    guard let self = self,
                          let rtcToken = tokenMap1[NetworkManager.AgoraTokenType.rtc.rawValue],
                          let rtmToken = tokenMap1[NetworkManager.AgoraTokenType.rtm.rawValue],
                          let rtcPlayerToken = tokenMap2[NetworkManager.AgoraTokenType.rtc.rawValue]
                    else {
                        _hideLoadingIfNeed()
                        agoraAssert(tokenMap1.count == 2, "rtcToken == nil || rtmToken == nil")
                        agoraAssert(tokenMap2.count == 1, "playerRtcToken == nil")
                        completion(nil, nil)
                        return
                    }
                    VLUserCenter.user.ifMaster = VLUserCenter.user.id == userId ? true : false
                    VLUserCenter.user.agoraRTCToken = rtcToken
                    VLUserCenter.user.agoraRTMToken = rtmToken
                    VLUserCenter.user.agoraPlayerRTCToken = rtcPlayerToken
                    self._autoOnSeatIfNeed { seatArray in
                        agoraPrint("joinRoom _autoOnSeatIfNeed cost: \(-date.timeIntervalSinceNow * 1000) ms")
                        _hideLoadingIfNeed()
                        let output = KTVJoinRoomOutputModel()
                        output.creatorNo = userId
                        output.seatsArray = seatArray
                        completion(nil, output)
                        self._addUserIfNeed()
                    }
                }
            }
        }
                
        if isConnected == false {
            login {[weak self] err in
                if err == nil {
                    _hideLoadingIfNeed()
                    enterScene()
                } else {
                    _hideLoadingIfNeed()
                    completion(err, nil)
                }
            }
        } else {
            enterScene()
        }
    }
    
    func leaveRoom(completion: @escaping (Error?) -> Void) {
        
        guard let roomInfo = roomList?.filter({ $0.roomNo == self.roomNo }).first else {
            agoraAssert("leaveRoom channelName = nil")
            completion(nil)
            return
        }
        
       let performLeaveRoom: () -> Void = {[weak self] in
                   guard let self = self else {return}
           
           //leave if enter seat
           if let seat = seatMap.filter({ $0.value.userNo == VLUserCenter.user.id }).first?.value {
               _removeSeat(seatInfo: seat) { error in
               }
           }
           
           //remove current user's choose song
           _removeAllUserChooseSong()
           
           _leaveRoom(roomId: roomInfo.roomNo ?? "", isRoomOwner: roomInfo.creatorNo == user.id)

          // syncManager.rtmManager.unsubscribeMessage(channelName: roomInfo.roomId, delegate: self)
           roomNo = nil
           unsubscribeAll()
           completion(nil)
       }
       
       if isConnected == false {
           login {[weak self] err in
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
        let updateRoomInfo: () -> Void = {[weak self] in
            guard let self = self else {return}
            
            let roomInfo = AUIRoomInfo()
            roomInfo.roomName = self.room?.name ?? ""
            roomInfo.roomId = self.room?.roomNo ?? ""
            roomInfo.customPayload = [
                "roomPeopleNum": userCount,
                "createdAt": self.room?.createdAt as Any,
                "isPrivate": self.room?.isPrivate == true,
                "password": self.room?.password as Any,
                "creatorNo": self.room?.creatorNo as Any,
                "icon": self.room?.icon as Any
            ]

            let owner = AUIUserThumbnailInfo()
            owner.userId = self.room?.creatorNo ?? ""
            owner.userName = self.room?.creatorName ?? ""
            owner.userAvatar = self.room?.creatorAvatar ?? ""
            roomInfo.owner = owner
            
            roomManager.updateRoom(room: roomInfo) {[weak self] err, info in
                if let err = err {
                    agoraPrint("enter scene fail: \(err.localizedDescription)")
                    completion(err)
                    return
                }
                completion(nil)
            }
        }
        
        if isConnected == false {
            login {[weak self] err in
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
    func enterSeat(inputModel: KTVOnSeatInputModel, completion: @escaping (Error?) -> Void) {
        let seatInfo = _getUserSeatInfo(seatIndex: Int(inputModel.seatIndex))
        _addSeatInfo(seatInfo: seatInfo,
                     finished: completion)
    }
    
    func leaveSeat(inputModel: KTVOutSeatInputModel, completion: @escaping (Error?) -> Void) {
        let seatInfo = seatMap["\(inputModel.seatIndex)"]!
        _removeSeat(seatInfo: seatInfo) { error in
        }
        
        //remove current user's choose song
        _removeAllUserChooseSong(userNo: seatInfo.userNo ?? "")
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
        
        seatInfo.isAudioMuted = muted ? 1 : 0
        _updateSeat(seatInfo: seatInfo,
                    finished: completion)
    }
    
    func updateSeatVideoMuteStatus(muted: Bool, completion: @escaping (Error?) -> Void) {
        guard let seatInfo = self.seatMap
            .filter({ $0.value.userNo == VLUserCenter.user.id })
            .first?.value else {
            agoraAssert("open video seat not found")
            completion(nil)
            return
        }
        
        seatInfo.isVideoMuted = muted ? 1 : 0
        _updateSeat(seatInfo: seatInfo,
                    finished: completion)
    }
}

// only for music
extension KTVRTMManagerServiceImpl {
    func removeSong(inputModel: KTVRemoveSongInputModel, completion: @escaping (Error?) -> Void) {
        _removeChooseSong(songId: inputModel.objectId,
                          completion: completion)
    }
    
    func getChoosedSongsList(completion: @escaping (Error?, [VLRoomSelSongModel]?) -> Void) {
        _getChooseSongInfo(finished: completion)
    }
    
    func markSongDidPlay(inputModel: VLRoomSelSongModel, completion: @escaping (Error?) -> Void) {
        inputModel.status = .playing
        _updateChooseSong(songInfo: inputModel, finished: completion)
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
        songInfo.userNo = VLUserCenter.user.id
//        songInfo.userId = UserInfo.userId
        /// 点歌人昵称
        songInfo.name = VLUserCenter.user.name

        _addChooseSongInfo(songInfo: songInfo) { error in
            // TODO(wushengtao): fetch all list can not be changed if immediately invoke
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.2) {
                completion(error)
            }
        }
    }
    
    func pinSong(inputModel: KTVMakeSongTopInputModel, completion: @escaping (Error?) -> Void) {
        guard let topSong = songList.first,
              let song = songList.filter({ $0.objectId == inputModel.objectId }).first
        else {
            agoraAssert("make song to top not found! \(inputModel.songNo)")
            completion(nil)
            return
        }

        // mark input song to top
        song.pinAt = Int64(Date().timeIntervalSince1970 * 1000)

        //if top song is playing status, keep it always on top(_sortChooseSongList)
        if topSong.objectId != song.objectId, topSong.status != .playing {
            topSong.pinAt = Int64(Date().timeIntervalSince1970 * 1000)
            _updateChooseSong(songInfo: topSong) { error in
            }
        }

        // mark current playing song to top
        _updateChooseSong(songInfo: song) { error in
            completion(error)
        }
    }
}

//for chorus
extension KTVRTMManagerServiceImpl {
    func joinChorus(inputModel: KTVJoinChorusInputModel, completion: @escaping (Error?) -> Void) {
        guard let topSong = self.songList.filter({ $0.songNo == inputModel.songNo}).first else {
            agoraAssert("join Chorus fail")
            completion(nil)
            return
        }
        //TODO: _markSeatToPlaying without callback
        _markSeatChoursStatus(songCode: topSong.chorusSongId(),  completion: completion)
    }
    
    func coSingerLeaveChorus(completion: @escaping (Error?) -> Void) {
        _markSeatChoursStatus(songCode: "", completion: completion)
    }
    
    func enterSoloMode() {
        _markSoloSongIfNeed()
    }
    
    func changeMVCover(inputModel: KTVChangeMVCoverInputModel, completion: @escaping (Error?) -> Void) {
        
    }
    
}

// for subscribe
extension KTVRTMManagerServiceImpl {
    func subscribeUserListCountChanged(changedBlock: @escaping (UInt) -> Void) {
        userListCountDidChanged = changedBlock
    }
    
    func subscribeUserChanged(changedBlock: @escaping (KTVSubscribe, VLLoginModel) -> Void) {
        userDidChanged = changedBlock
    }
    
    func subscribeSeatListChanged(changedBlock: @escaping (KTVSubscribe, VLRoomSeatModel) -> Void) {
        seatListDidChanged = changedBlock
    }
    
    func subscribeRoomStatusChanged(changedBlock: @escaping (KTVSubscribe, VLRoomListModel) -> Void) {
        roomStatusDidChanged = changedBlock
    }
    
    func subscribeChooseSongChanged(changedBlock: @escaping (KTVSubscribe, VLRoomSelSongModel, [VLRoomSelSongModel]) -> Void) {
        chooseSongDidChanged = changedBlock
    }
    
    func subscribeNetworkStatusChanged(changedBlock: @escaping (KTVServiceNetworkStatus) -> Void) {
        networkDidChanged = changedBlock
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
        guard let room = self.room else { return false }
        let currentTs = Int64(Date().timeIntervalSince1970 * 1000)
        let expiredDuration = 20 * 60 * 1000
        agoraPrint("checkRoomExpire: \(currentTs - room.createdAt) / \(expiredDuration)")
        
        if abs(currentTs - room.createdAt) > expiredDuration {
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
        let scene = getCurrentScene(with: channelName)
        scene.unbindRespDelegate(delegate: self)
        scene.userService.unbindRespDelegate(delegate: self)
        
        userListCountDidChanged = nil
        seatListDidChanged = nil
        roomStatusDidChanged = nil
        chooseSongDidChanged = nil
//        singingScoreDidChanged = nil
        networkDidChanged = nil
        roomExpiredDidChanged = nil
        expireTimer?.invalidate()
        expireTimer = nil
    }
}

// model, dict convert tool
extension KTVRTMManagerServiceImpl {
    private func convertAUIRoomInfo2KTVRoomInfo(with roomInfo: AUIRoomInfo) -> VLRoomListModel {
        let roomDict: [String: Any] = roomInfo.customPayload
        let room = VLRoomListModel()
        
        room.roomNo = roomInfo.roomId
        room.name = roomInfo.roomName
        room.creatorNo = roomInfo.owner?.userId ?? ""
        room.creatorAvatar = roomInfo.owner?.userAvatar
        room.creatorName = roomInfo.owner?.userName
        
        if let roomPeopleNum = roomDict["roomPeopleNum"] as? Int64 {
            room.roomPeopleNum = String(roomPeopleNum)
        }
        if let icon = roomDict["icon"] as? String {
            room.icon = icon
        }
        if let isPrivate = roomDict["isPrivate"] as? Bool {
            room.isPrivate = isPrivate
        }
        if let createdAt = roomDict["createdAt"] as? Int64 {
            room.createdAt = createdAt
        }
        if let updatedAt = roomDict["updatedAt"] as? Int64 {
            room.updatedAt = updatedAt
        }
        if let objectId = roomDict["objectId"] as? String {
            room.objectId = objectId
        }
        if let password = roomDict["password"] as? String {
            room.password = password
        }
        if let bgOption = roomDict["bgOption"] as? Int {
            room.bgOption = bgOption
        }
        if let soundEffect = roomDict["soundEffect"] as? String {
            room.soundEffect = soundEffect
        }
        if let status = roomDict["status"] as? String {
            room.status = status
        }
        if let deletedAt = roomDict["deletedAt"] as? String {
            room.deletedAt = deletedAt
        }
        return room
    }
    
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
    
    private func getCurrentSeatCollection(with roomId: String) -> AUIListCollection? {
        let collection: AUIListCollection? = getCurrentScene(with: roomId).getCollection(key: SYNC_MANAGER_SEAT_INFO)
     //   if !collectionBinded {
            collection?.subscribeAttributesDidChanged(callback: {[weak self] str1, str2, model in
                if let dict = model.getList(), let self = self {
                    
                    let seats = dict.compactMap { self.convertSeatDictToSeat(with: $0) }
                    let diff = seats.difference(from: self.seatList)
                    //麦位比较 结果返回
                    for change in diff {
                        switch change {
                        case let .remove(offset, oldElement, _):
                            unChangesOldList.remove(at: offset)
                            self.respDelegates.allObjects.forEach { obj in
                                obj.onChoristerDidLeave(chorister: oldElement)
                            }
                        case let .insert(_, newElement, _):
                            self.respDelegates.allObjects.forEach { obj in
                                obj.onChoristerDidEnter(chorister: newElement)
                            }
                            
                        case let .update():
                            break;
                        }
                    }
                    
                    self.seatList = seats;
                }
            })
        
//            collectionBinded = true
//        }
        return collection
    }
    
    private func getUserCollection(with roomId: String) -> AUIListCollection? {
        let collection: AUIListCollection? = getCurrentScene(with: roomId).getCollection(key: SYNC_SCENE_ROOM_USER_COLLECTION)
     //   if !collectionBinded {
            collection?.subscribeAttributesDidChanged(callback: {[weak self] str1, str2, model in
                if let dict = model.getList(), let self = self {
                    // seat change callback is here
                    //self.listener?.onStartGameInfoDidChanged(startGameInfo: self.convertDict2JoyStartGameInfo(with: dict))
                }
            })
//            collectionBinded = true
//        }
        return collection
    }
    
    private func getCurrentSongCollection(with roomId: String) -> AUIListCollection? {
        let collection: AUIListCollection? = getCurrentScene(with: roomId).getCollection(key: SYNC_MANAGER_CHOOSE_SONG_INFO)
     //   if !collectionBinded {
            collection?.subscribeAttributesDidChanged(callback: {[weak self] str1, str2, model in
                if let dict = model.getMap(), let self = self {
                    // seat change callback is here
                    //self.listener?.onStartGameInfoDidChanged(startGameInfo: self.convertDict2JoyStartGameInfo(with: dict))
                }
            })
//            collectionBinded = true
//        }
        return collection
    }
}

extension KTVRTMManagerServiceImpl: AUISceneRespDelegate {
    private func _leaveRoom(roomId: String, isRoomOwner: Bool) {
        KTVLog.info(text: "_leaveRoom: \(roomId) isRoomOwner:\(isRoomOwner)")
        let scene = self.syncManager.getScene(channelName: roomId)
        scene.unbindRespDelegate(delegate: self)
        scene.userService.unbindRespDelegate(delegate: self)
        if isRoomOwner {
            scene.delete()
            roomManager.destroyRoom(roomId: roomId) { _ in
                
            }
        } else {
            scene.leave()
        }
    }
    
    func onSceneDestroy(roomId: String) {
        KTVLog.info(text: "onSceneDestroy: \(roomId)")
        guard let model = self.roomList?.filter({ $0.roomNo == roomId }).first else {
            return
        }
        
        _leaveRoom(roomId: roomId, isRoomOwner: true)
        //self.listener?.onRoomDidDestroy(roomInfo: model)
    }
    
    func onTokenPrivilegeWillExpire(channelName: String?) {
        NetworkManager.shared.generateToken(appId: appId, appCertificate: appCertificate, channelName: "", uid: String(user.id), tokenType: .token007, type: .rtm) { token in
            if let token = token {
                self.syncManager.rtmManager.renew(token: token) { err in
                    KTVLog.error(text: "renew token：err \(String(describing: err?.reason))")
                    if err == nil {
                        
                    }
                }
            }
        }
    }
}

extension KTVRTMManagerServiceImpl: AUIUserRespDelegate {
    func onRoomUserSnapshot(roomId: String, userList: [AUIUserInfo]) {
        let list = userList.compactMap({ convertAUIUserInfo2UserInfo(with: $0)}) 
        self.userList = list
    }
    
    func onRoomUserEnter(roomId: String, userInfo: AUIUserInfo) {
        print("user: enter\(userInfo.userName)")
        let user = convertAUIUserInfo2UserInfo(with: userInfo)
        self.userList.append(user)
        self.userDidChanged?(.created, user)
        self.userListCountDidChanged?(UInt(self.userList.count))
    }
    
    func onRoomUserLeave(roomId: String, userInfo: AUIUserInfo) {
        print("user: leave\(userInfo.userName)")
        let userList = self.userList
        self.userList = userList.filter({ $0.userNo != userInfo.userId})
        let user = convertAUIUserInfo2UserInfo(with: userInfo)
        self.userDidChanged?(.deleted, user)
        self.userListCountDidChanged?(UInt(self.userList.count))
        if VLUserCenter.user.ifMaster {
            self.updateRoom(with: self.userList.count) { err in
                print("人数减少：\(self.userList.count)")
            }
        }
    }
    
    func onRoomUserUpdate(roomId: String, userInfo: AUIUserInfo) {
        print("user: update\(userInfo.userName)")
        if let idx = self.userList.firstIndex(where: { $0.userNo == userInfo.userId}) {
            self.userList[idx] = convertAUIUserInfo2UserInfo(with: userInfo)
            self.userDidChanged?(.updated, user)
            self.userListCountDidChanged?(UInt(self.userList.count))
            return
        }
        let user = convertAUIUserInfo2UserInfo(with: userInfo)
        self.userList.append(user)
        self.userDidChanged?(.updated, user)
        self.userListCountDidChanged?(UInt(self.userList.count))
        if VLUserCenter.user.ifMaster {
            self.updateRoom(with: self.userList.count) { err in
                print("人数增加：\(self.userList.count)")
            }
        }
    }
  
    func onUserAudioMute(userId: String, mute: Bool) {
        
    }
    
    func onUserVideoMute(userId: String, mute: Bool) {
        
    }
    
    func onUserBeKicked(roomId: String, userId: String) {
        
    }
}

extension KTVRTMManagerServiceImpl: AUIRtmErrorProxyDelegate {
    
    private func getCurrentScene(with channelName: String) -> AUIScene {
        let scene = self.syncManager.getScene(channelName: channelName)
      //  if !sceneBinded {
        scene.userService.bindRespDelegate(delegate: self)
        scene.bindRespDelegate(delegate: self)
           // sceneBinded = true
       // }
        return scene
    }
    
    private func login(completion:(@escaping (Error?)-> Void)) {
        NetworkManager.shared.generateToken(appId: appId, appCertificate: appCertificate, channelName: "", uid: String(user.id), tokenType: .token007, type: .rtm) { token in
            if let token = token {
                self.syncManager.rtmManager.login(token: token) { err in
                    self.isConnected = err == nil ? true : false
                    completion(err)
                }
            }
        }
    }
    
    public func onConnectionStateChanged(channelName: String,
                                         connectionStateChanged state: AgoraRtmClientConnectionState,
                                         result reason: AgoraRtmClientConnectionChangeReason) {
        if reason == .changedChangedLost {
            //这里断连了，需要重新login
            self.isConnected = false
            login { err in
                self.isConnected = err == nil ? true : false
            }
        }
    }
    
    

}

// user
extension KTVRTMManagerServiceImpl {
    private func _addUserIfNeed() {
//        _subscribeOnlineUsers {}
        _getUserInfo { error, userList in
            // current user already add
            if self.userList.contains(where: { $0.id == VLUserCenter.user.id }) {
                return
            }
            self._addUserInfo {
//                self._getUserInfo { error, userList in
//                }
            }
        }
    }

    private func _getUserInfo(finished: @escaping (Error?, [VLLoginModel]?) -> Void) {
        guard let channelName = roomNo else {
//            agoraAssert("channelName = nil")
            finished(nil, nil)
            return
        }
        agoraPrint("imp user get...")
        let collection = getUserCollection(with: channelName)
        collection?.getMetaData(callback: {[weak self] err, data in
            if err != nil {
                agoraPrint("imp user get fail :\(String(describing: err?.description))...")
                agoraPrint("error = \(String(describing: err?.description))")
                finished(err, nil)
            } else {
                if data == nil {
                    finished(err, [])
                } else {
                    if let userData = data as? [[String: Any]], let self = self {
                        let users = userData.compactMap { self.convertUserDictToUser(with: $0) }
                        finished(nil, users)
                    }
                }
            }
        })
//        SyncUtil
//            .scene(id: channelName)?
//            .collection(className: SYNC_SCENE_ROOM_USER_COLLECTION)
//            .get(success: { [weak self] list in
//                agoraPrint("imp user get success...")
//                let users = list.compactMap({ VLLoginModel.yy_model(withJSON: $0.toJson()!)! })
////            guard !users.isEmpty else { return }
//                self?.userList = users
//                self?._updateUserCount(completion: { error in
//
//                })
//                finished(nil, users)
//            }, fail: { error in
//                agoraPrint("imp user get fail :\(error.message)...")
//                agoraPrint("error = \(error.description)")
//                finished(error, nil)
//            })
    }

    private func _addUserInfo(finished: @escaping () -> Void) {
        guard let channelName = roomNo else {
//            assert(false, "channelName = nil")
            agoraPrint("addUserInfo channelName = nil")
            return
        }
        agoraPrint("imp user add ...")
        let model = VLUserCenter.user
        model.token = ""
        model.im_token = ""
        model.agoraRTCToken = ""
        model.agoraRTMToken = ""
        model.agoraPlayerRTCToken = ""
        let params = mapConvert(model: model)
        let collection = getUserCollection(with: channelName)
        print("params:\(params)")
        collection?.addMetaData(valueCmd: nil, value: params, filter: nil, callback: { err in
            if err != nil {
                agoraPrint("imp user get fail :\(String(describing: err?.description))...")
                agoraPrint("error = \(String(describing: err?.description))")
                finished()
            } else {
                agoraPrint("imp user add success...")
                finished()
            }
        })
//        SyncUtil
//            .scene(id: channelName)?
//            .collection(className: SYNC_SCENE_ROOM_USER_COLLECTION)
//            .add(data: params, success: { object in
//                agoraPrint("imp user add success...")
//                finished()
//            }, fail: { error in
//                agoraPrint("imp user add fail :\(error.message)...")
//                agoraPrint(error.message)
//                finished()
//            })
    }
}

// seat
extension KTVRTMManagerServiceImpl {
    
    private func _markSeatChoursStatus(songCode: String, completion: @escaping (Error?)->()) {
        guard let seatInfo = self.seatMap
            .filter({ $0.value.userNo == VLUserCenter.user.id })
            .first?.value else {
            agoraAssert("mark join seat not found")
            //TODO: error
            completion(nil)
            return
        }
        seatInfo.chorusSongCode = songCode
        _updateSeat(seatInfo: seatInfo, finished: completion)
    }
    
    private func _getInitSeats() -> [VLRoomSeatModel] {
        var seatArray = [VLRoomSeatModel]()
        for i in 0...7 {
            if let seat = seatMap["\(i)"] {
                seatArray.append(seat)
                continue
            }
            let seat = VLRoomSeatModel()
            seat.seatIndex = i
            seatArray.append(seat)

            seatMap["\(i)"] = seat
        }

        return seatArray
    }
    
    private func _getUserSeatInfo(seatIndex: Int, model: VLRoomSeatModel? = nil) -> VLRoomSeatModel {
        let user = VLUserCenter.user
        let seatInfo = VLRoomSeatModel()
        seatInfo.seatIndex = seatIndex
        seatInfo.rtcUid = user.id
        seatInfo.userNo = user.id
        seatInfo.headUrl = user.headUrl
        seatInfo.name = user.name
        seatInfo.objectId = ""
        
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
    
    private func _autoOnSeatIfNeed(completion: @escaping ([VLRoomSeatModel])->()) {
//        _subscribeSeats {}

       // userList.removeAll()
        songList.removeAll()
        seatMap.removeAll()
        _getSeatInfo { [weak self] (error, list) in
            guard let self = self, let list = list else {
                return
            }
            
            //TODO: _getSeatInfo will callback if remove seat invoke
            guard self.seatMap.count == 0 else {
                return
            }
            
            list.forEach { seat in
                self.seatMap["\(seat.seatIndex)"] = seat
            }

            // update seat info (user avater/nick name did changed) if seat existed
            if let seat = self.seatMap.filter({ $0.value.userNo == VLUserCenter.user.id }).first?.value {
                let targetSeatInfo = self._getUserSeatInfo(seatIndex: seat.seatIndex, model: seat)
                targetSeatInfo.objectId = seat.objectId
                self._updateSeat(seatInfo: targetSeatInfo) { error in
                    completion(self._getInitSeats())
                }
                return
            }
            guard VLUserCenter.user.ifMaster else {
                completion(self._getInitSeats())
                return
            }

            // add master to first seat
            let targetSeatInfo = self._getUserSeatInfo(seatIndex: 0)
            targetSeatInfo.isAudioMuted = 0
            targetSeatInfo.isMaster = true
            self._addSeatInfo(seatInfo: targetSeatInfo) { error in
                completion(self._getInitSeats())
            }
        }
    }
    
    private func _getSeatInfo(finished: @escaping (Error?, [VLRoomSeatModel]?) -> Void) {
        guard let channelName = roomNo else {
            agoraAssert("channelName = nil")
            finished(nil, nil)
            return
        }
        agoraPrint("imp seat get...")
        
        let collection = getCurrentSeatCollection(with: channelName)
        //cp todo 需要确认返回的数据格式
        var models = [VLRoomSeatModel]()
        collection?.getMetaData(callback: {[weak self] err, data in
            if err != nil {
                agoraPrint("imp seat get fail :\(String(describing: err?.description))...")
                agoraPrint("error = \(String(describing: err?.description))")
                finished(err, nil)
            } else {
                if data == nil {
                    finished(err, [])
                } else {
                    if let seatData = data as? [[String: Any]], let self = self {
                        let seats = seatData.compactMap { self.convertSeatDictToSeat(with: $0) }
                        finished(nil, seats)
                    }
                }
            }
        })
        
//        SyncUtil
//            .scene(id: channelName)?
//            .collection(className: SYNC_MANAGER_SEAT_INFO)
//            .get(success: { list in
//                agoraPrint("imp seat get success...")
//                let seats = list.compactMap({ VLRoomSeatModel.yy_model(withJSON: $0.toJson()!)! })
//                
//                finished(nil, seats)
//            }, fail: { error in
//                agoraPrint("imp seat get fail...")
//                agoraPrint("error = \(error.description)")
//                finished(error, nil)
//            })
    }

    private func _updateSeat(seatInfo: VLRoomSeatModel,
                            finished: @escaping (Error?) -> Void)
    {
        guard let channelName = roomNo,
              let objectId = seatInfo.objectId
        else {
            agoraPrint("updateSeatInfo channelName = nil")
            return
        }
        
        agoraPrint("imp seat update... [\(objectId)]")
        let collection = getCurrentSeatCollection(with: channelName)
        let params = mapConvert(model: seatInfo)
        collection?.updateMetaData(valueCmd: nil, value: params, filter: nil, callback: { err in
            finished(err)
        })
//        let params = mapConvert(model: seatInfo)
//        SyncUtil
//            .scene(id: channelName)?
//            .collection(className: SYNC_MANAGER_SEAT_INFO)
//            .update(id: objectId,
//                    data: params,
//                    success: {
//                agoraPrint("imp seat update success...")
//                finished(nil)
//            }, fail: { error in
//                agoraPrint("imp seat update fail...")
//                finished(NSError(domain: error.message, code: error.code))
//            })
    }

    private func _removeSeat(seatInfo: VLRoomSeatModel,
                            finished: @escaping (Error?) -> Void)
    {
        guard let channelName = roomNo
        else {
            agoraPrint("removeSeat channelName = nil")
            return
        }
        
        agoraPrint("imp seat delete... [\(String(describing: seatInfo.userNo))]")
        let collection = getCurrentSeatCollection(with: channelName)
        let model = VLRoomSeatModel()
        model.seatIndex = seatInfo.seatIndex
        let params = mapConvert(model: model)
        collection?.updateMetaData(valueCmd: nil, value: params, filter: nil, callback: { err in
            finished(err)
        })
//        SyncUtil
//            .scene(id: channelName)?
//            .collection(className: SYNC_MANAGER_SEAT_INFO)
//            .document(id: objectId)
//            .delete(success: {_ in
//                agoraPrint("imp seat delete success...")
//                finished(nil)
//            }, fail: { error in
//                agoraPrint("imp seat delete fail...")
//                finished(NSError(domain: error.message, code: error.code))
//            })
    }

    private func _addSeatInfo(seatInfo: VLRoomSeatModel,
                             finished: @escaping (Error?) -> Void)
    {
        guard let channelName = roomNo else {
//            assert(false, "channelName = nil")
            agoraPrint("addUserInfo channelName = nil")
            let error = NSError(domain: "addUserInfo channelName = nil", code: -1)
            finished(error)
            return
        }
        
        agoraPrint("imp seat add...")
        let params = mapConvert(model: seatInfo)
        let collection = getCurrentSeatCollection(with: channelName)
        collection?.addMetaData(valueCmd: nil, value: params, filter: nil, callback: {[weak self] err in
            self?.seatMap["\(seatInfo.seatIndex)"] = seatInfo
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
            if err != nil {
                agoraPrint("imp song get fail :\(String(describing: err?.description))...")
                agoraPrint("error = \(String(describing: err?.description))")
                finished(err, nil)
            } else {
                if songs == nil {
                    finished(err, nil)
                } else {
                    if let songData = songs as? [[String: Any]], let self = self {
                        let songs = songData.compactMap { self.convertSongDictToSong(with: $0) }
                        finished(nil, songs)
                    }
                }
            }
        })
//        SyncUtil
//            .scene(id: channelName)?
//            .collection(className: SYNC_MANAGER_CHOOSE_SONG_INFO)
//            .get(success: { [weak self] list in
//                guard let self = self else {
//                    return
//                }
//                agoraPrint("imp song get success... \(list.count)")
//                let totalList = list.compactMap({
//                    VLRoomSelSongModel.yy_model(withJSON: $0.toJson()!)!
//                })
//                self.songList = totalList.filterDuplicates({$0.songNo})
//                self._sortChooseSongList()
//                let songList = self.songList
//                finished(nil, songList)
//            }, fail: { error in
//                agoraPrint("imp song get fail \(error.description)...")
//                finished(error, nil)
//            })
    }

    private func _updateChooseSong(songInfo: VLRoomSelSongModel,
                                  finished: @escaping (Error?) -> Void)
    {
        guard let channelName = roomNo, let objectId = songInfo.objectId else {
//            assert(false, "channelName = nil")
            agoraPrint("update song channelName = nil")
            return
        }

        let params = mapConvert(model: songInfo)
        agoraPrint("imp song update... [\(objectId)]")
        let collection = getCurrentSongCollection(with: channelName)
        collection?.updateMetaData(valueCmd: nil, value: params, filter: nil, callback: { err in
            finished(err)
        })
//        SyncUtil
//            .scene(id: channelName)?
//            .collection(className: SYNC_MANAGER_CHOOSE_SONG_INFO)
//            .update(id: objectId,
//                    data: params,
//                    success: {
//                agoraPrint("imp song update success...")
//                finished(nil)
//            }, fail: { error in
//                agoraPrint("imp song update fail \(error.description)...")
//                finished(error)
//            })
    }

    private func _addChooseSongInfo(songInfo: VLRoomSelSongModel, finished: @escaping (Error?) -> Void) {
        guard let channelName = roomNo else {
//            assert(false, "channelName = nil")
            agoraPrint("addUserInfo channelName = nil")
            return
        }
        agoraPrint("imp song add...")
        songInfo.createAt = Int64(Date().timeIntervalSince1970 * 1000)
        let params = mapConvert(model: songInfo)
        let collection = getCurrentSongCollection(with: channelName)
        collection?.addMetaData(valueCmd: nil, value: params, filter: nil, callback: { err in
            finished(err)
        })
//        SyncUtil
//            .scene(id: channelName)?
//            .collection(className: SYNC_MANAGER_CHOOSE_SONG_INFO)
//            .add(data: params,
//                 success: { obj in
//                agoraPrint("imp song add success...")
//                finished(nil)
//            }, fail: { error in
//                agoraPrint("imp song add fail...")
//                finished(error)
//            })
    }
    
    private func _removeAllUserChooseSong(userNo: String = VLUserCenter.user.id) {
        let userSongLists = self.songList.filter({ $0.userNo == userNo})
        //reverse delete songs to fix conflicts (user A remove song1 & user B update song1.status = 2)
        userSongLists.reversed().forEach { model in
            self._removeChooseSong(songId: model.objectId) { error in
            }
        }
    }

    private func _removeChooseSong(songId: String?, completion: @escaping (Error?) -> Void) {
        guard let channelName = roomNo,
              let objectId = songId
        else {
            agoraAssert("channelName = nil")
            completion(nil)
            return
        }
        agoraPrint("imp song delete... [\(objectId)]")
        let collection = getCurrentSongCollection(with: channelName)
        collection?.removeMetaData(valueCmd: nil, filter: [["songCode": songId ?? ""]], callback: { err in
            completion(err)
        })
//        SyncUtil
//            .scene(id: channelName)?
//            .collection(className: SYNC_MANAGER_CHOOSE_SONG_INFO)
//            .document(id: objectId)
//            .delete(success: {_ in
//                completion(nil)
//                agoraPrint("imp song delete success...")
//            }, fail: { error in
//                agoraPrint("imp song delete fail \(error.message)...")
//                completion(NSError(domain: error.message, code: error.code))
//            })
    }

    private func _markCurrentSongIfNeed() {
        guard let topSong = songList.first,
              topSong.status == .playing, // ready status
             // topSong.isChorus == false,
              topSong.userNo == VLUserCenter.user.id
        else {
            return
        }

        topSong.status = .playing
        _updateChooseSong(songInfo: topSong) { error in
        }
    }

    private func _markSoloSongIfNeed() {
        guard let topSong = songList.first,
            //  topSong.isChorus == true, // current is chorus
              topSong.userNo == VLUserCenter.user.id
        else {
            KTVLog.warning(text: "_markSoloSongIfNeed break:  \(songList.first?.status.rawValue ?? 0) \(songList.first?.userNo ?? "")/\(VLUserCenter.user.id)")
            return
        }
        
        let status = topSong.status
        topSong.status = .playing
        _updateChooseSong(songInfo: topSong) { error in
        }
        topSong.status = status
        //自己不需要标记
//        _markSeatChoursStatus(songCode: nil) { err in
//        }
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

private func _showLoadingIfNeed() {
    SVProgressHUD.show()
}

private func _hideLoadingIfNeed() {
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

