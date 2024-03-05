//
//  SoundCardEffectView.swift
//  AgoraEntScenarios
//
//  Created by CP on 2023/10/12.
//

import Foundation

class VRSoundCardEffectView: UIView {
    var headIconView: UIView!
    var headTitleLabel: UILabel!
    var tableView: UITableView!
    var botView: UIView!
    var cancleView: UIButton!
    var sepView: UIView!
    let voiceArray: [String] = ["voice_aure_tertius".voice_localized,
                                "voice_low_royal_note".voice_localized,
                                "voice_adolescent_voice".voice_localized,
                                "voice_radish_note".voice_localized,
                                "voice_uncle_sound".voice_localized,
                                "voice_moma".voice_localized,
                                "voice_royal_mother_tone".voice_localized]
    let descArray: [String] = ["voice_musical_magnetic".voice_localized,
                               "voice_soft_magnetic".voice_localized,
                               "voice_liud_full".voice_localized,
                               "voice_clamps_girl".voice_localized,
                               "voice_high_reverb_ktv".voice_localized,
                               "voice_bright_magnetic".voice_localized,
                               "voice_low_warm".voice_localized,
                               "voice_mellow_full".voice_localized]
    let imgArray: [String] = ["chat-1", "chat-2", "chat-3", "chat-4", "chat-5", "chat-6", "chat-7", "chat-8", "chat-9", "chat-10"]
    @objc var effectType: Int = 0
    @objc var clickBlock: ((Int)->Void)?
    override init(frame: CGRect) {
        super.init(frame: frame)
        layoutUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func layoutUI() {
        self.backgroundColor = .white
        
        headIconView = UIView()
        headIconView.backgroundColor = UIColor(red: 212/255.0, green: 207/255.0, blue: 229/255.0, alpha: 1)
        headIconView.layer.cornerRadius = 2
        headIconView.layer.masksToBounds = true
        self.addSubview(headIconView)
        
        headTitleLabel = UILabel()
        headTitleLabel.text = "voice_preset_sound".voice_localized
        headTitleLabel.textAlignment = .center
        headTitleLabel.font = UIFont.systemFont(ofSize: 16, weight: .bold)
        self.addSubview(headTitleLabel)
        
        tableView = UITableView()
        tableView.dataSource = self
        tableView.delegate = self
        tableView.registerCell(VRSoundCardEffectCell.self, forCellReuseIdentifier: "cell")
        self.addSubview(tableView)
        
//        sepView = UIView()
//        sepView.backgroundColor = UIColor(red: 245/255.0, green: 244/255.0, blue: 246/255.0, alpha: 1)
//        self.addSubview(sepView)
        
//        cancleView = UIButton()
//        cancleView.titleLabel?.font = UIFont.systemFont(ofSize: 15)
//        cancleView.setTitleColor(.black, for: .normal)
//        cancleView.setTitle("取消", for: .normal)
//        cancleView.backgroundColor = .white
//        cancleView.addTarget(self, action: #selector(cancel), for: .touchUpInside)
//        self.addSubview(cancleView)
        
//        KTVHeadSetUtil.addSoundCardObserver {[weak self] flag in
//            guard let block = self?.clickBlock else {return}
//            block(-2)
//        }
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        headIconView.frame = CGRect(x: (self.bounds.width - 38)/2.0, y: 8, width: 38, height: 4)
        headTitleLabel.frame = CGRect(x: (self.bounds.width - 200)/2.0, y: 30, width: 200, height: 22)
        tableView.frame = CGRect(x: 0, y: headTitleLabel.frame.maxY + 10, width: self.bounds.width, height: 350)
       // sepView.frame = CGRect(x: 0, y: self.bounds.height - 70, width: self.bounds.width, height: 10)
       // cancleView.frame = CGRect(x: 10, y: 26, width: 50, height: 30)
    }
    
    @objc private func cancel() {
        guard let block = clickBlock else {return}
        block(-1)
    }
}

extension VRSoundCardEffectView: UITableViewDelegate, UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        voiceArray.count
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        70
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! VRSoundCardEffectCell
        cell.selectionStyle = .none
        cell.titleLabel.text = voiceArray[indexPath.row]
        cell.detailLabel.text = descArray[indexPath.row]
        cell.imgView.image = UIImage.sceneImage(name: imgArray[indexPath.row])
        cell.setIsSelected(indexPath.row == self.effectType)
        return cell
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        if self.effectType == indexPath.row {
            return
        }
        self.effectType = indexPath.row
        tableView.reloadData()
        guard let block = clickBlock else {return}
        block(indexPath.row)
    }
}

