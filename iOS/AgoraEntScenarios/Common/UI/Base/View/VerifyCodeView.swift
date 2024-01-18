//
//  VerifyCodeView.swift
//  VoiceRoomBaseUIKit
//
// Created by Zhu Jichao on August 25, 2022
//

import UIKit
import ZSwiftBaseLib

public class VerifyCodeView: UIView {
    @objc var beginEdit: (() -> Void)?

    /// Input value change
    @objc var textValueChange: ((_ text: String) -> Void)?

    /// Input complete
    @objc var inputFinish: ((_ text: String) -> Void)?

    /// Number of verification code input fields
    @objc var inputTextNum: Int = 4

    /// Input field
    @objc lazy var textFiled: VRVerifyCodeTextView = {
        let textFiled = VRVerifyCodeTextView(frame: CGRect(x: self.padding, y: 0, width: self.frame.width - 2 * self.padding, height: self.frame.height)).backgroundColor(.clear).textColor(.clear).delegate(self)
        textFiled.tintColor = .darkText
        textFiled.keyboardType = .decimalPad
        textFiled.addTarget(self, action: #selector(textFiledDidChange(_:)), for: .editingChanged)
        textFiled.addTarget(self, action: #selector(textFiledDidEnd(_:)), for: .editingDidEnd)
        return textFiled
    }()

    /// Number of captcha
    @objc var codeViews: [VRVerifyCodeNumberView] = []

    /// The margin between the two sides of the verification code input box
    @objc var padding: CGFloat = 15

    /// Each CAPTCHA input field spacing
    @objc var spacing: CGFloat = 10

    /// Whether it is in input
    @objc var isInput = true

    override init(frame: CGRect) {
        super.init(frame: frame)
    }

    @objc convenience init(frame: CGRect, codeNumbers: Int, space: CGFloat, padding: CGFloat) {
        self.init(frame: frame)
        spacing = space
        self.padding = padding
        inputTextNum = codeNumbers
        addSubview(textFiled)
        initSubviews()
        NotificationCenter.default.addObserver(self, selector: #selector(keyboardShow(note:)), name: UIResponder.keyboardWillShowNotification, object: nil)

        NotificationCenter.default.addObserver(self, selector: #selector(keyboardHidden(note:)), name: UIResponder.keyboardWillHideNotification, object: nil)
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }

    func initSubviews() {
        let itemWidth = CGFloat(frame.width - padding * 2 - spacing * (CGFloat(inputTextNum) - 1)) / CGFloat(inputTextNum)
        for i in 0..<inputTextNum {
            let codeNumView = VRVerifyCodeNumberView(frame: CGRect(x: padding + CGFloat(i) * (spacing + itemWidth), y: 0, width: itemWidth, height: frame.height)).isUserInteractionEnabled(false).backgroundColor(.white).cornerRadius(8).layerProperties(UIColor(0xE4E3ED), 1)
            codeNumView.setCursorStatus(true)
            codeViews.append(codeNumView)
        }
        addSubViews(codeViews)
    }

    @available(*, unavailable)
    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

// MARK: - Public

public extension VerifyCodeView {
    @objc func cleanCodes() {
        textFiled.text = ""
        textFiledDidChange(textFiled)
        allCursorHidden()
    }

    @objc func allCursorHidden() {
        DispatchQueue.main.async {
            for i in 0..<self.codeViews.count {
                let codeView = self.codeViews[i]
                codeView.setCursorStatus(true)
                if codeView.getNum().count == 0 {
                    codeView.setBottomLineFocus(isFocus: false)
                }
            }
        }
    }
}

public extension VerifyCodeView {
    @objc func keyboardShow(note: Notification) {
        isInput = false
        textFiledDidChange(textFiled)
        isInput = true
    }

    @objc func keyboardHidden(note: Notification) {
        allCursorHidden()
    }
}

// MARK: - UITextViewDelegate
extension VerifyCodeView: UITextFieldDelegate {
    public func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        var inputText = textFiled.text ?? ""

        if string.count == 0 {
            if range.location != inputText.count - 1 {
                if inputText.count > 0 {
                    textFiled.text?.removeLast()
                    textFiledDidChange(textFiled)
                }
                return false
            }
        }

        if let tempRange = Range(range, in: inputText) {
            inputText = inputText.replacingCharacters(in: tempRange, with: string)
            let meetRegx = "[0-9]*"
            let characterSet = NSPredicate(format: "SELF MATCHES %@", meetRegx)
            if characterSet.evaluate(with: inputText) == false {
                return false
            }
        }

        if inputText.count > inputTextNum {
            return false
        }

        return true
    }

    @objc func textFiledDidChange(_ textFiled: UITextField) {
        let inputStr = textFiled.text ?? ""

        textValueChange?(inputStr)

        for i in 0..<codeViews.count {
            let codeView = codeViews[i]
            if i < inputStr.count {
                codeView.setNum(num: inputStr[String.Index(utf16Offset: i, in: inputStr)].description)
                codeView.setBottomLineFocus(isFocus: true)
                codeView.setCursorStatus(true)
            } else {
                if inputStr.count == 0, i == 0 {
                    codeView.setCursorStatus(false)
                    codeView.setBottomLineFocus(isFocus: true)
                    codeView.setNum(num: nil)
                } else {
                    codeView.setCursorStatus(i != inputStr.count)
                    codeView.setNum(num: nil)
                    codeView.setBottomLineFocus(isFocus: i == inputStr.count)
                }
            }
        }

        if isInput, inputStr.count >= inputTextNum {
            DispatchQueue.main.async {
                textFiled.resignFirstResponder()
            }
            allCursorHidden()
        }
    }

    @objc func textFiledDidEnd(_ textFiled: UITextField) {
        guard let inputStr = textFiled.text else { return }
        if isInput {
            inputFinish?(inputStr)
        }
    }

    public func textFieldShouldBeginEditing(_ textField: UITextField) -> Bool {
        if beginEdit != nil {
            beginEdit!()
        }
        return true
    }
}
