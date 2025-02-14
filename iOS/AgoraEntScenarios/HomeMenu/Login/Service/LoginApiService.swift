//
//  LoginApiService.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/11/26.
//

import Foundation

struct SSOUserInfoResponse: Codable {
    let accountUid: String
    let accountType: String
    let email: String
    let companyId: Int
    let profileId: Int
    let displayName: String
    let companyName: String
    let companyCountry: String
    let invitationCode: String
}

struct InvitationCodeUserInfoResponse: Codable {
    let accountUid: String
}

struct InvitationCodeLoginResponse: Codable {
    let token: String
    let type: String
}

@objc class LoginApiService: NSObject {
    @objc static func loginWithInvitationCode(code: String, callback: ((Error?)->Void)?) {
        let loginModel = InviteCodeLoginModel()
        loginModel.invitationCode = code
        loginModel.accountUid = UUID().uuidString
        loginModel.request { error, res in
            if let err = error {
                callback?(err)
                return
            }
            
            guard let res = res else {
                callback?(NSError.init(domain: "response is empty", code: -1))
                return
            }
            
            if let jsonData = try? JSONSerialization.data(withJSONObject: res, options: []) {
                do {
                    let response = try JSONDecoder().decode(InvitationCodeLoginResponse.self, from: jsonData)
                    let model = VLLoginModel()
                    model.token = response.token
                    model.type = .CODELOGIN
                    VLUserCenter.shared().storeUserInfo(model)
                    print(model)
                    callback?(nil)
                } catch {
                    callback?(NSError.init(domain: "Failed to decode JSON", code: -1))
                    print("Failed to decode JSON: \(error)")
                }
            } else {
                callback?(NSError.init(domain: "Failed to convert Any to Data", code: -1))
            }
        }
    }
    
    @objc static func getInvitationCodeUserInfo(callback: ((Error?)->Void)?) {
        let userInfoModel = InvitationCodeUserInfoModel()
        userInfoModel.request { error, res in
            if let err = error {
                callback?(err)
                return
            }
            
            guard let res = res else {
                callback?(NSError.init(domain: "user info is empty", code: -1))
                return
            }
            
            if let jsonData = try? JSONSerialization.data(withJSONObject: res, options: []) {
                do {
                    let userInfo = try JSONDecoder().decode(InvitationCodeUserInfoResponse.self, from: jsonData)
                    let model = VLLoginModel()
                    let hashValue = abs(userInfo.accountUid.hashValue)
                    let hashString = String(hashValue)
                    let uid = String(hashString.suffix(4))
                    model.token = VLUserCenter.user.token
                    model.name = LoginManager.shared.getRandomName()
                    model.headUrl = LoginManager.shared.getRandomAvatar()
                    model.userNo = uid
                    model.chat_uid = uid
                    model.rtc_uid = uid
                    model.id = uid
                    model.type = .CODELOGIN
                    VLUserCenter.shared().storeUserInfo(model)
                    print(userInfo)
                    callback?(nil)
                } catch {
                    callback?(NSError.init(domain: "Failed to decode JSON", code: -1))
                    print("Failed to decode JSON: \(error)")
                }
            } else {
                callback?(NSError.init(domain: "Failed to convert Any to Data", code: -1))
            }
        }
    }
    
    @objc static func getUserInfo(callback: ((Error?)->Void)?) {
        let userInfoModel = SSOUserInfoModel()
        userInfoModel.request { error, res in
            if let err = error {
                callback?(err)
                return
            }
            
            guard let res = res else {
                callback?(NSError.init(domain: "user info is empty", code: -1))
                return
            }
            
            if let jsonData = try? JSONSerialization.data(withJSONObject: res, options: []) {
                do {
                    let userInfo = try JSONDecoder().decode(SSOUserInfoResponse.self, from: jsonData)
                    let model = VLLoginModel()
                    let hashValue = abs(userInfo.accountUid.hashValue)
                    let hashString = String(hashValue)
                    let uid = String(hashString.suffix(9))
                    model.token = VLUserCenter.user.token
                    model.name = userInfo.displayName
                    model.headUrl = LoginManager.shared.getRandomAvatar()
                    model.userNo = uid
                    model.chat_uid = uid
                    model.rtc_uid = uid
                    model.id = uid
                    model.type = .SSOLOGIN
                    model.invitationCode = userInfo.invitationCode
                    VLUserCenter.shared().storeUserInfo(model)
                    print(userInfo)
                    callback?(nil)
                } catch {
                    callback?(NSError.init(domain: "Failed to decode JSON", code: -1))
                    print("Failed to decode JSON: \(error)")
                }
            } else {
                callback?(NSError.init(domain: "Failed to convert Any to Data", code: -1))
            }
        }
    }
}
