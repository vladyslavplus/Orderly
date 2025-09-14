package grpc;

import com.google.protobuf.Empty;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import messaging.CartEventConsumer;
import messaging.OrderEventPublisher;
import models.OrderStatus;
import models.PaymentType;
import org.jboss.logging.Logger;
import org.vladyslavplus.orderservice.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@GrpcService
public class OrderGrpcService implements OrderService {

    private static final Logger LOG = Logger.getLogger(OrderGrpcService.class);

    @Inject
    CartEventConsumer cartEventConsumer;

    @Inject
    OrderEventPublisher orderEventPublisher;

    @Blocking
    @Override
    public Uni<Order> getOrderById(OrderIdRequest request) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            UUID orderId = UUID.fromString(request.getOrderId());
            models.Order order = models.Order.findById(orderId);
            if (order == null) {
                throw new RuntimeException("Order not found: " + orderId);
            }
            return toProtoOrder(order);
        }));
    }

    @Blocking
    @Override
    public Uni<OrdersResponse> getAllOrders(Empty request) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            List<models.Order> orders = models.Order.listAll();

            OrdersResponse.Builder responseBuilder = OrdersResponse.newBuilder();
            for (models.Order order : orders) {
                responseBuilder.addOrders(toProtoOrder(order));
            }

            return responseBuilder.build();
        }));
    }

    @Blocking
    @Override
    @Transactional
    public Uni<Order> createOrder(CreateOrderRequest request) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            UUID userId = UUID.fromString(request.getUserId());

            List<CartEventConsumer.CartItemDto> cartItems = cartEventConsumer.getCartItems(userId);
            if (cartItems.isEmpty()) {
                throw new IllegalStateException("Cart is empty or does not exist for user: " + userId);
            }

            List<CartEventConsumer.CartItemDto> orderSnapshot = new ArrayList<>();
            for (CartEventConsumer.CartItemDto item : cartItems) {
                orderSnapshot.add(new CartEventConsumer.CartItemDto(item.productId, item.quantity));
            }

            models.Order order = new models.Order();
            order.userId = userId;
            order.status = OrderStatus.PENDING;
            order.deliveryAddress = request.getDeliveryAddress();
            try {
                order.paymentType = PaymentType.valueOf(request.getPaymentType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid payment type: " + request.getPaymentType() +
                        ". Allowed values: " + Arrays.toString(PaymentType.values()));
            }
            order.persist();

            for (CartEventConsumer.CartItemDto cartItem : orderSnapshot) {
                models.OrderItem orderItem = new models.OrderItem();
                orderItem.order = order;
                orderItem.productId = cartItem.productId;
                orderItem.quantity = cartItem.quantity;
                orderItem.persist();
                order.items.add(orderItem);
            }

            orderEventPublisher.publishOrderCreated(order);

            LOG.infof("Order created: %s for user: %s with %d items",
                    order.id, userId, orderSnapshot.size());

            return toProtoOrder(order);
        }));
    }

    @Blocking
    @Override
    @Transactional
    public Uni<Order> updateOrderStatus(UpdateOrderStatusRequest request) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            UUID orderId = UUID.fromString(request.getOrderId());
            models.Order order = models.Order.findById(orderId);
            if (order == null) {
                throw new RuntimeException("Order not found: " + orderId);
            }

            OrderStatus newStatus;
            try {
                newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid order status: " + request.getStatus() +
                        ". Allowed values: " + Arrays.toString(OrderStatus.values()));
            }

            order.status = newStatus;
            order.persist();

            orderEventPublisher.publishOrderUpdated(order.id, order.status.name());

            LOG.infof("Order %s updated to status %s", order.id, order.status);

            return toProtoOrder(order);
        }));
    }

    @Blocking
    @Override
    @Transactional
    public Uni<Empty> deleteOrder(OrderIdRequest request) {
        return Uni.createFrom().item(Unchecked.supplier(() -> {
            UUID orderId = UUID.fromString(request.getOrderId());
            models.Order order = models.Order.findById(orderId);
            if (order == null) {
                throw new RuntimeException("Order not found: " + orderId);
            }

            String status = order.status != null ? order.status.name() : "UNKNOWN";

            order.delete();

            orderEventPublisher.publishOrderDeleted(order);

            LOG.infof("Order %s deleted with status %s", orderId, status);

            return Empty.getDefaultInstance();
        }));
    }

    private Order toProtoOrder(models.Order order) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        Order.Builder builder = Order.newBuilder()
                .setId(order.id.toString())
                .setUserId(order.userId.toString())
                .setStatus(order.status.name())
                .setCreatedAtString(order.createdAt.format(formatter))
                .setDeliveryAddress(order.deliveryAddress != null ? order.deliveryAddress : "")
                .setPaymentType(order.paymentType != null ? order.paymentType.name() : "");

        List<OrderItem> items = order.items.stream()
                .map(item -> OrderItem.newBuilder()
                        .setProductId(item.productId.toString())
                        .setQuantity(item.quantity)
                        .build())
                .collect(Collectors.toList());

        builder.addAllItems(items);
        return builder.build();
    }
}