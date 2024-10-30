//
//  SwitchTableViewCell.swift
//  DreamFlow
//
//  Created by qinhui on 2024/8/30.
//

import UIKit

class DFSwitchTableViewCell: UITableViewCell {
    var switchView: DFSwitchContentView = DFSwitchContentView()
    var switchHandler: ((Bool) -> Void)?
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        switchView.switchControl.addTarget(self, action: #selector(switchAction(_:)), for: .touchUpInside)
        self.contentView.addSubview(switchView)
        
        switchView.snp.makeConstraints { make in
            make.top.bottom.equalTo(0)
            make.leading.equalTo(20)
            make.trailing.equalTo(-20)
        }
    }
    
    func setData(title: String, state: Bool) {
        switchView.titleLabel.text = title
        switchView.switchControl.setOn(state, animated: false)
    }
        
    @objc private func switchAction(_ sender: UISwitch) {
        if let switchHandler = switchHandler {
            switchHandler(sender.isOn)
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
