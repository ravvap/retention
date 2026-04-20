package gov.fdic.tip.governance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.fdic.tip.governance.dto.RetentionPolicyRequest;
import gov.fdic.tip.governance.dto.RetentionPolicyResponse;
import gov.fdic.tip.governance.enums.*;
import gov.fdic.tip.governance.service.RetentionPolicyService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("RetentionPolicyController — Security and MVC Tests")
class RetentionPolicyControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private RetentionPolicyService policyService;

    private RetentionPolicyRequest validRequest;
    private RetentionPolicyResponse sampleResponse;
    private UUID policyId;

    @BeforeEach
    void setUp() {
        policyId = UUID.randomUUID();

        validRequest = new RetentionPolicyRequest();
        validRequest.setName("Case File Retention");
        validRequest.setContentType(ContentType.Document);
        validRequest.setClockStartTrigger(ClockStartTrigger.CreationDate);
        validRequest.setDispositionAction(DispositionAction.ArchiveThenPurge);
        validRequest.setArchivePeriodValue(3);
        validRequest.setArchivePeriodUnit(TimeUnit.Years);
        validRequest.setPurgePeriodValue(7);
        validRequest.setPurgePeriodUnit(TimeUnit.Years);

        sampleResponse = new RetentionPolicyResponse();
        sampleResponse.setId(policyId);
        sampleResponse.setName("Case File Retention");
        sampleResponse.setStatus(PolicyStatus.Draft);
        sampleResponse.setContentType(ContentType.Document);
        sampleResponse.setDispositionAction(DispositionAction.ArchiveThenPurge);
        sampleResponse.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-001: TIP_ADMIN can create retention policy → 201")
    void create_asAdmin_returns201() throws Exception {
        when(policyService.create(any(), any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/retention-policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(policyId.toString()))
                .andExpect(jsonPath("$.status").value("Draft"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GOV-001: MANAGER can create retention policy → 201")
    void create_asManager_returns201() throws Exception {
        when(policyService.create(any(), any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/retention-policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GOV-001: ANALYST cannot create retention policy → 403")
    void create_asAnalyst_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/retention-policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    @DisplayName("GOV-001: AUDITOR cannot create retention policy → 403")
    void create_asAuditor_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/retention-policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GOV-001: Unauthenticated request → 401")
    void create_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/retention-policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-001: Missing policy name → 400")
    void create_missingName_returns400() throws Exception {
        validRequest.setName("");
        mockMvc.perform(post("/api/v1/retention-policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-002: TIP_ADMIN can activate policy → 200")
    void activate_asAdmin_returns200() throws Exception {
        sampleResponse.setStatus(PolicyStatus.Active);
        when(policyService.activate(eq(policyId), any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/retention-policies/{id}/activate", policyId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Active"));
    }

    @Test
    @WithMockUser(roles = "SR_ANALYST")
    @DisplayName("GOV-002: SR_ANALYST cannot activate policy → 403")
    void activate_asSrAnalyst_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/retention-policies/{id}/activate", policyId).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    @DisplayName("GOV-003: MANAGER can retire policy → 200")
    void retire_asManager_returns200() throws Exception {
        sampleResponse.setStatus(PolicyStatus.Retired);
        when(policyService.retire(eq(policyId), any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/retention-policies/{id}/retire", policyId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Retired"));
    }

    @Test
    @WithMockUser(roles = "SR_ANALYST")
    @DisplayName("GOV-004: SR_ANALYST can view policy → 200")
    void getById_asSrAnalyst_returns200() throws Exception {
        when(policyService.getById(policyId)).thenReturn(sampleResponse);
        mockMvc.perform(get("/api/v1/retention-policies/{id}", policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(policyId.toString()));
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    @DisplayName("GOV-004: Basic ANALYST cannot view policy → 403")
    void getById_asAnalyst_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/retention-policies/{id}", policyId))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TIP_ADMIN")
    @DisplayName("GOV-004: List all policies → 200 array")
    void getAll_asAdmin_returns200() throws Exception {
        when(policyService.getAll()).thenReturn(List.of(sampleResponse));
        mockMvc.perform(get("/api/v1/retention-policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Case File Retention"));
    }
}
