//
//  LoginManager.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/11/26.
//

import Foundation

enum userType {
    case sso
    case inviteCode
}

protocol LoginManagerProtocol {
    func getUserType() -> userType
    func generateCode()
    func hasGeneratedCode() -> Bool
    func getRandomName() -> String
    func getRandomAvatar() -> String
}

class LoginManager: NSObject {
    private let generateFlag = "generateFlag"
    static let shared = LoginManager()
}

extension LoginManager: LoginManagerProtocol {
    func generateCode() {
        UserDefaults.standard.set(true, forKey: generateFlag)
    }
    
    func getUserType() -> userType {
        return .sso
    }
    
    func hasGeneratedCode() -> Bool {
        return UserDefaults.standard.bool(forKey: generateFlag)
    }
    
    func getRandomName() -> String {
        let names = ["Ezra", "Pledge", "Bonnie", "Seeds", "Shannon", "Red-Haired", "Montague", "Primavera", "Lucille", "Tess"]
        return names.randomElement() ?? ""
    }
    
    func getRandomAvatar() -> String {
        return "avatar_\(Int.random(in: 1...4))"
    }
}
