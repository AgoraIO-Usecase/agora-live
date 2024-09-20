//
//  DFStrengthTableViewCell.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/2.
//

import UIKit

class DFStrengthTableViewCell: DFStylizedCell {
    private let titleLabel = UILabel()
    private let slider = UISlider()
    let progressValueLabel = UILabel()
    var sliderHandler: ((Float) -> Void)?
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        titleLabel.font = UIFont.show_R_14
        titleLabel.textColor = UIColor.show_Ellipse6
                
        slider.minimumValue = 0
        slider.maximumValue = 1
        slider.value = 0.1 
        slider.minimumTrackTintColor = UIColor.show_zi03
        slider.maximumTrackTintColor = UIColor.show_segment_bg
        slider.thumbTintColor = UIColor.show_zi03
        
        progressValueLabel.font = UIFont.show_R_14
        progressValueLabel.textColor = UIColor.show_Ellipse6
        progressValueLabel.text = "0.1"
        
        contentView.addSubview(titleLabel)
        contentView.addSubview(slider)
        contentView.addSubview(progressValueLabel)

        titleLabel.snp.makeConstraints { make in
            make.left.equalToSuperview().offset(20)
            make.height.equalTo(48)
        }
        
        progressValueLabel.snp.makeConstraints { make in
            make.right.equalToSuperview().offset(-20)
            make.centerY.equalToSuperview()
        }
        
        slider.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.width.equalTo(100)
            make.right.equalTo(progressValueLabel.snp.left).offset(-20)
        }

        darkView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    
        setUserInteractionEnabled(enabled: true)
        
        slider.addTarget(self, action: #selector(sliderValueChanged(_:)), for: .valueChanged)
    }
    
    func setData(title: String, sliderValue: Float) {
        titleLabel.text = title
        slider.value = sliderValue
        progressValueLabel.text = "\(sliderValue)"
    }
    
    @objc private func sliderValueChanged(_ sender: UISlider) {
        if sender.value < 0.1 {
            sender.value = 0.1
        }
        
        let roundedValue = round(sender.value * 10) / 10.0
        progressValueLabel.text = "\(roundedValue)"
        
        if let handler = sliderHandler {
            handler(roundedValue)
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}
