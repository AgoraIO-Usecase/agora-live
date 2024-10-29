package io.agora.scene.base.component;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity.
 */
public abstract class BaseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
        initView(savedInstanceState);
        setDarkStatusIcon(isBlackDarkStatus());
        requestData();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        initListener();
    }

    protected void init() {
        if (getLayoutId() > 0) {
            setContentView(getLayoutId());
        } else {
            setContentView(getLayoutView());
        }
    }


    /**
     * Init View.
     *
     * @param savedInstanceState
     */
    public void initView(Bundle savedInstanceState) {

    }

    /**
     * Init Listener.
     */
    public void initListener() {
    }

    /**
     * Request Data.
     */
    public void requestData() {
    }

    public View getLayoutView() {
        return null;
    }

    abstract int getLayoutId();

    public boolean isBlackDarkStatus() {
        return true;
    }

    /**
     * Set dark status icon.
     *
     * @param bDark
     */
    public void setDarkStatusIcon(boolean bDark) {
        //Starting from version 5.x, you need to set the color to transparent; otherwise, the navigation bar will display the system’s default light gray color
        View decorView = getWindow().getDecorView();
        //The two flags must be used together to allow the main content of the application to occupy the space of the system status bar.
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                //| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION//| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // In 6.0, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR was added.
        // This field marks the status bar as light-colored, and then the font color of the status bar is automatically changed to dark.
        if (bDark) {
            option = option | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decorView.setSystemUiVisibility(option);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }
}