using ProductService.Exceptions;
using System.Net;
using System.Text.Json;

namespace ProductService.Middlewares
{
    public class ProductExceptionHandlingMiddleware
    {
        private readonly RequestDelegate _next;
        private readonly ILogger<ProductExceptionHandlingMiddleware> _logger;

        public ProductExceptionHandlingMiddleware(RequestDelegate next, ILogger<ProductExceptionHandlingMiddleware> logger)
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
                _logger.LogError(ex, "Exception caught in ProductService middleware.");

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
                ProductNotFoundException notFound => (HttpStatusCode.NotFound, nameof(ProductNotFoundException), notFound.Message),
                ProductAlreadyExistsException conflict => (HttpStatusCode.Conflict, nameof(ProductAlreadyExistsException), conflict.Message),
                ProductCreationFailedException creationFail => (HttpStatusCode.BadRequest, nameof(ProductCreationFailedException), creationFail.Message),
                ProductUpdateFailedException updateFail => (HttpStatusCode.BadRequest, nameof(ProductUpdateFailedException), updateFail.Message),
                ProductDeletionFailedException deletionFail => (HttpStatusCode.BadRequest, nameof(ProductDeletionFailedException), deletionFail.Message),
                ArgumentException argEx => (HttpStatusCode.BadRequest, nameof(ArgumentException), argEx.Message),
                _ => (HttpStatusCode.InternalServerError, "ServerError", "An unexpected error occurred.")
            };
        }
    }
}
