package gov.fdic.tip.governance.controller;

import gov.fdic.tip.governance.dto.*;
import gov.fdic.tip.governance.service.GovernedItemService;
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

import java.util.UUID;

/**
 * Epic 2  — Governed Item Registration  (GOV-005, GOV-006, GOV-007)
 * Epic 4  — Disposition Evaluation      (GOV-011)
 * Epic 6  — Batch and Execution         (GOV-015, GOV-016, GOV-017, GOV-018)
 *
 * Role matrix:
 *  Register (doc/db)  → SR_ANALYST, CASH_MGMT, COMPLIANCE_ANALYST
 *  View               → SR_ANALYST, ANALYST, CASH_MGMT, COMPLIANCE_ANALYST
 *  Disposition check  → SR_ANALYST, CASH_MGMT, COMPLIANCE_ANALYST
 *  Archive / Purge    → TIP_ADMIN, MANAGER (system-automated via batch)
 */
@RestController
@RequestMapping("/api/v1/governed-items")
@RequiredArgsConstructor
@Tag(name = "Governed Items", description = "GOV-005 to GOV-007, GOV-011, GOV-015 to GOV-018")
@SecurityRequirement(name = "bearerAuth")
public class GovernedItemController {

    private final GovernedItemService governedItemService;

    // ── GOV-005: Register a document ────────────────────────────────────────
    @PostMapping("/documents")
    @PreAuthorize("hasAnyRole('SR_ANALYST', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'TIP_ADMIN', 'MANAGER')")
    @Operation(summary = "GOV-005: Register a document under governance")
    public ResponseEntity<GovernedItemResponse> registerDocument(
            @Valid @RequestBody RegisterDocumentRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(governedItemService.registerDocument(req, user.getUsername()));
    }

    // ── GOV-006: Register a database record ─────────────────────────────────
    @PostMapping("/database-records")
    @PreAuthorize("hasAnyRole('SR_ANALYST', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'TIP_ADMIN', 'MANAGER')")
    @Operation(summary = "GOV-006: Register a database record under governance")
    public ResponseEntity<GovernedItemResponse> registerDbRecord(
            @Valid @RequestBody RegisterDbRecordRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(governedItemService.registerDbRecord(req, user.getUsername()));
    }

    // ── GOV-007: View a governed item ───────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'SR_ANALYST', 'ANALYST', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'AUDITOR')")
    @Operation(summary = "GOV-007: View a governed item and its current governance details")
    public ResponseEntity<GovernedItemResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(governedItemService.getById(id));
    }

    // ── GOV-011: Check disposition recommendation ───────────────────────────
    @GetMapping("/{id}/disposition-recommendation")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'SR_ANALYST', 'CASH_MGMT', 'COMPLIANCE_ANALYST')")
    @Operation(summary = "GOV-011: Check what disposition action applies to a governed item")
    public ResponseEntity<DispositionRecommendationResponse> getDispositionRecommendation(
            @PathVariable UUID id) {
        return ResponseEntity.ok(governedItemService.getRecommendation(id));
    }

    // ── GOV-015: Archive a document ─────────────────────────────────────────
    @PostMapping("/{id}/archive-document")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'SCHEDULER')")
    @Operation(summary = "GOV-015: Archive a document that has reached its archive eligibility date")
    public ResponseEntity<GovernedItemResponse> archiveDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(governedItemService.archiveDocument(id, user.getUsername()));
    }

    // ── GOV-016: Archive a database record ──────────────────────────────────
    @PostMapping("/{id}/archive-database-record")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'SCHEDULER')")
    @Operation(summary = "GOV-016: Archive a database record that has reached its archive eligibility date")
    public ResponseEntity<GovernedItemResponse> archiveDbRecord(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(governedItemService.archiveDbRecord(id, user.getUsername()));
    }

    // ── GOV-017: Purge a document ────────────────────────────────────────────
    @PostMapping("/{id}/purge-document")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'SCHEDULER')")
    @Operation(summary = "GOV-017: Purge a document that has reached its purge eligibility date")
    public ResponseEntity<GovernedItemResponse> purgeDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(governedItemService.purgeDocument(id, user.getUsername()));
    }

    // ── GOV-018: Purge a database record ────────────────────────────────────
    @PostMapping("/{id}/purge-database-record")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'SCHEDULER')")
    @Operation(summary = "GOV-018: Purge a database record that has reached its purge eligibility date")
    public ResponseEntity<GovernedItemResponse> purgeDbRecord(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(governedItemService.purgeDbRecord(id, user.getUsername()));
    }
}
