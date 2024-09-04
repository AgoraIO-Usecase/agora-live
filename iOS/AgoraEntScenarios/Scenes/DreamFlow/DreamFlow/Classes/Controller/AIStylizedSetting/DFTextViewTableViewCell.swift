//
//  TextViewTableViewCell.swift
//  DreamFlow
//
//  Created by qinhui on 2024/8/30.
//

import UIKit
import SnapKit

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

class DFTextViewTableViewCell: DFHorizontalScrollTableViewCell {
    let textView = DFPlaceholderTextView()
    var inputHandler: ((String) -> Void)?
    var enableInput: Bool = true {
        didSet {
            textView.userInteractionEnabled(enableInput)
        }
    }
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        
        textView.layer.cornerRadius = 20
        textView.backgroundColor = UIColor.show_segment_bg
        textView.isScrollEnabled = true
        textView.layer.borderWidth = 1
        textView.layer.borderColor = UIColor.show_segment_border.cgColor
        textView.textContainer.maximumNumberOfLines = 0
        textView.textContainerInset = UIEdgeInsets(top: 8, left: 8, bottom: 8, right: 8)
        textView.placeholder = "Customized Prompt"
        textView.delegate = self
        
        contentView.addSubview(textView)
        
        textView.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(20)
            make.trailing.equalToSuperview().offset(-20)
            make.top.equalTo(scrollView.snp.bottom).offset(8)
            make.bottom.equalToSuperview().offset(-8)
            make.height.equalTo(120)
            make.bottom.equalToSuperview().offset(-8)
        }
    }
    
    override func configSubViews() {
        titleLabel.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(20)
            make.top.equalToSuperview().offset(8)
        }
        
        scrollView.snp.makeConstraints { make in
            make.leading.equalToSuperview().offset(20)
            make.trailing.equalToSuperview().offset(-20)
            make.top.equalTo(titleLabel.snp.bottom).offset(8)
            make.height.equalTo(100)
        }
        
        stackView.snp.makeConstraints { make in
            make.edges.equalToSuperview()
            make.height.equalToSuperview()
        }
        
    }
    
    override func execHandler() {
        if selectedItem.title == "Customized" {
            textView.userInteractionEnabled(true)
        } else {
            textView.userInteractionEnabled(false)
        }
        
        super.execHandler()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

extension DFTextViewTableViewCell: UITextViewDelegate {
    func textViewDidChange(_ textView: UITextView) {
        if let handler = inputHandler {
            handler(textView.text)
        }
    }
}
