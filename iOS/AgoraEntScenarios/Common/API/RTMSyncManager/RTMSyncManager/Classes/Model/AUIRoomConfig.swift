//
//  AUIRoomConfig.swift
//  AUIKit
//
//  Created by wushengtao on 2023/2/24.
//

import Foundation

@objcMembers
open class AUICommonConfig: NSObject {
    /// Agora AppId
    public var appId: String = ""
    /// Agora App Certificate (optional, if you don't need the back-end token generation service, you can not set it)
    public var appCert: String = ""
    
    /// Huanxin AppKey(Optional, if you don't need the back-end IM service, you can not set it)
    public var imAppKey: String = ""
    /// Huanxin ClientId (optional, if you don't need the back-end IM service, you can not set it)
    public var imClientId: String = ""
    /// Huanxin ClientSecret (optional, if you don't use the back-end IM service, you can not set it)
    public var imClientSecret: String = ""
    
    /// Domain name (optional, if you don't need the back-end service, you can not set it)
    public var host: String = "" //(optional)
    /// User information
    public var owner: AUIUserThumbnailInfo?
    
    public func isValidate() -> Bool {
        if appId.isEmpty || owner == nil  {
            return false
        }
        
        return true
    }
}


