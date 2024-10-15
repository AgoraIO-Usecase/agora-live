//
//  DFStylizedSettting.swift
//  DreamFlow
//
//  Created by qinhui on 2024/8/30.
//

import Foundation

protocol DFStylizedSetttingDelegate: AnyObject {
    func saveStylizedSetting(setting: DFStylizedSettting)
}

class DFStylizedSettting: UIViewController {
    var workerState: DreamFlowWorkState = .unload
    
    let switchCellIdentifier = "SwitchCell"
    let presetsScrollCellIdentifier = "PresetsScrollCell"
    let arrowCellIdentifier = "ArrowCell"
    let presestInfoCellIdentifier = "PresetsInfoCell"
    
    var stylizedSettingConfig = DFStylizedSettingConfig()
    weak var delegate: DFStylizedSetttingDelegate?
    
    let tableView = UITableView()
    
    private lazy var currentStyle: DFStylizedSettingItem = {
        dataService.styles[0]
    }()
    
    private lazy var currentPreset: DFPresetSettingItem = {
        dataService.Presets[0]
    }()
    
    private lazy var currentServer: DreamFlowServer = {
        dataService.servers[0]
    }()
    
    private lazy var dataService: DFStylizedDataService = {
        let service = DFStylizedDataService()
        return service
    }()
    
    private lazy var navigationBar: ShowNavigationBar = {
        let bar = ShowNavigationBar()
        var rightItem = ShowBarButtonItem(action: #selector(rightAction))
        rightItem.title = "show_advanced_setting_save".show_localized
        
        bar.rightItems = [rightItem]
        return bar
    }()
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        loadData()
        configSubviews()
        addGestureAndObserver()
    }
    
    private func loadData() {
        guard let configData = DFStylizedDataService.loadStylizedSettingConfig() else {
            updateStylizedSettingConfig()
            return
        }
        
        let state = workerState == .initialize || workerState == .running

        stylizedSettingConfig = configData
        stylizedSettingConfig.style_effect = state

        for item in dataService.styles {
            if item.title == configData.style {
                currentStyle = item
                break
            }
        }
        
        for item in dataService.servers {
            if item.server == configData.server {
                currentServer = item
                break
            }
        }
        
        for item in dataService.Presets {
            if item.title == configData.preset {
                currentPreset = item
                if item.title == "Customized" {
                    synchronizeCustomPresetsData()
                }
                break
            }
        }
        
        if currentStyle == nil {
            currentStyle = dataService.styles[0]
        }
        
        if currentPreset == nil {
            currentPreset = dataService.Presets[0]
        }
        
        if currentServer == nil {
            currentServer = dataService.servers[0]
        }
    }
    
    @objc func dismissKeyboard() {
        view.endEditing(true)
    }
       
    @objc func keyboardWillShow(notification: NSNotification) {
        if let keyboardFrame = notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect {
            let contentInsets = UIEdgeInsets(top: 10, left: 0, bottom: keyboardFrame.height, right: 0)
            tableView.contentInset = contentInsets
            tableView.scrollIndicatorInsets = contentInsets
            
            let lastRowIndex = self.tableView.numberOfRows(inSection: self.tableView.numberOfSections - 1) - 1
            if lastRowIndex >= 0 {
                let indexPath = IndexPath(row: lastRowIndex, section: self.tableView.numberOfSections - 1)
                self.tableView.scrollToRow(at: indexPath, at: .bottom, animated: true)
            }
        }
    }
    
    @objc func keyboardWillHide(notification: NSNotification) {
        tableView.contentInset = UIEdgeInsets(top: 10, left: 0, bottom: 0, right: 0)
        tableView.scrollIndicatorInsets = .zero
        tableView.layoutIfNeeded()
    }
    
    private func addGestureAndObserver() {
        // 添加点击手势识别器以收起键盘
        let tapGesture = UITapGestureRecognizer(target: self, action: #selector(dismissKeyboard))
        tapGesture.cancelsTouchesInView = false
        view.addGestureRecognizer(tapGesture)
        
        // 监听键盘事件
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillShow), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardWillHide), name: UIResponder.keyboardWillHideNotification, object: nil)
    }
 
    private func configSubviews() {
        view.backgroundColor = .white
        navigationBar.title = "navi_title_dream_flow".show_localized
        view.addSubview(navigationBar)
        
        tableView.delegate = self
        tableView.dataSource = self
        tableView.register(DFSwitchTableViewCell.self, forCellReuseIdentifier: switchCellIdentifier)
        tableView.register(DFArrowTableViewCell.self, forCellReuseIdentifier: arrowCellIdentifier)
        tableView.register(DFPresetsScrollTableViewCell.self, forCellReuseIdentifier: presetsScrollCellIdentifier)
        tableView.register(DFPresetsInfoTableViewCell.self, forCellReuseIdentifier: presestInfoCellIdentifier)

        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)
        
        let contentInsets = UIEdgeInsets(top: 10, left: 0, bottom: 0, right: 0)
        tableView.contentInset = contentInsets

        tableView.snp.makeConstraints { make in
            make.top.equalTo(navigationBar.snp.bottom)
            make.left.right.bottom.equalTo(0)
        }
    }
    
    @objc private func rightAction() {
        updateStylizedSettingConfig()
        saveStylizedSettingConfig()
        
        self.delegate?.saveStylizedSetting(setting: self)
        self.navigationController?.popViewController(animated: true)
    }
    
    private func updateStylizedSettingConfig() {
        stylizedSettingConfig.preset = currentPreset.title
        stylizedSettingConfig.style = currentStyle.title
        stylizedSettingConfig.prompt = currentPreset.content
        stylizedSettingConfig.strength = currentPreset.strengthDefaultValue
        stylizedSettingConfig.superFrameFactor = currentPreset.superFrameDefaultValue
        stylizedSettingConfig.face_mode = currentPreset.faceMode
        stylizedSettingConfig.server = currentServer.server
    }
    
    private func synchronizeCustomPresetsData() {
        currentPreset.content = stylizedSettingConfig.prompt
        currentStyle.title = stylizedSettingConfig.style
        currentPreset.strengthDefaultValue = stylizedSettingConfig.strength
        currentPreset.superFrameDefaultValue = stylizedSettingConfig.superFrameFactor
        currentPreset.faceMode = stylizedSettingConfig.face_mode
        currentPreset.defaultStyleIndex = dataService.styles.firstIndex(where: { $0.title == stylizedSettingConfig.style }) ?? 0
    }
    
    private func saveStylizedSettingConfig() {
        DFStylizedDataService.saveStylizedSettingConfig(stylizedSettingConfig)
    }
    
    private func showServerAlert() {
        let alertController = UIAlertController(title: "", message: nil, preferredStyle: .actionSheet)
        let selectionHandler: ((DreamFlowServer) -> Void) = { [weak self] item in
            self?.currentServer = item
            self?.tableView.reloadData()
        }
        
        for option in dataService.servers {
            let action = UIAlertAction(title: option.name, style: .default) { _ in
                selectionHandler(option)
            }
            alertController.addAction(action)
        }
        
        let cancelAction = UIAlertAction(title: "Cancel", style: .cancel, handler: nil)
        alertController.addAction(cancelAction)
        
        present(alertController, animated: true)
    }
}

extension DFStylizedSettting: UITableViewDelegate, UITableViewDataSource {
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 4
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let row = indexPath.row
        switch row {
        case 0:
            let cell = tableView.dequeueReusableCell(withIdentifier: switchCellIdentifier, for: indexPath) as! DFSwitchTableViewCell
            cell.setData(title: dataService.titles[row], state: stylizedSettingConfig.style_effect)
            cell.selectionStyle = .none
            cell.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: .greatestFiniteMagnitude)
            cell.switchHandler = { [weak self] state in
                self?.stylizedSettingConfig.style_effect = state
            }
          
            return cell
            
        case 1:
            let cell = tableView.dequeueReusableCell(withIdentifier: arrowCellIdentifier, for: indexPath) as! DFArrowTableViewCell
            cell.selectionStyle = .none
            cell.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: .greatestFiniteMagnitude)
            cell.setData(title: "df_server_title".show_localized, content: currentServer.name)
            cell.serverHandler = { [weak self] in
                self?.showServerAlert()
            }
            
            return cell
            
        case 2:
            let cell = tableView.dequeueReusableCell(withIdentifier: presetsScrollCellIdentifier, for: indexPath) as! DFPresetsScrollTableViewCell
            cell.setData(with: dataService.titles[row], items: dataService.Presets, selectedItem: currentPreset)
            cell.selectionStyle = .none
            cell.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: .greatestFiniteMagnitude)
            cell.effectSelectHandler = { [weak self] selItem in
                self?.currentPreset = selItem
                self?.tableView.reloadData()
            }
            
            return cell
            
        case 3:
            let cell = tableView.dequeueReusableCell(withIdentifier: presestInfoCellIdentifier, for: indexPath) as! DFPresetsInfoTableViewCell
            cell.setData(preset: self.currentPreset, styles: dataService.styles)
            cell.selectionStyle = .none
            cell.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: .greatestFiniteMagnitude)
            cell.strengthSliderHandler = { [weak self] value in
                self?.currentPreset.strengthDefaultValue = value
            }
            cell.superFrameSliderHandler = { [weak self] value in
                self?.currentPreset.superFrameDefaultValue = Int(value)
            }
            cell.styleSelectHandler = { [weak self] selItem in
                self?.currentStyle = selItem
            }
            cell.switchHandler = { [weak self] state in
                self?.currentPreset.faceMode = state
            }
            cell.inputHandler = { [weak self] customPrompts in
                self?.currentPreset.content = customPrompts
            }
            
            return cell
            
        default:
            return UITableViewCell()
        }
    }
}

extension DFStylizedSettting: UITextViewDelegate {
    func textViewDidBeginEditing(_ textView: UITextView) {
        let indexPath = IndexPath(row: 4, section: 0)
        tableView.scrollToRow(at: indexPath, at: .middle, animated: true)
    }
}
