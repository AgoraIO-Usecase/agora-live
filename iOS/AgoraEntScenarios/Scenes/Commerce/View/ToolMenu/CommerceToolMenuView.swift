//
//  ShowToolMenuView.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2022/11/9.
//

import UIKit
import Agora_Scene_Utils

enum CommerceToolMenuType: CaseIterable {
    case switch_camera
    case camera
    case mic
    case real_time_data
    case setting
    case mute_mic
    
    var imageName: String {
        switch self {
        case .switch_camera: return "show_switch_camera"
        case .camera: return "show_camera"
        case .mic, .mute_mic: return "show_mic"
        case .real_time_data: return "show_realtime"
        case .setting: return "show_setting"
        }
    }
    
    var selectedImageName: String? {
        switch self {
        case .camera: return "show_camera_off"
        case .mic, .mute_mic: return "show_mic_off"
        default: return nil
        }
    }
    
    var title: String {
        switch self {
        case .switch_camera: return "show_setting_switch_camera".commerce_localized
        case .camera: return "show_setting_video_on".commerce_localized
        case .mic: return "show_setting_mic_on".commerce_localized
        case .real_time_data: return "show_setting_statistic".commerce_localized
        case .setting: return "show_setting_advance_setting".commerce_localized
        case .mute_mic: return "show_setting_mute".commerce_localized
        }
    }
    var selectedTitle: String? {
        switch self {
        case .camera: return "show_setting_video_off".commerce_localized
        case .mic: return "show_setting_mic_off".commerce_localized
        case .mute_mic: return "show_setting_unmute".commerce_localized
        default: return title
        }
    }
}

class CommerceToolMenuModel {
    var imageName: String = ""
    var selectedImageName: String = ""
    var title: String = ""
    var type: CommerceToolMenuType = .switch_camera
    var isSelected: Bool = false
}

enum CommerceMenuType {
    case idle_audience
    case idle_broadcaster
    case pking
    case managerMic
}

class CommerceToolMenuView: UIView {
    var title: String? {
        didSet {
            collectionView.reloadData()
        }
    }
    var onTapItemClosure: ((CommerceToolMenuType, Bool) -> Void)?
    var selectedMap: [CommerceToolMenuType: Bool]? {
        didSet {
            collectionView.reloadData()
        }
    }
    
    public lazy var collectionView: AGECollectionView = {
        let view = AGECollectionView()
        let w = Screen.width / 4
        view.itemSize = CGSize(width: w, height: 47)
        view.showsHorizontalScrollIndicator = false
        view.minInteritemSpacing = 0
        view.minLineSpacing = 33
        view.scrollDirection = .vertical
        view.delegate = self
        view.register(CommerceLiveToolViewCell.self,
                      forCellWithReuseIdentifier: CommerceLiveToolViewCell.description())
        view.register(CommerceLiveToolHeaderView.self,
                      forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader,
                      withReuseIdentifier: CommerceLiveToolHeaderView.description())
        return view
    }()
    
    var type: CommerceMenuType = .idle_audience {
        didSet {
            
            switch type {
            case .idle_broadcaster:
                updateToolType(type: [.switch_camera, .camera, .mic, .real_time_data, .setting])
            case .pking:
                updateToolType(type: [.switch_camera, .camera, .mute_mic, .real_time_data])
            case .managerMic:
                updateToolType(type: [.mute_mic, .real_time_data])
            case .idle_audience:
                updateToolType(type: [.real_time_data, .setting])
            }
        }
    }
    
    init(type: CommerceMenuType) {
        super.init(frame: .zero)
        setupUI()
        defer {
            self.type = type
        }
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func updateToolType(type: [CommerceToolMenuType]) {
        var datas = [CommerceToolMenuModel]()
        type.forEach({
            let model = CommerceToolMenuModel()
            model.imageName = $0.imageName
            model.selectedImageName = $0.selectedImageName ?? $0.imageName
            model.title = $0.title
            model.type = $0
            model.isSelected = selectedMap?[$0] ?? false
            datas.append(model)
        })
        collectionView.dataArray = datas
    }
    
    func updateStatus(type: CommerceToolMenuType, isSelected: Bool) {
        let index = collectionView.dataArray?.compactMap({ $0 as? CommerceToolMenuModel }).firstIndex(where: { $0.type == type }) ?? 0
        var datas = collectionView.dataArray
        if let model = datas?[index] as? CommerceToolMenuModel {
            model.isSelected = isSelected
            datas?[index] = model
        }
        collectionView.dataArray = datas
    }
    
    private func setupUI() {
        translatesAutoresizingMaskIntoConstraints = false
        collectionView.translatesAutoresizingMaskIntoConstraints = false
        backgroundColor = UIColor(hex: "#151325", alpha: 0.85)
        layer.cornerRadius = 10
        layer.maskedCorners = [.layerMinXMinYCorner, .layerMaxXMinYCorner]
        layer.masksToBounds = true
        
        addSubview(collectionView)
        
        widthAnchor.constraint(equalToConstant: Screen.width).isActive = true
        
        collectionView.leadingAnchor.constraint(equalTo: leadingAnchor).isActive = true
        collectionView.trailingAnchor.constraint(equalTo: trailingAnchor).isActive = true
        collectionView.topAnchor.constraint(equalTo: topAnchor, constant: 28).isActive = true
        collectionView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -Screen.safeAreaBottomHeight()).isActive = true
    }
}
extension CommerceToolMenuView: AGECollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: CommerceLiveToolViewCell.description(), for: indexPath) as! CommerceLiveToolViewCell
        cell.setToolData(item: self.collectionView.dataArray?[indexPath.item])
        return cell
    }
    
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let cell = collectionView.cellForItem(at: indexPath) as? CommerceLiveToolViewCell,
              let model = self.collectionView.dataArray?[indexPath.item] as? CommerceToolMenuModel else { return }
        let isSelected = cell.updateButtonState()
        onTapItemClosure?(model.type, isSelected)
    }
    
    func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
        let headerView = collectionView.dequeueReusableSupplementaryView(ofKind: UICollectionView.elementKindSectionHeader,
                                                                         withReuseIdentifier: CommerceLiveToolHeaderView.description(),
                                                                         for: indexPath) as! CommerceLiveToolHeaderView
        headerView.tipsLabel.text = title
        return headerView
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, referenceSizeForHeaderInSection section: Int) -> CGSize {
        CGSize(width: Screen.width, height: (type == .idle_audience || type == .idle_broadcaster) ? 0 : 50)
    }
}


class CommerceLiveToolHeaderView: UICollectionReusableView {
    lazy var tipsLabel: AGELabel = {
        let label = AGELabel(colorStyle: .white, fontStyle: .middle)
        return label
    }()
    private lazy var lineView: AGEView = {
        let view = AGEView()
        view.backgroundColor = .gray
        return view
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        addSubview(tipsLabel)
        addSubview(lineView)
        tipsLabel.translatesAutoresizingMaskIntoConstraints = false
        lineView.translatesAutoresizingMaskIntoConstraints = false
        
        tipsLabel.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 20).isActive = true
        tipsLabel.topAnchor.constraint(equalTo: topAnchor).isActive = true
        
        lineView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 20).isActive = true
        lineView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -20).isActive = true
        lineView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -20).isActive = true
        lineView.heightAnchor.constraint(equalToConstant: 1).isActive = true
    }
}

class CommerceLiveToolViewCell: UICollectionViewCell {
    private lazy var iconButton: UIButton = {
        let button = UIButton()
        button.setImage(UIImage(named: "icon-rotate"), for: .normal)
        button.isUserInteractionEnabled = false
        return button
    }()
    private lazy var titleLabel: UILabel = {
        let label = UILabel()
        label.text = "Switch_Camera"
        label.textColor = UIColor(hex: "#C6C4DD")
        label.font = .systemFont(ofSize: 12)
        label.textAlignment = .center
        label.numberOfLines = 2
        return label
    }()
    
    private var model: CommerceToolMenuModel?
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupUI() {
        iconButton.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        
        contentView.addSubview(iconButton)
        contentView.addSubview(titleLabel)
        
        iconButton.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        iconButton.bottomAnchor.constraint(equalTo: titleLabel.topAnchor, constant: -5).isActive = true
        
        titleLabel.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 5).isActive = true
        titleLabel.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -5).isActive = true
        titleLabel.bottomAnchor.constraint(equalTo: contentView.bottomAnchor).isActive = true
    }
    
    func setToolData(item: Any?) {
        guard let model = item as? CommerceToolMenuModel else { return }
        self.model = model
        iconButton.setImage(UIImage.commerce_sceneImage(name: model.imageName), for: .normal)
        iconButton.setImage(UIImage.commerce_sceneImage(name: model.selectedImageName), for: .selected)
        iconButton.isSelected = model.isSelected
        titleLabel.text = model.isSelected ? model.type.selectedTitle : model.type.title
    }
    
    @discardableResult
    func updateButtonState() -> Bool {
        iconButton.isSelected = !iconButton.isSelected
        model?.isSelected = iconButton.isSelected
        titleLabel.text = iconButton.isSelected ? model?.type.selectedTitle : model?.type.title
        return iconButton.isSelected
    }
}
