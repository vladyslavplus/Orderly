package dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class AddItemRequest {
    private UUID productId;
    private int quantity;
}