//
//  AUISceneCondition.swift
//  RTMSyncManager
//
//  Created by wushengtao on 2024/5/8.
//

import Foundation

private let kConditionKey = "AUICondition"

/// Judge the conditions for joining the room successfully
class AUISceneEnterCondition: NSObject {
    private var channelName: String
    private var arbiter: AUIArbiter
    var enterCompletion: (()->())?
    var lockOwnerRetrived: Bool = false {
        didSet {
            aui_info("set lockOwnerRetrived = \(lockOwnerRetrived)", tag: kConditionKey)
            checkRoomValid()
        }
    }
    var lockOwnerAcquireSuccess: Bool = false {
        didSet {
            aui_info("set lockOwnerAcquireSuccess = \(lockOwnerAcquireSuccess)", tag: kConditionKey)
            checkRoomValid()
        }
    }
    var subscribeSuccess: Bool = false {
        didSet {
            aui_info("set subscribeSuccess = \(subscribeSuccess)", tag: kConditionKey)
            checkRoomValid()
        }
    }
    
    var ownerId: String = "" {
        didSet {
            aui_info("set ownerId = \(ownerId)", tag: kConditionKey)
            AUIRoomContext.shared.roomOwnerMap[channelName] = ownerId
            checkRoomValid()
        }
    }
    
    
    init(channelName: String, arbiter: AUIArbiter) {
        self.channelName = channelName
        self.arbiter = arbiter
        super.init()
    }
    
    /*
     To check the success of joining the room, all the following conditions need to be met:
     1. Subscribe successfully
     1. Get the initialization information (get the ownerId owner uid)
     3. The lock is also successfully obtained (the callback of acquire is successfully received, and the callback can only be successfully written after receiving the callback. If the callback is not received but the lock owner is obtained, even if the lock is yourself, writing metadata will still fail)
     4. Get the lock owner (to modify metadata, you need to send a message to the lock owner)
     */
    private func checkRoomValid() {
        aui_info("checkRoomValid[\(channelName)] subscribeSuccess: \(subscribeSuccess), lockOwnerRetrived: \(lockOwnerRetrived), ownerId: \(ownerId) isArbiter: \(arbiter.isArbiter()), lockOwnerAcquireSuccess: \(lockOwnerAcquireSuccess)", tag: kConditionKey)
        guard subscribeSuccess, lockOwnerRetrived, !ownerId.isEmpty else { return }
        //If it is the lock owner, it is necessary to judge whether it has acquired a successful callback. After the callback, there is a local comparison. If there is no successful callback, setmetadata will fail-12008
        if arbiter.isArbiter(), lockOwnerAcquireSuccess == false {return}
        if let completion = self.enterCompletion {
            completion()
        }
    }
}

//Judgment of room expiration conditions
class AUISceneExpiredCondition: NSObject {
    private var roomExpiration: RoomExpirationPolicy
    private var channelName: String
    private var lastUpdateDate: Date?
    var roomDidExpired: (()->())?
    
    var offlineTimestamp: UInt64 = 0 {
        didSet {
            aui_info("[\(channelName)]did offline: \(offlineTimestamp)", tag: kConditionKey)
        }
    }
    
    var joinCompletion: Bool = false {
        didSet {
            aui_info("[\(channelName)]set joinCompletion \(joinCompletion)", tag: kConditionKey)
            checkRoomExpired()
        }
    }
    
    var createTimestemp: UInt64? {
        didSet {
            aui_info("[\(channelName)]set createTimestemp \(createTimestemp ?? 0)", tag: kConditionKey)
            checkRoomExpired()
        }
    }
    
    var userSnapshotList: [AUIUserInfo]? {
        didSet {
            aui_info("[\(channelName)]set userSnapshotList count = \(userSnapshotList?.count ?? 0)", tag: kConditionKey)
            checkRoomExpired()
        }
    }
    
    //The landlord once left the room (kill the app, non-disconnection)
    var ownerHasLeftRoom: Bool = false {
        didSet {
            aui_info("[\(channelName)]set ownerHasLeftRoom = \(ownerHasLeftRoom)", tag: kConditionKey)
            checkRoomExpired()
        }
    }
    
    var lastUpdateTimestemp: UInt64? {
        didSet {
            self.lastUpdateDate = Date()
            aui_info("[\(channelName)]set lastUpdateTimestemp = \(lastUpdateTimestemp ?? 0)", tag: kConditionKey)
            checkRoomExpired()
        }
    }
    
    required init(channelName: String, roomExpiration: RoomExpirationPolicy) {
        self.channelName = channelName
        self.roomExpiration = roomExpiration
        super.init()
    }
    
    func reconnectNow(timestamp: UInt64) {
        aui_info("[\(channelName)]reconnectNow: curentTs:\(timestamp), offlineTs:\(offlineTimestamp)", tag: kConditionKey)
        guard offlineTimestamp > 0, roomExpiration.ownerReconnectMaxTime > 0 else { return }
        guard timestamp - offlineTimestamp > roomExpiration.ownerReconnectMaxTime else { return }
        offlineTimestamp = 0
        roomDidExpired?()
    }
    
    /*
     Check that the room has expired. If one of them is not satisfied, it means that it has expired. It needs to be checked after the room is added (at present, it is considered that if the enter is not completed, the inspection will not be processed):
     1. To join the homeowner, you need to check whether the homeowner is in the user list (query through who now)
     2. The audience joins to check that the homeowner is not in the user list.
     3. The room time has expired (dynamic configuration, do not write directly for 20min)
     */
    private func checkRoomExpired() {
        aui_info("checkRoomExpired[\(channelName)] joinCompletion: \(joinCompletion), userSnapshotList count: \(userSnapshotList?.count ?? 0), createTimestemp: \(createTimestemp ?? 0)", tag: kConditionKey)
        guard joinCompletion, let userList = userSnapshotList, let createTs = createTimestemp else { return }
        
        if roomExpiration.isAssociatedWithOwnerOffline {
            let isRoomOwner = AUIRoomContext.shared.isRoomOwner(channelName: channelName)
            if isRoomOwner {
                //step 1
                if ownerHasLeftRoom {
                    aui_info("checkRoomExpired: room owner has left", tag: kConditionKey)
                    roomDidExpired?()
                }
            } else {
                //step 2
                guard let _ = userList.filter({ AUIRoomContext.shared.isRoomOwner(channelName: channelName, userId: $0.userId)}).first else {
                    //room owner not found, clean room
                    aui_info("checkRoomExpired: room owner leave", tag: kConditionKey)
                    roomDidExpired?()
                    return
                }
            }
        }
        
        //TODO: At present, when the inspection is only enter the room, if the landlord has been in the room, it will not expire. Do you need an internal inspection or let the upper floor deal with it?
        //step 3
        if roomExpiration.expirationTime > 0, let updateTs = lastUpdateTimestemp {
            if Int64(updateTs) - Int64(createTs) > roomExpiration.expirationTime {
                aui_info("checkRoomExpired: room is expired: \(updateTs) - \(createTs) > \(roomExpiration.expirationTime)", tag: kConditionKey)
                roomDidExpired?()
                return
            }
        }
    }
    
    /*
      createTimestemp --[createDuration]--> lastUpdateTimestemp (lastUpdateTimestemp - createTimestemp = createDuration)
                                              (lastUpdateDate) --[deltaDuration]--> nowDate (nowDate - lastUpdateDate = deltaDuration)
     */
    func roomUsageDuration() -> UInt64? {
        guard let currentTs = roomCurrentTs(), let createTs = createTimestemp else {return nil}
        let duration = currentTs - createTs
        
        return duration
    }
    
    func roomCurrentTs() -> UInt64? {
        guard let updateTs = lastUpdateTimestemp, let date = lastUpdateDate else {return nil}
        let deltaDuration = UInt64(-date.timeIntervalSinceNow * 1000)

        return updateTs + deltaDuration
    }
}
