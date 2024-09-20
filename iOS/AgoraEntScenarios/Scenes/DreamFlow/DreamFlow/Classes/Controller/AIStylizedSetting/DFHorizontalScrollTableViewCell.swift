//
//  HorizontalScrollTableViewCell.swift
//  DreamFlow
//
//  Created by qinhui on 2024/8/30.
//

import UIKit

class DFHorizontalScrollTableViewCell: DFStylizedCell {
    let scrollView = UIScrollView()
    let titleLabel = UILabel()
    let stackView = UIStackView()
    var selectedItem: DFStylizedSettingItem!
    var items: [DFStylizedSettingItem]!
    
    var styleHandler: ((DFStylizedSettingItem) -> Void)?
    var effectHandler: ((DFStylizedSettingItem) -> Void)?
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        stackView.axis = .horizontal
        stackView.spacing = 10
        
        scrollView.addSubview(stackView)
        contentView.addSubview(titleLabel)
        contentView.addSubview(scrollView)

        configSubViews()
        setUserInteractionEnabled(enabled: true)
    }
    
    func configSubViews() {
        titleLabel.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(20)
            make.top.equalToSuperview().offset(8)
        }
        
        scrollView.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(20)
            make.trailing.equalToSuperview().offset(-20)
            make.top.equalTo(titleLabel.snp.bottom).offset(8)
            make.bottom.equalToSuperview().offset(-8)
            make.height.equalTo(100)
        }
        
        stackView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
            make.height.equalToSuperview()
        }
        
        darkView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    }
    
    func setData(with title: String, items: [DFStylizedSettingItem], selectedItem: DFStylizedSettingItem) {
        self.items = items
        
        titleLabel.text = title
        titleLabel.font = UIFont.show_R_14
        
        stackView.arrangedSubviews.forEach { $0.removeFromSuperview() }
        
        var selectedIndex = 0
        for (index, item) in items.enumerated() {
            if (item.title == selectedItem.title) {
                selectedIndex = index
            }
            
            let cardView = createCardView(with: item, index: index)
            stackView.addArrangedSubview(cardView)
        }
        
        setSelectedIndex(selectedIndex)
    }
    
    private func createCardView(with item: DFStylizedSettingItem, index: Int) -> UIView {
        let cardView = UIView()
        cardView.snp.makeConstraints { make in
            make.width.equalTo(70)
        }
        
        let imageView = UIImageView()
        imageView.backgroundColor = .lightGray
        imageView.layer.cornerRadius = 10
        imageView.layer.masksToBounds = true
        imageView.image = UIImage.show_sceneImage(name: item.imageName)
        imageView.snp.makeConstraints { make in
            make.height.width.equalTo(70)
        }
        
        let titleLabel = UILabel()
        titleLabel.text = item.title
        titleLabel.font = UIFont.show_R_12
        titleLabel.textAlignment = .center
        
        let stackView = UIStackView(arrangedSubviews: [imageView, titleLabel])
        stackView.axis = .vertical
        stackView.spacing = 3
        
        cardView.addSubview(stackView)
        
        stackView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        // 添加选中效果
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(imageViewTapped(_:)))
        imageView.addGestureRecognizer(tapGesture)
        imageView.isUserInteractionEnabled = true
        imageView.tag = index
        
        return cardView
    }
    
    @objc private func imageViewTapped(_ sender: UITapGestureRecognizer) {
        guard let imageView = sender.view as? UIImageView else { return }
        setSelectedIndex(imageView.tag)
        execHandler()
    }
    
    func execHandler() {
        if let styleHandler = styleHandler {
            styleHandler(selectedItem)
        }
        
        if let effectHandler = effectHandler {
            effectHandler(selectedItem)
        }
    }
    
    private func setSelectedIndex(_ index: Int) {
        selectedItem = items[index]
        
        // 取消其他图片的选中状态
        stackView.arrangedSubviews.forEach { view in
            if let cardView = view as? UIView, let imageView = cardView.subviews.first?.subviews.first as? UIImageView {
                imageView.layer.borderWidth = 0
                imageView.layer.borderColor = nil
            }
        }
        
        // 设置选中图片的边框
        if let selectedView = stackView.arrangedSubviews[index] as? UIView, let selectedImageView = selectedView.subviews.first?.subviews.first as? UIImageView {
            selectedImageView.layer.borderWidth = 2
            selectedImageView.layer.borderColor = UIColor.show_zi03.cgColor
            
            // 检查选中项是否完全可见
            let selectedFrame = selectedView.convert(selectedView.bounds, to: scrollView)
            if !scrollView.bounds.contains(selectedFrame) {
                scrollView.scrollRectToVisible(selectedFrame, animated: true)
            }
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
