package gov.fdic.tip.governance;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
    info = @Info(
        title = "TIP Governance API",
        version = "1.0",
        description = "FDIC TIP Governance Service — GOV-001 through GOV-019. CONTROLLED // FDIC INTERNAL ONLY"
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class TipGovernanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TipGovernanceApplication.class, args);
    }
}
