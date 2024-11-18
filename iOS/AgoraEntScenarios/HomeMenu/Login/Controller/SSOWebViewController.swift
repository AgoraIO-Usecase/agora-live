//
//  SSOWebViewController.swift
//  AgoraEntScenarios
//
//  Created by qinhui on 2024/11/18.
//

import UIKit
import WebKit
import SVProgressHUD

class SSOWebViewController: UIViewController {
    // MARK: - Properties
    var urlString: String = ""
    private var webView: WKWebView!
    var completionHandler: ((String?) -> Void)?
    
    // MARK: - Lifecycle
    override func viewDidLoad() {
        super.viewDidLoad()
        setupWebView()
    }
    
    // MARK: - Setup
    private func setupWebView() {
        // Config WKWebView
        let configuration = WKWebViewConfiguration()
        let userContentController = WKUserContentController()
        
        // Register JavaScript callback
        userContentController.add(self, name: "handleResponse")
        configuration.userContentController = userContentController
        
        webView = WKWebView(frame: view.bounds, configuration: configuration)
        webView.navigationDelegate = self
        webView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(webView)
        
        if let url = URL(string: urlString) {
            let request = URLRequest(url: url)
            webView.load(request)
        }
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
        
        webView.evaluateJavaScript(jsCode, completionHandler: nil)
    }
}

// MARK: - WKNavigationDelegate
extension SSOWebViewController: WKNavigationDelegate {
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        SVProgressHUD.show()
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
                dismiss(animated: true)
            } else if let error = dict["error"] as? String {
                print("Error: \(error)")
                completionHandler?(nil)
                dismiss(animated: true)
            }
        }
    }
}
