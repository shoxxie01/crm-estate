package pl.delta.crm.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.delta.crm.agency.Agency;
import pl.delta.crm.agency.AgencyRepository;
import pl.delta.crm.auth.dto.AuthResponse;
import pl.delta.crm.auth.dto.LoginRequest;
import pl.delta.crm.auth.dto.RegisterRequest;
import pl.delta.crm.auth.dto.UserDto;
import pl.delta.crm.error.EmailAlreadyUsedException;
import pl.delta.crm.security.AppUserPrincipal;
import pl.delta.crm.security.JwtService;
import pl.delta.crm.user.Role;
import pl.delta.crm.user.User;
import pl.delta.crm.user.UserRepository;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository users;
    private final AgencyRepository agencies;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users,
                       AgencyRepository agencies,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.users = users;
        this.agencies = agencies;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());

        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        // Rejestracja zakłada nowe biuro. Dołączanie kolejnych agentów do
        // istniejącego biura to osobna ścieżka (zaproszenie), nie ta.
        Agency agency = agencies.save(new Agency(request.agencyName().trim(), email));

        User user = new User(
                email,
                passwordEncoder.encode(request.password()),
                request.firstName().trim(),
                request.lastName().trim(),
                agency,
                // Osoba zakładająca konto biura jest jego administratorem.
                Role.ADMIN
        );

        User saved = users.save(user);
        return new AuthResponse(jwtService.issue(saved), UserDto.from(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Rzuca BadCredentialsException zarówno przy złym haśle, jak i nieznanym
        // e-mailu — celowo nie rozróżniamy, żeby nie dało się enumerować kont.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalize(request.email()), request.password()));

        User user = ((AppUserPrincipal) authentication.getPrincipal()).user();
        return new AuthResponse(jwtService.issue(user), UserDto.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
