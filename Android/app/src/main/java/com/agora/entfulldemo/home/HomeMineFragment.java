package com.agora.entfulldemo.home;

import android.annotation.SuppressLint;
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
import com.agora.entfulldemo.webview.WebViewActivity;

import io.agora.scene.base.GlideApp;
import io.agora.scene.base.bean.User;
import io.agora.scene.base.component.AgoraApplication;
import io.agora.scene.base.component.BaseViewBindingFragment;
import io.agora.scene.base.component.OnButtonClickListener;
import io.agora.scene.base.manager.UserManager;
import io.agora.scene.base.utils.ToastUtils;
import io.agora.scene.widget.dialog.CommonDialog;
import io.agora.scene.widget.dialog.EditNameDialog;
import io.agora.scene.widget.utils.CenterCropRoundCornerTransform;

/**
 * The type Home mine fragment.
 */
public class HomeMineFragment extends BaseViewBindingFragment<AppFragmentHomeMineBinding> {
    private EditNameDialog editNameDialog;
    private CommonDialog debugModeDialog;

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
        getBinding().tvUserID.setText(getString(R.string.id_is_, user.userNo));
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
            if (editNameDialog == null) {
                editNameDialog = new EditNameDialog(getContext());
                editNameDialog.setOnDefineClickListener(name -> {
                    UserManager.getInstance().updateUserName(name);
                    getBinding().tvUserMobile.setText(name);
                });
            }
            editNameDialog.show();
        });
        getBinding().tvDebugMode.setOnClickListener(v -> showDebugModeCloseDialog());
        if (AgoraApplication.the().isDebugModeOpen()) {
            getBinding().tvDebugMode.setVisibility(View.VISIBLE);
        }
    }



    private void showDebugModeCloseDialog() {
        if (debugModeDialog == null) {
            debugModeDialog = new CommonDialog(requireContext());
            debugModeDialog.setDialogTitle(getString(R.string.app_exit_debug));
            debugModeDialog.setDescText(getString(R.string.app_exit_debug_tip));
            debugModeDialog.setDialogBtnText(getString(R.string.cancel), getString(R.string.app_exit));
            debugModeDialog.setOnButtonClickListener(new OnButtonClickListener() {
                @Override
                public void onLeftButtonClick() {
                }

                @Override
                public void onRightButtonClick() {
                    getBinding().tvDebugMode.setVisibility(View.GONE);
                    AgoraApplication.the().enableDebugMode(false);
                    ToastUtils.showToast(R.string.app_debug_off);
                }
            });
        }
        debugModeDialog.show();
    }
}
