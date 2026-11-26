package com.ticketing.inventory;

import com.ticketing.inventory.entity.Event;
import com.ticketing.inventory.exception.CapacityConflictException;
import com.ticketing.inventory.exception.NotEnoughTicketsException;
import com.ticketing.inventory.repository.EventRepository;
import com.ticketing.inventory.repository.VenueRepository;
import com.ticketing.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class InventoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.3.0");

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @BeforeEach
    void resetData() {
        Event event = eventRepository.findById(1L).orElseThrow();
        event.setLeftCapacity(22000L);
        eventRepository.save(event);
    }

    @Test
    void flywayMigrations_loadSampleData() {
        Event event = eventRepository.findById(1L).orElseThrow();

        assertThat(event.getName()).isEqualTo("BMTH Concert");
        assertThat(event.getVenue().getName()).isEqualTo("Stadion Tehelne Pole");
    }

    @Test
    void updateEventCapacity_enoughTickets_decreasesCapacityInDatabase() {
        inventoryService.updateEventCapacity(1L, 2L);

        assertThat(eventRepository.findById(1L).orElseThrow().getLeftCapacity()).isEqualTo(21998L);
    }

    @Test
    void updateEventCapacity_notEnoughTickets_leavesCapacityUnchanged() {
        assertThatThrownBy(() -> inventoryService.updateEventCapacity(1L, 22001L))
                .isInstanceOf(NotEnoughTicketsException.class);

        assertThat(eventRepository.findById(1L).orElseThrow().getLeftCapacity()).isEqualTo(22000L);
    }

    @Test
    void updateVenueCapacity_smallerThanEvent_isRejectedByRealQuery() {
        assertThatThrownBy(() -> inventoryService.updateVenueCapacity(1L, 100L))
                .isInstanceOf(CapacityConflictException.class);

        assertThat(venueRepository.findById(1L).orElseThrow().getTotalCapacity()).isEqualTo(22500L);
    }

    @Test
    void updateEventCapacity_concurrentBookings_neverOversells() throws Exception {
        int bookings = 30;               // 30 x 1000 = 30 000 tickets requested
        long ticketsPerBooking = 1000L;  // only 22 000 available, so max 22 can succeed

        ExecutorService executor = Executors.newFixedThreadPool(bookings);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        List<Future<?>> results = new ArrayList<>();

        for (int i = 0; i < bookings; i++) {
            results.add(executor.submit(() -> {
                startSignal.await();     // every thread waits here...
                try {
                    inventoryService.updateEventCapacity(1L, ticketsPerBooking);
                    succeeded.incrementAndGet();
                } catch (NotEnoughTicketsException e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }

        startSignal.countDown();         // ...and they all start at the same moment
        for (Future<?> result : results) {
            result.get(30, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertThat(succeeded.get()).isEqualTo(22);
        assertThat(rejected.get()).isEqualTo(8);
        assertThat(eventRepository.findById(1L).orElseThrow().getLeftCapacity()).isZero();
    }
}