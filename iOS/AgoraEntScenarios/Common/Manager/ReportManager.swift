//
//  ReportManager.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/12/4.
//

import Foundation
import AgoraRtcKit

@objc class ReportManager: NSObject {
   @objc public static func messageReport(rtcEngine: AgoraRtcEngineKit, type: Int) {
        func convertToJSONString(_ dictionary: [String: Any]) -> String? {
            if let jsonData = try? JSONSerialization.data(withJSONObject: dictionary, options: .prettyPrinted) {
                return String(data: jsonData, encoding: .utf8)
            }
            return nil
        }
        
        func getCurrentTs() -> Int {
            return Int(Date().timeIntervalSince1970)
        }
        
        rtcEngine.setParameters("{\"rtc.direct_send_custom_event\": true}")
        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] ?? ""
        let messageId = "agora:agoralive"
        let category = "\(type)_iOS_\(appVersion)"

        let eventMap: [String: Any] = ["type": type]
        let value = VLUserCenter.user.userNo
        let labelMap: [String: Any] = ["uid": value, "ts": getCurrentTs()]

        let event = convertToJSONString(eventMap) ?? ""
        let label = convertToJSONString(labelMap) ?? ""
        
        rtcEngine.sendCustomReportMessage(messageId, category: category, event: event, label: label, value: 0)
    }
}
