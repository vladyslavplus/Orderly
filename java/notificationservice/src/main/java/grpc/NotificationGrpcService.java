package grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.vladyslavplus.notificationservice.NotificationRequest;
import org.vladyslavplus.notificationservice.NotificationResponse;
import org.vladyslavplus.notificationservice.NotificationService;
import org.vladyslavplus.notificationservice.NotificationType;

@GrpcService
public class NotificationGrpcService implements NotificationService {

    private static final Logger LOG = Logger.getLogger(NotificationGrpcService.class);

    @Inject
    NotificationSender notificationSender;

    @Override
    public Uni<NotificationResponse> sendNotification(NotificationRequest request) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            if (request.getType() == NotificationType.EMAIL) {
                notificationSender.sendEmail(
                        request.getRecipient(),
                        request.getSubject(),
                        request.getBody()
                );

                LOG.infof("Email send triggered to %s with subject: %s",
                        request.getRecipient(), request.getSubject());

                return NotificationResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("Email send triggered successfully")
                        .build();
            } else {
                String notImplemented = "Notification type " + request.getType().name() + " is not implemented yet.";
                LOG.warn(notImplemented);
                return NotificationResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage(notImplemented)
                        .build();
            }
        }));
    }
}