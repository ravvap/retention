package gov.fdic.tip.governance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_disposition_run")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BatchDispositionRun {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "run_at", nullable = false)
    @Builder.Default
    private OffsetDateTime runAt = OffsetDateTime.now();

    @Builder.Default private int itemsEvaluated = 0;
    @Builder.Default private int itemsArchived  = 0;
    @Builder.Default private int itemsPurged    = 0;
    @Builder.Default private int itemsSkipped   = 0;
    @Builder.Default private int itemsError     = 0;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;
}
