using Contracts.Events.Order;
using MassTransit;
using Microsoft.EntityFrameworkCore;
using ProductService.Data;

namespace ProductService.Consumers
{
    public class OrderCreatedConsumer : IConsumer<OrderCreatedEvent>
    {
        private readonly ApplicationDbContext _db;
        private readonly ILogger<OrderCreatedConsumer> _logger;

        public OrderCreatedConsumer(ApplicationDbContext db, ILogger<OrderCreatedConsumer> logger)
        {
            _db = db;
            _logger = logger;
        }

        public async Task Consume(ConsumeContext<OrderCreatedEvent> context)
        {
            var message = context.Message;
            _logger.LogInformation("Received OrderCreatedEvent: {OrderId}", message.OrderId);

            foreach (var item in message.Items)
            {
                var product = await _db.Products.FirstOrDefaultAsync(p => p.Id == item.ProductId);
                if (product != null)
                {
                    var oldQty = product.Quantity;
                    product.Quantity = Math.Max(0, product.Quantity - item.Quantity);
                    _logger.LogInformation(
                        "Updated product {ProductId} quantity from {OldQty} to {NewQty}",
                        product.Id, oldQty, product.Quantity
                    );
                }
                else
                {
                    _logger.LogWarning("Product with ID {ProductId} not found", item.ProductId);
                }
            }

            await _db.SaveChangesAsync();
            _logger.LogInformation("Successfully processed OrderCreatedEvent for Order: {OrderId}", message.OrderId);
        }
    }
}
