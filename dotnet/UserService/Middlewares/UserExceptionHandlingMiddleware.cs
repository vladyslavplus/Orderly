using System.Net;
using System.Text.Json;
using UserService.Exceptions;

namespace UserService.Middlewares
{
    public class UserExceptionHandlingMiddleware
    {
        private readonly RequestDelegate _next;
        private readonly ILogger<UserExceptionHandlingMiddleware> _logger;

        public UserExceptionHandlingMiddleware(RequestDelegate next, ILogger<UserExceptionHandlingMiddleware> logger)
        {
            _next = next;
            _logger = logger;
        }

        public async Task InvokeAsync(HttpContext context)
        {
            try
            {
                await _next(context);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Exception caught in UserService middleware.");

                var (statusCode, errorType, message) = MapException(ex);

                context.Response.ContentType = "application/json";
                context.Response.StatusCode = (int)statusCode;

                var response = new
                {
                    statusCode = context.Response.StatusCode,
                    errorType,
                    message,
                    timestamp = DateTime.UtcNow
                };

                var json = JsonSerializer.Serialize(response, new JsonSerializerOptions
                {
                    PropertyNamingPolicy = JsonNamingPolicy.CamelCase
                });

                await context.Response.WriteAsync(json);
            }
        }

        private static (HttpStatusCode statusCode, string errorType, string message) MapException(Exception ex)
        {
            return ex switch
            {
                UserAlreadyExistsException uae => (HttpStatusCode.Conflict, nameof(UserAlreadyExistsException), uae.Message),
                UserCreationFailedException ucf => (HttpStatusCode.BadRequest, nameof(UserCreationFailedException), ucf.Message),
                InvalidCredentialsException ice => (HttpStatusCode.Unauthorized, nameof(InvalidCredentialsException), ice.Message),
                UserNotFoundException unf => (HttpStatusCode.NotFound, nameof(UserNotFoundException), unf.Message),
                UserUpdateFailedException uuf => (HttpStatusCode.BadRequest, nameof(UserUpdateFailedException), uuf.Message),
                UserDeletionFailedException udf => (HttpStatusCode.BadRequest, nameof(UserDeletionFailedException), udf.Message),
                InvalidRefreshTokenException irt => (HttpStatusCode.BadRequest, nameof(InvalidRefreshTokenException), irt.Message),
                RefreshTokenNotFoundException rtf => (HttpStatusCode.NotFound, nameof(RefreshTokenNotFoundException), rtf.Message),
                RefreshTokenAlreadyRevokedException rta => (HttpStatusCode.Conflict, nameof(RefreshTokenAlreadyRevokedException), rta.Message),
                ArgumentException argEx => (HttpStatusCode.BadRequest, nameof(ArgumentException), argEx.Message),
                _ => (HttpStatusCode.InternalServerError, "ServerError", "An unexpected error occurred.")
            };
        }
    }
}
