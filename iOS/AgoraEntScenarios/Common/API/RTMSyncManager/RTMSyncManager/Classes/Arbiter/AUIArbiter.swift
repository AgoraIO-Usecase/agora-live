//
//  AUIArbiter.swift
//  AUIKitCore
//
//  Created by wushengtao on 2023/11/2.
//

import AgoraRtmKit

@objc public class AUIArbiter: NSObject {
    private var channelName: String!
    private var rtmManager: AUIRtmManager!
    private var currentUserInfo: AUIUserThumbnailInfo!
    private(set) var lockOwnerId: String = "" {
        didSet {
            notifyArbiterDidChange()
        }
    }
    private var arbiterDelegates = NSHashTable<AUIArbiterDelegate>()
    
    deinit {
        aui_info("deinit AUIArbiter", tag: "AUIArbiter")
        rtmManager.unsubscribeLock(channelName: channelName, lockName: kRTM_Referee_LockName, delegate: self)
    }
    
    public init(channelName: String, rtmManager: AUIRtmManager, userInfo: AUIUserThumbnailInfo) {
        self.rtmManager = rtmManager
        self.channelName = channelName
        self.currentUserInfo = userInfo
        super.init()
        rtmManager.subscribeLock(channelName: channelName, lockName: kRTM_Referee_LockName, delegate: self)
        aui_info("init AUIArbiter", tag: "AUIArbiter")
    }
    
    public func subscribeEvent(delegate: AUIArbiterDelegate) {
        if arbiterDelegates.contains(delegate) { return }
        
        arbiterDelegates.add(delegate)
    }
    
    public func unSubscribeEvent(delegate: AUIArbiterDelegate) {
        arbiterDelegates.remove(delegate)
    }
    
    /// Create a lock
    public func create(completion: ((NSError?)-> ())? = nil) {
        rtmManager.setLock(channelName: channelName, lockName: kRTM_Referee_LockName) {[weak self] err in
            guard let err = err, err.code != AgoraRtmErrorCode.lockAlreadyExist.rawValue else {
                completion?(nil)
                return
            }
            self?.notifyError(error: err)
            completion?(err)
        }
    }
    
    /// Destroy the lock
    public func destroy(completion: ((NSError?)-> ())? = nil) {
        rtmManager.removeLock(channelName: channelName, lockName: kRTM_Referee_LockName) {[weak self] err in
            self?.notifyError(error: err)
            completion?(err)
        }
    }
    
    /// Get the lock
    public func acquire(completion: ((NSError?)-> ())? = nil) {
        rtmManager.acquireLock(channelName: channelName, lockName: kRTM_Referee_LockName) {[weak self] err in
            self?.notifyError(error: err)
            completion?(err)
        }
    }
    
    /// Release the lock
    public func release(completion: ((NSError?)-> ())? = nil) {
        rtmManager.releaseLock(channelName: channelName, lockName: kRTM_Referee_LockName) {[weak self] err in
            self?.notifyError(error: err)
            completion?(err)
        }
    }
    
    public func isArbiter() -> Bool {
        return lockOwnerId == currentUserInfo.userId
    }
}

//MARK: private
extension AUIArbiter {
    private func notifyError(error: NSError?) {
        guard let error = error else { return }
        aui_info("notifyError: \(error.localizedDescription)", tag: "AUIArbiter")
        arbiterDelegates.allObjects.forEach { delegate in
            delegate.onError(channelName: channelName, error: error)
        }
    }
    
    private func notifyArbiterDidChange() {
        arbiterDelegates.allObjects.forEach { delegate in
            delegate.onArbiterDidChange(channelName: channelName, arbiterId: self.lockOwnerId)
        }
    }
}

//MARK: AUIRtmLockProxyDelegate
extension AUIArbiter: AUIRtmLockProxyDelegate {
    public func onReceiveLockDetail(channelName: String, lockDetail: AgoraRtmLockDetail, eventType: AgoraRtmLockEventType) {
        aui_info("onReceiveLockDetail[\(channelName)]: \(lockDetail.owner)/\(currentUserInfo.userId)")
        guard channelName == self.channelName else {return}
        /*
         In the following two cases, metadata needs to be refreshed to the latest version.
         1. If lockOwnerId is your own, you need to refresh before notifying the external lock transfer.
         Two. If lockOwnerId is not yourself, and lockOwnerId was yourself before, it means that you have switched from an arbitrator to a non-arbitrator. You need to notify the outside and refresh it (because the collection does not use remote data if it is the lock owner), maybe you The local data is not up to date.
        */
        let gotLock = lockDetail.owner == currentUserInfo.userId
        let lossLockToOthers = lockOwnerId == currentUserInfo.userId && lockDetail.owner != currentUserInfo.userId
        if gotLock {
            rtmManager.fetchMetaDataSnapshot(channelName: channelName) {[weak self] error in
                guard let self = self else { return }
                //TODO: error handler, retry?
                self.lockOwnerId = lockDetail.owner
            }
        } else if lossLockToOthers {
            self.lockOwnerId = lockDetail.owner
            rtmManager.fetchMetaDataSnapshot(channelName: channelName) {[weak self] error in
                guard let self = self else { return }
                //TODO: error handler, retry?
                
            }
        } else {
            lockOwnerId = lockDetail.owner
        }
    }
    
    public func onReleaseLockDetail(channelName: String, lockDetail: AgoraRtmLockDetail, eventType: AgoraRtmLockEventType) {
        aui_info("onReleaseLockDetail[\(channelName)]: \(lockDetail.owner)")
        guard channelName == self.channelName else {return}
        rtmManager.acquireLock(channelName: channelName, lockName: kRTM_Referee_LockName) { err in
        }
        //Expired dates may be received after obtaining the lock, resulting in the cleaning of the correct lock owner, so it will only be handled when the lock owner is himself.
        if eventType == .lockExpired, lockOwnerId == currentUserInfo.userId {
            self.lockOwnerId = ""
        }
        
    }
}
