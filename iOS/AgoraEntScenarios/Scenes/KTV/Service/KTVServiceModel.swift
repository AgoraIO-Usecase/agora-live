//
//  KTVServiceModel.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/7/13.
//

import Foundation
import RTMSyncManager

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
}
