package com.ecoorganicstore.order.service;

import com.ecoorganicstore.order.domain.Order;
import com.ecoorganicstore.order.repo.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceTest {
    private OrderRepository repository;
    private OrderService service;

    @BeforeEach
    void setUp() {
        repository = mock(OrderRepository.class);
        service = new OrderService(repository, mock(RestClient.class), "internal-secret", mock(OrderNotifier.class));
    }

    @Test
    void ordersByUserReturnsNewestFirstWithoutSortingInTheDatabase() {
        Order older = order("HC-OLD", Instant.parse("2026-01-01T00:00:00Z"));
        Order newer = order("HC-NEW", Instant.parse("2026-06-01T00:00:00Z"));
        Order undated = order("HC-NONE", null);
        when(repository.findByUserId("user-1")).thenReturn(List.of(older, undated, newer));

        List<String> numbers = service.ordersByUser("user-1").stream().map(Order::getOrderNumber).toList();

        assertEquals(List.of("HC-NEW", "HC-OLD", "HC-NONE"), numbers);
    }

    private static Order order(String number, Instant createdAt) {
        Order order = new Order();
        order.setOrderNumber(number);
        order.setUserId("user-1");
        order.setCreatedAt(createdAt);
        return order;
    }
}
