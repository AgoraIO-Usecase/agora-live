//
//  InviteCodeViewController.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/11/22.
//

import UIKit

class InviteCodeViewController: UIViewController {
    lazy var innerView: UIView = {
        let view = UIView()
        view.layer.masksToBounds = true

        view.layer.addSublayer(self.gradientLayer)
        return view
    }()
    
    lazy var gradientLayer: CAGradientLayer = {
        let layer = CAGradientLayer()
        layer.frame = self.view.bounds
        layer.colors = [UIColor(red: 0.58, green: 1.00, blue: 0.89, alpha: 1.00).cgColor, UIColor(red: 1.00, green: 1.00, blue: 1.00, alpha: 1.00).cgColor]
        layer.locations = [0.00, 1.00]
        layer.startPoint = CGPoint(x: 0.5, y: 0)
        layer.endPoint = CGPoint(x: 0.5, y: 1)
        return layer
    }()
    
    lazy var inviteBackImageView: UIImageView = {
        let view = UIImageView()
        view.image = UIImage(named: "invite_code_icon")
        view.contentMode = .scaleAspectFit
        return view
    }()

    lazy var gridImageView: UIImageView = {
        let view = UIImageView()
        view.image = UIImage(named: "grid_icon")
        view.contentMode = .scaleAspectFit
        return view
    }()

    lazy var backButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage(named: "back"), for: .normal)
        button.addTarget(self, action: #selector(backAction), for: .touchUpInside)
        return button
    }()
    
    lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "invite code"
        label.font = UIFont.systemFont(ofSize: 20, weight: .medium)
        label.textAlignment = .center
        label.textColor = .black
        label.numberOfLines = 1;
        return label
    }()
    
    lazy var subTitleLabel: UILabel = {
        let label = UILabel()
        label.text = "My invite code"
        label.font = UIFont.systemFont(ofSize: 20, weight: .medium)
        label.textAlignment = .center
        label.textColor = .black
        label.numberOfLines = 1;
        return label
    }()

    lazy var descriptionLabel: UILabel = {
        let label = UILabel()
        label.text = "Generate a code to your friends"
        label.font = UIFont.boldSystemFont(ofSize: 14)
        label.textAlignment = .center
        label.textColor = .black
        label.numberOfLines = 1;
        return label
    }()
    
    lazy var codeLabel: UILabel = {
        let label = UILabel()
        label.text = "******"
        label.font = UIFont.boldSystemFont(ofSize: 49)
        label.textAlignment = .center
        label.textColor = .black
        label.numberOfLines = 1;
        return label
    }()
    
    lazy var generateButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setTitle("Generate A Code", for: .normal)
        button.setTitleColor(.black, for: .normal)
        button.setTitleColor(.black, for: .selected)
        button.layer.cornerRadius = 52 / 2
        button.layer.masksToBounds = true
        button.backgroundColor = .white
        button.addTarget(self, action: #selector(generateAction), for: .touchDown)

        return button
    }()

    lazy var lineView: UIView = {
        let view = UIView()
        view.backgroundColor = .black.withAlphaComponent(0.2)
        return view
    }()
    
    @objc
    private func backAction() {
        self.navigationController?.popViewController(animated: true)
    }
    
    @objc
    private func generateAction() {
        if generateButton.isSelected {
            UIPasteboard.general.string = VLUserCenter.user.invitationCode
            ToastView.show(text: "Coped")
            return
        }
        generateButton.isSelected.toggle()
        LoginManager.shared.generateCode()
        refreshUI()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupSubviews()
        setupConstraints()
        refreshUI()
    }
    
    private func refreshUI() {
        let result = LoginManager.shared.hasGeneratedCode()
        let code = VLUserCenter.user.invitationCode
        codeLabel.text = result ? code : "******"
        generateButton.isSelected = result
        if generateButton.isSelected {
            generateButton.setTitle("Copy", for: .normal)
        } else {
            generateButton.setTitle("Generate A Code", for: .normal)
        }

    }
    
    func setupSubviews() {
        view.addSubview(self.innerView)
        innerView.addSubview(gridImageView)
        innerView.addSubview(inviteBackImageView)
        innerView.addSubview(backButton)
        innerView.addSubview(titleLabel)
        inviteBackImageView.addSubview(subTitleLabel)
        
        innerView.addSubview(lineView)
        innerView.addSubview(generateButton)
        innerView.addSubview(codeLabel)
        innerView.addSubview(descriptionLabel)
    }

    func setupConstraints() {
        self.innerView.snp.makeConstraints { make in
            make.edges.equalTo(UIEdgeInsets.zero)
        }
        
        gridImageView.snp.makeConstraints { make in
            make.top.left.right.equalTo(0)
            make.height.equalTo(147)
        }
        
        backButton.snp.makeConstraints { make in
            make.left.equalTo(20)
            make.top.equalTo(38)
            make.height.width.equalTo(30)
        }
        
        titleLabel.snp.makeConstraints { make in
            make.top.equalTo(41)
            make.centerX.equalTo(self.innerView)
        }
        
        inviteBackImageView.snp.makeConstraints { make in
            make.top.equalTo(76)
            make.left.equalTo(20)
            make.right.equalTo(-20)
            make.height.equalTo(inviteBackImageView.snp.width).multipliedBy(526.0 / 335.0)
        }
        
        subTitleLabel.snp.makeConstraints { make in
            make.top.equalTo(47)
            make.left.equalTo(33)
        }
        
        lineView.snp.makeConstraints { make in
            make.left.equalTo(79)
            make.right.equalTo(-79)
            make.height.equalTo(2)
            make.centerY.equalTo(self.innerView)
        }
        
        generateButton.snp.makeConstraints { make in
            make.top.equalTo(lineView.snp.bottom).offset(41)
            make.left.equalTo(lineView)
            make.right.equalTo(lineView)
            make.height.equalTo(52)
        }
        
        codeLabel.snp.makeConstraints { make in
            make.bottom.equalTo(lineView.snp.top).offset(-15)
            make.left.right.equalTo(lineView)
        }
        
        descriptionLabel.snp.makeConstraints { make in
            make.centerX.equalTo(lineView)
            make.bottom.equalTo(codeLabel.snp.top).offset(-20)
        }

    }

}
