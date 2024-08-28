//
//  ShowLiveView.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/8.
//

import UIKit
import AgoraCommon

private let kTableViewBottomOffset: CGFloat = Screen.safeAreaBottomHeight() + 109
private let kChatInputViewHeight: CGFloat = 56

protocol ShowRoomLiveViewDelegate: ShowRoomBottomBarDelegate, ShowCanvasViewDelegate {
    func onClickSendMsgButton(text: String)
    func onClickCloseButton()
    func onClickMoreButton()
}

class ShowRoomLiveView: UIView {
    var roomUserCount: Int = 1 {
        didSet {
            countView.count = roomUserCount
        }
    }
    
    var room: ShowRoomListModel? {
        didSet{
            roomInfoView.setRoomInfo(avatar: room?.ownerAvatar, name: room?.roomName, id: room?.roomId, time: room?.createdAt)
            guard let count = room?.roomUserCount else {
                roomUserCount = 1
                return
            }
            self.roomUserCount = count
        }
    }
    
    weak var delegate: ShowRoomLiveViewDelegate? {
        didSet{
            bottomBar.delegate = delegate
            canvasView.delegate = delegate
        }
    }
    lazy var canvasView: ShowCanvasView = {
        let view = ShowCanvasView()
        return view
    }()
    
    var blurHostCanvas = false {
        didSet{
            canvasView.localView.showBlurView = blurHostCanvas
        }
    }
    
    var blurGusetCanvas = false {
        didSet{
            canvasView.remoteView.showBlurView = blurGusetCanvas
        }
    }
        
    private lazy var roomInfoView: ShowRoomInfoView = {
        let roomInfoView = ShowRoomInfoView()
        return roomInfoView
    }()
    
    private lazy var countView: ShowRoomMembersCountView = {
        let countView = ShowRoomMembersCountView()
        return countView
    }()
    private lazy var stackView: UIStackView = {
        let stackView = UIStackView()
        stackView.spacing = 10
        stackView.axis = .horizontal
        stackView.alignment = .fill
        stackView.distribution = .fill
        stackView.translatesAutoresizingMaskIntoConstraints = false
        return stackView
    }()
    private lazy var moreBtn: UIButton = {
        let button = UIButton()
        button.setImage(UIImage.show_sceneImage(name: "icon_live_more"), for: .normal)
        button.imageView?.contentMode = .scaleAspectFit
        button.addTarget(self, action: #selector(clickMore), for: .touchUpInside)
        return button
    }()
    
    private lazy var closeButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.show_sceneImage(name: "show_live_close"), for: .normal)
        button.addTarget(self, action: #selector(didClickCloseButton), for: .touchUpInside)
        return button
    }()
    
    private lazy var coverView: UIView = {
        let view = UIView()
        let tap = UITapGestureRecognizer(target: self, action: #selector(didTapedCoverView))
        view.addGestureRecognizer(tap)
        return view
    }()
    
    lazy var bottomBar: ShowRoomBottomBar = {
        let view = ShowRoomBottomBar(isBroadcastor: isBroadcastor)
        return view
    }()
    
    private lazy var chatInputView: ShowChatInputView = {
        let textField = ShowChatInputView()
        textField.isHidden = true
        textField.delegate = self
        return textField
    }()
    
    private lazy var chatButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setBackgroundImage(UIImage.show_sceneImage(name: "show_live_chat"), for: .normal)
        button.setTitle("create_live_chat_title".show_localized, for: .normal)
        button.titleLabel?.font = UIFont.show_R_12
        button.addTarget(self, action: #selector(didClickChatButton), for: .touchUpInside)
        return button
    }()
    
    private var isBroadcastor = false
    
    init(isBroadcastor: Bool = false) {
        super.init(frame: .zero)
        self.isBroadcastor = isBroadcastor
        createSubviews()
        addObserver()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        
        addSubview(canvasView)
        canvasView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        
        addSubview(roomInfoView)
        roomInfoView.snp.makeConstraints { make in
            let top = Screen.safeAreaTopHeight()
            make.top.equalTo(max(top, 20))
            make.left.equalTo(15)
        }
        
        addSubview(stackView)
        stackView.snp.makeConstraints { make in
            make.right.equalToSuperview().offset(-15)
            make.centerY.equalTo(roomInfoView)
        }
        stackView.addArrangedSubview(countView)
        stackView.addArrangedSubview(moreBtn)
        stackView.addArrangedSubview(closeButton)
        moreBtn.isHidden = isBroadcastor
        moreBtn.snp.makeConstraints { make in
            make.width.equalTo(20)
        }
    
        chatButton.isHidden = true
        addSubview(chatButton)
        chatButton.snp.makeConstraints { make in
            let bottomOffset = Screen.safeAreaBottomHeight() + 4
            make.left.equalTo(15)
            make.bottom.equalTo(-max(10, bottomOffset))
        }
        
        addSubview(bottomBar)
        bottomBar.snp.makeConstraints { make in
            make.centerY.equalTo(chatButton)
            make.right.equalTo(-15)
        }
        
        addSubview(chatInputView)
        chatInputView.snp.makeConstraints { make in
            make.left.right.equalToSuperview()
            make.height.equalTo(kChatInputViewHeight)
            make.bottom.equalToSuperview()
        }
    }
    
    private func addObserver(){
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: nil) { [weak self] notify in
            guard let self = self else {return}
            guard let keyboardRect = (notify.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue else { return }
            guard let duration = notify.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? TimeInterval else { return }
            let keyboradHeight = keyboardRect.size.height
            self.chatInputView.snp.updateConstraints { make in
                make.bottom.equalToSuperview().offset(-keyboradHeight)
            }
            
            UIView.animate(withDuration: duration) {
                self.layoutIfNeeded()
            }
            
            self.addSubview(self.coverView)
            self.coverView.snp.makeConstraints { make in
                make.left.right.top.equalToSuperview()
                make.bottom.equalTo(-keyboradHeight-kChatInputViewHeight)
            }
        }
        
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: nil) {[weak self] notify in
            guard let self = self else {return}
            self.chatInputView.snp.updateConstraints { make in
                make.bottom.equalToSuperview()
            }
           
            self.coverView.removeFromSuperview()
        }
    }
    
    func markExpired() {
        roomInfoView.stopTimer()
    }
    
    @objc private func didTapedCoverView(){
        chatInputView.textField.resignFirstResponder()
    }
    
    @objc private func didClickChatButton() {
        chatInputView.isHidden = false
        bottomBar.isHidden = true
        chatButton.isHidden = true
        chatInputView.textField.becomeFirstResponder()
    }
    
    @objc private func didClickCloseButton() {
        delegate?.onClickCloseButton()
    }
    
    @objc
    private func clickMore() {
        delegate?.onClickMoreButton()
    }
    
    private func sendMessage(){
        
    }

}

extension ShowRoomLiveView: ShowChatInputViewDelegate {
    
    func onEndEditing() {
        chatInputView.isHidden = true
        bottomBar.isHidden = false
        chatButton.isHidden = false
    }

    func onClickEmojiButton() {
        
    }
    
    func onClickSendButton(text: String) {
        if text.count > 80 {
            ToastView.show(text: "show_live_chat_text_length_beyond_bounds".show_localized)
            return
        }
        delegate?.onClickSendMsgButton(text: text)
    }
    
}
