package pl.delta.crm.property;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pl.delta.crm.PostgresTestcontainerConfig;
import pl.delta.crm.calendar.dictionary.EventOutcome;
import pl.delta.crm.calendar.dictionary.EventStatus;
import pl.delta.crm.calendar.dictionary.EventType;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dictionary.Financing;
import pl.delta.crm.client.dictionary.LeadSource;
import pl.delta.crm.client.dictionary.RequirementStatus;
import pl.delta.crm.inquiry.InquiryStatus;
import pl.delta.crm.property.dictionary.BuildingMaterial;
import pl.delta.crm.property.dictionary.BuildingType;
import pl.delta.crm.property.dictionary.CommercialUse;
import pl.delta.crm.property.dictionary.ConstructionStatus;
import pl.delta.crm.property.dictionary.Currency;
import pl.delta.crm.property.dictionary.EnergyClass;
import pl.delta.crm.property.dictionary.Feature;
import pl.delta.crm.property.dictionary.Flooring;
import pl.delta.crm.property.dictionary.GarretType;
import pl.delta.crm.property.dictionary.HallStructure;
import pl.delta.crm.property.dictionary.HeatingType;
import pl.delta.crm.property.dictionary.MarketType;
import pl.delta.crm.property.dictionary.MediaType;
import pl.delta.crm.property.dictionary.OwnershipForm;
import pl.delta.crm.property.dictionary.ParkingType;
import pl.delta.crm.property.dictionary.PlotType;
import pl.delta.crm.property.dictionary.Portal;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.PublicationStatus;
import pl.delta.crm.property.dictionary.RoadAccess;
import pl.delta.crm.property.dictionary.RoofType;
import pl.delta.crm.property.dictionary.Roofing;
import pl.delta.crm.property.dictionary.Surroundings;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.property.dictionary.Voivodeship;
import pl.delta.crm.property.dictionary.WindowsType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Każda kolumna słownikowa musi pomieścić najdłuższą nazwę stałej swojego enuma.
 *
 * <p>Ten test istnieje, bo `ddl-auto: validate` tego nie sprawdza: Hibernate
 * weryfikuje istnienie i typ kolumny, ale nie porównuje jej długości z zawartością
 * enuma. Pierwsza ofiara. `energy_class VARCHAR(3)`, którego długość dobrano pod
 * etykietę „A+", podczas gdy do bazy idzie nazwa stałej `A_PLUS`. Błąd wychodził
 * dopiero przy zapisie oferty z tą jedną klasą energetyczną.
 *
 * <p>Zamiast testu na jeden przypadek sprawdzamy wszystkie naraz. Dołożenie
 * dłuższej wartości do dowolnego słownika zapali się tutaj, a nie u użytkownika.
 */
@SpringBootTest
@Import(PostgresTestcontainerConfig.class)
class EnumColumnWidthTest {

    /** kolumna -> enum, który do niej trafia. */
    private static final Map<String, Class<? extends Enum<?>>> MAPPING = new LinkedHashMap<>();

    static {
        MAPPING.put("properties.property_type", PropertyType.class);
        MAPPING.put("properties.transaction_type", TransactionType.class);
        MAPPING.put("properties.market_type", MarketType.class);
        MAPPING.put("properties.status", PropertyStatus.class);
        MAPPING.put("properties.price_currency", Currency.class);
        MAPPING.put("properties.rent_currency", Currency.class);
        MAPPING.put("properties.deposit_currency", Currency.class);
        MAPPING.put("properties.voivodeship", Voivodeship.class);
        MAPPING.put("properties.building_type", BuildingType.class);
        MAPPING.put("properties.building_material", BuildingMaterial.class);
        MAPPING.put("properties.construction_status", ConstructionStatus.class);
        MAPPING.put("properties.windows_type", WindowsType.class);
        MAPPING.put("properties.roof_type", RoofType.class);
        MAPPING.put("properties.roofing", Roofing.class);
        MAPPING.put("properties.garret_type", GarretType.class);
        MAPPING.put("properties.ownership_form", OwnershipForm.class);
        MAPPING.put("properties.surroundings", Surroundings.class);
        MAPPING.put("properties.energy_class", EnergyClass.class);
        MAPPING.put("properties.plot_type", PlotType.class);
        MAPPING.put("properties.road_access", RoadAccess.class);
        MAPPING.put("properties.hall_structure", HallStructure.class);
        MAPPING.put("properties.hall_flooring", Flooring.class);
        MAPPING.put("properties.parking_type", ParkingType.class);
        MAPPING.put("property_features.feature", Feature.class);
        MAPPING.put("property_heating.heating_type", HeatingType.class);
        MAPPING.put("property_commercial_uses.commercial_use", CommercialUse.class);
        MAPPING.put("property_media.media_type", MediaType.class);
        MAPPING.put("property_portal_publications.portal", Portal.class);
        MAPPING.put("property_portal_publications.status", PublicationStatus.class);
        MAPPING.put("clients.source", LeadSource.class);
        MAPPING.put("clients.status", ClientStatus.class);
        MAPPING.put("client_requirements.status", RequirementStatus.class);
        MAPPING.put("client_requirements.transaction_type", TransactionType.class);
        MAPPING.put("client_requirements.market_type", MarketType.class);
        MAPPING.put("client_requirements.financing", Financing.class);
        MAPPING.put("client_requirement_property_types.property_type", PropertyType.class);
        MAPPING.put("client_requirement_features.feature", Feature.class);
        MAPPING.put("client_inquiries.status", InquiryStatus.class);
        MAPPING.put("calendar_events.type", EventType.class);
        MAPPING.put("calendar_events.status", EventStatus.class);
        MAPPING.put("calendar_events.outcome", EventOutcome.class);
    }

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("każda kolumna słownikowa mieści najdłuższą nazwę swojego enuma")
    void everyDictionaryColumnFitsItsLongestConstant() throws Exception {
        StringBuilder problems = new StringBuilder();

        try (Connection connection = dataSource.getConnection()) {
            for (Map.Entry<String, Class<? extends Enum<?>>> entry : MAPPING.entrySet()) {
                String[] parts = entry.getKey().split("\\.");
                Integer columnLength = columnLength(connection, parts[0], parts[1]);

                assertThat(columnLength)
                        .as("kolumna %s nie istnieje albo nie jest tekstowa", entry.getKey())
                        .isNotNull();

                String longest = Arrays.stream(entry.getValue().getEnumConstants())
                        .map(Enum::name)
                        .max(Comparator.comparingInt(String::length))
                        .orElseThrow();

                if (longest.length() > columnLength) {
                    problems.append("\n  %s: VARCHAR(%d), a najdłuższa wartość to %s (%d znaków)"
                            .formatted(entry.getKey(), columnLength, longest, longest.length()));
                }
            }
        }

        if (!problems.isEmpty()) {
            fail("Kolumny za wąskie dla swoich enumów:" + problems);
        }
    }

    private static Integer columnLength(Connection connection, String table, String column)
            throws Exception {
        String sql = """
                SELECT character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                int length = rs.getInt(1);
                return rs.wasNull() ? null : length;
            }
        }
    }
}
