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
        self.host = "http://104.15.30.249:49327"
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

@objcMembers
class DreamFlowCreatWorkModel: DreamFlowNetworkModel {
    var name: String?
    var rtcConfigure: DreamFlowUidConfig?
    var prompt: String?
    var style: String?
    var strength: Float = 0.1
    var face_mode: Bool = false
    
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
    var inToken: String = ""
    var inChannelName: String = ""
    var inRole: Int = 0
    var inVideo: String = ""
    var genaiUid: Int = 0
    var genaiToken: String = ""
    var genaiChannelName: String = ""
    var genaiRole: Int = 0
    var genaiVideo: String = ""
    var prompt: String = ""
}

@objcMembers
class DreamFlowUidConfig: NSObject {
    var userids: [DreamFlowRtcConfig]?
}

@objcMembers
class DreamFlowModifyWorkModel: DreamFlowNetworkModel {
    init(region: String, appId: String, workId: String, method: AUINetworkMethod) {
        super.init(region: region, appId: appId)
        self.interfaceName = "/\(region)/v1/projects/\(appId)/stylize/\(workId)"
        self.method = method
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

@objcMembers
class DreamFlowDeleteWorkModel: DreamFlowModifyWorkModel {
    var admin: Bool = true
    init(region: String, appId: String, workId: String) {
        super.init(region: region, appId: appId, workId: workId, method: .delete)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

class DreamFlowUpdateWorkModel: DreamFlowModifyWorkModel {
    var rtcConfigure: DreamFlowUidConfig?

    init(region: String, appId: String, workId: String) {
        super.init(region: region, appId: appId, workId: workId, method: .patch)
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
