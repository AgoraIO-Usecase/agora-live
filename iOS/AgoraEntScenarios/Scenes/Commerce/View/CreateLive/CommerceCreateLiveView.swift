//
//  ShowCreateLiveView.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/2.
//

import UIKit

protocol CommerceCreateLiveViewDelegate: NSObjectProtocol {
    func onClickCameraBtnAction()
    func onClickBeautyBtnAction()
    func onClickSettingBtnAction()
    func onClickStartBtnAction()
}

class CommerceCreateLiveView: UIView {

    weak var delegate: CommerceCreateLiveViewDelegate?
    var hideBottomViews = false {
        didSet {
            self.coverView.isHidden = hideBottomViews
        }
    }
    
    let roomNo: String = "\(arc4random_uniform(899999) + 100000)"
    let roomBg: String = "\(Int.random(in: 1...3))"
    var roomName: String? {
        get{
            return nameTextField.text?.trimmingCharacters(in: .whitespacesAndNewlines)
        }
    }

    private var roomBgImgView: UIImageView!
    private var nameTextField: UITextField!
    private var roomIdLabel: UILabel!
    private var coverView: UIImageView!
    
    override init(frame: CGRect) {
        super.init(frame: frame)
//        backgroundColor = .white
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        let roomInfoCoverVeiw = UIView()
        roomInfoCoverVeiw.backgroundColor = .commerce_cover
        roomInfoCoverVeiw.layer.cornerRadius = 10
        roomInfoCoverVeiw.layer.masksToBounds = true
        addSubview(roomInfoCoverVeiw)
        roomInfoCoverVeiw.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(103)
            make.width.equalTo(315)
            make.height.equalTo(84)
        }
        
        roomBgImgView = UIImageView()
        roomBgImgView.contentMode = .scaleAspectFill
        roomBgImgView.clipsToBounds = true
        roomBgImgView.layer.cornerRadius = 10
        roomBgImgView.image = UIImage.commerce_sceneImage(name: "show_room_bg_\(roomBg)")
        roomInfoCoverVeiw.addSubview(roomBgImgView)
        roomBgImgView.snp.makeConstraints { make in
            make.left.equalTo(10)
            make.centerY.equalToSuperview()
            make.width.height.equalTo(60)
        }
        
        nameTextField = UITextField()
        roomInfoCoverVeiw.addSubview(nameTextField)
        nameTextField.placeholder = "create_name_text_field_placeholder".commerce_localized
        nameTextField.font = .commerce_M_15
        nameTextField.textColor = .commerce_main_text
        nameTextField.snp.makeConstraints { make in
            make.left.equalTo(roomBgImgView.snp.right).offset(10)
            make.top.equalTo(18)
            make.right.equalTo(-50)
        }
//        nameTextField.becomeFirstResponder()
        
        let editButton = UIButton(type: .custom)
        editButton.setImage(UIImage.commerce_sceneImage(name: "show_create_edit"), for: .normal)
        addSubview(editButton)
        editButton.snp.makeConstraints { make in
            make.left.equalTo(nameTextField.snp.right).offset(3)
            make.centerY.equalTo(nameTextField)
        }
        roomIdLabel = UILabel()
        roomIdLabel.text = "ID: " + roomNo
        roomIdLabel.font = .commerce_R_10
        roomIdLabel.textColor = .commerce_main_text
        roomInfoCoverVeiw.addSubview(roomIdLabel)
        roomIdLabel.snp.makeConstraints { make in
            make.left.equalTo(nameTextField)
            make.bottom.equalTo(-19)
        }
        
        let copyButton = UIButton(type: .custom)
        copyButton.isHidden = true
        roomInfoCoverVeiw.addSubview(copyButton)
        copyButton.setImage(UIImage.commerce_sceneImage(name: "show_create_copy"), for: .normal)
        copyButton.addTarget(self, action: #selector(didClickCopyButton), for: .touchUpInside)
        copyButton.snp.makeConstraints { make in
            make.left.equalTo(roomIdLabel.snp.right).offset(10)
            make.centerY.equalTo(roomIdLabel)
        }
        
        coverView = UIImageView()
        coverView.isUserInteractionEnabled = true
        coverView.image = UIImage.commerce_sceneImage(name: "show_list_cover")
        addSubview(coverView)
        coverView.snp.makeConstraints { make in
            make.left.bottom.right.equalToSuperview()
            make.height.equalTo(254)
        }
        
        // tips
        let tipsLabel = UILabel()
        let tipsText = "create_tips".commerce_localized
        var attachment = NSTextAttachment()
        if #available(iOS 13.0, *) {
            attachment = NSTextAttachment(image: UIImage.commerce_sceneImage(name: "show_create_tips")!)
        } else {
            attachment.image = UIImage.commerce_sceneImage(name: "show_create_tips")
        }
        attachment.bounds = CGRect(x: 0, y: -2, width: 11, height: 11)
        let attriTipsImg = NSAttributedString(attachment: attachment)
        let attriTips = NSMutableAttributedString(attributedString: attriTipsImg)
        attriTips.append(NSAttributedString(string: tipsText))
        tipsLabel.attributedText = attriTips
        tipsLabel.font = .commerce_R_11
        tipsLabel.textColor = .commerce_main_text
        tipsLabel.numberOfLines = 0
        coverView.addSubview(tipsLabel)
        tipsLabel.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.bottom.equalTo(-134)
            make.leading.trailing.equalToSuperview().inset(15)
        }
        
        let btnHeight: CGFloat = 48
        let startButton = UIButton(type: .custom)
        startButton.setTitle("create_start_live".commerce_localized, for: .normal)
        startButton.backgroundColor = .commerce_btn_bg
        startButton.titleLabel?.font = .commerce_btn_title
        startButton.layer.cornerRadius = btnHeight * 0.5
        startButton.layer.masksToBounds = true
        coverView.addSubview(startButton)
        startButton.addTarget(self, action: #selector(didClickStartButton), for: .touchUpInside)
        startButton.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.bottom.equalTo(-60)
            make.width.equalTo(230)
            make.height.equalTo(btnHeight)
        }
        
        layoutButtonArray()
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        nameTextField.resignFirstResponder()
    }
    
    private func createButton(imgName: String, title: String) ->UIButton {
        let button = UIButton(type: .custom)
        let imageView = UIImageView(image: UIImage.commerce_sceneImage(name: imgName))
        imageView.contentMode = .center
        button.addSubview(imageView)
        imageView.snp.makeConstraints { make in
            make.left.top.right.equalToSuperview()
        }
        let label = UILabel()
        label.font = .commerce_M_12
        label.textColor = .commerce_main_text
        label.text = title
        label.textAlignment = .center
        button.addSubview(label)
        label.snp.makeConstraints { make in
            make.left.bottom.right.equalToSuperview()
            make.top.equalTo(imageView.snp.bottom).offset(3)
        }
        return button
    }
    
    private func layoutButtonArray(){
        let cameraButton = createButton(imgName: "show_create_camera", title: "create_button_switch".commerce_localized)
        cameraButton.addTarget(self, action: #selector(didClickCameraButton), for: .touchUpInside)
        
        let beautyButton = createButton(imgName: "show_create_beauty", title: "create_button_beauty".commerce_localized)
        beautyButton.addTarget(self, action: #selector(didClickBeautyButton), for: .touchUpInside)
        
        let settingButton = createButton(imgName: "show_setting", title: "create_button_settings".commerce_localized)
        settingButton.addTarget(self, action: #selector(didClickSettingButton), for: .touchUpInside)
        
        let buttonArray = [cameraButton, beautyButton, settingButton]
        
        let stackView = UIStackView(arrangedSubviews: buttonArray)
        stackView.spacing = 25
        stackView.alignment = .fill
        stackView.axis = .horizontal
        stackView.distribution = .fill
        coverView.addSubview(stackView)
        stackView.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.bottom.equalToSuperview().offset(-174)
        }
    }
}

extension CommerceCreateLiveView {
    @objc private func didClickCopyButton(){
        UIPasteboard.general.string = roomIdLabel.text
        ToastView.show(text: "create_toast_copy_to_paste_borad".commerce_localized)
    }
    
    @objc private func didClickCameraButton(){
        delegate?.onClickCameraBtnAction()
    }
    
    @objc private func didClickBeautyButton(){
        delegate?.onClickBeautyBtnAction()
    }
    @objc private func didClickSettingButton(){
        delegate?.onClickSettingBtnAction()
    }
    @objc private func didClickStartButton(){
        delegate?.onClickStartBtnAction()
    }
}
