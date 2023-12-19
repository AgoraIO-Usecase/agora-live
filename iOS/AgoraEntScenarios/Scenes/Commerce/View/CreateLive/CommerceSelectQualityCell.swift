//
//  ShowSelectQualityCell.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/7.
//

import UIKit

class CommerceSelectQualityCell: UICollectionViewCell {

    private lazy var indicatorView: UIView = {
        let view = UIView()
        view.layer.cornerRadius = 6
        view.layer.borderColor = UIColor.commerce_main_text.cgColor
        view.layer.borderWidth = 1
        view.isHidden = true
        return view
    }()
    
    private lazy var valueLabel: UILabel = {
        let label = UILabel()
        label.font = .commerce_R_12
        label.textColor = .commerce_main_text
        label.numberOfLines = 2
        return label
    }()
    
    private lazy var nameLabel: UILabel = {
        let label = UILabel()
        label.font = .commerce_R_12
        label.textColor = .commerce_main_text
        label.numberOfLines = 2
        return label
    }()
    
    override var isSelected: Bool {
        didSet{
            indicatorView.isHidden = !isSelected
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        contentView.addSubview(indicatorView)
        indicatorView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        

        contentView.addSubview(valueLabel)
        valueLabel.snp.makeConstraints { make in
            make.centerX.equalTo(indicatorView)
            make.top.equalTo(indicatorView).offset(6)
        }
        
        contentView.addSubview(nameLabel)
        nameLabel.snp.makeConstraints { make in
            make.centerX.equalTo(indicatorView)
            make.bottom.equalTo(indicatorView).offset(-6)
        }
    }
    
    func setValueStr(_ valueStr: String, name: String) {
        nameLabel.text = name
        valueLabel.text = valueStr
    }

}
