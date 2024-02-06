//
//  ShowLiveView.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/8.
//

import UIKit

private let kTableViewBottomOffset: CGFloat = Screen.safeAreaBottomHeight() + 54
private let kChatInputViewHeight: CGFloat = 56

protocol CommerceRoomLiveViewDelegate: CommerceRoomBottomBarDelegate {
    func onClickSendMsgButton(text: String)
    func onClickCloseButton()
    func onClickMoreButton()
}

class CommerceRoomLiveView: UIView {
    var roomUserCount: Int = 1 {
        didSet {
            countView.count = roomUserCount
        }
    }
    
    var room: CommerceRoomListModel? {
        didSet{
            clearChatModel()
            roomInfoView.setRoomInfo(avatar: room?.ownerAvatar, name: room?.roomName, id: room?.roomId, time: room?.createdAt)
            guard let count = room?.roomUserCount else {
                roomUserCount = 1
                return
            }
            self.roomUserCount = count
        }
    }
    
    weak var delegate: CommerceRoomLiveViewDelegate? {
        didSet{
            bottomBar.delegate = delegate
        }
    }
    lazy var canvasView: CommerceCanvasView = {
        let view = CommerceCanvasView()
        return view
    }()
    
    private var chatArray = [CommerceChatModel]()
    
    private lazy var roomInfoView: CommerceRoomInfoView = {
        let roomInfoView = CommerceRoomInfoView()
        return roomInfoView
    }()
    
    private lazy var countView: CommerceRoomMembersCountView = {
        let countView = CommerceRoomMembersCountView()
        return countView
    }()
    
    private lazy var moreBtn: UIButton = {
        let button = UIButton()
        button.setImage(UIImage.sceneImage(name: "icon_live_more"), for: .normal)
        button.imageView?.contentMode = .scaleAspectFit
        button.addTarget(self, action: #selector(clickMore), for: .touchUpInside)
        return button
    }()
    
    private lazy var closeButton: UIButton = {
        let button = UIButton(type: .custom)
        button.setImage(UIImage.commerce_sceneImage(name: "show_live_close"), for: .normal)
        button.addTarget(self, action: #selector(didClickCloseButton), for: .touchUpInside)
        return button
    }()
    
    private lazy var coverView: UIView = {
        let view = UIView()
        let tap = UITapGestureRecognizer(target: self, action: #selector(didTapedCoverView))
        view.addGestureRecognizer(tap)
        return view
    }()
    
    lazy var bottomBar: CommerceRoomBottomBar = {
        let view = CommerceRoomBottomBar(isBroadcastor: isBroadcastor)
        return view
    }()
    private lazy var tableView: GradualTableView = {
        let tableView = GradualTableView(frame: .zero,
                                         style: .plain,
                                         direction: [.top, .bottom],
                                         gradualValue: 0.3)
        tableView.backgroundColor = .clear
        tableView.separatorStyle = .none
        tableView.delegate = self
        tableView.dataSource = self
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 46
        tableView.showsVerticalScrollIndicator = false
        tableView.transform = CGAffineTransform(rotationAngle: Double.pi)
        return tableView
    }()
    
    private lazy var chatInputView: CommerceChatInputView = {
        let textField = CommerceChatInputView()
        textField.isHidden = true
        textField.delegate = self
        return textField
    }()
    
    private lazy var chatButton: UIButton = {
        let button = UIButton()
        button.setTitle("create_live_chat_title".commerce_localized, for: .normal)
        button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = UIFont.commerce_R_13
        button.layer.cornerRadius = 19
        button.layer.borderWidth = 1
        button.layer.borderColor = UIColor(hex: "#FFFFFF", alpha: 0.6).cgColor
        button.backgroundColor = UIColor(hex: "#000000", alpha: 0.25)
        button.translatesAutoresizingMaskIntoConstraints = false
        button.addTarget(self, action: #selector(didClickChatButton), for: .touchUpInside)
        return button
    }()
    private lazy var upvoteButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage.commerce_sceneImage(name: "commerce_upvote_icon"), for: .normal)
        button.isHidden = isBroadcastor
        button.addTarget(self, action: #selector(onClickUpvoteButton(sender:)), for: .touchUpInside)
        button.translatesAutoresizingMaskIntoConstraints = false
        return button
    }()
    private lazy var userJoinView: CommerceUserJoinView = {
        let view = CommerceUserJoinView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
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
        
        addSubview(closeButton)
        closeButton.snp.makeConstraints { make in
            make.right.equalTo(-15)
            make.centerY.equalTo(roomInfoView)
        }
        
        addSubview(moreBtn)
        moreBtn.snp.makeConstraints { make in
            make.trailing.equalTo(closeButton.snp_leadingMargin).offset(-18)
            make.centerY.equalTo(closeButton.snp.centerY)
            make.width.equalTo(24)
        }
        
        addSubview(countView)
        countView.snp.makeConstraints { make in
            make.centerY.equalTo(roomInfoView)
            make.right.equalTo(moreBtn.snp.left).offset(-10)
        }
        
        addSubview(tableView)
        tableView.snp.makeConstraints { make in
            make.left.equalTo(15)
            make.bottom.equalTo(-kTableViewBottomOffset)
            make.right.equalTo(-70)
            make.height.equalTo(168)
        }
        
        addSubview(chatButton)
        chatButton.snp.makeConstraints { make in
            let bottomOffset = Screen.safeAreaBottomHeight() + 4
            make.left.equalTo(15)
            make.width.equalTo(134)
            make.height.equalTo(38)
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
        
        addSubview(upvoteButton)
        upvoteButton.snp.makeConstraints { make in
            make.trailing.equalTo(bottomBar)
            make.bottom.equalTo(bottomBar.snp.top).offset(-16)
        }
        
        addSubview(userJoinView)
        userJoinView.trailingAnchor.constraint(equalTo: tableView.leadingAnchor, constant: -16).isActive = true
        userJoinView.bottomAnchor.constraint(equalTo: tableView.topAnchor, constant: -5).isActive = true
    }
    
    private func addObserver(){
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: nil) { [weak self] notify in
            guard let self = self else {return}
            guard let keyboardRect = (notify.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue else { return }
            guard let duration = notify.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? TimeInterval else { return }
            let keyboradHeight = keyboardRect.size.height
            let height = Screen.height == self.height ? 0 : Screen.height - self.height
            self.chatInputView.snp.updateConstraints { make in
                make.bottom.equalToSuperview().offset(-(keyboradHeight - height))
            }
            self.tableView.snp.updateConstraints { make in
                make.bottom.equalTo(-kTableViewBottomOffset - (keyboradHeight - height))
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
            guard let duration = notify.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? TimeInterval else { return }
            self.chatInputView.snp.updateConstraints { make in
                make.bottom.equalToSuperview()
            }
            self.tableView.snp.updateConstraints { make in
                make.bottom.equalTo(-kTableViewBottomOffset)
            }
            UIView.animate(withDuration: duration) {
                self.layoutIfNeeded()
            }
            self.coverView.removeFromSuperview()
        }
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
    
    @objc
    private func onClickUpvoteButton(sender: UIButton) {
        showHeartAnimation(at: CGPoint(x: sender.centerX, y: sender.origin.y))
    }

    func showHeartAnimation(at location: CGPoint) {
        var images = ["finger_heart", "thunder", "thumbs_up", "No_of_the_beast", "lips", "heart"].compactMap({ UIImage.commerce_sceneImage(name: "\($0)") })
        let animationLayer = CommerceEmitterLayer.emitterLayer(size: CGSize.init(width: 32, height: 32),
                                                               center: location,
                                                               image: images.randomElement() ?? UIImage())
        animationLayer.cm_delegate = self
        animationLayer.fromAlpha = 1.0
        animationLayer.toAlpha = 0
        animationLayer.fromScale = 1.0
        animationLayer.toScale = 1.5
        animationLayer.roateRange = Double.pi / 4
        animationLayer.startAnimation()
    }
}
extension CommerceRoomLiveView: CommerceEmitterLayerDelegate {
    func displayViewForEmitter() -> UIView {
        self
    }
}

extension CommerceRoomLiveView {
    func addChatModel(_ chatModel: CommerceChatModel) {
        if chatModel.text == "join_live_room".commerce_localized && chatModel.userName != VLUserCenter.user.name {
            userJoinView.joinHandler(nickName: chatModel.userName)
        }
        chatArray.insert(chatModel, at: 0)
        let indexPath = IndexPath(item: chatArray.count - 1, section: 0)
        tableView.insertRows(at: [indexPath], with: .top)
        tableView.reloadData()
    }
    
    func clearChatModel(){
        chatArray.removeAll()
        tableView.reloadData()
        tableView.scrollToTop()
    }
}

extension CommerceRoomLiveView: UITableViewDelegate, UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return chatArray.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cellID = "CommerceRoomChatCell"
        var cell = tableView.dequeueReusableCell(withIdentifier: cellID) as? CommerceRoomChatCell
        if cell == nil {
            cell = CommerceRoomChatCell(style: .default, reuseIdentifier: cellID)
        }
        let chatModel = chatArray[indexPath.row]
        cell?.setUserName(chatModel.userName, msg: chatModel.text)
        return cell!
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let chatModel = chatArray[indexPath.row]
        let cell = tableView.cellForRow(at: indexPath) as? CommerceRoomChatCell
        
        let alertVC = UIAlertController(title: cell?.msgLabel.text, message: nil, preferredStyle: .actionSheet)
        let report = UIAlertAction(title: "Report", style: .default) { _ in
            ToastView.show(text: "Report Susccessful")
        }
        let cancel = UIAlertAction(title: "show_advance_setting_action_sheet_cancel".commerce_localized,
                                   style: .cancel,
                                   handler: nil)
        alertVC.addAction(report)
        alertVC.addAction(cancel)
        UIViewController.cl_topViewController()?.present(alertVC, animated: true)
    }
}

extension CommerceRoomLiveView: CommerceChatInputViewDelegate {
    
    func onEndEditing() {
        chatInputView.isHidden = true
        bottomBar.isHidden = false
        chatButton.isHidden = false
    }

    func onClickEmojiButton() {
        
    }
    
    func onClickSendButton(text: String) {
        if text.count > 80 {
            ToastView.show(text: "show_live_chat_text_length_beyond_bounds".commerce_localized)
            return
        }
        delegate?.onClickSendMsgButton(text: text)
    }
}
