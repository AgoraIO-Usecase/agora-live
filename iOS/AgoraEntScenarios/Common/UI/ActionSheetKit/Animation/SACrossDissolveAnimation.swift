//
//  VoiceRoomAlertViewController.swift
//  VoiceRoomBaseUIKit
//
// Created by Zhu Jichao on August 30, 2022
//

import Foundation

public class SACrossDissolveAnimation: SAPresentationAnimation {
    override public func beforeAnimation(animationContext: SAAnimationContext) {
        super.beforeAnimation(animationContext: animationContext)
        animationContext.animatingView?.alpha = animationContext.isPresenting ? 0.0 : 1.0
    }

    override public func performAnimation(animationContext: SAAnimationContext) {
        super.performAnimation(animationContext: animationContext)
        animationContext.animatingView?.alpha = animationContext.isPresenting ? 1.0 : 0.0
    }

    override public func afterAnimation(animationContext: SAAnimationContext) {
        super.afterAnimation(animationContext: animationContext)
        animationContext.animatingView?.alpha = 1.0
    }
}
