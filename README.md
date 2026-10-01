# Delta CRM

CRM dla biura nieruchomości. Moduły: dashboard, kalendarz, nieruchomości,
klienci (właściciele powierzający sprzedaż lub najem), umowy, eksport na portale
ogłoszeniowe.

- **Backend**. Java 25 + Spring Boot 4.0.7 (`src/`)
- **Frontend**. React 19 + TypeScript + Vite + Tailwind v4 (`frontend/`),
  szata graficzna „Nordic Clean" ([szczegóły](frontend/README.md)),
  mapy na MapLibre GL + OpenFreeMap

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
  instancje. Patrz komentarz w `compose.yaml`),
- **MinIO**. Storage zdjęć: API S3 na **9000**, konsola na **9001**
  (`delta` / `delta12345`).

Dane w wolumenach `crm_postgres-data` i `crm_minio-data`, więc przeżywają
`docker compose down`. Bucket `delta-crm-media` zakłada sama aplikacja przy
starcie. Bez MinIO backend nadal wstaje. Nie działa tylko galeria.

### 2. Backend

Aplikacja **nie wstanie bez sekretu do podpisywania tokenów**. To celowe,
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

JWT (HS256) w nagłówku `Authorization: Bearer <token>`. API jest bezstanowe.
Brak sesji po stronie serwera, brak CSRF.

| Endpoint | Dostęp | Body | Odpowiedź |
|---|---|---|---|
| `POST /api/auth/register` | publiczny | `{ firstName, lastName, email, password, agencyName }` | `201` + `{ token, user }` |
| `POST /api/auth/login` | publiczny | `{ email, password }` | `200` + `{ token, user }` |
| `GET /api/auth/me` | token |. | `200` + `User` |

Każdy inny endpoint pod `/api/**` wymaga tokenu.

## Nieruchomości

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `POST /api/properties` | `CreatePropertyRequest` | `201` + pełna oferta |
| `GET /api/properties` |. | `200` + strona `PropertySummary` |
| `GET /api/properties/{id}` |. | `200` + pełna oferta |
| `PUT /api/properties/{id}` | `CreatePropertyRequest` | `200` + zaktualizowana oferta |
| `DELETE /api/properties/{id}` |. | `204` |
| `GET /api/properties/dictionaries` |. | `200` + wszystkie słowniki formularza |
| `GET /api/properties/{id}/media` |. | `200` + galeria oferty |
| `POST /api/properties/{id}/media` | multipart `files` | `201` + dodane materiały |
| `PUT /api/properties/{id}/media/{mediaId}/file` | multipart `file` | `200`. Podmiana pliku |
| `PUT /api/properties/{id}/media/order` | `{ mediaIds }` | `200`. Kolejność galerii |
| `PATCH /api/properties/{id}/media/{mediaId}` | `{ caption }` | `200`. Podpis zdjęcia |
| `DELETE /api/properties/{id}/media/{mediaId}` |. | `204` |

Filtry listy: `?status=`, `?type=`, `?transaction=`, plus standardowe `page`,
`size`, `sort` (domyślnie `createdAt,desc`).

Ofertę **kasuje się też z listy**, nie tylko z jej karty. Pomyłkę przy
zakładaniu prostuje się wtedy bez wchodzenia w szczegóły. Kosz w wierszu otwiera
**okno potwierdzenia** („Czy na pewno chcesz usunąć tę ofertę?" wraz z numerem
i tytułem), a nie dwa przyciski w komórce: wiersze są niskie i gęsto obok siebie,
więc potwierdzenie w miejscu wypadałoby dokładnie tam, gdzie przed chwilą był
kursor. Wiersz otwiera kartę oferty, więc kliknięcie kosza zatrzymuje się na
swojej komórce. Kasowanie jest **nieodwracalne** i zabiera też zdjęcia. Oferty
wycofanej z rynku nie kasuje się, tylko ustawia jej status na `ARCHIVED`.

Pozostałe potwierdzenia (karta oferty, klient, termin, zdjęcie w galerii) są
nadal dwustopniowe i w miejscu. Tam operacja dotyczy tego, co użytkownik ma
otwarte przed sobą, a nie jednego z wielu wierszy.

**Numer oferty nadaje serwer** w formacie `RRRR/MM/NNN`, osobno dla każdego
biura. Nie ma go w `CreatePropertyRequest`. Portal rozpoznaje po nim ogłoszenie
przy kolejnych wysyłkach, więc musi być stabilny, ale nie ma powodu, by wpisywał
go człowiek.

### Co jest wymagane, a co dopiero do publikacji

Do zapisu oferty wystarczy: rodzaj, transakcja, rynek, tytuł, opis, cena,
powierzchnia oraz województwo i miejscowość. Powiat, gmina, dzielnica **i ulica**
są opcjonalne. Agent często zakłada ofertę z telefonu od właściciela i uzupełnia
resztę po oględzinach, a blokowanie zapisu wypychałoby takie oferty do notatnika.

Ulica jest opcjonalna także z drugiego powodu: **spora część wsi nie ma nazw
ulic** i adres to tam sama miejscowość z numerem („Józefin 12"). Numer budynku
zostaje wymagany. Bez niego nie ma czego wpisać w umowę.

Kompletności pod kątem portalu pilnuje `Property.readyForExport()`, które
sprawdza m.in. powiat (Otodom wymaga pary województwo + powiat), liczbę pokoi
dla mieszkania i domu, co najmniej jedno zdjęcie oraz świadectwo energetyczne.
Warunki zależą od typu: świadectwa energetycznego **nie wymagają** działka,
garaż i pokój (`PropertyType.requiresEnergyCertificate()`). Dla nich brak
świadectwa nie blokuje publikacji. Lista ofert pokazuje ten stan w kolumnie
„Eksport".

**Zakres widoczności bierze się z tokenu.** Identyfikator biura nie jest
parametrem żądania i nie da się go podmienić. Repozytorium nie ma ani jednej
metody potrafiącej zwrócić ofertę bez podania agencji, łącznie z `findById`.
Oferta obcego biura daje `404`, nie `403`, żeby po kodzie odpowiedzi nie dało
się sprawdzać, co ma konkurencja.

### Zdjęcia

Pliki leżą w storage'u obiektowym (**MinIO** w dev, API S3), a nie na dysku
aplikacji ani w bazie. Baza trzyma wyłącznie `storage_key`. Klucz obiektu.

Wybór padł na S3 API, a nie na Ceph, bo skala tego nie uzasadnia: 50 zdjęć na
ofertę po ~1,5 MB przy 500 aktywnych ofertach to ok. 35 GB, czyli jeden dysk,
a nie klaster z MON-ami i OSD-ami. Ceph i tak wystawiłby to samo API przez
RadosGW, więc kod byłby identyczny. Różnica jest wyłącznie w nakładzie
utrzymania. Ta sama implementacja (`S3MediaStorage`) działa na MinIO w dev i na
prawdziwym S3 na produkcji; zmienia się endpoint w konfiguracji, nie kod.

**Przeglądarka pobiera zdjęcia bezpośrednio ze storage'u** po podpisany link
(15 min, `delta.storage.url-ttl`). Bajty nie przechodzą przez Springa.
To nie jest optymalizacja, tylko warunek działania: token leci w nagłówku
`Authorization`, a `<img src>` nagłówka nie wyśle. Klucz obiektu nie wychodzi
przez API, żeby po jego układzie nie dało się zgadywać cudzych ofert.

Przy wgrywaniu każde zdjęcie jest normalizowane: skala do 1920 px, przekodowanie
na JPEG, miniatura 400 px i **usunięcie EXIF-u**. To ostatnie ma znaczenie
niezależnie od rozmiaru pliku. Zdjęcie z telefonu niesie współrzędne GPS, więc
bez tego oferta z włączonym ukrywaniem adresu i tak zdradzałaby położenie.
Format rozpoznajemy po sygnaturze pliku, nie po nagłówku `Content-Type`.

- limit **50 materiałów na ofertę** (Otodom bierze pierwsze 20 wg pozycji),
- pozycja 0 to zdjęcie główne; kasowanie przenumerowuje resztę do ciągu 0..n-1,
- **zamiana pliku** zachowuje pozycję, podpis i identyfikator wpisu. Nowy plik
  ląduje pod tym samym kluczem, więc stara wersja znika bezpowrotnie,
- HEIC jest odrzucany z instrukcją, jak przełączyć iPhone'a na JPG. ImageIO go
  nie czyta, a to domyślny format zdjęć z iPhone'a,
- skasowanie **całej oferty** zabiera też jej pliki: wiersze znikają kaskadą
  (`ON DELETE CASCADE` w `V3`), ale kaskada bazy nie wie nic o buckecie,
  więc `PropertyService.delete` czyści go jawnie,
- **zdjęcia można wybrać już przy zakładaniu oferty.** Zdjęcie trzyma klucz obcy
  do oferty, więc przed jej zapisem nie ma czego nim obwiesić. Front trzyma
  pliki w pamięci i wysyła je zaraz po tym, jak serwer nada ofercie numer.
  Gdyby ten drugi krok padł, oferta i tak zostaje zapisana, a formularz otwiera
  się ponownie z komunikatem, zamiast gubić jedno i drugie,
- kolejność zapisu jest asymetryczna: przy dodawaniu najpierw plik, potem wiersz;
  przy kasowaniu najpierw wiersz, plik po commicie. Awaria w połowie zostawia
  najwyżej plik bez wiersza (zajęte miejsce), nigdy wiersza bez pliku (dziura
  w galerii i eksport bez czego złożyć paczki).

**Galerią zarządza się wyłącznie z formularza oferty.** Karta oferty pokazuje
zdjęcia w trybie tylko do odczytu. Otwiera się ją, żeby ofertę obejrzeć,
a przypadkowego skasowania zdjęcia przy przeglądaniu nie da się cofnąć.

Na karcie galeria stoi w **prawej kolumnie, przyklejona przy przewijaniu**.
Zdjęcie zostaje w polu widzenia przy czytaniu parametrów, bo to właśnie z nimi
się je zestawia. Poniżej `lg` układ wraca do jednej kolumny. Kliknięcie zdjęcia
otwiera **podgląd na całym ekranie**: przewijanie strzałkami (na ekranie i na
klawiaturze), zoom kółkiem lub przyciskami z przesuwaniem powiększonego kadru,
pasek miniatur, licznik i podpis. Podgląd renderuje się przez portal do `body`,
bo `position: fixed` wewnątrz przyklejonej kolumny przykleiłby się do niej
zamiast do okna. Wygaśnięcie podpisanego linku (15 min) galeria poznaje po
nieudanym załadowaniu i dociąga świeży komplet adresów. Bez tego karta
otwarta dłużej niż kwadrans pokazywałaby puste kadry.

Unikat `(property_id, position)` jest **odroczony do commitu** (migracja `V12`).
Zmiana kolejności przepisuje pozycje wielu wierszom naraz i po drodze przechodzi
przez stan z duplikatem, mimo że stan końcowy jest poprawny.

### Mapa i geokodowanie

Pod polami sekcji „Lokalizacja" W formularzu zakładania i edycji stoi mapa
**OpenStreetMap** na całą szerokość sekcji. Działa w obie strony:

- **pinezka → pola.** Kliknięcie w mapę albo przeciągnięcie pinezki wypełnia
  województwo, powiat, gminę, miejscowość, dzielnicę, kod pocztowy, ulicę
  i numer budynku. Postawienie pinezki jest jednoznacznym „to jest to miejsce",
  więc adres spod niej **zastępuje** zawartość pól. A **czego OSM dla tego
  punktu nie zna, to zostaje puste**, bez wyjątków. Zachowanie starej wartości
  obok nowej pinezki dawało adres zszyty z dwóch miejsc: powiat pruszkowski przy
  krakowskiej ulicy wyglądał na wpisany świadomie, a był resztką po poprzednim
  kliknięciu. Powiat i gminę dla miast na prawach powiatu dokłada
  [`CityCounties`](#miasta-na-prawach-powiatu), bo OSM ich nie zna.
- **pola → pinezka.** Adres wpisany ręcznie ustawia pinezkę sam, 0,8 s po
  ostatnim znaku. **Wymagane są województwo i miejscowość**. I tylko one. Sama
  miejscowość nie wystarcza, bo nazwy się powtarzają (samych „Nowych Wsi" jest
  ponad sto) i pinezka lądowałaby losowo. Reszta pól zawęża wynik: kod pocztowy
  rozdziela miejscowości o tej samej nazwie w jednym województwie, ulica schodzi
  z centrum miejscowości na ulicę, a numer budynku. Na budynek. Numer trafia
  w budynek tylko wtedy, gdy OSM go zna (są w nim adresy punktowe); inaczej
  pinezka staje na ulicy i dociąga się ją ręcznie.

Powiat, gminę, dzielnicę i kod pocztowy geokoder **dopisuje tylko do pustych
pól**. Agent zna adres z rozmowy z właścicielem i nie ma powodu poprawiać mu
tego, co wpisał świadomie. Wyjątek: po zmianie **miejscowości** te cztery pola
opisują już inne miejsce, więc zostają zastąpione. Bez tego oferta przeniesiona
z Kajetan do Krakowa zostawałaby z powiatem pruszkowskim.

Pętli między jednym kierunkiem a drugim pilnuje odcisk adresu: pola wpisane
przez geokoder są zapamiętywane i nie wyzwalają kolejnego szukania.

Na **karcie oferty** mapa stoi pod galerią, w tej samej przyklejonej kolumnie.
Zdjęcie mówi, jak obiekt wygląda, mapa, gdzie stoi, i jedno z drugim zestawia się
przy czytaniu parametrów. Jest tylko do odczytu; pinezkę ustawia się w formularzu,
tak samo jak zarządza się tam galerią. Karta pokazuje mapę wyłącznie wtedy, gdy
oferta ma zapisane współrzędne.

#### Wygląd mapy

Styl to **OpenFreeMap „positron"** (kafelki wektorowe, MapLibre GL): ulice,
obrysy budynków, nazwy i **numery domów**. Bez ikon sklepów, restauracji
i bankomatów, którymi standardowe kafelki OSM zalewają centrum miasta. Przy
ofercie liczy się, gdzie stoi budynek i przy jakiej ulicy, a nie co jest
naprzeciwko.

Adres otwiera się na **zoomie 17** (`ADDRESS_ZOOM`). Widać z niego najbliższe
przecznice, czyli przy której ulicy stoi obiekt i co go otacza. To zarazem
próg, od którego styl puszcza numery domów.

Trzy rzeczy, które warto znać, zanim się to ruszy:

- **Kafelki wektorowe, bo nie ma dziś bezkluczowego rastra w tym guście.**
  CARTO Positron, do niedawna standardowa odpowiedź na „czysta mapa OSM",
  zwraca teraz kafelki z napisem „API KEY REQUIRED". OpenFreeMap serwuje
  wektory bez klucza, bez rejestracji i bez limitów. Adres stylu jest w stałej
  `MAP_STYLE`, więc podmiana dostawcy (albo postawienie własnego serwera
  kafelków) to zmiana jednej linii.
- **Numery domów i polskie etykiety dokładamy do stylu przed utworzeniem mapy**,
  a nie po zdarzeniu `load`. Parser kafelka zachowuje tylko te warstwy danych,
  do których odwołuje się styl. Warstwa `housenumber` dołożona po fakcie trafia
  na kafelki, w których numerów już nikt nie zostawił. Z tego samego powodu jej
  `minzoom` równa się maksymalnemu zoomowi źródła (14), a próg „pokaż od z17"
  siedzi w przezroczystości, nie w `minzoom`.
- **MapLibre jest wyłączony z pre-bundlingu Vite** (`optimizeDeps.exclude`).
  Biblioteka dekoduje kafelki w web workerze, którego adres składa przez
  `new Worker(new URL(...))`; esbuild przepisuje ten adres tak, że worker nie
  wstaje, a mapa rysuje puste płótno. Bez błędu widocznego w interfejsie.
  Dotyczy tylko trybu dev, produkcyjny build przez Rollup radzi sobie sam.

Sama biblioteka waży więcej niż cała reszta aplikacji, a używają jej dwa
ekrany, więc jedzie **osobnym chunkiem** (`LazyPropertyMap`). Logowanie,
dashboard i kalendarz nie czekają na mapę, której nie pokazują.

#### Sterowanie

| Co | Jak |
|---|---|
| przybliżanie / oddalanie | **`Ctrl` + kółko myszy**, **`Ctrl` + `+` / `Ctrl` + `−`**, same `+` / `−`, przyciski, dwuklik |
| przewijanie strony | kółko myszy bez modyfikatora. Także nad mapą |
| przesuwanie kadru | przeciągnięcie myszą; strzałki, gdy mapa ma skupienie |
| ustawienie pinezki | kliknięcie w mapę albo przeciągnięcie pinezki (tylko w formularzu) |

**Kółkiem myszy rządzi `cooperativeGestures` MapLibre**: samo przewija stronę,
z `Ctrl` (na macOS `Cmd`) przybliża mapę. Bez tego mapa łapałaby każde
przewinięcie i formularz nie dałby się przewinąć, gdy kursor przejdzie nad
mapą. Kto kręci kółkiem bez modyfikatora, dostaje na mapie podpowiedź, co
wcisnąć. Jej treść, jak i podpowiedzi przycisków zoomu, ustawiamy przez opcję
`locale`, bo domyślne są po angielsku.

Skróty działają, **gdy kursor jest nad mapą**, i to nie jest wygoda, tylko
warunek działania: w formularzu kliknięcie w mapę przestawia pinezkę, więc nie
da się jej zafokusować, nie zmieniając przy okazji adresu oferty. Gdy mapa ma
skupienie (dojście Tabem), same `+` / `−` obsługuje MapLibre. Razem ze
strzałkami do przesuwania kadru.

**`Ctrl` + `+` / `Ctrl` + `−` (i `Ctrl` + kółko) to skróty przeglądarki na
powiększenie całej strony** i korzystają z nich osoby słabowidzące, więc
przechwytujemy je wyłącznie nad mapą. Kursor gdziekolwiek indziej w CRM-ie i powiększa się
strona, jak wszędzie. Ograniczenie do jednego prostokąta jest tu całym
zabezpieczeniem. (Na macOS w Safari `Cmd` + `+` bywa skrótem systemowym,
którego strona nie przechwyci; zostają wtedy przyciski i same `+` / `−`.)

Przy samym `+` / `−`, bez modyfikatora, **pisanie ma pierwszeństwo**: gdy
kursor tekstowy stoi w polu formularza, skrót nie działa. Kursor myszy potrafi
leżeć nad mapą, gdy agent wpisuje `+48` w telefonie albo `-1` w piętrze.
`Alt` przepuszczamy dalej jako skrót systemowy.

**Geokodowanie idzie przez backend** (`/api/geo/**` → Nominatim), a nie prosto
z przeglądarki. Nominatim wysyła nagłówki CORS i dałby się wołać z frontu, ale
jego polityka użycia stawia trzy warunki, których przeglądarka nie spełni:
`User-Agent` jednoznacznie wskazujący aplikację (nie da się go nadpisać),
najwyżej jedno żądanie na sekundę **z całej instalacji** oraz cache'owanie
powtórzeń. Biuro siedzi za jednym adresem IP, więc pięciu agentów przeciągających
pinezkę naraz zostałoby wspólnie odciętych. Przez proxy limit i cache (LRU
w pamięci procesu) są wspólne, a podmiana geokodera na własną instancję to
zmiana jednego adresu w konfiguracji.

| Endpoint | Dostęp | Opis |
|---|---|---|
| `GET /api/geo/reverse?lat=&lon=` | zalogowany | adres pod punktem; `204`, gdy go nie ma |
| `GET /api/geo/search?voivodeship=&city=&…` | zalogowany | punkty pasujące do adresu, od najlepszego |

#### Czego OSM nie powie o powiecie i gminie

Jest jedna przyczyna i dwa objawy. **Gdy granica jednostki administracyjnej
pokrywa się z granicą miasta, OSM ma jeden obiekt** i Nominatim opisuje go jako
`city`/`town`, a nie jako `county` czy `municipality`. Pole wraca puste, choć
jednostka istnieje:

| Przypadek | Powiat z OSM | Gmina z OSM | Przykłady |
|---|---|---|---|
| miasto na prawach powiatu | ✗ | ✗ | Warszawa, Kraków, Katowice, Płock |
| gmina miejska | ✓ | ✗ | Puławy, Zakopane, Świdnik |
| gmina miejsko-wiejska | ✓ | ✓ | Piaseczno, Kozienice, Grójec |
| gmina wiejska | ✓ | ✓ | Sułoszowa, Żelechlinek |

Nie ma czego dopytać. Sprawdzone, żaden poziom `zoom` w zapytaniu odwrotnym
powiatu Warszawy nie wyciąga, bo nie istnieje osobny obiekt, który by go niósł.
Wiedza musi przyjść z naszej strony, i przychodzi dwiema regułami:

- **Gmina**. Miasto bez `municipality` jest gminą miejską, a ta nazywa się tak
  jak miasto. Reguła dotyczy wyłącznie `city`/`town`: przy wsi brak gminy
  oznacza dziurę w danych, a nie że wieś jest gminą, więc wpisanie jej nazwy
  byłoby zgadywaniem.
- **Powiat**. Tu nazwy nie da się wyprowadzić z niczego (powiaty nazywają się
  przymiotnikowo: „Puławski"), więc potrzebna jest lista. `CityCounties`
  wylicza 66 miast na prawach powiatu. Zbiór jest zamknięty i praktycznie
  niezmienny (ostatnia zmiana to odzyskanie praw powiatu przez Wałbrzych
  w 2013 r.), więc trzymamy go w kodzie, zamiast dokładać zależność sieciową do
  faktu zmieniającego się raz na dekadę. Klucz to **para województwo + nazwa**,
  nie sama nazwa: „Chełm" to miasto na prawach powiatu w lubelskim, ale też wieś
  w małopolskim.

Powiat wychodzi **z wielkiej litery** („Puławski", „Warszawski Zachodni"),
choć OSM niesie go w formie zdaniowej („powiat puławski"). Tak samo formatuje
go formularz przy wpisaniu ręcznym (`titleCase`), więc ten sam powiat wygląda
identycznie niezależnie od tego, czy trafił do pola z pinezki, czy z klawiatury.
Gminy ta reguła **nie** dotyczy: „Nowe Miasto nad Pilicą" wyszłoby z błędnym
„Nad", a nazwy gmin przychodzą z OSM już poprawnie zapisane.

Obie reguły uzupełniające wchodzą tylko tam, gdzie pole zostałoby puste.
Wartość z OSM ma pierwszeństwo. Powiat jest tu ważniejszy niż gmina: wymaga go Otodom i sprawdza
`Property.readyForExport()`, więc bez tego **każda oferta w dużym mieście**
wychodziła z pinezki niezdatna do wysyłki.

Gdyby kiedyś trzeba było uzupełniać powiat i gminę także tam, gdzie OSM jest
dziurawy (a nie tylko dla miast na prawach powiatu), właściwym krokiem jest
import słownika **TERYT** do bazy. Kolumny `teryt_simc` i `teryt_ulic`
czekają w adresie od `V3`.

Współrzędne (`latitude`, `longitude`) były w modelu i w API od `V3`. Doszło
wypełnianie ich z interfejsu, więc mapa nie wymagała migracji.

### Skąd wziął się zestaw pól

Z wymagań importu portali ogłoszeniowych. Punktem odniesienia jest
[specyfikacja Otodom Import](https://cdn.prod.website-files.com/61a78ba1bfb5df5455656f40/6638c399538563422cecdd31_otoDom_Import_170130.pdf)
(format XML wysyłany przez FTP, paczki przetwarzane co godzinę).

Zasada, na której stoi cały model: **w bazie nie ma ani jednego kodu
portalowego**. Portale opisują ten sam atrybut różnymi słownikami, a Otodom
używa różnych słowników dla różnych typów obiektu. Ta sama liczba `2` znaczy
„dom wolnostojący" przy mieszkaniu, „szeregowiec" przy domu i „w bloku" przy
lokalu użytkowym. Do tego ich dokumentacja zaleca częste odświeżanie słowników.
Trzymamy więc własne, jednoznaczne nazwy (`pl.delta.crm.property.dictionary`),
a tłumaczenie na kody portalu będzie należało do modułu eksportu.

Świadectwo charakterystyki energetycznej jest w modelu, mimo że specyfikacja
Otodom z 2017 r. go nie zna. Obowiązek podania wskaźnika EP w ogłoszeniu
wynika z ustawy i obowiązuje od 28.04.2023 niezależnie od formatu XML.

Słowniki mają polskie etykiety po stronie backendu i wychodzą jednym
endpointem, więc front nie powiela dwudziestu kilku enumów w TypeScripcie.

**Payload tokenu:** `iss`, `sub` (UUID użytkownika), `email`, `role`, `iat`, `exp`.
Podmiotem jest id, nie e-mail. Token przeżyje zmianę adresu.

### Pola zależne od typu obiektu

Formularz oferty pokazuje inny zestaw pól i cech dla każdego z siedmiu rodzajów
obiektu (mieszkanie, dom, działka, lokal użytkowy, hala/magazyn,
garaż/miejsce postojowe, pokój). Garaż nie pyta o liczbę pokoi ani łazienek,
działka nie ma sekcji budynku ani świadectwa energetycznego, hala ma wysokość
i rampę zamiast pięter. Ta sama widoczność obowiązuje w formularzu i w widoku
szczegółów.

Pola specyficzne dla typu (migracja `V9`):

- **hala/magazyn**. Moc przyłącza [kW], nośność posadzki [t/m²], liczba bram/doków,
- **garaż**. Typ (`GarageType`: murowany, blaszany, podziemny, w hali, naziemny),
- **pokój**. Dla ilu osób, dostęp do łazienki (`RoomBathroom`: osobna/współdzielona).

**Cena za m²** (`price_per_m2`, migracja `V8`) liczona jest dwukierunkowo z ceny
i powierzchni. Wpisanie jednej wartości wylicza drugą (cena z ceny za m² jest
zaokrąglana do pełnych złotych). Dla garażu i pokoju pole jest ukryte, bo wycena
nie jest metrażowa.

**Cechy** (sekcja „Cechy") są filtrowane per pozycja: każda cecha (`Feature`)
zna typy obiektu, dla których ma sens, więc mieszkanie nie widzi „studni" ani
„ogrodzenia", a garaż „pralki". Kategoria bez pasujących cech w ogóle się nie
pokazuje.

### Formularze i walidacja

Walidacja działa po obu stronach. Bean Validation na backendzie i lustrzane
reguły w formularzu, żeby błąd był widoczny od razu, zanim żądanie poleci:

- pola liczbowe przyjmują tylko cyfry; kwoty i powierzchnie maks. 2 miejsca po
  przecinku; realne zakresy (piętro do 160, rok budowy do bieżącego + 10),
- kod pocztowy w formacie `00-000`. Myślnik dopisuje się sam w trakcie pisania,
  więc wbija się pięć cyfr (`lib/postalCode.ts`),
- telefon: kierunkowy `+48` stoi na stałe przy polu, wpisuje się dziewięć cyfr
  (`000 000 000`, spacje dopisuje maska). Numer zagraniczny zaczyna się od `+`
  albo `00`. Wtedy prefiks znika, a pole przyjmuje numer z kierunkowym bez
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
  z tym samym komunikatem. Inaczej dałoby się sprawdzać, kto ma konto.
- **E-mail** jest normalizowany do małych liter przy zapisie i przy logowaniu.
- **Pierwszy użytkownik** zakładający konto biura dostaje rolę `ADMIN`.
- **Sekret JWT** nie ma wartości domyślnej. Brak `DELTA_JWT_SECRET` to błąd startu,
  a nie ciche użycie czegoś słabego. Minimum 32 bajty (wymóg HS256).

## Klienci

Klient to **osoba fizyczna po stronie podaży, popytu albo obu naraz**. Role
**nie są polem klienta**: sprzedający / wynajmujący wynika z typu transakcji
ofert do niego przypisanych (`properties.owner_client_id`, migracja `V7`),
a kupujący / najemca. Z jego aktywnych poszukiwań (`client_requirements`,
migracja `V13`). Ta sama osoba potrafi sprzedawać kawalerkę i szukać większego
mieszkania, więc rola na sztywno przy kliencie byłaby fałszem.

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `POST /api/clients` | `CreateClientRequest` | `201` + `ClientResponse` |
| `GET /api/clients` |. | `200` + strona `ClientSummary` |
| `GET /api/clients/{id}` |. | `200` + `ClientResponse` |
| `PUT /api/clients/{id}` | `CreateClientRequest` | `200` + zaktualizowany klient |
| `DELETE /api/clients/{id}` |. | `204` |
| `PUT /api/clients/{clientId}/properties/{propertyId}` | | `200` przypisz ofertę |
| `DELETE /api/clients/{clientId}/properties/{propertyId}` | | `204` odłącz ofertę |

Filtry listy: `?status=`, `?search=` (imię, nazwisko, telefon, e-mail) plus
standardowe `page`, `size`, `sort`.

Wymagany jest **telefon albo e-mail**. Kontakt bez żadnego z nich nie ma sensu.
Usunięcie klienta **odłącza** jego oferty (ustawia właściciela na `null`) oraz
jego terminy, a nie kasuje ich. Za to jego poszukiwania znikają razem z nim.
Zakres, jak wszędzie, bierze się z tokenu. Nie ma metody repozytorium
zwracającej klienta bez podania biura.

### Poszukiwania

Czego klient szuka jako kupujący albo najemca. Osobna encja, a nie pola
klienta: jedna osoba może szukać kilku rzeczy naraz, a każde poszukiwanie ma
własny stan. **Aktywne, wstrzymane, zrealizowane, nieaktualne**. Do roli
liczą się tylko aktywne; pozostałe zostają na karcie jako historia.

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `GET /api/clients/{clientId}/requirements` |. | `200` + lista `RequirementResponse` |
| `POST /api/clients/{clientId}/requirements` | `RequirementRequest` | `201` + poszukiwanie |
| `PUT /api/clients/{clientId}/requirements/{id}` | `RequirementRequest` | `200` |
| `PUT /api/clients/{clientId}/requirements/{id}/status` | `{ "status": … }` | `200`. Szybka zmiana stanu |
| `DELETE /api/clients/{clientId}/requirements/{id}` |. | `204` |

Karta klienta (`GET /api/clients/{id}`) niesie poszukiwania od razu, a lista
klientów. `buyerCount` / `tenantCount` obok `sellCount` / `rentCount`.

Kryteria:

- **wymagane są tylko transakcja i rodzaj nieruchomości** (jeden lub kilka).
  Reszta to tyle, ile klient zdążył powiedzieć przez telefon,
- lokalizacje jako lista miejscowość + opcjonalna dzielnica, w kolejności
  ważności; duplikaty wypadają przy zapisie,
- zakresy budżetu (przy najmie: czynsz miesięczny), metrażu, pokoi i piętra.
  Każda granica z osobna opcjonalna, „od” nie większe niż „do”,
- rynek (pusty = obojętny), „bez ostatniego piętra”, termin zakupu / wprowadzenia,
- finansowanie (gotówka, kredyt, kredyt z decyzją…) **tylko przy kupnie**.
  Przy najmie serwis je czyści,
- cechy ze słownika ofert z podziałem na **konieczne i mile widziane**; cecha
  podana w obu zbiorach zostaje konieczna (pilnuje tego też klucz główny tabeli),
- pokój da się wyłącznie najmować. Poszukiwanie kupna pokoju jest odrzucane.

Kryteria są zwykłymi kolumnami, a nie JSON-em, bo następny krok. Dopasowanie
ofert do poszukiwań. Musi dać się napisać w SQL i zaindeksować
(`ix_client_requirements_matching`).

Formularz (`RequirementForm`) jest pod rozmowę telefoniczną: na górze
transakcja, rodzaj, lokalizacja, budżet, metraż i pokoje; rynek, piętro,
finansowanie, cechy i notatka są zwinięte. Pola pokoi i piętra pokazują się
tylko dla rodzajów, dla których mają sens, a cechy. Przefiltrowane pod
wybrane rodzaje. Kwoty grupują się spacjami w trakcie pisania (`lib/amount.ts`).

### Zgłoszenia z formularza

Klient zgłasza się sam na publicznej stronie `/zgloszenie/<klucz-biura>`. Bez
logowania. Link (do skopiowania w **Zgłoszenia**) biuro wysyła klientom, wstawia
na stronę www albo do ogłoszeń.

Formularz ma **dokładnie dwie ścieżki: „Chcę kupić” i „Chcę sprzedać”**. Najem
obsługuje agent przy rozmowie (API odrzuca kryteria najmu z formularza):

- **kupno**. Kryteria jak w poszukiwaniu (bez rynku, piętra i cech, o które
  klient i tak by nie umiał odpowiedzieć),
- **sprzedaż**. Rodzaj, miejscowość, dzielnica, powierzchnia, pokoje i oczekiwana
  cena (migracja `V15`). Zgłoszenia sprzedaży **nie zamieniamy na ofertę**:
  ogłoszenie wymaga adresu, opisu i zdjęć, których formularz nie zbiera, więc po
  przyjęciu opis nieruchomości trafia do notatki klienta, a ofertę agent zakłada
  po rozmowie lub oględzinach.

Zgłoszenie **nie trafia od razu do klientów**, tylko do skrzynki
(`client_inquiries`, migracja `V14`). Agent:

- **przyjmuje** je. Powstaje klient ze źródłem „Strona WWW”; przy kupnie
  z aktywnym poszukiwaniem (wiadomość klienta w jego notatce), przy sprzedaży
  z opisem nieruchomości w notatce klienta,
- **dopina** do istniejącego klienta. Skrzynka sama podpowiada osoby z tym
  samym telefonem lub e-mailem; u istniejącego klienta uzupełniamy tylko brakujące
  dane kontaktowe, niczego nie nadpisujemy,
- **odrzuca**. Zgłoszenie jest usuwane razem z danymi osobowymi.

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

**RODO:** zgoda na przetwarzanie danych jest obowiązkowa, marketingowa.
Opcjonalna. Przy zgłoszeniu zapisujemy moment zgody i **pełną treść klauzuli**,
którą klient widział (a nie numer wersji), więc dowód przetrwa zmianę tekstu.
Treść klauzul jest w jednym miejscu (`IntakeConsent`) i to ją serwer wysyła do
formularza. Front nie ma własnej kopii. Po przyjęciu na karcie klienta ląduje
notatka z datą zgłoszenia i informacją o zgodzie marketingowej. Usunięcie klienta
usuwa też jego zgłoszenie.

**Ochrona przed spamem:** ukryte pole-pułapka (wypełnione → udajemy sukces
i nic nie zapisujemy) oraz limit 5 zgłoszeń na 10 minut z jednego adresu IP.


### Dopasowanie ofert do poszukiwań

W obie strony, liczone na żądanie (nic nie jest zapisywane. Kryteria i oferty
zmieniają się ciągle, a zapisane dopasowania trzeba by unieważniać):

| Endpoint | Odpowiedź |
|---|---|
| `GET /api/properties/{propertyId}/matches` | klienci z pasującym poszukiwaniem. Sekcja „Pasujący klienci” na karcie oferty |
| `GET /api/clients/{clientId}/requirements/{id}/matches` | pasujące oferty. Pod każdym aktywnym poszukiwaniem na karcie klienta |

Kandydaci: to samo biuro, ta sama transakcja, rodzaj oferty w rodzajach
poszukiwania, poszukiwanie **aktywne**, oferta **robocza, aktywna albo
zarezerwowana** (robocza też. Agent chce wiedzieć, do kogo dzwonić, zanim
oferta trafi na portale). Oferta własna klienta nie pasuje do jego poszukiwania.

Resztę kryteriów ocenia `RequirementMatcher`, każde osobno, z werdyktem:

- **spełnione**,
- **prawie**. Cena lub metraż do 10% poza zakresem, mile widziane cechy nie
  wszystkie, oferta dostępna później niż termin klienta,
- **brak danych**. Oferta nie ma piętra, liczby pokoi, dzielnicy albo ma cenę
  w innej walucie niż PLN,
- **niespełnione**. Wyklucza ofertę.

Pasuje to, co nie ma żadnego „niespełnione”; na liście najpierw dopasowania bez
ostrzeżeń, potem z większą liczbą mile widzianych cech. Miasto i dzielnica
porównują się bez wielkości liter i polskich znaków („lodz” = „Łódź”).

## Kalendarz

Termin jest w tym CRM-ie **łącznikiem, a nie osobnym terminarzem**: wiąże agenta
z ofertą (`calendar_events.property_id`) i z właścicielem (`client_id`). Oba
wiązania są opcjonalne, bo połowa terminów powstaje, zanim będzie co wiązać.
Wycena poprzedza ofertę, a spotkanie akwizycyjne poprzedza klienta.

| Endpoint | Body | Odpowiedź |
|---|---|---|
| `POST /api/calendar/events` | `CreateEventRequest` | `201` + `EventResponse` |
| `GET /api/calendar/events?from=&to=` |. | `200` + lista `EventSummary` |
| `GET /api/calendar/events/{id}` |. | `200` + `EventResponse` |
| `PUT /api/calendar/events/{id}` | `CreateEventRequest` | `200` + zaktualizowany termin |
| `PUT /api/calendar/events/{id}/status` | `UpdateEventStatusRequest` | `200`. Domknięcie terminu |
| `DELETE /api/calendar/events/{id}` |. | `204` |
| `GET /api/calendar/dictionaries` |. | `200` + słowniki formularza |
| `GET /api/properties/{id}/events` | | `200` terminy oferty |
| `GET /api/clients/{id}/events` | | `200` terminy klienta |

Filtry listy: `?agentId=`, `?type=`, `?status=`, `?mine=true`. **Zakres dat jest
obowiązkowy i nie ma stronicowania**. Kalendarz z natury pyta o zamknięty
przedział („ten tydzień"), a nie o pierwsze 25 wpisów. Szerokość okna serwer
ogranicza do około pół roku (200 dni).

### Kupujący, których nie ma w modelu

Najczęstszy termin Prezentacja odbywa się z **kupującym lub najemcą**.
Kalendarz powstał, zanim kupujący trafili do bazy klientów, dlatego termin ma
własne pola `counterparty_name` / `counterparty_phone`. Od `V13` kupujący może
być klientem z poszukiwaniem, ale termin wiąże się na razie z jednym klientem
(`client_id`). Druga strona zostaje tekstem. Zamiana `counterparty_*` na klucz
obcy przyjdzie razem z dopasowaniem ofert do poszukiwań.

### Reguły, które warto znać

- **Tytuł jest opcjonalny.** Pusty serwer składa z rodzaju i adresu oferty „Prezentacja Grzybowska 41". Ręczne przepisywanie tego przy każdym terminie
  to praca, której komputer może nie zlecać człowiekowi.
- **Kolizje ostrzegają, ale nie blokują zapisu.** Nakładające się terminy tego
  samego agenta wracają w polu `conflicts`; agent bywa w dwóch miejscach naraz
  świadomie, a twarde `409` nauczyłoby go prowadzić terminarz obok systemu.
  Zdarzenia całodniowe są z liczenia kolizji wyłączone.
- **Rezultat (`outcome`) wolno podać wyłącznie przy statusie `COMPLETED`**.
  Pilnuje tego serwis i `CHECK` w migracji `V10`. To pole niesie całą wartość
  analityczną modułu: trzy prezentacje zamknięte jako „cena za wysoka" to
  argument w rozmowie z właścicielem, a nie wpis w terminarzu.
- **Skasowanie oferty albo klienta odpina termin, nie kasuje go.** Klucze obce
  `calendar_events.property_id` i `client_id` nie mają `ON DELETE`, więc bez
  jawnego odpięcia oferta z choćby jednym terminem w ogóle nie dałaby się
  usunąć. Historia pokazów zostaje. Tytuł ma w sobie adres, więc nadal
  wiadomo, czego dotyczyła.
- **Kalendarz jest wspólny dla biura.** Zawężenie do siebie (`mine=true`) to
  filtr, nie uprawnienie. Inaczej nie dałoby się umówić zastępstwa ani
  sprawdzić, czy ktoś już nie jedzie pod ten adres.
- **Czas w `TIMESTAMPTZ`**, nie w czasie lokalnym: inaczej przejście na czas
  letni przesuwałoby terminy zapisane wcześniej.
- **Rodzaj niesie kolor, status. Sposób podania.** Każdy z dziewięciu rodzajów
  ma własny kolor kafelka (legenda nad siatką), a status modyfikuje ten kafelek:
  pogrubienie, wyblaknięcie, przekreślenie, czerwona obwódka przy nieobecności.
  Paleta rozstrzela odcień i jasność naraz, bo samym odcieniem dziewięciu
  kategorii nie da się rozdzielić przy daltonizmie. Liczby w
  [README frontendu](frontend/README.md#kolory-kalendarza).
- Karty oferty i klienta dociągają swoje terminy **osobnym żądaniem**.
  `PropertyResponse` i `ClientResponse` nie zostały o nie rozszerzone, żeby nie
  obciążać każdego odczytu oferty (również z listy i z eksportu).

Poza zakresem tej wersji: przypomnienia i powiadomienia (bez mechanizmu wysyłki
pole byłoby martwe), wielu uczestników jednego terminu, terminy prywatne,
cykliczne i synchronizacja z Google/Outlook.

## Baza danych

PostgreSQL 17. Ten sam silnik na dev i w testach, żeby nie było klasy błędów
„działa lokalnie, wysypuje się na produkcji".

**Schemat należy do Flyway**, nie do Hibernate. `ddl-auto` jest ustawione na
`validate`, więc rozjazd między encją a migracją wywala aplikację przy starcie,
zamiast po cichu dopisywać kolumny. Nową zmianę schematu dodaje się jako kolejny
plik w `src/main/resources/db/migration/` (`V2__opis.sql`). Nigdy przez edycję
już zaaplikowanej migracji.

## Konfiguracja

| Zmienna / property | Domyślnie | Opis |
|---|---|---|
| `DELTA_JWT_SECRET` |. (Wymagane) | klucz HMAC, min. 32 bajty |
| `DELTA_DB_URL` | `jdbc:postgresql://localhost:5434/delta_crm` | JDBC URL |
| `DELTA_DB_USER` / `DELTA_DB_PASSWORD` | `delta` / `delta` | dane logowania do bazy |
| `delta.jwt.ttl` | `12h` | czas życia tokenu |
| `delta.jwt.issuer` | `delta-crm` | claim `iss`, weryfikowany przy odczycie |
| `delta.cors.allowed-origins` | `http://localhost:5173` | dozwolone originy dla `/api/**` |
| `DELTA_S3_ENDPOINT` | `http://localhost:9000` | API S3; musi być osiągalne także z przeglądarki |
| `DELTA_S3_BUCKET` | `delta-crm-media` | bucket na zdjęcia |
| `DELTA_S3_ACCESS_KEY` / `DELTA_S3_SECRET_KEY` | `delta` / `delta12345` | dane dostępu do storage'u |
| `delta.storage.url-ttl` | `15m` | ważność podpisanego linku do pliku |
| `DELTA_GEO_URL` | `https://nominatim.openstreetmap.org` | instancja Nominatim |
| `DELTA_GEO_USER_AGENT` | `delta-crm/1.0 (kontakt@delta-crm.pl)` | wymagany przez politykę OSM |
| `delta.geo.min-interval` | `1s` | minimalny odstęp między żądaniami do geokodera |
| `delta.geo.cache-size` | `500` | ile odpowiedzi geokodera trzymamy w pamięci |
| `server.port` | `8080` | port HTTP |

## Testy

```bash
mvn test     # wymaga działającego Dockera
```

Testy podnoszą własny kontener Postgresa (Testcontainers) i przepuszczają przez
niego migracje Flyway. Weryfikują więc także sam schemat, nie tylko kod.
`docker compose` nie musi przy tym działać; to osobny, jednorazowy kontener.

`CalendarModuleTest` sprawdza powiązanie terminu z ofertą i klientem, zapytanie
o zakres dat (przecięcie, nie zawieranie), ostrzeżenie o kolizji, regułę
rezultatu, izolację między biurami oraz odpięcie terminów przy kasowaniu oferty
i klienta. Ta ostatnia para to test regresyjny na błąd, przez który oferty
z choćby jednym terminem w ogóle nie dało się usunąć.

`ClientRequirementTest` sprawdza zapis poszukiwania razem z kolekcjami
(rodzaje, lokalizacje z usuwaniem duplikatów, cechy konieczne i mile widziane),
wyliczanie roli kupującego / najemcy wyłącznie z aktywnych poszukiwań. Na
karcie i na liście. Podmianę kolekcji przy edycji, reguły między polami,
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

`GeoServiceTest` sprawdza tłumaczenie nazewnictwa OpenStreetMap na nasz adres.
Bez sieci i bez kontekstu Springa, na odpowiedziach przepisanych z prawdziwych
wywołań: dzielnicę z `suburb` zamiast z `quarter` (Warszawa: „Wola", nie
„Mirów"), zdejmowanie rodzajników „powiat"/„gmina" wraz z podniesieniem
powiatu do wielkiej litery (także dwuczłonowego „Warszawski Zachodni"
i łączonego myślnikiem „Jastrzębie-Zdrój"), miejscowość z `town`
i awaryjnie z gminy, rozpoznanie województwa mimo znaków diakrytycznych
i myślnika oraz puste pole przy nazwie spoza słownika, a także budowanie
zapytania (dzielnica tylko wtedy, gdy nie ma ulicy). Osobno pilnuje uzupełniania
powiatu i gminy: Warszawa dostaje powiat i gminę „Warszawa", Puławy. Gminę
„Puławy" przy powiecie z OSM, wieś Chełm w małopolskim nie dostaje powiatu
(dopasowanie jest po parze województwo + nazwa), wieś bez gminy nie dostaje
gminy od swojej nazwy, wartość z OSM ma pierwszeństwo przed listą, a sama lista
ma mieć komplet 66 pozycji.

`AuthFlowTest` pokrywa pełną ścieżkę: rejestracja → hash hasła → logowanie
(w tym niewrażliwość na wielkość liter) → `/me` z tokenem i bez → odrzucenie
podrobionego tokenu → duplikat e-maila → walidacja pól. `PropertyModuleTest`
sprawdza zapis i odczyt oferty (w tym wyliczaną cenę za m²) oraz izolację
między biurami.

`PropertyMediaTest` podnosi obok Postgresa **własny kontener MinIO**. Atrapa
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
      instancjach. Limit we współdzielonym magazynie zamiast w pamięci
- [ ] treść klauzul RODO w `IntakeConsent` do zatwierdzenia przez prawnika biura
- [ ] `delta.cors.allowed-origins` na prawdziwą domenę
- [ ] `DELTA_GEO_USER_AGENT` na prawdziwy adres kontaktowy; przy większym ruchu
      własna instancja Nominatim zamiast publicznej (limit 1 żądanie/s)
- [ ] hasło do bazy oraz `DELTA_S3_*` z sekretów, nie z wartości domyślnych
- [ ] przypiąć konkretny `RELEASE` obrazu MinIO zamiast `latest`
- [ ] sprzątanie osieroconych obiektów w storage (kompensacja jest best-effort:
      przy padzie procesu między zapisem pliku a commitem zostaje sam plik)
