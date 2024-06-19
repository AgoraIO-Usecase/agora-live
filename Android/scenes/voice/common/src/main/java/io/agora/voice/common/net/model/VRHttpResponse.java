package io.agora.voice.common.net.model;

import java.io.InputStream;

/**
 * This class represents an HTTP response in the VR application.
 * It provides fields for the input stream, error stream, content length, exception, code, and content of the response.
 */
public class VRHttpResponse {
    /**
     * The input stream of the HTTP response.
     */
    public InputStream inputStream;

    /**
     * The error stream of the HTTP response.
     */
    public InputStream errorStream;

    /**
     * The content length of the HTTP response.
     */
    public long contentLength;

    /**
     * Any exception that occurred during the HTTP request.
     */
    public Exception exception;

    /**
     * The status code returned from the server.
     */
    public int code;

    /**
     * The content returned from the server.
     */
    public String content;

    /**
     * This method returns a string representation of the HTTP response.
     *
     * @return A string representation of the HTTP response.
     */
    @Override
    public String toString() {
        return "HttpResponse{" +
                "contentLength=" + contentLength +
                ", code=" + code +
                ", content='" + content + '\'' +
                '}';
    }
}