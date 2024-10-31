//
//  IAGDownloadManager.swift
//  AGResourceManager
//
//  Created by wushengtao on 2024/3/8.
//

import Foundation

public protocol IAGDownloadManager: NSObjectProtocol {
    
    
    /// Cancel the download
    /// - Parameter url: <#url description#>
    func cancelDownloadFile(withURL url: URL)
    
    /// Download the file and check md5
    /// - Parameters:
    ///   - url: <#url description#>
    ///   - md5: <#md5 description#>
    ///   - destinationPath: <#destinationPath description#>
    ///   - progressHandler: <#progressHandler description#>
    ///   - completionHandler: <#completionHandler description#>
    func startDownloadFile(withURL url: URL,
                           md5: String?,
                           destinationPath: String,
                           progressHandler: @escaping (Double) -> Void,
                           completionHandler: @escaping (URL?, NSError?) -> Void)
    
    
    /// Download the zip file and unzip it to the specified directory after downloading.
    /// - Parameters:
    ///   - url: <#url description#>
    ///   - md5: <#md5 description#>
    ///   - destinationFolderPath: <#destinationFolderPath description#>
    ///   - progressHandler: <#progressHandler description#>
    ///   - completionHandler: <#completionHandler description#>
    func startDownloadZip(withURL url: URL,
                          md5: String,
                          destinationFolderPath: String,
                          progressHandler: @escaping (Double) -> Void,
                          completionHandler: @escaping (URL?, NSError?) -> Void)
    
    
    
    /// Check whether the resources need to be downloaded
    /// - Parameters:
    ///   - url: <#url description#>
    ///   - completionHandler: <#completionHandler description#>
    func checkResource(destinationPath: String, 
                       md5: String?,
                       completionHandler: @escaping (NSError?) -> Void)
    
    /// Cancel the download task
    /// - Parameter url: <#url description#>
    func cancelDownload(forURL url: URL)
    
    
    /// Is it being downloaded?
    /// - Parameter url: <#url description#>
    /// - Returns: <#description#>
    func isDownloading(forUrl url: URL) -> Bool
}
