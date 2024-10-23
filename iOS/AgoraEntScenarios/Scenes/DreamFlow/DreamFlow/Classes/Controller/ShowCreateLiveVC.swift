//
//  ShowLivePreViewVC.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/2.
//

import UIKit
import AgoraRtcKit
import SVProgressHUD
import AgoraCommon

class ShowCreateLiveVC: UIViewController {

    private var createView: ShowCreateLiveView!
    private var localView: UIView!
            
    deinit {
        ShowLogger.info("deinit-- ShowCreateLiveVC")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setUpUI()
        configNaviBar()

        ShowAgoraKitManager.shared.resetBroadcasterProfile()
        ShowAgoraKitManager.shared.setupLocalVideo(canvasView: self.localView)
        ShowAgoraKitManager.shared.startPreview(canvasView: self.localView)
    }
    
    func configNaviBar() {
        self.navigationController?.isNavigationBarHidden = true
        
        let titleLabel = UILabel()
        view.addSubview(titleLabel)
        titleLabel.text = "navi_title_show_live".show_localized
        titleLabel.textColor = .show_main_text
        titleLabel.font = .show_navi_title
        titleLabel.snp.makeConstraints { make in
            make.top.equalTo(56)
            make.centerX.equalToSuperview()
        }
        
        let cancelButton = UIButton(type: .custom)
        cancelButton.setImage(UIImage.show_sceneImage(name: "show_preview_cancel"), for: .normal)
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
    
        createView = ShowCreateLiveView()
        createView.delegate = self
        view.addSubview(createView)
        createView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    }
    
    private func showPreset() {
        if AppContext.shared.isDebugMode {
            
        } else {
            ShowNetStateSelectViewController.showInViewController(self)
        }
    }
    
    @objc func didClickCancelButton(){
        SVProgressHUD.dismiss()
        ShowAgoraKitManager.shared.cleanCapture()
        dismiss(animated: true)
    }
}

extension ShowCreateLiveVC: ShowCreateLiveViewDelegate {
    
    func onClickSettingBtnAction() {
        showPreset()
    }
    
    func onClickCameraBtnAction() {
        ShowAgoraKitManager.shared.switchCamera(enableBeauty: true)
    }
    
    func onClickBeautyBtnAction() {
        createView.hideBottomViews = true
    }
    
    func onClickStartBtnAction() {
        guard let roomName = createView.roomName, roomName.count > 0 else {
            ToastView.show(text: "create_room_name_can_not_empty".show_localized)
            return
        }
        
        guard  let roomName = createView.roomName, roomName.count <= 16 else {
            ToastView.show(text: "create_room_name_too_long".show_localized)
            return
        }
        ShowLogger.info("onClickStartBtnAction[\(createView.roomNo)]")
        let roomId = createView.roomNo
        SVProgressHUD.show()
        self.view.isUserInteractionEnabled = false
        let room = ShowRoomDetailModel()
        room.roomName = roomName
        room.roomId = roomId
        room.thumbnailId = "\(Int.random(in: 1...3))"
        room.ownerId = VLUserCenter.user.id
        room.ownerName = VLUserCenter.user.name
        room.ownerAvatar = VLUserCenter.user.headUrl
        room.createdAt = Date().millionsecondSince1970()
        let params = (room.yy_modelToJSONObject() as? [String: Any]) ?? [:]
        AppContext.showServiceImp()?.createRoom(roomId: createView.roomNo,
                                                roomName: roomName, payload: params) { [weak self] err, detailModel in
            guard let wSelf = self else { return }
            SVProgressHUD.dismiss()
            wSelf.view.isUserInteractionEnabled = true
            if let _ = err {
                ToastView.show(text: "show_create_room_fail".show_localized)
                return
            }
            guard let detailModel = detailModel else { return }
            let liveVC = ShowLivePagesViewController()
            liveVC.roomList = [detailModel]
            liveVC.focusIndex = liveVC.roomList?.firstIndex(where: { $0.roomId == roomId }) ?? 0
            
            wSelf.navigationController?.pushViewController(liveVC, animated: false)
        }
    }
}
