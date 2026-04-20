package gov.fdic.tip.governance.service;

import gov.fdic.tip.governance.dto.ApplyHoldRequest;
import gov.fdic.tip.governance.dto.LegalHoldRequest;
import gov.fdic.tip.governance.dto.LegalHoldResponse;
import gov.fdic.tip.governance.entity.GovernedItem;
import gov.fdic.tip.governance.entity.LegalHold;
import gov.fdic.tip.governance.entity.LegalHoldItem;
import gov.fdic.tip.governance.enums.AuditAction;
import gov.fdic.tip.governance.enums.AuditRecordType;
import gov.fdic.tip.governance.enums.GovernanceStatus;
import gov.fdic.tip.governance.enums.HoldStatus;
import gov.fdic.tip.governance.exception.GovernanceBusinessException;
import gov.fdic.tip.governance.exception.ResourceNotFoundException;
import gov.fdic.tip.governance.repository.GovernedItemRepository;
import gov.fdic.tip.governance.repository.LegalHoldItemRepository;
import gov.fdic.tip.governance.repository.LegalHoldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LegalHoldService {

    private final LegalHoldRepository holdRepository;
    private final LegalHoldItemRepository holdItemRepository;
    private final GovernedItemRepository governedItemRepository;
    private final AuditService auditService;

    // ── GOV-008: Create a legal hold ────────────────────────────────────────
    @Transactional
    public LegalHoldResponse create(LegalHoldRequest req, String createdBy) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new GovernanceBusinessException("A hold name is required.");
        }

        LegalHold hold = LegalHold.builder()
                .name(req.getName())
                .matterReference(req.getMatterReference())
                .reason(req.getReason())
                .status(HoldStatus.Active)
                .createdBy(createdBy)
                .build();

        hold = holdRepository.save(hold);

        auditService.record(AuditAction.LegalHoldCreated, AuditRecordType.LegalHold,
                hold.getId(), createdBy,
                Map.of("holdName", hold.getName(),
                       "matterReference", String.valueOf(hold.getMatterReference())));

        return toResponse(hold);
    }

    // ── GOV-009: Apply a legal hold to governed items (batch) ────────────────
    @Transactional
    public void applyToItems(UUID holdId, ApplyHoldRequest req, String appliedBy) {
        LegalHold hold = findHoldOrThrow(holdId);

        if (hold.getStatus() == HoldStatus.Released) {
            throw new GovernanceBusinessException(
                    "A released hold cannot be applied to items.");
        }

        // Validate ALL items exist before making any changes (GOV-009 AC#4)
        List<UUID> itemIds = req.getGovernedItemIds();
        for (UUID itemId : itemIds) {
            if (!governedItemRepository.existsById(itemId)) {
                throw new ResourceNotFoundException(
                        "GovernedItem " + itemId + " could not be found. No changes applied.");
            }
        }

        // Apply (silently skip duplicates per GOV-009 AC#5)
        for (UUID itemId : itemIds) {
            Optional<LegalHoldItem> existing =
                    holdItemRepository.findByLegalHoldIdAndGovernedItemId(holdId, itemId);

            if (existing.isPresent()) {
                continue; // already linked — skip silently
            }

            GovernedItem item = governedItemRepository.findById(itemId).orElseThrow();

            LegalHoldItem link = LegalHoldItem.builder()
                    .legalHold(hold)
                    .governedItem(item)
                    .status(HoldStatus.Active)
                    .appliedBy(appliedBy)
                    .build();
            holdItemRepository.save(link);

            // Increment hold count & set OnHold
            item.setActiveHoldCount(item.getActiveHoldCount() + 1);
            item.setGovernanceStatus(GovernanceStatus.OnHold);
            governedItemRepository.save(item);

            auditService.record(AuditAction.LegalHoldApplied, AuditRecordType.GovernedItem,
                    itemId, appliedBy,
                    Map.of("legalHoldId", holdId.toString()));
        }
    }

    // ── GOV-010: Release a legal hold ───────────────────────────────────────
    @Transactional
    public LegalHoldResponse release(UUID holdId, String releasedBy) {
        LegalHold hold = findHoldOrThrow(holdId);

        if (hold.getStatus() == HoldStatus.Released) {
            throw new GovernanceBusinessException("Hold is already released. No change made.");
        }

        // Release each active hold-item link and update governed item counters
        List<LegalHoldItem> activeLinks =
                holdItemRepository.findByLegalHoldId(holdId).stream()
                        .filter(l -> l.getStatus() == HoldStatus.Active)
                        .toList();

        for (LegalHoldItem link : activeLinks) {
            link.setStatus(HoldStatus.Released);
            link.setReleasedBy(releasedBy);
            link.setReleasedAt(OffsetDateTime.now());
            holdItemRepository.save(link);

            GovernedItem item = link.getGovernedItem();
            int newCount = Math.max(0, item.getActiveHoldCount() - 1);
            item.setActiveHoldCount(newCount);

            // If no more active holds → restore Active status (GOV-010 AC#6)
            if (newCount == 0 && item.getGovernanceStatus() == GovernanceStatus.OnHold) {
                item.setGovernanceStatus(GovernanceStatus.Active);
            }
            governedItemRepository.save(item);
        }

        hold.setStatus(HoldStatus.Released);
        hold.setReleasedBy(releasedBy);
        hold.setReleasedAt(OffsetDateTime.now());
        holdRepository.save(hold);

        auditService.record(AuditAction.LegalHoldReleased, AuditRecordType.LegalHold,
                holdId, releasedBy);

        return toResponse(hold);
    }

    // ── GOV-019: View a legal hold ──────────────────────────────────────────
    @Transactional(readOnly = true)
    public LegalHoldResponse getById(UUID holdId) {
        // Viewing does NOT create an audit entry (GOV-019 AC#3)
        return toResponse(findHoldOrThrow(holdId));
    }

    @Transactional(readOnly = true)
    public List<LegalHoldResponse> getAll() {
        return holdRepository.findAll().stream().map(this::toResponse).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    public LegalHold findHoldOrThrow(UUID id) {
        return holdRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LegalHold", id));
    }

    private LegalHoldResponse toResponse(LegalHold h) {
        LegalHoldResponse r = new LegalHoldResponse();
        r.setId(h.getId());
        r.setName(h.getName());
        r.setMatterReference(h.getMatterReference());
        r.setReason(h.getReason());
        r.setStatus(h.getStatus());
        r.setCreatedBy(h.getCreatedBy());
        r.setCreatedAt(h.getCreatedAt());
        r.setReleasedBy(h.getReleasedBy());
        r.setReleasedAt(h.getReleasedAt());

        long activeCount = holdItemRepository.findByLegalHoldId(h.getId()).stream()
                .filter(l -> l.getStatus() == HoldStatus.Active).count();
        r.setActiveItemCount(activeCount);

        return r;
    }
}
