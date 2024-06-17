package io.agora.voice.common.net;

import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This class provides configuration for the VR HTTP client.
 * It includes methods for processing URLs, getting timeouts, and handling SSL.
 */
public class VRHttpClientConfig {
    private static final String TAG = VRHttpClientConfig.class.getSimpleName();
    public static String EM_TIME_OUT_KEY = "em_timeout";
    public static int EM_DEFAULT_TIMEOUT = 60 * 1000;

    /**
     * This method processes a URL by replacing certain characters with their URL-encoded equivalents.
     *
     * @param remoteUrl The URL to process.
     * @return The processed URL.
     */
    public static String processUrl(String remoteUrl){
        if (remoteUrl.contains("+")) {
            remoteUrl = remoteUrl.replaceAll("\\+", "%2B");
        }

        if (remoteUrl.contains("#")) {
            remoteUrl = remoteUrl.replaceAll("#", "%23");
        }

        return remoteUrl;
    }

    /**
     * This method retrieves the timeout from the headers, if it exists.
     * If the timeout does not exist in the headers, it returns the default timeout.
     *
     * @param headers The headers to retrieve the timeout from.
     * @return The timeout.
     */
    public static int getTimeout(Map<String,String> headers){
        int timeout = VRHttpClientConfig.EM_DEFAULT_TIMEOUT;

        if(headers != null && headers.get(VRHttpClientConfig.EM_TIME_OUT_KEY) != null){
            timeout = Integer.valueOf(headers.get(VRHttpClientConfig.EM_TIME_OUT_KEY));
            headers.remove(VRHttpClientConfig.EM_TIME_OUT_KEY);
        }

        return timeout;
    }

    /**
     * This TrustManager does not perform any checks, resulting in all certificates being trusted.
     */
    private static TrustManager trustManager = new X509TrustManager() {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    };

    /**
     * This method checks and processes SSL for a given URL and connection.
     * It sets up an SSL context with the trust manager defined in this class and applies it to the connection.
     *
     * @param url The URL to check and process SSL for.
     * @param conn The connection to apply the SSL context to.
     */
    static void checkAndProcessSSL(String url, HttpURLConnection conn) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
//            conn.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustManager);
        } catch ( NoSuchAlgorithmException | KeyManagementException |IllegalStateException e) {
            e.printStackTrace();
        }

    }
}