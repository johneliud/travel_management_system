package com.travelmanagementsystem.travel.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "travels")
public class Travel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "destination_country", nullable = false, length = 100)
    private String destinationCountry;

    @Column(name = "destination_city", nullable = false, length = 100)
    private String destinationCity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "available_slots", nullable = false)
    private Integer availableSlots;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TravelStatus status = TravelStatus.DRAFT;

    @Column(name = "manager_id", nullable = false)
    private Long managerId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "travel_activities", joinColumns = @JoinColumn(name = "travel_id"))
    @OrderColumn(name = "id")
    private List<Activity> activities = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "travel_transport", joinColumns = @JoinColumn(name = "travel_id"))
    @OrderColumn(name = "id")
    private List<Transport> transport = new ArrayList<>();

    protected Travel() {
    }

    public Travel(
            String title,
            String description,
            String destinationCountry,
            String destinationCity,
            LocalDate startDate,
            LocalDate endDate,
            Integer durationDays,
            BigDecimal price,
            Integer capacity,
            Long managerId) {
        this.title = title;
        this.description = description;
        this.destinationCountry = destinationCountry;
        this.destinationCity = destinationCity;
        this.startDate = startDate;
        this.endDate = endDate;
        this.durationDays = durationDays;
        this.price = price;
        this.capacity = capacity;
        this.availableSlots = capacity;
        this.status = TravelStatus.DRAFT;
        this.managerId = managerId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addActivity(Activity activity) {
        this.activities.add(activity);
    }

    public void removeActivity(Activity activity) {
        this.activities.remove(activity);
    }

    public void addTransport(Transport transportItem) {
        this.transport.add(transportItem);
    }

    public void removeTransport(Transport transportItem) {
        this.transport.remove(transportItem);
    }

    public void setStatus(TravelStatus status) {
        this.status = status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDestinationCountry(String destinationCountry) {
        this.destinationCountry = destinationCountry;
    }

    public void setDestinationCity(String destinationCity) {
        this.destinationCity = destinationCity;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setAvailableSlots(Integer availableSlots) {
        this.availableSlots = availableSlots;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getDestinationCountry() {
        return destinationCountry;
    }

    public String getDestinationCity() {
        return destinationCity;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getAvailableSlots() {
        return availableSlots;
    }

    public TravelStatus getStatus() {
        return status;
    }

    public Long getManagerId() {
        return managerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public List<Transport> getTransport() {
        return transport;
    }
}
