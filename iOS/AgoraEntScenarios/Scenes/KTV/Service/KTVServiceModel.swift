//
//  KTVServiceModel.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/7/13.
//

import Foundation
import RTMSyncManager


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


enum AUIMicSeatCmd: String {
    case leaveSeatCmd = "leaveSeatCmd"
    case enterSeatCmd = "enterSeatCmd"
    case muteAudioCmd = "muteAudioCmd"
    case muteVideoCmd = "muteVideoCmd"
    case kickSeatCmd = "kickSeatCmd"
}

enum AUIMusicCmd: String {
    case chooseSongCmd = "chooseSongCmd"
    case removeSongCmd = "removeSongCmd"
    case pingSongCmd = "pingSongCmd"
    case updatePlayStatusCmd = "updatePlayStatusCmd"
}

enum AUIChorusCMd: String {
    case joinCmd = "joinChorusCmd"
    case leaveCmd = "leaveChorusCmd"
}

@objcMembers
class KTVCreateRoomInfo: NSObject {
    var belCanto: String?
    var icon: String = ""
    var isPrivate: NSNumber?
    var name: String = ""
    var password: String?
    var soundEffect: String?
}

@objcMembers
class KTVChooseSongInputModel: NSObject {
    var songName: String?
    var songNo: String?
    var singer: String?
    var imageUrl: String?
}

extension AUIUserThumbnailInfo {
    @objc static func createUserInfo() -> AUIUserThumbnailInfo {
        let user = VLUserCenter.user
        let owner = AUIUserThumbnailInfo()
        owner.userName = user.name
        owner.userId = user.id
        owner.userAvatar = user.headUrl
        
        return owner
    }
}

/// 合唱者模型
@objcMembers 
open class KTVChoristerModel: NSObject {
    var userId: String = ""
    var chorusSongNo: String?          //合唱者演唱歌曲
 
    open override func isEqual(_ object: Any?) -> Bool {
        if let other = object as? KTVChoristerModel {
            return self.userId == other.userId && self.chorusSongNo == other.chorusSongNo
        }
        return false
    }
    
    open override var hash: Int {
        return userId.hashValue ^ (chorusSongNo?.hashValue ?? 0)
    }
    
    open override var description: String {
        return "\(userId)-\(chorusSongNo ?? "")"
    }
}
