//
//  CommerceShoppingListView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/20.
//

import UIKit

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
    init(isBroadcaster: Bool) {
        super.init(frame: .zero)
        self.isBroadcaster = isBroadcaster
        setupUI()
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
}

extension CommerceGoodsListView: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        10
    }
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "shoppingCell", for: indexPath) as! CommerceGoodsListViewCell
        
        return cell
    }
}

class CommerceGoodsListViewCell: UITableViewCell {
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
        button.setTitle("Buy", for: .normal)
        button.setTitleColor(UIColor(hex: "#191919", alpha: 1.0), for: .normal)
        button.titleLabel?.font = .systemFont(ofSize: 15, weight: .bold)
        button.cornerRadius(20)
        button.backgroundColor = UIColor(hex: "#DFE1E6", alpha: 1.0)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    private lazy var numberButton: CommerceNumberButton = {
        let button = CommerceNumberButton(frame: .zero)
        button.minValue = 1
        button.maxValue = 100
        button.shakeAnimation = true
        button.numberResult { number in
            print(number)
        }
        button.textField.textColor = UIColor(hex: "#191919", alpha: 1.0)
        button.textField.font = .systemFont(ofSize: 15)
        button.layer.cornerRadius = 20
        button.backgroundColor = UIColor(hex: "#FAFAFA", alpha: 1.0)
        button.borderWidth = 1
        button.borderColor = UIColor(hex: "#DFE1E6", alpha: 1.0)
        button.textFieldBorderWidth = 1
        button.textFieldBorderColor = UIColor(hex: "#DFE1E6", alpha: 1.0)
        button.textFieldHighlightBorderColor = UIColor(hex: "#099DFD", alpha: 1.0)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
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
        statusButton.setTitleColor(model.status.titleColor, for: .normal)
        statusButton.setBackgroundImage(createGradientImage(colors: model.status.backgroundColor), for: .normal)
        statusButton.isHidden = isBroadcaster
        numberButton.isHidden = !isBroadcaster
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
        
        contentView.addSubview(statusButton)
        statusButton.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor).isActive = true
        statusButton.bottomAnchor.constraint(equalTo: priceLabel.bottomAnchor).isActive = true
        statusButton.widthAnchor.constraint(equalToConstant: 100).isActive = true
        statusButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
        statusButton.isHidden = true
        
        contentView.addSubview(numberButton)
        numberButton.trailingAnchor.constraint(equalTo: titleLabel.trailingAnchor).isActive = true
        numberButton.bottomAnchor.constraint(equalTo: priceLabel.bottomAnchor).isActive = true
        numberButton.widthAnchor.constraint(equalToConstant: 120).isActive = true
        numberButton.heightAnchor.constraint(equalToConstant: 36).isActive = true
    }
}
