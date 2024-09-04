//
//  ShowCanvasView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2022/11/24.
//

import UIKit
import SnapKit

class DFLocalView: UIView {
    lazy var contentView: UIView = {
        let view = UIView()
        return view
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setupUI() {
        self.addSubview(contentView)
        contentView.snp.makeConstraints { make in
            make.left.top.right.bottom.equalTo(0)
        }
    }
}

class ShowCanvasView: UIView {
    lazy var contentView: UIImageView = {
        let imageView = UIImageView()
        imageView.image = UIImage.show_sceneImage(name: "drame_flow_avatar")
        imageView.contentMode = .scaleAspectFill
        return imageView
    }()
    
    lazy var localBackgroundView: DFLocalView = {
        let view = DFLocalView()
        view.layer.cornerRadius = 20
        view.layer.masksToBounds = true
        return view
    }()
    
    lazy var remoteView: UIView = {
        let view = UIView()
        return view
    }()
    
    lazy var indicatorButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage.show_sceneImage(name: "drame_flow_Indicator"), for: .normal)
        button.addTarget(self, action: #selector(toggleLocalView), for: .touchUpInside)
        return button
    }()
    
    lazy var originTitle: UILabel = {
        let label = UILabel()
        label.font = UIFont.show_R_12
        label.textColor = UIColor.show_slider_tint
        label.text = "Origin"
        return label
    }()
    
    lazy var arrowButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.show_sceneImage(name: "drame_flow_arrow"), for: .normal)
        button.addTarget(self, action: #selector(toggleLocalView), for: .touchUpInside)
    
        return button
    }()
    
    var localViewHidden = false
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupViews()
        setupGestures()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupViews() {
        addSubview(contentView)
        addSubview(remoteView)
        addSubview(localBackgroundView)
        addSubview(indicatorButton)
        localBackgroundView.addSubview(originTitle)
        localBackgroundView.addSubview(arrowButton)
        
        contentView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        remoteView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        localBackgroundView.snp.makeConstraints { make in
            make.width.equalTo(109)
            make.height.equalTo(163)
            make.left.equalToSuperview().offset(15)
            make.bottom.equalToSuperview().offset(-75)
        }
        
        arrowButton.snp.makeConstraints { make in
            make.right.equalTo(-12)
            make.bottom.equalTo(-10)
            make.height.equalTo(12)
            make.width.equalTo(25)
        }
        
        originTitle.snp.makeConstraints { make in
            make.right.equalTo(arrowButton.snp.left)
            make.centerY.equalTo(arrowButton.snp.centerY)
            make.height.equalTo(17)
        }
        
        indicatorButton.snp.makeConstraints { make in
            make.width.equalTo(20)
            make.height.equalTo(52)
            make.left.equalToSuperview().offset(0)
            make.centerY.equalTo(localBackgroundView.snp.centerY)
        }
        
        indicatorButton.isHidden = true
    }
    
    private func setupGestures() {
        let panGesture = UIPanGestureRecognizer(target: self, action: #selector(handlePanGesture(_:)))
        localBackgroundView.addGestureRecognizer(panGesture)
    }
    
    @objc private func handlePanGesture(_ gesture: UIPanGestureRecognizer) {
        let translation = gesture.translation(in: self)
        guard let localView = gesture.view else { return }
        
        var newCenter = CGPoint(x: localView.center.x + translation.x, y: localView.center.y + translation.y)
        
        // 限制 localView 的拖动范围
        let halfWidth = localView.bounds.width / 2
        let halfHeight = localView.bounds.height / 2
        
        newCenter.x = max(halfWidth, newCenter.x)
        newCenter.x = min(self.bounds.width - halfWidth, newCenter.x)
        
        newCenter.y = max(64 + halfHeight, newCenter.y)
        newCenter.y = min(self.bounds.height - 38 - halfHeight, newCenter.y)
        
        localView.center = newCenter
        gesture.setTranslation(.zero, in: self)
    }
    
    @objc private func toggleLocalView() {
        localViewHidden.toggle()
        if localViewHidden {
            UIView.animate(withDuration: 0.3) {
                self.localBackgroundView.snp.updateConstraints { make in
                    make.left.equalToSuperview().offset(-self.localBackgroundView.frame.width)
                }
                self.layoutIfNeeded()
            } completion: { _ in
                self.indicatorButton.isHidden = false
            }
        } else {
            self.indicatorButton.isHidden = true
            UIView.animate(withDuration: 0.3) {
                self.localBackgroundView.snp.updateConstraints { make in
                    make.left.equalToSuperview().offset(15)
                }
                self.layoutIfNeeded()
            }
        }
    }
}
