package gov.fdic.tip.governance.entity;

import gov.fdic.tip.governance.enums.HoldStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_hold_item",
       uniqueConstraints = @UniqueConstraint(columnNames = {"legal_hold_id","governed_item_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegalHoldItem {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_hold_id", nullable = false)
    private LegalHold legalHold;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "governed_item_id", nullable = false)
    private GovernedItem governedItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HoldStatus status = HoldStatus.Active;

    @Column(name = "applied_by", nullable = false, length = 255)
    private String appliedBy;

    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private OffsetDateTime appliedAt;

    @Column(name = "released_by", length = 255)
    private String releasedBy;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;
}
