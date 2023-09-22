package io.agora.scene.base.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import io.agora.scene.base.component.AgoraApplication;

/**
 * The type Toast utils.
 */
public final class ToastUtils {


    private ToastUtils() {

    }

    /**
     * The constant mainHandler.
     */
    private static Handler mainHandler;

    /**
     * Show toast.
     *
     * @param resStringId the res string id
     */
    public static void showToast(int resStringId) {
        runOnMainThread(() -> Toast.makeText(AgoraApplication.the(), resStringId, Toast.LENGTH_SHORT).show());
    }

    /**
     * Show toast.
     *
     * @param str the str
     */
    public static void showToast(String str) {
        runOnMainThread(() -> Toast.makeText(AgoraApplication.the(), str, Toast.LENGTH_SHORT).show());
    }

    /**
     * Show toast long.
     *
     * @param resStringId the res string id
     */
    public static void showToastLong(int resStringId) {
        runOnMainThread(() -> Toast.makeText(AgoraApplication.the(), resStringId, Toast.LENGTH_LONG).show());
    }

    /**
     * Show toast long.
     *
     * @param str the str
     */
    public static void showToastLong(String str) {
        runOnMainThread(() -> Toast.makeText(AgoraApplication.the(), str, Toast.LENGTH_LONG).show());
    }

    /**
     * Run on main thread.
     *
     * @param runnable the runnable
     */
    private static void runOnMainThread(Runnable runnable) {
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        if (Thread.currentThread() == mainHandler.getLooper().getThread()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
}
