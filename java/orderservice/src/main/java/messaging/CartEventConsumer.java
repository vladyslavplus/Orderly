package messaging;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CartEventConsumer {

    private static final Logger LOG = Logger.getLogger(CartEventConsumer.class);

    private final Map<UUID, List<CartItemDto>> cartCache = new ConcurrentHashMap<>();

    @Incoming("cart-events")
    public void receive(String eventMessage) {
        LOG.infof("Received cart event: %s", eventMessage);

        try {
            if (eventMessage.startsWith("CartItemAdded:")) {
                String[] parts = eventMessage.split(":");
                UUID userId = UUID.fromString(parts[1]);
                UUID productId = UUID.fromString(parts[2]);
                int quantity = Integer.parseInt(parts[3]);

                cartCache.computeIfAbsent(userId, k -> new ArrayList<>());
                List<CartItemDto> items = cartCache.get(userId);

                Optional<CartItemDto> existing = items.stream()
                        .filter(i -> i.productId.equals(productId))
                        .findFirst();

                if (existing.isPresent()) {
                    existing.get().quantity += quantity;
                } else {
                    items.add(new CartItemDto(productId, quantity));
                }

            } else if (eventMessage.startsWith("CartItemRemoved:")) {
                String[] parts = eventMessage.split(":");
                UUID userId = UUID.fromString(parts[1]);
                UUID productId = UUID.fromString(parts[2]);

                List<CartItemDto> items = cartCache.get(userId);
                if (items != null) {
                    items.removeIf(i -> i.productId.equals(productId));
                }

            } else if (eventMessage.startsWith("CartCleared:")) {
                UUID userId = UUID.fromString(eventMessage.split(":")[1]);
                cartCache.remove(userId);

            } else if (eventMessage.startsWith("CartItemQuantityChanged:")) {
                String[] parts = eventMessage.split(":");
                UUID userId = UUID.fromString(parts[1]);
                UUID productId = UUID.fromString(parts[2]);
                int newQuantity = Integer.parseInt(parts[3]);

                List<CartItemDto> items = cartCache.get(userId);
                if (items != null) {
                    Optional<CartItemDto> existing = items.stream()
                            .filter(i -> i.productId.equals(productId))
                            .findFirst();
                    existing.ifPresent(i -> i.quantity = newQuantity);
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to process cart event: %s", eventMessage);
        }
    }

    public List<CartItemDto> getCartItems(UUID userId) {
        return cartCache.getOrDefault(userId, Collections.emptyList());
    }

    public static class CartItemDto {
        public UUID productId;
        public int quantity;

        public CartItemDto(UUID productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
    }
}