//
//  ShowAdvancedSettingVC.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2022/11/14.
//

import UIKit
import AgoraRtcKit
import AgoraCommon

class CommerceAdvancedSettingVC: UIViewController, UIGestureRecognizerDelegate {
    
    var mode: ShowMode?
    var isBroadcaster = true
    var currentChannelId: String?

    private let naviBar = CommerceNavigationBar()
    
    var musicManager: CommerceMusicPresenter!

    private let titles = ["show_advance_setting_video_title".commerce_localized,
                          "show_advance_setting_audio_title".commerce_localized]
    
    private lazy var videoSettingVC: CommerceVideoSettingVC? = {
        return createSettingVCForIndex(0)
    }()
    
    private lazy var audioSettingVC: CommerceVideoSettingVC? = {
        return createSettingVCForIndex(1)
    }()
    
    private lazy var indicator: UIView = {
        let indicator = UIView()
        indicator.size = CGSize(width: 66, height: 2)
        indicator.backgroundColor = .commerce_zi03
        return indicator
    }()
     
    private lazy var segmentedView: CommerceAEACategoryView = {
        let layout = CommerceAEACategoryViewLayout()
        layout.itemSize = CGSize(width: Screen.width * 0.5, height: 40)
        let segmentedView = CommerceAEACategoryView(defaultLayout: layout)
        segmentedView.titles = titles
        segmentedView.delegate = self
        segmentedView.titleFont = .commerce_R_14
        segmentedView.titleSelectedFont = .commerce_navi_title
        segmentedView.titleColor = .commerce_Ellipse5
        segmentedView.titleSelectedColor = .commerce_Ellipse7
        segmentedView.backgroundColor = .clear
        segmentedView.defaultSelectedIndex = 0
        segmentedView.indicator = indicator
        return segmentedView
    }()
    
    private lazy var listContainerView: CommerceAEAListContainerView = {
        let containerView = CommerceAEAListContainerView()
        containerView.dataSource = self
        containerView.setSelectedIndex(0)
        return containerView
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setUpUI()
        navigationController?.interactivePopGestureRecognizer?.delegate = self
        navigationController?.interactivePopGestureRecognizer?.isEnabled = true
    }
    
    private func setUpUI() {
        view.backgroundColor = .white
        
        naviBar.title = "show_advanced_setting_title".commerce_localized
        view.addSubview(naviBar)
        
        view.addSubview(segmentedView)
        segmentedView.snp.makeConstraints { make in
            make.left.right.equalToSuperview()
            make.top.equalTo(naviBar.snp.bottom)
        }
        segmentedView.isHidden = !isBroadcaster
        
        view.addSubview(listContainerView)
        listContainerView.snp.makeConstraints { make in
            make.left.right.equalToSuperview()
            if self.isBroadcaster {
                make.top.equalTo(segmentedView.snp.bottom).offset(10)
            }else{
                make.top.equalTo(naviBar.snp.bottom).offset(10)
            }
            make.bottom.equalToSuperview()
        }
    }
    
    private func createSettingVCForIndex(_ index: Int) -> CommerceVideoSettingVC? {
        let insideSettings: [CommerceSettingKey] = [
            .CodecType,
            .colorEnhance,
            .lowlightEnhance,
            .videoDenoiser,
            .PVC,
            .videoEncodeSize,
            .FPS,
            .videoBitRate
        ]
        let broadcasterVideoSettings: [CommerceSettingKey] = insideSettings
        let audienceVideoSettings: [CommerceSettingKey] = [
            .SR
        ]
        
        let audioSettings: [CommerceSettingKey]  = [
            .earmonitoring,
            .recordingSignalVolume,
            .musicVolume,
        ]
        let settings = isBroadcaster ? [broadcasterVideoSettings, audioSettings] : [audienceVideoSettings]
        if settings.count <= index {
            return nil
        }
        
        let vc = CommerceVideoSettingVC()
        vc.musicManager = musicManager
        vc.currentChannelId = currentChannelId
        vc.dataArray = settings[index]
        return vc
    }
}

extension CommerceAdvancedSettingVC: CommerceAEAListContainerViewDataSource{
    func listContainerView(_ listContainerView: CommerceAEAListContainerView, viewControllerForIndex index: Int) -> UIViewController? {
        if index == 0 {
            return videoSettingVC ?? UIViewController()
        }
        return audioSettingVC ?? UIViewController()
    }
}

extension CommerceAdvancedSettingVC: CommerceAEACategoryViewDelegate {
    func categoryView(_ categoryView: CommerceAEACategoryView, didSelectItemat index: Int) {
        listContainerView.setSelectedIndex(index)
    }
}
