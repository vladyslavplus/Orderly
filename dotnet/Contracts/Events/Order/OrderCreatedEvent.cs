namespace Contracts.Events.Order
{
    public class OrderCreatedEvent
    {
        public string Type { get; set; } = string.Empty;
        public Guid OrderId { get; set; }
        public Guid UserId { get; set; }
        public List<OrderItemDto> Items { get; set; } = new();
    }
}
