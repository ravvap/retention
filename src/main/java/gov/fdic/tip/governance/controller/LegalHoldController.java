package gov.fdic.tip.governance.controller;

import gov.fdic.tip.governance.dto.ApplyHoldRequest;
import gov.fdic.tip.governance.dto.LegalHoldRequest;
import gov.fdic.tip.governance.dto.LegalHoldResponse;
import gov.fdic.tip.governance.service.LegalHoldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Epic 3 — Legal Hold Management
 *
 * Role matrix:
 *  GOV-008 Create hold      → TIP_ADMIN, MANAGER
 *  GOV-009 Apply hold       → TIP_ADMIN, MANAGER
 *  GOV-010 Release hold     → TIP_ADMIN, MANAGER
 *  GOV-019 View hold        → TIP_ADMIN, MANAGER, CASH_MGMT, COMPLIANCE_ANALYST
 */
@RestController
@RequestMapping("/api/v1/legal-holds")
@RequiredArgsConstructor
@Tag(name = "Legal Holds", description = "GOV-008, GOV-009, GOV-010, GOV-019 — Legal Hold Management")
@SecurityRequirement(name = "bearerAuth")
public class LegalHoldController {

    private final LegalHoldService legalHoldService;

    // ── GOV-008: Create a legal hold ────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER')")
    @Operation(summary = "GOV-008: Create a legal hold")
    public ResponseEntity<LegalHoldResponse> create(
            @Valid @RequestBody LegalHoldRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(legalHoldService.create(req, user.getUsername()));
    }

    // ── GOV-009: Apply a legal hold to governed items ───────────────────────
    @PostMapping("/{holdId}/apply")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER')")
    @Operation(summary = "GOV-009: Apply a legal hold to one or more governed items")
    public ResponseEntity<Void> applyToItems(
            @PathVariable UUID holdId,
            @Valid @RequestBody ApplyHoldRequest req,
            @AuthenticationPrincipal UserDetails user) {
        legalHoldService.applyToItems(holdId, req, user.getUsername());
        return ResponseEntity.noContent().build();
    }

    // ── GOV-010: Release a legal hold ───────────────────────────────────────
    @PostMapping("/{holdId}/release")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER')")
    @Operation(summary = "GOV-010: Release a legal hold")
    public ResponseEntity<LegalHoldResponse> release(
            @PathVariable UUID holdId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(legalHoldService.release(holdId, user.getUsername()));
    }

    // ── GOV-019: View a legal hold ──────────────────────────────────────────
    @GetMapping("/{holdId}")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'AUDITOR')")
    @Operation(summary = "GOV-019: View a legal hold and all its details")
    public ResponseEntity<LegalHoldResponse> getById(@PathVariable UUID holdId) {
        return ResponseEntity.ok(legalHoldService.getById(holdId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'AUDITOR')")
    @Operation(summary = "GOV-019: List all legal holds")
    public ResponseEntity<List<LegalHoldResponse>> getAll() {
        return ResponseEntity.ok(legalHoldService.getAll());
    }
}
