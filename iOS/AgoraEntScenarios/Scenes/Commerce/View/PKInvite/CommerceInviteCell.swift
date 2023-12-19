//
//  ShowInviteCell.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2022/11/30.
//

import Foundation
import Agora_Scene_Utils

enum CommercePKInviteStatus: CaseIterable {
    case invite
    case waitting
    case interacting
    case refused
    
    var title: String {
        switch self {
        case .invite: return "show_application".commerce_localized
        case .waitting: return "show_application_waitting".commerce_localized
        case .interacting: return "Interacting".commerce_localized
        case .refused: return "show_reject_onseat".commerce_localized
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
        case .invite: return UIImage.commerce_sceneImage(name: "show_invite_btn_bg")
        case .waitting: return UIImage.commerce_sceneImage(name: "show_invite_btn_bg")
        default: return nil
        }
    }
}

//base invite cell
class CommerceInviteCell: UITableViewCell {
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
class CommercePKInviteViewCell: CommerceInviteCell {
    var pkUser: CommercePKUserInfo? {
        didSet {
            defer {
                _refreshPKStatus()
            }
            
            guard let info = pkUser else { return }
            if (info.ownerAvatar ?? "").hasPrefix("http") {
                avatarImageView.sd_setImage(with: URL(string: info.ownerAvatar ?? ""),
                                            placeholderImage: UIImage.commerce_sceneImage(name: "show_default_avatar"))
            } else {
                avatarImageView.image = UIImage(named: info.ownerAvatar ?? "show_default_avatar")
            }
            nameLabel.text = info.ownerName
        }
    }
    var pkInvitation: CommercePKInvitation? {
        didSet {
            _refreshPKStatus()
        }
    }
    var pkStatus: CommercePKInviteStatus = .invite {
        didSet {
            statusButton.setTitle(pkStatus.title, for: .normal)
            statusButton.setTitleColor(pkStatus.titleColor, for: .normal)
            statusButton.setBackgroundImage(pkStatus.bgImage, for: .normal)
            statusButton.isEnabled = pkStatus != .waitting
        }
    }
    
    private func _refreshPKStatus() {
        var stauts: CommercePKInviteStatus = (pkUser?.interactStatus.isInteracting ?? false) ? .interacting : .invite
        if stauts == .invite {
            stauts = pkInvitation?.status == .waitting ? .waitting : .invite
        }
        pkStatus = stauts
    }
    
    @objc
    fileprivate override func onTapStatusButton(sender: UIButton) {
        super.onTapStatusButton(sender: sender)
        guard let invitation = pkUser, invitation.interactStatus == .idle else {
            return
        }

        AppContext.commerceServiceImp(roomId!)?.createPKInvitation(room: invitation) {[weak self] error in
            if let err = error {
                ToastView.show(text: err.localizedDescription)
                return
            }
            self?.refreshDataClosure?()
        }
    }
}


//mic seat apply and invite cell
class CommerceSeatApplyAndInviteViewCell: CommerceInviteCell {
    private var seatApplyModel: CommerceMicSeatApply?
    private var seatInvitationModel: CommerceUser?

    func setupApplyAndInviteData(model: Any?, isLink: Bool) {
        statusButton.isHidden = isLink
        if let model = model as? CommerceMicSeatApply {
            seatApplyModel = model
            nameLabel.text = model.userName
            statusButton.tag = 1
            if (model.avatar ?? "").hasPrefix("http") {
                avatarImageView.sd_setImage(with: URL(string: model.avatar ?? ""),
                                            placeholderImage: UIImage.commerce_sceneImage(name: "show_default_avatar"))
            } else {
                avatarImageView.image = UIImage(named: model.avatar ?? "")
            }
            
            switch model.status {
            case .accepted:
                statusButton.isUserInteractionEnabled = false
                statusButton.setTitle("show_is_onseat".commerce_localized, for: .normal)
                statusButton.setTitleColor(.black, for: .normal)
                statusButton.setBackgroundImage(nil, for: .normal)
                
            case .waitting:
                statusButton.isUserInteractionEnabled = true
                statusButton.setTitle("show_onseat_agree".commerce_localized, for: .normal)
                statusButton.setBackgroundImage(UIImage.commerce_sceneImage(name: "show_invite_btn_bg"), for: .normal)
                statusButton.setTitleColor(.white, for: .normal)
                
            default: break
            }
            
        } else if let model = model as? CommerceUser {
            seatInvitationModel = model
            nameLabel.text = model.userName
            statusButton.tag = 2
            if (model.avatar ?? "").hasPrefix("http") {
                avatarImageView.sd_setImage(with: URL(string: model.avatar ?? ""),
                                            placeholderImage: UIImage.commerce_sceneImage(name: "show_default_avatar"))
            } else {
                avatarImageView.image = UIImage(named: model.avatar ?? "")
            }
             
            switch model.status {
            case .waitting:
                statusButton.isUserInteractionEnabled = false
                statusButton.setTitle("show_is_waitting".commerce_localized, for: .normal)
                statusButton.setBackgroundImage(nil, for: .normal)
                statusButton.setTitleColor(.black, for: .normal)
                
            default:
                statusButton.setTitle("show_application".commerce_localized, for: .normal)
                statusButton.setBackgroundImage(UIImage.commerce_sceneImage(name: "show_invite_btn_bg"), for: .normal)
                statusButton.setTitleColor(.white, for: .normal)
                statusButton.isUserInteractionEnabled = true
            }
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
        if let model = seatApplyModel, sender.tag == 1 {
            AppContext.commerceServiceImp(roomId!)?.acceptMicSeatApply(apply: model) {[weak self] _ in
                self?.refreshDataClosure?()
            }
        } else if let model = seatInvitationModel {
            AppContext.commerceServiceImp(roomId!)?.createMicSeatInvitation(user: model) {[weak self] error in
                if let err = error {
                    ToastView.show(text: err.localizedDescription)
                    return
                }
                
                self?.refreshDataClosure?()
            }
        }
    }
}
