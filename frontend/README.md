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
| `event-*` / `event-*-tint` | rodzaj terminu w kalendarzu (9 kategorii) |
| `seq-1` … `seq-5` | ramp sekwencyjny (jeden odcień, więcej = ciemniej) |

### Kolory wykresów

Paleta jest **zwalidowana**, nie dobrana na oko — na tle `#FFFFFF`, pod kątem
pasma jasności, progu chromy, rozróżnialności przy CVD (daltonizm) i kontrastu:

- kategorialna `#2a78d6` / `#eb6834` — najgorsza para: ΔE 24,7 (protan), 33,6 (norma)
- sekwencyjna `#86b6ef → #104281` — monotoniczna, wszystkie odstępy ΔL ≥ 0,06

### Kolory kalendarza

Rodzaj terminu ma własną paletę (`--color-event-*`), bo dziewięciu kategorii nie
udźwignie paleta kategorialna wykresów — ta ma zwalidowane dwa kolory. Przy
dziewięciu odcień przestaje wystarczać: w symulacji protanopii najgorsza para
dobrana samym odcieniem wychodziła ΔE 0,3, czyli praktycznie ten sam kolor.

Dlatego paleta **rozstrzela też jasność** — L* od 29 (`event-task`) do 56
(`event-valuation`) — i to ona rozdziela odcienie, które przy daltonizmie się
zlewają. Wynik: najgorsza para to ΔE 11,3 (deuteranopia), 11,4 (protanopia)
i 27,3 przy normalnym widzeniu.

Warunki brzegowe: każdy kolor wiodący ma ≥ 3,5:1 kontrastu na białym (jako
element nietekstowy — obramowanie, ikona, kropka), a tekst na kafelku zostaje
`ink`, więc jego czytelność nie zależy od rodzaju (≥ 12,7:1 na każdej tincie).

Kolor **nigdy nie jest jedynym nośnikiem rodzaju**: na kafelku stoi obok niego
ikona, w panelu i na osi czasu także etykieta, a nad siatką jest legenda.

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
    calendar/  siatka miesiąc/tydzień/dzień, panel terminu, formularz,
               oś czasu wstawiana na karty oferty i klienta
    properties/ lista, formularz, karta oferty oraz dwie galerie:
               PropertyGallery (istniejąca oferta — upload, zamiana pliku,
               kolejność, podpisy; tryb readOnly na karcie) i
               PropertyGalleryDraft (nowa oferta — pliki czekają w pamięci)
```

## Kontrakt z backendem

Frontend oczekuje od Spring Boota:

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `POST /api/auth/login` | `{ email, password }` | `{ token, user }` |
| `POST /api/auth/register` | `{ firstName, lastName, email, password, agencyName }` | `{ token, user }` |
| `GET /api/auth/me` | — | `User` |
| `GET /api/properties` | — | strona `PropertySummary` |
| `POST /api/properties` | `CreatePropertyPayload` | pełna oferta |
| `GET /api/properties/dictionaries` | — | słowniki formularza |
| `POST /api/properties/{id}/media` | multipart `files` | dodane zdjęcia + podpisane linki |
| `PUT /api/properties/{id}/media/{mediaId}/file` | multipart `file` | podmiana pliku |
| `PUT /api/properties/{id}/media/order` | `{ mediaIds }` | kolejność galerii |
| `PATCH /api/properties/{id}/media/{mediaId}` | `{ caption }` | podpis zdjęcia |
| `DELETE /api/properties/{id}/media/{mediaId}` | — | `204` |
| `GET /api/calendar/events?from=&to=` | — | lista terminów w zakresie |
| `POST /api/calendar/events` | `CreateEventPayload` | pełny termin + kolizje |
| `PUT /api/calendar/events/{id}/status` | `{ status, outcome?, outcomeNote? }` | domknięcie terminu |
| `GET /api/properties/{id}/events`, `GET /api/clients/{id}/events` | — | terminy na kartach |

`User`: `{ id, email, firstName, lastName, role: "AGENT"｜"MANAGER"｜"ADMIN", agencyName }`

**Słowniki nie są przepisywane do TypeScriptu.** Wartości takie jak rodzaj
nieruchomości, forma własności czy materiał budowy to po stronie frontu zwykłe
stringi, a listy wyboru pochodzą z `/api/properties/dictionaries` — razem
z polskimi etykietami. Powielenie tych dwudziestu kilku enumów oznaczałoby dwa
źródła prawdy rozjeżdżające się przy każdej zmianie w backendzie.

**Zdjęcia nie przechodzą przez backend.** `url` i `thumbnailUrl` są podpisanymi
linkami prosto do storage'u i wygasają po 15 minutach — nie wolno ich zapisywać
ani cache'ować dłużej niż trwa widok. Dlatego też żądania galerii idą jako
`FormData`, a klient HTTP **nie ustawia wtedy `Content-Type`**: nagłówek z
granicą multiparta dokłada przeglądarka i wpisanie go ręcznie psuje żądanie.

**Zarządzanie galerią jest wyłącznie w formularzu oferty.** Karta oferty pokazuje
zdjęcia w trybie tylko do odczytu (`PropertyGallery` z `readOnly`) — kartę
otwiera się, żeby ofertę obejrzeć, a przypadkowego skasowania zdjęcia przy
przeglądaniu nie da się cofnąć.

Galerie są dwie, bo mają różne dane:

- **`PropertyGallery`** — oferta już istnieje. Każda operacja to osobne żądanie
  i działa od razu, niezależnie od przycisku „Zapisz ofertę".
- **`PropertyGalleryDraft`** — oferta dopiero powstaje. Zdjęcie trzyma klucz obcy
  do oferty, więc przed jej zapisem nie ma czego nim obwiesić: pliki czekają
  w pamięci (podgląd przez `URL.createObjectURL`, adresy zwalniane przy zmianie),
  a lecą na serwer zaraz po tym, jak zapis nada ofercie identyfikator. Jeśli
  wysyłka zdjęć padnie, oferta zostaje zapisana, a użytkownik ląduje w jej
  edycji z komunikatem — zamiast stracić jedno i drugie.

Lista ofert pokazuje miniaturę zdjęcia głównego (`PropertySummary.coverThumbnailUrl`,
`null` przy ofercie bez zdjęć — wtedy leci placeholder tego samego rozmiaru, żeby
wiersze się nie rozjeżdżały). Miniatury dla całej strony listy backend podpisuje
jednym zapytaniem, więc wiersz nie dociąga kolekcji zdjęć swojej oferty.

Kalendarz wysyła i odbiera czas jako ISO-8601 w UTC; siatka dni liczona jest
w czasie lokalnym przeglądarki. Kafelek terminu koduje dwie rzeczy naraz, więc
mają rozdzielone kanały: **rodzaj niesie kolor** (tło, obramowanie, ikona,
kropka w legendzie), a **status — sposób podania** (pogrubienie przy
potwierdzonym, wyblaknięcie przy odbytym, przekreślenie przy odwołanym,
czerwona obwódka przy nieobecności). Paleta rodzajów: patrz „Kolory kalendarza".

Błędy: `ProblemDetail` (RFC 7807). Pole `detail` trafia do komunikatu ogólnego,
opcjonalna mapa `errors: { pole: komunikat }` — pod konkretne pola formularza.
Token leci w nagłówku `Authorization: Bearer <token>`.
