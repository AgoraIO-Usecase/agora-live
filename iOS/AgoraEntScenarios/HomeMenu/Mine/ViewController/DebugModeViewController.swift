//
//  DebugModeViewController.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/8/21.
//

import UIKit
import AgoraCommon

class DebugItem {
    var title: String
    var state: Bool
    var key: String
    init(title: String, state: Bool, key: String) {
        self.title = title
        self.state = state
        self.key = key
    }
}

class DebugItemCell: UITableViewCell {
    let titleLabel = UILabel()
    let toggleSwitch = UISwitch()
    
    var debugItem: DebugItem? {
        didSet {
            guard let debugItem = debugItem else { return }
            titleLabel.text = debugItem.title
            toggleSwitch.isOn = debugItem.state
        }
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        setupViews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupViews() {
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        toggleSwitch.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(titleLabel)
        contentView.addSubview(toggleSwitch)
        
        NSLayoutConstraint.activate([
            titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            titleLabel.centerYAnchor.constraint(equalTo: contentView.centerYAnchor),
            toggleSwitch.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            toggleSwitch.centerYAnchor.constraint(equalTo: contentView.centerYAnchor)
        ])
        
        toggleSwitch.addTarget(self, action: #selector(switchToggled), for: .valueChanged)
    }
    
    @objc private func switchToggled() {
        debugItem?.state = toggleSwitch.isOn
        saveStateToSandbox()
    }
    
    private func saveStateToSandbox() {
        guard let debugItem = debugItem else { return }
        UserDefaults.standard.set(debugItem.state, forKey: debugItem.key)
    }
}

class DebugModeViewController: VRBaseViewController, UITableViewDelegate, UITableViewDataSource {
    var debugItems: [DebugItem] = []
    let tableView = UITableView()
    let uploadLogFileKey = "uploadLogFileKey"

    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.navigation.title.text = NSLocalizedString("app_debug_mode", comment: "")
       
        setupTableView()
        loadDebugItems()
    }
        
    private func setupTableView() {
        tableView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(tableView)
        
        NSLayoutConstraint.activate([
            tableView.topAnchor.constraint(equalTo: self.navigation.bottomAnchor),
            tableView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            tableView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            tableView.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])
        
        tableView.register(DebugItemCell.self, forCellReuseIdentifier: "DebugItemCell")
        tableView.dataSource = self
        tableView.delegate = self
    }
    
    private func loadDebugItems() {
        let state = UserDefaults.standard.bool(forKey: DebugModeKeyCenter.upLoadLogFileAutomatic)
        let item = DebugItem(title: NSLocalizedString("app_debug_upload_log_file_title", comment: ""), state: state, key: DebugModeKeyCenter.upLoadLogFileAutomatic)
        debugItems.append(item)
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return debugItems.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "DebugItemCell", for: indexPath) as! DebugItemCell
        cell.debugItem = debugItems[indexPath.row]
        return cell
    }
    
}
