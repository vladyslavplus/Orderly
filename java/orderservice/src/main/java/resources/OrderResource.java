package resources;

import dtos.*;
import io.quarkus.grpc.GrpcClient;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.vladyslavplus.orderservice.*;
import security.JwtUtils;

import java.util.List;
import java.util.stream.Collectors;

@Path("api/orders")
@RolesAllowed({"Admin", "User"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SecurityRequirement(name = "bearerAuth")
public class OrderResource {

    @GrpcClient("order")
    OrderService orderService;

    @Inject
    JwtUtils jwtUtils;

    @GET
    @Path("/{id}")
    public Uni<OrderDto> getOrderById(@PathParam("id") String id) {
        OrderIdRequest request = OrderIdRequest.newBuilder()
                .setOrderId(id)
                .build();
        return orderService.getOrderById(request)
                .map(this::convertToDto);
    }

    @GET
    public Uni<List<OrderDto>> getAllOrders() {
        return orderService.getAllOrders(com.google.protobuf.Empty.getDefaultInstance())
                .map(response -> response.getOrdersList().stream()
                        .map(this::convertToDto)
                        .collect(Collectors.toList()));
    }

    @POST
    public Uni<OrderDto> createOrder(CreateOrderDto dto) {
        String userId = jwtUtils.getUserId().toString();

        CreateOrderRequest request = CreateOrderRequest.newBuilder()
                .setUserId(userId)
                .setDeliveryAddress(dto.deliveryAddress != null ? dto.deliveryAddress : "")
                .setPaymentType(dto.paymentType != null ? dto.paymentType : "")
                .build();

        return orderService.createOrder(request)
                .map(this::convertToDto);
    }

    @PUT
    @Path("/{id}/status")
    public Uni<OrderDto> updateStatus(@PathParam("id") String id, UpdateStatusDto dto) {
        UpdateOrderStatusRequest req = UpdateOrderStatusRequest.newBuilder()
                .setOrderId(id)
                .setStatus(dto.status)
                .build();
        return orderService.updateOrderStatus(req)
                .map(this::convertToDto);
    }

    @DELETE
    @Path("/{id}")
    public Uni<DeleteResponse> deleteOrder(@PathParam("id") String id) {
        OrderIdRequest request = OrderIdRequest.newBuilder()
                .setOrderId(id)
                .build();
        return orderService.deleteOrder(request)
                .map(empty -> new DeleteResponse("Order deleted successfully"));
    }

    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.id = order.getId();
        dto.userId = order.getUserId();
        dto.deliveryAddress = order.getDeliveryAddress();
        dto.paymentType = order.getPaymentType();
        dto.status = order.getStatus();
        dto.createdAt = order.getCreatedAtString();

        dto.items = order.getItemsList().stream()
                .map(item -> {
                    var itemDto = new OrderItemDto();
                    itemDto.productId = item.getProductId();
                    itemDto.quantity = item.getQuantity();
                    return itemDto;
                })
                .collect(Collectors.toList());

        return dto;
    }
}
