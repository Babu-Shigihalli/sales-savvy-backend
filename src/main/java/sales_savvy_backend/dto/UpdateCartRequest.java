package sales_savvy_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCartRequest {
    private Integer quantity;
}