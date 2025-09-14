package models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntityBase {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    public UUID id;

    @Column(nullable = false)
    public UUID userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 500)
    public String deliveryAddress;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    public PaymentType paymentType;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    public List<OrderItem> items = new ArrayList<>();
}