//
//  DFSwitchContentView.swift
//  DreamFlow
//
//  Created by qinhui on 2024/10/14.
//

import Foundation

class DFSwitchContentView: UIView {
    let titleLabel = UILabel()
    let switchControl = UISwitch()
    let lineView = UIView()
    let backView = UIView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        configSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func configSubviews() {
        titleLabel.font = UIFont.show_R_14
        titleLabel.textColor = UIColor.show_Ellipse6
        switchControl.onTintColor = UIColor.show_zi03
        lineView.backgroundColor = UIColor.dream_flow_line

        self.addSubview(backView)
        self.addSubview(titleLabel)
        self.addSubview(switchControl)
        self.addSubview(lineView)
        
        backView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
            make.height.equalTo(48)
        }
        
        titleLabel.snp.makeConstraints { make in
            make.leading.equalTo(0)
            make.centerY.equalToSuperview()
        }
        
        switchControl.snp.makeConstraints { make in
            make.trailing.equalToSuperview()
            make.centerY.equalToSuperview()
        }
        
        lineView.snp.makeConstraints { make in
            make.left.right.bottom.equalTo(0)
            make.height.equalTo(1)
        }
    }
}
