//
//  DreamFlowModel.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/18.
//

import Foundation
import AgoraCommon

@objcMembers
class DreamFlowNetworkModel: AUINetworkModel {
    init(region: String, appId: String) {
        super.init()
        self.host = "http://175.121.93.70:50249"
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

@objcMembers
class DreamFlowCreatWorkModel: DreamFlowNetworkModel {
    var name: String?
    var style: String?
    var strength: Float = 0.1
    var superFrameFactor: Int = 1
    var rtcConfigure: DreamFlowUidConfig?
    var faceMode: Bool = false
    
    override init(region: String, appId: String) {
        super.init(region: region, appId: appId)
        self.interfaceName = "/\(region)/v1/projects/\(appId)/stylize"
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

@objcMembers
class DreamFlowRtcConfig: NSObject {
    var inUid: Int = 0
//    var inToken: String = ""
    var inChannelName: String = ""
//    var inRole: Int = 0
//    var inVideo: String = ""
    var genaiUid: Int = 0
    var genaiToken: String = ""
//    var genaiChannelName: String = ""
//    var genaiRole: Int = 0
//    var genaiVideo: String = ""
    var genaiVideoWidth: Int = 0
    var genaiVideoHeight: Int = 0
    var prompt: String = ""
}

@objcMembers
class DreamFlowUidConfig: NSObject {
    var userids: [DreamFlowRtcConfig]?
}

@objcMembers
class DreamFlowQueryWorkModel: DreamFlowNetworkModel {
    init(region: String, appId: String, workerId: String) {
        super.init(region: region, appId: appId)
        self.interfaceName = "/\(region)/v1/projects/\(appId)/stylize/\(workerId)"
        self.method = .get
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

@objcMembers
class DreamFlowDeleteWorkModel: DreamFlowNetworkModel {
    var admin: Bool = true
    init(region: String, appId: String, workerId: String) {
        super.init(region: region, appId: appId)
        self.interfaceName = "/\(region)/v1/projects/\(appId)/stylize/\(workerId)"
        self.method = .delete
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

class DreamFlowUpdateWorkModel: DreamFlowNetworkModel {
    var style: String?
    var strength: Float = 0.1
    var superFrameFactor: Int = 1
    var prompt: String?

    init(region: String, appId: String, workerId: String) {
        super.init(region: region, appId: appId)
        self.interfaceName = "/\(region)/v1/projects/\(appId)/stylize/\(workerId)"
        self.method = .patch
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

@objcMembers
class DreamFlowResponseModel: NSObject {
    var id: String?
    var createTs: Int64?
    var updateTs: Int64?
    var name: String?
    var stat: String?
}


struct DreamFlowServer {
    var name: String
    var server: String
}
