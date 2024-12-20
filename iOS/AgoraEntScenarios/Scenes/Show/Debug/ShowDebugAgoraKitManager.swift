//
//  ShowAgoraKitManager+DebugSetting.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2023/2/11.
//
import Foundation
import AgoraRtcKit

private let userDefaultKeyTag = "debug"
// Store the key of coding configuration
private let kEncodeWidth = "kEncodeWidth"
private let kEncodeHeight = "kEncodeHeight"
private let kEncodeFPS = "kEncodeFPS"
private let kEncodeBitrate = "kEncodeBitrate"


enum ShowDebug1TFSettingKey: String {
    
    case encodeFrameRate = "Encoded frame rate"
    case bitRate = "Bit rate"
    
    var unit: String {
        switch self {
        case .encodeFrameRate:
            return "fps"
        case .bitRate:
            return "kbps"
        }
    }
}

enum ShowDebug2TFSettingKey: String {
    case encodeVideoSize = "Encoding resolution"
    case exposureRange = "Exposure area"
    case colorSpace = "Color space"
    
    var separator: String {
        switch self {
        case .encodeVideoSize:
            return "x"
        case .exposureRange:
            return "x"
        case .colorSpace:
            return "/"
        }
    }
}

class ShowDebugAgoraKitManager {
    
    static let shared = ShowDebugAgoraKitManager()
    
    private lazy var encoderConfig :AgoraVideoEncoderConfiguration = {
        getEncoderConfig()
    }()
    
    public var engine: AgoraRtcEngineKit?
    
    var exposureRangeX: Int?
    var exposureRangeY: Int?
    var matrixCoefficientsExt: Int?
    var videoFullrangeExt: Int?
    
    private init() {
//        engine = AgoraRtcEngineKit.sharedEngine(with: AgoraRtcEngineConfig(), delegate: nil)
    }
    
    private func engineConfig() -> AgoraRtcEngineConfig {
        let config = AgoraRtcEngineConfig()
         config.appId = KeyCenter.AppId
         config.channelProfile = .liveBroadcasting
         config.areaCode = .global
         return config
    }
    
    private var debugEncodeItems: [Bool] {
        ShowAgoraEncode.allCases.map({$0.encodeValue})
    }
    
    private var debugCodeCTypeItems: [Int] {
        ShowAgoraCodeCType.allCases.map({$0.typeValue})
    }
    
    private var debugRenderModeItems: [AgoraVideoRenderMode] {
        ShowAgoraRenderMode.allCases.map({$0.modeValue})
    }
    
    private var debugSrTypeItems: [ShowSRType] {
        ShowAgoraSRType.allCases.map({$0.typeValue})
    }
    
    func debugDefaultBroadcastorSetting() {
        encoderConfig.dimensions = CGSize(width: 1920, height: 1080)
        encoderConfig.frameRate = .fps15
        encoderConfig.bitrate = 1800
        engine?.setVideoEncoderConfiguration(encoderConfig)
        saveVideoEncoderConfiguration()
        
        setExposureRange()
        setColorSpace()
        
        ShowDebugSettingKey.debugPVC.writeValue(false)
        ShowDebugSettingKey.focusFace.writeValue(false)
        ShowDebugSettingKey.encode.writeValue(0)
        ShowDebugSettingKey.codeCType.writeValue(0)
        ShowDebugSettingKey.mirror.writeValue(false)
        ShowDebugSettingKey.renderMode.writeValue(0)
        ShowDebugSettingKey.colorEnhance.writeValue(false)
        ShowDebugSettingKey.lowlightEnhance.writeValue(false)
        ShowDebugSettingKey.videoDenoiser.writeValue(false)
        
        updateSettingForDebugkey(.debugPVC)
        updateSettingForDebugkey(.focusFace)
        updateSettingForDebugkey(.encode)
        updateSettingForDebugkey(.codeCType)
        updateSettingForDebugkey(.mirror)
        updateSettingForDebugkey(.renderMode)
        updateSettingForDebugkey(.colorEnhance)
        updateSettingForDebugkey(.lowlightEnhance)
        updateSettingForDebugkey(.videoDenoiser)
    }
    
    func debugDefaultAudienceSetting() {
        ShowDebugSettingKey.debugSR.writeValue(false)
        ShowDebugSettingKey.debugSrType.writeValue(0)
        
        updateSettingForDebugkey(.debugSR)
        updateSettingForDebugkey(.debugSrType)
    }
    
    func debug1TFModelForKey(_ key: ShowDebug1TFSettingKey) -> ShowDebug1TFModel {
        var originalValue = ""
        switch key {
        case .encodeFrameRate:
            originalValue = "\(encoderConfig.frameRate.rawValue)"
        case .bitRate:
            originalValue = "\(encoderConfig.bitrate)"
        }
        return ShowDebug1TFModel(title: key.rawValue, tfText: originalValue, unitText: key.unit)
    }
    
    func debug2TFModelForKey(_ key: ShowDebug2TFSettingKey) -> ShowDebug2TFModel{
        var text1 = "", text2 = ""
        switch key {
        case .encodeVideoSize:
            text1 = "\(Int(encoderConfig.dimensions.width))"
            text2 = "\(Int(encoderConfig.dimensions.height))"
        case .exposureRange:
            if let exposureRangeX = exposureRangeX {
                text1 = "\(exposureRangeX)"
            }
            if let exposureRangeY = exposureRangeY {
                text2 = "\(exposureRangeY)"
            }
        case .colorSpace:
            if let videoFullrangeExt = videoFullrangeExt {
                text1 = "\(videoFullrangeExt)"
            }
            if let matrixCoefficientsExt = matrixCoefficientsExt {
                text2 = "\(matrixCoefficientsExt)"
            }
        }
        return ShowDebug2TFModel(title: key.rawValue, tf1Text: text1, tf2Text: text2, separatorText: key.separator)
    }
    
    func updateDebugProfileFor1TFMode(_ model: ShowDebug1TFModel) {
        guard let text = model.tfText else { return }
        guard let title = model.title, let key =  ShowDebug1TFSettingKey(rawValue: title) else { return }
        switch key {
        case .encodeFrameRate:
            guard let value = Int(text), let fps = AgoraVideoFrameRate(rawValue: value) else {
                ShowLogger.info("***Debug*** The encoding frame rate parameter is empty. ")
                return
            }
            encoderConfig.frameRate = fps
            engine?.setVideoEncoderConfiguration(encoderConfig)
            saveVideoEncoderConfiguration()
            ShowLogger.info("***Debug*** setVideoEncoderConfiguration.encodeFrameRate = \(encoderConfig.frameRate) ")
        case .bitRate:
            guard let value = Int(text) else {
                ShowLogger.info("***Debug*** The bit rate parameter is empty.")
                return
            }
            encoderConfig.bitrate = value
            engine?.setVideoEncoderConfiguration(encoderConfig)
            saveVideoEncoderConfiguration()
            ShowLogger.info("***Debug*** setVideoEncoderConfiguration.bitrate = \(encoderConfig.bitrate) ")
        }
    }
    
    func updateDebugProfileFor2TFModel(_ model: ShowDebug2TFModel) {
        guard let title = model.title, let key =  ShowDebug2TFSettingKey(rawValue: title) else { return }
        guard let text1 = model.tf1Text, let text2 = model.tf2Text else { return }
        guard let value1 = Int(text1), let value2 = Int(text2) else {return}
        guard value1 > 0, value2 > 0 else { return }
        switch key {
        case .encodeVideoSize:
            encoderConfig.dimensions = CGSize(width: value1, height: value2)
            engine?.setVideoEncoderConfiguration(encoderConfig)
            saveVideoEncoderConfiguration()
            ShowLogger.info("***Debug*** setVideoEncoderConfiguration.encodeVideoSize = \(encoderConfig.dimensions) ")
        case .exposureRange:
            exposureRangeX = value1
            exposureRangeY = value2
            setExposureRange()
        case .colorSpace:
            videoFullrangeExt = value1
            matrixCoefficientsExt = value2
            setColorSpace()
        }
    }
    
    /// Update settings
    /// - Parameter key: key to be updated
    func updateSettingForDebugkey(_ key: ShowDebugSettingKey, currentChannelId:String? = nil) {
        let isOn = key.boolValue
        let indexValue = key.intValue
        
        switch key {
        case .lowlightEnhance:
            engine?.setLowlightEnhanceOptions(isOn, options: AgoraLowlightEnhanceOptions())
        case .colorEnhance:
            engine?.setColorEnhanceOptions(isOn, options: AgoraColorEnhanceOptions())
        case .videoDenoiser:
            engine?.setVideoDenoiserOptions(isOn, options: AgoraVideoDenoiserOptions())
        case .focusFace:
            engine?.setCameraAutoFocusFaceModeEnabled(isOn)
            ShowLogger.info("***Debug*** setCameraAutoFocusFaceModeEnabled  \(isOn)")
        case .encode:
            let index = indexValue % debugEncodeItems.count
            engine?.setParameters("{\"engine.video.enable_hw_encoder\":\"\(debugEncodeItems[index])\"}")
            ShowLogger.info("***Debug*** engine.video.enable_hw_encoder  \(debugEncodeItems[index])")
        case .codeCType:
            let index = indexValue % debugCodeCTypeItems.count
            engine?.setParameters("{\"engine.video.codec_type\":\"\(debugCodeCTypeItems[index])\"}")
            ShowLogger.info("***Debug*** engine.video.codec_type  \(debugCodeCTypeItems[index])")

        case .mirror, .renderMode:
            let index = ShowDebugSettingKey.renderMode.intValue % debugRenderModeItems.count
            let mirrorIsOn = ShowDebugSettingKey.mirror.boolValue
            engine?.setLocalRenderMode(debugRenderModeItems[index], mirror: mirrorIsOn ? .enabled : .disabled)
            ShowLogger.info("***Debug*** setLocalRenderMode  mirror = \(mirrorIsOn ? AgoraVideoMirrorMode.enabled : AgoraVideoMirrorMode.disabled), rendermode = \(debugRenderModeItems[index])")
        case .debugSR, .debugSrType:
            let srIsOn = ShowDebugSettingKey.debugSR.boolValue
            let index = ShowDebugSettingKey.debugSrType.intValue % debugSrTypeItems.count
            setDebugSuperResolutionOn(srIsOn, srType: debugSrTypeItems[index])
        case .debugPVC:
            engine?.setParameters("{\"rtc.video.enable_pvc\":\(isOn)}")
            ShowLogger.info("***Debug*** rtc.video.enable_pvc \(isOn)")
        }
    }
}

extension ShowDebugAgoraKitManager {
    
    private func setExposureRange() {
        if let x = exposureRangeX, let y = exposureRangeY {
            engine?.setCameraExposurePosition(CGPoint(x: x, y: y))
            ShowLogger.info("***Debug*** setCameraExposurePosition = \(CGPoint(x: x, y: y)) ")
        }
    }
    
    private func setColorSpace(){
        if let v1 = videoFullrangeExt, let v2 = matrixCoefficientsExt {
            engine?.setParameters("{\"che.video.videoFullrangeExt\":\(v1)}")
            engine?.setParameters("{\"che.video.matrixCoefficientsExt\":\(v2)}")
            ShowLogger.info("***Debug*** {\"che.video.videoFullrangeExt\":\(v1)} {\"che.video.matrixCoefficientsExt\":\(v2)} ")
        }
    }
    
    /// Set the excess score and do not save the data
    /// - Parameters:
    /// - isOn: Switch
    /// - srType: 1.5 times by default
    func setDebugSuperResolutionOn(_ isOn: Bool, srType:ShowSRType = .none) {
        if srType == .none {
            engine?.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":\(false), \"mode\": 2}}")
        }else{
            engine?.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":\(false), \"mode\": 2}}")
            engine?.setParameters("{\"rtc.video.sr_type\":\(srType.rawValue)}")
            engine?.setParameters("{\"rtc.video.sr_max_wh\":\(921598)}")
            // Enabled should be placed after srType, otherwise the modification of the excess multiple may not take effect immediately.
            engine?.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":\(isOn), \"mode\": 2}}")
        }
    }
    
    func saveVideoEncoderConfiguration() {
        UserDefaults.standard.set(encoderConfig.dimensions.width, forKey: kEncodeWidth)
        UserDefaults.standard.set(encoderConfig.dimensions.height, forKey: kEncodeHeight)
        UserDefaults.standard.set(encoderConfig.frameRate.rawValue, forKey: kEncodeFPS)
        UserDefaults.standard.set(encoderConfig.bitrate, forKey: kEncodeBitrate)
        UserDefaults.standard.synchronize()
    }
    
    func getEncoderConfig() ->AgoraVideoEncoderConfiguration{
        let encoderConfig = AgoraVideoEncoderConfiguration()
        if let encodeWidth: CGFloat = UserDefaults.standard.value(forKey: kEncodeWidth) as? CGFloat ,let encodeHeight: CGFloat = UserDefaults.standard.value(forKey: kEncodeHeight) as? CGFloat {
            encoderConfig.dimensions = CGSize(width: encodeWidth, height: encodeHeight)
        }
        if let fps: Int = UserDefaults.standard.value(forKey: kEncodeFPS) as? Int {
            encoderConfig.frameRate =  AgoraVideoFrameRate(rawValue: fps) ?? .fps15
        }
        if let bitrate: Int = UserDefaults.standard.value(forKey: kEncodeBitrate) as? Int {
            encoderConfig.bitrate = bitrate
        }
        return encoderConfig
    }
}

enum ShowDebugSettingKey: String, CaseIterable {
    
    enum KeyType {
        case aSwitch
        case segment
        case slider
        case label
    }
    
    case lowlightEnhance        // Dark light enhancement
    case colorEnhance           // Color enhancement
    case videoDenoiser          // Reduce the noise
    case focusFace              // Face focus
    case encode                 // Hard editing/soft editing
    case codeCType                // Encoder
    case mirror                 // Mirror image
    case renderMode             // Model
    case debugSrType            // Excess multiple
    case debugSR                // debug Over-spart switch
    case debugPVC               // pvc
    
    var title: String {
        switch self {
        case .lowlightEnhance:
            return "show_advance_setting_lowlight_title".show_localized
        case .colorEnhance:
            return "show_advance_setting_colorEnhance_title".show_localized
        case .videoDenoiser:
            return "show_advance_setting_videoDenoiser_title".show_localized
        case .focusFace:
            return "Face focus"
        case .encode:
            return "Hard editing/soft editing"
        case .codeCType:
            return "Encoder"
        case .mirror:
            return "Mirror image"
        case .renderMode:
            return "fit/hidden"
        case .debugSrType:
            return "Excess multiple"
        case .debugSR:
            return "Over-spart switch"
        case .debugPVC:
            return "PVC"
        }
    }
    
    // Type
    var type: KeyType {
        switch self {
        case .lowlightEnhance:
            return .aSwitch
        case .colorEnhance:
            return .aSwitch
        case .videoDenoiser:
            return .aSwitch
        case .focusFace:
            return .aSwitch
        case .encode:
            return .label
        case .codeCType:
            return .label
        case .mirror:
            return .aSwitch
        case .renderMode:
            return .label
        case .debugSrType:
            return .label
        case .debugSR:
            return .aSwitch
        case .debugPVC:
            return .aSwitch
        }
    }
    
    // Pop-up window prompt copywriting
    var tips: String {
        switch self {
        case .lowlightEnhance:
            return "show_advance_setting_lowlightEnhance_tips".show_localized
        case .colorEnhance:
            return "show_advance_setting_colorEnhance_tips".show_localized
        case .videoDenoiser:
            return "show_advance_setting_videoDenoiser_tips".show_localized
        default:
            return ""
        }
    }
    
    // Be an option
    var items: [String] {
        switch self {
        case .encode:
            return ShowAgoraEncode.allCases.map({$0.rawValue})
        case .codeCType:
            return ShowAgoraCodeCType.allCases.map({$0.rawValue})
        case .renderMode:
            return ShowAgoraRenderMode.allCases.map({$0.rawValue})
        case .debugSrType:
            return ShowAgoraSRType.allCases.map({$0.rawValue})
        default:
            return []
        }
    }
    
    var boolValue: Bool {
        return UserDefaults.standard.bool(forKey: self.rawValue + userDefaultKeyTag)
    }
    
    var floatValue: Float {
        return UserDefaults.standard.float(forKey: self.rawValue + userDefaultKeyTag)
    }
    
    var intValue: Int {
        return UserDefaults.standard.integer(forKey: self.rawValue + userDefaultKeyTag)
    }
    
    func writeValue(_ value: Any?){
        UserDefaults.standard.set(value, forKey: self.rawValue + userDefaultKeyTag)
        UserDefaults.standard.synchronize()
    }
}
