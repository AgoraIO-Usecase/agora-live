//
//  ShowReceivePKView.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/16.
//

import UIKit

private let ButtonHeight: CGFloat = 40

protocol CommerceReceivePKViewDelegate: NSObjectProtocol {
    func onClickRefuseButton()
    func onClickAcceptButton()
}

class CommerceReceivePKView: UIView {

    weak var delegate: CommerceReceivePKViewDelegate?
    
    private var style: CommerceStyle? = .pk
    
    var countDown: Int = 0 {
        didSet {
            let text = "show_alert_pk_refuse".commerce_localized + "(\(countDown)s)"
            refuseButton.setTitle(text, for: .normal)
        }
    }
    
    var name: String? {
        didSet {
            let attributedText = NSMutableAttributedString(string: "show_alert_pk_title_1".commerce_localized,
                                                           attributes: [NSAttributedString.Key.foregroundColor : UIColor.commerce_Ellipse6])
            let attributedName = NSAttributedString(string: name ?? "", 
                                                    attributes: [NSAttributedString.Key.foregroundColor : UIColor.commerce_blue03])
            let styleText = style == .pk ? "show_alert_pk_title_2".commerce_localized : "show_alert_link_title_2".commerce_localized
            let tailText = NSAttributedString(string: styleText , 
                                              attributes: [NSAttributedString.Key.foregroundColor : UIColor.commerce_Ellipse6])
            attributedText.append(attributedName)
            attributedText.append(tailText)
            titleLabel.attributedText = attributedText
        }
    }
    
    private lazy var bgView: UIView = {
        let view = UIView()
        view.backgroundColor = .white
        view.layer.cornerRadius = 20
        view.layer.masksToBounds = true
        return view
    }()
    
    // PK
    private lazy var pkImgView: UIImageView =  {
        let imgeView =  UIImageView(image: UIImage.commerce_sceneImage(name: style == .pk ? "show_alert_pk" : "show_alert_mic"))
        return imgeView
    }()
    
    lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .commerce_Ellipse7
        label.font = .commerce_R_16
        label.numberOfLines = 0
        label.textAlignment = .center
        return label
    }()
    
    private lazy var refuseButton: UIButton = {
        let button = UIButton(type: .custom)
        button.backgroundColor = .commerce_btn_bg_not_recommended
        button.layer.cornerRadius = ButtonHeight * 0.5
        button.layer.masksToBounds = true
        button.setTitle("show_alert_pk_refuse".commerce_localized, for: .normal)
        button.setTitleColor(.commerce_Ellipse5, for: .normal)
        button.titleLabel?.font = .commerce_M_14
        button.addTarget(self, action: #selector(didClickRefuseButton), for: .touchUpInside)
        return button
    }()
    
    private lazy var acceptButton: UIButton = {
        let button = UIButton(type: .custom)
        button.backgroundColor = .commerce_zi03
        button.layer.cornerRadius = ButtonHeight * 0.5
        button.layer.masksToBounds = true
        button.setTitle("show_alert_pk_accept".commerce_localized, for: .normal)
        button.setTitleColor(.commerce_main_text, for: .normal)
        button.titleLabel?.font = .commerce_M_14
        button.addTarget(self, action: #selector(didClickAcceptButton), for: .touchUpInside)
        return button
    }()
    
    init(style: CommerceStyle?) {
        self.style = style
        super.init(frame: .zero)
        createSubviews()
    }

    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        backgroundColor = .clear
        
        addSubview(bgView)
        bgView.snp.makeConstraints { make in
            make.center.equalToSuperview()
            make.width.equalTo(295)
            make.height.equalTo(225)
        }
        
        addSubview(pkImgView)
        pkImgView.snp.makeConstraints { make in
            make.left.right.equalTo(bgView)
            make.bottom.equalTo(bgView.snp.top).offset(63)
        }
        
        bgView.addSubview(titleLabel)
        titleLabel.snp.makeConstraints { make in
            make.leading.trailing.equalToSuperview().inset(15)
            make.bottom.equalTo(-105)
        }
        
        bgView.addSubview(refuseButton)
        refuseButton.snp.makeConstraints { make in
            make.left.equalTo(30)
            make.bottom.equalTo(-40)
            make.height.equalTo(ButtonHeight)
            make.width.greaterThanOrEqualTo(108)
        }
        
        bgView.addSubview(acceptButton)
        acceptButton.snp.makeConstraints { make in
            make.right.equalTo(-30)
            make.bottom.equalTo(-40)
            make.height.equalTo(ButtonHeight)
            make.width.greaterThanOrEqualTo(108)
        }
        
    }
}

extension CommerceReceivePKView {
    enum CommerceStyle {
        case pk
        case mic
    }
}

extension CommerceReceivePKView {
    
    @objc private func didClickRefuseButton() {
        delegate?.onClickRefuseButton()
    }
    
    @objc private func didClickAcceptButton() {
        delegate?.onClickAcceptButton()
    }
}
