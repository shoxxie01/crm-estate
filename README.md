# Delta CRM

CRM dla biura nieruchomości. Moduły: dashboard, kalendarz, nieruchomości,
klienci (właściciele powierzający sprzedaż lub najem), umowy, eksport na portale
ogłoszeniowe.

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

## Nieruchomości

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `POST /api/properties` | `CreatePropertyRequest` | `201` + pełna oferta |
| `GET /api/properties` | — | `200` + strona `PropertySummary` |
| `GET /api/properties/{id}` | — | `200` + pełna oferta |
| `PUT /api/properties/{id}` | `CreatePropertyRequest` | `200` + zaktualizowana oferta |
| `DELETE /api/properties/{id}` | — | `204` |
| `GET /api/properties/dictionaries` | — | `200` + wszystkie słowniki formularza |

Filtry listy: `?status=`, `?type=`, `?transaction=`, plus standardowe `page`,
`size`, `sort` (domyślnie `createdAt,desc`).

**Numer oferty nadaje serwer** w formacie `RRRR/MM/NNN`, osobno dla każdego
biura — nie ma go w `CreatePropertyRequest`. Portal rozpoznaje po nim ogłoszenie
przy kolejnych wysyłkach, więc musi być stabilny, ale nie ma powodu, by wpisywał
go człowiek.

### Co jest wymagane, a co dopiero do publikacji

Do zapisu oferty wystarczy: rodzaj, transakcja, rynek, tytuł, opis, cena,
powierzchnia oraz województwo i miejscowość. Powiat, gmina i dzielnica są
opcjonalne — agent często zakłada ofertę z telefonu od właściciela i uzupełnia
resztę po oględzinach, a blokowanie zapisu wypychałoby takie oferty do notatnika.

Kompletności pod kątem portalu pilnuje `Property.readyForExport()`, które
sprawdza m.in. powiat (Otodom wymaga pary województwo + powiat), liczbę pokoi
dla mieszkania i domu, co najmniej jedno zdjęcie oraz świadectwo energetyczne.
Warunki zależą od typu: świadectwa energetycznego **nie wymagają** działka,
garaż i pokój (`PropertyType.requiresEnergyCertificate()`) — dla nich brak
świadectwa nie blokuje publikacji. Lista ofert pokazuje ten stan w kolumnie
„Eksport".

**Zakres widoczności bierze się z tokenu.** Identyfikator biura nie jest
parametrem żądania i nie da się go podmienić — repozytorium nie ma ani jednej
metody potrafiącej zwrócić ofertę bez podania agencji, łącznie z `findById`.
Oferta obcego biura daje `404`, nie `403`, żeby po kodzie odpowiedzi nie dało
się sprawdzać, co ma konkurencja.

### Skąd wziął się zestaw pól

Z wymagań importu portali ogłoszeniowych — punktem odniesienia jest
[specyfikacja Otodom Import](https://cdn.prod.website-files.com/61a78ba1bfb5df5455656f40/6638c399538563422cecdd31_otoDom_Import_170130.pdf)
(format XML wysyłany przez FTP, paczki przetwarzane co godzinę).

Zasada, na której stoi cały model: **w bazie nie ma ani jednego kodu
portalowego**. Portale opisują ten sam atrybut różnymi słownikami, a Otodom
używa różnych słowników dla różnych typów obiektu — ta sama liczba `2` znaczy
„dom wolnostojący" przy mieszkaniu, „szeregowiec" przy domu i „w bloku" przy
lokalu użytkowym. Do tego ich dokumentacja zaleca częste odświeżanie słowników.
Trzymamy więc własne, jednoznaczne nazwy (`pl.delta.crm.property.dictionary`),
a tłumaczenie na kody portalu będzie należało do modułu eksportu.

Świadectwo charakterystyki energetycznej jest w modelu, mimo że specyfikacja
Otodom z 2017 r. go nie zna — obowiązek podania wskaźnika EP w ogłoszeniu
wynika z ustawy i obowiązuje od 28.04.2023 niezależnie od formatu XML.

Słowniki mają polskie etykiety po stronie backendu i wychodzą jednym
endpointem, więc front nie powiela dwudziestu kilku enumów w TypeScripcie.

**Payload tokenu:** `iss`, `sub` (UUID użytkownika), `email`, `role`, `iat`, `exp`.
Podmiotem jest id, nie e-mail — token przeżyje zmianę adresu.

### Pola zależne od typu obiektu

Formularz oferty pokazuje inny zestaw pól i cech dla każdego z siedmiu rodzajów
obiektu (mieszkanie, dom, działka, lokal użytkowy, hala/magazyn,
garaż/miejsce postojowe, pokój). Garaż nie pyta o liczbę pokoi ani łazienek,
działka nie ma sekcji budynku ani świadectwa energetycznego, hala ma wysokość
i rampę zamiast pięter. Ta sama widoczność obowiązuje w formularzu i w widoku
szczegółów.

Pola specyficzne dla typu (migracja `V9`):

- **hala/magazyn** — moc przyłącza [kW], nośność posadzki [t/m²], liczba bram/doków,
- **garaż** — typ (`GarageType`: murowany, blaszany, podziemny, w hali, naziemny),
- **pokój** — dla ilu osób, dostęp do łazienki (`RoomBathroom`: osobna/współdzielona).

**Cena za m²** (`price_per_m2`, migracja `V8`) liczona jest dwukierunkowo z ceny
i powierzchni — wpisanie jednej wartości wylicza drugą (cena z ceny za m² jest
zaokrąglana do pełnych złotych). Dla garażu i pokoju pole jest ukryte, bo wycena
nie jest metrażowa.

**Cechy** (sekcja „Cechy") są filtrowane per pozycja: każda cecha (`Feature`)
zna typy obiektu, dla których ma sens, więc mieszkanie nie widzi „studni" ani
„ogrodzenia", a garaż „pralki". Kategoria bez pasujących cech w ogóle się nie
pokazuje.

### Formularze i walidacja

Walidacja działa po obu stronach — Bean Validation na backendzie i lustrzane
reguły w formularzu, żeby błąd był widoczny od razu, zanim żądanie poleci:

- pola liczbowe przyjmują tylko cyfry; kwoty i powierzchnie maks. 2 miejsca po
  przecinku; realne zakresy (piętro do 160, rok budowy do bieżącego + 10),
- kod pocztowy w formacie `00-000`, telefon wyłącznie `+000 000 000 000`,
- nazwy własne (miejscowość, ulica, numer budynku…) są automatycznie kapitalizowane,
- pola wymagane mają gwiazdkę `*`, a po nieudanym zapisie strona przewija się do
  pierwszego błędnego pola i ustawia na nim kursor.

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

## Klienci

Klient to **właściciel powierzający ofertę** — na tym etapie wyłącznie osoba
fizyczna. Rozróżnienie sprzedający / wynajmujący **nie jest polem klienta**:
wynika z typu transakcji ofert do niego przypisanych (`properties.owner_client_id`,
migracja `V7`). Ten sam właściciel może jednocześnie coś sprzedawać i coś
wynajmować, więc rola na sztywno przy kliencie byłaby fałszem.

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `POST /api/clients` | `CreateClientRequest` | `201` + `ClientResponse` |
| `GET /api/clients` | — | `200` + strona `ClientSummary` |
| `GET /api/clients/{id}` | — | `200` + `ClientResponse` |
| `PUT /api/clients/{id}` | `CreateClientRequest` | `200` + zaktualizowany klient |
| `DELETE /api/clients/{id}` | — | `204` |
| `PUT /api/clients/{clientId}/properties/{propertyId}` | — | `200` — przypisz ofertę |
| `DELETE /api/clients/{clientId}/properties/{propertyId}` | — | `204` — odłącz ofertę |

Filtry listy: `?status=`, `?search=` (imię, nazwisko, telefon, e-mail) plus
standardowe `page`, `size`, `sort`.

Wymagany jest **telefon albo e-mail** — kontakt bez żadnego z nich nie ma sensu.
Usunięcie klienta **odłącza** jego oferty (ustawia właściciela na `null`), a nie
kasuje ich. Zakres, jak wszędzie, bierze się z tokenu — nie ma metody
repozytorium zwracającej klienta bez podania biura.

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
podrobionego tokenu → duplikat e-maila → walidacja pól. `PropertyModuleTest`
sprawdza zapis i odczyt oferty (w tym wyliczaną cenę za m²) oraz izolację
między biurami.

## Przed produkcją

- [ ] upload zdjęć oferty (model `PropertyMedia` gotowy; planowany zapis na dysku
      serwera + ścieżka w bazie) — bez zdjęcia `readyForExport()` nie przepuści oferty
- [ ] refresh tokeny albo krótszy TTL z odświeżaniem
- [ ] rate limiting na `/api/auth/login`
- [ ] `delta.cors.allowed-origins` na prawdziwą domenę
- [ ] hasło do bazy z sekretów, nie z domyślnej wartości `delta`
