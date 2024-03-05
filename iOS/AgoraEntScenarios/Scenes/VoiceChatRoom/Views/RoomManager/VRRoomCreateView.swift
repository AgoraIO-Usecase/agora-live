//
//  VRRoomCreateView.swift
//  VoiceRoomBaseUIKit
//
//Created by Zhu Jichao on August 24, 2022
//

import UIKit
import ZSwiftBaseLib

public class VRRoomCreateView: UIImageView {
    var action: (() -> Void)?

    lazy var createRoom: UIButton = .init(type: .custom).frame(.zero).addTargetFor(self, action: #selector(createAction), for: .touchUpInside).font(.systemFont(ofSize: 16, weight: .semibold))

    lazy var createContainer: UIView = .init(frame: CGRect(x: ScreenWidth / 2.0 - 74, y: 10, width: 148, height: 56))

    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(createContainer)
        addSubview(createRoom)
//        createContainer.layer.cornerRadius = 25
//        createContainer.layer.shadowRadius = 8
//        createContainer.layer.shadowOffset = CGSize(width: 0, height: 4)
//        createContainer.layer.shadowColor = UIColor(red: 0, green: 0.55, blue: 0.98, alpha: 0.2).cgColor
//        createContainer.layer.shadowOpacity = 1
        isUserInteractionEnabled = true
        createRoom.imageView?.contentMode = .scaleAspectFit
        createRoom.setBackgroundImage(UIImage.voice_image("create_room"), for: .normal)
        createRoom.setImage(UIImage.voice_image("create_room_add"), for: .normal)
        createRoom.setTitle("voice_create_room".voice_localized, for: .normal)
        createRoom.spacingBetweenImageAndTitle = 7
        createRoom.accessibilityIdentifier = "voice_chat_create_room_button_title"
        createRoom.translatesAutoresizingMaskIntoConstraints = false
        createRoom.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        createRoom.topAnchor.constraint(equalTo: topAnchor, constant: 10).isActive = true
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    @objc private func createAction() {
        if action != nil {
            action!()
        }
    }
}

