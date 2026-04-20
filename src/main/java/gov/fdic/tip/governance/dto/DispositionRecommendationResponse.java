package gov.fdic.tip.governance.dto;
import gov.fdic.tip.governance.enums.DispositionRecommendation;
import lombok.*;
import java.util.UUID;
@Data @AllArgsConstructor
public class DispositionRecommendationResponse {
    private UUID governedItemId;
    private DispositionRecommendation recommendation;
    private String rationale;
}
