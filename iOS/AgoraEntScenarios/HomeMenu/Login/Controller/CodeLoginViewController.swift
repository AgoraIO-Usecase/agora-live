//
//  CodeLoginViewController.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/11/22.
//

import UIKit
import SnapKit
import SVProgressHUD

private var maxLengthKey: Int = 0

extension UITextField {
    @IBInspectable var maxLength: Int {
        get {
            return objc_getAssociatedObject(self, &maxLengthKey) as? Int ?? Int.max
        }
        set {
            objc_setAssociatedObject(self, &maxLengthKey, newValue, .OBJC_ASSOCIATION_RETAIN)
            addTarget(self, action: #selector(checkMaxLength), for: .editingChanged)
        }
    }
    
    @objc private func checkMaxLength(textField: UITextField) {
        guard let prospectiveText = textField.text, prospectiveText.count > maxLength else {
            return
        }
        
        let selection = selectedTextRange
        text = String(prospectiveText.prefix(maxLength))
        selectedTextRange = selection
    }
}

class InputView: UIView {
    lazy var text: UITextField = {
        let textField = UITextField()
        let placeholderAttributes: [NSAttributedString.Key: Any] = [.font: UIFont.systemFont(ofSize: 16)]
        textField.attributedPlaceholder = NSAttributedString(string: "input your invite code", attributes: placeholderAttributes)
        textField.font = UIFont.boldSystemFont(ofSize: 16)
        textField.textAlignment = .center
        textField.textColor = .black
        textField.borderStyle = .roundedRect
        textField.maxLength = 10
        return textField
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        self.backgroundColor = UIColor(red: 0.95, green: 0.96, blue: 0.98, alpha: 1.00)
        self.layer.cornerRadius = 3333
        setupSubviews()
        setupConstraints()
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
    }

    func setupSubviews() {
        self.addSubview(self.text)
    }

    func setupConstraints() {
        text.snp.makeConstraints { make in
            make.edges.equalTo(UIEdgeInsets.zero)
        }
    }
}

class RadiusView: UIView {
    lazy var innerView: UIView = {
        let view = UIView()
        view.layer.masksToBounds = true
        view.layer.cornerRadius = 10.667;

        view.layer.addSublayer(self.gradientLayer)
        return view
    }()

    lazy var gradientLayer: CAGradientLayer = {
        let layer = CAGradientLayer()
        layer.frame = self.bounds
        layer.colors = [UIColor(red: 0.48, green: 0.53, blue: 1.00, alpha: 1.00).cgColor, UIColor(red: 0.48, green: 0.32, blue: 0.95, alpha: 1.00).cgColor, UIColor(red: 0.70, green: 0.67, blue: 1.00, alpha: 1.00).cgColor]
        layer.locations = [0.00, 0.57, 0.99]
        layer.startPoint = CGPoint(x: 0, y: 0.5)
        layer.endPoint = CGPoint(x: 1, y: 0.5)
        return layer
    }()


    lazy var text: UILabel = {
        let label = UILabel()
        label.text = "Enter Now"
        label.font = UIFont.systemFont(ofSize: 16)
        label.textAlignment = .center
        label.textColor = .white
        label.numberOfLines = 1;
        return label
    }()

    lazy var image: UIImageView = {
        let view = UIImageView()
        view.image = UIImage(named: "login_arrow_icon")
        view.contentMode = .scaleAspectFit
        return view
    }()
    
    private let stackView: UIStackView = {
        let stack = UIStackView()
        stack.axis = .horizontal
        stack.alignment = .center
        stack.spacing = 8
        stack.translatesAutoresizingMaskIntoConstraints = false
        return stack
    }()

    var onTap: (() -> Void)?

    override init(frame: CGRect) {
        super.init(frame: frame)
        self.layer.cornerRadius = 10.667
        self.layer.masksToBounds = true
        setupSubviews()
        setupConstraints()
        self.backgroundColor = UIColor.clear
        self.isUserInteractionEnabled = true
        self.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(handleTap)))
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        self.gradientLayer.frame = self.bounds
    }

    func setupSubviews() {
        self.addSubview(self.innerView)
        self.innerView.addSubview(stackView)
        stackView.addArrangedSubview(text)
        stackView.addArrangedSubview(image)
    }

    func setupConstraints() {
        innerView.snp.makeConstraints { make in
            make.edges.equalTo(self)
        }

        stackView.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.centerY.equalToSuperview()
            make.leading.greaterThanOrEqualToSuperview()
            make.trailing.lessThanOrEqualToSuperview()
        }
        
        image.snp.makeConstraints { make in
            make.width.equalTo(20)
            make.height.equalTo(20)
        }
    }

    @objc func handleTap() {
        onTap?()
    }
}

class CodeLoginViewController: UIViewController {
    var loginCompletedCallback: (() -> Void)?
    
    lazy var contentView: UIView = {
        let view = UIView()
        view.backgroundColor = .white
        view.layer.cornerRadius = 20;
        return view
    }()
    
    lazy var subContentView: UIView = {
        let view = UIView()
        view.backgroundColor = .white
        view.layer.cornerRadius = 20;
        view.layer.shadowOpacity = 1
        view.layer.shadowRadius = 2.667
        view.layer.shadowOffset = CGSize(width: 0, height: 0)
        view.layer.shadowColor = UIColor(red: 0.00, green: 0.00, blue: 0.00, alpha: 0.10).cgColor
        return view
    }()
    
    lazy var codeInputView: InputView = {
        let view = InputView()
        view.layer.borderColor = UIColor.black.cgColor
        view.layer.borderWidth = 1
        view.layer.cornerRadius = 52 / 2.0
        view.layer.masksToBounds = true
        return view
    }()

    lazy var enterButton: RadiusView = {
        let view = RadiusView()
        view.layer.cornerRadius = 52 / 2.0
        view.layer.masksToBounds = true
        view.onTap = { [weak self] in
            guard let self = self, let code = self.codeInputView.text.text else { 
                ToastView.show(text: "Please enter the invitation code.")
                return
            }
            SVProgressHUD.show()
            LoginApiService.loginWithInvitationCode(code: code) { [weak self] error in
                if let err = error {
                    SVProgressHUD.dismiss()
                    ToastView.show(text: err.localizedDescription)
                } else {
                    self?.getUserInfo()
                }
            }
        }
        return view
    }()
    
    lazy var closeButton: UIButton = {
        let button = UIButton(type: .custom)
        button.addTarget(self, action: #selector(closeAction), for: .touchUpInside)
        button.setImage(UIImage(named: "login_close_icon"), for: .normal)
        return button
    }()
    
    lazy var imageView: UIImageView = {
        let view = UIImageView()
        view.image = UIImage(named: "login_phone_image_icon")
        view.contentMode = .scaleAspectFit
        return view
    }()
    
    lazy var welcomeLabel: UILabel = {
        let label = UILabel()
        label.text = "Welcome "
        label.font = UIFont.boldSystemFont(ofSize: 24)
        label.textAlignment = .left
        label.textColor = .black
        label.numberOfLines = 1;
        return label
    }()

    lazy var toLabel: UILabel = {
        let label = UILabel()
        label.text = "to"
        label.font = UIFont.boldSystemFont(ofSize: 24)
        label.textAlignment = .left
        label.textColor = .black
        label.numberOfLines = 1;
        return label
    }()
    
    lazy var agoraLabel: UILabel = {
        let label = UILabel()
        label.text = "Agora"
        label.font = UIFont.boldSystemFont(ofSize: 24)
        label.textAlignment = .left
        label.textColor = UIColor(hex: "#099DFD")
        label.numberOfLines = 1;
        return label
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .purple
        setupSubviews()
        setupConstraints()
        setupKeyboardObservers()
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    private func getUserInfo() {
        LoginApiService.getInvitationCodeUserInfo { [weak self] error in
            SVProgressHUD.dismiss()
            if let err = error {
                ToastView.show(text: err.localizedDescription)
            } else {
                guard let self = self, let completeCallback = self.loginCompletedCallback else { return }
                completeCallback()
            }
        }
    }
    
    func setupKeyboardObservers() {
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow(notification:)), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide(notification:)), name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    @objc func keyboardWillShow(notification: Notification) {
        guard let userInfo = notification.userInfo,
              let keyboardFrame = userInfo[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue else { return }
        
        let keyboardHeight = keyboardFrame.cgRectValue.height
        
        UIView.animate(withDuration: 0.3) {
            self.view.frame.origin.y = -keyboardHeight + 100
        }
    }
    
    @objc private func closeAction() {
        self.dismiss(animated: false)
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }

    @objc func keyboardWillHide(notification: Notification) {
        UIView.animate(withDuration: 0.3) {
            self.view.frame.origin.y = 0
        }
    }

    func setupSubviews() {
        view.addSubview(self.contentView)
        view.addSubview(self.imageView)
        view.addSubview(self.subContentView)
        subContentView.addSubview(self.codeInputView)
        subContentView.addSubview(self.enterButton)
        contentView.addSubview(self.welcomeLabel)
        contentView.addSubview(self.toLabel)
        contentView.addSubview(self.agoraLabel)
        contentView.addSubview(self.closeButton)
    }
    
    func setupConstraints() {
        contentView.snp.makeConstraints { make in
            make.left.right.bottom.equalTo(0)
            make.height.equalTo(388)
        }

        subContentView.snp.makeConstraints { make in
            make.left.right.bottom.equalTo(0)
            make.height.equalTo(288)
        }

        codeInputView.snp.makeConstraints { make in
            make.left.equalTo(30)
            make.right.equalTo(-30)
            make.top.equalTo(27)
            make.height.equalTo(52)
        }

        enterButton.snp.makeConstraints { make in
            make.top.equalTo(codeInputView.snp.bottom).offset(20)
            make.left.equalTo(30)
            make.right.equalTo(-30)
            make.height.equalTo(52)
        }

        welcomeLabel.snp.makeConstraints { make in
            make.left.equalTo(30)
            make.top.equalTo(21)
        }

        toLabel.snp.makeConstraints { make in
            make.left.equalTo(30)
            make.top.equalTo(welcomeLabel.snp.bottom).offset(1)
        }
        
        agoraLabel.snp.makeConstraints { make in
            make.left.equalTo(toLabel.snp.right).offset(2)
            make.top.equalTo(toLabel.snp.top)
        }

        imageView.snp.makeConstraints { make in
            make.bottom.equalTo(subContentView.snp.top)
            make.right.equalTo(-20)
            make.height.width.equalTo(192)
        }
        
        closeButton.snp.makeConstraints { make in
            make.top.equalTo(14)
            make.right.equalTo(-14)
            make.width.height.equalTo(22)
        }
    }
}
