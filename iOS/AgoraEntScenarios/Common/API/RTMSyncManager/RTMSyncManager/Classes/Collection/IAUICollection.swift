//
//  IAUICollection.swift
//  AUIKitCore
//
//  Created by wushengtao on 2024/1/4.
//

import Foundation

public typealias AUICollectionGetClosure = (NSError?, Any?)-> Void


//(publisher uid, valueCmd, new value of item) -> value[new value of edit item]
public typealias AUICollectionValueWillChangeClosure = (String, String?, [String: Any]) -> [String: Any]?

//(publisher uid, valueCmd, new value)
public typealias AUICollectionAddClosure = (String, String?, [String: Any]) -> NSError?

//(publisher uid, valueCmd, new value, old value of item)
public typealias AUICollectionUpdateClosure = (String, String?, [String: Any], [String: Any]) -> NSError?

//(publisher uid, valueCmd, oldValue)
public typealias AUICollectionRemoveClosure = (String, String?, [String: Any]) -> NSError?

//(publisher uid, valueCmd, old value of item, keys, update value, min, max)
public typealias AUICollectionCalculateClosure = (String, String?, [String: Any], [String], Int, Int, Int) -> NSError?

//(channelName, key, valueCmd, value[will set metadata])->value[can set metadata]
public typealias AUICollectionAttributesWillSetClosure = (String, String, String?, AUIAttributesModel) -> AUIAttributesModel

//(channelName, key, value)
public typealias AUICollectionAttributesDidChangedClosure = (String, String, AUIAttributesModel) -> Void

@objc public class AUIAttributesModel: NSObject {
    private var attributes: Any?
    public required init(list: [[String: Any]]) {
        self.attributes = list
        super.init()
    }
    public required init(map: [String: Any]) {
        self.attributes = map
        super.init()
    }
    
    public func getMap() -> [String: Any]? {
        return attributes as? [String: Any]
    }
    
    public func getList() -> [[String: Any]]? {
        return attributes as? [[String: Any]]
    }
}

@objc public protocol IAUICollection: NSObjectProtocol {
    
    init(channelName: String, observeKey: String, rtmManager: AUIRtmManager) 
    
    /// The corresponding node object will be updated to ask whether it needs to be added or deleted locally (for example, updating a node needs to update the latest time again)
    /// - Parameter callback: <#callback description#>
    @objc optional func subsceibeValueWillChange(callback: AUICollectionValueWillChangeClosure?)
    
    /// Subscribe to the event that is about to add a new node
    /// - Parameter callback: <#callback description#>
    @objc optional func subscribeWillAdd(callback: AUICollectionAddClosure?)
    
    /// Subscribe to the event that is about to replace a node
    /// - Parameter callback: <#callback description#>
    @objc optional func subscribeWillUpdate(callback: AUICollectionUpdateClosure?)
    
    /// Subscribe to the event callback that is about to merge a node
    /// - Parameter callback: <#callback description#>
    @objc optional func subscribeWillMerge(callback: AUICollectionUpdateClosure?)
    
    /// Subscribe to the event callback that is about to delete a node
    /// - Parameter callback: <#callback description#>
    @objc optional func subscribeWillRemove(callback: AUICollectionRemoveClosure?)
    
    /// Subscribe to the event callback that is about to calculate a node
    /// - Parameter callback: <#callback description#>
    @objc optional func subscribeWillCalculate(callback: AUICollectionCalculateClosure?)
    
    /// It is about to be written to meta data. Does the upper layer need to be modified?
    /// - Parameter callback: <#callback description#>
    func subscribeAttributesWillSet(callback: AUICollectionAttributesWillSetClosure?)
    
    /// Changes in the metadata received
    /// - Parameter callback: <#callback description#>
    func subscribeAttributesDidChanged(callback: AUICollectionAttributesDidChangedClosure?)
    
    /// Query all the contents of the current scene node
    /// - Parameter callback: <#callback description#>
    func getMetaData(callback: AUICollectionGetClosure?)
    
    /// Obtain local metadata, the arbitrator is the local cache data (may be updated than the remote data), and the audience is the real remote data.
    /// - Parameter attributes: <#attributes description#>
    func getLocalMetaData() -> AUIAttributesModel?
    
    
    /// Update local to remote
    func syncLocalMetaData()
}


@objc public protocol IAUIMapCollection: IAUICollection {
    
    /// Add nodes
    /// - Parameters:
    ///   - valueCmd: <#valueCmd description#>
    ///   - value: <#value description#>
    ///   - callback: <#callback description#>
    func addMetaData(valueCmd: String?,
                     value: [String: Any],
                     callback: ((NSError?)->())?)
    
    /// Update node
    /// - Parameters:
    ///   - valueCmd: Command type
    ///   - value: <#value description#>
    ///   - callback: <#callback description#>
    func updateMetaData(valueCmd: String?,
                        value: [String: Any],
                        callback: ((NSError?)->())?)
    
    /// Merge nodes
    /// - Parameters:
    ///   - valueCmd: <#valueCmd description#>
    ///   - value: <#value description#>
    ///   - callback: <#callback description#>
    func mergeMetaData(valueCmd: String?,
                       value: [String: Any],
                       callback: ((NSError?)->())?)
    
    /// Remove
    /// - Parameters:
    ///   - valueCmd: <#value description#>
    ///   - callback: <#callback description#>
    func removeMetaData(valueCmd: String?,
                        callback: ((NSError?)->())?)
    
    /// Increase/decrease nodes (nodes must be Int)
    /// - Parameters:
    ///   - valueCmd: <#valueCmd description#>
    ///   - key: <#key description#>
    ///   - value: <#value description#>
    ///   - min: <#min description#>
    ///   - max: <#max description#>
    ///   - callback: <#callback description#>
    func calculateMetaData(valueCmd: String?,
                           key: [String],
                           value: Int,
                           min: Int,
                           max: Int,
                           callback: ((NSError?)->())?)
    
    /// Remove the key corresponding to the whole collection
    /// - Parameter callback: <#callback description#>
    func cleanMetaData(callback: ((NSError?)->())?)
}


@objc public protocol IAUIListCollection: IAUICollection {
    
    /// Add nodes
    /// - Parameters:
    ///   - valueCmd: <#valueCmd description#>
    ///   - value: <#value description#>
    ///   - filter: If the original data meets the filter, the addition fails. If it is nil, it will be added unconditionally.
    ///   - callback: <#callback description#>
    func addMetaData(valueCmd: String?,
                     value: [String: Any],
                     filter: [[String: Any]]?,
                     callback: ((NSError?)->())?)
    
    /// Update nodes
    /// - Parameters:
    /// - valueCmd: Command type
    /// - value: <#value description#>
    /// - filter: If the raw data meets the filter, it will be updated successfully, and if it is nil, it will be updated all.
    /// - callback: <#callback description#>
    func updateMetaData(valueCmd: String?,
                        value: [String: Any],
                        filter: [[String: Any]]?,
                        callback: ((NSError?)->())?)
    
    /// Merge nodes
    /// - Parameters:
    /// - valueCmd: <#valueCmd description#>
    /// - value: <#value description#>
    /// - filter: If the original data meets the filter, it will be merged successfully, and if it is nil, it will merge all
    /// - callback: <#callback description#>
    func mergeMetaData(valueCmd: String?,
                       value: [String: Any],
                       filter: [[String: Any]]?,
                       callback: ((NSError?)->())?)
    
    /// Remove
    /// - Parameters:
    ///   - valueCmd: <#value description#>
    ///   - filter: <#value description#>
    ///   - callback: <#callback description#>
    func removeMetaData(valueCmd: String?,
                        filter: [[String: Any]]?,
                        callback: ((NSError?)->())?)
    
    /// Increase/decrease nodes (nodes must be Int)
    /// - Parameters:
    ///   - valueCmd: <#valueCmd description#>
    ///   - key: <#key description#>
    ///   - value: <#value description#>
    ///   - min: <#min description#>
    ///   - max: <#max description#>
    ///   - filter: <#filter description#>
    ///   - callback: <#callback description#>
    func calculateMetaData(valueCmd: String?,
                           key: [String],
                           value: Int,
                           min: Int,
                           max: Int,
                           filter: [[String: Any]]?,
                           callback: ((NSError?)->())?)
    
    /// Remove the key corresponding to the whole collection
    /// - Parameter callback: <#callback description#>
    func cleanMetaData(callback: ((NSError?)->())?)
}
