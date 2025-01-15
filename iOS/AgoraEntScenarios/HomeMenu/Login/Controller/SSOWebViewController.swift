//
//  SSOWebViewController.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/11/18.
//

import UIKit
import WebKit
import SVProgressHUD

struct CustomBarButtonItem {
    var title: String?
    var image: UIImage?
    weak var target: AnyObject?
    var action: Selector
}


class CustomNavigationBar: UIView {
    var title: String? {
        didSet {
            titleLabel.text = title
        }
    }
    
    var rightItems: [CustomBarButtonItem]? {
        didSet {
            addRightBarButtonItems(rightItems)
        }
    }
    
    var leftItems: [CustomBarButtonItem]? {
        didSet {
            addLeftBarButtonItems(leftItems)
        }
    }
    
    private var leftButtons = [UIButton]()
    private var rightButtons = [UIButton]()
    
    lazy var titleLabel: UILabel = {
        let titleLabel = UILabel()
        addSubview(titleLabel)
        titleLabel.text = "navi_title_show_live".show_localized
        titleLabel.textColor = .black
        titleLabel.font = .show_navi_title
        return titleLabel
    }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.frame = CGRect(x: 0, y: 0, width: Screen.width, height: Screen.safeAreaTopHeight() + 44)
        createSubviews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func createSubviews(){
        
        addSubview(titleLabel)
        titleLabel.snp.makeConstraints { make in
            make.top.equalTo(16 + Screen.safeAreaTopHeight())
            make.centerX.equalToSuperview()
        }
        
        setLeftButtonTarget(self, action: #selector(didClickLeftButtonAction))
    }
    
    func setLeftButtonTarget(_ target: AnyObject, action: Selector, image: UIImage? = UIImage.show_sceneImage(name: "show_navi_back"), title: String? = nil ) {
        self.leftItems = nil
        let item = CustomBarButtonItem(title: title, image: image ,target: target, action: action)
        self.leftItems = [item]
    }
   
}

extension CustomNavigationBar {
    
    @objc private func didClickLeftButtonAction(){
        currentNavigationController()?.popViewController(animated: true)
    }
    
    private func currentNavigationController() -> UINavigationController? {
        var nextResponder = next
        while (nextResponder is UINavigationController || nextResponder == nil) == false {
            nextResponder = nextResponder?.next
        }
        return nextResponder as? UINavigationController
    }
    
    private func addRightBarButtonItems(_ items: [CustomBarButtonItem]?) {
        if items == nil {
            for button in rightButtons {
                button.removeFromSuperview()
            }
            return
        }
        var firstButton: UIButton?
        for item in items! {
            let button = createBarButton(item: item)
            addSubview(button)
            rightButtons.append(button)
            button.snp.makeConstraints { make in
                if firstButton == nil {
                    firstButton = button
                    make.right.equalTo(-20)
                }else{
                    make.right.equalTo(firstButton!.snp.left).offset(-25)
                }
                make.centerY.equalTo(titleLabel)
            }
        }
    }
    
    private func addLeftBarButtonItems(_ items: [CustomBarButtonItem]?) {
        if items == nil {
            for button in leftButtons {
                button.removeFromSuperview()
            }
            return
        }
        var firstButton: UIButton?
        for item in items! {
            let button = createBarButton(item: item)
            addSubview(button)
            leftButtons.append(button)
            button.snp.makeConstraints { make in
                if firstButton == nil {
                    firstButton = button
                    make.left.equalTo(20)
                }else{
                    make.left.equalTo(firstButton!.right).offset(25)
                }
                make.centerY.equalTo(titleLabel)
            }
        }
    }
    
    private func createBarButton(item: CustomBarButtonItem) -> UIButton {
        let button = UIButton(type: .custom)
        button.setTitle(item.title, for: .normal)
        button.setImage(item.image, for: .normal)
        button.setTitleColor(.show_zi03, for: .normal)
        button.titleLabel?.font = .show_R_14
        button.addTarget(item.target, action: item.action, for: .touchUpInside)
        return button
    }
}

@objc class SSOWebViewController: UIViewController {
    private lazy var naviBar: CustomNavigationBar = {
        let view = CustomNavigationBar()
        view.title = "Agora Live"
        view.backgroundColor = .white
        view.setLeftButtonTarget(self, action: #selector(didClickCanelButton))
        return view
    }()

    var urlString: String = ""
    
    private lazy var ssoWebView: WKWebView = {
        // Config WKWebView
        let configuration = WKWebViewConfiguration()
        let userContentController = WKUserContentController()
        
        // Register JavaScript callback
        userContentController.add(self, name: "handleResponse")
        configuration.userContentController = userContentController
        
        let view = WKWebView(frame: CGRectZero, configuration: configuration)
        view.navigationDelegate = self
        view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        return view
    }()
    
    var completionHandler: ((String?) -> Void)?
    
    lazy var emptyView: UIView = {
        let view = UIView()
        view.backgroundColor = .white
        return view
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        setupUI()
        
        if let url = URL(string: urlString) {
            let request = URLRequest(url: url)
            ssoWebView.load(request)
        }
    }
    
    private func setupUI() {
        view.addSubview(naviBar)
        view.addSubview(ssoWebView)
        view.addSubview(emptyView)
        
        ssoWebView.snp.makeConstraints { make in
            make.left.right.bottom.equalTo(0)
            make.top.equalTo(naviBar.snp.bottom)
        }
        
        emptyView.snp.makeConstraints { make in
            make.left.right.bottom.equalTo(0)
            make.top.equalTo(naviBar.snp.bottom)
        }
        
        emptyView.isHidden = true

    }
    
    @objc private func didClickCanelButton() {
        SVProgressHUD.dismiss()
        self.navigationController?.popViewController(animated: true)
    }
    
    // MARK: - JavaScript Injection
    private func injectJavaScript() {
        let jsCode = """
        (function() {
            var jsonResponse = document.body.innerText;
            try {
                var jsonData = JSON.parse(jsonResponse);
                if (jsonData.code === 0) {
                    window.webkit.messageHandlers.handleResponse.postMessage({
                        token: jsonData.data.token,
                        error: null
                    });
                } else {
                    window.webkit.messageHandlers.handleResponse.postMessage({
                        token: null,
                        error: jsonData.msg
                    });
                }
            } catch (e) {
                window.webkit.messageHandlers.handleResponse.postMessage({
                    token: null,
                    error: e.message
                });
            }
        })();
        """
        
        ssoWebView.evaluateJavaScript(jsCode, completionHandler: nil)
    }
    
    @objc static func clearWebViewCache() {
        WKWebsiteDataStore.default().removeData(
            ofTypes: WKWebsiteDataStore.allWebsiteDataTypes(),
            modifiedSince: Date(timeIntervalSince1970: 0)
        ) { }
        
        let dataStore = URLCache.shared
        dataStore.removeAllCachedResponses()

        
        let cookieStorage = HTTPCookieStorage.shared
        for cookie in cookieStorage.cookies ?? [] {
            cookieStorage.deleteCookie(cookie)
        }
    }
}

// MARK: - WKNavigationDelegate
extension SSOWebViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        SVProgressHUD.show()
    }
    
    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let url = navigationAction.request.url {
            if url.absoluteString.contains("v1/sso/callback") && !url.absoluteString.contains("redirect_uri") {
                emptyView.isHidden = false
            }
        }
        
        decisionHandler(.allow)
    }
    
    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        SVProgressHUD.show(withStatus: error.localizedDescription)
    }
    
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        SVProgressHUD.dismiss()
        webView.evaluateJavaScript("document.title") { [weak self] (result, error) in
            if let title = result as? String {
                self?.title = title
            }
        }
        injectJavaScript()
    }
}

// MARK: - WKScriptMessageHandler
extension SSOWebViewController: WKScriptMessageHandler {
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        if message.name == "handleResponse" {
            guard let dict = message.body as? [String: Any] else { return }
            
            if let token = dict["token"] as? String {
                completionHandler?(token)
            } else if let error = dict["error"] as? String {
                print("Error: \(error)")
                completionHandler?(nil)
            }
        }
    }
}
