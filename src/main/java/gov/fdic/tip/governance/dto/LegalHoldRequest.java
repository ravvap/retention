package gov.fdic.tip.governance.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class LegalHoldRequest {
    @NotBlank private String name;
    private String matterReference;
    private String reason;
}
