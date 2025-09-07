package dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class CartItemResponse {
    private UUID cartItemId;
    private UUID productId;
    private int quantity;
}