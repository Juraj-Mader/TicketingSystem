package com.ticketing.order.service;

import com.ticketing.order.client.InventoryServiceClient;
import com.ticketing.order.entity.Order;
import com.ticketing.order.event.BookingEvent;
import com.ticketing.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private BookingEvent bookingEvent() {
        return new BookingEvent(1L, 1L, 2L, new BigDecimal("199.98"));
    }

    @Test
    void orderEvent_inventoryAccepts_updatesInventoryThenSavesOrder() {
        // given: saveAndFlush returns the same order it receives
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        orderService.orderEvent(bookingEvent());

        // then: inventory first, save second
        InOrder order = inOrder(inventoryServiceClient, orderRepository);
        order.verify(inventoryServiceClient).updateInventory(1L, 2L);
        order.verify(orderRepository).saveAndFlush(orderCaptor.capture());

        // and the saved order has the data from the event
        Order saved = orderCaptor.getValue();
        assertThat(saved.getCustomerId()).isEqualTo(1L);
        assertThat(saved.getEventId()).isEqualTo(1L);
        assertThat(saved.getTicketCount()).isEqualTo(2L);
        assertThat(saved.getTotalPrice()).isEqualByComparingTo("199.98");
    }

    @Test
    void orderEvent_inventoryRejects_doesNotSaveOrder() {
        doThrow(new HttpClientErrorException(HttpStatus.CONFLICT))
                .when(inventoryServiceClient).updateInventory(1L, 2L);

        orderService.orderEvent(bookingEvent());

        verifyNoInteractions(orderRepository);
    }

    @Test
    void orderEvent_inventoryDown_throwsAndDoesNotSaveOrder() {
        doThrow(new ResourceAccessException("Connection refused"))
                .when(inventoryServiceClient).updateInventory(1L, 2L);

        assertThatThrownBy(() -> orderService.orderEvent(bookingEvent()))
                .isInstanceOf(ResourceAccessException.class);

        verifyNoInteractions(orderRepository);
    }
}