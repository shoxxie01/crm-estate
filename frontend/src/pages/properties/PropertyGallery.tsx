import { useEffect, useRef, useState } from "react";
import {
  AlertTriangle,
  GripVertical,
  ImagePlus,
  RefreshCw,
  Star,
  Trash2,
} from "lucide-react";
import {
  deletePropertyMedia,
  fetchPropertyMedia,
  reorderPropertyMedia,
  replacePropertyMediaFile,
  updatePropertyMediaCaption,
  uploadPropertyMedia,
  type PropertyMedia,
} from "../../api/properties";
import { ApiError } from "../../api/client";
import { Button } from "../../components/ui/Button";
import { PropertyLightbox } from "./PropertyLightbox";

/** Tyle materiałów przyjmuje jedna oferta — limit pilnuje też backend. */
const MAX_MEDIA = 50;

interface Props {
  propertyId: string;
  media: PropertyMedia[];
  /** Wymagane tylko w trybie edycji — w podglądzie nic się nie zmienia. */
  onChange?: (media: PropertyMedia[]) => void;
  /**
   * Sam podgląd, bez wgrywania, kasowania i przestawiania. Karta oferty służy
   * do oglądania; wszystko, co zmienia ofertę, dzieje się w jej formularzu.
   */
  readOnly?: boolean;
}

/**
 * Galeria oferty.
 *
 * <p>W trybie edycji każda operacja idzie osobnym żądaniem i działa od razu —
 * galeria nie czeka na zapis formularza. Zdjęcie waży kilka megabajtów, więc
 * trzymanie go w pamięci do czasu, aż ktoś kliknie „Zapisz", oznaczałoby, że
 * nieudany zapis oferty przepuszcza też komplet zdjęć.
 */
export function PropertyGallery({
  propertyId,
  media,
  onChange = () => undefined,
  readOnly = false,
}: Props) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [draggedId, setDraggedId] = useState<string | null>(null);
  const [confirmId, setConfirmId] = useState<string | null>(null);
  const [lightboxIndex, setLightboxIndex] = useState<number | null>(null);
  // Świeże linki pobrane po wygaśnięciu podpisów (tylko podgląd). Trzymamy je
  // tutaj, żeby karta oferty nie musiała nic wiedzieć o czasie życia URL-i.
  const [refreshed, setRefreshed] = useState<PropertyMedia[] | null>(null);

  const addInput = useRef<HTMLInputElement>(null);
  const replaceInput = useRef<HTMLInputElement>(null);
  const replacingId = useRef<string | null>(null);

  const remaining = MAX_MEDIA - media.length;

  // W podglądzie renderujemy odświeżony komplet, jeśli po drodze go pobraliśmy.
  // W edycji zawsze `media` — tam stan galerii prowadzi formularz.
  const items = readOnly ? (refreshed ?? media) : media;

  // Nowy komplet z zewnątrz unieważnia to, co dociągnęliśmy sami.
  useEffect(() => setRefreshed(null), [media]);

  /**
   * Podpisy linków żyją ~15 minut, a kartę oferty da się trzymać otwartą dłużej.
   * Gdy zdjęcie przestaje się ładować, pobieramy galerię jeszcze raz — dostajemy
   * te same materiały ze świeżymi URL-ami.
   */
  async function refreshMedia() {
    try {
      setRefreshed(await fetchPropertyMedia(propertyId));
    } catch {
      // Zostaje to, co było. Komunikat o błędzie przeszkadzałby w oglądaniu
      // oferty bardziej niż jedno zdjęcie, które się nie wczytało.
    }
  }

  async function run(action: () => Promise<void>) {
    setBusy(true);
    setError(null);
    try {
      await action();
    } catch (cause) {
      // Backend zwraca mapę błędów pól; przy jednym polu komunikat z niej jest
      // konkretniejszy niż ogólne `detail`.
      const fieldError =
        cause instanceof ApiError && cause.fieldErrors
          ? Object.values(cause.fieldErrors)[0]
          : null;
      setError(
        fieldError ??
          (cause instanceof ApiError
            ? cause.message
            : "Nie udało się wykonać operacji."),
      );
    } finally {
      setBusy(false);
    }
  }

  function upload(files: FileList | null) {
    if (!files || files.length === 0) return;
    const picked = Array.from(files);

    if (picked.length > remaining) {
      setError(
        `Zostało miejsce na ${remaining} ${remaining === 1 ? "zdjęcie" : "zdjęć"} — wybrano ${picked.length}.`,
      );
      return;
    }

    void run(async () => {
      const added = await uploadPropertyMedia(propertyId, picked);
      onChange([...media, ...added]);
    });
  }

  function replace(files: FileList | null) {
    const mediaId = replacingId.current;
    if (!files || files.length === 0 || !mediaId) return;

    void run(async () => {
      const updated = await replacePropertyMediaFile(
        propertyId,
        mediaId,
        files[0],
      );
      onChange(media.map((item) => (item.id === mediaId ? updated : item)));
    });
  }

  function remove(mediaId: string) {
    void run(async () => {
      await deletePropertyMedia(propertyId, mediaId);
      // Pozycje przelicza serwer; lokalnie wystarczy usunąć kafelek, bo
      // kolejność pozostałych się nie zmienia.
      onChange(media.filter((item) => item.id !== mediaId));
      setConfirmId(null);
    });
  }

  function saveCaption(mediaId: string, caption: string) {
    const current = media.find((item) => item.id === mediaId);
    if (!current || (current.caption ?? "") === caption) return;

    void run(async () => {
      const updated = await updatePropertyMediaCaption(
        propertyId,
        mediaId,
        caption,
      );
      onChange(media.map((item) => (item.id === mediaId ? updated : item)));
    });
  }

  function dropOn(targetId: string) {
    if (!draggedId || draggedId === targetId) return;

    const next = [...media];
    const from = next.findIndex((item) => item.id === draggedId);
    const to = next.findIndex((item) => item.id === targetId);
    if (from < 0 || to < 0) return;

    const [moved] = next.splice(from, 1);
    next.splice(to, 0, moved);

    // Kolejność pokazujemy od razu, żeby przeciąganie nie czekało na sieć;
    // odpowiedź serwera i tak nadpisze ją wyliczonymi pozycjami.
    onChange(next);
    setDraggedId(null);

    void run(async () => {
      const saved = await reorderPropertyMedia(
        propertyId,
        next.map((item) => item.id),
      );
      onChange(saved);
    });
  }

  // Podgląd na karcie oferty: zdjęcie główne, pod nim reszta, zero sterowania.
  // Rozdzielenie jest celowe — kartę otwiera się, żeby ofertę obejrzeć,
  // a przypadkowego skasowania zdjęcia przy przeglądaniu nie da się cofnąć.
  // Kliknięcie kafelka otwiera pełny ekran; w trybie edycji kafelek jest
  // uchwytem do przeciągania, więc podglądu tam nie ma.
  if (readOnly) {
    const cover = items[0];

    return (
      <div className="flex flex-col gap-3">
        <p className="text-[12px] text-ink-muted">
          {items.length === 0
            ? "Brak zdjęć — dodasz je w edycji oferty. Bez co najmniej jednego oferta nie pójdzie na portal."
            : `${items.length} ${items.length === 1 ? "zdjęcie" : "zdjęć"}. Kliknij, aby otworzyć na pełnym ekranie.`}
        </p>

        {cover && (
          <>
            <figure className="flex flex-col gap-1.5">
              <button
                type="button"
                onClick={() => setLightboxIndex(0)}
                aria-label="Otwórz zdjęcie główne na pełnym ekranie"
                className="relative block aspect-4/3 w-full overflow-hidden rounded-md border border-line bg-subtle"
              >
                <img
                  src={cover.thumbnailUrl}
                  alt={cover.caption ?? cover.fileName}
                  className="h-full w-full object-cover"
                />
                <span className="absolute top-1.5 left-1.5 inline-flex items-center gap-1 rounded-sm bg-ink/80 px-1.5 py-0.5 text-[10px] font-medium text-white">
                  <Star size={10} />
                  Główne
                </span>
                {!cover.meetsPortalRequirements && (
                  <span
                    title="Portal odrzuci to zdjęcie — sprawdź rozmiar i wymiary."
                    className="absolute top-1.5 right-1.5 inline-flex items-center gap-1 rounded-sm bg-warning/90 px-1.5 py-0.5 text-[10px] font-medium text-ink"
                  >
                    <AlertTriangle size={10} />
                    Portal
                  </span>
                )}
              </button>
              {cover.caption && (
                <figcaption className="text-[11px] text-ink-secondary">
                  {cover.caption}
                </figcaption>
              )}
            </figure>

            {items.length > 1 && (
              <ul className="grid grid-cols-3 gap-2">
                {items.slice(1).map((item, index) => (
                  <li key={item.id}>
                    <button
                      type="button"
                      onClick={() => setLightboxIndex(index + 1)}
                      aria-label={`Otwórz zdjęcie ${index + 2} na pełnym ekranie`}
                      className="relative block aspect-4/3 w-full overflow-hidden rounded-md border border-line bg-subtle"
                    >
                      <img
                        src={item.thumbnailUrl}
                        alt={item.caption ?? item.fileName}
                        loading="lazy"
                        className="h-full w-full object-cover"
                      />
                      {!item.meetsPortalRequirements && (
                        <span
                          title="Portal odrzuci to zdjęcie — sprawdź rozmiar i wymiary."
                          className="absolute top-1 right-1 inline-flex items-center rounded-sm bg-warning/90 px-1 py-0.5 text-ink"
                        >
                          <AlertTriangle size={10} />
                        </span>
                      )}
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </>
        )}

        {lightboxIndex !== null && (
          <PropertyLightbox
            media={items}
            index={lightboxIndex}
            onClose={() => setLightboxIndex(null)}
            onIndexChange={setLightboxIndex}
            onExpired={refreshMedia}
          />
        )}
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-3">
        <p className="text-[12px] text-ink-muted">
          {media.length === 0
            ? "Brak zdjęć. Bez co najmniej jednego oferta nie pójdzie na portal."
            : `${media.length} z ${MAX_MEDIA}. Pierwsze zdjęcie jest zdjęciem głównym — kolejność zmienisz przeciąganiem.`}
        </p>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          disabled={busy || remaining <= 0}
          onClick={() => addInput.current?.click()}
        >
          <ImagePlus size={14} />
          Dodaj zdjęcia
        </Button>
      </div>

      {error && (
        <p
          role="alert"
          className="rounded-md border border-critical/30 bg-critical/5 px-3 py-2 text-[12px] text-critical"
        >
          {error}
        </p>
      )}

      <div
        onDragOver={(event) => {
          // Bez preventDefault przeglądarka potraktuje upuszczenie pliku jak
          // nawigację i otworzy zdjęcie zamiast je wgrać.
          event.preventDefault();
          if (!draggedId) setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(event) => {
          event.preventDefault();
          setDragOver(false);
          if (!draggedId) upload(event.dataTransfer.files);
        }}
        className={[
          "rounded-md border border-dashed p-3 transition-colors",
          dragOver ? "border-accent bg-accent/5" : "border-line bg-subtle/40",
        ].join(" ")}
      >
        {media.length === 0 ? (
          <p className="py-6 text-center text-[12px] text-ink-muted">
            Przeciągnij zdjęcia tutaj albo użyj przycisku „Dodaj zdjęcia”.
            <br />
            JPG, PNG, WEBP lub GIF, minimum 400×300 px, do 15 MB.
          </p>
        ) : (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {media.map((item, index) => (
              <li
                key={item.id}
                draggable
                onDragStart={() => setDraggedId(item.id)}
                onDragEnd={() => setDraggedId(null)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={(event) => {
                  event.preventDefault();
                  dropOn(item.id);
                }}
                className={[
                  "group flex flex-col overflow-hidden rounded-md border bg-surface",
                  draggedId === item.id
                    ? "border-accent opacity-60"
                    : "border-line",
                ].join(" ")}
              >
                <div className="relative aspect-4/3 bg-subtle">
                  <img
                    src={item.thumbnailUrl}
                    alt={item.caption ?? item.fileName}
                    loading="lazy"
                    className="h-full w-full object-cover"
                  />

                  {index === 0 && (
                    <span className="absolute left-1.5 top-1.5 inline-flex items-center gap-1 rounded-sm bg-ink/80 px-1.5 py-0.5 text-[10px] font-medium text-white">
                      <Star size={10} />
                      Główne
                    </span>
                  )}

                  {!item.meetsPortalRequirements && (
                    <span
                      title="Portal odrzuci to zdjęcie — sprawdź rozmiar i wymiary."
                      className="absolute right-1.5 top-1.5 inline-flex items-center gap-1 rounded-sm bg-warning/90 px-1.5 py-0.5 text-[10px] font-medium text-ink"
                    >
                      <AlertTriangle size={10} />
                      Portal
                    </span>
                  )}

                  <span className="absolute bottom-1.5 left-1.5 cursor-grab rounded-sm bg-surface/85 p-1 text-ink-muted opacity-0 transition-opacity group-hover:opacity-100">
                    <GripVertical size={12} />
                  </span>
                </div>

                <div className="flex flex-col gap-1.5 p-2">
                  <input
                    defaultValue={item.caption ?? ""}
                    placeholder="Podpis (opcjonalnie)"
                    maxLength={150}
                    disabled={busy}
                    onBlur={(event) =>
                      saveCaption(item.id, event.target.value.trim())
                    }
                    className="w-full rounded-sm border border-line bg-surface px-1.5 py-1 text-[11px] text-ink placeholder:text-ink-muted focus:border-accent focus:outline-none"
                  />

                  {confirmId === item.id ? (
                    <div className="flex items-center gap-1">
                      <Button
                        type="button"
                        size="sm"
                        variant="secondary"
                        className="h-7 flex-1 text-[11px]"
                        disabled={busy}
                        onClick={() => remove(item.id)}
                      >
                        Usuń trwale
                      </Button>
                      <Button
                        type="button"
                        size="sm"
                        variant="ghost"
                        className="h-7 text-[11px]"
                        onClick={() => setConfirmId(null)}
                      >
                        Anuluj
                      </Button>
                    </div>
                  ) : (
                    <div className="flex items-center gap-1">
                      <Button
                        type="button"
                        size="sm"
                        variant="ghost"
                        className="h-7 flex-1 text-[11px]"
                        disabled={busy}
                        title="Wgraj inny plik w miejsce tego zdjęcia"
                        onClick={() => {
                          replacingId.current = item.id;
                          replaceInput.current?.click();
                        }}
                      >
                        <RefreshCw size={12} />
                        Zamień
                      </Button>
                      <Button
                        type="button"
                        size="sm"
                        variant="ghost"
                        className="h-7 text-[11px] text-critical hover:text-critical"
                        disabled={busy}
                        onClick={() => setConfirmId(item.id)}
                      >
                        <Trash2 size={12} />
                      </Button>
                    </div>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <input
        ref={addInput}
        type="file"
        accept="image/*"
        multiple
        hidden
        onChange={(event) => {
          upload(event.target.files);
          // Reset, żeby wybranie tego samego pliku po błędzie znów wywołało zmianę.
          event.target.value = "";
        }}
      />
      <input
        ref={replaceInput}
        type="file"
        accept="image/*"
        hidden
        onChange={(event) => {
          replace(event.target.files);
          event.target.value = "";
        }}
      />
    </div>
  );
}
