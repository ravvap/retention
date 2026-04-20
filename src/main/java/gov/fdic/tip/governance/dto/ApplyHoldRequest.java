package gov.fdic.tip.governance.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
import java.util.UUID;
@Data
public class ApplyHoldRequest {
    @NotEmpty private List<UUID> governedItemIds;
}
