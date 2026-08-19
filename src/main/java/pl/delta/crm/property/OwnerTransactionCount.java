package pl.delta.crm.property;

import pl.delta.crm.property.dictionary.TransactionType;

import java.util.UUID;

/**
 * Ile ofert danego typu transakcji ma powierzonych dany właściciel. Projekcja
 * pod listę klientów: z niej wyprowadzamy etykietę „sprzedający / wynajmujący",
 * bez ładowania kolekcji ofert dla każdego klienta z osobna.
 */
public interface OwnerTransactionCount {

    UUID getOwnerId();

    TransactionType getTransactionType();

    long getCount();
}
