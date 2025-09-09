package messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import events.ProductCreatedEvent;
import events.ProductUpdatedEvent;
import events.ProductDeletedEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ProductEventConsumer {

    private static final Logger LOG = Logger.getLogger(ProductEventConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Integer> productCache = new ConcurrentHashMap<>();

    @Incoming("product-created")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> onProductCreated(Message<byte[]> message) {
        try {
            byte[] payloadBytes = message.getPayload();
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            LOG.infof("Received raw product-created message: %s", payload);

            ProductCreatedEvent event = extractEventFromMassTransitMessage(payload, ProductCreatedEvent.class);
            if (event == null) {
                LOG.errorf("Failed to extract ProductCreatedEvent from payload: %s", payload);
                return message.nack(new Exception("Invalid message format"));
            }

            LOG.infof("Parsed ProductCreatedEvent: productId=%s, quantity=%d",
                    event.productId(), event.quantity());

            productCache.put(event.productId().toString(), event.quantity());
            LOG.infof("Product cache updated. Current size: %d", productCache.size());

            return message.ack();
        } catch (Exception e) {
            String payloadStr;
            try {
                payloadStr = new String(message.getPayload(), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                payloadStr = "Unable to decode payload";
            }
            LOG.errorf(e, "Failed to process ProductCreatedEvent: %s", payloadStr);
            return message.nack(e);
        }
    }

    @Incoming("product-updated")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> onProductUpdated(Message<byte[]> message) {
        try {
            byte[] payloadBytes = message.getPayload();
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            LOG.infof("Received raw product-updated message: %s", payload);

            ProductUpdatedEvent event = extractEventFromMassTransitMessage(payload, ProductUpdatedEvent.class);
            if (event == null) {
                LOG.errorf("Failed to extract ProductUpdatedEvent from payload: %s", payload);
                return message.nack(new Exception("Invalid message format"));
            }

            LOG.infof("Parsed ProductUpdatedEvent: productId=%s, quantity=%d",
                    event.productId(), event.quantity());

            productCache.put(event.productId().toString(), event.quantity());
            LOG.infof("Product cache updated. Current size: %d", productCache.size());

            return message.ack();
        } catch (Exception e) {
            String payloadStr;
            try {
                payloadStr = new String(message.getPayload(), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                payloadStr = "Unable to decode payload";
            }
            LOG.errorf(e, "Failed to process ProductUpdatedEvent: %s", payloadStr);
            return message.nack(e);
        }
    }

    @Incoming("product-deleted")
    @Acknowledgment(Acknowledgment.Strategy.MANUAL)
    public CompletionStage<Void> onProductDeleted(Message<byte[]> message) {
        try {
            byte[] payloadBytes = message.getPayload();
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            LOG.infof("Received raw product-deleted message: %s", payload);

            ProductDeletedEvent event = extractEventFromMassTransitMessage(payload, ProductDeletedEvent.class);
            if (event == null) {
                LOG.errorf("Failed to extract ProductDeletedEvent from payload: %s", payload);
                return message.nack(new Exception("Invalid message format"));
            }

            LOG.infof("Parsed ProductDeletedEvent: productId=%s", event.productId());

            if (event.productId() == null) {
                LOG.errorf("ProductId is null in event: %s", payload);
                return message.nack(new Exception("ProductId cannot be null"));
            }

            productCache.remove(event.productId().toString());
            LOG.infof("Product removed from cache. Current size: %d", productCache.size());

            return message.ack();
        } catch (Exception e) {
            String payloadStr;
            try {
                payloadStr = new String(message.getPayload(), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                payloadStr = "Unable to decode payload";
            }
            LOG.errorf(e, "Failed to process ProductDeletedEvent: %s", payloadStr);
            return message.nack(e);
        }
    }

    private <T> T extractEventFromMassTransitMessage(String payload, Class<T> eventClass) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);

            JsonNode messageNode = rootNode.get("message");
            if (messageNode != null) {
                LOG.debugf("Detected MassTransit format, extracting from 'message' field");
                LOG.debugf("Raw message node: %s", messageNode.toString());
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

    public boolean isProductAvailable(String productId, int requestedQuantity) {
        Integer availableQuantity = productCache.get(productId);
        boolean available = availableQuantity != null && availableQuantity >= requestedQuantity;

        LOG.debugf("Product availability check: productId=%s, requested=%d, available=%s, inStock=%s",
                productId, requestedQuantity, availableQuantity, available);

        return available;
    }

    public int getCachedProductCount() {
        return productCache.size();
    }

    public Integer getProductQuantity(String productId) {
        return productCache.get(productId);
    }

    public void logCacheContents() {
        LOG.infof("Current product cache contents: %s", productCache);
    }
}