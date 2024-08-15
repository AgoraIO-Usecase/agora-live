//
//  Font+Voice.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/8/15.
//

import Foundation

extension UIFont {
    
    @objc private static func voice_regularFontSize(_ size: CGFloat) ->UIFont? {
        UIFont(name: "PingFangSC-Regular", size: size)
    }
    
    @objc private static func voice_MediumFontSize(_ size: CGFloat) ->UIFont? {
        UIFont(name: "PingFangSC-Medium", size: size)
    }
    
    @objc private static func voice_SemiboldFontSize(_ size: CGFloat) ->UIFont?{
        UIFont(name: "PingFangSC-Semibold", size: size)
    }
    
    @objc static var voice_chat_user_name: UIFont? {
        UIFont(name: "PingFangSC-Semibold", size: 13)
    }
    
    @objc static var voice_chat_msg: UIFont? {
        UIFont(name: "PingFangSC-Regular", size: 13)
    }
    
    @objc static var voice_M_12: UIFont? {
        voice_MediumFontSize(12)
    }
    
    @objc static var voice_M_14: UIFont? {
        voice_MediumFontSize(14)
    }
    
    @objc static var voice_M_15: UIFont? {
        voice_MediumFontSize(15)
    }
    
    @objc static var voice_R_9: UIFont? {
        voice_regularFontSize(9)
    }
    
    @objc static var voice_R_10: UIFont? {
        voice_regularFontSize(10)
    }
    
    @objc static var voice_R_11: UIFont? {
        voice_regularFontSize(11)
    }
    
    @objc static var voice_R_12: UIFont? {
        voice_regularFontSize(12)
    }
    
    @objc static var voice_R_13: UIFont? {
        voice_regularFontSize(13)
    }
    
    @objc static var voice_R_14: UIFont? {
        voice_regularFontSize(14)
    }
    
    @objc static var voice_R_16: UIFont? {
        voice_regularFontSize(16)
    }
    
    @objc static var voice_S_18: UIFont? {
        voice_SemiboldFontSize(18)
    }
    
    @objc static var voice_navi_title: UIFont? {
        voice_SemiboldFontSize(16)
    }
    
    @objc static var voice_btn_title: UIFont? {
        voice_SemiboldFontSize(16)
    }
    
    
}
