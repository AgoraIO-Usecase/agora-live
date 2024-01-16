//
//  CommerceEmitterLayer.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/21.
//

import UIKit

@objc public protocol CommerceEmitterLayerDelegate: AnyObject {
    @objc
    optional func emitterPath() -> UIBezierPath
    func displayViewForEmitter() -> UIView
}

private let positionAnimtionKey = "positionAnimtionKey"
private let opacityAnimtionKey = "opacityAnimtionKey"
private let roateAnimtionKey = "roateAnimtionKey"
private let scaleAnimtionKey = "scaleAnimtionKey"

class CommerceEmitterLayer: CALayer, CAAnimationDelegate {
    // 生命周期
    var lifeTime: TimeInterval = 2 {
        didSet {
            defaultConfig()
        }
    }
    // 速度 px/s 影响最终消失的距离
    var velocity: Float = 60 {
        didSet {
            defaultConfig()
        }
    }
    // 速度随机范围 必须在(0, 1]之间
    var velocityRange: Float = 0 {
        didSet {
            defaultConfig()
        }
    }
    weak var cm_delegate: CommerceEmitterLayerDelegate? {
        willSet {
            if newValue?.displayViewForEmitter() != nil {
                displayView = newValue?.displayViewForEmitter()
                displayView?.layer.addSublayer(self)
            }
        }
    }
    var fromAlpha: Float = 1.0
    var toAlpha: Float = 1.0
    // 旋转范围
    var roateRange: Double = 0
    var fromScale: Float = 1.0
    var toScale: Float = 1.0
    // 偏移 影响垂直发射出的位置
    var skewing: CGFloat = 30
    // 显示的视图
    private var displayView: UIView?
    // 最终移动的距离
    private var maxLength: CGFloat = 0
        
    class func emitterLayer(size: CGSize, center: CGPoint, image: UIImage) -> CommerceEmitterLayer {
        let layer = CommerceEmitterLayer()
        layer.contents = image.cgImage
        layer.frame = CGRect.init(x: 0, y: 0, width: size.width, height: size.height)
        layer.center = center
        layer.defaultConfig()
        return layer
    }
    
    open func startAnimation() {
        let time = CACurrentMediaTime()
        add(postionAnimation(time: time), forKey: positionAnimtionKey)
        add(opacityAnimation(time: time), forKey: opacityAnimtionKey)
        add(roateAnimation(time: time), forKey: roateAnimtionKey)
        add(scaleAnimation(time: time), forKey: scaleAnimtionKey)
    }
    
    override init() {
        super.init()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        print("fail")
    }
    
    private func defaultConfig() {
        maxLength = CGFloat(lifeTime * Double(randomLength()))
    }
    
    // MARK: Animation
    private func postionAnimation(time: TimeInterval) -> CAKeyframeAnimation {
        let animation = CAKeyframeAnimation.init(keyPath: "position")
        animation.duration = lifeTime
        animation.beginTime = time
        animation.isRemovedOnCompletion = false
        animation.fillMode = CAMediaTimingFillMode.forwards
        if cm_delegate?.emitterPath?() != nil {
            animation.path = cm_delegate?.emitterPath!().cgPath
        } else {
            let path = UIBezierPath()
            path.move(to: center)
            path.addLine(to: CGPoint.init(x: randomX(), y: center.y - maxLength))
            animation.path = path.cgPath
        }
        animation.delegate = self
        return animation
    }
    
    private func opacityAnimation(time: TimeInterval) -> CABasicAnimation {
        let animation = CABasicAnimation.init(keyPath: "opacity")
        animation.duration = lifeTime
        animation.beginTime = time
        animation.isRemovedOnCompletion = false
        animation.fillMode = CAMediaTimingFillMode.forwards
        animation.fromValue = fromAlpha
        animation.toValue = toAlpha
        return animation
    }
    
    private func roateAnimation(time: TimeInterval) -> CABasicAnimation {
        let animation = CABasicAnimation.init(keyPath: "transform.rotation")
        animation.duration = lifeTime
        animation.beginTime = time
        animation.isRemovedOnCompletion = false
        animation.fillMode = CAMediaTimingFillMode.forwards
        animation.fromValue = 0
        animation.toValue = randomRoate()
        return animation
    }
    
    private func scaleAnimation(time: TimeInterval) -> CABasicAnimation {
        let animation = CABasicAnimation.init(keyPath: "transform.scale")
        animation.duration = lifeTime
        animation.beginTime = time
        animation.isRemovedOnCompletion = false
        animation.fillMode = CAMediaTimingFillMode.forwards
        animation.fromValue = fromScale
        animation.toValue = toScale
        return animation
    }
    
    private func randomLength() -> Float {
        guard velocityRange > 0 else {
            return velocity
        }
        let minValue = velocity - velocity * velocityRange
        let maxValue = velocity + velocity * velocityRange
        return Float(arc4random_uniform(UInt32(maxValue - minValue))) + minValue
    }
    
    private func randomRoate() -> Double {
        guard roateRange > 0 else {
            return Double.pi / 2
        }
        let minValue: Double = 0
        let maxValue: Double = roateRange * 2.0
        let r = Double(Float(arc4random()) / Float(UINT32_MAX)) * maxValue + minValue
        if r < roateRange {
            return -r
        } else {
            return r - roateRange
        }
    }
    
    private func randomX() -> CGFloat {
        let leftX = center.x - skewing
        let rightX = center.x + skewing
        return CGFloat(arc4random_uniform(UInt32(rightX - leftX))) + leftX
    }
    
    // MARK: CAAnimationDelegate
    func animationDidStop(_ anim: CAAnimation, finished flag: Bool) {
        removeFromSuperlayer()
        removeAllAnimations()
    }
}
