//
//  DFArrowTableViewCell.swift
//  DreamFlow
//
//  Created by qinhui on 2024/10/12.
//

import Foundation

class DFArrowTableViewCell: UITableViewCell {
    var titleLabel = UILabel()
    var arrowImageView = UIImageView()
    var contentButton = UIButton()
    let lineView = UIView()

    var serverHandler: (() -> Void)?
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        configSubviews()
    }
    
    private func configSubviews() {
        arrowImageView.image = UIImage.show_sceneImage(name: "dream_flow_arrow_right")
        titleLabel.font = UIFont.show_R_14
        titleLabel.textColor = UIColor.show_Ellipse6
        titleLabel.textAlignment = .left
        
        contentButton.addTarget(self, action: #selector(selectServer), for: .touchUpInside)
        contentButton.titleLabel?.font = UIFont.show_R_14
        contentButton.setTitleColor(UIColor.show_Ellipse6, for: .normal)
        
        lineView.backgroundColor = UIColor.dream_flow_line

        contentView.addSubview(titleLabel)
        contentView.addSubview(contentButton)
        contentView.addSubview(arrowImageView)
        contentView.addSubview(lineView)
        
        titleLabel.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(20)
            make.centerY.equalToSuperview()
        }
        
        arrowImageView.snp.makeConstraints { make in
            make.trailing.equalToSuperview().offset(-20)
            make.height.width.equalTo(16)
            make.centerY.equalToSuperview()
        }
        
        contentButton.snp.makeConstraints { make in
            make.right.equalTo(arrowImageView.snp.left).offset(-6)
            make.height.equalTo(48)
        }
        
        lineView.snp.makeConstraints { make in
            make.left.equalTo(20)
            make.right.equalTo(-20)
            make.bottom.equalTo(0)
            make.height.equalTo(1)
        }
    }
    
    @objc private func selectServer() {
        guard let serverHandler = serverHandler else { return }
        
        serverHandler()
    }
    
    func setData(title: String, content: String) {
        titleLabel.text = title
        contentButton.setTitle(content, for: .normal)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
