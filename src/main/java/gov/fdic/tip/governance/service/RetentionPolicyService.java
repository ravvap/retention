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

    // ── GOV-001: Create a retention policy ──────────────────────────────────
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

    // ── GOV-002: Activate a retention policy ────────────────────────────────
    @Transactional
    public RetentionPolicyResponse activate(UUID id, String activatedBy) {
        RetentionPolicy policy = findOrThrow(id);

        if (policy.getStatus() == PolicyStatus.Active) {
            throw new GovernanceBusinessException("Policy is already active.");
        }
        if (policy.getStatus() == PolicyStatus.Retired) {
            throw new GovernanceBusinessException("Policy has been retired and can no longer be activated.");
        }

        policy.setStatus(PolicyStatus.Active);
        policy.setActivatedBy(activatedBy);
        policy.setActivatedAt(OffsetDateTime.now());
        policyRepository.save(policy);

        auditService.record(AuditAction.PolicyActivated, AuditRecordType.RetentionPolicy,
                id, activatedBy);

        return toResponse(policy);
    }

    // ── GOV-003: Retire a retention policy ──────────────────────────────────
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

    // ── GOV-004: View a retention policy ────────────────────────────────────
    @Transactional(readOnly = true)
    public RetentionPolicyResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<RetentionPolicyResponse> getAll() {
        return policyRepository.findAll().stream().map(this::toResponse).toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
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
            throw new GovernanceBusinessException("Archive period unit is required when value is supplied.");
        }
        if (req.getPurgePeriodValue() != null && req.getPurgePeriodUnit() == null) {
            throw new GovernanceBusinessException("Purge period unit is required when value is supplied.");
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
