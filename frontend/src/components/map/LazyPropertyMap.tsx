import { Suspense, lazy, type ComponentProps } from "react";
import type { PropertyMap } from "./PropertyMap";

export type { LatLng } from "./PropertyMap";

/**
 * Mapa dociągana osobnym plikiem, dopiero gdy jest potrzebna.
 *
 * MapLibre waży więcej niż cała reszta aplikacji razem wzięta (ok. 240 kB po
 * gzipie), a używają go dokładnie dwa ekrany: formularz oferty i jej karta.
 * Wpięty na sztywno wydłużałby pierwsze wejście do CRM-u — logowanie, dashboard
 * i kalendarz czekałyby na bibliotekę, której nigdy nie zobaczą.
 */
const PropertyMapImpl = lazy(() =>
  import("./PropertyMap").then((module) => ({ default: module.PropertyMap })),
);

type Props = ComponentProps<typeof PropertyMap>;

export function LazyPropertyMap({ height = 320, ...props }: Props) {
  return (
    <Suspense
      fallback={
        // Ta sama wysokość co mapa — inaczej formularz podskakuje w chwili,
        // gdy biblioteka dojedzie.
        <div className="flex flex-col gap-1.5">
          <div
            className="flex items-center justify-center rounded-md border border-line bg-subtle text-[12px] text-ink-muted"
            style={{ height }}
          >
            Wczytuję mapę…
          </div>
        </div>
      }
    >
      <PropertyMapImpl height={height} {...props} />
    </Suspense>
  );
}
