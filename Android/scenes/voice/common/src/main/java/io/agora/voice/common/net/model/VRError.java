package io.agora.voice.common.net.model;

/**
 * This enum represents specific error types in the VR application.
 * Each error type has an associated error code and message.
 */
public enum VRError {

    /**
     * Constant representing no error.
     */
    EM_NO_ERROR("no error",0),

    /**
     * Constant representing a general, unknown error.
     */
    GENERAL_ERROR("Unknown error type",1);

    private int code;
    private String errMsg;

    /**
     * Constructor for the VRError enum.
     *
     * @param errMsg The error message.
     * @param code The error code.
     */
    VRError(String errMsg, int code){
        this.code = code;
        this.errMsg = errMsg;
    }

    /**
     * This method returns the error code associated with the VRError.
     *
     * @return The error code.
     */
    public int errCode() {
        return code;
    }

    /**
     * This method returns the error message associated with the VRError.
     *
     * @return The error message.
     */
    public String errMsg() {
        return errMsg;
    }
}