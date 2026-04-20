package gov.fdic.tip.governance.service;

import gov.fdic.tip.governance.dto.*;
import gov.fdic.tip.governance.entity.GovernedItem;
import gov.fdic.tip.governance.entity.RetentionPolicy;
import gov.fdic.tip.governance.enums.*;
import gov.fdic.tip.governance.exception.GovernanceBusinessException;
import gov.fdic.tip.governance.exception.ResourceNotFoundException;
import gov.fdic.tip.governance.repository.GovernedItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GovernedItemService {

    private final GovernedItemRepository governedItemRepository;
    private final RetentionPolicyService policyService;
    private final AuditService auditService;

    // ── GOV-005: Register a document ────────────────────────────────────────
    @Transactional
    public GovernedItemResponse registerDocument(RegisterDocumentRequest req, String registeredBy) {
        RetentionPolicy policy = policyService.findOrThrow(req.getRetentionPolicyId());

        if (policy.getStatus() != PolicyStatus.Active) {
            throw new GovernanceBusinessException("Policy must be Active to register items under it.");
        }
        if (policy.getContentType() != ContentType.Document && policy.getContentType() != ContentType.Any) {
            throw new GovernanceBusinessException("Selected policy does not cover documents.");
        }

        GovernedItem item = GovernedItem.builder()
                .itemType(ItemType.Document)
                .documentReference(req.getDocumentReference())
                .retentionPolicy(policy)
                .retentionStartDate(req.getRetentionStartDate())
                .sourceSystem(req.getSourceSystem())
                .businessContentClass(req.getBusinessContentClass())
                .governanceStatus(GovernanceStatus.Active)
                .activeHoldCount(0)
                .registeredBy(registeredBy)
                .registeredAt(OffsetDateTime.now())
                .build();

        computeEligibilityDates(item, policy);
        item = governedItemRepository.save(item);

        auditService.record(AuditAction.ItemRegistered, AuditRecordType.GovernedItem,
                item.getId(), registeredBy,
                Map.of("documentReference", req.getDocumentReference()));

        return toResponse(item);
    }

    // ── GOV-006: Register a database record ─────────────────────────────────
    @Transactional
    public GovernedItemResponse registerDbRecord(RegisterDbRecordRequest req, String registeredBy) {
        RetentionPolicy policy = policyService.findOrThrow(req.getRetentionPolicyId());

        if (policy.getStatus() != PolicyStatus.Active) {
            throw new GovernanceBusinessException("Policy must be Active to register items under it.");
        }
        if (policy.getContentType() != ContentType.DatabaseRecord && policy.getContentType() != ContentType.Any) {
            throw new GovernanceBusinessException("Selected policy does not cover database records.");
        }

        GovernedItem item = GovernedItem.builder()
                .itemType(ItemType.DatabaseRecord)
                .dbTableName(req.getDbTableName())
                .dbRecordKey(req.getDbRecordKey())
                .retentionPolicy(policy)
                .retentionStartDate(req.getRetentionStartDate())
                .sourceSystem(req.getSourceSystem())
                .businessContentClass(req.getBusinessContentClass())
                .governanceStatus(GovernanceStatus.Active)
                .activeHoldCount(0)
                .registeredBy(registeredBy)
                .registeredAt(OffsetDateTime.now())
                .build();

        computeEligibilityDates(item, policy);
        item = governedItemRepository.save(item);

        auditService.record(AuditAction.ItemRegistered, AuditRecordType.GovernedItem,
                item.getId(), registeredBy,
                Map.of("table", req.getDbTableName(), "key", req.getDbRecordKey()));

        return toResponse(item);
    }

    // ── GOV-007: View a governed item ───────────────────────────────────────
    @Transactional(readOnly = true)
    public GovernedItemResponse getById(UUID id) {
        GovernedItem item = findOrThrow(id);
        GovernedItemResponse response = toResponse(item);
        response.setDispositionRecommendation(computeRecommendation(item));
        return response;
    }

    // ── GOV-011: Check disposition recommendation ───────────────────────────
    @Transactional(readOnly = true)
    public DispositionRecommendationResponse getRecommendation(UUID id) {
        GovernedItem item = findOrThrow(id);
        DispositionRecommendation rec = computeRecommendation(item);
        String rationale = buildRationale(item, rec);
        return new DispositionRecommendationResponse(id, rec, rationale);
    }

    // ── GOV-015: Archive a document ─────────────────────────────────────────
    @Transactional
    public GovernedItemResponse archiveDocument(UUID id, String authorisedBy) {
        GovernedItem item = findOrThrow(id);

        if (item.getItemType() != ItemType.Document) {
            throw new GovernanceBusinessException("This action is only applicable to documents.");
        }
        if (item.getActiveHoldCount() > 0) {
            throw new GovernanceBusinessException(
                    "Item is under active legal hold. Archive action abandoned.");
        }
        if (item.getGovernanceStatus() != GovernanceStatus.Active) {
            throw new GovernanceBusinessException(
                    "Only Active items can be archived. Current status: " + item.getGovernanceStatus());
        }

        item.setGovernanceStatus(GovernanceStatus.Archived);
        governedItemRepository.save(item);

        auditService.record(AuditAction.ItemArchived, AuditRecordType.GovernedItem,
                id, authorisedBy,
                Map.of("documentReference", item.getDocumentReference()));

        return toResponse(item);
    }

    // ── GOV-016: Archive a database record ──────────────────────────────────
    @Transactional
    public GovernedItemResponse archiveDbRecord(UUID id, String authorisedBy) {
        GovernedItem item = findOrThrow(id);

        if (item.getItemType() != ItemType.DatabaseRecord) {
            throw new GovernanceBusinessException("This action is only applicable to database records.");
        }
        if (item.getActiveHoldCount() > 0) {
            throw new GovernanceBusinessException(
                    "Item is under active legal hold. Archive action abandoned.");
        }
        if (item.getGovernanceStatus() != GovernanceStatus.Active) {
            throw new GovernanceBusinessException(
                    "Only Active items can be archived. Current status: " + item.getGovernanceStatus());
        }

        item.setGovernanceStatus(GovernanceStatus.Archived);
        governedItemRepository.save(item);

        auditService.record(AuditAction.ItemArchived, AuditRecordType.GovernedItem,
                id, authorisedBy,
                Map.of("table", item.getDbTableName(), "key", item.getDbRecordKey()));

        return toResponse(item);
    }

    // ── GOV-017: Purge a document ────────────────────────────────────────────
    @Transactional
    public GovernedItemResponse purgeDocument(UUID id, String authorisedBy) {
        GovernedItem item = findOrThrow(id);

        if (item.getItemType() != ItemType.Document) {
            throw new GovernanceBusinessException("This action is only applicable to documents.");
        }
        if (item.getActiveHoldCount() > 0) {
            throw new GovernanceBusinessException(
                    "Item is under active legal hold. Purge action abandoned. Legal hold blocks purging under all circumstances.");
        }

        // Pre-deletion audit record — retained indefinitely (GOV-017 AC#3)
        auditService.record(AuditAction.ItemPurged, AuditRecordType.GovernedItem,
                id, authorisedBy,
                Map.of("documentReference", item.getDocumentReference(),
                       "preDeletionStatus", item.getGovernanceStatus().name(),
                       "purgeEligibilityDate", String.valueOf(item.getPurgeEligibilityDate())));

        item.setGovernanceStatus(GovernanceStatus.Purged);
        governedItemRepository.save(item);

        return toResponse(item);
    }

    // ── GOV-018: Purge a database record ────────────────────────────────────
    @Transactional
    public GovernedItemResponse purgeDbRecord(UUID id, String authorisedBy) {
        GovernedItem item = findOrThrow(id);

        if (item.getItemType() != ItemType.DatabaseRecord) {
            throw new GovernanceBusinessException("This action is only applicable to database records.");
        }
        if (item.getActiveHoldCount() > 0) {
            throw new GovernanceBusinessException(
                    "Item is under active legal hold. Purge action abandoned. Legal hold blocks purging under all circumstances.");
        }

        // Pre-deletion audit record (GOV-018 AC#3)
        auditService.record(AuditAction.ItemPurged, AuditRecordType.GovernedItem,
                id, authorisedBy,
                Map.of("table", item.getDbTableName(),
                       "key", item.getDbRecordKey(),
                       "preDeletionStatus", item.getGovernanceStatus().name()));

        item.setGovernanceStatus(GovernanceStatus.Purged);
        governedItemRepository.save(item);

        return toResponse(item);
    }

    // ── Package-level helpers used by BatchDispositionService ────────────────
    public GovernedItem findOrThrow(UUID id) {
        return governedItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GovernedItem", id));
    }

    public void saveItem(GovernedItem item) {
        governedItemRepository.save(item);
    }

    public DispositionRecommendation computeRecommendation(GovernedItem item) {
        if (item.getActiveHoldCount() > 0) return DispositionRecommendation.SkipOnHold;

        LocalDate today = LocalDate.now();

        // Purge takes priority over archive (GOV-011 AC#6)
        if (item.getPurgeEligibilityDate() != null && !today.isBefore(item.getPurgeEligibilityDate())) {
            return DispositionRecommendation.Purge;
        }
        if (item.getArchiveEligibilityDate() != null && !today.isBefore(item.getArchiveEligibilityDate())) {
            return DispositionRecommendation.Archive;
        }
        return DispositionRecommendation.Retain;
    }

    // ── Private helpers ──────────────────────────────────────────────────────
    private void computeEligibilityDates(GovernedItem item, RetentionPolicy policy) {
        LocalDate start = item.getRetentionStartDate();

        if (policy.getArchivePeriodValue() != null) {
            item.setArchiveEligibilityDate(addPeriod(start,
                    policy.getArchivePeriodValue(), policy.getArchivePeriodUnit()));
        }
        if (policy.getPurgePeriodValue() != null) {
            item.setPurgeEligibilityDate(addPeriod(start,
                    policy.getPurgePeriodValue(), policy.getPurgePeriodUnit()));
        }
    }

    private LocalDate addPeriod(LocalDate base, int value, TimeUnit unit) {
        return switch (unit) {
            case Days   -> base.plusDays(value);
            case Months -> base.plusMonths(value);
            case Years  -> base.plusYears(value);
        };
    }

    private String buildRationale(GovernedItem item, DispositionRecommendation rec) {
        return switch (rec) {
            case SkipOnHold -> "Item has " + item.getActiveHoldCount() + " active legal hold(s). All disposition is blocked.";
            case Purge      -> "Purge eligibility date " + item.getPurgeEligibilityDate() + " has been reached or passed.";
            case Archive    -> "Archive eligibility date " + item.getArchiveEligibilityDate() + " has been reached or passed.";
            case Retain     -> "No eligibility dates have been reached yet. Item should be retained.";
        };
    }

    GovernedItemResponse toResponse(GovernedItem i) {
        GovernedItemResponse r = new GovernedItemResponse();
        r.setId(i.getId());
        r.setItemType(i.getItemType());
        r.setDocumentReference(i.getDocumentReference());
        r.setDbTableName(i.getDbTableName());
        r.setDbRecordKey(i.getDbRecordKey());
        r.setSourceSystem(i.getSourceSystem());
        r.setBusinessContentClass(i.getBusinessContentClass());
        r.setRetentionPolicyId(i.getRetentionPolicy().getId());
        r.setRetentionPolicyName(i.getRetentionPolicy().getName());
        r.setRetentionStartDate(i.getRetentionStartDate());
        r.setArchiveEligibilityDate(i.getArchiveEligibilityDate());
        r.setPurgeEligibilityDate(i.getPurgeEligibilityDate());
        r.setGovernanceStatus(i.getGovernanceStatus());
        r.setActiveHoldCount(i.getActiveHoldCount());
        r.setRegisteredBy(i.getRegisteredBy());
        r.setRegisteredAt(i.getRegisteredAt());
        return r;
    }
}
