//
//  SoundCardSettingView.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/10/12.
//

import Foundation

@objc class VRSoundCardSettingView: UIView {
    var headIconView: UIView!
    var headTitleLabel: UILabel!
    var noSoundCardView: UIView!
    var warNingLabel: UILabel!
    var tipsView: UIView!
    var tableView: UITableView!
    var headLabel: UILabel!
    var exLabel: UILabel!
    var coverView: UIView!
    @objc var soundOpen:Bool = false
    @objc var gainValue: Float = 0
    @objc var effectType: Int = 0 
    @objc var typeValue: Int = 2
    
    @objc var clicKBlock:((Int) -> Void)?
    @objc var gainBlock:((Float) -> Void)?
    @objc var typeBlock:((Int) -> Void)?
    @objc var soundCardBlock:((Bool) -> Void)?
    @objc func setUseSoundCard(enable: Bool) {
        self.noSoundCardView.isHidden = enable
        self.tableView.isHidden = !enable
        self.tableView.reloadData()
    }
    
    @objc override init(frame: CGRect) {
        super.init(frame: frame)
        
        self.backgroundColor = .white
        
        headIconView = UIView()
        headIconView.backgroundColor = UIColor(red: 212/255.0, green: 207/255.0, blue: 229/255.0, alpha: 1)
        headIconView.layer.cornerRadius = 2
        headIconView.layer.masksToBounds = true
        self.addSubview(headIconView)
        
        headTitleLabel = UILabel()
        headTitleLabel.text = "voice_SoundCard".voice_localized
        headTitleLabel.textAlignment = .center
        headTitleLabel.font = UIFont.systemFont(ofSize: 16, weight: .bold)
        self.addSubview(headTitleLabel)
        
        tableView = UITableView()
        tableView.dataSource = self
        tableView.delegate = self
        tableView.registerCell(UITableViewCell.self, forCellReuseIdentifier: "cell")
        tableView.registerCell(UITableViewCell.self, forCellReuseIdentifier: "switch")
        tableView.registerCell(UITableViewCell.self, forCellReuseIdentifier: "effect")
        tableView.registerCell(VRSoundCardMicCell.self, forCellReuseIdentifier: "mic")
        tableView.registerCell(VRSoundCardSwitchCell.self, forCellReuseIdentifier: "gain")
        self.addSubview(tableView)
        
        coverView = UIView()
        coverView.backgroundColor = .white
        coverView.alpha = 0.7
        self.addSubview(coverView)
        
        noSoundCardView = UIView()
        self.addSubview(noSoundCardView)
        
        //Create image attachment
        let imageAttachment = NSTextAttachment()
        imageAttachment.image = UIImage.sceneImage(name: "candel")

        //Set the size and position of the image
        let imageSize = CGSize(width: 20, height: 20)
        imageAttachment.bounds = CGRect(origin: .zero, size: imageSize)

        //Create rich text with images
        let attributedString = NSMutableAttributedString()
        let imageAttString = NSAttributedString(attachment: imageAttachment)
        attributedString.append(imageAttString)

        //Add Text Section
        let text = " 当前无法使用虚拟声卡，请连接优先输入设备！"
        let textAttributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 12),
            .foregroundColor: UIColor.red,
            .baselineOffset: (imageSize.height - UIFont.systemFont(ofSize: 12).capHeight) / 2  //Adjust image position to achieve vertical center
        ]
        let textAttString = NSAttributedString(string: text, attributes: textAttributes)
        attributedString.append(textAttString)

        warNingLabel = UILabel()
        warNingLabel.attributedText = attributedString
        warNingLabel.textColor = .red
        warNingLabel.font = UIFont.systemFont(ofSize: 12)
        noSoundCardView.addSubview(warNingLabel)
        
        tipsView = UIView()
        tipsView.backgroundColor = UIColor(red: 1, green: 251/255.0, blue: 252/255.0, alpha: 1)
        noSoundCardView.addSubview(tipsView)
        tipsView.layer.cornerRadius = 5
        tipsView.layer.masksToBounds = true
        
        headLabel = UILabel()
        headLabel.text = "voice_following_devices_supported".voice_localized
        headLabel.font = UIFont.systemFont(ofSize: 13, weight: .bold)
        tipsView.addSubview(headLabel)
        
        exLabel = UILabel()
        exLabel.text = "voice_wired_headphones_microphone".voice_localized
        exLabel.numberOfLines = 0
        exLabel.font = UIFont.systemFont(ofSize: 12)
        exLabel.textColor = UIColor(red: 60/255.0, green: 66/255.0, blue: 103/255.0, alpha: 1)
        tableView.tableFooterView = UIView()
        tipsView.addSubview(exLabel)
        
        noSoundCardView.isHidden = true
        
//        let flag = KTVHeadSetUtil.hasSoundCard()
//        self.noSoundCardView.isHidden = flag
//        self.tableView.isHidden = !flag
//
//        KTVHeadSetUtil.addSoundCardObserver {[weak self] flag in
//            self?.noSoundCardView.isHidden = flag
//            self?.tableView.isHidden = !flag
//            if flag == false {
//                self?.soundOpen = false
//                guard let soundCardBlock = self?.soundCardBlock else {return}
//                soundCardBlock(false)
//            }
//        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        headIconView.frame = CGRect(x: (self.bounds.width - 38)/2.0, y: 8, width: 38, height: 4)
        headTitleLabel.frame = CGRect(x: (self.bounds.width - 200)/2.0, y: 30, width: 200, height: 22)
        tableView.frame = CGRect(x: 0, y: headTitleLabel.frame.maxY + 10, width: self.bounds.width, height: self.bounds.height - headTitleLabel.frame.maxY - 10)
        
        coverView.frame = CGRect(x: 0, y: headTitleLabel.frame.maxY + 10 + 104, width: self.bounds.width, height: 156)
        
        noSoundCardView.frame = CGRect(x: 0, y: headTitleLabel.frame.maxY + 10, width: self.bounds.width, height: 200)
        warNingLabel.frame = CGRect(x: 20, y: 10, width: self.bounds.width, height: 20)
        tipsView.frame = CGRect(x: 20, y: 40, width: self.bounds.width - 40, height: 100)
        
        headLabel.frame = CGRect(x: 10, y: 10, width: 200, height: 20)
        exLabel.frame = CGRect(x: 20, y: headLabel.frame.maxY + 3, width: 80, height: 40)
        
    }
    
    private func getEffectDesc(with type: Int) -> String {
        switch type {
            case 0:
                return "voice_qing_shuyin".voice_localized
            case 1:
                return "voice_shaoyuyin".voice_localized
            case 2:
                return "voice_youth_voice".voice_localized
            case 3:
                return "voice_shaoluoyin".voice_localized
            case 4:
                return "voice_uncle_yin".voice_localized
            case 5:
                return "voice_ma_yin".voice_localized
            case 6:
                return "voice_qing_shuyin(Bright | Magnetic)".voice_localized
            case 7:
                return "voice_Yuma_Yin(bright | magnetic)".voice_localized
            case 8:
                return "voice_Youth voice (deep | warm)".voice_localized
            case 9:
                return "voice_Shao Yu Yin (mellow | full)".voice_localized
            default:
                break
        }
        return ""
    }
    
    @objc func soundChange(swich: UISwitch) {
        if swich.isOn {
            self.gainValue = 1.0;
            self.effectType = 0;
            self.typeValue = 4;
        }
        self.soundOpen = swich.isOn
        coverView.isHidden = swich.isOn
        tableView.reloadData()
        guard let soundCardBlock = soundCardBlock else {return}
        soundCardBlock(swich.isOn)
    }
    
}

extension VRSoundCardSettingView: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 4
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 52
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
       if indexPath.row == 0 {
          let cell: UITableViewCell = tableView.dequeueReusableCell(withIdentifier: "switch", for: indexPath)
            
           //Check if switch controls already exist, if not, create and add them
          var switchControl: UISwitch? = cell.contentView.viewWithTag(100) as? UISwitch
          if switchControl == nil {
              switchControl = UISwitch()
              switchControl?.translatesAutoresizingMaskIntoConstraints = false
              switchControl?.tag = 100
              switchControl?.addTarget(self, action: #selector(soundChange), for: .valueChanged)
              cell.contentView.addSubview(switchControl!)
              
              switchControl?.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor, constant: -20).isActive = true
              switchControl?.centerYAnchor.constraint(equalTo: cell.contentView.centerYAnchor).isActive = true
          }
          
          cell.textLabel?.text = "voice_start_virtual_sound_card".voice_localized
          cell.textLabel?.font = UIFont.systemFont(ofSize: 13)
          switchControl?.isOn = self.soundOpen

          cell.selectionStyle = .none
            return cell
        } else if indexPath.row == 1 {
            let cell: UITableViewCell = tableView.dequeueReusableCell(withIdentifier: "effect", for: indexPath)
            
            var rightLabel:UILabel? = nil
            if let label = self.viewWithTag(indexPath.row + 200) as? UILabel {
                rightLabel = label
            } else {
                let label = UILabel()
                label.numberOfLines = 0
                label.textAlignment = .right
                label.tag = indexPath.row + 200
                label.font = UIFont.systemFont(ofSize: 12)
                label.textColor = .gray
                label.translatesAutoresizingMaskIntoConstraints = false
                cell.contentView.addSubview(label)
                rightLabel = label
            }
            
            let font = UIFont.systemFont(ofSize: 13)
            let text = "voice_preset_sound".voice_localized
            
            cell.textLabel?.font = font
            cell.accessoryType = .disclosureIndicator
            cell.textLabel?.text = text
            rightLabel?.text = getEffectDesc(with: self.effectType)
            let size = text.size(font: font, drawRange: cell.size)
            let horizontalEdge: CGFloat = 30
            rightLabel?.snp.updateConstraints({ make in
                make.left.equalTo(horizontalEdge + size.width)
                make.height.equalTo(cell.contentView.snp.height)
                make.right.equalTo(cell.contentView).offset(-horizontalEdge)
                make.top.equalTo(cell.contentView)
            })
//            rightLabel?.frame = CGRect(x: horizontalEdge + size.width, y: 0, width: cell.contentView.width - size.width - horizontalEdge * 2, height: cell.contentView.height)
            cell.selectionStyle = .none
            return cell
        }else if indexPath.row == 2 {
            let cell: VRSoundCardSwitchCell = tableView.dequeueReusableCell(withIdentifier: "gain", for: indexPath) as! VRSoundCardSwitchCell
            cell.selectionStyle = .none
            cell.slider.value = Float(1/4.0 * gainValue)
            cell.numLable.text = String(format: "%.1f",gainValue)
            cell.valueBlock = {[weak self] gain in
                guard let self = self, let gainBlock = self.gainBlock else {return}
                self.gainValue = Float(gain)
                gainBlock(self.gainValue)
            }
            return cell
        } else {
            let cell: VRSoundCardMicCell = tableView.dequeueReusableCell(withIdentifier: "mic", for: indexPath) as! VRSoundCardMicCell
            cell.accessoryType = .none
            cell.selectionStyle = .none
            let text = self.typeValue > 0 ? "\(self.typeValue)" : "voice_off".voice_localized
            cell.numLable.text = "\(text)"
            cell.slider.value = Float(1.0/14 * Float(typeValue))
            cell.valueBlock = {[weak self] type in
                guard let self = self, let typeBlock = self.typeBlock else {return}
                self.typeValue = type
                typeBlock(self.typeValue)
            }
            coverView.isHidden = self.soundOpen
            return cell
        }
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        guard let block = clicKBlock, soundOpen else {return}
        if indexPath.row == 1 {
            //Pop up sound effect selection
            block(2)
        } else if indexPath.row == 3 {
////Pop up microphone type
//            block(4)
        }
    }
}
