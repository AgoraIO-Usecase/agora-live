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
        config.inUid = Int(UserInfo.userId) ?? 0
//        config.inToken = AppContext.shared.appId
        config.inChannelName = channelName
        config.genaiToken = stylizedConfig.aiToken
//        config.inRole = inRole
        config.genaiUid = genaiUid
        config.prompt = stylizedConfig.prompt
        config.genaiVideoWidth = stylizedConfig.videoWidth
        config.genaiVideoHeight = stylizedConfig.videHeight
        
        rtcConfigure.userids = [config]
        requestModel.name = "agoralive"
        requestModel.rtcConfigure = rtcConfigure
        requestModel.style = stylizedConfig.style
        requestModel.strength = stylizedConfig.strength
        requestModel.faceMode = stylizedConfig.face_mode
        requestModel.superFrameFactor = stylizedConfig.superFrameFactor
        requestModel.host = server
        requestModel.request { error, res in
            if error != nil {
                completion(error, .unload)
                ShowLogger.error("create worker error :\(error?.localizedDescription)")
                return
            }
            
            if let response = VLResponseData.yy_model(withJSON: res), let responseData = response.data {
                guard let model = DreamFlowResponseModel.yy_model(withJSON: responseData) else {
                    ShowLogger.error("create worker analysis json error")
                    completion(error, nil)
                    return
                }
                
                self.responseModel = model
                
                guard let stat = model.stat else {
                    self.workState = .failed
                    ShowLogger.error("work start failed")
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
                
                ShowLogger.info("work create success")
                completion(nil, self.workState)
                return
            }
            
            ShowLogger.error("Analysis json error")
            completion(error, nil)
        }
    }
    
    func deleteWorker(workerId: String, completion: @escaping ((Error?, Any?) -> Void)) {
        let requestModel = DreamFlowDeleteWorkModel(region: region, appId: AppContext.shared.appId, workerId: workerId)
        requestModel.host = server
        requestModel.request { error, res in
            if error != nil {
                ShowLogger.error("delete worker: \(workerId) failed error: \(error?.localizedDescription)")
                print("delete worker: \(workerId) failed error: \(error?.localizedDescription)")
            } else {
                ShowLogger.info("delete worker success, work Id: \(workerId)")
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
        requestModel.host = server
        requestModel.request { err, res in
            if let error = err {
                ShowLogger.error("request update worker failed: \(error.localizedDescription)")
                completion(err, nil)
                return
            }
            
            ShowLogger.info("update worker success")
            completion(nil, res)
        }
    }
    
    func deleteAllWorker(completion: @escaping ((Error?, Any?) -> Void)) {
        let requestModel = DreamFlowDeleteWorkModel(region: region, appId: AppContext.shared.appId, workerId: "b3a21856-88a8-4523-b553-cfaf14af5784")
        requestModel.host = server
        requestModel.request { error, res in
            if error != nil {
                ShowLogger.error("delete worker failed error: \(error?.localizedDescription)")
                print("delete all worker failed error: \(error?.localizedDescription)")
            } else {
                ShowLogger.info("delete all worker success")
            }
            
            completion(error, res)
        }
    }
    
    func queryWorker(workerId: String, completion: @escaping ((Error?, String?) -> Void)) {
        let requestModel = DreamFlowQueryWorkModel(region: region, appId: AppContext.shared.appId, workerId: workerId)
        requestModel.host = server
        requestModel.request { error, res in
            if let err = error {
                ShowLogger.error("request query worker failed error: \(err.localizedDescription)")
                completion(err, nil)
                return
            }
            
            if let response = VLResponseData.yy_model(withJSON: res),
                let responseData = response.data as? [String: Any],
                let workerInfo = responseData["workerInfo"] as? [String: Any],
                let stylizeAi = workerInfo["stylize_ai"] as? [String: Any],
                let state = stylizeAi["state"] as? String {
                ShowLogger.info("query worker success")
                completion(nil, state)
            } else {
                ShowLogger.error("query worker json failed error: \(error?.localizedDescription)")

                completion(error, nil)
            }
        }
    }
}


