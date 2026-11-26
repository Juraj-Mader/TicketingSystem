package com.ticketing.booking.controller;

import com.ticketing.booking.exception.NotEnoughTicketsException;
import com.ticketing.booking.exception.ResourceNotFoundException;
import com.ticketing.booking.request.BookingRequest;
import com.ticketing.booking.response.BookingResponse;
import com.ticketing.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String URL = "/api/v1/booking";
    private static final String VALID_BODY = """
            { "userId": 1, "eventId": 1, "ticketCount": 2 }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @Test
    void createBooking_validRequest_returns200WithTotalPrice() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class))).thenReturn(
                BookingResponse.builder()
                        .userId(1L).eventId(1L).ticketCount(2L)
                        .totalPrice(new BigDecimal("199.98"))
                        .build());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCount").value(2))
                .andExpect(jsonPath("$.totalPrice").value(199.98));
    }

    @Test
    void createBooking_negativeTickets_returns400WithFieldError() throws Exception {
        String body = """
                { "userId": 1, "eventId": 1, "ticketCount": -5 }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ticketCount").value("ticketCount must be greater than 0"));

        verifyNoInteractions(bookingService);
    }

    @Test
    void createBooking_customerMissing_returns404() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new ResourceNotFoundException("Customer not found: 1"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer not found: 1"));
    }

    @Test
    void createBooking_notEnoughTickets_returns409() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new NotEnoughTicketsException("Not enough tickets"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void createBooking_inventoryDown_returns503() throws Exception {
        when(bookingService.createBooking(any(BookingRequest.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Inventory service is not available"));
    }
}