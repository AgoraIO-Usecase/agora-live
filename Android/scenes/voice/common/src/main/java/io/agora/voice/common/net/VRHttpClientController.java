package io.agora.voice.common.net;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.agora.voice.common.net.model.VRHttpResponse;
import io.agora.voice.common.utils.LogTools;

/**
 * The type Vr http client controller.
 */
class VRHttpClientController {
    private static final String TAG = VRHttpClientController.class.getSimpleName();
    private URL mURL;
    private HttpURLConnection mConn;
    private static int EM_DEFAULT_TIMEOUT = 60 * 1000;
    private static int EM_DEFAULT_READ_TIMEOUT = 60 * 1000;
    private static final String BOUNDARY = java.util.UUID.randomUUID().toString();
    private static final String TWO_HYPHENS = "--";
    private static final String LINE_END = "\r\n";

    /**
     * Instantiates a new Vr http client controller.
     */
    public VRHttpClientController() {
    }


    /**
     * Sets url.
     *
     * @param url the url
     * @throws IOException the io exception
     */
    public void setURL(String url) throws IOException {
        setURL(url, -1);
    }

    /**
     * Sets url.
     *
     * @param url  the url
     * @param port the port
     * @throws IOException the io exception
     */
    public void setURL(String url, int port) throws IOException {
        url = VRHttpClientConfig.processUrl(url);
        // Set the default HTTP port number to 80
        URL originUrl = new URL(url);
        String protocol = originUrl.getProtocol();
        int originPort = originUrl.getPort();
        // The default port is -1, and originPort is not -1
        if (originPort != -1) {
            port = originPort;
        }
        mURL = new URL(protocol, originUrl.getHost(), port, originUrl.getFile());
        mConn = (HttpURLConnection) mURL.openConnection();
    }

    /**
     * Sets request method.
     *
     * @param requestMethod the request method
     * @throws ProtocolException the protocol exception
     */
    public void setRequestMethod(String requestMethod) throws ProtocolException {
        mConn.setRequestMethod(requestMethod);
    }

    /**
     * Sets connect timeout.
     *
     * @param timeout the timeout
     */
    public void setConnectTimeout(int timeout) {
        if (timeout <= 0) {
            timeout = EM_DEFAULT_TIMEOUT;
        }
        mConn.setConnectTimeout(timeout);
    }

    /**
     * Sets read timeout.
     *
     * @param timeout the timeout
     */
    public void setReadTimeout(int timeout) {
        if (timeout <= 0) {
            timeout = EM_DEFAULT_READ_TIMEOUT;
        }
        mConn.setReadTimeout(timeout);
    }

    /**
     * Sets token.
     */
    public void setToken() {
        mConn.setRequestProperty("Authorization", "Bearer ");
    }


    /**
     * Sets default request header.
     */
    public void setDefaultProperty() {
        mConn.setRequestProperty("Connection", "Keep-Alive");
    }

    /**
     * Sets get connection.
     */
    public void setGetConnection() {
        mConn.setDoInput(true);
    }

    /**
     * Sets post connection.
     */
    public void setPostConnection() {
        mConn.setDoOutput(true);
        mConn.setDoInput(true);
        mConn.setUseCaches(false);
    }


    /**
     * Sets delete connection.
     */
    public void setDeleteConnection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mConn.setDoOutput(true);
            mConn.setDoInput(true);
            mConn.setUseCaches(false);
        }
    }

    /**
     * Gets http url connection.
     *
     * @return the http url connection
     */
    public HttpURLConnection getHttpURLConnection() {
        return mConn;
    }

    /**
     * Add request header.
     *
     * @param headers the headers
     */
    public void addHeader(Map<String, String> headers) {
        if (headers != null && headers.size() > 0) {
            for (Map.Entry<String, String> item : headers.entrySet()) {
                mConn.setRequestProperty(item.getKey(), item.getValue());
            }
        }
    }

    /**
     * Add params.
     *
     * @param params the params
     * @param out    the out
     * @throws IOException the io exception
     */
    public void addParams(Map<String, String> params, OutputStream out) throws IOException {
        LogTools.d(TAG, "request Map params = " + params.toString());
        if (params == null || params.size() <= 0) {
            return;
        }
        String paramsString = getParamsString(params);
        if (TextUtils.isEmpty(paramsString)) {
            return;
        }
        out.write(paramsString.getBytes());
        out.flush();
    }

    /**
     * Add params.
     *
     * @param params the params
     * @param out    the out
     * @throws IOException the io exception
     */
    public void addParams(String params, OutputStream out) throws IOException {
        //LogTools.d(TAG, "request String params = "+params);
        if (TextUtils.isEmpty(params)) {
            return;
        }
        out.write(params.getBytes());
        out.flush();
    }

    /**
     * Connect http url connection.
     *
     * @return the http url connection
     * @throws IOException the io exception
     */
    public HttpURLConnection connect() throws IOException {
        printRequestInfo(true);
        mConn.connect();
        return mConn;
    }

    private void printRequestInfo(boolean showInfo) throws IllegalStateException {
        if (showInfo && mConn != null) {
            LogTools.d(TAG, "request start =========================== ");
            LogTools.d(TAG, "request url = " + mConn.getURL());
            LogTools.d(TAG, "request method = " + mConn.getRequestMethod());
            LogTools.d(TAG, "request header = " + mConn.getRequestProperties().toString());
            LogTools.d(TAG, "request end =========================== ");
        }
    }

    /**
     * Gets http response.
     *
     * @return the http response
     * @throws IOException the io exception
     */
    public VRHttpResponse getHttpResponse() throws IOException {
        VRHttpResponse response = new VRHttpResponse();
        response.code = mConn.getResponseCode();
        if (response.code == HttpURLConnection.HTTP_OK) {
            response.contentLength = mConn.getContentLength();
            response.inputStream = mConn.getInputStream();
            response.content = parseStream(response.inputStream);
        } else {
            response.errorStream = mConn.getErrorStream();
            response.content = parseStream(response.errorStream);
        }
        printResponseInfo(true, response);
        return response;
    }

    private void printResponseInfo(boolean showInfo, VRHttpResponse response) {
        if (mConn == null || response == null) {
            return;
        }
        if (showInfo) {
            LogTools.d(TAG, "response ==========================start =================");
            //LogTools.d(TAG, "content: "+response.content);
            LogTools.d(TAG, "url: " + mConn.getURL().toString());
            LogTools.d(TAG, "headers: " + mConn.getHeaderFields().toString());
            LogTools.d(TAG, "response ==========================end =================");
        } else {
            LogTools.d(TAG, "response code: " + response.code);
            if (response.code != HttpURLConnection.HTTP_OK) {
                LogTools.d(TAG, "error message: " + response.content);
            }
        }
    }

    private String getParamsString(Map<String, String> paramsMap) {
        if (paramsMap == null || paramsMap.size() <= 0) {
            return null;
        }
        StringBuffer strBuf = new StringBuffer();
        for (String key : paramsMap.keySet()) {
            strBuf.append(TWO_HYPHENS);
            strBuf.append(BOUNDARY);
            strBuf.append(LINE_END);
            strBuf.append("Content-Disposition: form-data; name=\"" + key + "\"");
            strBuf.append(LINE_END);

            strBuf.append("Content-Type: " + "text/plain");
            strBuf.append(LINE_END);
            strBuf.append("Content-Length: " + paramsMap.get(key).length());
            strBuf.append(LINE_END);
            strBuf.append(LINE_END);
            strBuf.append(paramsMap.get(key));
            strBuf.append(LINE_END);
        }
        return strBuf.toString();
    }

    /**
     * Get exception response
     *
     * @param e the e
     * @return exception response
     * @throws IOException the io exception
     */
    public VRHttpResponse getExceptionResponse(Exception e) throws IOException {
        VRHttpResponse response = new VRHttpResponse();
        if (mConn != null) {
            response.code = mConn.getResponseCode();
            response.contentLength = mConn.getContentLength();
            response.errorStream = mConn.getErrorStream();
            mConn.disconnect();
        }
        response.exception = e;
        return response;
    }

    private String parseStream(InputStream is) {
        String buf;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            buf = sb.toString();
            return buf;

        } catch (Exception e) {
            return null;
        }
    }

    private void checkAndProcessSSL(String url) {
        VRHttpClientConfig.checkAndProcessSSL(url, mConn);
    }

    public static class HttpParams {
        public String mRequestMethod;
        public int mPort = -1;//https default 443，http default 80
        public int mConnectTimeout;
        public int mReadTimeout;
        public boolean canRetry;
        public int mRetryTimes;

        public Map<String, String> mHeaders = new HashMap<>();
        public Map<String, String> mParams = new HashMap<>();
        public String mParamsString;
        public String mUrl;


        /**
         * Instantiates a new Http params.
         */
        public HttpParams() {

        }

        /**
         * Apply.
         *
         * @param controller the controller
         * @throws IOException the io exception
         */
        public void apply(VRHttpClientController controller) throws IOException {
            if (mPort != -1) {
                controller.setURL(mUrl, mPort);
            } else {
                controller.setURL(mUrl);
            }
            controller.setRequestMethod(mRequestMethod);

            if ("GET".equalsIgnoreCase(mRequestMethod)) {
                controller.setGetConnection();
            } else if ("DELETE".equalsIgnoreCase(mRequestMethod)) {
                controller.setDeleteConnection();
            } else {
                controller.setPostConnection();
            }

            controller.setConnectTimeout(mConnectTimeout);
            controller.setReadTimeout(mReadTimeout);
            controller.setDefaultProperty();
            controller.checkAndProcessSSL(mUrl);
            checkToken();
            controller.addHeader(mHeaders);
        }

        /**
         * Gets response.
         *
         * @param controller the controller
         * @return the response
         * @throws IOException the io exception
         */
        public VRHttpResponse getResponse(VRHttpClientController controller) throws IOException {
            return controller.getHttpResponse();
        }

        /**
         * Gets exception response.
         *
         * @param controller the controller
         * @param e          the e
         * @return the exception response
         * @throws IOException the io exception
         */
        public VRHttpResponse getExceptionResponse(VRHttpClientController controller, IOException e) throws IOException {
            if (controller != null) {
                return controller.getExceptionResponse(e);
            }
            return null;
        }

        /**
         * Check token.
         */
        public void checkToken() {
            if (mHeaders.keySet().contains("Authorization")) {
                if (TextUtils.isEmpty(mHeaders.get("Authorization"))) {

                }
            }
        }
    }
}
