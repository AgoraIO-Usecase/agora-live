//
//  CommerceAuctionShoppingView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/21.
//

import UIKit

class CommerceAuctionShoppingView: UIView {
    var startBidGoodsClosure: ((CommerceGoodsAuctionModel?) -> Void)?
    var endBidGoodsClosure: ((CommerceGoodsAuctionModel?) -> Void)?
    var bidInAuctionGoodsClosure: ((CommerceGoodsAuctionModel?) -> Void)?
    
    private lazy var stackView: UIStackView = {
        let stackView = UIStackView()
        stackView.spacing = 0
        stackView.axis = .vertical
        stackView.alignment = .fill
        stackView.distribution = .fill
        stackView.translatesAutoresizingMaskIntoConstraints = false
        return stackView
    }()
    private lazy var shoppingContainerView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor(hex: "#000000", alpha: 0.25)
        view.layer.cornerRadius = 12
        view.layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner, .layerMinXMaxYCorner, .layerMaxXMaxYCorner]
        view.layer.masksToBounds = true
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    private lazy var coverImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage.sceneImage(name: ""))
        imageView.contentMode = .scaleAspectFit
        imageView.layer.cornerRadius = 6
        imageView.layer.masksToBounds = true
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Micro USB to USB-A 2.0 Cable, Nylon Braided Cord, 480Mbps Transfer Speed, Gold-Plated, 10 Foot, Dark Gray"
        label.textColor = UIColor(hex: "#FFFFFF", alpha: 1.0)
        label.font = .systemFont(ofSize: 16, weight: .bold)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var descLabel: UILabel = {
        let label = UILabel()
        label.text = "Start from"
        label.textColor = UIColor(hex: "#FFFFFF", alpha: 1.0)
        label.font = .systemFont(ofSize: 15)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var priceLabel: UILabel = {
        let label = UILabel()
        label.text = "$1"
        label.textColor = UIColor(hex: "#FFFFFF", alpha: 1.0)
        label.font = .systemFont(ofSize: 18, weight: .bold)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var bidUserView: BidUserView = {
        let bidUserView = BidUserView()
        bidUserView.translatesAutoresizingMaskIntoConstraints = false
        return bidUserView
    }()
    private lazy var bidUserTransition: CATransition = {
        let transition = CATransition()
        transition.duration = 0.5
        transition.type = CATransitionType(rawValue: "cube")
        transition.subtype = .fromTop
        return transition
    }()
    
    private lazy var statusButton: UIButton = {
        let button = UIButton()
        button.setTitle("Start", for: .normal)
        button.setTitleColor(UIColor(hex: "#191919", alpha: 1.0), for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 15, weight: .bold)
        button.cornerRadius(18)
        button.addTarget(self, action: #selector(onClickStartAuctionButton(sender:)), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    private lazy var bidContainerView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor(hex: "#000000", alpha: 0.25)
        view.layer.cornerRadius = 28
        view.layer.maskedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]
        view.layer.masksToBounds = true
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    private lazy var bidButton: UIButton = {
        let button = UIButton()
        button.setTitle("Auction Not Started", for: .normal)
        button.setTitleColor(UIColor(hex: "#FFFFFF", alpha: 1.0), for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 15)
        button.cornerRadius(20)
        button.backgroundColor = UIColor(hex: "#FFFFFF", alpha: 0.25)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(onClickBidInAuctionButton(sender:)), for: .touchUpInside)
        return button
    }()
    private var isBroadcastor: Bool = false
    private var currentAuctionModel: CommerceGoodsAuctionModel?
    private var timer: Timer?
    private var lastUid: String?
    
    init(isBroadcastor: Bool = false) {
        super.init(frame: .zero)
        self.isBroadcastor = isBroadcastor
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setGoodsData(model: CommerceGoodsAuctionModel, isBroadcaster: Bool) {
        currentAuctionModel = model
        coverImageView.sd_setImage(with: URL(string: model.goods?.imageName ?? ""),
                                   placeholderImage: UIImage.commerce_sceneImage(name: model.goods?.imageName ?? ""))
        titleLabel.text = model.goods?.title
        descLabel.text = (model.bidUser == nil || model.bidUser?.id == "") ? "Start from" : "Current Bid:"
        priceLabel.text = "$\(model.goods?.price ?? 0)"
        bidContainerView.isHidden = isBroadcaster
        shoppingContainerView.layer.maskedCorners = isBroadcaster ? [.layerMinXMinYCorner, .layerMaxXMinYCorner, .layerMinXMaxYCorner, .layerMaxXMaxYCorner] : [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        
        bidUserView.isHidden = model.bidUser == nil || model.bidUser?.id == ""
        model.status = (model.bidUser?.id == VLUserCenter.user.id && model.status == .started) ? .top_price : model.status
        bidUserView.setShoppingData(model: model)
        bidUserAnimation(uid: model.bidUser?.id ?? "")
        statusButton.isHidden = isBroadcaster ? false : (model.status == .idle || model.status == .completion)
        statusButton.setTitleColor(model.status.statusTitleColor, for: .normal)
        statusButton.setBackgroundImage(createGradientImage(colors: model.status.statusBackgroundColor), for: .normal)
        bidButton.setTitleColor(model.status.bidTitleColor, for: .normal)
        bidButton.setBackgroundImage(createGradientImage(colors: model.status.bidBackgroundColor), for: .normal)
        bidButton.isUserInteractionEnabled = model.status == .started
        switch model.status {
        case .idle, .completion:
            bidButton.setTitle("Auction Not Started", for: .normal)
            statusButton.setTitle("Start", for: .normal)
            
        case .started:
            bidButton.setTitle("Bid: $\(model.bid)", for: .normal)
            setupTimer()
            
        case .top_price:
            bidButton.setTitle("You are the leading bidder.", for: .normal)
            setupTimer()
        }
    }
    
    private func setupTimer() {
        guard timer == nil else { return }
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: { [weak self] t in
            guard let self = self else { return }
            let time = 30 - (Date().millionsecondSince1970() / 1000 - (self.currentAuctionModel?.timestamp ?? 0) / 1000)
            self.statusButton.setTitle(self.convertToTimeFormat(seconds: Int(time)), for: .normal)
            if time <= 0 {
                t.invalidate()
                self.timer = nil
                guard self.isBroadcastor else { return }
                self.currentAuctionModel?.status = .completion
                self.endBidGoodsClosure?(self.currentAuctionModel)
            }
        })
        timer?.fire()
        if let t = timer {
            RunLoop.current.add(t, forMode: .common)
        }
    }
    
    private func convertToTimeFormat(seconds: Int) -> String {
        let minutes = (seconds % 3600) / 60
        let seconds = (seconds % 3600) % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
    
    private func setupUI() {
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        addSubview(stackView)
        stackView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 12).isActive = true
        stackView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        stackView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -12).isActive = true
        stackView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        
        stackView.addArrangedSubview(shoppingContainerView)
        stackView.addArrangedSubview(bidContainerView)
        
        shoppingContainerView.addSubview(coverImageView)
        coverImageView.leadingAnchor.constraint(equalTo: shoppingContainerView.leadingAnchor, constant: 8).isActive = true
        coverImageView.topAnchor.constraint(equalTo: shoppingContainerView.topAnchor, constant: 8).isActive = true
        coverImageView.bottomAnchor.constraint(equalTo: shoppingContainerView.bottomAnchor, constant: -8).isActive = true
        coverImageView.heightAnchor.constraint(equalToConstant: 72).isActive = true
        coverImageView.widthAnchor.constraint(equalToConstant: 72).isActive = true
        
        shoppingContainerView.addSubview(titleLabel)
        titleLabel.leadingAnchor.constraint(equalTo: coverImageView.trailingAnchor, constant: 12).isActive = true
        titleLabel.topAnchor.constraint(equalTo: coverImageView.topAnchor).isActive = true
        titleLabel.trailingAnchor.constraint(equalTo: shoppingContainerView.trailingAnchor, constant: -8).isActive = true
        
        shoppingContainerView.addSubview(descLabel)
        descLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor).isActive = true
        descLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 4).isActive = true
        
        shoppingContainerView.addSubview(priceLabel)
        priceLabel.leadingAnchor.constraint(equalTo: descLabel.leadingAnchor).isActive = true
        priceLabel.topAnchor.constraint(equalTo: descLabel.bottomAnchor, constant: 2).isActive = true
        
        shoppingContainerView.addSubview(bidUserView)
        bidUserView.leadingAnchor.constraint(equalTo: priceLabel.trailingAnchor, constant: 8).isActive = true
        bidUserView.centerYAnchor.constraint(equalTo: priceLabel.centerYAnchor).isActive = true
                        
        shoppingContainerView.addSubview(statusButton)
        statusButton.bottomAnchor.constraint(equalTo: priceLabel.bottomAnchor).isActive = true
        statusButton.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor).isActive = true
        statusButton.widthAnchor.constraint(equalToConstant: 96).isActive = true
        statusButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        
        bidContainerView.addSubview(bidButton)
        bidButton.leadingAnchor.constraint(equalTo: coverImageView.leadingAnchor).isActive = true
        bidButton.topAnchor.constraint(equalTo: bidContainerView.topAnchor, constant: 8).isActive = true
        bidButton.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor).isActive = true
        bidButton.bottomAnchor.constraint(equalTo: bidContainerView.bottomAnchor, constant: -8).isActive = true
        bidButton.heightAnchor.constraint(equalToConstant: 44).isActive = true
    }
    
    @objc
    private func onClickStartAuctionButton(sender: UIButton) {
        guard let model = currentAuctionModel, isBroadcastor, model.status != .started else { return }
        model.status = .started
        model.timestamp = Date().millionsecondSince1970()
        model.bidUser = VLLoginModel()
        model.bid = 1
        startBidGoodsClosure?(model)
    }
    
    @objc
    private func onClickBidInAuctionButton(sender: UIButton) {
        guard let model = currentAuctionModel else { return }
        model.bidUser = VLUserCenter.user
        model.bid += 1
        bidInAuctionGoodsClosure?(model)
        bidUserView.setShoppingData(model: model)
        bidUserAnimation(uid: VLUserCenter.user.id)
    }
    
    private func bidUserAnimation(uid: String) {
        guard lastUid != uid else { return }
        bidUserView.layer.add(bidUserTransition, forKey: nil)
        lastUid = uid
    }
}


class BidUserView: UIView {
    private lazy var avatarImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage.commerce_sceneImage(name: "show_default_avatar"))
        imageView.cornerRadius(7)
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    private lazy var nickNameLabel: UILabel = {
        let label = UILabel()
        label.text = ""
        label.textColor = UIColor(hex: "#BBBEBF", alpha: 1.0)
        label.font = .systemFont(ofSize: 15)
        label.setContentHuggingPriority(.defaultHigh, for: .horizontal)
        label.setContentCompressionResistancePriority(.defaultHigh, for: .horizontal)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setShoppingData(model: CommerceGoodsAuctionModel) {
        avatarImageView.sd_setImage(with: URL(string: model.bidUser?.headUrl ?? ""),
                                    placeholderImage: UIImage(named: model.bidUser?.headUrl ?? ""))
        let nickName = model.bidUser?.name.prefix(1) ?? ""
        nickNameLabel.text = "\(String(describing: nickName))**"
        if model.bidUser?.id == VLUserCenter.user.id {
            nickNameLabel.text = "You"
        }
    }
    
    private func setupUI() {
        addSubview(avatarImageView)
        avatarImageView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        avatarImageView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        avatarImageView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
        avatarImageView.widthAnchor.constraint(equalToConstant: 14).isActive = true
        avatarImageView.heightAnchor.constraint(equalToConstant: 14).isActive = true
        
        addSubview(nickNameLabel)
        nickNameLabel.leadingAnchor.constraint(equalTo: avatarImageView.trailingAnchor, constant: 4).isActive = true
        nickNameLabel.centerYAnchor.constraint(equalTo: avatarImageView.centerYAnchor).isActive = true
        nickNameLabel.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
    }
}
