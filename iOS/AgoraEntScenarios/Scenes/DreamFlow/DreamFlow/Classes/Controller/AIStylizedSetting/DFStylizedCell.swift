//
//  DFStylizedCell.swift
//  DreamFlow
//
//  Created by qinhui on 2024/9/20.
//

import UIKit

class DFStylizedCell: UITableViewCell {
    lazy var darkView: UIView = {
        let view = UIView()
        view.backgroundColor = .lightGray.withAlphaComponent(0.5)
        return view
    }()
    
    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        self.addSubview(darkView)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    func setUserInteractionEnabled(enabled: Bool) {
        isUserInteractionEnabled = enabled
        darkView.isHidden = enabled
    }
}
