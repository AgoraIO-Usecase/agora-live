package io.agora.scene.base.utils;

import android.os.SystemClock;
import android.util.Log;

import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * The type Time utils.
 */
public final class TimeUtils {

    private TimeUtils() {

    }

    /**
     * The constant workerExecutor.
     */
    private static final Executor WORKER_EXECUTOR = Executors.newSingleThreadExecutor();
    /**
     * The constant hasSync.
     */
    private static volatile boolean hasSync = false;
    /**
     * The constant diff.
     */
    private static volatile long diff = 0;
    /**
     * The constant TAG.
     */
    private static final String TAG = "TimeUtils";

    /**
     * Current time millis long.
     *
     * @return the long
     */
    public static long currentTimeMillis() {
        if (!hasSync) {
            CountDownLatch latch = new CountDownLatch(1);
            WORKER_EXECUTOR.execute(() -> {
                try {
                    URL url = new URL("https://www.bing.com/");
                    URLConnection uc = url.openConnection();
                    long startTime = SystemClock.elapsedRealtime();
                    uc.connect();
                    long ld = uc.getDate();
                    diff = ld + (SystemClock.elapsedRealtime() - startTime) - System.currentTimeMillis();
                    hasSync = true;
                    Log.d(TAG, "diff success, diff=" + diff);
                } catch (Exception e) {
                    Log.e(TAG, "get data failed", e);
                } finally {
                    latch.countDown();
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                Log.e(TAG, "wait too long", e);
            }
        }
        return System.currentTimeMillis() + diff;
    }

}
