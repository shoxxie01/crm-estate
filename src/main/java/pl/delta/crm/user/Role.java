package pl.delta.crm.user;

/**
 * Rola w obrębie biura. Pierwszy użytkownik zakładający konto biura dostaje
 * ADMIN; kolejnych zaprasza się już z konkretną rolą.
 */
public enum Role {
    AGENT,
    MANAGER,
    ADMIN
}
