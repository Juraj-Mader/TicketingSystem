package com.bookingservice.controller;


import com.bookingservice.request.BookingRequest;
import com.bookingservice.response.BookingResponse;
import com.bookingservice.service.BookingService;
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
