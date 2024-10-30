//
//  VoiceRoomAlertViewController.swift
//  VoiceRoomBaseUIKit
//
// Created by Zhu Jichao on August 30, 2022
//

import Foundation

// /Pan gesture sliding direction
///
// /- Bottom: Down
// /- Top: Up
// /- left: Left
// /- Right: To the right
public enum SAPanDismissDirection {
    case down
    case up
    case left
    case right
}

// /The starting position of the presentation
///
// /- Center: Screen center
// /- bottomOutOfLine: Below the bottom of the screen
// /- leftOutOfLine: Outside the left side of the screen
// /- rightOutOfLine: Outside the right side of the screen
// /- topOutOfLine: Above the top of the screen
// /- custom: custom center point
public enum SAPresentationOrigin: Equatable {
    case center
    case bottomOutOfLine
    case leftOutOfLine
    case rightOutOfLine
    case topOutOfLine
    case custom(center: CGPoint)

    // MARK: -  Equatable

    public static func == (lhs: SAPresentationOrigin, rhs: SAPresentationOrigin) -> Bool {
        switch (lhs, rhs) {
        case (.center, .center):
            return true
        case (.bottomOutOfLine, .bottomOutOfLine):
            return true
        case (.leftOutOfLine, .leftOutOfLine):
            return true
        case (.rightOutOfLine, .rightOutOfLine):
            return true
        case (.topOutOfLine, .topOutOfLine):
            return true
        case let (.custom(lhsCenter), .custom(rhsCenter)):
            return lhsCenter == rhsCenter
        default:
            return false
        }
    }
}

// /The final position of the presentation
///
// /- Center: Screen center
// /- bottomBaseline: Based on the bottom of the screen
// /- leftBaseline: Based on the left side of the screen
// /- rightBaseline: Based on the right side of the screen
// /- topBaseline: Based on the upper part of the screen
// /- custom: custom center point
public enum SAPresentationDestination: Equatable {
    case center
    case bottomBaseline
    case leftBaseline
    case rightBaseline
    case topBaseline
    case custom(center: CGPoint)

    // /Pan gesture direction
    var panDirection: SAPanDismissDirection {
        switch self {
        case .center, .bottomBaseline, .custom:
            return .down
        case .leftBaseline:
            return .left
        case .rightBaseline:
            return .right
        case .topBaseline:
            return .up
        }
    }

    // /Default starting position
    var defaultOrigin: SAPresentationOrigin {
        switch self {
        case .center:
            return .center
        case .leftBaseline:
            return .leftOutOfLine
        case .rightBaseline:
            return .rightOutOfLine
        case .topBaseline:
            return .topOutOfLine
        default:
            return .bottomOutOfLine
        }
    }

    // MARK: -  Equatable

    public static func == (lhs: SAPresentationDestination, rhs: SAPresentationDestination) -> Bool {
        switch (lhs, rhs) {
        case (.center, .center):
            return true
        case (.bottomBaseline, .bottomBaseline):
            return true
        case (.leftBaseline, .leftBaseline):
            return true
        case (.rightBaseline, .rightBaseline):
            return true
        case (.topBaseline, .topBaseline):
            return true
        case let (.custom(lhsCenter), .custom(rhsCenter)):
            return lhsCenter == rhsCenter
        default:
            return false
        }
    }
}

// /Transition animation types
///
// /- Translation: Translation
// /- crossDissolve: Fade in/out
// /- crossZoom: Zoom
// /- FlipHorizontal: Flip horizontally
// /- custom: custom animation
public enum SATransitionType: Equatable {
    case translation(origin: SAPresentationOrigin)
    case crossDissolve
    case crossZoom
    case flipHorizontal
    case custom(animation: SAPresentationAnimation)

    var animation: SAPresentationAnimation {
        switch self {
        case let .translation(origin):
            return SAPresentationAnimation(origin: origin)
        case .crossDissolve:
            return SACrossDissolveAnimation()
        case .crossZoom:
            return SACrossZoomAnimation(scale: 0.1)
        case .flipHorizontal:
            return SAFlipHorizontalAnimation()
        case let .custom(animation):
            return animation
        }
    }

    // MARK: -  Equatable

    public static func == (lhs: SATransitionType, rhs: SATransitionType) -> Bool {
        switch (lhs, rhs) {
        case let (.translation(lhsOrigin), .translation(rhsOrigin)):
            return lhsOrigin == rhsOrigin
        case (.crossDissolve, .crossDissolve):
            return true
        case (.flipHorizontal, .flipHorizontal):
            return true
        case (.crossZoom, .crossZoom):
            return true
        case let (.custom(lhsAnimation), .custom(rhsAnimation)):
            return lhsAnimation == rhsAnimation
        default:
            return false
        }
    }
}

// /Animation Options Settings
///
// /- normal: Normal type
// /- Spring: Spring type
public enum SAAnimationOptions {
    case normal(duration: TimeInterval)
    case spring(duration: TimeInterval, delay: TimeInterval, damping: CGFloat, velocity: CGFloat)

    var duration: TimeInterval {
        switch self {
        case let .normal(duration):
            return duration
        case let .spring(duration, _, _, _):
            return duration
        }
    }
}

// /The translation method of the keyboard
///
// /- unabgeshirmt: does not obscure PresentedView, compress: does the keyboard fit close to PresentedView
// /- compressInputView: Close to input box
public enum SAKeyboardTranslationType {
    case unabgeschirmt(compress: Bool)
    case compressInputView
}
