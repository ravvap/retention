package gov.fdic.tip.governance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fdic.tip.governance.dto.ApplyHoldRequest;
import gov.fdic.tip.governance.dto.LegalHoldRequest;
import gov.fdic.tip.governance.dto.LegalHoldResponse;
import gov.fdic.tip.governance.enums.HoldStatus;
import gov.fdic.tip.governance.service.LegalHoldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("LegalHoldController — Security and MVC Tests")
class LegalHoldControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private LegalHoldService legalHoldService;

    private LegalHoldRequest validHoldRequest;
    private LegalHoldResponse sampleHoldResponse;
    private UUID holdId;

    @BeforeEach
    void setUp() {
        holdId = UUID.randomUUID();

        validHoldRequest = new LegalHoldRequest();
        validHoldRequest.setName("Litigation Hold 2024");
        validHoldRequest.setMatterReference("MAT-2024-001");
        validHoldRequest.setReason("Active federal litigation.");

        sampleHoldResponse = new LegalHoldResponse();
        sampleHoldResponse.setId(holdId);
        sampleHoldResponse.setName("Litigation Hold 2024");
        sampleHoldResponse.setMatterReference("MAT-2024-001");
        sampleHoldResponse.setStatus(HoldStatus.Active);
        sampleHoldResponse.setCreatedBy("admin");
        sampleHoldResponse.setCreatedAt(OffsetDateTime.now());
        sampleHoldResponse.setActiveItemCount(0);
    }

    // ── GOV-008: Create ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-008: TIP_ADMIN can create legal hold → 201")
    void create_asAdmin_returns201() throws Exception {
        when(legalHoldService.create(any(), any())).thenReturn(sampleHoldResponse);
        mockMvc.perform(post("/api/v1/legal-holds").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validHoldRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(holdId.toString()))
                .andExpect(jsonPath("$.status").value("Active"));
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_ANALYST")
    @DisplayName("GOV-008: COMPLIANCE_ANALYST cannot create legal hold → 403")
    void create_asCompliance_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/legal-holds").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validHoldRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-008: Missing name → 400")
    void create_missingName_returns400() throws Exception {
        validHoldRequest.setName(null);
        mockMvc.perform(post("/api/v1/legal-holds").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validHoldRequest)))
                .andExpect(status().isBadRequest());
    }

    // ── GOV-009: Apply ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GOV-009: MANAGER can apply hold to items → 204")
    void applyToItems_asManager_returns204() throws Exception {
        ApplyHoldRequest req = new ApplyHoldRequest();
        req.setGovernedItemIds(List.of(UUID.randomUUID()));
        doNothing().when(legalHoldService).applyToItems(eq(holdId), any(), any());
        mockMvc.perform(post("/api/v1/legal-holds/{holdId}/apply", holdId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    @DisplayName("GOV-009: AUDITOR cannot apply holds → 403")
    void applyToItems_asAuditor_returns403() throws Exception {
        ApplyHoldRequest req = new ApplyHoldRequest();
        req.setGovernedItemIds(List.of(UUID.randomUUID()));
        mockMvc.perform(post("/api/v1/legal-holds/{holdId}/apply", holdId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-009: Empty item list → 400")
    void applyToItems_emptyList_returns400() throws Exception {
        ApplyHoldRequest req = new ApplyHoldRequest();
        req.setGovernedItemIds(List.of());
        mockMvc.perform(post("/api/v1/legal-holds/{holdId}/apply", holdId).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── GOV-010: Release ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-010: TIP_ADMIN can release hold → 200")
    void release_asAdmin_returns200() throws Exception {
        LegalHoldResponse released = new LegalHoldResponse();
        released.setId(holdId);
        released.setStatus(HoldStatus.Released);
        when(legalHoldService.release(eq(holdId), any())).thenReturn(released);
        mockMvc.perform(post("/api/v1/legal-holds/{holdId}/release", holdId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Released"));
    }

    @Test
    @WithMockUser(roles = "CASH_MGMT")
    @DisplayName("GOV-010: CASH_MGMT cannot release hold → 403")
    void release_asCashMgmt_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/legal-holds/{holdId}/release", holdId).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ── GOV-019: View ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "AUDITOR")
    @DisplayName("GOV-019: AUDITOR can view legal hold → 200")
    void getById_asAuditor_returns200() throws Exception {
        when(legalHoldService.getById(holdId)).thenReturn(sampleHoldResponse);
        mockMvc.perform(get("/api/v1/legal-holds/{holdId}", holdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(holdId.toString()))
                .andExpect(jsonPath("$.name").value("Litigation Hold 2024"));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GOV-019: Basic ANALYST cannot view legal hold → 403")
    void getById_asAnalyst_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/legal-holds/{holdId}", holdId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-019: List all holds → 200 array")
    void getAll_asAdmin_returns200() throws Exception {
        when(legalHoldService.getAll()).thenReturn(List.of(sampleHoldResponse));
        mockMvc.perform(get("/api/v1/legal-holds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("Active"));
    }
}
