import UIKit

struct TableViewGradualDirection: OptionSet {
    let rawValue: Int
    static let top = TableViewGradualDirection(rawValue: 1)
    static let bottom = TableViewGradualDirection(rawValue: 2)
}

class GradualTableView: UITableView {
    private var direction: TableViewGradualDirection = .top
    private var gradualValue: Any?
    
    init(frame: CGRect,
         style: UITableView.Style,
         direction: TableViewGradualDirection,
         gradualValue: Any) {
        super.init(frame: frame, style: style)
        self.direction = direction
        self.gradualValue = gradualValue
        addObserver(self, forKeyPath: "contentOffset", options: .new, context: nil)
        change()
    }
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        var topValue: NSNumber = 0, bottomValue: NSNumber = 0
        
        if direction.contains(.top) {
            if let value = gradualValue as? NSNumber {
                topValue = value
            } else if let valueArray = gradualValue as? [Any], let value = valueArray.first as? NSNumber {
                topValue = value
            }
        }
        
        if direction.contains(.bottom) {
            if let value = gradualValue as? NSNumber {
                bottomValue = value
            } else if let valueArray = gradualValue as? [Any], let value = valueArray.last as? NSNumber {
                bottomValue = value
            }
        }
        
        if layer.mask == nil {
            let maskLayer = CAGradientLayer()
            maskLayer.locations = [NSNumber(value: 0.0),
                                   topValue,
                                   NSNumber(value: 1 - bottomValue.doubleValue),
                                   NSNumber(value: 1.0)]
            maskLayer.bounds = CGRect(x: 0, 
                                      y: 0,
                                      width: frame.size.width,
                                      height: frame.size.height)
            maskLayer.anchorPoint = .zero
            layer.mask = maskLayer
        }
    }
    
    override func observeValue(forKeyPath keyPath: String?, 
                               of object: Any?,
                               change: [NSKeyValueChangeKey : Any]?,
                               context: UnsafeMutableRawPointer?) {
        if keyPath == "contentOffset" {
            self.change()
        }
    }
    
    deinit {
        removeObserver(self, forKeyPath: "contentOffset")
    }
    
    private func change() {
        let outerColor = UIColor(white: 1.0, alpha: 0.0).cgColor
        let innerColor = UIColor(white: 1.0, alpha: 1.0).cgColor
        
        var colors: [Any] = []
        
        if contentOffset.y + contentInset.top <= 0 {
            // Top of scrollView
            colors = [innerColor, innerColor, innerColor, outerColor]
        } else if contentOffset.y + frame.size.height >= contentSize.height {
            // Bottom of tableView
            colors = [outerColor, innerColor, innerColor, innerColor]
        } else {
            // Middle
            colors = [outerColor, innerColor, innerColor, outerColor]
        }
        
        (layer.mask as? CAGradientLayer)?.colors = colors
        
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        layer.mask?.position = CGPoint(x: 0, y: contentOffset.y)
        CATransaction.commit()
    }
}
