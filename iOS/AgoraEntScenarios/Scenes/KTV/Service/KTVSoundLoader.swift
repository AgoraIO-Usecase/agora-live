//
//  KTVSoundLoader.swift
//  AgoraEntScenarios
//
//  Created by Jonathan on 2024/6/21.
//

import Foundation
import YYModel

@objc class KTVSongModel: NSObject {
    @objc var lyric: String = ""
    @objc var music: String = ""
    @objc var name: String = ""
    @objc var singer: String = ""
    @objc var songCode: String = ""
}


@objc class KTVSoundLoader: NSObject {
    
    var sounds = [KTVSongModel]()
    
    func getLyricURL(songCode: Int) -> String? {
        let url = sounds.first(where: {Int($0.songCode) == songCode})
        return url?.lyric
    }
    
    func getMusicURL(songCode: Int) -> String? {
        let url = sounds.first(where: {Int($0.songCode) == songCode})
        return url?.music
    }
    
    @objc func fetchSongList(complete: ((_ list: [KTVSongModel]) -> Void)?) {
        if sounds.count > 0 {
            complete?(sounds)
        } else {
            VLSongListNetworkModel().request { e, rsp in
                guard let rsp = rsp as? VLResponseData,
                      rsp.code == 0,
                      let data = rsp.data as? [String: Any],
                      let songs = data["songs"]
                else {
                    complete?(self.sounds)
                    return
                }
                if let s = NSArray.yy_modelArray(with: KTVSongModel.self, json: songs) as? [KTVSongModel] {
                    self.sounds = s
                }
                complete?(self.sounds)
            }
        }
    }
}


private class VLSongListNetworkModel: VLCommonNetworkModel {
        
    public override init() {
        super.init()
        host = KeyCenter.baseServerUrl ?? ""
        host = "https://service-staging.agora.io/toolbox/v1"
        interfaceName = "/ktv/songs"
        method = .get
    }
}
