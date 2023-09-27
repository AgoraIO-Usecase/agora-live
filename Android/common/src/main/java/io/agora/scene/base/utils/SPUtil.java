package io.agora.scene.base.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import io.agora.scene.base.component.AgoraApplication;

/**
 * The type Sp util.
 */
public final class SPUtil {


    /**
     * The constant PREFERENCES_NAME.
     */
    private final static String PREFERENCES_NAME = "PREF_HEALTHAI_RECORD_SDK";
    /**
     * The constant mInstance.
     */
    private static SharedPreferences mInstance;

    /**
     * Instantiates a new Sp util.
     */
    private SPUtil() {
    }


    /**
     * Gets shared preference.
     *
     * @return the shared preference
     */
    private static SharedPreferences getSharedPreference() {
        String name = PREFERENCES_NAME;
        if (mInstance == null) {
            synchronized (SPUtil.class) {
                if (mInstance == null) {
                    mInstance = AgoraApplication.the().getSharedPreferences(name, Context.MODE_PRIVATE);
                }
            }
        }
        return mInstance;
    }

    /**
     * Put boolean boolean.
     *
     * @param key   the key
     * @param value the value
     * @return the boolean
     */
    public static boolean putBoolean(String key, Boolean value) {
        SharedPreferences sharedPreference = getSharedPreference();
        Editor editor = sharedPreference.edit();
        editor.putBoolean(key, value);
        return editor.commit();
    }

    /**
     * Put int boolean.
     *
     * @param key   the key
     * @param value the value
     * @return the boolean
     */
    public static boolean putInt(String key, int value) {
        SharedPreferences sharedPreference = getSharedPreference();
        Editor editor = sharedPreference.edit();
        editor.putInt(key, value);
        return editor.commit();
    }

    /**
     * Put float boolean.
     *
     * @param key   the key
     * @param value the value
     * @return the boolean
     */
    public static boolean putFloat(String key, float value) {
        SharedPreferences sharedPreference = getSharedPreference();
        Editor editor = sharedPreference.edit();
        editor.putFloat(key, value);
        return editor.commit();
    }

    /**
     * Put long boolean.
     *
     * @param key   the key
     * @param value the value
     * @return the boolean
     */
    public static boolean putLong(String key, long value) {
        SharedPreferences sharedPreference = getSharedPreference();
        Editor editor = sharedPreference.edit();
        editor.putLong(key, value);
        return editor.commit();
    }

    /**
     * Put string boolean.
     *
     * @param key   the key
     * @param value the value
     * @return the boolean
     */
    public static boolean putString(String key, String value) {
        SharedPreferences sharedPreference = getSharedPreference();
        Editor editor = sharedPreference.edit();
        editor.putString(key, value);
        return editor.commit();
    }

    /**
     * Gets string.
     *
     * @param key      the key
     * @param defValue the def value
     * @return the string
     */
    public static String getString(String key, String defValue) {
        SharedPreferences sharedPreference = getSharedPreference();
        return sharedPreference.getString(key, defValue);
    }

    /**
     * Gets int.
     *
     * @param key      the key
     * @param defValue the def value
     * @return the int
     */
    public static int getInt(String key, int defValue) {
        SharedPreferences sharedPreference = getSharedPreference();
        return sharedPreference.getInt(key, defValue);
    }

    /**
     * Gets float.
     *
     * @param key      the key
     * @param defValue the def value
     * @return the float
     */
    public static float getFloat(String key, Float defValue) {
        SharedPreferences sharedPreference = getSharedPreference();
        return sharedPreference.getFloat(key, defValue);
    }

    /**
     * Gets boolean.
     *
     * @param key      the key
     * @param defValue the def value
     * @return the boolean
     */
    public static boolean getBoolean(String key,
                                     Boolean defValue) {
        SharedPreferences sharedPreference = getSharedPreference();
        return sharedPreference.getBoolean(key, defValue);
    }

    /**
     * Gets long.
     *
     * @param key      the key
     * @param defValue the def value
     * @return the long
     */
    public static long getLong(String key, long defValue) {
        SharedPreferences sharedPreference = getSharedPreference();
        return sharedPreference.getLong(key, defValue);
    }

    /**
     * Remove key.
     *
     * @param key the key
     */
    public static void removeKey(String key) {
        try {
            SharedPreferences sharedPreference = getSharedPreference();
            Editor editor = sharedPreference.edit();
            editor.remove(key);
            editor.apply();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Clear.
     */
    public static void clear() {
        SharedPreferences sharedPreference = getSharedPreference();
        Editor editor = sharedPreference.edit();
        editor.clear();
        editor.commit();
    }
}
