//
//  AlertManager.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import AVFoundation
import UIKit

public let cl_screenWidht = UIScreen.main.bounds.width
public let cl_screenHeight = UIScreen.main.bounds.height
public class AlertManager: NSObject {
    private struct AlertViewCache {
        var view: UIView?
        var index: Int = 0
    }

    public enum AlertPosition {
        case top
        case center
        case bottom
    }

    private static var vc: UIViewController?
    private static var containerView: UIView?
    private static var currentPosition: AlertPosition = .center
    private static var viewCache: [AlertViewCache] = []
    private static var bottomAnchor: NSLayoutConstraint?

    public static func show(view: UIView,
                            alertPostion: AlertPosition = .center,
                            didCoverDismiss: Bool = true)
    {
        let index = viewCache.isEmpty ? 0 : viewCache.count
        viewCache.append(AlertViewCache(view: view, index: index))
        currentPosition = alertPostion
        if vc == nil {
            containerView = UIButton(frame: CGRect(x: 0, y: 0, width: cl_screenWidht, height: cl_screenHeight))
            containerView?.backgroundColor = UIColor(red: 0.0 / 255, green: 0.0 / 255, blue: 0.0 / 255, alpha: 0.0)
        }
        if didCoverDismiss {
            (containerView as? UIButton)?.addTarget(self, action: #selector(tapView), for: .touchUpInside)
        }
        guard let containerView = containerView else { return }
        containerView.addSubview(view)
        view.translatesAutoresizingMaskIntoConstraints = false
        view.alpha = 0
        switch alertPostion {
        case .top:
            view.centerXAnchor.constraint(equalTo: containerView.centerXAnchor).isActive = true
            view.topAnchor.constraint(equalTo: containerView.topAnchor, constant: Screen.safeAreaTopHeight() + 50).isActive = true
            
        case .center:
            view.centerXAnchor.constraint(equalTo: containerView.centerXAnchor).isActive = true
            view.centerYAnchor.constraint(equalTo: containerView.centerYAnchor).isActive = true
            
        case .bottom:
            bottomAnchor = view.bottomAnchor.constraint(equalTo: containerView.bottomAnchor)
            view.leadingAnchor.constraint(equalTo: containerView.leadingAnchor).isActive = true
            view.trailingAnchor.constraint(equalTo: containerView.trailingAnchor).isActive = true
        }
        if vc == nil {
            vc = UIViewController()
            vc?.view.layer.contents = nil
            vc?.view.backgroundColor = UIColor.clear
            vc?.view.addSubview(containerView)
            vc?.modalPresentationStyle = .custom
            if let topVC = UIViewController.cl_topViewController() {
                topVC.present(vc!, animated: false) {
                    showAlertPostion(alertPostion: alertPostion, view: view)
                }
            } else {
                vc = nil
            }
        } else {
            showAlertPostion(alertPostion: alertPostion, view: view)
        }
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow(notification:)), name: UIApplication.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide(notification:)), name: UIApplication.keyboardWillHideNotification, object: nil)
    }

    private static func showAlertPostion(alertPostion: AlertPosition, view: UIView) {
        containerView?.layoutIfNeeded()
        switch alertPostion {
        case .top, .center:
            showCenterView(view: view)
            
        case .bottom:
            bottomAnchor?.constant = view.frame.height
            bottomAnchor?.isActive = true
            containerView?.layoutIfNeeded()
            showBottomView(view: view)
        }
    }

    private static func showCenterView(view: UIView) {
        if !viewCache.isEmpty {
            viewCache.forEach({ $0.view?.alpha = 0 })
        }
        UIView.animate(withDuration: 0.25, animations: {
            containerView?.backgroundColor = UIColor(red: 0.0 / 255,
                                                     green: 0.0 / 255,
                                                     blue: 0.0 / 255,
                                                     alpha: 0.5)
            view.alpha = 1.0
        })
    }

    private static func showBottomView(view: UIView) {
        if !viewCache.isEmpty {
            viewCache.forEach({ $0.view?.alpha = 0 })
        }
        view.alpha = 1.0
        bottomAnchor?.constant = 0
        bottomAnchor?.isActive = true
        UIView.animate(withDuration: 0.25, animations: {
            containerView?.backgroundColor = UIColor(red: 0.0 / 255,
                                                     green: 0.0 / 255,
                                                     blue: 0.0 / 255,
                                                     alpha: 0.5)
            containerView?.superview?.layoutIfNeeded()
        })
    }

    static func updateViewHeight() {
        UIView.animate(withDuration: 0.25, animations: {
            containerView?.layoutIfNeeded()
        })
    }

    public static func hiddenView(all: Bool = true, completion: (() -> Void)? = nil) {
        if vc == nil {
            completion?()
            return
        }
        if currentPosition == .bottom {
            guard let lastView = viewCache.last?.view else { return }
            bottomAnchor?.constant = lastView.frame.height
            bottomAnchor?.isActive = true
        }
        UIView.animate(withDuration: 0.25, animations: {
            if all || viewCache.isEmpty {
                containerView?.backgroundColor = UIColor(red: 255.0 / 255,
                                                         green: 255.0 / 255,
                                                         blue: 255.0 / 255,
                                                         alpha: 0.0)
                containerView?.layoutIfNeeded()
            }
            if currentPosition != .bottom {
                viewCache.last?.view?.alpha = 0
            }
        }, completion: { _ in
            if all || viewCache.isEmpty {
                viewCache.removeAll()
                vc?.dismiss(animated: false, completion: completion)
                vc = nil
            } else {
                viewCache.removeLast()
                viewCache.last?.view?.alpha = 1
            }
        })
    }

    @objc
    private static func tapView() {
        DispatchQueue.main.asyncAfter(deadline: DispatchTime(uptimeNanoseconds: UInt64(0.1))) {
            self.hiddenView()
        }
    }

    @objc private static func keyboardWillShow(notification: Notification) {
        let keyboardHeight = (notification.userInfo?["UIKeyboardBoundsUserInfoKey"] as? CGRect)?.height ?? 304
        var y = cl_screenHeight - keyboardHeight - containerView!.frame.height
        if currentPosition == .top {
            y = y <= 0 ? 0 : y
            
        } else if currentPosition == .center {
            y = y <= 0 ? y * 0.5 : y
        }
        UIView.animate(withDuration: 0.25) {
            containerView?.frame.origin.y = y
        }
    }
    @objc private static func keyboardWillHide(notification: Notification) {
        let y = cl_screenHeight - containerView!.frame.height
        UIView.animate(withDuration: 0.25) {
            containerView?.frame.origin.y = y
        } completion: { _ in
            if currentPosition == .bottom {
//                hiddenView()
            }
        }
    }
}

extension UIViewController {
    static var keyWindow: UIWindow? {
        // Get connected scenes
        if #available(iOS 13.0, *) {
            return UIApplication.shared.connectedScenes
            // Keep only active scenes, onscreen and visible to the user
                .filter { $0.activationState == .foregroundActive }
            // Keep only the first `UIWindowScene`
                .first(where: { $0 is UIWindowScene })
            // Get its associated windows
                .flatMap({ $0 as? UIWindowScene })?.windows
            // Finally, keep only the key window
                .first(where: \.isKeyWindow)
        } else {
            return UIApplication.shared.keyWindow
        }
    }

    public static func cl_topViewController(_ viewController: UIViewController? = nil) -> UIViewController? {
        let viewController = viewController ?? keyWindow?.rootViewController

        if let navigationController = viewController as? UINavigationController,
           !navigationController.viewControllers.isEmpty
        {
            return cl_topViewController(navigationController.viewControllers.last)

        } else if let tabBarController = viewController as? UITabBarController,
                  let selectedController = tabBarController.selectedViewController
        {
            return cl_topViewController(selectedController)

        } else if let presentedController = viewController?.presentedViewController {
            return cl_topViewController(presentedController)
        }
        return viewController
    }
}
