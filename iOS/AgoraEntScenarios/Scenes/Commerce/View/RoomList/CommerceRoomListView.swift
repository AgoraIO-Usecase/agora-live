//
//  RoomListView.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/3.
//

import UIKit

class CommerceRoomListView: UIView {
    
    var roomList = [CommerceRoomListModel]() {
        didSet {
            collectionView.reloadData()
            emptyView.isHidden = roomList.count > 0
        }
    }
    
    var clickCreateButtonAction: (()->())?
    var joinRoomAction: ((_ room: CommerceRoomListModel)->())?
    var refreshValueChanged: (()->())?
    
    var collectionView: UICollectionView!
    
    private lazy var refreshControl: UIRefreshControl = {
        let ctrl = UIRefreshControl()
        ctrl.addTarget(self, action: #selector(refreshControlValueChanged), for: .valueChanged)
        return ctrl
    }()
    
    private var emptyView: CommerceEmptyView!

    override init(frame: CGRect) {
        super.init(frame: frame)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        let layout = UICollectionViewFlowLayout()
        layout.sectionInset = UIEdgeInsets(top: 0, left: 20, bottom: 0, right: 20)
        let itemWidth = (Screen.width - 15 - 20 * 2) * 0.5
        layout.itemSize = CGSize(width: itemWidth, height: 234.0 / 160.0 * itemWidth)
        collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        collectionView.backgroundColor = .clear
        collectionView.register(CommerceRoomListCell.self, forCellWithReuseIdentifier: NSStringFromClass(CommerceRoomListCell.self))
        collectionView.delegate = self
        collectionView.dataSource = self
        collectionView.refreshControl = self.refreshControl
        addSubview(collectionView)
        collectionView.snp.makeConstraints { make in
            make.edges.equalTo(UIEdgeInsets(top:  Screen.safeAreaTopHeight() + 54, left: 0, bottom: 0, right: 0))
        }
        
        emptyView = CommerceEmptyView()
        emptyView.isHidden = true
        collectionView.addSubview(emptyView)
        emptyView.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.top.equalTo(156)
        }
        
        let btnHeight: CGFloat = 48
        let createButton = UIButton(type: .custom)
        createButton.setTitleColor(.white, for: .normal)
        createButton.setTitle("room_list_create_room".commerce_localized, for: .normal)
        createButton.setImage(UIImage.commerce_sceneImage(name: "show_create_add"), for: .normal)
        createButton.imageEdgeInsets(UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 5))
        createButton.titleEdgeInsets(UIEdgeInsets(top: 0, left: 5, bottom: 0, right: 0))
        createButton.backgroundColor = .commerce_btn_bg
        createButton.titleLabel?.font = .commerce_btn_title
        createButton.layer.cornerRadius = btnHeight * 0.5
        createButton.layer.masksToBounds = true
        createButton.addTarget(self, action: #selector(didClickCreateButton), for: .touchUpInside)
        addSubview(createButton)
        createButton.snp.makeConstraints { make in
            make.centerX.equalToSuperview()
            make.bottom.equalToSuperview().offset(-max(Screen.safeAreaBottomHeight(), 10))
            make.height.equalTo(btnHeight)
            make.width.equalTo(195)
        }
    }

    @objc private func didClickCreateButton(){
       clickCreateButtonAction?()
    }
    
    @objc private func refreshControlValueChanged() {
        refreshValueChanged?()
    }
    
    func beginRefreshing(){
        refreshControl.beginRefreshing()
    }
    
    func endRefrshing(){
        refreshControl.endRefreshing()
    }
}

 
extension CommerceRoomListView: UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {
    
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
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        let room = roomList[indexPath.item]
        joinRoomAction?(room)
    }
    
}
