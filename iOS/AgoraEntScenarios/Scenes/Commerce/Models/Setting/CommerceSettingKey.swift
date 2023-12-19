//
//  ShowSettingManager.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/16.
//

import Foundation
import AgoraRtcKit

enum CommerceAgoraSRType: String, CaseIterable {
    case x1 = "x1"
    case x1_33 = "x1.33"
    case x1_5 = "x1.5"
    case x2 = "x2"
    
    var typeValue: CommerceSRType {
        switch self {
        case .x1:
            return .x1
        case .x1_33:
            return .x1_33
        case .x1_5:
            return .x1_5
        case .x2:
            return .x2
        }
    }
}

enum CommerceAgoraRenderMode: String, CaseIterable {
    case hidden = "hidden"
    case fit = "fit"
    
    var modeValue: AgoraVideoRenderMode {
        switch self {
        case .hidden:
            return .hidden
        case .fit:
            return .fit
        }
    }
}

enum CommerceAgoraEncode: String, CaseIterable {
    case hard = "hardweave"
    case soft = "softweave"
    
    var encodeValue: Bool {
        switch self {
        case .hard:
            return true
        case .soft:
            return false
        }
    }
}

enum CommerceAgoraCodeCType: String, CaseIterable {
    case h265 = "h265"
    case h264 = "h264"
    
    var typeValue: Int {
        switch self {
        case .h265:
            return 3
        case .h264:
            return 2
        }
    }
}

enum CommerceAgoraVideoDimensions: String, CaseIterable {
    case _360x640 = "360x640"
    case _480x856 = "480x856"
    case _540x960 = "540x960"
    case _720x1280 = "720x1280"
    case _1080x1920 = "1080x1920"
    var sizeValue: CGSize {
        let arr: [String] = rawValue.split(separator: "x").compactMap{"\($0)"}
        guard let first = arr.first, let width = Float(first), let last = arr.last, let height = Float(last) else {
            return CGSize(width: 360, height: 640)
        }
        return CGSize(width: CGFloat(width), height: CGFloat(height))
    }
    
    static func values() -> [CGSize] {
        return CommerceAgoraVideoDimensions.allCases.map({$0.sizeValue})
    }
}

enum CommerceSettingKey: String, CaseIterable {
    
    enum KeyType {
        case aSwitch
        case segment
        case slider
        case label
        case custom
    }
    
    case lowlightEnhance
    case colorEnhance
    case videoDenoiser
    case beauty
    case PVC
    case SR
    case BFrame
    case videoEncodeSize
    case FPS
    case H265
    case videoBitRate
    case earmonitoring
    case recordingSignalVolume
    case musicVolume
    case audioBitRate
    
    var title: String {
        switch self {
        case .lowlightEnhance:
            return "show_advance_setting_lowlight_title".commerce_localized
        case .colorEnhance:
            return "show_advance_setting_colorEnhance_title".commerce_localized
        case .videoDenoiser:
            return "show_advance_setting_videoDenoiser_title".commerce_localized
        case .beauty:
            return "show_advance_setting_beauty_title".commerce_localized
        case .PVC:
            return "PVC"
        case .SR:
            return "show_advance_setting_SR_title".commerce_localized
        case .BFrame:
            return "show_advance_setting_BFrame_title".commerce_localized
        case .videoEncodeSize:
            return "show_advance_setting_videoCaptureSize_title".commerce_localized
        case .FPS:
            return "show_advance_setting_FPS_title".commerce_localized
        case .videoBitRate:
            return "show_advance_setting_bitRate_title".commerce_localized
        case .H265:
            return "show_advance_setting_H265_title".commerce_localized
        case .earmonitoring:
            return "show_advance_setting_earmonitoring_title".commerce_localized
        case .recordingSignalVolume:
            return "show_advance_setting_recordingVolume_title".commerce_localized
        case .musicVolume:
            return "show_advance_setting_musicVolume_title".commerce_localized
        case .audioBitRate:
            return "show_advance_setting_audio_bitRate_title".commerce_localized
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
        case .beauty:
            return .aSwitch
        case .PVC:
            return .custom
        case .SR:
            return .custom
        case .BFrame:
            return .aSwitch
        case .videoEncodeSize:
            return .label
        case .FPS:
            return .label
        case .H265:
            return .aSwitch
        case .videoBitRate:
            return .custom
        case .earmonitoring:
            return .aSwitch
        case .recordingSignalVolume:
            return .slider
        case .musicVolume:
            return .slider
        case .audioBitRate:
            return .label
        }
    }
    
    var tips: String {
        switch self {
        case .lowlightEnhance:
            return "show_advance_setting_lowlightEnhance_tips".commerce_localized
        case .colorEnhance:
            return "show_advance_setting_colorEnhance_tips".commerce_localized
        case .videoDenoiser:
            return "show_advance_setting_videoDenoiser_tips".commerce_localized
        case .PVC:
            return "show_advance_setting_PVC_tips".commerce_localized
        case .SR:
            return "show_advance_setting_SR_tips".commerce_localized
        case .H265:
            return "show_advance_setting_H265_tips".commerce_localized
        case .videoBitRate:
            return "show_advance_setting_bitRate_tips".commerce_localized
        case .videoEncodeSize:
            return "show_advance_setting_videoEncodeSize_tips".commerce_localized
        case .FPS:
            return "show_advance_setting_fps_tips".commerce_localized
        default:
            return ""
        }
    }
    
    var items: [String] {
        switch self {
        case .videoEncodeSize:
            return CommerceAgoraVideoDimensions.allCases.map({ $0.rawValue })
        case .FPS:
            return [AgoraVideoFrameRate.fps1.stringValue(),
                    AgoraVideoFrameRate.fps7.stringValue(),
                    AgoraVideoFrameRate.fps10.stringValue(),
                    AgoraVideoFrameRate.fps15.stringValue(),
                    AgoraVideoFrameRate.fps24.stringValue(),
            ]
        case .audioBitRate:
            return ["2","3","5"]
        default:
            return []
        }
    }
    
    var boolValue: Bool {
        return UserDefaults.standard.bool(forKey: self.rawValue)
    }
    
    var floatValue: Float {
        return UserDefaults.standard.float(forKey: self.rawValue)
    }
    
    var intValue: Int {
        return UserDefaults.standard.integer(forKey: self.rawValue)
    }
    
    func writeValue(_ value: Any?){
        UserDefaults.standard.set(value, forKey: self.rawValue)
        UserDefaults.standard.synchronize()
    }
}

