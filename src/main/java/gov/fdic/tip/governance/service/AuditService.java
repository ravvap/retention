package gov.fdic.tip.governance.service;

import gov.fdic.tip.governance.entity.AuditEvent;
import gov.fdic.tip.governance.enums.AuditAction;
import gov.fdic.tip.governance.enums.AuditRecordType;
import gov.fdic.tip.governance.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public void record(AuditAction action, AuditRecordType recordType,
                       UUID recordId, String performedBy) {
        record(action, recordType, recordId, performedBy, null);
    }

    public void record(AuditAction action, AuditRecordType recordType,
                       UUID recordId, String performedBy, Map<String, Object> context) {
        AuditEvent event = AuditEvent.builder()
                .action(action)
                .recordType(recordType)
                .recordId(recordId)
                .performedBy(performedBy)
                .performedAt(OffsetDateTime.now())
                .context(context)
                .build();
        auditEventRepository.save(event);
    }

    public List<AuditEvent> getHistory(AuditRecordType recordType, UUID recordId) {
        return auditEventRepository
                .findByRecordTypeAndRecordIdOrderByPerformedAtAscIdAsc(recordType, recordId);
    }
}
