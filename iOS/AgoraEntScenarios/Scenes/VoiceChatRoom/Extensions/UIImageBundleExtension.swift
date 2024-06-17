//
//  UIImageBundleExtension.swift
//  Pods-VoiceRoomBaseUIKit_Example
//
//Created by Zhu Jichao on August 24, 2022
//

import Foundation
import UIKit

public extension UIImage {
    
    static func voice_image(_ named: String) -> UIImage? {
        if #available(iOS 13.0, *) {
            return self.init(named: named, in: Bundle.voiceRoomBundle, with: nil)
        } else {
            return self.init(named: named, in: Bundle.voiceRoomBundle, compatibleWith: nil)
        }
    }

}
