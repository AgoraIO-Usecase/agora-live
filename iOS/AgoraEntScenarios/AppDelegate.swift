//
//  AppDelegate.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/19.
//

import UIKit

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
//        VLUserCenter.shared().logout()
        window?.configRootViewController()
        return true
    }

    func applicationDidEnterBackground(_ application: UIApplication) {
        AgoraRtcEngineKit.destroy()
    }
}
