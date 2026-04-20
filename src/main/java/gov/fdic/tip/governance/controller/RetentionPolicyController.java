package gov.fdic.tip.governance.controller;

import gov.fdic.tip.governance.dto.RetentionPolicyRequest;
import gov.fdic.tip.governance.dto.RetentionPolicyResponse;
import gov.fdic.tip.governance.service.RetentionPolicyService;
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
 * Epic 1 — Retention Policy Management
 *
 * Role matrix:
 *  GOV-001 Create   → TIP_ADMIN, MANAGER
 *  GOV-002 Activate → TIP_ADMIN, MANAGER
 *  GOV-003 Retire   → TIP_ADMIN, MANAGER
 *  GOV-004 View     → TIP_ADMIN, MANAGER, SR_ANALYST
 */
@RestController
@RequestMapping("/api/v1/retention-policies")
@RequiredArgsConstructor
@Tag(name = "Retention Policies", description = "GOV-001 to GOV-004 — Retention Policy Management")
@SecurityRequirement(name = "bearerAuth")
public class RetentionPolicyController {

    private final RetentionPolicyService policyService;

    // ── GOV-001: Create a retention policy ──────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER')")
    @Operation(summary = "GOV-001: Create a retention policy")
    public ResponseEntity<RetentionPolicyResponse> create(
            @Valid @RequestBody RetentionPolicyRequest req,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(policyService.create(req, user.getUsername()));
    }

    // ── GOV-002: Activate a retention policy ────────────────────────────────
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER')")
    @Operation(summary = "GOV-002: Activate a retention policy")
    public ResponseEntity<RetentionPolicyResponse> activate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(policyService.activate(id, user.getUsername()));
    }

    // ── GOV-003: Retire a retention policy ──────────────────────────────────
    @PostMapping("/{id}/retire")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER')")
    @Operation(summary = "GOV-003: Retire a retention policy")
    public ResponseEntity<RetentionPolicyResponse> retire(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(policyService.retire(id, user.getUsername()));
    }

    // ── GOV-004: View a retention policy ────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'SR_ANALYST')")
    @Operation(summary = "GOV-004: View a retention policy")
    public ResponseEntity<RetentionPolicyResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(policyService.getById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'SR_ANALYST')")
    @Operation(summary = "GOV-004: List all retention policies")
    public ResponseEntity<List<RetentionPolicyResponse>> getAll() {
        return ResponseEntity.ok(policyService.getAll());
    }
}
