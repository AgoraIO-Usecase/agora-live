//
//  DF.swift
//  DreamFlow
//
//  Created by qinhui on 2024/10/14.
//

import Foundation

class DFPlaceholderTextView: UITextView {
    private var placeholderLabel: UILabel = UILabel()

    var placeholder: String? {
        didSet {
            placeholderLabel.text = placeholder
            placeholderLabel.sizeToFit()
        }
    }

    override var text: String! {
        didSet {
            textDidChange()
        }
    }

    override var attributedText: NSAttributedString! {
        didSet {
            textDidChange()
        }
    }

    override init(frame: CGRect, textContainer: NSTextContainer?) {
        super.init(frame: frame, textContainer: textContainer)
        setupPlaceholder()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupPlaceholder()
    }

    private func setupPlaceholder() {
        placeholderLabel.textColor = UIColor.show_segment_title_nor
        placeholderLabel.font = UIFont.show_R_12
        placeholderLabel.textAlignment = .left
        placeholderLabel.numberOfLines = 0
        addSubview(placeholderLabel)
        
        // 使用 SnapKit 设置约束
        placeholderLabel.snp.makeConstraints { make in
            make.leading.equalTo(9)
            make.trailing.equalTo(-8)
            make.top.equalTo(6)
        }
        NotificationCenter.default.addObserver(self, selector: #selector(textDidChange), name: UITextView.textDidChangeNotification, object: nil)
    }

    @objc private func textDidChange() {
        placeholderLabel.isHidden = !self.text.isEmpty
    }

    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}
