//
//  CommerceGoodsModel.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/21.
//

import UIKit

@objc
enum CommerceBuyStatus: Int {
    case idle = 0
    case buy = 1
    case sold_out = 2
    
    var titleColor: UIColor? {
        switch self {
        case .buy: return UIColor(hex: "#191919", alpha: 1.0)
        default: return UIColor(hex: "#A5ADBA", alpha: 1.0)
        }
    }
    var backgroundColor: [UIColor] {
        switch self {
        case .buy: return [UIColor(hex: "#FFEF4F", alpha: 1.0), UIColor(hex: "#FF416F", alpha: 1.0)]
        default: return [UIColor(hex: "#DFE1E6", alpha: 1.0)]
        }
    }
}

@objc
enum CommerceAuctionStatus: Int {
    case idle = 0
    case started = 1
    case completion = 2
    case top_price = 3
    
    var statusTitleColor: UIColor? {
        switch self {
        case .idle, .completion: return UIColor(hex: "#191919", alpha: 1.0)
        default: return UIColor(hex: "#FF5252", alpha: 1.0)
        }
    }
    var statusBackgroundColor: [UIColor] {
        switch self {
        case .idle, .completion: return [UIColor(hex: "#FFEF4F", alpha: 1.0), UIColor(hex: "#FF416F", alpha: 1.0)]
        default: return [UIColor(hex: "#000000", alpha: 0.25)]
        }
    }
    
    var bidTitleColor: UIColor? {
        switch self {
        case .idle, .completion: return UIColor(hex: "#FFFFFF", alpha: 1.0)
        default: return UIColor(hex: "#191919", alpha: 1.0)
        }
    }
    var bidBackgroundColor: [UIColor] {
        switch self {
        case .idle, .completion: return [UIColor(hex: "#FFFFFF", alpha: 0.25)]
        case .started: return [UIColor(hex: "#FFEF4F", alpha: 1.0), UIColor(hex: "#FF416F", alpha: 1.0)]
        default: return [UIColor(hex: "#9ADDFE", alpha: 1.0)]
        }
    }
}

@objcMembers
class CommerceGoodsModel: NSObject, YYModel {
    var imageName: String?
    var title: String?
    var quantity: Int = 0
    var price: Float = 0
}

@objcMembers
class CommerceGoodsBuyModel: NSObject {
    var goods: CommerceGoodsModel?
    var status: CommerceBuyStatus = .idle
}

@objcMembers
class CommerceGoodsAuctionModel: NSObject, YYModel {
    var goods: CommerceGoodsModel?
    var status: CommerceAuctionStatus = .idle
    var timestamp: Int64 = 0
    var bidUser: VLLoginModel?
    var bid: Float = 0
}
