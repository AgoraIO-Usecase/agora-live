package io.agora.scene.base.component;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewbinding.ViewBinding;

import java.util.ArrayList;
import java.util.List;

import io.agora.scene.base.R;
import io.agora.scene.base.bean.PermissionItem;
import io.agora.scene.base.utils.ToastUtils;
import kotlin.jvm.internal.Intrinsics;

/**
 * The type Base view binding activity.
 *
 * @param <T> the type parameter
 */
public abstract class BaseViewBindingActivity<T extends ViewBinding> extends BaseBindingActivity<T> {
    private View loadingView;
    private boolean isExit = false;

    private String appSettingInput;
    private ActivityResultLauncher<String> appSettingLauncher = registerForActivityResult(new ActivityResultContract<String, Boolean>() {

        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, String input) {
            appSettingInput = input;
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            return intent;
        }

        @Override
        public Boolean parseResult(int resultCode, @Nullable Intent intent) {
            return ActivityCompat.checkSelfPermission(BaseViewBindingActivity.this, appSettingInput) == PackageManager.PERMISSION_GRANTED;
        }
    }, result -> {
        checkPermission();
    });

    private void addLoadingView() {
        if (this.loadingView == null) {
            this.loadingView = LayoutInflater.from(this).inflate(R.layout.view_base_loading, null, false);
            ((ViewGroup) this.getBinding().getRoot()).addView(this.loadingView, new LayoutParams(-1, -1));
        }
        this.loadingView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////// Methods of Permission ////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    protected PermissionItem[] mPermissionArray;
    protected final List<String> mPermissionListDenied = new ArrayList<>();

    /**
     * The constant PERM_REQID_RECORD_AUDIO.
     */
    public static final int PERM_REQID_RECORD_AUDIO = 0x1001;
    /**
     * The constant PERM_REQID_CAMERA.
     */
    public static final int PERM_REQID_CAMERA = 0x1002;
    /**
     * The constant PERM_REQID_RDSTORAGE.
     */
    public static final int PERM_REQID_RDSTORAGE = 0x1003;
    /**
     * The constant PERM_REQID_WRSTORAGE.
     */
    public static final int PERM_REQID_WRSTORAGE = 0x1004;
    /**
     * The constant PERM_REQID_MGSTORAGE.
     */
    public static final int PERM_REQID_MGSTORAGE = 0x1005;
    /**
     * The constant PERM_REQID_WIFISTATE.
     */
    public static final int PERM_REQID_WIFISTATE = 0x1006;
    /**
     * The constant PERM_REQID_FINELOCAL.
     */
    public static final int PERM_REQID_FINELOCAL = 0x1007;

    private void checkPermission() {
        int reqIndex = requestNextPermission();
        if (reqIndex < 0) {
            getPermissions();
        }
    }

    /**
     * Request read storage permission.
     */
    public void requestReadStoragePermission() {
        this.requestReadStoragePermission(false);
    }

    /**
     * Request read storage permission.
     *
     * @param force the force
     */
    public void requestReadStoragePermission(boolean force) {
        if (force) {
            mPermissionListDenied.remove(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPermissionArray = new PermissionItem[1];
            mPermissionArray[0] = new PermissionItem(Manifest.permission.READ_EXTERNAL_STORAGE, PERM_REQID_RDSTORAGE);
            for (PermissionItem item : mPermissionArray) {
                item.granted = true;
            }
        } else if (VERSION.SDK_INT < Build.VERSION_CODES.R) {
            mPermissionArray = new PermissionItem[1];
            mPermissionArray[0] = new PermissionItem(Manifest.permission.READ_EXTERNAL_STORAGE, PERM_REQID_RDSTORAGE);
            for (PermissionItem item : mPermissionArray) {
                item.granted = ContextCompat.checkSelfPermission(this, item.permissionName) == PackageManager.PERMISSION_GRANTED;
            }
        } else {
            mPermissionArray = new PermissionItem[1];
            mPermissionArray[0] = new PermissionItem(Manifest.permission.READ_EXTERNAL_STORAGE, PERM_REQID_RDSTORAGE);
            for (PermissionItem item : mPermissionArray) {
                item.granted = ContextCompat.checkSelfPermission(this, item.permissionName) == PackageManager.PERMISSION_GRANTED;
            }
        }
        checkPermission();
    }

    protected void requestCameraPermission() {
        this.requestCameraPermission(false);
    }

    protected void requestCameraPermission(boolean force) {
        if (force) {
            mPermissionListDenied.remove(Manifest.permission.CAMERA);
        }
        if (VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPermissionArray = new PermissionItem[1];
            mPermissionArray[0] = new PermissionItem(Manifest.permission.CAMERA, PERM_REQID_CAMERA);
            for (PermissionItem item : mPermissionArray) {
                item.granted = true;
            }

        } else if (VERSION.SDK_INT < Build.VERSION_CODES.R) {
            mPermissionArray = new PermissionItem[1];
            mPermissionArray[0] = new PermissionItem(Manifest.permission.CAMERA, PERM_REQID_CAMERA);
            for (PermissionItem item : mPermissionArray) {
                item.granted = ContextCompat.checkSelfPermission(this, item.permissionName) == PackageManager.PERMISSION_GRANTED;
            }
        } else {
            mPermissionArray = new PermissionItem[1];
            mPermissionArray[0] = new PermissionItem(Manifest.permission.CAMERA, PERM_REQID_CAMERA);
            for (PermissionItem item : mPermissionArray) {
                item.granted = ContextCompat.checkSelfPermission(this, item.permissionName) == PackageManager.PERMISSION_GRANTED;
            }
        }
        checkPermission();
    }

    protected void requestRecordPermission() {
        this.requestRecordPermission(false);
    }

    protected void requestRecordPermission(boolean force) {
        if (force) {
            mPermissionListDenied.remove(Manifest.permission.RECORD_AUDIO);
        }
        if (VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPermissionArray = new PermissionItem[1];
            mPermissionArray[0] = new PermissionItem(Manifest.permission.RECORD_AUDIO, PERM_REQID_RECORD_AUDIO);
            for (PermissionItem item : mPermissionArray) {
                item.granted = true;
            }

        } else if (VERSION.SDK_INT < Build.VERSION_CODES.R) {
            mPermissionArray = new PermissionItem[1];
            mPermissionArray[0] = new PermissionItem(Manifest.permission.RECORD_AUDIO, PERM_REQID_RECORD_AUDIO);
            for (PermissionItem item : mPermissionArray) {
                item.granted = ContextCompat.checkSelfPermission(this, item.permissionName) == PackageManager.PERMISSION_GRANTED;
            }
        } else {
            mPermissionArray = new PermissionItem[1];
            mPermissionArray[0] = new PermissionItem(Manifest.permission.RECORD_AUDIO, PERM_REQID_RECORD_AUDIO);
            for (PermissionItem item : mPermissionArray) {
                item.granted = ContextCompat.checkSelfPermission(this, item.permissionName) == PackageManager.PERMISSION_GRANTED;
            }
        }
        checkPermission();
    }

    /**
     * Request next permission int.
     *
     * @return the int
     */
    protected int requestNextPermission() {
        if (mPermissionArray == null) {
            return -1;
        }
        for (int i = 0; i < mPermissionArray.length; i++) {
            if (!mPermissionArray[i].granted) {
                String permission = mPermissionArray[i].permissionName;
                if (mPermissionListDenied.contains(permission)) {
                    continue;
                }
                int requestCode = mPermissionArray[i].requestId;
                boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
                if (shouldShowRationale) {
                    doOnPermissionsDenied(requestCode);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                }
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setPermGrantedByReqId(requestCode);
        } else {
            doOnPermissionsDenied(requestCode);
            return;
        }

        int reqIndex = requestNextPermission();
        if (reqIndex < 0) {
            getPermissions();
        }
    }

    protected int setPermGrantedByReqId(int reqId) {
        if (mPermissionArray == null) {
            return -1;
        }
        for (int i = 0; i < mPermissionArray.length; i++) {
            if (mPermissionArray[i].requestId == reqId) {
                mPermissionArray[i].granted = true;
                return i;
            }
        }

        return -1;
    }


    /**
     * Request app permissions.
     *
     * @param permissions the permissions
     */
    @SuppressLint({"AutoDispose"})
    public void requestAppPermissions(@NonNull String... permissions) {
        Intrinsics.checkNotNullParameter(permissions, "permissions");
        if (VERSION.SDK_INT >= 23) {
            boolean hasAll = true;
            String[] var5 = permissions;
            int var6 = permissions.length;
            for (int var4 = 0; var4 < var6; ++var4) {
                String p = var5[var4];
                int i = ContextCompat.checkSelfPermission(this, p);
                if (i != 0) {
                    ActivityCompat.requestPermissions(this, permissions, 12);
                    hasAll = false;
                    break;
                }
            }
            if (hasAll) {
                this.getAlonePermissions();
            }
        } else {
            this.getAlonePermissions();
        }

    }

    private void doOnPermissionsDenied(int requestCode) {
        // ToastUtils.showToast(R.string.permission_leak_tip);
        if (mPermissionArray == null) {
            return;
        }
        String permission = null;
        for (int i = 0; i < mPermissionArray.length; i++) {
            if (mPermissionArray[i].requestId == requestCode) {
                permission = mPermissionArray[i].permissionName;
                break;
            }
        }
        if (permission == null) {
            return;
        }
        mPermissionListDenied.add(permission);
        onPermissionDined(permission);
    }

    protected void launchAppSetting(String permission) {
        appSettingLauncher.launch(permission);
    }

    protected void onPermissionDined(String permission) {

    }

    /**
     * Gets permissions.
     */
    public void getPermissions() {

    }

    /**
     * Gets alone permissions.
     */
    public void getAlonePermissions() {
    }

    @Nullable
    public View getLayoutView() {
        return super.getLayoutView();
    }

    /**
     * Show loading view.
     */
    public final void showLoadingView() {
        getWindow().getDecorView().post(this::addLoadingView);
        getWindow().getDecorView().postDelayed(() -> hideLoadingView(), 5000);
    }

    /**
     * Hide loading view.
     */
    public final void hideLoadingView() {
        if (this.loadingView == null) {
            return;
        }
        getWindow().getDecorView().post(() -> {
            if (this.loadingView != null) {
                this.loadingView.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Is can exit boolean.
     *
     * @return the boolean
     */
    protected boolean isCanExit() {
        return false;
    }


    /**
     * Hide input.
     */
    public final void hideInput() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        View v = this.getWindow().peekDecorView();
        if (v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    /**
     * Show input.
     *
     * @param et the et
     */
    public final void showInput(EditText et) {
        et.requestFocus();
        InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput((View) et, 1);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isCanExit()) {
            if (loadingView != null && loadingView.getVisibility() == View.VISIBLE) {
                loadingView.setVisibility(View.GONE);
                return true;
            }
            exitAPP();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void exitAPP() {
        if (!isExit) {
            isExit = true;
            ToastUtils.showToast(R.string.try_again_to_exit);
            new Handler(Looper.getMainLooper()).postDelayed(() -> isExit = false, 2000);
        } else {
            // RTMManager.getInstance().mRtmClient.release();
            finish();
            System.exit(0);
        }
    }

    protected void setOnApplyWindowInsetsListener(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets inset = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPaddingRelative(inset.left, inset.top, inset.right, inset.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
