//
//  VLLoginViewController.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/8/7.
//

import UIKit

class GradientView: UIView {
    private let gradientLayer = CAGradientLayer()
    private let button: UIButton
    private let imageView: UIImageView

    init(buttonTitle: String) {
        self.button = UIButton(type: .custom)
        self.imageView = UIImageView(image: UIImage(named: "login_invite_btn_icon"))
        super.init(frame: .zero)
        setupView(buttonTitle: buttonTitle)
    }
    
    override init(frame: CGRect) {
        self.button = UIButton(type: .system)
        self.imageView = UIImageView(image: UIImage(named: "login_invite_btn_icon"))
        super.init(frame: frame)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupView(buttonTitle: String) {
        gradientLayer.colors = [UIColor(red: 208/255, green: 146/255, blue: 255/255, alpha: 1).cgColor,
                                UIColor.white.cgColor,
                                UIColor(red: 66/255, green: 255/255, blue: 249/255, alpha: 1).cgColor]
        gradientLayer.startPoint = CGPoint(x: 0, y: 0.5)
        gradientLayer.endPoint = CGPoint(x: 1, y: 0.5)
        layer.addSublayer(gradientLayer)

        button.setTitle(buttonTitle, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .regular)
        button.backgroundColor = UIColor(hex: "#7B52F2")
        button.translatesAutoresizingMaskIntoConstraints = false
        
        addSubview(button)
        
        button.snp.makeConstraints { make in
            make.edges.equalToSuperview().inset(1)
        }
        
        addSubview(imageView)
        imageView.snp.makeConstraints { make in
            make.left.equalTo(12)
            make.centerY.equalTo(self)
            make.width.height.equalTo(32)
        }
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        gradientLayer.frame = bounds
        button.layer.cornerRadius = CGRectGetHeight(self.bounds) / 2.0
        button.layer.masksToBounds = true
        
        let titleWidth = button.titleLabel?.intrinsicContentSize.width ?? 0
        let imageWidth = button.imageView?.intrinsicContentSize.width ?? 0
        let spacing: CGFloat = 10
        button.titleEdgeInsets = UIEdgeInsets(top: 0, left: spacing, bottom: 0, right: -imageWidth)
        button.contentEdgeInsets = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: spacing + imageWidth)
    }
    
    func addTarget(_ target: Any?, action: Selector, for controlEvents: UIControl.Event) {
        button.addTarget(target, action: action, for: controlEvents)
    }
}

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
        button.setTitle(NSLocalizedString("please_check", comment: ""), for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 14)
        button.titleEdgeInsets(UIEdgeInsets(top: 0, left: 0, bottom: 5, right: 0))
        button.translatesAutoresizingMaskIntoConstraints = false
        button.alpha = 0
        return button
    }()
    private lazy var ssoLoginButton: UIView = {
        let view = UIView()
        
        let button = UIButton()
        button.setTitle(NSLocalizedString("agora_console_sso", comment: ""), for: .normal)
        button.setTitleColor(.black, for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 16, weight: .regular)
        button.backgroundColor = .white
        button.layer.cornerRadius = 26
        button.layer.masksToBounds = true
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(onClickSSOLoginButton(sender:)), for: .touchUpInside)
        
        view.addSubview(button)
        button.snp.makeConstraints { make in
            make.left.top.right.bottom.equalTo(0)
        }
        
        let imageView = UIImageView(image: UIImage(named: "login_sso_btn_icon"))
        view.addSubview(imageView)
        
        imageView.snp.makeConstraints { make in
            make.left.equalTo(12)
            make.centerY.equalTo(view)
            make.width.height.equalTo(32)
        }
        return view
    }()
    private lazy var codeLoginButton: GradientView = {
        let view = GradientView(buttonTitle: NSLocalizedString("login_with_a_code", comment: ""))
        view.layer.cornerRadius = 26
        view.layer.masksToBounds = true
        view.addTarget(self, action: #selector(onClickCodeLoginButton(sender:)), for: .touchUpInside)
        return view
    }()
    private lazy var agoraLogoImageView: UIImageView = {
        let view = UIImageView()
        view.image = UIImage(named: "login_logo_icon")
        return view
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
    }

    private func setupUI() {
        view.layer.contents = UIImage(named: "login_background_image")?.cgImage
        
        view.addSubview(backgroundIconImageView)
        backgroundIconImageView.snp.makeConstraints { (make) in
            make.leading.equalTo(view.snp.leading)
            make.top.equalTo(view.snp.top)
            make.trailing.equalTo(view.snp.trailing)
        }
        
        view.addSubview(backgroundHalfImageView)
        backgroundHalfImageView.snp.makeConstraints { (make) in
            make.leading.equalTo(view.snp.leading)
            make.bottom.equalTo(view.snp.bottom)
            make.trailing.equalTo(view.snp.trailing)
        }
        
        view.addSubview(policyContailerView)
        policyContailerView.snp.makeConstraints { (make) in
            make.centerX.equalTo(view.snp.centerX)
            make.bottom.equalTo(view.safeAreaLayoutGuide.snp.bottom).offset(-53)
        }
        
        policyContailerView.addSubview(agreeButton)
        agreeButton.snp.makeConstraints { (make) in
            make.leading.equalTo(policyContailerView.snp.leading)
            make.top.equalTo(policyContailerView.snp.top)
            make.bottom.equalTo(policyContailerView.snp.bottom)
        }
        
        policyContailerView.addSubview(policyButton)
        policyButton.snp.makeConstraints { (make) in
            make.leading.equalTo(agreeButton.snp.trailing)
            make.centerY.equalTo(policyContailerView.snp.centerY)
            make.trailing.equalTo(policyContailerView.snp.trailing)
        }
                
        view.addSubview(codeLoginButton)
        codeLoginButton.snp.makeConstraints { (make) in
            make.height.equalTo(52)
            make.leading.equalTo(view.snp.leading).offset(35)
            make.trailing.equalTo(view.snp.trailing).offset(-35)
            make.bottom.equalTo(policyContailerView.snp.top).offset(-30)
        }
        
        view.addSubview(ssoLoginButton)
        ssoLoginButton.snp.makeConstraints { (make) in
            make.height.equalTo(52)
            make.leading.equalTo(view.snp.leading).offset(35)
            make.trailing.equalTo(view.snp.trailing).offset(-35)
            make.bottom.equalTo(codeLoginButton.snp.top).offset(-20)
        }
        
        view.addSubview(agoraLogoImageView)
        agoraLogoImageView.snp.makeConstraints { make in
            make.left.equalTo(26)
            make.right.equalTo(-26)
            make.bottom.equalTo(ssoLoginButton.snp.top).offset(-36)
        }
        
        view.addSubview(policyTipsButton)
        policyTipsButton.snp.makeConstraints { (make) in
            make.leading.equalTo(agreeButton.snp.leading).offset(-20)
            make.bottom.equalTo(policyContailerView.snp.top)
        }
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
    private func onClickCodeLoginButton(sender: UIButton) {
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
        
        let codeLoginVC = CodeLoginViewController()
        codeLoginVC.modalPresentationStyle = .overFullScreen
        codeLoginVC.view.backgroundColor = UIColor.black.withAlphaComponent(0.5)
        codeLoginVC.loginCompletedCallback = {
            UIApplication.shared.delegate?.window??.configRootViewController()
        }
        self.present(codeLoginVC, animated: false)
    }
    
    @objc
    private func onClickSSOLoginButton(sender: UIButton) {
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
        
        let ssoWebVC = SSOWebViewController()
        let baseUrl = KeyCenter.debugBaseServerUrl ?? ""
        ssoWebVC.urlString = "\(baseUrl)/sso/login"
        ssoWebVC.completionHandler = { token in
            if let token = token {
                print("Received token: \(token)")
                let model = VLLoginModel()
                model.token = token
                model.type = .SSOLOGIN
                VLUserCenter.shared().storeUserInfo(model)
                LoginApiService.getUserInfo { error in
                    if let err = error {
                        VLUserCenter.shared().logout()
                        ToastView.showWait(text: err.localizedDescription)
                    } else {
                        UIApplication.shared.delegate?.window??.configRootViewController()
                    }
                }
            } else {
                print("Failed to get token")
            }
        }
        navigationController?.pushViewController(ssoWebVC, animated: true)
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
