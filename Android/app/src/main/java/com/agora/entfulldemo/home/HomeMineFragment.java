package com.agora.entfulldemo.home;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.entfulldemo.R;
import com.agora.entfulldemo.databinding.AppFragmentHomeMineBinding;
import com.agora.entfulldemo.home.constructor.URLStatics;
import com.agora.entfulldemo.home.mine.AboutUsActivity;
import com.agora.entfulldemo.home.mine.AppDebugActivity;
import com.agora.entfulldemo.home.mine.InviteCodeActivity;
import com.agora.entfulldemo.webview.WebViewActivity;
import com.agora.entfulldemo.welcome.WelcomeActivity;

import io.agora.scene.base.GlideApp;
import io.agora.scene.base.bean.User;
import io.agora.scene.base.component.AgoraApplication;
import io.agora.scene.base.component.BaseViewBindingFragment;
import io.agora.scene.base.component.OnButtonClickListener;
import io.agora.scene.base.manager.SSOUserManager;
import io.agora.scene.base.manager.UserManager;
import io.agora.scene.widget.dialog.CommonDialog;
import io.agora.scene.widget.dialog.EditNameDialog;
import io.agora.scene.widget.utils.CenterCropRoundCornerTransform;
import io.agora.scene.widget.utils.UiUtils;

/**
 * The type Home mine fragment.
 */
public class HomeMineFragment extends BaseViewBindingFragment<AppFragmentHomeMineBinding> {
    private EditNameDialog editNameDialog;

    @NonNull
    @Override
    protected AppFragmentHomeMineBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return AppFragmentHomeMineBinding.inflate(inflater);
    }


    @Override
    public void initView() {
        User user = UserManager.getInstance().getUser();
        GlideApp.with(this)
                .load(user.getFullHeadUrl())
                .transform(new CenterCropRoundCornerTransform(999))
                .into(getBinding().ivUserAvatar);
        getBinding().tvUserMobile.setText(user.name);
        getBinding().tvUserID.setText(getString(io.agora.scene.base.R.string.id_is_, user.userNo));

        if (SSOUserManager.isInvitationUser()) {
            getBinding().ivToEdit.setVisibility(View.VISIBLE);
            getBinding().tvInviteCode.setVisibility(View.GONE);
        } else {
            getBinding().ivToEdit.setVisibility(View.GONE);
            getBinding().tvInviteCode.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void initListener() {
        getBinding().tvUserAgreement.setOnClickListener(view -> {
            WebViewActivity.launch(getContext(), URLStatics.termsOfServiceURL);
        });
        getBinding().tvAbout.setOnClickListener(view -> {
            startActivity(new Intent(getActivity(), AboutUsActivity.class));
        });
        getBinding().vToEdit.setOnClickListener(view -> {
            if (SSOUserManager.isInvitationUser()) {
                if (editNameDialog == null) {
                    editNameDialog = new EditNameDialog(getContext());
                    editNameDialog.setOnDefineClickListener(name -> {
                        UserManager.getInstance().updateUserName(name);
                        getBinding().tvUserMobile.setText(name);
                    });
                }
                editNameDialog.show();
            }
        });
        getBinding().tvDebugMode.setOnClickListener(view -> {
            if (UiUtils.isFastClick()) {
                return;
            }
            Activity activity = getActivity();
            if (activity != null) {
                AppDebugActivity.startActivity(activity);
            }
        });
        if (AgoraApplication.the().isDebugModeOpen()) {
            getBinding().tvDebugMode.setVisibility(View.VISIBLE);
        }else {
            getBinding().tvDebugMode.setVisibility(View.GONE);
        }
        getBinding().tvInviteCode.setOnClickListener(view -> {
            if (!SSOUserManager.isInvitationUser()) {
                startActivity(new Intent(getContext(), InviteCodeActivity.class));
            }
        });
        getBinding().tvLogout.setOnClickListener(view -> {
            Context context = getContext();
            if (context != null) {
                showLogoutDialog(context);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (AgoraApplication.the().isDebugModeOpen()) {
            getBinding().tvDebugMode.setVisibility(View.VISIBLE);
        }else {
            getBinding().tvDebugMode.setVisibility(View.GONE);
        }
    }

    private void showLogoutDialog(Context context) {
        CommonDialog dialog = new CommonDialog(context);
        dialog.setDialogTitle(getString(R.string.app_logout));
        dialog.setDescText(getString(R.string.app_logout_tips));
        dialog.setDialogBtnText(
                getString(io.agora.scene.base.R.string.cancel),
                getString(R.string.app_exit)
        );
        dialog.setOnButtonClickListener(new OnButtonClickListener() {
            @Override
            public void onLeftButtonClick() {
                // do nothing
            }

            @Override
            public void onRightButtonClick() {
                SSOUserManager.logout();
                startActivity(new Intent(context, WelcomeActivity.class));
            }
        });
        dialog.show();
    }
}
