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
    weak var delegate: ShowLiveViewControllerDelegate?
    var onClickDislikeClosure: (() -> Void)?
    var onClickDisUserClosure: (() -> Void)?
    var room: ShowRoomListModel? {
        didSet{
            if oldValue?.roomId == room?.roomId { return }
            oldValue?.interactionAnchorInfoList.removeAll()
            liveView.room = room
            liveView.canvasView.canvasType = .none
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
                    _joinRoom(room)
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
    
    private var isPublishCameraStream: Bool {
        return role == .broadcaster || currentInteraction?.userId == VLUserCenter.user.id
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
        
    private var finishView: ShowReceiveFinishView?
    
    private var ownerExpiredView: ShowRoomOwnerExpiredView?
    
    //interaction list
    private var interactionList: [ShowInteractionInfo]? {
        didSet {
            if AppContext.shared.rtcToken?.isEmpty == false {
                self.currentInteraction = interactionList?.first
                return
            }
            ShowAgoraKitManager.shared.preGenerateToken {[weak self] _ in
                guard let self = self else {return}
                self.currentInteraction = self.interactionList?.first
            }
        }
    }
        
    //get current interaction status
    private var interactionStatus: InteractionType {
        return currentInteraction?.type ?? .idle
    }
    
    private var seatInteraction: ShowInteractionInfo? {
        get {
            if interactionStatus == .linking {
                return currentInteraction
            }
            return nil
        }
    }
    
    private var currentInteraction: ShowInteractionInfo? {
        didSet {
            if let currentInteraction = currentInteraction {
                ShowLogger.info("currentInteraction: \(currentInteraction.description)")
            }
            if self.room?.userId() == self.currentUserId {
                self.liveView.blurHostCanvas = false
                self.liveView.blurGusetCanvas = false
            }
            //update audio status
            if let interaction = currentInteraction {
                liveView.canvasView.setLocalUserInfo(name: room?.ownerName ?? "")
                liveView.canvasView.setRemoteUserInfo(name: interaction.userName)
            } else if role == .broadcaster {
                //unmute if interaction did stop
                self.muteLocalAudio = false
            }
            
            //stop or start interaction
            if currentInteraction == oldValue {
                return
            }
            
            if let info = oldValue {
                _onStopInteraction(interaction: info)
                
                room?.interactionAnchorInfoList.removeAll()
            }
            var needUpdateInteraction = false
            if let info = currentInteraction {
                needUpdateInteraction = true
                
                if let uid = UInt(info.userId) {
                    let anchorInfo = AnchorInfo()
                    anchorInfo.channelName = info.roomId
                    anchorInfo.uid = uid
                    anchorInfo.token = AppContext.shared.rtcToken ?? ""
                    assert(anchorInfo.token.count > 0)
                    room?.interactionAnchorInfoList = [anchorInfo]
                }
            }
            
            if let room = room {
                delegate?.interactionDidChange(roomInfo: room)
            }
            if needUpdateInteraction, let info = currentInteraction {
                //update pk in interactionDidChange before joining the channel, and ensure that delegate is added only after joining the channel (implemented in _onStartInteraction)
                _onStartInteraction(interaction: info)
            }
        }
    }
    
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
        
        guard let room = room else {return}
        setupUI()
        if room.ownerId == VLUserCenter.user.id {
            self.joinChannel()
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
        view.layer.contents = UIImage.show_sceneImage(name: "show_live_room_bg")?.cgImage
        navigationController?.isNavigationBarHidden = true
        liveView.room = room
        view.addSubview(liveView)
        liveView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
    }
    
    func leaveRoom(){
        AgoraEntLog.autoUploadLog(scene: ShowLogger.kLogKey)
        ShowAgoraKitManager.shared.removeRtcDelegate(delegate: self, roomId: roomId)
        ShowAgoraKitManager.shared.cleanCapture()
        ShowAgoraKitManager.shared.leaveChannelEx(roomId: roomId, channelId: roomId)
        
        serviceImp?.unsubscribeEvent(roomId: roomId, delegate: self)
        
        serviceImp?.leaveRoom(roomId: roomId) {_ in
        }
}
    
    //broadcaster join channel
    private func joinChannel() {
        assert(role == .broadcaster, "role invalid")
        guard let channelId = room?.roomId, let ownerId = room?.ownerId,  let uid: UInt = UInt(ownerId) else {
            return
        }
        currentChannelId = channelId
        ShowAgoraKitManager.shared.joinChannelEx(currentChannelId: channelId,
                                                 targetChannelId: channelId,
                                                 ownerId: uid,
                                                 options: self.channelOptions,
                                                 role: role) {
        }
        ShowAgoraKitManager.shared.addRtcDelegate(delegate: self, roomId: channelId)
        ShowAgoraKitManager.shared.setupLocalVideo(canvasView: self.liveView.canvasView.localView)
        liveView.canvasView.setLocalUserInfo(name: room?.ownerName ?? "", img: room?.ownerAvatar ?? "")
        self.muteLocalVideo = false
        self.muteLocalAudio = false
    }
    
    private func sendMessageWithText(_ text: String) {
        let showMsg = ShowMessage()
        showMsg.userId = VLUserCenter.user.id
        showMsg.userName = VLUserCenter.user.name
        showMsg.message = text
        showMsg.createAt = Date().millionsecondSince1970()
        
        serviceImp?.sendChatMessage(roomId: roomId, message: showMsg) { error in
        }
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
        guard invitation.userId == VLUserCenter.user.id, inviteVC == nil else { return }
        if invitation.type == .inviting {
            isSendJointBroadcasting = true
            muteLocalVideo = true
            //收到连麦邀请，先推流，加速出图
            ShowAgoraKitManager.shared.prePublishOnseatVideo(isOn: true, channelId: roomId)
            inviteVC = ShowReceivePKAlertVC.present(name: invitation.userName, style: .mic) {[weak self] result in
                guard let self = self else {return}
                switch result {
                case .accept:
                    ToastView.show(text: "show_is_onseat_doing".show_localized)
                    self.serviceImp?.acceptMicSeatInvitation(roomId: roomId,
                                                             invitationId: invitation.id) { err in
                        guard let err = err else { return }
                        ToastView.show(text: "\("show_accept_invite_linking_fail".show_localized)\(err.code)")
                        //失败，关闭推流
                        ShowAgoraKitManager.shared.prePublishOnseatVideo(isOn: false, channelId: self.roomId)
                    }
                default:
                    self.isSendJointBroadcasting = false
                    //拒绝邀请，关闭推流
                    ShowAgoraKitManager.shared.prePublishOnseatVideo(isOn: false, channelId: self.roomId)
                    self.serviceImp?.rejectMicSeatInvitation(roomId: roomId,
                                                             invitationId: invitation.id) { error in
                    }
                    break
                }
            }
        }
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
    
    func onInteractionUpdated(channelName: String, interactions: [ShowInteractionInfo]) {
        if interactions.count == 0 {
            var toastTitle = interruptInteractionReason
            let type = currentInteraction?.type ?? .idle
            switch type {
            case .idle:
                break
            case .linking:
                toastTitle = "show_end_broadcasting".show_localized
            case .pk:
                toastTitle = "show_end_pk".show_localized
            }
            if let toastStr = toastTitle {
                ToastView.show(text: toastStr)
            }
            interruptInteractionReason = nil
        }
        
        interactionList = interactions
        _refreshPKUserList()
    }
    
    private func _onStartInteraction(interaction: ShowInteractionInfo) {
        ShowLogger.info("_onStartInteraction: \(interaction.userId) \(interaction.userName) status: \(interaction.type.rawValue)")
        switch interaction.type {
        case .pk:
            view.layer.contents = UIImage.show_sceneImage(name: "show_live_pk_bg")?.cgImage
            let interactionRoomId = interaction.roomId
            if interactionRoomId.isEmpty { return }
            if roomId != interaction.roomId {
                ShowAgoraKitManager.shared.addRtcDelegate(delegate: self, roomId: interactionRoomId)
                
                ShowAgoraKitManager.shared.updateVideoProfileForMode(.pk)
                currentChannelId = roomId
                liveView.canvasView.canvasType = .pk
                liveView.canvasView.setRemoteUserInfo(name: interaction.userName)
            }
        case .linking:
            liveView.canvasView.canvasType = .joint_broadcasting
            liveView.canvasView.setRemoteUserInfo(name: interaction.userName)
            
            //TODO: 这个是不是需要真正的角色，放进switchRole里？
            if role == .audience {
                ShowAgoraKitManager.shared.setSuperResolutionOn(false)
            }
            //如果是连麦双方为broadcaster，观众不修改，因为观众可能已经申请上麦，申请时已经修改了角色并提前推流
            let toRole: AgoraClientRole? = isPublishCameraStream ? .broadcaster : nil
            ShowAgoraKitManager.shared.updateLiveView(role: toRole,
                                                      channelId: roomId,
                                                      uid: interaction.userId,
                                                      canvasView: liveView.canvasView.remoteView)
            ShowAgoraKitManager.shared.switchRole(role: toRole,
                                                  channelId: roomId,
                                                  options: self.channelOptions,
                                                  uid: interaction.userId)
            
            liveView.bottomBar.linkButton.isSelected = true
            AlertManager.hiddenView()
            if toRole == .broadcaster {
                self.delegate?.currentUserIsOnSeat()
                // setup default effect
            }
        default:
            break
        }
        if isPublishCameraStream {
            self.muteLocalVideo = false
            self.muteLocalAudio = false
        }
    }
    
    private func _onStopInteraction(interaction: ShowInteractionInfo) {
        ShowLogger.info("_onStopInteraction: \(interaction.userId) \(interaction.userName) status: \(interaction.type.rawValue)")
        switch interaction.type {
        case .pk:
            view.layer.contents = UIImage.show_sceneImage(name: "show_live_room_bg")?.cgImage
            ShowAgoraKitManager.shared.removeRtcDelegate(delegate: self, roomId: interaction.roomId)
            
            ShowAgoraKitManager.shared.updateVideoProfileForMode(.single)
            ShowAgoraKitManager.shared.leaveChannelEx(roomId: self.roomId, channelId: interaction.roomId)
            liveView.canvasView.canvasType = .none
            liveView.canvasView.setRemoteUserInfo(name: interaction.userName)
        case .linking:
            liveView.canvasView.setRemoteUserInfo(name: interaction.userName)
            liveView.canvasView.canvasType = .none
            liveView.bottomBar.linkButton.isSelected = false
//            currentInteraction?.ownerMuteAudio = false
            //TODO: 这个是不是需要真正的角色，放进switchRole里？
            if role == .audience {
                ShowAgoraKitManager.shared.setSuperResolutionOn(true)
            } else {
                ShowAgoraKitManager.shared.updateVideoProfileForMode(.single)
            }
            
            //停止连麦需要修改角色的只有连麦主播，其他用户保持原有角色，有可能观众已经申请连麦改变了角色并推流
            let toRole: AgoraClientRole? = interaction.userId == VLUserCenter.user.id ? .audience : nil
            ShowAgoraKitManager.shared.switchRole(role: toRole,
                                                  channelId: roomId,
                                                  options: self.channelOptions,
                                                  uid: interaction.userId)
            ShowAgoraKitManager.shared.updateLiveView(role: toRole,
                                                      channelId: roomId,
                                                      uid: interaction.userId,
                                                      canvasView: nil)
            self.delegate?.currentUserIsOffSeat()
        default:
            break
        }
        
        if isPublishCameraStream {
            self.muteLocalVideo = false
            self.muteLocalAudio = false
        }
    }
}

extension ShowLiveViewController: AgoraRtcEngineDelegate {
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurWarning warningCode: AgoraWarningCode) {
        ShowLogger.warn("rtcEngine warningCode == \(warningCode.rawValue)")
    }
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOccurError errorCode: AgoraErrorCode) {
        ShowLogger.warn("rtcEngine errorCode == \(errorCode.rawValue)")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        ShowLogger.info("rtcEngine didJoinChannel \(channel) with uid \(uid) elapsed \(elapsed)ms")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinedOfUid uid: UInt, elapsed: Int) {
        ShowLogger.info("rtcEngine didJoinedOfUid \(uid) channelId: \(roomId)", context: kShowLogBaseContext)
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        ShowLogger.info("rtcEngine didOfflineOfUid === \(uid) reason: \(reason.rawValue)")
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, 
                   remoteVideoStateChangedOfUid uid: UInt,
                   state: AgoraVideoRemoteState, 
                   reason: AgoraVideoRemoteReason, elapsed: Int) {
        if uid == roomOwnerId {
            if reason == .remoteMuted , currentInteraction?.type != .pk{
                liveView.blurHostCanvas = true
            }else if reason == .remoteUnmuted {
                liveView.blurHostCanvas = false
            }
        }
    }
    
    public func rtcEngine(_ engine: AgoraRtcEngineKit, didVideoMuted muted: Bool, byUid uid: UInt) {
        ShowLogger.info("didVideoMuted[\(uid)] \(muted)")
        if "\(uid)" == currentInteraction?.userId {
            liveView.blurGusetCanvas = muted
        } else if uid == roomOwnerId {
            liveView.blurHostCanvas = muted
        }
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didAudioMuted muted: Bool, byUid uid: UInt) {
        ShowLogger.info("didAudioMuted[\(uid)] \(muted)")
        if "\(uid)" == currentInteraction?.userId {
            liveView.canvasView.isRemoteMuteMic = muted
        } else if uid == roomOwnerId {
            liveView.canvasView.isLocalMuteMic = muted
        }
    }
    
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
    func onPKDidTimeout() {
        guard let _ = currentInteraction else { return }
        serviceImp?.stopInteraction(roomId: roomId) { _ in
        }
        
        interruptInteractionReason = "show_pk_end_timeout".show_localized
    }
    
    func getPKDuration() -> UInt64 {
        guard let ts = serviceImp?.getCurrentNtpTs(roomId: roomId),
              let interactionTs = currentInteraction?.createdAt,
              interactionTs <= ts else {
            return 0
        }
        return ts - interactionTs
    }
    
    func onClickRemoteCanvas() {
        guard let info = currentInteraction else { return }
        guard isPublishCameraStream else { return }
        
        let toolView = ShowToolMenuView(type: info.type == .linking ? .joint_broadcasting : .end)
        toolView.title = "To audience \(info.userName)"
        toolView.heightAnchor.constraint(equalToConstant: 150 + Screen.safeAreaBottomHeight()).isActive = true
        toolView.onTapItemClosure = { [weak self] type, _ in
            guard let self = self else {return}
            AlertManager.hiddenView()
            switch type {
            case .end_pk:
                self.serviceImp?.stopInteraction(roomId: self.roomId) { _ in
                }
                
            default: break
            }
        }
        AlertManager.show(view: toolView, alertPostion: .bottom)
    }
    
    func onClickSendMsgButton(text: String) {
        sendMessageWithText(text)
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
        self.navigationController?.pushViewController(vc, animated: true)
    }
    
    func onClickMusicButton() { }
    
    func onClickSettingButton() {
        let muteAudio = self.muteLocalAudio
        settingMenuVC.selectedMap = [.camera: self.muteLocalVideo, .mic: muteAudio, .mute_mic: muteAudio]
        
        if interactionStatus == .idle {
            settingMenuVC.type = role == .broadcaster ? .idle_broadcaster : .idle_audience
        }else{
            settingMenuVC.type = role == .broadcaster ? .pking : (currentInteraction?.userId == VLUserCenter.user.id ? .pking : .idle_audience)
            settingMenuVC.menuTitle = currentInteraction?.type == .pk ? "show_setting_menu_on_pk_title".show_localized : "show_setting_menu_on_seat_title".show_localized
        }
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
                if self.role == .broadcaster {
                    self.liveView.blurHostCanvas = true
                } else {
                    self.liveView.blurGusetCanvas = true
                }
            } else {
                ShowAgoraKitManager.shared.engine?.startPreview()
                if self.role == .broadcaster {
                    self.liveView.blurHostCanvas = false
                } else {
                    self.liveView.blurGusetCanvas = false
                }
            }
        }
    }
    
    // End wheat connection
    func onClickEndPkButtonSelected(_ menu:ShowToolMenuViewController, _ selected: Bool) {
        guard let _ = currentInteraction else { return }
        serviceImp?.stopInteraction(roomId: roomId) { _ in
        }
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
                vc.mode = wSelf.interactionStatus == .pk ? .pk : .single
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

