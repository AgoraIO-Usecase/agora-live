//
//  ShowCreateLiveVC+Beauty.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2024/3/12.
//

import AGResourceManager

private let kFuLicUri = "beauty/FULib_lic"
private let kFuUri = "beauty/FULib"

private let kLoadingViewTag = 11223344
private let kDownloadingAlreadyErr = -100001


@discardableResult
func setupFuResource() -> Bool {
    let manager = AGResourceManager.shared
    guard let fuLicResource = manager.getResource(uri: kFuLicUri),
          manager.getStatus(resource: fuLicResource) == .downloaded,
          let fuResource = manager.getResource(uri: kFuUri),
          manager.getStatus(resource: fuResource) == .downloaded else {
        return false
    }
    
    let beLicPath = manager.getFolderPath(resource: fuLicResource) + "/authpack.h"
    let beResourcePath = manager.getFolderPath(resource: fuResource) + "/FULib"
    FUDynmicResourceConfig.shareInstance().licFilePath = beLicPath
    FUDynmicResourceConfig.shareInstance().resourceFolderPath = beResourcePath
    return true
}

extension ShowCreateLiveVC {
    func isBeautyDownloaded() -> Bool {
        if setupFuResource() {
            return true
        }
        
        if KeyCenter.DynamicResourceUrl?.isEmpty ?? true {
            return true
        }
        
        checkAndSetupBeautyPath {[weak self] err in
            self?.markProgressCompletion(err: err)
        }
        return false
    }
    
    func cancelBeautyResource() {
        let manager = AGResourceManager.shared
        
        if let res = manager.getResource(uri: kFuLicUri) {
            manager.cancelDownloadResource(resource: res)
        }
        
        if let res = manager.getResource(uri: kFuUri) {
            manager.cancelDownloadResource(resource: res)
        }
    }
    
    func checkAndSetupBeautyPath(completion: ((NSError?) -> Void)?) {
        if KeyCenter.DynamicResourceUrl?.isEmpty ?? true {
            completion?(nil)
            return
        }
        if let _ = view.viewWithTag(kLoadingViewTag) {
            completion?(NSError(domain: "download already", code: kDownloadingAlreadyErr))
            return
        }
        
        //setup fu effect path
        guard setupFuResource() else {
            let type = BeautyFactoryType.fu
            self.updateDownloadProgress(title: type.title, progress: 0)
            AGResourceManager.autoDownload(uris: [kFuLicUri, kFuUri]) {[weak self] progress in
                self?.updateDownloadProgress(title: type.title, progress: progress)
            } completion: { [weak self] err in
                guard let self = self else {return}
                self.markProgressCompletion(err: err)
                if let _ = err { return }
                setupFuResource()
                self.checkAndSetupBeautyPath(completion: completion)
            }
            return
        }
        
        completion?(nil)
    }
    
    private func updateDownloadProgress(title: String, progress: Double) {
        DispatchQueue.main.async {
            guard let view = self.view else {
                return
            }
            var progressView = view.viewWithTag(kLoadingViewTag) as? ShowDownlodingProgressView
            if progressView == nil {
                let size = CGSize(width: 250, height: 60)
                let frame = CGRect(x: (view.frame.width - size.width) / 2,
                                   y: (view.frame.height - size.height) / 2,
                                   width: size.width,
                                   height: size.height)
                let _progressView = ShowDownlodingProgressView(frame: frame)
                _progressView.tag = kLoadingViewTag
                view.addSubview(_progressView)
                progressView = _progressView
            }
            progressView?.setProgress(title, Int(progress * 100))
        }
    }

    private func markProgressCompletion(err: NSError?) {
        if let err = err {
            //already downloading
            if err.code == kDownloadingAlreadyErr { return }
            
            ToastView.show(text: err.localizedDescription)
        }
        guard let progressView = view.viewWithTag(kLoadingViewTag) else {
            return
        }
        
        progressView.removeFromSuperview()
    }
}

