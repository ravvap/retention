package gov.fdic.tip.governance.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Getter
public class TipUserDetails implements UserDetails {
    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;

    public TipUserDetails(String username, String password, List<String> roles) {
        this.username = username;
        this.password = password;
        this.authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .map(a -> (GrantedAuthority) a)
                .toList();
    }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isAccountNonLocked()     { return true; }
    @Override public boolean isCredentialsNonExpired(){ return true; }
    @Override public boolean isEnabled()              { return true; }
}
