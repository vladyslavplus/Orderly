using MassTransit;

namespace UserService.Extensions
{
    public static class MassTransitExtensions
    {
        public static IServiceCollection AddRabbitMqMassTransit(this IServiceCollection services, IConfiguration configuration)
        {
            var rabbitMqConfig = configuration.GetSection("RabbitMq");

            var host = rabbitMqConfig["Host"];
            var username = rabbitMqConfig["Username"];
            var password = rabbitMqConfig["Password"];

            services.AddMassTransit(x =>
            {
                x.UsingRabbitMq((context, cfg) =>
                {
                    cfg.Host(host, "/", h =>
                    {
                        h.Username(username!);
                        h.Password(password!);
                    });
                });
            });

            return services;
        }
    }
}
