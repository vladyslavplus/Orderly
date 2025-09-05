namespace Contracts.Events.Product
{
    public record ProductDeletedEvent(
        Guid ProductId,
        string Name,
        string? Category,
        decimal Price,
        int Quantity,
        DateTime DeletedAt
    );
}
