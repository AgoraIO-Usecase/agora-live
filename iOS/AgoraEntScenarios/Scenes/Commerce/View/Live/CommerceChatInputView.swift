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
    
    private lazy var bgView: UIView = {
        let view = UIView()
        view.backgroundColor = .commerce_chat_input_bg
        view.layer.cornerRadius = kTextFiledHeight * 0.5
        view.layer.masksToBounds = true
        return view
    }()
    
    lazy var textField: UITextField = {
        let textField = UITextField()
        textField.delegate = self
        textField.font = .commerce_R_14
        textField.textColor = .commerce_chat_input_text
        textField.returnKeyType = .send
        return textField
    }()
    
    private lazy var emojiButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_chat_bar_emoji_nor"), for: .normal)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_chat_bar_emoji_nor"), for: .selected)
        button.addTarget(self, action: #selector(didClickEmojiButton), for: .touchUpInside)
        button.isHidden = true
        return button
    }()
    
    private lazy var sendButton: UIButton = {
        let button = UIButton(type: .custom)
        button.titleLabel?.font = .commerce_R_14
        button.setTitleColor(.commerce_main_text, for: .normal)
        button.setBackgroundImage(UIImage.commerce_sceneImage(name: "show_live_chat_bar_send"), for: .normal)
        button.setTitle("show_live_chat_bar_send".commerce_localized, for: .normal)
        button.addTarget(self, action: #selector(didClickSendButton), for: .touchUpInside)
        return button
    }()
        
    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        backgroundColor = .white
        
        addSubview(sendButton)
        sendButton.snp.makeConstraints { make in
            make.right.equalTo(-15)
            make.centerY.equalToSuperview()
        }
        
        addSubview(bgView)
        bgView.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.height.equalTo(kTextFiledHeight)
            make.left.equalTo(15)
            make.right.equalTo(sendButton.snp.left).offset(-20)
        }
        
        bgView.addSubview(emojiButton)
        emojiButton.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.right.equalTo(-10)
            make.width.height.equalTo(30)
        }
        
        bgView.addSubview(textField)
        textField.snp.makeConstraints { make in
            make.left.equalTo(10)
            make.top.bottom.equalToSuperview()
            make.right.equalTo(emojiButton.snp.left).offset(-5)
        }
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
