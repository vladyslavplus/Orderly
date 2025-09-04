using MassTransit;
using UserService.Services;

namespace UserService.Extensions
{
    public static class ServiceCollectionExtensions
    {
        public static IServiceCollection AddUserServiceDependencies(this IServiceCollection services, IConfiguration configuration)
        {
            services.AddScoped<IUserService, UserService.Services.UserService>();

            return services;
        }
    }
}
