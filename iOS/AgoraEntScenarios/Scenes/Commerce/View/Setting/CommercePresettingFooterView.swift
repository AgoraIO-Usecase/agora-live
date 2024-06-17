//
//  ShowPresettingFooterView.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/28.
//

import UIKit

class CommercePresettingFooterView: UITableViewHeaderFooterView {
    
    private lazy var bgImgView: UIImageView = {
        let imgView = UIImageView()
        imgView.backgroundColor = .commerce_preset_bg
        return imgView
    }()
    
    override init(reuseIdentifier: String?) {
        super.init(reuseIdentifier: reuseIdentifier)
        createSubviews()
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        bgImgView.commerce_setRoundingCorners([.bottomLeft,.bottomRight], radius: 16)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        
        contentView.addSubview(bgImgView)
        bgImgView.snp.makeConstraints { make in
            make.left.equalTo(20)
            make.right.equalTo(-20).priority(.medium)
            make.top.bottom.equalToSuperview()
        }
    }
}
