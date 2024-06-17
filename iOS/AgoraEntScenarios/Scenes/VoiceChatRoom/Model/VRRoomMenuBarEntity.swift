//
//  VRRoomMenuBarEntity.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 24, 2022
//

import Foundation

@objcMembers open class VRRoomMenuBarEntity: Codable {
    var title: String = ""
    var detail: String = ""
    var selected: Bool = false
    var soundType: Int = 0
    var index: Int? = 0
}
