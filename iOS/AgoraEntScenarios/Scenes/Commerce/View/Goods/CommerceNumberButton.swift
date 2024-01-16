//
//  CommerceNumberButton.swift
//  AgoraEntScenarios
//
//  Created by zhaoyongqiang on 2023/12/20.
//

import UIKit

public typealias ResultClosure = (_ number: String)->()

public protocol CommerceNumberButtonDelegate: NSObjectProtocol {
    func numberButtonResult(_ numberButton: CommerceNumberButton, number: String)
}

@IBDesignable open class CommerceNumberButton: UIView {
    weak var delegate: CommerceNumberButtonDelegate?  // 代理
    var NumberResultClosure: ResultClosure?     // 闭包
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
    private var timer: Timer!              // 快速加减定时器
    public var _minValue = 1                 // 最大值
    public var _maxValue = Int.max           // 最大值
    public var shakeAnimation: Bool = false  // 是否打开抖动动画
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
    
    override public init(frame: CGRect) {
        super.init(frame: frame)
        
        setupUI()
        //整个控件的默认尺寸(和某宝上面的按钮同样大小)
        if frame.isEmpty {self.frame = CGRect(x: 0, y: 0, width: 110, height: 30)}
    }
    
    required public init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        
    }
    
    override open func awakeFromNib() {
        setupUI()
    }
    
    //设置UI布局
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
    
    //设置加减按钮的公共方法
    fileprivate func setupButton(title: String) -> UIButton {
        let button = UIButton()
        button.setTitle(title, for: UIControl.State())
        button.setTitleColor(UIColor.gray, for: UIControl.State())
        button.addTarget(self, action:#selector(self.touchDown(_:)), for: .touchDown)
        button.addTarget(self, action:#selector(self.touchUp), for: .touchUpOutside)
        button.addTarget(self, action:#selector(self.touchUp), for: .touchUpInside)
        button.addTarget(self, action:#selector(self.touchUp), for: .touchCancel)
        return button
    }
    
    // MARK: - 加减按钮点击响应
    //点击按钮: 单击逐次加减,长按连续加减
    @objc fileprivate func touchDown(_ button: UIButton) {
        textField.endEditing(false)
        if button == decreaseBtn {
            timer = Timer.scheduledTimer(timeInterval: 0.15, 
                                         target: self,
                                         selector: #selector(self.decrease),
                                         userInfo: nil,
                                         repeats: true)
        } else {
            timer = Timer.scheduledTimer(timeInterval: 0.15, 
                                         target: self,
                                         selector: #selector(self.increase),
                                         userInfo: nil,
                                         repeats: true)
        }
        timer.fire()
    }
    
    //松开按钮:清除定时器
    @objc fileprivate func touchUp()  {
        cleanTimer()
    }
    
    // MARK: - 减运算
    @objc fileprivate func decrease() {
        guard let text = textField.text else { return }
        if text.count == 0 || (Int(text) ?? 0) <= _minValue {
            textField.text = "\(_minValue)"
        }
        
        let number = (Int(text) ?? 0) - 1;
        if number >= _minValue {
            textField.text = "\(number)";
            NumberResultClosure?("\(number)")
            delegate?.numberButtonResult(self, number: "\(number)")
            
        } else {
            if shakeAnimation {shakeAnimationFunc()}
            print("数量不能小于\(_minValue)");
        }
    }
    
    // MARK: - 加运算
    @objc fileprivate func increase() {
        guard let text = textField.text else { return }
        if text.count == 0 || (Int(text) ?? 0) <= _minValue {
            textField.text = "\(_minValue)"
        }
        let number = (Int(text) ?? 0) + 1;
        if number <= _maxValue {
            textField.text = "\(number)";
            NumberResultClosure?("\(number)")
            delegate?.numberButtonResult(self, number: "\(number)")
            
        } else {
            if shakeAnimation {shakeAnimationFunc()}
            print("已超过最大数量\(_maxValue)");
        }
    }
    
    // MARK: - 抖动动画
    fileprivate func shakeAnimationFunc() {
        let animation = CAKeyframeAnimation.init(keyPath: "position.x")
        let positionX = layer.position.x
        animation.values = [positionX-10, positionX, positionX+10]
        animation.repeatCount = 3
        animation.duration = 0.07
        animation.autoreverses = true
        layer.add(animation, forKey: nil)
    }
    
    fileprivate func cleanTimer() {
        if timer?.isValid != nil {
            timer.invalidate()
            timer = nil
        }
    }
    
    deinit {
        cleanTimer()
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
        //闭包回调
        NumberResultClosure?("\(text)")
        //delegate的回调
        delegate?.numberButtonResult(self, number: "\(text)")
    }
}

// MARK: - 自定义UI接口
public extension CommerceNumberButton {
    /**
     输入框中的内容
     */
    var currentNumber: String? {
        get {
            return textField.text
        }
        set {
            textField.text = newValue
        }
    }
    /**
     设置最小值
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
     设置最大值
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
     加减按钮的响应闭包回调
     */
    func numberResult(_ finished: @escaping ResultClosure) {
        NumberResultClosure = finished
    }
    
    /**
     输入框中的字体属性
     */
    func inputFieldFont(_ inputFieldFont: UIFont) {
        textField.font = inputFieldFont
    }
    
    /**
     加减按钮的字体属性
     */
    func buttonTitleFont(_ buttonTitleFont: UIFont) {
        increaseBtn.titleLabel?.font = buttonTitleFont
        decreaseBtn.titleLabel?.font = buttonTitleFont
    }
    
    /**
     设置按钮的边框颜色
     */
    func borderColor(_ borderColor: UIColor) {
        layer.borderColor = borderColor.cgColor
        decreaseBtn.layer.borderColor = borderColor.cgColor
        increaseBtn.layer.borderColor = borderColor.cgColor
        
        layer.borderWidth = 0.5
        decreaseBtn.layer.borderWidth = 0.5
        increaseBtn.layer.borderWidth = 0.5
    }
    
    //注意:加减号按钮的标题和背景图片只能设置其中一个,若全部设置,则以最后设置的类型为准
    
    /**
     设置加/减按钮的标题
     
     - parameter decreaseTitle: 减按钮标题
     - parameter increaseTitle: 加按钮标题
     */
    func setTitle(decreaseTitle: String, increaseTitle: String) {
        decreaseBtn.setBackgroundImage(nil, for: UIControl.State())
        increaseBtn.setBackgroundImage(nil, for: UIControl.State())
        
        decreaseBtn.setTitle(decreaseTitle, for: UIControl.State())
        increaseBtn.setTitle(increaseTitle, for: UIControl.State())
    }
    
    /**
     设置加/减按钮的背景图片
     
     - parameter decreaseImage: 减按钮背景图片
     - parameter increaseImage: 加按钮背景图片
     */
    func setImage(decreaseImage: UIImage, increaseImage: UIImage) {
        decreaseBtn.setTitle(nil, for: UIControl.State())
        increaseBtn.setTitle(nil, for: UIControl.State())
        
        decreaseBtn.setBackgroundImage(decreaseImage, for: UIControl.State())
        increaseBtn.setBackgroundImage(increaseImage, for: UIControl.State())
    }
}
