using Contracts.Events.User;
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

                    cfg.Message<UserCreatedEvent>(e => e.SetEntityName("UserCreatedEvent"));
                    cfg.Message<UserUpdatedEvent>(e => e.SetEntityName("UserUpdatedEvent"));
                    cfg.Message<UserDeletedEvent>(e => e.SetEntityName("UserDeletedEvent"));

                    cfg.Publish<UserCreatedEvent>(p =>
                    {
                        p.ExchangeType = RabbitMQ.Client.ExchangeType.Fanout;
                        p.Durable = true;
                    });
                    cfg.Publish<UserUpdatedEvent>(p =>
                    {
                        p.ExchangeType = RabbitMQ.Client.ExchangeType.Fanout;
                        p.Durable = true;
                    });
                    cfg.Publish<UserDeletedEvent>(p =>
                    {
                        p.ExchangeType = RabbitMQ.Client.ExchangeType.Fanout;
                        p.Durable = true;
                    });

                    cfg.ConfigureEndpoints(context);
                });
            });

            return services;
        }
    }
}
