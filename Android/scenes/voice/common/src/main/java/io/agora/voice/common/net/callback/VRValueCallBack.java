package io.agora.voice.common.net.callback;

/**
 * This interface represents a callback for value-based operations in the VR application.
 * It provides two methods: onSuccess and onError, which are invoked when the operation completes successfully or fails, respectively.
 *
 * @param <T> The type of the result that is returned if the operation succeeds.
 */
public interface VRValueCallBack<T> {
    /**
     * This method is invoked when the operation completes successfully.
     *
     * @param var1 The result of the operation. The class type of var1 is T.
     */
    void onSuccess(T var1);

    /**
     * This method is invoked when the operation fails.
     *
     * @param var1 The error code of the failure.
     * @param var2 The error message of the failure.
     */
    void onError(int var1, String var2);
}