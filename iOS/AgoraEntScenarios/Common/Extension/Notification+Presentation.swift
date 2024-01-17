//
//  VoiceRoomAlertViewController.swift
//  VoiceRoomBaseUIKit
//
// Created by Zhu Jichao on August 30, 2022
//

import Foundation

public extension Notification {
    var keyboardEndFrame: CGRect? {
        return (userInfo?[UIApplication.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue
    }
    var keyboardAnimationDuration: TimeInterval? {
        return (userInfo?[UIApplication.keyboardAnimationDurationUserInfoKey] as? NSNumber)?.doubleValue
    }
}
