package retrofit.http;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import retrofit.http.client.Client;
import retrofit.http.client.Request;
import retrofit.http.client.Response;

import java.io.IOException;
import java.util.Collections;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

public class ErrorHandlerTest {


  interface ExampleClient {
    @POST("/") @Multipart
    Void create(@Name("object") Object toCreate) throws InvalidObjectException;

    @POST("/") @Multipart
    void createAsync(@Name("object") Object toCreate, ValidatedCallback<Void> callback);
  }

  
  /* An HTTP client which always returns a 400 response */
  static class MockInvalidResponseClient implements Client {
    @Override
    public Response execute(Request request) throws IOException {
      return new Response(400, "invalid request", Collections.<Header>emptyList(), null);
    }
  }

  /**
   * An example exception representing an attempt to send an invalid object to the server
   */
  static class InvalidObjectException extends Exception {
    public InvalidObjectException(String message) {
      super(message);
    }
  }

  /**
   * An example Callback for an asynchronous request with validation capability
   */
  interface ValidatedCallback<T> extends Callback<T> {
    void invalid(InvalidObjectException cause);
  }

  /**
   * A custom error handler, which is responsible for detecting a validation error and 
   * either returning an exception, or calling the appropriate callback method for async requests
   */
  static class InvalidObjectErrorHandler implements ErrorHandler {
    private boolean isInvalidRequest(RetrofitError e) {
      Response response = e.getResponse();
      return response != null && response.getStatus() == 400 && "invalid request".equals(response.getReason());
    }
    
    @Override
    public Throwable handleError(RetrofitError e) {
      if(isInvalidRequest(e)) {
        return new InvalidObjectException(e.getResponse().getReason());
      }
      return e;
    }

    @Override
    public void handleErrorCallback(RetrofitError e, Callback<?> callback) {
      if(callback instanceof ValidatedCallback && isInvalidRequest(e)) {
        ((ValidatedCallback) callback).invalid(new InvalidObjectException(e.getResponse().getReason()));
      } else {
        callback.failure(e); 
      }
    }
  }
  
  ExampleClient client;

  @Before
  public void setup() {
    client = new RestAdapter.Builder()
        .setServer("http://example.com")
        .setClient(new MockInvalidResponseClient())
        .setErrorHandler(new InvalidObjectErrorHandler())
        .setExecutors(new Utils.SynchronousExecutor(), new Utils.SynchronousExecutor())
        .build()
        .create(ExampleClient.class);
  }


  @Test
  public void testSynchronousExceptionThrown() {
    try {
      client.create("Example Value");
      fail();
    } catch (InvalidObjectException e) {
      assertThat(e.getMessage()).isEqualTo("invalid request");
    }
  }

  @Test
  public void testAsyncInvalidCallback() {
    @SuppressWarnings("unchecked")
    ValidatedCallback<Void> mockCallback = Mockito.mock(ValidatedCallback.class);

    client.createAsync("Example Value", mockCallback);

    verify(mockCallback).invalid(any(InvalidObjectException.class));
  }

}
