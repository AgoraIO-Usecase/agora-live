//
//  AUIKitModel.swift
//  AUIKit
//
//  Created by wushengtao on 2023/2/20.
//

import Foundation

/// Room list display data
@objc(SyncRoomInfo)
@objcMembers open class AUIRoomInfo: NSObject {
    public var roomName: String = ""    //The name of the room
    public var roomId: String = ""            //Room id
    public var owner: AUIUserThumbnailInfo?   //Homeowner's information
    public var customPayload: [String: Any] = [:]   //Extended information
    public var createTime: Int64 = 0
}

///User brief information is used to pass simple data to each model.
@objc(SyncUserThumbnailInfo)
@objcMembers open class AUIUserThumbnailInfo: NSObject {
    public var userId: String = ""      //User Id
    public var userName: String = ""    //User name
    public var userAvatar: String = ""  //User's avatar
}

let kUserMuteAudioInitStatus = false
let kUserMuteVideoInitStatus = true

//User information
@objc(SyncUserInfo)
@objcMembers open class AUIUserInfo: AUIUserThumbnailInfo {
    public var muteAudio: Bool = kUserMuteAudioInitStatus  //Whether it is muted
    public var muteVideo: Bool = kUserMuteVideoInitStatus   //Whether to turn off the video status
    public var customPayload: String?   //Extended information
    
    public convenience init(thumbUser: AUIUserThumbnailInfo) {
        self.init()
        self.userId = thumbUser.userId
        self.userName = thumbUser.userName
        self.userAvatar = thumbUser.userAvatar
    }
}
