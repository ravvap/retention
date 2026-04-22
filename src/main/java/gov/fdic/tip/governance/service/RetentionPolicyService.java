package gov.fdic.tip.governance.service;

import gov.fdic.tip.governance.dto.RetentionPolicyRequest;
import gov.fdic.tip.governance.dto.RetentionPolicyResponse;
import gov.fdic.tip.governance.entity.RetentionPolicy;
import gov.fdic.tip.governance.enums.AuditAction;
import gov.fdic.tip.governance.enums.AuditRecordType;
import gov.fdic.tip.governance.enums.DispositionAction;
import gov.fdic.tip.governance.enums.PolicyStatus;
import gov.fdic.tip.governance.exception.GovernanceBusinessException;
import gov.fdic.tip.governance.exception.ResourceNotFoundException;
import gov.fdic.tip.governance.repository.RetentionPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RetentionPolicyService {

    private final RetentionPolicyRepository policyRepository;
    private final AuditService auditService;

    // ── GOV-001: Create ──────────────────────────────────────────────────────
    @Transactional
    public RetentionPolicyResponse create(RetentionPolicyRequest req, String createdBy) {
        validatePeriods(req);

        RetentionPolicy policy = RetentionPolicy.builder()
                .name(req.getName())
                .description(req.getDescription())
                .contentClassification(req.getContentClassification())
                .contentType(req.getContentType())
                .clockStartTrigger(req.getClockStartTrigger())
                .dispositionAction(req.getDispositionAction())
                .archivePeriodValue(req.getArchivePeriodValue())
                .archivePeriodUnit(req.getArchivePeriodUnit())
                .purgePeriodValue(req.getPurgePeriodValue())
                .purgePeriodUnit(req.getPurgePeriodUnit())
                .status(PolicyStatus.Draft)
                .createdBy(createdBy)
                .build();

        policy = policyRepository.save(policy);

        auditService.record(AuditAction.PolicyCreated, AuditRecordType.RetentionPolicy,
                policy.getId(), createdBy,
                Map.of("policyName", policy.getName()));

        return toResponse(policy);
    }

    // ── GOV-002: Activate ────────────────────────────────────────────────────
    @Transactional
    public RetentionPolicyResponse activate(UUID id, String activatedBy) {
        RetentionPolicy policy = findOrThrow(id);

        if (policy.getStatus() == PolicyStatus.Active) {
            throw new GovernanceBusinessException("Policy is already active.");
        }
        if (policy.getStatus() == PolicyStatus.Retired) {
            throw new GovernanceBusinessException(
                    "Policy has been retired and can no longer be activated.");
        }

        policy.setStatus(PolicyStatus.Active);
        policy.setActivatedBy(activatedBy);
        policy.setActivatedAt(OffsetDateTime.now());
        policyRepository.save(policy);

        auditService.record(AuditAction.PolicyActivated, AuditRecordType.RetentionPolicy,
                id, activatedBy);

        return toResponse(policy);
    }

    // ── GOV-003: Retire ──────────────────────────────────────────────────────
    @Transactional
    public RetentionPolicyResponse retire(UUID id, String retiredBy) {
        RetentionPolicy policy = findOrThrow(id);

        if (policy.getStatus() == PolicyStatus.Draft) {
            throw new GovernanceBusinessException(
                    "Policy is in Draft status and has never been active; it does not need to be retired.");
        }
        if (policy.getStatus() == PolicyStatus.Retired) {
            throw new GovernanceBusinessException("Policy is already retired.");
        }

        policy.setStatus(PolicyStatus.Retired);
        policy.setRetiredBy(retiredBy);
        policy.setRetiredAt(OffsetDateTime.now());
        policyRepository.save(policy);

        auditService.record(AuditAction.PolicyRetired, AuditRecordType.RetentionPolicy,
                id, retiredBy);

        return toResponse(policy);
    }

    // ── GOV-004: View ────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public RetentionPolicyResponse getById(UUID id) {
        // Viewing does NOT create an audit entry (GOV-004 AC#4)
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<RetentionPolicyResponse> getAll() {
        return policyRepository.findAll().stream().map(this::toResponse).toList();
    }

    // ── GOV-020: Edit a Draft policy ────────────────────────────────────────
    // AC#2  — any field may be edited
    // AC#3  — same validation rules as create apply
    // AC#4  — Active policy cannot be edited; must create a new policy instead
    // AC#5  — Retired policy cannot be edited; preserved for audit purposes
    // AC#7  — status remains Draft after edit
    // AC#8  — edit recorded in audit trail
    @Transactional
    public RetentionPolicyResponse update(UUID id, RetentionPolicyRequest req, String updatedBy) {
        RetentionPolicy policy = findOrThrow(id);

        // AC#4 — Active policies are immutable; user must create a new policy
        if (policy.getStatus() == PolicyStatus.Active) {
            throw new GovernanceBusinessException(
                    "Only Draft policies can be edited; to change the rules of an Active policy, " +
                    "you must create a new policy.");
        }

        // AC#5 — Retired policies are preserved for audit purposes and cannot be modified
        if (policy.getStatus() == PolicyStatus.Retired) {
            throw new GovernanceBusinessException(
                    "Retired policies are preserved for audit purposes and cannot be modified.");
        }

        // AC#3 — all create-time validation applies equally when editing
        validatePeriods(req);

        policy.setName(req.getName());
        policy.setDescription(req.getDescription());
        policy.setContentClassification(req.getContentClassification());
        policy.setContentType(req.getContentType());
        policy.setClockStartTrigger(req.getClockStartTrigger());
        policy.setDispositionAction(req.getDispositionAction());
        policy.setArchivePeriodValue(req.getArchivePeriodValue());
        policy.setArchivePeriodUnit(req.getArchivePeriodUnit());
        policy.setPurgePeriodValue(req.getPurgePeriodValue());
        policy.setPurgePeriodUnit(req.getPurgePeriodUnit());

        // AC#7 — status remains Draft; no status change on edit
        policyRepository.save(policy);

        // AC#8 — edit recorded in audit trail
        auditService.record(AuditAction.PolicyCreated, AuditRecordType.RetentionPolicy,
                id, updatedBy,
                Map.of(
                    "event",       "PolicyEdited",
                    "policyName",  policy.getName(),
                    "status",      policy.getStatus().name()
                ));

        return toResponse(policy);
    }

    // ── GOV-021: Delete a Draft policy ──────────────────────────────────────
    // AC#1  — permanently removes and displays confirmation (204)
    // AC#2  — Active policy cannot be deleted; must be retired instead
    // AC#3  — Retired policy cannot be deleted; preserved for audit purposes
    // AC#4  — non-existent id → 404
    // AC#5  — Draft policy has no governed items so deletion has no downstream effect
    // AC#6  — policy and identifier are permanently removed and cannot be recovered
    // AC#7  — audit trail captures name and key attributes at deletion time
    // Role  — TIP Admin / Manager (both roles per GOV-021)
    @Transactional
    public void delete(UUID id, String deletedBy) {
        RetentionPolicy policy = findOrThrow(id);

        // AC#2 — Active policy: must retire instead to preserve governance history
        if (policy.getStatus() == PolicyStatus.Active) {
            throw new GovernanceBusinessException(
                    "Only Draft policies can be deleted. An Active policy must be retired instead, " +
                    "so that its history and the governance record of items still assigned to it are preserved.");
        }

        // AC#3 — Retired policy: preserved for audit purposes, cannot be deleted
        if (policy.getStatus() == PolicyStatus.Retired) {
            throw new GovernanceBusinessException(
                    "Retired policies are preserved for audit purposes and cannot be deleted.");
        }

        // AC#7 — capture name and key attributes BEFORE deletion so the audit
        //         entry remains investigable even though the policy record is gone
        auditService.record(AuditAction.PolicyRetired, AuditRecordType.RetentionPolicy,
                id, deletedBy,
                Map.of(
                    "event",              "PolicyDeleted",
                    "policyName",         policy.getName(),
                    "contentType",        policy.getContentType().name(),
                    "dispositionAction",  policy.getDispositionAction().name(),
                    "clockStartTrigger",  policy.getClockStartTrigger().name(),
                    "createdBy",          policy.getCreatedBy(),
                    "createdAt",          policy.getCreatedAt().toString()
                ));

        // AC#6 — permanently remove; cannot be recovered
        policyRepository.delete(policy);
    }

    // ── Shared helpers ────────────────────────────────────────────────────────
    public RetentionPolicy findOrThrow(UUID id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RetentionPolicy", id));
    }

    private void validatePeriods(RetentionPolicyRequest req) {
        DispositionAction action = req.getDispositionAction();

        boolean needsArchive = action == DispositionAction.ArchiveOnly
                || action == DispositionAction.ArchiveThenPurge;
        boolean needsPurge = action == DispositionAction.PurgeOnly
                || action == DispositionAction.ArchiveThenPurge;

        if (needsArchive && (req.getArchivePeriodValue() == null || req.getArchivePeriodUnit() == null)) {
            throw new GovernanceBusinessException(
                    "Both archive period value and unit are required for the selected disposition action.");
        }
        if (needsPurge && (req.getPurgePeriodValue() == null || req.getPurgePeriodUnit() == null)) {
            throw new GovernanceBusinessException(
                    "Both purge period value and unit are required for the selected disposition action.");
        }
        if (req.getArchivePeriodValue() != null && req.getArchivePeriodUnit() == null) {
            throw new GovernanceBusinessException(
                    "Archive period unit is required when value is supplied.");
        }
        if (req.getPurgePeriodValue() != null && req.getPurgePeriodUnit() == null) {
            throw new GovernanceBusinessException(
                    "Purge period unit is required when value is supplied.");
        }
    }

    private RetentionPolicyResponse toResponse(RetentionPolicy p) {
        RetentionPolicyResponse r = new RetentionPolicyResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setDescription(p.getDescription());
        r.setContentClassification(p.getContentClassification());
        r.setContentType(p.getContentType());
        r.setClockStartTrigger(p.getClockStartTrigger());
        r.setDispositionAction(p.getDispositionAction());
        r.setArchivePeriodValue(p.getArchivePeriodValue());
        r.setArchivePeriodUnit(p.getArchivePeriodUnit());
        r.setPurgePeriodValue(p.getPurgePeriodValue());
        r.setPurgePeriodUnit(p.getPurgePeriodUnit());
        r.setStatus(p.getStatus());
        r.setCreatedBy(p.getCreatedBy());
        r.setCreatedAt(p.getCreatedAt());
        r.setActivatedAt(p.getActivatedAt());
        r.setRetiredAt(p.getRetiredAt());
        return r;
    }
}
