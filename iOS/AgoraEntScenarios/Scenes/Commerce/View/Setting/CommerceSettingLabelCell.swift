//
//  ShowSettingLabelCell.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/14.
//

import UIKit

class CommerceSettingLabelCell: CommerceSettingBaseCell {
    private var cellDidSelectedAction: (()->())?
    
    private lazy var valueLabel: UILabel = {
        let label = UILabel()
        label.textColor = .commerce_Ellipse5
        label.font = .commerce_R_14
        return label
    }()
    
    private lazy var arrowImgView: UIImageView = {
        let imgView = UIImageView()
        imgView.image = UIImage.commerce_sceneImage(name: "show_arrow_right")
        return imgView
    }()

    override func createSubviews(){
        super.createSubviews()
        
        contentView.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(didSelectedCell)))
        
        contentView.addSubview(arrowImgView)
        arrowImgView.snp.makeConstraints { make in
            make.right.equalTo(-20)
            make.centerY.equalToSuperview()
        }
        
        contentView.addSubview(valueLabel)
        valueLabel.snp.makeConstraints { make in
            make.right.equalTo(-44)
            make.centerY.equalToSuperview()
        }
        detailButton.isHidden = AppContext.shared.isDebugMode
    }
    
    func setTitle(_ title: String, value: String, cellDidSelectedAction: (()->())?, detailButtonAction: (()->())?) {
        titleLabel.text = title
        valueLabel.text = value
        self.cellDidSelectedAction = cellDidSelectedAction
        self.clickDetailButonAction = detailButtonAction
    }

    @objc private func didSelectedCell() {
        cellDidSelectedAction?()
    }
}
