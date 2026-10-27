package com.ticketing.inventory.service;

import com.ticketing.inventory.entity.Event;
import com.ticketing.inventory.entity.Venue;
import com.ticketing.inventory.exception.NotEnoughTicketsException;
import com.ticketing.inventory.exception.ResourceNotFoundException;
import com.ticketing.inventory.repository.EventRepository;
import com.ticketing.inventory.repository.VenueRepository;
import com.ticketing.inventory.response.EventInventoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private InventoryService inventoryService;

    // getEventInventory

    @Test
    void getEventInventory_eventExists_returnsResponse() {
        // given
        Venue venue = new Venue(1L, "Stadion Tehelne Pole", "Bratislava", 22500L);
        Event event = new Event(1L, "BMTH Concert", 22000L, 21000L, venue, new BigDecimal("99.99"));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        // when
        EventInventoryResponse response = inventoryService.getEventInventory(1L);

        // then
        assertThat(response.getEventId()).isEqualTo(1L);
        assertThat(response.getEvent()).isEqualTo("BMTH Concert");
        assertThat(response.getCapacity()).isEqualTo(21000L);
        assertThat(response.getTicketPrice()).isEqualByComparingTo("99.99");
    }

    @Test
    void getEventInventory_eventMissing_throwsNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getEventInventory(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found: 99");
    }

    // updateEventCapacity

    @Test
    void updateEventCapacity_enoughTickets_decreasesCapacity() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.decreaseCapacity(1L, 2L)).thenReturn(1);

        inventoryService.updateEventCapacity(1L, 2L);

        verify(eventRepository).decreaseCapacity(1L, 2L);
    }

    @Test
    void updateEventCapacity_notEnoughTickets_throwsConflict() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        when(eventRepository.decreaseCapacity(1L, 999999L)).thenReturn(0);

        assertThatThrownBy(() -> inventoryService.updateEventCapacity(1L, 999999L))
                .isInstanceOf(NotEnoughTicketsException.class);
    }

    @Test
    void updateEventCapacity_eventMissing_throwsNotFound() {
        when(eventRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> inventoryService.updateEventCapacity(99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).decreaseCapacity(anyLong(), anyLong());
    }

    @Test
    void updateEventCapacity_negativeTickets_throwsBadRequest() {
        assertThatThrownBy(() -> inventoryService.updateEventCapacity(1L, -5L))
                .isInstanceOf(IllegalArgumentException.class);

        verify(eventRepository, never()).decreaseCapacity(anyLong(), anyLong());
    }
}