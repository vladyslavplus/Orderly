package repositories;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import models.Cart;

import java.util.UUID;

@ApplicationScoped
public class CartRepository implements PanacheRepository<Cart> {
    public Cart findByUserId(UUID userId) {
        return find("userId", userId).firstResult();
    }
}