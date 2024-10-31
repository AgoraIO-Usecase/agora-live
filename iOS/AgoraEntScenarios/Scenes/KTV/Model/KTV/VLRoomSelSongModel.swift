//
//  VLRoomSelSongModel.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/7/13.
//

import Foundation
import RTMSyncManager

@objc enum VLSongPlayStatus: Int {
    case idle = 0 //Not played
    case playing = 1 //Now playing
}


class VLRoomSelSongModel: VLBaseModel {
    @objc var songName: String?
    @objc var songNo: String?
    @objc var singer: String?
    @objc var imageUrl: String?
    
    @objc var owner: AUIUserThumbnailInfo?
    
    /// Creation time
    @objc var createAt: UInt64 = 0
    // Pin time
    @objc var pinAt: UInt64 = 0
    
    /// 0 Not started 1. Sung 2. Singing 3. Match is over
    @objc var status: VLSongPlayStatus = .idle
}
