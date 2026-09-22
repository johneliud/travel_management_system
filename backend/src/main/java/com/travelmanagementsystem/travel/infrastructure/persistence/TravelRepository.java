package com.travelmanagementsystem.travel.infrastructure.persistence;

import com.travelmanagementsystem.travel.domain.Travel;
import com.travelmanagementsystem.travel.domain.TravelStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TravelRepository extends JpaRepository<Travel, Long> {

    List<Travel> findByManagerId(Long managerId);

    List<Travel> findByStatus(TravelStatus status);

    @Query("SELECT t FROM Travel t WHERE t.destinationCountry = :country AND t.status = 'PUBLISHED'")
    List<Travel> findPublishedByCountry(@Param("country") String country);

    @Query("SELECT t FROM Travel t WHERE t.startDate >= :from AND t.startDate <= :to AND t.status = 'PUBLISHED'")
    List<Travel> findPublishedByDateRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT t FROM Travel t WHERE t.availableSlots > 0 AND t.status = 'PUBLISHED'")
    List<Travel> findAvailable();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Travel t WHERE t.id = :id")
    Optional<Travel> findByIdForUpdate(@Param("id") Long id);
}
