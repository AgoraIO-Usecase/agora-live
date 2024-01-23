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
    private var dislikeRoomCache: [String :String] = [:]
    private var dislikeUserCache: [String :String] = [:]
    @objc var sceneConfig: VLSceneConfigsModel?
    
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
    
    func addDislikeRoom(at roomId: String?) {
        guard let roomId = roomId else { return }
        dislikeRoomCache[(sceneImageBundleName ?? "") + roomId] = roomId
    }
    func dislikeRooms() -> [String] {
        let value = dislikeRoomCache.filter({ $0.key.contains(sceneImageBundleName ?? "") })
        return value.map({ $0.value })
    }
    func addDislikeUser(at uid: String?) {
        guard let uid = uid else { return }
        dislikeUserCache[(sceneImageBundleName ?? "") + uid] = uid
    }
    func dislikeUsers() -> [String] {
        let value = dislikeUserCache.filter({ $0.key.contains(sceneImageBundleName ?? "") })
        return value.map({ $0.value })
    }
}
