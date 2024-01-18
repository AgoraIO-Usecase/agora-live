//
//  VoiceRoomChatCell.swift
//  Pods-VoiceRoomBaseUIKit_Example
//
//Created by Zhu Jichao on August 30, 2022
//

import UIKit
import ZSwiftBaseLib

public class VoiceRoomChatCell: UITableViewCell {
    var chat: VoiceRoomChatEntity?

    lazy var container: UIImageView = .init(frame: CGRect(x: 15, y: 6, width: self.contentView.frame.width - 30, height: self.frame.height - 6)).image(UIImage.sceneImage(name: "chatBg", bundleName: "VoiceChatRoomResource")!).backgroundColor(.clear)

    lazy var content: UILabel = .init(frame: CGRect(x: 10, y: 7, width: self.container.frame.width - 20, height: self.container.frame.height - 18)).backgroundColor(.clear).numberOfLines(0).lineBreakMode(.byWordWrapping)

    override public init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = .clear
        contentView.backgroundColor = .clear
        contentView.addSubview(container)
        container.addSubview(content)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func refresh(chat: VoiceRoomChatEntity) {
        container.frame = CGRect(x: 15, y: 6, width: chat.width! + 30, height: chat.height! - 6)
        content.attributedText = chat.attributeContent
        content.preferredMaxLayoutWidth = container.frame.width - 24
        content.frame = CGRect(x: 12, y: 7, width: container.frame.width - 24, height: container.frame.height - 16)
        container.image = (chat.joined == true ? UIImage.sceneImage(name: "joined_msg_bg", bundleName: "VoiceChatRoomResource")!.resizableImage(withCapInsets: UIEdgeInsets(top: 8, left: 12, bottom: 8, right: 12), resizingMode: .stretch) : UIImage.sceneImage(name: "chatBg", bundleName: "VoiceChatRoomResource")!)
    }
}
