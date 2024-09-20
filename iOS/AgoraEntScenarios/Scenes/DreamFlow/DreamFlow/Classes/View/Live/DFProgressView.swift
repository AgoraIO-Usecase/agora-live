//
//  DreamFlowProgressView.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/20.
//

import UIKit

import UIKit

class DFProgressView: UIView {
    private let progressLayer = CAShapeLayer()
    private let trackLayer = CAShapeLayer()
    private let titleLabel = UILabel()
    private let progressLabel = UILabel()
    
    var currentProgress: CGFloat = 0 {
        didSet {
            updateProgress()
        }
    }
    
    var totalProgress: CGFloat = 100 {
        didSet {
            updateProgress()
        }
    }
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }
    
    private func setupView() {
        // 设置轨道层
        trackLayer.path = createCircularPath().cgPath
        trackLayer.fillColor = UIColor.clear.cgColor
        trackLayer.strokeColor = UIColor.show_room_info_cover.cgColor
        trackLayer.lineWidth = 5
        trackLayer.lineCap = .round
        layer.addSublayer(trackLayer)
        
        // 设置进度层
        progressLayer.path = createCircularPath().cgPath
        progressLayer.fillColor = UIColor.clear.cgColor
        progressLayer.strokeColor = UIColor.show_zi03.cgColor
        progressLayer.lineWidth = 5
        progressLayer.lineCap = .round
        progressLayer.strokeEnd = 0
        layer.addSublayer(progressLayer)
        
        // 设置标题标签
        titleLabel.text = "dream_flow_initializing".show_localized
        titleLabel.textColor = UIColor.show_slider_tint
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)
        
        // 设置进度标签
        progressLabel.text = "0%"
        progressLabel.textColor = UIColor.show_slider_tint
        progressLabel.textAlignment = .center
        progressLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(progressLabel)
        
        // 布局标签
        NSLayoutConstraint.activate([
            titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: centerYAnchor, constant: -10),
            progressLabel.centerXAnchor.constraint(equalTo: centerXAnchor),
            progressLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 5)
        ])
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        trackLayer.frame = bounds
        progressLayer.frame = bounds
        trackLayer.path = createCircularPath().cgPath
        progressLayer.path = createCircularPath().cgPath
    }

    private func createCircularPath() -> UIBezierPath {
        let center = CGPoint(x: bounds.width / 2, y: bounds.height / 2)
        let radius = min(bounds.width, bounds.height) / 2 - 10
        return UIBezierPath(arcCenter: center, radius: radius, startAngle: -CGFloat.pi / 2, endAngle: 1.5 * CGFloat.pi, clockwise: true)
    }
    
    private func updateProgress() {
        let progress = currentProgress / totalProgress
        progressLayer.strokeEnd = progress
        progressLabel.text = "\(Int(progress * 100))%"
    }
    
    static func show(in view: UIView, withProgress progress: CGFloat = 0, totalProgress: CGFloat = 100) -> DFProgressView {
        let progressBar = DFProgressView()
        progressBar.currentProgress = progress
        progressBar.totalProgress = totalProgress
        view.addSubview(progressBar)
        progressBar.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            progressBar.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            progressBar.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            progressBar.widthAnchor.constraint(equalToConstant: 120),
            progressBar.heightAnchor.constraint(equalToConstant: 120)
        ])
        return progressBar
    }
    
    static func hide(from view: UIView) {
        for subview in view.subviews {
            if let progressBar = subview as? DFProgressView {
                progressBar.removeFromSuperview()
            }
        }
    }
}
