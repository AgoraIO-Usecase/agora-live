//
//  VoiceRoomBusinessRequest.swift
//  VoiceRoomBaseUIKit-VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 29, 2022
//

import KakaJSON
import UIKit
import ZSwiftBaseLib

public class VoiceRoomError: Error, Convertible {
    var code: String?
    var message: String?

    public required init() {}

    public func kj_modelKey(from property: Property) -> ModelPropertyKey {
        property.name
    }
}
