//
//  Bundle+extension.swift
//  AgoraEntScenarios
//
//  Created by CP on 2024/4/10.
//

import Foundation

public extension Bundle {
    static func localizedString(_ key: String, bundleName: String) -> String {
        guard let path = Bundle.main.path(forResource: bundleName, ofType: "bundle") else {
            return NSLocalizedString(key, comment: "")
        }
        guard let bundle = Bundle(path: path) else {
            return NSLocalizedString(key, comment: "")
        }
        return NSLocalizedString(key, tableName: nil, bundle: bundle, value: "", comment: "")
    }
}

