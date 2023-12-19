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
}


