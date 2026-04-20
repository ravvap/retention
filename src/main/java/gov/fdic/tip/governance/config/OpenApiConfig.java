package gov.fdic.tip.governance.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tipGovernanceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TIP Governance API")
                        .version("1.0.0")
                        .description("""
                                FDIC TIP Governance Service — GOV-001 through GOV-019.
                                
                                **CONTROLLED // FDIC INTERNAL ONLY**
                                
                                Covers six epics:
                                - Epic 1: Retention Policy Management (GOV-001 to GOV-004)
                                - Epic 2: Governed Item Registration (GOV-005 to GOV-007)
                                - Epic 3: Legal Hold Management (GOV-008 to GOV-010, GOV-019)
                                - Epic 4: Disposition Evaluation (GOV-011)
                                - Epic 5: Audit Logging (GOV-012 to GOV-013)
                                - Epic 6: Batch and Execution (GOV-014 to GOV-018)
                                """)
                        .contact(new Contact()
                                .name("TIP Platform Team")
                                .email("tip-platform@fdic.gov")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://tip-governance-dev.fdic.gov").description("Dev Environment")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from POST /api/v1/auth/login")));
    }
}
