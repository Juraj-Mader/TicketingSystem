package com.ticketing.booking.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotNull(message = "ticketCount is required")
    @Positive(message = "ticketCount must be greater than 0")
    private Long ticketCount;
}
