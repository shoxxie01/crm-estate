package pl.delta.crm.property;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import pl.delta.crm.error.BusinessValidationException;
import pl.delta.crm.error.MediaNotFoundException;
import pl.delta.crm.error.PropertyNotFoundException;
import pl.delta.crm.property.dictionary.MediaType;
import pl.delta.crm.property.dto.MediaResponse;
import pl.delta.crm.storage.MediaStorage;
import pl.delta.crm.user.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Galeria oferty: dodawanie, podmiana, kasowanie i kolejność.
 *
 * <p><b>Kolejność zapisu jest asymetryczna i to jest celowe.</b> Przy dodawaniu
 * najpierw leci plik do storage'u, potem wiersz do bazy; przy kasowaniu najpierw
 * znika wiersz, a plik dopiero po commicie. W obu wypadkach awaria w połowie
 * zostawia najwyżej plik bez wiersza. Czyli zajęte miejsce. Odwrotna kolejność
 * zostawiałaby wiersz bez pliku, a to już dziura w galerii i oferta, której
 * eksport nie ma z czego złożyć paczki.
 */
@Service
public class PropertyMediaService {

    private final PropertyRepository properties;
    private final PropertyMediaRepository media;
    private final MediaStorage storage;
    private final ImageProcessor images;

    public PropertyMediaService(PropertyRepository properties, PropertyMediaRepository media,
                                MediaStorage storage, ImageProcessor images) {
        this.properties = properties;
        this.media = media;
        this.storage = storage;
        this.images = images;
    }

    @Transactional
    public List<MediaResponse> upload(UUID propertyId, List<MultipartFile> files,
                                      MediaType type, User actor) {

        Property property = property(propertyId, actor);
        MediaType mediaType = resolveType(type);

        if (files == null || files.isEmpty()) {
            throw new BusinessValidationException(Map.of("files", "Nie wybrano żadnego pliku."));
        }

        int already = property.getMedia().size();
        if (already + files.size() > PropertyMedia.MAX_PER_PROPERTY) {
            throw new BusinessValidationException(Map.of("files",
                    "Oferta może mieć najwyżej %d zdjęć. Dodano już %d."
                            .formatted(PropertyMedia.MAX_PER_PROPERTY, already)));
        }

        List<PropertyMedia> added = new ArrayList<>();
        for (MultipartFile file : files) {
            added.add(store(property, file, mediaType));
        }

        // Sam flush, bez save(): oferta jest już zarządzana, a save() na takiej
        // encji robi merge, który nowe zdjęcia *kopiuje*. Do bazy trafiłyby
        // kopie, a obiekty z `added` zostałyby bez identyfikatorów i odpowiedź
        // wróciłaby z `id: null`. Flush jest tu potrzebny, bo identyfikatory
        // powstają dopiero przy zapisie, a front musi mieć czym zaadresować
        // kasowanie i zmianę kolejności.
        properties.flush();
        return added.stream().map(item -> MediaResponse.from(item, storage::url)).toList();
    }

    /**
     * Podmiana pliku pod istniejącym wpisem. Pozycja, podpis i identyfikator
     * zostają. Nowy plik ląduje pod tym samym kluczem, więc stara wersja znika
     * bezpowrotnie; galeria pokazuje ją jeszcze przez czas życia podpisanego
     * linku, bo przeglądarka ma ją w cache'u.
     */
    @Transactional
    public MediaResponse replaceFile(UUID propertyId, UUID mediaId, MultipartFile file, User actor) {
        Property property = property(propertyId, actor);
        PropertyMedia item = find(property, mediaId);

        ImageProcessor.ProcessedImage processed = images.process(read(file, "file"), "file");

        storage.put(item.getStorageKey(), processed.full(), ImageProcessor.OUTPUT_CONTENT_TYPE);
        storage.put(item.thumbnailKey(), processed.thumbnail(), ImageProcessor.OUTPUT_CONTENT_TYPE);

        item.replaceFile(fileName(file), ImageProcessor.OUTPUT_CONTENT_TYPE,
                processed.full().length, processed.widthPx(), processed.heightPx());

        // Bez save(). Encja jest zarządzana, zmianę zapisze dirty checking.
        return MediaResponse.from(item, storage::url);
    }

    @Transactional
    public void delete(UUID propertyId, UUID mediaId, User actor) {
        Property property = property(propertyId, actor);
        PropertyMedia item = find(property, mediaId);

        property.getMedia().remove(item);
        renumber(property.getMedia());
        // Hibernate wypuszcza UPDATE-y przed DELETE, więc przenumerowane pozycje
        // na moment zderzają się z wierszem, który dopiero zniknie. Unikat jest
        // odroczony do commitu (V12), więc ten stan przejściowy jest legalny.
        properties.flush();

        // Dopiero po commicie: gdyby transakcja się cofnęła, wiersz zostaje,
        // a skasowanego pliku nikt już nie odtworzy.
        deleteAfterCommit(item.getStorageKey(), item.thumbnailKey());
    }

    /**
     * Nowa kolejność galerii. Wymagamy kompletu identyfikatorów, bo lista krótsza
     * od galerii nie mówi, gdzie mają wylądować pominięte zdjęcia. A zgadywanie
     * kończyłoby się przestawieniem czegoś, czego użytkownik nie ruszał.
     */
    @Transactional
    public List<MediaResponse> reorder(UUID propertyId, List<UUID> mediaIds, User actor) {
        Property property = property(propertyId, actor);
        List<PropertyMedia> current = property.getMedia();

        Set<UUID> requested = new LinkedHashSet<>(mediaIds);
        if (requested.size() != mediaIds.size()) {
            throw new BusinessValidationException(
                    Map.of("mediaIds", "Ten sam materiał podany więcej niż raz."));
        }
        if (!requested.equals(current.stream().map(PropertyMedia::getId).collect(Collectors.toSet()))) {
            throw new BusinessValidationException(
                    Map.of("mediaIds", "Podaj wszystkie materiały oferty w docelowej kolejności."));
        }

        Map<UUID, PropertyMedia> byId = new HashMap<>();
        current.forEach(item -> byId.put(item.getId(), item));

        // Unikat (property_id, position) jest odroczony do commitu (migracja V12),
        // więc stany przejściowe z duplikatem pozycji są tu dozwolone.
        short position = 0;
        List<PropertyMedia> ordered = new ArrayList<>(current.size());
        for (UUID id : mediaIds) {
            PropertyMedia item = byId.get(id);
            item.setPosition(position++);
            ordered.add(item);
        }

        return ordered.stream().map(item -> MediaResponse.from(item, storage::url)).toList();
    }

    @Transactional
    public MediaResponse updateCaption(UUID propertyId, UUID mediaId, String caption, User actor) {
        Property property = property(propertyId, actor);
        PropertyMedia item = find(property, mediaId);

        String trimmed = caption == null || caption.isBlank() ? null : caption.trim();
        item.setCaption(trimmed);

        return MediaResponse.from(item, storage::url);
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> list(UUID propertyId, User viewer) {
        Property property = property(propertyId, viewer);
        return media.findByPropertyIdOrderByPositionAsc(property.getId()).stream()
                .map(item -> MediaResponse.from(item, storage::url))
                .toList();
    }

    /**
     * Pliki oferty kasowanej w całości.
     *
     * <p>Wiersze {@code property_media} znikają kaskadą (JPA + {@code ON DELETE
     * CASCADE} w V3), ale kaskada bazy nie wie nic o storage'u. Bez tego każde
     * skasowanie oferty zostawiałoby komplet jej zdjęć jako sieroty. Wołane
     * z {@link PropertyService#delete}, w jego transakcji.
     */
    void purgeStorageFor(Property property) {
        List<String> keys = new ArrayList<>();
        for (PropertyMedia item : property.getMedia()) {
            keys.add(item.getStorageKey());
            keys.add(item.thumbnailKey());
        }
        if (!keys.isEmpty()) {
            deleteAfterCommit(keys.toArray(String[]::new));
        }
    }

    // --- środek --------------------------------------------------------------

    private PropertyMedia store(Property property, MultipartFile file, MediaType mediaType) {
        byte[] bytes = read(file, "files");
        ImageProcessor.ProcessedImage processed = images.process(bytes, "files");

        String key = PropertyMedia.buildStorageKey(
                property.getAgency().getId(), property.getId());

        PropertyMedia item = new PropertyMedia(mediaType, key, fileName(file),
                ImageProcessor.OUTPUT_CONTENT_TYPE, processed.full().length);
        item.setDimensions(processed.widthPx(), processed.heightPx());

        storage.put(key, processed.full(), ImageProcessor.OUTPUT_CONTENT_TYPE);
        storage.put(item.thumbnailKey(), processed.thumbnail(), ImageProcessor.OUTPUT_CONTENT_TYPE);
        // Plik jest już w storage, a wiersz jeszcze nie w bazie. Gdyby transakcja
        // padła (np. na innym pliku z tej samej paczki), zostałby sierotą.
        deleteOnRollback(key, item.thumbnailKey());

        property.addMedia(item);
        return item;
    }

    private Property property(UUID propertyId, User actor) {
        return properties.findByIdAndAgencyId(propertyId, actor.getAgency().getId())
                .orElseThrow(PropertyNotFoundException::new);
    }

    /** Materiał szukany w obrębie oferty. Obcy identyfikator daje 404, nie 403. */
    private PropertyMedia find(Property property, UUID mediaId) {
        return property.getMedia().stream()
                .filter(item -> item.getId().equals(mediaId))
                .findFirst()
                .orElseThrow(MediaNotFoundException::new);
    }

    /** Pozycje muszą być ciągiem 0..n-1, inaczej „zdjęcie główne" przestaje być pierwsze. */
    private void renumber(List<PropertyMedia> items) {
        short position = 0;
        for (PropertyMedia item : items) {
            item.setPosition(position++);
        }
    }

    private MediaType resolveType(MediaType type) {
        if (type == null) {
            return MediaType.PHOTO;
        }
        if (type == MediaType.DOCUMENT) {
            throw new BusinessValidationException(
                    Map.of("type", "Tą drogą wgrywa się wyłącznie obrazy. Zdjęcia i rzuty."));
        }
        return type;
    }

    private byte[] read(MultipartFile file, String field) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException(Map.of(field, "Plik jest pusty."));
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessValidationException(Map.of(field, "Nie udało się odczytać pliku."));
        }
    }

    /**
     * Nazwa do pokazania i do paczki eksportu. Bierzemy samą nazwę pliku bez
     * ścieżki Niektóre przeglądarki wysyłają pełną i przycinamy do długości
     * kolumny.
     */
    private String fileName(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "zdjecie" + ImageProcessor.OUTPUT_EXTENSION;
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        if (name.isEmpty()) {
            name = "zdjecie" + ImageProcessor.OUTPUT_EXTENSION;
        }
        return name.length() <= 120 ? name : name.substring(0, 120);
    }

    private void deleteAfterCommit(String... keys) {
        Set<String> pending = new HashSet<>(List.of(keys));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pending.forEach(storage::delete);
            }
        });
    }

    private void deleteOnRollback(String... keys) {
        Set<String> pending = new HashSet<>(List.of(keys));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    pending.forEach(storage::delete);
                }
            }
        });
    }
}
