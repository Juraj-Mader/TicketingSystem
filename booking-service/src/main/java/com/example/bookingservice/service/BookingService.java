package com.example.bookingservice.service;

import com.example.bookingservice.Entity.Customer;
import com.example.bookingservice.repository.CustomerRepository;
import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final CustomerRepository customerRepository;

    @Autowired
    public BookingService(final CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public BookingResponse createBooking(final BookingRequest request) {

        Customer customer = customerRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        return BookingResponse.builder().build();

    }
}
