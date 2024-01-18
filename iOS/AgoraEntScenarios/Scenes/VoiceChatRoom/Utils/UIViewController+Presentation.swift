//
//  VoiceRoomAlertViewController.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 30, 2022
//

import Foundation

///UIViewController that complies with the PresentationViewType protocol
public typealias PresentationViewController = UIViewController & PresentedViewType
public typealias PresentationNavigationController = UINavigationController & SAPresentedViewType

extension VRBaseViewController {
    ///Custom presentation method
    func presentViewController(_ viewController: PresentationViewController, animated: Bool = true) {
        dismiss(animated: false)
        viewController.modalPresentationStyle = .custom
        viewController.transitioningDelegate = self
        present(viewController, animated: animated, completion: nil)
    }
    func navigationViewController(_ viewController: PresentationNavigationController, animated: Bool = true) {
        viewController.modalPresentationStyle = .custom
        viewController.transitioningDelegate = self
        present(viewController, animated: animated, completion: nil)
    }
}

// MARK: -  UIViewControllerTransitioningDelegate
//
extension VRBaseViewController: UIViewControllerTransitioningDelegate {
    public func presentationController(forPresented presented: UIViewController, presenting: UIViewController?, source: UIViewController) -> UIPresentationController? {
        return PresentationController(presentedViewController: presented, presenting: presenting)
    }

    public func animationController(forPresented presented: UIViewController, presenting: UIViewController, source: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        guard let presentedVC = presented as? PresentedViewType else { return nil }
        return presentedVC.presentTransitionType.animation
    }

    public func animationController(forDismissed dismissed: UIViewController) -> UIViewControllerAnimatedTransitioning? {
        guard let dismissedVC = dismissed as? PresentedViewType else { return nil }
        return dismissedVC.dismissTransitionType.animation
    }
}
