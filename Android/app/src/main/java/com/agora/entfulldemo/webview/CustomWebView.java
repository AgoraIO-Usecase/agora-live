package com.agora.entfulldemo.webview;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * The type Custom web view.
 */
public class CustomWebView extends WebView {

    /**
     * Instantiates a new Custom web view.
     *
     * @param context the context
     */
    public CustomWebView(@NonNull Context context) {
        super(context);
        initView();
    }

    /**
     * Instantiates a new Custom web view.
     *
     * @param context the context
     * @param attrs   the attrs
     */
    public CustomWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    /**
     * Instantiates a new Custom web view.
     *
     * @param context      the context
     * @param attrs        the attrs
     * @param defStyleAttr the def style attr
     */
    public CustomWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    /**
     * Init view.
     */
    private void initView() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setUseWideViewPort(false);
        getSettings().setDomStorageEnabled(true);
        setWebViewClient(new MyWebViewClient());
        getSettings().setAllowFileAccess(true);
        getSettings().setDatabaseEnabled(true);
        getSettings().setSaveFormData(true);
        getSettings().setAppCacheEnabled(true);
        getSettings().setLoadWithOverviewMode(true);
        getSettings().setSupportZoom(false);
        getSettings().setDefaultTextEncodingName("UTF-8");
    }

    /**
     * The type My web view client.
     */
    public static class MyWebViewClient extends WebViewClient {
        /**
         * On page finished.
         *
         * @param view the view
         * @param url  the url
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }

        /**
         * Should override url loading boolean.
         *
         * @param view the view
         * @param url  the url
         * @return the boolean
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith("http") || url.startsWith("https") || url.startsWith("ftp") || url.startsWith("file:///android_asset")) {
                if (url.startsWith("file:///android_asset")) {
                    WebViewActivity.launch(view.getContext(), url);
                    return true;
                }
                return false;
            }
            return true;
        }
    }

}
