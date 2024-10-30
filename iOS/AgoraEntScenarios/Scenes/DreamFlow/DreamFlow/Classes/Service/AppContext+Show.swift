//
//  AppContext+Show.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/7.
//

import Foundation
import SwiftyBeaver
import AgoraCommon

let kShowLogBaseContext = "AgoraKit"

private let kShowRoomListKey = "kShowRoomListKey"
private let kRtcTokenMapKey = "kRtcTokenMapKey"
private let kRtcToken = "kRtcToken"
private let kRtcTokenDate = "kRtcTokenDate"
private let kDebugModeKey = "kDebugModeKey"

public class ShowLogger: NSObject {
    
    public static let kLogKey = "Dream"
    
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
    static private var _showServiceImp: ShowSyncManagerServiceImp?
    static func showServiceImp() -> ShowServiceProtocol? {
        if let service = _showServiceImp {
            return service
        }
        
        _showServiceImp = ShowSyncManagerServiceImp(appId: AppContext.shared.appId,
                                                    host: AppContext.shared.rtmHost)
        
        return _showServiceImp
    }
    
    static func unloadShowServiceImp() {
        _showServiceImp = nil
    }
    
    public var rtcToken: String? {
        set {
            self.extDic[kRtcToken] = newValue
            self.tokenDate = Date()
        }
        get {
            return self.extDic[kRtcToken] as? String
        }
    }
    
    public var tokenDate: Date? {
        set {
            self.extDic[kRtcTokenDate] = newValue
        }
        get {
            return self.extDic[kRtcTokenDate] as? Date
        }
    }
}

