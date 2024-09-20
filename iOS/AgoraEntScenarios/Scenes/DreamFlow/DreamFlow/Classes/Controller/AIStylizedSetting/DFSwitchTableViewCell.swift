//
//  SwitchTableViewCell.swift
//  DreamFlow
//
//  Created by qinhui on 2024/8/30.
//

import UIKit

class DFSwitchTableViewCell: DFStylizedCell {
    private let titleLabel = UILabel()
    private let switchControl = UISwitch()
    
    var switchHandler: ((Bool) -> Void)?
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        titleLabel.font = UIFont.show_R_14
        titleLabel.textColor = UIColor.show_Ellipse6
        
        switchControl.onTintColor = UIColor.show_zi03
        switchControl.addTarget(self, action: #selector(switchAction(_:)), for: .touchUpInside)
        contentView.addSubview(titleLabel)
        contentView.addSubview(switchControl)
        
        titleLabel.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(20)
            make.height.equalTo(48)
        }
        
        switchControl.snp.makeConstraints { make in
            make.trailing.equalToSuperview().offset(-20)
            make.centerY.equalToSuperview()
        }
        
        darkView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    
        setUserInteractionEnabled(enabled: true)
    }
    
    func setData(title: String, state: Bool) {
        titleLabel.text = title
        switchControl.setOn(state, animated: false)
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
