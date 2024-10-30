//
//  VRVerifyCodeTextView.swift
//  VoiceRoomBaseUIKit
//
// Created by Zhu Jichao on August 25, 2022
//

import UIKit

public class VRVerifyCodeTextView: UITextField {
    // Whether to hide all menus
    var isHiddenAllMenu = false

    /// Paste - This closure is called twice when isTrigger = true for the actual paste
    var pasteClouruse: ((_ isTrigger: Bool) -> Bool)?

    /// Select - This closure is called twice when isTrigger = true is the actual selection
    var selectClouruse: ((_ isTrigger: Bool) -> Bool)?

    /// Select All - This closure is called twice when isTrigger = true to actually select all
    var selectAllClouruse: ((_ isTrigger: Bool) -> Bool)?

    /// Copy - This closure is called twice when isTrigger = true for the actual copy
    var copyClouruse: ((_ isTrigger: Bool) -> Bool)?

    /// Clipping - This closure is called twice when isTrigger = true for the actual clipping
    var cutClouruse: ((_ isTrigger: Bool) -> Bool)?

    /// Delete - This closure is called twice when isTrigger = true to actually delete
    var deleteClouruse: ((_ isTrigger: Bool) -> Bool)?

    public override func canPerformAction(_ action: Selector, withSender sender: Any?) -> Bool {
        if isHiddenAllMenu {
            return false
        }

        // Menu hidden or not
        var isTrigger = false

        if let vc = sender as? UIMenuController {
            isTrigger = vc.isMenuVisible
        }

        switch action {
        case #selector(UIResponderStandardEditActions.paste(_:)):
            if let pasteClouruse = pasteClouruse {
                return pasteClouruse(isTrigger)
            } else {
                return super.canPerformAction(action, withSender: sender)
            }
        case #selector(UIResponderStandardEditActions.select(_:)):
            if let selectClouruse = selectClouruse {
                return selectClouruse(isTrigger)
            } else {
                return super.canPerformAction(action, withSender: sender)
            }
        case #selector(UIResponderStandardEditActions.selectAll(_:)):
            if let selectAllClouruse = selectAllClouruse {
                return selectAllClouruse(isTrigger)
            } else {
                return super.canPerformAction(action, withSender: sender)
            }
        case #selector(UIResponderStandardEditActions.copy(_:)):
            if let copyClouruse = copyClouruse {
                return copyClouruse(isTrigger)
            } else {
                return super.canPerformAction(action, withSender: sender)
            }
        case #selector(UIResponderStandardEditActions.cut(_:)):
            if let cutClouruse = cutClouruse {
                return cutClouruse(isTrigger)
            } else {
                return super.canPerformAction(action, withSender: sender)
            }
        case #selector(UIResponderStandardEditActions.delete(_:)):
            if let deleteClouruse = deleteClouruse {
                return deleteClouruse(isTrigger)
            } else {
                return super.canPerformAction(action, withSender: sender)
            }
        default:
            return super.canPerformAction(action, withSender: sender)
        }
    }
}
