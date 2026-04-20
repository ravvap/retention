package gov.fdic.tip.governance.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;
@Data
public class RegisterDocumentRequest {
    @NotBlank  private String documentReference;
    @NotNull   private UUID retentionPolicyId;
    @NotNull   private LocalDate retentionStartDate;
    @NotBlank  private String sourceSystem;
    private String businessContentClass;
}
