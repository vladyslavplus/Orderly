package models;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    public UUID id;

    @Column(nullable = false)
    public UUID userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public NotificationType type;

    @Column(nullable = false)
    public String recipient;

    @Column(nullable = false)
    public String subject;

    @Column(columnDefinition = "TEXT")
    public String body;

    @Enumerated(EnumType.STRING)
    public NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime sentAt;
}
