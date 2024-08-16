package com.agora.entfulldemo.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.ActivityKt;
import androidx.navigation.NavController;
import androidx.navigation.ui.BottomNavigationViewKt;

import com.agora.entfulldemo.R;
import com.agora.entfulldemo.databinding.AppActivityMainBinding;

import io.agora.scene.base.SceneConfigManager;
import io.agora.scene.base.component.BaseViewBindingActivity;
import io.agora.scene.widget.dialog.PermissionLeakDialog;

/**
 * Main Activity.
 */
public class MainActivity extends BaseViewBindingActivity<AppActivityMainBinding> {
    private NavController navController;

    @Override
    protected AppActivityMainBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return AppActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SceneConfigManager.INSTANCE.fetchSceneConfig(null, null);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        navController = ActivityKt.findNavController(this, R.id.nav_host_fragment_activity_main);
        BottomNavigationViewKt.setupWithNavController(getBinding().navView, navController);
    }

    @Override
    protected boolean isCanExit() {
        return true;
    }

    @Override
    public void initListener() {
        getBinding().navView.setItemIconTintList(null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPermissionDined(String permission) {
        super.onPermissionDined(permission);
        new PermissionLeakDialog(this).show(permission, null, () -> launchAppSetting(permission));
    }

}
