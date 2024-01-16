//
//  ShowDebugSettingVC.swift
//  AgoraEntScenarios
//
//  Created by FanPengpeng on 2023/2/10.
//

import UIKit

private let SwitchCellID = "SwitchCellID"
private let SliderCellID = "SliderCellID"
private let LabelCellID = "LabelCellID"
private let Debug1TFCellID = "Debug1TFCellID"
private let Debug2TFCellID = "Debug2TFCellID"

class CommerceDebugSettingVC: UIViewController {
    
    var isBroadcastor = true
    
    private let transDelegate = CommercePresentTransitioningDelegate()
    private lazy var dataArray: [Any] = {
        isBroadcastor ? createBroadcastorDataArray() : createAudienceDataArray()
    }()
    
    private let naviBar = CommerceNavigationBar()
    
    private lazy var tableView: UITableView = {
        let tableView = UITableView()
        tableView.delegate = self
        tableView.dataSource = self
        tableView.allowsSelection = false
        tableView.rowHeight = 47
        tableView.separatorStyle = .none
        tableView.registerCell(CommerceSettingSwitchCell.self, forCellReuseIdentifier: SwitchCellID)
        tableView.registerCell(CommerceSettingSliderCell.self, forCellReuseIdentifier: SliderCellID)
        tableView.registerCell(CommerceSettingLabelCell.self, forCellReuseIdentifier: LabelCellID)
        tableView.registerCell(CommerceDebugSetting1TFCell.self, forCellReuseIdentifier: Debug1TFCellID)
        tableView.registerCell(CommerceDebugSetting2TFCell.self, forCellReuseIdentifier: Debug2TFCellID)
        return tableView
    }()
    
    private lazy var coverView: UIView = {
        let view = UIView()
        let tap = UITapGestureRecognizer(target: self, action: #selector(didTapedCoverView))
        view.addGestureRecognizer(tap)
        return view
    }()

    override func viewDidLoad() {
        super.viewDidLoad()
        configCustomNaviBar()
        setUpUI()
        addObserver()
    }
    
    private func configCustomNaviBar(){
        naviBar.title = "Developer mode Settings"
        naviBar.backgroundColor = .white
        view.addSubview(naviBar)
        
        let saveButtonItem = CommerceBarButtonItem(title: "show_advanced_setting_debugsetting_private".commerce_localized, target: self, action: #selector(didClickSaveButton))
        naviBar.rightItems = [saveButtonItem]
    }
    
    @objc private func didClickSaveButton() {
        let vc = CommerceDebugPrivateParamsVC()
        self.present(vc, animated: true)
    }
    
    private func setUpUI(){
        view.backgroundColor = .white
        view.addSubview(tableView)
        tableView.snp.makeConstraints { make in
            make.left.right.bottom.equalToSuperview()
            make.top.equalTo(naviBar.snp.bottom)
        }
    }
    
    private func createBroadcastorDataArray() -> [Any] {
        let settingManager = CommerceAgoraKitManager.shared
        return [
            settingManager.debug1TFModelForKey(.encodeFrameRate),
            settingManager.debug2TFModelForKey(.encodeVideoSize),
            settingManager.debug1TFModelForKey(.bitRate),
            CommerceDebugSettingKey.debugPVC,
            CommerceDebugSettingKey.focusFace,
            settingManager.debug2TFModelForKey(.exposureRange),
            settingManager.debug2TFModelForKey(.colorSpace), 
            CommerceDebugSettingKey.encode,
            CommerceDebugSettingKey.codeCType,
            CommerceDebugSettingKey.mirror,
            CommerceDebugSettingKey.renderMode,
            CommerceDebugSettingKey.colorEnhance,
            CommerceDebugSettingKey.lowlightEnhance,
            CommerceDebugSettingKey.videoDenoiser,
        ]
    }
    
    private func createAudienceDataArray() -> [Any] {
        [
            CommerceDebugSettingKey.debugSR,
            CommerceDebugSettingKey.debugSrType
        ]
    }
}
// MARK: - TableView Call Back
extension CommerceDebugSettingVC: UITableViewDelegate, UITableViewDataSource {
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return dataArray.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let model = dataArray[indexPath.row]
        if let tf1Model = model as? CommerceDebug1TFModel {
            let cell = tableView.dequeueReusableCell(withIdentifier: Debug1TFCellID, for: indexPath) as! CommerceDebugSetting1TFCell
            cell.setTitle(tf1Model.title, value: tf1Model.tfText, unit: tf1Model.unitText) { textField in
                tf1Model.tfText = textField.text
                CommerceAgoraKitManager.shared.updateDebugProfileFor1TFMode(tf1Model)
            } beginEditing: {
                tableView.scrollToRow(at: indexPath, at: .top, animated: true)
            }
            return cell
        }
        
        if let tf2Model = model as? CommerceDebug2TFModel {
            let cell = tableView.dequeueReusableCell(withIdentifier: Debug2TFCellID, for: indexPath) as! CommerceDebugSetting2TFCell
            cell.setTitle(tf2Model.title, value1: tf2Model.tf1Text, value2: tf2Model.tf2Text, separator: tf2Model.separatorText) { textField in
                tf2Model.tf1Text = textField.text
                CommerceAgoraKitManager.shared.updateDebugProfileFor2TFModel(tf2Model)
            } tf2DidEndEditing: { textField in
                tf2Model.tf2Text = textField.text
                CommerceAgoraKitManager.shared.updateDebugProfileFor2TFModel(tf2Model)
            } beginEditing: {
                tableView.scrollToRow(at: indexPath, at: .top, animated: true)
            }
            return cell
        }
        
        let data: CommerceDebugSettingKey = dataArray[indexPath.row] as! CommerceDebugSettingKey
        var cell: UITableViewCell!
        if data.type == .aSwitch {
            let cell = tableView.dequeueReusableCell(withIdentifier: SwitchCellID, for: indexPath) as! CommerceSettingSwitchCell
            cell.setTitle(data.title,enable:true, isOn: data.boolValue) {[weak self] isOn in
                self?.changeValue(isOn, forSettingKey: data)
            } detailButtonAction: {[weak self] in
                self?.commerce_showAlert(title: data.title, message: data.tips, confirmTitle: "OK", cancelTitle: nil)
            }
            return cell
        }else if data.type == .label {
            let cell = tableView.dequeueReusableCell(withIdentifier: LabelCellID, for: indexPath) as! CommerceSettingLabelCell
            let index = data.intValue
            let value = data.items[index]
            cell.setTitle(data.title, value: value) { [weak self] in
                let vc = CommerceSettingActionSheetVC()
                vc.transitioningDelegate = self?.transDelegate
                vc.title = data.title
                vc.defaultSelectedIndex = data.intValue
                vc.dataArray = data.items
                vc.didSelectedIndex = { index in
                    data.writeValue(index)
                    CommerceAgoraKitManager.shared.updateSettingForDebugkey(data)
                    tableView.reloadData()
                }
                self?.present(vc, animated: true, completion: {
                    vc.showBgView()
                })
            } detailButtonAction: {[weak self] in
                self?.commerce_showAlert(title: data.title, message: data.tips, confirmTitle: "OK", cancelTitle: nil)
            }

            return cell
        }else {
            cell = UITableViewCell()
        }
        return cell
    }
}

extension CommerceDebugSettingVC {
    func changeValue(_ value: Any, forSettingKey key: CommerceDebugSettingKey) {
        key.writeValue(value)
        CommerceAgoraKitManager.shared.updateSettingForDebugkey(key)
        tableView.reloadData()
    }
}

extension CommerceDebugSettingVC {
    
    private func addObserver(){
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: nil) { [weak self] notify in
            guard let self = self else {return}
            guard let keyboardRect = (notify.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue else { return }
            guard let duration = notify.userInfo?[UIResponder.keyboardAnimationDurationUserInfoKey] as? TimeInterval else { return }
            let keyboradHeight = keyboardRect.size.height
            UIView.animate(withDuration: duration) {
                self.view.layoutIfNeeded()
            }
            
            self.view.addSubview(self.coverView)
            self.coverView.snp.makeConstraints { make in
                make.left.right.top.equalToSuperview()
                make.bottom.equalTo(-keyboradHeight)
            }
        }
        
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: nil) {[weak self] notify in
            self?.coverView.removeFromSuperview()
        }
    }
    
    @objc private func didTapedCoverView(){
        view.endEditing(true)
    }
}
