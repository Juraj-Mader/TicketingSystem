package com.example.bookingservice.client;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class InventoryServiceClient {

    public InventoryResponse getInventory(final Long eventId) {
        final RestTemplate restTemplate = new RestTemplate();
    }
}
