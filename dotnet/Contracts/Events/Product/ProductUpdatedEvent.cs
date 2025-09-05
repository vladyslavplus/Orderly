namespace Contracts.Events.Product
{
    public record ProductUpdatedEvent(
        Guid ProductId,
        string Name,
        string? Description,
        decimal Price,
        int Quantity,
        string? Category,
        double Rating,
        DateTime UpdatedAt
    );
}
