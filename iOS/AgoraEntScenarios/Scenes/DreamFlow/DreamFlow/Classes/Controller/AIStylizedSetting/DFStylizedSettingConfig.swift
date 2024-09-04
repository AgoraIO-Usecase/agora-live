//
//  DFStylizedSettingConfig.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/2.
//

import Foundation

class DFStylizedSettingConfig: Codable {
    var prompt: String = ""
    var effect: String = ""
    var style: String = ""
    var strength: Float = 0.1
    var face_mode: Bool = false
    var style_effect: Bool = false
}
