//
//  DreamFlowContext.swift
//  Pods
//
//  Created by qinhui on 2024/8/28.
//

import Foundation
import AgoraCommon

extension AppContext {
    @objc public static func dreamFlowScene(viewController: UIViewController) {
        let vc = ShowRoomListVC()
        vc.hidesBottomBarWhenPushed = true
        viewController.navigationController?.pushViewController(vc, animated: true)
    }
}
