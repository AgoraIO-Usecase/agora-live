//
//  ShowRoomBottomBar.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/8.
//

import UIKit

private let dotWidth: CGFloat = 5

protocol CommerceRoomBottomBarDelegate: NSObjectProtocol {
    func onClickPKButton(_ button: CommerceRedDotButton)
    func onClickLinkButton(_ button: CommerceRedDotButton)
    func onClickBeautyButton()
    func onClickMusicButton()
    func onClickSettingButton()
}

class CommerceRedDotButton: UIButton {
    
    var isShowRedDot = false {
        didSet{
            redDotLayer.isHidden = !isShowRedDot
        }
    }
    
    private var redDotLayer: CALayer = {
        let layer = CALayer()
        layer.backgroundColor = UIColor.commerce_red_dot.cgColor
        layer.cornerRadius = dotWidth * 0.5
        layer.masksToBounds = true
        layer.isHidden = true
        return layer
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        layer.addSublayer(redDotLayer)
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        redDotLayer.frame = CGRect(x: bounds.width - 2 - dotWidth, y: 2, width: dotWidth, height: dotWidth)
    }
}

class CommerceRoomBottomBar: UIView {
    
    weak var delegate: CommerceRoomBottomBarDelegate?
    
    // pk
    private lazy var pkButton: CommerceRedDotButton = {
        let button = CommerceRedDotButton(type: .custom)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_PK"), for: .normal)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_pk_disable"), for: .disabled)
        button.addTarget(self, action: #selector(didClickPKButton), for: .touchUpInside)
        return button
    }()
    
    lazy var linkButton: CommerceRedDotButton = {
        let button = CommerceRedDotButton(type: .custom)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_link"), for: .selected)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_link_disable"), for: .normal)
        button.addTarget(self, action: #selector(didClickLinkButton), for: .touchUpInside)
        return button
    }()
    
    private lazy var beautyButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_beauty"), for: .normal)
        button.addTarget(self, action: #selector(didClickBeautyButton), for: .touchUpInside)
        return button
    }()
    
    private lazy var musicButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_music"), for: .normal)
        button.addTarget(self, action: #selector(didClickMusicButton), for: .touchUpInside)
        return button
    }()
    
    private lazy var settingButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_setting"), for: .normal)
        button.addTarget(self, action: #selector(didClickSettingButton), for: .touchUpInside)
        return button
    }()
    
    private var buttonArray = [UIButton]()
    private var isBroadcastor = false
    
    init(isBroadcastor: Bool = false) {
        super.init(frame: .zero)
        self.isBroadcastor = isBroadcastor
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        if isBroadcastor {
            buttonArray = [pkButton, linkButton, beautyButton, musicButton, settingButton]
        }else{
            buttonArray = [linkButton,settingButton]
        }
        
        var i = 0
        let btnWidth: CGFloat = 38
        let btnHeight: CGFloat = 38
        let spacing: CGFloat = 4
        for button in buttonArray {
            addSubview(button)
            button.snp.makeConstraints { make in
                make.left.equalTo((btnWidth + spacing) * CGFloat(i))
                make.top.bottom.equalToSuperview()
                make.height.equalTo(btnHeight)
                make.width.equalTo(btnWidth)
                if i == buttonArray.count - 1 {
                    make.right.equalToSuperview()
                }
            }
            i += 1
        }
    }
    
    @objc private func didClickPKButton() {
        delegate?.onClickPKButton(pkButton)
    }
    
    @objc private func didClickLinkButton() {
        delegate?.onClickLinkButton(linkButton)
    }
    
    @objc private func didClickBeautyButton() {
        delegate?.onClickBeautyButton()
    }
    
    @objc private func didClickMusicButton() {
        delegate?.onClickMusicButton()
    }
    
    @objc private func didClickSettingButton() {
        delegate?.onClickSettingButton()
    }
}
