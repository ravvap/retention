package gov.fdic.tip.governance.dto;
import gov.fdic.tip.governance.enums.*;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class RetentionPolicyRequest {
    @NotBlank  private String name;
    private String description;
    private String contentClassification;
    @NotNull   private ContentType contentType;
    @NotNull   private ClockStartTrigger clockStartTrigger;
    @NotNull   private DispositionAction dispositionAction;
    @Positive  private Integer archivePeriodValue;
    private TimeUnit archivePeriodUnit;
    @Positive  private Integer purgePeriodValue;
    private TimeUnit purgePeriodUnit;
}
