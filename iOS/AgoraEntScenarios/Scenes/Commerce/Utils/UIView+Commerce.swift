//
//  UIView+Show.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/7.
//

import Foundation

extension UIView {
    /// Set partial rounded corners (need to be determined after the frame)
    /// - Parameters:
    /// - corners: specifies the rounded corners
    /// -radius: indicates the radius of a rounded corner
    func commerce_setRoundingCorners(_ corners: UIRectCorner, rect: CGRect? = nil, radius: CGFloat) {
        let path = UIBezierPath(roundedRect: rect ?? bounds, byRoundingCorners: corners, cornerRadii: CGSize(width: radius, height: radius))
        let shapeLayer = CAShapeLayer()
        shapeLayer.path = path.cgPath
        layer.mask = shapeLayer
        layer.masksToBounds = true
    }
    
    func createGradientImage(colors: [UIColor]) -> UIImage? {
        if bounds.size == .zero {
            layoutIfNeeded()
        }
        if colors.count > 1 {
            let view = UIView(frame: bounds)
            let gradientLayer = CAGradientLayer()
            gradientLayer.frame = view.bounds
            gradientLayer.colors = colors.map({ $0.cgColor })
            gradientLayer.startPoint = CGPoint(x: 0, y: 0.5)
            gradientLayer.endPoint = CGPoint(x: 1, y: 0.5)
            view.layer.addSublayer(gradientLayer)

            let renderer = UIGraphicsImageRenderer(size: view.bounds.size)
            let image = renderer.image { (context) in
                view.layer.render(in: context.cgContext)
            }
            return image
        }
        UIGraphicsBeginImageContextWithOptions(size, false, UIScreen.main.scale)
        guard let context = UIGraphicsGetCurrentContext() else { return nil }
        colors.first?.setFill()
        let rectanglePath = UIBezierPath(rect: CGRect(origin: .zero, size: size))
        rectanglePath.fill()

        guard let image = UIGraphicsGetImageFromCurrentImageContext() else {
            fatalError("Unable to get the image from the current context.")
        }
        UIGraphicsEndImageContext()
        return image
    }
}

