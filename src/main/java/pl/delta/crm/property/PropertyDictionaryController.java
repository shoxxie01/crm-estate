package pl.delta.crm.property;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.property.dictionary.BuildingMaterial;
import pl.delta.crm.property.dictionary.BuildingType;
import pl.delta.crm.property.dictionary.CommercialUse;
import pl.delta.crm.property.dictionary.ConstructionStatus;
import pl.delta.crm.property.dictionary.Currency;
import pl.delta.crm.property.dictionary.EnergyClass;
import pl.delta.crm.property.dictionary.Feature;
import pl.delta.crm.property.dictionary.Flooring;
import pl.delta.crm.property.dictionary.GarageType;
import pl.delta.crm.property.dictionary.GarretType;
import pl.delta.crm.property.dictionary.HallStructure;
import pl.delta.crm.property.dictionary.HeatingType;
import pl.delta.crm.property.dictionary.MarketType;
import pl.delta.crm.property.dictionary.OwnershipForm;
import pl.delta.crm.property.dictionary.ParkingType;
import pl.delta.crm.property.dictionary.PlotType;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.RoadAccess;
import pl.delta.crm.property.dictionary.RoomBathroom;
import pl.delta.crm.property.dictionary.RoofType;
import pl.delta.crm.property.dictionary.Roofing;
import pl.delta.crm.property.dictionary.Surroundings;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.property.dictionary.Voivodeship;
import pl.delta.crm.property.dictionary.WindowsType;
import pl.delta.crm.property.dto.DictionaryEntry;
import pl.delta.crm.property.dto.FeatureGroup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Słowniki formularza w jednym miejscu.
 *
 * <p>Alternatywą byłoby przepisanie dwudziestu kilku enumów do TypeScriptu —
 * i rozjeżdżanie się ich z backendem przy każdej zmianie. Odpowiedź jest
 * statyczna i nadaje się do zacache'owania po stronie frontu na czas sesji.
 */
@RestController
@RequestMapping("/api/properties/dictionaries")
public class PropertyDictionaryController {

    @GetMapping
    public Map<String, Object> dictionaries() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("propertyType", DictionaryEntry.of(PropertyType.class));
        result.put("transactionType", DictionaryEntry.of(TransactionType.class));
        result.put("marketType", DictionaryEntry.of(MarketType.class));
        result.put("status", DictionaryEntry.of(PropertyStatus.class));
        result.put("currency", DictionaryEntry.of(Currency.class));
        result.put("voivodeship", DictionaryEntry.of(Voivodeship.class));
        result.put("ownershipForm", DictionaryEntry.of(OwnershipForm.class));
        result.put("buildingType", DictionaryEntry.of(BuildingType.class));
        result.put("buildingMaterial", DictionaryEntry.of(BuildingMaterial.class));
        result.put("constructionStatus", DictionaryEntry.of(ConstructionStatus.class));
        result.put("windowsType", DictionaryEntry.of(WindowsType.class));
        result.put("roofType", DictionaryEntry.of(RoofType.class));
        result.put("roofing", DictionaryEntry.of(Roofing.class));
        result.put("garretType", DictionaryEntry.of(GarretType.class));
        result.put("surroundings", DictionaryEntry.of(Surroundings.class));
        result.put("heatingType", DictionaryEntry.of(HeatingType.class));
        result.put("plotType", DictionaryEntry.of(PlotType.class));
        result.put("roadAccess", DictionaryEntry.of(RoadAccess.class));
        result.put("commercialUse", DictionaryEntry.of(CommercialUse.class));
        result.put("hallStructure", DictionaryEntry.of(HallStructure.class));
        result.put("flooring", DictionaryEntry.of(Flooring.class));
        result.put("parkingType", DictionaryEntry.of(ParkingType.class));
        result.put("garageType", DictionaryEntry.of(GarageType.class));
        result.put("roomBathroom", DictionaryEntry.of(RoomBathroom.class));
        result.put("energyClass", DictionaryEntry.of(EnergyClass.class));
        result.put("featureGroups", featureGroups());

        return result;
    }

    private static List<FeatureGroup> featureGroups() {
        return Feature.byCategory().entrySet().stream()
                .map(entry -> new FeatureGroup(
                        entry.getKey().name(),
                        entry.getKey().label(),
                        entry.getValue().stream()
                                .map(f -> new FeatureGroup.FeatureView(
                                        f.name(),
                                        f.label(),
                                        f.types().stream().map(Enum::name).toList()))
                                .toList()))
                .toList();
    }
}
