package dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChangeQuantityRequest {
    @NotNull
    @Min(value = -1000, message = "Delta must be non-zero and reasonable")
    private int delta;
}