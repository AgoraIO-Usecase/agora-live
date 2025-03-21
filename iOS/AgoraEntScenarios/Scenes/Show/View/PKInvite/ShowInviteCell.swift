//
//  ShowInviteCell.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/30.
//

import Foundation
import Agora_Scene_Utils

enum ShowPKInviteStatus: CaseIterable {
    case invite
    case waitting
    case interacting
    case refused
    
    var title: String {
        switch self {
        case .invite: return "show_application".show_localized
        case .waitting: return "show_application_waitting".show_localized
        case .interacting: return "Interacting".show_localized
        case .refused: return "show_reject_onseat".show_localized
        }
    }
    var titleColor: UIColor? {
        switch self {
        case .invite, .waitting: return .white
        default: return .black
        }
    }

    var bgImage: UIImage? {
        switch self {
        case .invite: return UIImage.show_sceneImage(name: "show_invite_btn_bg")
        case .waitting: return UIImage.show_sceneImage(name: "show_invite_btn_bg")
        default: return nil
        }
    }
}

//base invite cell
class ShowInviteCell: UITableViewCell {
    var roomId: String?
    var refreshDataClosure: (() -> Void)?
    fileprivate lazy var avatarImageView: AGEImageView = {
        let imageView = AGEImageView(type: .avatar)
//        imageView.image = UIImage.show_sceneImage(name: "show_default_avatar")
        imageView.cornerRadius = 22
        return imageView
    }()
    fileprivate lazy var nameLabel: AGELabel = {
        let label = AGELabel(colorStyle: .black, fontStyle: .middle)
//        label.text = "Antonovich A"
        return label
    }()
    fileprivate lazy var statusButton: UIButton = {
        let button = UIButton()
        button.titleLabel?.font = .systemFont(ofSize: 14)
        button.addTargetFor(self, action: #selector(onTapStatusButton(sender:)), for: .touchUpInside)
        let bgImage = UIImage.show_sceneImage(name: "show_invite_btn_bg")!
        button.setBackgroundImage(bgImage, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.setTitle("show_is_onseat".show_localized, for: .disabled)
        button.setTitleColor(.white, for: .disabled)
        button.setBackgroundImage(bgImage.color(.gray.withAlphaComponent(0.8), 
                                                width: bgImage.size.width,
                                                height: bgImage.size.height,
                                                cornerRadius: bgImage.size.height / 2), for: .disabled)
        return button
    }()
    fileprivate lazy var lineView: AGEView = {
        let view = AGEView()
        view.backgroundColor = UIColor(hex: "#F8F5FA")
        return view
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        contentView.addSubview(avatarImageView)
        contentView.addSubview(nameLabel)
        contentView.addSubview(statusButton)
        contentView.addSubview(lineView)
        
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        statusButton.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        
        avatarImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20).isActive = true
        avatarImageView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        avatarImageView.widthAnchor.constraint(equalToConstant: 44).isActive = true
        avatarImageView.heightAnchor.constraint(equalToConstant: 44).isActive = true
        
        nameLabel.leadingAnchor.constraint(equalTo: avatarImageView.trailingAnchor, constant: 11).isActive = true
        nameLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        
        statusButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20).isActive = true
        statusButton.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        
        lineView.leadingAnchor.constraint(equalTo: avatarImageView.leadingAnchor).isActive = true
        lineView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        lineView.trailingAnchor.constraint(equalTo: statusButton.trailingAnchor).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
    }
    
    @objc
    fileprivate func onTapStatusButton(sender: UIButton) {
        AlertManager.hiddenView()
    }
}

//pk invite cell
class ShowPKInviteViewCell: ShowInviteCell {
    var isCurrentInteracting: Bool = false
    var pkUser: ShowPKUserInfo? {
        didSet {
            defer {
                _refreshPKStatus()
            }
            
            guard let info = pkUser else { return }
            if (info.ownerAvatar).hasPrefix("http") {
                avatarImageView.sd_setImage(with: URL(string: info.ownerAvatar),
                                            placeholderImage: UIImage.show_sceneImage(name: "show_default_avatar"))
            } else {
                avatarImageView.image = UIImage(named: info.ownerAvatar)
            }
            nameLabel.text = info.ownerName
        }
    }
    var pkInvitation: ShowPKInvitation? {
        didSet {
            _refreshPKStatus()
        }
    }
    var pkStatus: ShowPKInviteStatus = .invite {
        didSet {
            statusButton.setTitle(pkStatus.title, for: .normal)
            statusButton.setTitleColor(pkStatus.titleColor, for: .normal)
            statusButton.setBackgroundImage(pkStatus.bgImage, for: .normal)
//            statusButton.isEnabled = pkStatus != .waitting
        }
    }
    
    private func _refreshPKStatus() {
        var stauts: ShowPKInviteStatus = (pkUser?.status.isInteracting ?? false) ? .interacting : .invite
        if stauts == .invite {
            stauts = pkInvitation?.type == .inviting ? .waitting : .invite
        }
        pkStatus = stauts
    }
    
    @objc
    fileprivate override func onTapStatusButton(sender: UIButton) {
        super.onTapStatusButton(sender: sender)
        guard let roomId = roomId,
              let invitation = pkUser,
              invitation.status == .idle else {
            return
        }
        
        if isCurrentInteracting {
            ToastView.show(text: "show_error_disable_pk".show_localized)
            return
        }

        statusButton.isEnabled = false
        AppContext.showServiceImp()?.createPKInvitation(roomId: roomId,
                                                        pkRoomId: invitation.roomId) {[weak self] error in
            guard let self = self else { return }
            self.statusButton.isEnabled = true
            if let err = error {
                ToastView.show(text: "\("show_request_invite_pk_fail".show_localized)\(err.code)")
                return
            }
            self.refreshDataClosure?()
        }
    }
}


//mic seat apply and invite cell
class ShowSeatApplyAndInviteViewCell: ShowInviteCell {
    private var isCurrentInteracting: Bool = false
    private var seatApplyModel: ShowMicSeatApply?
    private var seatInvitationModel: ShowUser?

    func setupApplyAndInviteData(model: Any?, linkingUid: String?, isInteracting: Bool) {
        self.isCurrentInteracting = isInteracting
        if let model = model as? ShowMicSeatApply {
            statusButton.isEnabled = linkingUid == model.userId ? false : true
            //apply
            seatApplyModel = model
            nameLabel.text = model.userName
            statusButton.tag = 1
            if (model.userAvatar).hasPrefix("http") {
                avatarImageView.sd_setImage(with: URL(string: model.userAvatar),
                                            placeholderImage: UIImage.show_sceneImage(name: "show_default_avatar"))
            } else {
                avatarImageView.image = UIImage(named: model.userAvatar)
            }
            
            statusButton.setTitle("show_onseat_agree".show_localized, for: .normal)
            
        } else if let model = model as? ShowUser {
            statusButton.isEnabled = linkingUid == model.userId ? false : true
            //invit
            seatInvitationModel = model
            nameLabel.text = model.userName
            statusButton.tag = 2
            if (model.userAvatar).hasPrefix("http") {
                avatarImageView.sd_setImage(with: URL(string: model.userAvatar),
                                            placeholderImage: UIImage.show_sceneImage(name: "show_default_avatar"))
            } else {
                avatarImageView.image = UIImage(named: model.userAvatar)
            }
            statusButton.setTitle("show_application".show_localized, for: .normal)
        }
    }
    
    private func setupUI() {
        contentView.addSubview(avatarImageView)
        contentView.addSubview(nameLabel)
        contentView.addSubview(statusButton)
        contentView.addSubview(lineView)
        
        avatarImageView.translatesAutoresizingMaskIntoConstraints = false
        nameLabel.translatesAutoresizingMaskIntoConstraints = false
        statusButton.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        
        avatarImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20).isActive = true
        avatarImageView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        avatarImageView.widthAnchor.constraint(equalToConstant: 44).isActive = true
        avatarImageView.heightAnchor.constraint(equalToConstant: 44).isActive = true
        
        nameLabel.leadingAnchor.constraint(equalTo: avatarImageView.trailingAnchor, constant: 11).isActive = true
        nameLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        
        statusButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20).isActive = true
        statusButton.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        
        lineView.leadingAnchor.constraint(equalTo: avatarImageView.leadingAnchor).isActive = true
        lineView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        lineView.trailingAnchor.constraint(equalTo: statusButton.trailingAnchor).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
    }
    
    @objc
    fileprivate override func onTapStatusButton(sender: UIButton) {
        super.onTapStatusButton(sender: sender)
        guard let roomId = roomId else {return}
        if let model = seatApplyModel, sender.tag == 1 {
            if isCurrentInteracting {
                ToastView.show(text: "show_error_disable_invite_linking".show_localized)
                return
            }
            self.statusButton.isEnabled = false
            AppContext.showServiceImp()?.acceptMicSeatApply(roomId: roomId, userId: model.userId) {[weak self] err in
                guard let self = self else { return }
                self.statusButton.isEnabled = true
                self.refreshDataClosure?()
                if let err = err {
                    ToastView.show(text: "\("show_accept_linking_fail".show_localized)\(err.code)")
                }
            }
        } else if let model = seatInvitationModel {
            if isCurrentInteracting {
                ToastView.show(text: "show_error_disable_linking".show_localized)
                return
            }
            
            self.statusButton.isEnabled = false
            AppContext.showServiceImp()?.createMicSeatInvitation(roomId: roomId, userId: model.userId) {[weak self] error in
                guard let self = self else { return }
                self.statusButton.isEnabled = true
                if let err = error {
                    ToastView.show(text: "\("show_request_invite_linking_fail".show_localized)\(err.code)")
                    return
                }
                
                self.refreshDataClosure?()
            }
        }
    }
}
