//
//  KTVServiceModel.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/7/13.
//

import Foundation
import RTMSyncManager

func toKtvLocalize(_ str: String) -> String {
    return str.toSceneLocalization(with: "KtvResource") as String
}

func createError(code: Int = -1, msg: String) -> NSError {
    return NSError(domain: "AUIKit Error", code: Int(code), userInfo: [ NSLocalizedDescriptionKey : "\(msg)\(code)"])
}

public enum KTVServiceError {
    case createRoomFail(Int)   //Failed to create a room
    case joinRoomFail(Int)     //Failed to join the room
    case enterSeatFail(Int)    //Failed to go to the microphone
    case leaveSeatFail(Int)    //Failed to lower the microphone
    case kickSeatFail(Int)     //Failed to kick the microphone
//    case getSongListFail(Int)  //Failed to get the song
    case chooseSongFail(Int)   //Failed to order songs
    case removeSongFail(Int)   //Failed to delete the song
    case pinSongFail(Int)      //Failed to pin the top song
    case switchSongFail(Int)   //Failed to cut the song
    
    public func toNSError() -> NSError {
        switch self {
        case .createRoomFail(let code):
            return createError(code: code, msg: toKtvLocalize("ktv_create_room_fail_toast"))
        case .joinRoomFail(let code):
            return createError(code: code, msg: toKtvLocalize("ktv_join_room_fail_toast"))
        case .enterSeatFail(let code):
            return createError(code: code, msg: toKtvLocalize("ktv_enter_seat_fail_toast"))
        case .leaveSeatFail(let code):
            return createError(code: code, msg: toKtvLocalize("ktv_leave_seat_fail_toast"))
        case .kickSeatFail(let code):
            return createError(code: code, msg: toKtvLocalize("ktv_kick_seat_fail_toast"))
        case .chooseSongFail(let code):
            return createError(code: code, msg: toKtvLocalize("ktv_choose_song_fail_toast"))
        case .removeSongFail(let code):
            return createError(code: code, msg: toKtvLocalize("ktv_remove_song_fail_toast"))
        case .pinSongFail(let code):
            return createError(code: code, msg: toKtvLocalize("ktv_pin_song_fail_toast"))
        case .switchSongFail(let code):
            return createError(code: code, msg: toKtvLocalize("ktv_switch_song_fail_toast"))
        }
    }
}

public enum KTVCommonError {
    case unknown      //Unknown error
    case micSeatNotIdle   //The wheat seat is not free.
    case micSeatAlreadyEnter   //It has been on the microphone.
    case userNoEnterSeat   //The audience is not on the microphone.
    case chooseSongAlreadyExist   //The song has been selected.
    case chooseSongNotExist   //The song has been selected.
    case choristerAlreadyExist   //The chorus user already exists
    case choristerNotExist    //Chorus users do not exist.
    case noPermission   //No permission
    case chooseSongIsFail   //Failed to select the song
    case currentSongNotFirst  //The song that is expected to be changed to playback is not the first one.
    
    public func toNSError() -> NSError {
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
        case .currentSongNotFirst:
            return createError(msg: "current song not first")
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
    case leaveSeatCmd = "leaveSeatCmd"    //Off the line
    case enterSeatCmd = "enterSeatCmd"    //On the line
    case muteAudioCmd = "muteAudioCmd"    //mute/unmute audio
    case muteVideoCmd = "muteVideoCmd"    //mute/unmute video
    case kickSeatCmd = "kickSeatCmd"      //Kick off a user on the line
}

enum AUIMusicCmd: String {
    case chooseSongCmd = "chooseSongCmd"   //Add a song
    case removeSongCmd = "removeSongCmd"   //Remove a song
    case pinSongCmd = "pinSongCmd"       //Pin a song to the top
    case updatePlayStatusCmd = "updatePlayStatusCmd"   //Update the playing status of the song
    case removedUserSongs = "removedUserSongsCmd"   //Remove all songs from the specified user
}

enum AUIChorusCmd: String {
    case joinCmd = "joinChorusCmd" //Join the chorus
    case leaveCmd = "leaveChorusCmd" //Quit the chorus
    case kickAllCmd = "kickAllOutOfChorusCmd"  //Remove all chorus
    case kickUserCmd = "KickUserOutOfChorusCmd"   //Kick out the designated user out of the chorus list
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

/// Choir model
@objcMembers
open class KTVChoristerModel: NSObject {
    var userId: String = ""
    var chorusSongNo: String?          //The chorer sings a song
 
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
