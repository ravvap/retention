package gov.fdic.tip.governance.controller;

import gov.fdic.tip.governance.dto.AuditEventResponse;
import gov.fdic.tip.governance.entity.AuditEvent;
import gov.fdic.tip.governance.enums.AuditRecordType;
import gov.fdic.tip.governance.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Epic 5 — Audit Logging
 *
 * GOV-012: Audit entries are written automatically — no manual creation allowed.
 * GOV-013: View/search the audit trail (read-only).
 *
 * Role matrix:
 *  View audit history → AUDITOR, CASH_MGMT, COMPLIANCE_ANALYST, TIP_ADMIN, MANAGER
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit Trail", description = "GOV-012, GOV-013 — Audit Logging")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    // ── GOV-013: View audit history for a governance record ─────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('AUDITOR', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'TIP_ADMIN', 'MANAGER')")
    @Operation(
        summary = "GOV-013: View the audit history for a governance record",
        description = "Returns all audit entries for a specific record in chronological order " +
                      "(oldest to most recent). Both recordType AND recordId are required."
    )
    public ResponseEntity<List<AuditEventResponse>> getHistory(
            @RequestParam AuditRecordType recordType,
            @RequestParam UUID recordId) {

        List<AuditEvent> events = auditService.getHistory(recordType, recordId);
        List<AuditEventResponse> response = events.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(response);
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        AuditEventResponse r = new AuditEventResponse();
        r.setSeq(e.getId());
        r.setRecordType(e.getRecordType());
        r.setRecordId(e.getRecordId());
        r.setAction(e.getAction());
        r.setPerformedBy(e.getPerformedBy());
        r.setPerformedAt(e.getPerformedAt());
        r.setContext(e.getContext());
        return r;
    }
}
