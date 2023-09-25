//
//  Config.swift
//  BreakoutRoom
//
//  Created by zhaoyongqiang on 2021/11/4.
//

import Foundation

let SYNC_MANAGER_PARAM_KEY_ID = "defaultChannel"
/// Subroom name
let SYNC_COLLECTION_SUB_ROOM = "SubRoom"

let SYNC_MANAGER_PARAM_KEY_APPID = "appId"
/// GIFT
let SYNC_MANAGER_GIFT_INFO = "giftInfo"
/// PK Game Info
let SYNC_MANAGER_GAME_APPLY_INFO = "gameApplyInfo"
/// Audience game information
let SYNC_MANAGER_GAME_INFO = "gameInfo"
/// pk info
let SYNC_MANAGER_PK_INFO = "pkInfo"
/// User Info
let SYNC_MANAGER_AGORA_VOICE_USERS = "agoraVoiceUsers"
/// Club User Info
let SYNC_MANAGER_AGORA_CLUB_USERS = "agoraClubUsers"
/// Message Info
let SYNC_SCENE_ROOM_MESSAGE_INFO = "messageInfo"
/// List of users in the room
let SYNC_SCENE_ROOM_USER_COLLECTION = "userCollection"
/// Commodity information
let SYNC_SCENE_SHOPPING_INFO = "shoppingInfo"

let APP_SCENARIO: Int = 100
let SERVICE_TYPE: Int = 12

let chatViewWidth = ScreenWidth * (287 / 375.0)

enum UserInfo {
    static var userId: String {
        return VLUserCenter.user.id

//        let id = UserDefaults.standard.integer(forKey: "UserId")
//        if id > 0 {
//            return UInt(id)
//        }
//        let user = UInt(arc4random_uniform(8999999) + 1000000)
//        UserDefaults.standard.set(user, forKey: "UserId")
//        UserDefaults.standard.synchronize()
//        return user
    }
//    static var uid: String {
//        "\(userId)"
//    }
}


enum AgoraScene: String {
    case KTV
    case ChatRoom
    case LiveShow
    case SpatialAudioChatRoom
}
