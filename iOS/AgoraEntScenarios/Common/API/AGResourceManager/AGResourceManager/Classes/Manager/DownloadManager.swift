//
//  DownloadManager.swift
//  AGResourceManager
//
//  Created by wushengtao on 2024/3/6.
//

import Foundation
import CommonCrypto

public func asyncToMainThread(closure: @escaping (() -> Void)) {
    if Thread.isMainThread {
        closure()
        return
    }
    
    DispatchQueue.main.async {
        closure()
    }
}

class URLSessionDownloader: NSObject {
    private(set) var task: URLSessionDataTask
    private(set) var temporaryPath: String
    private(set) var destinationPath: String
    private var progressHandler: ((Double) -> Void)?
    private var completionHandler: ((URL?, Error?) -> Void)?
    private var resumeRange: UInt64 = 0
    
    private lazy var fileHandle: FileHandle = {
        let fm = FileManager.default
        if fm.fileExists(atPath: temporaryPath) == false {
            fm.createFile(atPath: temporaryPath, contents: nil, attributes: nil)
        }
        let hander = FileHandle(forWritingAtPath: temporaryPath)!
        hander.seekToEndOfFile()
        return hander
    }()
    
    deinit {
        closeFile()
        cancelTask()
    }
    
    required init(task: URLSessionDataTask,
                  currentLength: UInt64,
                  temporaryPath: String,
                  destinationPath: String,
                  progressHandler: @escaping (Double) -> Void,
                  completionHandler: @escaping (URL?, Error?) -> Void) {
        self.task = task
        self.resumeRange = currentLength
        self.temporaryPath = temporaryPath
        self.destinationPath = destinationPath
        self.progressHandler = progressHandler
        self.completionHandler = completionHandler
        super.init()
    }
    
    func write(_ data: Data, countOfBytesExpectedToReceive: Int64) {
        fileHandle.write(data)
        
        // Update progress and other operations
        let totolDownloaded = Double(fileHandle.offsetInFile)
        let resumeRangeDouble = Double(resumeRange)
        let totalExpected = Double(countOfBytesExpectedToReceive)
        let progress = totolDownloaded / (totalExpected + resumeRangeDouble)
        progressHandler?(progress)
    }
    
    func cancelTask() {
        task.cancel()
    }
    
    func closeFile() {
        fileHandle.closeFile()
    }
    
    func markCompletion(error: Error?) {
        asyncToMainThread {
            if let error = error {
                aui_error("download fail: \(error.localizedDescription)")
                self.completionHandler?(nil, error)
            } else {
                self.completionHandler?(URL(fileURLWithPath: self.temporaryPath), nil)
            }
        }
        
    }
}

public class DownloadManager: NSObject {
    private var session: URLSession?
    private var downloaderMap: [URL: URLSessionDownloader] = [:]
    private var unzipOpMap: [URL: UnzipManager] = [:]
    
    override init() {
        super.init()
        let configuration = URLSessionConfiguration.default
        configuration.httpMaximumConnectionsPerHost = 3
        self.session = URLSession(configuration: configuration, delegate: self, delegateQueue: nil)
        if let urlCache = URLSession.shared.configuration.urlCache {
            // Delete all caches
            urlCache.removeAllCachedResponses()
            // Or delete the cache according to the specific URL request
            // urlCache.removeCachedResponse(for: URLRequest(url: yourURL))
        }
    }
}

extension DownloadManager: URLSessionDataDelegate {
    public func urlSession(_ session: URLSession,
                           dataTask: URLSessionDataTask,
                           didReceive response: URLResponse,
                           completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
        aui_info("----开始----\(response.expectedContentLength)---\(response.mimeType ?? "")")
//        totalLength = Int(response.expectedContentLength) + currentLength
        completionHandler(.allow)
        //completionHandler(.cancel)
    }
        
    public func urlSession(_ session: URLSession,
                           dataTask: URLSessionDataTask,
                           didReceive data: Data) {
        guard let url = dataTask.currentRequest?.url, let downloader = downloaderMap[url] else {
            return
        }
        downloader.write(data, countOfBytesExpectedToReceive: dataTask.countOfBytesExpectedToReceive)
    }
    
    public func urlSession(_ session: URLSession,
                           task: URLSessionTask,
                           didCompleteWithError error: Error?) {
        guard let url = task.currentRequest?.url, let downloader = downloaderMap[url] else {
            return
        }
        
        downloader.markCompletion(error: error)
    }
}

@available(iOS 13.0, *)
extension DownloadManager: IAGDownloadManager {
    public func checkResource(destinationPath: String, 
                              md5: String?,
                              completionHandler: @escaping (NSError?) -> Void) {
        let fm = FileManager.default
        
        //If it is a folder, the folder exists and the zip does not exist. It is temporarily used to indicate that the md5 file is decompressed correctly and completed.
        if calculateTotalSize(destinationPath) > 0 {
            asyncToMainThread {
                completionHandler(nil)
            }
            return
        }
        
        //Then check whether it is a file. If it is a file, check whether it exists first.
        if !fm.fileExists(atPath: destinationPath) {
            asyncToMainThread {
                completionHandler(ResourceError.resourceNotFoundError(url: destinationPath))
            }
            return
        }
        
        //The file exists, check md5
        guard let md5 = md5 else {
            aui_info("startDownload completion, file exist & without md5")
            asyncToMainThread {
                completionHandler(nil)
            }
            return
        }
        let queue = DispatchQueue.global(qos: .background)
        queue.async {
            let tempFileMD5 = calculateMD5(forFileAt: URL(fileURLWithPath: destinationPath)) ?? ""
            aui_info("check md5: '\(tempFileMD5)'-'\(md5)' \(destinationPath)")
            if md5 == tempFileMD5 {
                //Md5 is consistent, complete directly
                asyncToMainThread {
                    completionHandler(nil)
                }
                return
            }
            
            asyncToMainThread {
                completionHandler(ResourceError.md5MismatchError(msg: destinationPath))
            }
        }
    }
    
    public func cancelDownloadFile(withURL url: URL) {
        guard let downloader = downloaderMap[url] else {return}
        downloaderMap[url] = nil
    }
    
    public func startDownloadFile(withURL url: URL,
                                  md5: String?,
                                  destinationPath: String,
                                  progressHandler: @escaping (Double) -> Void,
                                  completionHandler: @escaping (URL?, NSError?) -> Void) {
        //If it is being downloaded, an error will be returned. Will one task support multiple progressHandler/completionHandler in the future?
        if let _ = self.downloaderMap[url] {
            completionHandler(nil, ResourceError.resourceDownloadingAlreadyError(url: url.absoluteString))
            return
        }
        
        let fm = FileManager.default
        if fm.fileExists(atPath: destinationPath) {
            checkResource(destinationPath: destinationPath, 
                          md5: md5) { err in
                guard let err = err else {
                    completionHandler(URL(string: destinationPath), nil)
                    return
                }
                
                if abs(err.code) == ResourceError.md5Mismatch.rawValue {
                    do {
//                        The file md5 is different, remove it (whether to wait for the download to be completed before removing it)
                        try fm.removeItem(atPath: destinationPath)
                    } catch {
                        aui_error("remove exist file fail: \(destinationPath)")
                    }
                }
                self.startDownloadFile(withURL: url,
                                       md5: md5,
                                       destinationPath: destinationPath,
                                       progressHandler: progressHandler,
                                       completionHandler: completionHandler)
            }
            return
        }
        
        let temporaryDirectoryURL = FileManager.default.temporaryDirectory
        let temporaryPath = "\(temporaryDirectoryURL.path)/\(md5 ?? NSUUID().uuidString)"
        let currentLength = fileSize(atPath: temporaryPath)
        
        var request = URLRequest(url: url)
        //Temporary file exists, calculate range breakpoint continuation
        if currentLength > 0 {
            let range = "bytes=\(currentLength)-"
            request.setValue(range, forHTTPHeaderField: "Range")
            aui_info("resume download from range[\(currentLength)] url: \(url.absoluteString)")
        }
        let task = session!.dataTask(with: request)
        task.resume()
        
        let downloader = URLSessionDownloader(task: task,
                                              currentLength: currentLength,
                                              temporaryPath: temporaryPath,
                                              destinationPath: destinationPath) { progress in
            
            aui_debug("download progress: \(Int(progress * 100))% url: \(url.absoluteString)")
            progressHandler(progress)
        } completionHandler: { [weak self] localUrl, err in
            guard let self = self else { return }
            self.downloaderMap.removeValue(forKey: url)
            let localPath = localUrl?.path ?? ""
            if let err = err {
                aui_error("post download error: \(err.localizedDescription) localPath: \(localPath)")
                completionHandler(nil, err as NSError)
                return
            }
            
            //Post-processing, compare with md5, move the temporary directory to the correct directory
            self.postDownloadProcessing(tempFilePath: temporaryPath,
                                        targetFilePath: destinationPath,
                                        md5: md5) { err in
                if let err = err {
                    aui_error("post download error: \(err.localizedDescription) localPath: \(localPath)")
                    completionHandler(nil, err as NSError)
                } else {
                    aui_info("post download success, localPath: \(localPath)")
                    completionHandler(URL(string: destinationPath), nil)
                }
            }
        }

        downloaderMap[url] = downloader
    }
    
    public func startDownloadZip(withURL url: URL,
                                 md5: String,
                                 destinationFolderPath: String,
                                 progressHandler: @escaping (Double) -> Void,
                                 completionHandler: @escaping (URL?, NSError?) -> Void) {
        let temporaryDirectoryURL = FileManager.default.temporaryDirectory
        let destinationZipPath = "\(temporaryDirectoryURL.path)/\(md5).zip"
        
        //The folder exists and the zip does not exist, which is temporarily used to indicate that the md5 file is decompressed correctly and completed.
        if calculateTotalSize(destinationFolderPath) > 0,
           !FileManager.default.fileExists(atPath: destinationZipPath) {
            
            completionHandler(URL(string: destinationFolderPath), nil)
            return
        }
        
        //TODO: The progress is temporarily distributed according to 80% download and 20% decompression.
        startDownloadFile(withURL: url,
                          md5: md5,
                          destinationPath: destinationZipPath) { percent in
            progressHandler(percent * 0.8)
        } completionHandler: {[weak self] localUrl, err in
            guard let self = self else { return }
            if let err = err {
                completionHandler(nil, err)
                return
            }
            
            let manager = UnzipManager()
            self.unzipOpMap[url] = manager
            let queue = DispatchQueue.global(qos: .default)
            queue.async {
                //Clean up the content of the target directory first.
                cleanDirectory(atPath: destinationFolderPath)
                //Decompress to the target path first to prevent abnormal interruptions such as crash in the middle of decompression.
                let tempFolderPath = "\(destinationFolderPath)_temp"
                try? FileManager.default.removeItem(atPath: tempFolderPath)
                try? FileManager.default.moveItem(atPath: destinationFolderPath, toPath: tempFolderPath)
                
                let date = Date()
                let ret = manager.unzipFile(zipFilePath: destinationZipPath,
                                  destination: tempFolderPath) { percent in
                    aui_debug("unzip progress: \(percent) file: \(destinationFolderPath)")
                    progressHandler(percent * 0.2 + 0.8)
                }
                
                aui_benchmark("file: \(destinationFolderPath) unzip completion", cost: -date.timeIntervalSinceNow)
                aui_info("unzip comletion[\(ret)] folderPath: \(destinationFolderPath)")
                let err = ret ? nil : NSError(domain: "unzip fail", code: -1)
                if ret {
                    try? FileManager.default.moveItem(atPath: tempFolderPath, toPath: destinationFolderPath)
                    //Unzip and remove the zip file
                    try? FileManager.default.removeItem(atPath: destinationZipPath)
                }
                asyncToMainThread {
                    self.unzipOpMap.removeValue(forKey: url)
                    completionHandler(URL(string: destinationFolderPath), err)
                }
            }
        }
    }
    
    public func cancelDownload(forURL url: URL) {
        if let downloader = downloaderMap[url] {
            downloader.cancelTask()
            downloaderMap.removeValue(forKey: url)
        }
        
        if let unzipOp = unzipOpMap[url] {
            unzipOp.cancelUnzip()
            unzipOpMap.removeValue(forKey: url)
        }
    }
    public func isDownloading(forUrl url: URL) -> Bool {
        return downloaderMap[url] == nil ? false : true
    }
}

@available(iOS 13.0, *)
extension DownloadManager {
    private func postDownloadProcessing(tempFilePath: String,
                                        targetFilePath: String,
                                        md5: String?,
                                        completion: @escaping (Error?) -> Void) {
        let queue = DispatchQueue.global(qos: .background)
        queue.async {
            let fileManager = FileManager.default
            
            do {
                // Calculate the MD5 of temporary files
                var isValideFile = true
                if let md5 = md5 {
                    let tempFileMD5 = calculateMD5(forFileAt: URL(fileURLWithPath: tempFilePath)) ?? ""
                    aui_info("check md5: '\(tempFileMD5)'-'\(md5)' \(tempFilePath)")
                    isValideFile = tempFileMD5 == md5 ? true : false
                }
                
                // Check whether MD5 is correct
                if isValideFile {
                    //The temporary path and the target path are inconsistent.
                    if tempFilePath != targetFilePath {
                        // Delete the existing files on the target path
                        if fileManager.fileExists(atPath: targetFilePath) {
                            try fileManager.removeItem(atPath: targetFilePath)
                        }
                        
                        // Move temporary files to the target path
                        try fileManager.moveItem(atPath: tempFilePath, toPath: targetFilePath)
                    }
                    
                    // The file processing is completed and the successful result is returned.
                    asyncToMainThread {
                        completion(nil)
                    }
                } else {
                    try fileManager.removeItem(atPath: tempFilePath)
                    // MD5 mismatches and returns an error result
                    asyncToMainThread {
                        completion(ResourceError.md5MismatchError(msg: targetFilePath))
                    }
                }
            } catch {
                try? fileManager.removeItem(atPath: tempFilePath)
                // An error occurred during the file processing process, and the error result was returned.
                asyncToMainThread {
                    completion(error)
                }
            }
        }
    }
}
