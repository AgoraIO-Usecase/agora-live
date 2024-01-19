package io.agora.voice.common.net.callback;

public interface VRHttpCallback {
 /**
  * success call back
  *
  * @param result
  */
 default void onSuccess(String result){}

 /**
  * failed call back
  *
  * @param code
  * @param msg
  */
 default void onError(int code,String msg){}

}
