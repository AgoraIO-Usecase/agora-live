//
//  BaseNavgationController.swift
//  Pods-VoiceRoomBaseUIKit_Example
//
// Created by Zhu Jichao on August 24, 2022
//

import UIKit
import ZSwiftBaseLib
import SVProgressHUD

@objcMembers
open class VRBaseViewController: UIViewController {
    open lazy var navigation: BaseNavigationView = .init(frame: CGRect(x: 0, y: 0, width: ScreenWidth, height: ZNavgationHeight))

    override open func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.navigationBar.isHidden = true
    }

    open override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        view.backgroundColor = .white
        view.addSubview(navigation)
        if #available(iOS 13.0, *) {
            setupNavigationAttributes()
        } else {
            // Fallback on earlier versions
        }
        navigation.back.addTarget(self, action: #selector(backAction), for: .touchUpInside)
    }

    open override var preferredStatusBarStyle: UIStatusBarStyle {
        .default
    }

    open override var title: String? {
        didSet {
            navigation.title.text = title
        }
    }
    
    @available(iOS 13.0, *)
    public func setupNavigationAttributes() {
        navigation.title.isHidden = !showTitle
        navigation.title.textColor = titleColor
        navigation.back.setImage(UIImage(systemName: backImageName)?.withTintColor(.black, renderingMode: .alwaysOriginal), for: .normal)
        navigation.backgroundColor = navBackgroundColor
    }
    
    open override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        SVProgressHUD.dismiss()
    }
}

extension VRBaseViewController {
    var showTitle: Bool { true }

    open var titleColor: UIColor { .darkText }

    open var backImageName: String { "chevron.left" }

    open var navBackgroundColor: UIColor { .clear }

    @objc public func backAction() {
        navigationController?.popViewController(animated: true)
    }
}
