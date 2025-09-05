using ProductService.Services;

namespace ProductService.Extensions
{
    public static class ServiceCollectionExtensions
    {
        public static IServiceCollection AddProductServiceDependencies(this IServiceCollection services, IConfiguration configuration)
        {
            services.AddScoped<IProductService, ProductService.Services.ProductService>();

            return services;
        }
    }
}
