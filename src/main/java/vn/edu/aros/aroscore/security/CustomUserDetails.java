package vn.edu.aros.aroscore.security;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import vn.edu.aros.aroscore.entity.Account;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long accountId;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    // Build từ Account Entity
    public static CustomUserDetails build(Account account) {
        // Lấy Role từ bảng Account (Ví dụ: ROLE_STUDENT, ROLE_LECTURER)
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + account.getRole().name());

        return new CustomUserDetails(
                account.getId(),
                account.getUsername(),
                account.getPassword(),
                Collections.singletonList(authority)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}