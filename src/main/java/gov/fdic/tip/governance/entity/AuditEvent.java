package gov.fdic.tip.governance.entity;

import gov.fdic.tip.governance.enums.AuditAction;
import gov.fdic.tip.governance.enums.AuditRecordType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 30)
    private AuditRecordType recordType;

    @Column(name = "record_id", nullable = false)
    private java.util.UUID recordId;

    @Column(name = "performed_by", nullable = false, length = 255)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    @Builder.Default
    private OffsetDateTime performedAt = OffsetDateTime.now();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> context;
}
