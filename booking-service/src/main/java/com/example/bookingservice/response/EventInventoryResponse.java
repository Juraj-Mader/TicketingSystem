package com.example.bookingservice.response;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EventInventoryResponse {

    private Long eventId;
    private String event;
    private Long capacity;
    private BigDecimal ticketPrice;
}