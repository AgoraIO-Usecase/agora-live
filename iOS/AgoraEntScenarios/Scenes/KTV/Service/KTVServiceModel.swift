//
//  KTVServiceModel.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/7/13.
//

import Foundation
class KTVCreateRoomInfo: NSObject {
    @objc var belCanto: String?
    @objc var icon: String = ""
    @objc var isPrivate: NSNumber?
    @objc var name: String = ""
    @objc var password: String?
    @objc var soundEffect: String?
    //@objc var userNo: String?
}

class KTVChangeMVCoverInputModel: NSObject {
    @objc var mvIndex: UInt = 0
}

//class KTVRemoveSongInputModel: NSObject {
//    @objc var songNo: String?
//    @objc var objectId: String?
//}

//class KTVJoinChorusInputModel: NSObject {
//    @objc var isChorus: Bool = false
//    @objc var songNo: String?
//}

class KTVChooseSongInputModel: NSObject {
    @objc var isChorus: Bool = false
    @objc var songName: String?
    @objc var songNo: String?
    //@objc var songUrl: String?
    @objc var singer: String?
    @objc var imageUrl: String?
}

class KTVMakeSongTopInputModel: NSObject {
    @objc var songNo: String?
    @objc var sort: NSNumber?
    @objc var objectId: String?
}
