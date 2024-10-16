//
//  ShowLiveViewController.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/7.
//

import UIKit
import AgoraRtcKit
import SwiftUI
import VideoLoaderAPI
import RTMSyncManager
import AgoraCommon

protocol ShowLiveViewControllerDelegate: NSObjectProtocol {
    func currentUserIsOnSeat()
    func currentUserIsOffSeat()
    
    func interactionDidChange(roomInfo: ShowRoomListModel)
    
    func willLeaveRoom(roomId: String)
}

class ShowLiveViewController: UIViewController {
    private var timer: Timer?
    private var isJoined = false
    private var videoWidth = 720
    private var videoHeight = 1280
    weak var delegate: ShowLiveViewControllerDelegate?
    let dreamFlowService: DreamFlowService = DreamFlowService()
    var onClickDislikeClosure: (() -> Void)?
    var onClickDisUserClosure: (() -> Void)?
    var room: ShowRoomListModel? {
        didSet{
            if oldValue?.roomId == room?.roomId { return }
            oldValue?.interactionAnchorInfoList.removeAll()
            liveView.room = room
//            if let oldRoom = oldValue {
//                _leavRoom(oldRoom)
//            }
//            if let room = room {
//                serviceImp = AppContext.showServiceImp()
//                _joinRoom(room)
//            }
//            loadingType = .prejoined
        }
    }
    
    var loadingType: AnchorState = .prejoined {
        didSet {
            if loadingType == oldValue {
                return
            }
            remoteVideoWidth = nil
            currentMode = nil
            switch loadingType {
            case .idle, .prejoined:
                leaveRoom()
            case .joinedWithVideo, .joinedWithAudioVideo:
                if let room = room {
                    serviceImp = AppContext.showServiceImp()
                }
            }
        }
    }
    private weak var inviteVC: UIViewController?
    private var currentChannelId: String?
    
    private var roomId: String {
        get {
            guard let roomId = room?.roomId else {
                assert(false, "room == nil")
                return ""
            }
            
            return roomId
        }
    }
    
    private var remoteVideoWidth: UInt?
    private var currentMode: ShowMode?
    
    private var joinRetry = 0
    
    private var interruptInteractionReason: String?
    
    //TODO: remove
    private lazy var settingMenuVC: ShowToolMenuViewController = {
        let settingMenuVC = ShowToolMenuViewController()
        settingMenuVC.type = ShowMenuType.idle_audience
        settingMenuVC.delegate = self
        return settingMenuVC
    }()
    
    private var roomOwnerId: UInt {
        get{
            UInt(room?.ownerId ?? "0") ?? 0
        }
    }
    
    private var currentUserId: String {
        get{
            VLUserCenter.user.id
        }
    }

    private var role: AgoraClientRole {
        return room?.ownerId == VLUserCenter.user.id ? .broadcaster : .audience
    }
    private var isSendJointBroadcasting: Bool = false
    
    let channelOptions:AgoraRtcChannelMediaOptions = AgoraRtcChannelMediaOptions()
    
    private(set) lazy var liveView: ShowRoomLiveView = {
        let view = ShowRoomLiveView(isBroadcastor: role == .broadcaster)
        view.delegate = self
        return view
    }()
        
    private var progressView: DFProgressView?
    
    private var finishView: ShowReceiveFinishView?
    
    private var ownerExpiredView: ShowRoomOwnerExpiredView?

    private var muteLocalAudio: Bool = false {
        didSet {
            ShowLogger.info("muteLocalAudio: \(muteLocalVideo)")
            channelOptions.publishMicrophoneTrack = !muteLocalAudio
            ShowAgoraKitManager.shared.updateChannelEx(channelId: self.room?.roomId ?? "", options: channelOptions)
        }
    }
    
    private var muteLocalVideo: Bool = false {
        didSet {
            ShowLogger.info("muteLocalVideo: \(muteLocalVideo)")
            channelOptions.publishCameraTrack = !muteLocalVideo
            ShowAgoraKitManager.shared.updateChannelEx(channelId: self.room?.roomId ?? "", options: channelOptions)
        }
    }
    
    private var serviceImp: ShowServiceProtocol?
    
    deinit {
        ShowLogger.info("deinit-- ShowLiveViewController \(self)")
    }
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        ShowLogger.info("init-- ShowLiveViewController \(self)")
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        deleteAllWork()
        guard let room = room else {return}
        setupUI()
        if room.ownerId == VLUserCenter.user.id {
            setupLocalView()
            AgoraEntAuthorizedManager.checkMediaAuthorized(parent: self)
        }
        
        joinChannel()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        navigationController?.interactivePopGestureRecognizer?.isEnabled = false
        UIApplication.shared.isIdleTimerDisabled = true
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        UIApplication.shared.isIdleTimerDisabled = false
    }
    
    override var preferredStatusBarStyle: UIStatusBarStyle {
        .lightContent
    }
    
    private func setupLocalView() {
        guard let channelId = room?.roomId else {
            return
        }
        
        ShowAgoraKitManager.shared.addRtcDelegate(delegate: self, roomId: channelId)
        ShowAgoraKitManager.shared.setupLocalVideo(canvasView: self.liveView.canvasView.localBackgroundView.contentView)
    }
        
    private func setupUI(){
        view.layer.contents = UIImage.show_sceneImage(name: "show_live_room_bg")?.cgImage
        navigationController?.isNavigationBarHidden = true
        liveView.room = room
        view.addSubview(liveView)
        liveView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    }
    
    private func startTimer() {
        if timer == nil {
            timer = Timer.scheduledTimer(timeInterval: 3.0, target: self, selector: #selector(updateCountdown), userInfo: nil, repeats: true)
        }
    }
    
    private func showProgressView() {
        progressView = DFProgressView.show(in: self.view)
    }
    
    private func hideProgressView() {
        DFProgressView.hide(from: self.view)
    }
    
    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
    
    private func updateViewState() {
        stopTimer()
        self.updateStylizedButton()
        self.hideProgressView()
    }
    
    @objc private func updateCountdown() {
        guard let workId = dreamFlowService.responseModel?.id else {
            stopTimer()
            return
        }
        
        dreamFlowService.queryWorker(workerId: workId) { [weak self] error, res in
            if error != nil {
                ToastView.showWait(text: "\(error?.localizedDescription)")
                self?.updateViewState()
                return
            }
            
            if let state = res, state == "start_success" {
                self?.dreamFlowService.workState = .running
                self?.updateViewState()
            }
        }
    }
    
    private func createWorker(stylizedConfig: DFStylizedSettingConfig) {
        guard let channelId = room?.roomId else { return }
        currentChannelId = channelId
        dreamFlowService.creatWork(channelName: currentChannelId ?? "", stylizedConfig: stylizedConfig) { [weak self] error, res in
            if error != nil {
                ToastView.show(text: "Failed to save the settings: \(error?.localizedDescription)")
                return
            }
            
            guard let state = res else {
                ToastView.show(text: "Failed to save the settings: \(error?.localizedDescription)")
                return
            }
            
            if state == .failed {
                ToastView.show(text: "Failed to initialize: \(error?.localizedDescription)")
                return
            }
            
            guard let res = res else { return }
            self?.showProgressView()
            
            self?.liveView.blurGusetCanvas = false
            if res == .initialize {
                DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                    self?.startTimer()
                }
            }
            
            self?.updateStylizedButton()
        }
    }
    
    private func deleteWorker() {
        guard let workId = dreamFlowService.responseModel?.id else { return }
        
        dreamFlowService.deleteWorker(workerId: workId) { error, res in
            if let err = error {
                ToastView.show(text: "Failed to save the settings: \(error?.localizedDescription)")
                print("delete work failed, error: \(err.localizedDescription)")
                return
            }
        }
    }
    
    private func updateWorker(stylizedConfig:DFStylizedSettingConfig) {
        guard let workerId = dreamFlowService.responseModel?.id else {
            return
        }
        
        dreamFlowService.updateWorker(workerId: workerId, stylizedConfig: stylizedConfig) { error, res in
            if error != nil {
                ToastView.show(text: "Failed to save the settings: \(error?.localizedDescription)")
                return
            }
            
            print("update worker success!")
        }
    }
    
    private func deleteAllWork() {
        if let settingConfig = DFStylizedDataService.loadStylizedSettingConfig() {
            dreamFlowService.server = settingConfig.server
        }
        
        dreamFlowService.deleteAllWorker { error, res in
            print("")
        }
    }
    
    private func updateStylizedButton() {
        let workState = dreamFlowService.workState
        switch workState {
        case .initialize:
            self.liveView.bottomBar.beautyButton.isHidden = true
            break
        case .running, .unload, .failed:
            self.liveView.bottomBar.beautyButton.isHidden = false
            break
        }
    }
    
    func leaveRoom(){
        stopTimer()
        deleteWorker()
        
        AgoraEntLog.autoUploadLog(scene: ShowLogger.kLogKey)
        ShowAgoraKitManager.shared.removeRtcDelegate(delegate: self, roomId: roomId)
        ShowAgoraKitManager.shared.cleanCapture()
        ShowAgoraKitManager.shared.leaveChannelEx(roomId: roomId, channelId: roomId)
        
        serviceImp?.unsubscribeEvent(roomId: roomId, delegate: self)
        
        serviceImp?.leaveRoom(roomId: roomId) {_ in }
    }
    
    //broadcaster join channel
    private func joinChannel() {
        assert(role == .broadcaster, "role invalid")
        guard let channelId = room?.roomId, let ownerId = room?.ownerId,  let uid: UInt = UInt(ownerId) else {
            return
        }
        currentChannelId = channelId
        ShowAgoraKitManager.shared.setVideoDimensions(CGSize(width: videoWidth, height: videoHeight))
        ShowAgoraKitManager.shared.joinChannelEx(currentChannelId: currentChannelId ?? "",
                                                 targetChannelId: currentChannelId ?? "",
                                                 ownerId: uid,
                                                 options: self.channelOptions,
                                                 role: role) {
        }
        self.muteLocalVideo = false
        self.muteLocalAudio = false
        ShowAgoraKitManager.shared.setupRemoteVideo(channelId: currentChannelId ?? "", uid: UInt(dreamFlowService.genaiUid), canvasView: liveView.canvasView.remoteView)
        
    }
}

//MARK: private
extension ShowLiveViewController {
    private func _updateApplyMenu() {
        if role == .broadcaster {
            serviceImp?.getAllMicSeatApplyList(roomId: roomId) {[weak self] _, list in
                guard let list = list?.filterDuplicates({ $0.userId }) else { return }
                self?.liveView.bottomBar.linkButton.isShowRedDot = list.count > 0
            }
        }
    }
    
    func _joinRoom(_ room: ShowRoomListModel){
        finishView?.removeFromSuperview()
        ownerExpiredView?.removeFromSuperview()
        ShowAgoraKitManager.shared.addRtcDelegate(delegate: self, roomId: room.roomId)
        if let service = serviceImp {
            service.joinRoom(room: room) {[weak self] error, detailModel in
                guard let self = self else {return}
                guard self.room?.roomId == room.roomId else { return }
                if let _ = error {
                    self.onRoomFailed(channelName: room.roomId, title: "show_join_room_fail".show_localized)
                }
            }
            self._subscribeServiceEvent()
        } else {
            ShowLogger.info("serviceImp is nil, roomid = \(roomId)")
            self.onRoomExpired(channelName: roomId)
        }
    }
    
    func _leavRoom(_ room: ShowRoomListModel){
        ShowAgoraKitManager.shared.removeRtcDelegate(delegate: self, roomId: room.roomId)
        serviceImp?.unsubscribeEvent(roomId: roomId, delegate: self)
        serviceImp?.leaveRoom(roomId: roomId) { error in
        }
    }
}

//MARK: service subscribe
extension ShowLiveViewController: ShowSubscribeServiceProtocol {
    private func _subscribeServiceEvent() {
        serviceImp?.subscribeEvent(roomId: roomId, delegate: self)
        
//        _refreshPKUserList()
//        _refreshInteractionList()
    }
    
    private func _refreshPKUserList() {
        
    }
    
    private func _refreshInteractionList() {
        serviceImp?.getInterationInfo(roomId: roomId) { [weak self] (error, interaction) in
            guard let self = self, error == nil else { return }
//            if self.interactionList == nil, let interaction = interaction {
//                // first load
//                if self.role == .broadcaster {
//                    self.serviceImp?.stopInteraction(roomId: self.roomId) { err in
//                    }
//                } else {
//                    self.onInteractionBegan(interaction: interaction)
//                }
//            }
//
//            if let interaction = interaction {
//                self.interactionList = [interaction]
//            } else {
//                self.interactionList = nil
//            }
            
            let list = interaction == nil ? [] : [interaction!]
            self.onInteractionUpdated(channelName: self.roomId, interactions: list)
        }
    }
    
    private func _broadcasterRoomExpired(){
        if ownerExpiredView != nil {return}
        ownerExpiredView = ShowRoomOwnerExpiredView()
        ownerExpiredView?.headImg = VLUserCenter.user.headUrl
        ownerExpiredView?.clickBackButtonAction = {[weak self] in
            self?.leaveRoom()
            self?.dismiss(animated: true)
        }
        self.view.addSubview(ownerExpiredView!)
        ownerExpiredView?.snp.makeConstraints { make in
            make.left.right.top.bottom.equalToSuperview()
        }
    }
    
    private func _audienceRoomOwnerExpired(title: String? = nil){
        finishView?.removeFromSuperview()
        finishView = ShowReceiveFinishView()
        finishView?.headImg = room?.ownerAvatar ?? ""
        finishView?.headName = room?.ownerName ?? ""
        finishView?.title = title
        finishView?.delegate = self
        self.view.addSubview(finishView!)
        finishView?.snp.makeConstraints { make in
            make.left.right.top.bottom.equalToSuperview()
        }
    }
    
    private func onRoomFailed(channelName: String, title: String? = nil) {
        if role == .broadcaster {
            _broadcasterRoomExpired()
        }else{
            _audienceRoomOwnerExpired(title: title)
        }
    }
    
    //MARK: ShowSubscribeServiceProtocol
    func onConnectStateChanged(channelName: String, state: ShowServiceConnectState) {
        guard state == .open else {
//            ToastView.show(text: "net work error: \(state)")
            return
        }
        
        _refreshPKUserList()
        _refreshInteractionList()
    }
    
    func onRoomExpired(channelName: String) {
        liveView.markExpired()
        ShowAgoraKitManager.shared.leaveAllRoom()
        ShowAgoraKitManager.shared.leaveChannelEx(roomId: roomId, channelId: roomId)
        ShowAgoraKitManager.shared.prePublishOnseatVideo(isOn: false, channelId: roomId)
        onRoomFailed(channelName: channelName)
    }
    
    func onRoomDestroy(channelName: String) {
        onRoomExpired(channelName: channelName)
    }
    
    func onUserCountChanged(channelName: String, userCount: Int) {
        self.liveView.roomUserCount = userCount
    }
    
    func onUserJoinedRoom(channelName: String, user: ShowUser) {
    }
    
    func onUserLeftRoom(channelName: String, user: ShowUser) {
        if user.userId == room?.ownerId {
            ShowLogger.info(" finishAlertVC onUserLeftRoom : roomid = \(roomId)")
            onRoomExpired(channelName: channelName)
        }
    }
    
    func onMessageDidAdded(channelName: String, message: ShowMessage) {
        
    }
    
    func onMicSeatApplyUpdated(channelName: String, applies: [ShowMicSeatApply]) {
        ShowLogger.info("onMicSeatApplyUpdated: \(applies.count)")
        _updateApplyMenu()
        if role == .broadcaster {
            liveView.bottomBar.linkButton.isShowRedDot = applies.count > 0 ? true : false
        } else {
            liveView.bottomBar.linkButton.isShowRedDot = false
        }
    }
    
    func onMicSeatInvitationUpdated(channelName: String, invitation: ShowMicSeatInvitation) {
    }
    
    func onMicSeatInvitationAccepted(channelName: String, invitation: ShowMicSeatInvitation) {
    }
    
    func onMicSeatInvitationRejected(channelName: String, invitation: ShowMicSeatInvitation) {
        guard role == .broadcaster else { return }
        AlertManager.hiddenView()
        let alertVC = UIAlertController(title: "\(invitation.userName)" + "show_reject_broadcasting".show_localized, message: nil, preferredStyle: .alert)
        let agree = UIAlertAction(title: "show_sure".show_localized, style: .default, handler: nil)
        alertVC.addAction(agree)
        present(alertVC, animated: true, completion: nil)
    }
    
    func onPKInvitationUpdated(channelName: String, invitation: ShowPKInvitation) { }
    
    func onPKInvitationAccepted(channelName: String, invitation: ShowPKInvitation) { }
    
    func onPKInvitationRejected(channelName: String, invitation: ShowPKInvitation) { }
    
    func onInteractionUpdated(channelName: String, interactions: [ShowInteractionInfo]) { }
    
}

extension ShowLiveViewController: AgoraRtcEngineDelegate {
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurWarning warningCode: AgoraWarningCode) {
        ShowLogger.warn("rtcEngine warningCode == \(warningCode.rawValue)")
    }
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        ShowLogger.warn("rtcEngine errorCode == \(errorCode.rawValue)")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        isJoined = true
        print("--------join channel success")
        ShowLogger.info("rtcEngine didJoinChannel \(channel) with uid \(uid) elapsed \(elapsed)ms")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        print("--------worker joined")
        ShowLogger.info("rtcEngine didJoinedOfUid \(uid) channelId: \(roomId)", context: kShowLogBaseContext)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        ShowLogger.info("rtcEngine didOfflineOfUid === \(uid) reason: \(reason.rawValue)")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, 
                   remoteVideoStateChangedOfUid uid: UInt,
                   state: AgoraVideoRemoteState, 
                   reason: AgoraVideoRemoteReason, elapsed: Int) {  }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, contentInspectResult result: AgoraContentInspectResult) {
        ShowLogger.warn("contentInspectResult: \(result.rawValue)")
        guard result != .neutral else { return }
        ToastView.show(text: "监测到当前内容存在违规行为")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, firstLocalVideoFramePublishedWithElapsed elapsed: Int, sourceType: AgoraVideoSourceType) {
        ShowLogger.info("firstLocalVideoFramePublishedWithElapsed: \(elapsed)ms \(sourceType.rawValue)",
                        context: kShowLogBaseContext)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, tokenPrivilegeWillExpire token: String) {
        ShowLogger.warn("tokenPrivilegeWillExpire: \(roomId)",
                        context: kShowLogBaseContext)
        if let channelId = currentChannelId {
            ShowAgoraKitManager.shared.renewToken(channelId: channelId)
        }
    }
}

//MARK: ShowRoomLiveViewDelegate
extension ShowLiveViewController: ShowRoomLiveViewDelegate {
    func onClickSendMsgButton(text: String) {
    }
    
    func onClickCloseButton() {
        if role == .broadcaster {
            show_showAlert(message: "show_alert_live_end_title".show_localized) {[weak self] in
                guard let self = self else {return}
                self.delegate?.willLeaveRoom(roomId: self.roomId)
                self.dismiss(animated: true)
            }
        }else {
            self.delegate?.willLeaveRoom(roomId: self.roomId)
            dismiss(animated: true)
        }
    }
    
    func onClickMoreButton() {
        let dialog = AUiMoreDialog(frame: view.bounds)
        view.addSubview(dialog)
        dialog.show()
    }
    
    func onClickPKButton(_ button: ShowRedDotButton) { }
    
    func onClickLinkButton(_ button: ShowRedDotButton) { }
    
    func onClickBeautyButton() {
        let vc = DFStylizedSettting()
        vc.workerState = dreamFlowService.workState
        vc.delegate = self
        self.navigationController?.pushViewController(vc, animated: true)
    }
    
    func onClickMusicButton() { }
    
    func onClickSettingButton() {
        let muteAudio = self.muteLocalAudio
        settingMenuVC.selectedMap = [.camera: self.muteLocalVideo, .mic: muteAudio, .mute_mic: muteAudio]
        settingMenuVC.type = .idle_broadcaster
        settingMenuVC.menuTitle = "show_setting_menu_on_seat_title".show_localized

        present(settingMenuVC, animated: true)
    }
}

extension ShowLiveViewController {
    private func showError(title: String, errMsg: String) {
        show_showAlert(title: title, message: errMsg) { [weak self] in
            guard let self = self else {return}
            self.delegate?.willLeaveRoom(roomId: self.roomId)
            self.dismiss(animated: true)
        }
    }
}

extension ShowLiveViewController: ShowToolMenuViewControllerDelegate {
    func onClickRealTimeDataButtonSelected(_ menu: ShowToolMenuViewController, _ selected: Bool) {
        
    }
    
    // Switch camera
    func onClickCameraButtonSelected(_ menu:ShowToolMenuViewController, _ selected: Bool) {
        AgoraEntAuthorizedManager.checkCameraAuthorized(parent: self) { granted in
            guard granted else { return }
            self.muteLocalVideo = selected
            if selected {
                ShowAgoraKitManager.shared.engine?.stopPreview()
            } else {
                ShowAgoraKitManager.shared.engine?.startPreview()
            }
        }
    }
    
    // End wheat connection
    func onClickEndPkButtonSelected(_ menu:ShowToolMenuViewController, _ selected: Bool) {
    }
    
    // Microphone switch
    func onClickMicButtonSelected(_ menu:ShowToolMenuViewController, _ selected: Bool) {
        AgoraEntAuthorizedManager.checkAudioAuthorized(parent: self) {[weak self] granted in
            guard let self = self, granted else { return }
            self.serviceImp?.muteAudio(roomId: self.roomId, mute: selected) { err in
            }
            self.muteLocalAudio = selected
        }
    }
    
    // Sound off
    func onClickMuteMicButtonSelected(_ menu:ShowToolMenuViewController, _ selected: Bool) {
        onClickMicButtonSelected(menu, selected)
    }
    
    func onClickSwitchCameraButtonSelected(_ menu:ShowToolMenuViewController, _ selected: Bool) {
        AgoraEntAuthorizedManager.checkCameraAuthorized(parent: self) { granted in
            guard granted else { return }
            ShowAgoraKitManager.shared.switchCamera(enableBeauty: self.role == .broadcaster)
        }
    }
    
    func onClickSettingButtonSelected(_ menu:ShowToolMenuViewController, _ selected: Bool) {
        settingMenuVC.dismiss(animated: true) {[weak self] in
            guard let wSelf = self else { return }
            if AppContext.shared.isDebugMode {
                
            }else {
                let vc = ShowAdvancedSettingVC()
                vc.isPureMode = (self?.room?.isPureMode != 0)
                vc.mode = .single
                vc.isBroadcaster = wSelf.role == .broadcaster
                vc.currentChannelId = wSelf.currentChannelId
                wSelf.navigationController?.pushViewController(vc, animated: true)
            }
        }
    }
}
// MARK: - ShowReceiveFinishViewDelegate
extension ShowLiveViewController: ShowReceiveFinishViewDelegate {
    func onClickFinishButton() {
        onClickCloseButton()
    }
}

extension ShowLiveViewController: DFStylizedSetttingDelegate {
    func saveStylizedSetting(setting: DFStylizedSettting) {
        let config = setting.stylizedSettingConfig
        config.videHeight = videoHeight
        config.videoWidth = videoWidth
        
        dreamFlowService.server = config.server

        let workerState = setting.workerState

        //初始化worker
        if config.style_effect, workerState == .unload {
            createWorker(stylizedConfig: config)
            return
        }
        
        //检查更新worker
        if config.style_effect, workerState == .running {
            guard let currentConfig = dreamFlowService.currentConfig else { return }
            let currentPrompt = currentConfig.prompt
            
//            if currentPrompt == config.prompt { return }
            
            updateWorker(stylizedConfig: config)
            return
        }
        
        //删除worker
        if !config.style_effect, workerState == .running {
            deleteWorker()
        }
    }
}
