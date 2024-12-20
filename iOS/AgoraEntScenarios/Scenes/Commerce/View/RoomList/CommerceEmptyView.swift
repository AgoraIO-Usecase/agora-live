//
//  ShowEmptyView.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/2.
//

import UIKit

class CommerceEmptyView: UIView {
    
    var imageView: UIImageView!
    var descLabel: UILabel!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        imageView = UIImageView()
        imageView.image = UIImage.commerce_sceneImage(name: "show_list_empty")
        addSubview(imageView)
        imageView.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalToSuperview().offset(-20)
        }
        
        descLabel = UILabel()
        descLabel.textColor = .commerce_empty_desc
        descLabel.font = .commerce_R_14
        descLabel.text = "commerce_room_list_empty".commerce_localized
        addSubview(descLabel)
        descLabel.snp.makeConstraints { make in
            make.top.equalTo(imageView.snp.bottom).offset(40)
            make.centerX.equalTo(imageView)
        }
    }
}
