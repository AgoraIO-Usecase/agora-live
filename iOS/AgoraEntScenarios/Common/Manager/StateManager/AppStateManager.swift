//
//  AppStateManager.swift
//  AFNetworking
//
//  Created by wushengtao on 2024/4/30.
//

import Foundation
import Network

public class AppStateManager {
    // Front background switching notification
    public var appStateChangeHandler: ((Bool) -> Void)?
    
    // Lock screen unlock notification
    public var screenLockHandler: ((Bool) -> Void)?
    
    // Network on-off status callback
    public var networkStatusChangeHandler: ((Bool) -> Void)?
    
    private let monitor = NWPathMonitor()
    
    public init() {
        // Notification of change of registration application status
        NotificationCenter.default.addObserver(self, selector: #selector(appStateChanged), name: UIApplication.didEnterBackgroundNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(appStateChanged), name: UIApplication.didBecomeActiveNotification, object: nil)
        
        // Registration lock screen unlock notification
        NotificationCenter.default.addObserver(self, selector: #selector(screenLocked), name: UIApplication.willResignActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(screenUnlocked), name: UIApplication.didBecomeActiveNotification, object: nil)
        
        // Monitor the change of network status
        monitor.pathUpdateHandler = { [weak self] path in
            let isNetworkAvailable = path.status == .satisfied
            agoraEnt_info("networkChange isNetworkAvailable: \(isNetworkAvailable)", tag: "AppStateManager")
            DispatchQueue.main.async {
                self?.networkStatusChangeHandler?(isNetworkAvailable)
            }
        }
        let queue = DispatchQueue(label: "NetworkMonitor")
        monitor.start(queue: queue)
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
        monitor.cancel()
    }
    
    // Application status change notification processing
    @objc private func appStateChanged() {
        let isInBackground = UIApplication.shared.applicationState == .background
        agoraEnt_info("appStateChanged isInBackground: \(isInBackground)", tag: "AppStateManager")
        appStateChangeHandler?(isInBackground)
    }
    
    // Lock screen notification processing
    @objc private func screenLocked() {
        agoraEnt_info("screenLocked", tag: "AppStateManager")
        screenLockHandler?(true)
    }
    
    // Unlock notification processing
    @objc private func screenUnlocked() {
        agoraEnt_info("screenUnlocked", tag: "AppStateManager")
        screenLockHandler?(false)
    }
}
