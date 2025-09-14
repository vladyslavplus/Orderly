namespace Contracts.Events.Order
{
    public class OrderDeletedEvent
    {
        public string Type { get; set; } = string.Empty;
        public Guid OrderId { get; set; }
        public string Status { get; set; } = string.Empty;
        public List<OrderItemDto> Items { get; set; } = new();
    }
}
