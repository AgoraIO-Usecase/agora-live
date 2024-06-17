//
//  NSString+Show.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/4.
//

import Foundation

extension String {
    var commerce_localized: String {
        let resource = AppContext.shared.sceneImageBundleName
        guard let bundlePath = Bundle.main.path(forResource: resource, ofType: "bundle"),
              let bundle = Bundle(path: bundlePath)
        else {
            return self
        }
        
        guard var lang = NSLocale.preferredLanguages.first else {
            return self
        }
        if lang.contains("zh") {
            lang = "zh-Hans"
        }
        guard let langPath = bundle.path(forResource: lang, ofType: "lproj") ?? bundle.path(forResource: "en", ofType: "lproj") else { return self }
        guard let detailBundle = Bundle(path: langPath) else { return self }
        let retStr = NSLocalizedString(self,tableName: "Localizable", bundle:detailBundle, value: self, comment: "")
        return retStr
    }
}
