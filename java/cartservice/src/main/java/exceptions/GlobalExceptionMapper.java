package exceptions;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("Unhandled exception caught: ", exception);

        int status;
        String message;

        if (exception instanceof ProductNotAvailableException) {
            status = Response.Status.BAD_REQUEST.getStatusCode();
            message = exception.getMessage();
        } else if (exception instanceof CartItemNotFoundException) {
            status = Response.Status.NOT_FOUND.getStatusCode();
            message = exception.getMessage();
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
            message = "Internal server error";
        }

        return Response.status(status)
                .entity(new ErrorResponse(status, message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    public static class ErrorResponse {
        public int status;
        public String message;

        public ErrorResponse(int status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}