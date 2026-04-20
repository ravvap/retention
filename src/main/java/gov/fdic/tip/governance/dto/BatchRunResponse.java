package gov.fdic.tip.governance.dto;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;
@Data
public class BatchRunResponse {
    private UUID id;
    private OffsetDateTime runAt;
    private int itemsEvaluated;
    private int itemsArchived;
    private int itemsPurged;
    private int itemsSkipped;
    private int itemsError;
    private OffsetDateTime completedAt;
    private String summary;
}
