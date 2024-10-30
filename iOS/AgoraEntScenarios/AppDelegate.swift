//
//  AppDelegate.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/19.
//

import UIKit
import AgoraCommon

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
//        VLUserCenter.shared().logout()
        configKeyCenterData()
        window?.configRootViewController()
        return true
    }

    func applicationWillTerminate(_ application: UIApplication) {
        AgoraRtcEngineKit.destroy()
    }
    
    func configKeyCenterData() {
        var isDebugMode = false
        if let index: Int = UserDefaults.standard.object(forKey: "TOOLBOXENV") as? Int {
            isDebugMode = index == 1
        } else {
            isDebugMode = false
        }
        
        AppContext.shared.appId = KeyCenter.AppId
        AppContext.shared.certificate = KeyCenter.Certificate ?? ""
        AppContext.shared.hostUrl = KeyCenter.HostUrl
        AppContext.shared.baseServerUrl = isDebugMode ? (KeyCenter.debugBaseServerUrl ?? "") : (KeyCenter.releaseBaseServerUrl ?? "")
        AppContext.shared.releaseBaseUrl = KeyCenter.releaseBaseServerUrl ?? ""
        AppContext.shared.debugBaseUrl = KeyCenter.debugBaseServerUrl ?? ""
        AppContext.shared.roomManagerUrl = "\(AppContext.shared.baseServerUrl)/room-manager"
        AppContext.shared.imAppKey = KeyCenter.IMAppKey ?? ""
        AppContext.shared.imClientId = KeyCenter.IMClientId ?? ""
        AppContext.shared.imClientSecret = KeyCenter.IMClientSecret ?? ""
        AppContext.shared.rtmHost = KeyCenter.RTMHostUrl
    }
}
