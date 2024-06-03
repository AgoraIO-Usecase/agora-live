//
//  ShowAgoraKitManager+Setting.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/12/5.
//

import Foundation
import AgoraRtcKit

private let kEncodeWidth = "kEncodeWidth"
private let kEncodeHeight = "kEncodeHeight"
private let kEncodeFPS = "kEncodeFPS"
private let kEncodeBitrate = "kEncodeBitrate"

enum ShowMode {
    case single // 单主播模式
    case pk // pk模式
}

private let fpsItems: [AgoraVideoFrameRate] = [
    .fps1,
    .fps7,
    .fps10,
    .fps15,
    .fps24,
    .fps30,
    .fps60
]

// overfraction
enum ShowSRType: Int {
    case none = -1
    case x1 = 6
    case x1_33 = 7
    case x1_5 = 8
    case x2 = 3
    case x_sharpen = 11
    case x_superQuality = 20
}

class ShowRTCParams {
    // Automatic setting data
    var suggested = true
    
    var sr = false
    var srType: ShowSRType = .x1_33
    var dualStream: AgoraSimulcastStreamConfig?
    var pvc = false
    var svc = false
    var musicVolume: Int = 30
    var recordingSignalVolume: Int = 80
}

// MARK: - Extension
extension ShowAgoraKitManager {
    
    func updateAudienceProfile() {
        _presetValuesWith(encodeSize: ._360x640, fps: .fps15, bitRate: 0, h265On: true)
    }
    
    func setupAudienceProfile() {
        setPVCon(false)
        _presetValuesWith(encodeSize: ._360x640, fps: .fps15, bitRate: 0, h265On: true)
    }
    
    func setupBroadcasterProfile() {
        setSuperResolutionOn(false)
        setPVCon(false)
        updateVideoProfileForMode(.single)
    }
    
    /// Set the hyperpartition not to save data
    /// - Parameters:
    /// -isOn: switch
    /// -srType: The default value is 1.5 times
    func setDebugSuperResolutionOn(_ isOn: Bool, srType:ShowSRType = .none) {
        // Avoid duplicate Settings
        if isOn == self.rtcParam.sr && srType == self.rtcParam.srType {
            return
        }
        self.rtcParam.sr = isOn
        self.rtcParam.srType = srType
        if srType == .none {
            engine?.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":\(false), \"mode\": 2}}")
        }else{
            engine?.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":\(false), \"mode\": 2}}")
            engine?.setParameters("{\"rtc.video.sr_type\":\(srType.rawValue)}")
            engine?.setParameters("{\"rtc.video.sr_max_wh\":\(921598)}")
            // The value of enabled must be placed after srType; otherwise, the change may not take effect immediately
            engine?.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":\(isOn), \"mode\": 2}}")
        }
    }
    
    /// Set the hyperpartition not to save data
    /// - Parameters:
    /// -isOn: switch
    func setSuperResolutionOn(_ isOn: Bool) {
        self.rtcParam.sr = isOn
        engine?.setParameters("{\"rtc.video.sr_max_wh\":\(921598)}")
        engine?.setParameters("{\"rtc.video.enable_sr\":{\"enabled\":\(isOn), \"mode\": 2}}")
    }
    /// Set PVC
    /// - Parameters:
    /// -isOn: switch
    func setPVCon(_ isOn: Bool) {
        self.rtcParam.pvc = isOn
        engine?.setParameters("{\"rtc.video.enable_pvc\":\(isOn)}")
    }
    /// Set up noise reduction
    /// - Parameters:
    /// -isOn: switch
    func setDenoiserOn(_ isOn: Bool) {
        let option = AgoraVideoDenoiserOptions()
        switch deviceLevel {
        case .high:
            option.mode = .manual
            option.level = .strength
        case .medium:
            option.mode = .manual
            option.level = .highQuality
        case .low:
            option.mode = .manual
            option.level = .fast
        }
        engine?.setVideoDenoiserOptions(isOn, options: option)
    }
    
    /** 设置小流参数
     */
    private func setSimulcastStream(isOn: Bool, dimensions: CGSize = CGSizeMake(360, 640), fps: Int32 = 5, bitrate: Int = 680, svc: Bool = false) {
        if isOn {
            let config = AgoraSimulcastStreamConfig()
            config.dimensions = dimensions
            config.framerate = fps
            config.kBitrate = Int32(bitrate)
            rtcParam.dualStream = config
            rtcParam.svc = svc
        } else {
            rtcParam.dualStream = nil
            rtcParam.svc = false
        }
    }
    /** Apply the small stream Settings
     * Configuration of streamer before streamer application after joinChannel success
     */
    func applySimulcastStream(connection: AgoraRtcConnection) {
        guard let simulcastConfig = rtcParam.dualStream else {
            engine?.setDualStreamModeEx(.disableSimulcastStream,
                                        streamConfig: AgoraSimulcastStreamConfig(),
                                        connection: connection)
            return
        }
        // Small stream SVC switch
        if (rtcParam.svc) {
            engine?.setParameters("{\"che.video.minor_stream_num_temporal_layers\": 2}")
            engine?.setParameters("{\"rtc.video.high_low_video_ratio_enabled\": true}")
            engine?.setParameters("{\"che.video.enable_264_fix_svc_nego\": false}")
            engine?.setParameters("{\"rtc.video.low_stream_enable_hw_encoder\": false}")
        } else {
            engine?.setParameters("{\"rtc.video.high_low_video_ratio_enabled\": false}")
            engine?.setParameters("{\"rtc.video.low_stream_enable_hw_encoder\": true}")
        }
        engine?.setDualStreamModeEx(.enableSimulcastStream, streamConfig: simulcastConfig, connection: connection)
    }
    
    // Default mode
    private func _presetValuesWith(encodeSize: ShowAgoraVideoDimensions, fps: AgoraVideoFrameRate, bitRate: Float, h265On: Bool) {
        if AppContext.shared.isDebugMode {
            return
        }
        ShowSettingKey.videoEncodeSize.writeValue(ShowAgoraVideoDimensions.values().firstIndex(of: encodeSize.sizeValue))
        ShowSettingKey.FPS.writeValue(fpsItems.firstIndex(of: fps))
        ShowSettingKey.videoBitRate.writeValue(bitRate)
        ShowSettingKey.H265.writeValue(h265On)
        ShowSettingKey.lowlightEnhance.writeValue(false)
        ShowSettingKey.colorEnhance.writeValue(false)
        ShowSettingKey.videoDenoiser.writeValue(false)
        
        updateSettingForkey(.videoEncodeSize)
        updateSettingForkey(.videoBitRate)
        updateSettingForkey(.FPS)
        updateSettingForkey(.H265)
        updateSettingForkey(.lowlightEnhance)
        updateSettingForkey(.colorEnhance)
        updateSettingForkey(.videoDenoiser)
        updateSettingForkey(.recordingSignalVolume)
    }

    /// Update configuration information This setting is not saved locally
    /// - Parameters:
    /// -mode: indicates the interaction type of the show
    func updateVideoProfileForMode(_ showMode: ShowMode) {
        let machine = deviceLevel
        let net = netCondition
        let performance = performanceMode
        let h265On = (performance != .pure)
        rtcParam.suggested = true
        if (machine == .high && net == .good && performance != .fluent && showMode == .single) {
            // High-end machine, good network, clear, unicast
            _presetValuesWith(encodeSize: ._1080x1920, fps: .fps24, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .high && net == .good && performance == .fluent && showMode == .single) {
            // High-end machine, good network, smooth, unicast
            _presetValuesWith(encodeSize: ._1080x1920, fps: .fps24, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(540, 960), fps: 15, bitrate: 1100, svc: false)
        } else if (machine == .high && net == .bad && performance != .fluent && showMode == .single) {
            // High-end machine, weak network, clear, unicast
            _presetValuesWith(encodeSize: ._1080x1920, fps: .fps24, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .high && net == .bad && performance == .fluent && showMode == .single) {
            // High-end machine, weak network, smooth, unicast
            _presetValuesWith(encodeSize: ._1080x1920, fps: .fps24, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 748, svc: true)
        } else if (machine == .medium && net == .good && performance != .fluent && showMode == .single) {
            // Mid-range machine, good network, clear, unicast
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps24, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .medium && net == .good && performance == .fluent && showMode == .single) {
            // Mid-range machine, good network, smooth, unicast
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps24, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 680, svc: false)
        } else if (machine == .medium && net == .bad && performance != .fluent && showMode == .single) {
            // Mid-range machine, weak network, clear, unicast
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps24, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .medium && net == .bad && performance == .fluent && showMode == .single) {
            // Mid-range machine, weak network, smooth, unicast
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps24, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 748, svc: true)
        } else if (machine == .low && net == .good && performance != .fluent && showMode == .single) {
            // Low-end machine, good network, clear, unicast
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps15, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .low && net == .good && performance == .fluent && showMode == .single) {
            // Low-end machine, good network, smooth, unicast
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps15, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 680, svc: false)
        } else if (machine == .low && net == .bad && performance != .fluent && showMode == .single) {
            // Low end machine, weak network, clear, unicast
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps15, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .low && net == .bad && performance == .fluent && showMode == .single) {
            // Low end machine, weak network, smooth, unicast
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps15, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 748, svc: true)
        }
        // pk
        else if (machine == .high && net == .good && performance != .fluent && showMode == .pk) {
            // High-end machine, good network, clear, pk
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps15, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .high && net == .good && performance == .fluent && showMode == .pk) {
            // High-end machine, good network, smooth, pk
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps15, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 680, svc: false)
        } else if (machine == .high && net == .bad && performance != .fluent && showMode == .pk) {
            // High-end machine, weak net, clear, pk
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps15, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .high && net == .bad && performance == .fluent && showMode == .pk) {
            // High-end machine, weak net, smooth, pk
            _presetValuesWith(encodeSize: ._720x1280, fps: .fps15, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 680, svc: false)
        } else if (machine == .medium && net == .good && performance != .fluent && showMode == .pk) {
            // Mid-end machine, good net, clear, pk
            _presetValuesWith(encodeSize: ._540x960, fps: .fps15, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .medium && net == .good && performance == .fluent && showMode == .pk) {
            // Mid-end machine, good net, smooth, pk
            _presetValuesWith(encodeSize: ._540x960, fps: .fps15, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 680, svc: false)
        } else if (machine == .medium && net == .bad && performance != .fluent && showMode == .pk) {
            // Mid-end machine, weak net, clear, pk
            _presetValuesWith(encodeSize: ._540x960, fps: .fps15, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .medium && net == .bad && performance == .fluent && showMode == .pk) {
            // Mid-end machine, weak net, smooth, pk
            _presetValuesWith(encodeSize: ._540x960, fps: .fps15, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 680, svc: false)
        } else if (machine == .low && net == .good && performance != .fluent && showMode == .pk) {
            // Low-end machine, good network, clear, pk
            _presetValuesWith(encodeSize: ._540x960, fps: .fps15, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .low && net == .good && performance == .fluent && showMode == .pk) {
            // Low-end machine, good network, smooth, pk
            _presetValuesWith(encodeSize: ._540x960, fps: .fps15, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 680, svc: false)
        } else if (machine == .low && net == .bad && performance != .fluent && showMode == .pk) {
            // Low end machine, weak net, clear, pk
            _presetValuesWith(encodeSize: ._540x960, fps: .fps15, bitRate: 0, h265On: h265On)
            setSimulcastStream(isOn: false)
        } else if (machine == .low && net == .bad && performance == .fluent && showMode == .pk) {
            // Low-end machine, weak net, smooth, pk
            _presetValuesWith(encodeSize: ._540x960, fps: .fps15, bitRate: 0, h265On: true)
            setSimulcastStream(isOn: true, dimensions: CGSizeMake(360, 640), fps: 15, bitrate: 680, svc: false)
        }
    }
    
    /// Update Settings
    /// -Parameter key: specifies the key to be updated
    func updateSettingForkey(_ key: ShowSettingKey, currentChannelId:String? = nil) {
        switch key {
        case .lowlightEnhance:
            let isOn = key.boolValue
            engine?.setLowlightEnhanceOptions(isOn, options: AgoraLowlightEnhanceOptions())
        case .colorEnhance:
            let isOn = key.boolValue
            engine?.setColorEnhanceOptions(isOn, options: AgoraColorEnhanceOptions())
        case .videoDenoiser:
            let isOn = key.boolValue
            setDenoiserOn(isOn)
        case .beauty:
            let isOn = key.boolValue
            engine?.setBeautyEffectOptions(isOn, options: AgoraBeautyOptions())
        case .PVC:
            break
        case .SR:
            break
        case .BFrame:
           break
        case .videoEncodeSize:
            let indexValue = key.intValue
            let dimensions = ShowAgoraVideoDimensions.values()
            let index = indexValue % dimensions.count
            let size = dimensions[index]
            let encoderConfig = getEncoderConfig()
            let captureConfig = getCaptureConfig()
            encoderConfig.dimensions = size
            captureConfig.dimensions = size
            
            if let currentChannelId = currentChannelId{
                engine?.setCameraCapturerConfiguration(captureConfig)
                updateVideoEncoderConfigurationForConnenction(currentChannelId: currentChannelId)
            } else {
                engine?.setCameraCapturerConfiguration(captureConfig)
                engine?.setVideoEncoderConfiguration(encoderConfig)
            }
        case .videoBitRate:
            let sliderValue = key.floatValue
            let encoderConfig = getEncoderConfig()
            encoderConfig.bitrate = Int(sliderValue)
            if let currentChannelId = currentChannelId {
                updateVideoEncoderConfigurationForConnenction(currentChannelId: currentChannelId)
            }else{
                engine?.setVideoEncoderConfiguration(encoderConfig)
            }
        case .FPS:
            let indexValue = key.intValue
            let index = indexValue % fpsItems.count
            let encoderConfig = getEncoderConfig()
            let captureConfig = getCaptureConfig()
            encoderConfig.frameRate = fpsItems[index]
            captureConfig.frameRate = Int32(fpsItems[index].rawValue)
            engine?.setCameraCapturerConfiguration(captureConfig)
            if let currentChannelId = currentChannelId {
                updateVideoEncoderConfigurationForConnenction(currentChannelId: currentChannelId)
            }else{
                engine?.setVideoEncoderConfiguration(encoderConfig)
            }
        case .H265:
            let isOn = key.boolValue
            let encoderConfig = getEncoderConfig()
            encoderConfig.codecType = isOn ? .H265 : .H264
            if let channelId = currentChannelId {
                updateVideoEncoderConfigurationForConnenction(currentChannelId: channelId)
            }
        case .earmonitoring:
            let isOn = key.boolValue
            engine?.enable(inEarMonitoring: isOn)
        case .recordingSignalVolume:
            let value = rtcParam.recordingSignalVolume
            engine?.adjustRecordingSignalVolume(value)
        case .musicVolume:
            let value = rtcParam.musicVolume
            engine?.adjustAudioMixingVolume(value)
        case .audioBitRate:
            break
        }
    }

    func getEncoderConfig() -> AgoraVideoEncoderConfiguration {
        let encoderConfig = AgoraVideoEncoderConfiguration()
        if AppContext.shared.isDebugMode {
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
        let indexValue = ShowSettingKey.videoEncodeSize.intValue
        let dimensions = ShowAgoraVideoDimensions.values()
        let index = indexValue % dimensions.count
        let size = dimensions[index]
        encoderConfig.dimensions = size
        
        let sliderValue = ShowSettingKey.videoBitRate.floatValue
        encoderConfig.bitrate = Int(sliderValue)
        
        let fpsIndex = ShowSettingKey.FPS.intValue
        let idx = fpsIndex % fpsItems.count
        encoderConfig.frameRate = fpsItems[idx]
        
        let isOn = ShowSettingKey.H265.boolValue
        encoderConfig.codecType = isOn ? .H265 : .H264
        return encoderConfig
    }
    
    func getCaptureConfig() -> AgoraCameraCapturerConfiguration {
        let config = AgoraCameraCapturerConfiguration()
        config.followEncodeDimensionRatio = true
        config.cameraDirection = .front
        
        if AppContext.shared.isDebugMode {
            if let encodeWidth: CGFloat = UserDefaults.standard.value(forKey: kEncodeWidth) as? CGFloat ,let encodeHeight: CGFloat = UserDefaults.standard.value(forKey: kEncodeHeight) as? CGFloat {
                config.dimensions = CGSize(width: encodeWidth, height: encodeHeight)
            }
            if let fps: Int = UserDefaults.standard.value(forKey: kEncodeFPS) as? Int {
                config.frameRate = Int32(fps)
            }
            return config
        }
       
        let indexValue = ShowSettingKey.videoEncodeSize.intValue
        let dimensions = ShowAgoraVideoDimensions.values()
        let index = indexValue % dimensions.count
        let size = dimensions[index]
        config.dimensions = size
        
        let fpsIndex = ShowSettingKey.FPS.intValue
        let idx = fpsIndex % fpsItems.count
        config.frameRate = Int32(fpsItems[idx].rawValue)
        
        return config
    }
}
    
// MARK: - Presetting options
extension ShowAgoraKitManager {
    // Default value: Network status
    enum NetCondition: Int {
        case good
        case bad
    }
    
    // Default value: Performance policy
    enum PerformanceMode: Int {
        // Clear strategy
        case smooth
        // Fluency strategy
        case fluent
        
        case pure
    }
    
    // Default value: Device status
    enum DeviceLevel: Int {
        case low = 0
        case medium
        case high
        
        func description() -> String {
            switch self {
            case .high:     return "show_setting_device_level_high".show_localized
            case .medium:   return "show_setting_device_level_mediu".show_localized
            case .low:      return "show_setting_device_level_low".show_localized
            }
        }
    }
}
