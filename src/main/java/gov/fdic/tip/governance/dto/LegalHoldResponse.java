package gov.fdic.tip.governance.dto;
import gov.fdic.tip.governance.enums.HoldStatus;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;
@Data
public class LegalHoldResponse {
    private UUID id;
    private String name;
    private String matterReference;
    private String reason;
    private HoldStatus status;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String releasedBy;
    private OffsetDateTime releasedAt;
    private long activeItemCount;
}
