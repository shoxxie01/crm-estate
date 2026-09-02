import { useEffect, useMemo, useRef, useState } from "react";
import { GripVertical, ImagePlus, Star, X } from "lucide-react";
import { Button } from "../../components/ui/Button";

/** Tyle materiałów przyjmuje jedna oferta — ten sam limit pilnuje backend. */
const MAX_MEDIA = 50;

/** Limit multiparta po stronie serwera (`spring.servlet.multipart.max-file-size`). */
const MAX_FILE_BYTES = 15 * 1024 * 1024;

interface Props {
  files: File[];
  onChange: (files: File[]) => void;
}

/**
 * Galeria oferty, której jeszcze nie ma w bazie.
 *
 * <p>Zdjęcie trzyma klucz obcy do oferty, więc przed jej zapisem nie ma czego
 * nim obwiesić. Zamiast kazać agentowi wracać do formularza po zapisie,
 * trzymamy pliki w pamięci przeglądarki i wysyłamy je zaraz po tym, jak serwer
 * nada ofercie identyfikator — z punktu widzenia użytkownika zdjęcia po prostu
 * zapisują się razem z ofertą.
 *
 * <p>To osobny komponent, a nie tryb {@code PropertyGallery}: tam każda operacja
 * jest osobnym żądaniem do serwera, tutaj wszystko dzieje się lokalnie i nie ma
 * jeszcze ani identyfikatorów, ani podpisanych linków. Wspólny byłby jednym
 * wielkim rozgałęzieniem.
 */
export function PropertyGalleryDraft({ files, onChange }: Props) {
  const [error, setError] = useState<string | null>(null);
  const [dragOver, setDragOver] = useState(false);
  const [draggedIndex, setDraggedIndex] = useState<number | null>(null);
  const input = useRef<HTMLInputElement>(null);

  // Podgląd bez wysyłania czegokolwiek na serwer. Adresy trzeba potem zwolnić,
  // inaczej pliki zostają w pamięci karty do końca jej życia.
  const previews = useMemo(
    () => files.map((file) => ({ file, url: URL.createObjectURL(file) })),
    [files],
  );

  useEffect(() => {
    return () => previews.forEach((preview) => URL.revokeObjectURL(preview.url));
  }, [previews]);

  const remaining = MAX_MEDIA - files.length;

  /**
   * Wstępne sito po stronie przeglądarki. Prawdziwa walidacja (sygnatura pliku,
   * minimalne wymiary) jest na serwerze — tu chodzi tylko o to, żeby oczywiste
   * pomyłki wyszły od razu, a nie dopiero po zapisaniu oferty.
   */
  function add(picked: FileList | null) {
    if (!picked || picked.length === 0) return;
    const chosen = Array.from(picked);

    if (chosen.length > remaining) {
      setError(
        `Zostało miejsce na ${remaining} ${remaining === 1 ? "zdjęcie" : "zdjęć"} — wybrano ${chosen.length}.`,
      );
      return;
    }

    const notImage = chosen.find((file) => !file.type.startsWith("image/"));
    if (notImage) {
      setError(`„${notImage.name}" nie jest obrazem.`);
      return;
    }

    const tooBig = chosen.find((file) => file.size > MAX_FILE_BYTES);
    if (tooBig) {
      setError(`„${tooBig.name}" jest większy niż 15 MB.`);
      return;
    }

    setError(null);
    onChange([...files, ...chosen]);
  }

  function remove(index: number) {
    setError(null);
    onChange(files.filter((_, position) => position !== index));
  }

  function dropOn(targetIndex: number) {
    if (draggedIndex === null || draggedIndex === targetIndex) return;

    const next = [...files];
    const [moved] = next.splice(draggedIndex, 1);
    next.splice(targetIndex, 0, moved);

    onChange(next);
    setDraggedIndex(null);
  }

  return (
    <div className="flex flex-col gap-3">
      <div className="flex items-center justify-between gap-3">
        <p className="text-[12px] text-ink-muted">
          {files.length === 0
            ? "Zdjęcia wgrają się razem z ofertą po kliknięciu „Zapisz ofertę”."
            : `${files.length} z ${MAX_MEDIA} do wgrania. Pierwsze będzie zdjęciem głównym — kolejność zmienisz przeciąganiem.`}
        </p>
        <Button
          type="button"
          variant="secondary"
          size="sm"
          disabled={remaining <= 0}
          onClick={() => input.current?.click()}
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
          event.preventDefault();
          if (draggedIndex === null) setDragOver(true);
        }}
        onDragLeave={() => setDragOver(false)}
        onDrop={(event) => {
          event.preventDefault();
          setDragOver(false);
          if (draggedIndex === null) add(event.dataTransfer.files);
        }}
        className={[
          "rounded-md border border-dashed p-3 transition-colors",
          dragOver ? "border-accent bg-accent/5" : "border-line bg-subtle/40",
        ].join(" ")}
      >
        {files.length === 0 ? (
          <p className="py-6 text-center text-[12px] text-ink-muted">
            Przeciągnij zdjęcia tutaj albo użyj przycisku „Dodaj zdjęcia”.
            <br />
            JPG, PNG, WEBP lub GIF, minimum 400×300 px, do 15 MB.
          </p>
        ) : (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
            {previews.map((preview, index) => (
              <li
                key={`${preview.file.name}-${index}`}
                draggable
                onDragStart={() => setDraggedIndex(index)}
                onDragEnd={() => setDraggedIndex(null)}
                onDragOver={(event) => event.preventDefault()}
                onDrop={(event) => {
                  event.preventDefault();
                  dropOn(index);
                }}
                className={[
                  "group flex flex-col overflow-hidden rounded-md border bg-surface",
                  draggedIndex === index
                    ? "border-accent opacity-60"
                    : "border-line",
                ].join(" ")}
              >
                <div className="relative aspect-4/3 bg-subtle">
                  <img
                    src={preview.url}
                    alt={preview.file.name}
                    className="h-full w-full object-cover"
                  />

                  {index === 0 && (
                    <span className="absolute left-1.5 top-1.5 inline-flex items-center gap-1 rounded-sm bg-ink/80 px-1.5 py-0.5 text-[10px] font-medium text-white">
                      <Star size={10} />
                      Główne
                    </span>
                  )}

                  <button
                    type="button"
                    title="Usuń z listy"
                    onClick={() => remove(index)}
                    className="absolute right-1.5 top-1.5 rounded-sm bg-surface/90 p-1 text-ink-secondary hover:bg-surface hover:text-critical"
                  >
                    <X size={12} />
                  </button>

                  <span className="absolute bottom-1.5 left-1.5 cursor-grab rounded-sm bg-surface/85 p-1 text-ink-muted opacity-0 transition-opacity group-hover:opacity-100">
                    <GripVertical size={12} />
                  </span>
                </div>

                <p className="truncate px-2 py-1.5 text-[11px] text-ink-muted">
                  {preview.file.name}
                </p>
              </li>
            ))}
          </ul>
        )}
      </div>

      <input
        ref={input}
        type="file"
        accept="image/*"
        multiple
        hidden
        onChange={(event) => {
          add(event.target.files);
          // Reset, żeby wybranie tego samego pliku po błędzie znów zadziałało.
          event.target.value = "";
        }}
      />
    </div>
  );
}
