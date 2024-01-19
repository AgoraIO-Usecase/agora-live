package io.agora.voice.common.utils.internal;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import androidx.annotation.RestrictTo;

import java.lang.reflect.Field;

@RestrictTo({RestrictTo.Scope.LIBRARY})
public final class InternalHookToast {

    private static Field sField_TN;
    private static Field sField_TN_Handler;

    static {
        try {
            Class<?> clazz = Toast.class;
            sField_TN = clazz.getDeclaredField("mTN");
            sField_TN.setAccessible(true);
            sField_TN_Handler = sField_TN.getType().getDeclaredField("mHandler");
            sField_TN_Handler.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void hook(Toast toast) {
        try {
            Object tn = sField_TN.get(toast);
            Handler preHandler = (Handler) sField_TN_Handler.get(tn);
            sField_TN_Handler.set(tn, new SafelyHandler(preHandler));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class SafelyHandler extends Handler {

        private final Handler impl;

        public SafelyHandler(Handler impl) {
            this.impl = impl;
        }

        @Override
        public void dispatchMessage(Message msg) {
            try {
                super.dispatchMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void handleMessage(Message msg) {
            impl.handleMessage(msg);
        }
    }

}
