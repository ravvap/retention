package gov.fdic.tip.governance.security;

import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;

/**
 * In-memory user store for demo purposes.
 * Replace with database-backed implementation for production.
 */
@Service
public class TipUserDetailsService implements UserDetailsService {

    // username -> {password, roles[]}
    private static final Map<String, TipUserDetails> USERS = Map.of(
        "admin",      new TipUserDetails("admin",      "{noop}admin123",    List.of("ROLE_TIP_ADMIN","ROLE_MANAGER")),
        "analyst",    new TipUserDetails("analyst",    "{noop}analyst123",  List.of("ROLE_SR_ANALYST","ROLE_ANALYST")),
        "compliance", new TipUserDetails("compliance", "{noop}comply123",   List.of("ROLE_COMPLIANCE_ANALYST")),
        "auditor",    new TipUserDetails("auditor",    "{noop}audit123",    List.of("ROLE_AUDITOR")),
        "scheduler",  new TipUserDetails("scheduler",  "{noop}sched123",    List.of("ROLE_SCHEDULER"))
    );

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        TipUserDetails user = USERS.get(username);
        if (user == null) throw new UsernameNotFoundException("User not found: " + username);
        return user;
    }
}
