package io.agora.voice.common.net.model;

/**
 * This class represents a custom exception for the VR application.
 * It extends the Exception class and provides additional fields for an error code and description.
 */
public class VRException extends Exception{

   /**
    * The error code associated with this exception.
    */
   protected int errorCode = -1;

   /**
    * The description of this exception.
    */
   protected String desc = "";

   /**
    * Default constructor for the VRException class.
    */
   public VRException() {
      super();
   }

   /**
    * Constructs an exception with the given description.
    *
    * @param desc The exception description.
    */
   public VRException(String desc) {
      super(desc);
   }

   /**
    * Constructs an exception with the given VRError.
    *
    * @param error The VRError to construct the exception with.
    */
   public VRException(VRError error) {
      super(error.errMsg());
      errorCode = error.errCode();
      desc = error.errMsg();
   }

   /**
    * Constructs an exception with the given description and exception cause.
    *
    * @param desc The exception description.
    * @param cause The exception cause.
    */
   public VRException(String desc, Throwable cause) {
      super(desc);
      super.initCause(cause);
   }

   /**
    * Constructs an exception with the given description and error code.
    *
    * @param errorCode The error code.
    * @param desc The exception description.
    */
   public VRException(int errorCode, String desc){
      super(desc);
      this.errorCode = errorCode;
      this.desc = desc;
   }

   /**
    * Gets the error code.
    *
    * @return  The error code.
    */
   public int getErrorCode() {
      return errorCode;
   }

   /**
    * Gets the exception description.
    *
    * @return  The exception description.
    */
   public String getDescription() {
      return this.desc;
   }

   /**
    * Sets the error code.
    *
    * @param errorCode The error code to set.
    */
   public void setErrorCode(int errorCode) {
      this.errorCode = errorCode;
   }

}