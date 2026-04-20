package gov.fdic.tip.governance.entity;

import gov.fdic.tip.governance.enums.GovernanceStatus;
import gov.fdic.tip.governance.enums.ItemType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "governed_item")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GovernedItem {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 30)
    private ItemType itemType;

    @Column(name = "document_reference", length = 500)
    private String documentReference;

    @Column(name = "db_table_name", length = 255)
    private String dbTableName;

    @Type(JsonBinaryType.class)
    @Column(name = "db_record_key", columnDefinition = "jsonb")
    private Map<String, Object> dbRecordKey;

    @Column(name = "source_system", nullable = false, length = 255)
    private String sourceSystem;

    @Column(name = "business_content_class", length = 255)
    private String businessContentClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retention_policy_id", nullable = false)
    private RetentionPolicy retentionPolicy;

    @Column(name = "retention_start_date", nullable = false)
    private LocalDate retentionStartDate;

    @Column(name = "archive_eligibility_date")
    private LocalDate archiveEligibilityDate;

    @Column(name = "purge_eligibility_date")
    private LocalDate purgeEligibilityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "governance_status", nullable = false, length = 30)
    @Builder.Default
    private GovernanceStatus governanceStatus = GovernanceStatus.Active;

    @Column(name = "active_hold_count", nullable = false)
    @Builder.Default
    private int activeHoldCount = 0;

    @Column(name = "registered_by", nullable = false, length = 255)
    private String registeredBy;

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;
}
