//
//  CommerceNumberButton.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/20.
//

import UIKit

public typealias ResultClosure = (_ number: String, _ isIncrease: Bool?)->()

public protocol CommerceNumberButtonDelegate: NSObjectProtocol {
    func numberButtonResult(_ numberButton: CommerceNumberButton, number: String, isIncrease: Bool)
}

@IBDesignable open class CommerceNumberButton: UIView {
    weak var delegate: CommerceNumberButtonDelegate?  // Proxy
    var NumberResultClosure: ResultClosure?     // Closure
    lazy var decreaseBtn: UIButton = {
        let decreaseBtn = setupButton(title: "－")
        return decreaseBtn
    }()
    lazy var increaseBtn: UIButton = {
        let increaseBtn = setupButton(title: "＋")
        return increaseBtn
    }()
    lazy var textField: UITextField = {
        let textField = UITextField.init()
        textField.text = "1"
        textField.font = UIFont.boldSystemFont(ofSize: 15)
        textField.delegate = self
        textField.keyboardType = .numberPad
        textField.textAlignment = .center
        return textField
    }()
    
    public var _minValue = 1                 // Maximum value
    public var _maxValue = Int.max           // Maximum value
    public var shakeAnimation: Bool = false  // Do you want to turn on the shake animation
    public var borderColor: UIColor? {
        didSet{
            layer.borderColor = borderColor?.cgColor
        }
    }
    public var borderWidth: CGFloat = 0 {
        didSet {
            layer.borderWidth = borderWidth
        }
    }
    public var textFieldBorderColor: UIColor? {
        didSet {
            textField.layer.borderColor = textFieldBorderColor?.cgColor
        }
    }
    public var textFieldHighlightBorderColor: UIColor?
    public var textFieldBorderWidth: CGFloat = 0 {
        didSet {
            textField.layer.borderWidth = textFieldBorderWidth
        }
    }
    private lazy var textFieldTopBorderView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    private lazy var textFieldBottomBorderView: UIView = {
        let view = UIView()
        view.translatesAutoresizingMaskIntoConstraints = false
        return view
    }()
    private var topCons: NSLayoutConstraint?
    private var bottomCons: NSLayoutConstraint?
    private var currentValue: Int = 0
    
    override public init(frame: CGRect) {
        super.init(frame: frame)
        
        setupUI()
        // The default size of the entire control (the same size as the button on a certain treasure)
        if frame.isEmpty {self.frame = CGRect(x: 0, y: 0, width: 110, height: 30)}
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        
    }
    
    override open func awakeFromNib() {
        setupUI()
    }
    
    // Set UI layout
    fileprivate func setupUI() {
        backgroundColor = .white
        layer.cornerRadius = 3.0
//        clipsToBounds = true
        addSubview(decreaseBtn)
        addSubview(increaseBtn)
        addSubview(textField)
        addSubview(textFieldTopBorderView)
        addSubview(textFieldBottomBorderView)
//        textFieldTopBorderView.leadingAnchor.constraint(equalTo: textField.leadingAnchor).isActive = true
//        textFieldTopBorderView.topAnchor.constraint(equalTo: textField.topAnchor).isActive = true
//        textFieldTopBorderView.trailingAnchor.constraint(equalTo: textField.trailingAnchor).isActive = true
//        topCons = textFieldTopBorderView.heightAnchor.constraint(equalToConstant: textFieldBorderWidth)
//        
//        textFieldBottomBorderView.leadingAnchor.constraint(equalTo: textField.leadingAnchor).isActive = true
//        textFieldBottomBorderView.trailingAnchor.constraint(equalTo: textField.trailingAnchor).isActive = true
//        textFieldBottomBorderView.bottomAnchor.constraint(equalTo: textField.bottomAnchor).isActive = true
//        bottomCons = textFieldBottomBorderView.heightAnchor.constraint(equalToConstant: textFieldBorderWidth)
    }
    
    override open func layoutSubviews() {
        super.layoutSubviews()
        
        let height = frame.size.height
        let width = frame.size.width
        decreaseBtn.frame = CGRect(x: 0, y: 0, width: height, height: height)
        increaseBtn.frame = CGRect(x: width - height, y: 0, width: height, height: height)
        textField.frame = CGRect(x: height, y: 0, width: width - 2.0*height, height: height)
        textFieldTopBorderView.frame = CGRect(x: height, y: 1, width: width - 2.0*height, height: textFieldBorderWidth)
        textFieldBottomBorderView.frame = CGRect(x: height, y: height - 2, width: width - 2.0*height, height: textFieldBorderWidth)
    }
    
    // Public method for setting addition and subtraction buttons
    fileprivate func setupButton(title: String) -> UIButton {
        let button = UIButton()
        button.setTitle(title, for: UIControl.State())
        button.setTitleColor(UIColor.gray, for: UIControl.State())
        button.addTarget(self, action:#selector(self.touchDown(_:)), for: .touchUpInside)
        return button
    }
    
    // MARK: - Add/subtract button click response
    // Click button: Click to add and subtract one by one, long press to continue adding and subtracting
    @objc fileprivate func touchDown(_ button: UIButton) {
        textField.endEditing(false)
        if button == decreaseBtn {
            decrease()
            
        } else {
            increase()
        }
    }
    
    // MARK: - Subtraction operation
    @objc fileprivate func decrease() {
        guard let text = textField.text else { return }
        if text.count == 0 || (Int(text) ?? 0) <= _minValue {
            textField.text = "\(_minValue)"
        }
        
        let number = (Int(text) ?? 0) - 1;
        if number >= _minValue {
            textField.text = "\(number)";
            NumberResultClosure?("\(number)", false)
            delegate?.numberButtonResult(self, number: "\(number)", isIncrease: false)
            
        } else {
            if shakeAnimation {shakeAnimationFunc()}
            print("The quantity cannot be less than\(_minValue)");
        }
        currentValue = number
    }
    
    // MARK: - addition operation
    @objc fileprivate func increase() {
        guard let text = textField.text else { return }
        if text.count == 0 || (Int(text) ?? 0) <= _minValue {
            textField.text = "\(_minValue)"
        }
        let number = (Int(text) ?? 0) + 1;
        if number <= _maxValue {
            textField.text = "\(number)";
            NumberResultClosure?("\(number)", true)
            delegate?.numberButtonResult(self, number: "\(number)", isIncrease: true)
            
        } else {
            if shakeAnimation {shakeAnimationFunc()}
            print("The maximum quantity has been exceeded \(_maxValue)");
        }
        currentValue = number
    }
    
    // MARK: - Jitter animation
    fileprivate func shakeAnimationFunc() {
        let animation = CAKeyframeAnimation.init(keyPath: "position.x")
        let positionX = layer.position.x
        animation.values = [positionX-10, positionX, positionX+10]
        animation.repeatCount = 3
        animation.duration = 0.07
        animation.autoreverses = true
        layer.add(animation, forKey: nil)
    }
}

extension CommerceNumberButton: UITextFieldDelegate {
    public func textFieldShouldBeginEditing(_ textField: UITextField) -> Bool {
        textField.layer.borderColor = textFieldHighlightBorderColor?.cgColor
        textFieldTopBorderView.backgroundColor = textFieldHighlightBorderColor
        textFieldBottomBorderView.backgroundColor = textFieldHighlightBorderColor
        return true
    }
    public func textFieldDidEndEditing(_ textField: UITextField) {
        guard let text = textField.text else { return }
        if text.count == 0 || (Int(text) ?? 0) < _minValue {
            textField.text = "\(_minValue)"
        }
        if (Int(text) ?? 0) > _maxValue {
            textField.text = "\(_maxValue)"
        }
        textField.layer.borderColor = textFieldBorderColor?.cgColor
        textFieldTopBorderView.backgroundColor = .clear
        textFieldBottomBorderView.backgroundColor = .clear
        // Closure callback
        let isIncrease = currentValue >= (Int(text) ?? 0) ? true : false
        NumberResultClosure?("\(text)", nil)
        // Callback for delegate
        delegate?.numberButtonResult(self, number: "\(text)", isIncrease: isIncrease)
    }
    public func textField(_ textField: UITextField, shouldChangeCharactersIn range: NSRange, replacementString string: String) -> Bool {
        let currentText = textField.text ?? ""
        guard let stringRange = Range(range, in: currentText) else { return false }
        
        let updatedText = currentText.replacingCharacters(in: stringRange, with: string)
        
        if let intValue = Int(updatedText), intValue >= minValue, intValue <= maxValue {
            return true
        } else {
            return updatedText.isEmpty ? true : false
        }
    }
}

// MARK: - Custom UI interface
public extension CommerceNumberButton {
    /**
     The content in the input box
     */
    var currentNumber: String? {
        get {
            return textField.text
        }
        set {
            textField.text = newValue
            currentValue = Int(newValue ?? "0") ?? 0
        }
    }
    /**
     Set minimum value
     */
    var minValue: Int {
        get {
            return _minValue
        }
        set {
            _minValue = newValue
            textField.text = "\(newValue)"
        }
    }
    /**
     Set maximum value
     */
    var maxValue: Int {
        get {
            return _maxValue
        }
        set {
            _maxValue = newValue
        }
    }
    
    /**
     Response closure callback for addition and subtraction buttons
     */
    func numberResult(_ finished: @escaping ResultClosure) {
        NumberResultClosure = finished
    }
    
    /**
     Font properties in the input box
     */
    func inputFieldFont(_ inputFieldFont: UIFont) {
        textField.font = inputFieldFont
    }
    
    /**
     Font properties of addition and subtraction buttons
     */
    func buttonTitleFont(_ buttonTitleFont: UIFont) {
        increaseBtn.titleLabel?.font = buttonTitleFont
        decreaseBtn.titleLabel?.font = buttonTitleFont
    }
    
    /**
     Set the border color of the button
     */
    func borderColor(_ borderColor: UIColor) {
        layer.borderColor = borderColor.cgColor
        decreaseBtn.layer.borderColor = borderColor.cgColor
        increaseBtn.layer.borderColor = borderColor.cgColor
        
        layer.borderWidth = 0.5
        decreaseBtn.layer.borderWidth = 0.5
        increaseBtn.layer.borderWidth = 0.5
    }
    
    // Note: The title and background image of the plus and minus buttons can only be set to one of them. If all are set, the type set last will prevail
    
    /**
     Set the title of the add/subtract button
     
     -Parameter decreaseTitle: Subtract button title
     -Parameter incrementTitle: Add button title
     */
    func setTitle(decreaseTitle: String, increaseTitle: String) {
        decreaseBtn.setBackgroundImage(nil, for: UIControl.State())
        increaseBtn.setBackgroundImage(nil, for: UIControl.State())
        
        decreaseBtn.setTitle(decreaseTitle, for: UIControl.State())
        increaseBtn.setTitle(increaseTitle, for: UIControl.State())
    }
    
    /**
     Set background image for add/subtract buttons
     
     -Parameter decreaseImage: Background image of the subtract button
     -Parameter incrementImage: Add button background image
     */
    func setImage(decreaseImage: UIImage, increaseImage: UIImage) {
        decreaseBtn.setTitle(nil, for: UIControl.State())
        increaseBtn.setTitle(nil, for: UIControl.State())
        
        decreaseBtn.setBackgroundImage(decreaseImage, for: UIControl.State())
        increaseBtn.setBackgroundImage(increaseImage, for: UIControl.State())
    }
}
