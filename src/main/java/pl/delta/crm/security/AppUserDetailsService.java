package pl.delta.crm.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import pl.delta.crm.user.UserRepository;

import java.util.Locale;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public AppUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmail(email.toLowerCase(Locale.ROOT))
                .map(AppUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("Nie znaleziono użytkownika."));
    }
}
