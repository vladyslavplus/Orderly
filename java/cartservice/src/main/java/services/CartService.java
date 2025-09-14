package services;

import dtos.AddItemRequest;
import dtos.CartResponse;
import dtos.CartItemResponse;
import dtos.ChangeQuantityRequest;
import exceptions.CartItemNotFoundException;
import exceptions.ProductNotAvailableException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import jakarta.validation.Valid;
import messaging.ProductEventConsumer;
import models.Cart;
import models.CartItem;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import repositories.CartRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class CartService {

    @Inject
    CartRepository cartRepository;

    @Inject
    ProductEventConsumer productConsumer;

    @Inject
    @Channel("cart-events")
    Emitter<String> cartEventEmitter;

    @Transactional(TxType.SUPPORTS)
    public CartResponse getCart(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            cart = createCart(userId);
        }
        return convertToDto(cart);
    }

    @Transactional
    protected Cart createCart(UUID userId) {
        Cart cart = new Cart();
        cart.userId = userId;
        cart.persist();
        return cart;
    }

    @Transactional
    public void addItem(UUID userId, @Valid AddItemRequest request) {
        Cart cart = getCartEntity(userId);

        CartItem existingItem = cart.items.stream()
                .filter(i -> i.productId.equals(request.getProductId()))
                .findFirst()
                .orElse(null);

        int alreadyInCart = existingItem != null ? existingItem.quantity : 0;
        int requestedNow = request.getQuantity();

        Integer availableStock = productConsumer.getProductQuantity(request.getProductId().toString());
        if (availableStock == null) {
            throw new ProductNotAvailableException("Product not found or unavailable");
        }

        if (alreadyInCart + requestedNow > availableStock) {
            throw new ProductNotAvailableException(
                    "Cannot add product to cart. Already in cart: " + alreadyInCart +
                            ", requested now: " + requestedNow +
                            ", available product in stock: " + availableStock
            );
        }

        if (existingItem != null) {
            existingItem.quantity = alreadyInCart + requestedNow;
            existingItem.persist();
        } else {
            CartItem newItem = new CartItem();
            newItem.cart = cart;
            newItem.productId = request.getProductId();
            newItem.quantity = requestedNow;
            newItem.persist();
            cart.items.add(newItem);
        }

        cart.persist();
        cartEventEmitter.send(
                "CartItemAdded:" + cart.userId + ":" + request.getProductId() + ":" + request.getQuantity()
        );
    }

    @Transactional
    public void removeItem(UUID userId, UUID productId) {
        Cart cart = getCartEntity(userId);

        CartItem item = cart.items.stream()
                .filter(i -> i.productId.equals(productId))
                .findFirst()
                .orElseThrow(() ->
                        new CartItemNotFoundException("Cart item not found: " + productId)
                );

        item.delete();
        cart.items.remove(item);
        cart.persist();
        cartEventEmitter.send("CartItemRemoved:" + cart.userId + ":" + productId);
    }

    @Transactional
    public void clearCart(UUID userId) {
        Cart cart = getCartEntity(userId);

        List<CartItem> items = List.copyOf(cart.items);
        for (CartItem item : items) {
            item.delete();
        }
        cart.items.clear();
        cart.persist();
        cartEventEmitter.send("CartCleared:" + userId);
    }

    @Transactional
    public void changeItemQuantity(UUID userId, UUID productId, @Valid ChangeQuantityRequest request) {
        int delta = request.getDelta();
        if (delta == 0) return;

        Cart cart = getCartEntity(userId);

        CartItem item = cart.items.stream()
                .filter(i -> i.productId.equals(productId))
                .findFirst()
                .orElse(null);

        if (item == null) return;

        int newQuantity = item.quantity + delta;

        if (newQuantity <= 0) {
            removeItem(userId, productId);
            return;
        }

        Integer availableStock = productConsumer.getProductQuantity(productId.toString());
        if (availableStock == null) {
            throw new ProductNotAvailableException("Product not found or unavailable");
        }

        if (newQuantity > availableStock) {
            throw new ProductNotAvailableException(
                    "Cannot change quantity. Requested: " + newQuantity +
                            ", available in stock: " + availableStock
            );
        }

        item.quantity = newQuantity;
        item.persist();
        cart.persist();

        cartEventEmitter.send("CartItemQuantityChanged:" + cart.userId + ":" + productId + ":" + newQuantity);
    }

    private Cart getCartEntity(UUID userId) {
        Cart cart = cartRepository.findByUserId(userId);
        if (cart == null) {
            cart = createCart(userId);
        }
        return cart;
    }

    private CartResponse convertToDto(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.id);
        response.setUserId(cart.userId);
        response.setCreatedAt(cart.createdAt);

        List<CartItemResponse> itemResponses = cart.items.stream()
                .map(this::convertItemToDto)
                .collect(Collectors.toList());
        response.setItems(itemResponses);

        return response;
    }

    private CartItemResponse convertItemToDto(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setCartItemId(item.id);
        response.setProductId(item.productId);
        response.setQuantity(item.quantity);
        return response;
    }
}