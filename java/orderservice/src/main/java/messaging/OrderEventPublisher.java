package messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class OrderEventPublisher {

    @Inject
    @Channel("order-created-events")
    Emitter<String> orderCreatedEmitter;

    @Inject
    @Channel("order-updated-events")
    Emitter<String> orderUpdatedEmitter;

    @Inject
    @Channel("order-deleted-events")
    Emitter<String> orderDeletedEmitter;

    @Inject
    ObjectMapper mapper;

    public enum EventType {
        OrderCreated, OrderUpdated, OrderDeleted
    }

    public void publishOrderCreated(models.Order order) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", EventType.OrderCreated.name());
        event.put("orderId", order.id.toString());
        event.put("userId", order.userId.toString());
        event.put("items", buildItems(order));

        sendEvent(orderCreatedEmitter, event);
    }

    public void publishOrderUpdated(UUID orderId, String status) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", EventType.OrderUpdated.name());
        event.put("orderId", orderId.toString());
        event.put("status", status);

        sendEvent(orderUpdatedEmitter, event);
    }

    public void publishOrderDeleted(models.Order order) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", EventType.OrderDeleted.name());
        event.put("orderId", order.id.toString());
        event.put("status", order.status != null ? order.status.name() : "UNKNOWN");
        event.put("items", buildItems(order));

        sendEvent(orderDeletedEmitter, event);
    }

    private List<Map<String, Object>> buildItems(models.Order order) {
        return order.items.stream()
                .map(item -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("productId", item.productId.toString());
                    map.put("quantity", item.quantity);
                    return map;
                })
                .toList();
    }

    private void sendEvent(Emitter<String> emitter, Map<String, Object> event) {
        try {
            String eventMessage = mapper.writeValueAsString(event);
            emitter.send(eventMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order event: " + event, e);
        }
    }
}
