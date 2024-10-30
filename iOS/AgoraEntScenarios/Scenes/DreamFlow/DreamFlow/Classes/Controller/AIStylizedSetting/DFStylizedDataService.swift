//
//  DFDataService.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/2.
//

import Foundation

class DFStylizedDataService {
    let titles = [
        "Stylized Effect",
        "Server",
        "Presets"
    ]
    
    let servers = [
        DreamFlowServer(name: "Server 1", server: "http://175.121.93.70:40743"),
        DreamFlowServer(name: "Server 2", server: "http://175.121.93.70:50249"),
        DreamFlowServer(name: "Server 3", server: "http://104.15.30.249:49327"),
        DreamFlowServer(name: "Server 4", server: "http://66.114.112.70:55587")
    ]
    
    let styles: [DFStylizedSettingItem] = [
        DFStylizedSettingItem(title: "toonyou",
                              imageName: "dream_flow_toonyou",
                              content: "toonyou"),
        DFStylizedSettingItem(title: "Miyazaki",
                              imageName: "dream_flow_miyazaki",
                              content: "Miyazaki"),
        DFStylizedSettingItem(title: "sexytoon",
                              imageName: "dream_flow_sexytoon",
                              content: "sexytoon"),
        DFStylizedSettingItem(title: "Clay",
                              imageName: "dream_flow_caly",
                              content: "Clay"),
        DFStylizedSettingItem(title: "Fantasy",
                              imageName: "dream_flow_fantasy",
                              content: "Fantasy")
    ]
    
    let Presets: [DFPresetSettingItem] = [
        DFPresetSettingItem(title: "Cartoons Full",
                            imageName: "dream_flow_cartoons_full",
                            content: "best quality",
                            faceMode: false,
                            strengthDefaultValue: 0.4),
        DFPresetSettingItem(title: "Clay Full",
                            imageName: "dream_flow_clay_full",
                            content: "Claymation, best quality",
                            faceMode: false,
                            strengthDefaultValue: 0.5,
                            styleIndex: 3),
        DFPresetSettingItem(title: "Anime",
                            imageName: "dream_flow_anime",
                            content: "anime, cute",
                            faceMode: true,
                            strengthDefaultValue: 0.5,
                            styleIndex: 1),
        DFPresetSettingItem(title: "3D",
                            imageName: "dream_flow_3D",
                            content: "elf, green hair",
                            faceMode: true,
                            strengthDefaultValue: 0.6,
                            styleIndex: 2),
        DFPresetSettingItem(title: "Joker",
                            imageName: "dream_flow_joker",
                            content: "Joker, pale face",
                            faceMode: true,
                            strengthDefaultValue: 0.8,
                            styleIndex: 4),
        DFPresetSettingItem(title: "Customized",
                            imageName: "dream_flow_custom",
                            content: "best quality",
                            faceMode: false,
                            strengthDefaultValue: 0.4,
                            isCustom: true)
    ]
    
    
    static func saveStylizedSettingConfig(_ config: DFStylizedSettingConfig) {
        UserDefaults.standard.saveStylizedSettingConfig(config)
    }
    
    static func loadStylizedSettingConfig() -> DFStylizedSettingConfig? {
        return UserDefaults.standard.loadStylizedSettingConfig()
    }
}

extension UserDefaults {
    private enum Keys {
        static let stylizedSettingConfig = "stylizedSettingConfig"
    }
    
    func saveStylizedSettingConfig(_ config: DFStylizedSettingConfig) {
        let encoder = JSONEncoder()
        if let encoded = try? encoder.encode(config) {
            set(encoded, forKey: Keys.stylizedSettingConfig)
        }
    }
    
    func loadStylizedSettingConfig() -> DFStylizedSettingConfig? {
        if let savedConfig = data(forKey: Keys.stylizedSettingConfig) {
            let decoder = JSONDecoder()
            if let loadedConfig = try? decoder.decode(DFStylizedSettingConfig.self, from: savedConfig) {
                return loadedConfig
            }
        }
        return nil
    }
}
