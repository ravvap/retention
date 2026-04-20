package gov.fdic.tip.governance.dto;
import lombok.*;
import java.util.List;
@Data @AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private List<String> roles;
}
