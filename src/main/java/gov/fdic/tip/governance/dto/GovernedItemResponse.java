package gov.fdic.tip.governance.dto;
import gov.fdic.tip.governance.enums.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
@Data
public class GovernedItemResponse {
    private UUID id;
    private ItemType itemType;
    private String documentReference;
    private String dbTableName;
    private Map<String, Object> dbRecordKey;
    private String sourceSystem;
    private String businessContentClass;
    private UUID retentionPolicyId;
    private String retentionPolicyName;
    private LocalDate retentionStartDate;
    private LocalDate archiveEligibilityDate;
    private LocalDate purgeEligibilityDate;
    private GovernanceStatus governanceStatus;
    private int activeHoldCount;
    private String registeredBy;
    private OffsetDateTime registeredAt;
    private DispositionRecommendation dispositionRecommendation;
}
