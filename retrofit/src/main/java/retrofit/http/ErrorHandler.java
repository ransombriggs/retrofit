package retrofit.http;

/**
 * A hook allowing clients to customize error exceptions and callbacks for requests.
 * 
 * 
 * @author Sam Beran sberan@gmail.com
 */
public interface ErrorHandler {
  /**
   * Called when errors occur during synchronous requests. The returned 
   * exception will be thrown from the client's interface method.
   *
   * If the exception is checked, the exception must be declared to be 
   * thrown on the interface method. 
   * 
   * @param e - the original RetrofitError exception
   * @return an exception which will be thrown from the client interface method.
   */
  Throwable handleError(RetrofitError e);

  /**
   * Called when errors occur during asynchronous requests. This method is responsible
   * for calling the appropriate failure method in the callback class.
   * 
   * @param e the original RetrofitError exception
   * @param callback the callback which was passed to the interface method
   */
  void handleErrorCallback(RetrofitError e, Callback<?> callback);
  
  static ErrorHandler DEFAULT = new ErrorHandler() {
    @Override
    public Throwable handleError(RetrofitError e) {
      return e;
    }

    @Override
    public void handleErrorCallback(RetrofitError e, Callback<?> callback) {
      callback.failure(e);
    }
  };
}
