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
import AgoraCommon

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
                    _leaveRoom(oldRoom)
                }
                
                if let room = room {
                    serviceImp = AppContext.commerceServiceImp(room.roomId)
//                    _joinRoom(room)
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
    private var currentLikeCount: Int?
    
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
    
    private var ownerExpiredView: CommerceRoomOwnerExpiredView?
    
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
        view.translatesAutoresizingMaskIntoConstraints = false
        view.startBidGoodsClosure = { [weak self, weak view] in
            guard let self = self else { return }
            view?.toggleLoadingIndicator(true)
            self.addBidGoodsInfo(status: .started) { error in
                view?.toggleLoadingIndicator(false)
                if let error = error {
                    ToastView.show(text: "\("show_auction_fail_toast".commerce_localized) Error:\(error.code)")
                    return
                }
            }
        }
        view.endBidGoodsClosure = { [weak self, weak view] model in
            guard let self = self, let model = model else { return }
            self.serviceImp?.endBidGoodsInfo(roomId: self.roomId, goods: model) { error in
                //TODO: retry
                guard let _ = error else { return }
                //retry every 5s
                DispatchQueue.main.asyncAfter(deadline: .now() + 5) {
                    view?.checkRetryCompletion()
                }
            }
        }
        view.bidInAuctionGoodsClosure = { [weak self, weak view] model in
            guard let self = self, let model = model else { return }
            view?.toggleLoadingIndicator(true)
            self.serviceImp?.updateBidGoodsInfo(roomId: self.roomId, goods: model, completion: {[weak self] error in
                view?.toggleLoadingIndicator(false)
                if let error = error {
                    if let _ = CommerceServiceError(rawValue: error.code) {
                        ToastView.show(text: error.localizedDescription)
                        return
                    }
                    ToastView.show(text: "\("show_bid_fail_toast".commerce_localized) Error:\(error.code)")
                    return
                }
            })
        }
        
        view.getCurrentTsClosure = {[weak self] in
            return self?.serviceImp?.getCurrentTs(roomId: self?.roomId ?? "") ?? 0
        }
        return view
    }()
    
    private lazy var realTimeView: CommerceRealTimeDataView = {
        let realTimeView = CommerceRealTimeDataView(isLocal: role == .broadcaster)
//        view.addSubview(realTimeView)
//        realTimeView.snp.makeConstraints { make in
//            make.centerX.equalToSuperview()
//            make.top.equalTo(Screen.safeAreaTopHeight() + 50)
//        }
        return realTimeView
    }()
    
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
            if room?.ownerId == VLUserCenter.user.id {
                self.liveView.showThumnbnailCanvasView = muteLocalVideo
            }
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
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        commerceLogger.info("init-- CommerceLiveViewController")
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        guard let room = room else {return}
        setupUI()
        if room.ownerId == VLUserCenter.user.id {
            joinChannel()
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
        view.addSubview(auctionView)
        auctionView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        auctionView.bottomAnchor.constraint(equalTo: view.bottomAnchor, constant: -(Screen.safeAreaBottomHeight() + 6)).isActive = true
        auctionView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        view.layoutIfNeeded()
        
        liveView.leadingAnchor.constraint(equalTo: view.leadingAnchor).isActive = true
        liveView.topAnchor.constraint(equalTo: view.topAnchor).isActive = true
        liveView.trailingAnchor.constraint(equalTo: view.trailingAnchor).isActive = true
        liveView.bottomAnchor.constraint(equalTo: auctionView.topAnchor,
                                         constant: -6).isActive = true
    }
    
    func leaveRoom(){
        guard let room = room else { return }
        AgoraEntLog.autoUploadLog(scene: CommerceLogger.kLogKey)
        _leaveRoom(room)
        
//        CommerceAgoraKitManager.shared.removeRtcDelegate(delegate: self, roomId: roomId)
//        CommerceAgoraKitManager.shared.cleanCapture()
//        CommerceAgoraKitManager.shared.leaveChannelEx(roomId: roomId, channelId: roomId)
//        
//        serviceImp?.leaveRoom {_ in
//        }
//        serviceImp?.unsubscribeEvent(delegate: self)
    }
    
    private func joinChannel(needUpdateCavans: Bool = true) {
        guard let channelId = room?.roomId, let ownerId = room?.ownerId, let uid: UInt = UInt(ownerId) else {
            commerceLogger.warning("joinChannel[\(room?.roomId ?? "")] break ownerId: \(room?.ownerId ?? "")")
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
        commerceLogger.info("joinChannelEx[\(channelId)] ownerId: \(ownerId) role: \(role.rawValue)")
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
    
    private func addGoodsList() {
        guard role == .broadcaster else { return }
        serviceImp?.addGoodsList(roomId: roomId,
                                 goods: CommerceGoodsBuyModel.createGoodsData().compactMap({ $0.goods }),
                                 completion: { _ in })
    }
    
    private func addBidGoodsInfo(status: CommerceAuctionStatus, completion: ((NSError?) -> ())?) {
        guard role == .broadcaster else { return }
        let auctionModel = CommerceGoodsAuctionModel()
        let goodsModel = CommerceGoodsModel()
        goodsModel.title = "Diamond Ring"
        goodsModel.price = 1
        goodsModel.quantity = 1
        auctionModel.goods = goodsModel
        auctionModel.status = status
        auctionModel.startTimestamp = serviceImp?.getCurrentTs(roomId: roomId) ?? 0
        auctionModel.endTimestamp = 30 * 1000 + auctionModel.startTimestamp
        auctionModel.bidUser = nil
        auctionModel.bid = 1
        serviceImp?.addBidGoodsInfo(roomId: roomId, goods: auctionModel, completion: { error in
            commerceLogger.info("error: \(error?.localizedDescription ?? "")")
            completion?(error)
        })
    }
    
    private func getBidGoodsInfo() {
        serviceImp?.getBidGoodsInfo(roomId: roomId, completion: { [weak self] error, auctionModel in
            guard let model = auctionModel else { return }
            self?.auctionView.setGoodsData(model: model, isBroadcaster: self?.role == .broadcaster)
        })
    }
    
    private func subscribeBidGoodsInfo() {
        serviceImp?.subscribeBidGoodsInfo(roomId: roomId, completion: { [weak self] error, auctionModel in
            guard let self = self else {return}
            if error != nil {
                commerceLogger.info("error: \(error?.localizedDescription ?? "")")
                return
            }
            guard let model = auctionModel else { return }
            let origGoodStatus = self.auctionView.currentGoodStatus()
            let isBroadcaster = self.role == .broadcaster
            self.auctionView.setGoodsData(model: model, isBroadcaster: isBroadcaster)
            var origGoodStatusValid = false
            if isBroadcaster {
                //主播completion无条件显示
                origGoodStatusValid = true
            } else if origGoodStatus != nil, origGoodStatus != .completion {
                //防止重新进入时再弹出(第一次为nil，第二次为completion)
                origGoodStatusValid = true
            }
            //之前是start、现在是completion，才会显示完成弹窗
            if model.status == .completion,
               origGoodStatusValid,
               model.bid > 1,
               model.bidUser?.id != "" {
                let resultView = CommerceAuctionResultView()
                resultView.setBidGoods(model: model)
                AlertManager.show(view: resultView)
            }
        })
    }
}

//MARK: private
extension CommerceLiveViewController {
    func _joinRoom(_ room: CommerceRoomListModel){
        finishView?.removeFromSuperview()
        CommerceAgoraKitManager.shared.addRtcDelegate(delegate: self, roomId: room.roomId)
        if let service = serviceImp, role == .audience {
            service.joinRoom(room: room) {[weak self] error, detailModel in
                guard let self = self else {return}
                guard self.room?.roomId == room.roomId else { return }
                if let err = error {
                    commerceLogger.info("joinRoom[\(room.roomId)] error: \(error?.code ?? 0)")
                    if err.code == -1 {
                        self.onRoomExpired()
                    } else {
                        ToastView.show(text: "\("show_join_fail_toast".commerce_localized): \(err.code)")
                        self.leaveRoom()
                        self.dismiss(animated: true)
                    }
                }
            }
        }
        _subscribeServiceEvent()
    }
    
    func _leaveRoom(_ room: CommerceRoomListModel){
        CommerceAgoraKitManager.shared.removeRtcDelegate(delegate: self, roomId: room.roomId)
        CommerceAgoraKitManager.shared.cleanCapture()
        CommerceAgoraKitManager.shared.leaveChannelEx(roomId: roomId, channelId: roomId)
        serviceImp?.unsubscribeEvent(delegate: self)
        serviceImp?.leaveRoom { error in
        }
        AppContext.unloadCommerceServiceImp(room.roomId)
        
        self.liveView.clearChatModel()
        self.currentLikeCount = nil
    }
    
    
    func updateLoadingType(playState: AnchorState) {
        if playState == .joinedWithVideo {
//            serviceImp?.initRoom(roomId: roomId, completion: { error in
//            })
            _joinRoom(room!)
        } else if playState == .prejoined {
//            serviceImp?.deinitRoom(roomId: roomId) { error in }
            _leaveRoom(room!)
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
        subscribeBidGoodsInfo()
        serviceImp?.subscribeUpvoteEvent(roomId: roomId, completion: { [weak self] userId, count in
            guard let self = self else {return}
            defer {
                self.currentLikeCount = count
            }
            guard let currentLikeCount = self.currentLikeCount,
                  currentLikeCount != count else {
                return
            }
            self.liveView.showHeartAnimation()
        })
        if role == .broadcaster {
            addBidGoodsInfo(status: .idle) { error in
            }
            addGoodsList()
        }
    }
        
    //MARK: CommerceSubscribeServiceProtocol
    func onConnectStateChanged(state: CommerceServiceConnectState) {
        guard state == .open else {
//            ToastView.show(text: "net work error: \(state)")
            return
        }
    }
    
    
    private func _broadcasterRoomExpired(){
        if ownerExpiredView != nil {return}
        ownerExpiredView = CommerceRoomOwnerExpiredView()
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
    
    private func _audienceRoomOwnerExpired(){
        AppContext.expireCommerceImp(roomId)
        serviceImp?.leaveRoom(completion: { _ in })
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
    
    func onRoomExpired() {
        if role == .broadcaster {
            _broadcasterRoomExpired()
        }else{
            _audienceRoomOwnerExpired()
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
        if let _ = message.message {
            self.liveView.addChatModel(message)
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
        assert(Thread.isMainThread)
        if uid == roomOwnerId {
            if reason == .remoteMuted{
                liveView.showThumnbnailCanvasView = true
            }else if reason == .remoteUnmuted {
                liveView.showThumnbnailCanvasView = false
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
            CommerceAgoraKitManager.shared.preGenerateToken {
                guard let rtmToken = AppContext.shared.commerceRtmToken, let rtcToken = AppContext.shared.commerceRtcToken else {return}
                CommerceAgoraKitManager.shared.renewToken(channelId: channelId, rtcToken: rtcToken)
                RTMSyncUtil.renew(rtmToken: rtmToken)
            }
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
                self._leaveRoom(room)
            }
            self.updateLoadingType(playState: .idle)
            self.onClickDislikeClosure?()
            self.dismiss(animated: true)
        }
        dialog.onClickDisUserClosure = { [weak self] in
            guard let self = self else { return }
            AppContext.shared.addDislikeUser(at: self.room?.ownerId)
            if let room = self.room {
                self._leaveRoom(room)
            }
            self.updateLoadingType(playState: .idle)
            self.onClickDisUserClosure?()
            self.dismiss(animated: true)
        }
        view.addSubview(dialog)
        dialog.show()
    }
    
    func onClickShoppingButton() {
        let goodsListView = CommerceGoodsListView(isBroadcaster: role == .broadcaster,
                                                  serviceImp: serviceImp,
                                                  roomId: roomId)
        AlertManager.show(view: goodsListView, alertPostion: .bottom)
    }
    
    func onClickSettingButton() {
        let muteAudio: Bool = self.muteLocalAudio
        settingMenuVC.selectedMap = [.camera: self.muteLocalVideo, .mic: muteAudio, .mute_mic: muteAudio]
        settingMenuVC.type = role == .broadcaster ? .idle_broadcaster : .idle_audience
        present(settingMenuVC, animated: true)
    }
    
    func onClickUpvoteButton(count: Int) {
        serviceImp?.upvote(roomId: roomId, count: count, completion: nil)
    }
    
    func getDuration() -> UInt64 {
        return serviceImp?.getRoomDuration(roomId: room?.roomId ?? "") ?? 0
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
                let datas = self.panelPresenter.generatePanelData(send: send, receive: receive, audience: (self.role == .audience))
                self.realTimeView.update(datas: datas.map({ ($0.left, $0.right) }))
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
