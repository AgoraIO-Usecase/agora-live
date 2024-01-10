//
//  VoiceRoomBusinessRequest.swift
//  VoiceRoomBaseUIKit-VoiceRoomBaseUIKit
//
//  Created by 朱继超 on 2022/8/29.
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
