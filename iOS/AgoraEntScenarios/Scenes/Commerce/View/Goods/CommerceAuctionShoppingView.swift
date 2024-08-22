//
//  CommerceAuctionShoppingView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/21.
//

import UIKit

class CommerceAuctionShoppingView: UIView {
    var startBidGoodsClosure: (() -> Void)?
    var endBidGoodsClosure: ((CommerceGoodsAuctionModel?) -> Void)?
    var bidInAuctionGoodsClosure: ((CommerceGoodsAuctionModel?) -> Void)?
    var getCurrentTsClosure: (()->UInt64)?
    
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
        let imageView = UIImageView()
        imageView.contentMode = .scaleAspectFit
        imageView.layer.cornerRadius = 6
        imageView.layer.masksToBounds = true
        imageView.translatesAutoresizingMaskIntoConstraints = false
        return imageView
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Diamond Ring"
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
    
    private lazy var activityView: UIActivityIndicatorView = {
        let view = UIActivityIndicatorView(style: .medium)
        view.translatesAutoresizingMaskIntoConstraints = false
        view.hidesWhenStopped = true
        view.color = .black
        view.backgroundColor = UIColor(hex: "#DFE1E6", alpha: 1.0)
        return view
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
    
    func currentGoodStatus() -> CommerceAuctionStatus? {
        return currentAuctionModel?.status
    }
    
    func toggleLoadingIndicator(_ loading: Bool) {
        if loading {
            statusButton.isUserInteractionEnabled = false
            activityView.startAnimating()
        } else {
            statusButton.isUserInteractionEnabled = true
            activityView.stopAnimating()
        }
    }
    
    func setGoodsData(model: CommerceGoodsAuctionModel, isBroadcaster: Bool) {
        currentAuctionModel = model
        let imageName = kDefaultAuctionGoodsName
        coverImageView.image = UIImage.commerce_sceneImage(name: imageName)
        titleLabel.text = model.goods?.title
        descLabel.text = (model.bidUser == nil || model.bidUser?.id == "") ? "Start from" : "Current Bid:"
        let bidPrint = model.status == .completion ? (model.goods?.price ?? 1) : model.bid
        priceLabel.text = "$\(bidPrint)"
        bidContainerView.isHidden = isBroadcaster
        shoppingContainerView.layer.maskedCorners = isBroadcaster ? [.layerMinXMinYCorner, .layerMaxXMinYCorner, .layerMinXMaxYCorner, .layerMaxXMaxYCorner] : [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        
        bidUserView.isHidden = model.status == .completion || model.bidUser == nil || model.bidUser?.id == ""
        bidUserView.setShoppingData(model: model)
        bidUserAnimation(uid: model.bidUser?.id ?? "")
        statusButton.isHidden = isBroadcaster ? false : (model.status == .idle || model.status == .completion)
        statusButton.setTitleColor(model.status.statusTitleColor, for: .normal)
        statusButton.setBackgroundImage(createGradientImage(colors: model.status.statusBackgroundColor), for: .normal)
        bidButton.setTitleColor(model.status.bidTitleColor, for: .normal)
        bidButton.setBackgroundImage(createGradientImage(colors: model.bidBackgroundColor()), for: .normal)
        bidButton.isUserInteractionEnabled = model.status == .started
        switch model.status {
        case .idle, .completion:
            bidButton.setTitle("Auction Not Started", for: .normal)
            statusButton.setTitle("Start", for: .normal)
            
        case .started:
            bidButton.setTitle(model.isTopPrice() ? "You are the leading bidder." : "Bid: $\(model.bid)", for: .normal)
            setupTimer()
        }
    }
    
    private func setupTimer() {
        guard timer == nil else { return }
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: { [weak self] t in
            guard let self = self else { return }
            let startTs = Int64(self.currentAuctionModel?.startTimestamp ?? 0)
            let currentTs = Int64(self.getCurrentTsClosure?() ?? 0)
            let time = max(30 - max((currentTs - startTs) / 1000, 0), 0)
            self.statusButton.setTitle(self.convertToTimeFormat(seconds: Int(time)), for: .normal)
            if time <= 0 {
                t.invalidate()
                self.timer = nil
                guard self.isBroadcastor else { return }
                self.endBidGoodsClosure?(self.currentAuctionModel)
            }
        })
        timer?.fire()
        if let t = timer {
            RunLoop.current.add(t, forMode: .common)
        }
    }
    
    func checkRetryCompletion() {
        let startTs = Int64(self.currentAuctionModel?.startTimestamp ?? 0)
        let currentTs = Int64(self.getCurrentTsClosure?() ?? 0)
        let time = max(30 - max((currentTs - startTs) / 1000, 0), 0)
        guard self.isBroadcastor, time <= 0, currentAuctionModel?.status == .started else { return }
        self.endBidGoodsClosure?(self.currentAuctionModel)
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
        
        statusButton.addSubview(activityView)
        NSLayoutConstraint.activate([
            activityView.widthAnchor.constraint(equalTo: statusButton.widthAnchor),
            activityView.heightAnchor.constraint(equalTo: statusButton.heightAnchor),
            activityView.centerXAnchor.constraint(equalTo: statusButton.centerXAnchor),
            activityView.centerYAnchor.constraint(equalTo: statusButton.centerYAnchor)
        ])
    }
    
    @objc
    private func onClickStartAuctionButton(sender: UIButton) {
        guard let model = currentAuctionModel, isBroadcastor, model.status != .started else { return }
        startBidGoodsClosure?()
    }
    
    @objc
    private func onClickBidInAuctionButton(sender: UIButton) {
        guard let model = currentAuctionModel, model.isTopPrice() == false else { return }
//        model.bidUser = VLUserCenter.user
//        model.bid += 1
        bidInAuctionGoodsClosure?(model)
//        bidUserView.setShoppingData(model: model)
//        bidUserAnimation(uid: VLUserCenter.user.id)
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
