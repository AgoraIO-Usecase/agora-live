//
//  AGListModel.swift
//  VideoLoaderAPI
//
//  Created by wushengtao on 2023/8/30.
//

import Foundation

public let kUIListViewCellIdentifier = "ag_UICollectionViewCell"
func createDateFormatter()-> DateFormatter {
    let formatter = DateFormatter()
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    return formatter
}

public let formatter = createDateFormatter()
@objc public protocol IVideoLoaderRoomInfo: NSObjectProtocol {
    
    /// The interactive object of the current room, if there is count > 1, it means pk or Lianmai, and count == 1 means a single anchor display.
    var anchorInfoList: [AnchorInfo] {get}
    
    /// The id of the current room
    /// - Returns: <#description#>
    func channelName() -> String
    
    /// The owner of the current room uid
    /// - Returns: <#description#>
    func userId() -> String
}

