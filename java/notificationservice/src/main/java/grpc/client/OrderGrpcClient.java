package grpc.client;

import io.quarkus.grpc.GrpcClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.vladyslavplus.orderservice.OrderService;

@ApplicationScoped
public class OrderGrpcClient {

    @GrpcClient("order")
    OrderService orderService;

    public OrderService getClient() {
        return orderService;
    }
}