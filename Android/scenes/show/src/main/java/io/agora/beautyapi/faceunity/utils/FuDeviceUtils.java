/*
 * MIT License
 *
 * Copyright (c) 2023 Agora Community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.agora.beautyapi.faceunity.utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FuDeviceUtils {

    public static final String TAG = "FuDeviceUtils";

    public static final int DEVICE_LEVEL_HIGH = 2;
    public static final int DEVICE_LEVEL_MID = 1;
    public static final int DEVICE_LEVEL_LOW = 0;

    /**
     * The default return value of any method in this class when an
     * error occurs or when processing fails (Currently set to -1). Use this to check if
     * the information about the device in question was successfully obtained.
     */
    public static final int DEVICEINFO_UNKNOWN = -1;

    private static final FileFilter CPU_FILTER = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String path = pathname.getName();
            //regex is slow, so checking char by char.
            if (path.startsWith("cpu")) {
                for (int i = 3; i < path.length(); i++) {
                    if (!Character.isDigit(path.charAt(i))) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    };


    /**
     * Calculates the total RAM of the device through Android API or /proc/meminfo.
     *
     * @param c - Context object for current running activity.
     * @return Total RAM that the device has, or DEVICEINFO_UNKNOWN = -1 in the event of an error.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static long getTotalMemory(Context c) {
        // memInfo.totalMem not supported in pre-Jelly Bean APIs.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
            am.getMemoryInfo(memInfo);
            if (memInfo != null) {
                return memInfo.totalMem;
            } else {
                return DEVICEINFO_UNKNOWN;
            }
        } else {
            long totalMem = DEVICEINFO_UNKNOWN;
            try {
                FileInputStream stream = new FileInputStream("/proc/meminfo");
                try {
                    totalMem = parseFileForValue("MemTotal", stream);
                    totalMem *= 1024;
                } finally {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return totalMem;
        }
    }

    /**
     * Method for reading the clock speed of a CPU core on the device. Will read from either
     * {@code /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq} or {@code /proc/cpuinfo}.
     *
     * @return Clock speed of a core on the device, or -1 in the event of an error.
     */
    public static int getCPUMaxFreqKHz() {
        int maxFreq = DEVICEINFO_UNKNOWN;
        try {
            for (int i = 0; i < getNumberOfCPUCores(); i++) {
                String filename =
                        "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq";
                File cpuInfoMaxFreqFile = new File(filename);
                if (cpuInfoMaxFreqFile.exists() && cpuInfoMaxFreqFile.canRead()) {
                    byte[] buffer = new byte[128];
                    FileInputStream stream = new FileInputStream(cpuInfoMaxFreqFile);
                    try {
                        stream.read(buffer);
                        int endIndex = 0;
                        //Trim the first number out of the byte buffer.
                        while (Character.isDigit(buffer[endIndex]) && endIndex < buffer.length) {
                            endIndex++;
                        }
                        String str = new String(buffer, 0, endIndex);
                        Integer freqBound = Integer.parseInt(str);
                        if (freqBound > maxFreq) {
                            maxFreq = freqBound;
                        }
                    } catch (NumberFormatException e) {
                        //Fall through and use /proc/cpuinfo.
                    } finally {
                        stream.close();
                    }
                }
            }
            if (maxFreq == DEVICEINFO_UNKNOWN) {
                FileInputStream stream = new FileInputStream("/proc/cpuinfo");
                try {
                    int freqBound = parseFileForValue("cpu MHz", stream);
                    freqBound *= 1024; //MHz -> kHz
                    if (freqBound > maxFreq) maxFreq = freqBound;
                } finally {
                    stream.close();
                }
            }
        } catch (IOException e) {
            maxFreq = DEVICEINFO_UNKNOWN; //Fall through and return unknown.
        }
        return maxFreq;
    }

    /**
     * Reads the number of CPU cores from the first available information from
     * {@code /sys/devices/system/cpu/possible}, {@code /sys/devices/system/cpu/present},
     * then {@code /sys/devices/system/cpu/}.
     *
     * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
     */
    public static int getNumberOfCPUCores() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // Gingerbread doesn't support giving a single application access to both cores, but a
            // handful of devices (Atrix 4G and Droid X2 for example) were released with a dual-core
            // chipset and Gingerbread; that can let an app in the background run without impacting
            // the foreground application. But for our purposes, it makes them single core.
            return 1;
        }
        int cores;
        try {
            cores = getCoresFromFileInfo("/sys/devices/system/cpu/possible");
            if (cores == DEVICEINFO_UNKNOWN) {
                cores = getCoresFromFileInfo("/sys/devices/system/cpu/present");
            }
            if (cores == DEVICEINFO_UNKNOWN) {
                cores = new File("/sys/devices/system/cpu/").listFiles(CPU_FILTER).length;
            }
        } catch (SecurityException e) {
            cores = DEVICEINFO_UNKNOWN;
        } catch (NullPointerException e) {
            cores = DEVICEINFO_UNKNOWN;
        }
        return cores;
    }

    /**
     * Tries to read file contents from the file location to determine the number of cores on device.
     *
     * @param fileLocation The location of the file with CPU information
     * @return Number of CPU cores in the phone, or DEVICEINFO_UKNOWN = -1 in the event of an error.
     */
    private static int getCoresFromFileInfo(String fileLocation) {
        InputStream is = null;
        try {
            is = new FileInputStream(fileLocation);
            BufferedReader buf = new BufferedReader(new InputStreamReader(is));
            String fileContents = buf.readLine();
            buf.close();
            return getCoresFromFileString(fileContents);
        } catch (IOException e) {
            return DEVICEINFO_UNKNOWN;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
        }
    }

    /**
     * Converts from a CPU core information format to number of cores.
     *
     * @param str The CPU core information string, in the format of "0-N"
     * @return The number of cores represented by this string
     */
    private static int getCoresFromFileString(String str) {
        if (str == null || !str.matches("0-[\\d]+$")) {
            return DEVICEINFO_UNKNOWN;
        }
        return Integer.valueOf(str.substring(2)) + 1;
    }

    /**
     * Helper method for reading values from system files, using a minimised buffer.
     *
     * @param textToMatch - Text in the system files to read for.
     * @param stream      - FileInputStream of the system file being read from.
     * @return A numerical value following textToMatch in specified the system file.
     * -1 in the event of a failure.
     */
    private static int parseFileForValue(String textToMatch, FileInputStream stream) {
        byte[] buffer = new byte[1024];
        try {
            int length = stream.read(buffer);
            for (int i = 0; i < length; i++) {
                if (buffer[i] == '\n' || i == 0) {
                    if (buffer[i] == '\n') i++;
                    for (int j = i; j < length; j++) {
                        int textIndex = j - i;
                        //Text doesn't match query at some point.
                        if (buffer[j] != textToMatch.charAt(textIndex)) {
                            break;
                        }
                        //Text matches query here.
                        if (textIndex == textToMatch.length() - 1) {
                            return extractValue(buffer, j);
                        }
                    }
                }
            }
        } catch (IOException e) {
            //Ignore any exceptions and fall through to return unknown value.
        } catch (NumberFormatException e) {
        }
        return DEVICEINFO_UNKNOWN;
    }

    /**
     * Helper method used by {@link #parseFileForValue(String, FileInputStream) parseFileForValue}. Parses
     * the next available number after the match in the file being read and returns it as an integer.
     *
     * @param index - The index in the buffer array to begin looking.
     * @return The next number on that line in the buffer, returned as an int. Returns
     * DEVICEINFO_UNKNOWN = -1 in the event that no more numbers exist on the same line.
     */
    private static int extractValue(byte[] buffer, int index) {
        while (index < buffer.length && buffer[index] != '\n') {
            if (Character.isDigit(buffer[index])) {
                int start = index;
                index++;
                while (index < buffer.length && Character.isDigit(buffer[index])) {
                    index++;
                }
                String str = new String(buffer, 0, start, index - start);
                return Integer.parseInt(str);
            }
            index++;
        }
        return DEVICEINFO_UNKNOWN;
    }

    public static long getAvailMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem;
    }


    public static String getBrand() {
        return Build.BRAND;
    }


    public static String getModel() {
        return Build.MODEL;
    }


    public static String getHardWare() {
        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text;
            String last = "";
            while ((text = br.readLine()) != null) {
                last = text;
            }
            if (last.contains("Hardware")) {
                String[] hardWare = last.split(":\\s+", 2);
                return hardWare[1];
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Build.HARDWARE;
    }


    /**
     * Level judgement based on current memory and CPU.
     *
     * @param context - Context object.
     * @return
     */
    public static int judgeDeviceLevel(Context context) {
        int level;
        int specialDevice = judgeDeviceLevelInDeviceName();
        if (specialDevice >= 0) return specialDevice;

        int ramLevel = judgeMemory(context);
        int cpuLevel = judgeCPU();
        if (ramLevel == 0 || ramLevel == 1 || cpuLevel == 0) {
            level = DEVICE_LEVEL_LOW;
        } else {
            if (cpuLevel > 1) {
                level = DEVICE_LEVEL_HIGH;
            } else {
                level = DEVICE_LEVEL_MID;
            }
        }
        LogUtils.d(TAG,"DeviceLevel: " + level);
        return level;
    }

    private static int judgeDeviceLevelInDeviceName() {
        String currentDeviceName = getDeviceName();
        for (String deviceName:upscaleDevice) {
            if (deviceName.equals(currentDeviceName)) {
                return DEVICE_LEVEL_HIGH;
            }
        }

        for (String deviceName:middleDevice) {
            if (deviceName.equals(currentDeviceName)) {
                return DEVICE_LEVEL_MID;
            }
        }

        for (String deviceName:lowDevice) {
            if (deviceName.equals(currentDeviceName)) {
                return DEVICE_LEVEL_LOW;
            }
        }
        return -1;
    }

    public static final String[] upscaleDevice = {"vivo X6S A","MHA-AL00","VKY-AL00","V1838A"};
    public static final String[] lowDevice = {};
    public static final String[] middleDevice = {"OPPO R11s","PAR-AL00","MI 8 Lite","ONEPLUS A6000","PRO 6","PRO 7 Plus"};


    private static int judgeMemory(Context context) {
        long ramMB = getTotalMemory(context) / (1024 * 1024);
        int level = -1;
        if (ramMB <= 2000) {
            level = 0;
        } else if (ramMB <= 3000) {
            level = 1;
        } else if (ramMB <= 4000) {
            level = 2;
        } else if (ramMB <= 6000) {
            level = 3;
        } else {
            level = 4;
        }
        return level;
    }

    private static int judgeCPU() {
        int level = 0;
        String cpuName = getHardWare();
        int freqMHz = getCPUMaxFreqKHz() / 1024;

        if (!TextUtils.isEmpty(cpuName)) {
            if (cpuName.contains("qcom") || cpuName.contains("Qualcomm")) {
                return judgeQualcommCPU(cpuName, freqMHz);
            } else if (cpuName.contains("hi") || cpuName.contains("kirin")) {
                return judgeSkinCPU(cpuName, freqMHz);
            } else if (cpuName.contains("MT")) {
                return judgeMTCPU(cpuName, freqMHz);
            }
        }

        if (freqMHz <= 1600) {
            level = 0;
        } else if (freqMHz <= 1950) {
            level = 1;
        } else if (freqMHz <= 2500) {
            level = 2;
        } else {
            level = 3;
        }
        return level;
    }


    private static int judgeMTCPU(String cpuName, int freqMHz) {
        int level = 0;
        int mtCPUVersion = getMTCPUVersion(cpuName);
        if (mtCPUVersion == -1) {
            if (freqMHz <= 1600) {
                level = 0;
            } else if (freqMHz <= 2200) {
                level = 1;
            } else if (freqMHz <= 2700) {
                level = 2;
            } else {
                level = 3;
            }
        } else if (mtCPUVersion < 6771) {
            if (freqMHz <= 1600) {
                level = 0;
            } else {
                level = 1;
            }
        } else {
            if (freqMHz <= 1600) {
                level = 0;
            } else if (freqMHz <= 1900) {
                level = 1;
            } else if (freqMHz <= 2500) {
                level = 2;
            } else {
                level = 3;
            }
        }

        return level;
    }

    private static int getMTCPUVersion(String cpuName) {
        int cpuVersion = -1;
        if (cpuName.length() > 5) {
            String cpuVersionStr = cpuName.substring(2, 6);
            try {
                cpuVersion = Integer.valueOf(cpuVersionStr);
            } catch (NumberFormatException exception) {
                exception.printStackTrace();
            }
        }

        return cpuVersion;
    }

    private static int judgeQualcommCPU(String cpuName, int freqMHz) {
        int level = 0;
        if (cpuName.contains("MSM")) {
            if (freqMHz <= 1600) {
                level = 0;
            } else {
                level = 1;
            }
        } else {
            if (freqMHz <= 1600) {
                level = 0;
            } else if (freqMHz <= 2000) {
                level = 1;
            } else if (freqMHz <= 2500) {
                level = 2;
            } else {
                level = 3;
            }
        }

        return level;
    }


    private static int judgeSkinCPU(String cpuName, int freqMHz) {
        int level = 0;
        if (cpuName.startsWith("hi")) {
            if (freqMHz <= 1600) {
                level = 0;
            } else if (freqMHz <= 2000) {
                level = 1;
            }
        } else {
            if (freqMHz <= 1600) {
                level = 0;
            } else if (freqMHz <= 2000) {
                level = 1;
            } else if (freqMHz <= 2500) {
                level = 2;
            } else {
                level = 3;
            }
        }

        return level;
    }

    public static final String Nexus_6P = "Nexus 6P";

    public static String getDeviceName() {
        String deviceName = "";
        if (Build.MODEL != null) deviceName = Build.MODEL;
        LogUtils.e(TAG,"deviceName: " + deviceName);
        return deviceName;
    }
}
