//
//  ShowReceiveFinishView.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/16.
//

import UIKit

private let HeadImgViewHeight: CGFloat = 61
private let BackButtonHeight: CGFloat = 40

protocol ShowReceiveFinishViewDelegate: NSObjectProtocol {
    func onClickFinishButton()
}


class ShowReceiveFinishView: UIView {
    var headName: String? {
        didSet {
            headLabel.text = headName
        }
    }
    
    var title: String? {
        didSet {
            titleLabel.text = title ?? "show_alert_live_finish_title".show_localized
        }
    }

    var headImg: String? {
        didSet {
            if (headImg ?? "").hasPrefix("http") {
                headImgView.sd_setImage(with: URL(string: headImg ?? ""))
            } else {
                headImgView.image = UIImage(named: headImg ?? "")
            }
        }
    }
    
    weak var delegate: ShowReceiveFinishViewDelegate?
    
    // Background picture
    private lazy var bgView: UIView = {
        let view = UIView()
        view.backgroundColor = .show_end_bg
        return view
    }()
    
    // Anchor's avatar
    private lazy var headImgView: UIImageView =  {
        let imgeView =  UIImageView()
        imgeView.layer.cornerRadius = HeadImgViewHeight * 0.5
        imgeView.contentMode = .scaleAspectFill
        imgeView.layer.masksToBounds = true
        return imgeView
    }()
    
    // The name of the anchor
    private lazy var headLabel: UILabel = {
        let label = UILabel()
        label.textColor = .show_Ellipse2
        label.font = .show_R_16
        return label
    }()
    
    // Title
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.textColor = .show_slider_tint
        label.font = .show_S_18
        label.text = "show_alert_live_finish_title".show_localized
        return label
    }()
    
    // Subtitle
    lazy var subTitleLabel: UILabel = {
        let label = UILabel()
        label.textColor = UIColor(hex: "C6C4DD")
        label.font = .show_R_14
        label.text = "show_alert_live_finish_subtitle".show_localized
        return label
    }()
    
    // Return to the list of rooms
    private lazy var backButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.show_sceneImage(name: "show_live_close"), for: .normal)
        button.addTarget(self, action: #selector(didClickBackButton), for: .touchUpInside)
        return button
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        backgroundColor = .clear
        
        addSubview(bgView)
        bgView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        bgView.addSubview(titleLabel)
        titleLabel.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(8 + Screen.safeAreaTopHeight())
        }
        
        bgView.addSubview(subTitleLabel)
        subTitleLabel.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(self.titleLabel.snp.bottom).offset(18)
        }
        
        bgView.addSubview(headImgView)
        headImgView.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(self.titleLabel.snp.bottom).offset(158)
            make.width.height.equalTo(HeadImgViewHeight)
        }
        
        bgView.addSubview(headLabel)
        headLabel.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(self.headImgView.snp.bottom).offset(8)
        }
        
        bgView.addSubview(backButton)
        backButton.snp.makeConstraints { make in
            make.centerY.equalTo(self.titleLabel)
            make.right.equalTo(-15)
        }
    }
}

extension ShowReceiveFinishView {
    
    @objc private func didClickBackButton() {
        delegate?.onClickFinishButton()
    }
}
