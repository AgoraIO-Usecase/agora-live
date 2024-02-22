//
//  CommerceAuctionResultView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/20.
//

import UIKit

class CommerceAuctionResultView: UIView {
    private lazy var avatarContainerView: UIImageView = {
        let imageView = UIImageView(image: UIImage.commerce_sceneImage(name: "commerce_auction_avatar_bg"))
        imageView.setContentHuggingPriority(.defaultHigh, for: .vertical)
        imageView.setContentCompressionResistancePriority(.defaultHigh, for: .vertical)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    private lazy var avatarImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage.commerce_sceneImage(name: "show_default_avatar"))
        imageView.cornerRadius(30)
        imageView.contentMode = .scaleAspectFit
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    private lazy var containerView: UIImageView = {
        let view = UIImageView(image: UIImage.commerce_sceneImage(name: "commerce_auction_container_bg"))
        view.cornerRadius(16)
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "armanikid"
        label.textColor = UIColor(hex: "#5C1300", alpha: 1.0)
        label.font = .systemFont(ofSize: 32, weight: .bold)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var descLabel: UILabel = {
        let label = UILabel()
        label.text = "won"
        label.textColor = UIColor(hex: "#191919", alpha: 1.0)
        label.font = .systemFont(ofSize: 18, weight: .bold)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var shoppingNameLabel: UILabel = {
        let label = UILabel()
        label.text = "Micro USB to USB-A 2.0 Cable, Nylon Braided Cord, 480Mbps Transfer Speed, Gold-Plated, 10 Foot, Dark Gray"
        label.textColor = UIColor(hex: "#191919", alpha: 1.0)
        label.font = .systemFont(ofSize: 18, weight: .bold)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var closeButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage.commerce_sceneImage(name: "commerce_auction_close_icon"), for: .normal)
        button.addTarget(self, action: #selector(onClickCloseButton), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    private lazy var countLabel: UILabel = {
        let label = UILabel()
        label.text = "3s"
        label.textColor = UIColor(hex: "#FFFFFF", alpha: 1.0)
        label.font = .systemFont(ofSize: 14)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
        var count: Int = 3
        Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { timer in
            if count <= 0 {
                timer.invalidate()
                self.onClickCloseButton()
            }
            guard count > 0 else { return }
            self.countLabel.text = "\(count)s"
            count -= 1
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setBidGoods(model: CommerceGoodsAuctionModel) {
        avatarImageView.sd_setImage(with: URL(string: model.bidUser?.headUrl ?? ""),
                                    placeholderImage: UIImage.commerce_sceneImage(name: model.goods?.imageName ?? ""))
        titleLabel.text = model.bidUser?.name
        shoppingNameLabel.text = model.goods?.title
    }
    
    private func setupUI() {
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        
        addSubview(containerView)
        containerView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 36).isActive = true
        containerView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -36).isActive = true
        containerView.centerYAnchor.constraint(equalTo: centerYAnchor).isActive = true
        
        addSubview(avatarContainerView)
        avatarContainerView.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        avatarContainerView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        avatarContainerView.bottomAnchor.constraint(equalTo: containerView.topAnchor, constant: 40).isActive = true
        
        avatarContainerView.addSubview(avatarImageView)
        avatarImageView.centerXAnchor.constraint(equalTo: avatarContainerView.centerXAnchor).isActive = true
        avatarImageView.bottomAnchor.constraint(equalTo: avatarContainerView.bottomAnchor, constant: -8).isActive = true
        avatarImageView.widthAnchor.constraint(equalToConstant: 60).isActive = true
        avatarImageView.heightAnchor.constraint(equalToConstant: 60).isActive = true
        
        addSubview(titleLabel)
        titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        titleLabel.topAnchor.constraint(equalTo: avatarContainerView.bottomAnchor, constant: 16.5).isActive = true
        
        addSubview(descLabel)
        descLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        descLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 8).isActive = true
        
        addSubview(shoppingNameLabel)
        shoppingNameLabel.leadingAnchor.constraint(equalTo: containerView.leadingAnchor, constant: 20).isActive = true
        shoppingNameLabel.topAnchor.constraint(equalTo: descLabel.bottomAnchor, constant: 4).isActive = true
        shoppingNameLabel.trailingAnchor.constraint(equalTo: containerView.trailingAnchor, constant: -20).isActive = true
        
        addSubview(closeButton)
        closeButton.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        closeButton.topAnchor.constraint(equalTo: containerView.bottomAnchor, constant: 36).isActive = true
        closeButton.widthAnchor.constraint(equalToConstant: 36).isActive = true
        closeButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        
        addSubview(countLabel)
        countLabel.centerXAnchor.constraint(equalTo: centerXAnchor).isActive = true
        countLabel.topAnchor.constraint(equalTo: closeButton.bottomAnchor, constant: 9).isActive = true
    }
    
    @objc
    private func onClickCloseButton() {
        AlertManager.hiddenView()
    }
}
