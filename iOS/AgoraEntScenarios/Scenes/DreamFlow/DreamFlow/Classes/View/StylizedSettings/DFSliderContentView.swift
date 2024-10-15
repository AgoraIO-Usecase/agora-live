//
//  DFSliderContentView.swift
//  DreamFlow
//
//  Created by qinhui on 2024/10/14.
//

import Foundation

class DFSliderContentView: UIView {
    let titleLabel = UILabel()
    let slider = UISlider()
    let sliderValueLabel = UILabel()
    let sliderMaxValueLable = UILabel()
    let lineView = UIView()

    var sliderHandler: ((Float) -> Void)?
    private var spacing:Float = 10.0

    override init(frame: CGRect) {
        super.init(frame: frame)
        configSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func configSubviews() {
        titleLabel.font = UIFont.show_R_14
        titleLabel.textColor = UIColor.show_Ellipse6
                
        slider.minimumValue = 0
        slider.maximumValue = 1
        slider.value = 0.1
        slider.minimumTrackTintColor = UIColor.show_zi03
        slider.maximumTrackTintColor = UIColor.show_segment_bg
        slider.thumbTintColor = UIColor.show_zi03
        
        sliderValueLabel.font = UIFont.show_R_14
        sliderValueLabel.textColor = UIColor.show_Ellipse6
        sliderValueLabel.text = "0.1"
        
        sliderMaxValueLable.font = UIFont.show_R_14
        sliderMaxValueLable.textColor = UIColor.show_Ellipse6
        sliderMaxValueLable.text = "0.1"
        
        lineView.backgroundColor = UIColor.dream_flow_line

        self.addSubview(titleLabel)
        self.addSubview(slider)
        self.addSubview(sliderValueLabel)
        self.addSubview(sliderMaxValueLable)
        self.addSubview(lineView)
        
        titleLabel.snp.makeConstraints { make in
            make.height.equalTo(48)
            make.left.top.bottom.equalTo(0)
        }
        
        sliderValueLabel.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.right.equalTo(slider.snp.left).offset(-26)
        }
        
        slider.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.width.equalTo(150)
            make.right.equalTo(sliderMaxValueLable.snp.left).offset(-15)
        }
        
        sliderMaxValueLable.snp.makeConstraints { make in
            make.right.equalTo(0)
            make.centerY.equalToSuperview()
        }
        
        lineView.snp.makeConstraints { make in
            make.left.right.bottom.equalTo(0)
            make.height.equalTo(1)
        }
        
        slider.addTarget(self, action: #selector(sliderValueChanged(_:)), for: .valueChanged)
    }
    
    func setData(title: String, sliderValue: Float, min: Float, max: Float, spacing: Float = 10.0) {
        titleLabel.text = title
        slider.minimumValue = min
        slider.maximumValue = max
        slider.value = sliderValue
        sliderValueLabel.text = "\(sliderValue)"
        sliderMaxValueLable.text = "\(max)"
        self.spacing = spacing
    }
    
    @objc private func sliderValueChanged(_ sender: UISlider) {
        if sender.value < 0.1 {
            sender.value = 0.1
        }
        
        let roundedValue = round(sender.value * spacing) / spacing
        sliderValueLabel.text = "\(roundedValue)"
        sender.value = roundedValue
        
        if let handler = sliderHandler {
            handler(roundedValue)
        }
    }
}
