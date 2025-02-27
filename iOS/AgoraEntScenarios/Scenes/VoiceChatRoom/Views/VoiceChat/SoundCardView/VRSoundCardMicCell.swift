//
//  SoundCardMicCell.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/10/12.
//

import UIKit

class VRSoundCardMicCell: UITableViewCell {
    var titleLabel: UILabel!
    var detailLabel: UILabel!
    var numLable: UILabel!
    var slider: UISlider!
    var valueBlock: ((Int)-> Void)?
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        layoutUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func layoutUI() {
        titleLabel = UILabel()
        titleLabel.text = "voice_microphone_type".voice_localized
        titleLabel.font = UIFont.systemFont(ofSize: 13)
        self.contentView.addSubview(titleLabel)
        
        detailLabel = UILabel()
        detailLabel.text = "voice_select_preset_parameters_micrphone".voice_localized
        detailLabel.font = UIFont.systemFont(ofSize: 12)
        detailLabel.textColor = .lightGray
        self.contentView.addSubview(detailLabel)
        
        numLable = UILabel()
        numLable.font = UIFont.systemFont(ofSize: 12)
        numLable.text = "voice_off".voice_localized
        numLable.textColor = .gray
        numLable.textAlignment = .center
        self.contentView.addSubview(numLable)
        
        slider = UISlider()
        slider.value = 0.5
        self.contentView.addSubview(slider)
        
        slider.addTarget(self, action: #selector(gain), for: .valueChanged)
        slider.addTarget(self, action: #selector(gainSend), for: .touchUpInside)
        slider.addTarget(self, action: #selector(gainSend), for: .touchUpOutside)
    }
    
    @objc func gain() {
        let gain = slider.value
        numLable.text = String(calculateLevel(for: gain))
    }

    @objc func gainSend() {
        let gain = slider.value
        let level = String(calculateLevel(for: gain))
//        let levNum = Double(level)
        print("send lev:\(calculateLevel(for: gain))")
        guard let valueBlock = valueBlock else {return}
        valueBlock(calculateLevel(for: gain))
    }
    
    func calculateLevel(for value: Float) -> Int {
        let stepSize: Float = 1/14

        if value <= 0 {
            return 0
        } else if value >= 1 {
            return 14
        } else {
            let level = Int(value / stepSize)
            return level
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let numWidth = 40.0
        let sliderWidth = 70.0
        titleLabel.frame = CGRect(x: 20, y: 5, width: width - 20 - numWidth - sliderWidth, height: 18)
        detailLabel.frame = CGRect(x: 20, y: 25, width: width - 20 - numWidth - sliderWidth, height: 18)
        numLable.frame = CGRect(x: self.bounds.size.width - numWidth, y: 16, width: numWidth, height: 20)
        slider.frame = CGRect(x: self.bounds.size.width - sliderWidth - numWidth, y: 11, width: sliderWidth, height: 30)
    }
}
