package dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public class CartResponse {
    private UUID id;
    private UUID userId;
    private LocalDateTime createdAt;
    private List<CartItemResponse> items;
}
