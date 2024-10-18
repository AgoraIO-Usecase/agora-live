//
//  SoundCardSettingView.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/10/12.
//

import Foundation

@objc class SoundCardSettingView: UIView {
    private lazy var backButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.ktv_sceneImage(name: "ktv_back_whiteIcon"), for: .normal)
        button.addTarget(self, action: #selector(backAction), for: .touchUpInside)
        return button
    }()
    var headIconView: UIView!
    var headTitleLabel: UILabel!
    var noSoundCardView: UIView!
    var warNingLabel: UILabel!
    var tipsView: UIView!
    var tableView: UITableView!
    var headLabel: UILabel!
    var exLabel: UILabel!
    var coverView: UIView!
    private var soundOpen:Bool = false
    private var gainValue: Float = 0
    private var effectType: Int = 0
    private var typeValue: Int = 2
    
    @objc var clickBackBlock: (()->())?
    @objc var clicKBlock:((Int) -> Void)?
    @objc var gainBlock:((Float) -> Void)?
    @objc var typeBlock:((Int) -> Void)?
    @objc var soundCardBlock:((Bool) -> Void)?
    
    @objc func setup(enable: Bool, typeValue: Int, gainValue: Float, effectType: Int) {
        self.soundOpen = enable
        self.gainValue = gainValue
        self.effectType = effectType
        self.typeValue = typeValue
        self.tableView.reloadData()
    }
    
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
        headTitleLabel.text = Bundle.localizedString("ktv_soundcard", bundleName: "KtvResource")
        headTitleLabel.textAlignment = .center
        headTitleLabel.font = UIFont.systemFont(ofSize: 16, weight: .bold)
        self.addSubview(headTitleLabel)
        
        self.addSubview(backButton)
        
        tableView = UITableView()
        tableView.dataSource = self
        tableView.delegate = self
        tableView.separatorColor = UIColor(hexString: "#F2F2F6")
        tableView.registerCell(UITableViewCell.self, forCellReuseIdentifier: "effect")
        tableView.registerCell(UITableViewCell.self, forCellReuseIdentifier: "cell")
        tableView.registerCell(SoundCardMicCell.self, forCellReuseIdentifier: "mic")
        tableView.registerCell(SoundCardSwitchCell.self, forCellReuseIdentifier: "switch")
        self.addSubview(tableView)
        
        coverView = UIView()
        coverView.backgroundColor = .white
        coverView.alpha = 0.7
        self.addSubview(coverView)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        backButton.frame = CGRect(x: 20, y: 24, width: 35, height: 35)
        headIconView.frame = CGRect(x: (self.bounds.width - 38)/2.0, y: 8, width: 38, height: 4)
        headTitleLabel.frame = CGRect(x: 0, y: 30, width: self.bounds.width, height: 22)
        tableView.frame = CGRect(x: 0, y: headTitleLabel.frame.maxY + 10, width: self.bounds.width, height: self.bounds.height - headTitleLabel.frame.maxY - 10)
        
        coverView.frame = CGRect(x: 0, y: headTitleLabel.frame.maxY + 10 + 52, width: self.bounds.width, height: 250)
//
//        noSoundCardView.frame = CGRect(x: 0, y: headTitleLabel.frame.maxY + 10, width: self.bounds.width, height: 200)
//        warNingLabel.frame = CGRect(x: 20, y: 10, width: self.bounds.width, height: 20)
//        tipsView.frame = CGRect(x: 20, y: 40, width: self.bounds.width - 40, height: 100)
        
      //  headLabel.frame = CGRect(x: 10, y: 10, width: 200, height: 20)
     //   exLabel.frame = CGRect(x: 20, y: headLabel.frame.maxY + 3, width: 80, height: 40)
        
    }
    
    private func getEffectDesc(with type: Int) -> String {
        switch type {
            case 0:
            return Bundle.localizedString("ktv_effect_desc1", bundleName: "KtvResource")
            case 1:
                return Bundle.localizedString("ktv_effect_desc2", bundleName: "KtvResource")
            case 2:
                return Bundle.localizedString("ktv_effect_desc3", bundleName: "KtvResource")
            case 3:
                return Bundle.localizedString("ktv_effect_desc4", bundleName: "KtvResource")
            case 4:
                return Bundle.localizedString("ktv_effect_desc5", bundleName: "KtvResource")
            case 5:
                return Bundle.localizedString("ktv_effect_desc6", bundleName: "KtvResource")
            default:
                break
        }
        return ""
    }
    
    @objc func soundChange(swich: UISwitch) {
        self.soundOpen = swich.isOn
        coverView.isHidden = swich.isOn
        guard let soundCardBlock = soundCardBlock else {return}
        soundCardBlock(swich.isOn)
    }
    
    @objc func backAction() {
        clickBackBlock?()
    }
}

extension SoundCardSettingView: UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 4
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 70
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        if indexPath.row == 1 {
            let cell: UITableViewCell = tableView.dequeueReusableCell(withIdentifier: "effect", for: indexPath)
            let rightLabel = UILabel()
            rightLabel.font = UIFont.systemFont(ofSize: 12)
            rightLabel.textColor = .gray
            rightLabel.translatesAutoresizingMaskIntoConstraints = false
            cell.contentView.addSubview(rightLabel)

            rightLabel.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor, constant: -20).isActive = true
            rightLabel.centerYAnchor.constraint(equalTo: cell.contentView.centerYAnchor).isActive = true
            
            cell.textLabel?.font = UIFont.systemFont(ofSize: 13)
            cell.accessoryType = .disclosureIndicator
            cell.textLabel?.text = Bundle.localizedString("ktv_pre_effect", bundleName: "KtvResource")
            rightLabel.text = getEffectDesc(with: self.effectType)
            cell.selectionStyle = .none
            return cell
        } else if indexPath.row == 0 {
            let cell: UITableViewCell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath)
            // Check whether the switch control already exists. If not, create and add
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
           
            cell.textLabel?.text = Bundle.localizedString("ktv_open_soundCard", bundleName: "KtvResource")
           cell.textLabel?.font = UIFont.systemFont(ofSize: 13)
           switchControl?.isOn = self.soundOpen

           cell.selectionStyle = .none
           return cell
        } else if indexPath.row == 2 {
            let cell: SoundCardSwitchCell = tableView.dequeueReusableCell(withIdentifier: "switch", for: indexPath) as! SoundCardSwitchCell
            cell.selectionStyle = .none
            cell.slider.value = Float(1/400.0 * gainValue)
            cell.numLable.text = String(format: "%.1f",gainValue)
            cell.valueBlock = {[weak self] gain in
                guard let self = self, let gainBlock = self.gainBlock else {return}
                self.gainValue = Float(gain)
                gainBlock(self.gainValue)
            }
            return cell
        } else {
            let cell: SoundCardMicCell = tableView.dequeueReusableCell(withIdentifier: "mic", for: indexPath) as! SoundCardMicCell
            cell.setupValue(self.typeValue)
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
        guard let block = clicKBlock else {return}
        if indexPath.row == 1 {
            //Pop-up sound effect selection
            block(2)
        } else if indexPath.row == 3 {
//            //Pop-up microphone type
//            block(4)
        }
    }
}
