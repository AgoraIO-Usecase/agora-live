package io.agora.scene.base.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * util for dynamic load so file
 */
public class DynamicLoadUtil {
    /**
     * load so file
     * @param context context
     * @param fromPath so file path, usually is download file
     * @param soName so file bane, eg: libeffect, without .so suffix
     */
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    public static void loadSoFile(Context context, String fromPath, String soName) {
        File dir = context.getDir("libs", Context.MODE_PRIVATE);
        if (!isLoadSoFile(dir, soName)) {
            int ret = copy(fromPath, dir.getAbsolutePath(), soName);
            if (ret != 0) {
                Log.e("DynamicLoadUtil", "copy so file failed");
                return;
            }
        }
        Log.d("DynamicLoadUtil", "load so file: " + dir.getAbsolutePath() + "/" + soName + ".so");
        System.load(dir.getAbsolutePath() + "/" + soName + ".so");
    }

    /**
     * Check if the so file exists.
     * @param dir The app's private directory where the so file is stored.
     * @param soName The name of the so file, for example, libeffect, without the .so suffix.
     * @return Whether it exists.
     */
    public static boolean isLoadSoFile(File dir, String soName) {
        File[] currentFiles = dir.listFiles();
        if (currentFiles == null) {
            return false;
        }
        return Arrays.stream(currentFiles).anyMatch(file -> file.getName().contains(soName));
    }

    /**
     * Copy the so file to the app's private directory.
     * @param fromFile The path of the so file, usually the download directory.
     * @param toFile The target app's private directory where the so file will be stored.
     * @param soName The name of the so file, for example, libeffect, without the .so suffix.
     * @return The result of the copy operation.
     */
    private static int copy(String fromFile, String toFile, String soName) {
        File root = new File(fromFile);
        if (!root.exists()) {
            return -1;
        }
        File[] currentFiles = root.listFiles();
        if (currentFiles == null) {
            return -1;
        }
        File targetDir = new File(toFile);
        if (!targetDir.exists()) {
            boolean ret = targetDir.mkdirs();
            Log.d("DynamicLoadUtil", "create dir: " + ret);
        }
        for (File currentFile : currentFiles) {
            if (currentFile.getName().contains(soName)) {
                return copySdcardFile(currentFile.getPath(), toFile + File.separator + currentFile.getName());
            }
        }
        return 0;
    }

    /**
     * Copy a file.
     * @param fromFile The path of the file, usually the download directory.
     * @param toFile The target app's private directory where the file will be stored.
     * @return The result of the copy operation.
     */
    private static int copySdcardFile(String fromFile, String toFile) {
        try (FileInputStream fosFrom = new FileInputStream(fromFile);
             FileOutputStream fosTo = new FileOutputStream(toFile)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fosFrom.read(buffer)) != -1) {
                fosTo.write(buffer, 0, len);
            }
            return 0;
        } catch (Exception ex) {
            Log.e("DynamicLoadUtil", "copy Sdcard File failed: " + ex.getMessage());
            return -1;
        }
    }
}
