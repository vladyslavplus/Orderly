namespace Contracts.Events.Product
{
    public record ProductCreatedEvent(
        Guid ProductId,
        string Name,
        string? Description,
        decimal Price,
        int Quantity,
        string? Category,
        double Rating,
        DateTime CreatedAt
    );
}
