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


public enum KTVCommonError {
    case unknown      //未知错误
    case micSeatNotIdle   //麦位不空闲
    case micSeatAlreadyEnter   //已经上麦过了
    case userNoEnterSeat   //观众未上麦
    case chooseSongAlreadyExist   //歌曲已经选择过了
    case chooseSongNotExist   //歌曲已经选择过了
    case choristerAlreadyExist   //合唱用户已存在
    case choristerNotExist    //合唱用户不存在
    case noPermission   //无权限
    case chooseSongIsFail   //选择歌曲失败
    
    public func toNSError() -> NSError {
        func createError(code: Int = -1, msg: String) -> NSError {
            return NSError(domain: "AUIKit Error", code: Int(code), userInfo: [ NSLocalizedDescriptionKey : msg])
        }
        switch self {
        case .micSeatNotIdle:
            return createError(msg: "mic seat not idle")
        case .micSeatAlreadyEnter:
            return createError(msg: "user already enter seat")
        case .userNoEnterSeat:
            return createError(msg: "user not enter seat")
        case .chooseSongAlreadyExist:
            return createError(msg: "already choose song")
        case .chooseSongNotExist:
            return createError(msg: "song not exist")
        case .choristerAlreadyExist:
            return createError(msg: "chorister already exist")
        case .choristerNotExist:
            return createError(msg: "chorister not exist")
        case .noPermission:
            return createError(msg: "no permission")
        case .chooseSongIsFail:
            return createError(msg: "choost song model fail")
        default:
            return createError(msg: "unknown error")
        }
    }
}

extension NSError {
    static func auiError(_ description: String) -> NSError {
        return NSError(domain: "AUIKit Error",
                       code: -1,
                       userInfo: [ NSLocalizedDescriptionKey : description])
    }
}

/// 座位信息
private let SYNC_MANAGER_SEAT_INFO = "seat_info"
// 选歌
private let SYNC_MANAGER_CHOOSE_SONG_INFO = "choose_song"
//合唱
private let SYNC_MANAGER_CHORUS_INFO = "chorister_info"

private enum AUIMicSeatCmd: String {
    case leaveSeatCmd = "leaveSeatCmd"
    case enterSeatCmd = "enterSeatCmd"
    case muteAudioCmd = "muteAudioCmd"
    case muteVideoCmd = "muteVideoCmd"
    case kickSeatCmd = "kickSeatCmd"
}

private enum AUIMusicCmd: String {
    case chooseSongCmd = "chooseSongCmd"
    case removeSongCmd = "removeSongCmd"
    case pingSongCmd = "pingSongCmd"
    case updatePlayStatusCmd = "updatePlayStatusCmd"
}

private enum AUIChorusCMd: String {
    case joinCmd = "joinChorusCmd"
    case leaveCmd = "leaveChorusCmd"
}

@objcMembers class KTVRTMManagerServiceImpl: NSObject, KTVServiceProtocol {
    
    private var appId: String
    private var user: VLLoginModel
    private var host: String
    private var appCertificate: String
    
    private var roomList: [AUIRoomInfo]?
//    private var userList: [VLLoginModel] = .init()
    @objc var seatMap: [String: VLRoomSeatModel] = [:]
    @objc var songList: [VLRoomSelSongModel] = []
    @objc var choristerList: [KTVChoristerModel] = []
    
    private weak var delegate: KTVServiceListenerProtocol?
    
    private var roomNo: String?
    private var expireTimer: Timer?
    private var isConnected: Bool = false
    
    @objc var room: AUIRoomInfo? {
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
        
        let userId = VLUserCenter.user.id
        let date = Date()
        NetworkManager.shared.generateTokens(channelName: "",
                                             uid: "\(userId)",
                                             tokenGeneratorType: .token007,
                                             tokenTypes: [.rtc, .rtm],
                                             expire: 24 * 60 * 60) {  tokenMap in
            guard let rtcToken = tokenMap[NetworkManager.AgoraTokenType.rtc.rawValue],
                  rtcToken.count > 0,
                  let rtmToken = tokenMap[NetworkManager.AgoraTokenType.rtm.rawValue],
                  rtmToken.count > 0 else {
                completion(NSError(domain: "generate token fail", code: -1))
                return
            }
            
            commerceLogger.info("[Timing]preGenerateToken rtc & rtm cost: \(Int64(-date.timeIntervalSinceNow * 1000)) ms")
            AppContext.shared.agoraRTCToken = rtcToken
            AppContext.shared.agoraRTMToken = rtmToken
            completion(nil)
        }
    }
    
    private func _subscribeAll() {
        guard let roomNo = self.roomNo else {return}
        let _ = self.syncManager.createScene(channelName: roomNo)
        _subscribeSeat()
        _subscribeSong()
        _subscribeChorus()
    }
    
    private func _subscribeSeat() {
        guard let roomNo = self.roomNo, let collection = getSeatCollection(with: roomNo) else {return}
        
        //get seat owner
        func getSeatUserId(_ map: Any) -> String? {
            return (((map as? [String: Any])?["owner"]) as? [String: Any])?["userId"] as? String
        }
        
        collection.subscribeAttributesDidChanged {[weak self] str1, str2, model in
            guard let self = self, let map = model.getMap() as? [String: [String: Any]] else { return }
            var seatMap: [String: VLRoomSeatModel] = [:]
            map.values.forEach { element in
                guard let micSeat = VLRoomSeatModel.yy_model(with: element) else {return}
                seatMap["\(micSeat.seatIndex)"] = micSeat
                
            }
            if self.seatMap.isEmpty {
                self.seatMap = seatMap
                self.delegate?.onMicSeatSnapshot(seat: seatMap)
            } else {
                seatMap.values.forEach { micSeat in
                    let index = micSeat.seatIndex
                    let origMicSeat = self.seatMap["\(index)"]
                    
                    self.seatMap["\(index)"] = micSeat
                    if let origMicSeat = origMicSeat,
                       let origUserId = origMicSeat.owner?.userId,
                       origUserId.count > 0,
                       let owner = micSeat.owner,
                        owner.userId != origUserId {
                        self.delegate?.onUserLeaveSeat(seatIndex: micSeat.seatIndex, user: owner)
                    }
                    
                    if let owner = micSeat.owner,
                       owner.userId.count > 0,
                       origMicSeat?.owner?.userId != owner.userId {
                        self.delegate?.onUserEnterSeat(seatIndex: micSeat.seatIndex, user: owner)
                    }
                    
                    if origMicSeat?.isAudioMuted != micSeat.isAudioMuted {
                        self.delegate?.onSeatAudioMute(seatIndex: micSeat.seatIndex, isMute: micSeat.isAudioMuted)
                    }
                    
                    if origMicSeat?.isVideoMuted != micSeat.isVideoMuted {
                        self.delegate?.onSeatVideoMute(seatIndex: micSeat.seatIndex, isMute: micSeat.isVideoMuted)
                    }
                }
            }
        }
        
        collection.subscribeWillMerge { publisherId, dataCmd, updateMap, currentMap in
            guard let dataCmd = AUIMicSeatCmd(rawValue: dataCmd ?? "") else {
                return AUICommonError.unknown.toNSError()
            }
            
            //only support enter one seat
            guard  updateMap.keys.count == 1, let key = updateMap.keys.first, let value = updateMap[key] else {
                return KTVCommonError.unknown.toNSError()
            }
            
            switch dataCmd {
            case .enterSeatCmd:
                let userId = getSeatUserId(value) ?? ""
                //check wheather the user has entered the seat
                if currentMap.values.contains(where: { getSeatUserId($0) == userId }) {
                    return KTVCommonError.micSeatAlreadyEnter.toNSError()
                }
                
                //check current seat already has user
                if getSeatUserId(value)?.isEmpty == false {
                    return KTVCommonError.micSeatNotIdle.toNSError()
                }
            case .leaveSeatCmd, .kickSeatCmd:
                guard let seatIndex = Int(key), let currentValue = currentMap[key] else {
                    return KTVCommonError.unknown.toNSError()
                }
                
                //room owner can not be leave seat
                if seatIndex == 0 {
                    return KTVCommonError.noPermission.toNSError()
                }
                let seatUserId = getSeatUserId(currentValue) ?? ""
                if dataCmd == .leaveSeatCmd {
                    let isRoomOwner = AUIRoomContext.shared.isRoomOwner(channelName: roomNo, userId: publisherId)
                    //only the user who entered the seat or room owner can leave the seat
                    guard seatUserId == publisherId || isRoomOwner else {
                        return KTVCommonError.userNoEnterSeat.toNSError()
                    }
                }
                
                //clean the user all metadata info
                self._removeChooseSong(userId: seatUserId) { _ in
                }
                self._removeChorus(userId: seatUserId) { _ in
                }
            case .muteAudioCmd, .muteVideoCmd:
                guard let seatIndex = Int(key), let currentValue = currentMap[key] else {
                    return KTVCommonError.unknown.toNSError()
                }
                
                //room owner can not be leave seat
                if seatIndex == 0 {
                    return KTVCommonError.noPermission.toNSError()
                }
                let seatUserId = getSeatUserId(currentValue) ?? ""
                let isRoomOwner = AUIRoomContext.shared.isRoomOwner(channelName: roomNo, userId: publisherId)
                //only the user who entered the seat or room owner can leave the seat
                guard seatUserId == publisherId || isRoomOwner else {
                    return KTVCommonError.userNoEnterSeat.toNSError()
                }
            }
            
            return nil
        }
        
        //set auto enter seat index
        collection.subsceibeValueWillChange {[weak collection] publisherId, dataCmd, newItem in
            guard let dataCmd = AUIMicSeatCmd(rawValue: dataCmd ?? ""),
                  dataCmd == .enterSeatCmd,
                  let currentSeat = collection?.getLocalMetaData()?.getMap() else {
                return newItem
            }
            //only support enter one seat
            guard newItem.keys.count == 1, let key = newItem.keys.first, currentSeat[key] == nil else {
                return newItem
            }
            var tempItem: [String: Any] = [:]
            
            for i in 1...7 {
                if let value = currentSeat["\(i)"], getSeatUserId(value)?.isEmpty == true {
                    tempItem["\(i)"] = newItem[key]
                    return tempItem
                }
            }
            
            return newItem
        }
    }
    
    private func _subscribeSong() {
        guard let roomNo = self.roomNo, let collection = getSongCollection(with: roomNo) else {return}
        
        //get the seat owner or song owner
        func getUserId(_ map: Any) -> String? {
            return (((map as? [String: Any])?["owner"]) as? [String: Any])?["userId"] as? String
        }
        
        collection.subscribeAttributesDidChanged(callback: {[weak self] str1, str2, model in
            guard let self = self,
                  let list = model.getList(),
                  let songs = NSArray.yy_modelArray(with: VLRoomSelSongModel.self, json: list) as? [VLRoomSelSongModel] else {
                return
            }
            self.delegate?.onChosenSongListDidChanged(songs: songs)
        })
        
        collection.subscribeWillAdd {[weak self] publisherId, dataCmd, newItem in
            guard let self = self, let dataCmd = AUIMusicCmd(rawValue: dataCmd ?? "") else {
                return AUICommonError.unknown.toNSError()
            }
            
            guard let seatValues = self.getSeatCollection(with: roomNo)?.getLocalMetaData()?.getMap()?.values else {
                return AUICommonError.unknown.toNSError()
            }
            
            let userId = getUserId(newItem) ?? ""
            switch dataCmd {
            case .chooseSongCmd:
                //check whether the song owner entered the seat
                guard seatValues.contains(where: { getUserId($0) == userId }) else {
                    return KTVCommonError.userNoEnterSeat.toNSError()
                }
                return nil
            default:
                break
            }
            
            return KTVCommonError.unknown.toNSError()
        }
        
        collection.subscribeWillMerge { [weak self] publisherId, dataCmd, updateMap, currentMap in
            guard let self = self, let dataCmd = AUIMusicCmd(rawValue: dataCmd ?? "") else {
                return AUICommonError.unknown.toNSError()
            }
            
            guard let seatValues = self.getSeatCollection(with: roomNo)?.getLocalMetaData()?.getMap()?.values else {
                return AUICommonError.unknown.toNSError()
            }
            
            let userId = getUserId(updateMap) ?? ""
            switch dataCmd {
            case .pingSongCmd:
                //only room owner or song owner can ping
                let isRoomOwner = AUIRoomContext.shared.isRoomOwner(channelName: roomNo, userId: publisherId)
                guard isRoomOwner else {
                    return KTVCommonError.noPermission.toNSError()
                }
                return nil
            case .updatePlayStatusCmd:
                //only song owner can update status
                guard seatValues.contains(where: { getUserId($0) == userId }) else {
                    return KTVCommonError.noPermission.toNSError()
                }
                return nil
            default:
                break
            }
            
            return KTVCommonError.unknown.toNSError()
        }
        
        collection.subscribeWillRemove { [weak self] publisherId, dataCmd, item in
            guard let self = self else {return nil}
            guard let dataCmd = AUIMusicCmd(rawValue: dataCmd ?? "") else {
                return AUICommonError.unknown.toNSError()
            }
            
            guard let seatValues = self.getSeatCollection(with: roomNo)?.getLocalMetaData()?.getMap()?.values else {
                return AUICommonError.unknown.toNSError()
            }
            let userId = getUserId(item) ?? ""
            switch dataCmd {
            case .removeSongCmd:
                //only room owner or song owner can remove song
                let isRoomOwner = AUIRoomContext.shared.isRoomOwner(channelName: roomNo, userId: publisherId)
                guard isRoomOwner || seatValues.contains(where: { getUserId($0) == userId }) else {
                    return KTVCommonError.noPermission.toNSError()
                }
                return nil
            default:
                break
            }
            
            return KTVCommonError.unknown.toNSError()
        }
        
        collection.subscribeAttributesWillSet {[weak self] channelName, key, valueCmd, attr in
            guard let self = self,
                  valueCmd == AUIMusicCmd.pingSongCmd.rawValue,
                  let value = attr.getList() else {
                return attr
            }
            
            let sortList = self._sortChooseSongList(chooseSongList: value)
            return AUIAttributesModel(list: sortList)
        }
    }
    
    private func _subscribeChorus() {
        guard let roomNo = self.roomNo, let collection = getChorusCollection(with: roomNo) else {return}
        
        //get the seat owner
        func getSeatUserId(_ map: Any) -> String? {
            return (((map as? [String: Any])?["owner"]) as? [String: Any])?["userId"] as? String
        }
        collection.subscribeAttributesDidChanged { [weak self] str1, str2, model in
            guard let self = self,
                  let list = model.getList(),
                  let chorusList = NSArray.yy_modelArray(with: KTVChoristerModel.self, json: list) as? [KTVChoristerModel] else {
                return
            }
                
            var unChangesOldList = self.choristerList
            //TODO: optimize
            let difference = chorusList.difference(from: self.choristerList)
            for change in difference {
                switch change {
                case let .remove(offset, oldElement, _):
                    unChangesOldList.remove(at: offset)
                    self.delegate?.onChoristerDidLeave(chorister: oldElement)
                case let .insert(_, newElement, _):
                    self.delegate?.onChoristerDidEnter(chorister: newElement)
                }
            }
            
            self.choristerList = choristerList
        }
        
        collection.subscribeWillAdd {[weak self] publisherId, dataCmd, newItem in
            guard let self = self, let dataCmd = AUIChorusCMd(rawValue: dataCmd ?? "") else {
                return AUICommonError.unknown.toNSError()
            }
            
            guard let seatValues = self.getSeatCollection(with: roomNo)?.getLocalMetaData()?.getMap()?.values else {
                return AUICommonError.unknown.toNSError()
            }
            
            let userId = getSeatUserId(newItem) ?? ""
            switch dataCmd {
            case .joinCmd:
                //check whether the song owner entered the seat
                guard seatValues.contains(where: { getSeatUserId($0) == userId }) else {
                    return KTVCommonError.userNoEnterSeat.toNSError()
                }
                return nil
            default:
                break
            }
            
            return KTVCommonError.unknown.toNSError()
        }
        
        collection.subscribeWillRemove { [weak self] publisherId, dataCmd, item in
            guard let self = self, let dataCmd = AUIChorusCMd(rawValue: dataCmd ?? "") else {
                return AUICommonError.unknown.toNSError()
            }
            
            guard let seatValues = self.getSeatCollection(with: roomNo)?.getLocalMetaData()?.getMap()?.values else {
                return AUICommonError.unknown.toNSError()
            }
            let userId = getSeatUserId(item) ?? ""
            switch dataCmd {
            case .leaveCmd:
                //only room owner or song owner can remove song
                let isRoomOwner = AUIRoomContext.shared.isRoomOwner(channelName: roomNo, userId: publisherId)
                guard isRoomOwner || seatValues.contains(where: { getSeatUserId($0) == userId }) else {
                    return KTVCommonError.noPermission.toNSError()
                }
                return nil
            default:
                break
            }
            
            return KTVCommonError.unknown.toNSError()
        }
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
            self?.roomNo = roomId
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
        guard let roomNo = roomNo else {
            completion(NSError(domain: "enterSeat fail", code: -1))
            return
        }
        let seatIdx = Int(seatIndex?.intValue ?? -1)
        let seatInfo = _createCurrentUserSeat(seatIndex: seatIdx)
        
        agoraPrint("enterSeat \(seatIdx)")
        var params = mapConvert(model: seatInfo)
        params["seatIndex"] = nil
        let collection = getSeatCollection(with: roomNo)
        collection?.mergeMetaData(valueCmd: AUIMicSeatCmd.enterSeatCmd.rawValue,
                                  value: ["\(seatIdx)": params],
                                  callback: completion)
    }
    
    func leaveSeat(completion: @escaping (Error?) -> Void) {
        guard let roomNo = roomNo,
              let seatInfo = seatMap.values.filter({ $0.owner?.userId == self.currentUserId() }).first else {
            completion(NSError(domain: "leaveSeat fail", code: -1))
            return
        }
        
        agoraPrint("leaveSeat [\(seatInfo.owner?.userId ?? "")]")
        let collection = getSeatCollection(with: roomNo)
        let model = VLRoomSeatModel()
        model.seatIndex = seatInfo.seatIndex
        var params = mapConvert(model: model)
        params["seatIndex"] = nil
        collection?.mergeMetaData(valueCmd: AUIMicSeatCmd.leaveSeatCmd.rawValue,
                                  value: ["\(seatInfo.seatIndex)": params],
                                  callback: completion)
    }
    
    func kickSeat(userId: String, completion: @escaping (NSError?) -> ()) {
        guard let roomNo = roomNo,
              let seatInfo = seatMap.values.filter({ $0.owner?.userId == userId }).first else {
            completion(NSError(domain: "kickSeat fail", code: -1))
            return
        }
        
        agoraPrint("kickSeat [\(seatInfo.owner?.userId ?? "")]")
        let collection = getSeatCollection(with: roomNo)
        let model = VLRoomSeatModel()
        model.seatIndex = seatInfo.seatIndex
        var params = mapConvert(model: model)
        params["seatIndex"] = nil
        collection?.mergeMetaData(valueCmd: AUIMicSeatCmd.kickSeatCmd.rawValue,
                                  value: ["\(seatInfo.seatIndex)": params],
                                  callback: completion)
    }
    
    func updateSeatAudioMuteStatus(muted: Bool, completion: @escaping (Error?) -> Void) {
        guard let seatInfo = self.seatMap
            .filter({ $0.value.owner?.userId == VLUserCenter.user.id })
            .first?.value else {
            agoraAssert("mute seat not found")
            completion(nil)
            return
        }
        
        let params = ["\(seatInfo.seatIndex)": ["isAudioMuted": muted ? 1 : 0]]
        let collection = getSeatCollection(with: roomNo ?? "")
        collection?.mergeMetaData(valueCmd: AUIMicSeatCmd.muteAudioCmd.rawValue, value: params, callback: completion)
    }
    
    func updateSeatVideoMuteStatus(muted: Bool, completion: @escaping (Error?) -> Void) {
        guard let seatInfo = self.seatMap
            .filter({ $0.value.owner?.userId == VLUserCenter.user.id })
            .first?.value else {
            agoraAssert("open video seat not found")
            completion(nil)
            return
        }
        
        let params = ["\(seatInfo.seatIndex)": ["isVideoMuted": muted ? 1 : 0]]
        let collection = getSeatCollection(with: roomNo ?? "")
        collection?.mergeMetaData(valueCmd: AUIMicSeatCmd.muteVideoCmd.rawValue, value: params, callback: completion)
    }
}

// only for music
extension KTVRTMManagerServiceImpl {
    func removeSong(songCode: String, completion: @escaping (Error?) -> Void) {
        guard let channelName = roomNo else {
            completion(NSError(domain: "removeSong fail", code: -1))
            return
        }
        agoraPrint("imp song delete... songCode[\(songCode)]")
        let collection = getSongCollection(with: channelName)
        collection?.removeMetaData(valueCmd: AUIMusicCmd.removeSongCmd.rawValue,
                                   filter: [["songNo": songCode]], callback: { err in
            completion(err)
        })
    }
    
    func getChoosedSongsList(completion: @escaping (Error?, [VLRoomSelSongModel]?) -> Void) {
        _getChooseSongInfo(finished: completion)
    }
    
    func markSongDidPlay(songCode: String, completion: @escaping (Error?) -> Void) {
        guard let roomNo = roomNo else {
            completion(NSError(domain: "markSongDidPlay fail", code: -1))
            return
        }
        let collection = getSongCollection(with: roomNo)
        collection?.mergeMetaData(valueCmd: nil,
                                  value: ["status": VLSongPlayStatus.playing.rawValue],
                                  filter: [["songNo": songCode]],
                                  callback: completion)
    }
    
    func chooseSong(inputModel: KTVChooseSongInputModel, completion: @escaping (Error?) -> Void) {
        guard let roomNo = roomNo else {
            completion(NSError(domain: "chooseSong fail", code: -1))
            return
        }
        let songInfo = VLRoomSelSongModel()
        songInfo.songName = inputModel.songName
        songInfo.songNo = inputModel.songNo
        songInfo.imageUrl = inputModel.imageUrl
        songInfo.singer = inputModel.singer
        songInfo.owner = AUIUserThumbnailInfo.createUserInfo()
        songInfo.createAt = getCurrentTs(channelName: roomNo)

        agoraPrint("imp song add...[\(roomNo)]")
        let params = mapConvert(model: songInfo)
        let collection = getSongCollection(with: roomNo)
        //add a filter to ensure that objects with the same songNo are not repeatedly inserted
        collection?.addMetaData(valueCmd: AUIMusicCmd.chooseSongCmd.rawValue,
                                value: params,
                                filter: [["songNo": songInfo.songNo ?? ""]],
                                callback: completion)
    }
    
    func pinSong(songCode: String, completion: @escaping (Error?) -> Void) {
        guard let roomNo = roomNo else {
            completion(NSError(domain: "pinSong fail", code: -1))
            return
        }
        let collection = getSongCollection(with: roomNo)
        collection?.mergeMetaData(valueCmd: nil, 
                                  value: ["pinAt": getCurrentTs(channelName: roomNo)],
                                  filter: [["songNo": songCode]],
                                  callback: completion)
    }
}

//for chorus
extension KTVRTMManagerServiceImpl {
    func joinChorus(songCode: String, completion: @escaping (Error?) -> Void) {
        guard let roomNo = roomNo else {
            completion(NSError(domain: "joinChorus fail", code: -1))
            return
        }
        let model = KTVChoristerModel()
        model.chorusSongNo = songCode
        model.userId = currentUserId()
        
        let value = mapConvert(model: model)
        let collection = getChorusCollection(with: roomNo)
        collection?.addMetaData(valueCmd: AUIChorusCMd.joinCmd.rawValue,
                                value: value,
                                filter: [["chorusSongNo": songCode, "userId": currentUserId()]],
                                callback: completion)
    }
    
    func leaveChorus(songCode: String, completion: @escaping (Error?) -> Void) {
        guard let roomNo = roomNo else {
            completion(NSError(domain: "leaveChorus fail", code: -1))
            return
        }
        let collection = getChorusCollection(with: roomNo)
        collection?.removeMetaData(valueCmd: AUIChorusCMd.leaveCmd.rawValue,
                                   filter: [["chorusSongNo": songCode, "userId": currentUserId()]],
                                   callback: completion)
    }
    
    func enterSoloMode() {
        _markSoloSongIfNeed()
    }
}

// for subscribe
extension KTVRTMManagerServiceImpl {
    func subscribe(listener: KTVServiceListenerProtocol?) {
        self.delegate = listener
        if self.seatMap.isEmpty == false {
            self.delegate?.onMicSeatSnapshot(seat: self.seatMap)
        }
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
    
    private func getSeatCollection(with roomId: String) -> AUIMapCollection? {
        let collection: AUIMapCollection? = getCurrentScene(with: roomId)?.getCollection(key: SYNC_MANAGER_SEAT_INFO)
        return collection
    }
    
    private func getSongCollection(with roomId: String) -> AUIListCollection? {
        let collection: AUIListCollection? = getCurrentScene(with: roomId)?.getCollection(key: SYNC_MANAGER_CHOOSE_SONG_INFO)
        return collection
    }
    
    private func getChorusCollection(with roomId: String) -> AUIListCollection? {
        let collection: AUIListCollection? = getCurrentScene(with: roomId)?.getCollection(key: SYNC_MANAGER_CHORUS_INFO)
        return collection
    }
}

extension KTVRTMManagerServiceImpl: AUISceneRespDelegate {
    func onWillInitSceneMetadata(channelName: String) -> [String : Any]? {
        var map: [String: Any] = [:]
        map["0"] = self._createCurrentUserSeat(seatIndex: 0).yy_modelToJSONObject()
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
        guard AUIRoomContext.shared.getArbiter(channelName: roomId)?.isArbiter() == true else {
            return
        }
        
        kickSeat(userId: userInfo.userId) { _ in
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
    private func _createCurrentUserSeat(seatIndex: Int) -> VLRoomSeatModel {
        let seatInfo = VLRoomSeatModel()
        seatInfo.seatIndex = seatIndex
        seatInfo.owner = AUIUserThumbnailInfo.createUserInfo()
        
        return seatInfo
    }
}

//chorus
extension KTVRTMManagerServiceImpl {
    private func _removeChorus(userId: String, completion: @escaping (Error?) -> Void) {
        guard let roomNo = roomNo else {
            completion(NSError(domain: "_removeChorus fail", code: -1))
            return
        }
        let collection = getChorusCollection(with: roomNo)
        collection?.removeMetaData(valueCmd: AUIChorusCMd.leaveCmd.rawValue,
                                   filter: [["userId": currentUserId()]],
                                   callback: completion)
    }
}

// for song
extension KTVRTMManagerServiceImpl {
    private func _sortChooseSongList(chooseSongList: [[String: Any]]) -> [[String: Any]] {
        let songList = chooseSongList.sorted(by: { model1, model2 in
            //歌曲播放中优先（只会有一个，多个目前没有，如果有需要修改排序策略）
            if model1["status"] as? Int == VLSongPlayStatus.playing.rawValue {
                return true
            }
            if model2["status"] as? Int == VLSongPlayStatus.playing.rawValue {
                return false
            }
            
            let pinAt1 = model1["pinAt"] as? Int64 ?? 0
            let pinAt2 = model2["pinAt"] as? Int64 ?? 0
            let createAt1 = model1["createAt"] as? Int64 ?? 0
            let createAt2 = model2["createAt"] as? Int64 ?? 0
            //都没有置顶时间，比较创建时间，创建时间小的在前（即创建早的在前）
            if pinAt1 < 1,  pinAt2 < 1 {
                return createAt1 - createAt2 < 0 ? true : false
            }
            
            //有一个有置顶时间，置顶时间大的在前（即后置顶的在前）
            return pinAt1 - pinAt2 > 0 ? true : false
        })
        
        return songList
    }
    
    private func _getChooseSongInfo(finished: @escaping (Error?, [VLRoomSelSongModel]?) -> Void) {
        guard let channelName = roomNo else {
//            agoraAssert("channelName = nil")
            return
        }
        agoraPrint("imp song get...")
        let collection = getSongCollection(with: channelName)
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
    
    private func _removeChooseSong(userId: String, completion: @escaping (Error?) -> Void) {
        guard let channelName = roomNo else {
            agoraAssert("channelName = nil")
            completion(nil)
            return
        }
        agoraPrint("imp song delete... userId[\(userId)]")
        let collection = getSongCollection(with: channelName)
        collection?.removeMetaData(valueCmd: AUIMusicCmd.removeSongCmd.rawValue,
                                   filter: [["owner": ["userId": userId]]],
                                   callback: completion)
    }

    private func _markSoloSongIfNeed() {
        guard let topSong = songList.first,
            //  topSong.isChorus == true, // current is chorus
              topSong.owner?.userId == VLUserCenter.user.id,
              let roomNo = roomNo,
              let songNo = topSong.songNo else {
            KTVLog.warning(text: "_markSoloSongIfNeed break:  \(songList.first?.status.rawValue ?? 0) \(songList.first?.owner?.userId ?? "")/\(VLUserCenter.user.id)")
            return
        }
        
        let collection = getSongCollection(with: roomNo)
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
    return params
}

