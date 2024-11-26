package com.agora.entfulldemo.webview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.core.view.isVisible
import com.agora.entfulldemo.R

class SSOWebViewActivity : WebViewActivity() {

    companion object {
        private const val TAG = "SSOWebViewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding?.apply {
            titleView.setTitle(getString(R.string.app_name))
            webView.clearCache(true)
            webView.clearHistory()
            webView.webViewClient = object : CustomWebView.MyWebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url.toString()

                    if (url.contains("v1/sso/callback") && !url.contains("redirect_uri")) {
                        Log.d(TAG, "start login url = $url")
                        viewEmpty.isVisible = true
                        showLoadingView()
                        return false
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    if (url.contains("v1/sso/callback") && !url.contains("redirect_uri")) {
                        Log.d(TAG, "onPageFinished url = $url")
                        injectJavaScript()
                    }
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    hideLoadingView()
                    binding.viewEmpty.isVisible = false
                    Log.e(TAG, "onReceivedError ${error?.description}")
                }
            }

            webView.addJavascriptInterface(WebAppInterface(this@SSOWebViewActivity), "Android")
        }
    }

    private fun injectJavaScript() {
        // Inject JavaScript code to retrieve JSON data
        val jsCode = """
        (function() {
             // Get the text content of the page
            var jsonResponse = document.body.innerText; // Assume JSON data is in the body of the page
             // Parse the JSON data
            try {
                var jsonData = JSON.parse(jsonResponse); // Parse it into a JSON object
                // Check if the code is 0
                if (jsonData.code === 0) {
                    // Call the Android interface and pass the token
                    Android.handleResponse(jsonData.data.token);
                } else {
                    // If the code is not 0, return the error message
                    Android.handleResponse("Error " + jsonData.msg);
                }
            } catch (e) {
                 // Handle JSON parsing errors
                Android.handleResponse("Error " + e.message);
            }
        })();
    """
        binding?.webView?.evaluateJavascript(jsCode, null)
    }

    inner class WebAppInterface(private val context: Context) {

        @JavascriptInterface
        fun handleResponse(response: String) {
            // Handle the returned JSON data or error message
            Log.d(TAG, "Received response: $response")

            // If it's a token, perform the corresponding action
            if (!response.startsWith("Error")) {
                Log.d(TAG, "handleResponse success response = $response")
                // Process the token, e.g., save it to SharedPreferences
                Log.d(TAG, "Token: $response")
                // Here you can save the token or perform other actions
                hideLoadingView()
                setResult(Activity.RESULT_OK, Intent().putExtra("token", response))
                finish()
            } else {
                Log.d(TAG, "handleResponse error response = $response")
                // Handle error messages
                hideLoadingView()
                binding.viewEmpty.isVisible = false
                Log.e(TAG, response)
            }
        }
    }
}