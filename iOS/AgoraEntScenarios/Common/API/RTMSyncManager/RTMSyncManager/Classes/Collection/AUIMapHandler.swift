//
//  AUIMapHandler.swift
//  AUIKitCore
//
//  Created by wushengtao on 2024/1/16.
//

import Foundation

func mergeMap(origMap: [String: Any], newMap: [String: Any]) -> [String: Any] {
    var _origMap = origMap
    newMap.forEach { (k, v) in
        if let dic = v as? [String: Any] {
            let origDic: [String: Any] = _origMap[k] as? [String: Any] ?? [:]
            let newDic = mergeMap(origMap: origDic, newMap: dic)
            _origMap[k] = newDic
        } else {
            //TODO: array ?
            _origMap[k] = v
        }
    }
    return _origMap
}

func calculateMap(origMap: [String: Any],
                  key: [String],
                  value: Int,
                  min: Int,
                  max: Int) throws -> [String: Any] {
    var _origMap = origMap
    if key.count > 1 {
        let curKey = key.first ?? ""
        let subKey = Array(key.suffix(from: 1))
        
        guard let subValue = _origMap[curKey] as? [String: Any]  else {
            throw AUICollectionOperationError.calculateMapFail.toNSError()
        }
        var newMap: [String: Any] = [:]
        do {
            newMap = try calculateMap(origMap: subValue,
                                      key: subKey,
                                      value: value,
                                      min: min,
                                      max: max)
        } catch {
            throw error
        }
        _origMap[curKey] = newMap
        return _origMap
    }
    guard let curKey = key.first, let subValue = _origMap[curKey] as? Int else { 
        throw AUICollectionOperationError.calculateMapFail.toNSError()
    }
    let curValue = subValue + value
    guard curValue <= max, curValue >= min else {
        aui_info("calculateMap out of range")
        throw AUICollectionOperationError.calculateMapOutOfRange.toNSError()
    }
    _origMap[curKey] = curValue
    
    return _origMap
}


/// Filter out the index hit by array according to the filter condition. If the filter is empty, all will be selected by default.
/// - Parameters:
///   - array: <#array description#>
///   - filter: <#filter description#>
/// - Returns: <#description#>
func getItemIndexes(array: [[String: Any]], filter: [[String: Any]]?) -> [Int]? {
    guard let filter = filter, filter.count > 0 else {
        let indexes = Array(array.indices)
        return indexes.isEmpty ? nil : indexes
    }
    
    func isMatchFilter(key: String, itemValue: [String: Any], filterValue: Any?)-> Bool {
        let valueV = itemValue[key]
        //only filter String/Bool/Int
        if let valueV = valueV as? String, let filterV = filterValue as? String {
            if valueV == filterV {
                return true
            }
        } else if let valueV = valueV as? Bool, let filterV = filterValue as? Bool {
            if valueV == filterV {
                return true
            }
        } else if let valueV = valueV as? Int, let filterV = filterValue as? Int {
            if valueV == filterV {
                return true
            }
        } else if let valueV = valueV as? [String: Any], 
                    let filterV = filterValue as? [String: Any],
                    filterV.keys.count == 1,
                    let filterVKey = filterV.keys.first {
            //next level match again
            return isMatchFilter(key: filterVKey, itemValue: valueV, filterValue: filterV[filterVKey])
        }
        
        return false
    }
    
    var indexes: [Int] = []
    for (i, value) in array.enumerated() {
        //For filter, as long as any filterItem is satisfied, all conditions need to be met.
        for filterItem in filter {
            var matchCount = 0
            for (k, v) in filterItem {
                if isMatchFilter(key: k, itemValue: value, filterValue: v) {
                    matchCount += 1
                }
            }
            if matchCount == filterItem.keys.count {
                indexes.append(i)
                break
            }
        }
    }
    return indexes.isEmpty ? nil : indexes
}
