# Delta CRM

CRM dla biura nieruchomości. Moduły: dashboard, kalendarz, nieruchomości,
klienci i ich predyspozycje, umowy, eksport na portale ogłoszeniowe.

- **Backend** — Java 25 + Spring Boot 4.0.7 (`src/`)
- **Frontend** — React 19 + TypeScript + Vite + Tailwind v4 (`frontend/`),
  szata graficzna „Nordic Clean" ([szczegóły](frontend/README.md))

## Wymagania

- JDK 25
- Maven 3.9+
- Node 20+
- Docker (baza i testy)

## Uruchomienie

### 1. Baza

```bash
docker compose up -d
```

PostgreSQL 17 na **porcie 5434** (5432 i 5433 bywają zajęte przez inne
instancje — patrz komentarz w `compose.yaml`). Dane w wolumenie
`crm_postgres-data`, więc przeżywają `docker compose down`.

### 2. Backend

Aplikacja **nie wstanie bez sekretu do podpisywania tokenów** — to celowe,
żeby nigdy nie podpisywać ich wartością zaszytą w repo.

```powershell
# PowerShell
$env:DELTA_JWT_SECRET = "dowolny-losowy-ciag-min-32-znaki"
mvn spring-boot:run
```

```bash
# bash
export DELTA_JWT_SECRET="dowolny-losowy-ciag-min-32-znaki"
mvn spring-boot:run
```

Startuje na `http://localhost:8080`. Flyway sam zaaplikuje migracje przy starcie.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev     # http://localhost:5173, proxy /api -> :8080
```

## Uwierzytelnianie

JWT (HS256) w nagłówku `Authorization: Bearer <token>`. API jest bezstanowe —
brak sesji po stronie serwera, brak CSRF.

| Endpoint | Dostęp | Body | Odpowiedź |
|---|---|---|---|
| `POST /api/auth/register` | publiczny | `{ firstName, lastName, email, password, agencyName }` | `201` + `{ token, user }` |
| `POST /api/auth/login` | publiczny | `{ email, password }` | `200` + `{ token, user }` |
| `GET /api/auth/me` | token | — | `200` + `User` |

Każdy inny endpoint pod `/api/**` wymaga tokenu.

**Payload tokenu:** `iss`, `sub` (UUID użytkownika), `email`, `role`, `iat`, `exp`.
Podmiotem jest id, nie e-mail — token przeżyje zmianę adresu.

### Błędy

Jednolity `ProblemDetail` (RFC 7807). Pole `detail` to komunikat ogólny,
opcjonalna mapa `errors` mapuje nazwę pola na komunikat:

```json
{
  "status": 400,
  "title": "Błąd walidacji",
  "detail": "Popraw zaznaczone pola.",
  "errors": { "password": "Hasło musi mieć co najmniej 8 znaków." }
}
```

### Decyzje, które warto znać

- **Hasła:** BCrypt. Encja trzyma wyłącznie hash, `UserDto` nie ma pola z hasłem.
- **Enumeracja kont:** złe hasło i nieistniejący e-mail dają identyczne `401`
  z tym samym komunikatem — inaczej dałoby się sprawdzać, kto ma konto.
- **E-mail** jest normalizowany do małych liter przy zapisie i przy logowaniu.
- **Pierwszy użytkownik** zakładający konto biura dostaje rolę `ADMIN`.
- **Sekret JWT** nie ma wartości domyślnej — brak `DELTA_JWT_SECRET` to błąd startu,
  a nie ciche użycie czegoś słabego. Minimum 32 bajty (wymóg HS256).

## Baza danych

PostgreSQL 17 — ten sam silnik na dev i w testach, żeby nie było klasy błędów
„działa lokalnie, wysypuje się na produkcji".

**Schemat należy do Flyway**, nie do Hibernate. `ddl-auto` jest ustawione na
`validate`, więc rozjazd między encją a migracją wywala aplikację przy starcie,
zamiast po cichu dopisywać kolumny. Nową zmianę schematu dodaje się jako kolejny
plik w `src/main/resources/db/migration/` (`V2__opis.sql`) — nigdy przez edycję
już zaaplikowanej migracji.

## Konfiguracja

| Zmienna / property | Domyślnie | Opis |
|---|---|---|
| `DELTA_JWT_SECRET` | — (wymagane) | klucz HMAC, min. 32 bajty |
| `DELTA_DB_URL` | `jdbc:postgresql://localhost:5434/delta_crm` | JDBC URL |
| `DELTA_DB_USER` / `DELTA_DB_PASSWORD` | `delta` / `delta` | dane logowania do bazy |
| `delta.jwt.ttl` | `12h` | czas życia tokenu |
| `delta.jwt.issuer` | `delta-crm` | claim `iss`, weryfikowany przy odczycie |
| `delta.cors.allowed-origins` | `http://localhost:5173` | dozwolone originy dla `/api/**` |
| `server.port` | `8080` | port HTTP |

## Testy

```bash
mvn test     # wymaga działającego Dockera
```

Testy podnoszą własny kontener Postgresa (Testcontainers) i przepuszczają przez
niego migracje Flyway — weryfikują więc także sam schemat, nie tylko kod.
`docker compose` nie musi przy tym działać; to osobny, jednorazowy kontener.

`AuthFlowTest` pokrywa pełną ścieżkę: rejestracja → hash hasła → logowanie
(w tym niewrażliwość na wielkość liter) → `/me` z tokenem i bez → odrzucenie
podrobionego tokenu → duplikat e-maila → walidacja pól.

## Przed produkcją

- [ ] refresh tokeny albo krótszy TTL z odświeżaniem
- [ ] rate limiting na `/api/auth/login`
- [ ] `delta.cors.allowed-origins` na prawdziwą domenę
- [ ] hasło do bazy z sekretów, nie z domyślnej wartości `delta`
