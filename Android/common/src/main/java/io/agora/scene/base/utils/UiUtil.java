package io.agora.scene.base.utils;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.Keep;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * The type Ui util.
 */
@Keep
public final class UiUtil {

    private UiUtil() {

    }

    /**
     * Dp 2 px int.
     *
     * @param dp the dp
     * @return the int
     */
    public static int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    /**
     * Gets generic class.
     *
     * @param <T>   the type parameter
     * @param clz   the clz
     * @param index the index
     * @return the generic class
     */
    public static <T> Class<T> getGenericClass(Class<?> clz, int index) {
        Type type = clz.getGenericSuperclass();
        if (type == null) {
            return null;
        }
        return (Class<T>) ((ParameterizedType) type).getActualTypeArguments()[index];
    }

    /**
     * Gets view binding.
     *
     * @param bindingClass the binding class
     * @param inflater     the inflater
     * @return the view binding
     */
    public static Object getViewBinding(Class<?> bindingClass, LayoutInflater inflater) {
        try {
            Method inflateMethod = bindingClass.getDeclaredMethod("inflate", LayoutInflater.class);
            return inflateMethod.invoke(null, inflater);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets view binding.
     *
     * @param <T>          the type parameter
     * @param bindingClass the binding class
     * @param inflater     the inflater
     * @param container    the container
     * @return the view binding
     */
    public static <T> T getViewBinding(Class<T> bindingClass, LayoutInflater inflater, ViewGroup container) {
        try {
            Method inflateMethod = bindingClass.getDeclaredMethod("inflate", LayoutInflater.class, ViewGroup.class, Boolean.TYPE);
            return (T) inflateMethod.invoke(null, inflater, container, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}