//
//  ShowPresettingCell.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/16.
//

import UIKit

class CommercePresettingCell: UITableViewCell {
    
    public var aSelected: Bool = false {
        didSet {
            indicatorView.isHidden = !aSelected
            whiteBgView.layer.borderWidth = aSelected ? 1 : 0
        }
    }

    private lazy var bgImgView: UIImageView = {
        let imgView = UIImageView()
        imgView.backgroundColor = .commerce_preset_bg
        return imgView
    }()
    
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .commerce_zi02
        label.font = .commerce_R_14
        label.textColor = .commerce_Ellipse7
        return label
    }()
    
    private lazy var descLabel: UILabel = {
        let label = UILabel()
        label.textColor = .commerce_Ellipse5
        label.font = .commerce_R_12
        label.numberOfLines = 0
        return label
    }()
    
    private lazy var whiteBgView: UIView = {
        let view = UIView()
        view.backgroundColor = .white
        view.layer.cornerRadius = 10
        view.layer.borderColor = UIColor.commerce_zi02.cgColor
        view.layer.borderWidth = 0
        view.layer.masksToBounds = true
        return view
    }()
    
    private lazy var indicatorView: UIImageView = {
        let indicatorImgView = UIImageView()
        indicatorImgView.isHidden = true
        indicatorImgView.image = UIImage.commerce_sceneImage(name: "show_preset_select_indicator")
        return indicatorImgView
    }()
    
    private lazy var appleIconImgView: UIImageView = {
        let imgView = UIImageView()
        imgView.image = UIImage.commerce_sceneImage(name: "show_preset_apple_icon")
        return imgView
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        selectionStyle = .none
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func createSubviews(){
        
        contentView.addSubview(bgImgView)
        bgImgView.snp.makeConstraints { make in
            make.left.equalTo(20)
            make.right.equalTo(-20)
            make.top.bottom.equalToSuperview()
        }
        
        contentView.addSubview(whiteBgView)
        whiteBgView.snp.makeConstraints { make in
            make.edges.equalTo(UIEdgeInsets(top: 5, left: 40, bottom: 5, right: 40))
        }

        contentView.addSubview(titleLabel)
        titleLabel.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.left.equalTo(55)
            make.width.equalTo(85)
        }
        
        contentView.addSubview(appleIconImgView)
        appleIconImgView.snp.makeConstraints { make in
            make.leading.equalTo(titleLabel.snp.trailing).offset(5)
            make.centerY.equalToSuperview()
        }
        
        contentView.addSubview(descLabel)
        descLabel.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.left.equalTo(appleIconImgView.snp.right).offset(8)
            make.right.equalTo(whiteBgView).offset(-8)
        }
        
        contentView.addSubview(indicatorView)
        indicatorView.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.right.equalTo(-54)
        }
    }
    
    func setTitle(_ title: String, desc: String) {
        titleLabel.text = title
        descLabel.text = desc
    }
}
