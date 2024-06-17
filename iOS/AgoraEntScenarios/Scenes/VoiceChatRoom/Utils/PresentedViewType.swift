//
//  VoiceRoomAlertViewController.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 30, 2022
//

import Foundation

///Settings for presentedView
public struct PresentedViewComponent {
    ///The size of presentedView
    public var contentSize: CGSize

    ///PresentedView final display location
    public var destination: PresentationDestination = .bottomBaseline

    ///Present transition animation, using nil based on destination
    public var presentTransitionType: TransitionType?

    ///Dismiss transition animation, using nil based on destination
    public var dismissTransitionType: TransitionType?

    ///Do you want to enable background display when clicking
    public var canTapBGDismiss: Bool = true

    ///Do you want to enable pan gesture display
    public var canPanDismiss: Bool = true

    ///Pan gesture direction, for nil, use based on destination
    public var panDismissDirection: PanDismissDirection?

    ///The translation method of the keyboard, which is close to PresentedView by default
    public var keyboardTranslationType: KeyboardTranslationType = .unabgeschirmt(compress: true)

    ///Keyboard interval, default to 0
    public var keyboardPadding: CGFloat = 0

    ///Initialization method
    ///
    /// - Parameters:
    ///- contentSize: The size of presentedView
    ///- Destination: presentedView
    ///- presentTransitionType: present transition animation
    ///- dispissTransitionType: dispissTransitionType animation
    ///- canTapBGDismiss: Is clicking background Dismiss enabled
    ///- canPanDismiss: Is pan gesture Dismiss enabled
    ///- panDisassisDirection: Pan gesture direction
    ///- keyboardTranslationType: The translation method that appears on the keyboard, which is close to PresentedView by default
    ///- keyboardPadding: keyboard spacing, default to 20
    public init(contentSize: CGSize,
                destination: PresentationDestination = .bottomBaseline,
                presentTransitionType: TransitionType? = nil,
                dismissTransitionType: TransitionType? = nil,
                canTapBGDismiss: Bool = true,
                canPanDismiss: Bool = true,
                panDismissDirection: PanDismissDirection? = nil,
                keyboardTranslationType: KeyboardTranslationType = .unabgeschirmt(compress: true),
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

///PresentedView must comply with this agreement
public protocol PresentedViewType {
    ///Settings for presentedView
    var presentedViewComponent: PresentedViewComponent? { get set }
}

extension PresentedViewType {
    var presentTransitionType: TransitionType {
        return presentedViewComponent!.presentTransitionType ?? .translation(origin: presentedViewComponent!.destination.defaultOrigin)
    }

    var dismissTransitionType: TransitionType {
        return presentedViewComponent!.dismissTransitionType ?? presentTransitionType
    }
}
