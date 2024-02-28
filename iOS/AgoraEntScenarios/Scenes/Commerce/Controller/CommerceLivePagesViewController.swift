//
//  CommerceLivePagesViewController.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2023/1/13.
//

import Foundation
import UIKit
import VideoLoaderAPI

private let kPagesVCTag = "CommercePagesVC"
class CommerceLivePagesViewController: ViewController {
    var onClickDislikeClosure: (() -> Void)?
    var onClickDisUserClosure: (() -> Void)?
    private lazy var delegateHandler = {
        let localUid = UInt(UserInfo.userId)!
        let handler = CommerceLivePagesSlicingDelegateHandler(localUid: localUid)
        handler.parentVC = self
        handler.vcDelegate = self
        handler.onClickDislikeClosure = onClickDislikeClosure
        handler.onClickDisUserClosure = onClickDisUserClosure
        handler.onRequireRenderVideo = {[weak self] info, cell, indexPath in
            guard let vc = cell.contentView.viewWithTag(kCommerceLiveRoomViewTag)?.next as? CommerceLiveViewController,
                  let room = vc.room,
                  localUid != info.uid else {
                return nil
            }
            commerceLogger.info("[\(room.roomId)]onRequireRenderVideo: \(info.channelName)  \(vc.liveView.canvasView.localView)", context: kPagesVCTag)
            if room.channelName() == info.channelName, room.userId() == "\(info.uid)" {
                return vc.liveView.canvasView.localView
            } else {
                if let _ = room.interactionAnchorInfoList.filter({ $0.uid == info.uid && $0.channelName == info.channelName }).first {
                    return vc.liveView.canvasView.remoteView
                }
                commerceLogger.info("onRequireRenderVideo fail: \(info.channelName)/\(room.roomId)", context: kPagesVCTag)
                return nil
            }
        }
        return handler
    }()
    
    var roomList: [CommerceRoomListModel]? {
        didSet {
            delegateHandler.roomList = CommerceCycleRoomArray(roomList: roomList)
        }
    }
    
    var focusIndex: Int = 0
    
    private var currentVC: CommerceLiveViewController?
    
    let agoraKitManager = CommerceAgoraKitManager.shared
        
    private lazy var collectionView: UICollectionView = {
        let layout = UICollectionViewFlowLayout()
        layout.minimumLineSpacing = 0
        layout.minimumInteritemSpacing = 0
        layout.sectionInset = .zero
        layout.itemSize = self.view.bounds.size
        let collectionView = UICollectionView(frame: self.view.bounds, collectionViewLayout: layout)
        collectionView.register(UICollectionViewCell.self, forCellWithReuseIdentifier: NSStringFromClass(UICollectionViewCell.self))
        collectionView.scrollsToTop = false
        collectionView.delegate = delegateHandler
        collectionView.dataSource = delegateHandler
        collectionView.isPagingEnabled = true
        collectionView.contentInsetAdjustmentBehavior = .never
        collectionView.bounces = false
        collectionView.showsVerticalScrollIndicator = false
        collectionView.backgroundColor = .clear
        return collectionView
    }()
    
    deinit {
        commerceLogger.info("deinit-- CommerceLivePagesViewController", context: kPagesVCTag)
        CommerceAgoraKitManager.shared.leaveAllRoom()
        AppContext.unloadCommerceServiceImp()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        view.layer.contents = UIImage.commerce_sceneImage(name: "show_list_Bg")?.cgImage
        navigationController?.setNavigationBarHidden(true, animated: true)
        view.addSubview(collectionView)
        collectionView.isScrollEnabled = roomList?.count ?? 0 > 1 ? true : false
        let realIndex = (delegateHandler.roomList as? CommerceCycleRoomArray)?.fakeCellIndex(with: focusIndex) ?? focusIndex
        collectionView.scrollToItem(at: IndexPath(row: realIndex, section: 0), at: .centeredVertically, animated: false)
    }
    
    private func addDebugButton(){
        let button = UIButton(type: .custom)
        button.setTitle("Video", for: .normal)
        button.addTarget(self, action: #selector(didClickDebugVideoButton), for: .touchUpInside)
        view.addSubview(button)
        button.snp.makeConstraints { make in
            make.left.top.equalTo(100)
            make.width.height.equalTo(60)
        }
        
        let Audiobutton = UIButton(type: .custom)
        Audiobutton.setTitle("Audio", for: .normal)
        Audiobutton.addTarget(self, action: #selector(didClickDebugAudioButton), for: .touchUpInside)
        view.addSubview(Audiobutton)
        Audiobutton.snp.makeConstraints { make in
            make.centerY.equalTo(button)
            make.left.equalTo(button.snp.right).offset(20)
            make.width.height.equalTo(60)
        }
        
        let superbutton = UIButton(type: .custom)
        superbutton.setTitle("show_super_resolution".commerce_localized, for: .normal)
        superbutton.addTarget(self, action: #selector(didClickDebugSuperButton), for: .touchUpInside)
        view.addSubview(superbutton)
        superbutton.snp.makeConstraints { make in
            make.centerY.equalTo(button)
            make.left.equalTo(Audiobutton.snp.right).offset(20)
            make.width.height.equalTo(60)
        }
    }
    
    @objc private func didClickDebugVideoButton(){
        agoraKitManager.setOffMediaOptionsVideo(roomid: currentVC?.room?.roomId ?? "")
    }
    
    @objc private func didClickDebugAudioButton(){
        agoraKitManager.setOffMediaOptionsAudio()
    }
    
    @objc private func didClickDebugSuperButton(){
        agoraKitManager.setSuperResolutionOn(false)
    }
}


extension CommerceLivePagesViewController {
    var isScrollEnable: Bool {
        set{
            collectionView.isScrollEnabled = newValue
        }
        get{
            return collectionView.isScrollEnabled
        }
    }
}

extension CommerceLivePagesViewController: CommerceLiveViewControllerDelegate {
    
    func interactionDidChange(roomInfo: CommerceRoomListModel) {
        //连麦中一方有自己则不走api
        if roomInfo.anchorInfoList.count == 2,
           roomInfo.anchorInfoList.first?.channelName == roomInfo.anchorInfoList.last?.channelName,
            let _ = roomInfo.anchorInfoList.filter({ return UserInfo.userId == "\($0.uid)" && $0.channelName == roomInfo.channelName()}).first {
            return
        }
        delegateHandler.roomList = delegateHandler.roomList
    }
    
    func currentUserIsOnSeat() {
        isScrollEnable = false
    }
    
    func currentUserIsOffSeat() {
        isScrollEnable = true
    }
}

class CommerceCycleRoomArray: AGRoomArray {
    private var halfCount: Int = 9999999
    fileprivate func fakeCellCount() -> Int {
        return roomList.count > 2 ? roomList.count + halfCount * 2 : roomList.count
    }
    
    required init(roomList: [IVideoLoaderRoomInfo]?) {
        super.init(roomList: roomList)
        let count = max(roomList?.count ?? 0, 1)
        halfCount = (9999999 / count) * count
    }

    fileprivate func realCellIndex(with fakeIndex: Int) -> Int {
        if fakeCellCount() < 3 {
            return fakeIndex
        }

        let realCount = roomList.count
        let offset = halfCount
        var realIndex = fakeIndex + realCount * max(1 + offset / realCount, 2) - offset
        realIndex = realIndex % realCount

        return realIndex
    }

    fileprivate func fakeCellIndex(with realIndex: Int) -> Int {
        if fakeCellCount() < 3 {
            return realIndex
        }

        let offset = halfCount
        let fakeIndex = realIndex + offset

        return fakeIndex
    }
    
    override subscript(index: Int) -> IVideoLoaderRoomInfo? {
        let realIndex = realCellIndex(with: index)
        if realIndex < roomList.count && realIndex >= 0 {
            return roomList[realIndex]
        } else {
            return nil
        }
    }
    
    override func count() -> Int {
        return fakeCellCount()
    }
}


let kCommerceLiveRoomViewTag = 12345
class CommerceLivePagesSlicingDelegateHandler: AGCollectionSlicingDelegateHandler {
    weak var parentVC: UIViewController?
    weak var vcDelegate: CommerceLiveViewControllerDelegate?
    var currentVC: CommerceLiveViewController?
    var onClickDislikeClosure: (() -> Void)?
    var onClickDisUserClosure: (() -> Void)?
    
    override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = super.collectionView(collectionView, cellForItemAt: indexPath)
        let idx = indexPath.row
        defer {
            commerceLogger.info("collectionView cellForItemAt: \(idx)/\(indexPath.row)", context: kPagesVCTag)
        }
        
        guard let room = roomList?[idx] as? CommerceRoomListModel else {
            return cell
        }
        
        if let origVC = cell.contentView.viewWithTag(kCommerceLiveRoomViewTag)?.next as? CommerceLiveViewController {
            origVC.room = room
            return cell
        }
        
        let vc = CommerceLiveViewController()
        vc.room = room
        vc.delegate = vcDelegate
        vc.view.frame = parentVC!.view.bounds
        vc.view.tag = kCommerceLiveRoomViewTag
        vc.loadingType = .joinedWithVideo
        vc.onClickDislikeClosure = onClickDislikeClosure
        vc.onClickDisUserClosure = onClickDisUserClosure
        cell.contentView.addSubview(vc.view)
        parentVC!.addChild(vc)
        return cell
    }
    
    override func collectionView(_ collectionView: UICollectionView, willDisplay cell: UICollectionViewCell, forItemAt indexPath: IndexPath) {
        guard let vc = cell.contentView.viewWithTag(kCommerceLiveRoomViewTag)?.next as? CommerceLiveViewController,
              vc.room?.ownerId != UserInfo.userId else {
            return
        }

        super.collectionView(collectionView, willDisplay: cell, forItemAt: indexPath)
        vc.loadingType = .joinedWithVideo
        currentVC = vc
    }
    
    override func collectionView(_ collectionView: UICollectionView, didEndDisplaying cell: UICollectionViewCell, forItemAt indexPath: IndexPath) {
        guard let vc = cell.contentView.viewWithTag(kCommerceLiveRoomViewTag)?.next as? CommerceLiveViewController,
              vc.room?.ownerId != UserInfo.userId else {
//            assert(false, "room at index \(idx) not found")
            return
        }
        super.collectionView(collectionView, didEndDisplaying: cell, forItemAt: indexPath)
        vc.loadingType = .prejoined
    }
    
    override func scrollViewDidEndDecelerating(_ scrollView: UIScrollView) {
        super.scrollViewDidEndDecelerating(scrollView)
        let currentIndex = Int(scrollView.contentOffset.y / scrollView.height)
        if currentIndex > 0, currentIndex < (roomList?.count() ?? 0) - 1 {return}
        let toIndex = currentIndex
        if let cycleArray = roomList as? CommerceCycleRoomArray {
            let realIndex = cycleArray.realCellIndex(with: toIndex)
            let fakeIndex = cycleArray.fakeCellIndex(with: realIndex)
            commerceLogger.info("scrollViewDidEndDecelerating: from: \(currentIndex) to: \(fakeIndex)", context: kPagesVCTag)
            self.scrollView = nil
            (scrollView as? UICollectionView)?.scrollToItem(at: IndexPath(row: fakeIndex, section: 0),
                                                            at: .centeredVertically,
                                                            animated: false)
        }
    }
}

