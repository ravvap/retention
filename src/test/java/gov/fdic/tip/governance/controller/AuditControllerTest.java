package gov.fdic.tip.governance.controller;

import gov.fdic.tip.governance.entity.AuditEvent;
import gov.fdic.tip.governance.enums.AuditAction;
import gov.fdic.tip.governance.enums.AuditRecordType;
import gov.fdic.tip.governance.service.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuditController — Security and MVC Tests (GOV-012, GOV-013)")
class AuditControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private AuditService auditService;

    @Test
    @WithMockUser(roles = "AUDITOR")
    @DisplayName("GOV-013: AUDITOR can retrieve audit history → 200 with entries")
    void getHistory_asAuditor_returns200() throws Exception {
        UUID recordId = UUID.randomUUID();
        AuditEvent event = AuditEvent.builder()
                .id(1L)
                .action(AuditAction.PolicyCreated)
                .recordType(AuditRecordType.RetentionPolicy)
                .recordId(recordId)
                .performedBy("admin")
                .performedAt(OffsetDateTime.now())
                .context(Map.of("policyName", "Test Policy"))
                .build();
        when(auditService.getHistory(AuditRecordType.RetentionPolicy, recordId))
                .thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/audit")
                        .param("recordType", "RetentionPolicy")
                        .param("recordId", recordId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].action").value("PolicyCreated"))
                .andExpect(jsonPath("$[0].performedBy").value("admin"));
    }

    @Test
    @WithMockUser(roles = "COMPLIANCE_ANALYST")
    @DisplayName("GOV-013: COMPLIANCE_ANALYST can view audit history → 200")
    void getHistory_asCompliance_returns200() throws Exception {
        UUID recordId = UUID.randomUUID();
        when(auditService.getHistory(AuditRecordType.GovernedItem, recordId))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/v1/audit")
                        .param("recordType", "GovernedItem")
                        .param("recordId", recordId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GOV-013: Basic ANALYST cannot access audit history → 403")
    void getHistory_asAnalyst_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .param("recordType", "RetentionPolicy")
                        .param("recordId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-013: Missing recordId → 400")
    void getHistory_missingRecordId_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .param("recordType", "RetentionPolicy"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-013: Empty history returns empty list, not error")
    void getHistory_noEntries_returnsEmptyList() throws Exception {
        UUID recordId = UUID.randomUUID();
        when(auditService.getHistory(AuditRecordType.LegalHold, recordId))
                .thenReturn(List.of());
        mockMvc.perform(get("/api/v1/audit")
                        .param("recordType", "LegalHold")
                        .param("recordId", recordId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GOV-013: Unauthenticated access → 401")
    void getHistory_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/audit")
                        .param("recordType", "RetentionPolicy")
                        .param("recordId", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }
}
