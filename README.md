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

Podnosi dwie usługi:

- **PostgreSQL 17** na porcie **5434** (5432 i 5433 bywają zajęte przez inne
  instancje — patrz komentarz w `compose.yaml`),
- **MinIO** — storage zdjęć: API S3 na **9000**, konsola na **9001**
  (`delta` / `delta12345`).

Dane w wolumenach `crm_postgres-data` i `crm_minio-data`, więc przeżywają
`docker compose down`. Bucket `delta-crm-media` zakłada sama aplikacja przy
starcie. Bez MinIO backend nadal wstaje — nie działa tylko galeria.

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
| `GET /api/properties/{id}/media` | — | `200` + galeria oferty |
| `POST /api/properties/{id}/media` | multipart `files` | `201` + dodane materiały |
| `PUT /api/properties/{id}/media/{mediaId}/file` | multipart `file` | `200` — podmiana pliku |
| `PUT /api/properties/{id}/media/order` | `{ mediaIds }` | `200` — kolejność galerii |
| `PATCH /api/properties/{id}/media/{mediaId}` | `{ caption }` | `200` — podpis zdjęcia |
| `DELETE /api/properties/{id}/media/{mediaId}` | — | `204` |

Filtry listy: `?status=`, `?type=`, `?transaction=`, plus standardowe `page`,
`size`, `sort` (domyślnie `createdAt,desc`).

Ofertę **kasuje się też z listy**, nie tylko z jej karty — pomyłkę przy
zakładaniu prostuje się wtedy bez wchodzenia w szczegóły. Kosz w wierszu otwiera
**okno potwierdzenia** („Czy na pewno chcesz usunąć tę ofertę?" wraz z numerem
i tytułem), a nie dwa przyciski w komórce: wiersze są niskie i gęsto obok siebie,
więc potwierdzenie w miejscu wypadałoby dokładnie tam, gdzie przed chwilą był
kursor. Wiersz otwiera kartę oferty, więc kliknięcie kosza zatrzymuje się na
swojej komórce. Kasowanie jest **nieodwracalne** i zabiera też zdjęcia — oferty
wycofanej z rynku nie kasuje się, tylko ustawia jej status na `ARCHIVED`.

Pozostałe potwierdzenia (karta oferty, klient, termin, zdjęcie w galerii) są
nadal dwustopniowe i w miejscu — tam operacja dotyczy tego, co użytkownik ma
otwarte przed sobą, a nie jednego z wielu wierszy.

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

### Zdjęcia

Pliki leżą w storage'u obiektowym (**MinIO** w dev, API S3), a nie na dysku
aplikacji ani w bazie. Baza trzyma wyłącznie `storage_key` — klucz obiektu.

Wybór padł na S3 API, a nie na Ceph, bo skala tego nie uzasadnia: 50 zdjęć na
ofertę po ~1,5 MB przy 500 aktywnych ofertach to ok. 35 GB, czyli jeden dysk,
a nie klaster z MON-ami i OSD-ami. Ceph i tak wystawiłby to samo API przez
RadosGW, więc kod byłby identyczny — różnica jest wyłącznie w nakładzie
utrzymania. Ta sama implementacja (`S3MediaStorage`) działa na MinIO w dev i na
prawdziwym S3 na produkcji; zmienia się endpoint w konfiguracji, nie kod.

**Przeglądarka pobiera zdjęcia bezpośrednio ze storage'u** po podpisany link
(15 min, `delta.storage.url-ttl`) — bajty nie przechodzą przez Springa.
To nie jest optymalizacja, tylko warunek działania: token leci w nagłówku
`Authorization`, a `<img src>` nagłówka nie wyśle. Klucz obiektu nie wychodzi
przez API, żeby po jego układzie nie dało się zgadywać cudzych ofert.

Przy wgrywaniu każde zdjęcie jest normalizowane: skala do 1920 px, przekodowanie
na JPEG, miniatura 400 px i **usunięcie EXIF-u**. To ostatnie ma znaczenie
niezależnie od rozmiaru pliku — zdjęcie z telefonu niesie współrzędne GPS, więc
bez tego oferta z włączonym ukrywaniem adresu i tak zdradzałaby położenie.
Format rozpoznajemy po sygnaturze pliku, nie po nagłówku `Content-Type`.

- limit **50 materiałów na ofertę** (Otodom bierze pierwsze 20 wg pozycji),
- pozycja 0 to zdjęcie główne; kasowanie przenumerowuje resztę do ciągu 0..n-1,
- **zamiana pliku** zachowuje pozycję, podpis i identyfikator wpisu — nowy plik
  ląduje pod tym samym kluczem, więc stara wersja znika bezpowrotnie,
- HEIC jest odrzucany z instrukcją, jak przełączyć iPhone'a na JPG — ImageIO go
  nie czyta, a to domyślny format zdjęć z iPhone'a,
- skasowanie **całej oferty** zabiera też jej pliki: wiersze znikają kaskadą
  (`ON DELETE CASCADE` w `V3`), ale kaskada bazy nie wie nic o buckecie,
  więc `PropertyService.delete` czyści go jawnie,
- **zdjęcia można wybrać już przy zakładaniu oferty.** Zdjęcie trzyma klucz obcy
  do oferty, więc przed jej zapisem nie ma czego nim obwiesić — front trzyma
  pliki w pamięci i wysyła je zaraz po tym, jak serwer nada ofercie numer.
  Gdyby ten drugi krok padł, oferta i tak zostaje zapisana, a formularz otwiera
  się ponownie z komunikatem, zamiast gubić jedno i drugie,
- kolejność zapisu jest asymetryczna: przy dodawaniu najpierw plik, potem wiersz;
  przy kasowaniu najpierw wiersz, plik po commicie. Awaria w połowie zostawia
  najwyżej plik bez wiersza (zajęte miejsce), nigdy wiersza bez pliku (dziura
  w galerii i eksport bez czego złożyć paczki).

**Galerią zarządza się wyłącznie z formularza oferty.** Karta oferty pokazuje
zdjęcia w trybie tylko do odczytu — otwiera się ją, żeby ofertę obejrzeć,
a przypadkowego skasowania zdjęcia przy przeglądaniu nie da się cofnąć.

Na karcie galeria stoi w **prawej kolumnie, przyklejona przy przewijaniu** —
zdjęcie zostaje w polu widzenia przy czytaniu parametrów, bo to właśnie z nimi
się je zestawia. Poniżej `lg` układ wraca do jednej kolumny. Kliknięcie zdjęcia
otwiera **podgląd na całym ekranie**: przewijanie strzałkami (na ekranie i na
klawiaturze), zoom kółkiem lub przyciskami z przesuwaniem powiększonego kadru,
pasek miniatur, licznik i podpis. Podgląd renderuje się przez portal do `body`,
bo `position: fixed` wewnątrz przyklejonej kolumny przykleiłby się do niej
zamiast do okna. Wygaśnięcie podpisanego linku (15 min) galeria poznaje po
nieudanym załadowaniu i dociąga świeży komplet adresów — bez tego karta
otwarta dłużej niż kwadrans pokazywałaby puste kadry.

Unikat `(property_id, position)` jest **odroczony do commitu** (migracja `V12`) —
zmiana kolejności przepisuje pozycje wielu wierszom naraz i po drodze przechodzi
przez stan z duplikatem, mimo że stan końcowy jest poprawny.

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
- kod pocztowy w formacie `00-000` — myślnik dopisuje się sam w trakcie pisania,
  więc wbija się pięć cyfr (`lib/postalCode.ts`),
- telefon: kierunkowy `+48` stoi na stałe przy polu, wpisuje się dziewięć cyfr
  (`000 000 000`, spacje dopisuje maska). Numer zagraniczny zaczyna się od `+`
  albo `00` — wtedy prefiks znika, a pole przyjmuje numer z kierunkowym bez
  maski. Wklejony `+48 …` / `0048 …` sam wraca do trybu krajowego
  (`lib/phone.ts`, `PhoneInput`). Na serwer zawsze idzie numer z kierunkowym,
  a `PhoneNumber` i tak dopisuje `48` do gołych dziewięciu cyfr,
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

Klient to **osoba fizyczna po stronie podaży, popytu albo obu naraz**. Role
**nie są polem klienta**: sprzedający / wynajmujący wynika z typu transakcji
ofert do niego przypisanych (`properties.owner_client_id`, migracja `V7`),
a kupujący / najemca — z jego aktywnych poszukiwań (`client_requirements`,
migracja `V13`). Ta sama osoba potrafi sprzedawać kawalerkę i szukać większego
mieszkania, więc rola na sztywno przy kliencie byłaby fałszem.

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
Usunięcie klienta **odłącza** jego oferty (ustawia właściciela na `null`) oraz
jego terminy, a nie kasuje ich — za to jego poszukiwania znikają razem z nim.
Zakres, jak wszędzie, bierze się z tokenu — nie ma metody repozytorium
zwracającej klienta bez podania biura.

### Poszukiwania

Czego klient szuka jako kupujący albo najemca. Osobna encja, a nie pola
klienta: jedna osoba może szukać kilku rzeczy naraz, a każde poszukiwanie ma
własny stan — **aktywne, wstrzymane, zrealizowane, nieaktualne**. Do roli
liczą się tylko aktywne; pozostałe zostają na karcie jako historia.

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `GET /api/clients/{clientId}/requirements` | — | `200` + lista `RequirementResponse` |
| `POST /api/clients/{clientId}/requirements` | `RequirementRequest` | `201` + poszukiwanie |
| `PUT /api/clients/{clientId}/requirements/{id}` | `RequirementRequest` | `200` |
| `PUT /api/clients/{clientId}/requirements/{id}/status` | `{ "status": … }` | `200` — szybka zmiana stanu |
| `DELETE /api/clients/{clientId}/requirements/{id}` | — | `204` |

Karta klienta (`GET /api/clients/{id}`) niesie poszukiwania od razu, a lista
klientów — `buyerCount` / `tenantCount` obok `sellCount` / `rentCount`.

Kryteria:

- **wymagane są tylko transakcja i rodzaj nieruchomości** (jeden lub kilka) —
  reszta to tyle, ile klient zdążył powiedzieć przez telefon,
- lokalizacje jako lista miejscowość + opcjonalna dzielnica, w kolejności
  ważności; duplikaty wypadają przy zapisie,
- zakresy budżetu (przy najmie: czynsz miesięczny), metrażu, pokoi i piętra —
  każda granica z osobna opcjonalna, „od” nie większe niż „do”,
- rynek (pusty = obojętny), „bez ostatniego piętra”, termin zakupu / wprowadzenia,
- finansowanie (gotówka, kredyt, kredyt z decyzją…) **tylko przy kupnie** —
  przy najmie serwis je czyści,
- cechy ze słownika ofert z podziałem na **konieczne i mile widziane**; cecha
  podana w obu zbiorach zostaje konieczna (pilnuje tego też klucz główny tabeli),
- pokój da się wyłącznie najmować — poszukiwanie kupna pokoju jest odrzucane.

Kryteria są zwykłymi kolumnami, a nie JSON-em, bo następny krok — dopasowanie
ofert do poszukiwań — musi dać się napisać w SQL i zaindeksować
(`ix_client_requirements_matching`).

Formularz (`RequirementForm`) jest pod rozmowę telefoniczną: na górze
transakcja, rodzaj, lokalizacja, budżet, metraż i pokoje; rynek, piętro,
finansowanie, cechy i notatka są zwinięte. Pola pokoi i piętra pokazują się
tylko dla rodzajów, dla których mają sens, a cechy — przefiltrowane pod
wybrane rodzaje. Kwoty grupują się spacjami w trakcie pisania (`lib/amount.ts`).

### Zgłoszenia z formularza

Klient zgłasza się sam na publicznej stronie `/zgloszenie/<klucz-biura>` — bez
logowania. Link (do skopiowania w **Zgłoszenia**) biuro wysyła klientom, wstawia
na stronę www albo do ogłoszeń.

Formularz ma **dokładnie dwie ścieżki: „Chcę kupić” i „Chcę sprzedać”** — najem
obsługuje agent przy rozmowie (API odrzuca kryteria najmu z formularza):

- **kupno** — kryteria jak w poszukiwaniu (bez rynku, piętra i cech, o które
  klient i tak by nie umiał odpowiedzieć),
- **sprzedaż** — rodzaj, miejscowość, dzielnica, powierzchnia, pokoje i oczekiwana
  cena (migracja `V15`). Zgłoszenia sprzedaży **nie zamieniamy na ofertę**:
  ogłoszenie wymaga adresu, opisu i zdjęć, których formularz nie zbiera, więc po
  przyjęciu opis nieruchomości trafia do notatki klienta, a ofertę agent zakłada
  po rozmowie lub oględzinach.

Zgłoszenie **nie trafia od razu do klientów**, tylko do skrzynki
(`client_inquiries`, migracja `V14`). Agent:

- **przyjmuje** je — powstaje klient ze źródłem „Strona WWW”; przy kupnie
  z aktywnym poszukiwaniem (wiadomość klienta w jego notatce), przy sprzedaży
  z opisem nieruchomości w notatce klienta,
- **dopina** do istniejącego klienta — skrzynka sama podpowiada osoby z tym
  samym telefonem lub e-mailem; u istniejącego klienta uzupełniamy tylko brakujące
  dane kontaktowe, niczego nie nadpisujemy,
- **odrzuca** — zgłoszenie jest usuwane razem z danymi osobowymi.

Przy zgłoszeniu kupna jeszcze przed przyjęciem widać pasujące oferty (to samo
dopasowanie co niżej, policzone dla niezapisanego poszukiwania).

| Endpoint | Dostęp | Opis |
|---|---|---|
| `GET /api/public/intake/{token}` | publiczny | nazwa biura, treść zgód, słowniki formularza |
| `POST /api/public/intake/{token}` | publiczny | wysłanie zgłoszenia → `204` |
| `GET /api/inquiries?status=NEW\|CONVERTED` | zalogowany | skrzynka z podpowiedzią duplikatów |
| `GET /api/inquiries/count` | zalogowany | licznik nowych do menu bocznego |
| `GET /api/inquiries/{id}/matches` | zalogowany | pasujące oferty przed przyjęciem |
| `POST /api/inquiries/{id}/convert` | zalogowany | `{ "clientId": null }` = nowy klient |
| `DELETE /api/inquiries/{id}` | zalogowany | odrzucenie (usunięcie) |
| `GET /api/inquiries/intake-link` | zalogowany | klucz formularza biura |
| `POST /api/inquiries/intake-link/regenerate` | administrator | nowy klucz, stary od razu przestaje działać |

**RODO:** zgoda na przetwarzanie danych jest obowiązkowa, marketingowa —
opcjonalna. Przy zgłoszeniu zapisujemy moment zgody i **pełną treść klauzuli**,
którą klient widział (a nie numer wersji), więc dowód przetrwa zmianę tekstu.
Treść klauzul jest w jednym miejscu (`IntakeConsent`) i to ją serwer wysyła do
formularza — front nie ma własnej kopii. Po przyjęciu na karcie klienta ląduje
notatka z datą zgłoszenia i informacją o zgodzie marketingowej. Usunięcie klienta
usuwa też jego zgłoszenie.

**Ochrona przed spamem:** ukryte pole-pułapka (wypełnione → udajemy sukces
i nic nie zapisujemy) oraz limit 5 zgłoszeń na 10 minut z jednego adresu IP.


### Dopasowanie ofert do poszukiwań

W obie strony, liczone na żądanie (nic nie jest zapisywane — kryteria i oferty
zmieniają się ciągle, a zapisane dopasowania trzeba by unieważniać):

| Endpoint | Odpowiedź |
|---|---|
| `GET /api/properties/{propertyId}/matches` | klienci z pasującym poszukiwaniem — sekcja „Pasujący klienci” na karcie oferty |
| `GET /api/clients/{clientId}/requirements/{id}/matches` | pasujące oferty — pod każdym aktywnym poszukiwaniem na karcie klienta |

Kandydaci: to samo biuro, ta sama transakcja, rodzaj oferty w rodzajach
poszukiwania, poszukiwanie **aktywne**, oferta **robocza, aktywna albo
zarezerwowana** (robocza też — agent chce wiedzieć, do kogo dzwonić, zanim
oferta trafi na portale). Oferta własna klienta nie pasuje do jego poszukiwania.

Resztę kryteriów ocenia `RequirementMatcher`, każde osobno, z werdyktem:

- **spełnione**,
- **prawie** — cena lub metraż do 10% poza zakresem, mile widziane cechy nie
  wszystkie, oferta dostępna później niż termin klienta,
- **brak danych** — oferta nie ma piętra, liczby pokoi, dzielnicy albo ma cenę
  w innej walucie niż PLN,
- **niespełnione** — wyklucza ofertę.

Pasuje to, co nie ma żadnego „niespełnione”; na liście najpierw dopasowania bez
ostrzeżeń, potem z większą liczbą mile widzianych cech. Miasto i dzielnica
porównują się bez wielkości liter i polskich znaków („lodz” = „Łódź”).

## Kalendarz

Termin jest w tym CRM-ie **łącznikiem, a nie osobnym terminarzem**: wiąże agenta
z ofertą (`calendar_events.property_id`) i z właścicielem (`client_id`). Oba
wiązania są opcjonalne, bo połowa terminów powstaje, zanim będzie co wiązać —
wycena poprzedza ofertę, a spotkanie akwizycyjne poprzedza klienta.

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `POST /api/calendar/events` | `CreateEventRequest` | `201` + `EventResponse` |
| `GET /api/calendar/events?from=&to=` | — | `200` + lista `EventSummary` |
| `GET /api/calendar/events/{id}` | — | `200` + `EventResponse` |
| `PUT /api/calendar/events/{id}` | `CreateEventRequest` | `200` + zaktualizowany termin |
| `PUT /api/calendar/events/{id}/status` | `UpdateEventStatusRequest` | `200` — domknięcie terminu |
| `DELETE /api/calendar/events/{id}` | — | `204` |
| `GET /api/calendar/dictionaries` | — | `200` + słowniki formularza |
| `GET /api/properties/{id}/events` | — | `200` — terminy oferty |
| `GET /api/clients/{id}/events` | — | `200` — terminy klienta |

Filtry listy: `?agentId=`, `?type=`, `?status=`, `?mine=true`. **Zakres dat jest
obowiązkowy i nie ma stronicowania** — kalendarz z natury pyta o zamknięty
przedział („ten tydzień"), a nie o pierwsze 25 wpisów. Szerokość okna serwer
ogranicza do około pół roku (200 dni).

### Kupujący, których nie ma w modelu

Najczęstszy termin — prezentacja — odbywa się z **kupującym lub najemcą**.
Kalendarz powstał, zanim kupujący trafili do bazy klientów, dlatego termin ma
własne pola `counterparty_name` / `counterparty_phone`. Od `V13` kupujący może
być klientem z poszukiwaniem, ale termin wiąże się na razie z jednym klientem
(`client_id`) — druga strona zostaje tekstem. Zamiana `counterparty_*` na klucz
obcy przyjdzie razem z dopasowaniem ofert do poszukiwań.

### Reguły, które warto znać

- **Tytuł jest opcjonalny.** Pusty serwer składa z rodzaju i adresu oferty —
  „Prezentacja — Grzybowska 41". Ręczne przepisywanie tego przy każdym terminie
  to praca, której komputer może nie zlecać człowiekowi.
- **Kolizje ostrzegają, ale nie blokują zapisu.** Nakładające się terminy tego
  samego agenta wracają w polu `conflicts`; agent bywa w dwóch miejscach naraz
  świadomie, a twarde `409` nauczyłoby go prowadzić terminarz obok systemu.
  Zdarzenia całodniowe są z liczenia kolizji wyłączone.
- **Rezultat (`outcome`) wolno podać wyłącznie przy statusie `COMPLETED`** —
  pilnuje tego serwis i `CHECK` w migracji `V10`. To pole niesie całą wartość
  analityczną modułu: trzy prezentacje zamknięte jako „cena za wysoka" to
  argument w rozmowie z właścicielem, a nie wpis w terminarzu.
- **Skasowanie oferty albo klienta odpina termin, nie kasuje go.** Klucze obce
  `calendar_events.property_id` i `client_id` nie mają `ON DELETE`, więc bez
  jawnego odpięcia oferta z choćby jednym terminem w ogóle nie dałaby się
  usunąć. Historia pokazów zostaje — tytuł ma w sobie adres, więc nadal
  wiadomo, czego dotyczyła.
- **Kalendarz jest wspólny dla biura.** Zawężenie do siebie (`mine=true`) to
  filtr, nie uprawnienie — inaczej nie dałoby się umówić zastępstwa ani
  sprawdzić, czy ktoś już nie jedzie pod ten adres.
- **Czas w `TIMESTAMPTZ`**, nie w czasie lokalnym: inaczej przejście na czas
  letni przesuwałoby terminy zapisane wcześniej.
- **Rodzaj niesie kolor, status — sposób podania.** Każdy z dziewięciu rodzajów
  ma własny kolor kafelka (legenda nad siatką), a status modyfikuje ten kafelek:
  pogrubienie, wyblaknięcie, przekreślenie, czerwona obwódka przy nieobecności.
  Paleta rozstrzela odcień i jasność naraz, bo samym odcieniem dziewięciu
  kategorii nie da się rozdzielić przy daltonizmie — liczby w
  [README frontendu](frontend/README.md#kolory-kalendarza).
- Karty oferty i klienta dociągają swoje terminy **osobnym żądaniem** —
  `PropertyResponse` i `ClientResponse` nie zostały o nie rozszerzone, żeby nie
  obciążać każdego odczytu oferty (również z listy i z eksportu).

Poza zakresem tej wersji: przypomnienia i powiadomienia (bez mechanizmu wysyłki
pole byłoby martwe), wielu uczestników jednego terminu, terminy prywatne,
cykliczne i synchronizacja z Google/Outlook.

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
| `DELTA_S3_ENDPOINT` | `http://localhost:9000` | API S3; musi być osiągalne także z przeglądarki |
| `DELTA_S3_BUCKET` | `delta-crm-media` | bucket na zdjęcia |
| `DELTA_S3_ACCESS_KEY` / `DELTA_S3_SECRET_KEY` | `delta` / `delta12345` | dane dostępu do storage'u |
| `delta.storage.url-ttl` | `15m` | ważność podpisanego linku do pliku |
| `server.port` | `8080` | port HTTP |

## Testy

```bash
mvn test     # wymaga działającego Dockera
```

Testy podnoszą własny kontener Postgresa (Testcontainers) i przepuszczają przez
niego migracje Flyway — weryfikują więc także sam schemat, nie tylko kod.
`docker compose` nie musi przy tym działać; to osobny, jednorazowy kontener.

`CalendarModuleTest` sprawdza powiązanie terminu z ofertą i klientem, zapytanie
o zakres dat (przecięcie, nie zawieranie), ostrzeżenie o kolizji, regułę
rezultatu, izolację między biurami oraz odpięcie terminów przy kasowaniu oferty
i klienta — ta ostatnia para to test regresyjny na błąd, przez który oferty
z choćby jednym terminem w ogóle nie dało się usunąć.

`ClientRequirementTest` sprawdza zapis poszukiwania razem z kolekcjami
(rodzaje, lokalizacje z usuwaniem duplikatów, cechy konieczne i mile widziane),
wyliczanie roli kupującego / najemcy wyłącznie z aktywnych poszukiwań — na
karcie i na liście — podmianę kolekcji przy edycji, reguły między polami,
izolację między biurami i znikanie poszukiwań razem z klientem.

`MatchingTest` ustawia wokół jednej oferty poszukiwania sprawdzające po jednej
regule: pełne dopasowanie (także bez polskich znaków w nazwie miasta), cenę
„prawie” w budżecie, wykluczenia (inne miasto, za drogo, brak cechy koniecznej,
inna transakcja, wstrzymane poszukiwanie), brak danych jako ostrzeżenie zamiast
wykluczenia, pomijanie sprzedanych ofert i izolację między biurami.

`InquiryTest` przechodzi całą ścieżkę zgłoszenia: formularz bez logowania
i 404 dla nieznanego klucza, zapis do skrzynki (a nie do klientów) z treścią
zgody, odrzucenie bez zgody / bez kontaktu / z błędnymi kryteriami, pole-pułapkę,
limit na adres IP, tylko kupno albo sprzedaż (bez najmu), zgłoszenie sprzedaży
zamieniane na klienta z notatką i bez poszukiwania, przyjęcie jako nowy klient z poszukiwaniem, podpowiedź
duplikatu i dopięcie do istniejącego klienta, odrzucenie z usunięciem,
izolację między biurami i unieważnienie starego linku.

`AuthFlowTest` pokrywa pełną ścieżkę: rejestracja → hash hasła → logowanie
(w tym niewrażliwość na wielkość liter) → `/me` z tokenem i bez → odrzucenie
podrobionego tokenu → duplikat e-maila → walidacja pól. `PropertyModuleTest`
sprawdza zapis i odczyt oferty (w tym wyliczaną cenę za m²) oraz izolację
między biurami.

`PropertyMediaTest` podnosi obok Postgresa **własny kontener MinIO** — atrapa
storage'u nie sprawdziłaby ani tego, że plik faktycznie ląduje w buckecie, ani
że kasowanie go stamtąd usuwa. Pokrywa skalowanie przy wgrywaniu, odrzucenie
pliku, który nie jest obrazem mimo poprawnego `Content-Type`, zachowanie pozycji
i podpisu przy zamianie pliku, przenumerowanie po skasowaniu, zmianę kolejności,
limit 50, sprzątanie plików przy kasowaniu całej oferty oraz izolację między
biurami.

Pierwsze `mvn test` po sklonowaniu repozytorium pobiera obrazy Postgresa
**i MinIO**, więc trwa dłużej niż kolejne.

## Przed produkcją

- [ ] refresh tokeny albo krótszy TTL z odświeżaniem
- [ ] rate limiting na `/api/auth/login`
- [ ] za reverse proxy: `server.forward-headers-strategy`, żeby limit zgłoszeń
      z formularza liczył się po adresie klienta, a nie proxy; przy kilku
      instancjach — limit we współdzielonym magazynie zamiast w pamięci
- [ ] treść klauzul RODO w `IntakeConsent` do zatwierdzenia przez prawnika biura
- [ ] `delta.cors.allowed-origins` na prawdziwą domenę
- [ ] hasło do bazy oraz `DELTA_S3_*` z sekretów, nie z wartości domyślnych
- [ ] przypiąć konkretny `RELEASE` obrazu MinIO zamiast `latest`
- [ ] sprzątanie osieroconych obiektów w storage (kompensacja jest best-effort:
      przy padzie procesu między zapisem pliku a commitem zostaje sam plik)
