package com.agora.entfulldemo.webview;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.entfulldemo.R;
import com.agora.entfulldemo.databinding.AppActivityWebviewBinding;
import com.agora.entfulldemo.webview.constructor.DeviceInfo;
import com.agora.entfulldemo.webview.constructor.UserModel;
import com.agora.entfulldemo.webview.constructor.WebUsage;
import com.agora.entfulldemo.webview.constructor.WebUsageModel;

import io.agora.scene.base.bean.User;
import io.agora.scene.base.component.BaseViewBindingActivity;
import io.agora.scene.base.manager.UserManager;
import io.agora.scene.base.utils.GsonUtils;

/**
 *
 */
public class WebViewActivity extends BaseViewBindingActivity<AppActivityWebviewBinding> {
    public static final String EXTRA_URL = "url";

    /**
     * h5 url.
     */
    private String url = "https://www.agora.io/cn/about-us/";

    /**
     * Launch.
     *
     * @param context
     * @param url
     */
    public static void launch(Context context, String url) {
        Intent intent = new Intent(context, WebViewActivity.class);
        intent.putExtra(EXTRA_URL, url);
        context.startActivity(intent);
    }
    @Override
    protected AppActivityWebviewBinding getViewBinding(@NonNull LayoutInflater layoutInflater) {
        return AppActivityWebviewBinding.inflate(layoutInflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        setOnApplyWindowInsetsListener(getBinding().superLayout);
        url = getIntent().getStringExtra(EXTRA_URL);
        if (url.contains("privacy/service")) {
            getBinding().titleView.setTitle(getString(R.string.app_user_agreement));
        } else if (url.contains("about-us")) {
            getBinding().titleView.setTitle(getString(R.string.app_about_us));
        } else if (url.contains("privacy/privacy")) {
            getBinding().titleView.setTitle(getString(R.string.app_privacy_agreement));
        } else if (url.contains("privacy/libraries")) {
            getBinding().titleView.setTitle(getString(R.string.app_third_party_info_data_sharing));
        } else if (url.contains("ent-scenarios/pages/manifest")) {
            getBinding().titleView.setTitle(getString(R.string.app_personal_info_collection_checklist));
        }

        getBinding().webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                Log.d("zhangw","newProgress:"+newProgress);
                if (newProgress == 100) {
                    getBinding().progressBar.setVisibility(View.GONE);
                } else {
                    if (getBinding().progressBar.getVisibility() == View.GONE) {
                        getBinding().progressBar.setVisibility(View.VISIBLE);
                    }
                    getBinding().progressBar.setProgress(newProgress);
                }
                super.onProgressChanged(view, newProgress);
            }
        });

        getBinding().webView.loadUrl(url);

        getBinding().titleView.setLeftClick(v -> {
            if (getBinding().webView.canGoBack()) {
                getBinding().webView.goBack();
            } else {
                finish();
            }
        });

        getBinding().webView.setWebViewClient(new CustomWebView.MyWebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String title = view.getTitle();
                if (!TextUtils.isEmpty(title)) {
                    getBinding().titleView.setTitle(title);
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (getBinding().webView.canGoBack()) {
            getBinding().webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void addJavascriptInterface() {
        getBinding().webView.addJavascriptInterface(new Object() {
            /**
             * JS requests a form from the App
             * @param day
             * @param callbackFuncName
             */
            @JavascriptInterface
            public void fetchUsage(final int day, final String callbackFuncName) {
                runOnUiThread(() -> {
                    // Do nothing
                });
            }

            /**
             * The App injects data into JS
             * @noinspection checkstyle:MagicNumber
             */
            @JavascriptInterface
            public void updateUsage() {
                runOnUiThread(() -> {
                    User user = UserManager.getInstance().getUser();
                    UserModel userModel = new UserModel(user.headUrl, user.name, user.mobile);
                    String type = "model:" + Build.MODEL + "\n"
                            + "manufacturer：" + Build.MANUFACTURER + "\n"
                            + "os_version：" + Build.VERSION.RELEASE + "\n"
                            + "imsi：" + "";
                    DeviceInfo deviceInfo = new DeviceInfo(type, "content", 111);
                    WebUsageModel webUsageModel = new WebUsageModel(userModel, deviceInfo);
                    String usage = GsonUtils.Companion.covertToString(new WebUsage(webUsageModel));
                    // Return system information to the H5 page
                    getBinding().webView.loadUrl("javascript:updateUsage('" + usage + "')");
                });
            }
        }, "android");
    }
}
