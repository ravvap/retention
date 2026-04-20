package gov.fdic.tip.governance.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
@Data
public class RegisterDbRecordRequest {
    @NotBlank  private String dbTableName;
    @NotNull   private Map<String, Object> dbRecordKey;
    @NotNull   private UUID retentionPolicyId;
    @NotNull   private LocalDate retentionStartDate;
    @NotBlank  private String sourceSystem;
    private String businessContentClass;
}
