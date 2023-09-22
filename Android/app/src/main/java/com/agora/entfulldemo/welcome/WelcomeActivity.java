package com.agora.entfulldemo.welcome;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.entfulldemo.R;
import com.agora.entfulldemo.databinding.AppActivityWelcomeBinding;
import com.agora.entfulldemo.home.MainActivity;
import com.agora.entfulldemo.webview.WebViewActivity;

import io.agora.scene.base.component.BaseViewBindingActivity;
import io.agora.scene.base.manager.UserManager;

/**
 * The type Welcome activity.
 */
public class WelcomeActivity extends BaseViewBindingActivity<AppActivityWelcomeBinding> {

    @Override
    protected AppActivityWelcomeBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return AppActivityWelcomeBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        String policyText = getString(R.string.app_policy_accept);
        String[] split = policyText.split("#");
        SpannableString ss = new SpannableString(split[0] + split[1]);
        ClickableSpan policyClickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                WebViewActivity.launch(WelcomeActivity.this, "https://accktvpic.oss-cn-beijing.aliyuncs.com/pic/meta/demo/fulldemoStatic/privacy/privacy.html");
            }
        };
        ss.setSpan(policyClickableSpan, split[0].length(), split[0].length() + split[1].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#32AEFF")), split[0].length(), split[0].length() + split[1].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        getBinding().tvPolicy.setText(ss);
        getBinding().tvPolicy.setMovementMethod(LinkMovementMethod.getInstance());

        if (UserManager.getInstance().isLogin()) {
            gotoHomeActivity();
        }
    }

    @Override
    public void initListener() {
        getBinding().btnEnterRoom.setOnClickListener(v -> {
            if (getBinding().cbPolicy.isChecked()) {
                UserManager.getInstance().loginWithRandomUserInfo();
                gotoHomeActivity();
            } else {
                animCheckTip();
            }
        });
        getBinding().cbPolicy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && getBinding().tvCheckTip.getVisibility() == View.VISIBLE) {
                getBinding().tvCheckTip.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public boolean isBlackDarkStatus() {
        return false;
    }


    private void animCheckTip() {
        getBinding().tvCheckTip.setVisibility(View.VISIBLE);
        TranslateAnimation animation = new TranslateAnimation(
                -10, 10, 0, 0
        );
        animation.setDuration(60);
        animation.setRepeatCount(4);
        animation.setRepeatMode(Animation.REVERSE);
        getBinding().tvCheckTip.clearAnimation();
        getBinding().tvCheckTip.startAnimation(animation);
    }


    private void gotoHomeActivity() {
        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
        finish();
    }


}
