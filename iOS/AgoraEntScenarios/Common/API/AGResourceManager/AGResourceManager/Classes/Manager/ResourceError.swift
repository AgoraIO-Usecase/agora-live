//
//  ResourceError.swift
//  AGResourceManager
//
//  Created by wushengtao on 2024/3/8.
//

import Foundation


enum ResourceError: Int {
    case unknown = 1000  //Unknown error
    case urlInvalide  //Url is abnormal
    case resourceNotFound  //Unable to find the file locally
    case resourceDownloadingAlready    //Downloading
    case md5Mismatch   //The local file md5 does not match
    
    func toNSError(errorStr: String) -> NSError {
        return NSError(domain: "ResourceManager",
                       code: -self.rawValue,
                       userInfo: [NSLocalizedDescriptionKey: errorStr])
    }
    
    static func unknownError() -> NSError {
        return ResourceError.unknown.toNSError(errorStr: "unknown error")
    }
    
    static func urlInvalidError(url: String) -> NSError {
        return ResourceError.urlInvalide.toNSError(errorStr: "download url(\(url) invalide")
    }
    
    static func resourceNotFoundError(url: String) -> NSError {
        return ResourceError.urlInvalide.toNSError(errorStr: "resource[\(url)] not found")
    }
    
    static func resourceDownloadingAlreadyError(url: String) -> NSError {
        return ResourceError.resourceDownloadingAlready.toNSError(errorStr: "downloading url(\(url) already")
    }
    
    static func md5MismatchError(msg: String) -> NSError {
        return ResourceError.md5Mismatch.toNSError(errorStr: "md5 missmatch \(msg)")
    }
}
