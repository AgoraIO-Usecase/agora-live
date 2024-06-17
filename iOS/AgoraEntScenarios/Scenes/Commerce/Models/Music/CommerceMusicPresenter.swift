//
//  ShowMusicManager.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/18.
//

import Foundation
import AgoraRtcKit

private let defaultBeautyVoiceIndex = 0
private let defaultMixVoiceIndex = 0

class CommerceMusicPresenter: NSObject {
    
    lazy var dataArray: [CommerceMusicConfigData] = {
        return [musicBg, beautyVoice, mixVoice]
    }()
    
    // Bel canto option
    private lazy var beautyPresets: [AgoraVoiceBeautifierPreset] = {
        return [
            .presetOff,
            .presetSingingBeautifier,
            .presetChatBeautifierVitality,
            .presetChatBeautifierFresh,
            .presetChatBeautifierMagnetic,
            ]
    }()
    
    // Reverberation option
    private lazy var mixPresets: [AgoraAudioEffectPreset] = {
        return [
            .off,
            .roomAcousticsKTV,
            .roomAcousVocalConcer,
            .roomAcousStudio,
            .roomAcousSpatial
        ]
    }()
    
    // Background music
    private lazy var musicBg: CommerceMusicConfigData = {
        var musicBgDataArray = [CommerceMusicEffectCell.CellData]()
        let titles = [
            "show_music_setting_bg_happy".commerce_localized,
            "show_music_setting_bg_romantic".commerce_localized,
            "show_music_setting_bg_happy_2".commerce_localized,
        ]
        let images = [
            "show_music_set_bg",
            "show_music_set_bg",
            "show_music_set_bg",
        ]
        
        for i in 0 ..< titles.count {
            let data = CommerceMusicEffectCell.CellData(image: images[i], title: titles[i], style: .imageTop, isSelected: false)
            musicBgDataArray.append(data)
        }
        return CommerceMusicConfigData(title: "show_music_setting_bg_title".commerce_localized, dataArray: musicBgDataArray, type: .resource)
    }()
    
    // Bel Canto
    private lazy var beautyVoice: CommerceMusicConfigData = {
        var beautyVoiceDataArray = [CommerceMusicEffectCell.CellData]()
        let titles = [
            "show_music_setting_beaty_yuansheng".commerce_localized,
            "show_music_setting_beaty_tianmei".commerce_localized,
            "show_music_setting_beaty_zhongxing".commerce_localized,
            "show_music_setting_beaty_wenzhong".commerce_localized,
            "show_music_setting_beaty_mohuan".commerce_localized,
        ]
        let images = [
            "show_music_beauty_yuanchang",
            "show_music_beauty_tianmei",
            "show_music_beauty_zhongxing",
            "show_music_beauty_wenzhong",
            "show_music_beauty_mohuan",
        ]
        for i in 0 ..< titles.count {
            let data = CommerceMusicEffectCell.CellData(image: images[i], title: titles[i], style: .imageBackground, isSelected: i == defaultBeautyVoiceIndex)
            beautyVoiceDataArray.append(data)
        }
        return CommerceMusicConfigData(title:  "show_music_setting_beatuy_title".commerce_localized, dataArray: beautyVoiceDataArray, type: .beauty)
    }()
    
    // reverberation
    private lazy var mixVoice: CommerceMusicConfigData = {
        var mixVoiceDataArray = [CommerceMusicEffectCell.CellData]()
        let titles = [
            "show_music_setting_mix_none".commerce_localized,
            "show_music_setting_mix_ktv".commerce_localized,
            "show_music_setting_mix_concert".commerce_localized,
            "show_music_setting_mix_record".commerce_localized,
            "show_music_setting_mix_hollowness".commerce_localized,
        ]
        let images = [
            "show_music_mix_none",
            "show_music_mix_KTV",
            "show_music_mix_concert",
            "show_music_mix_record",
            "show_music_mix_hollowness",
        ]
        for i in 0 ..< titles.count {
            let style: CommerceMusicEffectCell.LayoutStyle = i == 0 ? .imageOnly : .imageBackground
            let data = CommerceMusicEffectCell.CellData(image: images[i], title: titles[i], style: style, isSelected: i == defaultMixVoiceIndex)
            mixVoiceDataArray.append(data)
        }
        return CommerceMusicConfigData(title: "show_music_setting_mix_title".commerce_localized, dataArray: mixVoiceDataArray, type: .mixture)
    }()
}

extension CommerceMusicPresenter {
    // Select the music playback source
    func setMusicIndex(_ index: Int?) {
        guard let player = CommerceAgoraKitManager.shared.mediaPlayer() else {
            return
        }
        if let i = index  {
            let musicNames = ["happy", "romantic", "relax"]
            guard let path = Bundle.main.path(forResource: "showResource", ofType: "bundle"),
                  let bundle = Bundle(path: path) else {
                return
            }
            let musicPath = bundle.path(forResource: musicNames[i], ofType: "wav") ?? ""
            player.stop()
            let source = AgoraMediaSource()
            source.url = musicPath
            player.open(with: source)
            setMusicVolume(CommerceAgoraKitManager.shared.rtcParam.musicVolume)
        } else {
            player.stop()
        }
    }
    
    // Select music Bel Canto
    func setBeautyIndex(_ index: Int?) {
        guard let engine = CommerceAgoraKitManager.shared.engine else {
            return
        }
        switch index {
        case 1:
            engine.setVoiceConversionPreset(.sweet)
        case 2:
            engine.setVoiceConversionPreset(.neutral)
        case 3:
            engine.setVoiceConversionPreset(.changerSolid)
        case 4: //
            engine.setAudioEffectPreset(.voiceChangerEffectHulk)
        default:
            engine.setAudioEffectPreset(.off)
            engine.setVoiceConversionPreset(.off)
        }
    }
    
    // Selective reverberation
    func setMixIndex(_ index: Int?){
        guard let engine = CommerceAgoraKitManager.shared.engine else {
            return
        }
        engine.setAudioEffectPreset(mixPresets[index ?? 0])
    }
    
    func setMusicVolume(_ volume: Int) {
        guard let player = CommerceAgoraKitManager.shared.mediaPlayer() else {
            return
        }
        player.adjustPlayoutVolume(Int32(volume))
        player.adjustPublishSignalVolume(Int32(volume))
    }
}

