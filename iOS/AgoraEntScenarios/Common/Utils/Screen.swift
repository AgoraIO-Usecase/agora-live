//
//  Screen.swift
//  Scene-Examples
//
//  Created by zhaoyongqiang on 2021/11/10.
//

import Foundation
import UIKit

public struct Screen {
    public static let width = UIScreen.main.bounds.width
    public static let height = UIScreen.main.bounds.height
    public static let kSeparatorHeight = 1.0 / UIScreen.main.scale
    public static var kNavHeight: CGFloat {
        44 + statusHeight()
    }
    public static let scaleSize: CGFloat = width / 375.0
    
    public static func safeHeight() -> CGFloat {
        guard let safeInserts = UIApplication.kWindow?.safeAreaInsets else {
            return 0
        }
        return height - safeInserts.top - safeInserts.bottom
    }

    /// Proportional width
    public static func uiWidth(_ mywith: CGFloat) -> CGFloat {
        return mywith * scaleSize
    }
    
    public static var isFullScreen: Bool {
        if #available(iOS 11, *) {
            guard let w = UIApplication.shared.delegate?.window, let unwrapedWindow = w else {
                return false
            }
            
            if unwrapedWindow.safeAreaInsets.left > 0 || unwrapedWindow.safeAreaInsets.bottom > 0 {
                print(unwrapedWindow.safeAreaInsets)
                return true
            }
        }
        return false
    }

    public static func statusHeight() -> CGFloat {
        var height: CGFloat = 0.0
        if #available(iOS 13.0, *) {
            let statusBarManager = UIApplication.kWindow?.windowScene?.statusBarManager
            height = statusBarManager?.statusBarFrame.height ?? 44

        } else {
            height = UIApplication.shared.statusBarFrame.height
        }

        return height
    }

    /// tabbar height
    public static func tabbarHeight() -> CGFloat {
        let tabVc = UITabBarController()
        return tabVc.tabBar.frame.size.height
    }

    /// Safety zone bottom height
    public static func safeAreaBottomHeight() -> CGFloat {
        guard let safeInserts = UIApplication.kWindow?.safeAreaInsets else {
            return 0
        }
        return safeInserts.bottom
    }

    /// Height at the top of the safe zone
    public static func safeAreaTopHeight() -> CGFloat {
        guard let safeInserts = UIApplication.shared.windows.first?.safeAreaInsets else {
            return 0
        }
        return safeInserts.top
    }

    public static func contentPixel(pixel: CGFloat) -> CGFloat {
        return Screen.width * pixel / 375.0
    }
}

extension Int {
    public var fit: CGFloat {
        return Screen.scaleSize * CGFloat(self)
    }
}

extension CGFloat {
    public var fit: CGFloat {
        return Screen.scaleSize * CGFloat(self)
    }
}

extension Double {
    public var fit: CGFloat {
        return Screen.scaleSize * CGFloat(self)
    }
}
