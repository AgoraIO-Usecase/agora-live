//
//  AppContext.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/10/18.
//

import Foundation

@objc public class AppContext: NSObject {
    @objc public static let shared: AppContext = .init()
    @objc public var sceneLocalizeBundleName: String?
    @objc public var sceneImageBundleName: String?
    @objc public var extDic: NSMutableDictionary = NSMutableDictionary()
    @objc public var imageCahe = [String: AnyObject]()
    @objc public var localizedCache = [String: String]()
    @objc public var sceneConfig: VLSceneConfigsModel?
    @objc public var isDebugMode = false
    @objc public var agoraRTCToken: String = ""
    @objc public var agoraRTMToken: String = ""
    
    private var dislikeRoomCache: [String :String] = [:]
    private var dislikeUserCache: [String :String] = [:]
    
    private var _appId: String = ""
    private var _certificate: String = ""
    private var _imAppKey: String = ""
    private var _imClientId: String = ""
    private var _imClientSecret: String = ""
    private var _restfulApiKey: String = ""
    private var _restfulApiSecret: String = ""
    private var _hostUrl: String = ""
    private var _baseServerUrl: String = ""
    private var _roomManagerUrl: String = ""
    private var _cloudPlayerKey: String = ""
    private var _cloudPlayerSecret: String = ""
    private var _releaseBaseUrl: String = ""
    private var _debugBaseUrl: String = ""
    private var _rtmHost: String = ""
    
    
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
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
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

    @objc public var appId: String {
        get {
            return _appId
        }
        set {
            _appId = newValue
        }
    }
    
    @objc public var certificate: String {
        get {
            return _certificate
        }
        set {
            _certificate = newValue
        }
    }
    
    @objc public var imAppKey: String {
        get {
            return _imAppKey
        }
        set {
            _imAppKey = newValue
        }
    }
    
    @objc public var imClientId: String {
        get {
            return _imClientId
        }
        set {
            _imClientId = newValue
        }
    }
    
    @objc public var imClientSecret: String {
        get {
            return _imClientSecret
        }
        set {
            _imClientSecret = newValue
        }
    }
    
    @objc public var RestfulApiKey: String {
        get {
            return _restfulApiKey
        }
        set {
            _restfulApiKey = newValue
        }
    }
    
    @objc public var RestfulApiSecret: String {
        get {
            return _restfulApiSecret
        }
        set {
            _restfulApiSecret = newValue
        }
    }
    
    @objc public var hostUrl: String {
        get {
            return _hostUrl
        }
        set {
            _hostUrl = newValue
        }
    }
    
    @objc public var roomManagerUrl: String {
        get {
            return _roomManagerUrl
        }
        set {
            _roomManagerUrl = newValue
        }
    }
    
    @objc public var baseServerUrl: String {
        get {
            return _baseServerUrl
        }
        set {
            _baseServerUrl = newValue
        }
    }
    
    @objc public var cloudPlayerKey: String {
        get {
            return _cloudPlayerKey
        }
        set {
            _cloudPlayerKey = newValue
        }
    }
    
    @objc public var cloudPlayerSecret: String {
        get {
            return _cloudPlayerSecret
        }
        set {
            _cloudPlayerSecret = newValue
        }
    }
    
    @objc public var releaseBaseUrl: String {
        get {
            return _releaseBaseUrl
        }
        set {
            _releaseBaseUrl = newValue
        }
    }
    
    @objc public var debugBaseUrl: String {
        get {
            return _debugBaseUrl
        }
        set {
            _debugBaseUrl = newValue
        }
    }
    
    @objc public var rtmHost: String {
        get {
            return _rtmHost
        }
        set {
            _rtmHost = newValue
        }
    }
    
    public func addDislikeRoom(at roomId: String?) {
        guard let roomId = roomId else { return }
        dislikeRoomCache[(sceneImageBundleName ?? "") + roomId] = roomId
    }
    
    public func dislikeRooms() -> [String] {
        let value = dislikeRoomCache.filter({ $0.key.contains(sceneImageBundleName ?? "") })
        return value.map({ $0.value })
    }
    
    public func addDislikeUser(at uid: String?) {
        guard let uid = uid else { return }
        dislikeUserCache[(sceneImageBundleName ?? "") + uid] = uid
    }
    
    public func dislikeUsers() -> [String] {
        let value = dislikeUserCache.filter({ $0.key.contains(sceneImageBundleName ?? "") })
        return value.map({ $0.value })
    }
}
