//
//  AppContext.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/18.
//

import Foundation

@objc class AppContext: NSObject {
    @objc static let shared: AppContext = .init()
    @objc var sceneLocalizeBundleName: String?
    @objc var sceneImageBundleName: String?
    @objc var extDic: NSMutableDictionary = NSMutableDictionary()
    @objc var isDebugMode = false
    @objc var imageCahe = [String: AnyObject]()
    @objc var localizedCache = [String: String]()
    
    @objc var isAgreeLicense: Bool = false {
        didSet {
            guard isAgreeLicense else {
                return
            }
        }
    }
    
    override init() {
        super.init()
    }

    @objc func getLang() -> String {
        guard let lang = NSLocale.preferredLanguages.first else {
            return "en"
        }

        if lang.contains("zh") {
            return "zh-Hans"
        }

        return "en"
    }

    // MARK: user

    // MARK: App Config

    @objc func appId() -> String {
        return KeyCenter.AppId
    }

    @objc func appHostUrl() -> String {
        return KeyCenter.HostUrl
    }
    
    @objc func appRTCToken() -> String {
        return VLUserCenter.user.agoraRTCToken
    }
    
    @objc func appRTMToken() -> String {
        return VLUserCenter.user.agoraRTMToken
    }
}
