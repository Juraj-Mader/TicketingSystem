package com.ticketing.booking.controller;


import com.ticketing.booking.request.BookingRequest;
import com.ticketing.booking.response.BookingResponse;
import com.ticketing.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping(
            consumes = "application/json",
            produces = "application/json",
            path = "/booking"
    )
    public BookingResponse createBooking(@RequestBody final BookingRequest request) {
        return bookingService.createBooking(request);
    }
}
