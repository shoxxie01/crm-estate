package pl.delta.crm.property.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pl.delta.crm.property.dictionary.CommercialUse;
import pl.delta.crm.property.dictionary.Feature;
import pl.delta.crm.property.dictionary.GarageType;
import pl.delta.crm.property.dictionary.HeatingType;
import pl.delta.crm.property.dictionary.MarketType;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.RoomBathroom;
import pl.delta.crm.property.dictionary.TransactionType;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Formularz nowej oferty.
 *
 * <p>Obowiązkowy jest zestaw, bez którego oferta nie ma sensu jako oferta:
 * rodzaj, typ transakcji, rynek, tytuł, opis, cena, powierzchnia oraz
 * województwo i miejscowość. Reszta jest opcjonalna, bo praca agenta zwykle
 * wygląda tak, że najpierw powstaje szkic, a szczegóły dochodzą po oględzinach.
 *
 * <p>Numeru oferty tu nie ma — nadaje go serwis. Portal wymaga stabilnego,
 * unikalnego identyfikatora, ale wpisywanie go ręcznie było tylko okazją do
 * literówki i kolizji.
 *
 * <p>Zależności między polami (np. liczba pokoi wymagana dla mieszkania i domu)
 * sprawdza serwis — Bean Validation nie widzi tu jednego pola, tylko relację.
 */
public record CreatePropertyRequest(

        @NotNull(message = "Wybierz rodzaj nieruchomości.")
        PropertyType propertyType,

        @NotNull(message = "Wybierz typ transakcji.")
        TransactionType transactionType,

        @NotNull(message = "Wybierz rynek — portale wymagają tej informacji.")
        MarketType marketType,

        PropertyStatus status,

        @NotBlank(message = "Podaj tytuł ogłoszenia.")
        @Size(max = 50, message = "Tytuł może mieć najwyżej 50 znaków — dłuższy zostanie obcięty przez portal.")
        String title,

        @NotBlank(message = "Podaj opis.")
        @Size(max = 20_000, message = "Opis jest zbyt długi.")
        String description,

        @NotNull @Valid PricingRequest pricing,

        @NotNull @Valid AreaRequest area,

        @NotNull @Valid AddressRequest address,

        @Valid BuildingRequest building,

        @Valid EnergyRequest energy,

        @Valid LandRequest land,

        @Valid CommercialRequest commercial,

        /* Garaż / miejsce postojowe. */
        GarageType garageType,

        /* Pokój (wynajem). */
        @Min(value = 1, message = "Liczba osób musi być dodatnia.")
        @Max(value = 20, message = "Zbyt duża liczba osób.")
        Short occupants,

        RoomBathroom roomBathroom,

        LocalDate availableFrom,

        Set<Feature> features,

        Set<HeatingType> heatingTypes,

        Set<CommercialUse> commercialUses,

        @Size(max = 500)
        String videoUrl,

        @Size(max = 500)
        String panoramaUrl,

        String privateNotes,

        @Size(max = 200)
        String keysInfo,

        Boolean exportable,

        /** Agent prowadzący. Pusty = osoba dodająca ofertę. */
        UUID agentId
) {
}
