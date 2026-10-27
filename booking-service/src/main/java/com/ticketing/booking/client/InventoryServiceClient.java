package com.ticketing.booking.client;

import com.ticketing.booking.exception.ResourceNotFoundException;
import com.ticketing.booking.response.EventInventoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class InventoryServiceClient {

    @Value("${inventory.service.url}")
    private String inventoryServiceUrl;

    public EventInventoryResponse getInventory(final Long eventId) {
        final RestTemplate restTemplate = new RestTemplate();
        try {
            return restTemplate.getForObject(
                    inventoryServiceUrl + "/event/" + eventId,
                    EventInventoryResponse.class
            );
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Event not found: " + eventId);
        }
    }
}