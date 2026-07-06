package com.example.bookingservice.service;

import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    public BookingResponse createBooking(final BookingRequest request) {
        return BookingResponse.builder().build();

    }
}
