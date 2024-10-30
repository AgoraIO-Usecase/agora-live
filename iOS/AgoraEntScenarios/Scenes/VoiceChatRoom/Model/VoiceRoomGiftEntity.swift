//
//  VoiceRoomGiftEntity.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 31, 2022
//

import Foundation
import KakaJSON
import UIKit

@objc open class VoiceRoomGiftEntity: NSObject, Convertible, NSMutableCopying {
    public func mutableCopy(with zone: NSZone? = nil) -> Any {
        let model = VoiceRoomGiftEntity()
        model.gift_id = gift_id
        model.gift_count = gift_count
        model.gift_price = gift_price
        model.gift_name = gift_name
        model.portrait = portrait
        model.userName = userName
        model.selected = selected
        return model
    }

    var gift_id: String? = ""
    var gift_name: String? = ""
    var userName: String? = ""
    var gift_price: String? = ""
    var portrait: String? = ""
    var avatar: UIImage? {
        UIImage.voice_image(portrait ?? "")
    }

    var gift_count: String? = "0"
    var selected = false

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

open class VoiceRoomGiftCount {
    var number: Int
    var selected: Bool

    init(number: Int, selected: Bool) {
        self.number = number
        self.selected = selected
    }
}
