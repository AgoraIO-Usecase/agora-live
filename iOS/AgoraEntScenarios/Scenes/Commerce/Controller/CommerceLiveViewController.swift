//
//  CommerceLiveViewController.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/7.
//

import UIKit
import AgoraRtcKit
import SwiftUI
import VideoLoaderAPI

protocol CommerceLiveViewControllerDelegate: NSObjectProtocol {
    func currentUserIsOnSeat()
    func currentUserIsOffSeat()
    func interactionDidChange(roomInfo: CommerceRoomListModel)
}

class CommerceLiveViewController: UIViewController {
    weak var delegate: CommerceLiveViewControllerDelegate?
    var onClickDislikeClosure: (() -> Void)?
    var onClickDisUserClosure: (() -> Void)?
    var room: CommerceRoomListModel? {
        didSet{
            if oldValue?.roomId != room?.roomId {
                oldValue?.interactionAnchorInfoList.removeAll()
                liveView.room = room
                liveView.canvasView.canvasType = .none
                if let oldRoom = oldValue {
                    _leavRoom(oldRoom)
                }
                if let room = room {
                    serviceImp = AppContext.commerceServiceImp(room.roomId)
                    _joinRoom(room)
                }
                loadingType = .prejoined
            }
        }
    }
    
    var loadingType: AnchorState = .prejoined {
        didSet {
            if loadingType == oldValue {
                return
            }
            updateLoadingType(playState: loadingType)
            remoteVideoWidth = nil
            currentMode = nil
        }
    }
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
    private var currentMode: CommerceMode?
    
    private var joinRetry = 0
    
    private var interruptInteractionReason: String?
    
    //TODO: remove
    private lazy var settingMenuVC: CommerceToolMenuViewController = {
        let settingMenuVC = CommerceToolMenuViewController()
        settingMenuVC.type = CommerceMenuType.idle_audience
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
    
    let channelOptions: AgoraRtcChannelMediaOptions = AgoraRtcChannelMediaOptions()
    
    private lazy var musicPresenter: CommerceMusicPresenter? = {
        return CommerceMusicPresenter()
    }()
    
    private(set) lazy var liveView: CommerceRoomLiveView = {
        let view = CommerceRoomLiveView(isBroadcastor: role == .broadcaster)
        view.delegate = self
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    private lazy var auctionView: CommerceAuctionShoppingView = {
        let view = CommerceAuctionShoppingView(isBroadcastor: role == .broadcaster)
        view.isHidden = true
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    
    private lazy var realTimeView: CommerceRealTimeDataView = {
        let realTimeView = CommerceRealTimeDataView(isLocal: role == .broadcaster)
        view.addSubview(realTimeView)
        realTimeView.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(Screen.safeAreaTopHeight() + 50)
        }
        return realTimeView
    }()
    private var liveViewBottomCons: NSLayoutConstraint?
    
    private lazy var panelPresenter = CommerceDataPanelPresenter()
    
    private var finishView: CommerceReceiveFinishView?
    
    private var muteLocalAudio: Bool = false {
        didSet {
            let options = self.channelOptions
            options.publishMicrophoneTrack = !muteLocalAudio
            CommerceAgoraKitManager.shared.updateChannelEx(channelId: self.room?.roomId ?? "", options: options)
        }
    }
    
    private var muteLocalVideo: Bool = false {
        didSet {
            let options = self.channelOptions
            options.publishCameraTrack = !muteLocalVideo
            CommerceAgoraKitManager.shared.updateChannelEx(channelId: self.room?.roomId ?? "", options: options)
        }
    }
    
    private var serviceImp: CommerceServiceProtocol?
    
    deinit {
        let roomId = room?.roomId ?? ""
        leaveRoom()
        AppContext.unloadCommerceServiceImp(roomId)
        commerceLogger.info("deinit-- CommerceLiveViewController \(roomId)")
//        musicManager?.destory()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        guard let room = room else {return}
        setupUI()
        if room.ownerId == VLUserCenter.user.id {
            joinChannel()
            _subscribeServiceEvent()
            AgoraEntAuthorizedManager.checkMediaAuthorized(parent: self)
        }
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
        
    private func setupUI(){
        view.layer.contents = UIImage.commerce_sceneImage(name: "show_live_room_bg")?.cgImage
        navigationController?.isNavigationBarHidden = true
        liveView.room = room
        view.addSubview(liveView)
        liveView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        liveView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        liveView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        liveViewBottomCons = liveView.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        liveViewBottomCons?.isActive = true
        
        view.addSubview(auctionView)
        auctionView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        auctionView.topAnchor.constraint(equalTo: liveView.bottomAnchor).isActive = true
        auctionView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
    }
    
    func leaveRoom(){
        CommerceAgoraKitManager.shared.removeRtcDelegate(delegate: self, roomId: roomId)
        CommerceAgoraKitManager.shared.cleanCapture()
        CommerceAgoraKitManager.shared.leaveChannelEx(roomId: roomId, channelId: roomId)
        
        serviceImp?.leaveRoom {_ in
        }
        serviceImp?.unsubscribeEvent(delegate: self)
    }
    
    private func joinChannel(needUpdateCavans: Bool = true) {
        guard let channelId = room?.roomId, let ownerId = room?.ownerId,  let uid: UInt = UInt(ownerId) else {
            return
        }
        currentChannelId = channelId
        CommerceAgoraKitManager.shared.addRtcDelegate(delegate: self, roomId: channelId)
        if needUpdateCavans {
            if self.role == .audience {
                CommerceAgoraKitManager.shared.setupRemoteVideo(channelId: channelId,
                                                            uid: uid,
                                                            canvasView: self.liveView.canvasView.localView)
            } else {
                CommerceAgoraKitManager.shared.setupLocalVideo(uid: uid, canvasView: self.liveView.canvasView.localView)
            }
        }
        CommerceAgoraKitManager.shared.joinChannelEx(currentChannelId: channelId,
                                                 targetChannelId: channelId,
                                                 ownerId: uid,
                                                 options: self.channelOptions,
                                                 role: role) {
        }
        
        liveView.canvasView.setLocalUserInfo(name: room?.ownerName ?? "", img: room?.ownerAvatar ?? "")
        
        self.muteLocalVideo = false
        self.muteLocalAudio = false
    }
    
    private func sendMessageWithText(_ text: String) {
        let showMsg = CommerceMessage()
        showMsg.userId = VLUserCenter.user.id
        showMsg.userName = VLUserCenter.user.name
        showMsg.message = text
        showMsg.createAt = Date().millionsecondSince1970()
        
        serviceImp?.sendChatMessage(roomId: roomId, message: showMsg) { error in
        }
    }
}

//MARK: private
extension CommerceLiveViewController {
    func _joinRoom(_ room: CommerceRoomListModel){
        finishView?.removeFromSuperview()
        CommerceAgoraKitManager.shared.addRtcDelegate(delegate: self, roomId: room.roomId)
        if let service = serviceImp {
            service.joinRoom(room: room) {[weak self] error, detailModel in
                guard let self = self else {return}
                guard self.room?.roomId == room.roomId else { return }
                if let err = error {
                    commerceLogger.info("joinRoom[\(room.roomId)] error: \(error?.code ?? 0)")
                    if err.code == -1 {
                        self.onRoomExpired()
                    }
                } else {
                    self._subscribeServiceEvent()

                    self.updateLoadingType(playState: self.loadingType)
                }
            }
        } else {
            self.onRoomExpired()
        }
    }
    
    func _leavRoom(_ room: CommerceRoomListModel){
        CommerceAgoraKitManager.shared.removeRtcDelegate(delegate: self, roomId: room.roomId)
        AppContext.commerceServiceImp(room.roomId)?.unsubscribeEvent(delegate: self)
        AppContext.commerceServiceImp(room.roomId)?.leaveRoom { error in
        }
        AppContext.unloadCommerceServiceImp(room.roomId)
    }
    
    
    func updateLoadingType(playState: AnchorState) {
        if playState == .joinedWithVideo {
            serviceImp?.initRoom(roomId: roomId, completion: { error in
            })
        } else if playState == .prejoined {
            serviceImp?.deinitRoom(roomId: roomId) { error in }
        } else {
        }
        updateRemoteCavans()
    }
    
    func updateRemoteCavans() {
        guard role == .audience, loadingType == .joinedWithVideo else { return }
        let uid: UInt = UInt(room?.ownerId ?? "0") ?? 0
        CommerceAgoraKitManager.shared.setupRemoteVideo(channelId: roomId,
                                                        uid: uid,
                                                        canvasView: liveView.canvasView.localView)
    }
}

//MARK: service subscribe
extension CommerceLiveViewController: CommerceSubscribeServiceProtocol {
    func onRoomDestroy(roomId: String) {
        guard roomId == self.roomId else { return }
        onRoomExpired()
    }
    
    private func _subscribeServiceEvent() {
        serviceImp?.subscribeEvent(delegate: self)
    }
        
    //MARK: CommerceSubscribeServiceProtocol
    func onConnectStateChanged(state: CommerceServiceConnectState) {
        guard state == .open else {
//            ToastView.show(text: "net work error: \(state)")
            return
        }
    }
    
    func onRoomExpired() {
        AppContext.expireCommerceImp(roomId)
        serviceImp?.leaveRoom(completion: { _ in })
        serviceImp = nil
        finishView?.removeFromSuperview()
        finishView = CommerceReceiveFinishView()
        finishView?.headImg = room?.ownerAvatar ?? ""
        finishView?.headName = room?.ownerName ?? ""
        finishView?.delegate = self
        self.view.addSubview(finishView!)
        finishView?.snp.makeConstraints { make in
            make.left.right.top.bottom.equalToSuperview()
        }
    }
    
    func onUserCountChanged(userCount: Int) {
        self.liveView.roomUserCount = userCount
    }
    
    func onUserJoinedRoom(user: CommerceUser) {
        
    }
    
    func onUserLeftRoom(user: CommerceUser) {
        if user.userId == room?.ownerId {
            commerceLogger.info(" finishAlertVC onUserLeftRoom : roomid = \(roomId)")
            onRoomExpired()
        }
    }
    
    func onMessageDidAdded(message: CommerceMessage) {
        if let text = message.message {
            let model = CommerceChatModel(userName: message.userName ?? "", text: text)
            self.liveView.addChatModel(model)
        }
    }
}


extension CommerceLiveViewController: AgoraRtcEngineDelegate {
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurWarning warningCode: AgoraWarningCode) {
        commerceLogger.warning("rtcEngine warningCode == \(warningCode.rawValue)")
    }
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        commerceLogger.warning("rtcEngine errorCode == \(errorCode.rawValue)")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        commerceLogger.info("rtcEngine didJoinChannel \(channel) with uid \(uid) elapsed \(elapsed)ms")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        commerceLogger.info("rtcEngine didJoinedOfUid \(uid) channelId: \(roomId)", context: kCommerceLogBaseContext)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        commerceLogger.info("rtcEngine didOfflineOfUid === \(uid)")
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, reportRtcStats stats: AgoraChannelStats) {
        panelPresenter.updateChannelStats(stats)
        throttleRefreshRealTimeInfo()
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, localAudioStats stats: AgoraRtcLocalAudioStats) {
        panelPresenter.updateLocalAudioStats(stats)
        throttleRefreshRealTimeInfo()
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, localVideoStats stats: AgoraRtcLocalVideoStats, sourceType: AgoraVideoSourceType) {
        panelPresenter.updateLocalVideoStats(stats)
        throttleRefreshRealTimeInfo()
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, remoteVideoStats stats: AgoraRtcRemoteVideoStats) {
        if role == .audience {
            if let ownerId = room?.ownerId, stats.uid != (Int(ownerId) ?? 0) {
                return
            }
        }
        panelPresenter.updateVideoStats(stats)
        throttleRefreshRealTimeInfo()
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, remoteAudioStats stats: AgoraRtcRemoteAudioStats) {
        panelPresenter.updateAudioStats(stats)
        throttleRefreshRealTimeInfo()
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, uplinkNetworkInfoUpdate networkInfo: AgoraUplinkNetworkInfo) {
        panelPresenter.updateUplinkNetworkInfo(networkInfo)
        throttleRefreshRealTimeInfo()
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, downlinkNetworkInfoUpdate networkInfo: AgoraDownlinkNetworkInfo) {
        panelPresenter.updateDownlinkNetworkInfo(networkInfo)
        throttleRefreshRealTimeInfo()
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, contentInspectResult result: AgoraContentInspectResult) {
        commerceLogger.warning("contentInspectResult: \(result.rawValue)")
        guard result != .neutral else { return }
        ToastView.show(text: "A violation of the current content has been detected")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, videoSizeChangedOf sourceType: AgoraVideoSourceType, uid: UInt, size: CGSize, rotation: Int) {

    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit,
                   remoteVideoStateChangedOfUid uid: UInt,
                   state: AgoraVideoRemoteState,
                   reason: AgoraVideoRemoteReason,
                   elapsed: Int) {
        DispatchQueue.main.async {
            let channelId = self.room?.roomId ?? ""
            commerceLogger.info("didLiveRtcRemoteVideoStateChanged channelId: \(channelId) uid: \(uid) state: \(state.rawValue) reason: \(reason.rawValue)",
                            context: kCommerceLogBaseContext)
            if state == .decoding /*2*/,
               ( reason == .remoteUnmuted /*6*/ || reason == .localUnmuted /*4*/ || reason == .localMuted /*3*/ )   {
                commerceLogger.info("show first frame (\(channelId))", context: kCommerceLogBaseContext)
                // TODO: FAPNGEG
//                if let ts = ShowAgoraKitManager.shared.callTimestampEnd() {
//                    self.panelPresenter.updateTimestamp(ts)
//                    self.throttleRefreshRealTimeInfo()
//                }
            }
        }
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, firstLocalVideoFramePublishedWithElapsed elapsed: Int, sourceType: AgoraVideoSourceType) {
        commerceLogger.info("firstLocalVideoFramePublishedWithElapsed: \(elapsed)ms \(sourceType.rawValue)",
                        context: kCommerceLogBaseContext)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, tokenPrivilegeWillExpire token: String) {
        commerceLogger.warning("tokenPrivilegeWillExpire: \(roomId)",
                           context: kCommerceLogBaseContext)
        if let channelId = currentChannelId {
            CommerceAgoraKitManager.shared.renewToken(channelId: channelId)
        }
    }
}


extension CommerceLiveViewController: CommerceRoomLiveViewDelegate {
    func onClickSendMsgButton(text: String) {
        sendMessageWithText(text)
    }
    
    func onClickCloseButton() {
        if role == .broadcaster {
            commerce_showAlert(message: "show_alert_live_end_title".commerce_localized) {[weak self] in
                self?.leaveRoom()
                self?.dismiss(animated: true)
            }
        }else {
            updateLoadingType(playState: .idle)
            leaveRoom()
            dismiss(animated: true)
        }
    }
    
    func onClickMoreButton() {
        let dialog = CommerceLiveMoreDialog(frame: view.bounds)
        dialog.onClickDislikeClosure = { [weak self] in
            guard let self = self else { return }
            AppContext.shared.addDislikeRoom(at: self.room?.roomId)
            if let room = self.room {
                self._leavRoom(room)
            }
            self.updateLoadingType(playState: .idle)
            self.onClickDislikeClosure?()
            self.dismiss(animated: true)
        }
        dialog.onClickDisUserClosure = { [weak self] in
            guard let self = self else { return }
            AppContext.shared.addDislikeUser(at: self.room?.ownerId)
            if let room = self.room {
                self._leavRoom(room)
            }
            self.updateLoadingType(playState: .idle)
            self.onClickDisUserClosure?()
            self.dismiss(animated: true)
        }
        view.addSubview(dialog)
        dialog.show()
    }
    
    func onClickShoppingButton() {
//        let shoppingListView = CommerceShoppingListView(isBroadcaster: role == .broadcaster)
//        AlertManager.show(view: shoppingListView, alertPostion: .bottom)
        liveViewBottomCons?.constant = -(auctionView.height + Screen.safeAreaBottomHeight() + 6)
        liveViewBottomCons?.isActive = true
        auctionView.isHidden = false
        UIView.animate(withDuration: 0.25) {
            self.view.layoutIfNeeded()
        }
    }
    
    func onClickSettingButton() {
        let muteAudio: Bool = self.muteLocalAudio
        settingMenuVC.selectedMap = [.camera: self.muteLocalVideo, .mic: muteAudio, .mute_mic: muteAudio]
        settingMenuVC.type = role == .broadcaster ? .idle_broadcaster : .idle_audience
        present(settingMenuVC, animated: true)
    }
    
}

extension CommerceLiveViewController {
    
    private func throttleRefreshRealTimeInfo() {
        CommerceThrottler.throttle(delay: .seconds(1)) { [weak self] in
            guard let `self` = self else {
                return
            }
            DispatchQueue.main.async {
                let receive = true
                let send = true
                let data = self.panelPresenter.generatePanelData(send: send, receive: receive, audience: (self.role == .audience))
                self.realTimeView.update(left: data.left, right: data.right)
            }
        }
    }
}


extension CommerceLiveViewController {
    private func showError(title: String, errMsg: String) {
        commerce_showAlert(title: title, message: errMsg) { [weak self] in
            self?.leaveRoom()
            self?.dismiss(animated: true)
        }
    }
}

extension CommerceLiveViewController: CommerceToolMenuViewControllerDelegate {
    
    // Switch camera
    func onClickCameraButtonSelected(_ menu: CommerceToolMenuViewController, _ selected: Bool) {
        AgoraEntAuthorizedManager.checkCameraAuthorized(parent: self) { granted in
            guard granted else { return }
            self.muteLocalVideo = selected
            if selected {
                CommerceAgoraKitManager.shared.engine?.stopPreview()
            } else {
                CommerceAgoraKitManager.shared.engine?.startPreview()
            }
        }
    }
    
    // Microphone switch
    func onClickMicButtonSelected(_ menu: CommerceToolMenuViewController, _ selected: Bool) {
        AgoraEntAuthorizedManager.checkAudioAuthorized(parent: self) { granted in
            guard granted else { return }
            self.muteLocalAudio = selected
        }
    }
    
    // Sound off
    func onClickMuteMicButtonSelected(_ menu: CommerceToolMenuViewController, _ selected: Bool) {
        self.muteLocalAudio = selected
    }
    
    func onClickRealTimeDataButtonSelected(_ menu: CommerceToolMenuViewController, _ selected: Bool) {
        view.addSubview(realTimeView)
        realTimeView.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(Screen.safeAreaTopHeight() + 50)
        }
    }
    
    func onClickSwitchCameraButtonSelected(_ menu: CommerceToolMenuViewController, _ selected: Bool) {
        AgoraEntAuthorizedManager.checkCameraAuthorized(parent: self) { granted in
            guard granted else { return }
            CommerceAgoraKitManager.shared.switchCamera(self.roomId)
        }
    }
    
    func onClickSettingButtonSelected(_ menu: CommerceToolMenuViewController, _ selected: Bool) {
        settingMenuVC.dismiss(animated: true) {[weak self] in
            guard let wSelf = self else { return }
            if AppContext.shared.isDebugMode {
                let vc = CommerceDebugSettingVC()
                vc.isBroadcastor = wSelf.role == .broadcaster
                wSelf.navigationController?.pushViewController(vc, animated: true)
            }else {
                let vc = CommerceAdvancedSettingVC()
                vc.mode = .single
                vc.isBroadcaster = wSelf.role == .broadcaster
                vc.musicManager = wSelf.musicPresenter
                vc.currentChannelId = wSelf.currentChannelId
                wSelf.navigationController?.pushViewController(vc, animated: true)
            }
        }
    }
}
// MARK: - CommerceReceiveFinishViewDelegate
extension CommerceLiveViewController: CommerceReceiveFinishViewDelegate {
    func onClickFinishButton() {
        onClickCloseButton()
    }
}
