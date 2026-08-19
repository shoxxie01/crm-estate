package pl.delta.crm.property;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.delta.crm.agency.Agency;
import pl.delta.crm.client.Client;
import pl.delta.crm.property.dictionary.CommercialUse;
import pl.delta.crm.property.dictionary.Feature;
import pl.delta.crm.property.dictionary.GarageType;
import pl.delta.crm.property.dictionary.HeatingType;
import pl.delta.crm.property.dictionary.MarketType;
import pl.delta.crm.property.dictionary.PropertyStatus;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.RoomBathroom;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Oferta nieruchomości.
 *
 * <p>Zestaw pól jest podyktowany wymaganiami importu portali ogłoszeniowych —
 * szczegóły i uzasadnienie w migracji {@code V3__create_properties_tables.sql}.
 * Pola wymagane przez Otodom przy każdym typie obiektu (numer oferty, cena,
 * waluta, powierzchnia, opis, rynek, komplet lokalizacji) są tu
 * niemodyfikowalne po utworzeniu albo pilnowane przez {@code NOT NULL};
 * reszta jest opcjonalna i ustawiana setterami.
 */
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    /** Numer oferty w biurze. Otodom rozpoznaje po nim ogłoszenie (max 32 znaki). */
    @Column(name = "reference_number", nullable = false, length = 32)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    /**
     * Właściciel-zleceniodawca. Opcjonalny: oferta może powstać jako szkic, zanim
     * skojarzy się ją z klientem. To ta relacja — a nie pole na kliencie —
     * decyduje, czy klient jest „sprzedającym" czy „wynajmującym": wynika to
     * z {@code transactionType} powierzonych przez niego ofert.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_client_id")
    private Client owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", nullable = false, length = 20)
    private MarketType marketType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PropertyStatus status = PropertyStatus.ROBOCZA;

    /** Otodom obcina tytuł do 50 znaków — pilnujemy limitu u siebie. */
    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "description", nullable = false)
    private String description;

    @Embedded
    private Pricing pricing;

    @Column(name = "total_area", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalArea;

    @Column(name = "usable_area", precision = 10, scale = 2)
    private BigDecimal usableArea;

    /** Powierzchnia działki (Otodom: TerrainArea). */
    @Column(name = "plot_area", precision = 12, scale = 2)
    private BigDecimal plotArea;

    @Column(name = "rooms_count")
    private Short roomsCount;

    @Column(name = "bathrooms_count")
    private Short bathroomsCount;

    /** Umowa: -1 = suterena, 0 = parter, dalej numer piętra. */
    @Column(name = "floor_no")
    private Short floorNo;

    @Column(name = "building_floors_count")
    private Short buildingFloorsCount;

    @Column(name = "ceiling_height", precision = 5, scale = 2)
    private BigDecimal ceilingHeight;

    @Embedded
    private BuildingDetails building = new BuildingDetails();

    @Embedded
    private Address address;

    @Embedded
    private EnergyCertificate energyCertificate = new EnergyCertificate();

    @Embedded
    private LandDetails land = new LandDetails();

    @Embedded
    private CommercialDetails commercial = new CommercialDetails();

    /** Rodzaj garażu / miejsca postojowego — tylko dla typu GARAZ. */
    @Enumerated(EnumType.STRING)
    @Column(name = "garage_type", length = 30)
    private GarageType garageType;

    /** Dla ilu osób przeznaczony pokój — tylko dla typu POKOJ. */
    @Column(name = "occupants")
    private Short occupants;

    /** Dostęp do łazienki przy wynajmie pokoju — tylko dla typu POKOJ. */
    @Enumerated(EnumType.STRING)
    @Column(name = "room_bathroom", length = 20)
    private RoomBathroom roomBathroom;

    /** Od kiedy wolne (Otodom: FreeFrom). */
    @Column(name = "available_from")
    private LocalDate availableFrom;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "property_features", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "feature", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private Set<Feature> features = EnumSet.noneOf(Feature.class);

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "property_heating", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "heating_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<HeatingType> heatingTypes = EnumSet.noneOf(HeatingType.class);

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "property_commercial_uses", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "commercial_use", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<CommercialUse> commercialUses = EnumSet.noneOf(CommercialUse.class);

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<PropertyMedia> media = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PortalPublication> publications = new ArrayList<>();

    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /** Wirtualny spacer (Otodom: Panorama). */
    @Column(name = "panorama_url", length = 500)
    private String panoramaUrl;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "private_notes")
    private String privateNotes;

    @Column(name = "keys_info", length = 200)
    private String keysInfo;

    @Column(name = "exportable", nullable = false)
    private boolean exportable = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Property() {
        // wymagane przez JPA
    }

    /**
     * Konstruktor przyjmuje dokładnie te pola, bez których żaden portal nie
     * przyjmie ogłoszenia. Wszystko pozostałe da się uzupełnić później —
     * i tak zwykle wygląda praca agenta: najpierw szkic, potem szczegóły.
     */
    public Property(Agency agency,
                    String referenceNumber,
                    User agent,
                    User createdBy,
                    PropertyType propertyType,
                    TransactionType transactionType,
                    MarketType marketType,
                    String title,
                    String description,
                    Pricing pricing,
                    BigDecimal totalArea,
                    Address address) {
        this.agency = agency;
        this.referenceNumber = referenceNumber;
        this.agent = agent;
        this.createdBy = createdBy;
        this.propertyType = propertyType;
        this.transactionType = transactionType;
        this.marketType = marketType;
        this.title = title;
        this.description = description;
        this.pricing = pricing;
        this.totalArea = totalArea;
        this.address = address;
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    /**
     * Czy oferta jest kompletna na tyle, żeby dało się ją wysłać do portalu.
     * Sprawdza wyłącznie warunki wspólne dla wszystkich serwisów; walidacja
     * pod konkretny portal należy do modułu eksportu, który zna jego słowniki.
     */
    public boolean readyForExport() {
        if (!exportable || !status.publishable()) {
            return false;
        }
        if (propertyType.requiresRoomsCount() && roomsCount == null) {
            return false;
        }
        // Otodom wymaga kompletu kraj + województwo + powiat + miasto. Powiat
        // jest opcjonalny przy zapisie, więc brak wychodzi dopiero tutaj.
        if (address.getCounty() == null || address.getCounty().isBlank()) {
            return false;
        }
        if (media.isEmpty()) {
            return false;
        }
        // Działka, garaż i pokój nie wymagają świadectwa energetycznego.
        if (!propertyType.requiresEnergyCertificate()) {
            return true;
        }
        return getEnergyCertificate().satisfiesLegalRequirement();
    }

    public void addMedia(PropertyMedia item) {
        item.attachTo(this, (short) media.size());
        media.add(item);
    }

    public UUID getId() {
        return id;
    }

    public Agency getAgency() {
        return agency;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public User getAgent() {
        return agent;
    }

    public void setAgent(User agent) {
        this.agent = agent;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Client getOwner() {
        return owner;
    }

    public void setOwner(Client owner) {
        this.owner = owner;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public MarketType getMarketType() {
        return marketType;
    }

    public void setMarketType(MarketType marketType) {
        this.marketType = marketType;
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public void setStatus(PropertyStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Pricing getPricing() {
        return pricing;
    }

    public void setPricing(Pricing pricing) {
        this.pricing = pricing;
    }

    public BigDecimal getTotalArea() {
        return totalArea;
    }

    public void setTotalArea(BigDecimal totalArea) {
        this.totalArea = totalArea;
    }

    public BigDecimal getUsableArea() {
        return usableArea;
    }

    public void setUsableArea(BigDecimal usableArea) {
        this.usableArea = usableArea;
    }

    public BigDecimal getPlotArea() {
        return plotArea;
    }

    public void setPlotArea(BigDecimal plotArea) {
        this.plotArea = plotArea;
    }

    public Short getRoomsCount() {
        return roomsCount;
    }

    public void setRoomsCount(Short roomsCount) {
        this.roomsCount = roomsCount;
    }

    public Short getBathroomsCount() {
        return bathroomsCount;
    }

    public void setBathroomsCount(Short bathroomsCount) {
        this.bathroomsCount = bathroomsCount;
    }

    public Short getFloorNo() {
        return floorNo;
    }

    public void setFloorNo(Short floorNo) {
        this.floorNo = floorNo;
    }

    public Short getBuildingFloorsCount() {
        return buildingFloorsCount;
    }

    public void setBuildingFloorsCount(Short buildingFloorsCount) {
        this.buildingFloorsCount = buildingFloorsCount;
    }

    public BigDecimal getCeilingHeight() {
        return ceilingHeight;
    }

    public void setCeilingHeight(BigDecimal ceilingHeight) {
        this.ceilingHeight = ceilingHeight;
    }

    // Kiedy wszystkie kolumny grupy @Embeddable są puste, Hibernate wczytuje ją
    // jako null zamiast pustego obiektu. Dla nas „działka bez wypełnionych pól"
    // i „brak danych o działce" to ten sam stan, więc gettery domykają tę różnicę
    // — dzięki temu żaden mapper ani eksport nie musi sprawdzać null-a.

    public BuildingDetails getBuilding() {
        if (building == null) {
            building = new BuildingDetails();
        }
        return building;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public EnergyCertificate getEnergyCertificate() {
        if (energyCertificate == null) {
            energyCertificate = new EnergyCertificate();
        }
        return energyCertificate;
    }

    public LandDetails getLand() {
        if (land == null) {
            land = new LandDetails();
        }
        return land;
    }

    public CommercialDetails getCommercial() {
        if (commercial == null) {
            commercial = new CommercialDetails();
        }
        return commercial;
    }

    public GarageType getGarageType() {
        return garageType;
    }

    public void setGarageType(GarageType garageType) {
        this.garageType = garageType;
    }

    public Short getOccupants() {
        return occupants;
    }

    public void setOccupants(Short occupants) {
        this.occupants = occupants;
    }

    public RoomBathroom getRoomBathroom() {
        return roomBathroom;
    }

    public void setRoomBathroom(RoomBathroom roomBathroom) {
        this.roomBathroom = roomBathroom;
    }

    public LocalDate getAvailableFrom() {
        return availableFrom;
    }

    public void setAvailableFrom(LocalDate availableFrom) {
        this.availableFrom = availableFrom;
    }

    public Set<Feature> getFeatures() {
        return features;
    }

    public void setFeatures(Set<Feature> features) {
        this.features.clear();
        this.features.addAll(features);
    }

    public Set<HeatingType> getHeatingTypes() {
        return heatingTypes;
    }

    public void setHeatingTypes(Set<HeatingType> heatingTypes) {
        this.heatingTypes.clear();
        this.heatingTypes.addAll(heatingTypes);
    }

    public Set<CommercialUse> getCommercialUses() {
        return commercialUses;
    }

    public void setCommercialUses(Set<CommercialUse> commercialUses) {
        this.commercialUses.clear();
        this.commercialUses.addAll(commercialUses);
    }

    public List<PropertyMedia> getMedia() {
        return media;
    }

    public List<PortalPublication> getPublications() {
        return publications;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getPanoramaUrl() {
        return panoramaUrl;
    }

    public void setPanoramaUrl(String panoramaUrl) {
        this.panoramaUrl = panoramaUrl;
    }

    public String getPrivateNotes() {
        return privateNotes;
    }

    public void setPrivateNotes(String privateNotes) {
        this.privateNotes = privateNotes;
    }

    public String getKeysInfo() {
        return keysInfo;
    }

    public void setKeysInfo(String keysInfo) {
        this.keysInfo = keysInfo;
    }

    public boolean isExportable() {
        return exportable;
    }

    public void setExportable(boolean exportable) {
        this.exportable = exportable;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
