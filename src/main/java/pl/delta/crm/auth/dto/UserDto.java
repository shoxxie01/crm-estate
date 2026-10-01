package pl.delta.crm.auth.dto;

import pl.delta.crm.user.Role;
import pl.delta.crm.user.User;

/** Publiczna reprezentacja użytkownika. Bez hasła. Kształt zgodny z typem `User` na froncie. */
public record UserDto(
        String id,
        String email,
        String firstName,
        String lastName,
        Role role,
        String agencyName
) {
    public static UserDto from(User user) {
        return new UserDto(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getAgencyName()
        );
    }
}
