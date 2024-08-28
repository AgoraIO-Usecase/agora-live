//
//  VLLogAlert.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/8/6.
//

import UIKit

class VLLogToast: UIView {
    private static var shared: VLLogToast?
    private var confirmAction: (() -> Void)?
    private var cancelAction: (() -> Void)?
    static var showState: Bool = false

    private init(title: String, message: String, confirmButtonTitle: String, cancelButtonTitle: String, confirmAction: @escaping () -> Void, cancelAction: @escaping () -> Void) {
        super.init(frame: .zero)
        self.confirmAction = confirmAction
        self.cancelAction = cancelAction
        setupView(title: title, message: message, confirmButtonTitle: confirmButtonTitle, cancelButtonTitle: cancelButtonTitle)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func setupView(title: String, message: String, confirmButtonTitle: String, cancelButtonTitle: String) {
        self.backgroundColor = UIColor.black.withAlphaComponent(0.6)
        self.layer.cornerRadius = 10
        self.translatesAutoresizingMaskIntoConstraints = false

        let titleLabel = UILabel()
        titleLabel.text = title
        titleLabel.textColor = .white
        titleLabel.textAlignment = .center
        titleLabel.translatesAutoresizingMaskIntoConstraints = false

        let messageLabel = UILabel()
        messageLabel.text = message
        messageLabel.textColor = .white
        messageLabel.textAlignment = .center
        messageLabel.numberOfLines = 0
        messageLabel.translatesAutoresizingMaskIntoConstraints = false
        messageLabel.isUserInteractionEnabled = true

        let longPressGesture = UILongPressGestureRecognizer(target: self, action: #selector(copyMessageToClipboard(_:)))
        messageLabel.addGestureRecognizer(longPressGesture)

        let confirmButton = UIButton(type: .system)
        confirmButton.setTitle(confirmButtonTitle, for: .normal)
        confirmButton.addTarget(self, action: #selector(confirmButtonTapped), for: .touchUpInside)
        confirmButton.translatesAutoresizingMaskIntoConstraints = false

        let cancelButton = UIButton(type: .system)
        cancelButton.setTitle(cancelButtonTitle, for: .normal)
        cancelButton.addTarget(self, action: #selector(cancelButtonTapped), for: .touchUpInside)
        cancelButton.translatesAutoresizingMaskIntoConstraints = false

        self.addSubview(titleLabel)
        self.addSubview(messageLabel)
        self.addSubview(confirmButton)
        self.addSubview(cancelButton)

        NSLayoutConstraint.activate([
            titleLabel.topAnchor.constraint(equalTo: self.topAnchor, constant: 10),
            titleLabel.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: 10),
            titleLabel.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -10),

            messageLabel.topAnchor.constraint(equalTo: titleLabel.bottomAnchor, constant: 10),
            messageLabel.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: 10),
            messageLabel.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -10),

            confirmButton.topAnchor.constraint(equalTo: messageLabel.bottomAnchor, constant: 10),
            confirmButton.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: 10),
            confirmButton.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -10),

            cancelButton.topAnchor.constraint(equalTo: confirmButton.bottomAnchor, constant: 10),
            cancelButton.leadingAnchor.constraint(equalTo: self.leadingAnchor, constant: 10),
            cancelButton.trailingAnchor.constraint(equalTo: self.trailingAnchor, constant: -10),
            cancelButton.bottomAnchor.constraint(equalTo: self.bottomAnchor, constant: -10)
        ])
    }

    @objc private func copyMessageToClipboard(_ sender: UILongPressGestureRecognizer) {
        if sender.state == .began {
            UIPasteboard.general.string = (sender.view as? UILabel)?.text
            let alert = UIAlertController(title: "Copied", message: "Message copied to clipboard", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
            if let viewController = UIApplication.shared.windows.first?.rootViewController {
                viewController.present(alert, animated: true, completion: nil)
            }
        }
    }

    @objc private func confirmButtonTapped() {
        VLLogToast.dismiss()
        confirmAction?()
    }

    @objc private func cancelButtonTapped() {
        VLLogToast.dismiss()
        cancelAction?()
    }

    static func show(title: String, message: String, confirmButtonTitle: String, cancelButtonTitle: String, confirmAction: @escaping () -> Void = {}, cancelAction: @escaping () -> Void = {}) {
        guard let window = UIApplication.shared.windows.first else { return }
        let toast = VLLogToast(title: title, message: message, confirmButtonTitle: confirmButtonTitle, cancelButtonTitle: cancelButtonTitle, confirmAction: confirmAction, cancelAction: cancelAction)
        shared = toast
        VLLogToast.showState = true
        window.addSubview(toast)

        NSLayoutConstraint.activate([
            toast.centerXAnchor.constraint(equalTo: window.centerXAnchor),
            toast.centerYAnchor.constraint(equalTo: window.centerYAnchor),
            toast.widthAnchor.constraint(equalToConstant: 250),
            toast.heightAnchor.constraint(equalToConstant: 200)
        ])
        
        toast.alpha = 0
        UIView.animate(withDuration: 0.3) {
            toast.alpha = 1
        }
    }

    static func isShow() -> Bool {
        return VLLogToast.showState
    }


    static func dismiss() {
        guard let toast = shared else { return }
        VLLogToast.showState = false
        toast.alpha = 0
        toast.removeFromSuperview()
        shared = nil
    }
}
