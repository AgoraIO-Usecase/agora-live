//
//  AUIMapCollection.swift
//  AUIKitCore
//
//  Created by wushengtao on 2024/1/4.
//

import Foundation

@objcMembers
public class AUIMapCollection: AUIBaseCollection {
    private var currentMap: [String: Any] = [:] 
}

//MARK: private set meta data
extension AUIMapCollection {
    private func rtmSetMetaData(publisherId: String,
                                valueCmd: String?,
                                value: [String: Any],
                                callback: ((NSError?)->())?) {
        let newValue = self.valueWillChangeClosure?(publisherId, valueCmd, value) ?? value
        
        if let err = self.metadataWillAddClosure?(publisherId, valueCmd, newValue) {
            callback?(err)
            return
        }
        
        var map = newValue
        if let attr = self.attributesWillSetClosure?(channelName,
                                                     observeKey,
                                                     valueCmd,
                                                     AUIAttributesModel(map: map)),
           let attrMap = attr.getMap() {
            map = attrMap
        }
        guard let value = encodeToJsonStr(map) else {
            callback?(AUICollectionOperationError.encodeToJsonStringFail.toNSError())
            return
        }
        aui_collection_log("rtmSetMetaData valueCmd: \(valueCmd ?? "") value: \(value)")
        setBatchMetadata(value) { error in
            aui_collection_log("rtmSetMetaData completion: \(error?.localizedDescription ?? "success")")
            callback?(error)
        }
        
        currentMap = map
    }
    
    private func rtmUpdateMetaData(publisherId: String,
                                   valueCmd: String?,
                                   value: [String: Any],
                                   callback: ((NSError?)->())?) {
        let newValue = self.valueWillChangeClosure?(publisherId, valueCmd, value) ?? value
        
        if let err = self.metadataWillUpdateClosure?(publisherId, valueCmd, newValue, currentMap) {
            callback?(err)
            return
        }
        
        var map = currentMap
        newValue.forEach { (key: String, value: Any) in
            map[key] = value
        }
        if let attr = self.attributesWillSetClosure?(channelName,
                                                     observeKey,
                                                     valueCmd,
                                                     AUIAttributesModel(map: map)),
           let attrMap = attr.getMap() {
            map = attrMap
        }
        guard let value = encodeToJsonStr(map) else {
            callback?(AUICollectionOperationError.encodeToJsonStringFail.toNSError())
            return
        }
        aui_collection_log("rtmSetMetaData valueCmd: \(valueCmd ?? "") value: \(value)")
        setBatchMetadata(value) { error in
            aui_collection_log("rtmSetMetaData completion: \(error?.localizedDescription ?? "success")")
            callback?(error)
        }
        
        currentMap = map
    }
    
    private func rtmMergeMetaData(publisherId: String,
                                  valueCmd: String?,
                                  value: [String: Any],
                                  callback: ((NSError?)->())?) {
        let newValue = self.valueWillChangeClosure?(publisherId, valueCmd, value) ?? value
        
        if let err = self.metadataWillMergeClosure?(publisherId, valueCmd, newValue, currentMap) {
            callback?(err)
            return
        }
        
        var map = mergeMap(origMap: currentMap, newMap: newValue)
        if let attr = self.attributesWillSetClosure?(channelName,
                                                     observeKey,
                                                     valueCmd,
                                                     AUIAttributesModel(map: map)),
           let attrMap = attr.getMap() {
            map = attrMap
        }
        guard let value = encodeToJsonStr(map) else {
            callback?(AUICollectionOperationError.encodeToJsonStringFail.toNSError())
            return
        }
        aui_collection_log("rtmMergeMetaData valueCmd: \(valueCmd ?? "") value: \(value)")
        setBatchMetadata(value) { error in
            aui_collection_log("rtmMergeMetaData completion: \(error?.localizedDescription ?? "success")")
            callback?(error)
        }
        
        currentMap = map
    }
    
    private func rtmCalculateMetaData(publisherId: String,
                                      valueCmd: String?,
                                      key: [String],
                                      value: AUICollectionCalcValue,
                                      callback: ((NSError?)->())?) {
        
        if let err = self.metadataWillCalculateClosure?(publisherId,
                                                        valueCmd,
                                                        currentMap,
                                                        key,
                                                        value.value,
                                                        value.min,
                                                        value.max) {
            callback?(err)
            return
        }
        
        var map: [String: Any]?
        do {
            map = try calculateMap(origMap: currentMap,
                                   key: key,
                                   value: value.value,
                                   min: value.min,
                                   max: value.max)
        } catch {
            callback?(error as NSError)
            return
        }
        
        if let tmpMap = map,
            let attr = self.attributesWillSetClosure?(channelName,
                                                     observeKey,
                                                     valueCmd, 
                                                     AUIAttributesModel(map: tmpMap)),
           let attrMap = attr.getMap() {
            map = attrMap
        }
        guard let map = map, let value = encodeToJsonStr(map) else {
            callback?(AUICollectionOperationError.encodeToJsonStringFail.toNSError())
            return
        }
        aui_collection_log("rtmCalculateMetaData valueCmd: \(valueCmd ?? "") key: \(key), value: \(value)")
        setBatchMetadata(value) { error in
            aui_collection_log("rtmCalculateMetaData completion: \(error?.localizedDescription ?? "success")")
            callback?(error)
        }
        
        currentMap = map
    }
    
    private func rtmCleanMetaData(callback: ((NSError?)->())?) {
        aui_collection_log("rtmCleanMetaData[\(observeKey)]")
        self.rtmManager.cleanBatchMetadata(channelName: channelName,
                                           lockName: kRTM_Referee_LockName,
                                           removeKeys: [observeKey]) { error in
            aui_collection_log("rtmCleanMetaData completion: \(error?.localizedDescription ?? "success")")
            callback?(error)
        }
    }
}

//MARK: override IAUICollection
extension AUIMapCollection: IAUIMapCollection {
    
    /// Update, replace the root node
    /// - Parameters:
    ///   - valueCmd: Command type
    ///   - value: <#value description#>
    ///   - filter: <#objectId description#>
    ///   - callback: <#callback description#>
    public func updateMetaData(valueCmd: String?,
                                        value: [String: Any],
                                        callback: ((NSError?)->())?) {
        if AUIRoomContext.shared.getArbiter(channelName: channelName)?.isArbiter() ?? false {
            let currentUserId = AUIRoomContext.shared.currentUserInfo.userId
            rtmUpdateMetaData(publisherId: currentUserId,
                              valueCmd: valueCmd,
                              value: value,
                              callback: callback)
            return
        }
        
        let payload = AUICollectionMessagePayload(type: .update, 
                                                  dataCmd: valueCmd,
                                                  data: AUIAnyType(map: value))
        let message = AUICollectionMessage(channelName: channelName,
                                           messageType: AUIMessageType.normal,
                                           sceneKey: observeKey,
                                           uniqueId: UUID().uuidString,
                                           payload: payload)

        guard let jsonStr = encodeModelToJsonStr(message) else {
            callback?(AUICollectionOperationError.encodeToJsonStringFail.toNSError())
            return
        }
        let userId = AUIRoomContext.shared.getArbiter(channelName: channelName)?.lockOwnerId ?? ""
        rtmManager.publishAndWaitReceipt(userId: userId,
                                         channelName: channelName,
                                         message: jsonStr,
                                         uniqueId: message.uniqueId,
                                         completion: callback)
    }
    
    /// Merge and replace all sub-nodes
    /// - Parameters:
    ///   - valueCmd: <#valueCmd description#>
    ///   - value: <#value description#>
    ///   - filter: <#objectId description#>
    ///   - callback: <#callback description#>
    public func mergeMetaData(valueCmd: String?,
                              value: [String: Any],
                              callback: ((NSError?)->())?) {
        if AUIRoomContext.shared.getArbiter(channelName: channelName)?.isArbiter() ?? false {
            let currentUserId = AUIRoomContext.shared.currentUserInfo.userId
            rtmMergeMetaData(publisherId: currentUserId, 
                             valueCmd: valueCmd,
                             value: value,
                             callback: callback)
            return
        }
        
        let payload = AUICollectionMessagePayload(type: .merge, 
                                                  dataCmd: valueCmd,
                                                  data: AUIAnyType(map: value))
        let message = AUICollectionMessage(channelName: channelName,
                                           messageType: AUIMessageType.normal,
                                           sceneKey: observeKey,
                                           uniqueId: UUID().uuidString,
                                           payload: payload)

        guard let jsonStr = encodeModelToJsonStr(message) else {
            callback?(AUICollectionOperationError.encodeToJsonStringFail.toNSError())
            return
        }
        let userId = AUIRoomContext.shared.getArbiter(channelName: channelName)?.lockOwnerId ?? ""
        rtmManager.publishAndWaitReceipt(userId: userId,
                                         channelName: channelName,
                                         message: jsonStr,
                                         uniqueId: message.uniqueId,
                                         completion: callback)
    }
    
    
    /// Add, mapCollection is equivalent to update metadata
    /// - Parameter value: <#value description#>
    public func addMetaData(valueCmd: String?,
                            value: [String: Any],
                            callback: ((NSError?)->())?) {
        if AUIRoomContext.shared.getArbiter(channelName: channelName)?.isArbiter() ?? false {
            let currentUserId = AUIRoomContext.shared.currentUserInfo.userId
            rtmSetMetaData(publisherId: currentUserId,
                           valueCmd: valueCmd,
                           value: value,
                           callback: callback)
            return
        }
        
        let payload = AUICollectionMessagePayload(type: .add,
                                                  dataCmd: valueCmd,
                                                  data: AUIAnyType(map: value))
        let message = AUICollectionMessage(channelName: channelName,
                                           messageType: AUIMessageType.normal,
                                           sceneKey: observeKey,
                                           uniqueId: UUID().uuidString,
                                           payload: payload)

        guard let jsonStr = encodeModelToJsonStr(message) else {
            callback?(AUICollectionOperationError.encodeToJsonStringFail.toNSError())
            return
        }
        let userId = AUIRoomContext.shared.getArbiter(channelName: channelName)?.lockOwnerId ?? ""
        rtmManager.publishAndWaitReceipt(userId: userId,
                                         channelName: channelName,
                                         message: jsonStr,
                                         uniqueId: message.uniqueId,
                                         completion: callback)
    }
    
    /// Remove, map collection is not supported
    /// - Parameters:
    ///   - valueCmd: <#value description#>
    ///   - callback: <#callback description#>
    public func removeMetaData(valueCmd: String?,
                               callback: ((NSError?)->())?) {
        callback?(AUICollectionOperationError.unsupportedAction.toNSError("map removeMetaData fail"))
    }
    
    public func calculateMetaData(valueCmd: String?,
                                  key: [String],
                                  value: Int,
                                  min: Int,
                                  max: Int,
                                  callback: ((NSError?)->())?) {
        if AUIRoomContext.shared.getArbiter(channelName: channelName)?.isArbiter() ?? false {
            let currentUserId = AUIRoomContext.shared.currentUserInfo.userId
            rtmCalculateMetaData(publisherId: currentUserId,
                                 valueCmd: valueCmd,
                                 key: key,
                                 value: AUICollectionCalcValue(value: value, min: min, max: max),
                                 callback: callback)
            return
        }
        
        let calcData = AUICollectionCalcData(key: key,
                                             value: AUICollectionCalcValue(value: value, min: min, max: max))
        let data: [String: Any] = encodeModel(calcData) ?? [:]
        let payload = AUICollectionMessagePayload(type: .calculate,
                                                  dataCmd: valueCmd,
                                                  data: AUIAnyType(map: data))
        let message = AUICollectionMessage(channelName: channelName,
                                           messageType: AUIMessageType.normal,
                                           sceneKey: observeKey,
                                           uniqueId: UUID().uuidString,
                                           payload: payload)

        guard let jsonStr = encodeModelToJsonStr(message) else {
            callback?(AUICollectionOperationError.encodeToJsonStringFail.toNSError())
            return
        }
        let userId = AUIRoomContext.shared.getArbiter(channelName: channelName)?.lockOwnerId ?? ""
        rtmManager.publishAndWaitReceipt(userId: userId,
                                         channelName: channelName,
                                         message: jsonStr,
                                         uniqueId: message.uniqueId,
                                         completion: callback)
    }
    
    /// Clean up, map collection is to delete the key.
    /// - Parameter callback: <#callback description#>
    public func cleanMetaData(callback: ((NSError?)->())?) {
        if AUIRoomContext.shared.getArbiter(channelName: channelName)?.isArbiter() ?? false {
            rtmCleanMetaData(callback: callback)
            return
        }
        
        let payload = AUICollectionMessagePayload(type: .clean, data: nil)
        let message = AUICollectionMessage(channelName: channelName,
                                           messageType: AUIMessageType.normal,
                                           sceneKey: observeKey,
                                           uniqueId: UUID().uuidString,
                                           payload: payload)
        
        guard let jsonStr = encodeModelToJsonStr(message) else {
            callback?(AUICollectionOperationError.encodeToJsonStringFail.toNSError())
            return
        }
        let userId = AUIRoomContext.shared.getArbiter(channelName: channelName)?.lockOwnerId ?? ""
        rtmManager.publishAndWaitReceipt(userId: userId,
                                         channelName: channelName,
                                         message: jsonStr,
                                         uniqueId: message.uniqueId,
                                         completion: callback)
    }
    
    
    public override func syncLocalMetaData() {
        guard retryMetadata, let value = encodeToJsonStr(currentMap) else {
            return
        }
        let observeKey = observeKey
        aui_collection_log("syncLocalMetaData[\(observeKey)] start")
        setBatchMetadata(value) { error in
            aui_collection_log("syncLocalMetaData[\(observeKey)] completion: \(error?.localizedDescription ?? "success")")
        }
    }
}


//MARK: AUIRtmAttributesProxyDelegate
extension AUIMapCollection {
    public override func onAttributesDidChanged(channelName: String, key: String, value: Any) {
        guard channelName == self.channelName, key == self.observeKey else {return}
        guard let map = value as? [String: Any] else {return}
        //If it is an arbitrator, do not update it, because it has been modified locally, otherwise the message received here may be old data, such as update1->update2->resp1->resp2, then the data of resp1 is older than update2, which will cause a short rollback on ui.
        
        if AUIRoomContext.shared.getArbiter(channelName: channelName)?.isArbiter() != true {
            currentMap = map
        }
        
        self.attributesDidChangedClosure?(channelName, observeKey, AUIAttributesModel(map: map))
    }
    
    public override func getLocalMetaData() -> AUIAttributesModel? {
        return AUIAttributesModel(map: currentMap)
    }
}

//MARK: override AUIRtmMessageProxyDelegate
extension AUIMapCollection {
    public override func onMessageReceive(publisher: String, channelName: String, message: String) {
        guard let collectionMessage: AUICollectionMessage = decodeModel(jsonStr: message),
              collectionMessage.sceneKey == observeKey else {
            return
        }
        aui_collection_log("onMessageReceive: \(message)")
        let uniqueId = collectionMessage.uniqueId
        let channelName = collectionMessage.channelName
        guard channelName == self.channelName else {return}
        if collectionMessage.messageType == .receipt {
            if let callback = rtmManager.receiptCallbackMap[uniqueId]?.closure {
                rtmManager.markReceiptFinished(uniqueId: uniqueId)
                let data = collectionMessage.payload.data?.toJsonObject() as? [String : Any] ?? [:]
                let error: AUICollectionError? = decodeModel(data)
                let code = error?.code ?? 0
                let reason = error?.reason ?? "success"
                if code == 0 {
                    callback(nil)
                } else if let err = AUICollectionOperationError(rawValue: code) {
                    callback(err.toNSError(reason))
                } else {
                    let err = NSError(domain: "AUICollection Error",
                                      code: code,
                                      userInfo: [ NSLocalizedDescriptionKey : "\(reason)"])
//                    callback(AUICollectionOperationError.recvErrorReceipt.toNSError("code: \(code), reason: \(reason)"))
                    callback(err)
                }
            }
            return
        }
        
        guard let updateType = collectionMessage.payload.type else {
            sendReceipt(publisher: publisher,
                        uniqueId: uniqueId,
                        error: AUICollectionOperationError.updateTypeNotFound.toNSError())
            return
        }
        
        let valueCmd = collectionMessage.payload.dataCmd
        var err: NSError? = nil
        let value = collectionMessage.payload.data?.toJsonObject() as? [String : Any]
        switch updateType {
        case .add:
            if let value = value {
                    rtmSetMetaData(publisherId: publisher,
                                   valueCmd: valueCmd,
                                   value: value) {[weak self] error in
                        self?.sendReceipt(publisher: publisher,
                                          uniqueId: uniqueId,
                                          error: error)
                    }
                return
            }
            err = AUICollectionOperationError.invalidPayloadType.toNSError()
        case .update:
            if let value = value {
                rtmUpdateMetaData(publisherId: publisher,
                                  valueCmd: valueCmd,
                                  value: value) {[weak self] error in
                    self?.sendReceipt(publisher: publisher,
                                      uniqueId: uniqueId,
                                      error: error)
                }
                return
            }
        case .merge:
            if let value = value {
                rtmMergeMetaData(publisherId: publisher,
                                 valueCmd: valueCmd,
                                 value: value) {[weak self] error in
                    self?.sendReceipt(publisher: publisher,
                                      uniqueId: uniqueId,
                                      error: error)
                }
                return
            }
            err = AUICollectionOperationError.invalidPayloadType.toNSError()
        case .clean:
            rtmCleanMetaData { [weak self] error in
                self?.sendReceipt(publisher: publisher,
                                  uniqueId: uniqueId,
                                  error: error)
            }
        case .remove:
            err = AUICollectionOperationError.unsupportedAction.toNSError("map remove")
            break
        case .calculate:
            if let value = value,
               let data: AUICollectionCalcData = decodeModel(value) {
                rtmCalculateMetaData(publisherId: publisher,
                                     valueCmd: valueCmd,
                                     key: data.key,
                                     value: data.value) {[weak self] error in
                    self?.sendReceipt(publisher: publisher,
                                      uniqueId: uniqueId,
                                      error: error)
                }
                return
            }
            err = AUICollectionOperationError.invalidPayloadType.toNSError()
        }
        
        guard let err = err else {return}
        sendReceipt(publisher: publisher,
                    uniqueId: uniqueId,
                    error: err)
    }
}
