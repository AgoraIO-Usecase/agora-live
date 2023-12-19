//
//  ShowRoomListViewController.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/1.
//

import UIKit
import VideoLoaderAPI

class CommerceRoomListVC: UIViewController {
    
    let backgroundView = UIImageView()
    private var preloadRoom: CommerceRoomListModel?
    
    private let collectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.sectionInset = UIEdgeInsets(top: 0, left: 20, bottom: 0, right: 20)
        let itemWidth = (Screen.width - 15 - 20 * 2) * 0.5
        layout.itemSize = CGSize(width: itemWidth, height: 234.0 / 160.0 * itemWidth)
        return UICollectionView(frame: .zero, collectionViewLayout: layout)
    }()
        
    private lazy var refreshControl: UIRefreshControl = {
        let ctrl = UIRefreshControl()
        ctrl.addTarget(self, action: #selector(refreshControlValueChanged), for: .valueChanged)
        return ctrl
    }()
    
    private lazy var emptyView = CommerceEmptyView()
    
    private lazy var createButton = UIButton(type: .custom)    
    
    private var roomList = [CommerceRoomListModel]() {
        didSet {
            collectionView.reloadData()
            emptyView.isHidden = roomList.count > 0
        }
    }
    
    private lazy var naviBar = ShowNavigationBar()
    
    private var needUpdateAudiencePresetType = false
    private var isHasToken: Bool = true
    
    deinit {
        AppContext.unloadShowServiceImp()
        CommerceAgoraKitManager.shared.destoryEngine()
        commerceLogger.info("deinit-- ShowRoomListVC")
    }
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        hidesBottomBarWhenPushed = true
        commerceLogger.info("init-- ShowRoomListVC")
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        AppContext.shared.sceneImageBundleName = "CommerceResource"
        createViews()
        createConstrains()
        CommerceAgoraKitManager.shared.prepareEngine()
        CommerceRobotService.shared.startCloudPlayers()
        preGenerateToken()
        checkDevice()
        fetchRoomList()
    }
    
    @objc private func didClickCreateButton(){
        if let token = AppContext.shared.rtcToken, token.count > 0 {
            let preVC = CommerceCreateLiveVC()
            let preNC = UINavigationController(rootViewController: preVC)
            preNC.navigationBar.setBackgroundImage(UIImage(), for: .default)
            preNC.modalPresentationStyle = .fullScreen
            present(preNC, animated: true)
            isHasToken = true
            
        } else {
            guard isHasToken == true else { return }
            generateToken { [weak self] in
                self?.didClickCreateButton()
            }
            isHasToken = false
        }
    }
    
    @objc private func refreshControlValueChanged() {
        self.fetchRoomList()
    }
    
    private func checkDevice() {
         let score = CommerceAgoraKitManager.shared.engine?.queryDeviceScore() ?? 0
        if (score < 85) {// (0, 85)
            CommerceAgoraKitManager.shared.deviceLevel = .low
        } else if (score < 90) {// [85, 90)
            CommerceAgoraKitManager.shared.deviceLevel = .medium
        } else {// (> 90)
            CommerceAgoraKitManager.shared.deviceLevel = .high
        }
        CommerceAgoraKitManager.shared.deviceScore = Int(score)
    }
    
    private func joinRoom(_ room: CommerceRoomListModel){
        CommerceAgoraKitManager.shared.callTimestampStart()
        CommerceAgoraKitManager.shared.setupAudienceProfile()
        CommerceAgoraKitManager.shared.updateLoadingType(roomId: room.roomId, channelId: room.roomId, playState: .joined)
        
        if room.ownerId == VLUserCenter.user.id {
            ToastView.show(text: "show_join_own_room_error".commerce_localized)
        } else {
            let vc = CommerceLivePagesViewController()
            let nc = UINavigationController(rootViewController: vc)
            nc.modalPresentationStyle = .fullScreen
            vc.roomList = roomList.filter({ $0.ownerId != VLUserCenter.user.id })
            vc.focusIndex = vc.roomList?.firstIndex(where: { $0.roomId == room.roomId }) ?? 0
            vc.onClickDislikeClosure = { [weak self] in
                guard let self = self else { return }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15, execute: DispatchWorkItem(block: {
                    self.refreshControl.beginRefreshing()
                    self.collectionView.setContentOffset(CGPoint(x: 0, y: -self.refreshControl.frame.size.height), animated: true)
                    self.fetchRoomList()
                }))
            }
            present(nc, animated: true)
        }
    }
    
    private func fetchRoomList() {
        AppContext.commerceServiceImp("")?.getRoomList(page: 1) { [weak self] error, roomList in
            self?.refreshControl.endRefreshing()
            guard let self = self else { return }
            if let error = error {
                return
            }
            let list = roomList ?? []
            let dislikeRooms = AppContext.shared.dislikeRooms()
            
            self.roomList = list.filter({ !dislikeRooms.contains($0.roomId) })
            self.preLoadVisibleItems()
        }
    }
    
    private func preLoadVisibleItems() {
        guard let token = AppContext.shared.rtcToken, token.count > 0, roomList.count > 0 else {
            return
        }
        let firstItem = collectionView.indexPathsForVisibleItems.first?.item ?? 0
        let start = firstItem - 7 < 0 ? 0 : firstItem - 7
        let end = start + 19 >= roomList.count ? roomList.count - 1 : start + 19
        var preloadRoomList: [RoomInfo] = []
        for i in start...end {
            let room = roomList[i]
            let preloadItem = RoomInfo()
            preloadItem.channelName = room.roomId
            preloadItem.uid = UInt(VLUserCenter.user.id) ?? 0
            preloadItem.token = token
            preloadRoomList.append(preloadItem)
        }
        CommerceAgoraKitManager.shared.preloadRoom(preloadRoomList: preloadRoomList)
    }

    private func preGenerateToken() {
        AppContext.shared.rtcToken = nil
        generateToken { [weak self] in
            self?.preLoadVisibleItems()
        }
    }
    
    private func generateToken(completion: (() -> Void)?) {
        NetworkManager.shared.generateToken(
            channelName: "",
            uid: "\(UserInfo.userId)",
            tokenType: .token007,
            type: .rtc,
            expire: 24 * 60 * 60
        ) { token in
            guard let rtcToken = token else {
                return
            }
            AppContext.shared.rtcToken = rtcToken
            completion?()
        }
    }
}
// MARK: - UICollectionView Call Back
extension CommerceRoomListVC: UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {
    
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return roomList.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell: CommerceRoomListCell = collectionView.dequeueReusableCell(withReuseIdentifier: NSStringFromClass(CommerceRoomListCell.self),
                                                                            for: indexPath) as! CommerceRoomListCell
        let room = roomList[indexPath.item]
        cell.setBgImge((room.thumbnailId?.isEmpty ?? true) ? "0" : room.thumbnailId ?? "0",
                       name: room.roomName,
                       id: room.roomId,
                       count: room.roomUserCount)
        return cell
    }
    
    
    func collectionView(_ collectionView: UICollectionView, didHighlightItemAt indexPath: IndexPath) {
        commerceLogger.info("didHighlightItemAt: \(indexPath.row)", context: "collectionView")
        let room = roomList[indexPath.item]
        if let token = AppContext.shared.rtcToken, token.count > 0 {
            CommerceAgoraKitManager.shared.updateLoadingType(roomId: room.roomId, channelId: room.roomId, playState: .prejoined)
            preloadRoom = room
        } else { // fetch token when token is not exist
            generateToken(completion: nil)
        }
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        commerceLogger.info("didSelectItemAt: \(indexPath.row)", context: "collectionView")
        if let token = AppContext.shared.rtcToken, token.count > 0 {
            let room = roomList[indexPath.item]
            joinRoom(room)
        } else {
            ToastView.show(text: "Token is not exit, try again!")
        }
    }
    
    func scrollViewDidEndScrollingAnimation(_ scrollView: UIScrollView) {
        preLoadVisibleItems()
    }
    
    func scrollViewWillBeginDragging(_ scrollView: UIScrollView) {
//        commerceLogger.info("scrollViewWillBeginDragging", context: "collectionView")
        guard let room = preloadRoom else {return}
        CommerceAgoraKitManager.shared.updateLoadingType(roomId: room.roomId, channelId: room.roomId, playState: .idle)
    }
}

// MARK: - Creations
extension CommerceRoomListVC {
    private func createViews(){
        backgroundView.image = UIImage.commerce_sceneImage(name: "show_list_Bg")
        view.addSubview(backgroundView)
        
        collectionView.backgroundColor = .clear
        collectionView.register(CommerceRoomListCell.self, forCellWithReuseIdentifier: NSStringFromClass(CommerceRoomListCell.self))
        collectionView.delegate = self
        collectionView.dataSource = self
        collectionView.refreshControl = self.refreshControl
        view.addSubview(collectionView)
        
        emptyView.isHidden = true
        collectionView.addSubview(emptyView)
        
        createButton.setTitleColor(.white, for: .normal)
        createButton.setTitle("room_list_create_room".commerce_localized, for: .normal)
        createButton.setImage(UIImage.commerce_sceneImage(name: "show_create_add"), for: .normal)
        createButton.imageEdgeInsets(UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 5))
        createButton.titleEdgeInsets(UIEdgeInsets(top: 0, left: 5, bottom: 0, right: 0))
        createButton.backgroundColor = .commerce_btn_bg
        createButton.titleLabel?.font = .commerce_btn_title
        createButton.layer.cornerRadius = 48 * 0.5
        createButton.layer.masksToBounds = true
        createButton.addTarget(self, action: #selector(didClickCreateButton), for: .touchUpInside)
        view.addSubview(createButton)
        
        navigationController?.isNavigationBarHidden = true
        naviBar.title = "navi_title_show_live".commerce_localized
        view.addSubview(naviBar)
    }
    
    func createConstrains() {
        backgroundView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
        }
        collectionView.snp.makeConstraints { make in
            make.edges.equalTo(UIEdgeInsets(top:  Screen.safeAreaTopHeight() + 54, left: 0, bottom: 0, right: 0))
        }
        emptyView.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(156)
        }
        createButton.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.bottom.equalToSuperview().offset(-max(Screen.safeAreaBottomHeight(), 10))
            make.height.equalTo(48)
            make.width.equalTo(195)
        }
    }
}
