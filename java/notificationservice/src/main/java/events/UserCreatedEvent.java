package events;

public record UserCreatedEvent(
        String userName,
        String email,
        String userId,
        String createdAt
) {}
