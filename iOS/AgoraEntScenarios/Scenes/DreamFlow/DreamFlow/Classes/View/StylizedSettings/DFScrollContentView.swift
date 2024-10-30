//
//  DFScrollContentView.swift
//  DreamFlow
//
//  Created by qinhui on 2024/10/14.
//

import Foundation

class DFScrollContentView: UIView {
    let scrollView = UIScrollView()
    let stackView = UIStackView()
    var selectHandler: ((Int) -> Void)?
    
    private var cardViews: [UIView] = []
    private var selectedIndex = 0
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        configSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func configSubviews() {
        stackView.axis = .horizontal
        stackView.spacing = 10
        scrollView.addSubview(stackView)
        self.addSubview(scrollView)

        scrollView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        stackView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    }
    
    func setData(items: [DFStylizedSettingItem], selectedItem: DFStylizedSettingItem) {
        var selectedIndex = items.firstIndex(where: {$0.title == selectedItem.title}) ?? 0
        if cardViews.isEmpty || cardViews.count != items.count {
            stackView.arrangedSubviews.forEach { $0.removeFromSuperview() }
            for (index, item) in items.enumerated() {
                let cardView = createCardView(with: item, index: index)
                stackView.addArrangedSubview(cardView)
                cardViews.append(cardView)
            }
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
        if let selectHandler = selectHandler {
            selectHandler(selectedIndex)
        }
    }
    
    private func setSelectedIndex(_ index: Int) {
        selectedIndex = index
        
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
    
}
