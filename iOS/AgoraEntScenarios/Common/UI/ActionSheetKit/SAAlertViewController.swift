//
//  VoiceRoomAlertViewController.swift
//  VoiceRoomBaseUIKit
//
// Created by Zhu Jichao on August 30, 2022
//

import UIKit
import ZSwiftBaseLib
// public enum

public class SAAlertNavigationController: UINavigationController, SAPresentedViewType {
    public var presentedViewComponent: SAPresentedViewComponent?
    

    override public func viewDidLoad() {
        super.viewDidLoad()
        self.isNavigationBarHidden = true
    }
}

public class SAAlertViewController: UIViewController, SAPresentedViewType {
    public var presentedViewComponent: SAPresentedViewComponent?

    var customView: UIView?
    private var isLayout: Bool = false
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
    }

    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }

    public convenience init(compent: SAPresentedViewComponent, custom: UIView, isLayout: Bool = false) {
        self.init()
        self.isLayout = isLayout
        presentedViewComponent = compent
        customView = custom
    }

    override public func viewDidLoad() {
        super.viewDidLoad()
        view.layer.cornerRadius = 10
        view.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        view.layer.masksToBounds = true
        if customView != nil {
            view.addSubview(customView!)
            if isLayout {
                customView?.translatesAutoresizingMaskIntoConstraints = false
                customView?.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
                customView?.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
                customView?.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
                customView?.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
            }
        }
    }
}
