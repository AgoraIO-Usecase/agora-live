//
//  ShowRoomListCell.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/1.
//

import UIKit
import SnapKit

class ShowRoomListCell: UICollectionViewCell {
    
    var imageView: UIImageView!
    var nameLabel: UILabel!
    var idLablel: UILabel!
    var numberLabel: UILabel!
    private lazy var liveStatusLabel: UILabel = {
       let label = UILabel()
        label.text = "show_living".show_localized
        label.font = .systemFont(ofSize: 10)
        label.textColor = .white
        return label
    }()
    
    private let pureImageView = UIImageView()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setBgImge(_ img: String, name: String?, id: String?, count: Int, pureMode: Bool) {
        imageView.image = UIImage.show_sceneImage(name: "show_room_bg_\(img)")
        nameLabel.text = name
        idLablel.text = "ID: \(id ?? "0")"
        var attachment = NSTextAttachment()
        if #available(iOS 13.0, *) {
            attachment = NSTextAttachment(image: UIImage.show_sceneImage(name: "show_room_person")!)
        } else {
            attachment.image = UIImage.show_sceneImage(name: "show_room_person")
        }
        attachment.bounds = CGRect(x: 0, y: 0, width: 10, height: 10)
        let attriTipsImg = NSAttributedString(attachment: attachment)
        let attriTips = NSMutableAttributedString(attributedString: attriTipsImg)
        attriTips.append(NSAttributedString(string: "  \(count)"))
        numberLabel.attributedText = attriTips
        pureImageView.isHidden = !pureMode
    }
    
    private func createSubviews(){
        imageView = UIImageView()
        imageView.layer.cornerRadius = 10
        imageView.layer.masksToBounds = true
        imageView.contentMode = .scaleAspectFill
        contentView.addSubview(imageView)
        imageView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        let stateImgView = UIImageView()
        stateImgView.image = UIImage.show_sceneImage(name: "show_live_indictor")
        stateImgView.contentMode = .scaleAspectFit
        contentView.addSubview(stateImgView)
        stateImgView.snp.makeConstraints { make in
            make.top.equalTo(5)
            make.right.equalTo(-5)
        }
        stateImgView.addSubview(liveStatusLabel)
        liveStatusLabel.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.trailing.equalToSuperview().offset(-10)
            make.leading.equalToSuperview().offset(27)
        }
        
        pureImageView.image = UIImage.show_sceneImage(name: "show_live_indictor")
        pureImageView.contentMode = .scaleAspectFit
        pureImageView.isHidden = true
        contentView.addSubview(pureImageView)
        pureImageView.snp.makeConstraints { make in
            make.right.equalTo(stateImgView.snp.left)
            make.centerY.equalTo(liveStatusLabel)
        }

        let pureModeLabel = UILabel()
        pureModeLabel.text = "show_pure_mode".show_localized
        pureModeLabel.font = .systemFont(ofSize: 10)
        pureModeLabel.textColor = .white
        pureImageView.addSubview(pureModeLabel)
        pureModeLabel.snp.makeConstraints { make in
            make.centerY.equalToSuperview()
            make.trailing.equalToSuperview().offset(-10)
            make.leading.equalToSuperview().offset(27)
        }
        
        let coverImgView = UIImageView()
        coverImgView.image = UIImage.show_sceneImage(name: "show_list_cover")
        contentView.addSubview(coverImgView)
        coverImgView.snp.makeConstraints { make in
            make.left.right.bottom.equalToSuperview()
        }
        
        nameLabel = UILabel()
        nameLabel.font = .show_M_12
        nameLabel.textColor = .show_main_text
        nameLabel.numberOfLines = 2
        nameLabel.text = "Chat with Eve tonight and merry Christmas"
        contentView.addSubview(nameLabel)
        nameLabel.snp.makeConstraints { make in
            make.left.equalTo(10)
            make.bottom.equalTo(coverImgView).offset(-30)
            make.right.equalTo(-10)
        }
        
        // id
        idLablel = UILabel()
        idLablel.font = .show_R_10
        idLablel.textColor = .show_main_text
        contentView.addSubview(idLablel)
        idLablel.snp.makeConstraints { make in
            make.left.equalTo(nameLabel)
            make.bottom.equalTo(-10)
        }
        
        numberLabel = UILabel()
        numberLabel.font = .show_R_10
        numberLabel.textColor = .show_main_text
        contentView.addSubview(numberLabel)
        numberLabel.snp.makeConstraints { make in
            make.centerY.equalTo(idLablel)
            make.left.equalTo(102)
            make.width.equalTo(60)
        }
    }
}
