package gov.fdic.tip.governance.dto;
import gov.fdic.tip.governance.enums.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;
@Data
public class RetentionPolicyResponse {
    private UUID id;
    private String name;
    private String description;
    private String contentClassification;
    private ContentType contentType;
    private ClockStartTrigger clockStartTrigger;
    private DispositionAction dispositionAction;
    private Integer archivePeriodValue;
    private TimeUnit archivePeriodUnit;
    private Integer purgePeriodValue;
    private TimeUnit purgePeriodUnit;
    private PolicyStatus status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime activatedAt;
    private OffsetDateTime retiredAt;
}
