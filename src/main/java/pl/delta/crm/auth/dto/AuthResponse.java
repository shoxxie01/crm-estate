package pl.delta.crm.auth.dto;

public record AuthResponse(String token, UserDto user) {
}
