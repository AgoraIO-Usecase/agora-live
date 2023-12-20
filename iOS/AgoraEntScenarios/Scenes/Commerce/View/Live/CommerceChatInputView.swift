//
//  ShowChatInputView.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/10.
//

import UIKit

private let kTextFiledHeight: CGFloat = 36

protocol CommerceChatInputViewDelegate: UITextFieldDelegate {
    func onEndEditing()
    func onClickEmojiButton()
    func onClickSendButton(text: String)
}


class CommerceChatInputView: UIView {
    
    weak var delegate: CommerceChatInputViewDelegate?
    
    lazy var textField: UITextField = {
        let textField = UITextField()
        let attributes = [
            NSAttributedString.Key.foregroundColor: UIColor(hex: "#FFFFFF", alpha: 0.5),
            NSAttributedString.Key.font: UIFont.commerce_R_13
        ]
        textField.attributedPlaceholder = NSAttributedString(string: "create_live_chat_title".commerce_localized,
                                                             attributes: attributes as [NSAttributedString.Key : Any])
        textField.delegate = self
        textField.font = .commerce_R_13
        textField.textColor = UIColor(hex: "#FFFFFF", alpha: 1.0)
        textField.leftView = UIView(frame: CGRect(origin: .zero, size: CGSize(width: 16, height: 10)))
        textField.leftViewMode = .always
        textField.backgroundColor = UIColor(hex: "#000000", alpha: 0.25)
        textField.returnKeyType = .send
        textField.layer.borderWidth = 1
        textField.layer.borderColor = UIColor(hex: "#FFFFFF", alpha: 0.6).cgColor
        textField.layer.cornerRadius = 22
        textField.translatesAutoresizingMaskIntoConstraints = false
        return textField
    }()
        
    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        backgroundColor = .clear
        
        addSubview(textField)
        textField.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 12).isActive = true
        textField.topAnchor.constraint(equalTo: topAnchor).isActive = true
        textField.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -12).isActive = true
        textField.heightAnchor.constraint(equalToConstant: 38).isActive = true
        textField.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -10).isActive = true
    }
    
    @objc private func didClickEmojiButton(){
        delegate?.onClickEmojiButton()
    }
    
    
    @objc private func didClickSendButton(){
       sendAction()
    }
    
    private func sendAction(){
        if let text = textField.text?.trimmingCharacters(in: .whitespacesAndNewlines), text.count > 0 {
            delegate?.onClickSendButton(text: text)
        }
        textField.text = nil
        textField.resignFirstResponder()
    }
    
}


extension CommerceChatInputView: UITextFieldDelegate {
    
    func textFieldDidEndEditing(_ textField: UITextField) {
        delegate?.onEndEditing()
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        sendAction()
        return true
    }
    
}
