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
    
    
    var title: String? {
        switch self {
        case .sold_out: return "Sold Out"
        default: return "Buy"
        }
    }
    
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
}

@objcMembers
class CommerceGoodsModel: NSObject, YYModel {
    var imageName: String?
    var title: String?
    var quantity: Int = 0
    var price: Int = 0
    var goodsId: String? = UUID().uuidString
}

@objcMembers
class CommerceGoodsBuyModel: NSObject, YYModel {
    var goods: CommerceGoodsModel?
    var status: CommerceBuyStatus = .idle
    
    static func createGoodsData() -> [CommerceGoodsBuyModel] {
        var dataArray = [CommerceGoodsBuyModel]()
        var buyModel = CommerceGoodsBuyModel()
        var goodsModel = CommerceGoodsModel()
        goodsModel.imageName = "commerce_shop_goods_0"
        goodsModel.price = 20
        goodsModel.quantity = 6
        goodsModel.title = "Micro USB to USB-A 2.0 Cable, Nylon Braided Cord, 480Mbps Transfer Speed, Gold-Plated, 10 Foot, Dark Gray"
        buyModel.goods = goodsModel
        buyModel.status = .idle
        dataArray.append(buyModel)
        
        buyModel = CommerceGoodsBuyModel()
        goodsModel = CommerceGoodsModel()
        goodsModel.imageName = "commerce_shop_goods_1"
        goodsModel.price = 5
        goodsModel.quantity = 0
        goodsModel.title = "Meta Quest 2 - 128GB Holiday Bundle - Advanced All-In-One Virtual Reality Headset"
        buyModel.goods = goodsModel
        buyModel.status = .idle
        dataArray.append(buyModel)
        
        buyModel = CommerceGoodsBuyModel()
        goodsModel = CommerceGoodsModel()
        goodsModel.imageName = "commerce_shop_goods_2"
        goodsModel.price = 12
        goodsModel.quantity = 6
        goodsModel.title = "Meta Quest 2 - 128GB Holiday Bundle - Advanced All-In-One Virtual Reality Headset"
        buyModel.goods = goodsModel
        buyModel.status = .idle
        dataArray.append(buyModel)
        
        return dataArray
    }
}

let kDefaultAuctionGoodsName = "commerce_shop_goods_auction"

@objcMembers
class CommerceGoodsAuctionModel: NSObject, YYModel {
    var goods: CommerceGoodsModel?
    var status: CommerceAuctionStatus = .idle
    var startTimestamp: UInt64 = 0
    var endTimestamp: UInt64 = 0
    var bidUser: VLLoginModel?
    var bid: Int = 0
    
    func isTopPrice() -> Bool {
        return bidUser?.id == VLUserCenter.user.id && status == .started
    }
    
    func bidBackgroundColor() -> [UIColor] {
        switch status {
        case .idle, .completion: return [UIColor(hex: "#FFFFFF", alpha: 0.25)]
        case .started:
            return isTopPrice() ? [UIColor(hex: "#9ADDFE", alpha: 1.0)] : [UIColor(hex: "#FFEF4F", alpha: 1.0), UIColor(hex: "#FF416F", alpha: 1.0)]
        }
    }
}
