package pl.delta.crm.inquiry.dto;

import java.util.UUID;

/** Pusty {@code clientId} = załóż nowego klienta; podany = dopnij poszukiwanie do istniejącego. */
public record ConvertInquiryRequest(UUID clientId) {
}
