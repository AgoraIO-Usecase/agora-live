//
//  HomeMenuCell.swift
//  AgoraEntScenarios
//
//  Created by 朱继超 on 2022/10/27.
//

import UIKit

final class HomeMenuCell: UICollectionViewCell {
    lazy var itemView: VLHomeItemView = .init(frame: self.contentView.frame)

    private var isClicked = false
        
    @objc func handleClick() {
        guard !isClicked else {
            return
        }
        isClicked = true
    }
    
    override func prepareForReuse() {
        isClicked = false 
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        contentView.addSubview(itemView)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    @objc func refresh(item: VLHomeItemModel) {
        itemView.itemModel = item
    }
}
