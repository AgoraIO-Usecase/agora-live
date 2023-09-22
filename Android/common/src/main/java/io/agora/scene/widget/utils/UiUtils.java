package io.agora.scene.widget.utils;

/**
 * The type Ui utils.
 */
public final class UiUtils {

    private UiUtils() {

    }
    
    /**
     * The constant receiveMessageTime.
     */
    private static long receiveMessageTime = 0L;
    /**
     * The constant receiveMessageTime2.
     */
    private static long receiveMessageTime2 = 0L;

    /**
     * Is fast click boolean.
     *
     * @return the boolean
     */
    public static boolean isFastClick() {
        if (System.currentTimeMillis() - receiveMessageTime < 1000) {
            return true;
        }
        receiveMessageTime = System.currentTimeMillis();
        return false;
    }

    /**
     * Is fast click boolean.
     *
     * @param time the time
     * @return the boolean
     */
    public static boolean isFastClick(int time) {
        if (System.currentTimeMillis() - receiveMessageTime < time) {
            return true;
        }
        receiveMessageTime = System.currentTimeMillis();
        return false;
    }

    /**
     * Is fast click 3 boolean.
     *
     * @param time the time
     * @return the boolean
     */
    public static boolean isFastClick3(int time) {
        if (System.currentTimeMillis() - receiveMessageTime2 < time) {
            return true;
        }
        receiveMessageTime2 = System.currentTimeMillis();
        return false;
    }
}
