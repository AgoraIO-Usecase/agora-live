//
//  AppContext+Show.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/7.
//

import Foundation
import AgoraCommon

let kCommerceLogBaseContext = "AgoraKit"

let commerceLogger = AgoraEntLog.createLog(config: AgoraEntLogConfig(sceneName: "Commerce"))

private let kCommerceRoomListKey = "kCommerceRoomListKey"
private let kCommerceUserListKey = "kCommerceUserListKey"
private let kRtcTokenMapKey = "kRtcTokenMapKey"
private let kRtcToken = "kRtcToken"
private let kRtmToken = "kRtmToken"
private let kDebugModeKey = "kDebugModeKey"

public class CommerceLogger: NSObject {
    
    public static let kLogKey = "Commerce"
    
    public static func info(_ text: String, context: String? = nil) {
        agoraDoMainThreadTask {
            AgoraEntLog.getSceneLogger(with: kLogKey).info(text, context: context)
        }
    }
    
    public static func warning(_ text: String, context: String? = nil) {
        warn(text, context: context)
    }

    public static func warn(_ text: String, context: String? = nil) {
        agoraDoMainThreadTask {
            AgoraEntLog.getSceneLogger(with: kLogKey).warning(text, context: context)
        }
    }

    public static func error(_ text: String, context: String? = nil) {
        agoraDoMainThreadTask {
            AgoraEntLog.getSceneLogger(with: kLogKey).error(text, context: context)
        }
    }
}

extension AppContext {
    static private var _commerceServiceImpMap: [String: CommerceSyncManagerServiceImp] = [String: CommerceSyncManagerServiceImp]()
    
    static private var _commerceExpiredImp: [String] = [String]()
    
    static func commerceServiceImp(_ roomId: String) -> CommerceServiceProtocol? {
        if _commerceExpiredImp.contains(roomId) {
            return nil
        }
        let commerceServiceImp = _commerceServiceImpMap[roomId]
        guard let commerceServiceImp = commerceServiceImp else {
            let imp = CommerceSyncManagerServiceImp()
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
    
    public var commerceRtcToken: String? {
        set {
            self.extDic[kRtcToken] = newValue
        }
        get {
            return self.extDic[kRtcToken] as? String
        }
    }
    
    public var commerceRtmToken: String? {
        set {
            self.extDic[kRtmToken] = newValue
        }
        get {
            return self.extDic[kRtmToken] as? String
        }
    }
}

