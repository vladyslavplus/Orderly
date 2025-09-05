using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using System.Net;
using System.Text.Json;

namespace Common.Middlewares
{
    public class GlobalExceptionHandlingMiddleware
    {
        private readonly RequestDelegate _next;
        private readonly ILogger<GlobalExceptionHandlingMiddleware> _logger;
        private readonly IHostEnvironment _env;

        public GlobalExceptionHandlingMiddleware(RequestDelegate next, ILogger<GlobalExceptionHandlingMiddleware> logger, IHostEnvironment env)
        {
            _next = next;
            _logger = logger;
            _env = env;
        }

        public async Task InvokeAsync(HttpContext context)
        {
            string traceId = Guid.NewGuid().ToString();

            try
            {
                await _next(context);
            }
            catch (Exception ex)
            {
                var (statusCode, errorType) = MapExceptionToStatusCode(ex);

                _logger.LogError(ex, "Unhandled exception occurred. TraceId: {TraceId}", traceId);

                context.Response.ContentType = "application/json";
                context.Response.StatusCode = statusCode;

                var response = new
                {
                    statusCode,
                    errorType,
                    message = ex.Message,
                    traceId,
                    timestamp = DateTime.UtcNow,
                    stackTrace = _env.IsDevelopment() ? ex.StackTrace : null
                };

                var json = JsonSerializer.Serialize(response, new JsonSerializerOptions { PropertyNamingPolicy = JsonNamingPolicy.CamelCase });
                await context.Response.WriteAsync(json);
            }
        }

        private static (int StatusCode, string ErrorType) MapExceptionToStatusCode(Exception ex)
        {
            return ex switch
            {
                KeyNotFoundException => ((int)HttpStatusCode.NotFound, "NotFound"),
                ArgumentException => ((int)HttpStatusCode.BadRequest, "BadRequest"),
                UnauthorizedAccessException => ((int)HttpStatusCode.Unauthorized, "Unauthorized"),
                InvalidOperationException => ((int)HttpStatusCode.Conflict, "Conflict"),
                _ => ((int)HttpStatusCode.InternalServerError, "ServerError")
            };
        }
    }
}
