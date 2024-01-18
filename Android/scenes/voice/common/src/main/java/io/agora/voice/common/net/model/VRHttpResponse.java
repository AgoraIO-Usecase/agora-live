package io.agora.voice.common.net.model;

import java.io.InputStream;

public class VRHttpResponse {
    public InputStream inputStream;
    public InputStream errorStream;
    public long contentLength;
    public Exception exception;
    /**
     * code return form server
     */
    public int code;
    /**
     * content return from server
     */
    public String content;

    @Override
    public String toString() {
        return "HttpResponse{" +
                "contentLength=" + contentLength +
                ", code=" + code +
                ", content='" + content + '\'' +
                '}';
    }
}