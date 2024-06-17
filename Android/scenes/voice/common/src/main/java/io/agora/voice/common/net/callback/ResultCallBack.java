package io.agora.voice.common.net.callback;

/**
 * This interface represents a callback that is invoked upon the completion of a certain task.
 * The task can either succeed, in which case the onSuccess method is invoked, or fail, in which case the onError method is invoked.
 *
 * @param <T> The type of the result that is returned if the task succeeds.
 */
public interface ResultCallBack<T>
{
    /**
     * This method is invoked when the task completes successfully.
     *
     * @param value The result of the task. The class type of value is T.
     */
    void onSuccess(T value);

    /**
     * This method is invoked when the task fails.
     *
     * @param error     The error code. See {@link Error}.
     * @param errorMsg  A description of the issue that caused this call to fail.
     */
    void onError(final int error, final String errorMsg);

    /**
     * This method is a default implementation of the onError method that can be used when no error message is provided.
     *
     * @param error The error code. See {@link Error}.
     */
    default void onError(final int error){
        onError(error,"");
    }
}
