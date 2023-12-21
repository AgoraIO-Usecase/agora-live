//
//  CommerceShoppingModel.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/21.
//

import UIKit

enum CommerceBuyStatus: Int {
    case idle = 0
    case buy
    case sold_out
    
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

enum CommerceAuctionStatus: Int {
    case idle = 0
    case started
    case top_price
    
    var statusTitleColor: UIColor? {
        switch self {
        case .idle: return UIColor(hex: "#191919", alpha: 1.0)
        default: return UIColor(hex: "#FF5252", alpha: 1.0)
        }
    }
    var statusBackgroundColor: [UIColor] {
        switch self {
        case .idle: return [UIColor(hex: "#FFEF4F", alpha: 1.0), UIColor(hex: "#FF416F", alpha: 1.0)]
        default: return [UIColor(hex: "#000000", alpha: 0.25)]
        }
    }
    
    var bidTitleColor: UIColor? {
        switch self {
        case .idle: return UIColor(hex: "#FFFFFF", alpha: 1.0)
        default: return UIColor(hex: "#191919", alpha: 1.0)
        }
    }
    var bidBackgroundColor: [UIColor] {
        switch self {
        case .idle: return [UIColor(hex: "#FFFFFF", alpha: 0.25)]
        case .started: return [UIColor(hex: "#FFEF4F", alpha: 1.0), UIColor(hex: "#FF416F", alpha: 1.0)]
        default: return [UIColor(hex: "#9ADDFE", alpha: 1.0)]
        }
    }
}

class CommerceShoppingBaseModel: NSObject {
    var imageName: String?
    var title: String?
    var inventory: Int = 0
    var price: Float = 0
}

class CommerceShoppingBuyModel: CommerceShoppingBaseModel {
    var status: CommerceBuyStatus = .idle
}

class CommerceShoppingAuctionModel: CommerceShoppingBaseModel {
    var status: CommerceAuctionStatus = .idle
    var bidUser: VLLoginModel?
    var bidPrice: Float = 0
}
