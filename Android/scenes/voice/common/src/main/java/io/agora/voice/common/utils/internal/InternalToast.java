package io.agora.voice.common.utils.internal;

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

import io.agora.voice.common.R;
import io.agora.voice.common.utils.DeviceTools;

@RestrictTo({RestrictTo.Scope.LIBRARY})
public final class InternalToast {

    public static final int COMMON = 0;
    public static final int TIPS = 1;
    public static final int ERROR = 2;

    @SuppressLint("StaticFieldLeak")
    private static Application mApp;

    public static void init(@NonNull final Application app) {
        if (mApp == null) {
            mApp = app;
        }
    }

    public static Application getApp() {
        return mApp;
    }

    private InternalToast() {
        //避免初始化
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    private static void checkContext() {
        if (mApp == null) {
            throw new NullPointerException("ToastUtils context is not null，please first init");
        }
    }

    public static void show(CharSequence notice, int toastType,int duration) {
        checkMainThread();
        checkContext();
        if (TextUtils.isEmpty(notice)) {
            return;
        }
        new Builder(mApp)
                .setDuration(duration)
                .setGravity(Gravity.BOTTOM)
                .setOffset((int) DeviceTools.getDp(200))
                .setToastTYpe(toastType)
                .setTitle(notice)
                .build()
                .show();
    }


    public static final class Builder {

        private final Context context;
        private CharSequence title;
        private int gravity = Gravity.TOP;
        private int yOffset;
        private int duration = Toast.LENGTH_SHORT;
        private int toastType;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setToastTYpe(int toastType) {
            this.toastType = toastType;
            return this;
        }

        public Builder setGravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder setOffset(int yOffset) {
            this.yOffset = yOffset;
            return this;
        }

        public Builder setDuration(int duration) {
            this.duration = duration;
            return this;
        }

        private SoftReference<Toast> mToast;

        public Toast build() {
            if (!checkNull(mToast)) {
                mToast.get().cancel();
            }
            Toast toast = new Toast(context);
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
                //android 7.1.1 版本
                InternalHookToast.hook(toast);
            }

//            toast.setMargin(0, 0);
            View rootView = LayoutInflater.from(context).inflate(R.layout.voice_view_toast_custom, null);
            TextView textView = rootView.findViewById(R.id.tvContent);
            ImageView imageView = rootView.findViewById(R.id.ivToast);

            textView.setText(title);
            if (toastType == COMMON) {
                imageView.setVisibility(View.GONE);
            } else {
                imageView.setVisibility(View.VISIBLE);
                if (toastType == TIPS) {
                    imageView.setImageResource(R.drawable.voice_icon_toast_hint);
                } else {
                    imageView.setImageResource(R.drawable.voice_icon_toast_error);
                }
            }

            toast.setView(rootView);
            toast.setGravity(gravity, 0, yOffset);
            toast.setDuration(duration);
            mToast = new SoftReference<>(toast);
            return toast;
        }
    }

    private static boolean checkNull(SoftReference softReference) {
        if (softReference == null || softReference.get() == null) {
            return true;
        }
        return false;
    }

    public static void checkMainThread() {
        if (!isMainThread()) {
            throw new IllegalStateException("Please refrain from performing pop-up operations in a child thread.");
        }
    }

    private static boolean isMainThread() {
        return Looper.getMainLooper() == Looper.myLooper();
    }
}
