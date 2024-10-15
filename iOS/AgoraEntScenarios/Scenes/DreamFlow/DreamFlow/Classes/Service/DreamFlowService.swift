//
//  DreamFlowService.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/18.
//

import Foundation
import AgoraCommon

enum DreamFlowWorkState {
    case unload
    case running
    case initialize
    case failed
}

class DreamFlowService {
    let region = "cn"
    let inUid = 0
//    let inRole = 0
    let genaiUid = 999
    var workState: DreamFlowWorkState = .unload
    var responseModel: DreamFlowResponseModel?
    var currentConfig: DFStylizedSettingConfig?
    var server: String = "http://175.121.93.70:50249"
    
    func creatWork(channelName: String, stylizedConfig:DFStylizedSettingConfig, completion: @escaping ((Error?, DreamFlowWorkState?) -> Void)) {
        currentConfig = stylizedConfig
        
        var requestModel = DreamFlowCreatWorkModel(region: region, appId: AppContext.shared.appId)
        let rtcConfigure = DreamFlowUidConfig()
        let config = DreamFlowRtcConfig()
        config.inUid = inUid
//        config.inToken = AppContext.shared.appId
        config.inChannelName = channelName
        config.genaiToken = ""
//        config.inRole = inRole
        config.genaiUid = genaiUid
        config.prompt = stylizedConfig.prompt
        config.genaiVideoWidth = stylizedConfig.videoWidth
        config.genaiVideoHeight = stylizedConfig.videHeight
        
        rtcConfigure.userids = [config]
        requestModel.name = AppContext.shared.appId + channelName
        requestModel.rtcConfigure = rtcConfigure
        requestModel.style = stylizedConfig.style
        requestModel.strength = stylizedConfig.strength
        requestModel.faceMode = stylizedConfig.face_mode
        requestModel.superFrameFactor = stylizedConfig.superFrameFactor
        requestModel.host = server
        requestModel.request { error, res in
            if let response = VLResponseData.yy_model(withJSON: res), let responseData = response.data {
                guard let model = DreamFlowResponseModel.yy_model(withJSON: responseData) else {
                    completion(error, nil)
                    return
                }
                
                self.responseModel = model
                
                guard let stat = model.stat else {
                    self.workState = .failed
                    completion(nil, self.workState)
                    return
                }
                
                if stat == "starting" {
                    self.workState = .initialize
                } else if stat == "running" {
                    self.workState = .running
                } else {
                    self.workState = .failed
                }
                
                completion(nil, self.workState)
                return
            }
            
            completion(error, nil)
        }
    }
    
    func deleteWorker(workerId: String, completion: @escaping ((Error?, Any?) -> Void)) {
        let requestModel = DreamFlowDeleteWorkModel(region: region, appId: AppContext.shared.appId, workerId: workerId)
        requestModel.host = server
        requestModel.request { error, res in
            if error != nil {
                print("delete worker: \(workerId) failed error: \(error?.localizedDescription)")
            } else {
                self.workState = .unload
            }
            
            completion(error, res)
        }
    }
    
    func updateWorker(workerId: String, stylizedConfig:DFStylizedSettingConfig, completion: @escaping ((Error?, Any?) -> Void)) {
        let requestModel = DreamFlowUpdateWorkModel(region: region, appId: AppContext.shared.appId, workerId: workerId)
        requestModel.prompt = stylizedConfig.prompt
        requestModel.strength = stylizedConfig.strength
        requestModel.superFrameFactor = stylizedConfig.superFrameFactor
        requestModel.style = stylizedConfig.style
        requestModel.faceMode = stylizedConfig.face_mode
        requestModel.host = server
        requestModel.request(completion: completion)
    }
    
    func deleteAllWorker(completion: @escaping ((Error?, Any?) -> Void)) {
        let requestModel = DreamFlowDeleteWorkModel(region: region, appId: AppContext.shared.appId, workerId: "b3a21856-88a8-4523-b553-cfaf14af5784")
        requestModel.host = server
        requestModel.request { error, res in
            if error != nil {
                print("delete worker failed error: \(error?.localizedDescription)")
            }
            
            completion(error, res)
        }
    }
    
    func queryWorker(workerId: String, completion: @escaping ((Error?, String?) -> Void)) {
        let requestModel = DreamFlowQueryWorkModel(region: region, appId: AppContext.shared.appId, workerId: workerId)
        requestModel.host = server
        requestModel.request { error, res in
            if let response = VLResponseData.yy_model(withJSON: res), 
                let responseData = response.data as? [String: Any],
                let workerInfo = responseData["workerInfo"] as? [String: Any],
                let stylizeAi = workerInfo["stylize_ai"] as? [String: Any],
                let state = stylizeAi["state"] as? String {
                completion(nil, state)
            } else {
                completion(error, nil)
            }
        }
    }
}


