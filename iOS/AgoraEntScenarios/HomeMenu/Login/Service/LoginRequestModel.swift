//
//  LoginRequestModel.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/11/26.
//

import Foundation

@objcMembers
class LoginCommonModel: AUINetworkModel {
    override init() {
        super.init()
        host = KeyCenter.releaseBaseServerUrl ?? ""
    }
    
    public override func parse(data: Data?) throws -> Any? {
        var dic: Any? = nil
        do {
            try dic = super.parse(data: data)
        } catch let err {
            throw err
        }
        guard let dic = dic as? [String: Any] else {
            throw AUICommonError.networkParseFail.toNSError()
        }
        
        let message = dic["message"] as? String ?? ""
        if message == "unauthorized" {
            self.tokenExpired()
            throw AUICommonError.httpError(401, message).toNSError()
        }
        return dic["data"]
    }
}

@objcMembers
class InviteCodeLoginModel: LoginCommonModel {
    var invitationCode: String?
    var accountUid: String?
    
    override init() {
        super.init()
        method = .post
        interfaceName = "sso/invitationLogin"
    }
}

@objcMembers
class InvitationCodeUserInfoModel: LoginCommonModel {
    override init() {
        super.init()
        method = .get
        interfaceName = "sso/invitationUserInfo"
    }
    
    func getToken() -> String {
        if VLUserCenter.shared().isLogin() {
            return VLUserCenter.user.token
        }
        return ""
    }
    
    public override func getHeaders() -> [String : String] {
        var headers = super.getHeaders()
        headers["Authorization"] = "Bearer " + self.getToken()
        return headers
    }
}

@objcMembers
class SSOUserInfoModel: LoginCommonModel {
    override init() {
        super.init()
        method = .get
        interfaceName = "sso/userInfo"
    }
    
    func getToken() -> String {
        if VLUserCenter.shared().isLogin() {
            return VLUserCenter.user.token
        }
        return ""
    }
    
    public override func getHeaders() -> [String : String] {
        var headers = super.getHeaders()
        headers["Authorization"] = "Bearer " + self.getToken()
        return headers
    }
}
