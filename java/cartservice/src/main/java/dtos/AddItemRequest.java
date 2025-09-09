package dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class AddItemRequest {
    @NotNull(message = "ProductId cannot be null")
    private UUID productId;
    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}