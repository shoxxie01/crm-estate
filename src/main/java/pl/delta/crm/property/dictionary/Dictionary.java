package pl.delta.crm.property.dictionary;

/**
 * Wspólny kontrakt słowników nieruchomości.
 *
 * <p>Każda wartość ma stałą nazwę techniczną ({@link Enum#name()}) — i tylko ona
 * trafia do bazy — oraz polską etykietę do wyświetlenia. Dzięki temu front
 * dostaje listy wyboru z jednego endpointu zamiast powielać dwadzieścia kilka
 * enumów w TypeScripcie i rozjeżdżać się z backendem przy każdej zmianie.
 *
 * <p>Nazwy są nasze, nie portalowe. Tłumaczenie na kody Otodom / Morizon /
 * nieruchomosci-online należy do modułu eksportu — patrz komentarz w migracji
 * {@code V3__create_properties_tables.sql}.
 */
public interface Dictionary {

    String name();

    String label();
}
