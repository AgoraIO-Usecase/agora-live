package io.agora.voice.common.net.callback;

/**
 * This interface represents a callback for HTTP requests in the VR application.
 * It provides two methods: onSuccess and onError, which are invoked when the HTTP request completes successfully or fails, respectively.
 */
public interface VRHttpCallback {
 /**
  * This method is invoked when the HTTP request completes successfully.
  *
  * @param result The result of the HTTP request as a String.
  */
 default void onSuccess(String result){}

 /**
  * This method is invoked when the HTTP request fails.
  *
  * @param code The error code of the failure.
  * @param msg The error message of the failure.
  */
 default void onError(int code,String msg){}
}