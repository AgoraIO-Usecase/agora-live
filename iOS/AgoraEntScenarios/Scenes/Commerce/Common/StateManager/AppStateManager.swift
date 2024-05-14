//
//  AppStateManager.swift
//  AFNetworking
//
//  Created by wushengtao on 2024/4/30.
//

import Foundation
import Network
import RealReachability

public class AppStateManager {
    // 前后台切换通知
//    public var appStateChangeHandler: ((Bool) -> Void)?
//    
//    // 锁屏解锁通知
//    public var screenLockHandler: ((Bool) -> Void)?
    
    // 网络通断状态回调
    public var networkStatusChangeHandler: ((Bool) -> Void)?
    
    private let monitor = NWPathMonitor()
    
    public init() {
        // 注册应用状态改变通知
//        NotificationCenter.default.addObserver(self, selector: #selector(appStateChanged), name: UIApplication.didEnterBackgroundNotification, object: nil)
//        NotificationCenter.default.addObserver(self, selector: #selector(appStateChanged), name: UIApplication.didBecomeActiveNotification, object: nil)
//        
//        // 注册锁屏解锁通知
//        NotificationCenter.default.addObserver(self, selector: #selector(screenLocked), name: UIApplication.willResignActiveNotification, object: nil)
//        NotificationCenter.default.addObserver(self, selector: #selector(screenUnlocked), name: UIApplication.didBecomeActiveNotification, object: nil)
//        RealReachability.sharedInstance().startNotifier()
//        NotificationCenter.default.addObserver(self, selector: #selector(networkChanged(_:)), name: NSNotification.Name.realReachabilityChanged, object: nil)

        
        // 监听网络状态改变
        monitor.pathUpdateHandler = { [weak self] path in
            let isNetworkAvailable = path.status == .satisfied
            commercePrintLog("networkChange isNetworkAvailable: \(isNetworkAvailable)", tag: "AppStateManager")
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
    
//    @objc func networkChanged(_ notification: Notification) {
//        let status = RealReachability.sharedInstance().currentReachabilityStatus()
//        
//        switch status {
//        case .RealStatusNotReachable:
//            print("Network not reachable")
//            networkStatusChangeHandler?(false)
//        case .RealStatusViaWiFi:
//            print("Network reachable via WiFi")
//            networkStatusChangeHandler?(true)
//        case .RealStatusViaWWAN:
//            print("Network reachable via cellular")
//            networkStatusChangeHandler?(true)
//        default:
//            break
//        }
//    }
    
    // 应用状态改变通知处理
//    @objc private func appStateChanged() {
//        let isInBackground = UIApplication.shared.applicationState == .background
//        commercePrintLog("appStateChanged isInBackground: \(isInBackground)", tag: "AppStateManager")
//        appStateChangeHandler?(isInBackground)
//    }
//    
//    // 锁屏通知处理
//    @objc private func screenLocked() {
//        commercePrintLog("screenLocked", tag: "AppStateManager")
//        screenLockHandler?(true)
//    }
//    
//    // 解锁通知处理
//    @objc private func screenUnlocked() {
//        commercePrintLog("screenUnlocked", tag: "AppStateManager")
//        screenLockHandler?(false)
//    }
}
