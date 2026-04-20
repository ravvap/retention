package gov.fdic.tip.governance.dto;
import gov.fdic.tip.governance.enums.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
@Data
public class AuditEventResponse {
    private Long seq;
    private AuditRecordType recordType;
    private UUID recordId;
    private AuditAction action;
    private String performedBy;
    private OffsetDateTime performedAt;
    private Map<String, Object> context;
}
