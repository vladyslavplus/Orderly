using Contracts.Events.Product;
using MassTransit;
using ProductService.Consumers;
using System.Net.Mime;

namespace ProductService.Extensions
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
                x.AddConsumer<OrderCreatedConsumer>();
                x.AddConsumer<OrderDeletedConsumer>();

                x.UsingRabbitMq((context, cfg) =>
                {
                    cfg.Host(host, "/", h =>
                    {
                        h.Username(username!);
                        h.Password(password!);
                    });

                    cfg.ReceiveEndpoint("product-service-order-deleted", e =>
                    {
                        e.ConfigureConsumeTopology = false;
                        e.ClearSerialization();
                        e.DefaultContentType = new ContentType("application/json");
                        e.UseRawJsonSerializer();
                        e.Consumer<OrderDeletedConsumer>(context);
                        e.Bind("order-deleted-events", s =>
                        {
                            s.ExchangeType = RabbitMQ.Client.ExchangeType.Fanout;
                        });
                    });

                    cfg.ReceiveEndpoint("product-service-order-created", e =>
                    {
                        e.ConfigureConsumeTopology = false;
                        e.ClearSerialization();
                        e.DefaultContentType = new ContentType("application/json");
                        e.UseRawJsonSerializer();
                        e.Consumer<OrderCreatedConsumer>(context);
                        e.Bind("order-created-events", s =>
                        {
                            s.ExchangeType = RabbitMQ.Client.ExchangeType.Fanout;
                        });
                    });

                    cfg.Message<ProductCreatedEvent>(e => e.SetEntityName("ProductCreatedEvent"));
                    cfg.Message<ProductUpdatedEvent>(e => e.SetEntityName("ProductUpdatedEvent"));
                    cfg.Message<ProductDeletedEvent>(e => e.SetEntityName("ProductDeletedEvent"));

                    cfg.Publish<ProductCreatedEvent>(p =>
                    {
                        p.ExchangeType = RabbitMQ.Client.ExchangeType.Fanout;
                        p.Durable = true;
                    });
                    cfg.Publish<ProductUpdatedEvent>(p =>
                    {
                        p.ExchangeType = RabbitMQ.Client.ExchangeType.Fanout;
                        p.Durable = true;
                    });
                    cfg.Publish<ProductDeletedEvent>(p =>
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