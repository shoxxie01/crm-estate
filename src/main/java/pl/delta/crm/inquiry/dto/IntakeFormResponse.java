package pl.delta.crm.inquiry.dto;

import pl.delta.crm.property.dto.DictionaryEntry;

import java.util.List;

/**
 * Wszystko, czego potrzebuje publiczny formularz: dane biura, treść zgód
 * i słowniki. Jednym żądaniem, bez logowania — i bez niczego ponad to.
 */
public record IntakeFormResponse(
        String agencyName,
        String consentProcessingText,
        String consentMarketingText,
        String privacyNote,
        List<DictionaryEntry> propertyType,
        List<DictionaryEntry> financing
) {
}
