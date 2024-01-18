//
//  VRVerifyCodeNumberView.swift
//  VoiceRoomBaseUIKit
//
// Created by Zhu Jichao on August 25, 2022
//

import UIKit
import ZSwiftBaseLib

class VRVerifyCodeNumberView: UIView {
    /// Cursor color
    var cursorColor = UIColor(0x009FFF)

    lazy var numLabel: UILabel = .init(frame: CGRect(x: 0, y: 0, width: self.frame.width, height: self.frame.height)).textAlignment(.center).font(.systemFont(ofSize: 18)).textColor(.darkText)

    lazy var cursor: CAShapeLayer = {
        let shapeLayer = CAShapeLayer().fillColor(self.cursorColor.cgColor)
        shapeLayer.add(opacityAnimation, forKey: "kOpacityAnimation")
        return shapeLayer
    }()

    fileprivate var opacityAnimation: CABasicAnimation = {
        let opacityAnimation = CABasicAnimation(keyPath: "opacity")
        opacityAnimation.fromValue = 1.0
        opacityAnimation.toValue = 0.0
        opacityAnimation.duration = 0.9
        opacityAnimation.repeatCount = 9999
        /*
         removedOnCompletion：The default value is YES, which means that the animation will be removed from the layer after execution, and the graphics will be restored to the state before animation. If you want the layer to remain as it appears after the animation has been executed, set it to NO, but also set the fillMode to kCAFillModeForwards
         */
        opacityAnimation.isRemovedOnCompletion = true
        // Determines the behavior of the current object during the non-active time period. Like before the animation starts or after the animation ends
        opacityAnimation.fillMode = CAMediaTimingFillMode.forwards
        // Speed control function that controls the rhythm of animation running
        /*
         kCAMediaTimingFunctionLinear(Linear) : Uniform speed, giving you a relatively static feeling
         kCAMediaTimingFunctionEaseIn(Progressive) : The animation enters slowly and then speeds away
         kCAMediaTimingFunctionEaseOut(Fade out) : The animation enters at full speed, then slows down to reach the destination
         kCAMediaTimingFunctionEaseInEaseOut(Gradual exit) : The animation enters slowly, accelerates in the middle, and then slows down to reach the destination. This is the default animation behavior.
         */
        opacityAnimation.timingFunction = CAMediaTimingFunction(name: CAMediaTimingFunctionName.easeIn)
        return opacityAnimation
    }()

    override init(frame: CGRect) {
        super.init(frame: frame)
        addSubview(numLabel)
        layer.addSublayer(cursor)
        NotificationCenter.default.addObserver(self, selector: #selector(becomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)

        NotificationCenter.default.addObserver(self, selector: #selector(enterBack), name: UIApplication.didEnterBackgroundNotification, object: nil)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let path = UIBezierPath(rect: CGRect(x: frame.size.width * 0.5, y: frame.size.height * 0.1, width: 1, height: frame.size.height * 0.7))
        cursor.path = path.cgPath
    }

    @objc fileprivate func enterBack() {
        cursor.removeAnimation(forKey: "kOpacityAnimation")
    }

    @objc fileprivate func becomeActive() {
        cursor.add(opacityAnimation, forKey: "kOpacityAnimation")
    }

    @available(*, unavailable)
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

// MARK: - For external invocation of methods

extension VRVerifyCodeNumberView {
    /// Set whether the cursor is hidden
    ///
    /// - Parameter isHidden: isHidden
    func setCursorStatus(_ isHidden: Bool) {
        if isHidden {
            cursor.removeAnimation(forKey: "kOpacityAnimation")
        } else {
            cursor.add(opacityAnimation, forKey: "kOpacityAnimation")
        }
        UIView.animate(withDuration: 0.25) {
            self.cursor.isHidden = isHidden
        }
    }

    /// The captcha is assigned a value and the line color is modified
    ///
    /// - Parameter num: Verification code
    func setNum(num: String?) {
        numLabel.text = num
    }

    /// Sets whether the bottom line is in focus
    ///
    /// - Parameter isFocus: isFocus
    func setBottomLineFocus(isFocus: Bool) {
        if isFocus {
        } else {}
    }

    /// Gets the current verification code
    ///
    /// - Returns: code
    func getNum() -> String {
        return numLabel.text ?? ""
    }

    /// Returns a verification code value
    ///
    /// - Returns: Captcha value
    func getNum() -> String? {
        return numLabel.text
    }
}
