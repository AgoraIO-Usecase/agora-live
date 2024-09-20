//
//  DreamFlowService.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/18.
//

import Foundation
import AgoraCommon

enum DreamFlowWorkState {
    case loaded
    case initialize
}

class DreamFlowService {
    let region = "cn"
    let inUid = 0
    let inRole = 0
    let genaiUid = 999
    
    var responseModel: DreamFlowResponseModel?
    
    func creatWork(channelName: String, stylizedConfig:DFStylizedSettingConfig, completion: @escaping ((Error?, DreamFlowWorkState?) -> Void)) {
        var requestModel = DreamFlowCreatWorkModel(region: region, appId: AppContext.shared.appId)
        let rtcConfigure = DreamFlowUidConfig()
        let config = DreamFlowRtcConfig()
        config.inUid = inUid
        config.inToken = AppContext.shared.appId
        config.inChannelName = channelName
        config.inRole = inRole
        config.genaiUid = genaiUid
        config.prompt = stylizedConfig.prompt
        
        rtcConfigure.userids = [config]
        requestModel.name = AppContext.shared.appId + channelName
        requestModel.rtcConfigure = rtcConfigure
        requestModel.prompt = stylizedConfig.prompt
        requestModel.style = stylizedConfig.style
        requestModel.strength = stylizedConfig.strength
        requestModel.face_mode = stylizedConfig.face_mode
        requestModel.request { error, res in
            if let response = VLResponseData.yy_model(withJSON: res), let responseData = response.data {
                guard let model = DreamFlowResponseModel.yy_model(withJSON: responseData) else {
                    completion(error, nil)
                    return
                }
                
                self.responseModel = model
                if response.code == 0 {
                    completion(nil, .initialize)
                } else if response.code == 1 {
                    completion(nil, .loaded)
                }
                
                return
            }
            
            completion(error, nil)
        }
    }
    
    func deleteWork(workId: String, completion: @escaping ((Error?, Any?) -> Void)) {
        let requestModel = DreamFlowDeleteWorkModel(region: region, appId: AppContext.shared.appId, workId: workId)
        requestModel.request(completion: completion)
    }
    
    func updateWork(workId: String, completion: @escaping ((Error?, Any?) -> Void)) {
        let requestModel = DreamFlowUpdateWorkModel(region: region, appId: AppContext.shared.appId, workId: workId)
        requestModel.request(completion: completion)
    }
    
    func deleteAllWork(completion: @escaping ((Error?, Any?) -> Void)) {
        let requestModel = DreamFlowDeleteWorkModel(region: region, appId: AppContext.shared.appId, workId: "b3a21856-88a8-4523-b553-cfaf14af5784")
//        requestModel.admin = true
        requestModel.request(completion: completion)
    }
}


