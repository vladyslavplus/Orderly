namespace Contracts.Events.User
{
    public record UserUpdatedEvent(
        Guid UserId,
        string? UserName,
        string? Email,
        string? PhoneNumber,
        DateTime UpdatedAt);
}
