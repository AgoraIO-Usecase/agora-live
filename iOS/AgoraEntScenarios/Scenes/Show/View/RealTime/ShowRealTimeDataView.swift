//
//  ShowRealTimeDataView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2022/11/9.
//
import UIKit
import Agora_Scene_Utils
import AgoraRtcKit

class ShowRealTimeDataView: UIView {
    private lazy var leftInfoLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .small)
        label.textAlignment = .left
        label.numberOfLines = 0
        label.text = nil
        return label
    }()
    private lazy var rightInfoLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .small)
        label.textAlignment = .left
        label.numberOfLines = 0
        label.text = nil
        return label
    }()
    private lazy var closeButton: AGEButton = {
        let button = AGEButton(style: .systemImage(name: "xmark", imageColor: .white))
        button.addTarget(self, action: #selector(onTapCloseButton), for: .touchUpInside)
        return button
    }()
    
    
    init(isLocal: Bool) {
        super.init(frame: .zero)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public func update(left: String, right: String) {
        let attributedString = NSMutableAttributedString(string: left)
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineBreakMode = .byTruncatingTail
        attributedString.addAttribute(.paragraphStyle,
                                      value: paragraphStyle,
                                      range: NSRange(location: 0, length: attributedString.length))
        leftInfoLabel.attributedText = attributedString

        let rightAttributedString = NSMutableAttributedString(string: right)
        let rightParagraphStyle = NSMutableParagraphStyle()
        rightParagraphStyle.lineBreakMode = .byTruncatingTail
        rightAttributedString.addAttribute(.paragraphStyle,
                                           value: rightParagraphStyle,
                                           range: NSRange(location: 0, length: rightAttributedString.length))
        rightInfoLabel.attributedText = rightAttributedString
    }
    
    private func setupUI() {
        backgroundColor = UIColor(hex: "#151325", alpha: 0.8)
        layer.cornerRadius = 15
        layer.masksToBounds = true
        widthAnchor.constraint(equalToConstant: Screen.width - 30).isActive = true
        
        addSubview(leftInfoLabel)
        addSubview(rightInfoLabel)
        addSubview(closeButton)
        
        leftInfoLabel.snp.makeConstraints { make in
            make.leading.equalTo(15)
            make.trailing.equalTo(snp.centerX).offset(-2)
            make.top.equalTo(10)
            make.bottom.equalTo(-10)
        }
        rightInfoLabel.snp.makeConstraints { make in
            make.leading.equalTo(snp.centerX).offset(2)
            make.top.equalTo(leftInfoLabel)
            make.bottom.equalTo(leftInfoLabel)
            make.trailing.equalToSuperview().offset(-3)
        }
        closeButton.snp.makeConstraints { make in
            make.top.equalTo(12)
            make.right.equalTo(-15)
        }
    }
    
    @objc
    private func onTapCloseButton() {
        removeFromSuperview()
    }
}
