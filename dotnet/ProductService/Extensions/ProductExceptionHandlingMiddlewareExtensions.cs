using ProductService.Middlewares;

namespace ProductService.Extensions
{
    public static class ProductExceptionHandlingMiddlewareExtensions
    {
        public static IApplicationBuilder UseProductExceptionHandling(this IApplicationBuilder app)
        {
            return app.UseMiddleware<ProductExceptionHandlingMiddleware>();
        }
    }
}
