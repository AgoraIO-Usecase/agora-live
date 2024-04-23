//
//  AppContext+Show.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/7.
//

import Foundation

let kCommerceLogBaseContext = "AgoraKit"
let commerceLogger = AgoraEntLog.createLog(config: AgoraEntLogConfig(sceneName: "Commerce"))

private let kCommerceRoomListKey = "kCommerceRoomListKey"
private let kCommerceUserListKey = "kCommerceUserListKey"
private let kRtcTokenMapKey = "kRtcTokenMapKey"
private let kRtcToken = "kRtcToken"
private let kDebugModeKey = "kDebugModeKey"

extension AppContext {
    static private var _commerceServiceImpMap: [String: CommerceSyncManagerServiceImp] = [String: CommerceSyncManagerServiceImp]()
    
    static private var _commerceExpiredImp: [String] = [String]()
    
    static func commerceServiceImp(_ roomId: String) -> CommerceServiceProtocol? {
        if _commerceExpiredImp.contains(roomId) {
            return nil
        }
        let commerceServiceImp = _commerceServiceImpMap[roomId]
        guard let commerceServiceImp = commerceServiceImp else {
            let imp = roomId.count == 6 ? CommerceSyncManagerServiceImp() : CommerceRobotSyncManagerServiceImp()
            _commerceServiceImpMap[roomId] = imp
            return imp
        }
        return commerceServiceImp
    }
    
    static func expireCommerceImp(_ roomId: String) {
        if !_commerceExpiredImp.contains(roomId) {
            _commerceExpiredImp.append(roomId)
        }
    }
    
    static func unloadCommerceServiceImp(_ roomId: String) {
        _commerceServiceImpMap.removeValue(forKey: roomId)
    }
    
    static func unloadCommerceServiceImp() {
        _commerceServiceImpMap = [String: CommerceSyncManagerServiceImp]()
        SyncUtilsWrapper.cleanScene()
        _commerceExpiredImp.removeAll()
    }
    
    public var commerceRoomList: [CommerceRoomListModel]? {
        set {
            self.extDic[kCommerceRoomListKey] = newValue
        }
        get {
            return self.extDic[kCommerceRoomListKey] as? [CommerceRoomListModel]
        }
    }
    
    public var commerceUserList: [CommerceUser]? {
        set {
            extDic[kCommerceUserListKey] = newValue
        }
        get {
            return extDic[kCommerceUserListKey] as? [CommerceUser]
        }
    }
    
    public var commerceRtcToken: String? {
        set {
            self.extDic[kRtcToken] = newValue
        }
        get {
            return self.extDic[kRtcToken] as? String
        }
    }
}

