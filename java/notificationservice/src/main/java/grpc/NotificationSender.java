package grpc;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.reactive.ReactiveMailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationSender {

    private static final Logger LOG = Logger.getLogger(NotificationSender.class);

    @Inject
    ReactiveMailer reactiveMailer;

    public void sendEmail(String to, String subject, String body) {
        reactiveMailer.send(Mail.withHtml(to, subject, body))
                .subscribe().with(
                        success -> LOG.infof("Email sent to %s", to),
                        failure -> LOG.errorf(failure, "Failed to send email to %s", to)
                );
    }
}