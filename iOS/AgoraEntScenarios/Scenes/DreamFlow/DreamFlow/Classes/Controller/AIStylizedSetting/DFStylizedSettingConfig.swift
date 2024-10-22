//
//  DFStylizedSettingConfig.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/2.
//

import Foundation

class DFStylizedSettingConfig: Codable {
    var prompt: String = ""
    var preset: String = ""
    var style: String = ""
    var strength: Float = 0.1
    var superFrameFactor: Int = 1
    var face_mode: Bool = false
    var style_effect: Bool = false
    var server: String = ""
    var videoWidth: Int = 0
    var videHeight: Int = 0
    var aiToken: String = ""
}
