package gov.fdic.tip.governance.repository;

import gov.fdic.tip.governance.entity.AuditEvent;
import gov.fdic.tip.governance.enums.AuditRecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByRecordTypeAndRecordIdOrderByPerformedAtAscIdAsc(
            AuditRecordType recordType, UUID recordId);
}
