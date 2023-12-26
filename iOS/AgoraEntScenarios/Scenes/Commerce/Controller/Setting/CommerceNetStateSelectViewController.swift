//
//  ShowNetStateSelectViewController.swift
//  AgoraEntScenarios
//
//  Created by Jonathan on 2023/7/20.
//

import Foundation

private let CommercePresettingCellID = "CommercePresettingCellID"
private let CommercePresettingHeaderViewID = "CommercePresettingHeaderViewID"
class CommerceNetStateSelectViewController: UIViewController {
    
    private enum SectionType {
        case deviceLevel
        case netCondition
        case performance
    }
    
    public static func showInViewController(_ viewController: UIViewController) {
        let vc = ShowNetStateSelectViewController()
        vc.modalPresentationStyle = .fullScreen
        viewController.present(vc, animated: true)
    }
    
    private var sections: [SectionType] = [.performance, .netCondition]
    
    private var deviceLevels: [CommerceAgoraKitManager.DeviceLevel] = [.high, .medium, .low]
    
    private var netConditions: [CommerceAgoraKitManager.NetCondition] = [.good, .bad]
    
    private var performances: [CommerceAgoraKitManager.PerformanceMode] = [.fluent, .smooth]
    
    private var aDeviceLevel: CommerceAgoraKitManager.DeviceLevel = .high
    
    private var aNetCondition: CommerceAgoraKitManager.NetCondition = .good
    
    private var aPerformance: CommerceAgoraKitManager.PerformanceMode = .fluent
    
    private let topBar = CommerceNavigationBar()
    
    private let footerView = CommerceNetStateFooterView(frame: CGRect(x: 0, y: 0, width: 100, height: 220))

    private let tableView: UITableView = UITableView(frame: .zero, style: .grouped)
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        aDeviceLevel = CommerceAgoraKitManager.shared.deviceLevel
        
        createViews()
        createConstrains()
        updateSections()
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }
    
    @objc private func onClickSubmit() {
        CommerceAgoraKitManager.shared.netCondition = aNetCondition
        CommerceAgoraKitManager.shared.performanceMode = aPerformance
        CommerceAgoraKitManager.shared.deviceLevel = aDeviceLevel
        CommerceAgoraKitManager.shared.setupBroadcasterProfile()
        dismiss(animated: true)
    }
    
    private func updateSections() {
        var temp: [SectionType] = []
        if AppContext.shared.isDebugMode {
            temp.append(.deviceLevel)
        }
        temp.append(.performance)
        if aPerformance == .fluent {
            temp.append(.netCondition)
        }
        sections = temp
        let deviceStr = aDeviceLevel.description() + "（\(CommerceAgoraKitManager.shared.deviceScore)）"
        footerView.setDeviceLevel(text: deviceStr)
        tableView.reloadData()
    }
}

// MARK: - Callback UITableView
extension CommerceNetStateSelectViewController: UITableViewDelegate, UITableViewDataSource {
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return sections.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        let sectionType = sections[section]
        if (sectionType == .deviceLevel) {
            return deviceLevels.count
        } else if (sectionType == .performance) {
            return performances.count
        } else if (sectionType == .netCondition) {
            return netConditions.count
        } else {
            return 0
        }
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: CommercePresettingCellID, for: indexPath) as! CommercePresettingCell
        let sectionType = sections[indexPath.section]
        if (sectionType == .deviceLevel) {
            let a = deviceLevels[indexPath.row]
            switch a {
            case .high:
                cell.setTitle("show_presetting_device_level_high_title".commerce_localized,
                              desc: "show_presetting_device_level_high_desc".commerce_localized)
            case .medium:
                cell.setTitle("show_presetting_device_level_medium_title".commerce_localized,
                              desc: "show_presetting_device_level_medium_desc".commerce_localized)
            case .low:
                cell.setTitle("show_presetting_device_level_low_title".commerce_localized,
                              desc: "show_presetting_device_level_low_desc".commerce_localized)
            }
            cell.aSelected = (aDeviceLevel == a)
        } else if (sectionType == .performance) {
            let a = performances[indexPath.row]
            switch a {
            case .smooth:
                cell.setTitle("show_presetting_performances_smooth".commerce_localized,
                              desc: "")
            case .fluent:
                cell.setTitle("show_presetting_performances_fluent".commerce_localized,
                              desc: "")
            }
            cell.aSelected = (aPerformance == a)
        } else {
            let a = netConditions[indexPath.row]
            switch a {
            case .good:
                cell.setTitle("show_presetting_net_good".commerce_localized,
                              desc: "show_presetting_net_good_detail".commerce_localized)
            case .bad:
                cell.setTitle("show_presetting_net_bad".commerce_localized,
                              desc: "show_presetting_net_bad_detail".commerce_localized)
            }
            cell.aSelected = (aNetCondition == a)
        }
        return cell
    }
    
    func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? {
        let headerView = tableView.dequeueReusableHeaderFooterView(withIdentifier: CommercePresettingHeaderViewID) as! CommercePresettingHeaderView
        let sectionType = sections[section]
        if (sectionType == .deviceLevel) {
            headerView.setTitle("show_presetting_mode_show_title".commerce_localized,
                                desc: "show_presetting_mode_show_desc".commerce_localized)
        } else if (sectionType == .performance) {
            headerView.setTitle("show_presetting_performances_title".commerce_localized,
                                desc: "show_presetting_performances_title_detail".commerce_localized)
        } else if (sectionType == .netCondition) {
            headerView.setTitle("show_presetting_net_title".commerce_localized,
                                desc: "show_presetting_net_title_detail".commerce_localized)
        }
        return headerView
    }
    
    func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        return CommercePresettingFooterView()
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let sectionType = sections[indexPath.section]
        if (sectionType == .deviceLevel) {
            aDeviceLevel = deviceLevels[indexPath.row]
        } else if (sectionType == .performance) {
            aPerformance = performances[indexPath.row]
        } else if (sectionType == .netCondition) {
            aNetCondition = netConditions[indexPath.row]
        }
        updateSections()
    }
}
// MARK: - Creations
extension CommerceNetStateSelectViewController {
    
    func createViews() {
        view.backgroundColor = .white
        let deviceStr = aDeviceLevel.description() + "（\(CommerceAgoraKitManager.shared.deviceScore)）"
        footerView.setDeviceLevel(text: deviceStr)
        
        tableView.backgroundColor = .white
        tableView.delegate = self
        tableView.dataSource = self
        tableView.rowHeight = 60
        tableView.separatorStyle = .none
        tableView.sectionHeaderHeight = UITableView.automaticDimension
        tableView.sectionFooterHeight = 16
        tableView.tableFooterView = footerView
        tableView.register(CommercePresettingCell.self, forCellReuseIdentifier: CommercePresettingCellID)
        tableView.register(CommercePresettingHeaderView.self, forHeaderFooterViewReuseIdentifier: CommercePresettingHeaderViewID)
        view.addSubview(tableView)
        
        topBar.title = "show_advance_setting_presetting_title".commerce_localized
        topBar.leftItems = nil;
        let saveButtonItem = CommerceBarButtonItem(title: "show_advanced_setting_presetting_save".commerce_localized,
                                                   target: self,
                                                   action: #selector(onClickSubmit))
        topBar.rightItems = [saveButtonItem]
        view.addSubview(topBar)
    }
    
    func createConstrains() {
        tableView.snp.makeConstraints { make in
            make.left.bottom.right.equalToSuperview()
            make.top.equalTo(topBar.snp.bottom)
        }
    }
}
// MARK: - ShowNetStateFooterView
fileprivate class CommerceNetStateFooterView: UITableViewHeaderFooterView {
    
    private let radiusView = UIView()
    
    public let infoLabel = UILabel()
    
    private let suggestLabel = UILabel()
    
    private let suggestInfoLabel = UILabel()
    
    override init(reuseIdentifier: String?) {
        super.init(reuseIdentifier: reuseIdentifier)
        createViews()
        createConstrains()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setDeviceLevel(text: String) {
        infoLabel.text = "show_presetting_info_device_level".commerce_localized + text
    }
    
    private func createViews(){
        radiusView.backgroundColor = .commerce_preset_bg
        radiusView.layer.cornerRadius = 16
        radiusView.clipsToBounds = true
        contentView.addSubview(radiusView)
        
        infoLabel.font = UIFont.systemFont(ofSize: 16)
        infoLabel.textColor = .commerce_chat_input_text
        infoLabel.font = .commerce_R_14
        infoLabel.numberOfLines = 0
        infoLabel.textAlignment = .left
        contentView.addSubview(infoLabel)
        
        suggestLabel.font = UIFont.systemFont(ofSize: 16)
        suggestLabel.textColor = .commerce_chat_input_text
        suggestLabel.font = .commerce_R_14
        suggestLabel.numberOfLines = 1
        suggestLabel.textAlignment = .left
        suggestLabel.text = "show_presetting_info_suggest".commerce_localized
        contentView.addSubview(suggestLabel)
        
        suggestInfoLabel.font = UIFont.systemFont(ofSize: 16)
        suggestInfoLabel.textColor = .commerce_chat_input_text
        suggestInfoLabel.font = .commerce_R_14
        suggestInfoLabel.numberOfLines = 0
        suggestInfoLabel.textAlignment = .left
        suggestInfoLabel.text = "show_presetting_info_suggest_detail".commerce_localized
        contentView.addSubview(suggestInfoLabel)
    }
    
    private func createConstrains() {
        radiusView.snp.makeConstraints { make in
            make.top.equalTo(20)
            make.left.equalTo(20)
            make.right.equalTo(-20)
            make.bottom.equalTo(-20)
        }
        infoLabel.snp.makeConstraints { make in
            make.top.equalTo(radiusView).offset(20)
            make.left.equalTo(radiusView).offset(20)
            make.right.equalTo(radiusView).offset(-20)
        }
        suggestLabel.snp.makeConstraints { make in
            make.top.equalTo(infoLabel.snp.bottom).offset(15)
            make.left.equalTo(radiusView).offset(20)
        }
        suggestInfoLabel.snp.makeConstraints { make in
            make.top.equalTo(suggestLabel)
            make.left.equalTo(suggestLabel.snp.right)
            make.right.equalTo(radiusView).offset(-20)
            make.bottom.equalTo(radiusView).offset(-20)
        }
    }
}
