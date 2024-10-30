//
//  VoiceLog.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/8/6.
//

import Foundation
import AgoraCommon

public class VoiceLogger: NSObject {
    
    public static let kLogKey = "VoiceChat"
    
    public static func info(_ text: String, context: String? = nil) {
        agoraDoMainThreadTask {
            AgoraEntLog.getSceneLogger(with: kLogKey).info(text, context: context)
        }
    }
    
    public static func warning(_ text: String, context: String? = nil) {
        warn(text, context: context)
    }

    public static func warn(_ text: String, context: String? = nil) {
        agoraDoMainThreadTask {
            AgoraEntLog.getSceneLogger(with: kLogKey).warning(text, context: context)
        }
    }

    public static func error(_ text: String, context: String? = nil) {
        agoraDoMainThreadTask {
            AgoraEntLog.getSceneLogger(with: kLogKey).error(text, context: context)
        }
    }
}
