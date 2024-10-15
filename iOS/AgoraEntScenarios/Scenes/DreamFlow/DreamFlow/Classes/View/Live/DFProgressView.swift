//
//  DreamFlowProgressView.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/20.
//

import UIKit

class DFProgressView: UIView {
    private let progressLayer = CAShapeLayer()
    private let titleLabel = UILabel()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }
    
    private func setupView() {
        // 设置进度层
        progressLayer.path = createSemiCircularPath().cgPath
        progressLayer.fillColor = UIColor.clear.cgColor
        progressLayer.strokeColor = UIColor.show_zi03.cgColor
        progressLayer.lineWidth = 5
        progressLayer.lineCap = .round
        layer.addSublayer(progressLayer)
        
        // 设置标题标签
        titleLabel.text = "dream_flow_initializing".show_localized
        titleLabel.textColor = UIColor.show_slider_tint
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        addSubview(titleLabel)
        
        // 布局标签
        NSLayoutConstraint.activate([
            titleLabel.centerXAnchor.constraint(equalTo: centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: centerYAnchor)
        ])
        
        // 添加旋转动画
        addRotationAnimation()
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        progressLayer.frame = bounds
        progressLayer.path = createSemiCircularPath().cgPath
    }

    private func createSemiCircularPath() -> UIBezierPath {
        let center = CGPoint(x: bounds.width / 2, y: bounds.height / 2)
        let radius = min(bounds.width, bounds.height) / 2 - 10
        return UIBezierPath(arcCenter: center, radius: radius, startAngle: -CGFloat.pi / 4, endAngle: CGFloat.pi / 4, clockwise: true)
    }
    
    private func addRotationAnimation() {
        let rotationAnimation = CABasicAnimation(keyPath: "transform.rotation")
        rotationAnimation.toValue = NSNumber(value: Double.pi * 2)
        rotationAnimation.duration = 1
        rotationAnimation.isCumulative = true
        rotationAnimation.repeatCount = Float.infinity
        progressLayer.add(rotationAnimation, forKey: "rotationAnimation")
    }
    
    static func show(in view: UIView) -> DFProgressView {
        let progressBar = DFProgressView()
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
