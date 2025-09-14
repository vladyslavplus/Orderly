namespace Contracts.Events.Order
{
    public record OrderUpdatedEvent(
        Guid OrderId,
        string Status
    );
}
