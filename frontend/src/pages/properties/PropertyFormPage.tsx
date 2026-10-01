import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type ReactNode,
} from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  ArrowRight,
  Banknote,
  Building2,
  ClipboardCheck,
  FileText,
  Home,
  Images,
  ListChecks,
  MapPin,
  Ruler,
  type LucideIcon,
} from "lucide-react";
import {
  createProperty,
  fetchDictionaries,
  fetchProperty,
  updateProperty,
  uploadPropertyMedia,
  type Dictionaries,
  type PropertyMedia,
} from "../../api/properties";
import {
  canGeocode,
  reverseGeocode,
  searchLocation,
  type GeoQuery,
} from "../../api/geo";
import { PropertyGallery } from "./PropertyGallery";
import { PropertyGalleryDraft } from "./PropertyGalleryDraft";
import {
  LazyPropertyMap,
  type LatLng,
} from "../../components/map/LazyPropertyMap";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { Textarea } from "../../components/ui/Textarea";
import { maskPostalCode } from "../../lib/postalCode";
import { PropertyFormStepper, type StepState } from "./PropertyFormStepper";

/** Wszystkie pola tekstowe trzymamy jako stringi. Konwersja dopiero przy wysyłce. */
type Fields = Record<string, string>;

const INITIAL: Fields = {
  propertyType: "APARTMENT",
  transactionType: "SALE",
  marketType: "SECONDARY",
  status: "DRAFT",
  title: "",
  description: "",
  price: "",
  pricePerM2: "",
  priceCurrency: "PLN",
  rent: "",
  deposit: "",
  commissionPercent: "",
  totalArea: "",
  usableArea: "",
  plotArea: "",
  roomsCount: "",
  bathroomsCount: "",
  floorNo: "",
  buildingFloorsCount: "",
  ceilingHeight: "",
  voivodeship: "",
  county: "",
  commune: "",
  city: "",
  district: "",
  street: "",
  buildingNumber: "",
  postalCode: "",
  buildYear: "",
  buildingType: "",
  buildingMaterial: "",
  constructionStatus: "",
  ownershipForm: "",
  windowsType: "",
  surroundings: "",
  plotType: "",
  plotDimensions: "",
  roadAccess: "",
  zoningPlan: "",
  hallStructure: "",
  flooring: "",
  parkingType: "",
  powerConnectionKw: "",
  floorLoadPerM2: "",
  loadingDocksCount: "",
  garageType: "",
  occupants: "",
  roomBathroom: "",
  energyPrimary: "",
  energyFinal: "",
  energyClass: "",
  energyCertNumber: "",
  availableFrom: "",
  videoUrl: "",
  panoramaUrl: "",
  keysInfo: "",
  privateNotes: "",
  energyExemptNote: "",
};

/** Ile czekamy po ostatnim znaku, zanim ruszymy z szukaniem adresu na mapie. */
const GEOCODE_DEBOUNCE_MS = 800;

/** Pola adresu, z których składamy zapytanie do geokodera. */
const geoQueryOf = (fields: Fields): Partial<GeoQuery> => ({
  voivodeship: fields.voivodeship,
  city: fields.city,
  district: fields.district,
  street: fields.street,
  buildingNumber: fields.buildingNumber,
  postalCode: fields.postalCode,
});

/**
 * Odcisk adresu. Po nim poznajemy, czy pinezka wciąż odpowiada temu, co jest
 * w polach. Bez tego uzupełnienie pól z mapy wyglądałoby jak ręczna zmiana
 * adresu i natychmiast odesłałoby pinezkę do geokodera, w kółko.
 */
const sameCity = (a: string | null, b: string | null): boolean =>
  (a ?? "").trim().toLowerCase() === (b ?? "").trim().toLowerCase();

const signatureOf = (query: Partial<GeoQuery>): string =>
  [
    query.voivodeship,
    query.city,
    query.district,
    query.street,
    query.buildingNumber,
    query.postalCode,
  ]
    .map((value) => (value ?? "").trim().toLowerCase())
    .join("|");

/** Etapy kreatora. Kolejność taka, w jakiej agent zbiera dane od właściciela. */
type StepId =
  | "basic"
  | "area"
  | "location"
  | "price"
  | "details"
  | "features"
  | "media"
  | "description"
  | "summary";

interface StepDef {
  id: StepId;
  title: string;
  description?: string;
  icon: LucideIcon;
}

/**
 * Do którego etapu należy błąd o danym kluczu (klucze jak w Bean Validation).
 * Po nieudanym zapisie przenosimy agenta na pierwszy etap z błędem.
 */
const stepOfKey = (key: string): StepId => {
  if (key.startsWith("pricing.")) return "price";
  if (key.startsWith("area.")) return "area";
  if (key.startsWith("address.")) return "location";
  if (/^(building|land|commercial|energy)\./.test(key)) return "details";
  if (key === "title" || key === "description") return "description";
  return "basic";
};

export function PropertyFormPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const [dict, setDict] = useState<Dictionaries | null>(null);
  const [fields, setFields] = useState<Fields>(INITIAL);
  const [features, setFeatures] = useState<Set<string>>(new Set());
  const [heating, setHeating] = useState<Set<string>>(new Set());
  const [uses, setUses] = useState<Set<string>>(new Set());
  const [flags, setFlags] = useState({
    priceNegotiable: false,
    priceIncludesRent: false,
    hideExactAddress: true,
    furnished: false,
    plotFenced: false,
    officeSpace: false,
    socialFacilities: false,
    loadingRamp: false,
    energyExempt: false,
    exportable: true,
  });

  // `errors` trzyma wyłącznie błędy z backendu (ApiError). Błędy walidacji
  // klienta liczymy na żywo z fields. Patrz liveErrors/errorFor niżej.
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [touched, setTouched] = useState<Set<string>>(new Set());
  const [submitted, setSubmitted] = useState(false);
  // Rośnie przy każdej nieudanej próbie zapisu. Wyzwala przewinięcie do błędu.
  const [errorSignal, setErrorSignal] = useState(0);
  // Które pole steruje przeliczeniem: „price" = cena wpisana ręcznie (liczymy m²),
  // „ppm2" = cena za m² wpisana ręcznie (liczymy cenę). Zmiana powierzchni
  // przelicza to drugie zgodnie z ostatnim wyborem użytkownika.
  const [priceDriver, setPriceDriver] = useState<"price" | "ppm2">("price");
  // Przy edycji galeria żyje obok formularza. Jej operacje idą osobnymi
  // żądaniami i nie czekają na „Zapisz".
  const [media, setMedia] = useState<PropertyMedia[]>([]);
  // Przy nowej ofercie nie ma jeszcze do czego przypiąć zdjęć, więc czekają
  // w pamięci i lecą zaraz po tym, jak serwer nada ofercie identyfikator.
  const [draftFiles, setDraftFiles] = useState<File[]>([]);
  // Oferta zapisana, ale zdjęcia nie przeszły. Wchodzimy tu z jej edycji
  // z komunikatem, żeby nie wyglądało to na udany zapis kompletu.
  const mediaError = (location.state as { mediaError?: string } | null)
    ?.mediaError;
  const [formError, setFormError] = useState<string | null>(null);
  const [dictError, setDictError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // --- kreator ---------------------------------------------------------------
  const [stepId, setStepId] = useState<StepId>("basic");
  // Etapy, do których agent już doszedł. Tylko do nich wolno skakać z listy.
  // W edycji oferta jest kompletna, więc od razu wszystkie są dostępne.
  const [visited, setVisited] = useState<Set<StepId>>(new Set(["basic"]));
  // Etapy, z których agent próbował pójść dalej. Od tej chwili pokazujemy
  // w nich także braki w polach wymaganych.
  const [attempted, setAttempted] = useState<Set<StepId>>(new Set());
  const topRef = useRef<HTMLDivElement>(null);
  const firstStepRender = useRef(true);

  // --- mapa ------------------------------------------------------------------
  const [coords, setCoords] = useState<LatLng | null>(null);
  const [geoBusy, setGeoBusy] = useState(false);
  // Adres spod pinezki albo powód, dla którego go nie ma. Linia pod mapą.
  const [geoNote, setGeoNote] = useState<string | null>(null);
  const [geoError, setGeoError] = useState<string | null>(null);
  // Odcisk adresu, do którego pasuje obecna pinezka. Równy bieżącemu znaczy
  // „pola i pinezka są zgodne". Nie ma czego szukać.
  const pinnedSignature = useRef<string | null>(null);
  // Miejscowość, dla której pinezka ostatnio stanęła. Zmiana miejscowości
  // unieważnia wszystko, co z niej wynika. Powiat, gminę, dzielnicę i kod
  // pocztowy. Więc wtedy wolno je nadpisać mimo że nie są puste.
  const pinnedCity = useRef<string | null>(null);
  // Bieżące pola widziane z wnętrza asynchronicznych wywołań zwrotnych.
  const fieldsRef = useRef(fields);
  fieldsRef.current = fields;

  const loadDictionaries = () => {
    setDictError(null);
    fetchDictionaries()
      .then(setDict)
      .catch(() =>
        // Bez słowników każda lista wyboru jest pusta. Cichy błąd wygląda wtedy
        // jak zepsuty formularz, więc mówimy wprost, co się stało.
        setDictError(
          "Nie udało się wczytać słowników. Listy wyboru pozostaną puste.",
        ),
      );
  };

  useEffect(loadDictionaries, []);

  // Nowy etap zaczyna się od góry strony, a nie tam, gdzie skończył poprzedni.
  // Efekt stoi przed przewijaniem do błędu, żeby tamto miało ostatnie słowo.
  useEffect(() => {
    if (firstStepRender.current) {
      firstStepRender.current = false;
      return;
    }
    topRef.current?.scrollIntoView({ block: "start" });
  }, [stepId]);

  // Po nieudanym zapisie przewiń do pierwszego błędnego pola i ustaw na nim
  // kursor. `aria-invalid` mają tylko pola z błędem, a querySelector zwraca
  // pierwsze w kolejności DOM. Czyli najwyżej na stronie.
  useEffect(() => {
    if (errorSignal === 0) return;
    const invalid = document.querySelector<HTMLElement>('[aria-invalid="true"]');
    if (invalid) {
      invalid.scrollIntoView({ behavior: "smooth", block: "center" });
      invalid.focus({ preventScroll: true });
    }
  }, [errorSignal]);

  // Tryb edycji: wczytaj ofertę i rozłóż ją z powrotem na pola formularza.
  useEffect(() => {
    if (!id) return;
    fetchProperty(id)
      .then((p) => {
        setFields({
          propertyType: p.propertyType,
          transactionType: p.transactionType,
          marketType: p.marketType,
          status: p.status,
          title: p.title,
          description: p.description,
          price: str(p.pricing.price),
          pricePerM2: str(p.pricing.pricePerSquareMeter),
          priceCurrency: p.pricing.priceCurrency,
          rent: str(p.pricing.rent),
          deposit: str(p.pricing.deposit),
          commissionPercent: str(p.pricing.commissionPercent),
          totalArea: str(p.area.totalArea),
          usableArea: str(p.area.usableArea),
          plotArea: str(p.area.plotArea),
          roomsCount: str(p.area.roomsCount),
          bathroomsCount: str(p.area.bathroomsCount),
          floorNo: str(p.area.floorNo),
          buildingFloorsCount: str(p.area.buildingFloorsCount),
          ceilingHeight: str(p.area.ceilingHeight),
          voivodeship: p.address.voivodeship,
          county: p.address.county ?? "",
          commune: p.address.commune ?? "",
          city: p.address.city,
          district: p.address.district ?? "",
          street: p.address.street ?? "",
          buildingNumber: p.address.buildingNumber ?? "",
          postalCode: p.address.postalCode ?? "",
          buildYear: str(p.building.buildYear),
          buildingType: p.building.buildingType ?? "",
          buildingMaterial: p.building.buildingMaterial ?? "",
          constructionStatus: p.building.constructionStatus ?? "",
          ownershipForm: p.building.ownershipForm ?? "",
          windowsType: p.building.windowsType ?? "",
          surroundings: p.building.surroundings ?? "",
          plotType: p.land.plotType ?? "",
          plotDimensions: p.land.dimensions ?? "",
          roadAccess: p.land.roadAccess ?? "",
          zoningPlan: p.land.zoningPlan ?? "",
          hallStructure: p.commercial.structure ?? "",
          flooring: p.commercial.flooring ?? "",
          parkingType: p.commercial.parkingType ?? "",
          powerConnectionKw: str(p.commercial.powerConnectionKw),
          floorLoadPerM2: str(p.commercial.floorLoadPerM2),
          loadingDocksCount: str(p.commercial.loadingDocksCount),
          garageType: p.garageType ?? "",
          occupants: str(p.occupants),
          roomBathroom: p.roomBathroom ?? "",
          energyPrimary: str(p.energy.energyPrimary),
          energyFinal: str(p.energy.energyFinal),
          energyClass: p.energy.energyClass ?? "",
          energyCertNumber: p.energy.certificateNumber ?? "",
          availableFrom: p.availableFrom ?? "",
          videoUrl: p.videoUrl ?? "",
          panoramaUrl: p.panoramaUrl ?? "",
          keysInfo: p.keysInfo ?? "",
          privateNotes: p.privateNotes ?? "",
          energyExemptNote: p.energy.exemptNote ?? "",
        });
        setFeatures(new Set(p.features));
        setHeating(new Set(p.heatingTypes));
        setUses(new Set(p.commercialUses));
        setFlags({
          priceNegotiable: p.pricing.priceNegotiable,
          priceIncludesRent: p.pricing.priceIncludesRent,
          hideExactAddress: p.address.hideExactAddress,
          furnished: p.building.furnished ?? false,
          plotFenced: p.land.fenced ?? false,
          officeSpace: p.commercial.officeSpace ?? false,
          socialFacilities: p.commercial.socialFacilities ?? false,
          loadingRamp: p.commercial.loadingRamp ?? false,
          energyExempt: p.energy.exempt,
          exportable: p.exportable,
        });
        setMedia(p.media);
        setVisited(
          new Set<StepId>([
            "basic",
            "area",
            "location",
            "price",
            "details",
            "features",
            "media",
            "description",
            "summary",
          ]),
        );

        // Zapisana pinezka opisuje zapisany adres, więc wchodząc w edycję nie
        // mamy czego szukać. Dopiero zmiana pola albo ruch pinezką coś zmienia.
        if (p.address.latitude != null && p.address.longitude != null) {
          setCoords({ lat: p.address.latitude, lng: p.address.longitude });
          pinnedSignature.current = signatureOf({
            voivodeship: p.address.voivodeship,
            city: p.address.city,
            district: p.address.district ?? "",
            street: p.address.street ?? "",
            buildingNumber: p.address.buildingNumber ?? "",
            postalCode: p.address.postalCode ?? "",
          });
          pinnedCity.current = p.address.city;
        }
      })
      .catch(() => setFormError("Nie udało się wczytać oferty do edycji."));
  }, [id]);

  const set = (name: string) => (event: { target: { value: string } }) =>
    setFields((current) => ({ ...current, [name]: event.target.value }));

  // Pola całkowite (pokoje, łazienki, piętro, liczba pięter, rok). Przepuszczamy
  // wyłącznie cyfry. `allowNegative` dla piętra (-1 = suterena): minus tylko na początku.
  const setInt =
    (name: string, allowNegative = false) =>
    (event: { target: { value: string } }) => {
      const raw = event.target.value;
      const negative = allowNegative && raw.trimStart().startsWith("-");
      const digits = raw.replace(/\D/g, "");
      setFields((current) => ({
        ...current,
        [name]: (negative ? "-" : "") + digits,
      }));
    };

  // Kod pocztowy. Myślnik dopisuje maska. Wartość i karetkę ustawiamy na
  // elemencie od razu, przed setState: gdy DOM ma już to, co React zaraz
  // wyrenderuje, React nie tknie pola i karetka nie ucieka na koniec przy
  // poprawianiu cyfry w środku.
  const setPostalCode = (event: ChangeEvent<HTMLInputElement>) => {
    const input = event.target;
    const { value, caret } = maskPostalCode(
      input.value,
      input.selectionStart ?? input.value.length,
      fields.postalCode,
    );
    input.value = value;
    input.setSelectionRange(caret, caret);
    setFields((current) => ({ ...current, postalCode: value }));
  };

  // --- mapa: pinezka → pola --------------------------------------------------
  // Postawienie pinezki jest jednoznacznym „to jest to miejsce", więc adres
  // spod niej zastępuje zawartość pól. Zostawianie starych wartości obok nowej
  // pinezki dałoby ofertę, w której opis i mapa pokazują dwa różne miejsca.
  const handlePick = useCallback(({ lat, lng }: LatLng) => {
    setCoords({ lat, lng });
    setGeoError(null);
    setGeoBusy(true);

    reverseGeocode(lat, lng)
      .then((found) => {
        if (!found) {
          setGeoNote("W tym punkcie nie ma adresu. Pola zostawiam bez zmian.");
          return;
        }

        const current = fieldsRef.current;
        // Czego OSM nie zna dla tego punktu, tego nie ma. Pole zostaje puste.
        // Zachowanie starej wartości obok nowej pinezki dawało adres zszyty
        // z dwóch miejsc: powiat pruszkowski przy krakowskiej ulicy wyglądał
        // na wpisany świadomie, a był resztką po poprzednim kliknięciu.
        // Dotyczy też województwa: pinezka postawiona za granicą ma zostawić
        // puste pole, a nie poprzednie polskie województwo.
        const merged: Fields = {
          ...current,
          voivodeship: found.voivodeship ?? "",
          county: found.county ?? "",
          commune: found.commune ?? "",
          city: found.city ?? "",
          district: found.district ?? "",
          street: found.street ?? "",
          buildingNumber: found.buildingNumber ?? "",
          postalCode: found.postalCode ?? "",
        };

        setFields(merged);
        pinnedSignature.current = signatureOf(geoQueryOf(merged));
        pinnedCity.current = merged.city;
        setGeoNote(found.displayName ?? null);
      })
      .catch((error) =>
        setGeoError(
          error instanceof ApiError
            ? error.message
            : "Nie udało się odczytać adresu z mapy.",
        ),
      )
      .finally(() => setGeoBusy(false));
  }, []);

  /**
   * Zdejmuje pinezkę bez ruszania pól. Bieżący adres zapamiętujemy jako
   * „obsłużony", inaczej automat postawiłby ją z powrotem sekundę później.
   */
  const clearPin = () => {
    setCoords(null);
    setGeoNote(null);
    setGeoError(null);
    pinnedSignature.current = signatureOf(geoQueryOf(fieldsRef.current));
    pinnedCity.current = fieldsRef.current.city;
  };

  // --- mapa: pola → pinezka --------------------------------------------------
  const addressSignature = signatureOf(geoQueryOf(fields));
  const addressGeocodable = canGeocode(geoQueryOf(fields));

  useEffect(() => {
    if (!addressGeocodable) return;
    // Te pola sami wpisaliśmy z mapy. Pinezka już tam stoi.
    if (addressSignature === pinnedSignature.current) return;

    const timer = setTimeout(() => {
      const query = geoQueryOf(fieldsRef.current);
      if (!canGeocode(query)) return;

      setGeoBusy(true);
      setGeoError(null);

      searchLocation(query)
        .then((found) => {
          const best = found[0];
          if (!best) {
            setGeoNote(
              "Nie znaleziono tego adresu na mapie. Pinezkę można postawić ręcznie.",
            );
            // Zapamiętujemy mimo braku wyniku, żeby nie pytać o to samo w kółko.
            pinnedSignature.current = addressSignature;
            pinnedCity.current = fieldsRef.current.city;
            return;
          }

          setCoords({ lat: best.latitude, lng: best.longitude });

          const current = fieldsRef.current;

          // Powiat, gmina, dzielnica i kod pocztowy wynikają z miejscowości.
          // Dopóki miejscowość się nie zmieniła, tylko uzupełniamy puste pola.
          // Agent zna adres z rozmowy z właścicielem i nie ma powodu poprawiać
          // mu tego, co wpisał świadomie. Gdy miejscowość się zmieniła, stare
          // wartości opisują już inne miejsce i zostają zastąpione: inaczej
          // oferta w Krakowie zostawałaby z powiatem pruszkowskim.
          // Pierwsze szukanie w tym formularzu to nie jest „przeprowadzka".
          // Nie ma jeszcze poprzedniej miejscowości, więc nic nie zdezaktualizowało
          // tego, co agent zdążył wpisać.
          const moved =
            pinnedCity.current !== null &&
            !sameCity(pinnedCity.current, current.city);
          const derived = (mine: string, found: string | undefined) =>
            moved ? (found ?? "") : mine.trim() || found || "";

          const merged: Fields = {
            ...current,
            county: derived(current.county, best.county),
            commune: derived(current.commune, best.commune),
            district: derived(current.district, best.district),
            postalCode: derived(current.postalCode, best.postalCode),
          };
          setFields(merged);
          pinnedSignature.current = signatureOf(geoQueryOf(merged));
          pinnedCity.current = merged.city;
          setGeoNote(best.displayName ?? null);
        })
        .catch((error) =>
          setGeoError(
            error instanceof ApiError
              ? error.message
              : "Nie udało się znaleźć adresu na mapie.",
          ),
        )
        .finally(() => setGeoBusy(false));
    }, GEOCODE_DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [addressSignature, addressGeocodable]);

  // --- cena ↔ cena za m² -----------------------------------------------------
  // Liczba z pola (przecinek → kropka); null gdy puste/niepoprawne.
  const parseNum = (v: string): number | null => {
    const t = v.trim().replace(",", ".");
    if (t === "" || Number.isNaN(Number(t))) return null;
    return Number(t);
  };
  const round2 = (x: number) => (Math.round(x * 100) / 100).toString();
  const round0 = (x: number) => Math.round(x).toString();

  // Cena wpisana ręcznie → przelicz cenę za m² (cena ÷ powierzchnia).
  const onPriceChange = (event: { target: { value: string } }) => {
    const price = event.target.value;
    setPriceDriver("price");
    const p = parseNum(price);
    const area = parseNum(fields.totalArea);
    setFields((c) => ({
      ...c,
      price,
      pricePerM2: p != null && area != null && area > 0 ? round2(p / area) : c.pricePerM2,
    }));
  };

  // Cena za m² wpisana ręcznie → przelicz cenę (powierzchnia × m², pełne złote).
  const onPricePerM2Change = (event: { target: { value: string } }) => {
    const pricePerM2 = event.target.value;
    setPriceDriver("ppm2");
    const ppm2 = parseNum(pricePerM2);
    const area = parseNum(fields.totalArea);
    setFields((c) => ({
      ...c,
      pricePerM2,
      price: ppm2 != null && area != null && area > 0 ? round0(ppm2 * area) : c.price,
    }));
  };

  // Zmiana powierzchni przelicza to pole, którego użytkownik NIE wpisał ostatnio.
  const onTotalAreaChange = (event: { target: { value: string } }) => {
    const totalArea = event.target.value;
    const area = parseNum(totalArea);
    setFields((c) => {
      if (area == null || area <= 0) return { ...c, totalArea };
      if (priceDriver === "ppm2") {
        const ppm2 = parseNum(c.pricePerM2);
        return { ...c, totalArea, price: ppm2 != null ? round0(ppm2 * area) : c.price };
      }
      const p = parseNum(c.price);
      return { ...c, totalArea, pricePerM2: p != null ? round2(p / area) : c.pricePerM2 };
    });
  };

  // Oznacza pole jako „dotknięte" (opuszczone). Od tej chwili jego błąd jest
  // widoczny i aktualizuje się na żywo przy każdej zmianie.
  const markTouched = (key: string) => () =>
    setTouched((current) =>
      current.has(key) ? current : new Set(current).add(key),
    );

  // Nazwy własne (miejscowość, ulica…) porządkujemy przy opuszczeniu pola:
  // każde słowo z wielkiej litery, reszta mała. Także po myślniku (Bielsko-Biała).
  // Robimy to na blur, a nie przy każdym znaku, żeby nie przeszkadzać w pisaniu.
  const capitalizeOnBlur =
    (name: string, errorKey?: string) => () => {
      setFields((current) => ({
        ...current,
        [name]: titleCase(current[name]),
      }));
      if (errorKey) markTouched(errorKey)();
    };

  // Numer budynku. Polska konwencja to wielka litera dodatkowa (12A, nie 12a),
  // a że litera stoi po cyfrze, Title Case by jej nie podniósł. Stąd pełne wielkie.
  const upperCaseOnBlur =
    (name: string, errorKey?: string) => () => {
      setFields((current) => ({
        ...current,
        [name]: current[name].toLocaleUpperCase("pl"),
      }));
      if (errorKey) markTouched(errorKey)();
    };

  const toggle = (
    collection: Set<string>,
    setter: (next: Set<string>) => void,
    value: string,
  ) => {
    const next = new Set(collection);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    setter(next);
  };

  const type = fields.propertyType;
  const isRent = fields.transactionType === "RENT";
  const isMieszkanie = type === "APARTMENT";
  const isDom = type === "HOUSE";
  const isDzialka = type === "PLOT";
  const isLokal = type === "COMMERCIAL_UNIT";
  const isHala = type === "HALL_WAREHOUSE";
  const isGaraz = type === "GARAGE";
  const isPokoj = type === "ROOM";

  // Widoczność pól sekcji „Powierzchnia i układ" zależnie od typu obiektu.
  const showUsableArea = isMieszkanie || isDom || isLokal || isHala;
  const showPlotArea = isDom; // działka używa pola „powierzchnia" jako pow. działki
  const showRooms = isMieszkanie || isDom || isLokal;
  const roomsLabel = isLokal ? "Liczba pomieszczeń" : "Liczba pokoi";
  const showBaths = isMieszkanie || isDom || isLokal;
  const showFloor = isMieszkanie || isLokal || isPokoj || isGaraz;
  const floorLabel = isGaraz ? "Poziom" : "Piętro";
  const showBuildingFloors = isMieszkanie || isDom || isLokal;
  const showCeiling = isLokal || isHala;
  const totalAreaLabel = isDzialka
    ? "Powierzchnia działki (m²)"
    : isGaraz
      ? "Powierzchnia (m²)"
      : isPokoj
        ? "Powierzchnia pokoju (m²)"
        : "Powierzchnia całkowita (m²)";

  // Cena za m² nie ma sensu dla garażu i pokoju (wycena nie jest metrażowa).
  const showPricePerM2 = !isGaraz && !isPokoj;

  // Sekcje i pola sekcji „Budynek/Szczegóły".
  const buildingFull = isMieszkanie || isDom; // pełny zestaw pól budynku
  const showBuildingSection = !isDzialka;
  const showBuildYear = isMieszkanie || isDom || isLokal || isHala;
  const showConstructionStatus = isMieszkanie || isDom || isLokal || isHala;
  const showOwnershipForm = isMieszkanie || isDom || isLokal || isHala || isGaraz;
  const showFurnished = isMieszkanie || isDom || isPokoj;
  const showAvailableFrom = !isDzialka;
  const buildingTitle = isGaraz || isPokoj ? "Szczegóły" : "Budynek";

  const showLandSection = isDom || isDzialka;
  const showCommercialSection = isLokal || isHala;
  const showHeating = isMieszkanie || isDom || isLokal || isHala;
  const showEnergy = isMieszkanie || isDom || isLokal || isHala;
  // Pola wymagane zależnie od typu. Otodom odrzuca mieszkanie i dom bez liczby
  // pokoi; dla mieszkania wymagamy też łazienek, piętra i liczby pięter.
  const roomsRequired = isMieszkanie || isDom;
  const bathsRequired = isMieszkanie || isDom;
  const floorRequired = isMieszkanie;
  const buildingFloorsRequired = isMieszkanie;

  const options = useMemo(
    () => ({
      propertyType: dict?.propertyType ?? [],
      transactionType: dict?.transactionType ?? [],
      marketType: dict?.marketType ?? [],
      status: dict?.status ?? [],
      currency: dict?.currency ?? [],
      // Nazwy województw w słowniku są z małej litery (poprawnie ortograficznie),
      // ale w liście wyboru czytelniej wyglądają z wielkiej.
      voivodeship: (dict?.voivodeship ?? []).map((o) => ({
        ...o,
        label: o.label.charAt(0).toUpperCase() + o.label.slice(1),
      })),
      buildingType: dict?.buildingType ?? [],
      buildingMaterial: dict?.buildingMaterial ?? [],
      constructionStatus: dict?.constructionStatus ?? [],
      ownershipForm: dict?.ownershipForm ?? [],
      windowsType: dict?.windowsType ?? [],
      surroundings: dict?.surroundings ?? [],
      plotType: dict?.plotType ?? [],
      roadAccess: dict?.roadAccess ?? [],
      hallStructure: dict?.hallStructure ?? [],
      flooring: dict?.flooring ?? [],
      parkingType: dict?.parkingType ?? [],
      garageType: dict?.garageType ?? [],
      roomBathroom: dict?.roomBathroom ?? [],
      energyClass: dict?.energyClass ?? [],
    }),
    [dict],
  );

  // Cechy przefiltrowane pod wybrany typ obiektu. Pokazujemy tylko te, które
  // dla niego mają sens, i pomijamy kategorie, które po filtrze są puste.
  const featureGroups = useMemo(() => {
    if (!dict) return [];
    return dict.featureGroups
      .map((g) => ({
        ...g,
        features: g.features.filter((f) => f.types.includes(type)),
      }))
      .filter((g) => g.features.length > 0);
  }, [dict, type]);

  // Wartości cech dozwolonych dla bieżącego typu. Do przycięcia wyboru przy zapisie.
  const allowedFeatureValues = useMemo(
    () => new Set(featureGroups.flatMap((g) => g.features.map((f) => f.value))),
    [featureGroups],
  );

  // Walidacja realnych zakresów. Bean Validation na backendzie pilnuje reszty,
  // ale sensowne granice („piętro do 154", „rok budowy nie z przyszłości") lepiej
  // pokazać od razu przy polu, zanim żądanie w ogóle poleci.
  // `forSubmit` = true dopiero po kliknięciu „Zapisz". Tylko wtedy zgłaszamy
  // braki w polach wymaganych i wartości „w trakcie pisania" (np. samą kropkę),
  // żeby komunikat nie migał przy każdym znaku podczas wypełniania.
  function validate(forSubmit: boolean): Record<string, string> {
    const e: Record<string, string> = {};
    const n = (v: string): number | null => {
      const t = v.trim().replace(",", ".");
      return t === "" ? null : Number(t);
    };
    // Reguła „maks. 2 miejsca po przecinku": łapie 3+ cyfry po przecinku/kropce.
    const tooManyDecimals = (v: string): boolean => /[.,]\d{3,}/.test(v.trim());
    const maxYear = new Date().getFullYear() + 10;

    // Pola wymagane. Część pojawia się warunkowo (działka nie ma pięter/pokoi),
    // więc pilnujemy ich tylko, gdy są widoczne. Braki zgłaszamy przy zapisie.
    if (forSubmit) {
      const required: [string, string, boolean][] = [
        ["title", "title", true],
        ["description", "description", true],
        ["price", "pricing.price", true],
        ["pricePerM2", "pricing.pricePerM2", showPricePerM2],
        ["totalArea", "area.totalArea", true],
        ["roomsCount", "area.roomsCount", roomsRequired],
        ["bathroomsCount", "area.bathroomsCount", bathsRequired],
        ["floorNo", "area.floorNo", floorRequired],
        ["buildingFloorsCount", "area.buildingFloorsCount", buildingFloorsRequired],
        ["city", "address.city", true],
        ["postalCode", "address.postalCode", true],
        // Ulica jest opcjonalna: we wsiach bez nazw ulic adres to sama
        // miejscowość i numer („Nowa Wieś 12"). Backend jej nie wymaga,
        // a `readyForExport()` też nie. Formularz był tu surowszy niż
        // reszta systemu i wypychał takie oferty do notatnika.
        ["buildingNumber", "address.buildingNumber", true],
      ];
      for (const [field, key, active] of required)
        if (active && fields[field].trim() === "")
          e[key] = "To pole jest wymagane.";
    }

    const priceRaw = fields.price.trim();
    if (priceRaw !== "") {
      const price = n(fields.price);
      if (price == null || Number.isNaN(price)) {
        if (forSubmit) e["pricing.price"] = "Podaj poprawną cenę.";
      } else if (price <= 0)
        e["pricing.price"] = "Podaj cenę większą od zera.";
      else if (price > 9_999_999_999)
        e["pricing.price"] = "Cena jest nierealnie wysoka.";
    }

    const ppm2Raw = fields.pricePerM2.trim();
    if (ppm2Raw !== "") {
      const ppm2 = n(fields.pricePerM2);
      if (ppm2 == null || Number.isNaN(ppm2)) {
        if (forSubmit) e["pricing.pricePerM2"] = "Podaj poprawną cenę za m².";
      } else if (ppm2 <= 0)
        e["pricing.pricePerM2"] = "Cena za m² musi być większa od zera.";
      else if (ppm2 > 9_999_999_999)
        e["pricing.pricePerM2"] = "Cena za m² jest nierealnie wysoka.";
    }

    const rent = n(fields.rent);
    if (rent != null && (Number.isNaN(rent) || rent < 0))
      e["pricing.rent"] = "Czynsz nie może być ujemny.";
    else if (rent != null && rent > 1_000_000)
      e["pricing.rent"] = "Czynsz jest nierealnie wysoki.";

    const deposit = n(fields.deposit);
    if (deposit != null && (Number.isNaN(deposit) || deposit < 0))
      e["pricing.deposit"] = "Kaucja nie może być ujemna.";

    const commission = n(fields.commissionPercent);
    if (commission != null && (Number.isNaN(commission) || commission < 0 || commission > 100))
      e["pricing.commissionPercent"] = "Prowizja musi być w zakresie 0–100%.";

    let total: number | null = null;
    const totalRaw = fields.totalArea.trim();
    if (totalRaw !== "") {
      total = n(fields.totalArea);
      if (total == null || Number.isNaN(total)) {
        if (forSubmit) e["area.totalArea"] = "Podaj poprawną powierzchnię.";
        total = null;
      } else if (total <= 0)
        e["area.totalArea"] = "Podaj powierzchnię większą od zera.";
      else if (total > 1_000_000)
        e["area.totalArea"] = "Powierzchnia jest nierealnie duża.";
    }

    const usable = n(fields.usableArea);
    if (usable != null && (Number.isNaN(usable) || usable <= 0))
      e["area.usableArea"] = "Powierzchnia użytkowa musi być większa od zera.";
    else if (usable != null && total != null && usable > total)
      e["area.usableArea"] = "Powierzchnia użytkowa nie może przekraczać całkowitej.";

    const plot = n(fields.plotArea);
    if (plot != null && (Number.isNaN(plot) || plot <= 0))
      e["area.plotArea"] = "Powierzchnia działki musi być większa od zera.";

    const rooms = n(fields.roomsCount);
    if (rooms != null && (!Number.isInteger(rooms) || rooms < 1 || rooms > 100))
      e["area.roomsCount"] = "Liczba pokoi musi być z zakresu 1–100.";

    const baths = n(fields.bathroomsCount);
    if (baths != null && (!Number.isInteger(baths) || baths < 0 || baths > 50))
      e["area.bathroomsCount"] = "Liczba łazienek musi być z zakresu 0–50.";

    const floor = n(fields.floorNo);
    if (floor != null && (!Number.isInteger(floor) || floor < -1 || floor > 160))
      e["area.floorNo"] = "Piętro musi być z zakresu od -1 (suterena) do 160.";

    const buildingFloors = n(fields.buildingFloorsCount);
    if (buildingFloors != null && (!Number.isInteger(buildingFloors) || buildingFloors < 1 || buildingFloors > 200))
      e["area.buildingFloorsCount"] = "Liczba pięter musi być z zakresu 1–200.";

    if (
      floor != null && buildingFloors != null &&
      Number.isInteger(floor) && Number.isInteger(buildingFloors) &&
      floor > buildingFloors
    )
      e["area.floorNo"] = "Piętro nie może być wyższe niż liczba pięter w budynku.";

    const ceiling = n(fields.ceilingHeight);
    if (ceiling != null && (Number.isNaN(ceiling) || ceiling < 1 || ceiling > 50))
      e["area.ceilingHeight"] = "Wysokość pomieszczeń musi być z zakresu 1–50 m.";

    const year = n(fields.buildYear);
    if (year != null && (!Number.isInteger(year) || year < 1800 || year > maxYear))
      e["building.buildYear"] = `Rok budowy musi być z zakresu 1800–${maxYear}.`;

    const ep = n(fields.energyPrimary);
    if (ep != null && (Number.isNaN(ep) || ep < 0 || ep > 5000))
      e["energy.energyPrimary"] = "Wskaźnik EP jest poza realnym zakresem (0–5000).";

    const ek = n(fields.energyFinal);
    if (ek != null && (Number.isNaN(ek) || ek < 0 || ek > 5000))
      e["energy.energyFinal"] = "Wskaźnik EK jest poza realnym zakresem (0–5000).";

    const postal = fields.postalCode.trim();
    if (postal !== "" && !/^\d{2}-\d{3}$/.test(postal))
      e["address.postalCode"] = "Kod pocztowy w formacie 00-000.";

    // Wszystkie pola dziesiętne: maksymalnie 2 miejsca po przecinku. Sprawdzamy
    // na końcu, więc ten komunikat wygrywa z ewentualnym błędem zakresu.
    const decimalFields: [string, string][] = [
      ["price", "pricing.price"],
      ["pricePerM2", "pricing.pricePerM2"],
      ["rent", "pricing.rent"],
      ["deposit", "pricing.deposit"],
      ["commissionPercent", "pricing.commissionPercent"],
      ["totalArea", "area.totalArea"],
      ["usableArea", "area.usableArea"],
      ["plotArea", "area.plotArea"],
      ["ceilingHeight", "area.ceilingHeight"],
      ["powerConnectionKw", "commercial.powerConnectionKw"],
      ["floorLoadPerM2", "commercial.floorLoadPerM2"],
      ["energyPrimary", "energy.energyPrimary"],
      ["energyFinal", "energy.energyFinal"],
    ];
    for (const [field, key] of decimalFields) {
      if (tooManyDecimals(fields[field]))
        e[key] = "Maksymalnie 2 miejsca po przecinku.";
    }

    return e;
  }

  // Wynik walidacji klienta liczony przy każdym renderze. Zawsze świeży.
  // `strictErrors` zawiera też braki w polach wymaganych; `softErrors` tylko
  // błędy wartości, które pokazujemy w trakcie pisania.
  const strictErrors = validate(true);
  const softErrors = validate(false);

  // Błąd pokazujemy, gdy: backend go zwrócił, albo agent próbował już zapisać
  // ofertę lub przejść dalej z tego etapu (wtedy też braki w wymaganych),
  // albo opuścił to pole. Komunikat pojawia się od razu, nie dopiero przy zapisie.
  const errorFor = (key: string): string | undefined =>
    errors[key] ??
    (submitted || attempted.has(stepOfKey(key))
      ? strictErrors[key]
      : touched.has(key)
        ? softErrors[key]
        : undefined);

  // --- etapy -----------------------------------------------------------------
  const detailsTitle = isDzialka
    ? "Szczegóły działki"
    : isGaraz || isPokoj
      ? "Szczegóły"
      : "Budynek i wyposażenie";

  const steps: StepDef[] = [
    {
      id: "basic",
      title: "Informacje podstawowe",
      icon: Home,
    },
    { id: "area", title: "Powierzchnia i układ", icon: Ruler },
    {
      id: "location",
      title: "Lokalizacja",
      description:
        "Do zapisu wystarczą województwo i miejscowość. Powiat jest potrzebny dopiero do wysyłki na portal.",
      icon: MapPin,
    },
    {
      id: "price",
      title: "Cena nieruchomości",
      description:
        "Podaj cenę albo cenę za m². Drugą wartość policzymy z powierzchni.",
      icon: Banknote,
    },
    { id: "details", title: detailsTitle, icon: Building2 },
    // Bez słowników (albo dla typu bez cech) etap byłby pusty. Pomijamy go.
    ...(featureGroups.length > 0
      ? [{ id: "features" as const, title: "Cechy", icon: ListChecks }]
      : []),
    { id: "media", title: "Zdjęcia i multimedia", icon: Images },
    {
      id: "description",
      title: "Opis ogłoszenia",
      icon: FileText,
    },
    {
      id: "summary",
      title: "Podgląd i zapis",
      icon: ClipboardCheck,
    },
  ];

  // Etap mógł zniknąć (np. „Cechy" po zmianie rodzaju). Wracamy wtedy na
  // pierwszy, zamiast renderować pustkę.
  const stepIndex = Math.max(
    0,
    steps.findIndex((s) => s.id === stepId),
  );
  const step = steps[stepIndex];
  const isLastStep = stepIndex === steps.length - 1;
  const StepIcon = step.icon;

  const stepHasErrors = (id: StepId): boolean =>
    Object.keys(strictErrors).some((k) => stepOfKey(k) === id) ||
    Object.keys(errors).some((k) => stepOfKey(k) === id);

  const stateOf = (id: StepId): StepState => {
    if (id === step.id) return "current";
    if ((submitted || attempted.has(id)) && stepHasErrors(id)) return "error";
    if (visited.has(id)) return stepHasErrors(id) ? "todo" : "done";
    return "todo";
  };

  const goTo = (id: StepId) => {
    setStepId(id);
    setVisited((current) =>
      current.has(id) ? current : new Set(current).add(id),
    );
  };

  // „Dalej" puszcza tylko wtedy, gdy bieżący etap jest poprawny. Inaczej
  // pokazujemy braki i ustawiamy kursor na pierwszym z nich.
  // Błędy z backendu nie blokują. Znikają dopiero przy kolejnym zapisie,
  // więc po ich poprawieniu agent utknąłby na etapie.
  const goNext = () => {
    if (Object.keys(strictErrors).some((k) => stepOfKey(k) === step.id)) {
      setAttempted((current) => new Set(current).add(step.id));
      setErrorSignal((s) => s + 1);
      return;
    }
    const next = steps[stepIndex + 1];
    if (next) goTo(next.id);
  };

  const goBack = () => {
    const previous = steps[stepIndex - 1];
    if (previous) setStepId(previous.id);
  };

  /** Przenosi na pierwszy (w kolejności kreatora) etap z błędem. */
  const jumpToFirstError = (keys: string[]) => {
    const bad = new Set(keys.map(stepOfKey));
    const first = steps.find((s) => bad.has(s.id));
    if (first) goTo(first.id);
    // Wyzwól przewinięcie i ustawienie kursora na pierwszym błędnym polu.
    setErrorSignal((s) => s + 1);
  };

  // --- podgląd ---------------------------------------------------------------
  const labelOf = (list: { value: string; label: string }[], value: string) =>
    value === "" ? "" : (list.find((o) => o.value === value)?.label ?? value);
  const amount = (value: string, unit: string) => {
    const parsed = num(value);
    return parsed == null || Number.isNaN(parsed)
      ? ""
      : `${parsed.toLocaleString("pl-PL")} ${unit}`;
  };
  const photosCount = isEdit ? media.length : draftFiles.length;

  const summaryRows: SummaryRow[] = [
    { label: "Rodzaj nieruchomości", value: labelOf(options.propertyType, fields.propertyType), step: "basic" },
    { label: "Transakcja", value: labelOf(options.transactionType, fields.transactionType), step: "basic" },
    { label: "Rynek", value: labelOf(options.marketType, fields.marketType), step: "basic" },
    { label: "Status", value: labelOf(options.status, fields.status), step: "basic" },
    { label: "Powierzchnia", value: amount(fields.totalArea, "m²"), step: "area" },
    ...(showRooms
      ? [{ label: roomsLabel, value: fields.roomsCount, step: "area" as const }]
      : []),
    {
      label: "Adres",
      value: [
        [fields.street, fields.buildingNumber].filter(Boolean).join(" "),
        [fields.postalCode, fields.city].filter(Boolean).join(" "),
      ]
        .filter(Boolean)
        .join(", "),
      step: "location",
    },
    { label: "Cena", value: amount(fields.price, fields.priceCurrency), step: "price" },
    ...(showPricePerM2
      ? [
          {
            label: "Cena za m²",
            value: amount(fields.pricePerM2, `${fields.priceCurrency}/m²`),
            step: "price" as const,
          },
        ]
      : []),
    {
      label: "Zdjęcia",
      value: photosCount > 0 ? String(photosCount) : "",
      step: "media",
    },
    { label: "Tytuł ogłoszenia", value: fields.title, step: "description" },
  ];

  // Etapy z brakami. Na podglądzie podajemy je wprost, z linkiem do poprawki.
  const incompleteSteps = steps
    .filter((s) => s.id !== "summary" && stepHasErrors(s.id))
    .map((s) => ({ id: s.id, title: s.title }));

  // Enter w polu tekstowym wysyła formularz. W środku kreatora znaczy „Dalej".
  function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (isLastStep) void save();
    else goNext();
  }

  async function save() {
    setSubmitted(true);
    setErrors({});
    setFormError(null);

    const fieldErrors = validate(true);
    if (Object.keys(fieldErrors).length > 0) {
      setFormError("Popraw zaznaczone pola formularza.");
      jumpToFirstError(Object.keys(fieldErrors));
      return;
    }

    setSaving(true);

    try {
      const payload = {
        propertyType: fields.propertyType,
        transactionType: fields.transactionType,
        marketType: fields.marketType,
        status: fields.status,
        title: fields.title,
        description: fields.description,
        pricing: {
          price: num(fields.price) ?? 0,
          priceCurrency: fields.priceCurrency,
          priceNegotiable: flags.priceNegotiable,
          rent: num(fields.rent),
          priceIncludesRent: flags.priceIncludesRent,
          deposit: num(fields.deposit),
          commissionPercent: num(fields.commissionPercent),
          // Garaż i pokój nie mają ceny za m². Wtedy null (backend nie wymaga).
          pricePerM2: showPricePerM2 ? num(fields.pricePerM2) : null,
        },
        area: {
          totalArea: num(fields.totalArea) ?? 0,
          // Pola widoczne tylko dla części typów. Dla pozostałych wysyłamy null,
          // żeby po zmianie rodzaju nie zostały „osierocone" wartości.
          usableArea: showUsableArea ? num(fields.usableArea) : null,
          plotArea: showPlotArea ? num(fields.plotArea) : null,
          roomsCount: showRooms ? int(fields.roomsCount) : null,
          bathroomsCount: showBaths ? int(fields.bathroomsCount) : null,
          floorNo: showFloor ? int(fields.floorNo) : null,
          buildingFloorsCount: showBuildingFloors ? int(fields.buildingFloorsCount) : null,
          ceilingHeight: showCeiling ? num(fields.ceilingHeight) : null,
        },
        address: {
          voivodeship: fields.voivodeship,
          county: blank(fields.county),
          commune: blank(fields.commune),
          city: fields.city,
          district: blank(fields.district),
          street: blank(fields.street),
          buildingNumber: blank(fields.buildingNumber),
          postalCode: blank(fields.postalCode),
          latitude: coords?.lat ?? null,
          longitude: coords?.lng ?? null,
          hideExactAddress: flags.hideExactAddress,
        },
        building: showBuildingSection
          ? {
              buildYear: int(fields.buildYear),
              buildingType: blank(fields.buildingType),
              buildingMaterial: blank(fields.buildingMaterial),
              constructionStatus: blank(fields.constructionStatus),
              ownershipForm: blank(fields.ownershipForm),
              windowsType: blank(fields.windowsType),
              surroundings: blank(fields.surroundings),
              furnished: flags.furnished,
            }
          : undefined,
        land: showLandSection
          ? {
              plotType: blank(fields.plotType),
              dimensions: blank(fields.plotDimensions),
              roadAccess: blank(fields.roadAccess),
              fenced: flags.plotFenced,
              zoningPlan: blank(fields.zoningPlan),
            }
          : undefined,
        commercial: showCommercialSection
          ? {
              structure: blank(fields.hallStructure),
              flooring: blank(fields.flooring),
              parkingType: blank(fields.parkingType),
              officeSpace: flags.officeSpace,
              socialFacilities: flags.socialFacilities,
              loadingRamp: flags.loadingRamp,
              powerConnectionKw: num(fields.powerConnectionKw),
              floorLoadPerM2: num(fields.floorLoadPerM2),
              loadingDocksCount: int(fields.loadingDocksCount),
            }
          : undefined,
        garageType: isGaraz ? blank(fields.garageType) ?? null : null,
        occupants: isPokoj ? int(fields.occupants) : null,
        roomBathroom: isPokoj ? blank(fields.roomBathroom) ?? null : null,
        energy: {
          energyPrimary: flags.energyExempt ? null : num(fields.energyPrimary),
          energyFinal: flags.energyExempt ? null : num(fields.energyFinal),
          energyClass: flags.energyExempt ? undefined : blank(fields.energyClass),
          certificateNumber: blank(fields.energyCertNumber),
          exempt: flags.energyExempt,
          exemptNote: blank(fields.energyExemptNote),
        },
        availableFrom: blank(fields.availableFrom) ?? null,
        // Tylko cechy, które mają sens dla wybranego typu (reszta wycięta).
        features: dict
          ? [...features].filter((f) => allowedFeatureValues.has(f))
          : [...features],
        heatingTypes: [...heating],
        commercialUses: [...uses],
        videoUrl: blank(fields.videoUrl),
        panoramaUrl: blank(fields.panoramaUrl),
        keysInfo: blank(fields.keysInfo),
        privateNotes: blank(fields.privateNotes),
        exportable: flags.exportable,
      };

      const saved =
        isEdit && id
          ? await updateProperty(id, payload)
          : await createProperty(payload);

      // Zdjęcia wybrane przed zapisem czekały w pamięci, bo nie miały do czego
      // się przypiąć. Teraz oferta ma identyfikator, więc lecą jednym żądaniem.
      if (!isEdit && draftFiles.length > 0) {
        try {
          await uploadPropertyMedia(saved.id, draftFiles);
        } catch (cause) {
          // Oferta jest już zapisana i nie wolno jej zgubić przez zdjęcia.
          // Zamiast wracać do listy, zostajemy przy niej w edycji. Pliki są
          // wciąż wybrane w oknie wyboru, a agent widzi, czego brakuje.
          setDraftFiles([]);
          navigate(`/nieruchomosci/${saved.id}/edytuj`, {
            state: {
              mediaError:
                cause instanceof ApiError
                  ? cause.message
                  : "Oferta została zapisana, ale nie udało się wgrać zdjęć.",
            },
          });
          return;
        }
      }

      navigate("/nieruchomosci", { state: { saved: saved.id } });
    } catch (cause) {
      if (cause instanceof ApiError) {
        const backendErrors = cause.fieldErrors ?? {};
        setErrors(backendErrors);
        setFormError(cause.message);
        if (Object.keys(backendErrors).length > 0)
          jumpToFirstError(Object.keys(backendErrors));
      } else {
        setFormError("Nie udało się zapisać oferty.");
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4 pb-10">
      <div ref={topRef} className="scroll-mt-6" aria-hidden="true" />
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => navigate("/nieruchomosci")}
          >
            <ArrowLeft className="size-4" strokeWidth={2} />
            Wróć
          </Button>
          <h1 className="text-base font-semibold tracking-tight text-ink">
            {isEdit ? "Edytuj ofertę" : "Nowa oferta"}
          </h1>
        </div>

        {/* W edycji oferta jest już kompletna. Zapis bez przechodzenia
            przez wszystkie etapy. */}
        {isEdit && (
          <Button type="button" disabled={saving} onClick={() => void save()}>
            {saving ? "Zapisywanie…" : "Zapisz ofertę"}
          </Button>
        )}
      </header>

      {formError && (
        <p className="rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {formError}
        </p>
      )}

      {mediaError && (
        <p className="rounded-md border border-warning/40 bg-warning/10 px-3 py-2 text-[13px] text-ink">
          Oferta została zapisana, ale zdjęcia się nie wgrały: {mediaError}{" "}
          Spróbuj dodać je poniżej.
        </p>
      )}

      {dictError && (
        <p className="flex items-center justify-between gap-3 rounded-md border border-critical/30 bg-critical/8 px-3 py-2 text-[13px] text-critical">
          {dictError}
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={loadDictionaries}
          >
            Spróbuj ponownie
          </Button>
        </p>
      )}

      <div className="grid items-start gap-6 lg:grid-cols-[240px_minmax(0,1fr)]">
        <aside className="lg:sticky lg:top-6">
          <PropertyFormStepper
            steps={steps.map((s) => ({
              id: s.id,
              title: s.title,
              icon: s.icon,
              state: stateOf(s.id),
              reachable: visited.has(s.id),
            }))}
            onSelect={(target) => goTo(target as StepId)}
          />
        </aside>

        <div className="card">
          <header className="flex flex-col items-center gap-3 border-b border-line px-5 py-6 text-center">
            <span className="flex size-14 items-center justify-center rounded-full bg-accent-subtle text-accent">
              <StepIcon className="size-6" strokeWidth={1.75} />
            </span>
            <div>
              <p className="text-[11px] font-medium tracking-wide text-ink-muted uppercase">
                Krok {stepIndex + 1} z {steps.length}
              </p>
              <h2 className="mt-0.5 text-lg font-semibold tracking-tight text-ink">
                {step.title}
              </h2>
              {step.description && (
                <p className="mx-auto mt-1 max-w-xl text-[12px] text-ink-muted">
                  {step.description}
                </p>
              )}
            </div>
          </header>

          {step.id === "basic" && (
            <>
          <Section>
            <Select
              label="Rodzaj nieruchomości"
              options={options.propertyType}
              value={fields.propertyType}
              onChange={set("propertyType")}
              error={errors.propertyType}
            />
            <Select
              label="Typ transakcji"
              options={options.transactionType}
              value={fields.transactionType}
              onChange={set("transactionType")}
              error={errors.transactionType}
            />
            <Select
              label="Rynek"
              options={options.marketType}
              value={fields.marketType}
              onChange={set("marketType")}
              error={errors.marketType}
              hint="Wymagane przez Otodom."
            />
            <Select
              label="Status"
              options={options.status}
              value={fields.status}
              onChange={set("status")}
            />
          </Section>
            </>
          )}

          {step.id === "area" && (
            <>
          <Section>
            <Input
              label={totalAreaLabel}
              type="number"
              inputMode="decimal"
              step="0.01"
              min={0}
              value={fields.totalArea}
              onChange={onTotalAreaChange}
              onBlur={markTouched("area.totalArea")}
              error={errorFor("area.totalArea")}
              required
            />
            {showUsableArea && (
              <Input
                label="Powierzchnia użytkowa (m²)"
                type="number"
                inputMode="decimal"
                step="0.01"
                min={0}
                value={fields.usableArea}
                onChange={set("usableArea")}
                onBlur={markTouched("area.usableArea")}
                error={errorFor("area.usableArea")}
              />
            )}
            {showPlotArea && (
              <Input
                label="Powierzchnia działki (m²)"
                type="number"
                inputMode="decimal"
                step="0.01"
                min={0}
                value={fields.plotArea}
                onChange={set("plotArea")}
                onBlur={markTouched("area.plotArea")}
                error={errorFor("area.plotArea")}
              />
            )}
            {isGaraz && (
              <Select
                label="Typ"
                options={options.garageType}
                value={fields.garageType}
                onChange={set("garageType")}
                placeholder="Wybierz…"
              />
            )}
            {showRooms && (
              <Input
                label={roomsLabel}
                type="text"
                inputMode="numeric"
                value={fields.roomsCount}
                onChange={setInt("roomsCount")}
                onBlur={markTouched("area.roomsCount")}
                error={errorFor("area.roomsCount")}
                hint={
                  roomsRequired
                    ? "Wymagana. Bez niej portal odrzuci ofertę."
                    : undefined
                }
                required={roomsRequired}
              />
            )}
            {showBaths && (
              <Input
                label="Liczba łazienek"
                type="text"
                inputMode="numeric"
                value={fields.bathroomsCount}
                onChange={setInt("bathroomsCount")}
                onBlur={markTouched("area.bathroomsCount")}
                error={errorFor("area.bathroomsCount")}
                required={bathsRequired}
              />
            )}
            {isPokoj && (
              <Input
                label="Dla ilu osób"
                type="text"
                inputMode="numeric"
                value={fields.occupants}
                onChange={setInt("occupants")}
              />
            )}
            {isPokoj && (
              <Select
                label="Łazienka"
                options={options.roomBathroom}
                value={fields.roomBathroom}
                onChange={set("roomBathroom")}
                placeholder="Nie podano"
              />
            )}
            {showFloor && (
              <Input
                label={floorLabel}
                type="text"
                inputMode="numeric"
                value={fields.floorNo}
                onChange={setInt("floorNo", true)}
                onBlur={markTouched("area.floorNo")}
                error={errorFor("area.floorNo")}
                hint="-1 = suterena/podziemie, 0 = parter."
                required={floorRequired}
              />
            )}
            {showBuildingFloors && (
              <Input
                label="Liczba pięter w budynku"
                type="text"
                inputMode="numeric"
                value={fields.buildingFloorsCount}
                onChange={setInt("buildingFloorsCount")}
                onBlur={markTouched("area.buildingFloorsCount")}
                error={errorFor("area.buildingFloorsCount")}
                required={buildingFloorsRequired}
              />
            )}
            {showCeiling && (
              <Input
                label="Wysokość pomieszczeń (m)"
                type="number"
                inputMode="decimal"
                step="0.01"
                min={1}
                max={50}
                value={fields.ceilingHeight}
                onChange={set("ceilingHeight")}
                onBlur={markTouched("area.ceilingHeight")}
                error={errorFor("area.ceilingHeight")}
              />
            )}
          </Section>

            </>
          )}

          {step.id === "location" && (
            <>
          <Section>
            <Select
              label="Województwo"
              options={options.voivodeship}
              value={fields.voivodeship}
              onChange={set("voivodeship")}
              error={errors["address.voivodeship"]}
              placeholder="Wybierz…"
              required
            />
            <Input
              label="Powiat"
              value={fields.county}
              onChange={set("county")}
              onBlur={capitalizeOnBlur("county")}
              error={errors["address.county"]}
            />
            <Input
              label="Gmina"
              value={fields.commune}
              onChange={set("commune")}
              onBlur={capitalizeOnBlur("commune")}
            />
            <Input
              label="Miejscowość"
              value={fields.city}
              onChange={set("city")}
              onBlur={capitalizeOnBlur("city", "address.city")}
              error={errorFor("address.city")}
              required
            />
            <Input
              label="Dzielnica"
              value={fields.district}
              onChange={set("district")}
              onBlur={capitalizeOnBlur("district")}
            />
            <Input
              label="Kod pocztowy"
              value={fields.postalCode}
              onChange={setPostalCode}
              onBlur={markTouched("address.postalCode")}
              error={errorFor("address.postalCode")}
              placeholder="00-000"
              inputMode="numeric"
              maxLength={6}
              required
            />
            <Input
              label="Ulica"
              value={fields.street}
              onChange={set("street")}
              onBlur={capitalizeOnBlur("street", "address.street")}
              error={errorFor("address.street")}
              hint="Wieś bez nazw ulic. Zostaw puste."
            />
            <Input
              label="Numer budynku"
              value={fields.buildingNumber}
              onChange={set("buildingNumber")}
              onBlur={upperCaseOnBlur("buildingNumber", "address.buildingNumber")}
              error={errorFor("address.buildingNumber")}
              required
            />
            <div className="flex items-end pb-2">
              <Check
                label="Ukryj dokładny adres w ogłoszeniu"
                checked={flags.hideExactAddress}
                onChange={(value) =>
                  setFlags((f) => ({ ...f, hideExactAddress: value }))
                }
              />
            </div>

            {/* Mapa na całą szerokość sekcji i na jej dole. Pod polami, które
                uzupełnia, a nie obok nich. */}
            <div className="md:col-span-3">
              <LazyPropertyMap
                position={coords}
                onPick={handlePick}
                height={340}
                busy={geoBusy}
                footer={
                  <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
                    <p
                      className={
                        geoError
                          ? "text-[12px] text-critical"
                          : "text-[12px] text-ink-muted"
                      }
                    >
                      {geoError ??
                        geoNote ??
                        "Kliknij w mapę lub przeciągnij pinezkę, żeby uzupełnić adres. Wpisany ręcznie adres (wystarczy województwo i miejscowość) sam ustawi pinezkę. Zoom: Ctrl + / Ctrl −, gdy kursor jest nad mapą."}
                    </p>
                    {coords && (
                      <div className="flex items-baseline gap-3">
                        <span className="text-[12px] tabular-nums text-ink-muted">
                          {coords.lat.toFixed(6)}, {coords.lng.toFixed(6)}
                        </span>
                        <button
                          type="button"
                          onClick={clearPin}
                          className="text-[12px] text-accent hover:underline"
                        >
                          Usuń pinezkę
                        </button>
                      </div>
                    )}
                  </div>
                }
              />
            </div>
          </Section>

            </>
          )}

          {step.id === "price" && (
            <>
          <Section>
            <Input
              label="Cena"
              type="number"
              inputMode="decimal"
              step="0.01"
              min={0}
              value={fields.price}
              onChange={onPriceChange}
              onBlur={markTouched("pricing.price")}
              error={errorFor("pricing.price")}
              required
            />
            {showPricePerM2 && (
              <Input
                label="Cena za m²"
                type="number"
                inputMode="decimal"
                step="0.01"
                min={0}
                value={fields.pricePerM2}
                onChange={onPricePerM2Change}
                onBlur={markTouched("pricing.pricePerM2")}
                error={errorFor("pricing.pricePerM2")}
                required
              />
            )}
            <Select
              label="Waluta"
              options={options.currency}
              value={fields.priceCurrency}
              onChange={set("priceCurrency")}
            />
            <Input
              label="Prowizja biura (%)"
              type="number"
              inputMode="decimal"
              step="0.01"
              min={0}
              max={100}
              value={fields.commissionPercent}
              onChange={set("commissionPercent")}
              onBlur={markTouched("pricing.commissionPercent")}
              error={errorFor("pricing.commissionPercent")}
              hint="Nie trafia do ogłoszenia."
            />
            <Input
              label="Czynsz administracyjny"
              type="number"
              inputMode="decimal"
              step="0.01"
              min={0}
              value={fields.rent}
              onChange={set("rent")}
              onBlur={markTouched("pricing.rent")}
              error={errorFor("pricing.rent")}
            />
            {isRent && (
              <Input
                label="Kaucja"
                type="number"
                inputMode="decimal"
                step="0.01"
                min={0}
                value={fields.deposit}
                onChange={set("deposit")}
                onBlur={markTouched("pricing.deposit")}
                error={errorFor("pricing.deposit")}
              />
            )}
            <div className="flex flex-col justify-end gap-2 pb-1">
              <Check
                label="Cena do negocjacji"
                checked={flags.priceNegotiable}
                onChange={(value) =>
                  setFlags((f) => ({ ...f, priceNegotiable: value }))
                }
              />
              {isRent && (
                <Check
                  label="Cena zawiera czynsz"
                  checked={flags.priceIncludesRent}
                  onChange={(value) =>
                    setFlags((f) => ({ ...f, priceIncludesRent: value }))
                  }
                />
              )}
            </div>
          </Section>

            </>
          )}

          {step.id === "details" && (
            <>
          {showBuildingSection && (
            <Section title={buildingTitle}>
              {showBuildYear && (
                <Input
                  label="Rok budowy"
                  type="text"
                  inputMode="numeric"
                  maxLength={4}
                  value={fields.buildYear}
                  onChange={setInt("buildYear")}
                  onBlur={markTouched("building.buildYear")}
                  error={errorFor("building.buildYear")}
                />
              )}
              {buildingFull && (
                <Select
                  label="Rodzaj zabudowy"
                  options={options.buildingType}
                  value={fields.buildingType}
                  onChange={set("buildingType")}
                  placeholder="Nie podano"
                />
              )}
              {buildingFull && (
                <Select
                  label="Materiał budowy"
                  options={options.buildingMaterial}
                  value={fields.buildingMaterial}
                  onChange={set("buildingMaterial")}
                  placeholder="Nie podano"
                />
              )}
              {showConstructionStatus && (
                <Select
                  label="Stan wykończenia"
                  options={options.constructionStatus}
                  value={fields.constructionStatus}
                  onChange={set("constructionStatus")}
                  placeholder="Nie podano"
                />
              )}
              {showOwnershipForm && (
                <Select
                  label="Forma własności"
                  options={options.ownershipForm}
                  value={fields.ownershipForm}
                  onChange={set("ownershipForm")}
                  placeholder="Nie podano"
                />
              )}
              {buildingFull && (
                <Select
                  label="Okna"
                  options={options.windowsType}
                  value={fields.windowsType}
                  onChange={set("windowsType")}
                  placeholder="Nie podano"
                />
              )}
              {buildingFull && (
                <Select
                  label="Położenie"
                  options={options.surroundings}
                  value={fields.surroundings}
                  onChange={set("surroundings")}
                  placeholder="Nie podano"
                />
              )}
              {showAvailableFrom && (
                <Input
                  label="Dostępne od"
                  type="date"
                  value={fields.availableFrom}
                  onChange={set("availableFrom")}
                />
              )}
              {showFurnished && (
                <div className="flex items-end pb-2">
                  <Check
                    label="Umeblowane"
                    checked={flags.furnished}
                    onChange={(value) => setFlags((f) => ({ ...f, furnished: value }))}
                  />
                </div>
              )}
            </Section>
          )}

          {showLandSection && (
            <Section title="Działka">
              <Select
                label="Typ działki"
                options={options.plotType}
                value={fields.plotType}
                onChange={set("plotType")}
                placeholder="Nie podano"
              />
              <Input
                label="Wymiary"
                value={fields.plotDimensions}
                onChange={set("plotDimensions")}
                placeholder="np. 25x40"
                maxLength={32}
              />
              <Select
                label="Dojazd"
                options={options.roadAccess}
                value={fields.roadAccess}
                onChange={set("roadAccess")}
                placeholder="Nie podano"
              />
              <div className="md:col-span-2">
                <Input
                  label="Przeznaczenie w planie miejscowym"
                  value={fields.zoningPlan}
                  onChange={set("zoningPlan")}
                />
              </div>
              <div className="flex items-end pb-2">
                <Check
                  label="Ogrodzona"
                  checked={flags.plotFenced}
                  onChange={(value) => setFlags((f) => ({ ...f, plotFenced: value }))}
                />
              </div>
            </Section>
          )}

          {showCommercialSection && (
            <Section title={isHala ? "Hala / magazyn" : "Lokal użytkowy"}>
              {isHala && (
                <Select
                  label="Konstrukcja"
                  options={options.hallStructure}
                  value={fields.hallStructure}
                  onChange={set("hallStructure")}
                  placeholder="Nie podano"
                />
              )}
              {isHala && (
                <Select
                  label="Posadzka"
                  options={options.flooring}
                  value={fields.flooring}
                  onChange={set("flooring")}
                  placeholder="Nie podano"
                />
              )}
              <Select
                label="Parking"
                options={options.parkingType}
                value={fields.parkingType}
                onChange={set("parkingType")}
                placeholder="Nie podano"
              />
              {isHala && (
                <Input
                  label="Moc przyłącza (kW)"
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  min={0}
                  value={fields.powerConnectionKw}
                  onChange={set("powerConnectionKw")}
                  onBlur={markTouched("commercial.powerConnectionKw")}
                  error={errorFor("commercial.powerConnectionKw")}
                />
              )}
              {isHala && (
                <Input
                  label="Nośność posadzki (t/m²)"
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  min={0}
                  value={fields.floorLoadPerM2}
                  onChange={set("floorLoadPerM2")}
                  onBlur={markTouched("commercial.floorLoadPerM2")}
                  error={errorFor("commercial.floorLoadPerM2")}
                />
              )}
              {isHala && (
                <Input
                  label="Liczba bram / doków"
                  type="text"
                  inputMode="numeric"
                  value={fields.loadingDocksCount}
                  onChange={setInt("loadingDocksCount")}
                />
              )}
              <div className="flex flex-col justify-end gap-2 pb-1">
                <Check
                  label="Pomieszczenia biurowe"
                  checked={flags.officeSpace}
                  onChange={(value) => setFlags((f) => ({ ...f, officeSpace: value }))}
                />
                <Check
                  label="Zaplecze socjalne"
                  checked={flags.socialFacilities}
                  onChange={(value) =>
                    setFlags((f) => ({ ...f, socialFacilities: value }))
                  }
                />
                {isHala && (
                  <Check
                    label="Rampa"
                    checked={flags.loadingRamp}
                    onChange={(value) => setFlags((f) => ({ ...f, loadingRamp: value }))}
                  />
                )}
              </div>
              {dict && (
                <div className="md:col-span-3">
                  <CheckGroup
                    title="Przeznaczenie"
                    entries={dict.commercialUse}
                    selected={uses}
                    onToggle={(value) => toggle(uses, setUses, value)}
                  />
                </div>
              )}
            </Section>
          )}

          {showHeating && dict && (
            <Section title="Ogrzewanie">
              <div className="md:col-span-3">
                <CheckGroup
                  title="Rodzaj ogrzewania"
                  entries={dict.heatingType}
                  selected={heating}
                  onToggle={(value) => toggle(heating, setHeating, value)}
                />
              </div>
            </Section>
          )}

          {showEnergy && (
            <Section
              title="Świadectwo charakterystyki energetycznej"
              description="Obowiązkowe przy sprzedaży i najmie od 28.04.2023. Wskaźnik EP musi znaleźć się w ogłoszeniu."
            >
            <div className="md:col-span-3">
              <Check
                label="Budynek zwolniony z obowiązku posiadania świadectwa"
                checked={flags.energyExempt}
                onChange={(value) => setFlags((f) => ({ ...f, energyExempt: value }))}
              />
            </div>

            {flags.energyExempt ? (
              <div className="md:col-span-3">
                <Input
                  label="Podstawa zwolnienia"
                  value={fields.energyExemptNote}
                  onChange={set("energyExemptNote")}
                  error={errors["energy.exemptNote"]}
                  hint="np. zabytek wpisany do rejestru, budynek do 50 m², obiekt sakralny."
                />
              </div>
            ) : (
              <>
                <Input
                  label="EP. Energia pierwotna"
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  min={0}
                  value={fields.energyPrimary}
                  onChange={set("energyPrimary")}
                  onBlur={markTouched("energy.energyPrimary")}
                  error={errorFor("energy.energyPrimary")}
                  hint="kWh/(m²·rok)"
                />
                <Input
                  label="EK. Energia końcowa"
                  type="number"
                  inputMode="decimal"
                  step="0.01"
                  min={0}
                  value={fields.energyFinal}
                  onChange={set("energyFinal")}
                  onBlur={markTouched("energy.energyFinal")}
                  error={errorFor("energy.energyFinal")}
                  hint="kWh/(m²·rok)"
                />
                <Select
                  label="Klasa energetyczna"
                  options={options.energyClass}
                  value={fields.energyClass}
                  onChange={set("energyClass")}
                  placeholder="Nie podano"
                />
                <Input
                  label="Numer świadectwa"
                  value={fields.energyCertNumber}
                  onChange={set("energyCertNumber")}
                />
              </>
            )}
            </Section>
          )}

            </>
          )}

          {step.id === "features" && (
            <>
          {featureGroups.length > 0 && (
            <Section>
              <div className="flex flex-col gap-5 md:col-span-3">
                {featureGroups.map((group) => (
                  <CheckGroup
                    key={group.category}
                    title={group.label}
                    entries={group.features}
                    selected={features}
                    onToggle={(value) => toggle(features, setFeatures, value)}
                  />
                ))}
              </div>
            </Section>
          )}

            </>
          )}

          {step.id === "media" && (
            <>
          {isEdit && id ? (
            <Section
              title="Zdjęcia"
              description="Galeria zapisuje się od razu, niezależnie od przycisku „Zapisz ofertę”."
            >
              <div className="md:col-span-3">
                <PropertyGallery
                  propertyId={id}
                  media={media}
                  onChange={setMedia}
                />
              </div>
            </Section>
          ) : (
            <Section
              title="Zdjęcia"
              description="Bez co najmniej jednego zdjęcia oferta nie będzie gotowa do eksportu."
            >
              <div className="md:col-span-3">
                <PropertyGalleryDraft
                  files={draftFiles}
                  onChange={setDraftFiles}
                />
              </div>
            </Section>
          )}

          <Section title="Multimedia">
            <Input
              label="Link do filmu (YouTube)"
              value={fields.videoUrl}
              onChange={set("videoUrl")}
            />
            <Input
              label="Link do wirtualnego spaceru"
              value={fields.panoramaUrl}
              onChange={set("panoramaUrl")}
            />
          </Section>
            </>
          )}

          {step.id === "description" && (
            <>
          <Section>
            <div className="md:col-span-3">
              <Input
                label="Tytuł ogłoszenia"
                value={fields.title}
                onChange={set("title")}
                onBlur={markTouched("title")}
                error={errorFor("title")}
                hint="Do 50 znaków."
                maxLength={50}
                required
              />
            </div>
            <div className="md:col-span-3">
              <Textarea
                label="Opis"
                rows={6}
                value={fields.description}
                onChange={set("description")}
                onBlur={markTouched("description")}
                error={errorFor("description")}
                counter={{ value: fields.description.length, max: 20000 }}
                required
              />
            </div>
          </Section>

            </>
          )}

          {step.id === "summary" && (
            <>
              <Section>
                <div className="md:col-span-3">
                  <PropertySummary
                    rows={summaryRows}
                    incomplete={incompleteSteps}
                    onEdit={goTo}
                  />
                </div>
              </Section>
          <Section
            title="Informacje wewnętrzne"
            description="Nie trafiają do ogłoszenia."
          >
            <Input
              label="Klucze"
              value={fields.keysInfo}
              onChange={set("keysInfo")}
            />
            <div className="md:col-span-3">
              <Textarea
                label="Notatki wewnętrzne"
                rows={3}
                value={fields.privateNotes}
                onChange={set("privateNotes")}
              />
            </div>
            <div className="md:col-span-3">
              <Check
                label="Pozwól eksportować tę ofertę na portale"
                checked={flags.exportable}
                onChange={(value) => setFlags((f) => ({ ...f, exportable: value }))}
              />
            </div>
          </Section>

            </>
          )}

          <footer className="flex items-center justify-between gap-2 border-t border-line px-5 py-4">
            {stepIndex > 0 ? (
              <Button type="button" variant="secondary" onClick={goBack}>
                <ArrowLeft className="size-4" strokeWidth={2} />
                Wstecz
              </Button>
            ) : (
              <Button
                type="button"
                variant="secondary"
                onClick={() => navigate("/nieruchomosci")}
              >
                Anuluj
              </Button>
            )}
            {isLastStep ? (
              <Button type="submit" disabled={saving}>
                {saving ? "Zapisywanie…" : "Zapisz ofertę"}
              </Button>
            ) : (
              <Button type="button" onClick={goNext}>
                Dalej
                <ArrowRight className="size-4" strokeWidth={2} />
              </Button>
            )}
          </footer>
        </div>
      </div>
    </form>
  );
}

function Section({
  title,
  description,
  children,
}: {
  title?: string;
  description?: string;
  children: ReactNode;
}) {
  // Sekcja to fragment karty etapu. Kolejne oddziela kreska, bez własnej ramki.
  return (
    <section className="border-t border-line first-of-type:border-t-0">
      {title && (
        <header className="px-5 pt-4">
          <h3 className="text-[13px] font-semibold tracking-tight text-ink">
            {title}
          </h3>
          {description && (
            <p className="mt-0.5 text-[12px] text-ink-muted">{description}</p>
          )}
        </header>
      )}
      <div className="grid gap-4 p-5 md:grid-cols-3">{children}</div>
    </section>
  );
}

interface SummaryRow {
  label: string;
  value: string;
  step: StepId;
}

/** Podgląd przed zapisem: najważniejsze dane z odnośnikiem do ich etapu. */
function PropertySummary({
  rows,
  incomplete,
  onEdit,
}: {
  rows: SummaryRow[];
  incomplete: { id: StepId; title: string }[];
  onEdit: (step: StepId) => void;
}) {
  return (
    <div className="flex flex-col gap-4">
      {incomplete.length > 0 ? (
        <div className="rounded-md border border-warning/40 bg-warning/10 px-3 py-2 text-[13px] text-ink">
          Brakuje danych w etapach:{" "}
          {incomplete.map((s, index) => (
            <span key={s.id}>
              {index > 0 && ", "}
              <button
                type="button"
                onClick={() => onEdit(s.id)}
                className="font-medium text-accent hover:underline"
              >
                {s.title}
              </button>
            </span>
          ))}
          .
        </div>
      ) : (
        <div className="rounded-md border border-good/30 bg-good/8 px-3 py-2 text-[13px] text-ink">
          Wszystkie wymagane pola są uzupełnione. Ofertę można zapisać.
        </div>
      )}

      <dl className="divide-y divide-line rounded-md border border-line">
        {rows.map((row) => (
          <div
            key={row.label}
            className="grid grid-cols-[minmax(0,2fr)_minmax(0,3fr)_auto] items-baseline gap-3 px-3 py-2"
          >
            <dt className="text-[12px] text-ink-muted">{row.label}</dt>
            <dd
              className={
                row.value
                  ? "truncate text-[13px] text-ink"
                  : "text-[13px] text-ink-muted"
              }
            >
              {row.value || "-"}
            </dd>
            <button
              type="button"
              onClick={() => onEdit(row.step)}
              className="text-[12px] text-accent hover:underline"
            >
              Zmień
            </button>
          </div>
        ))}
      </dl>
    </div>
  );
}

function Check({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (value: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-center gap-2 text-[13px] text-ink">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="size-4 rounded border-line text-accent focus:ring-2 focus:ring-accent-ring/50"
      />
      {label}
    </label>
  );
}

function CheckGroup({
  title,
  entries,
  selected,
  onToggle,
}: {
  title: string;
  entries: { value: string; label: string }[];
  selected: Set<string>;
  onToggle: (value: string) => void;
}) {
  return (
    <fieldset>
      <legend className="mb-2 text-[11px] font-medium tracking-wide text-ink-muted uppercase">
        {title}
      </legend>
      <div className="grid gap-x-4 gap-y-2 sm:grid-cols-2 lg:grid-cols-3">
        {entries.map((entry) => (
          <Check
            key={entry.value}
            label={entry.label}
            checked={selected.has(entry.value)}
            onChange={() => onToggle(entry.value)}
          />
        ))}
      </div>
    </fieldset>
  );
}

// Przecinek jako separator dziesiętny (polska konwencja) sprowadzamy do kropki
// przed parsowaniem. Inaczej Number("2,5") to NaN.
const num = (value: string): number | null => {
  const trimmed = value.trim().replace(",", ".");
  return trimmed === "" ? null : Number(trimmed);
};

const int = (value: string): number | null => {
  const trimmed = value.trim().replace(",", ".");
  return trimmed === "" ? null : Math.trunc(Number(trimmed));
};

const blank = (value: string): string | undefined =>
  value.trim() === "" ? undefined : value.trim();

// Każde słowo z wielkiej litery (Unicode, po spacji i myślniku), reszta mała.
const titleCase = (value: string): string =>
  value
    .toLocaleLowerCase("pl")
    .replace(/(^|[\s-])(\p{L})/gu, (_, sep, ch) => sep + ch.toLocaleUpperCase("pl"));

const str = (value: number | null): string =>
  value == null ? "" : String(value);
