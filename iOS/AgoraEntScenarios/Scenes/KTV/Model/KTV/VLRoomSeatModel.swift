//
//  VLRoomSeatModel.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/7/13.
//

import Foundation
import RTMSyncManager

@objcMembers
class VLRoomSeatModel: VLBaseModel {
    /// In which seat?
    @objc var seatIndex: Int = 0
    //User information
    @objc var owner: AUIUserThumbnailInfo = AUIUserThumbnailInfo()
    /// Is the microphone muted?
    @objc var isAudioMuted: Bool = true
    /// Is video prohibited in the microphone?
    @objc var isVideoMuted: Bool = true
    
    
    /// New, judge whether the current song is your own order
    @objc var isSongOwner: Bool = false
    
    override init() {
        super.init()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    @objc func reset(seatInfo: VLRoomSeatModel) {
        self.owner = seatInfo.owner
        self.seatIndex = seatInfo.seatIndex
        self.isAudioMuted = seatInfo.isAudioMuted
        self.isVideoMuted = seatInfo.isVideoMuted
    }
    
    override var description: String {
        return "seatIndex: \(seatIndex), userNo: \(owner.userId), isAudioMuted: \(isAudioMuted)"
    }

}
