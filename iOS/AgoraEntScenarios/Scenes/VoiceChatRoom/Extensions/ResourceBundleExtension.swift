//
//  ResourceBundleExten.swift
//  Pods-VoiceRoomBaseUIKit_Example
//
//Created by Zhu Jichao on August 24, 2022
//

import Foundation

private let voice_bundle = Bundle(path: Bundle.main.path(forResource: "VoiceChatRoomResource", ofType: "bundle") ?? "") ?? Bundle.main
public let spatialResourceBundle = Bundle(path: Bundle.main.path(forResource: "SpatialAudioResource", ofType: "bundle") ?? "") ?? Bundle.main

public extension Bundle {
    static var voiceRoomBundle: Bundle { voice_bundle }
    static var spatialRoomBundle: Bundle { spatialResourceBundle }
}
