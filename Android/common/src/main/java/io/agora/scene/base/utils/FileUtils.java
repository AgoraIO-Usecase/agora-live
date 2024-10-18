package io.agora.scene.base.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.agora.scene.base.CommonBaseLogger;
import io.agora.scene.base.component.AgoraApplication;

/**
 * The type File utils.
 */
public final class FileUtils {

    private FileUtils() {

    }

    /**
     * Gets base str path.
     *
     * @return the base str path
     */
    public static String getBaseStrPath() {
        return AgoraApplication.the().getExternalFilesDir("media").getAbsolutePath() + File.separator;
    }

    /**
     * Gets temp sd path.
     *
     * @return the temp sd path
     */
    public static String getTempSDPath() {
        return getBaseStrPath() + "ag" + File.separator;
    }

    /**
     * The constant SEPARATOR.
     */
    public static final String SEPARATOR = File.separator;

    /**
     * Copy file from assets string.
     *
     * @param context        the context
     * @param assetsFilePath the assets file path
     * @param storagePath    the storage path
     * @return the string
     */
    public static String copyFileFromAssets(Context context, String assetsFilePath, String storagePath) {
        if (TextUtils.isEmpty(storagePath)) {
            return null;
        } else if (storagePath.endsWith(SEPARATOR)) {
            storagePath = storagePath.substring(0, storagePath.length() - 1);
        }

        if (TextUtils.isEmpty(assetsFilePath) || assetsFilePath.endsWith(SEPARATOR)) {
            return null;
        }

        String storageFilePath = storagePath + SEPARATOR + assetsFilePath;

        AssetManager assetManager = context.getAssets();
        try {
            File file = new File(storageFilePath);
            if (file.exists()) {
                return storageFilePath;
            }
            file.getParentFile().mkdirs();
            InputStream inputStream = assetManager.open(assetsFilePath);
            readInputStream(storageFilePath, inputStream);
        } catch (IOException e) {
            CommonBaseLogger.e("FileUtils", e.toString());
            return null;
        }
        return storageFilePath;
    }

    /**
     * Read input stream.
     *
     * @param storagePath the storage path
     * @param inputStream the input stream
     */
    private static void readInputStream(String storagePath, InputStream inputStream) {
        File file = new File(storagePath);
        try {
            if (!file.exists()) {
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buffer = new byte[inputStream.available()];
                int lenght = 0;
                while ((lenght = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, lenght);
                }
                fos.flush();
                fos.close();
                inputStream.close();
            }
        } catch (IOException e) {
            CommonBaseLogger.e("FileUtils", e.toString());
        }
    }

    /**
     *
     * @param filePath
     * @return
     */
    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
