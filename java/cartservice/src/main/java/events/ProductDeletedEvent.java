package events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductDeletedEvent(
        @JsonProperty("productId") UUID productId,
        @JsonProperty("name") String name,
        @JsonProperty("category") String category,
        @JsonProperty("price") BigDecimal price,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("deletedAt") Instant deletedAt
) {}