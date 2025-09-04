namespace Contracts.Events.User
{
    public record UserCreatedEvent(
        Guid UserId,
        string UserName,
        string Email,
        DateTime CreatedAt);
}
