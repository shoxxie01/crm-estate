# Delta CRM — frontend

React 19 + TypeScript + Vite + Tailwind v4. Styl: **Nordic Clean** — jasny,
gęsty, narzędziowy; separacja przez hairline 1px, nie przez cień.

## Uruchomienie

```bash
npm install
npm run dev     # http://localhost:5173
npm run build   # tsc -b && vite build -> dist/
```

Dev server proxuje `/api` na `http://localhost:8080` (Spring Boot).

## Logowanie

Domyślnie frontend woła prawdziwe `/api/auth/**` — uruchom backend
(patrz [README w rootcie](../README.md)) i załóż konto przez `/rejestracja`.

Do pracy nad samym frontem bez backendu: `VITE_USE_MOCK_AUTH=true`. Wtedy
logowanie przechodzi dla dowolnego poprawnego e-maila i **hasła ≥ 8 znaków**,
a rejestracja na adres `@example.com` symuluje konflikt 409 — żeby dało się
obejrzeć obsługę błędów pól.

## Design tokeny

Wszystkie kolory, promienie i typografia siedzą w bloku `@theme` w
`src/index.css`. To jedyne miejsce do zmiany — komponenty używają wyłącznie
nazw semantycznych (`bg-surface`, `text-ink-muted`, `border-line`).

| Token | Zastosowanie |
|---|---|
| `canvas` / `surface` / `subtle` | plan strony / karty / wypełnienia i hover |
| `line` / `line-strong` | hairline 1px / dzielniki i baseline |
| `ink` / `ink-secondary` / `ink-muted` | tekst podstawowy / opisy / osie i placeholdery |
| `accent` | jedyny kolor akcji (granat `#2F5FE0`) |
| `good` `warning` `serious` `critical` | statusy — zarezerwowane, nigdy jako kolor serii |
| `series-1` `series-2` | paleta kategorialna wykresów |
| `seq-1` … `seq-5` | ramp sekwencyjny (jeden odcień, więcej = ciemniej) |

### Kolory wykresów

Paleta jest **zwalidowana**, nie dobrana na oko — na tle `#FFFFFF`, pod kątem
pasma jasności, progu chromy, rozróżnialności przy CVD (daltonizm) i kontrastu:

- kategorialna `#2a78d6` / `#eb6834` — najgorsza para: ΔE 24,7 (protan), 33,6 (norma)
- sekwencyjna `#86b6ef → #104281` — monotoniczna, wszystkie odstępy ΔL ≥ 0,06

Zasady, które trzeba utrzymać przy dokładaniu wykresów:

- nigdy dwie osie Y na jednym wykresie,
- legenda zawsze przy ≥ 2 seriach; przy 1 serii tytuł wystarcza,
- etykiety bezpośrednie wybiórczo (szczyt, koniec) — nigdy liczba nad każdym słupkiem,
- kolor statusu zawsze w parze z ikoną i etykietą,
- każdy wykres ma alternatywę tabelaryczną albo widoczne wartości.

## Struktura

```
src/
  api/         klient HTTP + warstwa auth (mock ↔ Spring Boot)
  auth/        AuthContext, ProtectedRoute
  components/
    ui/        Button, Input, Card, Badge, Logo
    layout/    AppShell, Sidebar, Topbar, definicja nawigacji
    charts/    StatTile, Sparkline, StackedBarChart, RankedBarChart
  data/        dane demo dashboardu (kształt = przyszła odpowiedź API)
  pages/       Dashboard, Placeholder, auth/{Login,Register,AuthLayout}
```

## Kontrakt z backendem

Frontend oczekuje od Spring Boota:

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `POST /api/auth/login` | `{ email, password }` | `{ token, user }` |
| `POST /api/auth/register` | `{ firstName, lastName, email, password, agencyName }` | `{ token, user }` |
| `GET /api/auth/me` | — | `User` |

`User`: `{ id, email, firstName, lastName, role: "AGENT"｜"MANAGER"｜"ADMIN", agencyName }`

Błędy: `ProblemDetail` (RFC 7807). Pole `detail` trafia do komunikatu ogólnego,
opcjonalna mapa `errors: { pole: komunikat }` — pod konkretne pola formularza.
Token leci w nagłówku `Authorization: Bearer <token>`.
