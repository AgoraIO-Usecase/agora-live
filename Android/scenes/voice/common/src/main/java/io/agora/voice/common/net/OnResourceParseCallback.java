package io.agora.voice.common.net;

import androidx.annotation.Nullable;

/**
 * This abstract class is used to parse Resource<T> objects in the VR application.
 * It provides methods for handling success, error, loading, and hiding loading states.
 * It also provides a field for controlling whether error messages should be hidden.
 *
 * @param <T> The type of the data that is being parsed.
 */
public abstract class OnResourceParseCallback<T> {

    /**
     * A boolean value that determines whether error messages should be hidden.
     */
    public boolean hideErrorMsg;

    /**
     * Default constructor for the OnResourceParseCallback class.
     */
    public OnResourceParseCallback() {}

    /**
     * Constructor for the OnResourceParseCallback class that allows setting whether error messages should be hidden.
     *
     * @param hideErrorMsg A boolean value that determines whether error messages should be hidden.
     */
    public OnResourceParseCallback(boolean hideErrorMsg) {
        this.hideErrorMsg = hideErrorMsg;
    }

    /**
     * This method is invoked when the parsing operation is successful.
     *
     * @param data The parsed data.
     */
    public abstract void onSuccess(@Nullable T data);

    /**
     * This method is invoked when the parsing operation fails.
     *
     * @param code The error code of the failure.
     * @param message The error message of the failure.
     */
    public void onError(int code, String message){}

    /**
     * This method is invoked when the parsing operation is in progress.
     *
     * @param data The data that is currently being parsed.
     */
    public void onLoading(@Nullable T data){}

    /**
     * This method is invoked to hide the loading state.
     */
    public void onHideLoading(){}
}