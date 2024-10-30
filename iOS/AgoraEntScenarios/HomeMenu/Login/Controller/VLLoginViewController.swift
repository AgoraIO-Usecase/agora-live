//
//  VLLoginViewController.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/8/7.
//

import UIKit
import AgoraCommon

@objc
class VLLoginViewController: VRBaseViewController {
    private lazy var backgroundIconImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "login_background_icon"))
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    private lazy var backgroundHalfImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "login_background_image_half"))
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    private lazy var backgroundTextImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage(named: "login_background_text"))
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    private lazy var policyContailerView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    private lazy var agreeButton: UIButton = {
        let button = UIButton()
        button.setTitle("I accept the ", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 13)
        button.setImage(UIImage(named: "policy_bg_icon"), for: .normal)
        button.setImage(UIImage(named: "policy_bg_select_icon"), for: .selected)
        button.imageEdgeInsets(UIEdgeInsets(top: 0, left: -15, bottom: 0, right: 0))
        button.translatesAutoresizingMaskIntoConstraints = false
        button.touchAreaEdgeInsets = UIEdgeInsets(top: -20, left: -20, bottom: -20, right: 0)
        button.addTarget(self, action: #selector(onClickAgreeButton(sender:)), for: .touchUpInside)
        return button
    }()
    private lazy var policyButton: UIButton = {
        let button = UIButton()
        button.setTitle("Terms of Service", for: .normal)
        button.setTitleColor(UIColor(hex: "#32AEFF"), for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 13)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(onClickPolicyButton(sender:)), for: .touchUpInside)
        return button
    }()
    private lazy var policyTipsButton: UIButton = {
        let button = UIButton()
        button.setBackgroundImage(UIImage(named: "login_policy_tips"), for: .normal)
        button.setTitle("  Please check  ", for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 14)
        button.titleEdgeInsets(UIEdgeInsets(top: 0, left: 0, bottom: 5, right: 0))
        button.translatesAutoresizingMaskIntoConstraints = false
        button.alpha = 0
        return button
    }()
    private lazy var loginButton: UIButton = {
        let button = UIButton()
        button.setTitle("Enter now", for: .normal)
        button.setTitleColor(.black, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .bold)
        button.backgroundColor = .white
        button.layer.cornerRadius = 26
        button.layer.masksToBounds = true
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(onClickLoginButton(sender:)), for: .touchUpInside)
        return button
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    private func setupUI() {
        view.layer.contents = UIImage(named: "login_background_image")?.cgImage
        
        view.addSubview(backgroundIconImageView)
        backgroundIconImageView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        backgroundIconImageView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        backgroundIconImageView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        
        view.addSubview(backgroundHalfImageView)
        backgroundHalfImageView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        backgroundHalfImageView.bottomAnchor.constraint(equalTo: view.bottomAnchor).isActive = true
        backgroundHalfImageView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        
        view.addSubview(backgroundTextImageView)
        backgroundTextImageView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        backgroundTextImageView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        backgroundTextImageView.topAnchor.constraint(equalTo: view.centerYAnchor).isActive = true
        
        view.addSubview(loginButton)
        loginButton.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        loginButton.heightAnchor.constraint(equalToConstant: 52).isActive = true
        loginButton.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 35).isActive = true
        loginButton.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -35).isActive = true
        loginButton.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor, constant: -50).isActive = true
                
        view.addSubview(policyContailerView)
        policyContailerView.centerXAnchor.constraint(equalTo: view.centerXAnchor).isActive = true
        policyContailerView.bottomAnchor.constraint(equalTo: loginButton.topAnchor, constant: -18).isActive = true
        
        policyContailerView.addSubview(agreeButton)
        agreeButton.leadingAnchor.constraint(equalTo: policyContailerView.leadingAnchor).isActive = true
        agreeButton.topAnchor.constraint(equalTo: policyContailerView.topAnchor).isActive = true
        agreeButton.bottomAnchor.constraint(equalTo: policyContailerView.bottomAnchor).isActive = true
        
        policyContailerView.addSubview(policyButton)
        policyButton.leadingAnchor.constraint(equalTo: agreeButton.trailingAnchor).isActive = true
        policyButton.centerYAnchor.constraint(equalTo: policyContailerView.centerYAnchor).isActive = true
        policyButton.trailingAnchor.constraint(equalTo: policyContailerView.trailingAnchor).isActive = true
        
        view.addSubview(policyTipsButton)
        policyTipsButton.leadingAnchor.constraint(equalTo: agreeButton.leadingAnchor, constant: -20).isActive = true
        policyTipsButton.bottomAnchor.constraint(equalTo: policyContailerView.topAnchor).isActive = true
    }
    
    @objc
    private func onClickAgreeButton(sender: UIButton) {
        sender.isSelected = !sender.isSelected
    }
    @objc
    private func onClickPolicyButton(sender: UIButton) {
        pushToWebView(url: kURLPathH5TermsOfService)
    }
    @objc
    private func onClickLoginButton(sender: UIButton) {
        if agreeButton.isSelected == false {
            policyTipsButton.alpha = 1.0
            let shakeAnimation = CAKeyframeAnimation(keyPath: "position")
            shakeAnimation.duration = 0.1
            shakeAnimation.repeatCount = 2
            shakeAnimation.values = [
                NSValue(cgPoint: CGPoint(x: policyTipsButton.center.x - 2.0, y: policyTipsButton.center.y)),
                NSValue(cgPoint: CGPoint(x: policyTipsButton.center.x + 2.0, y: policyTipsButton.center.y))
            ]
            shakeAnimation.autoreverses = true
            policyTipsButton.layer.add(shakeAnimation, forKey: nil)
            UIView.animate(withDuration: 0.25, delay: 2.5) {
                self.policyTipsButton.alpha = 0
            }
            return
        }
        let model = VLLoginModel()
        model.userNo = "\(Int.random(in: 10000...9999999))"
        model.chat_uid = model.userNo
        model.rtc_uid = model.userNo
        model.id = model.userNo
        let names = ["Ezra", "Pledge", "Bonnie", "Seeds", "Shannon", "Red-Haired", "Montague", "Primavera", "Lucille", "Tess"]
        model.name = names.randomElement() ?? ""
        model.headUrl = "avatar_\(Int.random(in: 1...4))"
        VLUserCenter.shared().storeUserInfo(model)
        UIApplication.shared.delegate?.window??.configRootViewController()
    }
    
    private func pushToWebView(url: String) {
        let webVC = VLCommonWebViewController()
        webVC.urlString = url
        if (navigationController == nil) {
            present(webVC, animated: true)
        } else {
            navigationController?.pushViewController(webVC, animated: true)
        }
    }
    
    override var backImageName: String {
        ""
    }
    
    override var titleColor: UIColor {
        .white
    }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        .lightContent
    }
}
