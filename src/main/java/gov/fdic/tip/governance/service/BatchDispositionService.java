package gov.fdic.tip.governance.service;

import gov.fdic.tip.governance.dto.BatchRunResponse;
import gov.fdic.tip.governance.entity.BatchDispositionRun;
import gov.fdic.tip.governance.entity.GovernedItem;
import gov.fdic.tip.governance.enums.*;
import gov.fdic.tip.governance.repository.BatchDispositionRunRepository;
import gov.fdic.tip.governance.repository.GovernedItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchDispositionService {

    private final GovernedItemRepository governedItemRepository;
    private final BatchDispositionRunRepository runRepository;
    private final GovernedItemService governedItemService;
    private final AuditService auditService;

    // ── GOV-014: Daily automated sweep ──────────────────────────────────────
    // Runs every day at 01:00 AM server time; can also be triggered manually.
    @Scheduled(cron = "0 0 1 * * *")
    public BatchRunResponse runScheduled() {
        return runSweep("scheduler");
    }

    @Transactional
    public BatchRunResponse runSweep(String triggeredBy) {
        log.info("Starting disposition sweep triggered by: {}", triggeredBy);

        BatchDispositionRun run = BatchDispositionRun.builder()
                .runAt(OffsetDateTime.now())
                .build();
        run = runRepository.save(run);

        int evaluated = 0, archived = 0, purged = 0, skipped = 0, errors = 0;

        List<GovernedItem> candidates = governedItemRepository
                .findItemsDueForDisposition(LocalDate.now());

        // Also include all Active items to check hold/retain status
        List<GovernedItem> allActive = governedItemRepository
                .findByGovernanceStatus(GovernanceStatus.Active);

        // Merge — candidates are already filtered; allActive gives us items not yet due
        // For count we iterate the full active list
        List<GovernedItem> allItems = allActive;

        for (GovernedItem item : allItems) {
            // Skip already purged/error items
            if (item.getGovernanceStatus() == GovernanceStatus.Purged
                    || item.getGovernanceStatus() == GovernanceStatus.Error) {
                continue;
            }
            evaluated++;

            try {
                DispositionRecommendation rec = governedItemService.computeRecommendation(item);

                switch (rec) {
                    case SkipOnHold -> {
                        log.debug("Item {} skipped — on hold", item.getId());
                        skipped++;
                    }
                    case Purge -> {
                        auditService.record(AuditAction.ItemPurged, AuditRecordType.GovernedItem,
                                item.getId(), triggeredBy,
                                Map.of("trigger", "BatchSweep",
                                       "purgeEligibilityDate",
                                       String.valueOf(item.getPurgeEligibilityDate())));
                        item.setGovernanceStatus(GovernanceStatus.Purged);
                        governedItemService.saveItem(item);
                        purged++;
                        log.info("Item {} purged by batch sweep", item.getId());
                    }
                    case Archive -> {
                        auditService.record(AuditAction.ItemArchived, AuditRecordType.GovernedItem,
                                item.getId(), triggeredBy,
                                Map.of("trigger", "BatchSweep",
                                       "archiveEligibilityDate",
                                       String.valueOf(item.getArchiveEligibilityDate())));
                        item.setGovernanceStatus(GovernanceStatus.Archived);
                        governedItemService.saveItem(item);
                        archived++;
                        log.info("Item {} archived by batch sweep", item.getId());
                    }
                    case Retain -> skipped++;
                }

            } catch (Exception e) {
                log.error("Error processing item {} in sweep: {}", item.getId(), e.getMessage());
                item.setGovernanceStatus(GovernanceStatus.Error);
                governedItemService.saveItem(item);
                auditService.record(AuditAction.ItemError, AuditRecordType.GovernedItem,
                        item.getId(), triggeredBy, Map.of("error", e.getMessage()));
                errors++;
            }
        }

        String summary = String.format(
                "Sweep completed: %d evaluated, %d archived, %d purged, %d skipped, %d errors.",
                evaluated, archived, purged, skipped, errors);

        run.setItemsEvaluated(evaluated);
        run.setItemsArchived(archived);
        run.setItemsPurged(purged);
        run.setItemsSkipped(skipped);
        run.setItemsError(errors);
        run.setCompletedAt(OffsetDateTime.now());
        run.setSummary(summary);
        run = runRepository.save(run);

        // Audit the batch completion (GOV-012, GOV-014 AC#8)
        auditService.record(AuditAction.BatchRunCompleted, AuditRecordType.GovernedItem,
                run.getId(), triggeredBy,
                Map.of("evaluated", evaluated, "archived", archived,
                       "purged", purged, "skipped", skipped, "errors", errors));

        log.info(summary);
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public List<BatchRunResponse> getAllRuns() {
        return runRepository.findAll().stream().map(this::toResponse).toList();
    }

    private BatchRunResponse toResponse(BatchDispositionRun r) {
        BatchRunResponse resp = new BatchRunResponse();
        resp.setId(r.getId());
        resp.setRunAt(r.getRunAt());
        resp.setItemsEvaluated(r.getItemsEvaluated());
        resp.setItemsArchived(r.getItemsArchived());
        resp.setItemsPurged(r.getItemsPurged());
        resp.setItemsSkipped(r.getItemsSkipped());
        resp.setItemsError(r.getItemsError());
        resp.setCompletedAt(r.getCompletedAt());
        resp.setSummary(r.getSummary());
        return resp;
    }
}
