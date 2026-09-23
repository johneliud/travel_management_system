package com.travelmanagementsystem.travel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;

@Embeddable
public class Activity {

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "day_number")
    private Integer dayNumber;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    protected Activity() {
    }

    public Activity(String name, String description, Integer dayNumber, LocalTime startTime, LocalTime endTime) {
        this.name = name;
        this.description = description;
        this.dayNumber = dayNumber;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDayNumber() {
        return dayNumber;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Activity activity = (Activity) obj;
        return java.util.Objects.equals(name, activity.name)
                && java.util.Objects.equals(dayNumber, activity.dayNumber)
                && java.util.Objects.equals(startTime, activity.startTime);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, dayNumber, startTime);
    }
}
