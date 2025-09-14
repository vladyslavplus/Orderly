namespace Contracts.Events.Order
{
    public record OrderItemDto(
        Guid ProductId,
        int Quantity
    );
}
