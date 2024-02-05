//
//  AUiMoreDialog.swift
//  AgoraEntScenarios
//
//  Created by wushengtao on 2023/4/18.
//

import UIKit
import Agora_Scene_Utils

@objc public class ShowLiveMoreDialog: UIView {
    var onClickDislikeClosure: (() -> Void)?
    var onClickDisUserClosure:(() -> Void)?
    
    private lazy var contentView: UIView = {
        let view = UIView()
        view.backgroundColor = UIColor(hex: "#000000", alpha: 0.85)
        view.layer.cornerRadius = 15
        view.layer.maskedCorners = [.layerMinXMaxYCorner, .layerMaxXMaxYCorner]
        view.frame = CGRect(origin: .zero, size: CGSize(width: self.frame.size.width, height: 152))
        return view
    }()
    private lazy var stackView: UIStackView = {
        let stackView = UIStackView()
        stackView.spacing = 10
        stackView.axis = .horizontal
        stackView.alignment = .fill
        stackView.distribution = .fill
        stackView.translatesAutoresizingMaskIntoConstraints = false
        return stackView
    }()
    
    private lazy var reportButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage.sceneImage(name: "report_icon")?.withTintColor(.white, renderingMode: .alwaysOriginal),
                        for: .normal,
                        postion: .top,
                        spacing: 4)
        button.setTitle("show_report".show_localized, for: .normal)
        button.titleLabel?.font = UIFont.systemFont(ofSize: 12)
        button.setTitleColor(.gray, for: .normal)
        button.addTargetFor(self, action: #selector(onAction(_:)), for: .touchUpInside)
        return button
    }()
    private lazy var dislikeButton: AGEButton = {
        let button = AGEButton()
        button.setImage(UIImage.sceneImage(name: "report_content_icon")?.withTintColor(.white, renderingMode: .alwaysOriginal),
                        for: .normal,
                        postion: .top,
                        spacing: 4)
        button.setTitle("report_content".show_localized, for: .normal)
        button.titleLabel?.font = UIFont.systemFont(ofSize: 12)
        button.setTitleColor(.gray, for: .normal)
        
        button.addTargetFor(self, action: #selector(onDislikeButton(sender:)), for: .touchUpInside)
        return button
    }()
    
    public override init(frame: CGRect) {
        super.init(frame: frame)
        _loadSubViews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func _loadSubViews() {
        addSubview(contentView)
        contentView.addSubview(stackView)
        stackView.addArrangedSubview(reportButton)
        stackView.addArrangedSubview(dislikeButton)
        stackView.centerXAnchor.constraint(equalTo: contentView.centerXAnchor).isActive = true
        stackView.centerYAnchor.constraint(equalTo: contentView.centerYAnchor).isActive = true
        stackView.heightAnchor.constraint(equalToConstant: 68).isActive = true
    }
    
    open override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        super.touchesEnded(touches, with: event)
        if let point = touches.first?.location(in: self), !contentView.frame.contains(point) {
            hidden()
        }
    }
    
    @objc func show() {
        contentView.bottom = 0
        UIView.animate(withDuration: 0.3, delay: 0) {
            self.contentView.top = 0
        }
    }
    
    @objc func hidden() {
        UIView.animate(withDuration: 0.3, delay: 0) {
            self.contentView.bottom = 0
        } completion: { flag in
            self.removeFromSuperview()
        }
    }
    
    @objc private func onAction(_ sender: UIButton) {
        hidden()
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.2) {
            ToastView.show(text: "You have blocked this user.")
        }
        onClickDisUserClosure?()
    }
    @objc
    private func onDislikeButton(sender: UIButton) {
        hidden()
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.2) {
            ToastView.show(text: "You have blocked this content.")
        }
        onClickDislikeClosure?()
    }
}
