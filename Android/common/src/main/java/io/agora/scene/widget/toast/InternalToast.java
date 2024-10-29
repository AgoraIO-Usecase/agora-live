package io.agora.scene.widget.toast;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.ref.SoftReference;

import io.agora.scene.base.R;
import io.agora.scene.base.utils.UiUtil;

/**
 * The type Internal toast.
 */
@RestrictTo({RestrictTo.Scope.LIBRARY})
public final class InternalToast {

    /**
     * The constant COMMON.
     */
    public static final int COMMON = 0;
    /**
     * The constant TIPS.
     */
    public static final int TIPS = 1;
    /**
     * The constant ERROR.
     */
    public static final int ERROR = 2;

    /**
     * the app instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Application mApp;

    /**
     * init
     *
     * @param app the app
     */
    public static void init(@NonNull final Application app) {
        if (mApp == null) {
            mApp = app;
        }
    }

    /**
     * Gets app.
     *
     * @return the app
     */
    public static Application getApp() {
        return mApp;
    }

    /**
     * private constructor.
     */
    private InternalToast() {
        //avoid initialization
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * Check that the context cannot be null; initialization must be performed first.
     */
    private static void checkContext() {
        if (mApp == null) {
            throw new NullPointerException("ToastUtils context is not null，please first init");
        }
    }


    /**
     * Show.
     *
     * @param notice    the notice
     * @param toastType the toast type
     * @param duration  the duration
     */
    public static void show(CharSequence notice, int toastType, int duration) {
        checkMainThread();
        checkContext();
        if (TextUtils.isEmpty(notice)) {
            return;
        }
        new Builder(mApp)
                .setDuration(duration)
                .setGravity(Gravity.BOTTOM)
                .setOffset((int) UiUtil.dp2px(200))
                .setToastTYpe(toastType)
                .setTitle(notice)
                .build()
                .show();
    }

    /**
     * Show.
     *
     * @param notice    the notice
     * @param toastType the toast type
     * @param duration  the duration
     * @param gravity   the gravity
     * @param offsetY   the offset y
     */
    public static void show(CharSequence notice, int toastType, int duration, int gravity, int offsetY) {
        checkMainThread();
        checkContext();
        if (TextUtils.isEmpty(notice)) {
            return;
        }
        new Builder(mApp)
                .setDuration(duration)
                .setGravity(gravity)
                .setOffset(offsetY)
                .setToastTYpe(toastType)
                .setTitle(notice)
                .build()
                .show();
    }


    /**
     * The type Builder.
     */
    public static final class Builder {

        private final Context context;
        private CharSequence title;
        private int gravity = Gravity.TOP;
        private int yOffset;
        private int duration = Toast.LENGTH_SHORT;
        private int toastType;

        /**
         * Instantiates a new Builder.
         *
         * @param context the context
         */
        public Builder(Context context) {
            this.context = context;
        }

        /**
         * Sets title.
         *
         * @param title the title
         * @return the title
         */
        public Builder setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        /**
         * Sets toast t ype.
         *
         * @param toastType the toast type
         * @return the toast t ype
         */
        public Builder setToastTYpe(int toastType) {
            this.toastType = toastType;
            return this;
        }

        /**
         * Sets gravity.
         *
         * @param gravity the gravity
         * @return the gravity
         */
        public Builder setGravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        /**
         * Sets offset.
         *
         * @param yOffset the y offset
         * @return the offset
         */
        public Builder setOffset(int yOffset) {
            this.yOffset = yOffset;
            return this;
        }

        /**
         * Sets duration.
         *
         * @param duration the duration
         * @return the duration
         */
        public Builder setDuration(int duration) {
            this.duration = duration;
            return this;
        }

        private SoftReference<Toast> mToast;

        /**
         * Build toast.
         *
         * @return the toast
         */
        public Toast build() {
            if (!checkNull(mToast)) {
                mToast.get().cancel();
            }
            Toast toast = new Toast(context);
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                InternalHookToast.hook(toast);
            }

//            toast.setMargin(0, 0);
            View rootView = LayoutInflater.from(context).inflate(R.layout.view_toast_custom, null);
            TextView textView = rootView.findViewById(R.id.tvContent);
            ImageView imageView = rootView.findViewById(R.id.ivToast);

            textView.setText(title);
            if (toastType == COMMON) {
                imageView.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.VISIBLE);
                if (toastType == TIPS) {
                    imageView.setImageResource(R.mipmap.toast_icon_right);
                } else {
                    imageView.setImageResource(R.mipmap.toast_icon_wrong);
                }
            }

            toast.setView(rootView);
            toast.setGravity(gravity, 0, yOffset);
            toast.setDuration(duration);
            mToast = new SoftReference<>(toast);
            return toast;
        }

        /**
         * Update notice.
         *
         * @param text the text
         */
        public void updateNotice(String text) {
            if (mToast == null) {
                return;
            }
            Toast toast = mToast.get();
            if (toast == null) {
                return;
            }
            View view = toast.getView();
            if (view == null) {
                return;
            }
            TextView textView = view.findViewById(R.id.tvContent);
            if (textView == null) {
                return;
            }
            textView.setText(text);
        }
    }


    private static boolean checkNull(SoftReference softReference) {
        if (softReference == null || softReference.get() == null) {
            return true;
        }
        return false;
    }

    /**
     * Check main thread.
     */
    public static void checkMainThread() {
        if (!isMainThread()) {
            throw new IllegalStateException("Please do not perform popup operations in a background thread.");
        }
    }

    private static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
