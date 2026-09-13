package pl.delta.crm.client.requirement;

import pl.delta.crm.property.dictionary.TransactionType;

import java.util.UUID;

/** Ile poszukiwań danego typu transakcji ma klient — pod etykietę „kupujący / najemca". */
public interface RequirementTransactionCount {

    UUID getClientId();

    TransactionType getTransactionType();

    long getCount();
}
