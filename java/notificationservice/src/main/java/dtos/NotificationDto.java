package dtos;

import models.NotificationType;

import java.util.UUID;

public class NotificationDto {
    public UUID userId;
    public NotificationType type;
    public String recipient;
    public String subject;
    public String body;
}
