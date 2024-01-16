//
//  UIFont+Show.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/2.
//

import Foundation

extension UIFont {
    
    @objc private static func commerce_regularFontSize(_ size: CGFloat) ->UIFont? {
        UIFont(name: "PingFangSC-Regular", size: size)
    }
    
    @objc private static func commerce_MediumFontSize(_ size: CGFloat) ->UIFont? {
        UIFont(name: "PingFangSC-Medium", size: size)
    }
    
    @objc private static func commerce_SemiboldFontSize(_ size: CGFloat) ->UIFont?{
        UIFont(name: "PingFangSC-Semibold", size: size)
    }
    
    @objc static var commerce_chat_user_name: UIFont? {
        UIFont(name: "PingFangSC-Semibold", size: 13)
    }
    
    @objc static var commerce_chat_msg: UIFont? {
        UIFont(name: "PingFangSC-Regular", size: 13)
    }
    
    @objc static var commerce_M_12: UIFont? {
        commerce_MediumFontSize(12)
    }
    
    @objc static var commerce_M_14: UIFont? {
        commerce_MediumFontSize(14)
    }
    
    @objc static var commerce_M_15: UIFont? {
        commerce_MediumFontSize(15)
    }
    
    @objc static var commerce_R_9: UIFont? {
        commerce_regularFontSize(9)
    }
    
    @objc static var commerce_R_10: UIFont? {
        commerce_regularFontSize(10)
    }
    
    @objc static var commerce_R_11: UIFont? {
        commerce_regularFontSize(11)
    }
    
    @objc static var commerce_R_12: UIFont? {
        commerce_regularFontSize(12)
    }
    
    @objc static var commerce_R_13: UIFont? {
        commerce_regularFontSize(13)
    }
    
    @objc static var commerce_R_14: UIFont? {
        commerce_regularFontSize(14)
    }
    
    @objc static var commerce_R_16: UIFont? {
        commerce_regularFontSize(16)
    }
    
    @objc static var commerce_S_18: UIFont? {
        commerce_SemiboldFontSize(18)
    }
    
    @objc static var commerce_navi_title: UIFont? {
        commerce_SemiboldFontSize(16)
    }
    
    @objc static var commerce_btn_title: UIFont? {
        commerce_SemiboldFontSize(16)
    }
}
