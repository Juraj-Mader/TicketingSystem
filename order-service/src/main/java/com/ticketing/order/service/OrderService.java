package com.ticketing.order.service;

import com.ticketing.order.client.InventoryServiceClient;
import com.ticketing.order.entity.Order;
import com.ticketing.order.event.BookingEvent;
import com.ticketing.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryServiceClient inventoryServiceClient;

    @KafkaListener(topics = "booking-events", groupId = "order-service")
    public void orderEvent(BookingEvent bookingEvent) {
        log.info("Order event received: {}", bookingEvent);

        // Reserve the tickets first
        try {
            inventoryServiceClient.updateInventory(bookingEvent.getEventId(), bookingEvent.getTicketCount());
        } catch (HttpClientErrorException ex) {
            log.warn("Order rejected for event {}: {} {}",
                    bookingEvent.getEventId(), ex.getStatusCode(), ex.getResponseBodyAsString());
            return;
        }

        // Save the order only after the tickets were reserved
        Order order = orderRepository.saveAndFlush(createOrder(bookingEvent));
        log.info("Order {} saved for event {}, tickets: {}",
                order.getId(), order.getEventId(), order.getTicketCount());
    }

    private Order createOrder(BookingEvent bookingEvent) {
        return Order.builder()
                .customerId(bookingEvent.getUserId())
                .eventId(bookingEvent.getEventId())
                .ticketCount(bookingEvent.getTicketCount())
                .totalPrice(bookingEvent.getTotalPrice())
                .build();
    }
}