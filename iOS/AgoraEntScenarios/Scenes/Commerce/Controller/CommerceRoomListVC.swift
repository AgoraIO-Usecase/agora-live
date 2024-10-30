//
//  ShowRoomListViewController.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/1.
//

import UIKit
import VideoLoaderAPI
import MJRefresh
import AgoraCommon

class CommerceRoomListVC: UIViewController {
    
    let backgroundView = UIImageView()
    private lazy var delegateHandler = {
        let handler = CommerceCollectionLoadingDelegateHandler(localUid: UInt(UserInfo.userId)!)
        return handler
    }()
    
    private var preloadRoom: CommerceRoomListModel?
    
    private let collectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.sectionInset = UIEdgeInsets(top: 0, left: 20, bottom: 0, right: 20)
        let itemWidth = (Screen.width - 15 - 20 * 2) * 0.5
        layout.itemSize = CGSize(width: itemWidth, height: 234.0 / 160.0 * itemWidth)
        return UICollectionView(frame: .zero, collectionViewLayout: layout)
    }()
    
    private lazy var emptyView = CommerceEmptyView()
    
    private lazy var createButton = UIButton(type: .custom)    
    
    private var roomList = [CommerceRoomListModel]() {
        didSet {
            collectionView.reloadData()
            emptyView.isHidden = roomList.count > 0
        }
    }
    
    private lazy var naviBar:CommerceNavigationBar = {
        let bar = CommerceNavigationBar()
        bar.didCloseClosure = {
            RTMSyncUtil.destroy()
        }
        return bar
    }()
    
    private var needUpdateAudiencePresetType = false
    private var isHasToken: Bool = true
    
    deinit {
        AppContext.unloadCommerceServiceImp()
        CommerceAgoraKitManager.shared.destoryEngine()
        commercePrintLog("deinit-- CommerceRoomListVC")
    }
    
    override init(nibName nibNameOrNil: String?, bundle nibBundleOrNil: Bundle?) {
        AppContext.shared.sceneLocalizeBundleName = "CommerceResource"
        super.init(nibName: nibNameOrNil, bundle: nibBundleOrNil)
        hidesBottomBarWhenPushed = true
        commercePrintLog("init-- CommerceRoomListVC")
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        AppContext.shared.sceneImageBundleName = "CommerceResource"
        RTMSyncUtil.initRTMSyncManager()
        createViews()
        createConstrains()
        CommerceAgoraKitManager.shared.prepareEngine()
        CommerceRobotService.shared.startCloudPlayers()
        preGenerateToken()
        checkDevice()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        guard roomList.isEmpty else { return }
        self.collectionView.mj_header?.beginRefreshing()
    }
    
    @objc private func didClickCreateButton(){
        if AppContext.shared.commerceRtcToken == nil {
            preGenerateToken()
        }
        let preVC = CommerceCreateLiveVC()
        let preNC = UINavigationController(rootViewController: preVC)
        preNC.navigationBar.setBackgroundImage(UIImage(), for: .default)
        preNC.modalPresentationStyle = .fullScreen
        present(preNC, animated: true)
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
        CommerceAgoraKitManager.shared.updateLoadingType(roomId: room.roomId, channelId: room.roomId, playState: .joinedWithAudioVideo)
        
        if room.ownerId == VLUserCenter.user.id {
            ToastView.show(text: "show_join_own_room_error".commerce_localized)
            RTMSyncUtil.leaveScene(roomId: room.roomId)
            fetchRoomList()
        } else {
            let vc = CommerceLivePagesViewController()
            let nc = UINavigationController(rootViewController: vc)
            nc.modalPresentationStyle = .fullScreen
            vc.onClickDislikeClosure = { [weak self] in
                guard let self = self else { return }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15, execute: DispatchWorkItem(block: {
                    self.fetchRoomList()
                }))
            }
            vc.onClickDisUserClosure = { [weak self] in
                guard let self = self else { return }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15, execute: DispatchWorkItem(block: {
                    self.fetchRoomList()
                }))
            }
            vc.roomList = roomList.filter({ $0.ownerId != VLUserCenter.user.id })
            vc.focusIndex = vc.roomList?.firstIndex(where: { $0.roomId == room.roomId }) ?? 0
            present(nc, animated: true)
        }
    }
    
    private func fetchRoomList() {
        AppContext.commerceServiceImp("")?.getRoomList(page: 1) { [weak self] error, roomList in
            guard let self = self else { return }
            self.collectionView.mj_header?.endRefreshing()
            if let _ = error {
                return
            }
            let list = roomList ?? []
            let dislikeRooms = AppContext.shared.dislikeRooms()
            let dislikeUsers = AppContext.shared.dislikeUsers()
            self.roomList = list.filter({ !dislikeRooms.contains($0.roomId) })
            self.roomList = self.roomList.filter { !dislikeUsers.contains($0.ownerId) }
        }
    }

    private func preGenerateToken() {
        CommerceAgoraKitManager.shared.preGenerateToken { [weak self] in
            guard let self = self,
                  let _ = AppContext.shared.commerceRtcToken,
                  let _ = AppContext.shared.commerceRtmToken else { return }
            self.delegateHandler.preLoadVisibleItems(scrollView: self.collectionView)
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
        cell.ag_addPreloadTap(roomInfo: room, localUid: delegateHandler.localUid) {[weak self] state in
            if AppContext.shared.commerceRtcToken?.count ?? 0 == 0 {
                if state == .began {
                    self?.preGenerateToken()
                } else if state == .ended {
                    ToastView.show(text: "show_token_is_empty".commerce_localized)
                }
                return false
            }
            
            return true
        } onRequireRenderVideo: { info, canvas in
            return nil
        } completion: { [weak self] in
            self?.joinRoom(room)
        }

        return cell
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
        view.addSubview(collectionView)
        collectionView.mj_header = MJRefreshNormalHeader.init {
            self.fetchRoomList()
        }
        
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

class CommerceCollectionLoadingDelegateHandler: AGCollectionLoadingDelegateHandler {
    var didSelected: ((CommerceRoomListModel) -> Void)?
    
    open func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let item = roomList?[indexPath.row] as? CommerceRoomListModel else {return}
        didSelected?(item)
    }
}
