import { useEffect, useRef, useState, type ReactNode } from "react";
import * as maplibregl from "maplibre-gl";
import "maplibre-gl/dist/maplibre-gl.css";

export interface LatLng {
  lat: number;
  lng: number;
}

interface PropertyMapProps {
  /** Pinezka. `null` — brak lokalizacji, mapa pokazuje wtedy całą Polskę. */
  position: LatLng | null;
  /**
   * Wołane, gdy agent wskaże miejsce — kliknięciem w mapę albo przeciągnięciem
   * pinezki. Brak tej funkcji przełącza mapę w tryb tylko do odczytu.
   */
  onPick?: (position: LatLng) => void;
  /** Wysokość okna mapy w pikselach. */
  height?: number;
  /** Nakładka „pracuję" na czas geokodowania. */
  busy?: boolean;
  /** Linia pod mapą — znaleziony adres albo komunikat błędu. */
  footer?: ReactNode;
}

/**
 * Styl mapy: OpenFreeMap „positron" — same ulice, budynki i etykiety, bez
 * ikon sklepów, restauracji i bankomatów, którymi standardowe kafelki OSM
 * zalewają centrum miasta. Przy ofercie liczy się, gdzie stoi budynek i przy
 * jakiej ulicy, a nie co jest naprzeciwko.
 *
 * <p>Wybór padł na kafelki wektorowe, bo <b>nie ma dziś bezkluczowego
 * rastrowego stylu w tym guście</b> — CARTO Positron, do niedawna standardowa
 * odpowiedź, od pewnego czasu zwraca kafelki z napisem „API KEY REQUIRED".
 * OpenFreeMap serwuje wektory bez klucza, bez limitów i bez rejestracji.
 * Adres jest w stałej, więc podmiana dostawcy (albo postawienie własnego
 * serwera kafelków) to zmiana jednej linii.
 */
const MAP_STYLE = "https://tiles.openfreemap.org/styles/positron";

/** Środek Polski i zoom, przy którym widać cały kraj — widok startowy bez pinezki. */
const POLAND_CENTER: [number, number] = [19.4, 52.0];
const POLAND_ZOOM = 5;
/**
 * Zoom, na który schodzimy, pokazując konkretny adres.
 *
 * <p>17 pokazuje najbliższe przecznice — widać, przy której ulicy stoi obiekt
 * i co go otacza, a jednocześnie nie trzeba dojeżdżać kółkiem. To zarazem
 * próg, od którego styl puszcza numery domów, więc pojawiają się od razu.
 * Dalej (18+) kadr zawęża się do jednego kwartału i orientację trzeba
 * odbudowywać oddalaniem.
 */
const ADDRESS_ZOOM = 17;

/**
 * Pinezka rysowana inline, żeby brała kolor z tej samej zmiennej co reszta
 * interfejsu i nie wymagała dokładania plików graficznych do bundla.
 */
const PIN_SVG = `<svg width="28" height="38" viewBox="0 0 28 38" xmlns="http://www.w3.org/2000/svg">
    <path d="M14 1C7.4 1 2 6.3 2 12.9 2 21.6 14 37 14 37s12-15.4 12-24.1C26 6.3 20.6 1 14 1z"
          fill="var(--color-accent)" stroke="#ffffff" stroke-width="2"/>
    <circle cx="14" cy="13" r="4.5" fill="#ffffff"/>
  </svg>`;

function pinElement(): HTMLElement {
  const element = document.createElement("div");
  element.innerHTML = PIN_SVG;
  element.style.lineHeight = "0";
  return element;
}

/**
 * Styl przychodzi z angielskimi nazwami („Main Market Square" zamiast „Rynek
 * Główny"), bo tak wygląda domyślne pole `name` w OpenMapTiles. Przestawiamy
 * wszystkie etykiety na polską wersję, z odwrotem na nazwę lokalną tam, gdzie
 * polskiej nie ma.
 */
/**
 * Warstwa numerów domów. Nie ma jej w żadnym gotowym stylu OpenFreeMap, ale
 * dane są — schemat OpenMapTiles wystawia punkty adresowe warstwą
 * `housenumber`. Bez niej mapa nie odpowiada na pytanie „który to budynek".
 */
const HOUSE_NUMBERS_LAYER = {
  id: "delta-housenumbers",
  type: "symbol",
  source: "openmaptiles",
  "source-layer": "housenumber",
  /*
   * Kafelki kończą się na z14 i wyżej są rozciągane. Gdyby próg „pokaż od
   * z17" siedział w `minzoom`, warstwa byłaby przy kafelku z14 uznana za
   * nieaktywną i numery nie zostałyby z niego w ogóle wczytane — niezależnie
   * od tego, jak blisko podjedzie mapa. Dlatego `minzoom` równa się
   * maksymalnemu zoomowi źródła, a próg widoczności schodzi do przezroczystości.
   */
  minzoom: 14,
  layout: {
    "text-field": ["get", "housenumber"],
    "text-font": ["Noto Sans Regular"],
    "text-size": 10,
  },
  paint: {
    // Poniżej z17 numery zlewają się w nieczytelną plamę.
    "text-opacity": ["step", ["zoom"], 0, 17, 1],
    "text-color": "#8a9099",
    "text-halo-color": "#ffffff",
    "text-halo-width": 1,
  },
} as const;

/**
 * Styl pobrany i doprawiony <b>zanim</b> powstanie mapa.
 *
 * <p>Obie zmiany — polskie etykiety i numery domów — dałoby się teoretycznie
 * nanieść po `load`, na gotowej mapie. W praktyce jest za późno: kafelki dla
 * pierwszego widoku są wtedy już pobrane i zparsowane, a parser zachowuje
 * z kafelka tylko te warstwy danych, do których odwołuje się styl. Warstwa
 * `housenumber` dołożona po fakcie trafia więc na kafelki, w których numerów
 * nikt nie zostawił — i mapa uparcie pokazuje puste miejsca.
 *
 * <p>Wynik trzymamy w module: styl jest ten sam dla wszystkich map w aplikacji,
 * a formularz i karta oferty potrafią go zawołać w tej samej chwili.
 */
let stylePromise: Promise<maplibregl.StyleSpecification> | null = null;

function loadStyle(): Promise<maplibregl.StyleSpecification> {
  stylePromise ??= fetch(MAP_STYLE)
    .then((response) => response.json())
    .then((style: maplibregl.StyleSpecification) => {
      for (const layer of style.layers) {
        if (layer.type !== "symbol") continue;
        const layout = layer.layout as Record<string, unknown> | undefined;
        if (!layout?.["text-field"]) continue;
        // Styl przychodzi z angielskimi nazwami („Main Market Square" zamiast
        // „Rynek Główny") — tak wygląda domyślne pole `name` w OpenMapTiles.
        layout["text-field"] = [
          "coalesce",
          ["get", "name:pl"],
          ["get", "name"],
        ];
      }

      style.layers.push(
        HOUSE_NUMBERS_LAYER as unknown as maplibregl.LayerSpecification,
      );
      return style;
    })
    .catch(() => {
      // Gdy pobranie stylu padnie, oddajemy sam adres — mapa wstanie
      // z angielskimi etykietami i bez numerów, ale wstanie.
      stylePromise = null;
      return MAP_STYLE as unknown as maplibregl.StyleSpecification;
    });

  return stylePromise;
}

export function PropertyMap({
  position,
  onPick,
  height = 320,
  busy = false,
  footer,
}: PropertyMapProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<maplibregl.Map | null>(null);
  const markerRef = useRef<maplibregl.Marker | null>(null);
  // `onPick` bywa nową funkcją przy każdym renderze rodzica. Gdyby wisiał
  // w zależnościach efektu tworzącego mapę, mapa przebudowywałaby się co render.
  const onPickRef = useRef(onPick);
  onPickRef.current = onPick;
  // Mapa powstaje po pobraniu stylu, więc jest chwila, gdy komponent jest
  // zamontowany, a mapy jeszcze nie ma.
  const [ready, setReady] = useState(false);

  const readOnly = !onPick;

  // Mapa powstaje raz i żyje do odmontowania komponentu. Pozycję pinezki
  // i tryb zmieniamy osobnymi efektami, na gotowym obiekcie — stąd `ready`,
  // który je odpala, gdy styl dojedzie i mapa faktycznie istnieje.
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    let cancelled = false;
    let map: maplibregl.Map | null = null;
    let observer: ResizeObserver | null = null;

    loadStyle().then((style) => {
      // Komponent mógł zniknąć, zanim styl dojechał (StrictMode montuje dwa
      // razy, a agent potrafi wyjść z formularza od razu po wejściu).
      if (cancelled) return;

      map = new maplibregl.Map({
        container,
        style,
        center: POLAND_CENTER,
        zoom: POLAND_ZOOM,
        // Licencja ODbL wymaga widocznej informacji o źródle danych; styl
        // niesie ją ze sobą, my tylko zwijamy kontrolkę do ikony „i", żeby
        // w wąskiej kolumnie karty nie łamała się przez pół mapy.
        attributionControl: { compact: true },
        /*
         * Kółko myszy samo przewija stronę, a z wciśniętym `Ctrl` (na macOS
         * `Cmd`) przybliża mapę. Tego właśnie pilnuje `cooperativeGestures`:
         * bez niego mapa łapałaby każde przewinięcie i formularz nie dałby się
         * przewinąć, gdy kursor przejdzie nad mapą. Gdy ktoś kręci kółkiem bez
         * modyfikatora, MapLibre pokazuje na mapie podpowiedź, co wcisnąć.
         */
        cooperativeGestures: true,
        // Podpowiedzi po polsku, jak reszta interfejsu, i od razu ze skrótem —
        // to jedyne miejsca, w których skrót sam się pokazuje.
        locale: {
          "NavigationControl.ZoomIn": "Przybliż (Ctrl + lub +)",
          "NavigationControl.ZoomOut": "Oddal (Ctrl − lub −)",
          "CooperativeGesturesHandler.WindowsHelpText":
            "Przytrzymaj Ctrl i kręć kółkiem, żeby przybliżyć mapę",
          "CooperativeGesturesHandler.MacHelpText":
            "Przytrzymaj ⌘ i kręć kółkiem, żeby przybliżyć mapę",
          "CooperativeGesturesHandler.MobileHelpText":
            "Przesuwaj mapę dwoma palcami",
        },
      });
      // Obrót i pochylenie tylko mieszają: adres czyta się z mapy ustawionej
      // północą do góry, a przekrzywioną trzeba najpierw prostować.
      map.dragRotate.disable();
      map.touchZoomRotate.disableRotation();
      map.addControl(
        new maplibregl.NavigationControl({ showCompass: false }),
        "top-left",
      );

      // Bez tego błędy MapLibre (brakujący font, niedostępne kafelki) giną
      // bezgłośnie — mapa po prostu rysuje mniej, niż powinna.
      map.on("error", (event) => {
        console.error("[mapa]", event.error?.message ?? event);
      });

      map.on("load", () => {
        // Kontrolka atrybucji startuje rozwinięta i w wąskiej kolumnie karty
        // oferty łamie się na dwie linie przez pół mapy. Zwijamy ją do ikony
        // „i", tak jak zrobiłoby pierwsze kliknięcie — treść zostaje
        // o kliknięcie dalej, więc wymóg licencji ODbL jest spełniony.
        // Gdyby klasa kiedyś zniknęła, atrybucja po prostu zostanie rozwinięta.
        container
          .querySelector(".maplibregl-ctrl-attrib.maplibregl-compact-show")
          ?.classList.remove("maplibregl-compact-show");
      });

      // Sekcja formularza i przyklejona kolumna karty zmieniają szerokość bez
      // zmiany rozmiaru okna — sam `resize` na window tego nie łapie, a płótno
      // policzone na starej szerokości zostawia puste pasy.
      observer = new ResizeObserver(() => map?.resize());
      observer.observe(container);

      mapRef.current = map;
      setReady(true);
    });

    return () => {
      cancelled = true;
      observer?.disconnect();
      map?.remove();
      mapRef.current = null;
      markerRef.current = null;
      setReady(false);
    };
  }, []);

  /*
   * Zoom z klawiatury — `Ctrl` + `+` / `Ctrl` + `−`, a także same `+` / `−`.
   * Jedno i drugie działa, <b>gdy kursor jest nad mapą</b>.
   *
   * Na najechaniu, a nie po kliknięciu, i to nie jest wygoda, tylko warunek
   * działania: w formularzu kliknięcie w mapę przestawia pinezkę, więc nie da
   * się jej zafokusować, nie zmieniając przy okazji adresu oferty.
   *
   * <p><b>Ctrl + / Ctrl − to skrót przeglądarki na powiększenie całej strony</b>
   * i korzystają z niego osoby słabowidzące. Przechwytujemy go wyłącznie nad
   * mapą — kursor obok, w dowolnym innym miejscu CRM-u, i powiększa się strona,
   * jak wszędzie. Ograniczenie do jednego prostokąta jest tu całym
   * zabezpieczeniem: gdyby skrót był globalny, nie dałoby się już powiększyć
   * formularza.
   */
  useEffect(() => {
    const map = mapRef.current;
    const container = containerRef.current;
    if (!map || !container) return;

    let hovered = false;
    const onEnter = () => {
      hovered = true;
    };
    const onLeave = () => {
      hovered = false;
    };
    container.addEventListener("pointerenter", onEnter);
    container.addEventListener("pointerleave", onLeave);

    const handleKey = (event: KeyboardEvent) => {
      if (!hovered) return;
      // Alt bywa skrótem systemowym — w niego nie wchodzimy. Shift zostaje,
      // bo na większości układów bez niego nie ma jak wpisać plusa.
      if (event.altKey) return;

      const zoomIn =
        event.key === "+" || event.key === "=" || event.code === "NumpadAdd";
      const zoomOut =
        event.key === "-" ||
        event.key === "_" ||
        event.code === "NumpadSubtract";
      if (!zoomIn && !zoomOut) return;

      const withCtrl = event.ctrlKey || event.metaKey;
      const target = event.target as HTMLElement | null;

      if (!withCtrl) {
        // Samo „+" bez modyfikatora bywa zwykłym znakiem: kursor potrafi leżeć
        // nad mapą, gdy agent wpisuje „+48" w telefonie albo „-1" w piętrze.
        // Pisanie ma wtedy pierwszeństwo.
        if (target?.closest("input, textarea, select, [contenteditable='true']")) {
          return;
        }
        // Skupienie na mapie — MapLibre obsłuży klawisz sam, nie dublujemy.
        // Przy Ctrl już nie: tego wariantu MapLibre nie zna, a przeglądarka
        // powiększyłaby stronę.
        if (target && container.contains(target)) return;
      }

      if (zoomIn) map.zoomIn();
      else map.zoomOut();

      // Dopiero tu: przewijanie strony spacją czy strzałkami ma działać jak
      // zwykle, blokujemy wyłącznie klawisze, które faktycznie obsłużyliśmy.
      // Przy Ctrl to właśnie `preventDefault` powstrzymuje zoom przeglądarki.
      event.preventDefault();
    };

    window.addEventListener("keydown", handleKey);

    return () => {
      container.removeEventListener("pointerenter", onEnter);
      container.removeEventListener("pointerleave", onLeave);
      window.removeEventListener("keydown", handleKey);
    };
  }, [ready]);

  // Tryb: w widoku tylko do odczytu mapa daje się obejrzeć, ale nie zmienić.
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    const handleClick = (event: maplibregl.MapMouseEvent) => {
      onPickRef.current?.({ lat: event.lngLat.lat, lng: event.lngLat.lng });
    };

    if (!readOnly) {
      map.on("click", handleClick);
    }
    map.getCanvas().style.cursor = readOnly ? "" : "crosshair";

    return () => {
      map.off("click", handleClick);
    };
  }, [readOnly, ready]);

  // Pinezka: dostawiana, przesuwana i zdejmowana zgodnie z `position`.
  useEffect(() => {
    const map = mapRef.current;
    if (!map) return;

    if (!position) {
      markerRef.current?.remove();
      markerRef.current = null;
      map.jumpTo({ center: POLAND_CENTER, zoom: POLAND_ZOOM });
      return;
    }

    const point: [number, number] = [position.lng, position.lat];

    if (!markerRef.current) {
      const marker = new maplibregl.Marker({
        element: pinElement(),
        anchor: "bottom",
        draggable: !readOnly,
      })
        .setLngLat(point)
        .addTo(map);

      marker.on("dragend", () => {
        const moved = marker.getLngLat();
        onPickRef.current?.({ lat: moved.lat, lng: moved.lng });
      });

      markerRef.current = marker;

      // Pierwsza pinezka zawsze zabiera widok ze sobą. Bez tego wejście
      // w zapisaną ofertę pokazuje ją jako punkcik na mapie całego kraju —
      // formalnie poprawnie, ale nie widać z tego nic.
      map.jumpTo({ center: point, zoom: ADDRESS_ZOOM });
      return;
    }

    markerRef.current.setLngLat(point);
    markerRef.current.setDraggable(!readOnly);

    // Dalej widok przestawiamy tylko wtedy, gdy pinezka wyszła poza kadr.
    // Inaczej każde przeciągnięcie pinezki o centymetr wyszarpywałoby mapę na
    // środek, a to właśnie otoczenie punktu agent ogląda, przesuwając go.
    if (!map.getBounds().contains(point)) {
      map.easeTo({ center: point, zoom: Math.max(map.getZoom(), ADDRESS_ZOOM) });
    }
  }, [position, readOnly, ready]);

  return (
    <div className="flex flex-col gap-1.5">
      <div
        className="relative overflow-hidden rounded-md border border-line"
        style={{ height }}
      >
        <div ref={containerRef} className="h-full w-full" />
        {busy && (
          <div className="pointer-events-none absolute right-2 top-2 z-10 rounded-md bg-surface/90 px-2 py-1 text-[12px] text-ink-secondary shadow-sm">
            Szukam…
          </div>
        )}
      </div>
      {footer}
    </div>
  );
}
