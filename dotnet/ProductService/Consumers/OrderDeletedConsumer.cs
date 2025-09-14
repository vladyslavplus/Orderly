using Contracts.Events.Order;
using MassTransit;
using Microsoft.EntityFrameworkCore;
using ProductService.Data;

namespace ProductService.Consumers
{
    public class OrderDeletedConsumer : IConsumer<OrderDeletedEvent>
    {
        private readonly ApplicationDbContext _db;
        private readonly ILogger<OrderDeletedConsumer> _logger;

        public OrderDeletedConsumer(ApplicationDbContext db, ILogger<OrderDeletedConsumer> logger)
        {
            _db = db;
            _logger = logger;
        }

        public async Task Consume(ConsumeContext<OrderDeletedEvent> context)
        {
            var message = context.Message;
            _logger.LogInformation("Received OrderDeletedEvent: {OrderId}, Status: {Status}", message.OrderId, message.Status);

            if (!string.Equals(message.Status, "CANCELLED", StringComparison.OrdinalIgnoreCase))
            {
                _logger.LogInformation("Order {OrderId} is not CANCELLED. Skipping quantity restoration.", message.OrderId);
                return;
            }

            foreach (var item in message.Items)
            {
                var product = await _db.Products.FirstOrDefaultAsync(p => p.Id == item.ProductId);
                if (product != null)
                {
                    var oldQty = product.Quantity;
                    product.Quantity += item.Quantity;
                    _logger.LogInformation(
                        "Restored product {ProductId} quantity from {OldQty} to {NewQty}",
                        product.Id, oldQty, product.Quantity
                    );
                }
                else
                {
                    _logger.LogWarning("Product with ID {ProductId} not found", item.ProductId);
                }
            }

            await _db.SaveChangesAsync();
            _logger.LogInformation("Successfully processed OrderDeletedEvent for Order: {OrderId}", message.OrderId);
        }
    }
}