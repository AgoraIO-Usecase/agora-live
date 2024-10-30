//
//  PresetsInfoTableViewCell.swift
//  DreamFlow
//
//  Created by qinhui on 2024/10/14.
//

import Foundation

class DFPresetsInfoTableViewCell: UITableViewCell {
    let darkCoverView = UIView()
    let faceModeView = DFSwitchContentView()
    let strengthView = DFSliderContentView()
    let frameView = DFSliderContentView()
    let styleContentView = DFScrollContentView()
    let customizedPromptTitleLabel = UILabel()
    let textView = DFPlaceholderTextView()
    
    var strengthSliderHandler: ((Float) -> Void)?
    var superFrameSliderHandler: ((Float) -> Void)?
    var styleSelectHandler: ((DFStylizedSettingItem) -> Void)?
    var switchHandler: ((Bool) -> Void)?
    var inputHandler: ((String) -> Void)?
    
    private var styles: [DFStylizedSettingItem]!
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        configSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func configSubviews() {
        darkCoverView.layer.cornerRadius = 16
        darkCoverView.layer.masksToBounds = true
        darkCoverView.layer.borderWidth = 1
        darkCoverView.layer.borderColor = UIColor.dream_flow_border.cgColor
        
        faceModeView.titleLabel.text = "df_face_mode_title".show_localized
        
        customizedPromptTitleLabel.font = UIFont.show_R_12
        customizedPromptTitleLabel.textColor = UIColor.show_Ellipse5
        
        textView.layer.cornerRadius = 20
        textView.backgroundColor = UIColor.show_segment_bg
        textView.isScrollEnabled = true
        textView.layer.borderWidth = 1
        textView.layer.borderColor = UIColor.show_segment_border.cgColor
        textView.textContainer.maximumNumberOfLines = 0
        textView.textContainerInset = UIEdgeInsets(top: 8, left: 8, bottom: 8, right: 8)
        textView.textColor = UIColor.show_Ellipse6
        textView.placeholder = ""
        textView.delegate = self
        
        self.contentView.addSubview(faceModeView)
        self.contentView.addSubview(strengthView)
        self.contentView.addSubview(frameView)
        self.contentView.addSubview(styleContentView)
        self.contentView.addSubview(customizedPromptTitleLabel)
        self.contentView.addSubview(textView)
        self.contentView.addSubview(darkCoverView)

        faceModeView.snp.makeConstraints { make in
            make.top.equalTo(0)
            make.leading.equalTo(26)
            make.trailing.equalTo(-26)
        }
        
        strengthView.snp.makeConstraints { make in
            make.left.equalTo(26)
            make.right.equalTo(-26)
            make.top.equalTo(faceModeView.snp.bottom)
        }
        
        frameView.snp.makeConstraints { make in
            make.left.equalTo(26)
            make.right.equalTo(-26)
            make.top.equalTo(strengthView.snp.bottom)
        }
        
        styleContentView.snp.makeConstraints { make in
            make.top.equalTo(frameView.snp.bottom)
            make.left.equalTo(26)
            make.right.equalTo(-26)
            make.height.equalTo(100)
        }
        
        customizedPromptTitleLabel.snp.makeConstraints { make in
            make.top.equalTo(styleContentView.snp.bottom).offset(12)
            make.left.equalTo(26)
        }
        
        textView.snp.makeConstraints { make in
            make.top.equalTo(customizedPromptTitleLabel.snp.bottom).offset(8)
            make.left.equalTo(26)
            make.right.equalTo(-26)
            make.height.equalTo(120)
            make.bottom.equalTo(-24)
        }
        
        darkCoverView.snp.makeConstraints { make in
            make.top.bottom.equalTo(0)
            make.left.equalTo(10)
            make.right.equalTo(-10)
        }
        
        strengthView.sliderHandler = { [weak self] value in
            guard let self = self, let sliderHandler = self.strengthSliderHandler else { return }
            sliderHandler(value)
        }
        
        frameView.sliderHandler = { [weak self] value in
            guard let self = self, let sliderHandler = self.superFrameSliderHandler else { return }
            sliderHandler(value)
        }
        
        styleContentView.selectHandler = { [weak self] index in
            guard let self = self, let handler = self.styleSelectHandler else { return }
            var item = self.styles[index]
            
            handler(item)
        }
        
        faceModeView.switchControl.addTarget(self, action: #selector(switchAction(_:)), for: .touchUpInside)
    }
    
    func setData(preset: DFPresetSettingItem, styles: [DFStylizedSettingItem]) {
        self.styles = styles
        faceModeView.switchControl.setOn(preset.faceMode, animated: true)
        strengthView.setData(title: "df_strength_title".show_localized, sliderValue: preset.strengthDefaultValue, min: preset.strengthMinValue, max: preset.strengthMaxValue)
        frameView.setData(title: "df_super_frame_title".show_localized, sliderValue: Float(preset.superFrameDefaultValue), min: preset.superFrameMinValue, max: preset.superFrameMaxvalue, spacing: 1)
        styleContentView.setData(items: styles, selectedItem: styles[preset.defaultStyleIndex])
        textView.text = preset.content
        
        setUserInteractionEnabled(enabled: preset.isCustomPreset)
    }
    
    @objc private func switchAction(_ sender: UISwitch) {
        let value = sender.isOn
        if let switchHandler = switchHandler {
            switchHandler(value)
        }        
    }
    
    private func setUserInteractionEnabled(enabled: Bool) {
        darkCoverView.backgroundColor = enabled ? .clear : UIColor.black.withAlphaComponent(0.03)
        darkCoverView.isUserInteractionEnabled = !enabled
        darkCoverView.isHidden = enabled
    }
}

extension DFPresetsInfoTableViewCell: UITextViewDelegate {
    func textViewDidChange(_ textView: UITextView) {
        if let handler = inputHandler {
            handler(textView.text)
        }
    }
}
