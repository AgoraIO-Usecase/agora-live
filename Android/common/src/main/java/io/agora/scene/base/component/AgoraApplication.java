package io.agora.scene.base.component;

import androidx.multidex.MultiDexApplication;

import io.agora.scene.base.CommonBaseLogger;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Agora Application.
 */
public class AgoraApplication extends MultiDexApplication {
    private static AgoraApplication sInstance;


    /**
     * Agora application.
     * @return application
     */
    public static AgoraApplication the() {
        return sInstance;
    }

    private boolean isDebugModeOpen = false;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        RxJavaPlugins.setErrorHandler(throwable -> CommonBaseLogger.e("AgoraApplication", throwable.toString()));
    }

    /**
     * Enable debug mode.
     * @param enable
     */
    public void enableDebugMode(boolean enable) {
        this.isDebugModeOpen = enable;
    }

    public boolean isDebugModeOpen() {
        return isDebugModeOpen;
    }
}