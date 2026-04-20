package gov.fdic.tip.governance.controller;

import gov.fdic.tip.governance.entity.GovernedItemEvent;
import gov.fdic.tip.governance.entity.GovernedItem;
import gov.fdic.tip.governance.exception.ResourceNotFoundException;
import gov.fdic.tip.governance.repository.GovernedItemEventRepository;
import gov.fdic.tip.governance.repository.GovernedItemRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Governed Item Events — supports the EventDate clock-start trigger.
 * When a policy uses ClockStartTrigger.EventDate, business events
 * (e.g. CaseClosure) are recorded here to start the retention clock.
 *
 * Role matrix:
 *  Record event   → SR_ANALYST, CASH_MGMT, COMPLIANCE_ANALYST, TIP_ADMIN, MANAGER
 *  View events    → all analyst, auditor, admin roles
 */
@RestController
@RequestMapping("/api/v1/governed-items/{itemId}/events")
@RequiredArgsConstructor
@Tag(name = "Governed Item Events", description = "Business events that trigger the retention clock for EventDate policies")
@SecurityRequirement(name = "bearerAuth")
public class GovernedItemEventController {

    private final GovernedItemRepository governedItemRepository;
    private final GovernedItemEventRepository eventRepository;

    // ── Record a business event ──────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('SR_ANALYST', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'TIP_ADMIN', 'MANAGER')")
    @Operation(summary = "Record a business event for a governed item (e.g. CaseClosure)")
    public ResponseEntity<GovernedItemEventResponse> recordEvent(
            @PathVariable UUID itemId,
            @Valid @RequestBody GovernedItemEventRequest req,
            @AuthenticationPrincipal UserDetails user) {

        GovernedItem item = governedItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("GovernedItem", itemId));

        GovernedItemEvent event = GovernedItemEvent.builder()
                .governedItem(item)
                .eventType(req.getEventType())
                .eventDate(req.getEventDate())
                .recordedBy(user.getUsername())
                .recordedAt(OffsetDateTime.now())
                .build();

        event = eventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(event));
    }

    // ── List all events for a governed item ──────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('TIP_ADMIN', 'MANAGER', 'SR_ANALYST', 'ANALYST', 'CASH_MGMT', 'COMPLIANCE_ANALYST', 'AUDITOR')")
    @Operation(summary = "List all business events recorded for a governed item")
    public ResponseEntity<List<GovernedItemEventResponse>> listEvents(@PathVariable UUID itemId) {
        if (!governedItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("GovernedItem", itemId);
        }
        List<GovernedItemEventResponse> events =
                eventRepository.findByGovernedItemIdOrderByEventDateDesc(itemId)
                        .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(events);
    }

    // ── Inner request/response DTOs (kept local to avoid DTO package clutter) ─

    @Data
    public static class GovernedItemEventRequest {
        @NotBlank private String eventType;   // e.g. "CaseClosure", "ContractExpiry"
        @NotNull  private LocalDate eventDate;
    }

    @Data
    public static class GovernedItemEventResponse {
        private UUID id;
        private UUID governedItemId;
        private String eventType;
        private LocalDate eventDate;
        private String recordedBy;
        private OffsetDateTime recordedAt;
    }

    private GovernedItemEventResponse toResponse(GovernedItemEvent e) {
        GovernedItemEventResponse r = new GovernedItemEventResponse();
        r.setId(e.getId());
        r.setGovernedItemId(e.getGovernedItem().getId());
        r.setEventType(e.getEventType());
        r.setEventDate(e.getEventDate());
        r.setRecordedBy(e.getRecordedBy());
        r.setRecordedAt(e.getRecordedAt());
        return r;
    }
}
