package com.travelmanagementsystem.travel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class Transport {

    @Column(nullable = false, length = 50)
    private String type;

    @Column
    private String provider;

    @Column
    private String departure;

    @Column
    private String arrival;

    @Column(name = "departure_time")
    private Instant departureTime;

    @Column(name = "arrival_time")
    private Instant arrivalTime;

    protected Transport() {
    }

    public Transport(String type, String provider, String departure, String arrival, Instant departureTime, Instant arrivalTime) {
        this.type = type;
        this.provider = provider;
        this.departure = departure;
        this.arrival = arrival;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public String getType() {
        return type;
    }

    public String getProvider() {
        return provider;
    }

    public String getDeparture() {
        return departure;
    }

    public String getArrival() {
        return arrival;
    }

    public Instant getDepartureTime() {
        return departureTime;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Transport transport = (Transport) obj;
        return java.util.Objects.equals(type, transport.type)
                && java.util.Objects.equals(departure, transport.departure)
                && java.util.Objects.equals(departureTime, transport.departureTime);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(type, departure, departureTime);
    }
}
