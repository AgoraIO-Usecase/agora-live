//
//  ShowBeautyFaceCell.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/4.
//

import UIKit

class ShowBeautyFaceCell: UICollectionViewCell {
    var imageView: UIImageView!
    var nameLabel: UILabel!
    private var indicatorImgView: UIImageView!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupModel(model: BeautyModel) {
        nameLabel.text = model.name
        imageView.image = UIImage.show_beautyImage(name: model.icon)
        indicatorImgView.isHidden = !model.isSelected
        nameLabel.font = model.isSelected ? .show_M_12 : .show_R_11
        nameLabel.textColor = model.isSelected ? .show_main_text : .show_beauty_deselect
    }
    
    private func createSubviews(){
        imageView = UIImageView()
        imageView.image = UIImage.show_beautyImage(name: "show_beauty_none")
        imageView.contentMode = .scaleAspectFit
        contentView.addSubview(imageView)
        
        indicatorImgView = UIImageView()
        indicatorImgView.isHidden = true
        indicatorImgView.image = UIImage.show_beautyImage(name: "show_beauty_selected")
        contentView.addSubview(indicatorImgView)
        
        imageView.snp.makeConstraints { make in
            make.center.equalTo(indicatorImgView)
        }
        
        indicatorImgView.snp.makeConstraints { make in
            make.top.equalToSuperview()
            make.centerX.equalToSuperview()
            make.width.height.equalTo(52)
        }
        
        nameLabel = UILabel()
        nameLabel.font = .show_R_11
        nameLabel.textColor = .show_beauty_deselect
        nameLabel.numberOfLines = 2
        nameLabel.text = "show_beauty_item_beauty_whiten".show_localized
//        nameLabel.textAlignment = .center
        contentView.addSubview(nameLabel)
        nameLabel.snp.makeConstraints { make in
            make.centerX.equalTo(indicatorImgView)
//            make.leading.trailing.equalToSuperview().inset(2)
            make.bottom.equalToSuperview()
        }
    }

}
