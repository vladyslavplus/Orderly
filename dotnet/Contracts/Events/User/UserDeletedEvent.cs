namespace Contracts.Events.User
{
    public record UserDeletedEvent(
        Guid UserId,
        string Email,
        DateTime DeletedAt);
}
