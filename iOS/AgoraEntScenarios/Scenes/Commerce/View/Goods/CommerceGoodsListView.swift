//
//  CommerceShoppingListView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/20.
//

import UIKit
import AgoraCommon

class CommerceGoodsListView: UIView {
    private lazy var tableView: UITableView = {
        let tableView = UITableView(frame: .zero, style: .plain)
        tableView.translatesAutoresizingMaskIntoConstraints = false
        tableView.delegate = self
        tableView.dataSource = self
        tableView.backgroundColor = .clear
        tableView.register(CommerceGoodsListViewCell.self, forCellReuseIdentifier: "shoppingCell")
        tableView.separatorStyle = .none
        tableView.keyboardDismissMode = .onDrag
        tableView.translatesAutoresizingMaskIntoConstraints = false
        return tableView
    }()
    private var isBroadcaster: Bool = false
    private var serviceImp: CommerceServiceProtocol?
    private var roomId: String?
    private lazy var goodsList = CommerceGoodsBuyModel.createGoodsData()
    
    init(isBroadcaster: Bool, serviceImp: CommerceServiceProtocol?, roomId: String?) {
        super.init(frame: .zero)
        self.isBroadcaster = isBroadcaster
        self.serviceImp = serviceImp
        self.roomId = roomId
        setupUI()
        subscribeEventGoodsList()
        getGoodsList()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        backgroundColor = .white
        layer.cornerRadius = 12
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        heightAnchor.constraint(equalToConstant: 360).isActive = true
        
        addSubview(tableView)
        tableView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        tableView.topAnchor.constraint(equalTo: topAnchor).isActive = true
        tableView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        tableView.bottomAnchor.constraint(equalTo: bottomAnchor).isActive = true
    }
    
    private func getGoodsList() {
        serviceImp?.getGoodsList(roomId: roomId, completion: { [weak self] error, res in
            if error != nil {
                commerceLogger.error("error == \(error?.localizedDescription ?? "")")
                return
            }
            guard let list = res else { return }
            self?.goodsList = list
            self?.tableView.reloadData()
        })
    }
    
    private func updateGoodsInfo(goods: CommerceGoodsModel?, completion: @escaping (NSError?)-> ()) {
        serviceImp?.updateGoodsInfo(roomId: roomId, goods: goods, completion: { error in
            commerceLogger.error("error == \(error?.localizedDescription ?? "")")
            completion(error)
        })
    }
    
    private func calcGoodsInfo(goods: CommerceGoodsModel?, increase: Bool, completion: @escaping (NSError?)-> ()) {
        serviceImp?.calcGoodsInfo(roomId: roomId, goods: goods, increase: increase, completion: { error in
            commerceLogger.error("error == \(error?.localizedDescription ?? "")")
            completion(error)
        })
    }
    
    private func subscribeEventGoodsList() {
        serviceImp?.subscribeGoodsInfo(roomId: roomId, completion: { [weak self] error, res in
            if error != nil {
                commerceLogger.error("error == \(error?.localizedDescription ?? "")")
                return
            }
            guard let list = res?.compactMap({ item in
                let buyModel = CommerceGoodsBuyModel()
                buyModel.goods = item
                return buyModel
            }) else { return }
            self?.goodsList = list
            self?.tableView.reloadData()
        })
    }
    private func getGoodsInfo(goodsId: String?, callback: @escaping (Int) -> Void) {
        serviceImp?.getGoodsInfo(roomId: roomId, goodsId: goodsId, completion: { _, model in
            callback(model?.goods?.quantity ?? 0)
        })
    }
}

extension CommerceGoodsListView: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        goodsList.count
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "shoppingCell", for: indexPath) as! CommerceGoodsListViewCell
        let model = goodsList[indexPath.row]
        cell.setShoppingData(model: model, isBroadcaster: isBroadcaster)
        func showAlert(title: String) {
            let alertVC = UIAlertController(title: title, message: nil, preferredStyle: .alert)
            let okAction = UIAlertAction(title: "OK", style: .default)
            alertVC.addAction(okAction)
            
            let topVC = UIViewController.cl_topViewController()
            if topVC is UIAlertController {
                return
            }
            topVC?.present(alertVC, animated: false)
        }
        cell.onClickStatusButtonClosure = { [weak self, weak cell] in
            cell?.toggleLoadingIndicator(true)
            self?.calcGoodsInfo(goods: model.goods, increase: false) { err in
                cell?.toggleLoadingIndicator(false)
                var title = "show_buy_goods_success_toast".commerce_localized
                if let err = err {
                    title = "\("show_update_goods_fail_toast".commerce_localized) Error:\(err.code)"
                }
                
                showAlert(title: title)
            }
        }
        
        func makeRequestCompletion(error: NSError?) {
            var title = "show_update_goods_success_toast".commerce_localized
            if let err = error  {
                title = "\("show_update_goods_fail_toast".commerce_localized) Error:\(err.code)"
            }
            
            showAlert(title: title)
        }

        cell.onClickNumberButtonClosure = { [weak self, weak cell] number, isIncrease in
            guard let self = self else { return }
            cell?.toggleLoadingIndicator(true)
            if let isIncrease = isIncrease {
                self.calcGoodsInfo(goods: model.goods, increase: isIncrease) { err in
                    cell?.toggleLoadingIndicator(false)
                    makeRequestCompletion(error: err)
                }
            } else {
                model.goods?.quantity = number
                self.updateGoodsInfo(goods: model.goods) { err in
                    cell?.toggleLoadingIndicator(false)
                    makeRequestCompletion(error: err)
                }
            }
        }
        return cell
    }
}

class CommerceGoodsListViewCell: UITableViewCell {
    var onClickStatusButtonClosure: (() -> Void)?
    var onClickNumberButtonClosure: ((Int, Bool?) -> Void)?
    
    private lazy var coverImageView: UIImageView = {
        let imageView = UIImageView(image: UIImage.sceneImage(name: ""))
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.contentMode = .scaleAspectFit
        return imageView
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Micro USB to USB-A 2.0 Cable, Nylon Braided Cord, 480Mbps Transfer Speed, Gold-Plated, 10 Foot, Dark Gray"
        label.textColor = UIColor(hex: "#191919", alpha: 1.0)
        label.font = .systemFont(ofSize: 16, weight: .bold)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var numberLabel: UILabel = {
        let label = UILabel()
        label.text = "Qty: 6"
        label.textColor = UIColor(hex: "#191919", alpha: 1.0)
        label.font = .systemFont(ofSize: 15)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var priceLabel: UILabel = {
        let label = UILabel()
        label.text = "$20"
        label.textColor = UIColor(hex: "#5C1300", alpha: 1.0)
        label.font = .systemFont(ofSize: 18, weight: .bold)
        label.translatesAutoresizingMaskIntoConstraints = false
        return label
    }()
    private lazy var statusButton: UIButton = {
        let button = UIButton()
        button.setTitle("show_goods_set_title".commerce_localized, for: .selected)
        button.setTitleColor(UIColor(hex: "#191919", alpha: 1.0), for: .selected)
        button.setBackgroundImage(createGradientImage(colors: CommerceBuyStatus.buy.backgroundColor), for: .selected)
        button.titleLabel?.font = .systemFont(ofSize: 15, weight: .bold)
        button.cornerRadius(20)
        button.backgroundColor = UIColor(hex: "#DFE1E6", alpha: 1.0)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(onClickStatusButton), for: .touchUpInside)
        return button
    }()
    private lazy var numberButton: CommerceNumberButton = {
        let button = CommerceNumberButton(frame: .zero)
        button.minValue = 0
        button.maxValue = 99
        button.shakeAnimation = true
        button.numberResult { [weak self] number, isIncrease in
//            let qty = Int(number) ?? 0
//            self?.onClickNumberButtonClosure?(qty, isIncrease)
        }
        button.textField.textColor = UIColor(hex: "#191919", alpha: 1.0)
        button.textField.font = .systemFont(ofSize: 15)
        button.layer.cornerRadius = 20
        button.clipsToBounds = true
        button.backgroundColor = UIColor(hex: "#FAFAFA", alpha: 1.0)
        button.borderWidth = 1
        button.borderColor = UIColor(hex: "#DFE1E6", alpha: 1.0)
        button.textFieldBorderWidth = 1
        button.textFieldBorderColor = UIColor(hex: "#DFE1E6", alpha: 1.0)
        button.textFieldHighlightBorderColor = UIColor(hex: "#099DFD", alpha: 1.0)
        button.translatesAutoresizingMaskIntoConstraints = false
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
    
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setShoppingData(model: CommerceGoodsBuyModel?, isBroadcaster: Bool) {
        guard let model = model else { return }
        coverImageView.sd_setImage(with: URL(string: model.goods?.imageName ?? ""),
                                   placeholderImage: UIImage.commerce_sceneImage(name: model.goods?.imageName ?? ""))
        titleLabel.text = model.goods?.title
        numberLabel.text = "Qty: \(model.goods?.quantity ?? 0)"
        priceLabel.text = "$\(model.goods?.price ?? 0)"
        model.status = (model.goods?.quantity ?? 0) <= 0 ? .sold_out : .buy
        statusButton.setTitle(model.status.title, for: .normal)
        statusButton.isSelected = isBroadcaster
        commerceLogger.info("setShoppingData \(model.goods?.title ?? ""): \(model.goods?.quantity ?? 0)")
        if isBroadcaster {
            statusButton.widthAnchor.constraint(equalToConstant: 64).isActive = true
        } else {
            statusButton.widthAnchor.constraint(equalToConstant: 100).isActive = true
            if model.status != .sold_out {
                statusButton.isEnabled = true
                statusButton.setTitleColor(model.status.titleColor, for: .normal)
                statusButton.setBackgroundImage(createGradientImage(colors: model.status.backgroundColor), for: .normal)
            } else {
                statusButton.isEnabled = false
                statusButton.setTitleColor(model.status.titleColor, for: .disabled)
                statusButton.setBackgroundImage(createGradientImage(colors: model.status.backgroundColor), for: .disabled)
            }
        }
        setNeedsUpdateConstraints()
        updateConstraintsIfNeeded()
        numberButton.isHidden = !isBroadcaster
        numberButton.textField.text = "\(model.goods?.quantity ?? 0)"
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
    
    private func setupUI() {
        selectionStyle = .none
        
        contentView.addSubview(coverImageView)
        coverImageView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 20).isActive = true
        coverImageView.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 24).isActive = true
        coverImageView.widthAnchor.constraint(equalToConstant: 88).isActive = true
        coverImageView.heightAnchor.constraint(equalToConstant: 88).isActive = true
        coverImageView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
        
        contentView.addSubview(titleLabel)
        titleLabel.leadingAnchor.constraint(equalTo: coverImageView.trailingAnchor, constant: 16).isActive = true
        titleLabel.topAnchor.constraint(equalTo: coverImageView.topAnchor).isActive = true
        titleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -20).isActive = true
        
        contentView.addSubview(numberLabel)
        numberLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor).isActive = true
        numberLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 8).isActive = true
        
        contentView.addSubview(priceLabel)
        priceLabel.leadingAnchor.constraint(equalTo: titleLabel.leadingAnchor).isActive = true
        priceLabel.bottomAnchor.constraint(equalTo: coverImageView.bottomAnchor, constant: -4).isActive = true
        
        
//        contentView.addSubview(confirmButton)
//        confirmButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -10).isActive = true
//        confirmButton.bottomAnchor.constraint(equalTo: priceLabel.bottomAnchor).isActive = true
//        confirmButton.widthAnchor.constraint(equalToConstant: 64).isActive = true
//        confirmButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        
        contentView.addSubview(statusButton)
        statusButton.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -10).isActive = true
        statusButton.bottomAnchor.constraint(equalTo: priceLabel.bottomAnchor).isActive = true
        statusButton.widthAnchor.constraint(equalToConstant: 100).isActive = true
        statusButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        
        contentView.addSubview(numberButton)
        numberButton.trailingAnchor.constraint(equalTo: statusButton.leadingAnchor, constant: -10).isActive = true
        numberButton.bottomAnchor.constraint(equalTo: priceLabel.bottomAnchor).isActive = true
        numberButton.widthAnchor.constraint(equalToConstant: 100).isActive = true
        numberButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        
        statusButton.addSubview(activityView)
        NSLayoutConstraint.activate([
            activityView.widthAnchor.constraint(equalTo: statusButton.widthAnchor),
            activityView.heightAnchor.constraint(equalTo: statusButton.heightAnchor),
            activityView.centerXAnchor.constraint(equalTo: statusButton.centerXAnchor),
            activityView.centerYAnchor.constraint(equalTo: statusButton.centerYAnchor)
        ])
    }
    
    @objc
    private func onClickStatusButton() {
        if statusButton.isSelected {
            guard let number = Int(numberButton.currentNumber ?? "") else { return }
            onClickNumberButtonClosure?(number, nil)
        } else {
            self.onClickStatusButtonClosure?()
        }
    }
}
