package gov.fdic.tip.governance.entity;

import gov.fdic.tip.governance.enums.HoldStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_hold")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LegalHold {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "matter_reference", length = 255)
    private String matterReference;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private HoldStatus status = HoldStatus.Active;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "released_by", length = 255)
    private String releasedBy;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;
}
