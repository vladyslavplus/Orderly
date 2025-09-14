package messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import services.CartService;

import java.util.UUID;

@ApplicationScoped
public class OrderEventConsumer {

    private static final Logger LOG = Logger.getLogger(OrderEventConsumer.class);

    @Inject
    CartService cartService;

    @Inject
    ObjectMapper mapper;

    @Incoming("order-created-events")
    public void receiveOrderCreated(String eventMessage) {
        LOG.infof("Received order created event: %s", eventMessage);

        try {
            JsonNode event = mapper.readTree(eventMessage);
            UUID orderId = UUID.fromString(event.get("orderId").asText());
            UUID userId = UUID.fromString(event.get("userId").asText());
            int itemCount = event.get("items").size();

            LOG.infof("Processing order creation: orderId=%s, userId=%s, items=%d",
                    orderId, userId, itemCount);

            cartService.clearCart(userId);

            LOG.infof("Cart cleared for user %s after order %s creation", userId, orderId);

        } catch (Exception e) {
            LOG.errorf(e, "Failed to process order created event: %s", eventMessage);
        }
    }
}