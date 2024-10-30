//
//  ShowRealTimeDataView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2022/11/9.
//
import UIKit
import Agora_Scene_Utils
import AgoraRtcKit
import YYCategories
import AgoraCommon

private func createLabel() -> AGELabel {
    let label = AGELabel(colorStyle: .white, fontStyle: .small)
    label.textAlignment = .left
    label.numberOfLines = 0
    label.text = nil
    
    return label
}

class ShowRealTimeDataView: UIView {
    private var isThumbnailType: Bool = true
    private var realDatas: [(String, String)] = []
    private lazy var infoLabelArray: [(AGELabel, AGELabel)] = []
    private lazy var contentView: UIView = {
        let view = UIView()
        view.backgroundColor = .clear
        return view
    }()
    private lazy var closeButton: AGEButton = {
        let button = AGEButton(style: .systemImage(name: "xmark", imageColor: .white))
        button.addTarget(self, action: #selector(onTapCloseButton), for: .touchUpInside)
        return button
    }()
    
    private lazy var thumbnailButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.sceneImage(name: "show_arrow_right")?.byRotateRight90(), for: .normal)
        button.setImage(UIImage.sceneImage(name: "show_arrow_right")?.byRotateLeft90(), for: .selected)
        button.addTarget(self, action: #selector(onTapArrowButton), for: .touchUpInside)
        return button
    }()
    
    init(isLocal: Bool) {
        super.init(frame: .zero)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    public func update(datas: [(String, String)]) {
        self.realDatas = datas
        guard let firstData = datas.first else {return}
        infoLabelArray.forEach { (leftLabel, rightLabel) in
            leftLabel.snp.removeConstraints()
            rightLabel.snp.removeConstraints()
            leftLabel.text = ""
            rightLabel.text = ""
        }
        
        let visibleDatas = isThumbnailType ? [firstData] : datas
        var prevLeftLabel: UILabel? = nil
        visibleDatas.enumerated().forEach { index, data in
            var _leftLabel: UILabel? = nil
            var _rightLabel: UILabel? = nil
            if self.infoLabelArray.count <= index {
                let leftLabel = createLabel()
                let rightLabel = createLabel()
                self.contentView.addSubview(leftLabel)
                self.contentView.addSubview(rightLabel)
                
                self.infoLabelArray.append((leftLabel, rightLabel))
                _leftLabel = leftLabel
                _rightLabel = rightLabel
            } else {
                let (leftLabel, rightLabel) = self.infoLabelArray[index]
                _leftLabel = leftLabel
                _rightLabel = rightLabel
            }
            
            if let leftLabel = _leftLabel, let rightLabel = _rightLabel {
                self.udpate(leftLabel: leftLabel,
                            rightLabel: rightLabel,
                            leftContent: data.0,
                            rightContent: data.1)
                leftLabel.snp.makeConstraints { make in
                    make.leading.equalTo(15)
                    make.trailing.equalTo(snp.centerX).offset(-2)
                    if let prevLeftLabel = prevLeftLabel {
                        make.top.equalTo(prevLeftLabel.snp.bottom)
                    } else {
                        make.top.equalTo(10)
                    }
                    
                    if index == visibleDatas.count - 1 {
                        make.bottom.equalToSuperview().offset(-10)
                    }
                }
                rightLabel.snp.makeConstraints { make in
                    make.leading.equalTo(snp.centerX).offset(2)
                    make.top.equalTo(leftLabel)
                    make.trailing.equalToSuperview().offset(-3)
                    make.bottom.lessThanOrEqualTo(leftLabel)
                }
            }
            prevLeftLabel = _leftLabel
        }
    }
    
    private func udpate(leftLabel: UILabel,
                        rightLabel: UILabel,
                        leftContent: String,
                        rightContent: String) {
        let attributedString = NSMutableAttributedString(string: leftContent)
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.lineBreakMode = .byTruncatingTail
        paragraphStyle.lineHeightMultiple = 1.3
        attributedString.addAttribute(.paragraphStyle,
                                      value: paragraphStyle,
                                      range: NSRange(location: 0, length: attributedString.length))
        leftLabel.attributedText = attributedString

        let rightAttributedString = NSMutableAttributedString(string: rightContent)
        rightAttributedString.addAttribute(.paragraphStyle,
                                           value: paragraphStyle,
                                           range: NSRange(location: 0, length: rightAttributedString.length))
        rightLabel.attributedText = rightAttributedString
    }
    
    private func setupUI() {
        backgroundColor = UIColor(hex: "#151325", alpha: 0.8)
        layer.cornerRadius = 15
        layer.masksToBounds = true
        widthAnchor.constraint(equalToConstant: Screen.width - 30).isActive = true
        
        addSubview(contentView)
        addSubview(closeButton)
        addSubview(thumbnailButton)
        
        contentView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        closeButton.snp.makeConstraints { make in
            make.top.equalTo(12)
            make.right.equalTo(-15)
        }
        
        thumbnailButton.snp.makeConstraints { make in
            make.left.equalTo(0)
            make.right.equalTo(0)
            make.bottom.equalTo(0)
            make.height.equalTo(30)
        }
    }
    
    @objc
    private func onTapCloseButton() {
        removeFromSuperview()
    }
    
    @objc private func onTapArrowButton() {
        thumbnailButton.isSelected = !thumbnailButton.isSelected
        isThumbnailType = thumbnailButton.isSelected ? false : true
        update(datas: realDatas)
    }
}
