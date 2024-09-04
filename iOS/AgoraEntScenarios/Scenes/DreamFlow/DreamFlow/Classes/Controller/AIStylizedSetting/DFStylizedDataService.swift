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
        "Face Mode",
        "Strength",
        "Style",
        "Effect"
    ]
    
    let styles: [DFStylizedSettingItem] = [
        DFStylizedSettingItem(title: "Toonyou",
                              imageName: "dream_flow_toonyou",
                              content: "Toonyou"),
        DFStylizedSettingItem(title: "Miyazaki",
                              imageName: "dream_flow_miyazaki",
                              content: "Miyazaki"),
        DFStylizedSettingItem(title: "Sexytoon",
                              imageName: "dream_flow_sexytoon",
                              content: "Sexytoon"),
        DFStylizedSettingItem(title: "Clay",
                              imageName: "show_flow_clay",
                              content: "Clay")
    ]
    
    let effects: [DFStylizedSettingItem] = [
        DFStylizedSettingItem(title: "Cartoon",
                              imageName: "dream_flow_cartoon",
                              content: "WesternAnimeLike, ((1boy, solo, male focus)), ((cowboy shot)), military uniform, black eyes, closed mouth, grey hair, grey jacket, grey shirt, hair between eyes, jacket, looking at viewer, multicolored hair, red pupils, short hair, simple background, smile, solo, two-tone hair, white background, [CHAR-M Genshin Impact Alhaitham] ((perfect face)), very deep eyes, (cinematic lighting), detailed eyes, best quality, bishoujo, sidelight, highres, (intricate details)"),
        DFStylizedSettingItem(title: "Joker",
                              imageName: "dream_flow_cartoon",
                              content: "upper body,((glossy eyes)),(masterpiece, best quality:1.4)best quality, high detail, (detailed face), detailed eyes, (beautiful, aesthetic, perfect, delicate, intricate:1.0), joker painting of a man with green hair and a yellow jacket, digital art by Nicholas Marsicano, reddit, digital art, portrait of joker, portrait of the joker, portrait of a joker, the joker, joker, from joker (2019),  film still of the joker,"),
        DFStylizedSettingItem(title: "Elf",
                              imageName: "dream_flow_cartoon",
                              content: "(masterpiece, best quality), 1girl, solo, elf, green curvy hairs, mist, sundress, forest, sitting, in water, waterfall, looking at viewer, blurry foreground, dappled sunlight, moss, (intricate, lotus, mushroom)"),
        DFStylizedSettingItem(title: "Cyberpunk",
                              imageName: "dream_flow_cartoon",
                              content: "cyberpunk, skyscraper, (bokeh, best quality, masterpiece, highres_1) 1girl, purple hair, ((purple eyeshadow)), emo_hairstyle"),
        DFStylizedSettingItem(title: "Art",
                              imageName: "dream_flow_cartoon",
                              content: "bare shoulders, close-up shot, 35mm, masterpiece, best quality, highly detailed, 1girl, white hair, long hair, long white-haired female wearing a white hoodie, digital art"),
        DFStylizedSettingItem(title: "Customized",
                              imageName: "dream_flow_custom",
                              content: "bare shoulders, close-up shot, 35mm, masterpiece, best quality, highly detailed, 1girl, white hair, long hair, long white-haired female wearing a white hoodie, digital art")
    ]
    
    
    func saveStylizedSettingConfig(_ config: DFStylizedSettingConfig) {
        UserDefaults.standard.saveStylizedSettingConfig(config)
    }
    
    func loadStylizedSettingConfig() -> DFStylizedSettingConfig? {
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
