//
//  DFStylizedSettingItem.swift
//  DreamFlow
//
//  Created by qinhui on 2024/8/30.
//

import Foundation

class DFStylizedSettingItem {
    var title: String
    var imageName: String
    var content: String
    
    init(title: String, imageName: String, content: String) {
        self.title = title
        self.imageName = imageName
        self.content = content
    }
}

class DFPresetSettingItem: DFStylizedSettingItem {
    var faceMode: Bool
    var strengthDefaultValue: Float
    var strengthMinValue: Float = 0.1
    var strengthMaxValue: Float = 1
    var superFrameMinValue: Float = 0.1
    var superFrameMaxvalue: Float = 2
    var superFrameDefaultValue: Int = 1
    var defaultStyleIndex: Int
    var isCustomPreset: Bool
    
    init(title: String, imageName: String, content: String, faceMode: Bool, strengthDefaultValue: Float, styleIndex: Int = 0, isCustom: Bool = false) {
        self.faceMode = faceMode
        self.strengthDefaultValue = strengthDefaultValue
        self.defaultStyleIndex = styleIndex
        self.isCustomPreset = isCustom
        super.init(title: title, imageName: imageName, content: content)
    }
}
