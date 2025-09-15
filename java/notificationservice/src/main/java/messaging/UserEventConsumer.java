package messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import events.UserCreatedEvent;
import grpc.NotificationSender;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class UserEventConsumer {

    private static final Logger LOG = Logger.getLogger(UserEventConsumer.class);

    @Inject
    NotificationSender notificationSender;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("user-created")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public Uni<Void> consumeUserCreatedEvent(Message<byte[]> message) {
        try {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            LOG.infof("Received raw user-created message: %s", payload);

            UserCreatedEvent event = extractEventFromMassTransitMessage(payload, UserCreatedEvent.class);
            if (event == null) {
                LOG.errorf("Failed to extract UserCreatedEvent from payload: %s", payload);
                return Uni.createFrom().completionStage(message.nack(new Exception("Invalid message format")));
            }

            LOG.infof("Parsed UserCreatedEvent: userId=%s, userName=%s, email=%s",
                    event.userId(), event.userName(), event.email());

            String body = createWelcomeEmailBody(event.userName(), event.email());

            notificationSender.sendEmail(event.email(), "Welcome to Orderly!", body);

            return Uni.createFrom().completionStage(message.ack());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process UserCreatedEvent");
            return Uni.createFrom().completionStage(message.ack());
        }
    }

    private String createWelcomeEmailBody(String userName, String userEmail) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to Orderly</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Helvetica Neue', Arial, sans-serif; background: linear-gradient(45deg, #f0f2f5, #e8ecef);">
                <div style="max-width: 600px; margin: 40px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 10px 40px rgba(0,0,0,0.1);">
                    <!-- Geometric header -->
                    <div style="position: relative; background: linear-gradient(135deg, #2c3e50, #3498db); padding: 50px 30px; text-align: center;">
                        <div style="position: absolute; top: -20px; right: -20px; width: 80px; height: 80px; background-color: rgba(255,255,255,0.1); border-radius: 50%%; border: 2px solid rgba(255,255,255,0.2);"></div>
                        <div style="position: absolute; bottom: -10px; left: -10px; width: 40px; height: 40px; background-color: rgba(255,255,255,0.1); transform: rotate(45deg);"></div>
                        
                        <img src="cid:logo@orderly.local" alt="Orderly Logo" style="max-height: 60px; margin-bottom: 20px;" />
                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 300; letter-spacing: 2px;">
                            ORDERLY
                        </h1>
                    </div>
                    
                    <!-- Content section -->
                    <div style="padding: 45px 35px;">
                        <div style="text-align: center; margin-bottom: 35px;">
                            <div style="display: inline-block; background-color: #3498db; width: 60px; height: 4px; border-radius: 2px; margin-bottom: 25px;"></div>
                            <h2 style="color: #2c3e50; margin: 0; font-size: 24px; font-weight: 600;">
                                Welcome Aboard, %s!
                            </h2>
                        </div>
                        
                        <div style="background: linear-gradient(135deg, #f8f9fa, #e9ecef); border-radius: 6px; padding: 25px; margin-bottom: 30px; position: relative;">
                            <div style="position: absolute; top: 10px; right: 15px; width: 20px; height: 20px; background-color: #3498db; border-radius: 50%%; opacity: 0.3;"></div>
                            <p style="color: #555555; line-height: 1.6; font-size: 16px; margin: 0;">
                                Your account has been successfully created with the email address:
                            </p>
                            <p style="color: #3498db; font-weight: 600; font-size: 16px; margin: 10px 0 0 0; font-family: 'Courier New', monospace;">
                                %s
                            </p>
                        </div>
                        
                        <p style="color: #666666; line-height: 1.7; font-size: 15px; margin-bottom: 35px;">
                            We're thrilled to have you join our community of organized individuals. 
                            Get ready to experience a new level of productivity and efficiency.
                        </p>
                        
                        <!-- Call to action -->
                        <div style="text-align: center; margin: 35px 0;">
                            <a href="#" style="display: inline-block; background: linear-gradient(135deg, #3498db, #2980b9); 
                               color: #ffffff; text-decoration: none; padding: 18px 35px; border-radius: 30px; 
                               font-weight: 600; font-size: 15px; box-shadow: 0 6px 20px rgba(52, 152, 219, 0.3); 
                               letter-spacing: 0.5px; text-transform: uppercase;">
                                Start Organizing →
                            </a>
                        </div>
                        
                        <div style="text-align: center; margin-top: 35px; padding-top: 25px; border-top: 1px solid #e9ecef;">
                            <p style="color: #7f8c8d; font-size: 14px; margin: 0;">
                                Questions? We're here to help at 
                                <a href="mailto:support@orderly.local" style="color: #3498db; text-decoration: none;">support@orderly.local</a>
                            </p>
                        </div>
                    </div>
                    
                    <!-- Footer -->
                    <div style="background-color: #34495e; padding: 30px; text-align: center; position: relative;">
                        <div style="position: absolute; top: 0; left: 50%%; transform: translateX(-50%%); width: 50px; height: 3px; background: linear-gradient(135deg, #3498db, #2980b9);"></div>
                        <p style="color: #ecf0f1; margin: 0 0 10px 0; font-size: 15px; font-weight: 500;">
                            Best regards,
                        </p>
                        <p style="color: #ffffff; margin: 0; font-size: 16px; font-weight: 600;">
                            The Orderly Team
                        </p>
                        <div style="margin-top: 20px;">
                            <p style="color: #95a5a6; font-size: 12px; margin: 0;">
                                © 2025 Orderly. Organizing your world, one step at a time.
                            </p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName, userEmail);
    }

    private <T> T extractEventFromMassTransitMessage(String payload, Class<T> eventClass) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);

            JsonNode messageNode = rootNode.get("message");

            if (messageNode != null) {
                LOG.debugf("Detected MassTransit format, extracting from 'message' field");
                return objectMapper.treeToValue(messageNode, eventClass);
            } else {
                LOG.debugf("Detected plain JSON format, parsing directly");
                return objectMapper.readValue(payload, eventClass);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse message payload: %s", payload);
            return null;
        }
    }
}