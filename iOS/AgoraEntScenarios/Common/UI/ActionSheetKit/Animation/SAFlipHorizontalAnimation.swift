//
//  VoiceRoomAlertViewController.swift
//  VoiceRoomBaseUIKit
//
// Created by Zhu Jichao on August 30, 2022
//
import Foundation

public class SAFlipHorizontalAnimation: SAPresentationAnimation {
    override public func performAnimation(animationContext: SAAnimationContext) {
        animationContext.toView?.layer.zPosition = 999
        animationContext.fromView?.layer.zPosition = 999

        var fromViewRotationPerspectiveTrans = CATransform3DIdentity
        fromViewRotationPerspectiveTrans.m34 = -0.003
        fromViewRotationPerspectiveTrans = CATransform3DRotate(fromViewRotationPerspectiveTrans, .pi / 2.0, 0.0, -1.0, 0.0)

        var toViewRotationPerspectiveTrans = CATransform3DIdentity
        toViewRotationPerspectiveTrans.m34 = -0.003
        toViewRotationPerspectiveTrans = CATransform3DRotate(toViewRotationPerspectiveTrans, .pi / 2.0, 0.0, 1.0, 0.0)

        animationContext.toView?.layer.transform = toViewRotationPerspectiveTrans

        UIView.animate(withDuration: options.duration, delay: 0, options: .curveLinear, animations: {
            animationContext.fromView?.layer.transform = fromViewRotationPerspectiveTrans
        }) { _ in
            UIView.animate(withDuration: self.options.duration, delay: 0, options: .curveLinear, animations: {
                animationContext.toView?.layer.transform = CATransform3DMakeRotation(.pi / 2.0, 0.0, 0.0, 0.0)
            }, completion: nil)
        }
    }
}
