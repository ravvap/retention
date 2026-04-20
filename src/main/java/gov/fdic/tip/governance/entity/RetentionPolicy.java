package gov.fdic.tip.governance.entity;

import gov.fdic.tip.governance.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "retention_policy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RetentionPolicy {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "content_classification", length = 255)
    private String contentClassification;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 50)
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "clock_start_trigger", nullable = false, length = 50)
    private ClockStartTrigger clockStartTrigger;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposition_action", nullable = false, length = 50)
    private DispositionAction dispositionAction;

    @Column(name = "archive_period_value")
    private Integer archivePeriodValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "archive_period_unit", length = 20)
    private TimeUnit archivePeriodUnit;

    @Column(name = "purge_period_value")
    private Integer purgePeriodValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "purge_period_unit", length = 20)
    private TimeUnit purgePeriodUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PolicyStatus status = PolicyStatus.Draft;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "activated_by", length = 255)
    private String activatedBy;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @Column(name = "retired_by", length = 255)
    private String retiredBy;

    @Column(name = "retired_at")
    private OffsetDateTime retiredAt;
}
