//
//  HorizontalScrollTableViewCell.swift
//  DreamFlow
//
//  Created by qinhui on 2024/8/30.
//

import UIKit

class DFPresetsScrollTableViewCell: UITableViewCell {
    let titleLabel = UILabel()
    let presetsContentView = DFScrollContentView()
    
    var items: [DFPresetSettingItem]!
    var effectSelectHandler: ((DFPresetSettingItem) -> Void)?
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)

        contentView.addSubview(titleLabel)
        contentView.addSubview(presetsContentView)

        configSubViews()
    }
    
    func configSubViews() {
        titleLabel.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(20)
            make.top.equalToSuperview().offset(8)
        }
        
        presetsContentView.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(20)
            make.trailing.equalToSuperview().offset(-20)
            make.top.equalTo(titleLabel.snp.bottom).offset(8)
            make.bottom.equalToSuperview().offset(-8)
            make.height.equalTo(100)
        }
        
        presetsContentView.selectHandler = { [weak self] index in
            guard let self = self, let effectSelectHandler = effectSelectHandler else { return }
            var selectedItem = self.items[index]
            effectSelectHandler(selectedItem)
        }
    }
    
    func setData(with title: String, items: [DFPresetSettingItem], selectedItem: DFPresetSettingItem) {
        self.items = items
        
        titleLabel.text = title
        titleLabel.font = UIFont.show_R_14

        presetsContentView.setData(items: items, selectedItem: selectedItem)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
