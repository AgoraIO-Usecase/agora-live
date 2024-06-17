//
//  AUIError.swift
//  AUIKit
//
//  Created by wushengtao on 2023/3/29.
//

import Foundation

public enum AUICommonError {
    case unknown      // Unknown error
    case rtcError(Int32)    // RTC error
    case rtmError(Int32)    // RTM error
    case rtmNotPresence   // RTM presence error
    case httpError(Int, String)  // HTTP error
    case networkParseFail   // HTTP response parsing error
    case missmatchRoomConfig  // Unable to find corresponding room token information
    
    public func toNSError() -> NSError {
        switch self {
        case .httpError(let error, let msg):
            if error == 10001 {
                return NSError(domain: "AUIKit Error", code: Int(error), userInfo: [ NSLocalizedDescriptionKey : "the room has been destroyed"])
            }
            return NSError(domain: "AUIKit Error", code: Int(error), userInfo: [ NSLocalizedDescriptionKey : msg])
        case .rtcError(let error):
            return NSError(domain: "AUIKit Error", code: Int(error), userInfo: [ NSLocalizedDescriptionKey : "rtc operation fail: \(error)"])
        case .rtmError(let error):
            return NSError(domain: "AUIKit Error", code: Int(error), userInfo: [ NSLocalizedDescriptionKey : "rtm error: \(error)"])
        case .rtmNotPresence:
            return NSError(domain: "AUIKit Error", code: -1, userInfo: [ NSLocalizedDescriptionKey : "rtm fail: not presence"])
        case .networkParseFail:
            return NSError(domain: "AUIKit Error", code: -1, userInfo: [ NSLocalizedDescriptionKey : "http parse fail"])
        case .missmatchRoomConfig:
            return NSError(domain: "AUIKit Error", code: -1, userInfo: [ NSLocalizedDescriptionKey : "room config missmatch"])
        default:
            return NSError(domain: "AUIKit Error", code: -1, userInfo: [ NSLocalizedDescriptionKey : "unknown error"])
        }
    }
}
