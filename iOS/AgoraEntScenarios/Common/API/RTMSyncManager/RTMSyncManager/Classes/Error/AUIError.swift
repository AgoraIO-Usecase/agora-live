//
//  AUIError.swift
//  AUIKit
//
//  Created by wushengtao on 2023/3/29.
//

import Foundation

public enum AUICommonError {
    case unknown      //Unknown error
    case rtcError(Int32)    //Rtc error
    case rtmError(Int32)    //Rtm error
    case rtmNotPresence   //rtm presence error
    case httpError(Int, String)  //http error
    case networkParseFail   //Http response parsing error
    case missmatchRoomConfig  //Can't find the corresponding room token information
    case micSeatNotIdle   //The mic seat is not free.
    case micSeatAlreadyEnter   //It has been on the mic seat.
    case userNoEnterSeat   //The audience is not enter seat.
    case noPermission   //No permission
    case noResponse    //No response
    
    public func toNSError() -> NSError {
        func createError(code: Int = -1, msg: String) -> NSError {
            return NSError(domain: "AUIKit Error", code: Int(code), userInfo: [ NSLocalizedDescriptionKey : msg])
        }
        switch self {
        case .httpError(let error, let msg):
            if error == 10001 {
                return createError(code: Int(error), msg: "the room has been destroyed")
            }
            return createError(code: Int(error), msg: msg)
        case .rtcError(let error):
            return createError(code: Int(error), msg: "rtc operation fail: \(error)")
        case .rtmError(let error):
            return createError(code: Int(error), msg: "rtm error: \(error)")
        case .rtmNotPresence:
            return createError(msg: "rtm fail: not presence")
        case .networkParseFail:
            return createError(msg: "http parse fail")
        case .missmatchRoomConfig:
            return createError(msg: "room config missmatch")
        case .micSeatNotIdle:
            return createError(msg: "mic seat not idle")
        case .micSeatAlreadyEnter:
            return createError(msg: "user already enter seat")
        case .userNoEnterSeat:
            return createError(msg: "user not enter seat")
//        case .chooseSongAlreadyExist:
//            return createError(msg: "already choose song")
//        case .chooseSongNotExist:
//            return createError(msg: "song not exist")
//        case .choristerAlreadyExist:
//            return createError(msg: "chorister already exist")
//        case .choristerNotExist:
//            return createError(msg: "chorister not exist")
        case .noPermission:
            return createError(msg: "no permission")
//        case .chooseSongIsFail:
//            return createError(msg: "choost song model fail")
        case .noResponse:
            return createError(msg: "no response")
        default:
            return createError(msg: "unknown error")
        }
    }
}

extension NSError {
    static func auiError(_ description: String) -> NSError {
        return NSError(domain: "AUIKit Error",
                       code: -1,
                       userInfo: [ NSLocalizedDescriptionKey : description])
    }
}
