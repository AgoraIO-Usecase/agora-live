//
//  ShowAgoraKitManager+DebugSetting.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2023/2/11.
//
import Foundation
import AgoraRtcKit


enum ShowDebug1TFSettingKey: String {
    
    case encodeFrameRate = "Coded frame rate"
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
    case encodeVideoSize = "Coding resolution"
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

extension ShowAgoraKitManager {
    
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
                showLogger.info("***Debug*** The encoding frame rate parameter is null ")
                return
            }
            encoderConfig.frameRate = fps
            engine?.setVideoEncoderConfiguration(encoderConfig)
            showLogger.info("***Debug*** setVideoEncoderConfiguration.encodeFrameRate = \(encoderConfig.frameRate) ")
        case .bitRate:
            guard let value = Int(text) else {
                showLogger.info("***Debug*** The rate parameter is null")
                return
            }
            encoderConfig.bitrate = value
            engine?.setVideoEncoderConfiguration(encoderConfig)
            showLogger.info("***Debug*** setVideoEncoderConfiguration.bitrate = \(encoderConfig.bitrate) ")
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
            showLogger.info("***Debug*** setVideoEncoderConfiguration.encodeVideoSize = \(encoderConfig.dimensions) ")
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
    
    /// Update Settings
    /// -Parameter key: specifies the key to be updated
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
        case .PVC:
            engine?.setParameters("{\"rtc.video.enable_pvc\":\(isOn)}")
        case .focusFace:
            engine?.setCameraAutoFocusFaceModeEnabled(isOn)
            showLogger.info("***Debug*** setCameraAutoFocusFaceModeEnabled  \(isOn)")
        case .encode:
            let index = indexValue % debugEncodeItems.count
            engine?.setParameters("{\"engine.video.enable_hw_encoder\":\"\(debugEncodeItems[index])\"}")
            showLogger.info("***Debug*** engine.video.enable_hw_encoder  \(debugEncodeItems[index])")
        case .codeCType:
            let index = indexValue % debugCodeCTypeItems.count
            engine?.setParameters("{\"engine.video.codec_type\":\"\(debugCodeCTypeItems[index])\"}")
            showLogger.info("***Debug*** engine.video.codec_type  \(debugCodeCTypeItems[index])")

        case .mirror, .renderMode:
            let index = ShowDebugSettingKey.renderMode.intValue % debugRenderModeItems.count
            let mirrorIsOn = ShowDebugSettingKey.mirror.boolValue
            engine?.setLocalRenderMode(debugRenderModeItems[index], mirror: mirrorIsOn ? .enabled : .disabled)
            showLogger.info("***Debug*** setLocalRenderMode  mirror = \(mirrorIsOn ? AgoraVideoMirrorMode.enabled : AgoraVideoMirrorMode.disabled), rendermode = \(debugRenderModeItems[index])")
        case .debugSR, .debugSrType:
            let srIsOn = ShowDebugSettingKey.debugSR.boolValue
            let index = ShowDebugSettingKey.debugSrType.intValue % debugSrTypeItems.count
            setDebugSuperResolutionOn(srIsOn, srType: debugSrTypeItems[index])
        case .debugPVC:
            engine?.setParameters("{\"rtc.video.enable_pvc\":\(isOn)}")
            showLogger.info("***Debug*** rtc.video.enable_pvc \(isOn)")
        }
    }
}

extension ShowAgoraKitManager {
    
    private func setExposureRange() {
        if let x = exposureRangeX, let y = exposureRangeY {
            engine?.setCameraExposurePosition(CGPoint(x: x, y: y))
            showLogger.info("***Debug*** setCameraExposurePosition = \(CGPoint(x: x, y: y)) ")
        }
    }
    
    private func setColorSpace(){
        if let v1 = videoFullrangeExt, let v2 = matrixCoefficientsExt {
            engine?.setParameters("{\"che.video.videoFullrangeExt\":\(v1)}")
            engine?.setParameters("{\"che.video.matrixCoefficientsExt\":\(v2)}")
            showLogger.info("***Debug*** {\"che.video.videoFullrangeExt\":\(v1)} {\"che.video.matrixCoefficientsExt\":\(v2)} ")
        }
    }
}

private let userDefaultKeyTag = "debug"

enum ShowDebugSettingKey: String, CaseIterable {
    
    enum KeyType {
        case aSwitch
        case segment
        case slider
        case label
    }
    
    case lowlightEnhance
    case colorEnhance
    case videoDenoiser
    case PVC
    case focusFace
    case encode
    case codeCType
    case mirror
    case renderMode
    case debugSrType
    case debugSR
    case debugPVC
    
    var title: String {
        switch self {
        case .lowlightEnhance:
            return "show_advance_setting_lowlight_title".show_localized
        case .colorEnhance:
            return "show_advance_setting_colorEnhance_title".show_localized
        case .videoDenoiser:
            return "show_advance_setting_videoDenoiser_title".show_localized
        case .PVC:
            return "PVC"
        case .focusFace:
            return "Face focusing"
        case .encode:
            return "Hard/soft"
        case .codeCType:
            return "encoder"
        case .mirror:
            return "Mirror image"
        case .renderMode:
            return "fit/hidden"
        case .debugSrType:
            return "overfraction"
        case .debugSR:
            return "Supersplit switch"
        case .debugPVC:
            return "PVC"
        }
    }
    
    var type: KeyType {
        switch self {
        case .lowlightEnhance:
            return .aSwitch
        case .colorEnhance:
            return .aSwitch
        case .videoDenoiser:
            return .aSwitch
        case .PVC:
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
    
    var tips: String {
        switch self {
        case .lowlightEnhance:
            return "show_advance_setting_lowlightEnhance_tips".show_localized
        case .colorEnhance:
            return "show_advance_setting_colorEnhance_tips".show_localized
        case .videoDenoiser:
            return "show_advance_setting_videoDenoiser_tips".show_localized
        case .PVC:
            return "show_advance_setting_PVC_tips".show_localized
        default:
            return ""
        }
    }
    
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
