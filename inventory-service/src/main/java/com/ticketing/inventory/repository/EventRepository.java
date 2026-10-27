package com.ticketing.inventory.repository;

import com.ticketing.inventory.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Modifying
    @Query("UPDATE Event e SET e.leftCapacity = e.leftCapacity - :tickets " +
            "WHERE e.id = :eventId AND e.leftCapacity >= :tickets")
    int decreaseCapacity(@Param("eventId") Long eventId, @Param("tickets") Long tickets);
}