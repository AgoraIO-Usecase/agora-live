//
//  ShowRoomChatCell.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/8.
//

import UIKit

class CommerceRoomChatCell: UITableViewCell {
    
    private lazy var bgView: UIView = {
        let bgView = UIView()
        bgView.backgroundColor = .commerce_room_info_cover
        bgView.layer.cornerRadius = 13
        bgView.layer.masksToBounds = true
        return bgView
    }()
    
    lazy var msgLabel: UILabel = {
        let label = UILabel()
        label.lineBreakMode = .byWordWrapping
        label.numberOfLines = 0
        label.preferredMaxLayoutWidth = Screen.width - 130
        return label
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        contentView.backgroundColor = .clear
        backgroundColor = .clear
//        transform = CGAffineTransform(rotationAngle: Double.pi)
        contentView.addSubview(bgView)
        bgView.snp.makeConstraints { make in
            make.top.equalToSuperview().offset(5)
            make.bottom.equalToSuperview().offset(-5)
            make.left.equalToSuperview()
        }
        
        bgView.addSubview(msgLabel)
        msgLabel.snp.makeConstraints { make in
            make.left.equalToSuperview().offset(10)
            make.top.equalToSuperview().offset(8)
            make.right.equalToSuperview().offset(-10)
            make.bottom.equalToSuperview().offset(-10)
        }
        self.selectedBackgroundView = UIView()
    }
    
    func setUserName(_ userName: String, msg: String) {
        
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineHeightMultiple = 1.2
        let nameAttributes = [NSAttributedString.Key.paragraphStyle: paragraphStyle,
                              NSAttributedString.Key.font: UIFont.commerce_chat_user_name,
                              NSAttributedString.Key.foregroundColor: UIColor.commerce_chat_user_name]
        let msgAttributes = [NSAttributedString.Key.font: UIFont.commerce_chat_msg,
                             NSAttributedString.Key.foregroundColor: UIColor.commerce_main_text]
        let attributedText = NSMutableAttributedString(string: userName, 
                                                       attributes: nameAttributes as [NSAttributedString.Key : Any])
        let attributedMsg = NSAttributedString(string: ": " + msg, 
                                               attributes: msgAttributes as [NSAttributedString.Key : Any])
        attributedText.append(attributedMsg)
        msgLabel.attributedText = attributedText
    }
}
