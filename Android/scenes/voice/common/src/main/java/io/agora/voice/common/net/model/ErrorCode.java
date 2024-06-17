package io.agora.voice.common.net.model;

import io.agora.voice.common.R;

/**
 * This class defines some local error codes for the application.
 * It extends the Error class and provides a nested enum for more specific error types.
 */
public class ErrorCode extends Error {

    /**
     * Constant representing no error.
     */
    public final static int EM_NO_ERROR = 0;

    /**
     * Constant representing a network error.
     */
    public static final int NETWORK_ERROR = -2;

    /**
     * Constant representing an unknown error.
     */
    public static final int ERR_UNKNOWN = -20;

    /**
     * This enum represents specific error types in the application.
     * Each error type has an associated error code and message ID.
     */
    public enum Error {
        NETWORK_ERROR(ErrorCode.NETWORK_ERROR, R.string.voice_error_network_error),
        UNKNOWN_ERROR(-9999, 0);

        private int code;
        private int messageId;

        /**
         * Constructor for the Error enum.
         *
         * @param code The error code.
         * @param messageId The message ID associated with the error.
         */
        private Error(int code, int messageId) {
            this.code = code;
            this.messageId = messageId;
        }

        /**
         * This method parses an error code and returns the corresponding Error enum value.
         *
         * @param errorCode The error code to parse.
         * @return The corresponding Error enum value, or UNKNOWN_ERROR if the error code does not match any defined Error values.
         */
        public static Error parseMessage(int errorCode) {
            for (Error error: Error.values()) {
                if(error.code == errorCode) {
                    return error;
                }
            }
            return UNKNOWN_ERROR;
        }

        /**
         * This method returns the message ID associated with the Error.
         *
         * @return The message ID.
         */
        public int getMessageId() {
            return messageId;
        }
    }
}