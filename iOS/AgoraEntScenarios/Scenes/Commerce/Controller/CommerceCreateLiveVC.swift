//
//  ShowLivePreViewVC.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/2.
//

import UIKit
import AgoraRtcKit
import SVProgressHUD

class CommerceCreateLiveVC: UIViewController {

    private var createView: CommerceCreateLiveView!
    private var localView: UIView!
        
    private lazy var beautyVC = CommerceBeautySettingVC()
    
    deinit {
        commerceLogger.info("deinit-- ShowCreateLiveVC")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setUpUI()
        configNaviBar()
        
        if let e = CommerceAgoraKitManager.shared.engine {
            BeautyManager.shareManager.configBeautyAPIWithRtcEngine(engine: e)
        }
        CommerceAgoraKitManager.shared.startPreview(canvasView: self.localView)
        CommerceNetStateSelectViewController.showInViewController(self)
    }
    
    func configNaviBar() {
        self.navigationController?.isNavigationBarHidden = true
        
        let titleLabel = UILabel()
        view.addSubview(titleLabel)
        titleLabel.text = "navi_title_show_live".commerce_localized
        titleLabel.textColor = .commerce_main_text
        titleLabel.font = .commerce_navi_title
        titleLabel.snp.makeConstraints { make in
            make.top.equalTo(56)
            make.centerX.equalToSuperview()
        }
        
        let cancelButton = UIButton(type: .custom)
        cancelButton.setImage(UIImage.commerce_sceneImage(name: "show_preview_cancel"), for: .normal)
        cancelButton.addTarget(self, action: #selector(didClickCancelButton), for: .touchUpInside)
        view.addSubview(cancelButton)
        cancelButton.snp.makeConstraints { make in
            make.left.equalTo(15)
            make.centerY.equalTo(titleLabel)
        }
    }
    
    private func setUpUI() {
        localView = UIView()
        view.addSubview(localView)
        localView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    
        createView = CommerceCreateLiveView()
        createView.delegate = self
        view.addSubview(createView)
        createView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        beautyVC.dismissed = { [weak self] in
            self?.createView.hideBottomViews = false
        }
        
        CommerceBeautyFaceVC.beautyData.forEach({
            BeautyManager.shareManager.setBeauty(path: $0.path,
                                                     key: $0.key,
                                                     value: $0.value)
        })
    }
    
    private func showPreset() {
        if AppContext.shared.isDebugMode {
            let vc = ShowDebugSettingVC()
            vc.isBroadcastor = true
            self.navigationController?.pushViewController(vc, animated: true)
        } else {
            CommerceNetStateSelectViewController.showInViewController(self)
        }
    }
    
    @objc func didClickCancelButton(){
        BeautyManager.shareManager.destroy()
        CommerceAgoraKitManager.shared.cleanCapture()
        CommerceBeautyFaceVC.resetData()
        dismiss(animated: true)
    }
}

extension CommerceCreateLiveVC: CommerceCreateLiveViewDelegate {
    
    func onClickSettingBtnAction() {
        showPreset()
    }
    
    func onClickCameraBtnAction() {
        CommerceAgoraKitManager.shared.switchCamera()
    }
    
    func onClickBeautyBtnAction() {
        createView.hideBottomViews = true
        present(beautyVC, animated: true)
    }
    
    func onClickStartBtnAction() {
        guard let roomName = createView.roomName, roomName.count > 0 else {
            ToastView.show(text: "create_room_name_can_not_empty".commerce_localized)
            return
        }
        
        guard  let roomName = createView.roomName, roomName.count <= 16 else {
            ToastView.show(text: "create_room_name_too_long".commerce_localized)
            return
        }
        
        let roomId = createView.roomNo
        SVProgressHUD.show()
        AppContext.commerceServiceImp(createView.roomNo)?.createRoom(roomName: roomName,
                                                                roomId: roomId,
                                                                thumbnailId: createView.roomBg) { [weak self] err, detailModel in
            SVProgressHUD.dismiss()
            if err != nil {
                ToastView.show(text: err!.localizedDescription)
            }
            guard let wSelf = self, let detailModel = detailModel else { return }
            let liveVC = CommerceLivePagesViewController()
            liveVC.roomList = [detailModel]
            liveVC.focusIndex = liveVC.roomList?.firstIndex(where: { $0.roomId == roomId }) ?? 0
            
            wSelf.navigationController?.pushViewController(liveVC, animated: false)
        }
    }
}
