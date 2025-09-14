package dtos;

import java.util.List;

public class OrderDto {
    public String id;
    public String userId;
    public String deliveryAddress;
    public String paymentType;
    public String status;
    public String createdAt;
    public List<OrderItemDto> items;
}
