package pl.delta.crm.client.requirement;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pl.delta.crm.agency.Agency;
import pl.delta.crm.client.Client;
import pl.delta.crm.client.dictionary.Financing;
import pl.delta.crm.client.dictionary.RequirementStatus;
import pl.delta.crm.property.dictionary.Feature;
import pl.delta.crm.property.dictionary.MarketType;
import pl.delta.crm.property.dictionary.PropertyType;
import pl.delta.crm.property.dictionary.TransactionType;
import pl.delta.crm.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Poszukiwanie. Czego klient szuka jako kupujący albo najemca.
 *
 * <p>Osobna encja, a nie pola na {@link Client}: ta sama osoba potrafi szukać
 * kilku rzeczy naraz, a każde poszukiwanie ma własny stan (aktywne, wstrzymane,
 * zrealizowane). Z aktywnych poszukiwań wynika rola kupującego / najemcy.
 * Dokładnie tak, jak rola sprzedającego wynika z powierzonych ofert.
 *
 * <p>Każda granica zakresu jest opcjonalna. Klient przez telefon mówi „do
 * 600 tysięcy, minimum dwa pokoje". I tyle ma trafić do bazy, bez zmyślania
 * brakujących połówek.
 */
@Entity
@Table(name = "client_requirements")
public class ClientRequirement {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false, updatable = false)
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequirementStatus status = RequirementStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_type", length = 20)
    private MarketType marketType;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "client_requirement_property_types",
            joinColumns = @JoinColumn(name = "requirement_id"))
    @Column(name = "property_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Set<PropertyType> propertyTypes = EnumSet.noneOf(PropertyType.class);

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "client_requirement_locations",
            joinColumns = @JoinColumn(name = "requirement_id"))
    @OrderColumn(name = "position")
    private List<RequirementLocation> locations = new ArrayList<>();

    @Column(name = "price_min", precision = 14, scale = 2)
    private BigDecimal priceMin;

    @Column(name = "price_max", precision = 14, scale = 2)
    private BigDecimal priceMax;

    @Column(name = "area_min", precision = 10, scale = 2)
    private BigDecimal areaMin;

    @Column(name = "area_max", precision = 10, scale = 2)
    private BigDecimal areaMax;

    @Column(name = "rooms_min")
    private Short roomsMin;

    @Column(name = "rooms_max")
    private Short roomsMax;

    @Column(name = "floor_min")
    private Short floorMin;

    @Column(name = "floor_max")
    private Short floorMax;

    @Column(name = "exclude_top_floor", nullable = false)
    private boolean excludeTopFloor;

    @Enumerated(EnumType.STRING)
    @Column(name = "financing", length = 20)
    private Financing financing;

    @Column(name = "move_in_date")
    private LocalDate moveInDate;

    /** Cecha → czy konieczna ({@code true}) czy tylko mile widziana ({@code false}). */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "client_requirement_features",
            joinColumns = @JoinColumn(name = "requirement_id"))
    @MapKeyColumn(name = "feature", length = 40)
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "required", nullable = false)
    private Map<Feature, Boolean> features = new EnumMap<>(Feature.class);

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ClientRequirement() {
        // wymagane przez JPA
    }

    public ClientRequirement(Client client, User createdBy, TransactionType transactionType) {
        this.agency = client.getAgency();
        this.client = client;
        this.createdBy = createdBy;
        this.transactionType = transactionType;
    }

    private ClientRequirement(Agency agency, TransactionType transactionType) {
        this.agency = agency;
        this.transactionType = transactionType;
    }

    /**
     * Poszukiwanie bez klienta, <b>nigdy nie zapisywane</b>. Do podglądu
     * pasujących ofert dla zgłoszenia, zanim agent zamieni je na klienta.
     */
    public static ClientRequirement draft(Agency agency, TransactionType transactionType) {
        return new ClientRequirement(agency, transactionType);
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public RequirementStatus getStatus() {
        return status;
    }

    public void setStatus(RequirementStatus status) {
        this.status = status;
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

    public Set<PropertyType> getPropertyTypes() {
        return propertyTypes;
    }

    public void setPropertyTypes(Set<PropertyType> propertyTypes) {
        this.propertyTypes.clear();
        this.propertyTypes.addAll(propertyTypes);
    }

    public List<RequirementLocation> getLocations() {
        return locations;
    }

    public void setLocations(List<RequirementLocation> locations) {
        this.locations.clear();
        this.locations.addAll(locations);
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(BigDecimal priceMin) {
        this.priceMin = priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(BigDecimal priceMax) {
        this.priceMax = priceMax;
    }

    public BigDecimal getAreaMin() {
        return areaMin;
    }

    public void setAreaMin(BigDecimal areaMin) {
        this.areaMin = areaMin;
    }

    public BigDecimal getAreaMax() {
        return areaMax;
    }

    public void setAreaMax(BigDecimal areaMax) {
        this.areaMax = areaMax;
    }

    public Short getRoomsMin() {
        return roomsMin;
    }

    public void setRoomsMin(Short roomsMin) {
        this.roomsMin = roomsMin;
    }

    public Short getRoomsMax() {
        return roomsMax;
    }

    public void setRoomsMax(Short roomsMax) {
        this.roomsMax = roomsMax;
    }

    public Short getFloorMin() {
        return floorMin;
    }

    public void setFloorMin(Short floorMin) {
        this.floorMin = floorMin;
    }

    public Short getFloorMax() {
        return floorMax;
    }

    public void setFloorMax(Short floorMax) {
        this.floorMax = floorMax;
    }

    public boolean isExcludeTopFloor() {
        return excludeTopFloor;
    }

    public void setExcludeTopFloor(boolean excludeTopFloor) {
        this.excludeTopFloor = excludeTopFloor;
    }

    public Financing getFinancing() {
        return financing;
    }

    public void setFinancing(Financing financing) {
        this.financing = financing;
    }

    public LocalDate getMoveInDate() {
        return moveInDate;
    }

    public void setMoveInDate(LocalDate moveInDate) {
        this.moveInDate = moveInDate;
    }

    public Map<Feature, Boolean> getFeatures() {
        return features;
    }

    /** Konieczne wygrywają: cecha podana w obu zbiorach zostaje wyłącznie konieczna. */
    public void setFeatures(Set<Feature> required, Set<Feature> preferred) {
        this.features.clear();
        preferred.forEach(feature -> this.features.put(feature, Boolean.FALSE));
        required.forEach(feature -> this.features.put(feature, Boolean.TRUE));
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
