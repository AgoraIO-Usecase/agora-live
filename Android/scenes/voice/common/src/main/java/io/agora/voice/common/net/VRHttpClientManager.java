package io.agora.voice.common.net;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

import io.agora.voice.common.net.callback.VRHttpCallback;
import io.agora.voice.common.net.model.VRError;
import io.agora.voice.common.net.model.VRException;
import io.agora.voice.common.net.model.VRHttpResponse;
import io.agora.voice.common.utils.LogTools;
import io.agora.voice.common.utils.ThreadManager;

/**
 * The type Vr http client manager.
 */
public class VRHttpClientManager {
    private static final String TAG = "HttpClientManager";
    /**
     * The constant Method_GET.
     */
    public static String Method_GET = "GET";
    /**
     * The constant Method_POST.
     */
    public static String Method_POST = "POST";
    /**
     * The constant Method_PUT.
     */
    public static String Method_PUT = "PUT";
    /**
     * The constant Method_DELETE.
     */
    public static String Method_DELETE = "DELETE";
    /**
     * The constant mInstance.
     */
    public static VRHttpClientManager mInstance;
    private static final int REQUEST_FAILED_CODE = 408;
    private static Context mContext;

    /**
     * Gets instance.
     *
     * @param context the context
     * @return the instance
     */
    public static VRHttpClientManager getInstance(Context context) {
        if(mInstance == null) {
            synchronized (VRHttpClientManager.class) {
                if(mInstance == null) {
                    mInstance = new VRHttpClientManager(context);
                }
            }
        }
        return mInstance;
    }

    /**
     * Instantiates a new Vr http client manager.
     *
     * @param context the context
     */
    VRHttpClientManager(Context context){
        mContext = context;
    }

    /**
     * Send get request pair.
     *
     * @param url     the url
     * @param headers the headers
     * @return the pair
     * @throws VRException the vr exception
     */
    public Pair<Integer, String> sendGetRequest(String url, Map<String, String> headers) throws VRException {
        return sendRequest(url, null, headers, Method_GET);
    }

    /**
     * Send post request pair.
     *
     * @param url     the url
     * @param body    the body
     * @param headers the headers
     * @return the pair
     * @throws VRException the vr exception
     */
    public Pair<Integer, String> sendPostRequest(String url, String body, Map<String, String> headers) throws VRException {
        return sendRequest(url, body, headers, Method_POST);
    }

    /**
     * Send put request pair.
     *
     * @param url     the url
     * @param body    the body
     * @param headers the headers
     * @return the pair
     * @throws VRException the vr exception
     */
    public Pair<Integer, String> sendPutRequest(String url, String body, Map<String, String> headers) throws VRException {
        return sendRequest(url, body, headers, Method_PUT);
    }


    /**
     * Send delete request pair.
     *
     * @param url     the url
     * @param headers the headers
     * @return the pair
     * @throws VRException the vr exception
     */
    public Pair<Integer, String> sendDeleteRequest(String url, Map<String, String> headers) throws VRException {
        return sendRequest(url, null, headers, Method_DELETE);
    }

    /**
     * Send request pair.
     *
     * @param url    the url
     * @param body   the body
     * @param method the method
     * @return the pair
     * @throws VRException the vr exception
     */
    public Pair<Integer, String> sendRequest(String url, String body, String method) throws VRException {
        try {
            return sendHttpRequest(url,null,body,method);
        } catch (IOException e) {
            String errorMsg = " send request : " + url + " failed!";
            if(e != null && e.toString() != null){
                errorMsg = e.toString();
            }
            LogTools.d(TAG, errorMsg);
            throw new VRException(VRError.GENERAL_ERROR.errCode(),VRError.GENERAL_ERROR.errMsg());
        }
    }

    /**
     * Send request pair.
     *
     * @param url     the url
     * @param body    the body
     * @param headers the headers
     * @param method  the method
     * @return the pair
     * @throws VRException the vr exception
     */
    public Pair<Integer, String> sendRequest(String url, String body, Map<String, String> headers, String method) throws VRException {
        try {
            return sendHttpRequest(url,headers,body,method);
        } catch (IOException e) {
            String errorMsg = " send request : " + url + " failed!";
            if(e != null && e.toString() != null){
                errorMsg = e.toString();
            }
            LogTools.d(TAG, errorMsg);
            throw new VRException(VRError.GENERAL_ERROR.errCode(),VRError.GENERAL_ERROR.errMsg());
        }
    }

    /**
     * send io.agora.voice.network.http request with retry get token if it was overdue
     *
     * @param reqURL  the req url
     * @param headers the headers
     * @param body    the body
     * @param method  the method
     * @return return a pair which contains int statusCode and string response content
     * @throws VRException the vr exception
     * @throws IOException the io exception
     */
    public Pair<Integer, String> sendHttpRequest(final String reqURL, final Map<String, String> headers, final String body, final String method) throws VRException, IOException{
        return sendRequest(reqURL, headers, body, method);
    }

    /**
     * Send request pair.
     *
     * @param reqURL  the req url
     * @param headers the headers
     * @param body    the body
     * @param method  the method
     * @return the pair
     * @throws IOException the io exception
     * @throws VRException the vr exception
     */
    public static Pair<Integer,String> sendRequest(final String reqURL, final Map<String, String> headers, final String body, final String method) throws IOException, VRException{
        Pair<Integer,String> value = null;
        VRHttpResponse response = new Builder(mContext)
                .setRequestMethod(method)
                .setUrl(reqURL)
                .setHeaders(headers)
                .setParams(body)
                .execute();

        if (response != null) {
            value = new Pair<Integer, String>(response.code, response.content);
        }
        return value;
    }

    /**
     * Http execute vr http response.
     *
     * @param reqURL  the req url
     * @param headers the headers
     * @param body    the body
     * @param method  the method
     * @param timeout the timeout
     * @return the vr http response
     * @throws IOException the io exception
     */
    public VRHttpResponse httpExecute(String reqURL, Map<String, String> headers, String body, String method, int timeout) throws IOException{
        return new Builder(mContext)
                .setRequestMethod(method)
                .setUrl(reqURL)
                .setConnectTimeout(timeout)
                .setHeaders(headers)
                .setParams(body)
                .execute();
    }

    /**
     * Http execute vr http response.
     *
     * @param reqURL  the req url
     * @param headers the headers
     * @param body    the body
     * @param method  the method
     * @return the vr http response
     * @throws IOException the io exception
     */
    public VRHttpResponse httpExecute(String reqURL, Map<String, String> headers, String body, String method) throws IOException{
        int timeout = VRHttpClientConfig.getTimeout(headers);
        return httpExecute(reqURL, headers, body, method, timeout);
    }

    /**
     * Http execute vr http response.
     *
     * @param reqURL   the req url
     * @param headers  the headers
     * @param body     the body
     * @param method   the method
     * @param callback the callback
     * @param timeout  the timeout
     * @return the vr http response
     * @throws IOException the io exception
     */
    public VRHttpResponse httpExecute(String reqURL, Map<String, String> headers, String body, String method, VRHttpCallback callback, int timeout) throws IOException{
        return new Builder(mContext)
                .setRequestMethod(method)
                .setUrl(reqURL)
                .setConnectTimeout(timeout)
                .setHeaders(headers)
                .setParams(body)
                .execute(callback);
    }

    /**
     * Http execute vr http response.
     *
     * @param reqURL   the req url
     * @param headers  the headers
     * @param body     the body
     * @param method   the method
     * @param callback the callback
     * @return the vr http response
     * @throws IOException the io exception
     */
    public VRHttpResponse httpExecute(String reqURL, Map<String, String> headers, String body, String method, VRHttpCallback callback) throws IOException{
        int timeout = VRHttpClientConfig.getTimeout(headers);
        return httpExecute(reqURL, headers, body, method, callback,timeout);
    }

    public static class Builder {
        private final VRHttpClientController.HttpParams p;

        public Builder(Context context) {
            p = new VRHttpClientController.HttpParams(context);
        }

        public Builder get() {
            p.mRequestMethod = "GET";
            return this;
        }

        public Builder post() {
            p.mRequestMethod = "POST";
            return this;
        }

        public Builder put() {
            p.mRequestMethod = "PUT";
            return this;
        }

        public Builder delete() {
            p.mRequestMethod = "DELETE";
            return this;
        }

        /**
         * Sets request method.
         *
         * @param requestMethod the request method
         * @return the request method
         */
        public Builder setRequestMethod(@NonNull String requestMethod) {
            p.mRequestMethod = requestMethod;
            return this;
        }

        /**
         * Sets url.
         *
         * @param url the url
         * @return the url
         */
        public Builder setUrl(@NonNull String url) {
            p.mUrl = url;
            return this;
        }

        /**
         * Sets url.
         *
         * @param url  the url
         * @param port the port
         * @return the url
         */
        public Builder setUrl(@NonNull String url, int port) {
            p.mUrl = url;
            p.mPort = port;
            return this;
        }

        /**
         * Sets connect timeout.
         *
         * @param timeout the timeout
         * @return the connect timeout
         */
        public Builder setConnectTimeout(int timeout) {
            p.mConnectTimeout = timeout;
            return this;
        }

        /**
         * Sets read timeout.
         *
         * @param timeout the timeout
         * @return the read timeout
         */
        public Builder setReadTimeout(int timeout) {
            p.mReadTimeout = timeout;
            return this;
        }

        /**
         * Sets header.
         *
         * @param key   the key
         * @param value the value
         * @return the header
         */
        public Builder setHeader(String key, String value) {
            p.mHeaders.put(key, value);
            return this;
        }

        /**
         * Sets headers.
         *
         * @param headers the headers
         * @return the headers
         */
        public Builder setHeaders(Map<String, String> headers) {
            p.mHeaders.putAll(headers);
            return this;
        }

        /**
         * Sets param.
         *
         * @param key   the key
         * @param value the value
         * @return the param
         */
        public Builder setParam(String key, String value) {
            p.mParams.put(key, value);
            return this;
        }

        /**
         * Sets params.
         *
         * @param params the params
         * @return the params
         */
        public Builder setParams(Map<String, String> params) {
            p.mParams.putAll(params);
            return this;
        }

        /**
         * Sets params.
         *
         * @param params the params
         * @return the params
         */
        public Builder setParams(String params) {
            p.mParamsString = params;
            return this;
        }

        /**
         * Sets retry times.
         *
         * @param retryTimes the retry times
         * @return the retry times
         */
        public Builder setRetryTimes(int retryTimes) {
            p.canRetry = true;
            p.mRetryTimes = retryTimes;
            return this;
        }

        /**
         * Build vr http client controller.
         *
         * @return the vr http client controller
         * @throws IOException the io exception
         */
        public VRHttpClientController build() throws IOException {
            VRHttpClientController controller = new VRHttpClientController(p.mContext);
            p.apply(controller);
            return controller;
        }

        private VRHttpResponse executePrivate(VRHttpCallback callback) throws IOException {
            VRHttpResponse response = null;
            VRHttpClientController controller = null;
            try {
                controller = build();
                HttpURLConnection connect = controller.connect();
                boolean isConnectionReset = false;
                if(connect.getDoOutput()) {
                    DataOutputStream out = new DataOutputStream(connect.getOutputStream());
                    controller.addParams(p.mParamsString, out);
                    controller.addParams(p.mParams, out);
                }
                response = p.getResponse(controller);
                LogTools.d(TAG, response.toString());

                if(response.code==401) {
                    LogTools.d(TAG, "Unable to authenticate (OAuth)");
                }
                if(response.code == HttpURLConnection.HTTP_OK) {
                    if (callback != null) {
                        callback.onSuccess(response.content);
                    }
                }else {
                    if(callback != null) {
                        callback.onError(response.code, response.content);
                    }
                }
            } catch (IOException e) {
                LogTools.e(TAG, "error message = "+e.getMessage());
                throw e;
            } catch (IllegalStateException e) {
                LogTools.e(TAG, "error message = "+e.getMessage());
                throw e;
            }
            return response;
        }

        public VRHttpResponse execute() {
            return execute(null);
        }

        public VRHttpResponse execute(VRHttpCallback callback) {
            return executeNormal(callback);
        }

        public void asyncExecute(VRHttpCallback callback) {
            asyncExecuteNormal(callback);
        }

        private void asyncExecuteNormal(VRHttpCallback callback) {
            ThreadManager.getInstance().runOnIOThread(() -> {
                executeNormal(callback);
            });
        }

        private VRHttpResponse executeNormal(VRHttpCallback callback) {
            VRHttpResponse response = null;
            try {
                response = executePrivate(callback);
                if(response.code != HttpURLConnection.HTTP_OK) {
                    if(p.canRetry && p.mRetryTimes > 0) {
                        p.mRetryTimes--;
                        return executeNormal(callback);
                    }
                }
                return response;
            } catch (IOException e) {
                String message = (e != null && e.getMessage() != null)?e.getMessage():"failed to request";
                LogTools.e(TAG, "error execute:" + message);
                if(p.canRetry && p.mRetryTimes > 0) {
                    p.mRetryTimes--;
                    return executeNormal(callback);
                }
                if(response == null) {
                    response = new VRHttpResponse();
                }
                if(response.code == 0) {
                    response.code = REQUEST_FAILED_CODE;
                }
                response.content = message;
                if(callback != null){
                    callback.onError(response.code, message);
                }
                return response;
            }
        }
    }

}
