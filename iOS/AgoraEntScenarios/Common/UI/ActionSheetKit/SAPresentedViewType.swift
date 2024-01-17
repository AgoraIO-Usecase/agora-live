//
//  VoiceRoomAlertViewController.swift
//  VoiceRoomBaseUIKit
//
// Created by Zhu Jichao on August 30, 2022
//

import Foundation

// /Settings for presentedView
public struct SAPresentedViewComponent {
    // /The size of presentedView
    public var contentSize: CGSize

    // /PresentedView final display location
    public var destination: SAPresentationDestination = .bottomBaseline

    // /Present transition animation, using nil based on destination
    public var presentTransitionType: SATransitionType?

    // /Dismiss transition animation, using nil based on destination
    public var dismissTransitionType: SATransitionType?

    // /Do you want to enable background display when clicking
    public var canTapBGDismiss: Bool = true

    // /Do you want to enable pan gesture display
    public var canPanDismiss: Bool = true

    // /Pan gesture direction, for nil, use based on destination
    public var panDismissDirection: SAPanDismissDirection?

    // /The translation method of the keyboard, which is close to PresentedView by default
    public var keyboardTranslationType: SAKeyboardTranslationType = .unabgeschirmt(compress: true)

    // /Keyboard spacing, default to 20
    public var keyboardPadding: CGFloat = 0

    // /Initialization method
    ///
    /// - Parameters:
    // /- contentSize: The size of presentedView
    // /- Destination: presentedView
    // /- presentTransitionType: present transition animation
    // /- dispissTransitionType: dispissTransitionType animation
    // /- canTapBGDismiss: Is clicking background Dismiss enabled
    // /- canPanDismiss: Is pan gesture Dismiss enabled
    // /- panDisassisDirection: Pan gesture direction
    // /- keyboardTranslationType: The translation method that appears on the keyboard, which is close to PresentedView by default
    // /- keyboardPadding: keyboard spacing, default to 20
    public init(contentSize: CGSize,
                destination: SAPresentationDestination = .bottomBaseline,
                presentTransitionType: SATransitionType? = nil,
                dismissTransitionType: SATransitionType? = nil,
                canTapBGDismiss: Bool = true,
                canPanDismiss: Bool = true,
                panDismissDirection: SAPanDismissDirection? = nil,
                keyboardTranslationType: SAKeyboardTranslationType = .unabgeschirmt(compress: true),
                keyboardPadding: CGFloat = 0)
    {
        self.contentSize = contentSize
        self.destination = destination
        self.presentTransitionType = presentTransitionType
        self.dismissTransitionType = dismissTransitionType
        self.canTapBGDismiss = canTapBGDismiss
        self.canPanDismiss = canPanDismiss
        self.panDismissDirection = panDismissDirection
        self.keyboardTranslationType = keyboardTranslationType
        self.keyboardPadding = keyboardPadding
    }
}

// /PresentedView must comply with this agreement
public protocol SAPresentedViewType {
    // /Settings for presentedView
    var presentedViewComponent: SAPresentedViewComponent? { get set }
}

extension SAPresentedViewType {
    var presentTransitionType: SATransitionType {
        if self is UINavigationController {
            let vc = (self as? UINavigationController)?.viewControllers.last as? SAAlertViewController
            if vc?.presentedViewComponent?.presentTransitionType != nil {
                return vc!.presentedViewComponent!.presentTransitionType ?? .translation(origin: (vc?.presentedViewComponent?.destination.defaultOrigin ?? .none) ?? .bottomOutOfLine)
            }
            return .translation(origin: (vc?.presentedViewComponent?.destination.defaultOrigin ?? .bottomOutOfLine))
        }
        return presentedViewComponent!.presentTransitionType ?? .translation(origin: presentedViewComponent!.destination.defaultOrigin)
    }

    var dismissTransitionType: SATransitionType {
        if self is UINavigationController {
            let vc = (self as? UINavigationController)?.viewControllers.last as? SAAlertViewController
            if vc?.presentedViewComponent!.dismissTransitionType != nil {
                return vc!.presentedViewComponent!.dismissTransitionType ?? presentTransitionType
            }
            return presentTransitionType
        }
        return presentedViewComponent!.dismissTransitionType ?? presentTransitionType
    }
}
