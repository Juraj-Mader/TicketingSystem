package com.example.bookingservice.service;

import com.example.bookingservice.client.InventoryServiceClient;
import com.example.bookingservice.entity.Customer;
import com.example.bookingservice.repository.CustomerRepository;
import com.example.bookingservice.request.BookingRequest;
import com.example.bookingservice.response.BookingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final CustomerRepository customerRepository;
    private final InventoryServiceClient inventoryService;

    @Autowired
    public BookingService(final CustomerRepository customerRepository,
                          final InventoryServiceClient inventoryService) {
        this.customerRepository = customerRepository;
        this.inventoryService = inventoryService;
    }

    public BookingResponse createBooking(final BookingRequest request) {

        Customer customer = customerRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));



        return BookingResponse.builder().build();

    }
}
