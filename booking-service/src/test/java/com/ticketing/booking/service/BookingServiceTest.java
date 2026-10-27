package com.ticketing.booking.service;

import com.ticketing.booking.client.InventoryServiceClient;
import com.ticketing.booking.entity.Customer;
import com.ticketing.booking.event.BookingEvent;
import com.ticketing.booking.exception.NotEnoughTicketsException;
import com.ticketing.booking.exception.ResourceNotFoundException;
import com.ticketing.booking.repository.CustomerRepository;
import com.ticketing.booking.request.BookingRequest;
import com.ticketing.booking.response.BookingResponse;
import com.ticketing.booking.response.EventInventoryResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private BookingService bookingService;

    @Captor
    private ArgumentCaptor<BookingEvent> bookingEventCaptor;

    // helpers

    private BookingRequest request(long userId, long eventId, long ticketCount) {
        return BookingRequest.builder()
                .userId(userId)
                .eventId(eventId)
                .ticketCount(ticketCount)
                .build();
    }

    private EventInventoryResponse inventory(long capacity, String price) {
        EventInventoryResponse response = new EventInventoryResponse();
        response.setEventId(1L);
        response.setEvent("BMTH Concert");
        response.setCapacity(capacity);
        response.setTicketPrice(new BigDecimal(price));
        return response;
    }

    private Customer customer() {
        return new Customer(1L, "Jozko Mrkvicka", "jozko@example.com", "Bratislavova 12");
    }

    // tests

    @Test
    void createBooking_validRequest_sendsEventAndReturnsResponse() {
        // given
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(inventoryServiceClient.getInventory(1L)).thenReturn(inventory(100, "99.99"));

        // when
        BookingResponse response = bookingService.createBooking(request(1, 1, 2));

        // then: the event sent to Kafka is correct
        verify(kafkaTemplate).send(eq("booking-events"), bookingEventCaptor.capture());
        BookingEvent sentEvent = bookingEventCaptor.getValue();
        assertThat(sentEvent.getUserId()).isEqualTo(1L);
        assertThat(sentEvent.getEventId()).isEqualTo(1L);
        assertThat(sentEvent.getTicketCount()).isEqualTo(2L);
        assertThat(sentEvent.getTotalPrice()).isEqualByComparingTo("199.98");

        // and the response returned to the client is correct
        assertThat(response.getTotalPrice()).isEqualByComparingTo("199.98");
    }

    @Test
    void createBooking_exactlyEnoughTickets_succeeds() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(inventoryServiceClient.getInventory(1L)).thenReturn(inventory(2, "10.00"));

        BookingResponse response = bookingService.createBooking(request(1, 1, 2));

        assertThat(response.getTicketCount()).isEqualTo(2L);
    }

    @Test
    void createBooking_customerMissing_throwsNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.createBooking(request(99, 1, 2)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Customer not found: 99");

        verifyNoInteractions(inventoryServiceClient, kafkaTemplate);
    }

    @Test
    void createBooking_eventMissing_throwsNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(inventoryServiceClient.getInventory(99L))
                .thenThrow(new ResourceNotFoundException("Event not found: 99"));

        assertThatThrownBy(() -> bookingService.createBooking(request(1, 99, 2)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void createBooking_notEnoughTickets_throwsConflict() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer()));
        when(inventoryServiceClient.getInventory(1L)).thenReturn(inventory(1, "99.99"));

        assertThatThrownBy(() -> bookingService.createBooking(request(1, 1, 2)))
                .isInstanceOf(NotEnoughTicketsException.class)
                .hasMessageContaining("requested 2, available 1");

        verifyNoInteractions(kafkaTemplate);
    }
}