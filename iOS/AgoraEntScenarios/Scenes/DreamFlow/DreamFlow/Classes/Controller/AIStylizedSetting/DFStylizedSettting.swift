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
    let strengthCellIdentifier = "StrengthCell"
    let horizontalScrollCellIdentifier = "HorizontalScrollCell"
    let textViewCellIdentifier = "TextViewCell"
    var stylizedSettingConfig = DFStylizedSettingConfig()
    weak var delegate: DFStylizedSetttingDelegate?
    
    let tableView = UITableView()
    
    private var defaultStyle: DFStylizedSettingItem!
    private var defaultEffect: DFStylizedSettingItem!
    
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
        guard let configData = dataService.loadStylizedSettingConfig() else {
            defaultStyle = dataService.styles[0]
            defaultEffect = dataService.effects[0]
            return
        }
        
        self.stylizedSettingConfig = configData
        
        for item in dataService.styles {
            if item.title == configData.style {
                defaultStyle = item
                break
            }
        }
        
        for item in dataService.effects {
            if item.title == configData.effect {
                defaultEffect = item
                break
            }
        }
        
        if defaultStyle == nil {
            defaultStyle = dataService.styles[0]
        }
        
        if defaultEffect == nil {
            defaultEffect = dataService.effects[0]
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
        }
    }
    
    @objc func keyboardWillHide(notification: NSNotification) {
        tableView.contentInset = UIEdgeInsets(top: 10, left: 0, bottom: 0, right: 0)
        tableView.scrollIndicatorInsets = .zero
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
        tableView.register(DFStrengthTableViewCell.self, forCellReuseIdentifier: strengthCellIdentifier)
        tableView.register(DFHorizontalScrollTableViewCell.self, forCellReuseIdentifier: horizontalScrollCellIdentifier)
        tableView.register(DFTextViewTableViewCell.self, forCellReuseIdentifier: textViewCellIdentifier)
        
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
        dataService.saveStylizedSettingConfig(self.stylizedSettingConfig)
        self.delegate?.saveStylizedSetting(setting: self)
        self.navigationController?.popViewController(animated: true)
    }
}

extension DFStylizedSettting: UITableViewDelegate, UITableViewDataSource {
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return 5
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let row = indexPath.row
        switch row {
        case 0, 1:
            let cell = tableView.dequeueReusableCell(withIdentifier: switchCellIdentifier, for: indexPath) as! DFSwitchTableViewCell
            if row == 0 {
                cell.setData(title: dataService.titles[row], state: self.stylizedSettingConfig.style_effect)
                cell.switchHandler = { [weak self] state in
                    self?.stylizedSettingConfig.style_effect = state
                }
            } else {
                cell.setData(title: dataService.titles[row], state: self.stylizedSettingConfig.face_mode)
                cell.switchHandler = { [weak self] state in
                    self?.stylizedSettingConfig.face_mode = state
                }
                
                if workerState == .loaded {
                    cell.setUserInteractionEnabled(enabled: false)
                }
            }
            cell.selectionStyle = .none
            cell.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: .greatestFiniteMagnitude)
            return cell
        case 2:
            let cell = tableView.dequeueReusableCell(withIdentifier: strengthCellIdentifier, for: indexPath) as! DFStrengthTableViewCell
            cell.setData(title: dataService.titles[row], sliderValue: self.stylizedSettingConfig.strength)
            cell.selectionStyle = .none
            cell.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: .greatestFiniteMagnitude)
            cell.sliderHandler = { [weak self] value in
                self?.stylizedSettingConfig.strength = value
            }
            if workerState == .loaded {
                cell.setUserInteractionEnabled(enabled: false)
            }
            return cell
        case 3:
            let cell = tableView.dequeueReusableCell(withIdentifier: horizontalScrollCellIdentifier, for: indexPath) as! DFHorizontalScrollTableViewCell
            cell.setData(with: dataService.titles[row], items: dataService.styles, selectedItem: defaultStyle)
            cell.selectionStyle = .none
            cell.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: .greatestFiniteMagnitude)
            cell.styleHandler = { [weak self] selItem in
                self?.stylizedSettingConfig.style = selItem.title
                self?.defaultStyle = selItem
            }
            if workerState == .loaded {
                cell.setUserInteractionEnabled(enabled: false)
            }
            return cell
        case 4:
            let cell = tableView.dequeueReusableCell(withIdentifier: textViewCellIdentifier, for: indexPath) as! DFTextViewTableViewCell
            cell.setData(with: dataService.titles[row], items: dataService.effects, selectedItem: defaultEffect)
            cell.selectionStyle = .none
            cell.separatorInset = UIEdgeInsets(top: 0, left: 0, bottom: 0, right: .greatestFiniteMagnitude)
            cell.effectHandler = { [weak self] selItem in
                self?.stylizedSettingConfig.effect = selItem.title
                self?.stylizedSettingConfig.prompt = selItem.content
                self?.defaultEffect = selItem
            }
            cell.inputHandler = { [weak self] content in
                self?.stylizedSettingConfig.prompt = content
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
