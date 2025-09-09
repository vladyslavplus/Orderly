package events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductUpdatedEvent(
        @JsonProperty("productId") UUID productId,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("price") BigDecimal price,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("category") String category,
        @JsonProperty("rating") double rating,
        @JsonProperty("updatedAt") Instant updatedAt
) {}