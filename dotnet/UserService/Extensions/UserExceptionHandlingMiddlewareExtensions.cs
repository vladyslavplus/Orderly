using UserService.Middlewares;

namespace UserService.Extensions
{
    public static class UserExceptionHandlingMiddlewareExtensions
    {
        public static IApplicationBuilder UseUserExceptionHandling(this IApplicationBuilder app)
        {
            return app.UseMiddleware<UserExceptionHandlingMiddleware>();
        }
    }
}
