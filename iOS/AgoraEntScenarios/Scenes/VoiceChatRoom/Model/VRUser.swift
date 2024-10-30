//
//  VRUser.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 25, 2022
//

import Foundation
import KakaJSON

@objcMembers open class VRUser: NSObject, Convertible {
    public var uid: String?
    public var chat_uid: String?
    public var channel_id: String?
    public var name: String?
    public var portrait: String? 
    public var invited = false
    public var amount: Int? = 0
    public var mic_index: Int?
    public var rtc_uid: String?
    public var volume: Int = 0 //Microphone volume
    public var micStatus: Int = 1 //1 open microphone 0 autonomous quiet microphone

    override public required init() {
        super.init()
    }
    
    required public init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public func kj_modelKey(from property: Property) -> ModelPropertyKey {
        property.name
    }
}

@objc open class VoiceRoomUserInfo: NSObject {
    public static let shared = VoiceRoomUserInfo()

    public var user: VRUser?

    public var currentRoomOwner: VRUser?
}
