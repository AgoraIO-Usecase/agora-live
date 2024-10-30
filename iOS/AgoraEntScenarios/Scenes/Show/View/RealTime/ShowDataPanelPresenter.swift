//
//  ShowStatisticsInfo.swift
//  AgoraEntScenarios
//
//  Created by HeZhengQing on 2022/11/9.
//

import Foundation
import AgoraRtcKit
import AgoraCommon

struct ShowPanelData {
    let left: String
    let right: String
}

class ShowDataPanelPresenter {
    
    private var channelStats = AgoraChannelStats()
    private var localVideoStats = AgoraRtcLocalVideoStats()
    private var localAudioStats = AgoraRtcLocalAudioStats()
    private var remoteVideoStats = AgoraRtcRemoteVideoStats()
    private var remoteAudioStats = AgoraRtcRemoteAudioStats()
    private var uplink: Int32 = 0
    private var downlink: Int32 = 0
    private var callTs: Int = 0

    func updateChannelStats(_ stats: AgoraChannelStats) {
        channelStats = stats
    }
    
    func updateLocalVideoStats(_ stats: AgoraRtcLocalVideoStats) {
        localVideoStats = stats
    }
    
    func updateLocalAudioStats(_ stats: AgoraRtcLocalAudioStats) {
        localAudioStats = stats
    }
    
    func updateUplinkNetworkInfo(_ uplinkNetworkInfo: AgoraUplinkNetworkInfo) {
        uplink = uplinkNetworkInfo.videoEncoderTargetBitrateBps / 8 / 1024
    }
    
    func updateDownlinkNetworkInfo(_ downlinkNetworkInfo: AgoraDownlinkNetworkInfo) {
        downlink = downlinkNetworkInfo.bandwidthEstimationBps / 8 / 1024
    }
    
    func updateVideoStats(_ stats: AgoraRtcRemoteVideoStats) {
        remoteVideoStats = stats
    }
    
    func updateAudioStats(_ stats: AgoraRtcRemoteAudioStats) {
        remoteAudioStats = stats
    }
    
    func updateTimestamp(_ ts: TimeInterval) {
        callTs = Int(ts)
    }
    
    func generatePanelData(send: Bool, 
                           receive: Bool, 
                           audience: Bool,
                           isInteractionUser: Bool) -> [ShowPanelData] {
        let sendPanel = send ? sendData() : cleanSendData()
        let receivePanel = receive ? receiveData() : cleanReceiveData()
        let otherPanel = otherData(send: send, receive: receive, audience: audience)
        var datas: [ShowPanelData] = []
        if isInteractionUser {
            datas = [sendPanel, receivePanel, otherPanel]
        } else {
            if audience {
                datas = [receivePanel, otherPanel]
            } else {
                datas = [sendPanel, otherPanel]
            }
        }
        
        return datas
    }
    
    private func cleanReceiveData() -> ShowPanelData {
        let sendTitle = "show_statistic_receive_title".show_localized
        let videoSize = "show_statistic_receive_resolution".show_localized+": --"
        let videoSend = "show_statistic_bitrate".show_localized+": -- kbps"
        let downlink = "show_statistic_down_net_speech".show_localized+": \(downlink) KB/s"

        let fps = "show_statistic_receive_fps".show_localized+": -- fps"
        let vSendLoss = "show_statistic_down_loss_package".show_localized+": -- %"
        let lastmile = "show_statistic_delay".show_localized+": -- ms"
        
        let leftInfo = [sendTitle, videoSize, videoSend, downlink].joined(separator: "\n") + "\n"
        let rightInfo = ["   ", fps, vSendLoss, lastmile].joined(separator: "\n") + "\n"

        return ShowPanelData(left: leftInfo, right: rightInfo)
    }
    
    private func cleanSendData() -> ShowPanelData {
        let sendTitle = "show_statistic_send_title".show_localized
        let videoSize = "show_statistic_encode_resolution".show_localized+": --"
        let videoSend = "show_statistic_up_bitrate".show_localized+": -- kbps"
        let uplink = "show_statistic_up_net_speech".show_localized+": \(uplink) KB/s"
        
        let fps = "show_advance_setting_FPS_title".show_localized+": -- fps"
        let vSendLoss = "show_statistic_up_loss_package".show_localized+": -- %"
        
        let leftInfo =  [sendTitle, videoSize, videoSend, uplink].joined(separator: "\n") + "\n"
        let rightInfo = ["  ", fps, vSendLoss].joined(separator: "\n") + "\n"
        return ShowPanelData(left: leftInfo, right: rightInfo)
    }
    
    private func sendData() -> ShowPanelData {
        let sendTitle = "show_statistic_send_title".show_localized
        let videoSize = "show_statistic_encode_resolution".show_localized+": \(localVideoStats.encodedFrameHeight)x\(localVideoStats.encodedFrameWidth)"
        let videoSend = "show_statistic_up_bitrate".show_localized+": \(localVideoStats.sentBitrate) kbps"
        let uplink = "show_statistic_up_net_speech".show_localized+": \(uplink) KB/s"
        
        let fps = "show_advance_setting_FPS_title".show_localized+": \(localVideoStats.sentFrameRate) fps"
        let vSendLoss = "show_statistic_up_loss_package".show_localized+": \(localVideoStats.txPacketLossRate) %"
                
        let leftInfo =  [sendTitle, videoSize, videoSend, uplink].joined(separator: "\n") + "\n"
        let rightInfo = ["   ", fps, vSendLoss].joined(separator: "\n") + "\n"

        return ShowPanelData(left: leftInfo, right: rightInfo)
    }
    
    private func receiveData() -> ShowPanelData {
        let sendTitle = "show_statistic_receive_title".show_localized
        let videoSize = "show_statistic_receive_resolution".show_localized+": \(remoteVideoStats.height)x\(remoteVideoStats.width)"
        let videoSend = "show_statistic_bitrate".show_localized+": \(remoteVideoStats.receivedBitrate) kbps"
        let downlink = "show_statistic_down_net_speech".show_localized+": \(downlink) KB/s"

        let fps = "show_statistic_receive_fps".show_localized+": \(remoteVideoStats.rendererOutputFrameRate) fps"
        let vSendLoss = "show_statistic_down_loss_package".show_localized+": \(remoteVideoStats.packetLossRate) %"
        let lastmile = "show_statistic_delay".show_localized+": \(remoteVideoStats.delay) ms"
        
        let leftInfo = [sendTitle, videoSize, videoSend, downlink].joined(separator: "\n") + "\n"
        let rightInfo = ["  ", fps, vSendLoss, lastmile].joined(separator: "\n") + "\n"

        return ShowPanelData(left: leftInfo, right: rightInfo)
    }
    
    private func otherData(send: Bool, receive: Bool, audience: Bool) -> ShowPanelData {
        let params = ShowAgoraKitManager.shared.rtcParam
        let onStr = "show_setting_switch_on".show_localized
        let offStr = "show_setting_switch_off".show_localized
        // left:
        // others
        let title = "show_statistic_title_other".show_localized
        // fast open time
        let startup = audience ? "\(callTs) ms" : "--"
        let startupStr = "show_statistic_startup_time".show_localized + ": " + startup
        // h265 switch
        var encoderStr = "Encoder: --"
        if send {
            switch localVideoStats.codecType {
            case .H265:
                encoderStr = "Encoder: H265"
            case .AV1:
                encoderStr = "Encoder: AV1"
            default:
                encoderStr = "Encoder: H264"
            }
        }
        
        // super resolution switch
        let sr = audience ? (params.sr ? onStr : offStr) : "--"
        let srStr = "show_statistic_SR_switch".show_localized + ": " + sr
        // micro stream switch
        let microStream = send ? ((localVideoStats.dualStreamEnabled) ? onStr : offStr) : "--"
        let microStreamStr = "show_statistic_micro_stream_switch".show_localized + ": " + microStream
        // right:
        // device cpu level
        let levelStr = "show_statistic_device_level".show_localized
        + ": "
        + ShowAgoraKitManager.shared.deviceLevel.description()
        + "(\(ShowAgoraKitManager.shared.deviceScore))"
        //pvc switch
        let pvc = send ? (params.pvc ? onStr : offStr) : "--"
        let pvcStr = "show_statistic_pvc_switch".show_localized + ": " + pvc
        //svc switch
        let svc = send ? (params.svc ? onStr : offStr) : "--"
        let svcStr = "show_statistic_svc_switch".show_localized + ": " + svc
        let localUid = "Local UID: \(VLUserCenter.user.id)"
        if audience {
            let left = [title, startupStr, localUid, levelStr, srStr].joined(separator: "\n") + "\n"
            let right = ""
            return ShowPanelData(left: left, right: right)
        } else {
            let left = [title, localUid, svcStr, levelStr, microStreamStr].joined(separator: "\n") + "\n"
            let right = ["  ", encoderStr, pvcStr].joined(separator: "\n") + "\n"
            return ShowPanelData(left: left, right: right)
        }
    }
}
