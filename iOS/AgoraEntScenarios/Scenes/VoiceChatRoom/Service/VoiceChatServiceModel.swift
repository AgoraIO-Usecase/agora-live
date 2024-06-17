//
//  VoiceChatServiceModel.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/18.
//

import Foundation

@objcMembers
class VoiceChatBaseInfo: NSObject {
    var objectId: String?    //The unique identifier of the object obtained by SyncManager, used for differential modification and deletion
}

///User Information
@objcMembers
class VoiceChatUsers: VoiceChatBaseInfo {
    var userName: String = "User-\(UserInfo.userId)"                                //User Name
    var avatar: String = .init(format: "portrait%02d", Int.random(in: 1...14))      //User profile picture
    var userId: String = UserInfo.userId                                            //User UID
}

enum VoiceChatRoomType: Int {
    case normal = 0     //Ordinary language chat room
    case spatial = 1    //Space audio
}

///Room information
@objcMembers
class VoiceChatRoom: VoiceChatBaseInfo {

    var roomName: String = ""                                            //Room Name
    var type: VoiceChatRoomType = .normal                                //Room type
    var roomId: String = "\(arc4random_uniform(899999) + 100000)"        //Room ID
    var userId: String = "\(UserInfo.userId)"                            //User ID
    var createAt: Double = 0                                             //Creation time, milliseconds compared to 19700101 time
}

///Wheat position information
@objcMembers
class VoiceChatMicSeat: VoiceChatBaseInfo {
    var seatIndex: Int = 0     //Wheat spot index
    var userNo: String?        //If the user ID of the upper microphone is empty, there is no user
    var isLock: Bool = false   //Is the microphone locked
    var isMute: Bool = false   //Is it silent
}

///Apply for microphone access request
@objcMembers
class VoiceChatMicSeatRequest: VoiceChatBaseInfo {
    var seatIndex: Int = 0        //Seat information
    var userNo: String?           //User ID for applying for online streaming
    var createAt: Double = 0      //Creation time, milliseconds compared to 19700101 time
}


///Invitation to microphone request
@objcMembers
class VoiceChatMicSeatInvitation: VoiceChatBaseInfo {
    var seatIndex: Int = 0        //Seat information
    var userNo: String?           //Invite user ID for online streaming
    var createAt: Double = 0      //Creation time, milliseconds compared to 19700101 time
}



///Chat messages
@objcMembers
class VoiceChatMessage: VoiceChatBaseInfo {
    var content: String?          //Seat information
    var userNo: String?           //Invite user ID for online streaming
    var createAt: Double = 0      //Creation time, milliseconds compared to 19700101 time
}


///Chat messages
@objcMembers
class VoiceChatGift: VoiceChatBaseInfo {
    var giftId: String?           //Corresponding gift ID
    var userNo: String?           //Invite user ID for online streaming
    var createAt: Double = 0      //Creation time, milliseconds compared to 19700101 time
}
