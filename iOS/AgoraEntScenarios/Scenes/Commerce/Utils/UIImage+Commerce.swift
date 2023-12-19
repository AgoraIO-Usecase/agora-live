//
//  UIImage+Show.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/2.
//

import Foundation

extension UIImage {
    @objc static func commerce_sceneImage(name: String) -> UIImage? {
        return sceneImage(name: name, bundleName: "CommerceResource")
    }
    
    @objc
    static func commerce_beautyImage(name: String?) -> UIImage? {
        guard let imageName = name else { return nil }
        return sceneImage(name: imageName, bundleName: "BeautyResource")
    }
}
